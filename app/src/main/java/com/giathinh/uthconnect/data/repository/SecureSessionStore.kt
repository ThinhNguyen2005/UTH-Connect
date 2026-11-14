package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Device-bound refresh-token storage. The Android Keystore key is not backed up. */
@Singleton
class SecureSessionStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("uth_secure_session", Context.MODE_PRIVATE)

    data class SavedSession(val userId: String, val refreshToken: String)

    @Synchronized
    fun save(userId: String, refreshToken: String) {
        require(userId.isNotBlank() && refreshToken.isNotBlank())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal("$userId\n$refreshToken".toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString(KEY_SESSION, encoded).commit()) { "Cannot persist encrypted session" }
    }

    @Synchronized
    fun load(): SavedSession? {
        val encoded = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > IV_LENGTH)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, bytes.copyOfRange(0, IV_LENGTH)))
            val parts = cipher.doFinal(bytes.copyOfRange(IV_LENGTH, bytes.size)).toString(Charsets.UTF_8).split('\n', limit = 2)
            require(parts.size == 2 && parts.all(String::isNotBlank))
            SavedSession(parts[0], parts[1])
        } catch (error: Exception) {
            // An invalidated Keystore key or restored ciphertext cannot authenticate the user.
            clear()
            null
        }
    }

    @Synchronized
    fun clear() {
        check(prefs.edit().remove(KEY_SESSION).commit()) { "Cannot erase encrypted session" }
    }

    private fun key(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALIAS = "uth_connect_refresh_token_v1"
        const val KEY_SESSION = "encrypted_refresh_session"
        const val IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
