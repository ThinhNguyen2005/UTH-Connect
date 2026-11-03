package com.giathinh.uthconnect.presentation.tickets

import com.giathinh.uthconnect.domain.model.TicketQrPayload
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketsQrEncodingTest {
    @Test
    fun `QR tạo ra đọc lại đúng toàn bộ nội dung vé`() {
        val payload = TicketQrPayload.create("ticket-sports", 123456789L)
        val matrix = encodeTicketQr(payload)
        val scale = 8
        val width = matrix.width * scale
        val height = matrix.height * scale
        val pixels = IntArray(width * height) { index ->
            val x = index % width / scale
            val y = index / width / scale
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))

        val decoded = QRCodeReader().decode(bitmap).text

        assertEquals(payload, decoded)
        assertEquals("ticket-sports", TicketQrPayload.ticketIdOrNull(decoded))
    }

    @Test
    fun `QR sai phiên bản hoặc thiếu nonce bị từ chối`() {
        assertNull(TicketQrPayload.ticketIdOrNull("UTHCONNECT:TICKET:2:ticket-sports:1"))
        assertNull(TicketQrPayload.ticketIdOrNull("UTHCONNECT:TICKET:1:ticket-sports"))
    }
}
