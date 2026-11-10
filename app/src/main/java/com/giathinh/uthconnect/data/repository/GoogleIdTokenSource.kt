package com.giathinh.uthconnect.data.repository

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of the Google account picker, before Supabase has seen anything. */
sealed interface GoogleIdTokenResult {
    /** A Google identity plus the nonce needed to redeem it at Supabase. */
    data class Granted(val idToken: String, val rawNonce: String) : GoogleIdTokenResult

    /** The user backed out of the picker; not an error and must not show one. */
    data object Cancelled : GoogleIdTokenResult

    /** No Google app/account on this device, or the build has no Web client ID. */
    data object Unavailable : GoogleIdTokenResult
}

/**
 * Google Credential Manager is the replacement for the deprecated `GoogleSignInClient`; it
 * returns an ID token minted for [webClientId] without any auth code exchange in the app.
 *
 * The call needs an [Activity] context because the account picker is system UI, so it belongs
 * to the route layer, exactly like the biometric prompt. [BuildConfig.GOOGLE_WEB_CLIENT_ID]
 * is the **Web** OAuth client ID, never the Android one — Supabase validates the token's `aud`
 * against the client ID registered in its own Google provider settings.
 *
 * For explicit button clicks ("Tiếp tục với Google"), Google recommends [GetSignInWithGoogleOption]
 * which launches the explicit account chooser dialog listing all device accounts. If that is
 * unavailable or raises [NoCredentialException], it falls back to [GetGoogleIdOption].
 */
@Singleton
class GoogleIdTokenSource @Inject constructor() {
    suspend fun request(activity: Activity, webClientId: String): GoogleIdTokenResult {
        if (webClientId.isBlank()) return GoogleIdTokenResult.Unavailable
        val credentialManager = CredentialManager.create(activity)
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)

        // 1. Primary: Explicit button flow via GetSignInWithGoogleOption
        val explicitRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(webClientId)
                    .setNonce(hashedNonce)
                    .build(),
            )
            .build()

        try {
            val response = credentialManager.getCredential(activity, explicitRequest)
            return parseCredentialResponse(response, rawNonce)
        } catch (cancelled: GetCredentialCancellationException) {
            return GoogleIdTokenResult.Cancelled
        } catch (noCred: NoCredentialException) {
            Log.w(
                TAG,
                "Explicit sign-in threw NoCredentialException; trying GetGoogleIdOption fallback: ${noCred.message}",
            )
        } catch (failure: GetCredentialException) {
            Log.w(
                TAG,
                "Explicit sign-in failed [${failure.javaClass.simpleName}]: ${failure.message}; trying GetGoogleIdOption fallback",
            )
        }

        // 2. Fallback: Bottom-sheet flow via GetGoogleIdOption
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setNonce(hashedNonce)
                    .build(),
            )
            .build()

        return try {
            val response = credentialManager.getCredential(activity, fallbackRequest)
            parseCredentialResponse(response, rawNonce)
        } catch (cancelled: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (failure: GetCredentialException) {
            Log.w(
                TAG,
                "Credential Manager fallback failed [${failure.javaClass.simpleName}]: ${failure.message}",
                failure,
            )
            GoogleIdTokenResult.Unavailable
        }
    }

    private fun parseCredentialResponse(
        response: GetCredentialResponse,
        rawNonce: String,
    ): GoogleIdTokenResult = try {
        val idToken = GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        if (idToken.isNullOrBlank()) {
            GoogleIdTokenResult.Unavailable
        } else {
            GoogleIdTokenResult.Granted(idToken, rawNonce)
        }
    } catch (failure: GoogleIdTokenParsingException) {
        Log.w(
            TAG,
            "Google ID token parsing failed [${failure.javaClass.simpleName}]: ${failure.message}",
            failure,
        )
        GoogleIdTokenResult.Unavailable
    }

    companion object {
        private const val TAG = "GoogleIdTokenSource"
    }
}
