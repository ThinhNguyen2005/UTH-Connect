# UTH Connect — Cây thư mục & phân quyền sở hữu

> **Phiên bản:** v1.1 · **Cập nhật cấu trúc lúc:** 29/10/2026 · **Người giữ tài liệu:** chủ repo (`@giathinh`)
> **Phạm vi:** cây thư mục `app/src`, ai sở hữu nhánh nào, file nào được tạo/sửa trong phạm vi đó, và nghi thức gộp cây khi cả nhóm xong việc.
> **Không chứa:** quy ước viết code (`CODING_STANDARDS.md`), spec nghiệp vụ từng module (`README.md`, `Design.md`), token UI (`Design-system.md`), quy trình nhánh/PR (`docs/GIT_WORKFLOW.md`).
>
> **⚠ Luật bảo trì — đây là điều kiện merge, không phải lời khuyên:** mọi PR làm **thay đổi cấu trúc thư mục** (thêm/xoá package, thêm/xoá file thuộc một tầng, đổi chủ sở hữu, thêm dependency build làm đổi cây) **bắt buộc** phải cập nhật file này trong cùng commit: sửa mục 2 (cây thật), mục 3 (bảng phân công), thêm một dòng vào nhật ký mục 8. PR đổi cấu trúc mà không đổi `REPO_TREE.md` → review từ chối, không cần lý do khác.

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

```
app/src/main/java/com/giathinh/uthconnect/
├── MainActivity.kt                     @AndroidEntryPoint — điểm vào DI duy nhất     [chủ repo]
├── UTHConnectApplication.kt            @HiltAndroidApp — gốc SingletonComponent      [chủ repo]
├── core/
│   └── designsystem/
│       ├── component/  UthComponents.kt · UthStatusBadge.kt                          [chủ repo]
│       └── theme/      Color.kt · Dimens.kt · Theme.kt · Type.kt                     [chủ repo]
├── data/
│   ├── di/            UtilitiesDataModule.kt      ← binding interface→impl           [utilities]
│   └── repository/    StaticUtilityModuleRepository.kt                               [utilities]
├── domain/
│   ├── model/         UtilityModule.kt                                                [utilities]
│   ├── repository/    UtilityModuleRepository.kt                                      [utilities]
│   └── usecase/       GetUtilityModulesUseCase.kt                                     [utilities]
└── presentation/
    ├── navigation/    UthApp.kt · UthDestination.kt                                   [chủ repo]
    ├── home/          HomeScreen.kt          (placeholder, chưa MVVM)                 [events]
    ├── schedule/      ScheduleScreen.kt      (placeholder, chưa MVVM)                 [timetable]
    ├── profile/       ProfileScreen.kt       (placeholder, chưa MVVM)                 [auth]
    └── utilities/     Screen · Route · ViewModel · UiState · Action                   [lost&found]
                       ↑ module mẫu, 5 file — đã đạt checklist CODING_STANDARDS mục 12

app/src/test/java/com/giathinh/uthconnect/
├── testing/           MainDispatcherRule.kt      (dùng chung — chỉ đọc, đừng sửa)     [chủ repo]
└── presentation/utilities/  UtilitiesViewModelTest.kt  (4 test, đang xanh)            [utilities]
```

Trống nhưng còn trên đĩa: `core/di/` (xoá cùng `AppContainer.kt` khi chuyển sang Hilt — **không** tái tạo nếu chưa có provider dùng chung), `ui/` (rác template Android Studio, git đã xoá file, chỉ còn thư mục rỗng ở máy local).

---

## 3. Cây đích: mỗi người một nhánh

Phân công khớp `docs/GIT_WORKFLOW.md` mục 3. Mỗi ô "được tạo" là cây riêng — ngoài nó, xem mục 5.

| Người | Nhánh | `presentation/` | `domain/` | `data/` | `test/` mirror |
|---|---|---|---|---|---|
| **Anh** | `feature/events-qr-checkin-anh` | `events/` (5 file MVVM) · `tickets/` · `scanner/` · `home/` (nâng HomeScreen thành slice thật) | `model/Event.kt` `model/Registration.kt` `model/Ticket.kt` · `repository/EventRepository.kt` `repository/AttendanceRepository.kt` · `usecase/GetEventsUseCase.kt` `CheckInUseCase.kt` … | `repository/` `remote/firestore/EventFirestoreRepository.kt` · `di/EventsDataModule.kt` | `presentation/events/` `presentation/tickets/` `presentation/scanner/` |
| **Bạn B** | `feature/checkin-<tên>` | gộp vào nhánh events (QR + máy quét là một luồng) — nếu tách người thì tạo `presentation/scanner/` và **thỏa thuận boundary với Anh trước**, không tự chia | `model/Attendance.kt` | `di/CheckInDataModule.kt` | `presentation/scanner/` |
| **Bạn C** | `feature/auth-<tên>` | `auth/` · `profile/` | `model/User.kt` `model/Faculty.kt` · `repository/AuthRepository.kt` `repository/ProfileRepository.kt` · `usecase/SendSchoolOtpUseCase.kt` `SignInUseCase.kt` `GetProfileUseCase.kt` | `remote/auth/SchoolEmailAuthRepository.kt` · `di/AuthDataModule.kt` | `presentation/auth/` `presentation/profile/` |
| **Bạn D** | `feature/timetable-<tên>` | `schedule/` | `model/Course.kt` `model/Deadline.kt` `model/StudyGroup.kt` · `repository/TimetableRepository.kt` `repository/DeadlineRepository.kt` · `usecase/GetWeekScheduleUseCase.kt` `ScheduleReminderUseCase.kt` | `local/room/…` (Room) · `di/TimetableDataModule.kt` | `presentation/schedule/` |
| **Bạn E** | `feature/lostfound-<tên>` | `utilities/` (đang có — kế thừa nguyên slice mẫu) · `lostfound/` | `model/LostFoundListing.kt` · `repository/LostFoundRepository.kt` · `usecase/…` | `repository/` `remote/firestore/LostFoundFirestoreRepository.kt` · `di/LostFoundDataModule.kt` | `presentation/utilities/` `presentation/lostfound/` |
| **Chủ repo** | `chore/<việc>` | `navigation/` | `domain/common/` (kết quả phân trang, `DeliveryPhase` — chỉ để thứ thật sự dùng chung) | — | `testing/` |

Quy tắc đặt tên file trong cây: theo `CODING_STANDARDS.md` mục 2 và 3.2 — một module có đúng **5 file** trong `presentation/<module>/` (Hilt đã xoá bỏ file factory thứ sáu), thêm `<Module>Navigation.kt` khi module có route con.

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
| `presentation/navigation/UthApp.kt` | chủ repo | nhờ thêm **một dòng** `composable<XDestination> { XRoute() }` khi route của bạn đủ chín | đổi `Scaffold`, `popUpTo`, transition, hay tự ý thêm route |
| `presentation/navigation/UthDestination.kt` | chủ repo | gửi tên `@Serializable object`/tab cần thêm | sửa route của module khác; đổi cơ chế type-safe |
| `UTHConnectApplication.kt` · `MainActivity.kt` | chủ repo | — | sửa. Cần khởi tạo cấp app (WorkManager, push) → mở issue |
| `core/designsystem/theme/*` (Color/Dimens/Type/Theme) | chủ repo | **xin thêm token** nếu thiếu, kèm lý do dùng ở đâu | viết số `.dp`/`.sp`/hex ở ngoài 3 file token (`CODING_STANDARDS.md` mục 7) |
| `core/designsystem/component/*` | chủ repo | dùng; bug component → mở issue + test tái hiện | thêm variant mới "cho riêng" module mình — cái đó thành composable trong `presentation/<module>/` |
| `app/src/main/res/values/strings.xml` + `values-en/` | chủ repo merge | **thêm key có prefix module** (`events_…`, `timetable_…`), cộng dòng ở cuối file, luôn đủ cả vi + en | sửa key của module khác; dịch lại chuỗi người khác; xoá key |
| `app/src/main/res/drawable/` | chủ repo merge | thêm `ic_<module>_<tên>.xml` (vector tự vẽ) | đổi icon design-system của người khác |
| `gradle/libs.versions.toml` · `build.gradle.kts` (gốc & app) · `gradle.properties` | chủ repo | yêu cầu dependency + mục đích | tự thêm plugin/annotation processor — KSP, Hilt, serialization đã bật sẵn, chỉ việc dùng |
| `test/testing/MainDispatcherRule.kt` | chủ repo | dùng trong test của bạn | sửa; cần rule khác → tạo trong `test/presentation/<module>/` |
| `REPO_TREE.md` (file này) | chủ repo | cập nhật mục 2/3 khi cây bạn đổi (bắt buộc, xem header) | tạo một tài liệu phân công thứ hai song song |

Không gian tên `res/` là chung nên dễ trùng nhất: tên file resource **phải unique toàn app**; prefix module là cách rẻ nhất để đạt điều đó.

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
# 5. cập nhật REPO_TREE.md mục 2 + 3 + nhật ký 8
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

| Bản | Ngày | Thay đổi cấu trúc |
|---|---|---|
| v1.1 | 29/10/2026 | **Navigation chuyển type-safe.** `UthDestination.kt`: 4 destination thành `@Serializable object` (`HomeDestination`…), enum `UthDestination` giữ nhãn/icon + `routeClass`; `fromRoute(String)` → `from(NavDestination)` dùng `hasRoute`. `UthApp.kt`: `composable<T>`, `navigate(T)`, `popUpTo(T)`. Build: thêm plugin `org.jetbrains.kotlin.plugin.serialization` (khớp Kotlin 2.2.10) + `kotlinx-serialization-json 1.9.0`; `app/build.gradle.kts` và gốc bật plugin. Không thêm/bớt package nào. `:app:assembleDebug` + `:app:testDebugUnitTest` xanh (5 test). |
| v1.0 | 29/10/2026 | **Chuyển DI sang Hilt.** Xoá `core/di/AppContainer.kt` + `core/di/LocalAppContainer` (không còn `ProvidableCompositionLocal` mang dependency), xoá `presentation/utilities/UtilitiesViewModelFactory.kt` → module còn **5 file** thay vì 6. Thêm `UTHConnectApplication.kt` (`@HiltAndroidApp`), `data/di/UtilitiesDataModule.kt` (`@Binds`), manifest khai `android:name`. `MainActivity` gắn `@AndroidEntryPoint`; `UtilitiesRoute` dùng `hiltViewModel()` (artifact `androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0`). Build: KSP 2.3.12 + Hilt 2.60.1 trên AGP 9.3.3 built-in Kotlin — `:app:assembleDebug` + `:app:testDebugUnitTest` xanh. |
| trước v1.0 | 28/10/2026 | Dựng cây Clean Architecture `core/ · data/ · domain/ · presentation/`; xoá `ui/theme/` của template Android Studio (chuyển vào `core/designsystem/theme/`); thêm slice MVVM mẫu ở `presentation/utilities/` + `test/testing/MainDispatcherRule.kt`. |
