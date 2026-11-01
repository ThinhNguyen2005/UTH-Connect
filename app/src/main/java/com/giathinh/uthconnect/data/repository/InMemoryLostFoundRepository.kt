package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryLostFoundRepository @Inject constructor() : LostFoundRepository {

    private val mutex = Mutex()
    private val listings = mutableListOf(
        // 4 tin LOST (Thất lạc)
        LostFoundListing(
            id = "LF-001",
            title = "Rơi thẻ sinh viên UTH tại phòng B204",
            description = "Mình để quên thẻ sinh viên mang tên Nguyễn Văn An, MSSV 2251050012 ở bàn dãy 3 phòng B204 cơ sở 1 Võ Oanh sau giờ học Kỹ thuật phần mềm.",
            kind = ListingKind.LOST,
            category = ItemCategory.STUDENT_CARD,
            location = "Phòng B204, Cơ sở 1 (Võ Oanh)",
            date = "31/10/2026",
            contactName = "Nguyễn Văn An",
            contactPhone = "0901234567",
        ),
        LostFoundListing(
            id = "LF-002",
            title = "Quên sạc laptop MacBook Type-C",
            description = "Sạc màu trắng 67W Apple kèm dây cáp C to C, để quên ở khu tự học tầng 2 thư viện CS1 khoảng 11h trưa nay. Ai thấy cho mình xin lại với ạ.",
            kind = ListingKind.LOST,
            category = ItemCategory.ELECTRONICS,
            location = "Khu tự học Tầng 2 Thư viện, Cơ sở 1",
            date = "31/10/2026",
            contactName = "Trần Minh Thư",
            contactPhone = "0912345678",
        ),
        LostFoundListing(
            id = "LF-003",
            title = "Rơi chùm chìa khóa xe Honda Smartkey",
            description = "Chùm chìa khóa xe máy có gắn móc khóa gấu bông màu nâu và thẻ chung cư, rơi dọc đoạn đường từ nhà xe sinh viên ra cổng số 2 CS2 Thủ Đức.",
            kind = ListingKind.LOST,
            category = ItemCategory.PERSONAL_ITEMS,
            location = "Bãi giữ xe Cơ sở 2 (Thủ Đức)",
            date = "30/10/2026",
            contactName = "Lê Hoàng Long",
            contactPhone = "0987654321",
        ),
        LostFoundListing(
            id = "LF-004",
            title = "Thất lạc ví tiền da màu đen tại Căn tin CS3",
            description = "Bên trong có CCCD mang tên Phạm Quốc Hưng, một ít tiền mặt và thẻ ATM Vietcombank. Ai nhặt được liên hệ giúp mình, xin cảm ơn và hậu tạ.",
            kind = ListingKind.LOST,
            category = ItemCategory.WALLET_PAPERS,
            location = "Căn tin Cơ sở 3 (Tô Ký, Q.12)",
            date = "29/10/2026",
            contactName = "Phạm Quốc Hưng",
            contactPhone = "0933445566",
        ),
        // 4 tin FOUND (Nhặt được)
        LostFoundListing(
            id = "LF-005",
            title = "Nhặt được thẻ xe tháng tại bãi xe CS1",
            description = "Nhặt được thẻ gửi xe máy sinh viên rơi tại bãi xe tầng trệt khu C Võ Oanh. Đã gửi lại bác bảo vệ chốt cổng chính quản lý.",
            kind = ListingKind.FOUND,
            category = ItemCategory.STUDENT_CARD,
            location = "Bãi xe sinh viên khu C, Cơ sở 1",
            date = "31/10/2026",
            contactName = "Bác Bảo vệ Cổng 1",
            contactPhone = "02838992862",
        ),
        LostFoundListing(
            id = "LF-006",
            title = "Nhặt được tai nghe AirPods Pro trong hộp màu xanh",
            description = "Tai nghe nhặt được tại dãy ghế chờ hành lang lầu 3 nhà A CS1 sau tiết học sáng. Ai là chủ nhân vui lòng liên hệ xác nhận tên bluetooth.",
            kind = ListingKind.FOUND,
            category = ItemCategory.ELECTRONICS,
            location = "Hành lang Lầu 3 Nhà A, Cơ sở 1",
            date = "31/10/2026",
            contactName = "Đỗ Quỳnh Mai",
            contactPhone = "0977889900",
        ),
        LostFoundListing(
            id = "LF-007",
            title = "Nhặt được máy tính Casio fx-580VN X",
            description = "Máy tính bỏ túi còn dán sticker UTH Connect ở mặt lưng, để quên ở bàn cuối phòng E102 cơ sở 3 sau ca thi giữa kỳ môn Toán cao cấp.",
            kind = ListingKind.FOUND,
            category = ItemCategory.PERSONAL_ITEMS,
            location = "Phòng E102, Cơ sở 3 (Tô Ký)",
            date = "30/10/2026",
            contactName = "Võ Thành Đạt",
            contactPhone = "0966554433",
        ),
        LostFoundListing(
            id = "LF-008",
            title = "Nhặt được bóp đựng bút và thẻ ngân hàng",
            description = "Bóp viết vải màu xám bên trong có 3 cây bút bi, 1 bút chì và 1 thẻ sinh viên tên Đặng Ngọc Ánh. Mình đang giữ tại phòng CTSV.",
            kind = ListingKind.FOUND,
            category = ItemCategory.WALLET_PAPERS,
            location = "Phòng Công tác sinh viên (A003), Cơ sở 1",
            date = "28/10/2026",
            contactName = "Ban cán sự lớp CNTT",
            contactPhone = "0944332211",
        ),
    )

    override suspend fun getListings(): Result<List<LostFoundListing>> = mutex.withLock {
        Result.success(listings.toList())
    }

    override suspend fun reportItem(item: LostFoundListing): Result<LostFoundListing> = mutex.withLock {
        val newItem = item.copy(
            id = if (item.id.isBlank()) "LF-${UUID.randomUUID().toString().take(6).uppercase()}" else item.id
        )
        listings.add(0, newItem)
        Result.success(newItem)
    }
}
