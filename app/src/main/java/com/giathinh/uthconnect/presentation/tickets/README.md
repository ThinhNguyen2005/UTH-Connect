# Tickets

- Luồng: AUTH-1 profile `studentId` → `GetTicketsUseCase` → vé mock; chọn vé phát one-shot event tới `TicketQrDestination`.
- Màn hình: danh sách vé và QR động demo hoặc QR có thể quét từ backend Supabase.
- Supabase dùng QR ký ổn định theo vé đã đăng ký; QR hiển thị trước giờ check-in, backend chỉ ghi attendance trong cửa sổ sự kiện và từ chối quét trùng.
- Thay đổi backend cần áp dụng migration `20261104000000_stable_ticket_qr.sql` sau các migration trước.
