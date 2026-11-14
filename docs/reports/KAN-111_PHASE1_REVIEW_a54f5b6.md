# KAN-111: Báo cáo Phase 1 sau đối chiếu

## 1. Kết quả và phát hiện chính

Phạm vi commit `a54f5b6` (`origin/develop`), ngày 14/11/2026. Trên bản này `assembleDebug`, `detekt`, `spotlessCheck`, `testDebugUnitTest` đều trả 0. Test XML ghi 414 test trong 48 suite, 0 failure, 0 error và 0 skip. Build hoàn thành trong 4 phút 15 giây trên máy này. `checkDebugAarMetadata` bị cấu hình tắt; file Firebase không tồn tại nên Gradle không áp dụng các plugin Firebase theo điều kiện trong build script.

Detekt CLI 1.23.8, plugin Compose 0.6.8, cấu hình dự án và default rules, không baseline, quét `app/src/main/java`, `app/src/test/java`, `app/src/androidTest/java`: **566 finding**, exit 2. Quét lại cùng phạm vi với baseline: **7 finding**, exit 2, đều thuộc `androidTest`. Như vậy baseline che **559 finding trong phép quét CLI này**. Task Gradle `detekt` trả 0, báo cáo XML rỗng và không phát hiện bảy finding `androidTest` này. Baseline gồm 476 ID; không thể lấy 566 trừ 476 để suy ra finding chưa che.

Ba vấn đề: số finding hiện có lớn; phạm vi task Gradle và phép quét bao gồm `androidTest` khác nhau; hai workflow CI trên PR vào `main` trùng build/test. Toàn bộ finding, file và dòng, thống kê theo rule/tầng, 15 file nhiều finding nhất và danh sách file theo từng rule nằm trong [phụ lục findings](KAN-111_PHASE1_FINDINGS_a54f5b6.md).

## 2. Môi trường và cấu hình

Temurin 21.0.7; Gradle wrapper 9.8.2; AGP 9.3.3; Kotlin 2.4.21; module `:app`; Detekt 1.23.8 và Spotless 8.11.0. Không dùng type resolution trong phép quét CLI; cần giữ giới hạn đó khi so sánh với các tác vụ khác.

Detekt `buildUponDefaultConfig=true`, `allRules=false`, dùng `config/detekt/detekt.yml` và `config/detekt/baseline.xml`; plugin Compose được cài. LongMethod ngưỡng 60, LongParameterList 6 và CyclomaticComplexMethod 15 bỏ qua `Composable`; FunctionNaming cũng bỏ qua `Composable`. MagicNumber bỏ qua khai báo property và companion property, cùng `Composable`/`Preview`; TooGenericExceptionCaught bỏ qua `Composable`. Không phát hiện rule được đặt `active: false` trong file cấu hình. Spotless dùng ktlint và ngoại lệ FunctionNaming cho `Composable`.

## 3. Finding theo rule và tầng

| Rule | Finding | Rule | Finding |
| --- | ---: | --- | ---: |
| MaxLineLength | 269 | MagicNumber | 123 |
| UnusedPrivateMember | 81 | TooGenericExceptionCaught | 21 |
| TooManyFunctions | 19 | CyclomaticComplexMethod | 11 |
| ReturnCount | 11 | LongMethod | 8 |
| MatchingDeclarationName | 8 | UseCheckOrError | 5 |
| ComplexCondition | 4 | SwallowedException | 3 |
| UnusedPrivateProperty | 2 | LongParameterList | 1 |

Theo khu vực: presentation trừ ViewModel 201, ViewModel 66, data 161, domain 58, DI 1, unit test 70, androidTest 7, khác 2. Phân loại theo đường dẫn, chưa chứng minh coverage. `AuthViewModel.kt` có 35 finding, `SupabaseRestClient.kt` 34, `BooksScreen.kt` 25, `TicketsScreen.kt` 23 và `SupabaseBookListingRepository.kt` 22.

## 4. Test, rủi ro và cấu hình

Rủi ro thấp: định dạng, sau khi kiểm tra không thay đổi chuỗi/biểu thức. Trung bình: đặt tên, tách hàm, quản lý hằng số và preview. Cao: điều kiện nghiệp vụ, dispatcher/cancellation, exception, repository và dữ liệu. Kết quả test thành công không đo line/branch coverage từng finding.

Có `AuthViewModelTest`, `BooksViewModelTest`, `TicketsViewModelTest`, các test use case và UI test; chỉ chứng minh có test liên quan, không xác nhận bao phủ đầy đủ. `SupabaseBookListingRepositoryTest` kiểm tra magic bytes, giới hạn kích thước và stream; chưa thấy test trực tiếp cho toàn bộ `saveListing`. Tìm kiếm test theo tên không thấy test trực tiếp `SupabaseRestClient`, `FirestoreEventSeeder` hay `SupabaseAuthRepository`; có test mô phỏng một phần hành vi auth. Cần bổ sung kiểm thử hành vi trước khi refactor các luồng lỗi, mạng và coroutine chưa được chứng minh bao phủ.

Spotless báo 0 file vi phạm; CLI Detekt báo 269 MaxLineLength. Kiểm tra sự khác biệt ngưỡng và tập source trước khi chọn formatter. Nhiều UnusedPrivateMember là preview cần IDE gọi; không tự xóa. Có thể cân nhắc `ignoreAnnotated: ['Preview']` cho đúng rule nếu xác minh là false positive và ghi lại ảnh hưởng; không khuyến nghị nới rule chung.

## 5. CI thực tế

`android-ci.yml`: push `main`/`develop`, PR vào `main`, thủ công; một job `build-and-test`, JDK 21, `setup-gradle`, build và test tuần tự, upload test/APK. Không chạy Detekt hoặc Spotless trong workflow này. Bước publish kết quả bỏ qua PR từ fork. Nhánh inject Firebase chỉ chạy khi đủ secret/variable; nhánh còn lại báo dùng file mặc định dù file không còn theo dõi trong Git. Do plugin Firebase được áp dụng theo sự tồn tại file, build không inject có thể thành công nhưng không xác minh cấu hình Firebase.

**Đính chính báo cáo trước:** `android-release.yml` có job `pr-validation`, chạy Detekt, Spotless, unit test và debug build trên PR vào `main`, push `main` và chạy thủ công; release job phụ thuộc job đó và chỉ chạy trên push `main` hoặc thủ công. Hai workflow vì vậy trùng build/test trên PR vào `main`. Cả hai dùng `setup-gradle`; cache hiệu quả và thời gian Actions thực tế chưa xác định. Tra cứu GitHub cho commit này trả không có PR-triggered run; không suy ra các run push không tồn tại. Chưa kiểm tra branch protection. Worktree có `ci.yml` chưa theo dõi từ lượt trước; đây không phải workflow đã merge.

## 6. Suppress, baseline và giới hạn

Tìm trong `app/src` thấy 12 `@Suppress`, không tìm thấy `@Ignore`/`@Disabled`: `AppAvailabilityTest.kt:416,425`; `TicketsScreen.kt:662`; `HomeScreen.kt:1235`; `LostFoundUiState.kt:93`; `MainActivity.kt:127,167`; `Dimens.kt:214,224,273`; `AnnouncementScreen.kt:708`; `HousingLocationPickerRoute.kt:160`. Comment cạnh `LostFoundUiState.equals` giải thích so sánh mọi trường; các ngoại lệ khác cần xem riêng lý do tại nơi khai báo. Tên file của toàn bộ 476 ID baseline đều còn tồn tại; điều này không chứng minh ID còn khớp. Chưa xác định được ID lỗi thời vì chưa có phép so sánh ID với finding hiện tại, và Phase 1 cấm sinh baseline.

Không lưu được đầy đủ log bốn lệnh từ lần chạy trước thành file riêng; kết quả và XML test/Detekt được ghi ở đây. Chưa đo coverage, chưa kiểm tra run push trên GitHub hoặc branch protection, chưa xác minh kết quả build có Firebase. Các số finding là kết quả CLI, còn các lệnh Gradle là bằng chứng độc lập; không khẳng định hai tập source hoàn toàn giống nhau.

## 7. Kế hoạch sửa đề xuất — chờ duyệt

Danh sách **mọi file và số vị trí theo từng rule** nằm ở mục “Files by rule for planning” trong phụ lục findings; các số sau là finding hiện tại, không cam kết số sẽ giảm sau một lô vì rule có thể tương tác.

| Lô | Phạm vi và cách làm | Số hiện tại | Rủi ro, kiểm tra, công sức |
| --- | --- | ---: | --- |
| 1 | MaxLineLength: `BooksScreen.kt` 23, `AuthViewModel.kt` 22, `EventsScreen.kt` 12, `AuthScreen.kt` 11 và các file còn lại theo phụ lục; sửa từng nhóm, giữ nguyên giá trị chuỗi | 269 | Thấp; Spotless, Detekt, build và unit test; L |
| 2 | MagicNumber: `SupabaseRestClient.kt` 31, `FirestoreEventSeeder.kt` 17, `SupabaseBookListingRepository.kt` 8, `AuthViewModel.kt` 8 và các file còn lại; đặt tên theo ý nghĩa, xác nhận giới hạn | 123 | Trung bình; test auth/books, thêm test client/seed thiếu coverage; L |
| 3 | UnusedPrivateMember: `TicketsScreen.kt` 19, `UtilitiesScreen.kt` 11, `AnnouncementScreen.kt` 10, `HomeScreen.kt` 9 và phần còn lại; xác minh preview/tooling trước khi đề xuất ngoại lệ | 81 | Thấp đến trung bình; build, preview/UI test phù hợp, Detekt; M |
| 4 | Naming, property và UseCheckOrError theo phụ lục; rà các khai báo ảnh hưởng callers và điều kiện lỗi | 15 | Trung bình; tìm caller, test liên quan, build; M |
| 5 | Complexity, LongMethod, LongParameterList, ReturnCount, TooManyFunctions và ComplexCondition; gồm `BookListingUseCases`, `SignUpUseCase`, `EventsScreen`, `ScheduleViewModel`, `AuthViewModel`, `SupabaseBookListingRepository` | 54 | Cao; thêm test nhánh, cancellation và lỗi trước khi tách; chạy test tương ứng và toàn bộ unit test; L |
| 6 | TooGenericExceptionCaught và SwallowedException: `SupabaseHousingRepository` 8, `SupabaseAuthRepository` 3, `SupabaseBookListingRepository` 2, `GoogleIdTokenSource` 2 và file còn lại | 24 | Cao; đặc tả lỗi mạng, cancellation và bảo toàn dữ liệu trước khi sửa; L |
| 7 | Rà 7 finding `androidTest`, baseline và phạm vi Gradle; chọn cách đưa source vào kiểm tra sau khi xác minh workflow | 7 vị trí đã tính trong các lô trên | Trung bình; kiểm tra Gradle task, test phù hợp và CI; M |
| 8 | Hợp nhất CI theo hiện trạng: giữ release signing, loại trùng các bước check, thiết kế bốn job song song, cache và artifact; xác minh Firebase có/không secret | Chưa định lượng thời gian | Trung bình; PR thật và đo Actions; M |

Không cộng lô 7 vào tổng vì đã nằm trong lô rule. Tổng công sức L; đề xuất PR riêng cho định dạng, refactor logic và CI để giới hạn rủi ro review. Sau khi sửa các lô, đối chiếu lại baseline và chỉ giữ các ID có lý do được ghi. Cần quyết định ngoại lệ Preview, có giữ ID baseline nào và phạm vi `androidTest`; không thay đổi rule chỉ để che finding. Mục tiêu dưới 6 phút cần đo sau khi chạy CI thực, hiện chưa có cơ sở dự báo.

**Chờ người dùng duyệt kế hoạch trước Phase 2.**
