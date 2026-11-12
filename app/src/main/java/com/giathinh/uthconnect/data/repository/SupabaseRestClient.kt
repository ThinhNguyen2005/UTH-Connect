package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal PostgREST/GoTrue transport. Only publishable keys are shipped in the APK. */
@Singleton
class SupabaseRestClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshMutex = Mutex()

    @Volatile private var sessionStore: SecureSessionStore? = null

    @Volatile private var rememberSession = false

    @Volatile private var credentials: Credentials? = null

    val userId: String get() = credentials?.userId ?: error("Supabase sign-in required")

    /** Called by the session repository, which owns the remember-me decision. */
    fun attachSecureStore(store: SecureSessionStore) {
        sessionStore = store
    }

    /** Restores a persisted session, clearing it only when the refresh token is rejected. */
    suspend fun restoreSavedSession(): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val saved = sessionStore?.load() ?: return@withLock false
            if (credentials != null) return@withLock true
            try {
                val updated = refreshSession(saved.userId, saved.refreshToken)
                // Persist rotation before making the new access token available.
                sessionStore?.save(updated.userId, updated.refreshToken)
                rememberSession = true
                credentials = updated
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: SupabaseHttpException) {
                if (error.status == 400 || error.status == 401) {
                    sessionStore?.clear()
                    rememberSession = false
                    false
                } else {
                    throw error
                }
            }
        }
    }

    /** Updates whether the current refresh token should be persisted across launches. */
    fun setRememberSession(remember: Boolean) {
        rememberSession = remember
        if (remember) {
            val current = credentials ?: error("No authenticated Supabase session")
            sessionStore?.save(current.userId, current.refreshToken)
        } else {
            sessionStore?.clear()
        }
    }

    /** Authenticates with Supabase and replaces any prior in-memory session. */
    suspend fun signIn(email: String, password: String) {
        // A failed login must not accidentally leave the previous user's tokens active.
        // Password confirmation is a separate flow and must preserve the current session on failure.
        credentials = null
        CrashIdentity.clear()
        rememberSession = false
        sessionStore?.clear()
        val data = send(
            "/auth/v1/token?grant_type=password",
            "POST",
            buildJsonObject {
                put("email", email)
                put("password", password)
            },
            bearer = null,
        ).jsonObject
        credentials = parseCredentials(data)
    }

    /** Validate the old password without replacing or rotating the active session. */
    suspend fun verifyCurrentPassword(email: String, password: String): Boolean {
        val currentUserId = userId
        val response = send(
            "/auth/v1/token?grant_type=password",
            "POST",
            buildJsonObject {
                put("email", email)
                put("password", password)
            },
            bearer = null,
        ).jsonObject
        return parseCredentials(response).userId == currentUserId
    }

    /** Creates an account and returns the authentication response. */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
        redirectTo: String = "",
    ): JsonObject {
        val encodedRedirect = urlEncode(redirectTo)
        val payload = buildJsonObject {
            put("email", email)
            put("password", password)
            put(
                "data",
                buildJsonObject {
                    put("display_name", displayName)
                    put("faculty", faculty)
                },
            )
        }
        val response = send(if (redirectTo.isBlank()) "/auth/v1/signup" else "/auth/v1/signup?redirect_to=$encodedRedirect", "POST", payload, bearer = null).jsonObject
        if (response.containsKey("access_token") && response.containsKey("user")) {
            credentials = parseCredentials(response)
        }
        return response
    }

    /** Requests a password recovery message for the supplied email. */
    suspend fun recoverPassword(email: String) {
        val payload = buildJsonObject { put("email", email) }
        send("/auth/v1/recover", "POST", payload, bearer = null)
    }

    /**
     * One-shot recovery: use the access token returned by GoTrue OTP verification
     * exclusively for updating that account's password. Never adopt or persist it.
     */
    suspend fun resetPasswordWithOtp(email: String, otp: String, password: String) {
        val verified = send(
            "/auth/v1/verify",
            "POST",
            buildJsonObject {
                put("type", "recovery")
                put("email", email)
                put("token", otp)
            },
            bearer = null,
        ).jsonObject
        val recoveryAccessToken = verified["access_token"]?.jsonPrimitive?.content
            ?: error("Supabase recovery verification did not return a session")
        send(
            "/auth/v1/user",
            "PUT",
            buildJsonObject { put("password", password) },
            bearer = recoveryAccessToken,
        )
    }

    /** Sends a signup verification email, never a password-recovery email. */
    suspend fun resendSignupConfirmation(email: String) {
        send(
            "/auth/v1/resend",
            "POST",
            buildJsonObject {
                put("type", "signup")
                put("email", email)
            },
            bearer = null,
        )
    }

    /** Clears the active credentials and any persisted refresh token. */
    suspend fun signOut() {
        val old = credentials
        credentials = null
        CrashIdentity.clear()
        rememberSession = false
        sessionStore?.clear()
        if (old != null) {
            try {
                send("/auth/v1/logout", "POST", bearer = old.accessToken)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A revoked local session cannot be restored, including when offline.
            }
        }
    }

    /**
     * Adopts a session that was established outside this client — today only the
     * supabase-kt native Google exchange. The tokens behave identically to a password
     * login, so [freshToken] can still refresh them through `grant_type=refresh_token`.
     *
     * The session repository subsequently applies the user's remember-me choice.
     */
    suspend fun attachSession(
        userId: String,
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
    ) {
        credentials = Credentials(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresInSeconds.coerceAtLeast(0L) * 1000L,
        )
        rememberSession = false
        sessionStore?.clear()
    }

    /** Reads a PostgREST table or view using the authenticated session. */
    suspend fun table(path: String): JsonElement = authorized("/rest/v1/$path", "GET")

    /** Updates matching rows through PostgREST. */
    suspend fun patchTable(path: String, body: JsonObject): JsonElement = authorized("/rest/v1/$path", "PATCH", body)

    /** Inserts a row through PostgREST and returns the server response. */
    suspend fun insertTable(path: String, body: JsonObject): JsonElement = authorized("/rest/v1/$path", "POST", body)

    /** Deletes matching rows through PostgREST. */
    suspend fun deleteTable(path: String): JsonElement = authorized("/rest/v1/$path", "DELETE")

    /** Uploads binary data to Supabase Storage using the authenticated session. */
    suspend fun uploadStorage(
        bucket: String,
        path: String,
        mimeType: String,
        data: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val accessToken = freshToken()
        val cleanPath = path.trimStart('/')
        val endpoint = "/storage/v1/object/$bucket/$cleanPath"
        val connection = (URL(BuildConfig.SUPABASE_URL + endpoint).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", mimeType)
            connection.doOutput = true
            connection.outputStream.use { it.write(data) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val error = json.parseToJsonElement(response).jsonObject
                    (error["error_description"] ?: error["message"] ?: error["error"])?.jsonPrimitive?.content
                }.getOrNull() ?: "Supabase Storage HTTP $status"
                throw SupabaseHttpException(status, message)
            }
            cleanPath
        } finally {
            connection.disconnect()
        }
    }

    /** Deletes an object from Supabase Storage. */
    suspend fun deleteStorage(bucket: String, path: String): Unit = withContext(Dispatchers.IO) {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val accessToken = freshToken()
        val cleanPath = path.trimStart('/')
        val endpoint = "/storage/v1/object/$bucket/$cleanPath"
        val connection = (URL(BuildConfig.SUPABASE_URL + endpoint).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            val status = connection.responseCode
            if (status !in 200..299 && status != 404) {
                val response = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = runCatching {
                    val error = json.parseToJsonElement(response).jsonObject
                    (error["error_description"] ?: error["message"] ?: error["error"])?.jsonPrimitive?.content
                }.getOrNull() ?: "Supabase Storage delete HTTP $status"
                throw SupabaseHttpException(status, message)
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Invokes an authenticated Postgres RPC function. */
    suspend fun rpc(function: String, params: JsonObject): JsonElement = authorized("/rest/v1/rpc/$function", "POST", params)

    /** Updates the password for the currently authenticated user. */
    suspend fun updatePassword(password: String): JsonElement {
        val token = freshToken()
        return send("/auth/v1/user", "PUT", buildJsonObject { put("password", password) }, bearer = token)
    }

    /** Sends an authenticated request after obtaining a valid access token. */
    private suspend fun authorized(path: String, method: String, body: JsonObject? = null): JsonElement {
        val accessToken = freshToken()
        return send(path, method, body, accessToken)
    }

    /** Returns the current access token, refreshing it when it is close to expiration. */
    private suspend fun freshToken(): String {
        val session = credentials ?: error("Supabase sign-in required")
        if (System.currentTimeMillis() < session.expiresAtMillis - 30_000L) return session.accessToken
        return refreshMutex.withLock {
            val latest = credentials ?: error("Supabase session expired")
            if (System.currentTimeMillis() < latest.expiresAtMillis - 30_000L) return@withLock latest.accessToken
            try {
                val updated = refreshSession(latest.userId, latest.refreshToken)
                if (rememberSession) sessionStore?.save(updated.userId, updated.refreshToken)
                credentials = updated
                updated.accessToken
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: SupabaseHttpException) {
                if (error.status == 400 || error.status == 401) {
                    credentials = null
                    CrashIdentity.clear()
                    rememberSession = false
                    sessionStore?.clear()
                }
                throw error
            }
        }
    }

    /** Rotates a refresh token and rejects a response for a different authenticated user. */
    private suspend fun refreshSession(expectedUserId: String, refreshToken: String): Credentials {
        val updated = parseCredentials(
            send(
                "/auth/v1/token?grant_type=refresh_token",
                "POST",
                buildJsonObject { put("refresh_token", refreshToken) },
                bearer = null,
            ).jsonObject,
        )
        check(updated.userId == expectedUserId) { "Session user changed" }
        return updated
    }

    /** Parses the session fields returned by the Supabase Auth API. */
    private fun parseCredentials(payload: JsonObject): Credentials {
        val id = payload["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: error("Auth response missing user ID")
        val access = payload["access_token"]?.jsonPrimitive?.content
            ?: error("Auth response missing access token")
        val refresh = payload["refresh_token"]?.jsonPrimitive?.content
            ?: error("Auth response missing refresh token")
        val expiresIn = payload["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        return Credentials(id, access, refresh, System.currentTimeMillis() + expiresIn * 1000L)
    }

    /** Performs one HTTP request and converts non-success responses to [SupabaseHttpException]. */
    private suspend fun send(
        path: String,
        method: String,
        body: JsonObject? = null,
        bearer: String?,
    ): JsonElement = withContext(Dispatchers.IO) {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val connection = (URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection)
        try {
            if (method == "PATCH") {
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                connection.setRequestProperty("Prefer", "return=representation")
            } else {
                connection.requestMethod = method
                if (method == "POST" && path.startsWith("/rest/v1/")) {
                    connection.setRequestProperty("Prefer", "return=representation")
                }
            }
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val error = json.parseToJsonElement(response).jsonObject
                    (error["error_description"] ?: error["message"] ?: error["error"])?.jsonPrimitive?.content
                }.getOrNull() ?: "Supabase HTTP $status"
                throw SupabaseHttpException(status, message)
            }
            if (response.isBlank()) JsonNull else json.parseToJsonElement(response)
        } finally {
            connection.disconnect()
        }
    }

    private data class Credentials(
        val userId: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAtMillis: Long,
    )
}

class SupabaseHttpException(val status: Int, message: String) : IllegalStateException(message)
