# Hướng dẫn hoàn tất nghiệm thu Observability

## 1. Cấu hình GitHub Actions

Quản trị viên repository mở **Settings → Secrets and variables → Actions** và cấu hình:

- **Variable:** `FIREBASE_PROJECT_ID` đúng với `project_info.project_id` của Firebase dành cho môi trường cần build.
- **Secret:** `GOOGLE_SERVICES_JSON` chứa toàn bộ nội dung file cấu hình Android tương ứng, có package `com.giathinh.uthconnect`.
- **Secrets Supabase:** `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`; nếu dùng Google Sign-In, thêm `GOOGLE_WEB_CLIENT_ID`. Chỉ dùng publishable key, không dùng service role key trong APK.
- **Secrets ký APK:** `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, theo keystore phát hành của dự án.

Không gửi các giá trị bí mật qua chat hoặc commit chúng vào Git. Sau khi cấu hình, chạy lại [GitHub Actions run #5](https://github.com/ThinhNguyen2005/UTH-Connect/actions/runs/42579449766), hoặc báo người thực hiện để chạy lại. Kiểm tra đủ bước inject, build, ký, verify và artifact. Đây là nhánh test riêng, không phải kết quả của pipeline KAN-101 đã merge.

## 2. Kiểm tra Remote Config

Sử dụng Firebase project và thiết bị thử nghiệm được cấp quyền. Thiết lập hai tham số `force_update_min_version` và `maintenance_mode`, xuất bản một thay đổi có kiểm soát, sau đó mở lại app hoặc đưa app về foreground để gọi `fetchAndActivate`. Xác nhận giá trị có nguồn `remote` và giao diện thay đổi đúng; sau thử nghiệm, khôi phục cấu hình mong muốn.

Không bật bảo trì hoặc tăng phiên bản tối thiểu cho toàn bộ người dùng thật chỉ để kiểm thử. Nếu dùng project hiện hành, cần nhắm mục tiêu thiết bị hoặc nhóm thử nghiệm được phê duyệt.

## 3. Kiểm tra Crashlytics

Cần một tài khoản Supabase thử nghiệm được cấp quyền và quyền xem Firebase Crashlytics Console. Đăng nhập trực tiếp trên thiết bị, tạo sự kiện lỗi thử nghiệm có kiểm soát trong môi trường thử nghiệm, sau đó đối chiếu UID Supabase và custom key `user_role` trên Console. Không gửi mật khẩu hoặc token qua chat.

Nhánh hiện tại chưa lưu phiên Supabase qua lần đóng ứng dụng; vì vậy cần thiết kế lưu và khôi phục phiên an toàn trước khi kiểm tra trường hợp khôi phục phiên sau cold start.
