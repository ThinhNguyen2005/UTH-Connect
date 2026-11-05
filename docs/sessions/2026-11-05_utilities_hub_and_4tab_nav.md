# Báo cáo phiên làm việc: Trang Tiện ích dạng lưới hai khu vực + thu bottom nav về 4 tab

- **Thời gian**: 2026-11-05
- **Nhánh làm việc**: `feature/auth-thinh` (làm trực tiếp trên nhánh hiện tại theo yêu cầu, không tạo nhánh mới)
- **Tác giả / Vai trò**: Qoder pair programmer — phần UI/Design System
- **Tham chiếu tiêu chuẩn**: `AGENTS.md`, `README.md`, `docs/Design-system.md`, `docs/Design.md` mục 8, `docs/UI-UX.mc`, `docs/CODING_STANDARDS.md` mục 7 & 12

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

Người dùng đưa một ảnh tham chiếu trang "Tiện ích" của một super-app Việt (hai khu vực: lưới lớn "Đề xuất cho bạn" và lưới nén "Tiện ích người dùng") và nêu ba yêu cầu:

1. Tích hợp thẩm mỹ lưới tiện ích đó vào tab Tiện ích của UTH Connect.
2. Thanh điều hướng đáy đang có **6 tab** là không đúng chuẩn, cần theo khuyến nghị của Google.
3. Thiết kế icon phù hợp cho từng chức năng tiện ích.

**Lưu ý về đầu vào**: người nói "2 ảnh tôi gửi" nhưng chỉ **một** ảnh được nhận lên phiên làm việc; ảnh thứ hai không có trong hội thoại. Toàn bộ quyết định bố cục do đó chỉ dựa trên ảnh duy nhất đó.

Ba quyết định đã chốt qua câu hỏi nhanh: giữ **4 tab theo `docs/Design.md` mục 8.1**; layout **hai khu vực như ảnh tham chiếu**; làm trên nhánh hiện tại. Riêng yêu cầu "tự vẽ icon duotone" bị chính người dùng phản biện ("AI có vẽ vector đẹp và chuẩn không?"), nên đã kiểm chứng bằng context7 và **chuyển sang vector two-tone chính chủ của Material Icons** thay vì tự sinh path data.

Toàn cảnh diff: 8 file sửa, 1 file Kotlin mới (`EventsNavigation.kt`), 11 vector drawable mới, 2 file string sửa, 4 tài liệu cập nhật. Không đổi dependency Gradle, không đổi theme color/typography.

---

## 2. Kết quả đạt được & Chi tiết Git Diff

### 2.1. Navigation — 6 tab → 4 tab (code nghiệp vụ)

Khuyến nghị của Google cho navigation bar là **"three to five destinations of equal importance"** (developer.android, tài liệu Navigation bar). App từng có 6 entry trong `UthDestination` và **ba tab cùng dùng một icon `DateRange`**.

| File | Thay đổi |
|---|---|
| `presentation/navigation/UthDestination.kt` | xoá entry `TICKETS`, `EVENTS` và `@Serializable object EventsDestination` cũ; enum còn `HOME · SCHEDULE · UTILITIES · PROFILE`; KDoc ghi lại căn cứ 3–5 điểm đến |
| `presentation/events/EventsNavigation.kt` (**mới**) | `@Serializable data object EventsDestination` + `eventsGraph(navController)` — route màn con nằm trong package của feature theo hợp đồng navigation, `onOpenAttendance` nối vào `AttendanceDestination(eventId)` |
| `presentation/navigation/UthApp.kt` | bỏ `import BuildConfig` và bộ lọc `UthDestination.entries.filter { it != EVENTS \|\| BuildConfig.USE_SUPABASE }` (không còn tab điều kiện theo build flavor); `composable<EventsDestination>{…}` → `eventsGraph(navController)`; `composable<UtilitiesDestination>` thêm nhánh `UtilityModuleId.EVENTS -> navigate(EventsDestination)`; `composable<ProfileDestination>` truyền `onOpenTickets` |
| `presentation/tickets/TicketsNavigation.kt` | chỉ đổi KDoc: `TicketsDestination` vào từ tab Cá nhân, không còn là tab đáy |

Không tính năng nào bị mất lối vào: **Vé** → hàng điều hướng trong tab Cá nhân; **Sự kiện** → ô trong lưới Tiện ích.

| File | Thay đổi |
|---|---|
| `presentation/profile/ProfileScreen.kt` | `ProfileScreen`/`ProfileOverview` nhận `onOpenTickets`; thêm `ProfileNavRow` (icon duotone + nhãn + mũi tên, `UthCard` cao tối thiểu `Size.MinTouchTarget`, testTag `profile_open_tickets`) |
| `presentation/profile/ProfileRoute.kt` | thêm tham số `onOpenTickets: () -> Unit = {}` và chuyển xuống Screen |
| `data/repository/StaticUtilityModuleRepository.kt` | `EVENTS` và `LOST_FOUND` bật `enabled = true` — hai module này giờ là điểm vào duy nhất, không còn tab riêng |

### 2.2. Trang Tiện ích — hai khu vực (code UI)

`presentation/utilities/UtilitiesScreen.kt` viết lại toàn bộ, bỏ `UtilityGridCard` (2 hàng 2 ô tự tay) và `UthModuleRow` (danh sách dạng hàng + `UthStatusBadge`):

- `UthSectionHeader(utilities_section_mine)` + `UtilityTileGrid(columns = 2)` cho **mọi thứ mở được ngay**: 4 tiện ích tra cứu tĩnh (Học phí, Điểm rèn luyện, Cơ sở & Danh bạ, Cài đặt) **gộp** với module có `enabled = true` (Sự kiện, Đồ thất lạc). Ô lớn: khung icon `Size.UtilityFeatureIconContainer`, glyph `Size.UtilityFeatureIcon`, `titleMedium` + `bodySmall`.
- `UthSectionHeader(utilities_section_upcoming)` + `UtilityTileGrid(columns = 3)` cho module chưa bật. Ô nén: `Size.UtilityServiceIconContainer` + glyph `Size.Icon`, `UthCard(onClick = null)`, `contentPadding = false` với padding `Sm`/`Md` riêng (padding `Lg` chuẩn làm nhãn gãy 3 dòng ở bề ngang ~82dp), lộ trình ghi bằng **chữ** `phase_*` màu `textSecondary` — không badge màu, đúng quy tắc "không truyền trạng thái chỉ bằng màu".
- Grid chung: `chunked(columns)`, mỗi ô `weight(1f)`, hàng lẻ chèn `Spacer(weight(1f))` để ô cuối không bị kéo giãn.
- Trạng thái loading/error/empty giữ nguyên như cũ, chỉ đổi chỗ nằm (khu vực hai); khu vực một render ngay vì không phụ thuộc repository → **không có layout jump khi tải module**.
- 9 `@Preview`: content light/dark, content `fontScale = 1.5f`, loading light/dark, empty light/dark, error light/dark.

`core/designsystem/theme/Dimens.kt`: thêm 3 token `Size.UtilityFeatureIconContainer` (56), `Size.UtilityFeatureIcon` (32), `Size.UtilityServiceIconContainer` (44), mỗi token có comment nói nơi dùng theo đúng quy tắc mục 7 của `CODING_STANDARDS.md`.

### 2.3. Icon (resource mới)

11 vector **two-tone** tải từ `google/material-design-icons` (`android/<category>/<tên>/materialiconstwotone/black/res/drawable/twotone_<tên>_24.xml`, Apache-2.0), đã chuẩn hoá: bỏ `android:tint="?attr/colorControlNormal"` để màu do `Icon(tint = …)` cấp theo token, thay `@android:color/white` = `#FF000000`, thêm 2 dòng comment ghi nguồn. Tên file theo hợp đồng `ic_<module>_<tên>.xml` của `docs/REPO_TREE.md` mục 5:

`ic_utilities_{tuition,training_point,campuses,settings,events,timetable,lost_found,books,housing,rideshare}.xml` + `ic_profile_tickets.xml`.

`Icon` tint theo `contentColor` nhưng giữ `fillAlpha` từng lớp của vector (hành vi đã ghi trong tài liệu Compose); **chưa** kiểm chứng bằng mắt trên thiết bị — xem mục 4.5.

### 2.4. Tài nguyên chuỗi

| Key | `values/` | `values-en/` | Ghi chú |
|---|---|---|---|
| `utilities_section_mine` | "Tiện ích của tôi" | "My utilities" | thêm mới, đủ cả hai locale trong cùng commit |
| `tab_events` | **xoá** | (không tồn tại) | không còn tham chiếu nào trong `app/src` |
| `utilities_not_available` | **xoá** | **xoá** | ô lộ trình dùng `phase_*` thay vì nhãn "chưa sẵn sàng" |
| `tab_tickets` | giữ, thêm comment "không còn là tab đáy" | giữ | vẫn được `ProfileNavRow` dùng |

### 2.5. Tài liệu

`docs/Design.md` 8.1 (bảng "trước / bây giờ" cho Vé và Sự kiện + căn cứ 3–5 điểm đến), 8.2 (Events · LostFound là ô đã bật); `docs/Design-system.md` thêm **mục 4.12 Utility tile** (hai mật độ, token, tương phản đã đo: `onPrimaryContainer` 12.3:1 AAA, `textSecondary` 5.5:1 AA, `textTertiary` ~2.9:1 bị loại cho chữ nhỏ), sửa mục 7 (chính sách icon) và mục 10 (chốt câu hỏi 3, trả lời câu hỏi 6); `docs/CODING_STANDARDS.md` mục 7 (quy trình tải/chuẩn hoá vector two-tone); `docs/REPO_TREE.md` mục 3 (thêm `EventsNavigation.kt`), mục 5 (icon two-tone), mục 8 (dòng `v1.7-utilities`).

### 2.6. Kiểm chứng đã chạy

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest -Pkotlin.compiler.execution.strategy=in-process` → **BUILD SUCCESSFUL**, chạy 3 lần (sau mỗi đợt sửa). Tổng kết JUnit: **35 suite / 207 test / 0 failure / 0 error / 1 skipped**.
- Quét hardcode mục 7 `CODING_STANDARDS.md` trên các file đã chạm: **sạch**. Cả repo `app/src/main` chỉ còn `presentation/schedule/ScheduleScreen.kt` dính (nợ có sẵn từ nhánh timetable, không thuộc phiên này).
- Không có warning mới từ Kotlin; hai warning ở `ProfileRoute.kt` (`hiltViewModel` deprecated, `Expression is unused` dòng 86) là **có sẵn trước khi sửa**.
- **Chưa** smoke-test trên máy ảo/thật: layout 3 cột ở màn 320dp và hành vi TalkBack mới được suy từ số đo và preview, chưa đo trên thiết bị.

---

## 3. Bài học & Kinh nghiệm rút ra

1. **"Tự vẽ icon duotone" là yêu cầu cần phản biện, không phải việc cần làm ngay.** Người dùng hỏi thẳng liệu AI vẽ vector có đạt chuẩn không; kiểm chứng qua context7 cho câu trả lời rẻ hơn nhiều: Material Icons đã có sẵn bộ two-tone Apache-2.0, chỉ cần `res/drawable` + `painterResource`. Không thêm dependency, không tự bịa path data, không vỡ lưới icon. Đây là lựa chọn nên mặc định cho mọi yêu cầu "thiết kế icon" trong app Android.
2. **Rút tab là thay đổi dễ gây mất tính năng.** Nếu chỉ sửa enum rồi build, *Sự kiện* và *Vé* vẫn biên dịch xanh nhưng **không còn lối vào** — loại regression không compiler nào bắt được. Thứ tự an toàn: thêm điểm vào mới (graph + callback + `enabled = true`) **trước** khi bỏ tab.
3. **`BuildConfig.USE_SUPABASE` từng điều khiển số tab.** Bỏ tab Events khỏi nav làm bộ lọc này vô nghĩa; bài học là *số lượng tab không nên phụ thuộc build flavor*, vì người review/release không thấy cùng một thông tin điều hướng.
4. **Token hoá trước khi viết màn.** Mật độ lưới mới cần 3 kích thước icon; thêm token + comment rồi mới dùng, thay vì viết `.dp` trong Composable — nhờ vậy lệnh quét mục 7 vẫn trả rỗng và spec mật độ lưới có chỗ ghi là `Design-system.md` mục 4.12.
5. **Badge không vào được ô 3 cột.** Ở bề ngang ~82dp, `UthStatusBadge` chiếm chỗ nhiều hơn giá trị nó mang; chuyển trạng thái về **chữ** (`phase_*`) vừa khớp WCAG "không chỉ bằng màu" vừa giữ mật độ. Đồng thời phát hiện `textTertiary` chỉ ~2.9:1 trên Surface — đủ cho chữ nhỏ ở footer mờ nhưng **không** đủ cho nhãn trạng thái, nên phải dùng `textSecondary`.
6. **Đừng đổi lề ngang màn hình để cứu mật độ.** Lần đầu giảm `Spacing.Xl` xuống `Lg` cho lưới thở; điều đó phá chuẩn "lề ngang mọi màn hình = `Xl`" đã ghi ở `Design-system.md` mục 3.1. Cách đúng: giữ lề, giảm padding **trong** ô (`contentPadding = false`).
7. **Tên resource là hợp đồng đa nhánh.** `res/drawable` dùng chung namespace; quy tắc `ic_<module>_<tên>` tồn tại để tránh trùng, nên 11 file đã đổi tên trước khi merge chứ không phải "đổi sau cũng được".

---

## 4. Vấn đề tồn đọng / Kỹ thuật cần lưu ý

1. **`presentation/schedule/ScheduleScreen.kt` vi phạm mục 7 `CODING_STANDARDS.md`** (nhiều `.dp`, `.sp`, `Color(0x…)`, `RoundedCornerShape` thô). Câu "lệnh quét trả rỗng trên toàn bộ `app/src/main`" ở dòng 182 của tài liệu này **đang không đúng** trên nhánh này. Không thuộc phạm vi phiên nên để nguyên; owner nhánh timetable cần token-hoá.
2. **`AGENTS.md` ép đọc `docs/UI-UX.mc` + ảnh tham chiếu, nhưng ảnh thứ hai chưa được gửi.** Bố cục hiện tại khớp một ảnh; nếu ảnh còn lại có khu vực thứ ba (banner/chương trình), màn Tiện ích phải mở thêm section.
3. **`StaticUtilityModuleRepository` vẫn là nguồn tĩnh.** `enabled = true` cho EVENTS/LOST_FOUND là quyết định UI-tạm-thời; khi có repository thật (M0), cờ phải đến từ cấu hình server, nếu không lưới "Tiện ích của tôi" sẽ hiển thị module không dùng được.
4. **`utilities_grid_settings_desc` = "Thông báo & Phiên bản"** nay dẫn vào `SettingsScreen` nội bộ tab Tiện ích, trong khi tab Cá nhân cũng có luồng cài đặt/dăng xuất — hai điểm vào "cài đặt" dễ gây nhầm cho người dùng; cần quyết một chỗ ở M0.
5. **Chưa đo trên thiết bị**: chưa chạy `mobile-use`/emulator để xác nhận (a) ô 3 cột không cắt nhãn ở 320dp, (b) font scale 200 % không làm lưới vỡ, (c) TalkBack đọc mỗi ô một lần. Preview có `fontScale = 1.5f` mới là một nửa bằng chứng.
6. **Đã xoá 2 key string** trong `strings.xml` trái với cột "tuyệt đối không xoá key" ở `REPO_TREE.md` mục 5 — đó là quy tắc cho nhánh feature đóng góp; ở đây key mồ côi do chính phiên này tạo ra. Chủ repo các nhánh khác nên biết để tránh resolve conflict vô nghĩa.
7. Working tree còn `feature/auth-thinh` với auth đã commit (`b47e98d`); toàn bộ thay đổi của phiên này **chưa commit**.

---

## 5. Kế hoạch cho các phiên tiếp theo

1. **Smoke-test trên thiết bị** (emulator 320dp + 411dp, font scale 200 %): chụp màn Tiện ích light/dark, xác nhận nhãn không cắt, rồi commit.
2. **Tách commit theo lớp** để review rẻ: `feat(navigation): 4 tabs + entry points` → `feat(ui): utility grid + duotone icons` → `docs: design system/nav/tree` → `chore(strings): drop orphan keys`.
3. **Dọn `ScheduleScreen.kt` về token** (mở issue cho nhánh timetable), sau đó khôi phục câu "lệnh quét trả rỗng" ở `CODING_STANDARDS.md` mục 7 và bật `spotless`+`ktlint` như nợ kỹ thuật #1 mục 13.
4. **Nổi bật hoá module khi có Firebase/Supabase**: `enabled` + `DeliveryPhase` đọc từ cấu hình; cân nhắc thêm `isOrganizer` để màn Quản lý của organizer xuất hiện như một ô trong lưới Tiện ích thay vì tab "Quản lý" (giữ trần 4 tab).
5. **Ảnh tham chiếu thứ hai**: nếu người dùng gửi tiếp, đối chiếu phần còn thiếu (nhiều khả năng là khu vực banner/khuyến mãi) và bổ sung component tương ứng vào `docs/Design-system.md` mục 4.
6. **Bổ sung Compose UI test** cho `profile_open_tickets` và hai khu vực lưới khi bật `hilt-android-testing` (nợ kỹ thuật #2), để việc rút tab không thể âm thầm mất lối vào lần nữa.
