# Profile — feature/auth-phu

ProfileScreen nhận UiState, phát Action; ViewModel điều phối GetProfile, UpdateContact, ChangePassword
và ManageSession use cases. Chỉ ba trường liên hệ được sửa; dữ liệu học tập và quyền do server cấp.

Route quản lý Back khi lưu, prompt sinh trắc học và vùng Snackbar riêng để không che nút đăng xuất.
Production cần Firebase/Firestore/claims/rules được cấu hình; xem `docs/AUTH_PROFILE_STATUS.md`.
Xác nhận sinh trắc học thật và đồng bộ nhiều thiết bị vẫn cần nghiệm thu môi trường đã cấu hình.
