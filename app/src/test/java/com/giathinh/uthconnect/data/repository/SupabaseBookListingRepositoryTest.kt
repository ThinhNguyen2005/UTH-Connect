package com.giathinh.uthconnect.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class SupabaseBookListingRepositoryTest {

    @Test
    fun `validateImageBytes accepts valid JPEG magic bytes`() {
        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)

        val (bytes, mime) = SupabaseBookListingRepository.validateImageBytes(jpegBytes)

        assertEquals("image/jpeg", mime)
        assertEquals(12, bytes.size)
    }

    @Test
    fun `validateImageBytes accepts valid PNG magic bytes`() {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(), 0, 0, 0, 0)

        val (bytes, mime) = SupabaseBookListingRepository.validateImageBytes(pngBytes)

        assertEquals("image/png", mime)
        assertEquals(12, bytes.size)
    }

    @Test
    fun `validateImageBytes accepts valid WebP magic bytes`() {
        val webpBytes = "RIFF1234WEBP".toByteArray(Charsets.US_ASCII)

        val (bytes, mime) = SupabaseBookListingRepository.validateImageBytes(webpBytes)

        assertEquals("image/webp", mime)
        assertEquals(12, bytes.size)
    }

    @Test
    fun `validateImageBytes rejects plain text and non-image files`() {
        val textBytes = "This is not an image file at all!".toByteArray(Charsets.UTF_8)

        assertThrows(IllegalStateException::class.java) {
            SupabaseBookListingRepository.validateImageBytes(textBytes)
        }
    }

    @Test
    fun `validateImageBytes rejects files exceeding 5MB`() {
        val largeBytes = ByteArray(5 * 1024 * 1024 + 1)
        largeBytes[0] = 0xFF.toByte()
        largeBytes[1] = 0xD8.toByte()
        largeBytes[2] = 0xFF.toByte()

        assertThrows(IllegalArgumentException::class.java) {
            SupabaseBookListingRepository.validateImageBytes(largeBytes)
        }
    }

    @Test
    fun `readAndValidateStream accepts valid stream and parses mime type`() {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(), 0, 0, 0, 0)
        val stream = ByteArrayInputStream(pngBytes)

        val (bytes, mime) = SupabaseBookListingRepository.readAndValidateStream(stream)

        assertEquals("image/png", mime)
        assertEquals(12, bytes.size)
    }

    @Test
    fun `readAndValidateStream stops reading and throws early when stream exceeds 5MB`() {
        var totalBytesSupplied = 0L
        val oversizedStream = object : InputStream() {
            override fun read(): Int {
                totalBytesSupplied++
                return 0
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val toRead = minOf(len, 8192)
                for (i in 0 until toRead) {
                    b[off + i] = 0
                }
                totalBytesSupplied += toRead
                return toRead
            }
        }

        val exception = assertThrows(IllegalArgumentException::class.java) {
            SupabaseBookListingRepository.readAndValidateStream(oversizedStream)
        }

        assertTrue(exception.message?.contains("5 MB") == true)
        // Should have aborted shortly after exceeding 5 MB (e.g. within 1 buffer chunk of 8 KB)
        assertTrue(totalBytesSupplied in (5L * 1024 * 1024)..(5L * 1024 * 1024 + 16384))
    }
}
