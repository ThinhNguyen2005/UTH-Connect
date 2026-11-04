# Kế hoạch hoàn thiện điểm danh sự kiện

> Cập nhật: 03/11/2026. Phạm vi: lưu điểm danh lâu dài trên Supabase, danh sách theo dõi trong ứng dụng cho ban tổ chức và xuất báo cáo CSV.

**Tiến độ trên `feature/checking-duc-v2`:** đã thêm danh sách điểm danh trên Android cho organizer/admin, tìm/lọc, thống kê, tải lại và xuất CSV; dữ liệu đọc từ Supabase. Đã chuẩn bị migration chặn xóa dây chuyền lịch sử điểm danh, nhưng chưa áp dụng/kiểm tra trên dự án Supabase staging thật, chưa thử thiết bị Android hoặc chốt chính sách lưu trữ và sao lưu. Web admin và đồng bộ Google Sheets tự động chưa nằm trong bản này.

**Kiểm tra cục bộ:** `:app:assembleDebug` và `:app:testDebugUnitTest` thành công (98 tests, 0 failures). Chưa có thiết bị trong `adb devices`; chưa có URL/publishable key staging trong cấu hình build tại chỗ.

## 1. Mục tiêu và nguyên tắc

- Mỗi lượt quét hợp lệ tạo đúng một bản ghi `attendance` cho một cặp `(event_id, user_id)` trên Supabase. Chỉ báo thành công sau khi RPC `check_in` trả về thành công.
- Người tổ chức xem danh sách người đăng ký và trạng thái điểm danh của **sự kiện mình quản lý** ngay trong ứng dụng. Admin xem theo quyền quản trị hiện có; sinh viên chỉ xem trạng thái của mình.
- Supabase là nguồn dữ liệu gốc. CSV là ảnh chụp báo cáo tại thời điểm xuất để mở bằng Excel hoặc Google Sheets. Không ghi trực tiếp từng lượt quét vào Google Sheets.
- Trạng thái **Chưa điểm danh** được suy ra từ đăng ký `REGISTERED` chưa có bản ghi `attendance`; không tự ghi `ABSENT` khi sự kiện chưa kết thúc.
- Mất mạng hoặc RPC thất bại: hiển thị lỗi, cho quét lại; không ghi nhận thành công cục bộ. QR hết hạn cần sinh mã mới.

## 2. Hiện trạng cần giữ đúng khi triển khai

- `public.registrations` lưu người đăng ký; `public.attendance` lưu `event_id`, `user_id`, `checked_in_by`, `status`, `checked_in_at`. Khóa chính `(event_id, user_id)` chặn bản ghi trùng.
- RPC `issue_checkin_token` cấp token 60 giây; `check_in` xác minh token, quyền tổ chức, đăng ký và khung giờ rồi ghi attendance.
- RLS hiện cho sinh viên đọc hàng của mình, organizer đọc hàng thuộc sự kiện mình quản lý, admin đọc theo quyền quản trị.
- `SupabaseTicketRepository` hiện đọc trạng thái vé của sinh viên và gọi RPC điểm danh. **Chưa có màn danh sách điểm danh theo sự kiện, thống kê và xuất CSV.**
- `supabase/README.md` ghi Supabase là backend staging tùy chọn. Cần kiểm tra migration và cấu hình trên dự án thực tế trước khi tuyên bố đã vận hành điểm danh thật.

## 3. Luồng sản phẩm

1. Sinh viên đã đăng ký mở vé QR; app lấy token 60 giây từ Supabase.
2. Organizer mở sự kiện mình quản lý, vào máy quét và quét QR.
3. App gọi `check_in(token)`; server ghi bản ghi attendance hoặc trả lỗi cụ thể. Scanner hiển thị kết quả nhận từ server.
4. Màn **Danh sách điểm danh** của sự kiện tải người đăng ký và attendance từ Supabase, ghép bằng `(event_id, user_id)`, hiển thị số đã điểm danh / tổng đăng ký. Có tìm theo tên, MSSV và lọc đã/chưa điểm danh.
5. Organizer làm mới danh sách sau khi quét hoặc kéo để làm mới. Có thể thêm Supabase Realtime sau khi luồng tải lại đã ổn định; khi kết nối Realtime gián đoạn vẫn tải lại được.
6. Organizer xuất CSV từ cùng tập dữ liệu đang được truy vấn, với thời điểm xuất rõ ràng. File mở được bằng Excel và Google Sheets.

## 4. Các chặng triển khai

### A. Xác nhận backend điểm danh thật

- Kiểm tra trạng thái migration trên Supabase staging và cấu hình build `useMockData=false`, `useSupabase=true`; không dùng khóa bí mật trong Android.
- Dùng hai tài khoản sinh viên/organizer và hai thiết bị: đăng ký, lấy QR, quét, xác nhận đúng một hàng trong `attendance`; mở lại app để xác nhận dữ liệu còn nguyên.
- Kiểm tra QR hết hạn, quét trùng, tài khoản khác tổ chức, sinh viên chưa đăng ký và mất mạng. Kết quả UI phải khớp lỗi server.
- Chỉ chuyển môi trường dùng thật sau khi RLS/RPC, camera trên thiết bị và xác thực tài khoản đã qua kiểm thử theo `supabase/README.md`.

### B. Danh sách điểm danh trong ứng dụng

- Thêm hợp đồng domain cho hàng danh sách: `eventId`, `userId`, tên, MSSV, trạng thái đăng ký, thời gian điểm danh, người điểm danh. Không đưa kiểu JSON/Supabase vào domain.
- Repository truy vấn theo `event_id` chỉ cho organizer sở hữu sự kiện hoặc admin; dùng RLS làm lớp bảo vệ ở server. Phân trang nếu sự kiện lớn, tránh tải từng hồ sơ bằng nhiều request riêng.
- Thêm `GetEventAttendanceUseCase` và màn danh sách theo kiến trúc Screen/Route/ViewModel/UiState/Action của dự án. Route lấy `hiltViewModel()`, Screen stateless; text có `values` và `values-en`, dùng design tokens, route `@Serializable`.
- Hiển thị loading, empty, lỗi và thử lại; thống kê `đã điểm danh / đăng ký hợp lệ / chưa điểm danh`. Bản ghi `CANCELLED` không được tính là người chờ điểm danh.
- Tìm kiếm và lọc trên tập dữ liệu đã tải; với sự kiện lớn, chuyển sang truy vấn có phân trang và lọc phía server. Làm mới sau lượt quét thành công và khi quay lại màn hình.

### C. Xuất CSV cho ban tổ chức

- Nút **Xuất danh sách** tạo CSV UTF-8 gồm tên sự kiện, thời điểm xuất, MSSV, họ tên, email (nếu được phép), trạng thái đăng ký, trạng thái điểm danh, giờ vào và người điểm danh.
- Xuất toàn bộ danh sách đúng theo quyền của organizer, không chỉ trang đang hiển thị. Ghi rõ múi giờ `Asia/Ho_Chi_Minh` và định dạng ngày giờ; dùng cách mã hóa CSV an toàn cho dấu phẩy, xuống dòng và giá trị bắt đầu bằng ký tự công thức bảng tính.
- Dùng chia sẻ/lưu tệp của Android; không gửi dữ liệu sinh viên tới Google Sheets tự động. Kiểm tra file bằng Excel hoặc Google Sheets với tiếng Việt có dấu.

### D. Lưu trữ lâu dài và đối soát

- Hiện khóa ngoại từ `attendance` đến `events` và `profiles` dùng `ON DELETE CASCADE`. Trước khi cho xóa sự kiện/tài khoản trong môi trường thật, chốt chính sách lưu trữ: ưu tiên lưu trữ mềm hoặc chặn xóa khi đã có attendance để không mất lịch sử.
- Nếu cần sửa điểm danh thủ công, tạo RPC được kiểm tra quyền và bảng nhật ký riêng ghi trạng thái trước/sau, người sửa, thời điểm, lý do. Không cho client cập nhật `attendance` trực tiếp.
- Xác định thời gian lưu dữ liệu, quyền tải báo cáo và quy trình sao lưu/khôi phục cùng nhóm phụ trách Supabase.
- Đối soát định kỳ: tổng số bản ghi attendance theo sự kiện phải khớp thống kê trong app và số hàng đã điểm danh trong CSV.

## 5. Kiểm thử và tiêu chí hoàn thành

- Unit tests: ghép đăng ký/attendance, loại `CANCELLED` khỏi mẫu số, thống kê, tìm/lọc, loading/empty/error và định dạng CSV.
- Staging tests: organizer chỉ thấy sự kiện của mình; sinh viên không đọc danh sách; admin theo quyền hiện có; hai lượt quét đồng thời chỉ tạo một bản ghi; dữ liệu vẫn còn sau khi khởi động lại app.
- Kiểm tra CSV với tên tiếng Việt, dấu phẩy, xuống dòng và nội dung có thể bị hiểu là công thức; số hàng và trạng thái khớp danh sách server tại thời điểm xuất.
- Chạy `:app:assembleDebug` và `:app:testDebugUnitTest` theo lệnh chuẩn của nhóm; kiểm thử camera trên thiết bị thật.

**Hoàn thành khi:** organizer quét QR hợp lệ, thấy sinh viên chuyển sang **Đã điểm danh** trong danh sách sự kiện trên thiết bị khác và xuất được CSV tương ứng; quét trùng không tạo thêm hàng; quyền truy cập và dữ liệu sau khởi động lại được xác nhận trên Supabase staging.

## 6. Phạm vi tiếp theo

- Supabase Realtime để danh sách tự cập nhật là cải tiến sau khi truy vấn và tải lại ổn định.
- Đồng bộ Google Sheets tự động chỉ bổ sung khi ban tổ chức cần một Sheet luôn cập nhật; khi đó đồng bộ một chiều từ Supabase, có thử lại và đối soát. Sheet không thay thế bản ghi attendance.
- Cập nhật `README.md`, `docs/Design.md` và `docs/DATABASE_SCHEMA.md` khi Supabase được chốt làm backend chính; hiện các tài liệu đó còn mô tả Firebase/Firestore.
