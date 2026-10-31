package com.giathinh.uthconnect.presentation.utilities.campuses

data class CampusInfo(
    val name: String,
    val address: String,
    val hotline: String,
    val email: String,
    val note: String,
)

data class DepartmentInfo(
    val name: String,
    val hotline: String,
    val email: String,
    val location: String,
)

data class CampusesUiState(
    val campuses: List<CampusInfo> = listOf(
        CampusInfo(
            name = "Cơ sở 1 (Trụ sở chính)",
            address = "Số 2 Võ Oanh, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh",
            hotline = "028 3899 2862",
            email = "ut-hcmc@ut.edu.vn",
            note = "Khu hiệu bộ, các khoa, viện và giảng đường chính.",
        ),
        CampusInfo(
            name = "Cơ sở 2 (Cơ sở Thủ Đức)",
            address = "Số 10/80 Song Hành Xa Lộ Hà Nội, Phường Tân Phú, TP. Thủ Đức, TP. Hồ Chí Minh",
            hotline = "028 3730 7908",
            email = "cs2@ut.edu.vn",
            note = "Khu xưởng thực hành, phòng thí nghiệm chuyên ngành và ký túc xá.",
        ),
        CampusInfo(
            name = "Cơ sở 3 (Cơ sở Quận 12)",
            address = "Số 70 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh",
            hotline = "028 3715 0098",
            email = "cs3@ut.edu.vn",
            note = "Viện Đào tạo Quốc tế, Trung tâm nghiên cứu & ứng dụng Logistics.",
        ),
    ),
    val departments: List<DepartmentInfo> = listOf(
        DepartmentInfo("Phòng Đào tạo", "028 3899 2862", "daotao@ut.edu.vn", "Phòng A.102 - CS1"),
        DepartmentInfo("Phòng Công tác Sinh viên (CTSV)", "028 3512 0782", "ctsv@ut.edu.vn", "Phòng A.105 - CS1"),
        DepartmentInfo("Văn phòng Đoàn - Hội Sinh viên", "028 3512 5599", "doanthanhnien@ut.edu.vn", "Nhà B - CS1"),
        DepartmentInfo("Trung tâm Thông tin - Thư viện", "028 3899 1373", "thuvien@ut.edu.vn", "Tòa nhà Thư viện - CS1"),
    ),
)
