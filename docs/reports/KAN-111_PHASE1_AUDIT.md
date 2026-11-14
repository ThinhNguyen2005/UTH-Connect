# Báo cáo audit chất lượng mã nguồn — KAN-111, giai đoạn 1

## 1. Tóm tắt

Ngày 14/11/2026, bốn lệnh `assembleDebug`, `detekt`, `spotlessCheck`, `testDebugUnitTest` đều trả mã 0. Kết quả test XML ghi 256 test, 0 lỗi, 0 skip. Các task trong lần chạy này đều UP-TO-DATE; unit test đã được thực thi thành công trong lượt trước.

Ba phát hiện: baseline có 459 ID; lần quét không baseline ở lượt trước báo 535 finding trong 110 file; workflow CI mới trong worktree chưa được kiểm tra trên GitHub. Không coi 535 finding là 535 ID baseline: một ID có thể tương ứng nhiều vị trí.

## 2. Môi trường và phạm vi

Temurin JDK 21.0.7, Gradle wrapper 9.8.2, AGP 9.3.3, Kotlin 2.4.21; module `:app`. Detekt 1.23.8 dùng `config/detekt/detekt.yml`, baseline `config/detekt/baseline.xml`, buildUponDefaultConfig và plugin Compose. Spotless 8.11.0 dùng ktlint và bỏ qua quy tắc tên hàm cho `Composable`.

Detekt cấu hình LongMethod 60, LongParameterList 6, CyclomaticComplexMethod 15 và bỏ qua `Composable` trong các rule này; FunctionNaming cũng bỏ qua `Composable`. MagicNumber bỏ qua khai báo property, companion property và các hàm `Composable`/`Preview`; TooGenericExceptionCaught bỏ qua `Composable`. Các rule Compose được bật trong cấu hình. Đây là cấu hình hiện có, chưa đánh giá đầy đủ ảnh hưởng từng ngoại lệ.

## 3. Kết quả kiểm tra

| Lệnh | Mã thoát | Kết quả |
| --- | ---: | --- |
| `assembleDebug` | 0 | Thành công, UP-TO-DATE |
| `detekt` | 0 | Thành công với baseline, 0 finding ngoài baseline trong báo cáo |
| `spotlessCheck` | 0 | Thành công, 0 file vi phạm được báo |
| `testDebugUnitTest` | 0 | UP-TO-DATE; XML của 41 suite: 256 test, 0 failure, 0 error, 0 skipped |

Lần chạy build ban đầu và test đầy đủ đã thành công ở lượt trước. Output Gradle hiển thị cảnh báo tính năng deprecated trước Gradle 10; `checkDebugAarMetadata` bị cấu hình SKIPPED, không phải test bị bỏ qua.

## 4. Phân tích Detekt

Lần quét ngày 14/11 trước giai đoạn này, khi tạm thời không gắn baseline, báo 535 finding: 466 ở `app/src/main`, 69 ở `app/src/test`, trên 110 file. Lần quét đó đã hoàn tác cấu hình và không còn báo cáo chi tiết: không thể xác minh lại vị trí từng finding, top 15 file hoặc số ID baseline lỗi thời mà không thực hiện thêm phép quét không baseline. Không suy ra các giá trị này từ tên ID.

| Rule | Finding khi bỏ baseline |
| --- | ---: |
| MaxLineLength | 259 |
| MagicNumber | 120 |
| UnusedPrivateMember | 77 |
| TooManyFunctions | 14 |
| TooGenericExceptionCaught | 13 |
| ReturnCount | 12 |
| CyclomaticComplexMethod | 10 |
| LongMethod | 8 |
| MatchingDeclarationName | 8 |
| ComplexCondition | 4 |
| UseCheckOrError | 4 |
| SwallowedException | 3 |
| UnusedPrivateProperty | 2 |
| LongParameterList | 1 |
| **Tổng** | **535** |

Số finding đang được baseline che theo từng vị trí và phân bố theo tầng kiến trúc chưa xác minh được. Baseline có 459 ID, không phải phép đếm vị trí chính xác.

## 5. Rủi ro khi sửa

Định dạng và tên hằng số: thấp nếu không đổi biểu thức; tách hàm, đổi tên và chia lớp: trung bình; điều kiện, ngoại lệ, coroutine và luồng dữ liệu: cao. Test hiện có bao phủ một phần ViewModel, use case và repository, chưa đo coverage theo từng finding. Các preview Compose có thể được IDE sử dụng dù bị UnusedPrivateMember báo; cần kiểm tra từng trường hợp trước khi sửa hoặc cấu hình ngoại lệ có lý do.

## 6. Spotless

`spotlessCheck` thành công, không báo vi phạm. Detekt ghi nhận 259 MaxLineLength trong lần quét không baseline trước đó; điều này cho thấy hai công cụ hiện không tạo cùng kết quả về độ dài dòng, cần kiểm tra ngưỡng và cách xử lý cụ thể trước khi định dạng hàng loạt.

## 7. CI

Ở `origin/develop` không có `android.yml` hay `detekt.yml`; có `android-release.yml` chạy release khi thủ công và push vào `main` hoặc nhánh được cấu hình. Workflow release dùng JDK 21 và setup-gradle, build release và ký APK; không có bốn check yêu cầu trong cùng workflow.

Worktree đã có `ci.yml` chưa được theo dõi từ lượt trước, với bốn job build-debug, detekt, spotless và unit-tests, cache Gradle, artifact và trigger PR/push vào `develop`/`main`. Chưa kiểm tra YAML bằng actionlint hoặc chạy trên GitHub; thời gian Actions gần nhất và branch protection chưa xác định. Không xác minh được cache, điều kiện skip hay `continue-on-error` của hai workflow được nhắc trong kế hoạch vì chúng không tồn tại trên nhánh gốc.

## 8. Phát hiện ngầm

Tìm `@Suppress`, `@Ignore`, `@Disabled` dưới `app/src`: có bảy `@Suppress`, không tìm thấy `@Ignore` hoặc `@Disabled`. Các vị trí: `Dimens.kt:211,221,267`, `HomeScreen.kt:1235`, `AnnouncementScreen.kt:708`, `LostFoundUiState.kt:92`, `TicketsScreen.kt:662`. Sáu chỗ suppress ktlint property-naming, một chỗ suppress CyclomaticComplexMethod; chưa xác minh lý do từng chỗ. Baseline lỗi thời chưa xác định.

## 9. Hạn chế

Danh sách chi tiết `file:dòng`, top 15 file, phân bố theo tầng, mức coverage theo finding và baseline lỗi thời chưa có. Báo cáo Detekt hiện tại là kết quả có baseline; báo cáo đầy đủ từ lượt trước đã bị lượt chạy sau thay thế. Các lệnh trong lần audit này dùng output UP-TO-DATE. Chưa có actionlint/YAML parser và chưa kiểm tra GitHub Actions. Báo cáo này được tạo riêng vì `DETEKT_QUALITY_AUDIT.md` đã tồn tại; workflow `ci.yml` đã có từ trước giai đoạn audit.

## 10. Kế hoạch sửa đề xuất

1. **Hoàn thiện bằng chứng (S):** chạy phân tích không baseline trong môi trường cách ly, lưu XML/HTML và danh sách `file:dòng`, đối chiếu ID baseline, phân bố tầng và test liên quan. Xác định đầy đủ file trước khi ước lượng số sửa theo lô.
2. **Định dạng (L):** 259 MaxLineLength, tập trung các file được xác định từ báo cáo đầy đủ; kiểm tra tương thích ktlint/Detekt rồi sửa từng nhóm, chạy Spotless, Detekt và unit test. Rủi ro thấp nếu không thay đổi biểu thức.
3. **MagicNumber (L):** 120 finding, xác định các ngữ nghĩa và hằng số trong từng file; kiểm tra use case/repository/ViewModel tương ứng. Rủi ro thấp đến trung bình vì một số giá trị là giới hạn nghiệp vụ.
4. **Preview và tên (M):** rà 77 UnusedPrivateMember và 8 MatchingDeclarationName; phân biệt preview được tooling sử dụng và khai báo thực sự thừa. Đề xuất cấu hình bỏ qua `Preview` chỉ khi kiểm chứng đây là false positive; ghi lý do và tác động. Test build và preview liên quan.
5. **Cấu trúc và điều kiện (L):** 14 TooManyFunctions, 12 ReturnCount, 10 CyclomaticComplexMethod, 8 LongMethod, 4 ComplexCondition, 1 LongParameterList. Bổ sung test đặc tả nhánh ở những hàm chưa có test trước khi tách; đặc biệt ViewModel coroutine, repository và validation. Rủi ro trung bình đến cao.
6. **Ngoại lệ và phần còn lại (M):** rà 13 TooGenericExceptionCaught, 3 SwallowedException, 4 UseCheckOrError, 2 UnusedPrivateProperty; giữ xử lý lỗi và hành vi. Chạy test tương ứng trước/sau. Không xóa file hoặc test.
7. **Baseline và CI (M):** sau khi xử lý an toàn, đối chiếu lại findings và baseline, chỉ giữ mục có lý do. Rà `ci.yml`, secrets cho PR và artifact; chạy thật trên PR để đo thời gian, không thể ước lượng đáng tin dưới 6 phút trước khi đo. Cấu hình branch protection yêu cầu bốn check.

Tổng công sức sơ bộ **L**; số file từng lô và số finding thực sự giải quyết được phải chốt sau bước 1. Đề xuất PR riêng cho định dạng, refactor nghiệp vụ và CI để dễ review và khoanh vùng lỗi. Cần quyết định sau khi có bằng chứng: ngoại lệ Compose nào hợp lệ, có chấp nhận finding còn trong baseline và số PR cuối cùng.

**Chờ người dùng duyệt kế hoạch trước giai đoạn sửa.**
