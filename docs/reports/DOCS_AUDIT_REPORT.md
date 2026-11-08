# Báo cáo kiểm toán tài liệu UTH Connect

**Ngày:** 08/11/2026  
**Căn cứ:** `README.md`, `.agents/memory/architecture_decisions.md`, và kiểm tra từng tài liệu Markdown dưới `docs/`.  
**Phạm vi an toàn:** Báo cáo chỉ đề xuất; chưa xóa file nào. `docs/plan/` không thuộc danh sách đề xuất và phải được giữ nguyên.

## Đề xuất xóa sau khi được duyệt

Các đề xuất dưới đây tập trung vào tài liệu thiết kế đang mô tả một kiến trúc đã bị thay thế. Trước khi xóa, cần chuyển những quyết định hoặc chi tiết còn giá trị sang tài liệu chuẩn Supabase hiện hành.

| File | Đề xuất | Lý do cụ thể / điều kiện trước khi xóa |
|---|---|---|
| `docs/data/DATABASE_SCHEMA.md` | Giữ và cập nhật theo migrations | Đã thay bản Firestore cũ bằng bản đồ Supabase PostgreSQL/RLS/RPC, kèm chỉ dẫn kiểm chứng migrations. Rà lại khi schema đổi. |
| `docs/Design.md` | Xóa sau khi nội dung nghiệp vụ được chuyển sang tài liệu chuẩn mới | Tài liệu tự nhận là nguồn ưu tiên về nghiệp vụ nhưng mô tả Firebase Auth, Firestore, Cloud Functions, custom claims, Firestore security rules và gói Blaze. Các quyết định backend, check-in và xác thực đã lỗi thời. Trước khi xóa, chuyển các yêu cầu nghiệp vụ/NFR còn đúng vào README hoặc tài liệu thay thế; không xóa chỉ để xử lý mâu thuẫn. |
| `docs/data/DATA_LAYER_GUIDE.md` | Giữ và cập nhật theo code | Đã thay nội dung mock/Firebase lỗi thời bằng cấu hình Gradle, Hilt bindings và Supabase REST hiện hành; tiếp tục cập nhật khi build mode đổi. |
| `docs/process/REPO_TREE.md` | Xóa hoặc lưu trữ sau khi đối chiếu cây hiện tại | Cây và ma trận sở hữu ghi các gói `remote/firestore`, Firestore seeder, Cloud Functions và kế hoạch nhánh/nhân sự theo một cấu trúc cũ. Không còn đáng tin làm bản đồ repository hiện tại. Trước khi xóa, xác nhận không còn quy tắc quyền sở hữu cần giữ. |

Không đề xuất xóa tài liệu khác chỉ vì có ngày cũ, tên công nghệ trong bối cảnh lịch sử, hoặc nội dung cần cập nhật. Các file trên cũng không được xóa trong công việc này.

## Các tài liệu đã rà soát và hướng xử lý

| File | Kết luận |
|---|---|
| `docs/features/ATTENDANCE_PLAN.md` | Giữ; kế hoạch điểm danh Supabase còn hữu ích. Cập nhật trạng thái/migration và bỏ câu cho rằng README/schema chưa chuyển Supabase khi đồng bộ tài liệu. |
| `docs/security/AUTH_CONSTRAINTS.md` | Giữ; có ràng buộc Supabase Auth, xác minh MSSV và phiên in-memory. Đối chiếu chi tiết với code khi thay đổi luồng auth. |
| `docs/CODING_STANDARDS.md` | Giữ; chuẩn code áp dụng rộng, nhưng các đoạn Firestore/Emulator cần đổi sang Supabase khi cập nhật. |
| `docs/data/DATA_LAYER_GUIDE.md` | Giữ; hiện mô tả Supabase Gradle configuration và các giới hạn thực tế của repositories. |
| `docs/data/DATABASE_SCHEMA.md` | Giữ; bản tra cứu đã được đồng bộ với migrations Supabase. |
| `docs/Design-system.md` | Giữ; chuẩn token và component Android, không bị thay bởi quyết định backend. |
| `docs/Design.md` | Đề xuất thay thế/xóa theo bảng trên do mâu thuẫn kiến trúc cốt lõi. |
| `docs/process/GIT_WORKFLOW.md` | Giữ; quy trình Git độc lập với backend. Rà lại tên nhánh/nhân sự nếu còn áp dụng. |
| `docs/security/GOOGLE_SIGNIN_SETUP.md` | Giữ; hướng dẫn cấu hình Google OAuth với Supabase, vẫn phù hợp. Rà soát bí mật và thông tin môi trường trước khi chia sẻ. |
| `docs/process/REPO_TREE.md` | Đề xuất xóa/lưu trữ theo bảng trên vì cây và quyền sở hữu đã lỗi thời. |
| `docs/features/TIMETABLE_1.md` | Giữ như biên bản nghiệm thu lịch sử; không coi như chuẩn hiện hành nếu milestone đã kết thúc. |
| `docs/UI-UX.md` | Giữ; checklist UX/accessibility dùng được, không phụ thuộc backend. |
| `docs/sessions/2026-11-04_auth_minimalist_redesign.md` | Giữ làm lịch sử phiên; không phải tài liệu kiến trúc hiện hành. |
| `docs/sessions/2026-11-04_auth_redesign_and_google_sso_hardening.md` | Giữ làm lịch sử; quyết định auth chuẩn hiện nằm trong `AUTH_CONSTRAINTS.md` và architecture decisions. |
| `docs/sessions/2026-11-04_supabase_auth_security_expansion.md` | Giữ làm lịch sử Supabase. |
| `docs/sessions/2026-11-04_supabase_optimization_and_security.md` | Giữ làm lịch sử Supabase. |
| `docs/sessions/2026-11-05_auth_forgot_password_and_verification_handover.md` | Giữ làm handover lịch sử; thông tin có thể đã cũ. |
| `docs/sessions/2026-11-05_daily_reports_group2.md` | Giữ làm báo cáo lịch sử; không dùng thay spec hiện hành. |
| `docs/sessions/2026-11-05_schedule_colors_and_settings_locale.md` | Giữ làm lịch sử triển khai UI. |
| `docs/sessions/2026-11-05_utilities_hub_and_4tab_nav.md` | Giữ làm lịch sử triển khai; kiểm tra lại trạng thái nếu cần dùng làm hướng dẫn. |
| `docs/sessions/2026-11-07_auth_rbac_and_supabase_security.md` | Giữ làm lịch sử bảo mật/RBAC. |
| `docs/sessions/2026-11-07_web_architecture_and_login_integration.md` | Giữ làm lịch sử; ghi Next.js 14 trong bối cảnh thời điểm tạo, trong khi định hướng hiện hành là Next.js 15. Không xóa chỉ vì số phiên bản cũ. |
| `docs/sessions/2026-11-08_ponytail_codex_plugin.md` | Giữ làm nhật ký cài công cụ, không phải đặc tả sản phẩm. |
| `docs/web/ANTI_AI_DESIGN_SYSTEM.md` | Giữ; là nguồn chuẩn giao diện Web theo architecture decisions. |
| `docs/UI-UX.mc` | Không đánh giá nội dung trong báo cáo Markdown; đang là đường dẫn đã bị xóa trong working tree trước tác vụ. Cần xử lý riêng để tránh ghi đè thay đổi hiện có. |
| `docs/plan/PLAN_OBSERVABILITY_AND_WEB_DEPLOYMENT.md` | Không sửa/không xóa; bảo toàn nguyên trạng theo yêu cầu. |

## Ranh giới đề xuất

- Báo cáo không phê duyệt việc xóa. Chỉ thực hiện xóa sau khi người dùng xem và duyệt danh sách cụ thể.
- Không có lệnh xóa hàng loạt; từng file phải được đọc và xác nhận lại ngay trước thao tác xóa.
- Không sửa hoặc xóa bất cứ file nào trong `docs/plan/`.
