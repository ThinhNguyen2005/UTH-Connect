package com.giathinh.uthconnect.domain.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Unit test cho phần giải mã thuần JVM của máy quét: không CameraX, không Compose,
 * không thiết bị — chỉ cần một khung hình YUV tổng hợp đúng luật `rowStride`.
 *
 * Token có dạng `eventId.uid.hmac` do RPC `issue_ticket_qr` sinh ra (migration
 * 20261104000000_stable_ticket_qr.sql).
 */
class QrFrameDecoderTest {

    private val token = "0f1e2d3c-4b5a-4998-8f7e-6d5c4b3a2918." +
        "0bd0ca80-4e31-4b40-8e57-44e1ac3d725d." +
        "a".repeat(64)

    private val white: Byte = 0xFF.toByte()
    private val black: Byte = 0x00

    /** Vẽ QR thật ra mảng luminance khít, có vùng trắng lặng bao quanh theo chuẩn QR. */
    private fun renderTicketQr(content: String, quietZone: Int): Pair<ByteArray, Int> {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            300,
            300,
            mapOf(EncodeHintType.MARGIN to quietZone, EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
        val side = matrix.width
        val luminance = ByteArray(side * side) { white }
        for (y in 0 until matrix.height) {
            val rowStart = y * side
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) luminance[rowStart + x] = black
            }
        }
        return luminance to side
    }

    @Test
    fun `decode doc duoc token tu frame YUV tong hop`() {
        val (luminance, side) = renderTicketQr(token, quietZone = 16)

        assertEquals(token, QrFrameDecoder.decode(luminance, side, side))
    }

    @Test
    fun `compactLuminance loai bo padding cuoi moi hang`() {
        val (luminance, side) = renderTicketQr(token, quietZone = 8)
        val stride = side + 8
        val padded = ByteBuffer.allocate(stride * side)
        for (row in 0 until side) {
            for (column in 0 until side) {
                padded.put(row * stride + column, luminance[row * side + column])
            }
        }

        val compact = QrFrameDecoder.compactLuminance(padded, side, side, stride)

        assertArrayEquals(luminance, compact)
        assertEquals(token, QrFrameDecoder.decode(compact, side, side))
    }

    @Test
    fun `compactLuminance khong doi position cua buffer nguoi khac van can`() {
        val (luminance, side) = renderTicketQr(token, quietZone = 4)
        val stride = side + 4
        val plane = ByteBuffer.allocate(stride * side)
        for (row in 0 until side) {
            for (column in 0 until side) plane.put(row * stride + column, luminance[row * side + column])
        }
        val positionBefore = plane.position()

        QrFrameDecoder.compactLuminance(plane, side, side, stride)

        assertEquals(positionBefore, plane.position())
        assertEquals(plane.limit(), stride * side)
    }

    @Test
    fun `decode tra null khi frame khong co ma`() {
        val side = 120
        val blank = ByteArray(side * side) { white }

        assertNull(QrFrameDecoder.decode(blank, side, side))
    }

    @Test
    fun `decode tra null khi du lieu thieu so duong anh da bao`() {
        val (luminance, side) = renderTicketQr(token, quietZone = 8)

        assertNull(QrFrameDecoder.decode(luminance, side + 1, side))
    }

    @Test
    fun `compactLuminance tra mang rong khi plane ngan hon khung da bao`() {
        val stride = 64
        val plane = ByteBuffer.allocate(300)

        assertArrayEquals(ByteArray(0), QrFrameDecoder.compactLuminance(plane, 32, 10, stride))
    }
}
