package com.giathinh.uthconnect.domain.model

import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max

/**
 * Nội dung đầy đủ của một thông báo (màn hình Chi tiết).
 *
 * Không phải bài nào cũng giống bài nào, nên phần thân bài **không** phải một khuôn cố định:
 * nó là danh sách [AnnouncementBlock] và UI chỉ vẽ những khối có mặt. Bài chỉ có tiêu đề thì
 * `blocks` rỗng; bài có mốc thời gian thì có [AnnouncementBlock.Milestones]; bài có tài liệu
 * đính kèm thì `attachments` khác rỗng. Thêm một dạng nội dung mới = thêm một nhánh `when`,
 * không phải nhân bản màn hình.
 *
 * `title`, `lead`, nội dung khối… là **dữ liệu** (nguồn thật là Firestore), không phải chuỗi
 * giao diện — nên không nằm trong `strings.xml` (`docs/CODING_STANDARDS.md` mục 8).
 */
data class AnnouncementDetail(
    val id: String,
    val category: AnnouncementCategory,
    val title: String,
    val publisher: String,
    val publishedAt: LocalDateTime,
    /** Nơi đăng, hiện trong chip trên ảnh bìa; `null` thì không có chip. */
    val source: String? = null,
    /** Lần cập nhật gần nhất; `null` thì UI ẩn hẳn dòng "Cập nhật …". */
    val updatedAt: LocalDateTime? = null,
    /** Đoạn mở đầu in đậm nhẹ ngay dưới ảnh bìa; `null` khi thông báo chỉ có tiêu đề. */
    val lead: String? = null,
    /** Thân bài, theo đúng thứ tự cần hiển thị. */
    val blocks: List<AnnouncementBlock> = emptyList(),
    /** Tài liệu đính kèm; rỗng thì ẩn cả mục "Tài liệu đính kèm". */
    val attachments: List<AnnouncementAttachment> = emptyList(),
) {

    /**
     * Thời lượng đọc ước tính, luôn ≥ 1 phút.
     *
     * Tính từ chính nội dung thay vì lấy con số cố định của bản thiết kế: bài ngắn
     * không được ghi "4 phút đọc". Tính **một lần** lúc dựng object — đây là `val`,
     * không phải `get()`.
     */
    val readMinutes: Int = max(1, ceil(wordCount().toDouble() / WordsPerMinute).toInt())

    private fun wordCount(): Int {
        val text = buildString {
            append(lead.orEmpty())
            blocks.forEach { block ->
                when (block) {
                    is AnnouncementBlock.Heading -> append(' ').append(block.text)
                    is AnnouncementBlock.Paragraph -> append(' ').append(block.text)
                    is AnnouncementBlock.Bullets -> block.items.forEach { append(' ').append(it) }
                    is AnnouncementBlock.Milestones -> block.items.forEach { milestone ->
                        append(' ').append(milestone.title).append(' ').append(milestone.body)
                    }
                    is AnnouncementBlock.Contact -> append(' ').append(block.label).append(' ').append(block.body)
                }
            }
        }
        return text.split(WordSeparator).count { it.isNotBlank() }
    }
}

/**
 * Một khối nội dung của thân bài. `sealed` để `when` ở UI bắt buộc xét đủ nhánh khi có
 * khối mới (`docs/CODING_STANDARDS.md` mục 4).
 */
sealed interface AnnouncementBlock {

    /** Tiêu đề mục, ví dụ "Khung thời gian cụ thể". */
    data class Heading(val text: String) : AnnouncementBlock

    /** Một đoạn văn bản thường. */
    data class Paragraph(val text: String) : AnnouncementBlock

    /** Danh sách gạch đầu dòng cho các bước/điều kiện ngắn. */
    data class Bullets(val items: List<String>) : AnnouncementBlock

    /** Các mốc thời gian dạng thẻ — mỗi mốc là một [Milestone]. */
    data class Milestones(val items: List<Milestone>) : AnnouncementBlock

    /** Một kênh liên hệ/hỗ trợ, có thể kèm email bấm được. */
    data class Contact(
        val label: String,
        val body: String,
        val email: String? = null,
    ) : AnnouncementBlock
}

/**
 * Một mốc thời gian trong thông báo ("Đợt 1", "Đợt 2"…).
 *
 * [badge] là nhãn ngắn do dữ liệu quyết định; [emphasized] đánh dấu mốc đang diễn ra để UI
 * dùng tone nhấn — UI không tự suy đoán từ ngày.
 */
data class Milestone(
    val title: String,
    val body: String,
    val badge: String? = null,
    val emphasized: Boolean = false,
)

/**
 * Tệp đính kèm của thông báo.
 *
 * [sizeLabel] là **dữ liệu** ("2.4 MB") chứ không phải số byte: chuỗi này do nguồn nội dung
 * chuẩn hoá, UI không tự định dạng lại theo locale.
 */
data class AnnouncementAttachment(
    val fileName: String,
    val sizeLabel: String,
    val kind: AttachmentKind = AttachmentKind.OTHER,
)

/** Loại tệp — quyết định icon và nhãn phụ, không quyết định cách tải. */
enum class AttachmentKind {
    PDF,
    DOCX,
    IMAGE,
    OTHER,
}

/** Tốc độ đọc tham chiếu của người lớn (từ/phút) khi ước lượng thời lượng đọc. */
private const val WordsPerMinute = 200

private val WordSeparator = Regex("\\s+")
