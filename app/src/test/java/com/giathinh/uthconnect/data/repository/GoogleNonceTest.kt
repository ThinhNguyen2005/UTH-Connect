package com.giathinh.uthconnect.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.UUID

/**
 * Supabase hashes the raw nonce itself and compares the digest with the ID token's `nonce`
 * claim, so the value handed to Google must be the lowercase hex SHA-256 of exactly what is
 * later sent to Supabase. A wrong encoding fails the sign-in with an opaque invalid-nonce error.
 */
class GoogleNonceTest {
    @Test fun `sha256Hex matches the published digest of abc`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc"),
        )
    }

    @Test fun `sha256Hex is lowercase hex of fixed length`() {
        val digest = sha256Hex("")
        assertEquals(64, digest.length)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", digest)
    }

    @Test fun `every request uses a fresh nonce`() {
        val first = sha256Hex(UUID.randomUUID().toString())
        val second = sha256Hex(UUID.randomUUID().toString())
        assertNotEquals(first, second)
    }
}
