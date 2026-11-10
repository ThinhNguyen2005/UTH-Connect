package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
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

@Singleton
class SupabaseRestClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshMutex = Mutex()

    @Volatile private var sessionStore: SecureSessionStore? = null

    @Volatile private var rememberSession = false

    @Volatile private var credentials: Credentials? = null

    val userId: String get() = credentials?.userId ?: error("Supabase sign-in required")
    val userIdOrNull: String? get() = credentials?.userId

    /** Called by the session repository, which owns the remember-me decision. */
    fun attachSecureStore(store: SecureSessionStore) {
        sessionStore = store
    }

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

    fun setRememberSession(remember: Boolean) {
        rememberSession = remember
        if (remember) {
            val current = credentials ?: error("No authenticated Supabase session")
            sessionStore?.save(current.userId, current.refreshToken)
        } else {
            sessionStore?.clear()
        }
    }

    suspend fun signIn(email: String, password: String) {
        // A failed login must not accidentally leave the previous user's tokens active.
        // Password confirmation is a separate flow and must preserve the current session on failure.
        credentials = null
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

    suspend fun signUp(
        email: String,
        password: String,
        metadata: JsonObject = buildJsonObject {},
        redirectTo: String = "",
    ): JsonElement {
        val encodedRedirect = urlEncode(redirectTo)
        val payload = buildJsonObject {
            put("email", email)
            put("password", password)
            put("data", metadata)
        }
        val response = send(
            if (redirectTo.isBlank()) "/auth/v1/signup" else "/auth/v1/signup?redirect_to=$encodedRedirect",
            "POST",
            payload,
            bearer = null,
        )
        runCatching {
            val creds = parseCredentials(response.jsonObject)
            credentials = creds
        }
        return response
    }

    suspend fun recoverPassword(email: String, redirectTo: String = "uthconnect://auth/reset-password") {
        val encodedRedirect = urlEncode(redirectTo)
        val payload = buildJsonObject { put("email", email) }
        val path = if (redirectTo.isBlank()) "/auth/v1/recover" else "/auth/v1/recover?redirect_to=$encodedRedirect"
        send(path, "POST", payload, bearer = null)
    }

    /**
     * One-shot recovery: use the access token returned by verify-otp as bearer for the subsequent
     * user update call. We deliberately do NOT establish a full session here so that cancel flows
     * cannot leave the app in a logged-in state without an explicit user sign-in.
     */
    suspend fun resetPasswordWithOtp(email: String, otp: String, newPassword: String) {
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
            ?: error("OTP verification response missing recovery token")
        send(
            "/auth/v1/user",
            "PUT",
            buildJsonObject { put("password", newPassword) },
            bearer = recoveryAccessToken,
        )
    }

    /** Resend a signup verification email, never a password-recovery email. */
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

    suspend fun signOut() {
        val old = credentials
        credentials = null
        rememberSession = false
        sessionStore?.clear()
        if (old != null) {
            runCatching { send("/auth/v1/logout", "POST", bearer = old.accessToken) }
        }
    }

    fun hasActiveSession(): Boolean = credentials != null

    /**
     * Establish or overwrite the in-memory session directly from raw tokens.
     * Used by the deep-link recovery callback flow, which bypasses the standard signIn() method.
     */
    fun attachSession(accessToken: String, refreshToken: String, expiresInSeconds: Long = 3600L) {
        val payload = runCatching {
            val tokenPayload = accessToken.split(".")[1]
            val padded = when (tokenPayload.length % 4) {
                2 -> "$tokenPayload=="
                3 -> "$tokenPayload="
                else -> tokenPayload
            }
            java.util.Base64.getUrlDecoder().decode(padded).decodeToString()
        }.getOrNull() ?: error("Cannot decode access token")
        val userId = runCatching {
            json.parseToJsonElement(payload).jsonObject["sub"]?.jsonPrimitive?.content
        }.getOrNull() ?: error("Cannot parse sub from access token")

        credentials = Credentials(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresInSeconds.coerceAtLeast(0L) * 1000L,
        )
        rememberSession = false
        sessionStore?.clear()
    }

    suspend fun table(
        path: String,
        allowAnonymous: Boolean = false,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "GET",
        allowAnonymous = allowAnonymous,
    )

    suspend fun patchTable(
        path: String,
        body: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "PATCH",
        body = body,
        preferRepresentation = true,
    )

    suspend fun insertTable(
        path: String,
        body: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "POST",
        body = body,
        preferRepresentation = true,
    )

    suspend fun rpc(
        function: String,
        params: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/rpc/$function",
        method = "POST",
        body = params,
    )

    suspend fun updatePassword(password: String): JsonElement {
        val token = freshToken()
        return send("/auth/v1/user", "PUT", buildJsonObject { put("password", password) }, bearer = token)
    }

    suspend fun uploadStorage(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): String = withContext(Dispatchers.IO) {
        val token = freshToken()
        val uploadUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/$bucket/$path"
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mimeType)
            setRequestProperty("x-upsert", "true")
            setFixedLengthStreamingMode(bytes.size)
        }
        connection.outputStream.use { it.write(bytes) }
        val status = connection.responseCode
        if (status !in 200..299) {
            val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw SupabaseHttpException(status, errBody.ifBlank { "Storage upload failed ($status)" })
        }
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
    }

    suspend fun deleteStorage(
        bucket: String,
        path: String,
    ): Unit = withContext(Dispatchers.IO) {
        val token = freshToken()
        val deleteUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/$bucket/$path"
        val connection = (URL(deleteUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        val status = connection.responseCode
        if (status !in 200..299 && status != 404) {
            val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw SupabaseHttpException(status, errBody.ifBlank { "Storage delete failed ($status)" })
        }
    }

    private suspend fun authorized(
        path: String,
        method: String,
        body: JsonObject? = null,
        allowAnonymous: Boolean = false,
        preferRepresentation: Boolean = false,
    ): JsonElement {
        val accessToken = if (allowAnonymous) {
            freshTokenOrNull()
        } else {
            freshToken()
        }
        return send(path, method, body, bearer = accessToken, preferRepresentation = preferRepresentation)
    }

    private suspend fun freshTokenOrNull(): String? = runCatching { freshToken() }.getOrNull()

    private suspend fun freshToken(): String {
        val session = credentials ?: error("Supabase sign-in required")
        if (System.currentTimeMillis() < session.expiresAtMillis - 60_000L) {
            return session.accessToken
        }
        return refreshMutex.withLock {
            val latest = credentials ?: error("Supabase session expired")
            if (System.currentTimeMillis() < latest.expiresAtMillis - 60_000L) {
                return@withLock latest.accessToken
            }
            val updated = refreshSession(latest.userId, latest.refreshToken)
            if (rememberSession) {
                sessionStore?.save(updated.userId, updated.refreshToken)
            }
            credentials = updated
            updated.accessToken
        }
    }

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

    private suspend fun send(
        path: String,
        method: String,
        body: JsonObject? = null,
        bearer: String? = null,
        preferRepresentation: Boolean = false,
    ): JsonElement = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                if (bearer != null) {
                    connection.setRequestProperty("Authorization", "Bearer $bearer")
                }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                }
                if (method == "PATCH") {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    connection.requestMethod = method
                }
                if (preferRepresentation || (method == "POST" && path.startsWith("/rest/v1/"))) {
                    connection.setRequestProperty("Prefer", "return=representation")
                }
            }
            if (body != null) {
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val error = json.parseToJsonElement(response).jsonObject
                    error["message"]?.jsonPrimitive?.content
                        ?: error["msg"]?.jsonPrimitive?.content
                        ?: error["error_description"]?.jsonPrimitive?.content
                        ?: error["error"]?.jsonPrimitive?.content
                }.getOrNull() ?: response.ifBlank { "HTTP $status" }
                throw SupabaseHttpException(status, message)
            }
            if (response.isBlank()) JsonNull else json.parseToJsonElement(response)
        } finally {
            connection?.disconnect()
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class Credentials(
        val userId: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAtMillis: Long,
    )
}
