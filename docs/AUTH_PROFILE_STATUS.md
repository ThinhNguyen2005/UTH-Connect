# Auth/Profile — phạm vi và nghiệm thu

Ngày rà soát: 03/11/2026. Nhánh `feature/auth-phu`. Không đổi toolchain, commit hoặc push.
HEAD gốc giữ nguyên: `bf38503e17405025e83b25fe518b97fe9f5daf94`.

## Phạm vi gốc và hiện trạng trước sửa

Nguồn quyết định: `Design.md:38–66,200–205,208–255`; `../README.md:669–687`;
`REPO_TREE.md` mục 3; `GIT_WORKFLOW.md:89`; `CODING_STANDARDS.md` mục 3,6,9,12.
Số dòng bảng sau tham chiếu HEAD trước thay đổi trong phiên này.

| Yêu cầu | Phân loại ban đầu | Bằng chứng |
|---|---|---|
| Đăng nhập, validation, hiện/ẩn mật khẩu, loading, retry | Hoàn thành nhưng chỉ demo | `AuthScreen.kt:47–141`, `AuthViewModel.kt:86–118`, `LoginValidator.kt:4–23` |
| Firebase Auth thật | Chưa thực hiện | `AuthDataModule.kt:16–18` bind demo; `app/build.gradle.kts:46–74` không có Firebase |
| Quên/đặt lại mật khẩu | Chưa thực hiện | `AuthScreen.kt:145–160`, `strings.xml:69`: dialog thông báo demo |
| Đổi mật khẩu | Hoàn thành một phần | `DemoAuthRepository.kt:100–115`: PBKDF2 nhưng chỉ tại máy |
| Hồ sơ/liên hệ | Hoàn thành một phần | `DemoAuthRepository.kt:29–64,118–129`: dữ liệu cố định/preferences |
| Role/xác minh UTH | Hoàn thành một phần | `UserRole.kt:4`, `DemoAuthRepository.kt:36–47`: mô phỏng |
| Session | Hoàn thành nhưng còn lỗi | `PreferencesSessionRepository.kt:31–55`: chỉ tin MSSV, không retry sau lỗi |
| I/O/cancellation | Hoàn thành nhưng còn lỗi | `DemoAuthRepository.kt:86,96,103,120–127`, session gọi `commit()` trên Main |
| Biometric | Hoàn thành nhưng còn lỗi | `AuthRoute.kt:80–96`, `ProfileRoute.kt:74–90`: chưa quản lý CancellationSignal |
| Form Profile/Back | Hoàn thành nhưng còn lỗi | `ProfileViewModel.kt:58–76,94,122,144`, `ProfileRoute.kt:44–47` |
| Test | Hoàn thành một phần | 37 unit test, 7 Android test; thiếu repository/session tests |
| Admin Dashboard, QR, module khác, triển khai backend | OUT OF SCOPE | Bảng phân công `REPO_TREE.md` mục 3,5 |

Auth/Profile gồm đăng nhập, xác minh UTH, hồ sơ, liên hệ, mật khẩu, phiên, đọc và hiển thị claims.
Không có hợp đồng ánh xạ MSSV → Firebase email trong tài liệu; không tự tạo quy tắc ghép email/API.
Cấp tài khoản/duyệt quyền qua Admin Dashboard và triển khai rules do owner/backend thực hiện.

## Thay đổi

- `FirebaseAuthSource.kt`: adapter SDK và ranh giới kiểm thử độc lập.
- `FirebaseAuthRepository.kt`: đăng nhập email, reload user, token mới, đọc hồ sơ server,
  reauthenticate trước đổi mật khẩu, reset email, chỉ ghi ba trường liên hệ.
- `SessionPreferences.kt`: tùy chọn theo UID, I/O ngoài Main; không lưu token/mật khẩu riêng.
- `PreferencesSessionRepository.kt`: chống restore đồng thời, retry, xác thực lại trước mở khóa,
  đăng xuất Firebase và thu hồi quyền UI ngay cả khi ghi đĩa lỗi.
- `ResetPasswordUseCase.kt`; hai Route quản lý một prompt, hủy khi stop/dispose, bỏ callback cũ.
- Form xóa lỗi/notice cũ, chặn Back khi lưu; password keyboard; chuỗi VI/EN.
- Demo repository giữ lại nhưng không bind. Dữ liệu/grants demo cũ không thành phiên Firebase.
  Preview và fake trong test source set không phải xác thực production.

## Cấu hình bên ngoài bắt buộc

1. Cung cấp `app/google-services.json` đúng applicationId `com.giathinh.uthconnect`.
   File được ignore; không tạo file giả. Google Services chỉ apply khi file tồn tại.
   Debug thiếu cấu hình vẫn build nhưng yêu cầu xác thực báo lỗi, không cấp phiên demo.
2. Bật Email/Password, cấu hình thư verify/reset và bảo vệ chống dò tài khoản trong Console.
   Firebase xử lý đặt lại mật khẩu qua liên kết email; gửi thư không đồng nghĩa đã đổi mật khẩu.
3. Backend/Admin SDK cấp tài khoản, xác minh UTH, hồ sơ `users/{uid}` và claims.
   Client không tự tạo dữ liệu học tập hoặc cấp quyền. Đăng ký tự do chưa có tiêu chí trong task.
4. Claims: `uthVerified: true`, `role: student|organizer|admin` (cũng đọc enum chữ hoa).
   Role lạ/thiếu → STUDENT; thiếu verified claim → không cấp phiên; document role/verified bị bỏ qua.
5. Hồ sơ: `studentId`, `displayName` bắt buộc; `className`, `major`, `faculty`, `avatarUrl` theo dữ liệu
   được cấp. Liên hệ: `phoneNumber`, `personalEmail`, `currentAddress`. Email lấy từ Auth.
6. Owner cần ghép rules bên dưới vào rules hiện có và kiểm thử Emulator trước deploy.
   Không thay thế rules của các collection/module khác.
7. MSSV-only vẫn PARTIAL: cần hợp đồng ánh xạ tin cậy, quyền riêng tư, chống dò và rate limit.
   Hiện app báo hướng dẫn dùng email trường, không suy đoán email và không giả thành công.
8. Validator giữ tên miền `ut.edu.vn` có sẵn. Design.md mục 5.3 vẫn yêu cầu nhà trường xác nhận;
   backend phải kiểm tra tên miền/quy trình duyệt, không chỉ tin regex client.

## Thiết kế rules cho Auth/Profile — chưa triển khai

Ghép vào service Firestore hiện có; từ chối mặc định các thao tác không được cấp rõ ràng.

```text
match /users/{uid} {
  function owner() {
    return request.auth != null && request.auth.uid == uid
      && request.auth.token.email_verified == true
      && request.auth.token.uthVerified == true;
  }
  allow get: if owner();
  allow list, create, delete: if false;
  allow update: if owner()
    && request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(['phoneNumber', 'personalEmail', 'currentAddress'])
    && request.resource.data.phoneNumber is string
    && request.resource.data.phoneNumber.size() <= 12
    && (request.resource.data.phoneNumber == ''
      || request.resource.data.phoneNumber.matches('(0[0-9]{9}|[+]84[0-9]{9})'))
    && request.resource.data.personalEmail is string
    && request.resource.data.personalEmail.size() <= 254
    && (request.resource.data.personalEmail == ''
      || request.resource.data.personalEmail.matches('[^ @]+@[^ @]+[.][^ @]+'))
    && request.resource.data.currentAddress is string
    && request.resource.data.currentAddress.size() <= 200;
}
```

Rules test bắt buộc: anonymous bị từ chối; A không đọc/ghi B; không list users; thiếu claims/email verify
bị từ chối; chủ hồ sơ sửa liên hệ hợp lệ được phép; không sửa studentId/displayName/faculty/role/uthVerified;
không create/delete từ client; từ chối kiểu sai và dữ liệu quá dài. Backend/rules của module khác phải
kiểm tra quyền organizer/admin; badge không phải cơ chế phân quyền.

## Giới hạn và bằng chứng kiểm thử

- Restore/refresh bắt buộc có mạng, không cấp phiên từ cache chưa kiểm tra server.
- AuthStateListener thu hồi UI khi SDK sign-out; foreground refresh kiểm tra user/token/profile.
  Không hứa phát hiện tức thì thao tác khóa tài khoản từ Console trong lúc app vẫn foreground.
  Backend/rules phải kiểm tra từng yêu cầu nhạy cảm.
- Biometric là cổng mở phiên Firebase hiện có, không thay thế server, không giảm BIOMETRIC_STRONG.
- Chưa xác nhận kết nối project thật, thư reset, claims/rules và đồng bộ nhiều thiết bị vì thiếu cấu hình.
- Không có Node/npm/Firebase CLI trong PATH khi kiểm tra; chưa chạy Security Rules Emulator.
- Kết quả Gradle cuối cùng được cập nhật sau khi chạy; không dùng kết quả cũ trong README làm bằng chứng.
  UI integration dùng backend test, được phân biệt với Firebase online.

## Checklist đối chiếu yêu cầu người dùng

PASS ở đây nghĩa mã client và bằng chứng kiểm thử tương ứng đạt; không thay thế nghiệm thu backend thật.
PARTIAL chỉ rõ phần đã có và phụ thuộc chưa giải quyết. Không có test thất bại nào được bỏ qua.

| Mục gốc | Trạng thái | Bằng chứng/kết luận |
|---|---|---|
| I.1–6: tài liệu, phạm vi, đối chiếu, bằng chứng, bảo toàn Git | PASS | Bảng ban đầu, HEAD gốc, REPO_TREE và git status; không đụng `.idea`/`integration_backups` có sẵn |
| II.1: MSSV/email + mật khẩu | PARTIAL | FirebaseAuthRepository:26; email dùng SDK thật, MSSV thiếu backend ánh xạ |
| II.2–6: validation, lỗi, hiện/ẩn, trạng thái, ghi nhớ | PASS | AuthScreen/AuthViewModel; SignInUseCase; SessionRepository tests |
| II.7: đăng xuất an toàn | PASS | PreferencesSessionRepository:128; test cả ghi đĩa thất bại; UI gate về form |
| II.8: quên/đặt lại mật khẩu | PARTIAL | ResetPasswordUseCase và dialog đã nối SDK; thiếu Console để xác nhận thư/liên kết thật |
| II.9: đổi mật khẩu | PARTIAL | Reauthenticate + updatePassword; test chính sách đạt, chưa thử tài khoản thật |
| II.10: sai tài khoản/mật khẩu, mất mạng/ngoại lệ | PASS | Lỗi SDK riêng kiểm thử trên Android; không tiết lộ user tồn tại qua reset |
| III.1–3: BoM/plugin/FirebaseAuthRepository | PASS | BoM 34.7.0 tương thích compiler; plugin 4.5.0 apply khi có config; Hilt bind Firebase |
| III.4–6: login/logout/auth state/startup | PARTIAL | Adapter và session đã nối; thiếu google-services.json để nghiệm thu project thật |
| III.7: khóa/xóa/phiên hết hiệu lực | PARTIAL | Reload/token mới khi restore/foreground; lỗi terminal thu hồi; không phát hiện tức thì mọi thao tác Console |
| III.8–10: roles, claims tin cậy, rules/backend | PARTIAL | Chỉ đọc role/uthVerified từ token; thiết kế users rules; chưa triển khai/test Emulator backend |
| III.11: không log mật khẩu/token | PASS | Không thêm logging dữ liệu xác thực |
| IV.1–10: session/retry/logout/cancellation/I/O/concurrency | PASS | PreferencesSessionRepository/SessionPreferences; test retry, hủy, UID, đồng thời, auth events; SignInUseCase thu hồi khi start lỗi/hủy |
| V.1–5: hồ sơ, dữ liệu học tập/liên hệ/cập nhật/repository | PARTIAL | Firestore users/{uid}, chỉ sửa liên hệ; cần cấp hồ sơ/rules thật |
| V.6–11: validation, lỗi/notice cũ, trạng thái, Back, Logout | PASS | ProfileViewModel:49,96,126,150,170; ProfileRoute:56; unit/UI flow tests |
| V.12: giữ dữ liệu khi khởi động lại | PARTIAL | Lưu trên Firestore, đọc từ server; test repository mới đọc contact từ source; chưa chứng minh nhiều thiết bị/project thật |
| VI.1–8: hỗ trợ/quyền/prompt/vòng đời/trùng/hủy/đổi cấu hình/thất bại | PASS | USE_BIOMETRIC, BIOMETRIC_STRONG, ON_STOP/dispose hủy, callback kiểm tra signal; không tự mở khóa khi lỗi |
| VI.9: kiểm thử sinh trắc học quan trọng | PARTIAL | Unit test trạng thái cold start, sai UID, thất bại/unsolicited success; chưa nghiệm thu vân tay/khuôn mặt thật và rotation của prompt trên thiết bị đã enroll |
| VII.1–5: MVVM/Clean/Hilt/Flow/tách nghiệp vụ | PASS | Kiểm tra import không thấy Firebase trong domain/presentation; DI module riêng |
| VII.6: Auth → Main/Profile → Logout | PASS | AuthProfileFlowUiTest qua AuthRoute/ProfileRoute; không thay graph/tab gốc |
| VII.7: không ảnh hưởng tab khác | PASS | Không có diff Home/Schedule/Utilities/navigation; Utilities tests vẫn đạt |
| VII.8: VI/EN | PASS | 91 key auth_ mỗi locale, không thiếu; kiểm thử màn Auth/Profile VI/EN font 200% |
| VII.9: đồng bộ tài liệu | PASS | README, Design, REPO_TREE, README hai feature và tài liệu này |
| VIII.1–10: các lỗi đã xác minh | PASS | Thay demo binding; claims server; I/O; cancellation; retry; vòng đời prompt; validation/notice/Back/password keyboard |
| IX: unit/UI/build | PASS cho môi trường kiểm thử client | Kết quả Gradle/XML bên dưới; online Firebase và Rules Emulator vẫn PARTIAL như trên |
| X.1–12: quy tắc bắt buộc | PASS | Không commit/push/checkout/reset/clean; không đổi AGP/Kotlin/Gradle/SDK; không credentials giả hoặc mock production |
| XI: báo cáo | PASS | Checklist, file inventory, kết quả, dependency bên ngoài và rủi ro trong tài liệu này |
| Admin Dashboard, cấp quyền Admin SDK, triển khai server/module khác | OUT OF SCOPE | Không tạo hoặc triển khai backend ngoài phân công |

## Lỗi đã sửa và rủi ro còn lại

Các lỗi ưu tiên cao: bỏ cấp phiên bằng MSSV lưu máy và bỏ quyền demo khỏi production binding.
Các lỗi mức trung bình: I/O Main, nuốt cancellation, retry restore, cleanup sau session start lỗi/hủy,
prompt trùng/callback muộn, session khác UID, Back/double submit khi lưu.
Các lỗi giao diện: validation/notice cũ, password keyboard/visibility, Snackbar che nút Logout
(được phát hiện bởi Android UI flow test, sau đó sửa và chạy lại đạt).

Rủi ro còn lại cần xử lý trước phát hành: thiếu cấu hình Firebase và rules đã triển khai/test,
chưa có ánh xạ MSSV, chưa xác nhận tên miền với trường, chưa kiểm chứng thư/claims/hồ sơ thật.
Native biometric và khả năng đọc bố cục bằng TalkBack vẫn cần kiểm thử thiết bị;
font/locale UI test chỉ xác nhận nội dung và hành động còn truy cập được, không thay kiểm tra thị giác toàn diện.
BoM 35.1.0 đã thử nhưng metadata Kotlin 2.4 không phù hợp compiler 2.2, nên dependency mới
được cố định ở 34.7.0; không nâng Kotlin để vượt lỗi. Không đổi phiên bản toolchain gốc.

Tham khảo: [Firebase Android](https://firebase.google.com/docs/android/setup),
[quản lý tài khoản](https://firebase.google.com/docs/auth/android/manage-users),
[custom claims](https://firebase.google.com/docs/auth/admin/custom-claims),
[rules theo trường](https://firebase.google.com/docs/firestore/security/rules-fields).


## Kết quả trước rebase (lịch sử)

Lệnh đã chạy (không clean/reset hoặc đổi toolchain):

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

- Lượt cuối: **BUILD SUCCESSFUL**, 45 giây; unit task UP-TO-DATE sau lượt trước đã thực thi đạt.
- Unit XML hiện tại: **75 tests**, 0 failures, 0 errors, 0 skipped.
- Android XML hiện tại: **18 tests**, 0 failures, 0 errors, 0 skipped.
- Thiết bị: `emulator-5554`, `Medium_Phone_API_37.0(AVD) - 17`.
- Báo cáo unit: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Báo cáo Android: `app/build/reports/androidTests/connected/debug/index.html`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- 91 key auth_ ở mỗi locale VI/EN, không thiếu key; không thấy lỗi mã hóa hoặc import Firebase vượt tầng.
- Lỗi ban đầu về chuỗi, metadata Firebase/Kotlin, exception SDK trong JVM và Snackbar che Logout
  đã được sửa; các ca tương ứng không bị tắt/ignore để đạt kết quả.
- Firebase thật: **chưa xác nhận chức năng nào hoạt động với project thật**, vì không có cấu hình.
  Đã có Android test adapter từ chối đăng nhập khi thiếu cấu hình.
- Demo: chỉ còn mã lịch sử không bind, preview và fake test; không có demo login production.
- Chưa chạy Rules Emulator, native biometric thật/TalkBack, hoặc nghiệm thu email/claims/server nhiều thiết bị.

## Danh sách file thay đổi trong task

Không bao gồm các thay đổi `.idea/` và `integration_backups/` đã có trước task và được giữ nguyên.
- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileLocaleUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/FirebaseAuthErrorMappingTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/giathinh/uthconnect/data/di/AuthDataModule.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SessionPreferences.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/StudentProfile.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/UserRole.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/SessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/AccessStudentSessionUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ChangePasswordUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ResetPasswordUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/UpdateStudentContactUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/README.md`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/README.md`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/ResetPasswordUseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SignInUseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/PasswordChangeValidatorTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModelTest.kt`
- `build.gradle.kts`
- `docs/AUTH_PROFILE_STATUS.md`
- `docs/Design.md`
- `docs/REPO_TREE.md`
- `gradle/libs.versions.toml`

Plugin được cài riêng theo yêu cầu tiếp theo của người dùng: `ponytail@ponytail` phiên bản 5.3.3,
installed/enabled được CLI xác nhận; không dùng plugin mới để sửa phạm vi task hoặc chạy workflow ngầm.


## Hợp nhất origin/feature/auth-phu — 03/11/2026

Nguồn remote: `d608a09d7ed361671cbd5ee8e6b32aeef3940d0b` (32 commit mới).
Backup không thay đổi: `backup/auth-phu-before-rebase-d2761b9` tại
`d2761b9856d593d6ea795441afa81cc965165de6`. Commit replay giữ tiêu đề `KAN _75 1/2`.
Phần này cập nhật trạng thái mới, thay cho các bảng phiên bản/kết quả trước rebase bên trên.

### Chín conflict thực tế

| File | Hợp nhất |
|---|---|
| app/build.gradle.kts | Giữ toolchain, kotlin-android, BuildConfig/mock data flag, Storage và dependency remote; apply Google Services khi có cấu hình; bật lại AAR metadata |
| build.gradle.kts | Giữ kotlin-android và plugin remote; mỗi alias chỉ khai một lần |
| gradle/libs.versions.toml | Giữ AGP/Kotlin/Hilt/Firebase remote; chọn AndroidX tương thích SDK 36 theo metadata thật |
| AndroidManifest.xml | Giữ USE_BIOMETRIC, INTERNET và ACCESS_NETWORK_STATE; không trùng permission |
| AuthDataModule.kt | Giữ provider binding; Auth và Contact luôn dùng Firebase, Source dùng provider SDK chung của remote, không fallback demo |
| FirebaseAuthRepository.kt | Giữ contract authenticate/findStudent; tích hợp SDK chung qua adapter, bảo toàn claims, reset/change password/contact/cancellation; không giữ suy đoán email hoặc tự tạo hồ sơ học tập khi thiếu server record |
| values/strings.xml | Hợp tất cả key Auth và Home/Announcement/Lost & Found, sửa vị trí resources đóng sớm; không mất key remote |
| values-en/strings.xml | Hợp đầy đủ như VI, không trùng key, một root resources hợp lệ |
| docs/REPO_TREE.md | Giữ cây các module remote, bổ sung lát cắt Auth và test, bỏ hàng mô tả cây bị lặp do conflict |

README tự hợp nhất, không phải conflict. Không dùng chiến lược chọn cả ours/theirs cho toàn bộ rebase.
Các API/fixture demo vẫn tồn tại, cùng lịch/bảng tin/hồ sơ/điểm fake, các graph mới và rules/indexes remote.
Cờ useMockData không còn chọn cơ chế xác thực: yêu cầu production luôn Firebase và không fallback demo
có ưu tiên theo quyết định người dùng. Demo chỉ dùng riêng trong test; các module mock khác không đổi.

### Cấu hình cuối, đã kiểm tra bằng Gradle

- AGP 8.13.0; Gradle Wrapper 8.13 (checksum remote); Kotlin 2.2.10; compileSdk 36; targetSdk 36; minSdk 31.
- Dagger Hilt 2.58; KSP 2.3.12; Compose BoM 2026.02.01; Firebase BoM 33.10.0; Google Services 4.4.2.
- Giữ toàn bộ các giá trị trên từ remote, không ghi đè bằng bộ AGP 9.3.3/Gradle 9.5/SDK 37.1 của backup.
- Điều chỉnh AndroidX Core 1.19.1 → 1.18.0, Navigation 2.10.2 → 2.9.5,
  AndroidX Hilt 1.4.0 → 1.3.0; nhóm Lifecycle 2.11.0 → 2.10.0.
  Đây là thay đổi dependency tối thiểu theo từng nhóm để tương thích AGP 8.13/SDK 36;
  không đổi API hay logic các module. hiltViewModel trong package mới đã có ở AndroidX Hilt 1.3.
- Cấu hình remote vốn tắt mọi task AarMetadata. Lần đầu compile/unit đạt khi bỏ qua task này,
  nhưng metadata thật xác nhận yêu cầu AGP 9.1/SDK 37 từ Core/Navigation/AndroidX Hilt và Lifecycle Compose.
  Đã bỏ cơ chế tắt task, điều chỉnh các pin trên và chạy kiểm tra metadata thật, không bypass lỗi.

### Bảo toàn và kiểm thử liên quan hợp nhất

- DemoDataRepositoryTest của remote gọi DemoAuthRepository() dù constructor hiện tại cần Context.
  Giữ các assertion login/fixture, chuyển sang DemoAuthRepositoryIntegrationTest trên Android runtime,
  dùng test application context để không ghi đè preferences người dùng.
- Kiểm tra binding demo trước đây được cập nhật thành kiểm tra binding production dùng Firebase
  nhưng không khởi tạo SDK sớm. Các ca lịch, bảng tin và hồ sơ fake vẫn giữ nguyên.
- Home, Announcement, Lost & Found, Schedule, Utilities, navigation, các fixture/data repository khác,
  Firestore rules/indexes được đối chiếu với remote; không chỉnh logic các phần này.
- Giữ stash riêng thay đổi IDE, các file chưa theo dõi và integration_backups; xác minh checksum khi khôi phục.
- Emulator được khởi động lại ở chế độ ẩn vì adb không còn thiết bị; không wipe dữ liệu AVD.

### Rủi ro có sẵn trên remote, không phải đã được giải quyết bởi rebase

`firestore.rules` hiện có nhánh isAdmin/isOrganizer đọc role trong users và cho owner write hồ sơ.
Nếu triển khai nguyên trạng, client có thể sửa document role để tăng quyền backend; không đạt nguyên tắc
claims-only. File này không xung đột và không bị đổi/triển khai trong rebase. Owner backend cần sửa và
kiểm thử Rules Emulator trước production. Client Auth vẫn chỉ tin claims và không tự ghi role.
Firebase thật vẫn chưa có cấu hình/tài khoản để nghiệm thu; không khẳng định đã kiểm thử online.


### Nghiệm thu sau rebase

Lệnh sau rebase:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

- **BUILD SUCCESSFUL**; lượt sau rebase hoàn tất trong 1 phút 23 giây.
- **99 unit test**, 0 failures, 0 errors, 0 skipped; có cả các suite mới của remote.
- **19 Android test**, 0 failures, 0 errors, 0 skipped, trên emulator-5554/Medium_Phone_API_37.0.
- Bản trước rebase: 75 unit/18 Android. Toàn bộ tính năng và kiểm thử Auth trước đó được giữ;
  các suite remote được hợp nhất, một ca demo cần Context được chuyển sang Android test thay vì bỏ qua.
- AAR metadata được bật và đạt; không dựa vào cấu hình bỏ qua kiểm tra của remote để công nhận thành công.
- 12 file presentation Auth/Profile trùng nội dung backup; 33 file bảo vệ thuộc module khác,
  fixture và cấu hình remote được đối chiếu nguyên vẹn. Mỗi locale giữ đủ 200 key remote và có 222 key sau hợp nhất.
- Checksum SHA256 của cả 8 file IDE/integration_backups ban đầu không đổi sau khôi phục stash.
- Không có file bị xóa trong commit; không đưa .idea, integration_backups, build, .gradle,
  local.properties, google-services.json hoặc credentials vào commit.
- git diff --check và git diff --cached --check đạt. Nhánh backup gốc vẫn tồn tại.
- Tiêu đề commit giữ `KAN _75 1/2`; hash sau cùng được báo trong kết quả push, không tự tham chiếu hash trong file.

Tham khảo API giữ tương thích: [AndroidX Hilt 1.3.0](https://developer.android.com/jetpack/androidx/releases/hilt#1.3.0),
[Navigation 2.9.5](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.5).
Tính tương thích của tổ hợp cuối được xác nhận bằng artifact metadata và Gradle thực tế trên repository.

### Danh sách 50 file trong commit sau hợp nhất

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/giathinh/uthconnect/data/repository/DemoAuthRepositoryIntegrationTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileLocaleUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/FirebaseAuthErrorMappingTest.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/di/AuthDataModule.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SessionPreferences.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/StudentProfile.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/UserRole.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/SessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/AccessStudentSessionUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ChangePasswordUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ResetPasswordUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/UpdateStudentContactUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/README.md`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/README.md`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/DemoDataRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/ResetPasswordUseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SignInUseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/PasswordChangeValidatorTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModelTest.kt`
- `docs/AUTH_PROFILE_STATUS.md`
- `docs/DATA_LAYER_GUIDE.md`
- `docs/Design.md`
- `docs/REPO_TREE.md`
- `gradle/libs.versions.toml`
