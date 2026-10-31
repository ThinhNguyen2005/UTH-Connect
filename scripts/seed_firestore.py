import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

API_KEY = "AIzaSyBEHyI3RB4cefMk9n0F9UYbr1CojA5F8c8"
PROJECT_ID = "uthconnect-d4d91"
EMAIL = "2023040123@ut.edu.vn"
PASSWORD = "123456"

print("1. Authenticating with Firebase Auth...")
auth_url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={API_KEY}"
auth_data = json.dumps({"email": EMAIL, "password": PASSWORD, "returnSecureToken": True}).encode("utf-8")
req = urllib.request.Request(auth_url, data=auth_data, headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as resp:
    res = json.loads(resp.read().decode("utf-8"))

token = res["idToken"]
uid = res["localId"]
print(f"   Authenticated UID: {uid} ({EMAIL})")

courses_list = [
    {"mapValue": {"fields": {"code": {"stringValue": "IT001"}, "name": {"stringValue": "Lập trình thiết bị di động"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.100.000 VNĐ"}}}},
    {"mapValue": {"fields": {"code": {"stringValue": "IT002"}, "name": {"stringValue": "Kiến trúc & Thiết kế phần mềm"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.100.000 VNĐ"}}}},
    {"mapValue": {"fields": {"code": {"stringValue": "IT003"}, "name": {"stringValue": "Kiểm thử & Đảm bảo chất lượng"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.100.000 VNĐ"}}}},
    {"mapValue": {"fields": {"code": {"stringValue": "IT004"}, "name": {"stringValue": "Công nghệ phần mềm chuyên sâu"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.100.000 VNĐ"}}}},
    {"mapValue": {"fields": {"code": {"stringValue": "IT005"}, "name": {"stringValue": "Đồ án chuyên ngành KTPM"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.100.000 VNĐ"}}}},
    {"mapValue": {"fields": {"code": {"stringValue": "GE006"}, "name": {"stringValue": "Kỹ năng nghề nghiệp & Giao tiếp"}, "credits": {"integerValue": "3"}, "fee": {"stringValue": "2.150.000 VNĐ"}}}}
]

criteria_list = [
    {"mapValue": {"fields": {"title": {"stringValue": "1. Ý thức tham gia học tập"}, "score": {"integerValue": "19"}, "maxScore": {"integerValue": "20"}, "description": {"stringValue": "Đi học đầy đủ, đúng giờ, không vi phạm quy chế kiểm tra thi cử."}}}},
    {"mapValue": {"fields": {"title": {"stringValue": "2. Ý thức chấp hành nội quy, quy chế"}, "score": {"integerValue": "25"}, "maxScore": {"integerValue": "25"}, "description": {"stringValue": "Chấp hành tốt quy định của Nhà nước, nhà trường và nội quy sinh viên."}}}},
    {"mapValue": {"fields": {"title": {"stringValue": "3. Hoạt động phong trào Đoàn - Hội"}, "score": {"integerValue": "19"}, "maxScore": {"integerValue": "20"}, "description": {"stringValue": "Tham gia các buổi hội thảo, hoạt động tình nguyện, CLB học thuật."}}}},
    {"mapValue": {"fields": {"title": {"stringValue": "4. Ý thức công dân & Quan hệ cộng đồng"}, "score": {"integerValue": "24"}, "maxScore": {"integerValue": "25"}, "description": {"stringValue": "Tích cực tham gia hoạt động văn hóa, giúp đỡ cộng đồng, lối sống lành mạnh."}}}},
    {"mapValue": {"fields": {"title": {"stringValue": "5. Khen thưởng và thành tích đặc biệt"}, "score": {"integerValue": "5"}, "maxScore": {"integerValue": "10"}, "description": {"stringValue": "Đạt giải trong các cuộc thi học thuật hoặc phong trào nghiên cứu khoa học."}}}}
]

doc_payload = {
    "fields": {
        "studentId": {"stringValue": "2023040123"},
        "email": {"stringValue": EMAIL},
        "fullName": {"stringValue": "Nguyễn Minh Thắng"},
        "faculty": {"stringValue": "Công nghệ thông tin"},
        "class": {"stringValue": "CNTT2023"},
        "tuitionSemester": {"stringValue": "Học kỳ 1 (Năm học 2025 - 2026)"},
        "tuitionTotalAmount": {"stringValue": "12.650.000 VNĐ"},
        "tuitionPaidAmount": {"stringValue": "12.650.000 VNĐ"},
        "tuitionRemainingAmount": {"stringValue": "0 VNĐ"},
        "tuitionIsPaid": {"booleanValue": True},
        "tuitionCreditsCount": {"integerValue": "18"},
        "tuitionDueDate": {"stringValue": "15/11/2025"},
        "tuitionCourses": {"arrayValue": {"values": courses_list}},
        "trainingTotalScore": {"integerValue": "92"},
        "trainingMaxScore": {"integerValue": "100"},
        "trainingRank": {"stringValue": "XUẤT SẮC"},
        "trainingSemester": {"stringValue": "Học kỳ 1 (Năm học 2025 - 2026)"},
        "trainingCriteria": {"arrayValue": {"values": criteria_list}}
    }
}

print(f"2. Updating Firestore document: users/{uid}...")
user_url = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents/users/{uid}"
body = json.dumps(doc_payload).encode("utf-8")
req = urllib.request.Request(user_url, data=body, headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"}, method="PATCH")
with urllib.request.urlopen(req) as resp:
    res = json.loads(resp.read().decode("utf-8"))
    print("   Firestore seeded successfully!")
    print(f"   Name: {res['name']}")
    print(f"   Student: {res['fields']['fullName']['stringValue']} ({res['fields']['studentId']['stringValue']})")
    print(f"   Tuition: {res['fields']['tuitionTotalAmount']['stringValue']}")
    print(f"   Training Point: {res['fields']['trainingTotalScore']['integerValue']}/{res['fields']['trainingMaxScore']['integerValue']} ({res['fields']['trainingRank']['stringValue']})")
