# TIMETABLE-1 — 31/10/2026

Nhánh feature/timetable-nghia, tách từ origin/develop (d20fcf9).
Nền tảng CORE có sẵn: Hilt, theme/tokens, strings song ngữ và ScheduleDestination @Serializable.

## Luồng nghiệm thu

1. Mở tab Thời khóa biểu từ thanh điều hướng chính.
2. Hai lớp ngày mai xuất hiện theo giờ tăng dần. Tổng fixture có 5 buổi.
3. 4 deadline mẫu: một quá hạn, hai trong 48 giờ và một sau 5 ngày.
4. Thêm deadline: chọn môn, nhập tên, ngày yyyy-MM-dd và giờ HH:mm.
5. Tên trống/ngày giờ sai giữ form mở và hiện lỗi.
6. Đánh dấu hoàn tất: mục biến mất khỏi danh sách chờ, bộ đếm tăng.
7. Chuyển tab vẫn giữ dữ liệu trong tiến trình; khởi động tiến trình mới trở về mock.

Ngày mock dựa trên Clock múi giờ Asia/Ho_Chi_Minh lúc tạo repository.
Deadline cấp bách: chưa hoàn tất và hạn <= hiện tại + 48 giờ, bao gồm quá hạn.
Trạng thái thời gian được làm mới mỗi phút. Không Room, network hoặc nhắc lịch trong task này.
Tên môn/tên bài mẫu được dịch tại UI; tên deadline người dùng nhập là nội dung động.

## Kiểm tra

~~~powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
~~~

PR nhắm develop và cần người khác review trước merge; chủ repo quyết định merge.
Các mốc 12:00/13:30/15:00/16:00/16:30/17:00–18:00 là mốc của nhóm,
không được coi là đã check-in/review/merge chỉ vì hoàn thành code.

Kết quả trên máy ngày 31/10/2026: assembleDebug thành công; 18 unit tests đạt, gồm 8 tests TIMETABLE-1. Chưa kiểm thử thao tác trên thiết bị.
