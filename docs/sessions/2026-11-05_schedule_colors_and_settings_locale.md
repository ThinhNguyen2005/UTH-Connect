# Báo cáo phiên làm việc: Token-hóa màu tab Thời khóa biểu, sửa vỡ lưới Tiện ích, thêm Cài đặt Ngôn ngữ & Giao diện

- **Thời gian**: 2026-11-05 (nối tiếp phiên [`2026-11-05_utilities_hub_and_4tab_nav.md`](2026-11-05_utilities_hub_and_4tab_nav.md))
- **Nhánh làm việc**: `feature/auth-thinh` (làm trực tiếp trên nhánh hiện tại theo yêu cầu, không tạo nhánh mới, **không commit**)
- **Tác giả / Vai trò**: Qoder pair programmer — phần UI/Design System + cơ chế locale
- **Tham chiếu tiêu chuẩn**: `AGENTS.md`, `docs/Design-system.md` mục 1.4 / 4.12 / 4.13 / 4.14, `docs/UI-UX.md`, `docs/Design.md` mục 8, `docs/CODING_STANDARDS.md` mục 3.1 / 6 / 7 / 8.1 / 12

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

Người dùng đặt ba câu hỏi nối tiếp sau phiên Tiện ích + nav 4 tab:

1. **"Trang thời khóa biểu là hardcode hả?"** — Xác nhận: **đúng**, và nặng hơn câu hỏi. `presentation/schedule/ScheduleScreen.kt` (1052 dòng) chứa ~200 literal trái `CODING_STANDARDS.md` mục 7: 26 mã hex **không thuộc bảng màu UTH** (phần lớn là palette Tailwind `F8FAFD / 111827 / 1F2937 / 6B7280 / E5E7EB / 10B981`), 40+ giá trị `.dp/.sp` tự do, và chữ tiếng Anh hardcode trong Composable. File này là **file duy nhất** khiến lệnh quét mục 7 chạy trên toàn repo trả kết quả.
2. **"Fix phá vỡ layout các card trong trang tiện ích"** — Nguyên nhân quy kết được bằng số, không đoán: trên màn 320dp, ô lưới 3 cột chỉ còn **82.67dp card / 66.67dp bề ngang chữ**; `labelLarge` 14sp ≈ 7.4dp/ký tự ⇒ ~9 ký tự/dòng, trong khi "Chợ sách giáo trình" (19 ký tự) cần 3 dòng nhưng `maxLines = 2` **không kèm `overflow`** ⇒ Material 3 mặc định `TextOverflow.Clip`, mất chữ không dấu "…". Ngoài ra `Row` trong `UtilityTileGrid` không đặt alignment/height ⇒ hai card cùng hàng so le đáy.
3. **"Có thiếu trang nào không?"** — Đã kiểm kê toàn bộ `presentation/*` (mục 4.1 dưới đây).

**Ba quyết định phạm vi đã chốt với người dùng qua câu hỏi nhanh:**

- Thời khóa biểu: **chỉ đổi màu/shape/typography/chuỗi, không đụng dữ liệu và logic** ⇒ vẫn giữ `sampleEvents()`, giữ bộ lọc từ khóa, giữ nguyên `UiState` (không thêm nhánh Error). Gradient header được **giữ** nhưng tách sang token mới.
- Tiện ích: **giữ mật độ 2+3 cột**, sửa bằng cơ chế (ellipsis, height harmonisation, nhánh font-scale) chứ không hạ hết về 2 cột.
- Màn thiếu: vừa **báo cáo** vừa **dựng luôn** trang Cài đặt đổi ngôn ngữ EN/VI + giao diện Sáng/Tối theo `docs/Design.md:303`, dùng `attachBaseContext + recreate`, **không thêm dependency**; đồng thời nối pill VI/EN vốn chỉ là Trang trí trên AuthScreen vào cơ chế thật.

**Một quyết định phụ người dùng chốt giữa phiên**: khi `spotlessApply` toàn repo định dạng lại 115 file ngoài phạm vi, người dùng chọn **"Sửa luôn: apply + vá 13 lỗi"** (thay vì restore) để cổng `detekt spotlessCheck` của repo đi từ đỏ sang xanh.

**Toàn cảnh diff**: 146 file thay đổi, trong đó **31 file thực chất** (28 sửa + 2 file mới + `docs/sessions/` của chính phiên này) và **~115 file chỉ đổi định dạng** do `spotlessApply` (không đổi hành vi). 0 dependency mới. 4 tài liệu chuẩn được cập nhật.

---

## 2. Kết quả đạt được & Chi tiết Git Diff

### 2.1. Workstream 1 — Lưới Tiện ích hết vỡ (`presentation/utilities/UtilitiesScreen.kt`, +97/−34)

| Việc | Cách làm |
|---|---|
| Đều đáy card | `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min))` + mỗi ô `Modifier.weight(1f).fillMaxHeight()` — dựng lại đúng idiom sẵn có của repo ở `TicketsScreen.kt:394-398` |
| Chữ không biến mất | cả 4 `Text` giờ có `maxLines` + `overflow = TextOverflow.Ellipsis` (title 2, subtitle 2, nhãn dịch vụ 2, phase 1) |
| Thích ứng cỡ chữ | `val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale`; khu 3 cột tự hạ về 2 cột. Số cột thành token theo vai trò: `Layout.FeatureGridColumns = 2`, `Layout.ServiceGridColumns = 3`, `Layout.ServiceGridColumnsLargeText = 2` |
| Hết recomposition thừa | `UtilityTile` bỏ field `onClick: () -> Unit` (nguồn unstable) ⇒ `@Immutable data class` + grid gọi `onTileClick(index)`, lambda ở lại phía Screen |
| Preview chứng minh | thêm `@Preview(spec = "w320dp,h640dp")` cho cả hai khu; preview `fontScale = 1.5f` đã có trước đó |

`core/designsystem/component/UthComponents.kt` **không sửa** (file nóng, DoD mục 12). Ngoại lệ `contentPadding = false` và padding `Sm/Md` của ô dịch vụ được ghi chính thức vào `docs/Design-system.md` §4.12.

**Đã từ chối có lý do**: `LazyVerticalGrid` (grid trục chính không xác định nằm trong `Column.verticalScroll` cùng trục ⇒ lỗi layout); `FlowRow` (ô bó theo nội dung nên các cột lệch nhau); gộp tất cả về 2 cột (danh sách module khoá dài gấp đôi, phải viết lại spec 4.12).

### 2.2. Workstream 2 — Tab Thời khóa biểu token-hóa (`presentation/schedule/ScheduleScreen.kt`, +355/−508)

Toàn bộ literal về 0. Bảng ánh xạ đã áp (không sinh hex mới — mọi đích đều đã có trong bảng màu UTH):

| Trước | Sau |
|---|---|
| `0xFFF8FAFD` nền trang | `colorScheme.background` |
| Gradient `002D54→094074→0B4F8A` | `UthExtendedColors.headerGradientStart/End` + `headerOnGradient` (light: `UthNavy`→`UthNavyLight`, chữ `White`; dark: `UthNavyContainerDark`→`UthOnNavyDark`, chữ `UthOnNavyContainerDark`) |
| `White.copy(0.15/0.20/0.25)` | `headerOnGradient` + `Opacity.HeaderMetadata` / `HeaderActionFill` / `HeaderActionBorder` |
| `0xFF003366`, `0xFF003866` | `colorScheme.primary` + `onPrimary` |
| `0xFFF5A623`, `0xFFF59E0B` | `colorScheme.secondary` / `extendedColors.warning` (CTA trên Gold đổi chữ từ `1A1C1F` → `onSecondary`) |
| `10B981/ECFDF5/A7F3D0/047857`, `EFF6FF/BFDBFE/1D4ED8`, `FFFBEB/FDE68A/B45309` | `UthStatusBadge(tone = Success / Info / Warning)` thay pill tự vẽ |
| `F9FAFB/F1F5F9/E5E7EB/E2E8F0/DBE5F1` | `surfaceVariant` / `outlineVariant` / `outline` |
| `111827/1F2937` · `6B7280/374151/4B5563` | `onSurface` · `textSecondary` / `onSurfaceVariant` |
| `RoundedCornerShape(9999/12/8/4.dp)` | `MaterialTheme.shapes.*` + `Radius.*` |
| elevation `2.dp`, viền `1.5.dp` | `Elevation.Card`, `Size.Stroke` |
| `letterSpacing 0.5.sp`, `fontSize 10/11.sp`, `lineHeight 22.sp` | bỏ `.copy()`, dùng thẳng `labelSmall/labelMedium/titleMedium` (`Type.kt` đã mang letter-spacing) |
| stripe `6.dp`, dot `5–7.dp` | token mới `Size.AccentStripe`, `Size.IndicatorDot`, `Size.HeaderAction` |

**Lý do không dùng `Brush.verticalGradient(listOf(primary, tertiary, primary))`** (nghiêm túc đã cân nhắc): ở dark scheme `primary` là xanh **sáng** `#7FB2E5`, nên public hai màu đó thành gradient rồi đặt chữ tối lên sẽ ra **2.0:1 — fail AA**. Token riêng theo vai trò là lựa chọn duy nhất giữ được gradient mà vẫn AAA/AA ở cả hai scheme.

**Chuỗi**: thêm **25 key** `schedule_*` / `settings_*`, **đối xứng cả `values/` và `values-en/`** (đã kiểm: 25/25 key mới, tổng 40/40 key theo hai prefix). Dịch lại luôn `no_events_for_date` / `event_*` vốn đang là tiếng Anh trong file tiếng Việt.

**Code chết đã cắt**: 3 composable `private` không ai gọi (`SimpleQrPreview`, `QrTicketDialog`, `EventDetailsDialog`, ~170 dòng) và 4 ID `UnusedPrivateMember` tương ứng trong `config/detekt/baseline.xml`. **Có một hiệu chỉnh so với kế hoạch**: `TicketQrScreen`/dialog QR **không phải code chết** — vẫn được `CampusEventCard` gọi thật — nên giữ lại, không xoá import.

### 2.3. Workstream 3 — Cài đặt Ngôn ngữ & Giao diện (dựng mới)

| Tầng | Thay đổi |
|---|---|
| Domain | `AppSettings` thêm `languageTag` (mặc định `"vi"`) + `themeMode` (mặc định `SYSTEM`); `enum ThemeMode { SYSTEM, LIGHT, DARK }` kèm `resolveDarkTheme()`; `SettingsRepository` thêm `setLanguageTag`, `setThemeMode` và getter **đồng bộ** `currentLanguageTag()`; `UpdateSettingsUseCase` thêm hai hàm mirror |
| Data | `PreferencesSettingsRepository` thêm key `key_language_tag`, `key_theme_mode` vào file `uth_connect_settings` sẵn có |
| App shell | `MainActivity.attachBaseContext` bọc cấu hình theo tag qua `applyOverrideConfiguration(...)`; đọc tag bằng **`@EntryPoint` + `EntryPointAccessors.fromApplication`** qua file mới `SettingsEntryPoint.kt`; `setContent` phân giải `darkTheme` theo `ThemeMode` từ flow settings; `AndroidManifest` thêm `android:localeConfig="@xml/locales_config"` + file `res/xml/locales_config.xml` (`vi`, `en`) để Android 13+ hiện lựa chọn trong Settings hệ thống |
| UI | `SettingsScreen` thêm card **"Ngôn ngữ & Giao diện"**: hai hàng `Row + Column(weight(1f)) + nhóm nút segmented` tự chứa trong file (không đụng `UthComponents.kt`), chọn ngôn ngữ 2 ô, chọn theme 3 ô; ô đang chọn có **icon Check** (trạng thái không chỉ bằng màu — `docs/UI-UX.md`) và `Modifier.semantics { selected = … }`; chiều cao `Size.SegmentedControlHeight`, min touch `Size.MinTouchTarget` |
| Nối | `UtilitiesAction` thêm `LanguageSelected` / `ThemeModeSelected`; `UtilitiesViewModel.onAction` xử lý, gửi `UtilitiesEvent.LanguageChanged` để `UtilitiesRoute` gọi `activity.recreate()`; **pill VI/EN trên AuthScreen** giờ ghi thật vào store qua `UpdateSettingsUseCase` + request recreate bằng `Channel`/`receiveAsFlow()`/`LaunchedEffect` (mẫu mục 6) |

**Hai hiệu chỉnh so với kế hoạch** (đều vì kế hoạch giả định sai cơ chế Android):
- `attachBaseContext` chạy **trước** Hilt field injection ⇒ không `@Inject lateinit var` được; phải dùng `@EntryPoint`.
- `createConfigurationContext()` không đủ để Compose đọc `stringResource` theo locale mới ⇒ dùng `applyOverrideConfiguration(Configuration)` để ghi đè lên toàn bộ activity context.

**Thứ tự bắt buộc**: ghi store **xong rồi mới** phát event recreate — nếu ngược lại, activity khởi động lại đọc phải tag cũ. Và **không** recreate khi người dùng bấm lại đúng ngôn ngữ đang bật (trang trắng màn hình vô nghĩa).

### 2.4. Kết quả kiểm chứng (chạy thật, không suy đoán)

Cổng tĩnh — `./gradlew.bat :app:testDebugUnitTest detekt spotlessCheck -Pkotlin.compiler.execution.strategy=in-process`:

```
BUILD SUCCESSFUL in 39s
tests 210 / suites 35 / failures 0 / errors 0 / skipped 1
```

- Quét mục 7 trên toàn `presentation/` + `core/` (loại `theme/{Color,Dimens,Type}.kt`): **rỗng**. `ScheduleScreen.kt` — thủ phạm duy nhất — đã sạch.
- Tương phản WCAG đo bằng script (không ước lượng): light gradient **12.61 / 8.48 / 8.22**, cặp xấu nhất **5.87** (vẫn AA); Navy trên Gold **6.22**; dark gradient **8.87 / 11.21 / 6.01 / 7.32**.
- `config/detekt/baseline.xml`: **không thêm ID mới**; 1 ID `LongParameterList:AuthViewModel.kt` cập nhật chữ ký (thêm 2 use case DI), phần còn lại là kết quả regenerate do `spotlessApply`.

Kiểm chứng trên thiết bị — emulator API 37 (1440×3120), APK build mock-data (`-PuseMockData=true -PuseSupabase=false`), đăng nhập sinh viên demo:

| Kiểm tra | Cách làm | Kết quả |
|---|---|---|
| Không crash ở cold start (rủi ro lớn nhất của `@EntryPoint` trong `attachBaseContext`) | `adb logcat -s AndroidRuntime:E` qua mọi bước | không dòng nào |
| Đổi ngôn ngữ **trong app** | Cài đặt → chạm "Tiếng Việt" | **toàn bộ màn hình recreate sang tiếng Việt** ("Cài đặt ứng dụng", "Ngôn ngữ & Giao diện", nav "Trang chủ / Thời khóa biểu / Tiện ích / Cá nhân"); icon Check nhảy sang ô mới chọn |
| Ngôn ngữ **bền qua restart** | force-stop rồi mở lại (đã kiểm ở chiều EN trước đó) | vẫn giữ lựa chọn (SharedPreferences) |
| Override theme không theo hệ thống | `cmd uimode night yes` (hệ thống tối) rồi chọn **"Sáng"** | app **chuyển sáng hoàn toàn** dù hệ thống vẫn tối ⇒ `ThemeMode` thắng `isSystemInDarkTheme()`, không trắng màn hình |
| Header gradient ở dark | chọn **"Tối"** → tab Thời khóa biểu | gradient đảo sang `headerGradientStart/End` dark, chữ `UthOnNavyContainerDark`, **không còn chữ tối trên nền tối** |
| Lưới Tiện ích ở 320dp | `wm density 720` | card cùng hàng **bằng đáy**, chữ dài bị cắt hiện **"…"** thay vì mất chữ |

Toàn bộ thay đổi được chụp màn hình tại `C:\Users\Abc\AppData\Local\Temp\uthshots\` (13→18). **Đã khôi phục emulator** sau khi đo: `wm density reset` (560), `cmd uimode night no`.

### 2.5. Tài liệu cập nhật

- `docs/Design-system.md`: §4.12 bổ sung 5 gạch đầu dòng bắt buộc cho lưới card; **mới §4.13 "Choice row"** (hàng chọn segmented); **mới §4.14 "Tab Thời khóa biểu"** (token gradient, `UthStatusBadge`, `Size.AccentStripe`/`IndicatorDot`, cấm `.copy(fontSize/letterSpacing)`, prefix `schedule_`); §1.4 thêm 7 dòng tương phản gradient đã đo + ghi chú "vì sao không dùng primary/tertiary"; §7 cập nhật trạng thái quét.
- `docs/CODING_STANDARDS.md`: **mới §8.1** "Cơ chế đổi ngôn ngữ/giao diện đã chọn" (lý do enum đặt tầng domain, `applyOverrideConfiguration` + `@EntryPoint`, thứ tự ghi-store-rồi-event, `localeConfig`).
- `docs/Design.md`: §8.1 viết lại bảng trạng thái 4 tab theo code thật; §8.2 viết lại sơ đồ điều hướng (Auth đã dựng, Settings nằm dưới Tiện ích) + đoạn "Màn còn thiếu so với spec".
- `docs/REPO_TREE.md`: thêm dòng `v1.8-schedule-settings` ở §8, cảnh báo cả 5 nhánh về ~115 file đổi định dạng + baseline regenerate + danh sách file nóng.

---

## 3. Bài học & Kinh nghiệm rút ra

1. **"Hardcode" không chỉ là bệnh thẩm mỹ — nó là bệnh đo được.** Cách nhanh nhất để định lượng một màn hình bẩn là chạy đúng lệnh quét trong `CODING_STANDARDS.md` mục 7 và đếm literal; con số đó cũng chính là bằng chứng hoàn thành khi chạy lại lệnh quét cuối phiên.
2. **Đừng sửa layout bằng cảm giác.** "Card bị vỡ" được giải thích bằng 82.67dp card / 66.67dp chữ / ~9 ký tự mỗi dòng. Có phép tính thì biết chắc phải sửa `overflow` chứ không phải `maxLines`, và biết 3 cột chỉ vỡ ở màn hẹp chứ không phải mọi màn.
3. **Token theo vai trò thắng token theo bảng màu khi tương phản là ràng buộc.** `primary`/`tertiary` nghe hợp lý làm gradient, nhưng ở dark scheme `primary` là xanh *sáng* ⇒ chữ tối đặt lên chỉ 2.0:1. Bảng màu không tự đảm bảo contrast; contrast phải được đo và ghi vào spec.
4. **`maxLines` mà thiếu `overflow` là mất chữ thầm lặng.** Mặc định của M3 là `TextOverflow.Clip`, cắt không dấu hiệu. Đây là lỗi dễ tái phạm ở mọi screen — đã thành quy định cứng ở §4.12.
5. **`attachBaseContext` nằm ngoài vòng đời Hilt.** Mọi hướng dẫn "inject repository vào Activity rồi đọc trong `attachBaseContext`" đều fail im lặng; `@EntryPoint` + `EntryPointAccessors.fromApplication` là đường đúng. Tương tự, `createConfigurationContext` đổi locale cho *một context con*, còn `applyOverrideConfiguration` đổi cho *cả activity* — chỉ cái thứ hai làm `stringResource` theo locale mới.
6. **Event recreate phải phát SAU khi ghi store.** Đây là bug kinh điển one-way-flow: thứ tự đảo thì activity khởi động lại đọc giá trị cũ, người dùng thấy "bấm không ăn".
7. **Sửa code thay vì phình baseline.** `TooManyFunctions:UtilitiesViewModel` (11 hàm) giải quyết bằng cách gom 4 hàm công khai vào `UtilitiesAction`, không phải bằng một dòng baseline. `MaxLineLength` do Spotless gộp expression body về một dòng thì giải quyết bằng `when {}` block.
8. **`spotlessApply` toàn repo là thương vụ tập thể, không phải cá nhân.** 115 file đổi định dạng chạm vào mọi nhánh đang mở; phải ghi vào `REPO_TREE.md` và báo chủ file trước khi merge, nếu không người khác sẽ nhận conflict khổng lồ không hiểu vì sao.
9. **Preview ≠ thiết bị.** `@Preview` chứng minh ellipsis và đáy card; chỉ thiết bị mới chứng minh `recreate()` không crash, locale thật sự áp lên `stringResource`, và `ThemeMode` thắng `uimode` hệ thống.

---

## 4. Vấn đề tồn đọng & Nợ kỹ thuật

### 4.1. Bảng khoảng trống màn hình (trả lời câu "có thiếu trang nào không?")

**Kiểm kê: không có màn mồ côi** — mọi screen trong `presentation/*` đều tới được từ 4 tab. Thiếu là thiếu **so với spec**, cụ thể:

| Màn / luồng | Spec nói gì | Code hiện tại | Mức độ |
|---|---|---|---|
| **Deadline tracker** | `README.md:215` luồng "Nhập deadline từng môn → Notification T-3/T-1/giờ G" | Domain **xong**: `TimetableUseCase` sinh `urgentDeadlines`/`laterDeadlines` ngay trong `ScheduleUiState.kt:31-32`, `InMemoryTimetableRepository` có 4 deadline. `ScheduleScreen` tham chiếu chữ "deadline" **0 lần** ⇒ tính ra rồi bỏ đó | **Nghiêm trọng** — UI chết trên domain sống |
| **Tab Thời khóa biểu hiển thị dữ liệu giả** | `docs/Design.md` mục 8 | `ScheduleViewModel.kt:115-180` render `sampleEvents()` bịa sẵn; `useCase.timetable` chỉ được dùng cho **chấm dot trên ô ngày**, không cho danh sách | Trung bình — người dùng không phân biệt được |
| **Admin Dashboard** | `README.md:99` và `:669` ghi **MVP/Phase 1 BẮT BUỘC** ("duyệt Organizer") | **Không có package** `presentation/admin` | **Nghiêm trọng** — cam kết MVP chưa dựng |
| **EventStats + export CSV** | `README.md:157`, mốc M2 (`:699`) | Không có package | Trung bình — milestone M2 |
| **Chi tiết Đồ thất lạc** | `README.md:232-248` (3.4, P1/Phase 3) | `presentation/lostfound/` có đủ 6 file **danh sách**, nhưng **không có route chi tiết** | Nhẹ — đúng phase |
| **OCR nhập lịch** | `README.md:706` ghi rõ **Backlog V1**, non-goal | Không có | Không phải nợ |
| **Cài đặt ngôn ngữ/theme** | `docs/Design.md:303` | **Đã dựng xong trong phiên này** ✅ | Hết |

### 4.2. Nợ kỹ thuật đã ghi nhận, cố ý không sửa trong phiên

1. **Bộ lọc sự kiện bằng `title.contains("Workshop")` nằm trong Screen** (`ScheduleScreen.kt:128-130`) — nghiệp vụ đặt sai tầng, phải xuống `ScheduleViewModel`/UseCase. Không sửa vì thuộc phạm vi dữ liệu (người dùng chốt "chỉ đổi màu").
2. **Nhãn ca "Morning/Afternoon/Evening" không dịch** — nó đến từ field `session` của `sampleEvents()` (`ScheduleViewModel.kt:165,177,189`), tức là **dữ liệu**, không phải chuỗi UI. Sẽ tự hết khi nối `TimetableUseCase` thật; không được nhân bản sang UI.
3. **`ScheduleScreen.kt` vẫn ~900 dòng** — chưa tách thành `presentation/schedule/components/{ScheduleHeader,WeekPicker,EventFilterChips,CampusEventCard}.kt` vì cần xin ngoại lệ quy tắc "đúng 5 file/module" và sẽ invalidate baseline detekt. ID `TooManyFunctions:ScheduleScreen.kt` vẫn còn hiệu lực.
4. **`UtilityModuleId.TIMETABLE` vẫn `enabled = false`** trong `StaticUtilityModuleRepository` **trong khi tab Thời khóa biểu đã tồn tại** ⇒ ô lưới "Thời khóa biểu / Phase 2" hiện như dịch vụ chưa mở, gây nhầm. Hướng xử lý (gỡ khỏi lưới hoặc bật) thuộc tầng dữ liệu, để phiên sau. Cùng trạng thái: `BOOKS`, `HOUSING`, `RIDE_SHARE`.
5. **`Error` state của tab Thời khóa biểu chưa có** — không thêm vì `UiState` đang giữ nguyên (quyết định phạm vi).
6. **Churn định dạng ~115 file** — không đổi hành vi nhưng gây conflict cho 5 nhánh khác; đã cảnh báo ở `docs/REPO_TREE.md` §8.

---

## 5. Kế hoạch cho các phiên tiếp theo

1. **Nối dữ liệu thật cho tab Thời khóa biểu** (stage B/C, nhánh `feature/timetable-<tên>`, do **Nghĩa** sở hữu `presentation/schedule/` theo `REPO_TREE.md` §5): thay `sampleEvents()` bằng `useCase.timetable`, **dựng UI Deadline tracker** từ `urgentDeadlines`/`laterDeadlines` đã có sẵn, chuyển bộ lọc từ khóa xuống VM, thêm nhánh `Error`. **PR đổi màu của phiên này phải merge TRƯỚC** PR tính năng, và cần báo chủ file vì `strings.xml` + `Dimens.kt` chắc chắn conflict.
2. **Admin Dashboard** — nợ MVP lớn nhất (`README.md:669`), cần spec riêng cho luồng duyệt Organizer.
3. **EventStats + export CSV** cho organizer (milestone M2).
4. **Quyết định `UtilityModuleId.TIMETABLE.enabled`**: gỡ ô "Thời khóa biểu" khỏi lưới dịch vụ chưa mở, hoặc bật lên và trỏ thẳng sang tab. Một dòng data, làm chung với phiên (1).
5. **Tách `ScheduleScreen.kt`** thành 4 component — xin ngoại lệ 5-file/module, làm ở phiên riêng để regenerate baseline một lần.
6. **Chạy lại cổng tĩnh trước mỗi lần merge** và xem `docs/Design-system.md` §4.13/§4.14 là chuẩn mới cho mọi màn chọn lựa chọn / mọi header gradient sau này.
