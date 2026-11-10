package com.giathinh.uthconnect.data.repository

import android.util.Log
import com.giathinh.uthconnect.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Log tag for the Google ID-token exchange; visible with `adb logcat -s SupabaseGoogleExchange`. */
internal const val GOOGLE_EXCHANGE_LOG_TAG = "SupabaseGoogleExchange"

/**
 * Native Google sign-in (ID token flow) through the official supabase-kt Auth module.
 *
 * Only the token exchange lives here. The session it produces is handed to
 * [SupabaseRestClient], which every other Supabase repository already uses, so the app keeps
 * exactly one active Bearer token instead of two competing session stores.
 *
 * The SDK's session manager is deliberately in-memory. The shared RestClient persists only an
 * Android Keystore-encrypted refresh token when the user opts in to remember-me.
 */
@Singleton
class SupabaseGoogleExchange @Inject constructor(
    private val client: SupabaseRestClient,
) {
    private val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) { sessionManager = MemorySessionManager() }
        }
    }

    /**
     * @param idToken the raw Google ID token from Credential Manager.
     * @param rawNonce the unhashed nonce that produced it; Supabase hashes it itself and
     * compares the result with the token's `nonce` claim.
     */
    suspend fun exchange(idToken: String, rawNonce: String) {
        require(BuildConfig.USE_SUPABASE) { "Google sign-in requires the Supabase backend" }
        val session = try {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                nonce = rawNonce
            }
            supabase.auth.currentSessionOrNull() ?: error("Google sign-in returned no Supabase session")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Exception text and stack traces may contain provider responses or sensitive claims.
            Log.w(GOOGLE_EXCHANGE_LOG_TAG, "Google ID-token exchange rejected")
            throw error
        }
        client.attachSession(
            userId = session.user?.id ?: error("Supabase session has no user id"),
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresInSeconds = session.expiresIn,
        )
    }
}
