# Push notification delivery

The `dispatch-notification` Edge Function turns inserts into `public.notification_inbox`
and sends each pending inbox row to the recipient's registered FCM devices. It supports
Android FCM tokens now and stores a `WEB` platform value for later web clients.

## Deploy

1. Apply `supabase/migrations/20261108000000_push_notification_inbox.sql`.
2. Deploy the function with `supabase functions deploy dispatch-notification`.
3. Add these Edge Function secrets in Supabase project settings:
   - `WEBHOOK_SHARED_SECRET` (generate a long random value)
   - `FCM_SERVICE_ACCOUNT_JSON` (the Firebase service-account JSON; never commit it)
4. In Supabase Database Webhooks, create HTTP `POST` webhooks to
   `https://<project-ref>.supabase.co/functions/v1/dispatch-notification` with the
   header `x-webhook-secret: <same WEBHOOK_SHARED_SECRET>`:
   - `public.events`: `INSERT` and `UPDATE` (sends only when an event first becomes `PUBLISHED`)
   - `public.notifications`: `INSERT` and `UPDATE` (sends published academic/system notices; an update only notifies when it publishes a previously scheduled row)
   - `public.housing_posts`: `INSERT` (sends new listings with `status = OPEN`)

The dispatcher claims inbox rows in small batches, limits each invocation's runtime
and device fan-out, and writes each row's `SENT`/`PENDING` state before continuing.
Rows left pending after a time or device limit are safe to resume on the next
dispatch/retry invocation; interrupted `PROCESSING` claims are released after five
minutes.

The function rejects requests without the shared webhook header, filters unpublished
events and non-open housing posts, writes an idempotent per-user inbox item, and claims
pending rows before delivery so duplicate webhook invocations do not normally duplicate
pushes. Users must have a verified profile to receive broadcast messages; a targeted
school notification is sent only to its `recipient_id`.

## Android setup and verification

The Android package is configured by `app/google-services.json`. On Android 13 and newer,
the app asks for notification permission when the user opens Notification Center. Token
registration happens after session restore/login and token rotation; sign-out removes
the installation row from that user's active device list.

To verify end to end, deploy the migration and function, add the secrets and webhooks,
install the app on a Google Play services device, sign in and grant notification
permission, then insert a published event or an `OPEN` housing listing. Also insert an
academic row in `public.notifications`. Confirm the inbox row and push, then tap the
push with the app foregrounded, backgrounded, and its process removed. Android force-stop
is a separate OS state and must be relaunched before FCM delivery resumes.
