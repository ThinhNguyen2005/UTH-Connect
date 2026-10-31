package com.giathinh.uthconnect.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfile: GetProfileUseCase,
    private val session: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                _uiState.update { it.copy(isLoading = false, student = profile) }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Logout -> logout()
        }
    }

    private fun logout() {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, failure = null) }
            try {
                session.signOut()
                _uiState.update { it.copy(isLoggingOut = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoggingOut = false, failure = ProfileFailure.LogoutFailed) }
            }
        }
    }
}
