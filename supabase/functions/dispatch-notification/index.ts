type WebhookEventPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  schema: string;
  table: string;
  record: Record<string, unknown> | null;
  old_record: Record<string, unknown> | null;
  continuationDepth?: number;
};

type WebhookPayload = WebhookEventPayload | {
  type: "DISPATCH_PENDING";
  continuationDepth?: number;
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
  push_attempts?: number;
};

type DispatchProgress = {
  dedupe_key: string;
  last_recipient_id: string | null;
  source_payload?: WebhookEventPayload | null;
  fanout_complete?: boolean;
  recovery_attempts?: number;
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
const MAX_CONTINUATION_DEPTH = 4;
const MAX_FANOUT_RECOVERY_ATTEMPTS = 5;
const FANOUT_RECOVERY_DELAY_MS = 60_000;
const MAX_PUSH_ATTEMPTS = 5;
const PUSH_RETRY_DELAY_MS = 60_000;
const CONTINUATION_TIMEOUT_MS = 2_000;
const CLEANUP_BUDGET_MS = 5_000;

class DispatchDeadlineError extends Error {}

type FcmResult = "SENT" | "FAILED" | "UNREGISTERED" | "PERMANENT_FAILURE";
type ContinuationResult = "SENT" | "FAILED" | "TIMED_OUT" | "SKIPPED";

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "POST required" }, 405);

  const webhookSecret = Deno.env.get("WEBHOOK_SHARED_SECRET");
  if (!webhookSecret || !(await secretsEqual(request.headers.get("x-webhook-secret") ?? "", webhookSecret))) {
    return json({ error: "Unauthorized webhook" }, 401);
  }

  try {
    const payload = await request.json() as WebhookPayload;
    const deadline = Date.now() + MAX_INVOCATION_MS;
    if (payload.type === "DISPATCH_PENDING") {
      const incompleteFanouts = await supabaseRest<DispatchProgress[]>(
        `notification_dispatch_progress?select=dedupe_key,source_payload,recovery_attempts,last_recipient_id&fanout_complete=eq.false&fanout_failed=eq.false&source_payload=not.is.null&or=(recovery_retry_at.is.null,recovery_retry_at.lte.${encodeURIComponent(new Date().toISOString())})&order=updated_at.asc&limit=10`,
        {},
        deadline,
      );
      for (const progress of incompleteFanouts) {
        if (progress.source_payload) {
          const continuationResult = await queueContinuation(
            payload.continuationDepth ?? 0,
            progress.source_payload,
          );
          if (continuationResult === "SKIPPED") continue;
          const currentProgress = await supabaseRest<DispatchProgress[]>(
            `notification_dispatch_progress?select=last_recipient_id,fanout_complete&dedupe_key=eq.${
              encodeURIComponent(progress.dedupe_key)
            }&limit=1`,
            {},
            deadline,
          );
          const latestProgress = currentProgress[0];
          if (!latestProgress || latestProgress.fanout_complete) continue;
          const cursorAdvanced = latestProgress.last_recipient_id !== progress.last_recipient_id;
          const timedOutWithoutProgress = continuationResult === "TIMED_OUT" && !cursorAdvanced;
          let attempts = progress.recovery_attempts ?? 0;
          if (cursorAdvanced || continuationResult === "SENT") attempts = 0;
          else if (continuationResult === "FAILED" || timedOutWithoutProgress) {
            attempts = Math.min(attempts + 1, MAX_FANOUT_RECOVERY_ATTEMPTS);
          }
          // A timeout is ambiguous, so only a confirmed failed response can end recovery.
          const terminal = continuationResult === "FAILED" && attempts >= MAX_FANOUT_RECOVERY_ATTEMPTS;
          const cursorFilter = latestProgress.last_recipient_id === null
            ? "last_recipient_id=is.null"
            : `last_recipient_id=eq.${encodeURIComponent(latestProgress.last_recipient_id)}`;
          await supabaseRest(
            `notification_dispatch_progress?dedupe_key=eq.${encodeURIComponent(progress.dedupe_key)}&fanout_complete=eq.false&${cursorFilter}`,
            {
              method: "PATCH",
              body: JSON.stringify({
                recovery_attempts: attempts,
                recovery_retry_at: terminal
                  ? null
                  : new Date(
                    Date.now() + FANOUT_RECOVERY_DELAY_MS * 2 ** (Math.max(attempts, 1) - 1),
                  ).toISOString(),
                fanout_failed: terminal,
                updated_at: new Date().toISOString(),
              }),
            },
            deadline,
          );
          // Save the backoff, then leave room in this invocation for due push rows.
          if (timedOutWithoutProgress) break;
        }
      }

      const dueNotifications = await supabaseRest<Array<{ source_payload: WebhookEventPayload }>>(
        "rpc/claim_due_notification_fanouts",
        { method: "POST", body: JSON.stringify({ p_limit: 10 }) },
        deadline,
      );
      await Promise.all(
        dueNotifications.map(({ source_payload: sourcePayload }) =>
          queueContinuation(payload.continuationDepth ?? 0, sourcePayload)
        ),
      );
    } else {
      const source = toSourceNotification(payload);
      if (!source) return json({ delivered: 0, ignored: true });

      try {
        const dedupeKey = `${payload.table}:${source.sourceId}`;
        await supabaseRest("notification_dispatch_progress?on_conflict=dedupe_key", {
          method: "POST",
          headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
          body: JSON.stringify({ dedupe_key: dedupeKey, source_payload: payload, fanout_complete: false }),
        }, deadline);
        if (payload.table === "housing_posts" && source.broadcastAuthorId) {
          const allowed = await claimHousingBroadcast(source.broadcastAuthorId, source.sourceId, deadline);
          if (!allowed) {
            await markFanoutComplete(dedupeKey, deadline);
            return json({ delivered: 0, ignored: true, rate_limited: true }, 429);
          }
        }

        const progressRows = await supabaseRest<DispatchProgress[]>(
          `notification_dispatch_progress?select=dedupe_key,last_recipient_id&dedupe_key=eq.${encodeURIComponent(dedupeKey)}&limit=1`,
          {},
          deadline,
        );
        let lastRecipientId = progressRows[0]?.last_recipient_id ?? null;
        while (true) {
          const recipients = await recipientIdPage(
            source.recipientId,
            source.excludeUserId,
            lastRecipientId,
            deadline,
          );
          if (recipients.length === 0) break;

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

          // Persist each bounded page before fetching the next one, so a later
          // invocation resumes after the last successfully inserted recipient.
          await supabaseRest("notification_inbox?on_conflict=recipient_id,dedupe_key", {
            method: "POST",
            headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
            body: JSON.stringify(rows),
          }, deadline);
          lastRecipientId = recipients[recipients.length - 1];
          await supabaseRest("notification_dispatch_progress?on_conflict=dedupe_key", {
            method: "POST",
            headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
            body: JSON.stringify({
              dedupe_key: dedupeKey,
              last_recipient_id: lastRecipientId,
            }),
          }, deadline);
          if (recipients.length < INBOX_INSERT_BATCH_SIZE) break;
        }
        await markFanoutComplete(dedupeKey, deadline);
      } catch (error) {
        // Preserve the source payload so a later invocation resumes from the
        // last recipient page whose inbox insert and cursor update both succeeded.
        await queueContinuation(payload.continuationDepth ?? 0, payload);
        throw error;
      }
    }

    // Recover a bounded set of expired leases. Attempts are counted when an FCM
    // request starts, so rows only claimed before interruption keep their budget.
    const staleClaimCutoff = new Date(Date.now() - 5 * 60_000).toISOString();
    const staleClaims = await supabaseRest<Array<{ id: string; push_attempts?: number }>>(
      `notification_inbox?select=id,push_attempts&push_status=eq.PROCESSING&push_claimed_at=lt.${encodeURIComponent(staleClaimCutoff)}&order=push_claimed_at.asc&limit=${INBOX_BATCH_SIZE}`,
      {},
      deadline,
    );
    const staleGroups = new Map<number, string[]>();
    for (const row of staleClaims) {
      const attempts = row.push_attempts ?? 0;
      staleGroups.set(attempts, [...(staleGroups.get(attempts) ?? []), row.id]);
    }
    for (const [attempts, ids] of staleGroups) {
      const terminal = attempts >= MAX_PUSH_ATTEMPTS;
      await patchInboxRows(
        ids,
        {
          push_status: terminal ? "FAILED" : "PENDING",
          push_retry_at: terminal || attempts === 0
            ? null
            : new Date(Date.now() + PUSH_RETRY_DELAY_MS * 2 ** (attempts - 1)).toISOString(),
          push_claimed_at: null,
        },
        `&push_status=eq.PROCESSING&push_claimed_at=lt.${encodeURIComponent(staleClaimCutoff)}`,
        false,
        cleanupDeadline(),
      );
    }
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
        `notification_inbox?select=id,recipient_id,title,summary,content,target_type,target_id,push_device_cursor,push_attempts&push_status=eq.PENDING&${pushRetryDueFilter()}&order=id.asc&limit=${pageSize}`,
        {},
        deadline,
      );
      const ids = pending.map((row) => row.id);
      if (ids.length === 0) break;

      const claimed = await patchInboxRows(
        ids,
        { push_status: "PROCESSING", push_claimed_at: new Date().toISOString() },
        `&push_status=eq.PENDING&${pushRetryDueFilter()}`,
        true,
        cleanupDeadline(),
      );
      if (claimed.length === 0) return json({ delivered: deliveredTotal, duplicate: true });

      const heldClaimIds = new Set(claimed.map((row) => row.id));
      const attemptsSinceProgress = new Map<string, number>();
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
            {},
            deadline,
          );
          let rowFailed = false;
          let rowDeferred = false;
          let rowAttempts = row.push_attempts ?? 0;
          let attemptCounted = false;
          for (const device of devices) {
            if (processedDevices >= MAX_DEVICES_PER_INVOCATION) {
              rowDeferred = true;
              break;
            }
            ensureWithinDeadline(deadline);
            accessToken ??= await getFcmAccessToken(deadline);
            let result: FcmResult;
            try {
              result = await sendFcmWithRetry(accessToken, device.token, row, deadline, async () => {
                if (attemptCounted) return;
                rowAttempts++;
                await patchInboxRows(
                  [row.id],
                  { push_attempts: rowAttempts },
                  "&push_status=eq.PROCESSING",
                  false,
                  cleanupDeadline(),
                );
                attemptCounted = true;
                attemptsSinceProgress.set(row.id, rowAttempts);
              });
            } catch (error) {
              if (!(error instanceof DispatchDeadlineError) || !attemptCounted) throw error;
              rowFailed = true;
              deferred = true;
              break;
            }
            if (result === "SENT") {
              deliveredTotal++;
            } else if (result === "UNREGISTERED") {
              await supabaseRest(`notification_devices?token=eq.${encodeURIComponent(device.token)}`, { method: "DELETE" }, deadline);
            } else if (result === "PERMANENT_FAILURE") {
              console.warn("Skipping device after permanent FCM error", device.device_id);
            } else {
              rowFailed = true;
            }
            if (rowFailed) break;
            await patchInboxRows(
              [row.id],
              {
                push_device_cursor: device.device_id,
                push_claimed_at: new Date().toISOString(),
                push_attempts: 0,
              },
              "&push_status=eq.PROCESSING",
              false,
              cleanupDeadline(),
            );
            rowAttempts = 0;
            attemptCounted = false;
            attemptsSinceProgress.delete(row.id);
            processedDevices++;
          }

          if (rowFailed) {
            const terminal = rowAttempts >= MAX_PUSH_ATTEMPTS;
            await patchInboxRows(
              [row.id],
              {
                push_status: terminal ? "FAILED" : "PENDING",
                push_attempts: rowAttempts,
                push_retry_at: terminal
                  ? null
                  : new Date(Date.now() + PUSH_RETRY_DELAY_MS * 2 ** (rowAttempts - 1)).toISOString(),
                push_claimed_at: null,
              },
              "&push_status=eq.PROCESSING",
              false,
              cleanupDeadline(),
            );
            heldClaimIds.delete(row.id);
            attemptsSinceProgress.delete(row.id);
            if (!terminal) pendingTotal++;
            if (deferred) break;
          } else if (rowDeferred || devices.length === DEVICES_PER_ROW_BATCH) {
            await patchInboxRows(
              [row.id],
              { push_status: "PENDING", push_claimed_at: null, push_attempts: 0 },
              "&push_status=eq.PROCESSING",
              false,
              cleanupDeadline(),
            );
            heldClaimIds.delete(row.id);
            attemptsSinceProgress.delete(row.id);
            deferred = true;
          } else {
            await patchInboxRows(
              [row.id],
              { push_status: "SENT", push_claimed_at: null, push_device_cursor: null },
              "&push_status=eq.PROCESSING",
              false,
              cleanupDeadline(),
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
            false,
            cleanupDeadline(),
          );
          heldClaimIds.clear();
        }
      } catch (error) {
        const unattemptedIds: string[] = [];
        const attemptedGroups = new Map<number, string[]>();
        for (const id of heldClaimIds) {
          const attempts = attemptsSinceProgress.get(id) ?? 0;
          if (attempts === 0) unattemptedIds.push(id);
          else attemptedGroups.set(attempts, [...(attemptedGroups.get(attempts) ?? []), id]);
        }
        if (unattemptedIds.length > 0) {
          await patchInboxRows(
            unattemptedIds,
            { push_status: "PENDING", push_claimed_at: null },
            "&push_status=eq.PROCESSING",
            false,
            cleanupDeadline(),
          );
        }
        for (const [attempts, ids] of attemptedGroups) {
          const terminal = attempts >= MAX_PUSH_ATTEMPTS;
          await patchInboxRows(
            ids,
            {
              push_status: terminal ? "FAILED" : "PENDING",
              push_attempts: attempts,
              push_retry_at: terminal
                ? null
                : new Date(Date.now() + PUSH_RETRY_DELAY_MS * 2 ** (attempts - 1)).toISOString(),
              push_claimed_at: null,
            },
            "&push_status=eq.PROCESSING",
            false,
            cleanupDeadline(),
          );
        }
        heldClaimIds.clear();
        if (error instanceof DispatchDeadlineError) {
          deferred = true;
          break;
        }
        throw error;
      }

      if (pending.length < pageSize || deferred) break;
    }
    // Keep the webhook retryable while durable PENDING work remains. A 2xx here
    // would acknowledge the delivery even though some recipients were not sent.
    if (deferred || pendingTotal > 0) {
      await queueContinuation(payload.continuationDepth ?? 0);
      return json({ delivered: deliveredTotal, pending: pendingTotal, deferred }, 503);
    }
    if (deliveredTotal === 0 && pendingTotal === 0) return json({ delivered: 0, ignored: true });
    return json({ delivered: deliveredTotal, pending: pendingTotal });
  } catch (error) {
    console.error("notification dispatch failed", error);
    return json({ error: "Notification dispatch failed" }, 500);
  }
});

async function secretsEqual(left: string, right: string): Promise<boolean> {
  const [leftDigest, rightDigest] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(left)),
    crypto.subtle.digest("SHA-256", encoder.encode(right)),
  ]);
  const leftBytes = new Uint8Array(leftDigest);
  const rightBytes = new Uint8Array(rightDigest);
  let difference = 0;
  for (let index = 0; index < leftBytes.length; index++) difference |= leftBytes[index] ^ rightBytes[index];
  return difference === 0;
}

async function queueContinuation(depth: number, sourcePayload?: WebhookEventPayload): Promise<ContinuationResult> {
  if (depth >= MAX_CONTINUATION_DEPTH) {
    console.warn("notification dispatch continuation limit reached", depth);
    return "SKIPPED";
  }
  const baseUrl = Deno.env.get("SUPABASE_URL");
  const secret = Deno.env.get("WEBHOOK_SHARED_SECRET");
  if (!baseUrl || !secret) {
    console.warn("notification dispatch continuation is not configured");
    return "SKIPPED";
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), CONTINUATION_TIMEOUT_MS);
  try {
    const response = await fetch(`${baseUrl.replace(/\/$/, "")}/functions/v1/dispatch-notification`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-webhook-secret": secret,
      },
      body: JSON.stringify(
        sourcePayload
          ? { ...sourcePayload, continuationDepth: depth + 1 }
          : { type: "DISPATCH_PENDING", continuationDepth: depth + 1 },
      ),
      signal: controller.signal,
    });
    if (!response.ok) {
      console.warn("notification dispatch continuation failed", response.status);
      return "FAILED";
    }
    return "SENT";
  } catch (error) {
    if (controller.signal.aborted) return "TIMED_OUT";
    console.warn("notification dispatch continuation request failed", error);
    return "FAILED";
  } finally {
    clearTimeout(timeout);
  }
}

function toSourceNotification(payload: WebhookEventPayload) {
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
  if (payload.type === "UPDATE" && payload.old_record) {
    const previousPublishedAt = Date.parse(stringValue(payload.old_record.published_at));
    if (!Number.isNaN(previousPublishedAt) && previousPublishedAt <= Date.now()) return null;
  }
  const publishedAt = stringValue(row.published_at);
  if (publishedAt && Date.parse(publishedAt) > Date.now()) return null;
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

async function claimHousingBroadcast(authorId: string, sourceId: string, deadline: number): Promise<boolean> {
  return await supabaseRest<boolean>("rpc/claim_housing_notification_broadcast", {
    method: "POST",
    body: JSON.stringify({ p_author_id: authorId, p_source_id: sourceId }),
  }, deadline);
}

async function markFanoutComplete(dedupeKey: string, deadline: number): Promise<void> {
  await supabaseRest(`notification_dispatch_progress?dedupe_key=eq.${encodeURIComponent(dedupeKey)}`, {
    method: "PATCH",
    body: JSON.stringify({ fanout_complete: true, updated_at: new Date().toISOString() }),
  }, deadline);
}

async function recipientIdPage(
  recipientId: string | null,
  excludedUserId: string | null,
  afterRecipientId: string | null,
  deadline: number,
): Promise<string[]> {
  if (recipientId) {
    return !afterRecipientId || recipientId > afterRecipientId ? [recipientId] : [];
  }

  const queryParams = [
    "select=id",
    "student_id_verified=eq.true",
    "order=id.asc",
    `limit=${INBOX_INSERT_BATCH_SIZE}`,
  ];
  if (afterRecipientId) queryParams.push(`id=gt.${encodeURIComponent(afterRecipientId)}`);
  if (excludedUserId) queryParams.push(`id=neq.${encodeURIComponent(excludedUserId)}`);

  const page = await supabaseRest<Array<{ id: string }>>(
    `profiles?${queryParams.join("&")}`,
    {},
    deadline,
  );
  return page.map((profile) => profile.id);
}

async function sendFcm(accessToken: string, token: string, row: InboxRow, deadline: number): Promise<FcmResult> {
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
          android: { priority: "HIGH" },
        },
      }),
      signal: deadlineSignal(deadline),
    },
  );
  if (response.ok) return "SENT";
  const errorBody = await response.text();
  console.error("FCM send failed", response.status, errorBody.slice(0, 500));
  const fcmErrorCode = fcmSpecificErrorCode(errorBody);
  // Chỉ xóa token khi FCM xác nhận UNREGISTERED trong payload — tránh xóa nhầm khi 404 do cấu hình sai
  if (fcmErrorCode === "UNREGISTERED" || errorBody.includes("UNREGISTERED")) return "UNREGISTERED";
  if (fcmErrorCode === "INVALID_ARGUMENT" || fcmErrorCode === "SENDER_ID_MISMATCH") {
    return "PERMANENT_FAILURE";
  }
  return "FAILED";
}

export function fcmSpecificErrorCode(errorBody: string): string | null {
  try {
    const parsed = JSON.parse(errorBody) as {
      error?: {
        details?: Array<{ "@type"?: string; errorCode?: string }>;
      };
    };
    return parsed.error?.details?.find(
      (detail) => detail["@type"] === "type.googleapis.com/google.firebase.fcm.v1.FcmError",
    )?.errorCode ?? null;
  } catch {
    return null;
  }
}

async function sendFcmWithRetry(
  accessToken: string,
  token: string,
  row: InboxRow,
  deadline: number,
  onAttemptStart: () => Promise<void>,
): Promise<FcmResult> {
  for (let attempt = 0; attempt < FCM_MAX_ATTEMPTS; attempt++) {
    ensureWithinDeadline(deadline);
    await onAttemptStart();
    try {
      const result = await sendFcm(accessToken, token, row, deadline);
      if (result !== "FAILED" || attempt === FCM_MAX_ATTEMPTS - 1) return result;
    } catch (error) {
      if (Date.now() >= deadline) throw new DispatchDeadlineError("FCM request deadline reached");
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
  deadline?: number,
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
      deadline,
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

function pushRetryDueFilter(): string {
  return `or=(push_retry_at.is.null,push_retry_at.lte.${encodeURIComponent(new Date().toISOString())})`;
}

async function getFcmAccessToken(deadline: number): Promise<string> {
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
  let response: Response;
  try {
    response = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
      signal: deadlineSignal(deadline),
    });
  } catch (error) {
    if (Date.now() >= deadline) throw new DispatchDeadlineError("FCM OAuth deadline reached");
    throw error;
  }
  if (!response.ok) throw new Error(`FCM OAuth token request failed: ${response.status}`);
  const token = await response.json() as { access_token: string; expires_in: number };
  fcmAccessToken = { value: token.access_token, expiresAt: Date.now() + token.expires_in * 1000 };
  return token.access_token;
}

async function supabaseRest<T = unknown>(path: string, init: RequestInit = {}, deadline?: number): Promise<T> {
  const url = requiredEnv("SUPABASE_URL").replace(/\/$/, "");
  const key = supabaseSecretKey();
  const headers = new Headers({
    apikey: key,
    "Content-Type": "application/json",
  });
  // Khóa sb_ không phải JWT — chỉ gửi qua apikey; JWT legacy mới cần Authorization Bearer
  if (!key.startsWith("sb_")) headers.set("Authorization", `Bearer ${key}`);
  new Headers(init.headers).forEach((value, name) => headers.set(name, value));
  try {
    const response = await fetch(`${url}/rest/v1/${path}`, {
      ...init,
      headers,
      signal: deadline ? deadlineSignal(deadline) : init.signal,
    });
    if (!response.ok) throw new Error(`Supabase REST ${response.status}: ${(await response.text()).slice(0, 500)}`);
    const body = await response.text();
    return (body ? JSON.parse(body) : null) as T;
  } catch (error) {
    if (deadline && Date.now() >= deadline) throw new DispatchDeadlineError("Supabase REST deadline reached");
    throw error;
  }
}

function deadlineSignal(deadline: number): AbortSignal {
  const remainingMs = Math.max(1, deadline - Date.now());
  return AbortSignal.timeout(remainingMs);
}

function cleanupDeadline(): number {
  return Date.now() + CLEANUP_BUDGET_MS;
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
