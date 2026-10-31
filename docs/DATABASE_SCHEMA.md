# UTH Connect – Cloud Firestore Database Schema & Indexing

Tài liệu thiết kế cơ sở dữ liệu NoSQL Cloud Firestore dành cho dự án **UTH Connect**.
Người phụ trách: **Trọng Nghĩa (KAN-29 - Data Layer / Backend)**.

---

## 1. Danh sách Collections chính

| Collection Path | Mô tả | Ai có quyền ghi |
|---|---|---|
| `users/{uid}` | Thông tin tài khoản sinh viên/người dùng | User sở hữu, Admin |
| `events/{eventId}` | Danh sách sự kiện, workshop, hội thảo | BTC (Organizer), Admin |
| `events/{eventId}/registrations/{uid}` | Danh sách đăng ký tham gia sự kiện | Sinh viên đăng ký |
| `events/{eventId}/attendance/{uid}` | Thông tin điểm danh (check-in QR) | BTC |
| `notifications/{notificationId}` | Thông báo, tin tức nhà trường | Admin, Ban Đào tạo |
| `schedules/{scheduleId}` | Lịch học & thời khóa biểu | Hệ thống đào tạo / Admin |
| `grades/{gradeId}` | Điểm thi & kết quả học tập | Hệ thống đào tạo / Admin |

---

## 2. Chi tiết cấu trúc Documents

### 2.1 Collection: `users`
*Document ID: `uid` (Firebase Auth UID)*

```json
{
  "uid": "String (ex: 'v8Z...B9')",
  "studentId": "String (MSSV: '2251010001')",
  "email": "String ('nghia.nt@uth.edu.vn')",
  "displayName": "String ('Nguyễn Trọng Nghĩa')",
  "className": "String ('22DHTH04')",
  "major": "String ('Công nghệ Thông tin')",
  "faculty": "String ('Khoa CNTT')",
  "avatarUrl": "String? (URL Storage)",
  "phoneNumber": "String?",
  "role": "String ('student' | 'organizer' | 'admin')",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### 2.2 Collection: `events`
*Document ID: `eventId` (Auto-generated)*

```json
{
  "id": "String",
  "title": "String ('Workshop AI & LLM')",
  "description": "String",
  "bannerUrl": "String?",
  "location": "String ('Hội trường A')",
  "campus": "String ('Quang Trung' | 'Nguyễn Tất Thành' | 'Thủ Đức')",
  "startAt": "Timestamp",
  "endAt": "Timestamp",
  "registrationDeadline": "Timestamp",
  "maxParticipants": "Number (ex: 200)",
  "currentRegistered": "Number (Atomic increment via Transaction)",
  "points": "Number (Điểm rèn luyện, ex: 5)",
  "status": "String ('UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELLED')",
  "organizerId": "String (uid của ban tổ chức)",
  "organizerName": "String ('Đoàn Thanh niên UTH')"
}
```

#### Subcollection: `events/{eventId}/registrations/{uid}`
```json
{
  "registeredAt": "Timestamp",
  "studentId": "String",
  "displayName": "String",
  "email": "String",
  "status": "String ('REGISTERED' | 'CANCELLED')"
}
```

#### Subcollection: `events/{eventId}/attendance/{uid}`
```json
{
  "checkedInAt": "Timestamp",
  "checkedInBy": "String (uid người quét)",
  "status": "String ('PRESENT' | 'ABSENT')"
}
```

### 2.3 Collection: `notifications`
*Document ID: `notificationId` (Auto-generated)*

```json
{
  "id": "String",
  "title": "String",
  "summary": "String",
  "content": "String (Markdown / HTML)",
  "category": "String ('ACADEMIC' | 'EXAMINATION' | 'TUITION' | 'ACTIVITIES' | 'GENERAL')",
  "author": "String ('Phòng Đào tạo')",
  "publishedAt": "Timestamp / Epoch ms",
  "isPinned": "Boolean",
  "attachmentUrl": "String?",
  "imageUrl": "String?"
}
```

### 2.4 Collection: `schedules`
*Document ID: `scheduleId` (Auto-generated)*

```json
{
  "id": "String",
  "studentId": "String ('2251010001')",
  "semester": "String ('HK1 2025-2026')",
  "courseCode": "String ('ENG201')",
  "courseName": "String ('Tiếng Anh Chuyên Ngành')",
  "lecturerName": "String ('ThS. Nguyễn Thị Lan')",
  "room": "String ('B102')",
  "campus": "String ('Quang Trung')",
  "dayOfWeek": "Number (2..8)",
  "startPeriod": "Number (1..12)",
  "endPeriod": "Number (1..12)",
  "startTime": "String ('07:00')",
  "endTime": "String ('09:30')",
  "credits": "Number (3)"
}
```

### 2.5 Collection: `grades`
*Document ID: `gradeId` (Auto-generated)*

```json
{
  "id": "String",
  "studentId": "String ('2251010001')",
  "semester": "String ('HK1 2024-2025')",
  "courseCode": "String ('CS101')",
  "courseName": "String ('Nhập môn Lập trình')",
  "credits": "Number (4)",
  "attendanceScore": "Number (9.5)",
  "midtermScore": "Number (8.5)",
  "finalScore": "Number (9.0)",
  "totalScore10": "Number (9.0)",
  "totalScore4": "Number (4.0)",
  "letterGrade": "String ('A')"
}
```

---

## 3. Composite Indexes cần cấu hình

Xem file [`firestore.indexes.json`](../firestore.indexes.json) tại root dự án:
1. `events`: `campus ASC` + `status ASC` + `startAt ASC`
2. `events`: `status ASC` + `startAt ASC`
3. `notifications`: `category ASC` + `isPinned DESC` + `publishedAt DESC`
4. `notifications`: `isPinned DESC` + `publishedAt DESC`
5. `schedules`: `studentId ASC` + `semester ASC` + `dayOfWeek ASC` + `startPeriod ASC`
6. `grades`: `studentId ASC` + `semester ASC`

---

## 4. Deploy Rules & Indexes lên Firebase Console

Khi cần deploy rules và indexes từ máy lên Firebase:
```powershell
# Yêu cầu đã cài firebase-tools: npm install -g firebase-tools
firebase login
firebase use uthconnect-d4d91
firebase deploy --only firestore:rules,firestore:indexes
```
Hoặc copy trực tiếp nội dung `firestore.rules` và paste vào tab **Rules** trên Firebase Console.
