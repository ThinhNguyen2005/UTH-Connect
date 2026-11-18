# Quy Chuẩn Triển Khai UTH Connect Web Portal Lên Vercel

Tài liệu hướng dẫn cấu hình, tích hợp và triển khai hệ thống Web Portal (`web/`) của UTH Connect lên nền tảng đám mây Vercel, tuân thủ các quy định tại `docs/plan/PLAN_OBSERVABILITY_AND_WEB_DEPLOYMENT.md` và `docs/web/ANTI_AI_DESIGN_SYSTEM.md`.

---

## 1. Kiến Trúc & Cấu Hình Vercel

Dự án UTH Connect là mô hình đa nền tảng (Android App tại thư mục gốc và Next.js Web Portal tại thư mục `web/`). Để Vercel nhận diện và xử lý chính xác mã nguồn Next.js:

### 1.1. Cấu hình Monorepo Root Directory trong Vercel Project Settings
Do dự án là monorepo (Android App tại thư mục gốc và Next.js Web Portal tại thư mục `web/`), thư mục gốc không sử dụng thuộc tính `rootDirectory` trong `vercel.json` (schema chính thức `https://openapi.vercel.sh/vercel.json` thiết lập `additionalProperties: false` và không cho phép `rootDirectory`, do nền tảng Vercel xác định thư mục làm việc của dự án trước khi phân tích file cấu hình).

Thay vào đó, Root Directory được cấu hình tường minh trong **Vercel Project Settings**:
- **Vercel Dashboard**: Truy cập project `uth-connect-web` -> **Settings** -> **General** -> mục **Root Directory**: thiết lập giá trị `web`.
- **Vercel CLI / Monorepo**: Khi triển khai bằng CLI, luôn chỉ định rõ `--project uth-connect-web` và chạy từ thư mục `web/` hoặc truyền tham số thư mục `web` (`vercel deploy web --project uth-connect-web`).

### 1.2. Thiết Lập Dự Án (Project Settings)
- **Project Name**: `uth-connect-web`
- **Framework Preset**: `Next.js`
- **Root Directory**: `web`
- **Build Command**: `next build` (hoặc `npm run build`)
- **Output Directory**: `.next` (mặc định của Next.js App Router)
- **Node.js Version**: `24.x` (đã kích hoạt đồng bộ trên Vercel Project)

---

## 2. Quản Lý Biến Môi Trường (Environment Variables)

Toàn bộ các biến môi trường kết nối Supabase được trích xuất từ `web/.env.local` và phải được thiết lập trên Vercel cho các môi trường **Preview**, **Production** và **Development**:

| Tên biến | Giá trị mẫu | Phạm vi | Mô tả |
|---|---|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | `https://bvxilfcctwmgmrwrncvd.supabase.co` | Preview, Production, Development | Endpoint REST / Realtime của Supabase Cloud |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | `sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo` | Preview, Production, Development | Public Anon Key cho SSR & Client-side queries |

> ⚠️ **Quy tắc an ninh**: Tuyệt đối không đưa `SUPABASE_SERVICE_ROLE_KEY` lên môi trường Vercel của Web Portal client. Biến `NEXT_PUBLIC_*` sẽ bị nhúng công khai vào JavaScript bundle.

---

## 3. Cấu Hình Supabase Auth Callbacks

Sau khi Vercel tạo domain Preview hoặc Production, bắt buộc phải cập nhật whitelist Redirect URLs tại **Supabase Dashboard -> Authentication -> URL Configuration**:

- **Site URL**: `https://<ten-mien-chinh>.vercel.app`
- **Redirect URLs**:
  - `http://localhost:3000/auth/callback` (môi trường lập trình nội bộ)
  - `https://*-<team-slug>.vercel.app/auth/callback` (wildcard cho các bản Preview)
  - `https://<ten-mien-chinh>.vercel.app/auth/callback` (Production)

---

## 4. Quy Trình Triển Khai Bằng Vercel CLI

### 4.1. Cài đặt và Đăng nhập
```powershell
npx --yes vercel login
```
Trình duyệt sẽ mở trang cấp phép mã thiết bị (`oauth/device?user_code=...`) để xác nhận liên kết tài khoản.

### 4.2. Triển khai Bản Preview (Preview Deployment)
Thực hiện triển khai bằng một trong hai cách dưới đây (luôn kèm cờ `--project uth-connect-web` để đảm bảo gắn đúng project):

**Cách 1: Triển khai từ thư mục `web/`**:
```powershell
cd d:\APP\UTH-Connect\web
npx --yes vercel deploy --project uth-connect-web `
  --build-env NEXT_PUBLIC_SUPABASE_URL="https://bvxilfcctwmgmrwrncvd.supabase.co" `
  --build-env NEXT_PUBLIC_SUPABASE_ANON_KEY="sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo" `
  --env NEXT_PUBLIC_SUPABASE_URL="https://bvxilfcctwmgmrwrncvd.supabase.co" `
  --env NEXT_PUBLIC_SUPABASE_ANON_KEY="sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo" `
  --yes
```

**Cách 2: Triển khai từ thư mục gốc của repository**:
```powershell
npx --yes vercel deploy web --project uth-connect-web `
  --build-env NEXT_PUBLIC_SUPABASE_URL="https://bvxilfcctwmgmrwrncvd.supabase.co" `
  --build-env NEXT_PUBLIC_SUPABASE_ANON_KEY="sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo" `
  --env NEXT_PUBLIC_SUPABASE_URL="https://bvxilfcctwmgmrwrncvd.supabase.co" `
  --env NEXT_PUBLIC_SUPABASE_ANON_KEY="sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo" `
  --yes
```

### 4.3. Kiểm thử Health Check Sau Triển Khai
Truy cập URL Preview được sinh ra và kiểm tra mã phản hồi:
```powershell
# Kiểm tra Route chính
npx vercel curl / --deployment <preview-url>

# Kiểm tra Trang đăng nhập
npx vercel curl /login --deployment <preview-url>
```
Cả hai endpoint phải trả về HTTP 200 OK và tải giao diện hoàn chỉnh.

### 4.4. Bản Triển Khai Hiện Hành (Active Live Deployments)
- **Vercel Project**: `uth-connect-web` (`prj_KiOeJtlZcQVKJ88sYzxX1oLkHAJQ`)
- **Node.js Runtime**: `24.x` (Do Vercel ngừng hỗ trợ Node.js 20.x vào cuối năm 2026)
- **Preview Deployment (develop)**:
  - URL: `https://uth-connect-4ynxduvvn-nguyenthinhk52005-5982.vercel.app`
  - Alias: `https://uth-connect-web-git-develop-nguyenthinhk52005-5982.vercel.app`
  - Health: HTTP 200 OK (`/`, `/login`)
- **Preview Deployment (main)**:
  - URL: `https://uth-connect-4l9sn55uk-nguyenthinhk52005-5982.vercel.app`
  - Alias: `https://uth-connect-web-git-main-nguyenthinhk52005-5982.vercel.app`
  - Health: HTTP 200 OK (`/`, `/login`)


---

## 5. Quy Chuẩn Vận Hành & Giám Sát (Observability)
- **Gói Vercel hiện hành**: Hobby / Pro (theo dõi usage tại `vercel.com/dashboard/usage`).
- **Giám sát Logs**: Sử dụng `npx vercel logs --deployment <preview-url>` để kiểm tra runtime errors và status codes $\ge 500$.
- **Người chịu trách nhiệm**: Quản trị viên dự án UTH Connect định kỳ rà soát build bandwidth và function execution time trước các kỳ cao điểm đăng ký tín chỉ và công bố sự kiện trường.
