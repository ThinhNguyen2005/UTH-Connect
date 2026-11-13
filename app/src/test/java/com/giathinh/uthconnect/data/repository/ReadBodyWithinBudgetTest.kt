package com.giathinh.uthconnect.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.io.StringReader

/**
 * Trần TỔNG thời gian đọc response OSRM: server nhỏ giọt (mỗi lần read một ít,
 * lần nào cũng dưới timeout socket) vẫn bị chặn khi cộng dồn vượt ngân sách.
 */
class ReadBodyWithinBudgetTest {

    @Test
    fun `doc het body trong ngan sach`() {
        val body = readBodyWithinBudget(StringReader("""{"routes":[{"distance":1234.5}]}"""), 8_000L) { 0L }

        assertEquals("""{"routes":[{"distance":1234.5}]}""", body)
    }

    @Test
    fun `body rong tra chuoi rong`() {
        assertEquals("", readBodyWithinBudget(StringReader(""), 8_000L) { 0L })
    }

    @Test
    fun `body nhieu chunk van doc du`() {
        val long = "a".repeat(10_000)

        val body = readBodyWithinBudget(StringReader(long), 8_000L) { 0L }

        assertEquals(long, body)
    }

    @Test
    fun `server nho giot qua tong ngan sach thi nem IOException`() {
        var now = 0L
        val slow = object : StringReader("a".repeat(10_000)) {
            override fun read(cbuf: CharArray): Int {
                now += 3_000L
                return super.read(cbuf)
            }
        }

        assertThrows(IOException::class.java) {
            readBodyWithinBudget(slow, 8_000L) { now }
        }
    }

    @Test
    fun `moi lan read duoi ngan sach thi khong bi chan`() {
        var now = 0L
        val steady = object : StringReader("a".repeat(10_000)) {
            override fun read(cbuf: CharArray): Int {
                now += 1_000L
                return super.read(cbuf)
            }
        }

        val body = readBodyWithinBudget(steady, 8_000L) { now }

        assertEquals(10_000, body.length)
    }
}
