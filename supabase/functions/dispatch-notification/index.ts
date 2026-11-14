type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  schema: string;
  table: string;
  record: Record<string, unknown> | null;
  old_record: Record<string, unknown> | null;
};

type InboxRow = {
  id: string;
  recipient_id: string;
  title: string;
  summary: string;
  content: string;
  target_type: string;
  target_id: string | null;
  push_device_cursor?: string | null;
};

const encoder = new TextEncoder();
let fcmAccessToken: { value: string; expiresAt: number } | null = null;
const INBOX_BATCH_SIZE = 50;
const INBOX_INSERT_BATCH_SIZE = 500;
const MAX_ROWS_PER_INVOCATION = 250;
const MAX_DEVICES_PER_INVOCATION = 2_000;
const DEVICES_PER_ROW_BATCH = 100;
const MAX_INVOCATION_MS = 20_000;
const PATCH_ID_BATCH_SIZE = 100;
const FCM_MAX_ATTEMPTS = 3;
const FCM_RETRY_DELAY_MS = 500;

class DispatchDeadlineError extends Error {}

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "POST required" }, 405);

  const webhookSecret = Deno.env.get("WEBHOOK_SHARED_SECRET");
  if (!webhookSecret || request.headers.get("x-webhook-secret") !== webhookSecret) {
    return json({ error: "Unauthorized webhook" }, 401);
  }

  try {
    const payload = await request.json() as WebhookPayload;
    const source = toSourceNotification(payload);
    if (!source) return json({ delivered: 0, ignored: true });

    if (payload.table === "housing_posts" && source.broadcastAuthorId) {
      const allowed = await claimHousingBroadcast(source.broadcastAuthorId, source.sourceId);
      if (!allowed) return json({ delivered: 0, ignored: true, rate_limited: true }, 429);
    }

    const recipients = await recipientIds(source.recipientId, source.excludeUserId);
    if (recipients.length === 0) return json({ delivered: 0, ignored: true });

    const dedupeKey = `${payload.table}:${source.sourceId}`;
    const rows = recipients.map((recipientId) => ({
      recipient_id: recipientId,
      title: source.title,
      summary: source.summary,
      content: source.content,
      category: source.category,
      author: source.author,
      target_type: source.targetType,
      target_id: source.targetId,
      source_table: payload.table,
      source_id: source.sourceId,
      dedupe_key: dedupeKey,
      published_at: source.publishedAt,
    }));
    // Keep recipient fan-out inserts bounded so a large verified-user list does
    // not exceed PostgREST request limits or get partially dropped as one body.
    for (let offset = 0; offset < rows.length; offset += INBOX_INSERT_BATCH_SIZE) {
      await supabaseRest("notification_inbox?on_conflict=recipient_id,dedupe_key", {
        method: "POST",
        headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
        body: JSON.stringify(rows.slice(offset, offset + INBOX_INSERT_BATCH_SIZE)),
      });
    }

    // Duplicate webhook deliveries can resume a pending push; a stale PROCESSING
    // claim is released after five minutes if an Edge invocation was interrupted.
    await supabaseRest(
      `notification_inbox?push_status=eq.PROCESSING&push_claimed_at=lt.${new Date(Date.now() - 5 * 60_000).toISOString()}`,
      { method: "PATCH", body: JSON.stringify({ push_status: "PENDING", push_claimed_at: null }) },
    );
    const deadline = Date.now() + MAX_INVOCATION_MS;
    let deliveredTotal = 0;
    let pendingTotal = 0;
    let processedRows = 0;
    let deferred = false;
    while (true) {
      if (Date.now() >= deadline) {
        deferred = true;
        break;
      }
      if (processedRows >= MAX_ROWS_PER_INVOCATION) {
        deferred = true;
        break;
      }
      const pageSize = Math.min(INBOX_BATCH_SIZE, MAX_ROWS_PER_INVOCATION - processedRows);
      const pending = await supabaseRest<InboxRow[]>(
        `notification_inbox?select=id,recipient_id,title,summary,content,target_type,target_id,push_device_cursor&push_status=eq.PENDING&order=id.asc&limit=${pageSize}`,
      );
      const ids = pending.map((row) => row.id);
      if (ids.length === 0) break;

      const claimed = await patchInboxRows(
        ids,
        { push_status: "PROCESSING", push_claimed_at: new Date().toISOString() },
        "&push_status=eq.PENDING",
        true,
      );
      if (claimed.length === 0) return json({ delivered: deliveredTotal, duplicate: true });

      const heldClaimIds = new Set(claimed.map((row) => row.id));
      let batchFailed = false;
      try {
        ensureWithinDeadline(deadline);
        let accessToken: string | null = null;
        let processedDevices = 0;
        for (const row of claimed) {
          if (processedDevices >= MAX_DEVICES_PER_INVOCATION) {
            deferred = true;
            break;
          }
          ensureWithinDeadline(deadline);
          const cursor = row.push_device_cursor
            ? `&device_id=gt.${encodeURIComponent(row.push_device_cursor)}`
            : "";
          const devices = await supabaseRest<Array<{ device_id: string; token: string }>>(
            `notification_devices?select=device_id,token&user_id=eq.${row.recipient_id}${cursor}&order=device_id.asc&limit=${DEVICES_PER_ROW_BATCH}`,
          );
          let rowFailed = false;
          let rowDeferred = false;
          for (const device of devices) {
            if (processedDevices >= MAX_DEVICES_PER_INVOCATION) {
              rowDeferred = true;
              break;
            }
            ensureWithinDeadline(deadline);
            accessToken ??= await getFcmAccessToken();
            const result = await sendFcmWithRetry(accessToken, device.token, row, deadline);
            if (result === "SENT") {
              deliveredTotal++;
            } else if (result === "UNREGISTERED") {
              await supabaseRest(`notification_devices?token=eq.${encodeURIComponent(device.token)}`, { method: "DELETE" });
            } else {
              rowFailed = true;
            }
            if (rowFailed) break;
            await patchInboxRows(
              [row.id],
              { push_device_cursor: device.device_id, push_claimed_at: new Date().toISOString() },
              "&push_status=eq.PROCESSING",
            );
            processedDevices++;
          }

          if (rowFailed) {
            await patchInboxRows(
              [row.id],
              { push_status: "PENDING", push_claimed_at: null },
              "&push_status=eq.PROCESSING",
            );
            heldClaimIds.delete(row.id);
            pendingTotal++;
            batchFailed = true;
          } else if (rowDeferred || devices.length === DEVICES_PER_ROW_BATCH) {
            await patchInboxRows(
              [row.id],
              { push_status: "PENDING", push_claimed_at: null },
              "&push_status=eq.PROCESSING",
            );
            heldClaimIds.delete(row.id);
            deferred = true;
          } else {
            await patchInboxRows(
              [row.id],
              { push_status: "SENT", push_claimed_at: null, push_device_cursor: null },
              "&push_status=eq.PROCESSING",
            );
            heldClaimIds.delete(row.id);
            processedRows++;
          }
        }
        if (deferred) {
          await patchInboxRows(
            [...heldClaimIds],
            { push_status: "PENDING", push_claimed_at: null },
            "&push_status=eq.PROCESSING",
          );
          heldClaimIds.clear();
        }
      } catch (error) {
        await patchInboxRows(
          [...heldClaimIds],
          { push_status: "PENDING", push_claimed_at: null },
          "&push_status=eq.PROCESSING",
        );
        if (error instanceof DispatchDeadlineError) {
          deferred = true;
          break;
        }
        throw error;
      }

      if (pending.length < pageSize || batchFailed || deferred) break;
    }
    // Keep the webhook retryable while durable PENDING work remains. A 2xx here
    // would acknowledge the delivery even though some recipients were not sent.
    if (deferred || pendingTotal > 0) {
      return json({ delivered: deliveredTotal, pending: pendingTotal, deferred }, 503);
    }
    if (deliveredTotal === 0 && pendingTotal === 0) return json({ delivered: 0, ignored: true });
    return json({ delivered: deliveredTotal, pending: pendingTotal });
  } catch (error) {
    console.error("notification dispatch failed", error);
    return json({ error: "Notification dispatch failed" }, 500);
  }
});

function toSourceNotification(payload: WebhookPayload) {
  const row = payload.record;
  if (!row || payload.schema !== "public" || !["events", "notifications", "housing_posts"].includes(payload.table)) return null;

  if (payload.table === "events") {
    if (row.status !== "PUBLISHED") return null;
    if (payload.type === "UPDATE" && payload.old_record?.status === "PUBLISHED") return null;
    return {
      sourceId: requiredString(row.id), recipientId: null, excludeUserId: null,
      title: requiredString(row.title),
      summary: "Sự kiện mới đã được đăng.",
      content: [stringValue(row.description), stringValue(row.location), stringValue(row.start_at)].filter(Boolean).join("\n"),
      category: "ACTIVITIES", author: stringValue(row.organizer_name) || "UTH Connect",
      targetType: "EVENT", targetId: requiredString(row.id),
      broadcastAuthorId: null,
      publishedAt: stringValue(row.updated_at) || new Date().toISOString(),
    };
  }

  if (payload.table === "housing_posts") {
    if (payload.type !== "INSERT" || row.status !== "OPEN") return null;
    const price = Number(row.monthly_rent_vnd ?? 0);
    return {
      sourceId: requiredString(row.id), recipientId: null, excludeUserId: stringValue(row.author_id),
      title: requiredString(row.title), summary: "Có bài đăng trọ mới.",
      content: [stringValue(row.description), stringValue(row.address_label), price > 0 ? `${price.toLocaleString("vi-VN")} VND` : ""].filter(Boolean).join("\n"),
      category: "GENERAL", author: "Ghép trọ UTH",
      targetType: "HOUSING", targetId: requiredString(row.id),
      broadcastAuthorId: nullableString(row.author_id),
      publishedAt: stringValue(row.created_at) || new Date().toISOString(),
    };
  }

  if (payload.type === "DELETE") return null;
  const publishedAt = stringValue(row.published_at);
  if (publishedAt && Date.parse(publishedAt) > Date.now()) return null;
  const oldPublishedAt = stringValue(payload.old_record?.published_at);
  if (payload.type === "UPDATE" && oldPublishedAt && Date.parse(oldPublishedAt) <= Date.now()) return null;
  const sourceCategory = stringValue(row.category);
  const academic = ["ACADEMIC", "EXAMINATION"].includes(sourceCategory);
  return {
    sourceId: requiredString(row.id), recipientId: nullableString(row.recipient_id), excludeUserId: null,
    title: requiredString(row.title), summary: stringValue(row.summary), content: stringValue(row.content),
    category: academic ? "ACADEMIC" : "GENERAL", author: stringValue(row.author) || "Nhà trường",
    targetType: academic ? "ACADEMIC" : "SYSTEM", targetId: requiredString(row.id),
    broadcastAuthorId: null,
    publishedAt: publishedAt || new Date().toISOString(),
  };
}

async function claimHousingBroadcast(authorId: string, sourceId: string): Promise<boolean> {
  return await supabaseRest<boolean>("rpc/claim_housing_notification_broadcast", {
    method: "POST",
    body: JSON.stringify({ p_author_id: authorId, p_source_id: sourceId }),
  });
}

async function recipientIds(recipientId: string | null, excludedUserId: string | null): Promise<string[]> {
  if (recipientId) return [recipientId];
  // Phân trang để không bỏ sót hồ sơ khi vượt giới hạn 1.000 hàng mặc định của Supabase
  // Phân trang profiles bằng keyset để không bỏ sót hồ sơ
  const allProfiles: Array<{ id: string }> = [];
  const pageSize = 1000;
  let lastProfileId: string | undefined = undefined;

  while (true) {
    const queryParams = [
      "select=id",
      "student_id_verified=eq.true",
      "order=id.asc",
      `limit=${pageSize}`
    ];
    if (lastProfileId) queryParams.push(`id=gt.${lastProfileId}`);

    const page = await supabaseRest<Array<{ id: string }>>(
      `profiles?${queryParams.join("&")}`,
    );
    allProfiles.push(...page);
    if (page.length < pageSize) break;
    lastProfileId = page[page.length - 1].id;
  }
  return allProfiles.map((profile) => profile.id).filter((id) => id !== excludedUserId);
}

async function sendFcm(accessToken: string, token: string, row: InboxRow): Promise<"SENT" | "FAILED" | "UNREGISTERED"> {
  const serviceAccount = JSON.parse(fcmServiceAccountJson());
  const messageBody = (row.summary || row.content).slice(0, 180);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          // Data-only delivery keeps recipient validation inside the Android service;
          // an FCM notification payload would be auto-displayed before that check.
          data: {
            notification_id: row.id,
            recipient_id: row.recipient_id,
            target_type: row.target_type,
            target_id: row.target_id ?? "",
            title: row.title,
            body: messageBody,
          },
          android: { priority: "HIGH", notification: { channel_id: "uth_updates" } },
        },
      }),
    },
  );
  if (response.ok) return "SENT";
  const errorBody = await response.text();
  console.error("FCM send failed", response.status, errorBody.slice(0, 500));
  // Chỉ xóa token khi FCM xác nhận UNREGISTERED trong payload — tránh xóa nhầm khi 404 do cấu hình sai
  if (errorBody.includes("UNREGISTERED")) return "UNREGISTERED";
  return "FAILED";
}

async function sendFcmWithRetry(
  accessToken: string,
  token: string,
  row: InboxRow,
  deadline: number,
): Promise<"SENT" | "FAILED" | "UNREGISTERED"> {
  for (let attempt = 0; attempt < FCM_MAX_ATTEMPTS; attempt++) {
    ensureWithinDeadline(deadline);
    try {
      const result = await sendFcm(accessToken, token, row);
      if (result !== "FAILED" || attempt === FCM_MAX_ATTEMPTS - 1) return result;
    } catch (error) {
      console.error("FCM send request failed", error);
      if (attempt === FCM_MAX_ATTEMPTS - 1) return "FAILED";
    }
    const delayMs = FCM_RETRY_DELAY_MS * 2 ** attempt;
    ensureWithinDeadline(deadline, delayMs);
    await new Promise((resolve) => setTimeout(resolve, delayMs));
  }
  return "FAILED";
}

async function patchInboxRows(
  ids: string[],
  update: Record<string, unknown>,
  extraFilter = "",
  returnRows = false,
): Promise<InboxRow[]> {
  const updated: InboxRow[] = [];
  for (let offset = 0; offset < ids.length; offset += PATCH_ID_BATCH_SIZE) {
    const batch = ids.slice(offset, offset + PATCH_ID_BATCH_SIZE);
    const response = await supabaseRest<InboxRow[]>(
      `notification_inbox?id=in.(${batch.join(",")})${extraFilter}`,
      {
        method: "PATCH",
        headers: returnRows ? { Prefer: "return=representation" } : undefined,
        body: JSON.stringify(update),
      },
    );
    if (returnRows) updated.push(...response);
  }
  return updated;
}

function ensureWithinDeadline(deadline: number, additionalDelayMs = 0) {
  if (Date.now() + additionalDelayMs >= deadline) {
    throw new DispatchDeadlineError("Notification dispatch time budget reached");
  }
}

async function getFcmAccessToken(): Promise<string> {
  if (fcmAccessToken && fcmAccessToken.expiresAt > Date.now() + 60_000) return fcmAccessToken.value;
  const account = JSON.parse(fcmServiceAccountJson());
  const now = Math.floor(Date.now() / 1000);
  const jwtHeader = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const jwtClaims = base64Url(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const signingInput = `${jwtHeader}.${jwtClaims}`;
  const keyBytes = decodePem(account.private_key);
  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", privateKey, encoder.encode(signingInput));
  const assertion = `${signingInput}.${base64UrlBytes(new Uint8Array(signature))}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`FCM OAuth token request failed: ${response.status}`);
  const token = await response.json() as { access_token: string; expires_in: number };
  fcmAccessToken = { value: token.access_token, expiresAt: Date.now() + token.expires_in * 1000 };
  return token.access_token;
}

async function supabaseRest<T = unknown>(path: string, init: RequestInit = {}): Promise<T> {
  const url = requiredEnv("SUPABASE_URL").replace(/\/$/, "");
  const key = supabaseSecretKey();
  const headers = new Headers({
    apikey: key,
    "Content-Type": "application/json",
  });
  // Khóa sb_ không phải JWT — chỉ gửi qua apikey; JWT legacy mới cần Authorization Bearer
  if (!key.startsWith("sb_")) headers.set("Authorization", `Bearer ${key}`);
  new Headers(init.headers).forEach((value, name) => headers.set(name, value));
  const response = await fetch(`${url}/rest/v1/${path}`, {
    ...init,
    headers,
  });
  if (!response.ok) throw new Error(`Supabase REST ${response.status}: ${(await response.text()).slice(0, 500)}`);
  const body = await response.text();
  return (body ? JSON.parse(body) : null) as T;
}

function supabaseSecretKey(): string {
  const legacyKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (legacyKey) return legacyKey;
  const keys = JSON.parse(requiredEnv("SUPABASE_SECRET_KEYS")) as Record<string, string>;
  const key = keys.default ?? Object.values(keys)[0];
  if (!key) throw new Error("No Supabase server-side secret key is available");
  return key;
}

function decodePem(pem: string): ArrayBuffer {
  const bytes = Uint8Array.from(
    atob(pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "")),
    (char) => char.charCodeAt(0),
  );
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
}

function base64Url(value: string): string {
  return base64UrlBytes(encoder.encode(value));
}

function base64UrlBytes(value: Uint8Array): string {
  let binary = "";
  for (const byte of value) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function requiredEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing ${name} secret`);
  return value;
}

function fcmServiceAccountJson(): string {
  return Deno.env.get("FCM_SERVICE_ACCOUNT_JSON")
    ?? requiredEnv("FIREBASE_SERVICE_ACCOUNT");
}

function requiredString(value: unknown): string {
  if (typeof value !== "string" || !value) throw new Error("Webhook row is missing a required string");
  return value;
}

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function nullableString(value: unknown): string | null {
  return typeof value === "string" && value ? value : null;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
