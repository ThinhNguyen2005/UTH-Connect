# KAN-117 — Chuẩn bị kiểm thử Android

Trạng thái: chưa thực hiện kiểm thử thủ công. Không có thiết bị hoặc emulator kết nối khi kiểm tra bằng `adb devices -l` ngày 26/11/2026.

## Điều kiện đầu vào

- [Android release APK](https://github.com/ThinhNguyen2005/UTH-Connect/actions/runs/45673302459) trên `main` có trạng thái thành công. Đã tải artifact `uth-connect-release-apk` (ID 15051426976), giải nén `app-release-signed.apk` và đọc metadata bằng Android `aapt`: package `com.giathinh.uthconnect`, versionName `1.0`, versionCode `1`, min SDK 31, target SDK 36. SHA-256 của APK: `ef40063796d2bdfbdde66fce332fba341b8521f1615309c557a2b4ec49bb7ed1`. Đây là kiểm tra artifact, chưa cài hoặc kiểm thử ứng dụng.
- [Android CI](https://github.com/ThinhNguyen2005/UTH-Connect/actions/runs/45673302277) trên `main`, commit `5fabecdc97a27b3e71f4e3adc06fe0fb394636aa`, thất bại ở Build Debug APK; Gradle không phân giải được plugin `org.jetbrains.kotlin.android` phiên bản `2.4.21`. Bước unit test bị bỏ qua. Do đó điều kiện CI xanh của task chưa đạt.
- [KAN-115](https://nguyenthinhk52005.atlassian.net/browse/KAN-115) yêu cầu bốn nhóm tài khoản: Sinh viên, Giảng viên, Tổ chức và Admin. Mã `origin/main` tại `5fabecdc97a27b3e71f4e3adc06fe0fb394636aa` khai báo `UserRole`: STUDENT, ORGANIZER, ADMIN. Cần xác nhận cách phân biệt tài khoản Giảng viên trong môi trường thử; không giả định đây là một giá trị enum riêng.

## Ma trận thực hiện

Với từng role được xác nhận, dùng đúng APK release và ghi phiên bản, thiết bị/API, tài khoản thử không chứa mật khẩu, thời gian, bước thực hiện, kết quả thực tế và ảnh hoặc link bằng chứng. Không ghi Pass khi chưa so với yêu cầu được duyệt.

| Mã | Ca kiểm thử | Sinh viên | Giảng viên | Tổ chức | Admin |
| --- | --- | --- | --- | --- | --- |
| TKB-01 | Mở lịch, chọn ngày/tuần, xem card và chi tiết môn/lịch thi | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| TKB-02 | Kiểm tra thông tin môn, chuyển ngày, trạng thái trống và lỗi | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| SK-01 | Xem danh sách, lọc campus, badge và chi tiết sự kiện | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| SK-02 | Đăng ký bằng studentId; đối chiếu quyền theo role | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| SK-03 | Kiểm tra khóa đăng ký khi hết chỗ hoặc trạng thái khác OPEN | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| CS-01 | Đăng bán/trao đổi và tải ảnh với dữ liệu thử | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| CS-02 | Tìm kiếm, lọc, xem chi tiết và liên hệ người bán | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| CS-03 | Sửa, xóa, đổi trạng thái bài của mình; xác minh tài khoản khác bị chặn | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |
| CS-04 | Mở lại app, kiểm tra bài còn; kiểm tra loading, rỗng và lỗi mạng | Chưa chạy | Chưa chạy | Chưa chạy | Chưa chạy |

Với mỗi ô, ghi riêng kết quả thực tế, Pass/Fail/Blocked hoặc N/A có lý do và liên kết bằng chứng. Các thao tác xóa trong ca CS-03 chỉ thực hiện trên dữ liệu thử được phép, sau khi xác nhận phạm vi dữ liệu; không xóa tệp trên máy.

Đăng nhập chỉ là điều kiện vào ứng dụng theo issue. Kiểm thử cần dùng dữ liệu và tài khoản thử được phép; các thao tác tạo hoặc sửa dữ liệu cần môi trường thử được xác nhận.
