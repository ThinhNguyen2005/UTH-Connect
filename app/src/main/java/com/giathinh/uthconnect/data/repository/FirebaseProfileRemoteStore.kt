package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/** Firestore REST có bearer token Firebase, không tạo disk cache hồ sơ riêng hoặc xóa cache module khác. */
internal class FirebaseProfileRemoteStore(private val authProvider: Provider<FirebaseAuth>) {
    private suspend fun identity(): Pair<String, String> {
        val user = authProvider.get().currentUser ?: throw AuthException(AuthError.InvalidSession)
        val token = user.getIdToken(false).await().token ?: throw AuthException(AuthError.InvalidSession)
        return user.uid to token
    }
    suspend fun read(): Map<String, Any> {
        val (uid, token) = identity()
        val document = request(uid, token, null)
        val fields = document.optJSONObject("fields") ?: return emptyMap()
        return fields.keys().asSequence().mapNotNull { key ->
            val value = fields.optJSONObject(key)
            if (value?.has("stringValue") == true) key to value.getString("stringValue") else null
        }.toMap()
    }
    suspend fun update(fields: Map<String, String>) {
        val (uid, token) = identity()
        require(fields.keys == setOf("phoneNumber", "personalEmail", "currentAddress"))
        request(uid, token, fields)
    }
    private suspend fun request(uid: String, token: String, update: Map<String, String>?): JSONObject = withContext(Dispatchers.IO) {
        val project = FirebaseApp.getInstance().options.projectId ?: throw AuthException(AuthError.Configuration)
        require(project.matches(Regex("[a-z][a-z0-9-]{4,62}")))
        val encodedUid = URLEncoder.encode(uid, "UTF-8").replace("+", "%20")
        val query = if (update == null) "" else "?currentDocument.exists=true&" +
            update.keys.joinToString("&") { "updateMask.fieldPaths=$it" }
        val connection = URI("https://firestore.googleapis.com/v1/projects/$project/databases/(default)/documents/users/$encodedUid$query")
            .toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Cache-Control", "no-store")
            if (update != null) {
                connection.requestMethod = "PATCH"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val fields = JSONObject()
                update.forEach { (key, value) -> fields.put(key, JSONObject().put("stringValue", value)) }
                connection.outputStream.use { it.write(JSONObject().put("fields", fields).toString().toByteArray(Charsets.UTF_8)) }
            }
            when (connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                401 -> throw AuthException(AuthError.InvalidSession)
                403 -> throw AuthException(AuthError.PermissionDenied)
                404 -> throw AuthException(AuthError.ProfileMissing)
                429 -> throw AuthException(AuthError.TooManyRequests)
                in 500..599 -> throw AuthException(AuthError.Network)
                else -> throw AuthException(AuthError.Unknown)
            }
        } finally { connection.disconnect() }
    }
}
