# UTH Connect — Design System

> **Phiên bản:** v0.3.1 · **Trạng thái:** đã áp dụng vào code (`core/designsystem/theme/`) — còn mục component nâng cao (skeleton, bottom sheet) chờ module
> **Phạm vi:** token màu, chữ, khoảng cách, bo góc, component, dark mode, khả năng truy cập.
> **Không chứa:** quy ước viết code (xem `CODING_STANDARDS.md`), mô tả nghiệp vụ (xem `Design.md`).
> **Nguồn:** chuyển thể từ spec *CampusHub Design System* + nhận diện thương hiệu UTH (Navy `#003366`, Gold `#F5A623`, Trắng `#FFFFFF`).

---

## 0. Quy ước đặt tên (đã chốt trong hệ thống tài liệu)

| Thuật ngữ | Quyết định |
|---|---|
| **Tên sản phẩm** | `UTH Connect` — khớp `rootProject.name`, `applicationId = com.giathinh.uthconnect` và `res/values/strings.xml` |
| UTH Campus / CampusHub | tên cũ trong bản thảo, **không dùng trong code, tài liệu, UI** |
| Theme composable | `UTHConnectTheme(...)` trong `core/designsystem/theme/Theme.kt` |
| Prefix token tùy biến | `Uth*` — `UthNavy`, `UthGold`, `UthDimens.Spacing.Lg`, `UthTheme.extendedColors` |
| Package màu/chữ/khoảng cách | `core/designsystem/theme/` (Color.kt · Type.kt · Dimens.kt · Theme.kt) |
| Component dùng chung | `core/designsystem/component/` (UthCard · UthPrimaryButton · UthStatusBadge · UthEmptyState) |

**Nguyên tắc vàng:** trong Composable **không hardcode** `.dp`, `hex`, `.sp`. Mọi giá trị lấy qua token. Vi phạm = fail review.

---

## 1. Colors

### 1.1. Light (nền sáng — mặc định)

| Token (tên trong `Color.kt`) | Hex | Vai trò |
|---|---|---|
| `UthNavy` → `colorScheme.primary` | `#003366` | Nút chính, tab đang chọn, top bar, khung QR active |
| `White` → `onPrimary` | `#FFFFFF` | Chữ trên Navy — **12.6:1** |
| `PrimaryContainer` | `#E6EDF5` | Nền item đang chọn, indicator của bottom bar |
| `OnPrimaryContainer` | `#0A2A4A` | Chữ trên `PrimaryContainer` — **12.4:1** |
| `UthGold` → `colorScheme.secondary` | `#F5A623` | FAB, badge MVP, điểm nhấn — **chỉ làm nền** |
| `UthNavy` → `onSecondary` | `#003366` | Chữ Navy trên Gold — **6.2:1**. Cấm chữ trắng trên Gold |
| `UthGoldContainer` → `secondaryContainer` | `#FFF3DC` | Nền badge nhẹ |
| `UthGoldText` → `onSecondaryContainer` | `#7A4E00` | Chữ ánh vàng — **6.6:1** trên container |
| `Background` | `#FAFAF9` | Nền màn hình, tông ấm chống mỏi mắt |
| `Surface` | `#FFFFFF` | Card, BottomSheet, Dialog, Bottom bar |
| `SurfaceSubtle` | `#F0F3F6` | List item xen kẽ, vùng inactive, skeleton |
| `Outline` | `#D5DCE3` | Viền 1.dp của Card/TextField |
| `TextPrimary` | `#14213D` | Tiêu đề, nội dung quan trọng — **15.6:1** |
| `TextSecondary` | `#5A6B7C` | Mô tả, timestamp — **5.5:1** |
| `TextTertiary` | `#8C99A6` | **Chỉ** chữ disabled / trang trí — 2.9:1 |
| `Success` / `SuccessContainer` / `SuccessOnContainer` | `#10B981` / `#ECFDF5` / `#047857` | Check-in hợp lệ, đã trả đồ, đã nộp bài |
| `Warning` / `WarningContainer` / `WarningOnContainer` | `#F59E0B` / `#FFFBEB` / `#92400E` | Deadline ≤ 3 ngày, hết khung giờ quét |
| `Error(danger)` / `ErrorContainer` / `ErrorOnContainer` | `#EF4444` / `#FEF2F2` / `#B91C1C` | Quét sai mã, mất kết nối, vé không tồn tại |
| `Info` / `InfoContainer` / `InfoOnContainer` | `#0EA5E9` / `#F0F9FF` / `#075985` | Gợi ý, hướng dẫn lần đầu |

> Trung tính chuyển từ tông kẽm (`#F4F4F5`/`#E4E4E7`/`#18181B`) sang tông **xanh lạnh** để ăn khớp Navy. Đây là thay đổi duy nhất so với spec *CampusHub* về nền/viền.

### 1.2. Dark

| Token | Hex | | Token | Hex |
|---|---|---|---|---|
| `Background` | `#0B1622` | | `Primary` (Navy sáng hoá) | `#7FB2E5` |
| `Surface` | `#13202E` | | `OnPrimary` | `#062038` |
| `SurfaceSubtle` | `#1C2C3D` | | `PrimaryContainer` | `#143252` |
| `Outline` | `#33465C` | | `OnPrimaryContainer` | `#BBD8F5` |
| `TextPrimary` | `#EAF1F8` | | `Secondary` (Gold sáng hoá) | `#F5B740` |
| `TextSecondary` | `#A9B7C6` | | `OnSecondary` | `#0B1622` |
| `TextTertiary` | `#6B7A8A` | | `SecondaryContainer` / text | `#3A2C10` / `#FCD98A` |
| `Success` / container / text | `#34D399` / `#0E241C` / `#6EE7B7` | | `Warning` / container / text | `#FBBF24` / `#2A2110` / `#FCD34D` |
| `Error` / container / text | `#F87171` / `#2A1414` / `#FCA5A5` | | `Info` / container / text | `#38BDF8` / `#0D2436` / `#7DD3FC` |

`#003366` **không** dùng làm `primary` trong dark (quá tối trên nền tối) → dark tự động chuyển sang `#7FB2E5`.

### 1.3. Ngữ nghĩa theo module (không đổi màu thương hiệu)

Module **không** có màu riêng cho nút/bảng. Chỉ badge phân loại được dùng tint, và chỉ trong 5 trường hợp (tương ứng `enum BadgeTone`):

| Badge | `BadgeTone` | Nền | Chữ |
|---|---|---|---|
| `SV đã xác thực` | `Info` | `InfoContainer` | `InfoOnContainer` |
| `Gợi ý bởi AI` | `Neutral` | `SurfaceSubtle` | `TextSecondary` |
| `MVP` / module đang mở | `Accent` | `UthGoldContainer` | `UthGoldText` |
| `Đang mở / Còn chỗ / Đã điểm danh` | `Success` | `SuccessContainer` | `SuccessOnContainer` |
| `Sắp deadline / Hết hạn / Trùng scan` | `Warning` / `Danger` | `WarningContainer` / `ErrorContainer` | `WarningOnContainer` / `ErrorOnContainer` |

### 1.4. Tương phản đã đo (WCAG 2.1)

Tất cả tỷ số dưới đây tính theo công thức WCAG trên nền trắng (trừ cặp ghi rõ nền khác). Light đo trước, dark đo sau.

| Cặp | Ratio | Kết luận |
|---|---|---|
| `OnPrimary` trắng trên `UthNavy` (chữ nút chính) | **12.6:1** | Đạt AAA |
| `UthNavy` trên `UthGold` (chữ trên FAB/badge vàng) | **6.2:1** | Đạt AA — **đây là cách duy nhất được dùng Gold** |
| Trắng trên `UthGold` | **2.0:1** | ❌ cấm tuyệt đối |
| `UthGoldText` #7A4E00 trên `UthGoldContainer` #FFF3DC | **6.6:1** | Đạt AA (badge MVP) |
| `UthGold` làm **chữ** trên trắng | **2.0:1** | ❌ → dùng `UthGoldText` |
| `TextPrimary` #14213D trên `Surface` | **15.6:1** | Đạt AAA |
| `TextSecondary` #5A6B7C trên `Surface` | **5.5:1** | Đạt AA (body) |
| `TextTertiary` #8C99A6 trên `Surface` | **2.9:1** | ❌ **không đạt** → chỉ text disabled hoặc số trang trí ≥ 24.sp |
| `Success` #10B981 làm **chữ** trên trắng | **2.5:1** | ❌ → `SuccessOnContainer` #047857 (5.5:1 trên trắng, **5.2:1** trên container) |
| `Warning` #F59E0B làm **chữ** trên trắng | **2.2:1** | ❌ → `WarningOnContainer` #92400E (**6.8:1** trên container) |
| `Error` #EF4444 làm **chữ** trên trắng | **3.8:1** | ❌ body → `ErrorOnContainer` #B91C1C (**5.9:1** trên container) |
| Dark: `TextPrimary` #EAF1F8 trên #0B1622 | **15.8:1** | Đạt AAA |
| Dark: `Primary` #7FB2E5 trên #0B1622 | **8.1:1** | Đạt AAA |
| Dark: `OnPrimary` #062038 trên #7FB2E5 | **7.4:1** | Đạt AAA |
| Dark: `OnSecondary` #0B1622 trên #F5B740 | **10.0:1** | Đạt AAA |
| `UthNavy` làm viền/icon (không phải chữ) | ≥ 3:1 | Đạt UI component |

> **Kết luận quan trọng:** `Success`/`Warning`/`Error`/`Gold` **chỉ là màu nền, icon, viền**. Chữ bắt buộc dùng biến `*OnContainer`. Trạng thái không được truyền tải chỉ bằng màu — phải kèm icon + chữ (colour-not-only-meaning).

### 1.5. Dynamic color: mặc định tắt (đã áp dụng)

Với `minSdk = 31` (Android 12+), Material You sẽ **ghi đè toàn bộ** Navy/Gold bằng bảng màu do hình nền của thiết bị sinh ra, trên mọi thiết bị được hỗ trợ → design system này vô nghĩa nếu để bật.

`core/designsystem/theme/Theme.kt` hiện tại:

```kotlin
@Composable
fun UTHConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // ← giữ nhận diện thương hiệu + tương phản đã đo
    content: @Composable () -> Unit,
)
```

Tham số vẫn còn để một màn hình thử nghiệm *chủ động* bật, nhưng **không được bật ở mặc định ứng dụng**, và nếu bật phải đo lại tương phản.

---

## 2. Typography

Font: **Roboto** (khai sẵn của Material 3, không cần nhúng) — tương đương thị giác với Inter. Nếu chọn Inter: nhúng qua `res/font` và cấp quyền thương mại.

| Token M3 | Cỡ / Line-height / Trọng số | Letter-spacing | Dùng cho |
|---|---|---|---|
| `headlineLarge` | 32 / 40 / 600 | 0 | Tiêu đề trang chính, số đếm thống kê to |
| `headlineMedium` | 28 / 36 / 600 | 0 | Header tab khi có hero |
| `titleLarge` | 20 / 28 / 600 | 0 | **Tên sự kiện**, tiêu đề BottomSheet |
| `titleMedium` | 16 / 22 / 600 | 0.15 | Tên người đăng Lost & Found, **tên môn học** |
| `titleSmall` | 14 / 20 / 600 | 0.1 | Label nhóm mục, section header phụ |
| `bodyLarge` | 16 / 24 / 400 | 0.5 | Nội dung bài đăng, tin nhắn |
| `bodyMedium` | 14 / 20 / 400 | 0.25 | Mô tả phòng trọ, ghi chú môn học |
| `bodySmall` | 12 / 16 / 400 | 0.4 | Tooltip AI, vị trí địa lý, timestamp |
| `labelLarge` | 14 / 20 / 500 | 0.1 | **Chữ trên nút**, tag phân loại |
| `labelMedium` | 12 / 16 / 500 | 0.5 | Chip filter, số chỗ còn lại |
| `labelSmall` | 11 / 16 / 500 | 0.5 | Nhãn trong badge nhỏ (không dùng cho nội dung đọc dài) |

**Quy tắc**
- Cỡ chữ và line-height **đã chốt ở trên**; không dùng `.sp` lẻ, không tự thêm token mới khi chưa cập nhật file này.
- Tiếng Anh dài hơn tiếng Việt ~15–20 % → mọi text phải test với cả hai locale và với font scale 200 %.
- Text xuống dòng tối đa 3 dòng rồi `ellipsis` + "Xem thêm" cho mô tả dài (mô tả sự kiện, bài đăng).
- MSSV, số tiền, thời gian: dùng `FontFamily.Monospace` hoặc `tabularFigures` để cột thẳng hàng.
- Tiền: `1.500.000đ` (dấu chấm phân nghìn), không dùng `1.5tr` trong UI chính thức.

---

## 3. Layout

### 3.1. Lưới 8 pt

| Token | Giá trị | Dùng cho |
|---|---|---|
| `Xxs` | 2.dp | Viền trong, gap biểu tượng nhỏ |
| `Xs` | 4.dp | Icon ↔ text trong cùng một thẻ |
| `Sm` | 8.dp | Title ↔ subtitle |
| `Md` | 12.dp | Padding trong Card nhỏ, badge |
| `Lg` | 16.dp | Padding chuẩn của component và nút |
| `Xl` | 24.dp | **Lề ngang mọi màn hình** |
| `Xxl` | 32.dp | Khoảng giữa các section lớn |
| `Section` | 40.dp | Khoảng trước header nhóm mới |

### 3.2. Bo góc

| Token | Giá trị | Dùng cho |
|---|---|---|
| `Sm` | 4.dp | Tag, checkbox |
| `Md` | 8.dp | Nút, TextField |
| `Lg` | 16.dp | Card sự kiện, bài Lost & Found |
| `Xl` | 24.dp | BottomSheet, Dialog |
| `Full` | 50 % | Avatar; FAB dùng `CircleShape` của M3 |

**Không bo góc quá 24.dp cho phần tử chiếm toàn màn hình.** Card nằm trong `LazyColumn` có `Xl` lề → bo góc `Lg`, không dùng `Xl` lồng `Xl`.

### 3.3. Khu vực cố định

| Khu vực | Chuẩn |
|---|---|
| Top app bar | 64.dp cao, `titleLarge`, lùi `Xl` hai bên |
| Bottom navigation | 80.dp, nền `Surface`, **label luôn hiện** (hỗ trợ TalkBack); tab đang chọn: icon+chữ `primary` (Navy), indicator `PrimaryContainer` |
| FAB | 56.dp thường, 40.dp mini; cách đáy `Lg` trên top của bottom nav; nền `secondary` (Gold) + icon `onSecondary` (Navy) |
| Loading indicator | `UthDimens.Size.Progress` 40.dp đường kính, nét `ProgressStroke` 3.dp, màu `primary`; đặt cạnh chữ `bodyMedium` màu `textSecondary`, cách `Md`. **Không** dùng overlay chặn tương tác khi tải danh sách |
| Touch target | **≥ 48×48.dp** cho mọi phần tử bấm được (icon trong card cũng vậy) |
| Divider | 1.dp `Outline`, cách nội dung `Md` |
| Ảnh bìa sự kiện | tỉ lệ 16:9, bo góc `Lg`, mặc định `SurfaceSubtle` khi chưa tải xong |
| Khung QR trên vé | ≥ 220.dp vuông, nền trắng để tối đa tương phản với máy quét |

---

## 4. Components

### 4.1. Primary Button
- Cao 48.dp (`Size.ButtonHeight`), nền `primary` = **Navy `#003366`**, chữ `onPrimary` trắng `labelLarge` (12.6:1), bo góc `Md`, padding ngang `Lg`. Text trên nút viết hoa bằng `text.uppercase()` — không nhập hoa sẵn trong `strings.xml`.
- Trạng thái: pressed → state layer M3 8 % (không giảm opacity chữ); disabled → nền `surfaceVariant` + chữ `TextTertiary`; loading → spinner `onPrimary`, giữ nguyên chiều rộng, chặn chạm.
- **Một** primary button mỗi màn hình. Hành động phụ dùng Text button / Outlined.
- Nút vàng (chỉ dùng cho FAB): nền `secondary` Gold + chữ/icon `onSecondary` Navy. **Không** tạo Button vàng toàn màn hình — dễ nhầm với nút chính và tương phản chữ trắng không đạt.

### 4.2. Card (sự kiện, phòng trọ, sách, lost & found)
- Nền `Surface`, padding `Lg`, bo góc `Lg`, viền 1.dp `Outline` (không dùng elevation mạnh để phân cấp).
- Toàn card bấm được (Fitts), nhưng các control trong card (Lưu, Nhắn tin) có vùng chạm riêng ≥ 48.dp.
- Trạng thái: đang chọn / đã đăng ký → nền `PrimaryContainer`; đã kết thúc / đã bán → opacity 60 % + badge trạng thái.
- Skeleton khi tải: khối `SurfaceSubtle` cùng kích thước, animation `shimmer` 1.2 s.

### 4.3. Badge / Tag
- Cao ~24.dp (padding dọc `Xs` + `labelSmall`), padding ngang `Sm`, bo góc `Sm`, chữ `labelSmall`.
- Gọi qua **`UthStatusBadge(text, tone)`** với `tone: BadgeTone` (`Success` · `Warning` · `Danger` · `Info` · `Accent` · `Neutral`) — cặp màu nền/chữ lấy theo bảng 1.3, không truyền hex.
- Badge mặc định có icon (`Success` ✓, `Warning` ⚠, `Danger` ✕, `Info` ⓘ). `Accent`/`Neutral` bỏ icon khi chữ đã tự mô tả ("MVP", "Gợi ý bởi AI").
- Badge không được chứa quá 3 từ.

### 4.4. Filter Chip
- Cao 32.dp, bo góc `Sm`, không chọn: viền `Outline` + chữ `TextSecondary`; đang chọn: nền `PrimaryContainer` + chữ `primary` (Navy) + icon `✓` (icon bắt buộc, để không chỉ dựa vào màu).

### 4.5. TextField
- Cao 56.dp, bo góc `Md`, nền `Surface`, viền 1.dp `Outline`; focus: viền 2.dp `primary` (Navy) + label `bodySmall` màu `primary`; lỗi: viền `Error` + `ErrorOnContainer` helper **có icon**.
- Số điện thoại/giá: `keyboardType` đúng loại, không dùng validate chỉ bằng màu.

### 4.6. Ticket QR (màn `TicketQr`)
- QR ≥ 220.dp, nền trắng, margin 16.dp, không đặt trên ảnh nền.
- Đồng hồ đếm ngược hiệu lực token: `bodyMedium` `TextSecondary`; còn ≤ 10 giây → `WarningOnContainer` + tự làm mới.
- Trong lúc refresh: hiện `SurfaceSubtle` mờ + label "Đang làm mới mã…", **không** hiện QR cũ.
- Mã 6 số dự phòng: `TitleLarge` Monospace, chữ "Mã dự phòng (khi không quét được)".

### 4.7. Attendance Scanner (màn `AttendanceScanner`)
- Vùng camera toàn chiều rộng, overlay khung quét `primary` (Navy) 3.dp, bo góc `Lg`, 4 góc đậm.
- Kết quả hiện trong panel 88.dp dưới khung:
  - Hợp lệ → `SuccessContainer` + icon ✓ + `SuccessOnContainer`: "Đã điểm danh · Họ tên · MSSV · 07:04"
  - Quét trùng → `WarningContainer` + icon ! + `WarningOnContainer`: "Đã điểm danh lúc 07:01"
  - Không tồn tại / sai sự kiện → `ErrorContainer` + icon ✕ + `ErrorOnContainer`
- Phản hồi **không chỉ thị giác**: haptic `VibrationEffect` + âm "beep" khác nhau cho OK / Trùng / Lỗi.
- Bộ đếm phiên: `TitleLarge` — Đã quét / Trùng / Vắng, luôn hiển thị, cập nhật realtime.

### 4.8. Deadline item (module thời khóa biểu)
| Điều kiện | Màu | Nội dung bắt buộc |
|---|---|---|
| > 7 ngày | `TextSecondary` | "Còn N ngày" |
| ≤ 7 ngày | `WarningOnContainer` | "Còn N ngày" + icon đồng hồ |
| ≤ 48 giờ | `ErrorOnContainer` | "Sắp hết hạn" + thời gian cụ thể |
| Quá hạn | `ErrorOnContainer` + nền `ErrorContainer` | "Quá hạn N ngày" |
| Đã nộp | `SuccessOnContainer` + strikethrough nhẹ | "Đã nộp · ngày" |

### 4.9. Empty / Error state
- Minh hoạ 120.dp (`SurfaceSubtle`), title `TitleMedium`, mô tả `BodyMedium` `TextSecondary`, 1 primary action.
- Văn phong: nói nguyên nhân + cách xử lý ("Chưa có sự kiện nào ở cơ sở Thủ Đức · Thử đổi bộ lọc"), không dùng "Không có dữ liệu".

### 4.10. BottomSheet / Dialog
- Sheet: bo góc `Xl` trên, drag handle 32×4.dp `Outline`, content padding `Lg`, cao tối đa 80 % màn hình.
- Dialog: tối đa 2 action, action phá huỷ (xóa sự kiện, hủy đăng ký hàng loạt) dùng `ErrorOnContainer` và phải có bước xác nhận.

---

## 5. Motion

| Loại | Duration | Easing |
|---|---|---|
| Press/state layer | 100 ms | `FastOutSlowIn` |
| Chuyển tab/Route fade | 200 ms | `FastOutLinearIn` |
| BottomSheet mở | 250 ms | `Emphasized` |
| Refresh QR | 150 ms fade | `Linear` |

- Tôn trọng `Settings → Giảm chuyển động` (Android): mọi animation > 100 ms phải tắt khi `reduceMotion` bật.
- Không dùng animation để chờ dữ liệu thay vì skeleton.

---

## 6. Accessibility (bắt buộc, không phải nice-to-have)

| Yêu cầu | Chuẩn áp dụng |
|---|---|
| Tương phản chữ | ≥ 4.5:1 (body), ≥ 3:1 (≥ 18 pt / icon UI) — theo bảng 1.4 |
| Vùng chạm | ≥ 48×48.dp, gap ≥ 8.dp |
| TalkBack | Mọi icon-only button có `contentDescription`; QR có description "Mã vé sự kiện X, còn hiệu lực N giây"; kết quả quét đọc to trạng thái |
| Không chỉ bằng màu | Trạng thái luôn = icon + chữ (mục 1.4, 4.7, 4.8) |
| Font scale | Layout không vỡ ở 200 %; ảnh bìa co lại trước, text không co |
| Cảm ứng | Haptic cho kết quả check-in, không dùng khi scroll |
| Nội dung nhạy cảm | Ảnh CCCD/giấy tờ trong Lost & Found phải làm mờ trước khi đăng (mã hoá ở data layer) |
| Locale | Chuỗi ở `res/values/strings.xml` (vi) + `res/values-en/strings.xml` (en); không hardcode trong Composable |

---

## 7. Ánh xạ vào code (trạng thái đã kiểm chứng bằng build)

| Token | File | Trạng thái |
|---|---|---|
| Bảng màu light/dark | `core/designsystem/theme/Color.kt` | ✅ đã thay 6 màu template (`Purple80`/`PurpleGrey40`/…); thư mục `ui/theme/` cũ đã xoá |
| `Typography` 11 role | `core/designsystem/theme/Type.kt` → `UthTypography` | ✅ Roboto, có `Typography.tabularFigures` cho MSSV/giờ/số liệu |
| Spacing / radius / kích thước / motion | `core/designsystem/theme/Dimens.kt` → `UthDimens.Spacing·Radius·Size·Motion` | ✅ + `UthShapes` đưa vào `MaterialTheme.shapes` |
| `lightColorScheme` / `darkColorScheme` | `core/designsystem/theme/Theme.kt` | ✅ `dynamicColor = false` mặc định |
| Màu mở rộng (surfaceSubtle, textSecondary/Tertiary, 4 nhóm trạng thái, accent) | `Theme.kt` → `UthExtendedColors` + `LocalUthExtendedColors`, đọc qua `UthTheme.extendedColors` | ✅ một chỗ duy nhất, tự động đổi theo dark |
| Component dùng chung | `core/designsystem/component/UthComponents.kt` (`UthCard`, `UthSectionHeader`, `UthPrimaryButton`, `UthEmptyState`) | ✅ |
| Badge trạng thái | `core/designsystem/component/UthStatusBadge.kt` — `enum BadgeTone` | ✅ 6 tone theo mục 1.3 |
| Skeleton + shimmer (mục 4.2) | `core/designsystem/component/` | ⏳ chưa làm — cần màn hình tải dữ liệu đầu tiên |
| Bản mẫu một màn hình đạt checklist mục 8 | `presentation/utilities/` — `UtilitiesScreen` stateless nhận `UiState`, `UtilitiesRoute` nối `UtilitiesViewModel` | ✅ 8 `@Preview` (4 trạng thái × light/dark) + 4 unit test; `:app:assembleDebug`/`testDebugUnitTest` xanh. Màn hình mới copy cấu trúc này, không tự bày lại |
| Icon | `androidx.compose.material:material-icons-core:1.7.8` | ⚠ thư viện đã đóng băng (không cập nhật nữa); chỉ dùng cho khung nav + badge, icon thiết kế riêng sẽ vào `res/drawable` |

Toàn bộ màu phải đi qua `MaterialTheme.colorScheme.*`; token chưa có chỗ trong `ColorScheme` (VD `surfaceSubtle`, `textSecondary`, `successContainer`) lấy từ `UthTheme.extendedColors`. **Không rải hex, không tạo `Color(0x…)` trong Composable.**

---

## 8. Definition of Done cho UI

Checklist dưới đây kiểm tra **kết quả thị giác**; cách viết code để đạt được nó (lệnh grep quét literal, ranh giới tầng, quy ước token) ở `CODING_STANDARDS.md` mục 7 và mục 12.

- [ ] Không có `.dp`, `hex`, `.sp` hardcode trong Composable mới (ngoại trừ file token)
- [ ] `@Preview` cho 4 trạng thái: loading / empty / error / có dữ liệu, cả light + dark
- [ ] `@Preview(fontScale = 1.3f)` và test locale `en` không tràn/không cắt
- [ ] TalkBack đi hết luồng chính mà không gặp phần tử vô nghĩa
- [ ] Tương phản đạt theo bảng 1.4 (dùng plugin contrast checker trong Android Studio)
- [ ] Trạng thái có icon + chữ, không chỉ màu
- [ ] Ảnh/Figma khớp với spec này; lệch phải ghi chú ở PR

---

## 9. Đã chỉnh gì so với spec *CampusHub* gốc

| Spec gốc | Vấn đề | Bản này |
|---|---|---|
| Tên "CampusHub" / "UTH Campus" | lệch `applicationId` + `app_name` = UTH Connect | thống nhất **UTH Connect** (mục 0) |
| Chỉ có bảng màu sáng | `Theme.kt` đang có `DarkColorScheme` → thiếu spec | bổ sung bảng dark 1.2 |
| `Success/Warning/Error` dùng làm chữ | tương phản 2.2–3.8:1, **fail WCAG** | tách `*Bg` / `*Text` + bảng đo 1.4 |
| `TextTertiary` #A1A1AA | 2.5:1 | giới hạn cho disabled/trang trí |
| Không có letter-spacing | thiếu để implement đúng M3 | cấp letter-spacing từng role (mục 2) |
| Typography liệt kê 7 token | thiếu `headlineMedium`, `titleSmall`, `labelMedium`, `labelSmall` | đủ 11 role M3 |
| "Hover/Pressed giảm opacity 80 %" | Android không có hover; dễ thành trạng thái duy nhất | dùng state layer M3 + yêu cầu icon/chữ kèm màu |
| Radius FAB "Full 50 %" | FAB M3 đã là `CircleShape` | giữ `CircleShape`, bỏ token tự chế |
| Không đề cập dynamic color | `dynamicColor = true` sẽ **ghi đè** `PrimaryAccent` trên mọi thiết bị hỗ trợ (minSdk 31) | quy tắc tắt ở mục 1.5 |
| Không có motion, a11y, scanner spec | module QR check-in là trung tâm demo | thêm mục 4.6–4.7, 5, 6 |

### 9.1. Đã chỉnh ở v0.3 (chuyển sang nhận diện UTH)

| v0.2 | v0.3 | Lý do |
|---|---|---|
| `PrimaryAccent` `#4F46E5` (indigo) | `primary` **Navy `#003366`** | màu nhận diện trường UTH; trắng trên Navy 12.6:1 (tốt hơn indigo 6.3:1) |
| `Secondary` không dùng | **Gold `#F5A623`** cho FAB/badge nhấn | dùng được nhưng **bắt buộc** chữ Navy (6.2:1); trắng trên Gold chỉ 2.0:1 |
| Trung tính kẽm `#F4F4F5`/`#E4E4E7`/`#18181B` | Trung tính xanh lạnh `#F0F3F6`/`#D5DCE3`/`#14213D` | đồng tông với Navy, tương phản vẫn AAA/AA |
| Dark `Primary` `#A5B4FC` | Dark `Primary` `#7FB2E5` (Navy sáng hoá) | đo lại 8.1:1 trên nền `#0B1622` |
| Badge `VERIFIED`/`AI` dùng tint indigo | `BadgeTone` 6 giá trị, `Accent` =Gold cho MVP | khớp `UthStatusBadge` trong code |
| Spec trên giấy | Token + component đã compile (`:app:compileDebugKotlin` BUILD SUCCESSFUL) | tên API trong code là hợp đồng, đổi phải cập nhật mục 7 |

---

### 9.2. Đã bổ sung ở v0.3.1 (khi dựng sườn MVVM)

| Bổ sung | Lý do |
|---|---|
| Token `UthDimens.Size.Progress` (40.dp) và `ProgressStroke` (3.dp), ghi ở mục 3.3 | `UtilitiesScreen` cần indicator loading; không được viết `.dp` tự do trong Composable nên phải thành token |
| Dòng "Bản mẫu một màn hình" ở mục 7 | checklist mục 8 yêu cầu 4 trạng thái × light/dark; từ v0.3.1 đã có một màn hình thật đạt để làm chuẩn, thay vì chỉ mô tả trên giấy |

---

## 10. Câu hỏi còn mở

1. Chốt font **Roboto** (mặc định, không nhúng — đang dùng) hay **Inter** (nhúng `res/font`)? Ảnh hưởng tới metric line-height.
2. Navy `#003366` + Gold `#F5A623` đã lấy đúng bộ nhận diện chính thức của Trường/Đoàn–Hội chưa? Nếu có bản chuẩn khác, **chỉ đổi hex trong `Color.kt`**, không đổi cấu trúc token.
3. `material-icons-core` đã đóng băng — chấp nhận cho khung app, hay thay hết bằng vector drawable ngay từ đầu?
4. Admin Dashboard (web) dùng lại token này qua CSS variables hay thiết kế riêng theo thư viện của nó?
5. `#10B981`/`#F59E0B`/`#EF4444` là bảng Tailwind — có muốn đồng bộ sang Material 3 tonal palette (tone 40/80) để thống nhất với `ColorScheme` không?
6. Tab Cá nhân đã đăng nhập thì bottom nav giữ 4 tab hay thêm tab "Quản lý" cho organizer (hiện: organizer vào qua màn hình Quản lý trong tab Cá nhân — xem `Design.md` mục 8)?
