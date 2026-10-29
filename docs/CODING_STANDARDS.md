# UTH Connect — Coding Standards

> **Phiên bản:** v1.2 · **Phạm vi:** *cách viết code* — đặt tên, ranh giới tầng, Kotlin/Compose style, coroutines, navigation type-safe, test, tài nguyên.
> **Không chứa:** nghiệp vụ & phạm vi (xem `docs/Design.md`), token màu/chữ/spacing (xem `docs/Design-system.md`), nhánh & phân quyền Git (xem `docs/GIT_WORKFLOW.md`), cây thư mục & quyền sở hữu file (xem `docs/REPO_TREE.md`), spec từng module (xem `README.md`).
> **Ép thực hiện:** chưa có ktlint/spotless/CI — mọi mục dưới đây đang là chuẩn **review bằng mắt**. Khi CI vào (M0), mục 12 đổi thành chuẩn **bắt buộc bởi máy**.

## 0. Quyền ưu tiên khi mâu thuẫn

| Câu hỏi thuộc về | File quyết |
|---|---|
| "Có làm tính năng này không, ai được làm?" | `docs/Design.md` |
| "Màu này, bo góc này, tương phản này đúng không?" | `docs/Design-system.md` |
| "Đặt file ở đâu, đặt tên ra sao, tầng nào được gọi tầng nào?" | **tài liệu này** |
| "File này của ai, tôi được tạo/sửa những gì?" | `docs/REPO_TREE.md` |
| "Nhánh nào, PR vào đâu, ai approve?" | `docs/GIT_WORKFLOW.md` |

Code và tài liệu này lệch nhau → **sửa code trước, chỉ sửa tài liệu khi có quyết định mới được ghi rõ ở PR.**

---

## 1. Nền tảng

| Hạng mục | Chuẩn |
|---|---|
| Ngôn ngữ | Kotlin (không viết Java trong `app/src/main`) |
| UI | Jetpack Compose + Material 3, không XML layout, không View |
| Indent | 4 space, không tab |
| Bề rộng dòng | mềm 120 ký tự; vượt thì xuống dòng theo tham số |
| Brace | cùng dòng khai báo (`fun x(y: Int) {`), K&R |
| import | không dùng wildcard `*`; IDE remove unused import trước mỗi commit |
| Files cuối chuỗi | LF, kết thúc file có một dòng trống |

---

## 2. Đặt tên

| Đối tượng | Khuôn | Ví dụ |
|---|---|---|
| Package module | `presentation/<module>` — danh từ số ít, không `_` | `presentation/utilities` |
| File của module | 5 file đã chốt ở mục 3.2 | `UtilitiesScreen.kt`, `UtilitiesViewModel.kt` |
| Module Hilt | `<Module>DataModule.kt`, đặt trong `data/di/` của **chính module** | `UtilitiesDataModule`, `EventsDataModule` |
| Application | `<TênApp>Application` mở đầu bằng tên app | `UTHConnectApplication` |
| Component design system | mở đầu `Uth` | `UthCard`, `UthStatusBadge`, `UthPreview` |
| Token | `UthDimens.<Nhóm>.<Tên>`, `UthTheme.extendedColors.<tên>` | `UthDimens.Size.IconBadge` |
| Màu | `Uth<Tên>` + hậu tố `Light`/`Dark`/`Container`/`OnContainer` | `UthNavyContainerLight`, `SuccessOnContainerDark` |
| Hàm public | verb-first, không `get` vô nghĩa | `collectTicket()`, không `getCollectTicket()` |
| Composable | PascalCase, **danh từ / tính từ**, không thêm hậu tố `View`/`Screen` | `UthModuleRow`, `UtilitiesScreen` |
| Tham số Composable | `modifier: Modifier = Modifier` đặt **ngay sau tham số bắt buộc, trước mọi tham số tuỳ chọn** (chuẩn API Compose) | `UtilitiesScreen(uiState, onAction, modifier)` · `UthEmptyState(title, body, modifier, icon, …)` |
| Trạng thái riêng trong VM | `_uiState` / expose `uiState` | xem `UtilitiesViewModel` |
| Resource Android | `snake_case`, prefix module | `utilities_error_title`, `common_retry` |
| Drawable | `ic_<tên>`, `img_<tên>`, không prefix module dài | `ic_qr_ticket` |
| Test | tên lớp `<Class>Test`; method backtick mô tả hành vi | `` `action Retry - ViewModel nạp lại và xoá failure` `` |

Không đặt tên theo tầng ở cuối file: `UtilitiesViewModel`, **không** `UtilitiesViewModelClass`/`UtilitiesVM`/`UtilitiesUtil`.

---

## 3. Ranh giới tầng — phần hay bị phạm nhất

### 3.1. Chiều được phép / bị cấm

| Tầng | Được import | Bị cấm |
|---|---|---|
| `presentation/<module>/…Screen.kt` | `core/designsystem`, `domain/model`, `androidx.compose.*` | repository, use case, `hiltViewModel()`, Firestore, `Context` |
| `presentation/<module>/…Route.kt` | như Screen **cộng** `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`; `LaunchedEffect` khi module có event một lần | logic nghiệp vụ, gọi repository thẳng, tự dựng `ViewModelProvider.Factory` |
| `presentation/<module>/…ViewModel.kt` | `domain/usecase`, `domain/model`, `androidx.lifecycle`, `dagger.hilt.android.lifecycle.HiltViewModel`, `javax.inject`, `kotlinx.coroutines` | `android.content.Context`, `Modifier`, `@StringRes`, Compose, Firestore type |
| `domain/` | thuần Kotlin (`kotlin.*`) + `javax.inject` (JSR-330 — `@Inject`, `@Singleton`) | **mọi** `androidx.*`/`android.*`/`dagger.*`/`javax.hilt`, `Result` của Firestore, enum UI |
| `data/` | `domain`, Firebase/Room/Ktor, `dagger.*` (khai binding) | `presentation`, Compose |
| `data/di/<Module>DataModule.kt` | `domain/repository`, `data/repository`, `dagger`, `dagger.hilt` | `presentation`, Compose, binding **hộ** module khác |
| `core/designsystem` | `androidx.compose.*` | `domain`, `data`, `presentation` (design system không biết nghiệp vụ) |

Quy tắc nén lại một dòng: `presentation → domain ← data`, và trong `presentation`: `Route → ViewModel → UseCase`. **Không nhảy cóc.** Module A không import file của module B — muốn dùng chung thì đẩy xuống `domain`/`core`.

### 3.2. Một module mới gồm đúng 5 file

Copy nguyên `presentation/utilities/` (đang có test, đạt checklist `docs/Design-system.md` mục 8):

```
<Module>Screen.kt            stateless: nhận UiState, phát Action
<Module>Route.kt             nối ViewModel với Screen; NavHost chỉ gọi file này
<Module>ViewModel.kt         @HiltViewModel + @Inject constructor; StateFlow<UiState>
<Module>UiState.kt           data class bất biến + enum failure
<Module>Action.kt            sealed interface ý định người dùng
```

Không còn file thứ sáu. `…ViewModelFactory.kt` và `core/di/AppContainer.kt` đã bị xoá khi chuyển sang Hilt — **cấm** tạo lại: `@HiltViewModel` + `hiltViewModel()` thay cả hai.

Hai file chỉ xuất hiện khi module thật sự cần, và vẫn nằm trong cây của module đó:

```
data/di/<Module>DataModule.kt            @Binds interface→impl (chỉ khi có interface repository)
presentation/<module>/<Module>Navigation.kt   graph route con type-safe (`composable<…>`); UthApp.kt chỉ nhận thêm một dòng
```

Thêm module mới **không** phải sửa file DI của người khác và không cần xin thêm dependency: KSP + Hilt + serialization đã bật ở `app/build.gradle.kts`. Quyền sở hữu cây file theo `docs/REPO_TREE.md` mục 3–5.

### 3.2.1. Route là type-safe, không phải chuỗi

Destination cấp 1 khai ở `presentation/navigation/UthDestination.kt` dưới dạng `@Serializable object` (không có tham số) hoặc `@Serializable data class` (có tham số). NavHost đăng ký bằng `composable<T> { }`, chuyển hướng bằng `navController.navigate(T)`, `popUpTo(T)`; tab đang chọn suy ra bằng `NavDestination.hasRoute(T::class)` — **không** so chuỗi route, **không** `"home"`/`"schedule"` tự do. Route con của một module đặt trong `<Module>Navigation.kt` của chính module, vẫn dùng `composable<T>`. Cần `@Serializable` → plugin `org.jetbrains.kotlin.plugin.serialization` (đã bật) + runtime `kotlinx-serialization-json`.

### 3.3. Model và lỗi

- `UiState`: `data class`, **mọi tham số có giá trị mặc định** là trạng thái an toàn nhất lúc màn hình vừa mở.
- `isLoading` và `isEmpty` là hai trạng thái khác nhau. Mặc định phải là loading — để UI không nháy "chưa có dữ liệu" trước khi dữ liệu về.
- Lỗi trong `UiState` là **enum** (`UtilitiesFailure.LoadFailed`), không phải `String` hay throwable. Map enum → chuỗi đặt ở Screen. Lý do: `ViewModel` cầm `@StringRes` là không đổi được ngôn ngữ và không test được.
- Repository trả `Result<T>` và là `suspend`. UseCase trả lại đúng `Result` đó hoặc biến đổi nghiệp vụ, không bắt exception nuốt im lặng.
- Không dùng `String` trần cho giá trị có tập hữu hạn: dùng `enum` (`DeliveryPhase`, `UtilityModuleId`) hoặc `value class`.
- Không leak type của backend: `DocumentSnapshot`, `@DocumentId`, Entity Room **dừng lại ở `data/`**.

---

## 4. Kotlin

- Ưu tiên `val`; `var` chỉ khi thật sự cần tích luỹ.
- Bất biến trước: danh sách trả về là `List`, không `MutableList`. Sao chép bằng `copy(...)` với data class.
- Không `!!`. Rủi ro null thì `?.`/`?:` với lý do tường minh, hoặc `requireNotNull(x) { "lý do" }` ở ranh giới.
- `when` phải phủ mọi `sealed`/`enum` branch (không `else` khi compiler đã biết hết). Muốn "chưa xử lý" → viết nhánh trống kèm comment giải thích *vì sao* để trống, như `OpenModule` trong `UtilitiesViewModel`.
- Không `runBlocking` ở bất cứ đâu ngoài test/benchmark.
- Không `object` làm service locator toàn cục; dependency đi qua `@Inject constructor` và do Hilt dựng. Không `LocalContext.current` để lách dependency.
- Extension function: đặt cạnh type bị mở rộng (`Typography.tabularFigures`, `DeliveryPhase.badgeSpec()`), private nếu chỉ dùng trong file.
- Hàm mềm ≤ 40 dòng, độ sâu lồng ≤ 3; chạm ngưỡng thì tách composable/helper private — nhưng **không** tách thành file "Utils" chung.
- File mềm ≤ 250 dòng; Screen nhiều trạng thái được phép dài hơn vì mỗi trạng thái là một composable riêng.
- Chuỗi nội bộ trong code (log, exception message) dùng tiếng Việt có dấu; **chuỗi hiển thị** bắt buộc qua `strings.xml`.

---

## 5. Jetpack Compose

- Composable **stateless khi có thể**: nhận data + callback, không tự giữ trạng thái nghiệp vụ. Trạng thái nâng lên ViewModel (`hoisting`).
- `remember` phải có khoá đúng: `remember(moduleId) { … }`, không `remember { … }` khi nội dung phụ thuộc input.
- **Không có tác dụng phụ trong composition.** Đọc ghi dữ liệu, delay, log → `LaunchedEffect`/`SideEffect` với khoá tường minh.
- Không gọi `hiltViewModel()` trong composable lá; `…Route.kt` là chỗ duy nhất (khai ở giá trị mặc định của tham số `viewModel` để preview/test vẫn truyền fake được). Cấm `@Inject` field trong composable. Ngoại lệ có chủ đích: getter theme (`UthTheme.extendedColors`, `MaterialTheme.*`).
- `Flow` trạng thái trong Composable luôn thu thập bằng `collectAsStateWithLifecycle()`, không `collectAsState()`. **Ngoại lệ:** event một lần (`Flow` từ `Channel.receiveAsFlow()`) thu bằng `LaunchedEffect(viewModel) { viewModel.events.collect { … } }`, không dùng `collectAsStateWithLifecycle()` — mỗi event phải tới đúng một người nhận, không tái phát khi recomposition.
- Danh sách dài: `LazyColumn`/`LazyRow` bắt buộc `key = { … }`; nội dung tính lại đắt dùng `derivedStateOf`.
- Type trạng thái cho Compose đánh dấu `@Immutable`/`@Stable` (xem `UthExtendedColors`) để giữ skip recomposition.
- `modifier` đặt ngay sau tham số bắt buộc, trước tham số tuỳ chọn; `content: @Composable` luôn cuối cùng. Mọi composable public đều nhận `modifier`.
  - **Ngoại lệ có chủ đích** (không vẽ khung riêng nên không nhận `modifier`): `UTHConnectTheme`, `UthPreview`, `UthApp`. Thêm ngoại lệ khác phải ghi ở PR.
- Mỗi màn hình phải có `@Preview` cho **4 trạng thái × light/dark** (mẫu: 8 preview cuối `UtilitiesScreen.kt`), và dùng `UthPreview` thay vì `UTHConnectTheme` trực tiếp.
- Không `Icon`/`AsyncImage` thiếu `contentDescription` có nghĩa; ảnh trang trí truyền `null` một cách có chủ đích, ảnh tải thì có nhãn bằng `stringResource`.

---

## 6. Coroutines & Flow

- Nạp dữ liệu trong `init`, nhưng luôn có đường nạp lại bằng action (`Retry`), không nạp lại thủ công trong Screen.
- Chỉ một nguồn sự thật mỗi màn hình: `StateFlow<UiState>`. Không thêm `StateFlow` phụ song song.
- Mọi cập nhật qua `_uiState.update { it.copy(...) }` — tránh race khi nhiều coroutine cùng ghi.
- Scope: `viewModelScope` cho việc dẫn đường UI; `Dispatchers.IO` cho repository khi có I/O thật. Khi thêm Room/Firestore, UseCase nhận `Dispatcher` qua constructor để test không cần `Thread.sleep`.
- ViewModel luôn là `@HiltViewModel class X @Inject constructor(...)`. Cấm `new` ViewModel trong code app; nhưng **test thì new trực tiếp bằng fake** — Hilt dựng ViewModel qua `ViewModelProvider`, còn constructor vẫn là Kotlin thường, nên unit test không cần `HiltAndroidRule`.
- Việc chạy nền ngoài ViewModel phải có lý do sống-sót ghi bằng comment (`WorkManager`/`Service`).
- Event một lần (Snackbar, dẫn đường từ ViewModel): **đã chốt — `Channel` + `receiveAsFlow()`.** ViewModel giữ kênh private, phát `send(...)`, Screen/Route thu một lần qua `LaunchedEffect`. Kiểu event khai trong chính file `<Module>ViewModel.kt` (không thêm file thứ sáu). Mẫu:

  ```kotlin
  // <Module>ViewModel.kt
  sealed interface <Module>Event { data class ShowMessage(val res: Int) : <Module>Event }
  private val _events = Channel<<Module>Event>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()
  // phát trong coroutine của viewModelScope: _events.send(<Module>Event.ShowMessage(R.string.x))

  // <Module>Route.kt
  LaunchedEffect(viewModel) { viewModel.events.collect { /* snackbar / navigate */ } }
  ```
  Không dùng cờ đã-tiêu-dùng trong `UiState` cho việc này (dễ tái phát khi xoay màn hình).

---

## 7. Thiết kế giao diện trong code

Tóm tắt; chi tiết và số đo tương phản ở `docs/Design-system.md`.

| Cấm | Thay bằng |
|---|---|
| `Color(0xFF…)` ngoài `theme/Color.kt` | `MaterialTheme.colorScheme.<role>` hoặc `UthTheme.extendedColors.<token>` |
| `<số>.dp` ngoài `theme/Dimens.kt` | `UthDimens.Spacing·Radius·Size·Elevation` — thiếu thì **thêm token**, không viết số |
| `<số>.sp` / `FontWeight` rời ngoài `theme/Type.kt` | `MaterialTheme.typography.<role>` |
| Chuỗi hardcode trong Composable | `stringResource(R.string…)` + thêm cả `values/` và `values-en/` trong cùng commit |
| Tự tạo `Shape`, `BorderStroke` số mới | `MaterialTheme.shapes.*` + `UthDimens.Size.Stroke` |
| `dynamicColor = true` | giữ `false` mặc định (`minSdk 31` → Material You đè màu nhận diện) |

Ba file **được phép** chứa literal: `Color.kt`, `Dimens.kt`, `Type.kt`. Kiểm tra nhanh trước khi push (không có kết quả nghĩa là đạt; lưu ý lệnh quét cả **comment**, nên đừng chép số hardcode vào comment):

> Trạng thái đã kiểm chứng ở v1.0: lệnh dưới đây trả rỗng trên toàn bộ `app/src/main`.

```powershell
cd "d:\UTH Connect\app\src\main\java\com\giathinh\uthconnect"
Get-ChildItem -Recurse -Filter *.kt | Where-Object Name -notin 'Dimens.kt','Color.kt','Type.kt' |
  Select-String -Pattern '[0-9]+\.dp|Color\(0x|[0-9.]+\.sp'
```

Icon: `material-icons-core 1.7.8` đã đóng băng, chỉ dùng cho khung nav/badge. Icon thiết kế riêng vào `res/drawable` và gọi qua `painterResource`, không thêm dependency icon mới.

Ghi chú khi thêm token mới: token phải có **tên theo vai trò** (`IconBadge`, không `Size14`), có comment nói dùng ở đâu, và cập nhật bảng tương ứng trong `docs/Design-system.md` mục 3.

---

## 8. Tài nguyên & ngôn ngữ

- Tiếng Việt là locale mặc định (`res/values/strings.xml`); tiếng Anh ở `res/values-en/`. **Thiếu một trong hai là hỏng build review.**
- Một key = một nghĩa. Không dùng `common_generic_2`.
- Số đếm dùng `<plurals>`; ngày giờ dùng `java.time` + `DateTimeFormatter` theo locale trường (dd/MM/yyyy HH:mm); tiền `1.500.000đ`.
- MSSV, giờ, số liệu trong bảng: `MaterialTheme.typography.tabularFigures` để thẳng cột.
- Không ghép chuỗi bằng `+` để dựng câu có nghĩa: dùng placeholder `%1$d`, `%2$s`.

---

## 9. Test

| Loại | Ở đâu | Bắt buộc khi |
|---|---|---|
| Unit test ViewModel/UseCase | `app/src/test/java/…` (mirror package) | mọi module |
| Compose UI test | `app/src/androidTest/java/…` | luồng chính của module (không test mọi pixel) |
| Rules Firestore | `firestore-tests/` (tên đề xuất — chưa tạo, M0) | mọi collection thêm mới |

Chuẩn viết test:

- Một test = một hành vi. Bố cục AAA, tên method backtick nói rõ kỳ vọng (`` `repository thất bại - UiState báo LoadFailed` ``).
- Test ViewModel cần `MainDispatcherRule` (`app/src/test/java/…/testing/`) vì `viewModelScope` pin `Dispatchers.Main`.
- **Fake trước mock:** repository giả là `class FakeRepository : UtilityModuleRepository` như trong `UtilitiesViewModelTest`. Chỉ dùng MockK khi đối tượng quá đắt để giả.
- Test phải chạy được không cần mạng, không cần thiết bị, không `sleep()`.
- Ít nhất cho mỗi module: success, failure, retry/refresh, và một test chốt rằng loading ≠ empty ở trạng thái ban đầu.
- Không `Thread.sleep`, không đo thời gian thật, không phụ thuộc thứ tự test.

---

## 10. Comment, tài liệu nội bộ, TODO

- Comment giải thích **vì sao**, không giải thích **cái gì**. Code tự mô tả được thì xoá comment.
- KDoc bắt buộc cho: mọi public API trong `core/`, mọi file tầng trong `presentation/<module>/`, quy tắc tương phản/elevation trong token.
- Tỷ lệ: code mới phải có comment đặc tả ở các điểm dễ hiểu sai (bo màu Gold, saveState của tab, `popUpTo`) — tài liệu `docs/Design.md`/`docs/Design-system.md` là chỗ dẫn nguồn, ghi `Xem docs/Design-system.md mục 1.4`.
- `TODO(<tên>): <việc> — <issue/PR>` — không có tên người thì không được merge.
- Không commit code bị comment-out, không giữ `println`/`Log.d` tạm (dùng `Log` có tag khi thật sự cần chẩn đoán, và ghi rõ lý do).
- Mỗi folder module có `README.md` ngắn: luồng chính, ai phụ trách, màn hình nào chưa làm.

---

## 11. Lệnh kiểm tra trước khi push

```powershell
cd "d:\UTH Connect"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-Pkotlin.compiler.execution.strategy=in-process'
```

| Điều | Ghi chú |
|---|---|
| PowerShell dùng `;`, **không** dùng `&&` | máy nhóm chạy Windows |
| Property `-P…` phải **bọc dấu nháy đơn** | không nháy, PowerShell tách thành task name và build fail |
| `in-process` là workaround máy RAM thấp | Kotlin daemon chết với "The paging file is too small (errno 1455)"; bỏ flag khi máy đủ nhớ |
| errno 1455 cũng **giết luôn Gradle Test Executor** | triệu chứng: `:app:testDebugUnitTest FAILED` + `java.io.IOException: The pipe is being closed`, không phải lỗi test. Chạy `.\gradlew.bat --stop` rồi build lại; Hilt + KSP ăn RAM nhiều hơn trước nên đừng chạy song song với Android Studio index |
| Cần SDK platform **Android 37.1** | `compileSdk 37.1`; thiếu là fail `checkDebugAarMetadata` |
| `targetSdk` vẫn 36, `minSdk` 31 | đừng đổi `targetSdk` để "sửa" lỗi build |

## 12. Checklist tự review (điền vào PR)

- [ ] Không có `.dp`/hex/`.sp` ngoài 3 file token (lệnh grep mục 7 trả rỗng)
- [ ] Screen stateless, không repository; Route là chỗ nối duy nhất bằng `hiltViewModel()`
- [ ] Module đủ 5 file; có interface repository thì thêm `data/di/<Module>DataModule.kt` **trong cây của mình**, không sửa DI module khác
- [ ] Không tạo lại `AppContainer`/`CompositionLocal` mang dependency/`ViewModelFactory`
- [ ] Route dẫn đường bằng `composable<T>`/`navigate(T)` type-safe (`@Serializable`), không chuỗi route trần; tab chọn suy ra bằng `hasRoute`, không bằng biến state riêng
- [ ] Event một lần (nếu có) qua `Channel` + `receiveAsFlow()` thu bằng `LaunchedEffect`, không dùng cờ trong `UiState`
- [ ] `UiState` bất biến, mặc định loading, lỗi bằng enum, không `@StringRes` trong ViewModel
- [ ] Dependency đúng chiều, không module nào import module khác
- [ ] Chuỗi đủ cả `values/` và `values-en/`
- [ ] `@Preview` 4 trạng thái × light/dark
- [ ] Test: success / failure / retry, chạy không cần mạng
- [ ] `:app:assembleDebug` + `:app:testDebugUnitTest` xanh trên máy mình
- [ ] Không đổi file nóng (`UthApp.kt`, `libs.versions.toml`, `strings.xml`, theme, `MainActivity.kt`, `UTHConnectApplication.kt`) quá mức cần thiết — mỗi file một dòng cộng
- [ ] Đổi cấu trúc thư mục (thêm/xoá package, tầng, file chung) → đã cập nhật `docs/REPO_TREE.md` mục 2/3/8 trong cùng commit

---

## 13. Nợ kỹ thuật và câu hỏi còn mở

| # | Hiện trạng | Cần quyết |
|---|---|---|
| 1 | Chưa bật ktlint/spotless/CI; chuẩn ở đây đang review bằng mắt | bật `spotless` + `ktlint` ở M0, rồi mục 1–5 thành lỗi build |
| 2 | DI đã chuyển sang **Hilt** (2.60.1 + KSP 2.3.12): `@HiltAndroidApp` · `@AndroidEntryPoint` · `@Binds` trong `data/di/` · `@HiltViewModel` · `hiltViewModel()`. `AppContainer` và các `ViewModelFactory` đã xoá hẳn | chưa thêm `hilt-android-testing` + `kspAndroidTest` — chỉ cần khi viết Compose UI test có `HiltAndroidRule` (M1) |
| 3 | Event một lần **đã chốt**: `Channel` + `receiveAsFlow()` thu bằng `LaunchedEffect` (mẫu ở mục 6) | screen thật sự có snackbar/dẫn đường mới thêm kênh; slice `utilities` chưa có nên chưa thêm — đừng thêm code chết |
| 4 | Navigation **đã chuyển type-safe**: 4 destination là `@Serializable object`, plugin serialization + `kotlinx-serialization-json` đã bật | khi module có route con kèm tham số, khai `@Serializable data class` trong `<Module>Navigation.kt` của module đó |
| 5 | `kotlinx-coroutines` dùng bản resolve qua BOM (1.9.0), chưa khai trong catalog | khai tường minh để tránh bị BOM đổi bản âm thầm |
| 6 | Icon từ `material-icons-core` đã đóng băng | thay toàn bộ bằng vector drawable hay giữ cho khung app (`docs/Design-system.md` mục 10) |
| 7 | `docs/CODING_STANDARDS.md` chưa có `docs/adr/` | ADR đầu tiên nên là quyết định #3 hoặc #4 |
| 8 | `hiltViewModel()` đã dời package sang `androidx.hilt.lifecycle.viewmodel.compose`; bản trong `hilt-navigation-compose` bị deprecated | nếu Android Studio gợi ý import cũ (navigation-compose), sửa lại import — code này chỉ dùng artifact mới |
