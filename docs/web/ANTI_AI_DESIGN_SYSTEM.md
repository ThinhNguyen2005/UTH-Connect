# UTH Connect — Anti-AI Design System Playbook

> **Quy chuẩn Thiết kế Web Bắt buộc**: Tài liệu này là nguồn sự thật tuyệt đối (Single Source of Truth) cho toàn bộ giao diện Web Portal và Admin Dashboard trong thư mục `web/`. Mục tiêu tối thượng là đạt đẳng cấp thẩm mỹ "Human-Crafted" (tương đương chuẩn mực của Linear, Stripe, Vercel, Apple) và triệt tiêu 100% các dấu hiệu nhận diện rập khuôn, rẻ tiền của AI ("AI-generated look").

---

## 1. Danh sách Cấm Tuyệt đối (Anti-AI Prohibitions)

1. **Tuyệt đối không dùng viền chia khối thông thường (No Borders)**:
   - CẤM dùng `border`, `border-gray-*`, `border-slate-*`, hay thẻ `<hr>`, `divide-y` để đóng khung các Card hoặc ngăn cách các dòng.
   - Thay vào đó, phân tách không gian bằng **Tonal Layering (Độ chênh lệch sắc độ nền)** và **Khoảng trắng (Whitespace)**.
2. **Không dùng Icon bọc trong quả cầu màu nhạt (No Colored Circle Icons)**:
   - CẤM đặt icon trong khối tròn/vuông màu nhạt (ví dụ: icon xanh đặt trong `bg-blue-100 rounded-full`).
   - CẤM dùng emoji làm icon hệ thống.
3. **Không Gradient lòe loẹt**:
   - CẤM dùng gradient text (tím sang hồng/xanh neon) hoặc nút bấm dải màu cầu vồng.
4. **Không bo góc quá đà (No Over-rounding)**:
   - CẤM dùng `rounded-2xl`, `rounded-3xl`, hoặc `rounded-full` cho Card nội dung, Input, hay Button thông thường.
5. **Không dùng Shadow đen/đậm mặc định**:
   - CẤM dùng `shadow-md`, `shadow-lg`, `shadow-xl` mặc định của Tailwind tạo bóng đục, bẩn và nặng nề.
6. **Không căn giữa văn bản dài**:
   - CẤM `text-center` cho các đoạn văn dài hơn 2 dòng hoặc căn giữa toàn bộ ruột Card.

---

## 2. Hệ thống Bo Góc Chuẩn (4px Corner-Radius Scale)

Mọi phần tử trên giao diện tuân theo đúng bảng kích thước vàng:

| Phần tử Giao diện | Kích thước Bo góc | Class Tailwind |
| :--- | :--- | :--- |
| **Badge, Tag, Kbd, Checkbox** | `4px` – `6px` | `rounded` hoặc `rounded-md` |
| **Button, Input, Select, Textarea** | **`8px` (Con số vàng)** | `rounded-lg` (`0.5rem`) |
| **Card nội dung, Toast, Popover, Bento Item** | **`12px`** | `rounded-xl` (`0.75rem`) |
| **Modal, Dialog, Container bao ngoài lớn** | **`16px` (Giới hạn tối đa)** | `rounded-2xl` (`1rem`) |

### Quy tắc Lồng Khối Đồng Tâm (Concentric Nested Radius)
$$\text{Radius}_{\text{ngoài}} = \text{Radius}_{\text{trong}} + \text{Padding}$$
- *Ví dụ:* Nếu Card bao ngoài có `rounded-xl` (`12px`) và khoảng cách đệm `p-1` (`4px`), thì ảnh/khối bên trong bắt buộc phải có `border-radius: 8px` (`rounded-lg`).

---

## 3. Phân Tầng Sắc Độ (Surface Tonal Layering) & Ambient Shadow

Không bao giờ dùng đường viền để chia card. Hãy phân tầng theo 3 lớp sắc độ:

### 3.1. Bảng màu phân lớp (Light Mode)
- **Lớp 0 (Nền trang web chính - Background)**: `#F8FAFC` (Slate 50).
- **Lớp 1 (Card nội dung, khối nổi)**: `#FFFFFF` (Trắng tinh khiết). Tự động nổi bật trên nền Slate 50 mà không cần viền.
- **Lớp 2 (Input field, khối chìm, vùng hover)**: `#F1F5F9` (Slate 100).

### 3.2. Chuẩn Ambient Shadow Siêu Nhẹ
- 90% các Card trên trang **không cần shadow** (chỉ cần màu nền trắng trên nền Slate).
- Đối với Modal, Popover hoặc Header trôi nổi: Chỉ dùng duy nhất công thức Ambient Shadow khuếch tán rộng với độ mờ cực thấp ($\alpha \le 0.04$):
  ```css
  box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.04), 0 2px 6px -1px rgba(15, 23, 42, 0.02);
  ```

---

## 4. Typography & Số Liệu

1. **Bộ Font**: `Inter` hoặc hệ thống Sans-serif trung tính `system-ui`.
2. **Khoảng cách chữ (Tracking) & Chiều cao dòng (Leading)**:
   - **Heading lớn ($\ge 20\text{px}$)**: Co chữ lại với `tracking-tight` (`-0.025em`), `leading-tight` (`1.2`).
   - **Body text (`14px` - `16px`)**: `tracking-normal`, `leading-relaxed` (`1.5` - `1.6`).
   - **Label / Overline in hoa**: `text-xs font-semibold tracking-wider text-slate-500 uppercase`.
3. **Số liệu tài chính & thống kê**:
   - Mọi con số (giá phòng trọ, điểm rèn luyện, số lượng người, ngày giờ) **BẮT BUỘC** khai báo `tabular-nums` (`font-variant-numeric: tabular-nums`) để các chữ số thẳng cột tuyệt đối.
4. **Phân cấp sắc độ chữ (3 tầng)**:
   - **Primary Text (Tiêu đề, dữ liệu quan trọng)**: `#0F172A` (Slate 900).
   - **Secondary Text (Mô tả, nội dung đọc)**: `#475569` (Slate 600).
   - **Muted Text (Nhãn phụ, thời gian, placeholder)**: `#94A3B8` (Slate 400).

---

## 5. Icon Đơn Sắc (Monochrome Iconography)

1. **Bộ Icon đồng nhất**: Chỉ dùng duy nhất **Lucide Icons**.
2. **Độ dày nét (Stroke Width)**: Cố định ở `1.5px` hoặc `1.75px`.
3. **Kích thước**: `16px` (`w-4 h-4`) đi kèm chữ nhỏ, `20px` (`w-5 h-5`) cho navigation.
4. **Màu sắc**: Luôn mang màu **Muted** hoặc **Secondary** (`#64748B`), khi hover phần tử cha mới đổi sang màu Primary (`#0F172A`). Tuyệt đối không bọc icon vào khung tròn màu mè.

---

## 6. Nút Bấm Tinh Tế & Tỷ Lệ Màu 60-30-10

1. **Tiết chế màu Brand**: Xanh Cobalt UTH `#0F5FC2` và Vàng Amber `#F59E0B` chỉ chiếm **5% - 10%** diện tích giao diện (nút CTA chính, badge trạng thái quan trọng).
2. **Nút Primary Đen Than (`#0F172A`)**: Ưu tiên dùng nền đen than chữ trắng `#FFFFFF` cho các thao tác tiêu chuẩn (chuẩn Stripe / Vercel).
3. **Nút Secondary**: Dùng nền xám nhạt `#F1F5F9` chữ `#334155`, không viền, khi hover đổi màu `#E2E8F0`.
4. **Tương tác vi mô (Micro-interaction)**: Nút bấm khi nhấn giữ có hiệu ứng vật lý `active:scale-[0.98] transition-transform duration-150`.

---

## 7. Phân Tách Không Gian Làm Việc Web

- **`/` (UTH Connect Portal - Dành cho Sinh viên & Giảng viên)**: Nhẹ nhàng, trực quan, phục vụ tra cứu thông báo, đăng ký sự kiện, đăng tin ghép trọ.
- **`/admin` (Management Console - Dành riêng cho Quản trị viên)**: Mật độ dữ liệu cao (Data-dense), bảng biểu quản lý người dùng, phân quyền RBAC và kiểm duyệt tin. Chặn 100% truy cập trái phép bằng `AdminGuard`.
