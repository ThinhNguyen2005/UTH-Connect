package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.data.repository.SupabaseHttpException
import com.giathinh.uthconnect.data.repository.SupabaseRestClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

sealed interface AuthDeepLinkTarget {
    data class PasswordRecovery(val accessToken: String?, val tokenHash: String?) : AuthDeepLinkTarget
    data class EmailVerification(val tokenHash: String?, val accessToken: String?) : AuthDeepLinkTarget
    data class Error(val message: String, val isRecovery: Boolean) : AuthDeepLinkTarget
    data object Unknown : AuthDeepLinkTarget
}

object AuthDeepLinkParser {
    fun parse(uriString: String): AuthDeepLinkTarget {
        val fragment = uriString.substringAfter('#', "")
        val query = uriString.substringAfter('?', "").substringBefore('#')

        val params = mutableMapOf<String, String>()
        extractParams(query, params)
        extractParams(fragment, params)

        val error = params["error"]
        val errorCode = params["error_code"]
        val errorDesc = params["error_description"]

        if (!error.isNullOrBlank() || !errorCode.isNullOrBlank()) {
            val isRecovery = uriString.contains("reset-password") || params["type"] == "recovery"
            val message = when (errorCode) {
                "otp_expired" -> "Liên kết đã hết hạn hoặc đã được sử dụng trước đó. Vui lòng gửi lại yêu cầu mới."
                else -> errorDesc?.let { decodeUrl(it) } ?: "Liên kết xác thực không hợp lệ hoặc đã hết hạn."
            }
            return AuthDeepLinkTarget.Error(message = message, isRecovery = isRecovery)
        }

        val type = params["type"].orEmpty()
        val accessToken = params["access_token"]
        val tokenHash = params["token_hash"]

        return when {
            type == "recovery" || uriString.contains("reset-password") ->
                AuthDeepLinkTarget.PasswordRecovery(accessToken, tokenHash)

            type == "signup" || type == "email" ->
                AuthDeepLinkTarget.EmailVerification(tokenHash, accessToken)

            else -> AuthDeepLinkTarget.Unknown
        }
    }

    private fun decodeUrl(value: String): String = try {
        java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        value
    }

    private fun extractParams(segment: String, out: MutableMap<String, String>) {
        if (segment.isBlank()) return
        segment.split('&').forEach { param ->
            val parts = param.split('=', limit = 2)
            if (parts.size == 2) out[parts[0]] = parts[1]
        }
    }

    fun handle(
        target: AuthDeepLinkTarget,
        client: SupabaseRestClient,
        uiState: MutableStateFlow<AuthUiState>,
        events: Channel<AuthEvent>,
        scope: CoroutineScope,
    ) {
        when (target) {
            is AuthDeepLinkTarget.PasswordRecovery -> handleRecovery(target, client, uiState, events, scope)
            is AuthDeepLinkTarget.EmailVerification -> handleVerification(target, client, uiState, events, scope)
            is AuthDeepLinkTarget.Error -> handleError(target, uiState, events, scope)
            AuthDeepLinkTarget.Unknown -> Unit
        }
    }

    private fun handleError(
        target: AuthDeepLinkTarget.Error,
        uiState: MutableStateFlow<AuthUiState>,
        events: Channel<AuthEvent>,
        scope: CoroutineScope,
    ) {
        if (target.isRecovery) {
            uiState.update {
                it.copy(
                    screenStep = AuthScreenStep.ResetPassword,
                    resetPasswordError = target.message,
                )
            }
        }
        scope.launch {
            events.send(AuthEvent.GeneralMessage(target.message))
        }
    }

    private fun handleRecovery(
        target: AuthDeepLinkTarget.PasswordRecovery,
        client: SupabaseRestClient,
        uiState: MutableStateFlow<AuthUiState>,
        events: Channel<AuthEvent>,
        scope: CoroutineScope,
    ) {
        val accessToken = target.accessToken
        val tokenHash = target.tokenHash
        if (!accessToken.isNullOrBlank()) {
            uiState.update {
                it.copy(
                    screenStep = AuthScreenStep.ResetPassword,
                    recoveryAccessToken = accessToken,
                    resetPasswordError = null,
                )
            }
            scope.launch {
                events.send(AuthEvent.GeneralMessage("Đã xác thực liên kết email. Vui lòng nhập mật khẩu mới."))
            }
        } else if (!tokenHash.isNullOrBlank()) {
            scope.launch {
                try {
                    val verified = client.verifyEmailTokenHash(tokenHash, "recovery")
                    val token = verified["access_token"]?.jsonPrimitive?.content
                    if (token != null) {
                        uiState.update {
                            it.copy(
                                screenStep = AuthScreenStep.ResetPassword,
                                recoveryAccessToken = token,
                                resetPasswordError = null,
                            )
                        }
                        events.send(AuthEvent.GeneralMessage("Đã xác thực thành công. Vui lòng nhập mật khẩu mới."))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: SupabaseHttpException) {
                    val msg = error.message?.ifBlank {
                        "Liên kết đặt lại mật khẩu đã hết hạn hoặc không hợp lệ."
                    } ?: "Liên kết đặt lại mật khẩu đã hết hạn hoặc không hợp lệ."
                    uiState.update { it.copy(screenStep = AuthScreenStep.ResetPassword, resetPasswordError = msg) }
                } catch (io: IOException) {
                    val msg = io.message?.ifBlank { "Lỗi mạng" } ?: "Lỗi kết nối mạng khi xác thực liên kết."
                    uiState.update { it.copy(screenStep = AuthScreenStep.ResetPassword, resetPasswordError = msg) }
                }
            }
        } else {
            uiState.update { it.copy(screenStep = AuthScreenStep.ResetPassword) }
        }
    }

    private fun handleVerification(
        target: AuthDeepLinkTarget.EmailVerification,
        client: SupabaseRestClient,
        uiState: MutableStateFlow<AuthUiState>,
        events: Channel<AuthEvent>,
        scope: CoroutineScope,
    ) {
        val tokenHash = target.tokenHash
        if (!tokenHash.isNullOrBlank()) {
            scope.launch {
                try {
                    client.verifyEmailTokenHash(tokenHash, "signup")
                    uiState.update { it.copy(screenStep = AuthScreenStep.Password) }
                    events.send(AuthEvent.GeneralMessage("Xác thực email thành công! Bạn có thể đăng nhập ngay."))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: SupabaseHttpException) {
                    val msg = error.message?.ifBlank {
                        "Liên kết xác thực đã hết hạn hoặc không hợp lệ."
                    } ?: "Liên kết xác thực đã hết hạn hoặc không hợp lệ."
                    events.send(AuthEvent.GeneralMessage(msg))
                } catch (io: IOException) {
                    val msg = io.message?.ifBlank { "Lỗi mạng" } ?: "Lỗi kết nối mạng khi xác thực email."
                    events.send(AuthEvent.GeneralMessage(msg))
                }
            }
        } else {
            uiState.update { it.copy(screenStep = AuthScreenStep.Password) }
            scope.launch {
                events.send(AuthEvent.GeneralMessage("Xác thực email thành công! Bạn có thể đăng nhập ngay."))
            }
        }
    }
}
