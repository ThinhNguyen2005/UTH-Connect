package com.giathinh.uthconnect.presentation.auth.verifyemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.usecase.SendVerificationUseCase
import com.giathinh.uthconnect.domain.usecase.CheckVerificationUseCase
import com.giathinh.uthconnect.domain.usecase.SignOutUseCase
import com.giathinh.uthconnect.domain.usecase.ManageAuthAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

/** Chờ email và callable claims; không cấp Main chỉ vì email đã đổi trạng thái. */
@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val send: SendVerificationUseCase,
    private val check: CheckVerificationUseCase,
    private val signOut: SignOutUseCase,
    account: ManageAuthAccountUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(VerifyEmailUiState())
    val uiState = state.asStateFlow()
    private var countdown: Job? = null
    private var identityUid: String? = null
    init {
        viewModelScope.launch {
            account.observePendingIdentity().collect { identity ->
                if (identity?.uid != identityUid) {
                    identityUid = identity?.uid
                    countdown?.cancel()
                    state.value = VerifyEmailUiState(email = identity?.email.orEmpty())
                    if (identity != null) startCooldown()
                }
            }
        }
    }
    fun onAction(action: VerifyEmailAction) {
        if (state.value.isChecking || state.value.isSending) return
        if (action == VerifyEmailAction.Resend && state.value.cooldownSeconds > 0) return
        state.update { it.copy(error = null, emailSent = false,
            isChecking = action != VerifyEmailAction.Resend, isSending = action == VerifyEmailAction.Resend) }
        viewModelScope.launch {
            try {
                when (action) {
                    VerifyEmailAction.Resend -> send().fold(onSuccess = {
                        state.update { it.copy(emailSent = true) }; startCooldown()
                    }, onFailure = { error -> fail(error) })
                    is VerifyEmailAction.Check -> check(action.remember).onFailure { fail(it) }
                    VerifyEmailAction.SignOut -> signOut()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fail(error) }
            finally { state.update { it.copy(isChecking = false, isSending = false) } }
        }
    }
    private fun fail(error: Throwable) {
        state.update { it.copy(error = (error as? AuthException)?.reason ?: AuthError.Unknown) }
    }
    private fun startCooldown() {
        countdown?.cancel()
        state.update { it.copy(cooldownSeconds = 60) }
        countdown = viewModelScope.launch {
            repeat(60) { delay(1000); state.update { it.copy(cooldownSeconds = (it.cooldownSeconds - 1).coerceAtLeast(0)) } }
        }
    }
}
