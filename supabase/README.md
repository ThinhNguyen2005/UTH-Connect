# UTH Connect — Supabase staging backend

Supabase is the backend selected by the current Android Gradle configuration. The
Android build requires `useSupabase=true`, `useMockData=false`, a project URL and
a publishable key. Firebase repositories remain in the source tree but are not a
working alternate build mode. This folder does not migrate existing data.

## Apply migrations on a disposable staging project

1. Create a separate Supabase project. Record its **project URL** and
   `sb_publishable_...` key (never put an `sb_secret_...` or `service_role` key
   in Android). Do not use a production project for initial verification.
2. Check `supabase_migrations.schema_migrations` and the existing tables to
   determine which migrations have already run. Keep the original
   `20261103000000_init_uth_connect.sql` unchanged wherever it has run.
3. Apply the files in **lexicographic order**, each in its own committed
   transaction (Supabase CLI migrations or separate SQL Editor executions):

   - `migrations/20261103000000_init_uth_connect.sql` (only on an empty project)
   - `migrations/20261103005000_add_draft_status.sql`
   - `migrations/20261103007500_legacy_capacity_preflight.sql`
   - `migrations/20261103010000_secure_core_workflows.sql`
   - `migrations/20261103020000_event_management.sql`
   - `migrations/20261103030000_enforce_verified_checkin.sql`
   - `migrations/20261103040000_remove_legacy_capacity_check.sql`
   - `migrations/20261103050000_identity_uniqueness.sql`
   - `migrations/20261103060000_preserve_attendance_history.sql`
   - `migrations/20261104000000_stable_ticket_qr.sql`

   The `05000` migration adds an enum value that must be committed before it is
   used. The `07500` migration removes the original capacity CHECK **before**
   `10000` recalculates registration counts: the original CHECK treats zero as
   zero seats, whereas the Android model uses zero to mean unlimited. The
   following migrations restrict previously broad privileges, enforce event
   management through checked RPCs, and preserve registrations, user IDs,
   schedules and grades. The `30000` migration prevents unverified students
   from issuing/using QR tokens. The `40000` and `50000` migrations defend
   against remaining legacy capacity checks; `50000` also enforces normalized
   uniqueness of verified student numbers. The `60000` migration prevents
   deleting an event or profile while an attendance row still refers to it.
   Data retention and privacy erasure still require a reviewed administrative
   policy and backup procedure. The `040000` migration adds a stable signed
   ticket QR; the scanner still enforces the event check-in window and rejects
   duplicate attendance.

   **Existing projects:** Check the recorded migration history and SQL schema
   before running anything. Never modify or re-run an already-applied migration
   blindly. If `10000` is already applied, check whether the legacy capacity
   CHECK remains before applying the still-pending forward fixes. If `10000`
   failed while recalculating counts, run `07500` first in a controlled
   transaction and reconcile the migration history with your deployment tool
   before retrying `10000`. Do not automatically push out-of-order migrations
   to a project whose history is unknown.
4. Run `tests/security_contract.sql` in the staging SQL Editor. **It checks
   the catalog only**, not real-user RPC behavior or race conditions.
5. Inspect any pre-existing over-capacity events before changing them. The
   updated capacity constraint is added `NOT VALID` so legacy records are
   retained, but those inconsistent rows still need a deliberate repair before
   final validation (`ALTER TABLE public.events VALIDATE CONSTRAINT
   events_capacity_consistent`).
   Migration `30000` likewise adds `profiles_verified_student_id_present`
   as `NOT VALID`. Investigate any legacy profiles marked verified without a
   valid student number, correct them through trusted administration and then
   run `ALTER TABLE public.profiles VALIDATE CONSTRAINT
   profiles_verified_student_id_present`.
   Before `50000`, audit collisions among verified student IDs after trimming
   spaces and normalizing case. It will deliberately stop on duplicates:

   ```sql
   select upper(btrim(student_id)) as normalized_student_id,
          count(*) as owners, array_agg(id) as profile_ids
   from public.profiles
   where student_id_verified
   group by upper(btrim(student_id))
   having count(*) > 1;
   ```

   Resolve any conflict through the trusted university identity process; do
   not automatically merge profiles or reassign student records.

## Bootstrap and identity verification

Supabase Auth handles passwords and issues access tokens. Creating an Auth
account automatically creates a profile with `role='student'` and
`student_id_verified=false`. Signup metadata is **not** evidence of student
identity. A trusted school/admin process must verify the student and assign
their number; students cannot change the role, student number or verification
flag through the API. A verified student number can never be reassigned by
`admin_verify_student`.

For the **first admin only**, from the staging SQL Editor as the database
owner, after creating that Auth account and independently verifying its owner:

```sql
-- REPLACE with the UUID from Authentication > Users for your trusted admin.
update public.profiles
set role = 'admin'
where id = '00000000-0000-0000-0000-000000000000';
```

Subsequent changes use authenticated RPCs:

```json
{"p_user_id":"<auth.users UUID>","p_student_id":"<verified MSSV>"}
```

Call `/rest/v1/rpc/admin_verify_student` with the JSON above and the admin's
access token. For organizer approval call `/rest/v1/rpc/admin_set_user_role`
with `{"p_user_id":"<UUID>","p_role":"organizer"}`. These are privileged
administrative actions. Admin/operator accounts also need a verified profile
to use the current Android login UI.

**Auth email:** The Android login currently accepts a real email address, or
an MSSV that it interprets as `<MSSV>@ut.edu.vn`. Provision accounts with this
mapping if MSSV login is desired. Email verification / identity verification
must be handled by an authorized school process; the backend does not claim
that owning an `@ut.edu.vn` email proves enrollment.

## Build Android against Supabase

Using PowerShell in the repository root (use real staging URL/key):

```powershell
.\gradlew.bat :app:assembleDebug -PuseMockData=false -PuseSupabase=true '-PsupabaseUrl=https://YOUR-PROJECT.supabase.co' '-PsupabasePublishableKey=sb_publishable_YOUR_KEY'
```

Android uses the publishable key, a user JWT and server-side RLS/RPC checks.
The Events tab is visible only for Supabase builds. The student can sign in,
list events, register/cancel, open a stable server-signed QR in Tickets; an
organizer/admin can capture and validate a QR with the camera. Login state is
held **in memory** and requires signing in again after process death; remember
me / biometric restoration is intentionally disabled for Supabase until secure
refresh-token storage is implemented.

## Required staging acceptance tests (real users / two sessions)

Create at least two verified student Auth users, two organizer users assigned
different events, and one admin. Never use real student records for this test.

After applying the migrations and running `tests/security_contract.sql`, use
`tests/staging_acceptance.py` to exercise **real** PostgREST/RPC permissions,
concurrent registrations, cancellation counts, signed QR, ownership, replay
and expiry. Requires Python 3.10+ (standard library only). In PowerShell,
set the following **process-only** environment variables to credentials for
five distinct staging test accounts (two verified students, two organizers and
one admin, provisioned by a trusted staging administrator):

```powershell
$env:SUPABASE_URL = "https://YOUR-STAGING-PROJECT.supabase.co"
$env:SUPABASE_PUBLISHABLE_KEY = "sb_publishable_STAGING_KEY"
$env:SUPABASE_STAGING_ACCEPTANCE = "I_UNDERSTAND_STAGING_ONLY"
$env:TEST_STUDENT_A_EMAIL = "student-a@example.edu"
$env:TEST_STUDENT_A_PASSWORD = "<staging-only password>"
$env:TEST_STUDENT_B_EMAIL = "student-b@example.edu"
$env:TEST_STUDENT_B_PASSWORD = "<staging-only password>"
$env:TEST_ORGANIZER_A_EMAIL = "organizer-a@example.edu"
$env:TEST_ORGANIZER_A_PASSWORD = "<staging-only password>"
$env:TEST_ORGANIZER_B_EMAIL = "organizer-b@example.edu"
$env:TEST_ORGANIZER_B_PASSWORD = "<staging-only password>"
$env:TEST_ADMIN_EMAIL = "admin@example.edu"
$env:TEST_ADMIN_PASSWORD = "<staging-only password>"
python supabase/tests/staging_acceptance.py
```

Do not commit these credentials or point the script at production. The script
creates three specially named test events, uses real registration/attendance
rows, and verifies tampered/invalid signed QR rejection. It also creates temporary
schedule, grade and recipient-only notification fixtures through the admin
account, checks owner vs. stranger visibility and deletes those fixture rows.
It cancels all test events when finished (the event audit rows are deliberately
retained). If cleanup fails, it prints the exact row or event IDs for manual
cleanup. The organizer accounts must already have the `organizer` role, the
admin must have the `admin` role, and student accounts must have their MSSV
independently verified before running the script. Passing this script
does **not** replace camera testing on real devices or validating legacy rows.

| Test | Expected result |
| --- | --- |
| Student updates `profiles.role`, `student_id`, `student_id_verified` via REST | Denied (column privilege). |
| Admin submits a NULL, blank or too-short MSSV to `admin_verify_student` | `INVALID_STUDENT_ID`; profile stays unchanged. |
| Student selects another student's profile, registration, attendance, grades or schedule by ID | No rows; only their own / allowed public fields. |
| Student inserts registration or attendance directly | Denied. Use approved RPC. |
| Student calls `create_event`, `edit_event` or `set_event_status` | Denied (`FORBIDDEN`). |
| Organizer A creates a draft and attempts to edit Organizer B's event | Draft created, cross-owner edit denied. |
| Organizer publishes draft, starts and completes event | Valid state transitions succeed; invalid transitions are denied. |
| Student updates event title, status, count or organizer ID directly | Denied. Organizers must use management RPCs. |
| Two students call `register_for_event` simultaneously for a 1-seat event | Exactly one succeeds; count = 1; other returns `EVENT_FULL`. |
| Student cancels then repeats cancellation, or registers twice | Count decrements/increments once per state change; never negative. |
| Organizer B checks in ticket issued for Organizer A's event | `FORBIDDEN`. |
| Check in a valid registered QR with its own organizer | One attendance record; second scan returns `ALREADY_CHECKED_IN`. |
| Old registration of an unverified student attempts QR issue or check-in | `STUDENT_NOT_VERIFIED` and no attendance. |
| Change a signed QR character | `INVALID_QR`; no attendance. |
| Attempt check-in outside event window or after cancellation | `CHECKIN_NOT_OPEN` / `NOT_REGISTERED`. |
| App signs in → Events → Register → Tickets QR → organizer camera | State and attendance persist after refreshing from the server. |
| Android build with configured Supabase URL/publishable key | Compile against the selected backend configuration. |

Use separate clients authenticated as different users, with the **same** event
and with a realistic start/deadline. For the concurrency test, send both RPC
requests at once and then query the event and registrations from the staging
admin session. All sign-in, registration and QR verification uses server time.

**Current validation boundary:** Until migration execution, staging RLS/RPC
tests, two-client concurrency, physical camera testing and end-to-end Auth
testing are completed against a live project, Supabase must **not** replace
Firebase in production. This repository provides the implementation and
repeatable verification steps, but it is not proof of live deployment.
