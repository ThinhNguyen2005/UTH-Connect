# Tickets

- Luồng: AUTH-1 profile `studentId` → `GetTicketsUseCase` → vé mock; chọn vé phát one-shot event tới `TicketQrDestination`.
- Màn hình: danh sách vé và QR máy đọc được, chứa ID vé đầy đủ; hình QR đổi sau 60 giây và vẫn có mã dự phòng.
- Scanner ORGANIZER quét QR thì tự check-in, báo trùng khi quét lại; dữ liệu Attendance demo giữ trong bộ nhớ.
- Chưa làm: token có chữ ký, xác minh bằng backend, lưu Attendance lâu dài và đồng bộ giữa các thiết bị.
