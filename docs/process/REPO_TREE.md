# UTH Connect — Cây thư mục & phân quyền sở hữu

Scanner CHECKIN-2: `presentation/scanner/` gồm Screen, Route, ViewModel, UiState, Action và thành phần camera `QrCameraPreview.kt`; `domain/usecase/CheckInUseCase.kt` kiểm tra role và mã vé. QR trong `Ticket.kt` dùng payload ID vé đầy đủ; `MockTicketRepository` lưu Attendance trong bộ nhớ và cập nhật trạng thái vé. Test ở `test/.../domain/usecase/CheckInUseCaseTest.kt`, `test/.../presentation/scanner/ScannerViewModelTest.kt` và `test/.../presentation/tickets/TicketsQrEncodingTest.kt`. Build thêm ZXing core để tạo QR; mã demo chưa được backend ký/xác minh.

> **Phiên bản:** v1.4 · **Cập nhật cấu trúc lúc:** 31/10/2026 · **Người giữ tài liệu:** chủ repo (`Thịnh`)
> **Phạm vi:** cây thư mục `app/src`, ai sở hữu nhánh nào, file nào được tạo/sửa trong phạm vi đó, và nghi thức gộp cây khi cả nhóm xong việc.
> **Không chứa:** quy ước viết code (`docs/CODING_STANDARDS.md`), spec nghiệp vụ từng module (`README.md`, `docs/Design.md`), token UI (`docs/Design-system.md`), quy trình nhánh/PR (`docs/process/GIT_WORKFLOW.md`).
>
> **⚠ Luật bảo trì — đây là điều kiện merge, không phải lời khuyên:** mọi PR làm **thay đổi cấu trúc thư mục** (thêm/xoá package, thêm/xoá file thuộc một tầng, đổi chủ sở hữu, thêm dependency build làm đổi cây) **bắt buộc** phải cập nhật file này trong cùng commit: sửa mục 2 (cây thật), mục 3 (bảng phân công), thêm một dòng vào nhật ký mục 8. PR đổi cấu trúc mà không đổi `docs/process/REPO_TREE.md` → review từ chối, không cần lý do khác.

---

## 1. Ba nguyên tắc

| # | Nguyên tắc | Hệ quả thực tế |
|---|---|---|
| 1 | **Một người = một cây.** Mỗi thành viên sở hữu nguyên package `presentation/<module>/` cộng các lát cắt `domain/…` và `data/…` của chính module đó. | Trong cây của mình, bạn tự tạo/sửa/xoá file, không xin phép. |
| 2 | **Cây người khác chỉ đọc.** Cần đổi file ngoài cây (thêm token màu, sửa component, thêm route) → mở issue hoặc PR nhỏ **gửi owner của file đó**, hoặc nhờ chủ repo merge một dòng. | Không "tiện tay" sửa `UthComponents.kt` của người khác: đó là nguồn conflict số một. |
| 3 | **File chung chỉ mở rộng, không viết lại.** Mỗi file chung có quy tắc "một dòng cộng thêm" ghi ở mục 5. | Merge không conflict; ai sửa vượt quy tắc phải được chủ repo approve. |

Giải thích vì sao chia theo cây dọc (vertical slice) chứ không phải cây ngang: mỗi module đi trọn `presentation → domain ← data`, nên hai người làm hai module không bao giờ đụng nhau. Chia ngang (người A viết hết `ui`, người B viết hết `data`) sẽ bắt 5 người sửa cùng một file.

---

## 2. Cây hiện tại (đã kiểm chứng bằng `Get-ChildItem`, không phải cây trên giấy)

**Bổ sung trên `feature/checking-duc-v2` (03/11/2026):** `presentation/attendance/` gồm `AttendanceScreen`, `AttendanceRoute`, `AttendanceViewModel`, `AttendanceUiState`, `AttendanceAction`, `AttendanceNavigation`; `domain/model/AttendanceRoster.kt`, `domain/repository/AttendanceRosterRepository.kt`, `domain/usecase/{GetAttendanceRosterUseCase,ExportAttendanceCsvUseCase}.kt`; `data/repository/SupabaseAttendanceRosterRepository.kt`, `data/di/AttendanceRosterDataModule.kt`; unit tests tương ứng trong `test/presentation/attendance/` và `test/domain/usecase/`. Màn chi tiết được nối bằng một nút trên thẻ sự kiện và một graph con trong `UthApp.kt`.

**Bổ sung trên `feature/checking-duc-v2` (04/11/2026):** `presentation/scanner/` gồm 4 file `QrScannerScreen`, `QrScannerRoute`, `QrScannerUiState`, `QrScannerAction`. Scanner **không** có `ViewModel` và không phải một module đủ 5 file theo `docs/CODING_STANDARDS.md` mục 3.2, vì nó là Dialog quét nối trong `EventsRoute` chứ không có tab/route riêng, và `PreviewView.SurfaceProvider` mà CameraX cần thuộc về View — đưa binding camera vào ViewModel sẽ phạm mục 3.1 dòng "ViewModel cấm Compose/Context". Logic giải mã frame đẩy xuống `domain/qr/QrFrameDecoder.kt` (thuần JVM + zxing, test được) để `presentation/scanner/` không có file thứ sáu kiểu helper. Phần dịch mã lỗi server đặt ở `presentation/common/ServerErrorMessage.kt`: đây là ngoại lệ có chủ đích của mục 3.1 dòng "muốn dùng chung thì đẩy xuống domain/core", vì hàm là `@Composable` (`stringResource`) nên không xuống `domain` được, còn `core/designsystem` bị cấm import `domain`.

**Bổ sung trên `feature/retal-place` (07/11/2026):** `presentation/housing/` hoàn thiện trọn vẹn 3 màn hình và data layer:
- Feed: `HousingScreen`, `HousingRoute`, `HousingViewModel`, `HousingUiState`, `HousingAction`, `HousingNavigation` (+ `README.md` module) hỗ trợ 3 hàng chip lọc nhanh chuẩn Jira: cơ sở (CS1/CS2/CS3), 3 khoảng giá (< 2tr, 2 - 3.5tr, > 3.5tr), giới tính (Nam/Nữ/Bất kỳ).
- Chi tiết: `presentation/housing/detail/` gồm `HousingDetailScreen`, `HousingDetailRoute`, `HousingDetailViewModel`, `HousingDetailUiState`, `HousingDetailAction` (carousel ảnh, thẻ sinh viên xác thực UTH kèm MSSV, tiện ích phòng, gọi điện / mở Zalo trực tiếp, đánh dấu đã tìm được phòng).
- Đăng tin: `presentation/housing/create/` gồm `CreateHousingPostScreen`, `CreateHousingPostRoute`, `CreateHousingPostViewModel`, `CreateHousingPostUiState`, `CreateHousingPostAction` (form chọn cơ sở, thông tin phòng, giá thuê/chi phí, tiện ích, tiêu chí bạn trọ, liên hệ).
- Domain: `HousingListing`, `CampusReference`, `HousingPriceRange`, `HousingRepository`, use cases (`GetHousingListingsUseCase`, `GetHousingListingDetailUseCase`, `CreateHousingListingUseCase`, `MarkHousingListingFulfilledUseCase`, `ReportHousingListingUseCase`).
- Data: `SupabaseHousingRepository` CRUD thật trên `housing_posts` (create, update, mark fulfilled, delete, report qua RPC), upload ảnh cục bộ lên bucket `housing-images` và dọn ảnh khi thất bại; `HousingDataModule` bind thẳng bản Supabase (đã gỡ `InMemoryHousingRepository` cùng test của nó).
- Supabase: migration chính thức `supabase/migrations/20261108000000_housing_schema_rebuild.sql` (dựng lại bảng khớp domain, RLS, trigger giới hạn 2 tin mở chịu race, RPC `mark_housing_listing_fulfilled` / `report_housing_listing`, bucket ảnh + storage policy). Hai migration `20261107000000` trùng tên `housing_posts` trước đây đã được xử lý: phần housing trong `auth_rbac_and_housing` gỡ bỏ, `create_housing_posts` thành no-op superseded.
- Tests mirror đầy đủ: `test/presentation/housing/` (Feed, Detail, Create ViewModel + UiState).

**Bổ sung trên `feature/retal-place` (10/11/2026):** Bỏ mocking Housing — `SupabaseHousingRepository` + `SupabaseRestClient` thêm `deleteTable`, `uploadStorageObject`, `deleteStorageObject`; `ServerErrorCode` thêm 4 mã `HOUSING_*` với chuỗi song ngữ `common_error_housing_*`; `CreateHousingPostEvent.SubmitFailed` mang mã lỗi server để Route báo snackbar đúng nguyên nhân (vd đủ 2 tin mở).

```
app/src/main/java/com/giathinh/uthconnect/
├── MainActivity.kt                     @AndroidEntryPoint — điểm vào DI duy nhất     [chủ repo]
├── UTHConnectApplication.kt            @HiltAndroidApp — gốc SingletonComponent      [chủ repo]
├── core/
│   └── designsystem/
│       ├── component/  UthComponents.kt · UthStatusBadge.kt                          [chủ repo]
│       └── theme/      Color.kt · Dimens.kt · Theme.kt · Type.kt                     [chủ repo]
├── data/
│   ├── di/            UtilitiesDataModule.kt · TicketDataModule.kt                   [utilities · checkin]
│   └── repository/    StaticUtilityModuleRepository.kt · MockTicketRepository.kt     [utilities · checkin]
├── domain/
│   ├── model/         UtilityModule.kt · Ticket.kt · Attendance.kt                    [utilities · checkin]
│   ├── repository/    UtilityModuleRepository.kt · TicketRepository.kt                [utilities · checkin]
│   └── usecase/       GetUtilityModulesUseCase.kt · GetTicketsUseCase.kt               [utilities · checkin]
└── presentation/
    ├── navigation/    UthApp.kt · UthDestination.kt                                   [chủ repo; extended for tickets]
    ├── tickets/       README · TicketsScreen · Route · ViewModel · UiState · Action · Navigation [checkin]
    ├── scanner/       QrScannerScreen · QrScannerRoute · QrScannerUiState · QrScannerAction  [checkin]
    ├── common/        ServerErrorMessage.kt — mã lỗi server → câu hướng xử lý (dùng chung)  [checkin]
    ├── events/        EventsScreen · EventsRoute · EventsViewModel · EventsNavigation.kt (`@Serializable data object EventsDestination` + `eventsGraph`, route của tab Tiện ích)   [events]
    ├── attendance/    Screen · Route · ViewModel · UiState · Action · Navigation        [checkin]
    ├── home/          HomeScreen.kt          (placeholder, chưa MVVM)                 [events]
│   ├── di/            Auth · Events · Firebase · Grade · Home · Network · Notification · Schedule · User · Utilities
│   ├── network/       NetworkMonitor · ConnectivityManagerNetworkMonitor
│   ├── repository/    demo/Firebase auth, session, Firestore events, fake data, announcements, utilities
│   └── seed/          FirestoreEventSeeder.kt
├── domain/
│   ├── model/         Announcement · Event · HomeFeed · User · StudentProfile · CourseSchedule · ServerErrorCode · …
│   ├── repository/    Auth · Session · Event · Announcement · User · Schedule · Grade · Notification · Utilities
│   ├── usecase/       SignIn · GetProfile · GetHomeFeed · GetAnnouncementDetail · GetUtilityModules
│   ├── qr/            QrFrameDecoder.kt — nén luminance + giải mã QR, thuần JVM          [checkin]
│   └── validation/    LoginValidator.kt
└── presentation/
    ├── navigation/    UthApp.kt · UthDestination.kt                                   [chủ repo]
    ├── auth/          Screen · Route · ViewModel · UiState · Action                   [auth]
    ├── announcement/  Screen · Route · ViewModel · UiState · Action · Navigation
    ├── home/          Screen · Route · ViewModel · UiState · Action
    ├── schedule/      ScheduleScreen.kt      (placeholder, chưa MVVM)                 [timetable]
    ├── profile/       Screen · Route · ViewModel · UiState · Action                   [auth]
    └── utilities/     Screen · Route · ViewModel · UiState · Action                   [utilities]

app/src/test/java/com/giathinh/uthconnect/
├── testing/           MainDispatcherRule.kt      (dùng chung — chỉ đọc, đừng sửa)     [chủ repo]
└── presentation/utilities/  UtilitiesViewModelTest.kt  (4 test, đang xanh)            [utilities]
    └── presentation/tickets/ TicketsViewModelTest.kt                                      [checkin]
    └── domain/qr/       QrFrameDecoderTest.kt  (6 test, mirror package của `domain/qr`)   [checkin]
    └── domain/model/    ServerErrorCodeTest.kt  (7 test, hợp đồng 21 mã lỗi)              [checkin]
    └── presentation/events/ EventEditorTimeTest.kt  (6 test, mặc định giờ tạo sự kiện)     [checkin]
```

Đây là snapshot checkout `main` tại `9267f65`; `schedule/` vẫn là placeholder, Events và Check-in chưa có package presentation trên `main`. **Cây `origin/develop` đã tiến lên `3f545c3`**, khác `main` vì PR #20 đã merge Lost & Found. Phần chênh lệch được xác minh bằng `git diff --name-status 9267f65..origin/develop`:

```text
presentation/lostfound/      LostFoundAction · Route · Screen · UiState · ViewModel
domain/model/                LostFoundListing.kt
domain/repository/           LostFoundRepository.kt
domain/usecase/              GetLostFoundListingsUseCase · ReportLostFoundItemUseCase
data/repository/             InMemoryLostFoundRepository.kt
data/di/                     LostFoundDataModule.kt
test/presentation/lostfound/ LostFoundViewModelTest.kt
res/values/ + values-en/     41 dòng string mới trong mỗi file
```

Tại nhánh tích hợp CORE-1 (`codex/core-integration`, merge `24117e7` từ `origin/develop`), phần cây mới đã được nối như sau:

```text
presentation/lostfound/  LostFoundAction.kt · LostFoundRoute.kt · LostFoundScreen.kt
                         LostFoundUiState.kt · LostFoundViewModel.kt · LostFoundNavigation.kt
presentation/navigation/ UthApp.kt: lostFoundGraph() + mở LostFoundDestination từ Utilities
data/repository/         StaticUtilityModuleRepository.kt: LOST_FOUND chưa được bật
domain/model/            LostFoundListing.kt
domain/repository/       LostFoundRepository.kt
domain/usecase/          GetLostFoundListingsUseCase.kt · ReportLostFoundItemUseCase.kt
data/repository/         InMemoryLostFoundRepository.kt
data/di/                 LostFoundDataModule.kt
test/presentation/lostfound/ LostFoundViewModelTest.kt
```

`LostFoundNavigation.kt` khai báo `@Serializable LostFoundDestination` cùng `lostFoundGraph`. Module vẫn bị tắt trong Utilities để chờ Thắng sửa lỗi KAN-49, bao gồm dữ liệu liên hệ giả và thao tác gọi điện trong Screen. Bản `origin/develop` tại `3f545c3` và nhánh tích hợp đều build thành công, qua **42 unit tests/10 suites, 0 failures**. APK tại `adb9449` đã được smoke-test trên Samsung SM-G990E: đăng nhập demo, chọn lần lượt bốn tab và kiểm tra tab được chọn, mở chi tiết thông báo rồi Back về Trang chủ; Tiện ích hiển thị nhãn chưa sẵn sàng và card Sự kiện không nhận thao tác bấm. Kết quả này không xác nhận các module đang chờ tích hợp hoặc Lost & Found đang tắt.

---

### Auth/Profile da tich hop - 31/10/2026

- `presentation/auth/` va `presentation/profile/`: moi module 5 file Screen/Route/ViewModel/UiState/Action.
- `domain/model/StudentProfile.kt`; `domain/repository/{AuthRepository,SessionRepository,StudentContactRepository}.kt`; `domain/validation/{LoginValidator,PasswordChangeValidator,StudentContactValidator}.kt`; `domain/usecase/{SignInUseCase,GetProfileUseCase,ChangePasswordUseCase,UpdateStudentContactUseCase}.kt`.
- `data/repository/{DemoAuthRepository,PreferencesSessionRepository}.kt`; `data/di/AuthDataModule.kt`; test unit va Compose UI.
- Co permission USE_BIOMETRIC, resource song ngu, quy tac backup; Auth V1 da sua UthApp.kt. Ban demo cuc bo chua ket noi API UTH.

### Auth/Profile V3 - role and verified

- domain/model/StudentProfile.kt: extend existing model with role (STUDENT default), uthVerified (false default), retain studentId and V2 contact fields.
- domain/model/UserRole.kt: STUDENT, ORGANIZER, ADMIN.
- data/repository/DemoAuthRepository.kt: two isolated demo accounts; verified and organizer badges are simulated and must never authorize server-side actions.
- presentation/auth and presentation/profile: Channel + receiveAsFlow for one-off UI events; failed logout has visible Retry.
- Tests: StudentProfileRoleTest, AuthViewModelTest, ProfileViewModelTest, ProfileScreenUiTest.
- No changes to global nav, MainActivity, Gradle or design system; V1 UthApp changes still need repo owner review.

## 3. Cây đích: mỗi người một nhánh

Phân công khớp `docs/process/GIT_WORKFLOW.md` mục 3. Mỗi ô "được tạo" là cây riêng — ngoài nó, xem mục 5.

| Người | Nhánh | `presentation/` | `domain/` | `data/` | `test/` mirror |
|---|---|---|---|---|---|
| **Anh** | `feature/events-qr-checkin-anh` | `events/` (5 file MVVM) · `tickets/` · `scanner/` · `home/` (nâng HomeScreen thành slice thật) | `model/Event.kt` `model/Registration.kt` `model/Ticket.kt` · `repository/EventRepository.kt` `repository/AttendanceRepository.kt` · `usecase/GetEventsUseCase.kt` `CheckInUseCase.kt` … | `repository/` `remote/firestore/EventFirestoreRepository.kt` · `di/EventsDataModule.kt` | `presentation/events/` `presentation/tickets/` `presentation/scanner/` |
| **Bạn B** | `feature/checkin-<tên>` | gộp vào nhánh events (QR + máy quét là một luồng) — nếu tách người thì tạo `presentation/scanner/` và **thỏa thuận boundary với Anh trước**, không tự chia | `model/Attendance.kt` | `di/CheckInDataModule.kt` | `presentation/scanner/` |
| **Bạn C** | `feature/auth-<tên>` | `auth/` · `profile/` | `model/User.kt` `model/Faculty.kt` · `repository/AuthRepository.kt` `repository/ProfileRepository.kt` · `usecase/SendSchoolOtpUseCase.kt` `SignInUseCase.kt` `GetProfileUseCase.kt` | `remote/auth/SchoolEmailAuthRepository.kt` · `di/AuthDataModule.kt` | `presentation/auth/` `presentation/profile/` |
| **Nghĩa (TIMETABLE-1)** | feature/timetable-nghia | schedule/ (5 file MVVM) | model/Timetable.kt (Course/ClassSession/Deadline) · repository/TimetableRepository.kt · usecase/TimetableUseCase.kt | repository/InMemoryTimetableRepository.kt · di/TimetableDataModule.kt | domain/usecase/TimetableUseCaseTest.kt |
| **Bạn C** | `feature/auth-phu` | `auth/` · `profile/` (2 × 5 file) | `StudentProfile`, `AuthRepository`, `SessionRepository`, `StudentContactRepository`, `ChangePasswordUseCase`, `UpdateStudentContactUseCase`, validators | `DemoAuthRepository`, `PreferencesSessionRepository`, `AuthDataModule` | Auth/Profile unit + UI tests |
| **Bạn D** | `feature/timetable-<tên>` | `schedule/` | `model/Course.kt` `model/Deadline.kt` `model/StudyGroup.kt` · `repository/TimetableRepository.kt` `repository/DeadlineRepository.kt` · `usecase/GetWeekScheduleUseCase.kt` `ScheduleReminderUseCase.kt` | `local/room/…` (Room) · `di/TimetableDataModule.kt` | `presentation/schedule/` |
| **Bạn E** | `feature/lostfound-<tên>` | `utilities/` (đang có — kế thừa nguyên slice mẫu) · `lostfound/` | `model/LostFoundListing.kt` · `repository/LostFoundRepository.kt` · `usecase/…` | `repository/` `remote/firestore/LostFoundFirestoreRepository.kt` · `di/LostFoundDataModule.kt` | `presentation/utilities/` `presentation/lostfound/` |
| **Anh Phú** | `feature/auth-phu` | `auth/` · `profile/` | model và hợp đồng auth/session | repository và DI auth/session | auth · profile |
| **Văn Tạ Huy Thiên** | `feature/events-van` | `events/` | Event · Registration · use case | repository và DI events | events |
| **Đức Dịch Viết** | `feature/checking-duc-v2` | `tickets/` · `scanner/` · `attendance/` · `common/` (chỉ `ServerErrorMessage.kt`, xem mục 5) | Ticket · Attendance · AttendanceRoster · `model/ServerErrorCode.kt` · `qr/QrFrameDecoder.kt` · use case | repository và DI check-in/danh sách | tickets · scanner · attendance · `domain/qr/` · `domain/model/` |
| **Đức Dịch Viết** | `feature/retal-place` | `housing/` | `HousingListing` · `CampusReference` · `HousingRepository` · use case | `SupabaseHousingRepository` · `HousingDataModule` | housing |
| **Trọng Nghĩa** | `feature/timetable-nghia` | `schedule/` | model và use case thời khóa biểu | repository và DI thời khóa biểu | schedule |
| **Thắng Nguyễn Minh** | `feature/lostfound-thang` | `lostfound/` | model và use case Lost & Found | repository và DI Lost & Found | lostfound |
| **Chủ repo** | `chore/<việc>` | `navigation/` | `domain/common/` (kết quả phân trang, `DeliveryPhase` — chỉ để thứ thật sự dùng chung) | — | `testing/` |

Quy tắc đặt tên file trong cây: theo `docs/CODING_STANDARDS.md` mục 2 và 3.2 — một module có đúng **5 file** trong `presentation/<module>/` (Hilt đã xoá bỏ file factory thứ sáu), thêm `<Module>Navigation.kt` khi module có route con.

Phân công trên lấy từ Jira KAN-42–49 ngày 31/10/2026; tên nhánh là kế hoạch, không chứng minh nhánh hoặc PR đã tồn tại, đã merge hay đạt DoD. Khi đối chiếu GitHub ngày 31/10, nhánh Check-in hiện có tên `feature/checking-duc`, khác với tên `feature/checkin-duc` trong Jira; cần thống nhất trước khi tích hợp. `feature/timetable` cũng tồn tại bên cạnh `feature/timetable-nghia`: đối chiếu trước khi tích hợp để tránh trùng chủ sở hữu `schedule/`.

**Cập nhật đồng bộ `develop`:** Lost & Found (Thắng) đã merge qua PR #20 tại `3f545c3`. Nhánh tích hợp CORE-1 đã merge commit đó tại `24117e7` và chuẩn bị graph; chờ Thắng xử lý review KAN-49 trước khi bật module. Chỉ đánh dấu ✅ DoD sau khi build/test tích hợp, review và smoke-test trên thiết bị.

**Phụ thuộc Auth cho Events/Check-in (đối chiếu 31/10/2026):** `origin/feature/auth-phu` tại `bf38503` đã khai báo `StudentProfile.studentId`, `role: UserRole` và `uthVerified`; `UserRole` có `STUDENT`, `ORGANIZER`, `ADMIN`. Đây là role demo cục bộ, không thay thế kiểm tra quyền bằng server claims. `origin/develop` tại `3f545c3` có `studentId` nhưng chưa có `role` và `uthVerified` trong `StudentProfile`; không coi các trường này đã sẵn sàng trên nhánh tích hợp. PR Auth #16 hiện nhắm vào `main` và GitHub báo không merge được; phép kiểm tra merge ba chiều cũng xuất hiện conflict, cần đối chiếu lại với `develop` theo README và xử lý các file chung trước khi tích hợp. Nhánh còn lỗi khôi phục session sau lỗi tạm thời; Events/Check-in cần đợi Auth được sửa, review và tích hợp trước khi phụ thuộc vào hợp đồng mới.

Module nào cũng bắt đầu bằng 5 file đã chốt, copy nguyên `presentation/utilities/`:

```
<Module>Screen.kt · <Module>Route.kt · <Module>ViewModel.kt · <Module>UiState.kt · <Module>Action.kt
+ data/di/<Module>DataModule.kt          (chỉ khi có interface repository cần binding)
+ presentation/<module>/<Module>Navigation.kt   (chỉ khi có route con)
```

---

## 4. DI theo Hilt: vì sao cây đã chia được sạch

Đây là lý do mục 3 chia được mà không cần file DI chung:

| Việc | Cách làm (Hilt) | Ai chạm |
|---|---|---|
| Repository interface → impl | `@Module @InstallIn(SingletonComponent::class) abstract class <Module>DataModule` có `@Binds`, đặt trong **`data/di/` của chính module** | chỉ owner module |
| UseCase | `class GetXUseCase @Inject constructor(repo)` — Hilt tự sinh binding, **không cần khai báo ở đâu cả** | chỉ owner module |
| ViewModel | `@HiltViewModel class XViewModel @Inject constructor(useCase)` | chỉ owner module |
| Lấy ViewModel trong Compose | `<Module>Route.kt`: `viewModel: XViewModel = hiltViewModel()` (artifact `androidx.hilt:hilt-lifecycle-viewmodel-compose`) | chỉ owner module |
| Gốc của cây DI | `UTHConnectApplication` (`@HiltAndroidApp`) + `MainActivity` (`@AndroidEntryPoint`) | **chủ repo** — bạn không sửa hai file này |

Hệ quả: thêm module mới **không** phải sửa file DI nào của người khác. Trước đây (AppContainer + factory) mỗi module phải cộng một `val` vào `core/di/AppContainer.kt` và một factory vào `presentation/<module>/`; cả hai chỗ đó đã bị xoá khi chuyển Hilt (nhật ký mục 8).

Cấm: `@Component`/`@Subcomponent` tự chế, `ServiceLocator` dạng `object`, `EntryPoint` khi chưa thật sự cần, và mọi `CompositionLocal` mang dependency (ngoại lệ duy nhất còn lại: theme trong `core/designsystem`).

---

## 5. File chung — bảng phân quyền

| File | Owner | Bạn được làm | Tuyệt đối không |
|---|---|---|---|
| `presentation/navigation/UthApp.kt` | chủ repo | nhờ thêm một dòng gọi graph module hoặc route tab khi đã sẵn sàng | đổi `Scaffold`, `popUpTo`, transition, hay tự ý thêm route |
| `presentation/navigation/UthDestination.kt` | chủ repo | giữ bốn tab hiện có; gửi route con cho chủ repo tích hợp | sửa route của module khác; đổi cơ chế type-safe |
| `UTHConnectApplication.kt` · `MainActivity.kt` | chủ repo | — | sửa. Cần khởi tạo cấp app (WorkManager, push) → mở issue |
| `core/designsystem/theme/*` (Color/Dimens/Type/Theme) | chủ repo | **xin thêm token** nếu thiếu, kèm lý do dùng ở đâu | viết số `.dp`/`.sp`/hex ở ngoài 3 file token (`docs/CODING_STANDARDS.md` mục 7) |
| `core/designsystem/component/*` | chủ repo | dùng; bug component → mở issue + test tái hiện | thêm variant mới "cho riêng" module mình — cái đó thành composable trong `presentation/<module>/` |
| `app/src/main/res/values/strings.xml` + `values-en/` | chủ repo merge | **thêm key có prefix module** (`events_…`, `timetable_…`), cộng dòng ở cuối file, luôn đủ cả vi + en | sửa key của module khác; dịch lại chuỗi người khác; xoá key |
| `app/src/main/res/drawable/` | chủ repo merge | thêm `ic_<module>_<tên>.xml` — vector **two-tone** tải từ `google/material-design-icons` (Apache-2.0), đã bỏ `?attr/colorControlNormal` để màu do `Icon(tint=…)` cấp; xem `docs/CODING_STANDARDS.md` mục 7 | đổi icon design-system của người khác |
| `gradle/libs.versions.toml` · `build.gradle.kts` (gốc & app) · `gradle.properties` | chủ repo | yêu cầu dependency + mục đích | tự thêm plugin/annotation processor — KSP, Hilt, serialization đã bật sẵn, chỉ việc dùng |
| `test/testing/MainDispatcherRule.kt` | chủ repo | dùng trong test của bạn | sửa; cần rule khác → tạo trong `test/presentation/<module>/` |
| `presentation/common/ServerErrorMessage.kt` | Đức (`feature/checking-duc-v2`) | gọi `ServerErrorCode.messageText()` từ Screen/Route của module mình; thêm `when` nhánh khi migration khai mã mới | nhét nghiệp vụ riêng của một module vào đây; import file từ `presentation/<module>/` khác |
| `domain/qr/QrFrameDecoder.kt` | Đức (`feature/checking-duc-v2`) | dùng trong máy quét nào cần giải mã frame YUV | chuyển thành lớp Android (CameraX/Bitmap) — mất khả năng test JVM |
| `docs/process/REPO_TREE.md` (file này) | chủ repo | cập nhật mục 2/3 khi cây bạn đổi (bắt buộc, xem header) | tạo một tài liệu phân công thứ hai song song |

Không gian tên `res/` là chung nên dễ trùng nhất: tên file resource **phải unique toàn app**; prefix module là cách rẻ nhất để đạt điều đó.

**Hợp đồng navigation:** giữ bốn tab `Home`, `Schedule`, `Utilities`, `Profile`. Route màn con khai báo `@Serializable` trong `<Module>Navigation.kt`; đăng ký `composable<T>` qua `NavGraphBuilder.<module>Graph(...)`, theo `announcementGraph`. Chủ repo nối graph vào `UthApp.kt`; `Route` nhận callback điều hướng và lấy ViewModel, `Screen` nhận state/action. Không đặt `NavHost` lồng trong tab cho các màn con cần ẩn bottom bar: `UthApp` chỉ quan sát back stack của host chính để xác định tab hiện tại. Với `Schedule`, giữ placeholder hiện tại đến khi tích hợp route của owner; không tạo ViewModel giả.

---

## 6. Nghi thức gộp cây (chủ repo chạy khi cả 5 nhánh xong)

Thứ tự gộp: `auth` → `events` (+`tickets`/`scanner`) → `timetable` → `lostfound`. Lý do: auth là dependency của mọi luồng có MSSV; events là MVP; lostfound kế thừa cây `utilities` đã có nên vào cuối để không giẫm mẫu.

Mỗi một module, làm đúng 6 bước:

```powershell
cd "d:\UTH Connect"
git checkout develop; git pull
git merge --no-ff feature/<module>-<tên>     # một PR = một module, không gộp hai module
# 1. đường biên tầng còn đúng? (pattern dưới đây tìm dấu hiệu code cũ quay lại: repository gọi từ UI, AppContainer/LocalAppContainer bị tái tạo)
Select-String -Path app\src\main\java\com\giathinh\uthconnect\presentation -Pattern 'repository\.', 'LocalAppContainer' -Recurse
# 2. không literal ngoài 3 file token? (lệnh ở CODING_STANDARDS mục 7)
# 3. build + test (ksp chạy lại toàn bộ graph DI)
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
# 4. cộng đúng một dòng route vào UthApp.kt / UthDestination.kt (nếu PR chưa làm)
# 5. cập nhật docs/process/REPO_TREE.md mục 2 + 3 + nhật ký 8
# 6. git push origin develop
```

Ba chỗ va chạm khi gộp và cách xử lý:
- `strings.xml` / `values-en/` conflict vì cả hai đều thêm ở cuối file → giữ cả hai phía, sort lại theo prefix module.
- `UthApp.kt` conflict khi hai branch cùng chèn route gần nhau → chủ repo quyết thứ tự, module nào merge trước đứng trước.
- Trùng `domain/model` (VD hai module cùng định nghĩa `Campus`) → **không** nhân bản; đẩy về `domain/common` do chủ repo sở hữu, rồi hai bên import chung.

Sau khi gộp xong toàn bộ: chạy ``.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest`` một lần cuối, cập nhật `README.md` mục trạng thái, và gắn tag `M1`.

---

## 7. Khi nào phải sửa file này

| Biến cố | Sửa ở đâu |
|---|---|
| Tạo package mới (dù rỗng) | mục 2 + mục 3 |
| Xoá/rename package hoặc file thuộc một tầng | mục 2 + nhật ký 8 |
| Đổi quyền sở hữu một file | mục 3 + mục 5 |
| Thêm file dùng chung mới (VD `core/di/DispatchersModule.kt`) | mục 4 + mục 5 |
| Thêm dependency build làm đổi cách tạo file (Room, WorkManager, kotlinx-serialization) | mục 3 + mục 5 + nhật ký 8 |
| Module của bạn đạt DoD và được merge | đánh dấu ✅ ở mục 3, cập nhật mục 2 |

Lệnh lấy lại cây thật để dán vào mục 2 (chạy từ `app/src`, xoá dòng có `build`):

```powershell
cd "d:\UTH Connect\app\src"; Get-ChildItem -Recurse -Directory | ForEach-Object { $_.FullName.Replace("D:\UTH Connect\app\src\","") } | Where-Object { $_ -notmatch '\\build' }
```

Không cần giữ cây đẹp theo ngữ pháp `tree` — cái cần là **tên package + cột owner đúng thực tế**.

---

## 8. Nhật ký cấu trúc

| v1.10b-housing-ux | 10/11/2026 | Ba fix trải nghiệm Ghép trọ sau test tay: (1) feed không tự nạp lại sau khi đăng tin/đánh dấu hết phòng ở màn con — `HousingRoute` thêm `LifecycleEventEffect(ON_RESUME)` phát `HousingAction.Refresh` (action mới, nạp nền không nháy spinner, theo tiền lệ `AttendanceRoute`); (2) ảnh tải chậm — `SupabaseHousingRepository.downscaleJpeg` thu nhỏ cạnh dài tối đa 1600px + nén JPEG 85 trước khi upload (ảnh gốc vài MB chỉ còn vài trăm KB); (3) giá màn detail dùng locale `vi` ra dấu phẩy — đổi sang `vi-VN` khớp feed (`1.800.000đ`). |
| v1.10-housing-supabase | 10/11/2026 | **Housing bỏ mocking, chạy Supabase thật.** Phát hiện staging giữ schema cũ do hai migration `20261107000000` cùng tạo `housing_posts` lệch nhau (`if not exists` bỏ qua âm thầm bản sau): gỡ phần housing khỏi `auth_rbac_and_housing`, `create_housing_posts` thành no-op superseded, thêm `20261108000000_housing_schema_rebuild.sql` dựng lại bảng khớp `HousingListing` (RLS sở hữu, trigger giới hạn 2 tin mở khóa dòng chống race, trigger chặn tự đổi status/report_count, RPC `mark_housing_listing_fulfilled`/`report_housing_listing` ngưỡng 3 báo cáo, bucket `housing-images` public-read + policy ghi theo thư mục `auth.uid()`). Thêm `data/repository/SupabaseHousingRepository.kt` (CRUD + report RPC, upload ảnh `content://` lên Storage, dọn ảnh mồ côi khi thất bại, khoảng cách luôn tính qua `CampusReference`); gỡ `InMemoryHousingRepository` + test mirror. `SupabaseRestClient` thêm `deleteTable`/`uploadStorageObject`/`deleteStorageObject`. `ServerErrorCode` thêm 4 mã `HOUSING_*` + key `common_error_housing_*` song ngữ; `CreateHousingPostEvent.SubmitFailed(code)` để snackbar nêu đúng nguyên nhân. `HousingDataModule` bind bản Supabase. |
| v1.9-housing-full | 07/11/2026 | **Hoàn thiện trọn vẹn Module Ghép trọ (Housing) theo Jira.** Thêm package `presentation/housing/detail/` (5 file: Screen, Route, ViewModel, UiState, Action — carousel ảnh phòng, card sinh viên xác thực UTH kèm MSSV, tiện ích phòng, gọi điện / mở Zalo trực tiếp, đánh dấu hết phòng) và `presentation/housing/create/` (5 file: Screen, Route, ViewModel, UiState, Action — form đăng tin, chọn cơ sở gần nhất, tiện ích, tiêu chí bạn trọ, liên hệ). Nâng cấp `HousingScreen.kt` với 3 hàng chip lọc nhanh theo Jira (Cơ sở CS1/CS2/CS3, 3 khoảng giá <2tr/2-3.5tr/>3.5tr, Giới tính Nam/Nữ/Bất kỳ); cập nhật `HousingNavigation.kt` (`HousingDetailDestination`, `CreateHousingPostDestination`, `housingGraph` đủ 3 route). Mở rộng domain: `HousingPriceRange`, `GetHousingListingDetailUseCase`, `CreateHousingListingUseCase`, `MarkHousingListingFulfilledUseCase`, các trường liên hệ (`contactPhone`, `contactZaloUrl`), `hostStudentId`, ảnh và tiện ích mới (`MEZZANINE`, `AIR_CONDITIONER`, `FREE_HOURS`, `GOVERNMENT_UTILITY_RATES`). `InMemoryHousingRepository` hoàn thiện CRUD và 7 tin mẫu chuẩn. Migration Supabase `20261107000000_create_housing_posts.sql` với RLS. Thêm unit test ViewModel cho cả Detail và Create, tất cả test housing xanh. |
| v1.9-housing | 07/11/2026 | **Slice Ghép trọ đầu tiên (mock).** Thêm `presentation/housing/` đủ 5 file + `HousingNavigation.kt` (`HousingDestination` + `housingGraph`); domain `HousingListing`/`CampusReference` (haversine từ toạ độ chuẩn CS1/CS2/CS3, không gọi API map); repository interface + 2 use case; `InMemoryHousingRepository` + `HousingDataModule`. `UthApp.kt` nối graph và nhánh `UtilityModuleId.HOUSING`; `StaticUtilityModuleRepository` bật ô HOUSING (nhãn vẫn Backlog). Kết quả báo tin đi kênh event một lần (snackbar ở Route), lỗi feed bằng enum `HousingFailure`; 59 key `housing_*` song ngữ; 25 unit test mới (ViewModel/UiState/CampusReference/InMemory) + `HousingScreenUiTest` (androidTest, 5 test luồng chính). `HousingListing.distanceToCampusM` mặc định 0 để repository tự tính qua `CampusReference`; factory tin mẫu nhận thẳng `HousingListing`. Baseline detekt thêm 20 mục cấu trúc Compose (preview `UnusedPrivateMember`, `TooManyFunctions` của Screen, `MatchingDeclarationName` của Navigation, vài dòng dài) theo tiền lệ v1.8; `spotlessApply` chuẩn hoá format. Chưa có đăng tin/chi tiết/liên hệ, chưa migration Supabase, chưa Maps SDK — phạm vi Maps đã chốt là LocationPicker khi đăng + deep-link khi xem, không embed map. |
| v1.6e-conv | 04/11/2026 | Sửa hai lệch chuẩn kiến trúc trong cây của check-in. `presentation/tickets/TicketsScreen.kt` không còn chạm `Context`: clipboard + toast xác nhận chuyển hết sang `TicketQrRoute` (Route là nơi hợp lệ để lấy `LocalContext`), Screen nhận callback `onCopy: (String) -> Unit`; chuỗi "Đang cấp mã QR bảo mật…" cứng trong Screen thành key `tickets_qr_issuing` (vi+en); `TicketsRoute` đổi import `hiltViewModel` sang `androidx.hilt.lifecycle.viewmodel.compose` theo mục 13 (#8) CODING_STANDARDS. `presentation/attendance/` bỏ lỗi-bằng-chuỗi: `AttendanceUiState.error: String?` thay bằng `failure: AttendanceFailure` (`LoadFailed`/`ExportFailed`) + `errorCode: ServerErrorCode?` + `errorDetail: String?`, `AttendanceViewModel.showError("EXPORT")` thay bằng `showExportFailure(reason)`, Screen hết so `"FORBIDDEN"`/`"EXPORT"` và dùng `messageText()` chung; key `attendance_forbidden` thành chuỗi thừa (giữ lại vì `strings.xml` là file nóng, không xoá). Thêm 2 unit test attendance (tổng 119 test/23 suite, 0 failure). |
| v1.6d-structure | 04/11/2026 | Chuẩn hoá cấu trúc theo `docs/CODING_STANDARDS.md` mục 3.1/3.2/5 cho các file mới của `feature/checking-duc-v2`. Tách `presentation/scanner/QrScanState.kt` thành `QrScannerUiState.kt` (enum `QrScannerOutcome` + data class `QrScannerUiState`) và `QrScannerAction.kt`, đổi tên loại `QrScanUiState/QrScanAction/QrScanOutcome` → `QrScanner*` để khớp khuôn `<Module>…`; chuyển `QrFrameDecoder` từ `presentation/scanner/` xuống `domain/qr/` (kèm test mirror `test/domain/qr/`) để scanner không có file thứ sáu; rename 23 key `error_*` → `common_error_*` (theo tiền lệ `common_retry`) và 12 key `event_editor_*` → `events_editor_*` cho đúng quy ước prefix module. Ghi rõ hai ngoại lệ vào mục 2: scanner không có ViewModel vì `PreviewView.SurfaceProvider` thuộc về View, còn `presentation/common/ServerErrorMessage.kt` là `@Composable` nên không xuống `domain`/`core` được. Bổ sung dòng sở hữu vào mục 3 và bảng file chung mục 5. Build + 23 suite/117 test, 0 failure. |
| v1.6c-errors | 04/11/2026 | Mã lỗi server không còn hiện nguyên văn lên UI. Thêm `domain/model/ServerErrorCode.kt` — enum mô phỏng đúng 21 mã `raise exception` trích từ `supabase/migrations` cùng `fromMessage()`/`of()` (ưu tiên token dài, phân biệt `IOException` → `Network`); thêm package `presentation/common/` với `ServerErrorMessage.kt` cung cấp `ServerErrorCode.messageText(fallback)` dịch sang câu có hướng xử lý. `EventsUiState.errorCode` + `TicketsUiState.qrErrorCode` mang mã từ ViewModel tới Route/Screen (ViewModel không đụng Context); `EventsScreen`, `EventsRoute` (banner máy quét) và `TicketsRoute` (panel QR vé) ưu tiên câu dịch, chỉ in chuỗi thô khi mã là `Unknown`. 23 key `error_*` song ngữ; unit test `test/domain/model/ServerErrorCodeTest.kt` (7 test, có test hợp đồng liệt kê toàn bộ mã). |
| v1.6b-events | 04/11/2026 | Sửa logic tạo/sửa sự kiện trong `presentation/events/EventsScreen.kt`: mặc định của sự kiện mới đổi từ "bây giờ + 24 giờ" sang `defaultEventWindow()` = bây giờ + 60 phút (làm tròn lên bội số 5 phút) kéo dài 120 phút, hết cảnh người tạo chỉ sửa giờ và quên sửa ngày; hộp thoại hiện **cửa sổ điểm danh** tính theo `start_at − 30 phút … end_at + 30 phút` (khớp `check_in_ticket_qr`) kèm cảnh báo còn bao lâu nữa mới quét được; validation tách từng trường với thông báo riêng thay vì một câu gộp, và chỉ bắt giờ bắt đầu ở tương lai khi *tạo mới* (sửa thì server cho phép lùi giờ). Ba hàm quy đổi thời gian thành `internal` để test; thêm 12 key `event_editor_*` song ngữ; unit test mới `test/presentation/events/EventEditorTimeTest.kt` (6 test). |
| v1.6-checkin | 04/11/2026 | Máy quét QR điểm danh chuyển từ "mượn app Camera hệ thống chụp một tấm JPEG" sang quét live trong app: thêm package `presentation/scanner/` (`QrFrameDecoder` thuần JVM, `QrScanState`, `QrScannerScreen` stateless, `QrScannerRoute` sở hữu CameraX), dependency `androidx.camera` 1.6.2 (core/camera2/lifecycle/view), quyền `CAMERA` + ba `uses-feature` không bắt buộc, hai drawable `ic_scanner_torch_on/off`, 17 key `scanner_*` song ngữ, và `EventsViewModel.checkedInCount` + `clearCheckInResult()` thay `showError()`. `presentation/events/EventsRoute.kt` không còn FileProvider/TakePicture. Unit test `test/presentation/scanner/QrFrameDecoderTest.kt`. Migration `20261104000000_stable_ticket_qr.sql` đổi token sang `eventId.uid.hmac` không hết hạn. |

| v1.8-schedule-settings | 05/11/2026 | **Thời khóa biểu token-hoá + Cài đặt đổi ngôn ngữ/giao diện thật.** Không thêm package mới. `presentation/schedule/ScheduleScreen.kt` viết lại 886 dòng: hết hex/dp/sp tự do, xoá 3 composable chết (`SimpleQrPreview`, `QrTicketDialog`, `EventDetailsDialog`) và bỏ import chéo tầng `presentation.tickets`; thêm token `UthExtendedColors.headerGradientStart/End/headerOnGradient`, `Size.AccentStripe`, `Size.IndicatorDot`, `Opacity.Header*`, `Layout.FeatureGridColumns/ServiceGridColumns/ServiceGridColumnsLargeText`. `UtilitiesScreen.kt`: lưới đều đáy bằng `height(IntrinsicSize.Min)`, `TextOverflow.Ellipsis` mọi nhãn, 3 cột hạ 2 cột khi `fontScale ≥ LargeTextScale`, `UtilityTile` thành `@Immutable` không giữ lambda. Ngôn ngữ/theme: `AppSettings` thêm `languageTag`/`themeMode` (enum `ThemeMode` + `resolveDarkTheme` — đặt ở domain vì mục 3.1 cấm `core/designsystem` import `domain`), `SettingsRepository` thêm `setLanguageTag`/`setThemeMode`/`currentLanguageTag()` đồng bộ, `MainActivity.attachBaseContext` áp Locale qua `@EntryPoint` (file mới `SettingsEntryPoint.kt`) + `applyOverrideConfiguration`, resource mới `res/xml/locales_config.xml` và `android:localeConfig` trong manifest, pill VI/EN của AuthScreen nối vào store thật qua `AuthEvent.LanguageChanged`. `UtilitiesAction` nhận 4 case cài đặt để `UtilitiesViewModel` không vượt `TooManyFunctions`. **Chú ý cho cả 5 nhánh**: `spotlessApply` chạy toàn repo theo thoả thuận phiên này — 140 file đổi format, `config/detekt/baseline.xml` regenerate (53 thêm/84 xoá); `strings.xml`, `Dimens.kt`, `Theme.kt`, `baseline.xml` là file nóng, nhánh khác phải rebase sớm. Build + 210 test/35 suite 0 failure, `detekt spotlessCheck` xanh. |
| v1.7-utilities | 05/11/2026 | **Bottom nav về đúng 4 tab + trang Tiện ích thành lưới.** `UthDestination.kt` bỏ entry `TICKETS`/`EVENTS`; route Sự kiện chuyển sang file mới `presentation/events/EventsNavigation.kt` (`EventsDestination` + `eventsGraph`); Vé vào bằng `ProfileNavRow` trong tab Cá nhân (`ProfileRoute` thêm callback `onOpenTickets`). `UtilitiesScreen.kt` viết lại theo hai khu vực tile (2 cột mở được / 3 cột lộ trình), 11 vector two-tone mới `res/drawable/ic_utilities_*` + `ic_profile_tickets`, 3 token `Size.Utility*` trong `Dimens.kt`. Xoá 2 key string mồ côi (`tab_events`, `utilities_not_available`), thêm `utilities_section_mine` vi+en. |

| v1.5-checkin | 03/11/2026 | Thêm slice `presentation/attendance/`, domain model/repository/use case cho danh sách điểm danh và CSV, Supabase repository + Hilt binding, tests; nối graph type-safe từ sự kiện. Màn danh sách đọc `registrations` và `attendance` từ Supabase. Thêm migration `60000_preserve_attendance_history` và kiểm tra constraint trong SQL contract. |

| Bản | Ngày | Thay đổi cấu trúc |
| v1.5-checkin | 03/11/2026 | QR máy đọc được bằng ZXing core; QR payload trong Ticket.kt, scanner xử lý mã đầy đủ; MockTicketRepository giữ Attendance trong bộ nhớ; thêm ScannerViewModelTest và TicketsQrEncodingTest. |
| v1.3.1-auth | 31/10/2026 | AUTH-2.1: retry button on login errors; AuthAction.Retry, two ViewModel tests and AuthScreenUiTest. No other module changed. |
| v1.3-auth | 31/10/2026 | AUTH-1/V3: UserRole and uthVerified, 2 demo accounts, badges, Retry logout, one-off events and tests. Owner review pending. |
| v1.2-auth | 31/10/2026 | Auth/Profile: doi mat khau PBKDF2 demo, thong tin lien he, Android BiometricPrompt, confirm logout, test va backup exclusions. Owner review pending. |
|---|---|---|
| v1.3 | 31/10/2026 | Phát hiện `origin/develop` tại `3f545c3` đã merge PR #20: bổ sung 5 file presentation Lost & Found, 6 file domain/data, 1 file test cùng strings VI/EN. Chưa nối route; KAN-49 có lỗi dữ liệu liên hệ mặc định. CORE-1 ban đầu: 35 tests/9 suites; `origin/develop` sau merge: `assembleDebug` thành công, **42 tests/10 suites, 0 failures** (worktree kiểm thử riêng). Chưa smoke-test APK. |
| v1.4 | 31/10/2026 | Nhánh tích hợp `codex/core-integration`: merge `origin/develop` ở `24117e7`, bổ sung `presentation/lostfound/LostFoundNavigation.kt` (destination type-safe + graph), chuẩn bị điều hướng từ Utilities; module vẫn tắt, chờ Thắng sửa lỗi KAN-49. Build thành công, 42 unit tests/10 suites đều qua; APK `adb9449` smoke-test trên Samsung SM-G990E thành công với đăng nhập demo, bốn tab và mở thông báo/Back; kiểm tra card Sự kiện chưa sẵn sàng. Các module chưa tích hợp vẫn cần test sau khi sẵn sàng. |
| v1.2 | 31/10/2026 | Đối chiếu cây checkout `main` và cập nhật phân công theo Jira KAN-42–49; sửa checksum Gradle 8.13-bin. Chưa xác nhận build/test hoặc trạng thái merge các feature. |
| v1.1 | 29/10/2026 | **Navigation chuyển type-safe.** `UthDestination.kt`: 4 destination thành `@Serializable object` (`HomeDestination`…), enum `UthDestination` giữ nhãn/icon + `routeClass`; `fromRoute(String)` → `from(NavDestination)` dùng `hasRoute`. `UthApp.kt`: `composable<T>`, `navigate(T)`, `popUpTo(T)`. Build: thêm plugin `org.jetbrains.kotlin.plugin.serialization` (khớp Kotlin 2.2.10) + `kotlinx-serialization-json 1.9.0`; `app/build.gradle.kts` và gốc bật plugin. Không thêm/bớt package nào. `:app:assembleDebug` + `:app:testDebugUnitTest` xanh (5 test). |
| v1.2 | 31/10/2026 | CHECKIN-1: thêm `domain/model/Ticket.kt` + `Attendance.kt`, repository/use case lọc theo `studentId` từ AUTH-1, mock data + Hilt binding, `presentation/tickets/` (danh sách, route type-safe tới QR text placeholder, countdown 60 giây), song ngữ strings, module README và 5 unit test. Thêm tab Vé để màn danh sách có lối vào. |
| v1.0 | 29/10/2026 | **Chuyển DI sang Hilt.** Xoá `core/di/AppContainer.kt` + `core/di/LocalAppContainer` (không còn `ProvidableCompositionLocal` mang dependency), xoá `presentation/utilities/UtilitiesViewModelFactory.kt` → module còn **5 file** thay vì 6. Thêm `UTHConnectApplication.kt` (`@HiltAndroidApp`), `data/di/UtilitiesDataModule.kt` (`@Binds`), manifest khai `android:name`. `MainActivity` gắn `@AndroidEntryPoint`; `UtilitiesRoute` dùng `hiltViewModel()` (artifact `androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0`). Build: KSP 2.3.12 + Hilt 2.60.1 trên AGP 9.3.3 built-in Kotlin — `:app:assembleDebug` + `:app:testDebugUnitTest` xanh. |
| trước v1.0 | 28/10/2026 | Dựng cây Clean Architecture `core/ · data/ · domain/ · presentation/`; xoá `ui/theme/` của template Android Studio (chuyển vào `core/designsystem/theme/`); thêm slice MVVM mẫu ở `presentation/utilities/` + `test/testing/MainDispatcherRule.kt`. |

### TIMETABLE-1 — bổ sung cây thật ngày 31/10/2026 (mục 2/3)

- domain/model/Timetable.kt: Course, ClassSession, Deadline, Timetable (java.time).
- domain/repository/TimetableRepository.kt; domain/usecase/TimetableUseCase.kt.
- data/repository/InMemoryTimetableRepository.kt; data/di/TimetableDataModule.kt.
- presentation/schedule/: đúng 5 file Screen/Route/ViewModel/UiState/Action, phụ trách Nghĩa.
- test/domain/usecase/TimetableUseCaseTest.kt: mock, filter/sort, validation và hoàn tất.

Nhật ký mục 8: 31/10/2026 — Nghĩa triển khai TIMETABLE-1 in-memory; nối ScheduleRoute
vào ScheduleDestination @Serializable hiện có. Không thêm dependency hoặc thay token theme.
