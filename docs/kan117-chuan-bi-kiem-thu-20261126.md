# KAN-117 — Chuẩn bị kiểm thử Android

Trạng thái: chưa thực hiện kiểm thử thủ công. Không có thiết bị hoặc emulator kết nối khi kiểm tra bằng `adb devices -l` ngày 26/11/2026.

## Điều kiện đầu vào

- [Android release APK](https://github.com/ThinhNguyen2005/UTH-Connect/actions/runs/45673302459) trên `main` có trạng thái thành công; cần xác minh và tải đúng artifact trước khi cài.
- [Android CI](https://github.com/ThinhNguyen2005/UTH-Connect/actions/runs/45673302277) trên `main`, commit `5fabecdc97a27b3e71f4e3adc06fe0fb394636aa`, thất bại ở Build Debug APK; Gradle không phân giải được plugin `org.jetbrains.kotlin.android` phiên bản `2.4.21`. Bước unit test bị bỏ qua. Do đó điều kiện CI xanh của task chưa đạt.
- Mã `origin/main` tại `5fabecdc97a27b3e71f4e3adc06fe0fb394636aa` khai báo ba giá trị `UserRole`: STUDENT, ORGANIZER, ADMIN. Issue yêu cầu bốn role nhưng chưa xác định role thứ tư; cần xác nhận ma trận vai trò và tài khoản thử trước khi kết luận đủ phạm vi.

## Ma trận thực hiện

Với từng role được xác nhận, dùng đúng APK release và ghi phiên bản, thiết bị/API, tài khoản thử không chứa mật khẩu, thời gian, bước thực hiện, kết quả thực tế và ảnh hoặc link bằng chứng. Không ghi Pass khi chưa so với yêu cầu được duyệt.

| Chức năng | Kiểm tra cơ bản | Kết quả |
| --- | --- | --- |
| Thời khóa biểu (KAN-31) | Mở, xem dữ liệu và chuyển khoảng thời gian; kiểm tra trạng thái rỗng/lỗi nếu có điều kiện | Chưa thực hiện |
| Sự kiện (KAN-44/45) | Mở danh sách và chi tiết; kiểm tra thao tác và quyền tương ứng của role | Chưa thực hiện |
| Chợ sách (KAN-82/84/88) | Mở danh sách và chi tiết; kiểm tra thao tác và quyền tương ứng của role | Chưa thực hiện |

Đăng nhập chỉ là điều kiện vào ứng dụng theo issue. Kiểm thử cần dùng dữ liệu và tài khoản thử được phép; các thao tác tạo hoặc sửa dữ liệu cần môi trường thử được xác nhận.
