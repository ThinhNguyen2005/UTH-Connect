# Phiên 2026-11-05 — Báo cáo ngày Nhóm 2

## 1. Bối cảnh và thay đổi
Tạo báo cáo công việc nhóm UTH Connect cho 04/11 và 05/11, mỗi ngày một tệp LaTeX và một PDF, đủ sáu thành viên; đối chiếu Jira KAN-75,79,80,81,82,83,84, git log và báo cáo DOCX được cung cấp.

## 2. Kết quả và Git diff
Tạo docs/reports/Bao_cao_nhom_2_2026-11-04.{tex,pdf}, docs/reports/Bao_cao_nhom_2_2026-11-05.{tex,pdf} và script dựng PDF. PDF mỗi tệp 1 trang, xác minh bằng pypdfium2; LaTeX có 6 phần thành viên và end document. Không sửa mã Kotlin. `git status --short` có thư mục mới docs/reports/, `git diff --stat` không có diff tracked.

## 3. Bài học
Tránh coi trạng thái Jira Done là bằng chứng có commit trong cùng ngày; phân biệt yêu cầu Firebase KAN-75 với thay đổi Supabase Auth trong Git.

## 4. Vấn đề còn lại
Không có XeLaTeX và Word COM bị chặn; PDF được dựng trực tiếp bằng Pillow, nội dung không chọn được như văn bản. Tệp .doc audit chưa đọc được bằng công cụ hiện có; vài thành viên thiếu commit đúng ngày.

## 5. Kế hoạch tiếp theo
Khi có trình biên dịch XeLaTeX, xuất lại PDF trực tiếp từ .tex; cập nhật minh chứng commit / nghiệm thu còn thiếu theo báo cáo các thành viên.
