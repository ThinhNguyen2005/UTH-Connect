# KAN-111 — Audit Phase 1 sau merge

## 1. Tóm tắt

Audit commit `a54f5b6` của `origin/develop`, ngày 14/11/2026. Bốn lệnh build, Detekt, Spotless và unit test đều thoát mã 0. Detekt có baseline 476 ID và báo 0 finding ngoài baseline. XML unit test ghi 414 test, 0 failure, 0 error, 0 skipped trên 48 suite. Tổng finding khi không có baseline chưa được đo trên commit này.

Ba vấn đề cần xử lý: baseline lớn; CI hiện có chỉ chạy build và test theo thứ tự, thiếu Detekt/Spotless; workflow mô tả fallback Firebase mặc định trong khi file cấu hình đã ngừng theo dõi trên Git.

## 2. Môi trường

Temurin JDK 21.0.7, Gradle 9.8.2, AGP 9.3.3, Kotlin 2.4.21, module `:app`; Detekt 1.23.8 và Spotless 8.11.0. Detekt dùng `config/detekt/detekt.yml`, baseline `config/detekt/baseline.xml`, mặc định cộng cấu hình bổ sung và plugin Compose. Spotless dùng ktlint.

## 3. Kết quả kiểm tra

| Lệnh | Mã thoát | Kết quả |
| --- | ---: | --- |
| `assembleDebug` | 0 | Build thành công trong 4 phút 15 giây |
| `detekt` | 0 | Chạy thực tế với baseline; XML rỗng |
| `spotlessCheck` | 0 | Chạy thực tế, không báo file vi phạm |
| `testDebugUnitTest` | 0 | Chạy thực tế; 414 test, 0 lỗi, 0 bỏ qua |

Gradle cảnh báo tính năng deprecated trước Gradle 10; `checkDebugAarMetadata` được cấu hình bỏ qua. Build thiếu file Firebase và do đó cấu hình plugin Firebase theo điều kiện trong Gradle có thể khác với build có file; không suy ra Firebase đã được kiểm thử.

## 4. Detekt

Baseline hiện chứa 476 ID, nhóm theo rule: MaxLineLength 232; UnusedPrivateMember 81; MagicNumber 79; TooManyFunctions 19; ReturnCount 12; TooGenericExceptionCaught 12; CyclomaticComplexMethod 11; LongMethod 8; MatchingDeclarationName 8; ComplexCondition 5; UseCheckOrError 4; UnusedPrivateProperty 2; SwallowedException 2; LongParameterList 1. Đây là số ID, không phải số vị trí finding thực tế.

Các rule LongMethod, LongParameterList, CyclomaticComplexMethod và FunctionNaming bỏ qua `Composable`; MagicNumber bỏ qua khai báo property, companion property và `Composable`/`Preview`; TooGenericExceptionCaught bỏ qua `Composable`. Các rule Compose được bật. Báo cáo Detekt hiện tại không cho biết vị trí finding bị che, số finding thực, ID lỗi thời, phân bố theo tầng hoặc top 15 file.

## 5. Phân loại rủi ro

Định dạng: thấp nếu không đổi biểu thức; hằng số: thấp đến trung bình vì có giới hạn nghiệp vụ; tách hàm và đổi tên: trung bình; điều kiện, exception, coroutine và repository: cao. Test có ở ViewModel, use case và repository nhưng chưa đo coverage cho từng finding. Preview Compose có thể là false positive của UnusedPrivateMember; cần đối chiếu thực tế trước khi thay đổi rule.

## 6. Spotless

Không có file vi phạm được báo. Baseline chứa 232 ID MaxLineLength, cho thấy cần đối chiếu cấu hình và cách tính của Detekt với ktlint trước khi sửa định dạng.

## 7. CI hiện tại

`android-ci.yml` chạy khi push `main`/`develop`, PR vào `main` và thủ công. Một job chạy tuần tự build debug và unit test, JDK 21, setup-gradle, tải artifact HTML/XML và APK. Không thấy bước Detekt hoặc Spotless. Công bố kết quả test có điều kiện bỏ qua PR từ fork; bước inject Firebase bỏ qua khi thiếu secret hoặc variable và in thông báo dùng file mặc định, dù file này không còn được Git theo dõi. `android-release.yml` vẫn là workflow release riêng. Chưa xác minh Actions gần nhất, branch protection hoặc thời gian chạy thực tế.

Worktree còn `ci.yml` chưa theo dõi được tạo ở lượt trước; đây không phải workflow đã merge. Các file `android.yml` và `detekt.yml` theo kế hoạch không tồn tại trên commit này.

## 8. Phát hiện ngầm

Tìm trong `app/src` thấy 12 `@Suppress`, không thấy `@Ignore`/`@Disabled`: `AppAvailabilityTest.kt:416,425`; `TicketsScreen.kt:662`; `HomeScreen.kt:1235`; `LostFoundUiState.kt:93`; `MainActivity.kt:127,167`; `Dimens.kt:214,224,273`; `AnnouncementScreen.kt:708`; `HousingLocationPickerRoute.kt:160`. Chưa kiểm chứng lý do tại từng điểm. Chưa xác định ID baseline lỗi thời.

## 9. Giới hạn audit

Chưa có danh sách `file:dòng`, tổng finding không baseline, top 15 file, phân bố theo tầng và độ bao phủ test cho từng finding. Không thay đổi cấu hình Gradle hoặc baseline để thu các số này trong Phase 1. Các báo cáo cũ và `ci.yml` chưa theo dõi có từ trước lần audit; không có thay đổi mã nguồn hay cấu hình mới trong Phase 1 này. Log chi tiết hiện có trong kết quả chạy Gradle của phiên, chưa lưu thành file riêng.

## 10. Kế hoạch sửa đề xuất

1. **Thu bằng chứng (S):** chạy Detekt không baseline trong môi trường cách ly, lưu XML/HTML, thống kê file, dòng, tầng và baseline lỗi thời; xác định test bao phủ trước khi định số finding cần sửa.
2. **Định dạng (L):** rà 232 ID MaxLineLength, đối chiếu ktlint; sửa từng file và chạy Spotless/Detekt. Rủi ro thấp nếu giữ biểu thức.
3. **Hằng số và preview (L):** rà 79 ID MagicNumber và 81 ID UnusedPrivateMember; phân biệt hằng số nghiệp vụ và preview dùng bởi IDE, xác minh test/build. Chỉ đề xuất ngoại lệ Preview nếu kiểm chứng false positive.
4. **Cấu trúc, nhánh và ngoại lệ (L):** rà TooManyFunctions, complexity, ReturnCount, LongMethod, điều kiện và exception theo báo cáo đầy đủ; bổ sung test hành vi cho ViewModel coroutine, repository và validation thiếu coverage trước khi tách. Rủi ro trung bình đến cao.
5. **Baseline và CI (M):** đối chiếu baseline sau refactor, giữ lại chỉ các mục có lý do. Rà hai workflow hiện có và `ci.yml` dự thảo; thiết kế bốn job song song, cache, artifact và Firebase injection rõ ràng. Chạy PR thực để đo thời gian và cấu hình branch protection; không thể xác nhận mục tiêu dưới 6 phút trước khi đo.

Ước lượng tổng L, cần danh sách file và finding thực tế trước khi chia lô chính xác. Đề xuất tách PR định dạng, refactor và CI. Cần người dùng duyệt kế hoạch và quyết định ngoại lệ Compose/baseline sau khi có bằng chứng.
