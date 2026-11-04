package com.giathinh.uthconnect.data.repository

import android.util.Log
import com.giathinh.uthconnect.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Log tag for the Google ID-token exchange; visible with `adb logcat -s SupabaseGoogleExchange`. */
internal const val GoogleExchangeLogTag = "SupabaseGoogleExchange"

/**
 * Native Google sign-in (ID token flow) through the official supabase-kt Auth module.
 *
 * Only the token exchange lives here. The session it produces is handed to
 * [SupabaseRestClient], which every other Supabase repository already uses, so the app keeps
 * exactly one active Bearer token instead of two competing session stores.
 *
 * The SDK's session manager is deliberately in-memory. This project treats a Supabase session
 * as valid for the current process only (see PreferencesSessionRepository, which refuses
 * remember-me under Supabase), and the SDK's default storage would persist tokens in plain
 * SharedPreferences.
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
            // The UI only shows a generic "not authenticated" message, so GoTrue's actual reason
            // (bad audience, provider disabled, nonce mismatch, ...) is otherwise unrecoverable.
            Log.w(GoogleExchangeLogTag, "Google ID-token exchange rejected", error)
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
