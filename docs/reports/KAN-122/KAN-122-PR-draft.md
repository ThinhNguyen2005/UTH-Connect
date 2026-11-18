## Summary

Add meal List/Form/Search views on `product.template` and prepare menu/action integration for KAN-122. **Draft for review, not ready to merge or deploy.**

## Implementation

- New meal views, search/filter/grouping, currency-aware prices, and explicit list/form action bindings.
- Two narrowly scoped patches: fix existing action/menu load order, then load the new views and connect the existing meal menu. Applied and checked on a separate KAN-120 snapshot.
- Static manifest/XML/local-ID/ACL-format checker plus 11 QA tests.
- Security matrix, test results, UAT/demo plans, Word report and actual browser screenshot of static QA results.
- Existing POS ACL/groups are preserved. No new ACL, group, record rule, Python business model or Android change.

## Dependency

Backend `uth_canteen_pos` exists on `origin/Kan120` at `b99ecdf63cf2d28c8947bad342d9c3bfafa08c7e`, but is absent from `develop` at `ed7cecbb`. This branch was created from develop and contains no KAN-120 commits or backend copy.

The new XML file is an extension of that backend, not an independently installable addon. Continue using an isolated integration snapshot for review. After the backend is reviewed and integrated into develop, convert the patches into manifest/menu diffs and retest. Check whether upstream has already fixed the load order before applying patches. Keep this PR targeted at develop; do not merge it independently while these dependencies remain unresolved.

## Testing

Pre-push validation rerun on 2026-11-18 (Asia/Bangkok):

| Check | Result | Evidence |
| --- | --- | --- |
| QA unit tests | PASS: 11 tests | [Pre-push log](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/evidence/prepush-validation-2026-11-18.txt) |
| Backend simulation | PASS: 5 tests | Same pre-push log; simulation is not Odoo runtime |
| Baseline local references | FAIL before fix: 2 forward references | Same log; retained as before-fix evidence |
| Patch check/apply and final static check | PASS | Same log |
| Python syntax / Word and PNG integrity | PASS | Same log |
| Odoo 20 fields and List/Search schema | PASS, checked 2026-11-17 | [Source/schema log](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/evidence/odoo20-source-check.txt) |
| External Odoo IDs, effective ACL/groups, install/upgrade, functional/security/regression | BLOCKED | No Odoo runtime/test DB; static checks do not prove these |
| UAT / demo | PENDING APPROVAL / NOT RUN | No acceptance or runtime demo claimed |

## Screenshots

Actual Chrome screenshot of a report populated from a real unittest run, **not an Odoo UI, terminal screenshot or runtime security proof**:

![Static QA: 11 tests passed; Odoo runtime blocked](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/evidence/20-static-qa-report.png)

[Capture provenance](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/evidence/screenshot-provenance.txt). Odoo UI/ACL/install/upgrade/UAT screenshots are still missing.

## Documentation

- [Word report with screenshot](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/Bao_cao_ngay_KAN-122_17-11-2026.docx)
- [Technical report](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-technical-report.md)
- [Security matrix](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-security-matrix.md)
- [Test results](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-test-results.md)
- [Acceptance checklist](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-acceptance-checklist.md)
- [UAT](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-UAT-checklist.md) / [Demo and screenshot plan](https://github.com/ThinhNguyen2005/UTH-Connect/blob/feat/kan-122-ui-security-qa/docs/reports/KAN-122/KAN-122-demo-script.md)

## Known Issues

- No verified Odoo runtime/test database; functional and security behavior are unverified.
- CRUD policy and company/location isolation require confirmation. Existing ACL is not certified secure merely because its CSV parses.
- `available_in_pos` filters the UI and may include other POS products; it is not a security record rule or canteen-only isolation.
- Backend SQL/report compatibility and manager write rights on the SQL report need backend/security review.
- Historical reports dated 2026-11-17 describe the pre-approval state. The user authorized commit/push/Draft PR on 2026-11-18; this PR does not imply UAT or merge approval.

## Acceptance Criteria

- [x] Meal view/action source and minimal integration patches prepared.
- [x] Static/QA and simulation checks with real logs.
- [x] Word report and accurately labeled static QA screenshot.
- [ ] Runtime views/menu/actions validated (AC01–03).
- [ ] Approved/effective ACL, groups and record rules (AC04–06).
- [ ] Odoo functional/security/integration/regression PASS (AC07–09).
- [ ] UAT, runtime demo/screenshots, reviewer approval and no critical issues (remaining DoD).

## Reviewer Notes

Review only KAN-122 source, patches and evidence. Confirm dependency integration, the CRUD/company policy, then run clean install/upgrade and non-admin tests before readiness. No backend or unrelated Android commits should appear. No automatic merge is authorized.
