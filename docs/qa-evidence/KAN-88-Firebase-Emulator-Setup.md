# KAN-88 Book Market — Backend Architecture and Verification Guide

## 1. Backend Migration Overview

In accordance with UTH Connect's architectural standard, the Book Market (Chợ sách cũ) feature has been fully migrated from Firebase to **Supabase** (PostgreSQL, Row Level Security, Supabase Auth, and Supabase Storage):

- **Identity**: Supabase GoTrue Auth is the single identity provider. `owner_user_id` references `public.profiles(id)` via a foreign key constraint.
- **Data Layer**: Listings are persisted in `public.book_listings` via PostgREST through `SupabaseRestClient.kt` and `SupabaseBookListingRepository.kt`.
- **Media**: Cover photos are stored in the Supabase Storage bucket `book_listings` using path `${userId}/${listingId}/${uuid}.jpg`. Photos are validated by magic bytes (JPEG/PNG/WebP, ≤ 5 MB).
- **Access Control (RLS)**: Row Level Security restricts INSERT, UPDATE, and DELETE operations to the verified listing owner (`owner_user_id = auth.uid()`).
- **Atomic Status Updates**: Status toggles (`AVAILABLE` ↔ `SOLD`) are executed via PATCH requests exclusively affecting the `status` and `updated_at` columns, avoiding lost updates and preventing overwrite of book details or photos.

## 2. Test Classification

To ensure test evidence integrity, tests in this repository are categorized as follows:

| Test Type | Scope & Environment | Backend Used |
| :--- | :--- | :--- |
| **Unit Tests** | Domain models, use cases, mappers, input validation, ViewModel state machines, dialer fallbacks. Fast, hermetic, run via `./gradlew testDebugUnitTest`. | Mock / Fake repositories / Memory |
| **Compose UI Tests** | Component rendering, state restoration across configuration changes/process recreation, contact picker fallbacks. | Compose Test Rule / Test Dispatcher |
| **Firebase Emulator Tests (Legacy)** | The 51 unit tests and 3 connected tests in prior QA reports verified the initial Firebase prototype against `firebase-tools` emulators. They are superseded by Supabase tests. | Local Firebase Emulator (9099, 8080, 9199) |
| **Supabase Integration Tests** | Catalog contract verification (`supabase/tests/security_contract.sql`), REST and Storage policies verification on disposable staging projects. | Supabase Staging Project (`useSupabase=true`) |

## 3. Staging and Local Verification

### Running Android Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

### Building with Supabase Configuration
UTH Connect enforces Supabase builds (`useSupabase=true` and `useMockData=false`). In `app/build.gradle.kts`, builds without Supabase configuration fail fast via Gradle property validation. Builds require valid HTTPS Supabase URL and a publishable key:
```powershell
.\gradlew.bat -PuseSupabase=true -PuseMockData=false -PsupabaseUrl="https://<project-ref>.supabase.co" -PsupabasePublishableKey="sb_publishable_..." assembleDebug
```
> [!IMPORTANT]
> Never commit `service_role` or secret keys. Only `sb_publishable_...` keys belong in client configurations.

### Database Migration
The Book Market migration script is located at:
- `supabase/migrations/20261110000000_book_market.sql`

Apply in sequential order to your disposable staging project via the Supabase CLI or SQL Editor.
