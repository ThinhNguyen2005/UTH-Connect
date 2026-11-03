# Auth/Profile — phạm vi và nghiệm thu

> Xem [kết quả mới nhất](#auth3-latest); các mục phía dưới trước đó là lịch sử nghiệm thu.

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


# Cập nhật nghiệm thu Auth3-1 — 03/11/2026

Phần này là trạng thái hiện tại, thay thế các mô tả giới hạn của giai đoạn trước khi có khác biệt.
**Kết luận: PARTIAL; chưa đủ điều kiện tuyên bố Auth3-1 hoàn thành production.**
Mã nguồn/UI/backend/Rules và kiểm thử local đã triển khai; Firebase thật, Google OAuth, email giao thực tế,
triển khai backend/Rules, nguồn học vụ và kiểm tra vận hành vẫn BLOCKED.

## Baseline và bảo toàn

- Branch `feature/auth-phu`; HEAD gốc `5efb1f37cc8ca75fed8b861fb0a782aabacd5d5b`, tên `KAN _75 1/2`.
  Đã đối chiếu tracking ref và `git ls-remote origin refs/heads/feature/auth-phu` cùng hash.
  Backup `backup/auth-phu-before-rebase-d2761b9` giữ nguyên; không commit/push/rebase thêm.
- Đã chạy lại baseline: Build PASS, 99 Unit Test PASS, 19 Android Test PASS, không lấy số cũ làm bằng chứng.
- Kế thừa MVVM/Clean Architecture, Hilt, StateFlow, session/biometric, Profile/password/contact,
  navigation và Design System. Các API cũ vẫn giữ, bổ sung capability có default fail cấu hình cho test cũ.
- Không sửa Home/Schedule/Utilities, fixtures/mock hoặc cache của module khác.
  `.idea/*`, `integration_backups/` là thay đổi có sẵn, giữ nguyên và không nằm trong danh sách task.
- Giữ AGP 8.13.0, Gradle 8.13, Kotlin 2.2.10, compile/targetSdk 36, minSdk 31, Hilt 2.58,
  KSP 2.3.12, Firebase BoM 33.10.0, Google Services 4.4.2.
- Bổ sung Credentials 1.5.0, Google ID 1.1.1, Functions theo BoM; không nâng toolchain Android.

## Checklist theo yêu cầu và bằng chứng

PASS dưới đây nghĩa là mã và kiểm thử local đạt; không thay thế nghiệm thu cloud thật.
PARTIAL/BLOCKED là giới hạn còn lại cần xử lý trước PR/triển khai.

| Yêu cầu | Trạng thái | Bằng chứng / giới hạn |
|---|---|---|
| I. Kiểm tra README/docs/Git/baseline và kế thừa | PASS | README.md, phần lịch sử tài liệu này; HEAD/remote và baseline nêu trên |
| II. MVVM/Clean/Hilt/API/Design System/fixtures cũ | PASS | `app/src/main/java/com/giathinh/uthconnect/data/di/AuthDataModule.kt:30`; test cũ giữ nguyên và chạy lại |
| III. Đăng ký email/password, đăng nhập, Google Credential Manager | PARTIAL | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:66`; `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt:73`; thiếu cấu hình OAuth/Firebase thật |
| III. Gửi/resend/reload email, reset, logout, refresh token | PARTIAL | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt:91`; SDK adapter đã nối, chưa nghiệm thu mail/cloud |
| III. Account collision, không link chỉ vì email trùng | PASS | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:244`; SDK error mapping Android test; không có linkWithCredential |
| IV. Exact domain, trim/lowercase, từ chối sai định dạng/subdomain/suffix | PASS | `app/src/main/java/com/giathinh/uthconnect/domain/validation/LoginValidator.kt:6`; `functions/src/policy.cjs:2`; Unit/Emulator tests |
| IV. Chỉ email UTH verified được cấp uthVerified phía server | PASS | `functions/index.cjs:22`; Emulator kiểm tra Admin record thay token/email client |
| V. Callable UID từ auth, Admin SDK, claims STUDENT, giữ role/claims hợp lệ | PASS | `functions/index.cjs:9`; `functions/src/policy.cjs:10`; không deploy |
| V. Tạo users idempotent, không tự xác minh studentId | PASS | `functions/index.cjs:24`; kiểm thử đồng thời, studentId null, không client create/academic update |
| V. Lỗi có cấu trúc, Emulator, beforeUserCreated | PARTIAL | `functions/src/optional-blocking.cjs:4`; blocking chưa export, Identity Platform/triển khai chưa được xác minh |
| VI. Claims-only, owner UID, bảo vệ metadata và role | PASS | `firestore.rules:12`; 22 Emulator tests gồm quyền hợp lệ các module |
| VI. Rules production và review các policy liên module | BLOCKED | Không deploy; owner cần phê duyệt rollout claims, review registrations read và staging |
| VII. AuthError, validation và các Repository/UseCase | PASS | `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt:33`; domain/usecase/{SignUp,SignInWithGoogle,SendVerification,CheckVerification,RefreshClaims,SignOut}UseCase.kt |
| VIII. Session theo Firebase/claims, cold start/refresh/network/retry | PASS | `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt:85`; test token role update, invalid token, không vượt biometric |
| VIII. Logout/private state/cache độc lập | PARTIAL | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseProfileRemoteStore.kt:17`; profile không ghi disk cache mới; dữ liệu SDK cache từ bản cũ cần audit có phạm vi |
| VIII. Profile theo UID, contacts, role/verified từ claims, missing profile | PARTIAL | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:204`; không mặc định role khi thiếu claim; REST/Firestore thật chưa nghiệm thu |
| IX. Biometric không cấp phiên/claims, hủy lifecycle/chống trùng | PASS | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt:137`; session/biometric Unit Test, chưa có phần cứng biometric để nghiệm thu prompt thật |
| X. Tab login/signup, Google, password, loading/error, forgot screen | PASS | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt:45`; `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordScreen.kt:29`; UI tests |
| X. Verify screen/cooldown60/foreground reload→claims→Main | PASS | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailViewModel.kt:24`; `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailRoute.kt:20`; pending identity không phải phiên Main |
| X. Channel events, lifecycle state, badge Role/Verified, VI/EN, previews | PASS | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt:34`; ProfileScreen, resources values/values-en, previews Loading/Error |
| XI. Unit/UI/Emulator suites và bảo toàn test cũ | PASS | Kết quả, lệnh và đường dẫn báo cáo ở phần kiểm thử bên dưới |
| XII. Firebase Console, SHA, JSON, Enumeration Protection, template | BLOCKED | Không có app/google-services.json/dự án thật; hướng dẫn docs/AUTH3_FIREBASE_SETUP.md; không tạo credential giả |
| XIII. Git/scope/toolchain/cloud controls | PASS | Không commit/push/deploy/migration; chỉ Auth3-1 và cấu hình/test/docs trực tiếp |
| XIV. README M0, docs trạng thái/cấu hình/tree | PASS | README.md; tài liệu này; docs/AUTH3_FIREBASE_SETUP.md; docs/REPO_TREE.md |

## Các lỗi bảo mật/chức năng đã xử lý

| Mức độ ban đầu | Lỗi | Cách xử lý / bằng chứng |
|---|---|---|
| Nghiêm trọng | users.role do client sửa có thể thành quyền admin | Tái hiện bằng baseline.security.test; bỏ fallback document role trong rules; chống nâng quyền ở Emulator |
| Cao | Hồ sơ UID khác đọc được theo policy cũ | users chỉ owner verified get/update contacts; list/create/delete client bị chặn; test cross UID |
| Cao | Quyền STUDENT có thể được mặc định ở client khi thiếu role | mapProfile chỉ map claim hợp lệ; thiếu/role lạ bị từ chối; Unit Test riêng |
| Cao | Pending Firebase credential có nguy cơ bị coi như đăng nhập | AuthIdentity tách profile; chỉ checkVerification + claims mới start session/Main |
| Cao | Refresh token tự mở khóa biometric | Observer chỉ cập nhật phiên đã mở; Unit Test token không vượt biometric |
| Trung bình | Lỗi/cancellation khi lưu phiên có thể để credential một phần | Verification/Google usecases thu hồi auth trong NonCancellable; không nuốt CancellationException |
| Trung bình | Profile private form còn sau logout/cache đĩa mới | Reset ProfileUiState khi session null; transport REST không disk cache, không purge module khác |
| Trung bình | Gọi Google lặp | AuthViewModel chặn request và job đang chạy; không lưu ID token vào state/log |
| Trung bình | Thiếu trạng thái/validation và lỗi SDK nhất quán | Signup/Verify/Forgot VMs, typed AuthError, lỗi form được xóa khi sửa dữ liệu, VI/EN |

## Kiểm thử và giới hạn bằng chứng

- Unit Test cuối: **131 tests, 0 failures, 0 errors, 0 skipped**; XML `app/build/test-results/testDebugUnitTest/`.
- Build + Android Test cuối: **BUILD SUCCESSFUL**, **24/24 Android Test**, không skip,
  emulator `Medium_Phone_API_37.0`, serial emulator-5554. Lệnh thực tế:
  `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest`.
- HTML: `app/build/reports/tests/testDebugUnitTest/index.html`,
  `app/build/reports/androidTests/connected/debug/index.html`; Android XML trong
  `app/build/outputs/androidTest-results/connected/debug/`.
- Firebase Emulator cuối: **22/22 PASS, 0 fail/skip** sau cập nhật Rules và vá dependencies,
  Node 22.23.4, firebase-tools 15.34.2, Auth/Functions/Firestore localhost, project demo-uth-auth3.
  Callable chạy bằng Admin SDK với Auth Emulator token; Rules chạy trên Firestore Emulator thật.
- Baseline exploit: 1/1 PASS trên rules lịch sử trước thay đổi, chứng minh hai lỗ hổng nêu trên;
  không dùng test khai thác này để đánh giá rules mới.
- Trong quá trình phát triển đã có test compile lỗi khai báo fake và 1 UI assertion sai nút ở trạng thái
  Configuration; đã sửa và chạy lại toàn suite. Không bỏ qua, xóa hoặc làm yếu điều kiện bảo mật của test cũ.
- `npm audit --omit=dev`: **0 vulnerabilities**. Audit toàn bộ: **13 moderate, 0 high/critical**,
  thuộc dependencies tooling/test Firebase CLI (glob/minimatch/uuid/archiver/OTel và nhánh phụ thuộc).
  Không dùng audit fix --force để downgrade/break SDK. package-lock và overrides grpc-js/rimraf→glob
  giữ runtime đã vá, suites đã chạy lại. Cần theo dõi bản vá upstream cho tooling trước CI ngoài môi trường local.
- Không có bằng chứng end-to-end Firebase production, Google OAuth thật, giao email/nhấn link,
  account collision giữa hai provider thật hoặc biometric phần cứng. Không nhận PASS cho các hạng mục đó.
- Existing demo repository/fixture và fake chỉ phục vụ test/preview/module cũ; Hilt production vẫn
  bind FirebaseAuthRepository. Không có fallback demo. Không tạo dữ liệu hoặc tài khoản cloud giả.

## Rủi ro còn lại và việc cần làm trước PR/deploy

1. **Cao/BLOCKED:** thiếu Firebase Android config/Web client ID, provider/Console/SHA, backend deployment,
   staging data và tài khoản @ut.edu.vn kiểm thử. Thực hiện docs/AUTH3_FIREBASE_SETUP.md rồi nghiệm thu thật.
2. **Cao:** phối hợp single-writer khi đổi custom claims; Firebase Admin không cung cấp CAS.
   Callable idempotent không thể bảo vệ writer Admin khác đang ghi đồng thời nếu không cùng giao thức.
3. **Cao, policy ngoài Auth3-1:** registrations của event vẫn đọc bởi mọi tài khoản đã đăng nhập,
   theo chính sách remote. Nếu chứa PII cần owner sự kiện review, bổ sung test tác động và PR riêng;
   không tự sửa policy này trong task. Dữ liệu sự kiện/thông báo công khai authenticated giữ nguyên.
4. **Trung bình:** token đã cấp có thể tồn tại tới refresh/hết hạn sau disable/role change;
   cần chính sách revocation hệ thống nếu yêu cầu thu hồi tức thì. Client foreground/unlock đã reload.
5. **Trung bình:** cache SDK private từ bản cũ chưa được migration; REST mới không ghi cache hồ sơ.
   Không global clearPersistence vì ảnh hưởng module khác. REST PATCH/GET cần nghiệm thu server thật.
6. **Trung bình:** tooling dev còn 13 cảnh báo moderate; không có cảnh báo ở runtime dependencies.
7. **BLOCKED:** Identity Platform beforeCreate chưa deploy; email ngoài trường vẫn có thể tạo Auth account
   trực tiếp nếu dùng API ngoài app, nhưng không được cấp UTH claims/truy cập dữ liệu UTH.
8. **BLOCKED:** chưa có nguồn học vụ đối chiếu MSSV; studentId mới null và UI hiển thị chờ cập nhật.
   Không nhận studentId do client tự xác minh; badge thể hiện email UTH verified, không giả danh xác minh MSSV.
9. Cần backend/owner review Functions+Rules, rollout claims cho tài khoản cũ và staging module sự kiện,
   lịch học/thông báo trước deploy. Không deploy Rules lên production trong phiên này.

## Danh sách file thuộc thay đổi Auth3-1

Danh sách dưới không gồm `.idea/`, integration_backups, build, node_modules, local.properties hoặc credential.

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3FlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/FirebaseAuthErrorMappingTest.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseProfileRemoteStore.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/AuthIdentity.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/StudentProfile.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CheckVerificationUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ManageAuthAccountUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/RefreshClaimsUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SendVerificationUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInWithGoogleUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignOutUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/validation/LoginValidator.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/Auth3ValidatorTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3UseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3ViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/ForgotPasswordViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/VerifyEmailViewModelTest.kt`
- `docs/AUTH3_FIREBASE_SETUP.md`
- `docs/AUTH_PROFILE_STATUS.md`
- `docs/REPO_TREE.md`
- `firebase.json`
- `firestore.rules`
- `functions/index.cjs`
- `functions/package-lock.json`
- `functions/package.json`
- `functions/src/optional-blocking.cjs`
- `functions/src/policy.cjs`
- `functions/test/auth3.emulator.test.cjs`
- `functions/test/baseline.security.test.cjs`
- `gradle/libs.versions.toml`


# Kiểm định cuối Auth3-1 — 03/11/2026

**Kết luận hiện tại: PARTIAL. Không đủ bằng chứng hoàn thành Firebase production.**
Phần này là kết quả kiểm định mới nhất, thay thế số test/bằng chứng dòng của các phần lịch sử phía trên.
Đã đọc lại README (799 dòng), báo cáo trạng thái, hướng dẫn Firebase và toàn bộ 64 file của lần triển khai.
Package lock 10.275 dòng được parse và đối chiếu dependency/dev flags; XML VI/EN và JSON được parse.
Lần kiểm định bổ sung sửa lỗi/test/docs, không viết lại các thành phần đã hoạt động đúng.

## Phạm vi, mã đã có và mức bằng chứng

| Phạm vi | Trạng thái | Giới hạn |
|---|---|---|
| 64 file Auth3-1 trước kiểm định | PASS kiểm tra mã | Đã đối chiếu inventory, compiler/test, ranh giới Presentation/Domain/Data và cấu hình |
| Build/Unit/UI/Emulator local | PASS | Các kết quả mới bên dưới; UI dùng fake test, Emulator dùng backend local thật |
| Auth Email/Password/Google/verify/reset production | BLOCKED | Không có app/google-services.json, OAuth client/SHA, tài khoản và Firebase project nghiệm thu |
| Profile REST production, biometric phần cứng | BLOCKED | Chưa có cấu hình/backend thật hoặc thiết bị sinh trắc học để nghiệm thu |
| Bảo toàn claim khi nhiều backend writer đồng thời | PARTIAL | Giữ claim hợp lệ và chặn false; chưa có CAS/cơ chế single-writer ngoài repo được xác minh |
| Cache/thu hồi quyền tức thời toàn hệ thống | PARTIAL | Không tạo disk cache Profile mới; cache bản cũ, token cũ và policy module khác cần owner review |
| Git/toolchain/module ngoài Auth3-1 | PASS | Branch feature/auth-phu; HEAD 5efb1f37cc8ca75fed8b861fb0a782aabacd5d5b; không commit/push/merge/deploy |

Production vẫn bind FirebaseAuthRepository; không fallback demo. Firebase BoM 33.10.0 và toàn bộ
AGP 8.13.0/Gradle 8.13/Kotlin 2.2.10/SDK 36/Hilt 2.58 được giữ nguyên. Mock của các module khác còn nguyên.
`git diff --name-only` với Home/Schedule/Utilities, root build.gradle.kts/settings.gradle.kts,
Gradle Wrapper không có thay đổi. Credentials/Google ID/Functions là dependency trực tiếp Auth3-1 đã được build.
Không coi BuildConfig.USE_MOCK_DATA của nhóm là quyền bật demo cho Auth production.

## Lỗi xác minh và sửa trong lần kiểm định

1. **Cao — tự cấp lại quyền đã thu hồi:** mergeVerifiedClaims từng ghi true đè lên uthVerified=false
   do backend đặt. Kiểm thử trực tiếp trước sửa thất bại với `Missing expected exception` (exit 1),
   xác nhận lỗi, không chỉ suy đoán. Đã chặn explicit false ở backend trước tạo profile và kiểm tra lại
   trước setCustomUserClaims; client cũng từ chối provisioning khi thấy false. Claims/role khác giữ nguyên.
2. **Trung bình — thông báo Verify sai nhóm lỗi:** PermissionDenied/Disabled/InvalidSession trước đó
   rơi về lỗi không mong đợi, ApprovalRequired hiển thị nhầm lỗi cấu hình. Đã dùng các resource lỗi
   tương ứng có sẵn ở VI/EN, không thêm màn hình hoặc đổi luồng đang hoạt động đúng.
3. **Khoảng trống kiểm thử:** bổ sung thu hồi quyền, token đổi role/network, callable chưa triển khai,
   cold restart/logout, foreground lỗi, Back sau logout, Verify/Forgot VI/EN font 200%, ký tự điều khiển email.
   Kiểm thử ký tự xuống dòng đã chứng minh validator hiện có từ chối đúng, vì vậy giữ nguyên logic domain.

Bằng chứng sửa:

- `functions/src/policy.cjs:12`
- `functions/index.cjs:23`
- `functions/index.cjs:47`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:106`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:195`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailScreen.kt:39`

**Quy ước thu hồi cần owner chấp thuận:** uthVerified=false là thu hồi rõ ràng; chỉ backend có thẩm quyền
được bật lại. Xóa claim không đồng nghĩa thu hồi bền vững: tài khoản trường verified thiếu claim vẫn được
onboarding theo yêu cầu task. Nếu cần cách thu hồi khác, phải thống nhất metadata/backend đáng tin cậy
và kiểm thử trước rollout. Cơ chế này chưa giải quyết race với Admin writer ngoài repo hoặc token cũ chưa refresh.

## Checklist các tình huống bắt buộc

PASS ở cột local không mang nghĩa đã chạy production.

| Tình huống | Local | Bằng chứng hiện tại | Production |
|---|---|---|---|
| Chưa đăng nhập | PASS | `functions/test/auth3.emulator.test.cjs:65`; `functions/test/auth3.emulator.test.cjs:129` | BLOCKED |
| Email ngoài trường/suffix/subdomain | PASS | `app/src/main/java/com/giathinh/uthconnect/domain/validation/LoginValidator.kt:6`; `functions/src/policy.cjs:2`; `functions/test/auth3.emulator.test.cjs:88` | BLOCKED |
| Email chưa xác minh | PASS | `functions/test/auth3.emulator.test.cjs:77`; `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt:49` | BLOCKED |
| Email verified nhưng chưa có claim | PASS | `functions/test/auth3.emulator.test.cjs:68`; `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt:115`; thiếu role không mặc định tại client | BLOCKED |
| Tài khoản bị khóa/xóa | PASS | `functions/test/auth3.emulator.test.cjs:121`; `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt:169` | BLOCKED |
| Role đổi/thu hồi uthVerified=false | PASS | `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt:93`; `functions/test/auth3.emulator.test.cjs:199` | PARTIAL: token đã cấp/race backend |
| Mất mạng lúc xác thực/foreground | PASS | `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt:115`; `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt:169`; SDK error mapping Android test | BLOCKED |
| Functions chưa deploy | PASS mã/contract | `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:233`; `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt:115`; không giả lập cloud thành công | BLOCKED HTTP/Firebase thật |
| Tự sửa role/uthVerified/metadata | PASS | `functions/test/auth3.emulator.test.cjs:148`; `functions/test/auth3.emulator.test.cjs:154`; `firestore.rules:38` | BLOCKED deploy rules |
| UID khác đọc/ghi hồ sơ | PASS | `functions/test/auth3.emulator.test.cjs:140`; users get/update chỉ owner verified | BLOCKED deploy rules |
| Logout rồi nhấn Back | PASS | `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt:93`; session vẫn null | BLOCKED tài khoản thật |
| Khởi động lại sau đăng nhập/logout | PASS | `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt:182`; nhớ UID cần xác thực Firebase lại, không nhớ thì logout | BLOCKED process death với SDK thật |
| Sinh trắc học/lifecycle | PASS logic | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt:137`; `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt:61`; foreground reload/cancel ON_STOP | BLOCKED phần cứng |
| Cooldown 60s/chống gọi trùng | PASS | `app/src/test/java/com/giathinh/uthconnect/presentation/auth/VerifyEmailViewModelTest.kt:47`; ViewModel giữ qua đổi cấu hình, tạo VM mới chờ lại60 | BLOCKED quota/mail thật |
| Google/account collision | PASS mã/typed lỗi | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt:73`; FirebaseAuthErrorMappingTest; không linkWithCredential theo email | BLOCKED OAuth/provider thật |
| Quên/reset mật khẩu không dò tài khoản | PASS mã/UI | `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordViewModel.kt:17`; repository che ERROR_USER_NOT_FOUND | BLOCKED giao mail/reset link thật |
| Compose/Navigation/Preview/VI-EN | PASS | `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3FlowUiTest.kt:91`; 108 key Auth/Profile VI/EN tương ứng, Preview Loading/Error compile; gate pending→Verify, valid→Main | BLOCKED trải nghiệm provider thật |

## Kết quả test thực tế sau sửa

- **Gradle Build PASS**, lệnh cuối:
  `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest`.
- **136 Unit Test, 0 failures/errors/skipped**. Lượt cuối compile và chạy test mới, không chỉ dùng kết quả cache cũ.
- **28 Android Test, 0 failures/errors/skipped**, emulator-5554, Medium_Phone_API_37.0. Lượt cuối có kiểm tra Back.
- **24 Firebase Emulator Test, 0 fail/skip**, Functions/Auth/Firestore local demo-uth-auth3, CLI exit 0.
  Lệnh từ functions/:
  `firebase emulators:exec --config ../firebase.json --project demo-uth-auth3 --only auth,firestore,functions "node --test --test-concurrency=1 test/auth3.emulator.test.cjs"`.
- Emulator có cảnh báo MetadataLookupWarning khi tìm metadata credential; các host Auth/Firestore đều localhost,
  toàn suite exit 0. Không dùng cảnh báo này làm bằng chứng kết nối Firebase cloud.
- Báo cáo XML/HTML: `app/build/test-results/testDebugUnitTest/`, `app/build/reports/tests/testDebugUnitTest/`,
  `app/build/outputs/androidTest-results/connected/debug/`, `app/build/reports/androidTests/connected/debug/`.
- `git diff --check` PASS; cảnh báo LF→CRLF từ Git là thông báo chuẩn hóa EOL, không phải lỗi whitespace.
- Không nâng dependency/toolchain trong lần kiểm định; package.json/package-lock không thay đổi thêm.

## Rà soát đủ 13 moderate của tooling

`npm audit --json` mới: **13 moderate, 0 high/critical**; `npm audit --omit=dev --json`: **0**.
Đây là 13 package entries, quy về bốn advisory gốc, không phải 13 lỗi độc lập trong ứng dụng.
Lockfile xác nhận node_modules/@opentelemetry/core 1.30.1, @google-cloud/pubsub 5.3.1,
uuid 9.0.1, glob 10.5.0, firebase-tools 15.34.2 đều dev=true; glob runtime của rimraf là14.0.3 đã vá.
Những package bị audit đánh dấu không thuộc cây production khi omit dev. APK không đóng gói npm packages.
Đây là đánh giá dependency/reachability hiện tại, **không phải chứng nhận toàn bộ CVE runtime Android**
và chưa xác minh artifact Cloud Functions đã deploy vì chưa có deploy. CI/deployment phải dùng đúng dependency production.

| Package bị audit đánh dấu | Gốc cảnh báo | Khả năng ảnh hưởng trong repo hiện tại |
|---|---|---|
| @opentelemetry/core | GHSA-8988-4f7v-96qf | Baggage header quá lớn có thể tiêu tốn bộ nhớ khi dùng propagator; nằm dev tree, callable không nhận/parse baggage trong code Auth3 |
| @google-cloud/pubsub | Kéo @opentelemetry/core | Nhánh CLI; Auth3 không dùng Pub/Sub production |
| glob | GHSA-3g3m-2fjr-6pw4 + minimatch | DoS CPU/I/O khi glob cây/pattern không tin cậy và AbortSignal không dừng traversal; tooling CLI/đóng gói, không phải đầu vào callable |
| minimatch | GHSA-g3hv-75wg-m249 | Amplification pattern gây CPU/memory DoS; chưa xác định đường từ request Auth3 tới glob pattern |
| readdir-glob | Kéo minimatch | Lan theo archiver; rủi ro CLI/CI với cây đầu vào không tin cậy |
| superstatic | Kéo minimatch | Nhánh tooling static hosting; task không chạy hosting emulator |
| archiver-utils | Kéo glob | Nhánh đóng gói CLI, không nằm dependencies runtime khai báo |
| archiver | Kéo archiver-utils/readdir-glob/zip-stream | Nhánh CLI/CI, cần tránh build/deploy nội dung không tin cậy với credentials |
| zip-stream | Kéo archiver-utils | Cùng nhánh đóng gói, không phải lỗi riêng mới |
| exegesis | Kéo glob | Tooling OpenAPI; không được code callable Auth3 import |
| uuid | GHSA-w5hq-g745-h8pq | Advisory áp dụng API v3/v5/v6 khi có buf/offset; nhánh gaxios dùng uuid.v4() không truyền buffer |
| gaxios (bản dev dưới firebase-tools) | Kéo uuid | `functions/node_modules/firebase-tools/node_modules/gaxios/build/src/gaxios.js:417` gọi v4(); chưa xác định exploit qua nhánh này |
| firebase-tools | Tổng hợp các nhánh trên | Tool chạy local/CI, không deploy vào runtime khi omit dev; vẫn cần theo dõi upstream, không coi moderate là vô hại |

Môi trường Emulator chỉ bind127.0.0.1; không expose cổng ra Internet, không deploy/mở hosting trong lần kiểm định.
Cần theo dõi bản vá tương thích ở upstream; không ép minimatch major mới vào dependency cũ, không audit fix --force
hay downgrade Firebase SDK/CLI để làm mất cảnh báo bằng cách phá compatibility. Không có bằng chứng 13 mục này
đang ảnh hưởng runtime production Auth3; vẫn có rủi ro tooling/CI nếu nhập repository/pattern/header không tin cậy.

Nguồn advisory đã kiểm tra:
[OpenTelemetry](https://github.com/open-telemetry/opentelemetry-js/security/advisories/GHSA-8988-4f7v-96qf),
[UUID](https://github.com/uuidjs/uuid/security/advisories/GHSA-w5hq-g745-h8pq),
[minimatch](https://github.com/advisories/GHSA-g3hv-75wg-m249),
[glob](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4).

## Những file được sửa thêm trong lần kiểm định này

- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailScreen.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3FlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt`
- `functions/index.cjs`
- `functions/src/policy.cjs`
- `functions/test/auth3.emulator.test.cjs`
- `docs/AUTH_PROFILE_STATUS.md`
- `docs/AUTH3_FIREBASE_SETUP.md`

Tổng inventory hiện tại: **65 file task** = 64 file triển khai trước + AuthProfileFlowUiTest.kt được bổ sung
thao tác Back. Không thêm file production mới, không đụng .idea/integration_backups hoặc dữ liệu người khác.

## Điều kiện trước commit, PR và nghiệm thu production

- Trước commit: review diff đúng phạm vi, whitelist 65 file task; loại .idea/, integration_backups/, build/,
  .gradle/, local.properties, node_modules/logs và mọi credentials; diff check/test đạt, tài liệu cùng kết quả.
  Phiên này không stage/commit/push/merge, index không có file staged.
- PR phải ghi PARTIAL/BLOCKED trung thực, đính kèm kiểm thử và yêu cầu backend owner review.
  Không gọi đây là hoàn tất production hoặc tự merge develop/main. Nếu mở draft PR, các blocker phải giữ rõ.
- Trước nghiệm thu production: cung cấp cấu hình Firebase thật, SHA/OAuth/providers, Password Policy,
  Enumeration Protection, template; deploy staging Functions/Rules sau phê duyệt; nghiệm thu SDK Android,
  Profile REST, Google collision, giao email/links, cold start/process death và biometric thiết bị thật.
- Owner thống nhất uthVerified=false để thu hồi, single-writer cho claims, rollout tài khoản cũ;
  kiểm tra ID token cũ/revocation. Chưa có cơ chế thu hồi tức thời toàn hệ thống được nghiệm thu.
- Owner module sự kiện review policy registrations cho phép mọi tài khoản authenticated đọc (được giữ từ remote),
  đánh giá PII và test tác động trước khi sửa. Không thay chính sách này trong lần kiểm định.
- Audit cache SDK cũ theo UID; không purge cache chung gây mất dữ liệu module khác. Nguồn học vụ/MSSV,
  Identity Platform blocking và cấu hình App Check vẫn là việc owner/backend cần quyết định, chưa tự triển khai.

## Inventory đối chiếu 64 file gốc và file kiểm thử bổ sung

Các đường dẫn bên dưới đã được đọc; dòng đầu mối giúp review cấu trúc. Với code SDK/backend phụ thuộc cloud,
PASS review không thay thế trạng thái BLOCKED production đã nêu. Source usecases/models/VM compile và có test;
lockfile được parse, tài nguyên VI/EN có108 key Auth/Profile tương ứng. Dòng tài liệu có thể đổi sau cập nhật.

| File | Dòng đầu mối | Loại kiểm tra |
|---|---|---|
| `.gitignore` | 1 | tài liệu/resources/ignore |
| `README.md` | 1 | tài liệu/resources/ignore |
| `app/build.gradle.kts` | 1 | dependency/config/rules |
| `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt` | 13 | test/coverage |
| `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3FlowUiTest.kt` | 22 | test/coverage |
| `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt` | 33 | test/coverage |
| `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/FirebaseAuthErrorMappingTest.kt` | 25 | test/coverage |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt` | 28 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt` | 25 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseProfileRemoteStore.kt` | 17 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt` | 21 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/model/AuthIdentity.kt` | 4 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/model/StudentProfile.kt` | 4 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt` | 9 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CheckVerificationUseCase.kt` | 11 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ManageAuthAccountUseCase.kt` | 6 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/RefreshClaimsUseCase.kt` | 6 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SendVerificationUseCase.kt` | 6 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInUseCase.kt` | 16 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInWithGoogleUseCase.kt` | 13 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignOutUseCase.kt` | 6 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCase.kt` | 10 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/domain/validation/LoginValidator.kt` | 4 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt` | 3 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt` | 44 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt` | 45 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt` | 6 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt` | 24 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/README.md` | 1 | tài liệu/resources/ignore |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordAction.kt` | 2 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordRoute.kt` | 11 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordScreen.kt` | 29 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordUiState.kt` | 4 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordViewModel.kt` | 17 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailAction.kt` | 3 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailRoute.kt` | 15 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailScreen.kt` | 25 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailUiState.kt` | 5 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailViewModel.kt` | 24 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt` | 49 | SDK/domain/UI/data review + compile |
| `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt` | 25 | SDK/domain/UI/data review + compile |
| `app/src/main/res/values-en/strings.xml` | 1 | tài liệu/resources/ignore |
| `app/src/main/res/values/strings.xml` | 1 | tài liệu/resources/ignore |
| `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt` | 15 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepositoryTest.kt` | 15 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt` | 21 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/domain/validation/Auth3ValidatorTest.kt` | 6 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt` | 13 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3UseCaseTest.kt` | 12 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3ViewModelTest.kt` | 23 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/ForgotPasswordViewModelTest.kt` | 19 | test/coverage |
| `app/src/test/java/com/giathinh/uthconnect/presentation/auth/VerifyEmailViewModelTest.kt` | 23 | test/coverage |
| `docs/AUTH3_FIREBASE_SETUP.md` | 1 | tài liệu/resources/ignore |
| `docs/AUTH_PROFILE_STATUS.md` | 1 | tài liệu/resources/ignore |
| `docs/REPO_TREE.md` | 109 | tài liệu/resources/ignore |
| `firebase.json` | 1 | dependency/config/rules |
| `firestore.rules` | 1 | dependency/config/rules |
| `functions/index.cjs` | 9 | tài liệu/resources/ignore |
| `functions/package-lock.json` | 10 | dependency/config/rules |
| `functions/package.json` | 14 | dependency/config/rules |
| `functions/src/optional-blocking.cjs` | 7 | tài liệu/resources/ignore |
| `functions/src/policy.cjs` | 1 | tài liệu/resources/ignore |
| `functions/test/auth3.emulator.test.cjs` | 56 | test/coverage |
| `functions/test/baseline.security.test.cjs` | 8 | test/coverage |
| `gradle/libs.versions.toml` | 1 | dependency/config/rules |


<a id="auth3-latest"></a>

# Hoàn thiện phần còn thiếu Auth3-1 — trạng thái mới nhất

**PARTIAL, chưa nghiệm thu staging/production.** Kết quả dưới đây thay thế các mục lịch sử về
claims writer, registrations và số bài test ở phía trên. Branch feature/auth-phu;
HEAD vẫn 5efb1f37cc8ca75fed8b861fb0a782aabacd5d5b. Không stage/commit/push/merge/deploy/tạo PR.
Đã kiểm tra lại README, Design, tài liệu Auth3-1 và toàn bộ file task trong working tree;
không ghi đè các thay đổi có sẵn của thành viên khác.

## Thực hiện thêm trong lượt này

- Thêm withClaimsLock dùng atomic Firestore create theo UID; verifyUthAccount đọc/kiểm tra Admin record
  bên trong khóa và giữ claim/role hợp lệ. Các writer hợp tác được serialize; callback đọc lại record
  trước ghi. Kết quả ghi không xác định giữ khóa để owner đối soát; không tự lấy lại khóa cũ theo TTL.
- Client map lỗi contention TooManyRequests sang thông báo thử lại, không bỏ qua Firebase gate.
- Rules registrations chỉ cho owner UTH verified hoặc ORGANIZER/ADMIN verified đọc; chặn student
  đọc UID khác/list roster, account chưa verify và anonymous. Quyền staff/owner hợp lệ có test tác động.
- Thêm kiểm thử writer cấp role đồng thời verify; thu hồi false đồng thời verify bằng token cũ;
  lock kẹt từ chối writer sau và client không đọc/xóa được lock.
- Cập nhật README M0, cây repo, hướng dẫn Console và tài liệu staging có checklist thật,
  quy trình đối soát lock, MSSV/cache, nội dung PR và hướng dẫn GIF. Chưa tạo PR hoặc ghi GIF giả.

## Checklist theo yêu cầu mới

| Mục | Trạng thái | Kết quả |
|---|---|---|
| I. Git/tài liệu/checklist/bảo toàn | PASS | Đúng branch, 67 file task; .idea/integration_backups giữ nguyên; index chưa stage |
| II. Console staging | BLOCKED | CLI login:list JSON có authenticatedAccounts=0; thiếu project staging được chọn, JSON Android; không xác nhận được providers/billing/SHA/OAuth/templates |
| III. UID/Admin/domain/verified/claims/idempotency | PASS local | Giữ test trước, thêm khóa hợp tác và kiểm thử đồng thời; không tự cấp MSSV/role đặc quyền |
| III. Mọi writer backend/nhất quán trên cloud | PARTIAL | Phải đưa writer ngoài repo vào helper và giới hạn IAM; không có CAS để chặn Admin writer bỏ qua khóa |
| IV. beforeUserCreated | PARTIAL/BLOCKED | Handler domain-only đã chuẩn bị, chưa export; không xác nhận Identity Platform/Blaze và chưa deploy/test trigger thật |
| V. Rules hồ sơ/claims/metadata/registrations | PASS local | Chặn hồ sơ UID khác/tự sửa quyền và vé người khác; giữ staff access; staging deployment BLOCKED |
| V. Toàn bộ policy module khác | PARTIAL | Không tự đổi orgIds/attendance/checkIn vì chưa có hợp đồng backend; cần owner review trước production |
| VI. Auth thật/Google/email/reset/collision/link | BLOCKED | Chưa có môi trường/tài khoản thật; app xử lý collision, không tự link theo email; chưa có luồng link có consent/reauth để nhận PASS chức năng link |
| VII. Profile/session/logout/biometric | PASS local, PARTIAL thật | Kiểm thử gate/Back/restore còn đạt; Firestore REST thật, token cũ, cache legacy và phần cứng còn cần nghiệm thu |
| VIII. MSSV | PASS ranh giới, BLOCKED dữ liệu | Auth3-1 là email trường; không có API học vụ UTH hoặc mapping email→MSSV; studentId null, client không tự xác minh |
| IX. Hồi quy | PASS | Build/136 Unit/28 Android/28 Emulator/diff check; không sửa Home/Schedule/Utilities/toolchain |
| X. Tài liệu/staging/PR/GIF | PASS chuẩn bị | AUTH3_STAGING_ACCEPTANCE.md; không tạo PR trước nghiệm thu |
| XI. Báo cáo | PASS | Có evidence, file inventory, giới hạn và điều kiện bàn giao |

Không còn FAIL đã xác minh trong các kiểm thử Auth3-1 cuối lượt. Không chuyển BLOCKED thành PASS
vì test dùng Emulator/fake. Phần liên kết tài khoản có consent và nghiệm thu provider thật chưa đạt.

## Bằng chứng quan trọng

- `functions/src/claims-writer.cjs:7`
- `functions/index.cjs:16`
- `firestore.rules:60`
- `functions/test/auth3.emulator.test.cjs:216`
- `functions/test/auth3.emulator.test.cjs:230`
- `functions/test/auth3.emulator.test.cjs:246`
- `functions/test/auth3.emulator.test.cjs:259`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt:242`

## Kết quả thực tế

- Lệnh Gradle cuối: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest.
  **BUILD SUCCESSFUL**, 136 Unit Test và 28 Android Test, 0 failure/error/skip trong XML.
  Dùng GRADLE_USER_HOME trỏ cache hiện có của người dùng, không đổi Gradle Wrapper.
- AVD Medium_Phone_API_37.0 được khởi động ẩn, không wipe; emulator-5554 chạy toàn bộ UI test.
- Firebase Emulator cuối: **28/28 PASS, 0 fail/skip**, CLI exit0, project demo-uth-auth3,
  Functions/Auth/Firestore localhost; không truy cập dữ liệu staging/production.
- Lượt đầu Gradle không thấy device; đã khởi động AVD và chạy lại. Lượt đầu Emulator không load
  Functions trong discovery timeout10s; giữ nguyên assertions, chạy lại với FUNCTIONS_DISCOVERY_TIMEOUT=60
  đã đạt. Không lấy các lượt lỗi đó làm PASS hoặc bỏ qua bài test.
- Node portable hiện chỉ có node.exe, npm CLI không còn trong runtime folder. Đã audit mới qua
  npm registry bulk advisory API với chính package-lock và semver: bốn advisory gốc moderate ở
  @opentelemetry/core, glob, minimatch, uuid; mọi node ảnh hưởng đều dev=true; **runtime affected nodes=0**.
  Số 13 package cảnh báo lan theo dependency là kết quả npm audit của lượt kiểm định trước;
  không nhận là đã chạy npm audit CLI mới trong lượt này. Package-lock/dependency versions không thay đổi.
- Không nâng AGP/Kotlin/SDK/Hilt/Firebase BoM/Gradle. git diff --check PASS (chỉ thông báo LF→CRLF).
- XML: app/build/test-results/testDebugUnitTest/ và app/build/outputs/androidTest-results/connected/debug/.

## File sửa thêm trong lượt này

- `functions/src/claims-writer.cjs`
- `functions/index.cjs`
- `functions/test/auth3.emulator.test.cjs`
- `firestore.rules`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `README.md`
- `docs/REPO_TREE.md`
- `docs/AUTH3_FIREBASE_SETUP.md`
- `docs/AUTH3_STAGING_ACCEPTANCE.md`
- `docs/AUTH_PROFILE_STATUS.md`

## Phần còn cần người dùng/backend/UTH

1. Owner đăng nhập Firebase CLI/Console, chọn hoặc tạo staging riêng và cung cấp project ID/config Android;
   bật providers/Enumeration Protection/Password Policy, xác nhận OAuth consent/Web client/SHA,
   templates tiếng Việt và billing/IAM/Identity Platform. Không tạo credentials giả.
2. Phê duyệt deploy staging rồi chạy đúng phạm vi theo AUTH3_STAGING_ACCEPTANCE.md;
   chưa có quyền/phê duyệt này nên không deploy. Tất cả kiểm thử Firebase staging hiện BLOCKED.
3. Backend owner đưa mọi claims writer vào khóa, review IAM, giám sát và xử lý lock kẹt.
   Writer bỏ qua helper có thể vẫn ghi đè; Auth token cũ không mất hiệu lực tức thì chỉ nhờ lock/Rules.
4. Cache SDK từ bản cũ dùng chung chưa có thông tin phiên bản/UID/retention để purge an toàn;
   không xóa cache chung hoặc dữ liệu demo của nhóm. Profile mới không ghi cache đĩa riêng.
5. UTH cần cung cấp API, quyền và schema học vụ nếu muốn xác minh MSSV thực tế; không suy luận từ email.
6. Owner sự kiện cần xử lý mâu thuẫn có sẵn: FirestoreEventRepository.registerEvent/cancelRegistration
   cập nhật registeredCount từ client, trong khi Rules chỉ cho staff sửa events. Không mở quyền sửa event
   cho STUDENT để che lỗi tích hợp. Policy orgIds/attendance rộng hơn README cũng cần review riêng.
7. Chạy ma trận Firebase thật, Google collision/reset/verification links và thiết bị biometric;
   nghiệm thu Profile REST/process death/refresh token và các module trước tạo PR theo yêu cầu.

Auth3-1 chỉ hoàn thành hoàn toàn khi các mục BLOCKED có evidence thật, policy backend được owner phê duyệt,
không còn thất bại chức năng yêu cầu, và không còn rủi ro quyền/cache chưa được chấp nhận.

## Danh sách sẵn sàng review trước commit thủ công

**67 file task** dưới đây sẵn sàng review/stage theo phạm vi; không đồng nghĩa đã nghiệm thu cloud hoặc
được phép deploy. Loại .idea/, integration_backups/, build/, .gradle/, local.properties, credentials,
node_modules và emulator logs. Chưa stage/commit file nào. Hai file mới từ baseline65 là claims-writer.cjs
và AUTH3_STAGING_ACCEPTANCE.md. Danh sách thay thế inventory cũ cho lượt này.

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/Auth3FlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/AuthProfileFlowUiTest.kt`
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/auth/FirebaseAuthErrorMappingTest.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthSource.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseProfileRemoteStore.kt`
- `app/src/main/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/AuthIdentity.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/model/StudentProfile.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CheckVerificationUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ManageAuthAccountUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/RefreshClaimsUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SendVerificationUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInWithGoogleUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignOutUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCase.kt`
- `app/src/main/java/com/giathinh/uthconnect/domain/validation/LoginValidator.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/README.md`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/forgotpassword/ForgotPasswordViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailAction.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailRoute.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailUiState.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/verifyemail/VerifyEmailViewModel.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuth3RepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/data/repository/PreferencesSessionRepositoryTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/Auth3ValidatorTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3Fakes.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3UseCaseTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/Auth3ViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/ForgotPasswordViewModelTest.kt`
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/VerifyEmailViewModelTest.kt`
- `docs/AUTH3_FIREBASE_SETUP.md`
- `docs/AUTH3_STAGING_ACCEPTANCE.md`
- `docs/AUTH_PROFILE_STATUS.md`
- `docs/REPO_TREE.md`
- `firebase.json`
- `firestore.rules`
- `functions/index.cjs`
- `functions/package-lock.json`
- `functions/package.json`
- `functions/src/claims-writer.cjs`
- `functions/src/optional-blocking.cjs`
- `functions/src/policy.cjs`
- `functions/test/auth3.emulator.test.cjs`
- `functions/test/baseline.security.test.cjs`
- `gradle/libs.versions.toml`
