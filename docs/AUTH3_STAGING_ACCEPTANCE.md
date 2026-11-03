# Auth3-1: triển khai staging và nghiệm thu thật

Chưa thực hiện deploy hoặc nghiệm thu staging. Tài liệu này là quy trình cho owner có quyền.
Không tạo PR hoặc ghi nhận PASS cho bước chưa có bằng chứng.

## 1. Điều kiện mở staging

- Owner cung cấp project staging riêng, tách production; xác nhận project ID, billing,
  Firestore database `(default)`, region Functions `us-central1`, quyền IAM người deploy.
- Hiện Firebase CLI có 0 tài khoản đăng nhập; chưa có `.firebaserc` hoặc `app/google-services.json`.
  Không thể xác nhận dự án, Blaze hay Identity Platform từ mã nguồn Android.
- Console: đăng ký applicationId `com.giathinh.uthconnect`, bật Email/Password và Google,
  Email Enumeration Protection, Password Policy, kiểm tra template verification/reset tiếng Việt.
- Mỗi developer thêm SHA-1/SHA-256 debug từ `gradlew.bat :app:signingReport`;
  release lấy từ keystore thật và Play App Signing nếu có. Không tạo keystore mới để vượt lỗi.
- Tải JSON staging mới sau khi bật Google, đặt tại `app/google-services.json` (ignored),
  xác nhận resource `default_web_client_id` là Web OAuth client ID cùng project.
- Chỉ dùng tài khoản kiểm thử được trường/owner cho phép. Không dùng dữ liệu sinh viên thật
  làm fixture. Không lưu token, mật khẩu, thư reset/verification hoặc JSON credentials trong bằng chứng.

## 2. Claims và khóa writer

`functions/src/claims-writer.cjs` cung cấp `withClaimsLock(db, uid, operation)`.
Mọi backend cấp/thu hồi role, uthVerified hoặc claims khác phải gọi helper và đọc Admin record
**bên trong** callback. Không đọc claims trước khi lấy khóa rồi ghi snapshot cũ.

- `_authClaimLocks/{uid}` tạo bằng atomic create, không cho client đọc/ghi theo Rules mặc định deny.
- Writer khác chờ có giới hạn rồi nhận lỗi retry; không có TTL tự xóa hoặc tự chiếm khóa cũ.
- Thành công hoặc lỗi nghiệp vụ HttpsError đã xác định: nhả khóa. Lỗi ghi không xác định/crash:
  giữ khóa, đóng cấp quyền cho UID đó đến khi vận hành đối soát.
- Khôi phục khóa kẹt: tạm dừng mọi writer cho UID, xác nhận không còn request đang chạy,
  kiểm tra Auth record và log/audit backend, đối soát role/uthVerified đúng, sau đó owner mới
  xóa đúng lock bằng Admin SDK. Không xóa lock chỉ vì cũ; không cấp client quyền mở khóa.
- Firebase Auth không có CAS cho custom claims. Helper bảo vệ các writer cùng tuân thủ;
  không ngăn một admin/tool khác gọi trực tiếp setCustomUserClaims. Owner phải kiểm kê
  mọi writer và giới hạn IAM trước rollout. Chưa nghiệm thu điều này trên cloud.
- Thu hồi rõ ràng bằng `uthVerified=false`; xóa claim đơn thuần vẫn cho onboarding lại nếu
  email trường đã verified. Disable/revoke tokens khi cần; token cũ có thể dùng tới refresh/hết hạn.

## 3. Triển khai staging sau phê duyệt

Không chạy các lệnh deploy trước khi owner xác nhận project và cho phép. Giá trị trong dấu
`<...>` là thông tin owner phải cung cấp, không phải credential/project giả.

```powershell
# Sau khi owner đăng nhập bằng tài khoản hợp lệ và kiểm tra project ID:
firebase projects:list
cd functions
npm ci
$env:FUNCTIONS_DISCOVERY_TIMEOUT='60'
npm run emulators:test
cd ..
firebase deploy --project <STAGING_PROJECT_ID> --only functions:auth3:verifyUthAccount
# Sau review Rules và xác nhận tác động module:
firebase deploy --project <STAGING_PROJECT_ID> --only firestore:rules
```

Kiểm tra cú pháp selector codebase bằng CLI hiện có trước thực thi nếu CLI khác phiên bản.
Không deploy toàn project, không chọn project mặc định, không deploy indexes/migration ngoài yêu cầu.
Giữ bản Rules/Functions trước rollout để owner rollback có kiểm soát; không rollback bằng mở quyền công khai.
Review log, IAM và artifact production dependencies; không đưa dev tooling/keys vào runtime.

### Blocking function

Owner kiểm tra Authentication → Identity Platform và Billing trong Console.
Blocking Auth cần Identity Platform; khả năng triển khai Functions/billing cần được owner xác nhận.
`functions/src/optional-blocking.cjs` đã có beforeUserCreated domain-only nhưng **chưa export**.
Chỉ sau khi được duyệt mới nối export, kiểm tra trigger trên staging và thử đăng ký ngoài domain
bằng cả app lẫn Auth API. Không yêu cầu emailVerified trước khi tạo Email/Password account.
Trước khi bật blocking, phương án hiện tại là từ chối cấp UTH claims và chặn dữ liệu bằng Rules;
điều này không chặn mọi tài khoản ngoài trường được tạo trực tiếp trong Firebase Auth.

## 4. Phiếu kiểm thử thật (tất cả hiện BLOCKED)

Ghi cho mỗi ca: ngày, project ID, commit/worktree được test, thiết bị/API, thao tác,
kết quả mong đợi/thực tế và liên kết bằng chứng đã che thông tin nhạy cảm.

| Ca | Tiêu chí đạt |
|---|---|
| Email signup/login | @ut.edu.vn thật tạo được account; không vào Main trước verified + claims |
| Domain | Gmail, hậu tố ut.edu.vn.evil.com và sub.ut.edu.vn không có quyền UTH |
| Email verification | Thư tiếng Việt tới inbox; link có hiệu lực; reload rồi refresh token/claims |
| Resend | Không gửi lặp trước 60s; lỗi quota/network đúng, có retry |
| Google | Credential Manager đúng Web client/SHA/nonce; tài khoản UTH được xác minh hợp lệ |
| Claims | Thiếu claim không vào Main khi backend chưa cấp; STUDENT/ORGANIZER/ADMIN từ token |
| Collision | Cùng email khác provider không tự link bởi app; báo dùng phương thức hiện có |
| Link credential | App chưa có luồng link có consent/reauth; không ghi PASS link. Nếu cần bắt buộc, bổ sung luồng được review và test riêng trước nghiệm thu |
| Reset | Thông báo chung, không dò account; mail/reset link thật; mật khẩu mới đăng nhập được |
| Disabled/deleted/network | Không cấp Main; foreground/unlock refresh thất bại đóng phiên, retry đúng |
| Revocation | false không tự cấp lại; writer đồng thời không mất claim; kiểm tra token cũ tới expiry |
| Profile | GET/PATCH theo UID, chỉ sửa contacts; role/metadata/UID khác bị chặn |
| Lifecycle | Nhớ đăng nhập, force-stop/reopen, logout→Back, chuyển background/foreground |
| Biometric | Thiết bị thật: thành công/hủy/thất bại/background không vượt Firebase gate |
| Rules registrations | Sinh viên chỉ đọc vé mình; staff verified đọc roster; student không list vé người khác |
| Regression | Home/Schedule/Utilities, điều hướng và fixtures giữ nguyên |

### MSSV và dữ liệu cũ

Auth3-1 yêu cầu email trường; README/Design còn nêu duyệt MSSV thủ công ở Admin Dashboard.
Không có API học vụ UTH, hợp đồng mapping email→MSSV hoặc quyền truy cập dữ liệu học vụ trong repo.
`studentId` mới vẫn null; không suy luận từ email. Tích hợp sau cần endpoint server, quyền truy cập,
schema UID/MSSV/nguồn xác minh/thời điểm, quy trình sửa sai và kiểm thử chống giả mạo.
Không thêm client tự cập nhật/xác minh studentId.

Profile mới không ghi disk cache. Cache Firestore SDK cũ dùng chung có thể chứa dữ liệu của module khác;
không gọi clearPersistence/xóa toàn bộ thư mục. Owner phải xác định phiên bản đã phát hành, nguồn cache,
UID và chính sách retention trước khi cho phép migration có phạm vi. Đây vẫn PARTIAL/BLOCKED.

## 5. Nội dung PR chuẩn bị (chưa tạo PR)

Giới hạn tích hợp sự kiện cần owner review: repository sự kiện hiện cập nhật `registeredCount`
trong transaction từ client, trong khi Rules chỉ cho staff sửa event. Đây là mâu thuẫn có sẵn,
không giải quyết bằng mở quyền sửa events cho STUDENT. Cần backend đăng ký sự kiện đáng tin cậy
hoặc thiết kế Rules/transaction được review riêng trước nghiệm thu module sự kiện thật.
Policy Organizer sửa mọi events và ghi attendance cũng còn rộng so với README (orgIds/checkIn server);
chưa sửa vì thiếu hợp đồng quyền tổ chức và backend checkIn, không tuyên bố policy toàn bộ module đạt production.

Tiêu đề: `[Auth3-1] Firebase UTH Auth, claims và Profile/session`

Ứng dụng xác thực email trường/Google bằng Firebase, đưa account chưa đủ điều kiện qua VerifyEmail,
chỉ mở Main sau claims hợp lệ. Callable kiểm tra Admin record, giữ quyền backend và dùng khóa theo UID;
Rules bảo vệ hồ sơ, metadata và vé đăng ký sự kiện.

Đính kèm kết quả kiểm thử mới nhất trong AUTH_PROFILE_STATUS.md, inventory file đúng phạm vi,
giới hạn staging/OAuth/biometric/cache/claims writers và kết quả nghiệm thu thật trước mở PR theo yêu cầu.
Không gọi Auth3-1 hoàn tất production khi những hàng kiểm thử thật vẫn BLOCKED.

GIF minh họa sau nghiệm thu: login→Verify→Main→Profile→logout→Back; clip riêng Google/reset nếu cần.
Dùng tài khoản staging được phép, che email/MSSV, bàn phím mật khẩu, token/URL email và thông tin cá nhân.
Hiện chưa ghi GIF vì chưa có môi trường/tài khoản nghiệm thu thật.
