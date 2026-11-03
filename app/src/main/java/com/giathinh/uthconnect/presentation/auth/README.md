# Auth — feature/auth-phu

Luồng: AuthScreen → Action → AuthViewModel → SignIn/ResetPassword/AccessSession use cases.
Route nối Hilt, trạng thái theo lifecycle, cổng nội dung đã đăng nhập và prompt sinh trắc học.

Production dùng FirebaseAuthRepository. Cần google-services.json, tài khoản, hồ sơ và claims được cấp
từ backend. MSSV-only chưa có hợp đồng ánh xạ; app hướng dẫn dùng email trường. Không fallback demo.
Xem tài liệu gốc tại `docs/AUTH_PROFILE_STATUS.md` và các test mirror Auth/Profile.
