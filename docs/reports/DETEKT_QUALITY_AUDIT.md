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
- All six batches passed `assembleDebug`, `detekt`, `spotlessCheck`, and `testDebugUnitTest` after the change. The latest unit test XML reports 436 tests, zero failures, errors or skips.
- No Detekt rule or suppression was added or loosened. The baseline has not been regenerated because cleanup remains incomplete.

After the first five batches, a separate Detekt 1.23.8 CLI scan without baseline over `app/src/main/java` and `app/src/test/java` reported 536 findings: 263 MaxLineLength, 101 MagicNumber, 81 UnusedPrivateMember, and 91 from other rules. This scan predates the schedule batch and does not include `androidTest`; it has a different scope and revision from the prior 566-finding Phase 1 scan and cannot be used as a before/after comparison.

## Outstanding work

- Audit and safely resolve the remaining findings on this `main`-based branch, including stale entries associated with the refactored code. Regenerate and review the baseline only after the remaining fixes are complete.
- Consolidate the existing `android-ci.yml`, `detekt.yml`, and overlapping validation in `android-release.yml` while retaining release signing and existing checks. The no-deletion policy prevents removing workflow files; decide a compliant way to preserve them and avoid duplicate automatic runs.
- Run final verification, create the requested PR and measure GitHub Actions duration. `clean` removes build outputs and is prohibited by the current no-deletion policy; use `assembleDebug` without `clean` until the user changes that policy.
- Configure branch protection for required checks on `main` and `develop` in GitHub settings.

## Local verification

```bash
./gradlew assembleDebug detekt spotlessCheck testDebugUnitTest
```

The Phase 1 reports and detailed findings are in the other `KAN-111_PHASE1_*` files in this directory. Full CI timing, Firebase configuration with real secrets, and the remaining baseline entries have not been validated in Phase 2.
