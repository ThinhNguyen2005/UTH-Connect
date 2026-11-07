package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
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

private const val STORAGE_TIMEOUT_MILLIS = 30_000

/** Minimal PostgREST/GoTrue transport. Only publishable keys are shipped in the APK. */
@Singleton
class SupabaseRestClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshMutex = Mutex()

    @Volatile private var credentials: Credentials? = null

    @Volatile var recoveryToken: String? = null

    val userId: String get() = credentials?.userId ?: error("Supabase sign-in required")
    val userIdOrNull: String? get() = credentials?.userId

    /**
     * Authenticates a user with email and password, establishing an active session.
     */
    suspend fun signIn(email: String, password: String, replaceExisting: Boolean = true) {
        // A failed login must not accidentally leave the previous user's tokens active.
        // Password confirmation is a separate flow and must preserve the current session on failure.
        if (replaceExisting) credentials = null
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

    /**
     * Registers a new user account with student credentials and metadata.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
        redirectTo: String = "uthconnect://auth/callback",
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
        val response = send("/auth/v1/signup?redirect_to=$encodedRedirect", "POST", payload, bearer = null).jsonObject
        if (response.containsKey("access_token") && response.containsKey("user")) {
            runCatching { credentials = parseCredentials(response) }
        }
        return response
    }

    /**
     * Initiates a password recovery workflow by dispatching a reset email.
     */
    suspend fun recoverPassword(email: String, redirectTo: String = "uthconnect://auth/reset-password") {
        val encodedRedirect = urlEncode(redirectTo)
        val payload = buildJsonObject { put("email", email) }
        send("/auth/v1/recover?redirect_to=$encodedRedirect", "POST", payload, bearer = null)
    }

    /**
     * Clears local credentials and invalidates the remote session token.
     */
    suspend fun signOut() {
        val old = credentials
        credentials = null
        recoveryToken = null
        if (old != null) runCatching { send("/auth/v1/logout", "POST", bearer = old.accessToken) }
    }

    /**
     * Adopts a session that was established outside this client — today only the
     * supabase-kt native Google exchange. The tokens behave identically to a password
     * login, so [freshToken] can still refresh them through `grant_type=refresh_token`.
     *
     * Kept in memory only: a Google session is not a remembered-login grant
     * (see PreferencesSessionRepository, which refuses remember-me under Supabase).
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
    }

    /**
     * Executes a GET query against a PostgREST table endpoint.
     *
     * @param path The resource path with optional query filters.
     * @param allowAnonymous Whether to allow anonymous access using the publishable key.
     */
    suspend fun table(
        path: String,
        allowAnonymous: Boolean = false,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "GET",
        allowAnonymous = allowAnonymous,
    )

    /**
     * Inserts a record into a PostgREST table requiring authenticated credentials.
     *
     * @param path The resource table endpoint path.
     * @param body The JSON payload representing the record.
     */
    suspend fun insertTable(
        path: String,
        body: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "POST",
        body = body,
        preferRepresentation = true,
    )

    /**
     * Patches an existing record in a PostgREST table requiring authenticated credentials.
     *
     * @param path The resource endpoint path with equality filters.
     * @param body The JSON payload containing fields to update.
     */
    suspend fun patchTable(
        path: String,
        body: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/$path",
        method = "PATCH",
        body = body,
        preferRepresentation = true,
    )

    /**
     * Invokes a Postgres Remote Procedure Call (RPC) function via PostgREST.
     */
    suspend fun rpc(
        function: String,
        params: JsonObject,
    ): JsonElement = authorized(
        path = "/rest/v1/rpc/$function",
        method = "POST",
        body = params,
    )

    /**
     * Updates the password of the authenticated user or token holder.
     */
    suspend fun updatePassword(password: String): JsonElement {
        val token = recoveryToken ?: freshToken()
        return send("/auth/v1/user", "PUT", buildJsonObject { put("password", password) }, bearer = token)
    }

    /**
     * Uploads binary object data to a Supabase Storage bucket.
     * Requires an authenticated session and streams data with fixed length to prevent memory buffering.
     *
     * @param bucket Target bucket name.
     * @param path Path within the bucket.
     * @param bytes Binary payload bytes.
     * @param mimeType Media type string.
     * @return Public URL pointing to the uploaded object.
     */
    suspend fun uploadStorage(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): String = withContext(Dispatchers.IO) {
        require(BuildConfig.USE_SUPABASE) { "Supabase is disabled" }
        val token = freshToken()
        val cleanPath = path.trimStart('/')
        val url = URL("${BuildConfig.SUPABASE_URL}/storage/v1/object/$bucket/$cleanPath")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = STORAGE_TIMEOUT_MILLIS
            readTimeout = STORAGE_TIMEOUT_MILLIS
            doOutput = true
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mimeType)
            setRequestProperty("x-upsert", "true")
        }
        try {
            connection.outputStream.use { it.write(bytes) }
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
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$cleanPath"
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Executes an authorized HTTP request adding Bearer and API key headers.
     */
    private suspend fun authorized(
        path: String,
        method: String,
        body: JsonObject? = null,
        preferRepresentation: Boolean = false,
        allowAnonymous: Boolean = false,
    ): JsonElement {
        val accessToken = if (allowAnonymous) {
            runCatching { freshToken() }.getOrNull() ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY
        } else {
            freshToken()
        }
        return send(path, method, body, accessToken, preferRepresentation)
    }

    /**
     * Retrieves an active access token, refreshing if expiring soon.
     */
    private suspend fun freshToken(): String {
        val session = credentials ?: error("Supabase sign-in required")
        if (System.currentTimeMillis() < session.expiresAtMillis - 30_000L) return session.accessToken
        return refreshMutex.withLock {
            val latest = credentials ?: error("Supabase session expired")
            if (System.currentTimeMillis() < latest.expiresAtMillis - 30_000L) return@withLock latest.accessToken
            try {
                val response = send(
                    "/auth/v1/token?grant_type=refresh_token",
                    "POST",
                    buildJsonObject { put("refresh_token", latest.refreshToken) },
                    bearer = null,
                ).jsonObject
                val updated = parseCredentials(response)
                check(updated.userId == latest.userId) { "Session user changed" }
                credentials = updated
                updated.accessToken
            } catch (error: Exception) {
                credentials = null
                throw error
            }
        }
    }

    /**
     * Parses authentication tokens and expiration timestamp from GoTrue JSON response.
     */
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

    /**
     * Performs a raw HTTP request to Supabase endpoints.
     */
    private suspend fun send(
        path: String,
        method: String,
        body: JsonObject? = null,
        bearer: String?,
        preferRepresentation: Boolean = false,
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
                if (preferRepresentation) {
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
