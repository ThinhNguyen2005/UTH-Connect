package com.giathinh.uthconnect.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.ManageStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.ChangePasswordResult
import com.giathinh.uthconnect.domain.usecase.ChangePasswordUseCase
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateContactResult
import com.giathinh.uthconnect.domain.usecase.UpdateStudentContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfile: GetProfileUseCase,
    private val manageSession: ManageStudentSessionUseCase,
    private val changePassword: ChangePasswordUseCase,
    private val updateContact: UpdateStudentContactUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                _uiState.update { it.copy(isLoading = false, student = profile) }
            }
        }
        viewModelScope.launch {
            manageSession.observeBiometricEnabled().collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OpenPassword -> _uiState.update {
                it.copy(page = ProfilePage.ChangePassword, passwordForm = PasswordForm(), failure = null, notice = null)
            }
            ProfileAction.OpenContact -> _uiState.update { s ->
                s.copy(page = ProfilePage.EditContact, contactForm = ContactForm(
                    s.student?.phoneNumber.orEmpty(), s.student?.personalEmail.orEmpty(), s.student?.currentAddress.orEmpty()
                ), failure = null, notice = null)
            }
            ProfileAction.Back -> _uiState.update { it.copy(page = ProfilePage.Overview, passwordForm = PasswordForm(), failure = null) }
            is ProfileAction.OldPasswordChanged -> _uiState.update {
                it.copy(passwordForm = it.passwordForm.copy(current = action.value, incorrectCurrent = false))
            }
            is ProfileAction.NewPasswordChanged -> _uiState.update {
                it.copy(passwordForm = it.passwordForm.copy(updated = action.value))
            }
            is ProfileAction.ConfirmPasswordChanged -> _uiState.update {
                it.copy(passwordForm = it.passwordForm.copy(confirmed = action.value))
            }
            ProfileAction.SubmitPassword -> submitPassword()
            is ProfileAction.PhoneChanged -> _uiState.update {
                it.copy(contactForm = it.contactForm.copy(phone = action.value))
            }
            is ProfileAction.EmailChanged -> _uiState.update {
                it.copy(contactForm = it.contactForm.copy(email = action.value))
            }
            is ProfileAction.AddressChanged -> _uiState.update {
                it.copy(contactForm = it.contactForm.copy(address = action.value))
            }
            ProfileAction.SubmitContact -> submitContact()
            ProfileAction.RequestBiometricEnroll -> Unit // platform prompt handled in ProfileRoute
            ProfileAction.BiometricVerified -> setBiometric(true)
            ProfileAction.BiometricFailed -> _uiState.update { it.copy(failure = ProfileFailure.BiometricFailed) }
            ProfileAction.DisableBiometric -> setBiometric(false)
            ProfileAction.RequestLogout, ProfileAction.Logout -> _uiState.update {
                it.copy(confirmLogout = true, failure = null)
            }
            ProfileAction.CancelLogout -> _uiState.update { it.copy(confirmLogout = false, failure = null) }
            ProfileAction.ConfirmLogout, ProfileAction.RetryLogout -> logout()
        }
    }

    private fun submitPassword() {
        val snapshot = _uiState.value
        if (snapshot.isSaving || snapshot.student == null) return
        _uiState.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            try {
                val p = snapshot.passwordForm
                when (val result = changePassword(p.current, p.updated, p.confirmed)) {
                    is ChangePasswordResult.InvalidForm -> _uiState.update {
                        it.copy(passwordForm = it.passwordForm.copy(errors = result.validation))
                    }
                    ChangePasswordResult.IncorrectCurrentPassword -> _uiState.update {
                        it.copy(passwordForm = it.passwordForm.copy(incorrectCurrent = true))
                    }
                    ChangePasswordResult.Success -> {
                        _uiState.update {
                            it.copy(page = ProfilePage.Overview, passwordForm = PasswordForm(), notice = ProfileNotice.PasswordChanged)
                        }
                        eventChannel.send(ProfileEvent.PasswordChanged)
                    }
                    else -> _uiState.update { it.copy(failure = ProfileFailure.SaveFailed) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = ProfileFailure.SaveFailed) } }
            finally { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    private fun submitContact() {
        val snapshot = _uiState.value
        if (snapshot.isSaving || snapshot.student == null) return
        _uiState.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            try {
                val c = snapshot.contactForm
                when (val result = updateContact(c.phone, c.email, c.address)) {
                    is UpdateContactResult.InvalidForm -> _uiState.update {
                        it.copy(contactForm = it.contactForm.copy(errors = result.validation))
                    }
                    UpdateContactResult.Success -> {
                        _uiState.update { it.copy(page = ProfilePage.Overview, notice = ProfileNotice.ContactSaved) }
                        eventChannel.send(ProfileEvent.ContactSaved)
                    }
                    else -> _uiState.update { it.copy(failure = ProfileFailure.SaveFailed) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = ProfileFailure.SaveFailed) } }
            finally { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    private fun setBiometric(enable: Boolean) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            try {
                if (enable) {
                    manageSession.enableBiometricAfterPrompt()
                } else manageSession.disableBiometric()
                _uiState.update { it.copy(notice = if (enable) ProfileNotice.BiometricEnabled else ProfileNotice.BiometricDisabled) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = ProfileFailure.BiometricFailed) } }
            finally { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    private fun logout() {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, failure = null) }
            try {
                manageSession.signOut()
                _uiState.update { it.copy(isLoggingOut = false, confirmLogout = false, page = ProfilePage.Overview,
                    passwordForm = PasswordForm(), contactForm = ContactForm(), notice = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.update { it.copy(isLoggingOut = false, confirmLogout = true, failure = ProfileFailure.LogoutFailed) }
                eventChannel.send(ProfileEvent.LogoutFailed)
            }
        }
    }
}
