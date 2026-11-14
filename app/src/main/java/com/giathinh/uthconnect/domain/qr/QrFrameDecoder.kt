package com.giathinh.uthconnect.domain.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer

/**
 * Giải mã QR trực tiếp từ khung hình `YUV_420_888` mà CameraX đẩy lên, không qua
 * trung gian JPEG/Bitmap. Chỉ mặt phẳng Y (luminance) được dùng nên lớp này không
 * tham chiếu lớp nào của Android và chạy được trong unit test JVM.
 *
 * Đây là lý do máy quét điểm danh không còn phải "chụp một tấm ảnh": mỗi frame
 * đã là một ảnh cường độ sáng sẵn cho [QRCodeReader].
 *
 * Đặt ở `domain/qr` vì `docs/CODING_STANDARDS.md` mục 3.2 cấm file thứ sáu trong một
 * module presentation: đây là thuần logic giải mã, không phải giao diện.
 */
object QrFrameDecoder {

    private const val CHANNEL_MASK = 0xFF
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8

    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)

    /**
     * Bộ đệm luminance của CameraX thường có `rowStride > width` vì mỗi hàng được chèn
     * padding để thẳng hàng bộ nhớ. Nếu đưa nguyên mảng này cho bộ giải mã thì mọi dòng
     * bị lệch một nửa mã, vì vậy phải nén lại thành mảng khít `width * height`.
     *
     * Hàm dùng `get` tuyệt đối nên không làm thay đổi `position`/`limit` của
     * [plane] — CameraX vẫn còn quyền sở hữu buffer đó cho tới khi `ImageProxy.close()`.
     *
     * @return mảng luminance khít, hoặc mảng rỗng khi [plane] thiếu dữ liệu cho khung đã báo.
     */
    fun compactLuminance(plane: ByteBuffer, width: Int, height: Int, rowStride: Int): ByteArray {
        require(width > 0 && height > 0) { "Frame must have positive size, got ${width}x$height" }
        require(rowStride >= width) { "rowStride $rowStride cannot be smaller than width $width" }
        if (plane.limit() < (height - 1) * rowStride + width) return ByteArray(0)
        val luminance = ByteArray(width * height)
        if (rowStride == width) {
            plane.get(0, luminance, 0, luminance.size)
            return luminance
        }
        for (row in 0 until height) {
            val rowStart = row * rowStride
            val targetStart = row * width
            for (column in 0 until width) luminance[targetStart + column] = plane.get(rowStart + column)
        }
        return luminance
    }

    /**
     * @param luminance mảng khít trả về từ [compactLuminance].
     * @return chuỗi bên trong QR, hoặc `null` khi frame chưa có mã (rất bình thường,
     *   xảy ra với gần như mọi frame khi chưa chĩa đúng vé).
     *
     * `zxing:core` không có `PlanarYUVLuminanceSource` (lớp đó thuộc `android-core`),
     * nên luminance phải nhân bản thành ảnh Xám rồi đưa cho [RGBLuminanceSource]. Ở
     * 640×480 điều này tốn một mảng int ~1,2 MB mỗi frame — đổi lại không phải thêm
     * dependency Android và hàm vẫn test được trên JVM.
     */
    fun decode(luminance: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || luminance.size < width * height) return null
        val pixels = IntArray(width * height)
        for (index in pixels.indices) {
            val gray = luminance[index].toInt() and CHANNEL_MASK
            pixels[index] = (CHANNEL_MASK shl ALPHA_SHIFT) or (gray shl RED_SHIFT) or (gray shl GREEN_SHIFT) or gray
        }
        val source = RGBLuminanceSource(width, height, pixels)
        return runCatching { QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text }
            .getOrNull()
    }
}
