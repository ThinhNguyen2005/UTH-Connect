package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal PostgREST/GoTrue transport. Only publishable keys are shipped in the APK. */
private const val STORAGE_READ_TIMEOUT_MS = 60_000
private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val DEFAULT_REQUEST_TIMEOUT_MS = 15_000L

@Singleton
class SupabaseRestClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout)
    }
    private val sessionMutex = SessionMutationGate()

    @Volatile private var sessionStore: SecureSessionStore? = null

    @Volatile private var rememberSession = false

    @Volatile private var credentials: Credentials? = null

    val userId: String get() = credentials?.userId ?: error("Supabase sign-in required")
    val userIdOrNull: String? get() = credentials?.userId

    /** Called by the session repository, which owns the remember-me decision. */
    fun attachSecureStore(store: SecureSessionStore) {
        sessionStore = store
    }

    /** Restores a persisted session, clearing it only when the refresh token is rejected. */
    suspend fun restoreSavedSession(requestTimeoutMs: Int? = null): Boolean = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            val saved = sessionStore?.load() ?: return@withLock false
            if (credentials != null) return@withLock true
            try {
                val deadlineNanos = requestTimeoutMs?.let { System.nanoTime() + it * NANOS_PER_MILLISECOND }
                val updated = refreshSession(saved.userId, saved.refreshToken, deadlineNanos)
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
    suspend fun setRememberSession(remember: Boolean) {
        sessionMutex.withLock {
            rememberSession = remember
            if (remember) {
                val current = credentials ?: error("No authenticated Supabase session")
                sessionStore?.save(current.userId, current.refreshToken)
            } else {
                sessionStore?.clear()
            }
        }
    }

    /** Authenticates with Supabase and replaces any prior in-memory session. */
    suspend fun signIn(email: String, password: String) {
        sessionMutex.withLock {
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

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    /** Creates an account and returns the authentication response. */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
        redirectTo: String = "",
    ): JsonObject = sessionMutex.withLock {
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
        response
    }

    /** Requests a password recovery message for the supplied email. */
    suspend fun recoverPassword(email: String, redirectTo: String = "") {
        val payload = buildJsonObject { put("email", email) }
        val path = if (redirectTo.isBlank()) {
            "/auth/v1/recover"
        } else {
            val encodedRedirect = urlEncode(redirectTo)
            "/auth/v1/recover?redirect_to=$encodedRedirect"
        }
        send(path, "POST", payload, bearer = null)
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

    /** Updates the user's password using a recovery access token obtained via deep link. */
    suspend fun resetPasswordWithToken(recoveryAccessToken: String, password: String) {
        send(
            "/auth/v1/user",
            "PUT",
            buildJsonObject { put("password", password) },
            bearer = recoveryAccessToken,
        )
    }

    /** Verifies a token hash from email link (recovery or signup) and returns access token if present. */
    suspend fun verifyEmailTokenHash(tokenHash: String, type: String): JsonObject = send(
        "/auth/v1/verify",
        "POST",
        buildJsonObject {
            put("type", type)
            put("token_hash", tokenHash)
        },
        bearer = null,
    ).jsonObject

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
    suspend fun signOut() = sessionMutex.withLock {
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
    ) = withSessionMutation {
        attachSessionLocked(userId, accessToken, refreshToken, expiresInSeconds)
    }

    /** Runs an external auth exchange in order with restores and other session changes. */
    internal suspend fun <T> withSessionMutation(block: suspend () -> T): T = sessionMutex.withLock { block() }

    /** Installs credentials while the caller already owns [sessionMutex]. */
    internal fun attachSessionLocked(
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

    suspend fun table(
        path: String,
        allowAnonymous: Boolean = false,
    ): JsonElement = authorized("/rest/v1/$path", "GET", allowAnonymous = allowAnonymous)

    /** Updates matching rows through PostgREST. */
    suspend fun patchTable(path: String, body: JsonObject): JsonElement = authorized("/rest/v1/$path", "PATCH", body)

    /** Inserts a row through PostgREST and returns the server response. */
    suspend fun insertTable(path: String, body: JsonObject): JsonElement = authorized("/rest/v1/$path", "POST", body)

    /** Deletes matching rows through PostgREST. */
    suspend fun deleteTable(path: String, requestTimeoutMs: Int? = null): JsonElement = authorized("/rest/v1/$path", "DELETE", requestTimeoutMs = requestTimeoutMs)

    /** Inserts or updates a row using a PostgREST unique constraint as the conflict key. */
    suspend fun upsertTable(path: String, body: JsonObject): JsonElement = authorized("/rest/v1/$path", "POST", body, "resolution=merge-duplicates,return=minimal")

    /** Invokes an authenticated Postgres RPC function. */
    suspend fun rpc(
        function: String,
        params: JsonObject,
        expectedUserId: String? = null,
    ): JsonElement = authorized(
        "/rest/v1/rpc/$function",
        "POST",
        params,
        expectedUserId = expectedUserId,
    )

    /** Upload nhị phân lên Storage; path phải thuộc thư mục `auth.uid()` để qua RLS. */
    suspend fun uploadStorageObject(bucket: String, path: String, bytes: ByteArray, contentType: String) {
        storageRequest("/object/$bucket/$path", "POST", bytes, contentType)
    }

    suspend fun deleteStorageObject(bucket: String, path: String) {
        storageRequest("/object/$bucket/$path", "DELETE", null, null)
    }

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

    /** Updates the password for the currently authenticated user. */
    suspend fun updatePassword(password: String, expectedUserId: String? = null): JsonElement {
        val token = freshToken(expectedUserId = expectedUserId)
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
            readTimeout = STORAGE_READ_TIMEOUT_MS
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mimeType)
            setRequestProperty("x-upsert", "true")
            setFixedLengthStreamingMode(bytes.size)
        }
        try {
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw SupabaseHttpException(status, errBody.ifBlank { "Storage upload failed ($status)" })
            }
            connection.inputStream?.bufferedReader()?.use { it.readText() }
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun authorized(
        path: String,
        method: String,
        body: JsonObject? = null,
        prefer: String? = null,
        allowAnonymous: Boolean = false,
        requestTimeoutMs: Int? = null,
        expectedUserId: String? = null,
    ): JsonElement {
        val deadlineNanos = requestTimeoutMs?.let { System.nanoTime() + it * NANOS_PER_MILLISECOND }
        val accessToken = if (allowAnonymous) {
            freshTokenOrNull(deadlineNanos)
        } else {
            freshToken(deadlineNanos, expectedUserId)
        }
        return send(path, method, body, bearer = accessToken, prefer = prefer, deadlineNanos = deadlineNanos)
    }

    private suspend fun freshTokenOrNull(deadlineNanos: Long? = null): String? = try {
        freshToken(deadlineNanos)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private suspend fun storageRequest(
        path: String,
        method: String,
        body: ByteArray?,
        contentType: String?,
    ) = withContext(Dispatchers.IO) {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val accessToken = freshToken()
        val connection = (URL(BuildConfig.SUPABASE_URL + "/storage/v1" + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = STORAGE_READ_TIMEOUT_MS
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType ?: "application/octet-stream")
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw SupabaseHttpException(status, message.ifBlank { "Storage HTTP $status" })
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Returns the current access token, refreshing it when it is close to expiration. */
    private suspend fun freshToken(deadlineNanos: Long? = null, expectedUserId: String? = null): String {
        val session = credentials ?: error("Supabase sign-in required")
        expectedUserId?.let { check(session.userId == it) { "Supabase session changed" } }
        if (System.currentTimeMillis() < session.expiresAtMillis - 30_000L) return session.accessToken
        return sessionMutex.withLock {
            val latest = credentials ?: error("Supabase session expired")
            expectedUserId?.let { check(latest.userId == it) { "Supabase session changed" } }
            if (System.currentTimeMillis() < latest.expiresAtMillis - 30_000L) return@withLock latest.accessToken
            try {
                val updated = refreshSession(latest.userId, latest.refreshToken, deadlineNanos)
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
    private suspend fun refreshSession(
        expectedUserId: String,
        refreshToken: String,
        deadlineNanos: Long? = null,
    ): Credentials {
        val updated = parseCredentials(
            send(
                "/auth/v1/token?grant_type=refresh_token",
                "POST",
                buildJsonObject { put("refresh_token", refreshToken) },
                bearer = null,
                deadlineNanos = deadlineNanos,
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
        prefer: String? = null,
        deadlineNanos: Long? = null,
    ): JsonElement {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val request = RestRequest(path, method, body, bearer, prefer, requestTimeoutMs(deadlineNanos))
        val response = executeRequest(request)
        val responseBody = response.bodyAsText()
        val status = response.status.value
        if (status !in 200..299) {
            throw SupabaseHttpException(status, supabaseErrorMessage(responseBody, status))
        }
        return if (responseBody.isBlank()) JsonNull else json.parseToJsonElement(responseBody)
    }

    private suspend fun executeRequest(request: RestRequest) = httpClient.request(
        BuildConfig.SUPABASE_URL + request.path,
    ) {
        method = HttpMethod(request.method)
        timeout { requestTimeoutMillis = request.timeoutMs }
        headers {
            append("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            request.bearer?.let { append(HttpHeaders.Authorization, "Bearer $it") }
            requestPrefer(request.method, request.path, request.prefer)
                ?.let { append(HttpHeaders.Prefer, it) }
        }
        if (request.body != null) {
            contentType(ContentType.Application.Json)
            setBody(request.body.toString())
        }
    }

    private fun requestTimeoutMs(deadlineNanos: Long?): Long = deadlineNanos?.let { deadline ->
        ((deadline - System.nanoTime()) / NANOS_PER_MILLISECOND)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
    } ?: DEFAULT_REQUEST_TIMEOUT_MS

    private fun requestPrefer(method: String, path: String, prefer: String?): String? = prefer ?: when {
        method == "PATCH" -> "return=representation"
        method == "POST" && path.startsWith("/rest/v1/") -> "return=representation"
        else -> null
    }

    private fun supabaseErrorMessage(responseBody: String, status: Int): String = runCatching {
        val error = json.parseToJsonElement(responseBody).jsonObject
        (error["error_description"] ?: error["message"] ?: error["error"])
            ?.jsonPrimitive
            ?.content
    }.getOrNull() ?: "Supabase HTTP $status"

    private data class RestRequest(
        val path: String,
        val method: String,
        val body: JsonObject?,
        val bearer: String?,
        val prefer: String?,
        val timeoutMs: Long,
    )

    private data class Credentials(
        val userId: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAtMillis: Long,
    )
}

class SupabaseHttpException(val status: Int, message: String) : IllegalStateException(message)
