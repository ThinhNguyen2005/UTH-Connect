# Báo Cáo Phiên Làm Việc: Kiến Trúc Web Portal & Admin Console + Tích Hợp Đăng Nhập Supabase SSR

**Ngày thực hiện:** 07/11/2026  
**Chủ đề:** Xây dựng khung kiến trúc Cổng Thông Tin Web & Bảng Điều Khiển Quản Trị UTH Connect (`web/`) theo Anti-AI Design System và tích hợp xác thực đa phương thức Supabase (Google SSO + Email/Password + PKCE Session Cookies).

---

## 1. Tóm tắt bối cảnh & Toàn cảnh thay đổi

- **Bối cảnh:**
  - Hệ thống UTH Connect trước đây tập trung vào ứng dụng Android. Cần mở rộng phiên bản Web Portal dành cho Sinh viên và Admin Console riêng biệt dành cho Ban quản trị (`/admin`).
  - Dự án đòi hỏi một ngôn ngữ thiết kế kỷ luật, chuyên nghiệp ("Anti-AI Design System"), cấm tiệt phong cách bo tròn quá đà, viền card giả tạo, gradient neon và hiệu ứng AI generic.
  - Phân quyền người dùng đã được định hình vững chắc trên Supabase Auth & RLS (`admin` cho `nguyenthinhk52005@gmail.com`, `lecturer`/`organizer` cho giảng viên, `student` cho sinh viên).
- **Phạm vi hoàn thành:**
  - Thiết lập nền tảng Next.js 14 (App Router) + Tailwind CSS + Refine v4 + Supabase SSR (`@supabase/ssr`).
  - Ban hành cẩm nang thiết kế [docs/web/ANTI_AI_DESIGN_SYSTEM.md](file:///d:/APP/UTH-Connect/docs/web/ANTI_AI_DESIGN_SYSTEM.md) và tích hợp vào quy chuẩn [AGENTS.md](file:///d:/APP/UTH-Connect/AGENTS.md).
  - Tách bạch cấu trúc 2 tầng:
    - **Cổng Sinh viên (`/`)**: Sử dụng kiến trúc Sidebar ứng dụng dọc linh hoạt (`AppSidebar.tsx` + `PortalLayout.tsx`), thu gọn 260px sang thanh 72px lưu trạng thái `localStorage`, phân nhóm Học tập & Tra cứu, tích hợp tìm kiếm nhanh, mobile drawer và chip người dùng. Bao gồm Trang chủ tổng quan, Thông báo, Sự kiện & Vé QR, Ghép trọ & KTX, Tra cứu Cơ sở & Danh bạ, Quản lý phiên bảo mật (`/settings/security`), Chờ kích hoạt email (`/verify-email`), Đăng ký tài khoản chuẩn hóa (`/register`).
    - **Admin Console (`/admin`)**: Gồm Trang tổng quan, Thành viên & RBAC, Kiểm duyệt tin trọ, Soạn thảo thông báo (`AdminLayout`), bảo vệ nghiêm ngặt qua `AdminGuard`.
  - Triển khai toàn diện tính năng **Đăng nhập & Xác thực**:
    - Chuyển đổi liên kết "Use OTP" sang "Xác thực email..." điều hướng tới luồng gửi link kích hoạt email chuẩn xác.
    - Chuẩn hóa form Đăng ký (`/register`) tương thích 100% với model ứng dụng Android (`StudentProfile`) và bảng `public.profiles` của Supabase: Mã số sinh viên (MSSV), Họ tên, Email trường, Khoa/Viện, Chuyên ngành, Lớp sinh hoạt, Số điện thoại, Mật khẩu & Xác nhận mật khẩu.
    - Nâng cấp trigger `handle_new_user()` trên Supabase PostgreSQL để tự động lưu trọn vẹn thông tin sinh viên vào `public.profiles` ngay khi đăng ký.
    - Trang Đăng nhập (`/login`): Hỗ trợ Google SSO UTH (`@ut.edu.vn`), hỗ trợ nhập MSSV trực tiếp (tự chuẩn hóa MSSV -> email), thông báo lỗi/thành công tức thời, điều hướng theo vai trò (`/admin` cho admin, `/` cho sinh viên/giảng viên), và bảng điều khiển 1-click test nhanh 3 tài khoản.
  - Khắc phục triệt để lỗi đứng hình khi bấm đăng nhập (Refine silent swallow & Supabase GoTrue token NULL scan).
  - Hoàn tất đợt rà soát và khắc phục rò rỉ bộ nhớ (Memory Leak Audit): gắn cờ `isMounted` cho tất cả hook `useEffect` gọi API Supabase bất đồng bộ, dọn dẹp bộ đếm timer / interval khi component unmount.

---

## 2. Kết quả đạt được & Chi tiết Kỹ thuật

### A. Cấu trúc Thư mục `web/`
```
web/
├── src/
│   ├── app/
│   │   ├── auth/callback/route.ts       # OAuth / PKCE Code Exchange Route
│   │   ├── admin/                       # Admin Console pages
│   │   │   ├── page.tsx                 # Tổng quan số liệu & Quản trị
│   │   │   ├── users/page.tsx           # Phân quyền RBAC & Quản lý người dùng
│   │   │   ├── moderation/page.tsx      # Kiểm duyệt tin đăng trọ
│   │   │   └── announcements/page.tsx   # Soạn thảo thông báo trường
│   │   ├── campuses/page.tsx            # Tra cứu cơ sở & danh bạ
│   │   ├── events/page.tsx              # Danh sách sự kiện & Đăng ký
│   │   ├── housing/page.tsx             # Danh sách phòng trọ & tìm bạn ghép
│   │   ├── notifications/page.tsx       # Thông báo nhà trường
│   │   ├── login/page.tsx               # Màn hình Đăng nhập / Đăng ký
│   │   └── page.tsx                     # Cổng sinh viên UTH Connect
│   ├── components/
│   │   ├── auth/AdminGuard.tsx          # Bảo vệ đường dẫn /admin/*
│   │   └── layout/                      # PortalLayout & AdminLayout
│   ├── providers/
│   │   ├── auth-provider.ts             # Refine AuthProvider (Login, Register, Check, Role)
│   │   └── access-control-provider.ts   # RBAC Access Control
│   ├── utils/
│   │   ├── supabase-client.ts           # Browser Client (@supabase/ssr)
│   │   ├── supabase-server.ts           # Server Client (@supabase/ssr)
│   │   └── supabase-middleware.ts       # Middleware session updater
│   └── middleware.ts                    # Edge middleware đồng bộ session
```

### B. Kết quả Biên dịch `next build`
```
Route (app)                              Size     First Load JS
┌ ○ /                                    4.3 kB          252 kB
├ ○ /_not-found                          873 B          88.2 kB
├ ○ /admin                               3.85 kB         251 kB
├ ○ /admin/announcements                 2.54 kB         253 kB
├ ○ /admin/moderation                    1.79 kB         252 kB
├ ○ /admin/users                         2.05 kB         252 kB
├ ƒ /auth/callback                       0 B                0 B
├ ○ /campuses                            3.68 kB         190 kB
├ ○ /events                              3.78 kB         251 kB
├ ○ /events/create                       4.04 kB         252 kB
├ ○ /housing                             3.92 kB         252 kB
├ ○ /housing/create                      4.05 kB         252 kB
├ ○ /login                               4.36 kB         181 kB
├ ○ /notifications                       3.86 kB         251 kB
└ ○ /notifications/create                4.04 kB         252 kB
+ First Load JS shared by all            87.3 kB

✓ Compiled successfully
✓ Generating static pages (21/21)
✓ Exit code: 0
```

---

## 3. Bài học & Kinh nghiệm rút ra

1. **Khắc phục Refine `useLogin()` nuốt lỗi im lặng:**
   - Trong `@refinedev/core`, hook `useLogin()` bọc lời gọi `authProvider.login()`. Do `authProvider.login` trả về `{ success: false, error }` mà không ném ngoại lệ (`throw`), React Query Mutation đánh dấu thao tác là `isSuccess: true`. Hệ quả là `onError` không kích hoạt, và nếu không có router provider cấu hình chuyển hướng, trang đăng nhập bị đứng im không phản hồi.
   - Giải pháp: Gọi trực tiếp `authProvider.login()` trong `LoginPage`, bắt kết quả trả về để hiển thị ngay banner thông báo lỗi màu đỏ hoặc thành công màu xanh, sau đó chủ động thực hiện `window.location.href = res.redirectTo`.
2. **Lỗi GoTrue Go Driver NULL String Scan trong Supabase `auth.users`:**
   - Khi tạo thủ công hoặc cập nhật người dùng trong bảng `auth.users` qua SQL, nếu các cột chuỗi như `confirmation_token`, `recovery_token`, `email_change_token_new` bị gán `NULL` thay vì chuỗi rỗng `''`, trình quét GoTrue nội bộ sẽ bị sập với lỗi `Database error querying schema`. Cần đảm bảo các cột token luôn mang giá trị `''` thay vì `NULL`.
3. **Phòng chống Rò rỉ Bộ nhớ (Memory Leak Audit):**
   - Khi gọi API bất đồng bộ Supabase trong `useEffect`, luôn sử dụng cờ hủy `let isMounted = true; return () => { isMounted = false; };` để ngăn chặn việc gọi `setState` sau khi component đã bị unmount khi người dùng chuyển trang nhanh.
   - Mọi `setTimeout` và `setInterval` (như bộ đếm ngược xác thực email hay thông báo toast bảo mật) phải được lưu vào biến ref và `clearTimeout`/`clearInterval` triệt để trong cleanup function.
4. **Cơ chế PKCE Flow với `@supabase/ssr` & Hydration Safety:**
   - Trong Next.js App Router, sử dụng `/auth/callback/route.ts` với `exchangeCodeForSession` ghi nhận session cookies trực tiếp. Khởi tạo `QueryClient` thông qua `useState(() => new QueryClient(...))` để tránh khởi tạo lại đối tượng qua mỗi chu kỳ render của React.

---

## 4. Vấn đề tồn đọng / Nợ kỹ thuật

- Sau khi người dùng Google đăng nhập lần đầu, trigger `handle_new_user()` trên Supabase đã tự động gán role `admin` cho `nguyenthinhk52005@gmail.com` và `student` cho các email khác. Cần thử nghiệm end-to-end trên trình duyệt thực tế khi cấu hình Google Client ID trên Supabase Dashboard hoàn tất.
- Trang chi tiết tin trọ và chi tiết sự kiện trên Web có thể bổ sung tương tác bình luận thời gian thực qua Supabase Realtime Channels.

---

## 5. Kế hoạch cho các phiên tiếp theo

1. **Kiểm thử trải nghiệm thực tế trên trình duyệt:** Chạy `npm run dev` trên cổng `3000` và thực hiện kịch bản đăng nhập với các tài khoản sinh viên, giảng viên và admin.
2. **Nâng cấp Dashboard biểu đồ:** Bổ sung biểu đồ thống kê sinh viên tham gia sự kiện và tỷ lệ duyệt tin trọ trên `/admin` sử dụng thư viện biểu đồ nhẹ (Recharts hoặc CSS Bars).
3. **Đồng bộ hóa tính năng Android - Web:** Bổ sung tính năng quét mã QR điểm danh trực tuyến cho ban tổ chức ngay trên giao diện Web.
