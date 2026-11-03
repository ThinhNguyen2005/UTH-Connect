# Auth — feature/auth-phu

Luồng: AuthScreen → Action → AuthViewModel → SignIn/ResetPassword/AccessSession use cases.
Route nối Hilt, trạng thái theo lifecycle, cổng nội dung đã đăng nhập và prompt sinh trắc học.

Production dùng FirebaseAuthRepository. Cần google-services.json, tài khoản, hồ sơ và claims được cấp
từ backend. MSSV-only chưa có hợp đồng ánh xạ; app hướng dẫn dùng email trường. Không fallback demo.
Xem tài liệu gốc tại `docs/AUTH_PROFILE_STATUS.md` và các test mirror Auth/Profile.


Auth3-1 bổ sung SignUp/Google, pending AuthIdentity và hai slice verifyemail/forgotpassword.
AuthRoute chỉ mở Main khi session chứa profile đã kiểm tra Firebase/claims; pending đi VerifyEmail.
Channel cho Credential Manager; nonce được kiểm tra và Firebase xác thực token. Không link dựa vào email.
Xem `docs/AUTH3_FIREBASE_SETUP.md` cho Console/Emulator và các giới hạn nghiệm thu production.
