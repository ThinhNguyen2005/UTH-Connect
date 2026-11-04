# Báo cáo phiên làm việc: Tối ưu hóa Supabase Transport, Chuẩn hóa ZXing Catalog & Security Hardening
**Ngày thực hiện**: 2026-11-04  
**Nhánh Git**: `feature/fix_security`  
**Trạng thái Git**: Đã xác minh toàn bộ Git Diff, Test Suite Pass 100%

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi Git

Phiên làm việc này tổng hợp toàn bộ các thay đổi chưa commit trong working tree của nhánh `feature/fix_security` (bao gồm các chỉnh sửa thủ công của lập trình viên và các cải tiến kỹ thuật/bảo mật vừa triển khai):

1. **Chuẩn hóa Version Catalog cho thư viện ZXing QR (`libs.versions.toml` & `app/build.gradle.kts`)**:
   - Trước đó nhánh `feature/fix_security` đã commit tính năng camera quét mã QR vé (`1bfff37 feat(events): add QR check-in camera capture and ticket scan UI`).
   - Lập trình viên đã chuẩn hóa dependency ZXing (`com.google.zxing:core:3.5.4`) vào Gradle Version Catalog thay vì để chuỗi hardcode thô trong file build.
2. **Quyết định kiến trúc & Tối ưu hóa Supabase Data Layer**:
   - Khảo sát mã nguồn mẫu từ Supabase Dashboard (Todo Quickstart) và thống nhất **giữ nguyên kiến trúc hiện tại** (Lựa chọn 1: `SupabaseRestClient` + PostgREST transport) thay vì nạp thêm thư viện bên thứ ba `supabase-kt` gây phình bộ dependency và rủi ro biến động giữa lúc đang hardening bảo mật.
   - Giải quyết triệt để thảm họa hiệu năng N+1 query trong lấy danh sách vé sinh viên.
   - Đóng toàn bộ lớp lỗ hổng query injection và ký tự đặc biệt bằng cơ chế canonical UUID validation và URL-encoding.
3. **Audit bảo mật & Bộ kiểm thử**:
   - Dùng Context7 tra cứu live docs Supabase, đối chiếu các RPCs `SECURITY DEFINER SET search_path = ''` và catalog assertions trong `security_contract.sql`.
   - Bổ sung unit tests cho tầng mapping; chạy toàn bộ test suite pass 100%.

---

## 2. Kết quả đạt được & Chi tiết Git Diff (Commit Ready)

Dưới đây là chi tiết kỹ thuật của từng file đang có diff trong `git status`:

### Nhóm 1: Chuẩn hóa Dependency & Gradle Build
* **[`gradle/libs.versions.toml`](file:///d:/APP/UTH-Connect/gradle/libs.versions.toml)**:
  * Khai báo version `core = "3.5.4"` trong `[versions]`.
  * Khai báo library `core = { module = "com.google.zxing:core", version.ref = "core" }` trong `[libraries]`.
* **[`app/build.gradle.kts`](file:///d:/APP/UTH-Connect/app/build.gradle.kts)**:
  * Thay thế dòng hardcode `implementation("com.google.zxing:core:3.5.4")` bằng `implementation(libs.core)`.
  * Giúp quản lý phiên bản tập trung, tránh phân mảnh version trong dự án.

### Nhóm 2: Tối ưu hiệu năng & Bảo mật tầng Supabase Data Layer
* **[`app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseTicketRepository.kt`](file:///d:/APP/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseTicketRepository.kt)**:
  * **Xóa bỏ N+1 query**: Thay vì lặp qua từng vé để query `events?id=eq.$id` ($1 + 1 + N$ requests), query nay sử dụng PostgREST Resource Embedding:
    ```
    registrations?select=event_id,status,events(*)&user_id=eq...
    ```
    Giảm xuống **1 request duy nhất** cho toàn bộ vé và sự kiện kèm theo.
  * **Ủy quyền mapping**: Đưa logic parse JSON sang `row.toDomainTicket(...)`.
  * **Sanitize URL**: Bọc `urlEncode(uuid(uid))` cho tham số truy vấn `profiles`, `registrations`, `attendance`.
  * **Clean build warning**: Thay `Unit` ở cuối hàm `checkIn()` bằng `return@runCatching` để xóa sạch cảnh báo `Expression is unused`.
* **[`app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseMappers.kt`](file:///d:/APP/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseMappers.kt)**:
  * Thêm helper `uuid(id: String)`: Parse bằng `UUID.fromString(id).toString()` chuẩn hóa dạng 8-4-4-4-12 hex; ngắt ngay các chuỗi injection hoặc filter độc hại (`&or=...`, `,gt.0`) trước khi gọi mạng.
  * Thêm helper `urlEncode(value: String)`: Mã hóa an toàn ký tự đặc biệt theo chuẩn UTF-8.
  * Thêm hàm pure mapping `JsonObject.toDomainTicket(...)`: Hỗ trợ đọc cả key `"events"` lẫn `"event"`, tái sử dụng hằng số tĩnh `TICKET_DATE_FORMATTER` (Zone `Asia/Ho_Chi_Minh`), bọc `runCatching` khi parse timestamp để chống crash app nếu dữ liệu server bị lỗi.
* **[`app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseEventRepository.kt`](file:///d:/APP/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseEventRepository.kt)**:
  * Áp dụng `urlEncode(uuid(eventId))` và `urlEncode(uuid(uid))` trên các hàm `getEventDetail`, `isRegistered` và `currentUserRegistrations`.
  * Loại bỏ hàm private `uuid()` trùng lặp, dùng chung helper nội bộ từ `SupabaseMappers`.
* **[`app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt`](file:///d:/APP/UTH-Connect/app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt)**:
  * Áp dụng `urlEncode(uuid(client.userId))` khi query `ownProfile()`.

### Nhóm 3: Kiểm thử Unit Test
* **[`app/src/test/java/com/giathinh/uthconnect/data/repository/SupabaseMappersTest.kt`](file:///d:/APP/UTH-Connect/app/src/test/java/com/giathinh/uthconnect/data/repository/SupabaseMappersTest.kt)**:
  * Bổ sung 3 test cases:
    1. `toDomainTicketMapsEmbeddedEventCorrectly`: Kiểm tra parse payload PostgREST lồng `events(*)`.
    2. `toDomainTicketDistinguishesStatuses`: Kiểm tra phân biệt các trạng thái `VALID`, `CANCELLED`, `CHECKED_IN`, và fallback an toàn khi thiếu event lồng.
    3. `uuidAndUrlEncodingBehaveSafely`: Kiểm tra mã hóa URL và chặn input độc hại/malformed UUID.
  * **Kết quả chạy test**: Toàn bộ 32 test tasks pass 100% (`BUILD SUCCESSFUL in 10s`).

---

## 3. Bài học & Kinh nghiệm rút ra

1. **Rà soát toàn diện Git Working Tree trước khi kết thúc phiên**:
   * Khi làm việc song song giữa lập trình viên (human) và các trợ lý AI (Codex, Cursor, Copilot, Antigravity), diff trong repository thường bao gồm cả thay đổi build/catalog và thay đổi mã nguồn logic. Agent phải luôn chạy `git status` / `git diff` toàn diện để không bỏ sót ngữ cảnh của bất kỳ file nào.
2. **Biên giới bảo mật nằm ở Postgres RLS + RPC**:
   * Chuyển SDK không làm tăng tính an toàn của app. Ranh giới bảo mật thực sự được bảo vệ bởi PostgreSQL Row Level Security (RLS) và các hàm RPC có `SECURITY DEFINER SET search_path = ''`.
3. **Kế thừa quyền RLS trong PostgREST Embedding**:
   * Khi embed `events(*)`, PostgREST tự động áp dụng RLS policy của bảng `events`. Nếu sự kiện chưa publish (`DRAFT`), trường `events` trả về `null`. Phía client chỉ cần phòng thủ bằng `get("events") as? JsonObject ?: return null` là vừa loại bỏ được dữ liệu không hợp lệ, vừa không làm rò rỉ sự kiện nháp.
4. **Cô lập Seam trong Clean Architecture**:
   * Cả 4 repository Supabase chỉ phụ thuộc qua lớp giao tiếp `SupabaseRestClient`. Việc giữ transport tự viết tối giản (~135 dòng) giúp app cực kỳ nhẹ, không phụ thuộc bộ Ktor cồng kềnh, và hoàn toàn dễ dàng đảo ngược sang SDK khác trong tương lai mà không sửa đổi UI/Domain.

---

## 4. Vấn đề tồn đọng / Kỹ thuật cần lưu ý

1. **Session Persistence (Lưu phiên đăng nhập)**:
   * Hiện tại `credentials` trong `SupabaseRestClient` chỉ lưu trên RAM (`@Singleton`). Khi tắt hẳn app (cold start), phiên đăng nhập bị mất. Cần lưu an toàn token vào `EncryptedSharedPreferences` / Android Keystore.
2. **Supabase Storage cho ảnh đại diện**:
   * Cột `avatar_url` đã có sẵn trên bảng `profiles`, nhưng client hiện tại chưa có cơ chế upload file multipart lên bucket Supabase Storage nếu không dùng SDK hoặc thư viện ngoài.
3. **Cấp quyền runtime cho MCP Supabase**:
   * Server MCP Supabase cục bộ chưa được kích hoạt quyền thực thi công cụ trong phiên IDE này; hiện tại đang dùng Context7 để tra cứu tài liệu thay thế.

---

## 5. Kế hoạch cho các phiên tiếp theo & Đề xuất Commit

### Đề xuất chia Commit cho Git Diff hiện tại:
* **Commit 1 (Build/Deps)**:
  `build(deps): migrate zxing core dependency to version catalog`
  * Sửa: `gradle/libs.versions.toml`, `app/build.gradle.kts`.
* **Commit 2 (Data/Security)**:
  `data(supabase): eliminate N+1 ticket query with resource embedding and sanitize URL parameters`
  * Sửa: `SupabaseTicketRepository.kt`, `SupabaseMappers.kt`, `SupabaseEventRepository.kt`, `SupabaseAuthRepository.kt`, `SupabaseMappersTest.kt`.
* **Commit 3 (Docs)**:
  `docs(session): add session report for supabase optimization and security hardening`
  * Thêm: `docs/sessions/2026-11-04_supabase_optimization_and_security.md`.

### Hành động phiên tiếp theo:
* [ ] Triển khai cơ chế `SessionStorage` an toàn bằng `EncryptedSharedPreferences` để duy trì `accessToken` và `refreshToken` qua các lần khởi động lại app.
* [ ] Chạy thử nghiệm file kịch bản chấp thuận `staging_acceptance.py` trên môi trường Supabase staging thật khi được cấp cấu hình URL/Key.

---

## 6. Bổ sung cùng phiên: Đăng nhập Google (Credential Manager + supabase-kt auth-kt)

**Trạng thái**: đã build, đã cài và kiểm tra UI trên thiết bị thật `528d39bc`; test suite pass ở cả hai chế độ backend.

### 6.1. Điều chỉnh quyết định kiến trúc ở mục 1.2

Đầu phiên, kết luận là "giữ nguyên Lựa chọn 1, chưa nạp `supabase-kt`". Kết luận đó **vẫn đúng cho data layer** (PostgREST/GoTrue tự viết), nhưng không còn đúng tuyệt đối: tính năng Google Sign-In bắt buộc phải có trình đổi ID token sang phiên Supabase (`grant_type=id_token`), và viết tay phần verify `aud`/`nonce` là tự tái tạo một phần GoTrue client. Vì vậy SDK được nạp **có chủ đích và có ranh giới**:

* `io.github.jan-tennert.supabase:auth-kt` (BOM `3.8.0`) + `ktor-client-okhttp` chỉ phục vụ `SupabaseGoogleExchange` — một cuộc gọi đổi token duy nhất.
* `gotrue-kt` là tên module cũ, **không còn được publish từ 3.0.0**; package mới là `io.github.jan.supabase.auth`.
* Phiên trả về được **bridge** vào `SupabaseRestClient.attachSession(...)` để toàn app dùng chung một Bearer token. Nếu để SDK tự giữ phiên, `client.userId` sẽ vẫn báo "Supabase sign-in required" và mọi màn hình sự kiện chết.
* `sessionManager = MemorySessionManager()`: không lưu token ra SharedPreferences, khớp với quyết định "Supabase session chỉ có hiệu lực trong process" đã ghi ở `PreferencesSessionRepository`.

### 6.2. Các file thêm/sửa

* **Data**: `GoogleNonce.kt` (SHA-256 hex cho Google, nonce thô cho Supabase), `GoogleIdTokenSource.kt` (Credential Manager, phân loại Cancelled/Unavailable), `SupabaseGoogleExchange.kt`, `SupabaseRestClient.kt` (+`attachSession`), `SupabaseAuthRepository.kt` (+`authenticateWithGoogle`, tách `ownProfileRow`).
* **Domain**: `AuthRepository.authenticateWithGoogle` có default "unsupported" nên `FirebaseAuthRepository`/`DemoAuthRepository` không phải sửa; thêm `StudentVerificationRequiredException`, `GoogleSignInUnsupportedException`, `SignInWithGoogleUseCase` (luôn `remember = false`).
* **Presentation**: `AuthAction`/`AuthUiState`/`AuthViewModel` thêm trạng thái Google; `AuthRoute` gọi Credential Manager qua `GoogleSignInEntryPoint` (giữ quy ước "Route gọi OS API, ViewModel không"); `AuthScreen` thêm nút "Tiếp tục với Google" và khối "Đang chờ xác minh MSSV".
* **Build**: `libs.versions.toml` (+supabase BOM/auth-kt, ktor, credentials 1.6.0, googleid 1.2.1), `app/build.gradle.kts` (+`GOOGLE_WEB_CLIENT_ID` từ `-PgoogleWebClientId`).
* **Tài liệu**: `docs/GOOGLE_SIGNIN_SETUP.md`.
* **UI theo yêu cầu**: bỏ dòng "Chế độ demo – không xác thực với hệ thống đào tạo UTH." khỏi `AuthScreen` và khỏi `values/strings.xml` + `values-en/strings.xml`.

### 6.3. Lỗi gặp phải và cách sửa

1. **`Unresolved reference 'auth'`**: trong 3.x, `SupabaseClient.auth` là extension property, phải `import io.github.jan.supabase.auth.auth`.
2. **Test phụ thuộc cấu hình build**: test vừa viết khẳng định `googleSignInAvailable == false`, chỉ đúng ở chế độ mock, nên fail khi chạy `-PuseSupabase=true`. Đã đổi sang bất biến cấu hình-độc lập: "token Google không mở được phiên khi backend không hỗ trợ Google". **Bài học**: đừng assert giá trị suy ra từ `BuildConfig` trong unit test.
3. **Nút Google không hiện trên thiết bị**: nguyên nhân không nằm ở UI mà ở build — APK cài từ Android Studio được build bằng cờ mặc định `useMockData=true`, nên `BuildConfig.USE_SUPABASE=false`. Đã ghi năm property công khai (`useSupabase`, `useMockData`, `supabaseUrl`, `supabasePublishableKey`, `googleWebClientId`) vào `~/.gradle/gradle.properties` (có backup `gradle.properties.bak-uthconnect`), không có secret nào.

### 6.4. Việc còn treo của mục 6

* [ ] Bật provider Google trong Supabase Dashboard (Client IDs = Web client ID, Client secret, **Skip Nonce Check tắt**) và xác nhận SHA-1 debug `9E:CC:99:8D:...` đã khai trên Android OAuth client.
* [ ] Test luồng Google thật trên thiết bị (tạo `auth.users` mới) — chưa thực hiện vì có side effect ghi dữ liệu thật vào project.
* [ ] Quyết định nghiệp vụ cho tài khoản Google chưa xác minh MSSV (chặn domain email, quy trình `admin_verify_student`, chống trùng tài khoản qua `linkIdentity`).
* [ ] Secret Web OAuth đã bị dán vào khung chat — nên *Reset client secret* trong Google Console và cập nhật lại Dashboard.

### 6.5. Chẩn đoán lỗi "Không xác thực được với Google" trên thiết bị

Triệu chứng: bấm "Tiếp tục với Google", chọn được tài khoản, nhưng UI trả `AuthFailure.GoogleRejected`.

Nguyên nhân gốc (đã đo, không phỏng đoán):

* `runCatching` trong `SupabaseAuthRepository.authenticateWithGoogle` + `getOrElse` trong `SignInWithGoogleUseCase` nuốt exception của GoTrue, logcat trống nên không kết luận được gì → đã thêm `Log.w("SupabaseGoogleExchange", "Google ID-token exchange rejected", error)` tại điểm exchange.
* Probe chỉ-đọc `GET /auth/v1/authorize?provider=google` với publishable key trả **302** với `client_id=324596143171-p52o...` → provider Google **đã bật**, nhưng Dashboard đang lưu **Android client ID** làm Client ID của provider.
* ID token app xin được ký với `aud` = Web client ID `324596143171-ue1ff5n...` (đúng chuẩn: `setServerClientId` phải là Web client ID). GoTrue đối chiếu `aud` với client ID đã đăng ký nên từ chối → exchange fail. Đây là chính mục 6.4 dự đoán.

Cách sửa (thao tác Dashboard, cần người dùng thực hiện vì ghi cấu hình project thật): đặt Client ID = Web client ID + Web client secret đã reset, Keep Skip Nonce Check OFF, chờ vài phút rồi chạy lại probe để xác nhận `client_id=` đã đổi. Toàn bộ bước này, kèm bảng lỗi→nguyên nhân, đã ghi vào `docs/GOOGLE_SIGNIN_SETUP.md` mục 7.

