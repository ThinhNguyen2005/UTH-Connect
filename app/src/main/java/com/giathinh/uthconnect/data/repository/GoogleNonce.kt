package com.giathinh.uthconnect.data.repository

import java.security.MessageDigest

/**
 * Supabase compares the `nonce` claim of a Google ID token with `SHA-256(rawNonce)` rendered
 * as lowercase hex. Google therefore receives the hashed value, while
 * [SupabaseGoogleExchange] sends the raw value to Supabase. Both halves are required —
 * sending the hashed value to Supabase makes the check fail with an invalid-nonce error.
 */
internal fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
