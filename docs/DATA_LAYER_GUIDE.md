# Bàn giao Data Layer cho Phú, Văn, Đức, Thắng

## Chạy ngay với mock

Nhánh: feature/data-backend-nghia. Mở bằng Android Studio, chọn Debug và chạy app.
Mặc định useMockData=true: đăng nhập không cần mạng hoặc app/google-services.json.

- MSSV: 2023040123 hoặc email: demo@ut.edu.vn
- Mật khẩu: UTH@123456
- UID hồ sơ mẫu: demo-uid-001
- Người dùng: Nguyễn Minh Anh
- Khi đổi chế độ, đăng xuất hoặc xóa dữ liệu app để tránh khôi phục phiên cũ.

Build và kiểm thử trên Windows:

~~~powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest -PuseMockData=true
~~~

## Interface dùng chung

Interface: domain/repository; models: domain/model.
Hilt cung cấp implementation; UI không khởi tạo FakeRepository và không hardcode dữ liệu.

| Nhu cầu | Interface / hàm | Kết quả |
|---|---|---|
| Đăng nhập | AuthRepository.authenticate(identifier, password) | suspend Result<StudentProfile?> |
| Hồ sơ | UserRepository.getUserProfile(uid) | Flow<User?> |
| Người dùng mẫu | UserRepository.getCurrentUser() | Flow<User?> |
| Lịch | ScheduleRepository.getScheduleByDate(date) | Flow<List<CourseSchedule>> |
| Bảng tin | NotificationRepository.getNewsfeed(category) | Flow<List<PostNotification>> |
| Chi tiết tin | NotificationRepository.getNotificationDetail(id) | suspend Result<PostNotification?> |
| Điểm | StudentGradeRepository.getGrades(semester) | Flow<List<StudentGrade>> |

authenticate tương đương chức năng login; giữ tên để tương thích màn hình hiện có.
UI đăng nhập dùng SignInUseCase(identifier, password, remember) để validation và lưu phiên.

StudentProfile là model Auth/Session hiện có. User có thêm uid, phoneNumber và role.
DemoAccount tạo cả hai từ cùng một nguồn. getCurrentUser của fake là fixture,
không phải trạng thái đăng nhập; dùng SessionRepository cho phiên.
updateProfile chỉ cập nhật bộ nhớ FakeUserRepository, không đồng bộ vào Auth/Session,
và mất khi tiến trình kết thúc.

## Cách nối UI

Tuân thủ Route → ViewModel → UseCase → Repository. Screen chỉ nhận trạng thái/callback.
Ví dụ thêm use case vào domain/usecase:

~~~kotlin
import com.giathinh.uthconnect.domain.repository.ScheduleRepository
import javax.inject.Inject

class GetScheduleByDateUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    operator fun invoke(date: String) = repository.getScheduleByDate(date)
}
~~~

Inject use case vào @HiltViewModel; gọi trong viewModelScope, collect Flow và cập nhật UiState.
Route dùng collectAsStateWithLifecycle để chuyển trạng thái vào Screen.
Loading/error thuộc UiState; danh sách rỗng hiển thị empty state; null nghĩa là không tìm thấy
(hoặc sai thông tin đăng nhập với authenticate); Result.failure là lỗi thao tác.
Flow lịch/bảng tin/điểm là snapshot, không giả lập lỗi mạng hay realtime.

## Dữ liệu mẫu

- Lịch: yyyy-MM-dd, có dữ liệu từ 2026-11-03 đến 2026-11-08.
  Ngày 2026-11-03 có 2 môn; ngoài mẫu trả rỗng, không tự lặp theo tuần.
- Bảng tin: 8 tin; NotificationCategory.ALL lấy tất cả; danh mục khác dùng enum trong model.
  Tin ghim trước, sau đó thời gian mới nhất.
- Hồ sơ: UID demo-uid-001; UID lạ trả null.
- Điểm: 10 môn thuộc HK1 2024-2025, HK2 2024-2025, HK1 2025-2026.
  semester=null lấy tất cả; chưa phải bảng điểm thực.

## Bật Firebase Auth

Lấy app/google-services.json từ người quản lý Firebase, đúng applicationId com.giathinh.uthconnect.
Không commit cấu hình cá nhân trong thay đổi này.

~~~powershell
.\gradlew.bat :app:assembleDebug -PuseMockData=false
~~~

Android Studio: đặt useMockData=false trong cấu hình Gradle cá nhân rồi Sync/rebuild.
Cần bật Email/Password và chuẩn bị tài khoản cùng users/{uid}.
Xem DATABASE_SCHEMA.md cho schema/rules/indexes; tài liệu này không xác nhận deploy.

Cờ chỉ chuyển Auth: lịch, bảng tin, hồ sơ, điểm vẫn fake ở cả hai chế độ.
EventRepository vẫn là Firestore, cần Firebase thật, ngoài phạm vi mock của các API trên.
Không coi hồ sơ fake là hồ sơ người vừa đăng nhập Firebase.
Build mặc định phục vụ tích hợp UI; phát hành cần cấu hình backend và nghiệm thu luồng thật.

## Kiểm tra bàn giao

DemoDataRepositoryTest kiểm tra đăng nhập MSSV/email khớp hồ sơ, sai mật khẩu,
mock không khởi tạo Firebase, lịch/empty, bảng tin/lọc, cập nhật hồ sơ.
Chạy toàn bộ unit test và assembleDebug trước khi đẩy nhánh.
