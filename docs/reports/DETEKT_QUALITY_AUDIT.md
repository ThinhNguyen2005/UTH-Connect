# KAN-111: Quality audit and Phase 2 progress

## Scope and starting point

Phase 2 is being performed on `Kan111`, based on `main` at `9e56eca`. The earlier detailed Phase 1 scan was performed on `develop` at `a54f5b6`; its counts do not describe this exact tree. At the start of Phase 2, the `main` baseline contained 473 issue IDs. `testDebugUnitTest`, `detekt` with the checked-in baseline, and `spotlessCheck` passed. The existing Detekt configuration generates HTML and XML reports.

## Changes completed

- `fce2f55`: named book listing validation limits and split the complex condition into ordered checks, preserving the validation result. Added a test covering field boundaries and rejection before persistence.
- `539ceb7`: named sign-up student ID and password limits and extracted the student email conversion.
- `d34a3b1`: named password length limits in password change and password reset validation without changing their input handling.
- `86fec1a`: named separate student ID length limits for password recovery and verification.
- `2171991`: named channel and shift constants in QR luminance conversion.
- `3eae303`: named schedule period boundaries and added a unit test for the noon and evening transitions.
- `09016b2`: split sign-up validation and event registration guards into named conditions.
- `2138fca`: added `.github/workflows/ci.yml` with four independent jobs, cached Gradle, and uploaded build, analysis, formatting, and test artifacts. The two old quality workflows remain available for manual runs only because deleting files is prohibited.
- `4a5b415` and `c96dbeb`: named security, refresh, authentication, and status constants while retaining their values.
- `9f4f8c1`, `ac552c6`, and `ae6aafc`: wrapped long lines in authentication, book, and event screens.
- `bdf2968`: simplified the event editor guard and used the equivalent Kotlin error checks in the Firestore transaction and profile test fixture.
- `b0dcf79`: named Supabase transport timeouts, token lifetime values, and HTTP status values without changing their numeric values.
- All code batches passed `assembleDebug`, `detekt`, `spotlessCheck`, and `testDebugUnitTest` after the change. The latest unit test XML reports 436 tests, zero failures, errors or skips.
- `UnusedPrivateMember.ignoreAnnotated: ['Preview']` is a targeted Detekt configuration change: all 81 reported private members carried `@Preview` and are used by Android Studio's Compose Preview. This follows the [Detekt 1.23.8 Compose guidance](https://detekt.dev/docs/1.23.8/introduction/compose/). Other private members remain checked. No `@Suppress` was added. The baseline has not been regenerated because cleanup remains incomplete.

After the first five batches, a separate Detekt 1.23.8 CLI scan without baseline over `app/src/main/java` and `app/src/test/java` reported 536 findings: 263 MaxLineLength, 101 MagicNumber, 81 UnusedPrivateMember, and 91 from other rules. This scan predates the schedule batch and does not include `androidTest`; it has a different scope and revision from the prior 566-finding Phase 1 scan and cannot be used as a before/after comparison.

On the current branch, the same scope and CLI reported 532 findings before the Preview configuration and 451 afterward: 263 MaxLineLength, 99 MagicNumber, zero UnusedPrivateMember, and 89 other findings. The 81 removed findings are IDE-only preview entry points, not dead code.

At `ae6aafc`, the same CLI scope reported 339 findings without baseline: 206 MaxLineLength, 49 MagicNumber, 21 TooGenericExceptionCaught, 19 TooManyFunctions, 11 ReturnCount, 11 CyclomaticComplexMethod, 8 LongMethod, 8 MatchingDeclarationName, 3 SwallowedException, 2 UnusedPrivateProperty, and 1 LongParameterList. The scan excludes `androidTest`; its finding count must not be directly compared with the 566-finding Phase 1 scan on `develop`.

## Outstanding work

- Audit and safely resolve the remaining findings on this `main`-based branch, including stale entries associated with the refactored code. Regenerate and review the baseline only after the remaining fixes are complete.
- Review the new CI workflow on GitHub. The old quality workflows are manually triggered; the release workflow still runs its existing checks on `main`, so that path has some duplicate Gradle validation. Release signing was not changed.
- Run final verification, create the requested PR and measure GitHub Actions duration. `clean` removes build outputs and is prohibited by the current no-deletion policy; use `assembleDebug` without `clean` until the user changes that policy.
- Configure branch protection for required checks on `main` and `develop` in GitHub settings.
- Compiler warnings remain for deprecated `Icons.Filled.ArrowBack` and `Locale` constructor usage in `BooksScreen.kt`; they were observed during the build and require separate review.

## Local verification

```bash
./gradlew assembleDebug detekt spotlessCheck testDebugUnitTest
```

The Phase 1 reports and detailed findings are in the other `KAN-111_PHASE1_*` files in this directory. Full CI timing, Firebase configuration with real secrets, and the remaining baseline entries have not been validated in Phase 2.
