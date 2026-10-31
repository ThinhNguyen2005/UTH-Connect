# Báo cáo task: danh sách sự kiện mẫu

## Đã làm

- Giữ nguyên file `presentation/home/HomeScreen.kt` và màn Home hiện tại.
- Dùng CORE-1 đã có: Hilt, Navigation type-safe, UthCard, UthStatusBadge, UthPrimaryButton và UthDimens.
- Thêm Event, Registration, Campus, EventStatus; EventRepository và GetEventsUseCase.
- StaticEventRepository trả 6 sự kiện cố định, mỗi cơ sở có 2 sự kiện. Có trường hợp đang mở còn chỗ, đang mở hết chỗ, đã đóng đăng ký, đã kết thúc và đã hủy.
- EventsScreen có danh sách, FilterChip theo cơ sở và badge trạng thái/số chỗ. Bộ lọc cuộn ngang, badge tự xuống dòng khi cần.
- Chỉ bật Đăng ký khi status là OPEN và còn chỗ. Xem chi tiết vẫn dùng được với các trạng thái khác.
- Cả Xem chi tiết và Đăng ký hợp lệ đều mở placeholder detail, truyền eventId qua `@Serializable`.
- Có loading, lỗi và Thử lại, danh sách rỗng; trở lại từ detail giữ bộ lọc. Đổi chip hủy lần tải cũ.
- Tất cả text hiển thị mới dùng stringResource, đủ tiếng Việt và tiếng Anh. Không thêm Color(hex), dp hoặc sp ngoài token.
- Có preview light/dark cho 4 trạng thái list và cho placeholder detail.
- Cập nhật cây thư mục và phân công trong REPO_TREE.md.

## Cách mở và demo

1. Chạy app, màn Home vẫn như cũ.
2. Chọn **Tiện ích → Sự kiện & CLB**.
3. Tất cả cơ sở có 6 sự kiện; Bình Thạnh, Thủ Đức, Quận 12 mỗi cơ sở có 2 sự kiện.
4. Ngày hội việc làm còn 55/200 chỗ và English Club còn 36/60 chỗ: nút Đăng ký bật.
5. Workshop Android hết chỗ; Chủ nhật xanh đóng đăng ký; talkshow logistics đã hủy; chào tân sinh viên đã kết thúc: nút Đăng ký tắt.
6. Bấm Xem chi tiết hoặc Đăng ký hợp lệ để mở placeholder, rồi Quay lại để kiểm tra bộ lọc vẫn còn.
7. Khi đổi ngôn ngữ hệ thống sang tiếng Anh, nhãn và nội dung sự kiện mẫu cũng đổi theo.

## Code ở đâu

Các đường dẫn dưới tính từ `app/src/main/java/com/giathinh/uthconnect/`:

| Vị trí | Vai trò |
|---|---|
| `domain/model/Event.kt` | Event, EventTopic, số chỗ còn lại, điều kiện được đăng ký và kiểm tra dữ liệu hợp lệ |
| `domain/model/Registration.kt` | Model đăng ký cho task tiếp theo |
| `domain/model/Campus.kt`, `EventStatus.kt` | Enum cơ sở và trạng thái |
| `domain/repository/EventRepository.kt` | Hợp đồng tải danh sách |
| `domain/usecase/GetEventsUseCase.kt` | Lọc campus và sắp xếp theo thời gian |
| `data/repository/StaticEventRepository.kt` | 6 bản ghi demo cố định |
| `data/di/EventsDataModule.kt` | Hilt nối interface với repository tĩnh |
| `presentation/events/` | Đúng 5 file chính: Screen, Route, ViewModel, UiState, Action |
| `presentation/events/navigation/EventsNavigation.kt` | Graph con và các destination type-safe |
| `presentation/events/detail/EventDetailScreen.kt` | Placeholder detail stateless |

File có sẵn đã sửa: `UtilitiesRoute.kt` để mở sự kiện; `UthApp.kt` để gắn graph con; `UthDestination.kt` để tab Tiện ích vẫn được chọn khi vào màn con; hai `strings.xml` để thêm key `events_*`.

## Test và build

- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/GetEventsUseCaseTest.kt`: 6 test kiểm tra dữ liệu cố định/ID, tất cả sự kiện/sắp xếp, lọc từng cơ sở, lỗi repository, điều kiện đăng ký và dữ liệu vượt sức chứa.
- `app/src/test/java/com/giathinh/uthconnect/presentation/events/EventsViewModelTest.kt`: 7 test kiểm tra tải ban đầu, chọn/xóa bộ lọc, thất bại, thử lại giữ bộ lọc, dữ liệu rỗng, hủy lần tải cũ và loading mặc định.
- Tổng bộ test của dự án: 18 test, gồm 13 test mới và 5 test có sẵn.

Lệnh yêu cầu:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
```

Máy hiện tại có JAVA_HOME cũ trỏ tới ổ C không tồn tại. Khi chạy build trong phiên terminal này, đặt lại tạm thời bằng:

```powershell
$env:JAVA_HOME = 'G:\Development\Android\Gradle\jdks\eclipse_adoptium-21-amd64-windows.2'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Báo cáo test: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Chỗ cần biết khi review

- Đây là dữ liệu demo có nội dung gần với hoạt động sinh viên, **không phải thông báo chính thức**. Ngày, trạng thái và số chỗ cố định, không tự thay đổi theo đồng hồ.
- `EventTopic` chứa 6 loại nội dung mẫu để UI chọn resource Việt/Anh. Khi có backend cho nội dung do người dùng nhập, cần thay phần nội dung mẫu bằng dữ liệu title/description/location từ server; không tiếp tục mở rộng enum cho mọi sự kiện thật.
- Registration mới là model. Task này chưa có đăng ký thật, Firestore, vé, QR hoặc kiểm tra chỗ trên server.
- Graph Sự kiện nằm trong graph tab Tiện ích. `UthDestination.from` tìm cả hierarchy để không mất trạng thái tab được chọn ở list/detail.
- Không thêm thư viện, không sửa Gradle hay design-system. Các thay đổi `.idea` đã có sẵn được giữ nguyên.
- Build và unit test đã chạy thành công. Lúc kiểm tra, `adb devices` không có thiết bị/emulator nên chưa xác nhận thao tác UI trực tiếp trên Android; dùng các bước demo phía trên để nghiệm thu trên máy.
- Chưa commit, push, tạo PR hoặc merge. Working tree đang ở nhánh `feature/timetable` của người dùng; khi tạo PR, cần tách phần events sang nhánh feature riêng và review vào develop theo quy trình nhóm.

## Nội dung đề xuất cho PR

**Tiêu đề:** `feat(events): thêm danh sách sự kiện mẫu và lọc theo cơ sở`

Mở Sự kiện & CLB từ tab Tiện ích, hiển thị 6 sự kiện mẫu với bộ lọc campus, trạng thái và số chỗ. Giữ nguyên HomeScreen; chỉ bật Đăng ký khi OPEN và còn chỗ, dẫn đến placeholder detail type-safe. Dùng 5 file presentation chính theo CORE-1, có resource Việt/Anh và 13 unit test mới.

Validation: `:app:assembleDebug` và `:app:testDebugUnitTest` pass, tổng 18 test. Cần reviewer kiểm tra luồng list → lọc → detail → quay lại trên emulator/thiết bị trước khi merge. Placeholder chưa tạo đăng ký/vé thật.
