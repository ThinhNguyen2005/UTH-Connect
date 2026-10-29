# UTH Connect — Quy trình Git cho nhóm 6 người

> Áp dụng từ milestone M0. Mâu thuẫn về **phạm vi** → xem `docs/Design.md`; về **giao diện** → `docs/Design-system.md`; về **quy trình Git** → file này.
> Shell của nhóm là Windows PowerShell → **dùng `;` để nối lệnh, không dùng `&&`**.

---

## 1. Hiện trạng repo

| Nhánh | Commit | Trạng thái |
|---|---|---|
| `main` | `4e54f4b first commit` | có remote `origin` (GitHub) |
| `develop` | `4e54f4b` (vừa tạo, **chưa push**) | nhánh tích hợp chung |

Việc còn thiếu một lệnh — người giữ repo chạy (xem mục 2.3):

```powershell
cd "d:\UTH Connect"; git push -u origin develop
```

**Không commit hộ nhau.** Mỗi người tự commit trên `feature/` của mình; khi làm xong, báo cho người giữ repo merge PR.

---

## 2. Mô hình nhánh

```
main            ← chỉ nhận từ develop khi bàn giao/demo; luôn build được; tag release
  └── develop   ← nhánh tích hợp, nhận mọi feature đã review
        └── feature/<tên-module>-<tên-bạn>   ← mỗi người một nhánh, ngắn (1–3 ngày)
        └── hotfix/<vấn-đề>                  ← chỉ dùng khi main đã hỏng, xuất phát từ main
```

Quy tắc cứng:

1. **Không push trực tiếp vào `main` hoặc `develop`** (chặn bằng branch protection, mục 4).
2. Feature nào cũng phải **tách từ `develop`**, không tách từ `main`.
3. Một PR = **một module/một tính năng nhỏ**. PR trên ~400 dòng thay đổi → tách đôi.
4. Merge vào `develop` bằng **Merge commit** (giữ lịch sử feature); `main` nhận từ `develop` bằng PR khi demo, có tag `v0.x`.
5. Không `push --force` lên `main`/`develop`. Trên nhánh của chính mình, nếu cần rewrite thì dùng `--force-with-lease`.

### 2.1. Format tên nhánh (bắt buộc)

```
feature/<tên-module>-<tên-bạn>
```

- `<tên-module>`: viết thường, có dấu gạch nối, nằm trong bảng phân công mục 3.
- `<tên-bạn>`: tên định danh ngắn, **không dấu**, không hoa (vd. `thinh`, `khanh`, `nhu`).
- Ví dụ đúng: `feature/events-thinh`, `feature/checkin-khanh`, `feature/timetable-nhu`.
- Ví dụ sai: `feature/Task1`, `login-fix`, `thinh-dev`, `feature/mua-ban-sach-nhu-2026`.

Nhánh phụ trợ: `chore/<việc>` (CI, gradle, docs), `fix/<lỗi>` (lỗi nhỏ chưa ra bản).

### 2.2. Vòng làm việc của một bạn

```powershell
cd "d:\UTH Connect"
git switch develop; git pull --rebase origin develop
git switch -c feature/timetable-khanh          # tạo nhánh từ develop
# ... viết code, mỗi bước nhỏ thì commit ...
git add <file>; git commit -m "feat(timetable): thêm màn danh sách môn học"
git push -u origin feature/timetable-khanh      # rồi mở Pull Request trên GitHub
```

Trước khi mở PR, kéo thay đổi mới nhất của `develop` để khỏi xung đột:

```powershell
git fetch origin; git rebase origin/develop
```

### 2.3. Người giữ repo (1 người duy nhất)

```powershell
git push -u origin develop                      # tạo nhánh phát triển trên GitHub
git switch develop; git pull                    # trước mỗi lần merge PR
git switch main; git merge --no-ff develop; git push origin main   # khi demo/bàn giao
git tag v0.1.0; git push origin v0.1.0
```

---

## 3. Bảng phân công (điền tên 5 bạn vào)

| Module / phần việc | Giai đoạn (`docs/Design.md` mục 3) | Tên nhánh | Người | File được đụng chính |
|---|---|---|---|---|
| Sự kiện: danh sách + chi tiết + đăng ký | MVP | `feature/events-<tên>` | … | `presentation/home/`, `presentation/events/` |
| Vé QR + máy quét điểm danh | MVP | `feature/checkin-<tên>` | … | `presentation/tickets/`, `presentation/scanner/` |
| Xác thực email trường + Cá nhân/Cài đặt | MVP | `feature/auth-<tên>` | … | `presentation/profile/`, `data/remote/` |
| Thời khóa biểu + deadline | Phase 2 | `feature/timetable-<tên>` | … | `presentation/schedule/`, `data/local/` (Room) |
| Đồ thất lạc (kèm listing engine cho sách) | Phase 3 / Backlog | `feature/lostfound-<tên>` | … | `presentation/utilities/`, `domain/model/` |
| Nền chung: theme, navigation, CI, Firestore rules | M0–M5 | `chore/core-<tên>` | Chủ nhiệm nhóm | `core/`, `presentation/navigation/`, `gradle/` |

**Quy ước chống giẫm chân nhau:** mỗi người chỉ sửa folder `presentation/<module-của-mình>/` và `domain`/`data` của module mình. Các file *nóng* dưới đây bắt buộc đổi tối thiểu và phải báo trước trong nhóm:

| File nóng | Vì sao dễ xung đột | Cách giảm |
|---|---|---|
| `presentation/navigation/UthApp.kt` + `UthDestination.kt` | mọi module đều muốn thêm route | khai graph con trong `presentation/<module>/<Module>Navigation.kt`, `UthApp.kt` chỉ gọi một dòng `moduleNavigation(navController)` |
| `gradle/libs.versions.toml` | ai cũng thêm dependency | gom vào một PR `chore/`, ghi rõ lý do từng lib |
| `res/values/strings.xml` + `values-en/strings.xml` | hai file, thêm chuỗi liên tục | mỗi module có nhóm chuỗi tiền tố riêng (`timetable_`, `lost_found_`); luôn thêm cả hai ngôn ngữ trong cùng commit |
| `core/designsystem/theme/*` | design system dùng chung | chỉ người giữ `chore/core-*` sửa; ai cần token mới thì mở issue/PR riêng, kèm **số đo tương phản** |
| `app/build.gradle.kts` | compileSdk/minSdk | như trên, qua PR `chore/` |
| `UTHConnectApplication.kt` · `MainActivity.kt` | gốc DI của toàn app | **không** thêm `@Provides`/`@Binds` nghiệp vụ vào đây; binding của module để trong `data/di/<Module>DataModule.kt` của chính module |
| `core/designsystem/component/*` · `theme/*` | ai cũng muốn dùng component đẹp | dùng lại token có sẵn; cần component mới thì mở PR `chore/` riêng, không sửa file cũ trong PR feature |

**Không còn điểm đăng ký DI chung.** Trước đây `core/di/AppContainer.kt` là file nóng nhất (mọi module phải thêm một `val`); file đó đã bị xoá khi chuyển sang Hilt, và `ViewModelFactory` của từng module cũng vậy. Nay mỗi module tự khai binding trong `data/di/<Module>DataModule.kt` — không đụng file của ai khác. Quy ước phạm vi file theo từng người: `docs/REPO_TREE.md` mục 3 và 5.

### 3.1. Một module mới gồm những file nào (sườn MVVM đã chốt)

Copy nguyên cấu trúc của `presentation/utilities/` — màn hình đang đạt checklist và có test:

```
presentation/<module>/
  <Module>Screen.kt            # stateless: nhận UiState, phát Action. KHÔNG gọi repository
  <Module>Route.kt             # nối ViewModel với Screen; NavHost chỉ biết file này
  <Module>ViewModel.kt         # @HiltViewModel + @Inject constructor; StateFlow<UiState>; gọi UseCase bằng viewModelScope
  <Module>UiState.kt           # data class bất biến; loading ≠ empty; lỗi bằng enum
  <Module>Action.kt            # sealed interface ý định người dùng
  <Module>Navigation.kt        # chỉ thêm khi module có route con (graph riêng của bạn)
domain/model/ · domain/repository/ · domain/usecase/   # interface + use case của bạn (`@Inject constructor`)
data/repository/                                      # implementation + datasource (`@Singleton`)
data/di/<Module>DataModule.kt                         # @Binds interface→impl của riêng module bạn
test/…/presentation/<module>/<Module>ViewModelTest.kt # ít nhất: success / failure / retry
```

Đúng **5 file** cho một module (`docs/CODING_STANDARDS.md` mục 3.2) — thêm file factory thứ sáu là sai chuẩn. Route là **type-safe** (`@Serializable`, xem `docs/CODING_STANDARDS.md` mục 3.2.1). Việc phải làm ở file chung chỉ còn **một dòng** `composable<XDestination> { <Module>Route() }` trong `UthNavHost` (ViewModel do `hiltViewModel()` tự dựng, không cần đăng ký ở đâu khác). Nếu module cần route con, khai hết trong `<Module>Navigation.kt` của bạn, đừng viết dài trong `UthApp.kt`.

---

## 4. Phân quyền trên GitHub

### 4.1. Vai trò

| Người | Vai trò repo | Quyền có được |
|---|---|---|
| Chủ nhiệm nhóm (bạn) | **Admin** | Settings, branch protection, merge `main`/`develop`, tag |
| 5 bạn còn lại | **Write** | push nhánh `feature/*`, mở PR, comment review; **không** đổi Settings |

Mời cộng tác viên: *Settings → Collaborators and teams → Add people* (hoặc `gh api -X PUT /repos/{owner}/{repo}/collaborators/{username} -f permission=push`).

### 4.2. Branch protection (làm một lần, cho cả `main` và `develop`)

*Settings → Branches → Add branch protection rule*, đặt pattern `main` rồi lặp lại với `develop`:

- [x] **Require a pull request before merging** → Required approving reviews = **1**
- [x] **Require branches to be up to date before merging**
- [x] **Require status checks to pass** → chọn check `build` từ GitHub Actions (khi CI đã có ở M0; chưa có thì chưa tick)
- [x] **Do not allow bypassing the above settings** → thêm **Restrict pushes that create merges**
- [x] **Block force pushes**
- [ ] Include administrators (tick khi nhóm đã quen quy trình; giai đoạn đầu để trống cho linh hoạt)

Bản `gh api` tương đương cho `develop` — **chạy sau khi CI đã có status check**; nếu chưa có, xoá dòng `required_status_checks`:

```powershell
gh api -X PUT "repos/{owner}/{repo}/branches/develop/protection" `
  -H "Accept: application/vnd.github+json" `
  -f "required_status_checks[strict]=true" `
  -f "required_status_checks[contexts][]=build" `
  -f "required_pull_request_reviews[required_approving_review_count]=1" `
  -F "enforce_admins=false" -F "allow_force_pushes=false" -F "required_pull_request_reviews[dismiss_stale_reviews]=true"
```

### 4.3. CODEOWNERS (bắt buộc review đúng người)

`.github/CODEOWNERS` — file này nằm trong kế hoạch M0, mẫu:

```github
*                       @<username-chu-nhiem>
/core/                  @<username-chu-nhiem>
/presentation/navigation/ @<username-chu-nhiem>
/presentation/events/   @<username-events>
/presentation/tickets/  @<username-checkin>
/presentation/schedule/ @<username-timetable>
/presentation/utilities/ @<username-lostfound>
/app/src/main/res/      @<username-design> @<username-chu-nhiem>
```

---

## 5. Commit & Pull Request

Commit theo **Conventional Commits** (chi tiết ở `README.md` mục 13):

```
feat(events): thêm danh sách sự kiện lọc theo CLB
fix(scanner): chặn quét trùng khi mất mạng giữa chừng
chore(ci): bật kapt/spotless và cache gradle
docs(design-system): bổ sung bảng đo tương phản Navy/Gold
```

Scope = tên module, khớp với nhánh. Một PR phải có:

- Tiêu đề `feat(<module>): <một câu mô tả kết quả>`, không viết hoa toàn bộ.
- Phần mô tả: **Làm gì · Vì sao · Cách thử (`.\gradlew.bat :app:assembleDebug`) · Ảnh/ghi chú màn hình · Checklist DoD** (`README.md` mục 10.2, `docs/Design-system.md` mục 8).
- Không có `TODO` mơ hồ; muốn để lại việc thì mở issue và link trong PR.
- Reviewer kiểm tra: không hardcode hex/`.dp`/`.sp`, có `@Preview` light + dark, chuỗi đủ `values` + `values-en`, domain không import `androidx`.

Trạng thái PR: `Draft` khi đang làm → `Open` khi xin review → **chỉ người giữ repo bấm merge**, không phải tác giả PR.

---

## 6. Xử lý tình huống hay gặp

| Tình huống | Lệnh/cách an toàn |
|---|---|
| Nhánh tụt sau `develop` | `git fetch origin; git rebase origin/develop` (khi nhánh **chưa** có ai khác dùng) |
| Xung đột khi rebase | giải quyết từng file, `git add <file>; git rebase --continue`; đừng `git checkout --theirs` bừa với `strings.xml`/`libs.versions.toml` |
| Muốn lưu việc đang dở | `git switch -c wip/timetable-khanh; git add -A; git commit -m "wip: lưu trạng thái"; git switch develop` |
| Commit nhầm vào `develop` (bị chặn push) | `git switch -c feature/<module>-<tên>` — commit đã nằm trên nhánh mới, reset `develop` về `origin/develop` |
| Build fails sau pull | `cd "d:\UTH Connect"; .\gradlew.bat --stop` rồi build lại; máy RAM thấp dùng `.\gradlew.bat :app:assembleDebug "-Pkotlin.compiler.execution.strategy=in-process"` |
| Cần hủy commit vừa push trên nhánh mình | `git revert <sha>` (an toàn hơn `reset --hard` + force) |
