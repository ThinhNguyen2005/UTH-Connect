# Cấu hình và nghiệm thu Firebase Auth3-1

Ngày 03/11/2026. Mã nguồn đã chuẩn bị; chưa cấu hình hoặc deploy vào dự án cloud.
Không dùng credential mẫu, không dùng Firebase project demo cho production.

## 1. Android và Console

1. Owner chọn Firebase Project thật, đăng ký Android applicationId `com.giathinh.uthconnect`.
2. Authentication → Sign-in method: bật Email/Password và Google; chọn email hỗ trợ.
3. Authentication → Settings → User actions: bật Email Enumeration Protection nếu chưa bật.
   Console có thể thay đổi bố cục; kiểm tra trạng thái trong phần bảo vệ người dùng/Identity Platform.
   Reset password trong app luôn báo chung; không dùng fetchSignInMethodsForEmail để dò tài khoản.
4. Authentication → Templates: đặt ngôn ngữ tiếng Việt, tên ứng dụng, người gửi và nội dung
   email verification/password reset; kiểm tra liên kết trên thiết bị thật và địa chỉ hỗ trợ.
   Client hiện không override languageCode; owner cần kiểm tra ngôn ngữ template thực tế khi nghiệm thu.
5. Chạy `./gradlew.bat :app:signingReport` trên **từng máy dev** để lấy SHA-1 và SHA-256 debug.
   Release lấy fingerprints từ keystore do owner quản lý; nếu Google Play App Signing được bật,
   thêm cả fingerprints của app-signing certificate trên Play Console. Không tạo/đổi keystore để vượt lỗi.
6. Firebase Project settings → Android app → thêm SHA-1/SHA-256, tải lại
   `google-services.json` sau khi bật Google provider. Đặt đúng `app/google-services.json` (đang ignore).
   Không commit JSON, service-account key, keystore hoặc token. File JSON chứa cấu hình client,
   không thay thế Security Rules hay quyền backend.
7. Google Services plugin tự tạo `default_web_client_id`: phải là **Web OAuth client ID** của dự án,
   không phải Android client ID. Thiếu resource này app báo lỗi cấu hình, không dùng client ID giả.
8. Kiểm tra OAuth consent screen, authorized domains, tổ chức Google Workspace của trường và
   tài khoản thử nghiệm nếu consent screen đang testing. Credential Manager nhận ID token có nonce;
   Firebase SDK xác thực credential, client kiểm tra domain để UX, callable/rules quyết định truy cập.
9. Thử email/password và Google với tài khoản @ut.edu.vn được owner cho phép. Xác nhận gửi mail,
   xác minh, token claims, logout, khởi động lại, mất mạng, đổi mật khẩu và account collision.
   App **không tự link credential theo email**. Khi collision, dùng phương thức đăng nhập cũ;
   thêm liên kết sau này phải có reauthentication hợp lệ và sự đồng ý rõ ràng.

## 2. Backend do owner triển khai sau phê duyệt

- Runtime Node 22, Functions v2, region `us-central1`; client gọi đúng region mặc định.
- Cần quyền Firebase/GCP phù hợp và thông thường Blaze để deploy Cloud Functions.
- Dùng Application Default Credentials/service identity của môi trường deploy, không thêm JSON key vào repo.
- `functions/index.cjs` chỉ export `verifyUthAccount`. Payload phải là `{}`;
  UID/email/emailVerified đọc bằng Admin SDK, không nhận role/UID/studentId từ client.
- Function tạo `users/{uid}` lần đầu bằng transaction, không ghi đè hồ sơ đã có; MSSV để null.
  Backend học vụ cần cung cấp nguồn MSSV/lớp/ngành/khoa đáng tin cậy. Xác thực email UTH
  không đồng nghĩa đã xác thực MSSV hoặc đang là sinh viên.
- Claims thiếu role được cấp STUDENT; ORGANIZER/ADMIN hiện có được giữ, claim khác được giữ.
  Role lạ yêu cầu admin xử lý. Không cung cấp endpoint nâng quyền từ client.
- Admin Auth **không có compare-and-swap** cho custom claims. Mọi writer đặc quyền phải dùng
  cùng quy trình single-writer/khóa theo UID; trước khi deploy cần kiểm tra hệ thống Admin hiện có.
  Hai callable xác minh đồng thời đã được kiểm thử idempotency; điều này không đảm bảo an toàn
  với một Admin writer khác không tuân thủ quy trình. Không đồng thời cấp/thu hồi role ngoài quy trình.
- App Check chưa enforce để tránh khóa ứng dụng chưa cấu hình. Owner cấu hình provider trước,
  theo dõi metrics rồi phê duyệt bật enforcement; App Check không thay thế xác thực/phân quyền.
- Review tác động `firestore.rules` với owners module, seed claims hợp lệ cho tài khoản cũ,
  kiểm thử staging trước production. Không dùng `users.role` làm quyền nữa.
- File `firebase.json` không chọn project production, không có `.firebaserc`/credentials giả.
  Chỉ owner đã được phê duyệt mới chạy lệnh deploy với project thật và phạm vi functions/rules cụ thể.
  Phiên làm việc này không chạy lệnh deploy.

## 3. Identity Platform và giới hạn chặn đăng ký

`functions/src/optional-blocking.cjs` chuẩn bị handler `beforeUserCreated`, **chưa export/deploy**.
Cần nâng Firebase Authentication lên Identity Platform, kiểm tra billing/quota và hỗ trợ blocking functions
của dự án trước khi owner quyết định bật. Chặn domain ở bước tạo tài khoản cần cấu hình đó.
Hiện tại app từ chối email ngoài trường; callable không cấp UTH claims và Rules chặn dữ liệu UTH.
Không tuyên bố đã chặn mọi tài khoản ngoài domain được tạo trực tiếp qua Auth REST API.
Không đòi emailVerified tại beforeCreate vì tài khoản Email/Password chưa thể xác minh trước khi tạo.

## 4. Emulator và kiểm thử local

Cần Node 22, Java 21+ và dependencies backend. Trong `functions/`:

```powershell
npm ci
npm run emulators:test
npm audit --omit=dev
```

Script dùng project `demo-uth-auth3`, chỉ localhost Auth:9099, Firestore:8080, Functions:5001.
Test kiểm tra biến host localhost trước khi dùng Admin SDK; không chạy với emulator host từ máy ngoài.
Test tự tạo dữ liệu fixture/email/password **chỉ trong Emulator**, không tạo tài khoản production.
Không chạy song song nhiều bộ test dùng cùng cổng/project; chưa import/export dữ liệu thật.
Các test dùng Auth Emulator phát token test, gọi callable thật qua HTTP và kiểm tra Rules trong Firestore Emulator.
`baseline.security.test.cjs` là phép tái hiện lỗ hổng ở rules lịch sử commit 5efb1f3,
chạy riêng bằng Firestore Emulator và project demo: không phải tiêu chí chấp nhận rules mới.

Android vẫn dùng Gradle Wrapper hiện có:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

Android UI test dùng fake nằm trong test source set, kiểm tra gate và tương tác UI.
Đây không phải nghiệm thu Google OAuth thật, khả năng giao email, hay Play Integrity.
SDK adapter Android/Firestore REST chưa được nghiệm thu kết nối Firebase production vì thiếu cấu hình.
Không tự chuyển endpoint app sang Emulator hoặc tạo google-services giả. Để kiểm thử app end-to-end
với Emulator, owner cần cung cấp cấu hình Firebase test hợp lệ và một debug-only wiring được review.

## 5. Cache, phiên và vận hành

- Profile dùng Firestore REST authenticated theo UID với updateMask ba trường liên hệ,
  không ghi cache hồ sơ xuống đĩa. Token chỉ dùng làm bearer header, không log/persist riêng.
- Logout xóa state/form Profile, Firebase session và preferences grant riêng Auth; yêu cầu
  Credential Manager xóa trạng thái chọn credential. Không xóa cache/dữ liệu module khác.
- Cache Firestore SDK từ phiên bản cũ hoặc module khác không bị purge toàn cục: owner cần
  audit dữ liệu nhạy cảm cũ và lập migration có phạm vi nếu đã từng đưa bản cũ cho người dùng thật.
- Khi network/token lỗi, client đóng quyền Main và cho retry; không đăng nhập bằng MSSV lưu máy.
  Biometric chỉ mở lại phiên Firebase hợp lệ, không cấp claims, không bỏ qua Firebase.
- ID token Firebase có thời hạn; Rules tin token đã ký. Disable/delete/thu hồi role trên server
  không đảm bảo token cũ mất hiệu lực tức thì ở mọi module trước khi refresh/hết hạn.
  Callable đọc lại Admin record; app reload/force-refresh khi foreground/unlock. Nếu cần thu hồi
  tức thời toàn hệ thống, backend owner phải bổ sung chính sách revocation được review riêng.
- Resend cooldown 60 giây ở ViewModel; giữ qua configuration change. Tạo ViewModel/khởi động lại
  sẽ chờ lại 60 giây (không bỏ qua cooldown), Firebase còn áp dụng rate limiting phía dịch vụ.

## Tài liệu chính thức

- [Google Sign-In Android/Firebase](https://firebase.google.com/docs/auth/android/google-signin)
- [Custom claims](https://firebase.google.com/docs/auth/admin/custom-claims)
- [Callable Functions](https://firebase.google.com/docs/functions/callable)
- [Blocking functions/Identity Platform](https://firebase.google.com/docs/auth/extend-with-blocking-functions)
- [Authentication Emulator](https://firebase.google.com/docs/emulator-suite/connect_auth)
- [Chống dò tài khoản](https://cloud.google.com/identity-platform/docs/admin/email-enumeration-protection)


## Bổ sung sau kiểm định cuối 03/11/2026

- Thu hồi quyền UTH bền vững: backend đặt `uthVerified=false`, giữ các claims hợp lệ khác;
  callable từ chối cấp lại và client không tự gọi provisioning để vượt cờ này. Backend có thẩm quyền
  mới được đặt lại true sau quy trình duyệt. **Xóa claim không tương đương thu hồi bền vững**:
  tài khoản email UTH đã verified nhưng thiếu claim vẫn thuộc luồng onboarding được phép xác minh.
  Admin phải thống nhất cách thu hồi này, ngoài revokeRefreshTokens/disable theo nhu cầu.
- Trước cấp/thu hồi claims, dùng single-writer theo UID; kiểm tra Admin SDK record mới nhất
  không loại bỏ hoàn toàn race với writer bên ngoài không cùng giao thức.
- Password policy: client áp dụng 8–64 ký tự, hoa/thường/số/ký tự đặc biệt, không whitespace cho signup.
  Owner cần cấu hình Firebase Authentication Password Policy tương ứng và kiểm thử tài khoản cũ;
  validation client không ngăn được việc gọi trực tiếp Auth REST API với chính sách password mặc định.
- Console không chặn mọi account ngoài trường khi chưa có Identity Platform blocking function;
  phải nghiệm thu provider Google, Enumeration Protection, mail và Rules trên staging trước production.
- Audit tooling chi tiết và kết quả kiểm định mới nhất xem phần cuối `AUTH_PROFILE_STATUS.md`.

## Cập nhật hoàn thiện phần còn thiếu

Đã có khóa atomic `_authClaimLocks/{uid}` và helper `withClaimsLock` cho các writer hợp tác;
callable đọc Admin record bên trong khóa. Không tự chiếm khóa cũ khi kết quả ghi không rõ.
Owner vẫn phải đưa mọi writer khác vào cùng giao thức và review IAM; cơ chế không chặn được
Admin writer bỏ qua khóa. Rules registrations hiện chỉ cho owner hoặc staff verified đọc.

Firebase CLI kiểm tra hiện có 0 tài khoản đăng nhập; không có project staging được chọn hoặc JSON Android.
Các bước Console, deploy staging sau phê duyệt, đối soát khóa, phiếu test thật, MSSV/cache,
nội dung PR và hướng dẫn GIF được chuẩn bị tại [AUTH3_STAGING_ACCEPTANCE.md](AUTH3_STAGING_ACCEPTANCE.md).
Các ghi chú lịch sử single-writer chưa triển khai ở phần trước được thay bằng trạng thái mới này.
