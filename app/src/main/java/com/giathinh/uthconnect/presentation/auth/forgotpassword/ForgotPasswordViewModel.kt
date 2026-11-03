package com.giathinh.uthconnect.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val reset: ResetPasswordUseCase) : ViewModel() {
    private val state = MutableStateFlow(ForgotPasswordUiState())
    val uiState = state.asStateFlow()
    fun onAction(action: ForgotPasswordAction) {
        if (state.value.isSending) return
        when (action) {
            is ForgotPasswordAction.EmailChanged -> state.update { it.copy(email = action.value, error = null, sent = false) }
            ForgotPasswordAction.Clear -> state.value = ForgotPasswordUiState()
            ForgotPasswordAction.Send -> {
                val email = state.value.email
                state.update { it.copy(isSending = true, error = null, sent = false) }
                viewModelScope.launch {
                    try { reset(email).fold(onSuccess = { state.update { it.copy(sent = true) } },
                        onFailure = { error -> state.update { it.copy(error = (error as? AuthException)?.reason ?: AuthError.Unknown) } }) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: IllegalArgumentException) { state.update { it.copy(error = AuthError.DomainNotAllowed) } }
                    catch (_: Exception) { state.update { it.copy(error = AuthError.Unknown) } }
                    finally { state.update { it.copy(isSending = false) } }
                }
            }
        }
    }
}
