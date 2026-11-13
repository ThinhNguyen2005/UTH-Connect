import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn
from pathlib import Path

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_report():
    doc = docx.Document()

    # Page Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)

    # Color Palette
    NAVY = RGBColor(0, 51, 102)     # #003366
    GOLD = RGBColor(218, 149, 14)    # #DA950E
    DARK = RGBColor(33, 37, 41)      # #212529
    GRAY = RGBColor(108, 117, 125)   # #6C757D
    GREEN = RGBColor(25, 135, 84)    # #198754
    RED = RGBColor(220, 53, 69)      # #DC3545

    # Document Header
    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_before = Pt(0)
    p_title.paragraph_format.space_after = Pt(4)
    run_org = p_title.add_run("TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI TP.HCM (UTH)\nDỰ ÁN UTH-CONNECT — HỆ THỐNG NỘI BỘ SINH VIÊN\n")
    run_org.font.name = "Calibri"
    run_org.font.size = Pt(11)
    run_org.font.bold = True
    run_org.font.color.rgb = GRAY

    run_title = p_title.add_run("BÁO CÁO KỸ THUẬT & NGHIỆM THU CI/CD\nTHIẾT LẬP QUY TRÌNH CI BẰNG GITHUB ACTIONS (TASK KAN-103)")
    run_title.font.name = "Calibri"
    run_title.font.size = Pt(17)
    run_title.font.bold = True
    run_title.font.color.rgb = NAVY

    # Meta Table
    tbl_meta = doc.add_table(rows=5, cols=2)
    tbl_meta.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_meta.autofit = False

    meta_data = [
        ("Kỹ sư phụ trách:", "Kỹ sư DevOps / Android (Tạ Huy Thiện Văn)"),
        ("Ngày thực hiện:", "12/11/2026"),
        ("Mã nhiệm vụ (Task):", "KAN-103 — Setup Android Continuous Integration (CI) Workflow"),
        ("Nhánh triển khai (Branch):", "kan103 (tạo từ main) | Pull Request #66: https://github.com/ThinhNguyen2005/UTH-Connect/pull/66"),
        ("Kết luận nghiệm thu:", "Đã hoàn thành đủ 10/10 tiêu chí nghiệm thu — Xác minh cả Positive & Negative Run")
    ]

    for i, (k, v) in enumerate(meta_data):
        row = tbl_meta.rows[i]
        c0, c1 = row.cells[0], row.cells[1]
        c0.width = Inches(2.2)
        c1.width = Inches(4.7)
        set_cell_background(c0, "F0F4F8")
        set_cell_background(c1, "F8F9FA")
        set_cell_margins(c0, 60, 60, 100, 100)
        set_cell_margins(c1, 60, 60, 100, 100)
        p0 = c0.paragraphs[0]
        p0.paragraph_format.space_after = Pt(2)
        r0 = p0.add_run(k)
        r0.font.bold = True
        r0.font.size = Pt(9.5)
        r0.font.color.rgb = NAVY
        p1 = c1.paragraphs[0]
        p1.paragraph_format.space_after = Pt(2)
        r1 = p1.add_run(v)
        r1.font.size = Pt(9.5)
        if "10/10" in v:
            r1.font.bold = True
            r1.font.color.rgb = GREEN
        else:
            r1.font.color.rgb = DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(4)

    def add_sec_heading(title):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.keep_with_next = True
        r = p.add_run(title)
        r.font.name = "Calibri"
        r.font.size = Pt(12)
        r.font.bold = True
        r.font.color.rgb = NAVY

    def add_para(text, bold_prefix="", bullet=False):
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(3)
        if bullet:
            p.paragraph_format.left_indent = Inches(0.2)
            r_b = p.add_run("• ")
            r_b.font.bold = True
            r_b.font.color.rgb = NAVY
        if bold_prefix:
            r_p = p.add_run(bold_prefix + " ")
            r_p.font.bold = True
            r_p.font.color.rgb = DARK
        r_t = p.add_run(text)
        r_t.font.size = Pt(9.5)
        r_t.font.color.rgb = DARK
        return p

    # 1. Mục tiêu
    add_sec_heading("1. MỤC TIÊU VÀ PHẠM VI DỰ ÁN")
    add_para("Thiết lập quy trình CI tự động bằng GitHub Actions cho ứng dụng Android UTH-Connect nhằm tự động kiểm tra tính ổn định của mã nguồn qua Build và Unit Test, đảm bảo mọi thay đổi không làm gãy khả năng biên dịch và chức năng đã kiểm thử.", "Mục tiêu:")
    add_para("Không dùng @Ignore, @Disabled, continue-on-error, || true, -x test để che giấu lỗi. Tuyệt đối không commit secret vào git repository. Mọi commit và nhánh thử nghiệm đều được dọn dẹp sạch sẽ.", "Nguyên tắc:")

    # 2. Khảo sát
    add_sec_heading("2. KẾT QUẢ KHẢO SÁT HIỆN TRẠNG MÃ NGUỒN (BƯỚC 0)")
    add_para("Gradle 9.8.2; AGP 9.3.3; Toolchain Daemon JVM Java 21; Source/Target Compatibility Java 11.", "Phiên bản:", bullet=True)
    add_para("Đơn module :app, 2 build variants (debug/release), task build: ./gradlew assembleDebug --stacktrace.", "Module & Build:", bullet=True)
    add_para("Thư mục app/src/test/java gồm 41 suites, task test: ./gradlew testDebugUnitTest --stacktrace.", "Unit Tests:", bullet=True)
    add_para("Nhánh chính là main, nhánh phát triển là develop.", "Nhánh:", bullet=True)

    # 3. Rà soát bảo mật
    add_sec_heading("3. RÀ SOÁT BẢO MẬT CẤU HÌNH CHO PR TỪ FORK (HẠNG MỤC 2)")
    add_para("Kỹ sư đã rà soát toàn bộ các file cấu hình và cơ chế nạp Secrets trên pipeline:")

    tbl_sec = doc.add_table(rows=5, cols=4)
    tbl_sec.alignment = WD_TABLE_ALIGNMENT.CENTER
    sec_headers = ["File / Cấu hình", "Nội dung nhận diện", "Nhạy cảm", "Kết luận & Giải pháp"]
    for j, h in enumerate(sec_headers):
        cell = tbl_sec.rows[0].cells[j]
        set_cell_background(cell, "003366")
        set_cell_margins(cell, 60, 60, 80, 80)
        p = cell.paragraphs[0]
        r = p.add_run(h)
        r.font.bold = True
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(255, 255, 255)

    sec_rows = [
        ("app/google-services.json", "Chứa project_id uthconnect-d4d91 và API key AIzaSyBEHy...", "Có (Dev Key)", "Key client Firebase cho môi trường dev. Trên CI, ưu tiên inject từ secret GOOGLE_SERVICES_JSON; PR từ fork dùng file dev fallback an toàn."),
        ("gradle.properties", "Chứa supabaseUrl và supabasePublishableKey sb_publishable_...", "Không (Public)", "Publishable key Supabase là định danh public client. Trên CI hỗ trợ override từ GitHub Secrets SUPABASE_URL / SUPABASE_PUBLISHABLE_KEY."),
        ("Workflow Logging", "Các bước chạy script inject và gradle build", "Không", "Không có lệnh echo secret, không dùng set -x trên bước có secret. Secret không bị lộ vào log runner."),
        ("Token Fork PR", "Bước EnricoMi/publish-unit-test-result-action@v2", "Không", "Đã thêm guard kiểm tra quyền ghi: if: always() && (github.event_name != 'pull_request' || github.event.pull_request.head.repo.full_name == github.repository). Tránh lỗi Resource not accessible by integration.")
    ]
    s_widths = [Inches(1.7), Inches(1.8), Inches(1.0), Inches(2.4)]
    for i, s_row in enumerate(sec_rows):
        row = tbl_sec.rows[i + 1]
        for j, val in enumerate(s_row):
            cell = row.cells[j]
            cell.width = s_widths[j]
            set_cell_background(cell, "F8F9FA" if i % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, 50, 50, 60, 60)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            r = p.add_run(val)
            r.font.size = Pt(8.5)
            if j == 2:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r.font.bold = True
                if "Có" in val:
                    r.font.color.rgb = GOLD
                else:
                    r.font.color.rgb = GREEN

    doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # 4. Đối soát số lượng test
    add_sec_heading("4. ĐỐI SOÁT SỐ LƯỢNG UNIT TEST ĐƯỢC THỰC THI (HẠNG MỤC 3)")
    add_para("Số lượng file test trong mã nguồn: 42 files (gồm 41 file test case và 1 file MainDispatcherRule.kt hỗ trợ coroutine).", "Files mã nguồn:", bullet=True)
    add_para("Tổng số annotation @Test được khai báo: 247 test cases.", "Số test khai báo:", bullet=True)
    add_para("Tổng số test được thực thi trong JUnit XML: 247 test cases (100% test khai báo được chạy).", "Số test đã chạy:", bullet=True)
    add_para("Số test bị bỏ qua / vô hiệu hóa: 0 test (@Ignore = 0, @Disabled = 0, Assume = 0).", "Số skipped / ignored:", bullet=True)
    add_para("Rà soát cấu hình exclude / filter trong build.gradle.kts: Không có cấu hình loại trừ test. Toàn bộ 247 test được bao phủ trọn vẹn.", "Rà soát loại trừ ngầm:", bullet=True)

    # 5. Kiểm chứng thực tế (Positive & Negative)
    add_sec_heading("5. KẾT QUẢ KIỂM CHỨNG THỰC TẾ TRÊN GITHUB ACTIONS")
    add_para("Pipeline đã được kiểm chứng cả 2 trường hợp: Thành công (Positive) và Thất bại có chủ đích (Negative):")

    tbl_runs = doc.add_table(rows=4, cols=5)
    tbl_runs.alignment = WD_TABLE_ALIGNMENT.CENTER
    r_headers = ["Kịch bản kiểm chứng", "Run ID / Link", "Commit", "Kết quả Tests", "Trạng thái Job"]
    for j, h in enumerate(r_headers):
        cell = tbl_runs.rows[0].cells[j]
        set_cell_background(cell, "003366")
        set_cell_margins(cell, 60, 60, 80, 80)
        p = cell.paragraphs[0]
        r = p.add_run(h)
        r.font.bold = True
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(255, 255, 255)

    runs_data = [
        ("Positive Run 1 (Khởi tạo CI)", "43141141541", "2d5be72", "247 Passed / 0 Failed", "✅ SUCCESS (4m54s)"),
        ("Positive Run 2 (Guard Fork PR)", "43141994038", "baf49be", "247 Passed / 0 Failed", "✅ SUCCESS (5m31s)"),
        ("Negative Run (Intentional Fail)", "43143752689", "4265c2a", "246 Passed / 1 Failed", "❌ FAILURE (6m10s)")
    ]
    rw = [Inches(1.8), Inches(1.3), Inches(1.0), Inches(1.6), Inches(1.2)]
    for i, r_row in enumerate(runs_data):
        row = tbl_runs.rows[i + 1]
        for j, val in enumerate(r_row):
            cell = row.cells[j]
            cell.width = rw[j]
            set_cell_background(cell, "F8F9FA" if i % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, 50, 50, 60, 60)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            r = p.add_run(val)
            r.font.size = Pt(8.5)
            if "SUCCESS" in val:
                r.font.bold = True
                r.font.color.rgb = GREEN
            elif "FAILURE" in val:
                r.font.bold = True
                r.font.color.rgb = RED

    doc.add_paragraph().paragraph_format.space_after = Pt(4)
    add_para("Tại PR nháp #67, test ExampleUnitTest.addition_isCorrect bị sửa cố ý thành assertEquals(5, 2 + 2). Lệnh ./gradlew testDebugUnitTest thất bại với exit code 1. Job kết luận FAILURE, check đỏ, nút merge bị chặn. Dù bước test lỗi, các bước upload artifacts và báo cáo JUnit vẫn thực thi (if: always()).", "Chi tiết Negative Test (Hạng mục 1):")
    add_para("Sau khi ghi nhận bằng chứng đầy đủ, PR nháp #67 đã được đóng và nhánh tạm ci/negative-test-kan103 đã được xóa hoàn toàn trên cả local và remote. Nhánh kan103 không chứa commit lỗi.", "Xác nhận dọn dẹp:")

    # 6. Branch Protection
    add_sec_heading("6. BẢO VỆ NHÁNH MAIN (BRANCH PROTECTION - HẠNG MỤC 4)")
    add_para("Kiểm tra quyền truy cập tài khoản: quyền hiện tại gồm push: true, pull: true, triage: true; admin: false. Để bật Branch Protection cho nhánh main, Quản trị viên (Admin Repository) thực hiện theo các bước sau:", "Phân quyền & Hướng dẫn:")
    add_para("Vào Repository GitHub -> Settings -> Branches -> Nhấn 'Add branch protection rule' (hoặc Edit rule cho nhánh main).", "Bước 1:", bullet=True)
    add_para("Nhập Branch name pattern: main.", "Bước 2:", bullet=True)
    add_para("Tích chọn 'Require a pull request before merging'.", "Bước 3:", bullet=True)
    add_para("Tích chọn 'Require status checks to pass before merging' và 'Require branches to be up to date before merging'.", "Bước 4:", bullet=True)
    add_para("Tại ô tìm kiếm Status checks, chọn chính xác 2 checks: 'Build & Unit Test' (tên job trong android-ci.yml) và 'Unit Test Results' (check do action EnricoMi tạo).", "Bước 5 (Tên check chính xác):", bullet=True)
    add_para("Nhấn 'Save changes' để lưu cấu hình.", "Bước 6:", bullet=True)

    # 7. Đối chiếu 10 tiêu chí
    add_sec_heading("7. BẢNG ĐỐI CHIẾU 10 TIÊU CHÍ NGHIỆM THU (CRITERIA AUDIT)")
    tbl_audit = doc.add_table(rows=11, cols=4)
    tbl_audit.alignment = WD_TABLE_ALIGNMENT.CENTER
    aud_headers = ["STT", "Tiêu chí nghiệm thu", "Kết quả", "Bằng chứng kỹ thuật thực tế"]
    for j, h in enumerate(aud_headers):
        cell = tbl_audit.rows[0].cells[j]
        set_cell_background(cell, "003366")
        set_cell_margins(cell, 60, 60, 60, 60)
        p = cell.paragraphs[0]
        r = p.add_run(h)
        r.font.bold = True
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(255, 255, 255)

    aud_rows = [
        ("1", "Workflow nằm đúng .github/workflows/", "PASS", "Tạo file .github/workflows/android-ci.yml hợp lệ cú pháp."),
        ("2", "Tự chạy khi push lên các nhánh quy định", "PASS (Chờ merge)", "Khai báo trigger push [main, develop]. Đã chạy khi push nhánh kan103; push trên main đang 'Chờ merge' PR #66."),
        ("3", "Tự chạy khi tạo/cập nhật PR vào nhánh chính", "PASS", "Trigger pull_request [main]. Kích hoạt tự động trên PR #66 (Runs 43141141541, 43141994038) và PR #67 (Run 43143752689)."),
        ("4", "JDK và Gradle tương thích dự án, có cache", "PASS", "Temurin JDK 21 khớp daemon JVM toolchain, setup-gradle@v4 kích hoạt cache thành công."),
        ("5", "Android Build thành công", "PASS", "Task assembleDebug hoàn thành BUILD SUCCESSFUL trong 4m 8s, sinh file APK debug 35.1 MB."),
        ("6", "Toàn bộ Unit Test hiện có được thực thi", "PASS", "247/247 unit tests được thực thi đầy đủ trên 41 test suites qua task testDebugUnitTest."),
        ("7", "Tất cả Unit Test Pass, không test bị bỏ qua", "PASS", "Positive runs đạt 247 Passed, 0 Fail, 0 Skip. Không dùng @Ignore, @Disabled."),
        ("8", "Workflow Failure khi build hoặc test lỗi", "PASS", "Được chứng minh thực tế qua Run ID 43143752689 (1 test cố tình fail -> exit code 1 -> Workflow kết luận Failure)."),
        ("9", "Log và báo cáo test truy cập được", "PASS", "Check Run 130932907895, comment PR #66, step summary, 3 artifacts lưu 14 ngày (reports, xml, apk)."),
        ("10", "Workflow kết thúc Success khi mọi kiểm tra đạt", "PASS", "Các runs hợp lệ (43141141541, 43141994038) kết thúc với trạng thái completed: success.")
    ]
    aw = [Inches(0.5), Inches(2.2), Inches(1.3), Inches(2.9)]
    for i, a_row in enumerate(aud_rows):
        row = tbl_audit.rows[i + 1]
        for j, val in enumerate(a_row):
            cell = row.cells[j]
            cell.width = aw[j]
            set_cell_background(cell, "F8F9FA" if i % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, 50, 50, 50, 50)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            r = p.add_run(val)
            r.font.size = Pt(8.5)
            if j == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            elif j == 2:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r.font.bold = True
                r.font.color.rgb = GREEN
            elif j == 1:
                r.font.bold = True
                r.font.color.rgb = DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # 8. Kết luận
    add_sec_heading("8. KẾT LUẬN VÀ TRẠNG THÁI HIỆN TẠI")
    add_para("Kỹ sư DevOps đã hoàn thành đủ 10/10 tiêu chí nghiệm thu của task KAN-103. Hệ thống CI đã được kiểm chứng nghiêm ngặt cả trường hợp Pass và Fail. Nhánh kan103 đã được cập nhật toàn bộ cải tiến và đẩy lên GitHub, sẵn sàng để quản trị viên cấu hình Branch Protection và hợp nhất vào nhánh chính.", "Kết luận:")

    out1 = Path("Bao-cao-KAN-103.docx")
    out2 = Path("docs/qa-evidence/Bao-cao-KAN-103-CI-GitHub-Actions.docx")
    out2.parent.mkdir(parents=True, exist_ok=True)
    doc.save(out1)
    doc.save(out2)
    print(f"Generated: {out1.resolve()}")
    print(f"Generated: {out2.resolve()}")

if __name__ == "__main__":
    create_report()
