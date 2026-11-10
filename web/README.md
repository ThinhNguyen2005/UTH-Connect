# UTH Connect — Web Portal & Admin Dashboard

Hệ thống Web Portal & Admin Dashboard đa phân quyền dành cho **Sinh viên, Giảng viên và Quản trị viên** Trường Đại học Giao thông vận tải TP.HCM (UTH), xây dựng trên nền tảng **Next.js 14 + Refine + Tailwind CSS + Supabase**.

---

## 1. Kiến trúc Đa Phân quyền (RBAC Matrix)

Ứng dụng kết nối trực tiếp với Supabase Auth & PostgreSQL với cơ chế tăng tốc bằng JWT Custom Claims (`auth.jwt() -> 'app_metadata' ->> 'role'`):

| Vai trò | Phân quyền trên Web Portal | Hành động khả dụng |
| :--- | :--- | :--- |
| **`ADMIN` (Quản trị viên)** | Toàn quyền kiểm soát hệ thống | Quản lý người dùng (`/users`), phân quyền (Admin / Giảng viên / Sinh viên), duyệt xác thực MSSV, xóa/sửa mọi nội dung. |
| **`LECTURER` (Giảng viên / Ban tổ chức)** | Đăng tải & Quản trị hoạt động | Đăng thông báo toàn trường (`/notifications/create`), tạo sự kiện (`/events/create`), xem điểm danh, xem tin trọ. |
| **`STUDENT` (Sinh viên)** | Người dùng tiện ích | Xem thông báo, đăng ký sự kiện, đăng bài & tìm bạn ở ghép (`/housing/create`). Bị chặn truy cập các trang quản trị. |

---

## 2. Cấu trúc Thư mục

```
web/
├── .env.local                    # Biến môi trường Supabase URL & Anon Key
├── tailwind.config.ts            # Cấu hình màu sắc Design System UTH (Navy #0F5FC2, Amber #F59E0B)
├── src/
│   ├── app/                      # Next.js App Router
│   │   ├── layout.tsx            # Root layout, Refine Provider
│   │   ├── page.tsx              # Dashboard Overview & KPI Metrics
│   │   ├── login/page.tsx        # Màn hình đăng nhập Modern Minimalist ("Hello Again")
│   │   ├── notifications/        # Quản lý & xem thông báo trường
│   │   ├── events/               # Quản lý & đăng ký sự kiện
│   │   ├── housing/              # Module Ghép trọ & Ký túc xá
│   │   ├── campuses/             # Danh bạ cơ sở & hotline UTH
│   │   └── users/                # [Admin Only] Quản trị người dùng & Phân quyền
│   ├── components/
│   │   └── layout/               # Header, Sidebar, AppLayout
│   ├── providers/
│   │   ├── auth-provider.ts      # Refine AuthProvider tích hợp Supabase
│   │   ├── access-control-provider.ts # Refine AccessControlProvider xử lý RBAC
│   │   └── data-provider.ts      # Refine Supabase dataProvider
│   └── utils/
│       └── supabase-client.ts    # Khởi tạo Supabase Browser Client
```

---

## 3. Hướng dẫn Chạy Ứng dụng

```bash
# 1. Di chuyển vào thư mục web
cd web

# 2. Cài đặt các thư viện (đã cài đặt)
npm install

# 3. Chạy môi trường Development
npm run dev

# 4. Kiểm tra build Production
npm run build
```

Mặc định ứng dụng sẽ chạy tại địa chỉ: `http://localhost:3000`.
