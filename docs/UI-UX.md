# UTH Connect — UI/UX Engineering Rules

> **Loại file:** quy chuẩn thiết kế và checklist triển khai cho người thiết kế, lập trình viên và AI agent.
> **Phạm vi:** Android (Kotlin, Jetpack Compose, Material 3), có thể tham khảo nguyên tắc UX cho admin dashboard.
> **Trạng thái:** checklist UX và accessibility bổ trợ cho Design System; tài liệu được duy trì dưới tên `UI-UX.md`.
> **Mục tiêu:** giao diện hiện đại, dễ hiểu, nhất quán, dễ truy cập, có chiều sâu vừa đủ, hoạt động tốt trong tình huống thực tế.

## 0. Cách sử dụng và thứ tự ưu tiên

Khi được giao **thiết kế màn hình, sửa UI, kiểm tra UX, viết Compose hoặc review giao diện**, đọc file này **trước khi thực hiện** và áp dụng checklist cuối file.

1. Yêu cầu trực tiếp của người dùng và nghiệp vụ trong `docs/Design.md`, `README.md` quyết định **chức năng và luồng**.
2. `docs/Design-system.md` cùng code thật trong `core/designsystem/theme/` quyết định **token và nhận diện**. Khi tài liệu và code khác nhau, kiểm tra hiện trạng rồi làm rõ/đồng bộ, không tự sáng tạo một palette song song.
3. `docs/CODING_STANDARDS.md`, các component hiện có và kiến trúc của repository quyết định **cách tổ chức code**.
4. File này quyết định **quy trình thiết kế, nguyên tắc chất lượng và tiêu chí kiểm tra**. Yêu cầu trợ năng/WCAG phải được đáp ứng; nếu style cũ gây vi phạm, đề xuất sửa token/thiết kế và nêu rõ chỗ xung đột.
5. Không sao chép nguyên văn, hình minh họa hay asset của hai sách nguồn. Chỉ chuyển hóa nguyên tắc thành thiết kế riêng cho UTH Connect.

## 1. Tư duy UX — thiết kế đúng vấn đề trước khi làm đẹp

**Mỗi màn hình phải trả lời được:** Ai sử dụng? Họ đang muốn làm gì? Hành động quan trọng nhất là gì? Tác vụ thành công trông như thế nào? Thất bại được xử lý ra sao?

- Chọn **một primary action** rõ ràng ở mỗi ngữ cảnh; hành động phụ giảm nhấn bằng text/outlined button. Không để 3–4 nút cùng độ nổi bật.
- Sắp thông tin theo mức độ quan trọng với người dùng, không theo độ dễ code hay sở thích trang trí: tiêu đề → dữ liệu quyết định → hành động → chi tiết bổ sung.
- Áp dụng **visual hierarchy, proximity, alignment, contrast, whitespace**: nhóm thông tin liên quan gần nhau, tách nhóm bằng khoảng trắng; canh theo cùng trục; giữ nhịp khoảng cách nhất quán.
- Ưu tiên **recognition over recall**: nhãn dễ hiểu, trạng thái rõ, filter đang chọn hiển thị, vị trí hiện tại có dấu hiệu; không buộc người dùng nhớ dữ liệu từ màn hình trước.
- Giảm **interaction cost**: ít bước, ít lần nhập lại, giữ lựa chọn khi quay lại, đưa thao tác phổ biến tới gần nội dung, tự điền khi có dữ liệu hợp lệ và quyền sử dụng.
- **Progressive disclosure**: chỉ hiển thị thông tin cần để quyết định ngay; chi tiết nâng cao mở khi có nhu cầu. Không giấu thông tin quan trọng để giao diện trông tối giản.
- Mỗi thao tác có phản hồi trong thời điểm hợp lý: pressed, đang xử lý, hoàn tất/thất bại; ngăn gửi nhiều lần khi request đang chạy.
- Không dùng dark pattern: không ép chấp thuận, không đánh tráo lựa chọn, không tạo khẩn cấp giả, không che hành động hủy/quay lại.
- Không thêm animation, gradient, ảnh hero, glass effect hoặc thẻ nổi chỉ để lấp khoảng trống.
- Trước khi dựng UI, viết luồng **happy path + loading + empty + error + offline + permission denied + retry**; bổ sung các trường hợp thực tế của tính năng.

### Gắn với UTH Connect

- **Sự kiện:** card cho biết *tên, ngày giờ, địa điểm/cơ sở, trạng thái đăng ký* trước thông tin phụ. CTA chính phù hợp trạng thái: `Đăng ký` / `Xem vé` / `Đã kết thúc`.
- **Vé/QR:** ưu tiên mã rõ nét, thời gian hiệu lực, trạng thái check-in và mã dự phòng theo tài liệu nghiệp vụ. Khi token hết hạn/đang làm mới, không hiển thị mã cũ như còn hợp lệ.
- **Máy quét:** phân biệt `thành công`, `đã quét`, `hết hạn`, `sai sự kiện`, `mất mạng` bằng **chữ + biểu tượng + phản hồi phù hợp**, không chỉ màu hoặc âm thanh.
- **Thời khóa biểu:** hiện thời điểm bắt đầu/kết thúc, phòng/cơ sở và deadline gần nhất, ưu tiên ngữ cảnh "Hôm nay".
- **Đồ thất lạc:** xem nhanh hình, địa điểm, thời gian, tình trạng; giảm rủi ro lộ thông tin cá nhân từ ảnh/giấy tờ.
- Không tự thêm tính năng, tab hoặc luồng nghiệp vụ ngoài `docs/Design.md` chỉ vì thấy một mẫu thiết kế đẹp.

## 2. Ngôn ngữ thị giác và design tokens

### 2.1. Nhận diện và màu sắc

| Vai trò | Dùng trong UTH Connect | Quy tắc |
| --- | --- | --- |
| Primary | Navy `#003366` (light) | CTA chính, điều hướng đang chọn, điểm nhấn nhận diện. |
| Accent | Gold `#F5A623` (light) | FAB, badge nhấn; **chữ/icon Navy**, tuyệt đối tránh chữ trắng trên Gold. |
| Background / Surface | `MaterialTheme.colorScheme.background/surface` | Tạo tầng nền có phân cấp; tránh nền trắng ngẫu nhiên trong dark mode. |
| Nội dung / Viền | `onSurface`, `outline`, `UthTheme.extendedColors` | Màu theo **vai trò ngữ nghĩa**, không lấy màu trực tiếp từ `Color.kt` trong từng màn. |
| Success / Warning / Error / Info | `UthTheme.extendedColors.*Container` + `on*Container` | Ghép icon và chữ; không dùng màu thuần có độ tương phản kém cho body text. |

- Tái sử dụng `UTHConnectTheme`, `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `UthTheme.extendedColors`.
- `dynamicColor` đang mặc định **false** vì nhận diện UTH; chỉ bật sau khi kiểm tra lại các cặp tương phản và phê duyệt thay đổi.
- Thiết kế **cả light/dark** với semantic roles thay vì đảo màu thủ công. Màu trạng thái phải nhất quán giữa các module.
- Không đưa hex tùy ý, `Color(0x...)`, `.sp`, `.dp` tùy ý vào Composable; nếu thực sự thiếu token, đề xuất bổ sung vào theme/design spec trước.
- Sử dụng tối đa 1 màu nhấn mạnh chủ đạo trong một vùng, tránh nhiều accent cạnh tranh. Gradient dùng rất tiết chế, chỉ khi có tác dụng phân tầng nội dung và không hạ tương phản.

### 2.2. Khoảng cách, lưới và mật độ

**Dùng token đã có:** `UthDimens.Spacing.Xxs=2`, `Xs=4`, `Sm=8`, `Md=12`, `Lg=16`, `Xl=24`, `Xxl=32`, `Section=40` (dp).

- Ưu tiên nhịp **8dp**; dùng 4/12dp cho điều chỉnh nhỏ theo vai trò. Không tạo gap 7, 13, 19dp chỉ vì nhìn "vừa mắt".
- Màn hình điện thoại: lề ngang theo token `Xl` (=24dp) của dự án, trừ khi component đã xử lý hoặc trải ảnh sát mép theo thiết kế.
- Nội dung trong card: padding chuẩn `Lg` (=16dp). Khoảng giữa các nhóm độc lập lớn hơn khoảng giữa label và giá trị.
- Căn theo baseline/đường biên ổn định. Canh trái nội dung tiếng Việt dài; canh phải số trong bảng; không center paragraph dài.
- Dùng đường viền/divider có mục đích. Khoảng trắng phải tạo nhịp đọc, không biến thành vùng trống lãng phí.
- Không cố định chiều cao ô chứa đoạn chữ có thể tăng khi font scale; ưu tiên `wrapContentHeight` và scroll hợp lý.

### 2.3. Bo góc (corner radius)

**Tuân thủ `UthDimens.Radius` và `UthShapes`:**

| Token | Giá trị | Thành phần chuẩn |
| --- | --- | --- |
| `Sm` | 4dp | Badge nhỏ, tag, checkbox. |
| `Md` | 8dp | Nút, text field, input. |
| `Lg` | 16dp | Card, ảnh trong thẻ, khối nội dung. |
| `Xl` | 24dp | Bottom sheet, dialog/surface lớn theo spec. |
| `CircleShape` | Tròn | Avatar và hình tròn chuyên dụng; không tạo radius 999dp tùy ý. |

- Bo góc **theo chức năng và kích thước**; không dùng 24dp cho mọi card rồi lồng 3 lớp card 24dp.
- Trong nested surfaces, bán kính ngoài phải có tương quan với padding/bán kính trong; tránh viền "đứt", mất góc hoặc lệch clipping.
- Ảnh phải được clip cùng shape của container; shadow đặt trên **surface** bao ngoài, không đổ bóng riêng lên text/icon.
- Cân nhắc `MaterialTheme.shapes.medium` / token của dự án trước khi tạo `RoundedCornerShape` mới.
- Không dùng bo góc để che thiếu phân cấp, tương phản hoặc khoảng trắng.

### 2.4. Shadow, elevation và chiều sâu

- Phân biệt **`tonalElevation`** (điều chỉnh sắc độ bề mặt) với **`shadowElevation`** (độ cao tạo bóng); đây là hai cơ chế khác nhau của Material 3 `Surface`.
- Dùng `UthDimens.Elevation.Card = 1dp` và `BottomBar = 2dp` hiện có; card thường **ưu tiên outline 1dp + shadow rất nhẹ/không bóng**. Tránh shadow đậm đồng loạt.
- Shadow rõ hơn chỉ phù hợp cho thành phần **nổi tạm thời** (dialog, menu, sheet, floating control) khi thực sự cần tách khỏi background. Chưa có token cho các mức đó: bổ sung vào `UthDimens.Elevation` và design spec trước khi dùng.
- Trong dark theme, ưu tiên surface tone và viền nhẹ để phân lớp. Đừng dùng một lớp bóng đen dày làm phương tiện phân cấp duy nhất.
- Bóng phải theo shape, không bị parent clip sai, không làm chữ/viền mờ. Tránh đổ bóng nhiều lớp, neon glow và floating card không có lý do UX.
- **Không dùng shadow thay cho focus/selection state**; trạng thái tương tác cần indicator, semantic state và tương phản rõ.
- Glassmorphism/blur chỉ sử dụng khi nội dung phía sau không làm giảm readability, có phương án opaque/high contrast và có lợi ích UX cụ thể.

### 2.5. Typography và iconography

- Font hệ thống hiện dùng **Roboto**, thang chữ dùng `UthTypography` trong `Type.kt`.
- Sử dụng `headline*` cho tiêu đề lớn hiếm gặp; `title*` cho tên/section; `body*` cho nội dung; `label*` cho controls. Không dùng tiêu đề lớn trên từng item.
- Text chính có thể chọn `bodyLarge` (16sp / 24sp) khi phải đọc lâu. Text 11–12sp chỉ dùng nhãn phụ, không để nội dung quan trọng đọc bằng chữ tí hon.
- Tôn trọng **font scaling từ hệ thống**, không khóa fontScale; dòng, container, icon và touch target phải thích nghi.
- Tiếng Việt có dấu cần line-height đủ rộng và không cắt dấu; thử tên dài, mã số dài, ngày tháng, empty state dài, locale tiếng Anh.
- Dùng `Typography.tabularFigures` hoặc font hỗ trợ số đều cột cho MSSV, số liệu, giá và thời gian.
- Icon cùng hệ nét (stroke), kích thước/optical alignment nhất quán. Icon phải **có nhãn/ý nghĩa** khi cần, không sử dụng biểu tượng mơ hồ thay cho chữ.
- Nút chủ đạo theo cách viết hiện hành trong `docs/Design-system.md`; kiểm tra đọc hiểu, locale và TalkBack trước khi biến đổi chữ hoa.

## 3. Navigation, responsive và adaptive layout

- Mobile-first nhưng **không khóa theo một kích thước máy**. Kiểm tra cửa sổ compact/medium/expanded (portrait, landscape, split screen, tablet, foldable nếu hỗ trợ).
- Thiết kế theo **kích thước cửa sổ hiện tại**, không phỏng đoán chỉ từ model thiết bị. Nếu cần Material 3 Adaptive API, kiểm tra dependency đang có trước khi thêm.
- Với cửa sổ rộng, dùng max content width, danh sách–chi tiết hoặc nhiều cột khi có lợi; đừng kéo giãn card 1 cột tới hết chiều ngang tablet.
- Top bar/bottom navigation theo `UthDimens.Size.TopBar` và `BottomBar` hiện có; nhãn tab chính luôn rõ nghĩa. Không vượt quá số điểm điều hướng mà tài liệu nghiệp vụ xác định.
- Áp dụng `WindowInsets` / `Scaffold` padding đúng cách để nội dung không bị status bar, navigation bar, bàn phím hoặc display cutout che; không cộng padding hệ thống hai lần.
- Khi mở keyboard, CTA nhập liệu và lỗi phải nhìn thấy hoặc cuộn tới được; không che text field ở cuối màn.
- Screen state cần khôi phục khi quay lại nếu hợp lý (scroll, filter, form draft), tránh reset chỉ vì recomposition.
- Không lồng nhiều vùng scroll cùng chiều khiến cuộn kẹt; danh sách dài dùng lazy list, key ổn định.

## 4. Accessibility — tiêu chuẩn chặn merge

**Mục tiêu:** áp dụng WCAG 2.2 AA như bộ nguyên tắc kiểm tra có thể chuyển sang native UI và các hướng dẫn Android Accessibility/Material 3 tương ứng. WCAG định nghĩa target size bằng CSS pixels cho web; với **Android native**, UTH Connect áp dụng chuẩn riêng **48×48dp**.

| Kiểm tra | Mức yêu cầu |
| --- | --- |
| Chữ thông thường | **≥ 4.5:1** trên nền thực tế ở cả light/dark. |
| Chữ lớn (≥24px thường hoặc ≥18.66px đậm, tương đương tiêu chí WCAG) | **≥ 3:1**; không dùng ngoại lệ này tùy tiện cho body text. |
| Icon mang thông tin, border của control, focus indicator | **≥ 3:1** với màu liền kề khi tiêu chí WCAG áp dụng. |
| Touch targets Android | Vùng tương tác **≥ 48×48dp**, kể cả icon nhỏ/chip; tránh vùng chạm chồng lấn. |
| Dynamic text | Không cắt, mất chức năng hay giấu action tại **font scale 200%** (kiểm tra thực tế). |
| Screen reader | TalkBack đọc đúng nhãn, vai trò, trạng thái, thứ tự; mọi tác vụ chính dùng được. |
| Điều hướng ngoài cảm ứng | Hỗ trợ focus bàn phím/D-pad/hardware input nơi thích hợp; luôn nhìn thấy focus. |
| Motion | Giảm/tắt chuyển động không thiết yếu khi hệ thống yêu cầu, không tạo nhấp nháy/scroll tự động gây khó chịu. |

**Quy tắc bắt buộc:**

1. Icon trang trí: `contentDescription = null`; icon-only action: label rõ bằng `stringResource(...)`. Không gán description trùng lặp cho icon có Text ngay cạnh nếu khiến TalkBack lặp.
2. Với nút tùy biến dùng role, state và `Modifier.semantics { ... }`/`stateDescription` phù hợp; nhóm semantics cẩn thận. **Không dùng `clearAndSetSemantics` trên container** nếu làm mất hành động của con.
3. Selected/checked/expanded/error/disabled không chỉ có màu; kèm trạng thái trực quan và semantic. Focus rõ ràng, không bị sheet/header che.
4. Live updates (ví dụ kết quả quét, hoàn thành đăng ký) cần announcement hoặc live region khi thích hợp, không spam TalkBack theo từng giây countdown.
5. Không ẩn nhãn input chỉ bằng placeholder; TextField có label thật và thông báo lỗi cụ thể gắn với trường cần sửa.
6. Error phải nói **điều gì sai + cách sửa**, ở nơi dễ tìm; không chỉ rung/đổi viền đỏ. Hành động nguy hiểm cần confirm rõ hậu quả.
7. Không dùng swipe/drag/pinch là **cách duy nhất** để thực hiện một thao tác; cung cấp nút tương đương.
8. Ảnh có ý nghĩa cần nội dung thay thế có ích; ảnh trang trí không đọc. QR cần cả nhãn và phương án thay thế theo nghiệp vụ.
9. Không tự tăng mật độ bằng cách giảm font, vùng chạm hoặc tương phản khi màn hình hẹp.
10. Kiểm tra ở chế độ grayscale, tăng kích thước chữ, TalkBack và dark mode; trạng thái quan trọng vẫn phải phân biệt được.

**Các cặp màu dễ sai trong dự án:** không dùng chữ trắng trên `UthGold`, Gold trên trắng, `TextTertiary` cho nội dung cần đọc, hoặc màu `success/warning/error` thuần làm body text trên nền trắng. Dùng `on*Container` với container phù hợp, kiểm tra contrast thực tế của từng cặp. Với `onError`, kiểm tra lại nếu đặt chữ nhỏ trực tiếp trên nền error thuần.

## 5. Thành phần và interaction states

Mọi component tương tác cần định nghĩa ít nhất: **default, pressed/focused, disabled, loading, success, error**; thêm **selected/expanded/empty** khi có ý nghĩa.

| Thành phần | Quy tắc thiết kế và hành vi |
| --- | --- |
| Primary button | Tối thiểu 48dp vùng chạm; một CTA nổi bật; nhãn là động từ rõ ràng; loading giữ width, chống click trùng. Tái sử dụng `UthPrimaryButton`. |
| Cards | Bo 16dp, padding 16dp, viền nhẹ, shadow tiết chế; vùng bấm rõ và tách action phụ để tránh click nhầm. Tái sử dụng `UthCard`. |
| Text fields | Label/hint/helper phân cấp rõ; bàn phím và IME action đúng; lỗi tại chỗ, giữ dữ liệu đã nhập; không validate gây gián đoạn khi người dùng chưa xong. |
| Chips/filters | Selected có icon/dấu hiệu bổ sung màu; hiển thị cách xóa filter; visible chip có thể thấp nhưng hitbox ≥48dp. |
| Status badges | Dùng `UthStatusBadge(text, tone)`; ngữ nghĩa nhất quán; không biến badge thành nút nếu thiếu affordance. |
| Bottom sheet/dialog | Focus, dismiss và back handling rõ; action phá hủy tách khỏi action thường; không che nội dung thiết yếu hoặc phím hệ thống. |
| Empty state | Nói vì sao chưa có nội dung + bước tiếp theo, không chỉ ghi "Không có dữ liệu". Tái sử dụng `UthEmptyState`. |
| Error state | Mô tả lỗi hữu ích + `Thử lại` hoặc phương án thay thế; giữ phần dữ liệu trước đó nếu vẫn hợp lệ. |
| Loading | Chỗ tải có skeleton phù hợp cấu trúc; không phủ modal blocking toàn màn hình khi chỉ đang refresh danh sách. |
| Snackbars/feedback | Một thông điệp ngắn, có action nếu cần; feedback thành công không ngắt luồng. Lỗi nghiêm trọng có nơi hiển thị ổn định. |
| Images | Tỷ lệ hợp lý, crop có chủ đích, placeholder ổn định để tránh layout shift; nội dung chữ không đặt trực tiếp lên ảnh mà không có lớp bảo đảm tương phản. |

**Microcopy:** ưu tiên tiếng Việt tự nhiên, ngắn, cụ thể (`Đã đăng ký thành công`, `Mã đã hết hạn — đang tạo mã mới`, `Không thể kết nối. Thử lại`). Không trách người dùng, không dùng thông báo lỗi chung chung khi có nguyên nhân xác định.

## 6. Motion và phản hồi

- Bám `UthDimens.Motion`: press **100ms**, route fade **200ms**, sheet **250ms**, QR refresh **150ms**, shimmer **1200ms** theo spec hiện có.
- Animation phải giải thích thay đổi trạng thái, duy trì ngữ cảnh hoặc phản hồi thao tác. Không animate trang trí vô tận; tránh chuyển cảnh chậm làm mất cảm giác phản hồi.
- Với thông tin nhạy thời gian (QR, scanner), hiệu lực và tính đúng phải đến từ state/logic; hiệu ứng hình ảnh không được tạo ảo giác token còn hiệu lực.
- Dùng haptic/âm thanh theo trạng thái khi phù hợp, nhưng luôn có phản hồi **bằng chữ và biểu tượng**; cung cấp khả năng sử dụng khi tắt âm/rung.
- Kiểm tra `animator_duration_scale` / cài đặt giảm chuyển động được hỗ trợ bởi nền tảng, tránh phụ thuộc duy nhất vào animation để báo trạng thái.
- Tối ưu animation cho máy tầm trung; tránh blur động hoặc shadow nặng gây drop frame trong danh sách.

## 7. Triển khai Jetpack Compose — tránh dựng lại component đã có

1. Trước khi sửa màn hình, kiểm tra `core/designsystem/component/`, `core/designsystem/theme/`, `docs/Design-system.md`, screen liên quan và `UiState`/`ViewModel` hiện tại.
2. Reuse `UthCard`, `UthPrimaryButton`, `UthStatusBadge`, `UthEmptyState`, `UthPreview` khi đúng nhu cầu. Chỉ tách component mới nếu được tái sử dụng hoặc thực sự giảm độ phức tạp.
3. UI hiển thị theo **state**; business logic, network, validation nghiệp vụ ở ViewModel/use case. Route nối ViewModel; screen nhận `UiState` và callbacks, thuận tiện `@Preview`/test.
4. Không đưa logic chạy mạng, tạo UUID, format ngẫu nhiên hoặc mutate state vào body composable. Không tạo nhiều nguồn sự thật cho selection/loading.
5. Khi cần shadow, dùng thuộc tính M3 như `Surface(shadowElevation = ..., tonalElevation = ...)` hoặc elevation đúng của `Card`; **dùng token**. Không hardcode blur `graphicsLayer`/Canvas cho card thông thường.
6. Khi cần touch target lớn cho icon 24dp, dùng `IconButton` hoặc `Modifier.sizeIn(minWidth = UthDimens.Size.MinTouchTarget, minHeight = UthDimens.Size.MinTouchTarget)` trên đúng vùng click, tránh chồng lên action khác.
7. Dùng `stringResource` cho text, `contentDescription` và lỗi; giữ hỗ trợ ngôn ngữ đã có. Không hardcode text tiếng Việt trong màn khi đã có localization.
8. Sử dụng `Scaffold`/insets, lazy list + stable key, state hoisting và immutable state phù hợp; không xây lại hệ thống điều hướng, button, shadow, ripple hay spacing.
9. Nếu cần API mới (Material 3 Adaptive, animation, icon), **đọc tài liệu Android hiện hành và kiểm tra dependency/version** của dự án trước khi giới thiệu.
10. Giữ backward compatibility với UI đang dùng; mọi thay đổi theme dùng chung phải preview/test các module liên quan.

### Mẫu Compose minh họa token và elevation (chỉ dùng khi component dùng chung không đáp ứng)

```kotlin
Surface(
    shape = MaterialTheme.shapes.medium, // bo 16dp từ UthShapes hiện tại
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = UthDimens.Elevation.Card,
    shadowElevation = UthDimens.Elevation.Card,
    border = BorderStroke(
        UthDimens.Size.Stroke,
        MaterialTheme.colorScheme.outline,
    ),
) {
    // Nội dung dùng UthDimens.Spacing.Lg và MaterialTheme.typography.*
}
```

**Lưu ý:** Mẫu minh họa không phải lời khuyên dùng đồng thời cả tonal + shadow elevation trên mọi card. Chọn mức phù hợp với nền thực tế; mặc định ưu tiên `UthCard` của dự án.

## 8. Quy trình bắt buộc khi được yêu cầu thiết kế / sửa UI

**Bước 1 — Hiểu:** mô tả tác vụ chính, người dùng, dữ liệu cần quyết định, luồng, giả định, giới hạn nghiệp vụ.

**Bước 2 — Audit:** tìm trong UI/code hiện tại các lỗi về hierarchy, navigation, affordance, trạng thái, tính nhất quán, khả năng đọc, accessibility; ưu tiên lỗi **P0: không hoàn thành tác vụ / mất dữ liệu / không truy cập**, **P1: dễ nhầm / thiếu phản hồi / layout vỡ**, rồi mới **P2: thẩm mỹ**.

**Bước 3 — Thiết kế:** nêu cấu trúc thông tin và 1 CTA chính; đề xuất component, token, bo góc, shadow, empty/loading/error, adaptive layout, TalkBack. Nếu có phương án khác, giải thích trade-off.

**Bước 4 — Implement:** thay đổi nhỏ, tái sử dụng design system, tránh hardcode, giữ behavior nghiệp vụ. Không cài dependency hay tạo framework mới nếu component đang có đáp ứng.

**Bước 5 — Verify:** preview light/dark, font 200%, nội dung dài, vùng chạm, focus/TalkBack, loading/error/offline, orientation/window resize; build/test phù hợp thay đổi và kiểm tra trực quan screenshot khi có.

**Bước 6 — Handoff:** mô tả **đã thay đổi gì, vì sao tốt hơn cho người dùng, file/komponent nào, kết quả kiểm tra, rủi ro còn lại**. Không nói "đã đạt WCAG" nếu chưa kiểm tra đủ.

## 9. Definition of Done / review gate

Không coi UI là hoàn tất cho đến khi kiểm tra được các điều sau:

- [ ] Có một mục tiêu và CTA chính rõ ràng; luồng hoàn thành được trong ít bước cần thiết.
- [ ] Typography, spacing, shape, color, elevation đúng token UTH; không hardcode mới trong Composable.
- [ ] Không có card lồng card, bo góc vô tội vạ, shadow quá mạnh, gradient/blur gây rối.
- [ ] Light/dark có phân cấp rõ; đo tương phản theo **nền thực tế** đạt yêu cầu.
- [ ] Mọi target Android ≥48×48dp; các touch targets không chồng lấn.
- [ ] TalkBack đọc đúng tên, vai trò, trạng thái và thứ tự, sử dụng được tác vụ chính.
- [ ] Focus nhìn thấy; status/error/selected có chữ hoặc icon thay vì chỉ màu.
- [ ] Font 200%, tiếng Việt dài, locale tiếng Anh (nếu được hỗ trợ), màn nhỏ và rộng không mất nội dung hoặc action.
- [ ] Insets, keyboard, bottom nav và chế độ landscape không che CTA hoặc nội dung chính.
- [ ] Đủ `loading / empty / error / success / offline`, hành động retry và chống submit lặp phù hợp.
- [ ] Motion không gây khó chịu, tôn trọng tùy chọn giảm chuyển động; feedback không chỉ bằng haptic/âm thanh.
- [ ] Có `@Preview` cho state trọng yếu, light/dark; cập nhật test khi behavior thay đổi.
- [ ] Không tạo component/dependency/logic trùng lặp; UI state tách khỏi business logic.
- [ ] Không làm sai nghiệp vụ, đặc biệt thời hạn QR, check-in trùng, trạng thái đăng ký và deadline.
- [ ] Reviewer có thể đối chiếu thay đổi với spec, screenshot và kết quả kiểm tra thực tế.

## 10. Nguồn, phạm vi áp dụng và cập nhật

**Hai tài liệu nền tảng (đã đối chiếu mục lục và các phần liên quan; số trang là trang in/PDF):**

- *App Design Apprentice, 2nd Edition*, Prateek Prasad (2022), file người dùng: `D:\Sách android bản quyền\app-design-apprentice-2nd.pdf`. Tham chiếu **Ch. 4** wireframe/screen layout (tr. 80–109), **Ch. 5** reusable elements (110–154), **Ch. 6** typography (155–177), **Ch. 7–8** color/design systems (178–220), **Ch. 9** transitions (222–251), **Ch. 10** feedback/testing (252–281).
- *UXPeak UI/UX Playbook*, file người dùng: `D:\Sách android bản quyền\uxpeak-ui-ux-playbook.pdf`. Tham chiếu hierarchy (trang in 6–11), proximity (12), clarity (13–15), alignment (16–19), contrast (20–21), whitespace/layout (24–34), visual cues (46–48), depth/shadow (49–64), color (65–70), typography (71–96), interaction cost (97–108), các mẹo về confirmation, forms và error states (109+). **Trang in của sách bắt đầu tại PDF page 2**, tức số trang in +1 ≈ số trang PDF.

**Nguồn chuẩn hiện đại (đọc bản mới nếu API/chuẩn thay đổi):**

- W3C WCAG 2.2: https://www.w3.org/TR/WCAG22/ — contrast, non-text contrast, focus, dragging alternative, accessible authentication, target size (web).
- Android Accessibility: https://developer.android.com/develop/ui/compose/accessibility — semantics, TalkBack, touch targets ≥48dp và kiểm tra thực tế.
- Android adaptive apps: https://developer.android.com/develop/adaptive-apps — window size classes, layout theo kích thước cửa sổ.
- AndroidX Material 3 Compose API: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary — `MaterialTheme`, `Surface`, `Card`, semantics và elevation.

**Diễn giải nguồn:** hai sách dạy nguyên tắc thiết kế, **không mặc nhiên là tiêu chuẩn kỹ thuật mới nhất**. Giá trị số và lựa chọn implementation được chốt bằng design system hiện có, hướng dẫn Android và quy chuẩn accessibility cập nhật; không chép nguyên layout, asset hoặc mã nguồn bản quyền của sách.

**Cập nhật quy chuẩn:** nếu xuất hiện component, radius/elevation hoặc palette mới, cập nhật `docs/Design-system.md` cùng token implementation và test trước khi coi chúng là chuẩn sử dụng chung.