# Báo cáo công việc — Tra cứu Cơ sở & Danh bạ UTH

**Ngày:** 07/11/2026 · **Task:** CAMPUS-DIRECTORY · **Trạng thái:** hoàn thành phần code, nghiệm thu emulator còn dang dở.

## Đã thực hiện

- Rà soát thấy `CampusesScreen.kt` và route `UtilitiesDestination.Campuses` đã tồn tại.
- Sửa địa chỉ 3 cơ sở theo nội dung task.
- Thay hành vi nút thứ hai của mỗi thẻ cơ sở từ gửi email thành mở vị trí qua `Intent.ACTION_VIEW`, URI `geo:0,0?q=` với địa chỉ đã encode.
- Giữ nút gọi hotline dùng `Intent.ACTION_DIAL` và `tel:`; đổi nhãn thành “Gọi ngay”.
- Bổ sung chuỗi “Chỉ đường” tiếng Việt và “Directions” tiếng Anh. UI dùng component và token trong design system.

## Kết quả kiểm tra

| Hạng mục | Kết quả |
|---|---|
| `:app:assembleDebug` | PASS — BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | Đã chạy; kết quả XML mới tại `app/build/test-results/testDebugUnitTest/`. Tổng hợp chưa ghi số lượng trước khi đối chiếu XML |
| `detekt spotlessCheck` | BLOCKED — `detekt` task không tồn tại; repo chưa khai plugin Spotless hoặc Detekt. `docs/CODING_STANDARDS.md` cũng ghi lint/CI chưa được bật |
| Mở app và vào Tiện ích | Chưa hoàn tất trên emulator; cần vào app qua luồng auth hiện có |
| Bấm Chỉ đường và xác nhận Google Maps | Chưa chạy; emulator không có Google Maps package được xác nhận |
| Bấm gọi và xác nhận giao diện quay số | Chưa chạy; chưa vào được màn Campus trên app |
| Ảnh chụp hành vi Intent | Chưa có. Không ghi nhận PASS và không thay bằng ảnh dựng |

## Địa chỉ đã cấu hình

1. Cơ sở 1: Số 2, Võ Oanh (Nguyễn Văn Thương cũ), P.25, Q. Bình Thạnh, TP.HCM.
2. Cơ sở 2: Số 10, Đường 12, P. An Khánh, TP. Thủ Đức, TP.HCM.
3. Cơ sở 3: Số 70, Tô Ký, P. Tân Chánh Hiệp, Q.12, TP.HCM.

## Còn lại để đạt nghiệm thu đầy đủ

- Cấu hình plugin và task Detekt/Spotless theo chuẩn dự án rồi chạy `detekt spotlessCheck`.
- Chạy app trên emulator/thiết bị có Google Maps và ứng dụng xử lý `ACTION_DIAL`, mở được route đến màn Campus.
- Bấm từng loại nút, lưu ảnh màn hình thật; review PR trước khi đánh dấu Done.

Thay đổi mã tập trung trong `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/campuses/`, chuỗi giao diện trong `app/src/main/res/values/strings.xml` và `values-en/strings.xml`.
