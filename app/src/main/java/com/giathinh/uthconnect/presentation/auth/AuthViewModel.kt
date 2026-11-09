package com.giathinh.uthconnect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.SupabaseRestClient
import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_ENGLISH
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_VIETNAMESE
import com.giathinh.uthconnect.domain.model.ThemeMode
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import com.giathinh.uthconnect.domain.repository.VerificationStatus
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.CompletePasswordResetOutcome
import com.giathinh.uthconnect.domain.usecase.CompletePasswordResetUseCase
import com.giathinh.uthconnect.domain.usecase.ForgotPasswordOutcome
import com.giathinh.uthconnect.domain.usecase.ForgotPasswordUseCase
import com.giathinh.uthconnect.domain.usecase.GetSettingsUseCase
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleResult
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.domain.usecase.SignUpOutcome
import com.giathinh.uthconnect.domain.usecase.SignUpUseCase
import com.giathinh.uthconnect.domain.usecase.SubmitVerificationOutcome
import com.giathinh.uthconnect.domain.usecase.SubmitVerificationRequestUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateSettingsUseCase
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val sessions: AccessStudentSessionUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signUp: SignUpUseCase,
    private val forgotPassword: ForgotPasswordUseCase,
    private val completePasswordReset: CompletePasswordResetUseCase,
    private val submitVerification: SubmitVerificationRequestUseCase,
    private val supabaseRestClient: SupabaseRestClient,
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    constructor(
        signIn: SignInUseCase,
        sessions: AccessStudentSessionUseCase,
        signInWithGoogle: SignInWithGoogleUseCase,
    ) : this(
        signIn = signIn,
        sessions = sessions,
        signInWithGoogle = signInWithGoogle,
        signUp = SignUpUseCase(object : com.giathinh.uthconnect.domain.repository.AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success(null)
            override suspend fun findStudent(studentId: String) = Result.success(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }),
        forgotPassword = ForgotPasswordUseCase(object : com.giathinh.uthconnect.domain.repository.AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success(null)
            override suspend fun findStudent(studentId: String) = Result.success(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }),
        completePasswordReset = CompletePasswordResetUseCase(object : com.giathinh.uthconnect.domain.repository.AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success(null)
            override suspend fun findStudent(studentId: String) = Result.success(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }),
        submitVerification = SubmitVerificationRequestUseCase(object : com.giathinh.uthconnect.domain.repository.AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success(null)
            override suspend fun findStudent(studentId: String) = Result.success(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }),
        supabaseRestClient = SupabaseRestClient(),
        getSettings = GetSettingsUseCase(NoopSettingsRepository),
        updateSettings = UpdateSettingsUseCase(NoopSettingsRepository),
    )
    private val googleEnabled = BuildConfig.USE_SUPABASE && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
    private val _uiState = MutableStateFlow(AuthUiState(googleSignInAvailable = googleEnabled))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var cooldownJob: Job? = null

    /**
     * Ngôn ngữ đang thực sự bật (BCP-47). Giữ bản sao trong VM để pill VI/EN biết
     * đâu là lựa chọn hiện tại sau khi activity đã `recreate()`, và không phát
     * `recreate()` thừa khi người dùng bấm lại đúng ô đang chọn.
     */
    private var appliedLanguageTag = LANGUAGE_TAG_VIETNAMESE

    init {
        viewModelScope.launch {
            // Pill VI/EN phải phản ánh đúng ngôn ngữ đã lưu, không phải giá trị cứng "VI".
            val stored = getSettings().first()
            appliedLanguageTag = stored.languageTag
            _uiState.update { it.copy(selectedLanguage = stored.languageTag.toLanguageCode()) }
        }
        viewModelScope.launch {
            try {
                sessions.restore()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isRestoring = false, failure = AuthFailure.SessionRestore) }
            }
            sessions.observe().collect { current ->
                _uiState.update {
                    val justLoggedOut = it.student != null && current.profile == null
                    it.copy(
                        isRestoring = current.isRestoring,
                        student = current.profile,
                        identifier = if (justLoggedOut) "" else it.identifier,
                        password = if (justLoggedOut) "" else it.password,
                        rememberMe = current.remembered,
                        failure = if (current.failure != null) AuthFailure.SessionRestore else it.failure,
                    )
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.SelectLanguage -> selectLanguage(action.lang)

            AuthAction.SwitchAccount -> _uiState.update {
                it.copy(
                    screenStep = AuthScreenStep.Password,
                    identifier = "",
                    password = "",
                    failure = null,
                )
            }

            is AuthAction.IdentifierChanged -> _uiState.update {
                it.copy(identifier = action.value, identifierError = null, failure = null, forgotEmail = action.value)
            }

            is AuthAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordError = null, failure = null)
            }

            is AuthAction.RememberChanged -> _uiState.update { it.copy(rememberMe = action.value) }

            AuthAction.TogglePassword -> _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

            AuthAction.ForgotPassword -> _uiState.update {
                it.copy(
                    forgotDialogOpen = true,
                    forgotEmail = it.forgotEmail.ifEmpty { it.identifier },
                    forgotError = null,
                    forgotSuccessNotice = false,
                )
            }

            AuthAction.DismissForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = false) }

            is AuthAction.ForgotEmailChanged -> _uiState.update { it.copy(forgotEmail = action.value, forgotError = null) }

            AuthAction.SubmitForgotPassword -> submitForgotPassword()

            AuthAction.Submit, AuthAction.Retry -> submit()

            AuthAction.RequestGoogleSignIn -> _uiState.update {
                if (googleEnabled && it.student == null) it.copy(isSigningInWithGoogle = true, failure = null) else it
            }

            is AuthAction.GoogleIdTokenReceived -> googleSignIn(action.idToken, action.rawNonce)

            AuthAction.GoogleSignInCancelled -> _uiState.update { it.copy(isSigningInWithGoogle = false) }

            AuthAction.GoogleSignInUnavailable -> _uiState.update {
                it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleUnavailable)
            }

            // Next-step flow actions
            AuthAction.ContinueIdentifier -> continueIdentifier()

            is AuthAction.SelectStep -> _uiState.update {
                it.copy(
                    screenStep = action.step,
                    failure = null,
                    identifierError = null,
                    passwordError = null,
                    registeredEmail = if (action.step == AuthScreenStep.VerifyEmail && it.registeredEmail.isBlank()) {
                        it.forgotEmail.ifBlank { it.identifier }
                    } else {
                        it.registeredEmail
                    },
                )
            }

            is AuthAction.NewStudentNameChanged -> _uiState.update { it.copy(newStudentName = action.value) }

            is AuthAction.NewStudentFacultyChanged -> _uiState.update { it.copy(newStudentFaculty = action.value) }

            is AuthAction.ConfirmPasswordChanged -> _uiState.update { it.copy(confirmPassword = action.value) }

            AuthAction.SubmitFirstTimeSetup -> submitFirstTimeSetup()

            AuthAction.ClearSetupNotice -> _uiState.update { it.copy(setupSuccessNotice = false) }

            // Student ID verification actions
            is AuthAction.VerificationStudentIdChanged -> _uiState.update { it.copy(verificationStudentId = action.value) }

            AuthAction.SubmitVerificationRequest -> submitVerificationRequest()

            // Reset password actions
            is AuthAction.ResetNewPasswordChanged -> _uiState.update { it.copy(resetNewPassword = action.value, resetPasswordError = null) }

            is AuthAction.ResetConfirmPasswordChanged -> _uiState.update { it.copy(resetConfirmPassword = action.value, resetPasswordError = null) }

            AuthAction.ToggleResetPasswordVisibility -> _uiState.update { it.copy(resetPasswordVisible = !it.resetPasswordVisible) }

            AuthAction.SubmitResetPassword -> submitResetPassword()

            // Email verification actions
            AuthAction.ResendVerificationEmail -> resendVerificationEmail()

            is AuthAction.DeepLinkReceived -> handleDeepLink(action.token, action.type)
        }
    }

    private fun continueIdentifier() {
        val snapshot = _uiState.value
        val id = snapshot.identifier.trim()
        if (id.isBlank()) {
            _uiState.update { it.copy(identifierError = LoginFieldError.Required) }
            return
        }
        val isNumericId = id.all { it.isDigit() } && id.length in 8..12
        val isUtEmail = (id.endsWith("@ut.edu.vn", ignoreCase = true) || id.endsWith("@uth.edu.vn", ignoreCase = true)) && id.indexOf('@') > 0
        if (!isNumericId && !isUtEmail) {
            _uiState.update { it.copy(identifierError = LoginFieldError.InvalidIdentifier) }
            return
        }
        _uiState.update { it.copy(identifierError = null, failure = null, forgotEmail = id) }
        val isEnrolled = id == "2023040123" || id.startsWith("2023") || id.startsWith("215") || isUtEmail
        if (isEnrolled) {
            _uiState.update { it.copy(screenStep = AuthScreenStep.Password) }
        } else {
            _uiState.update { it.copy(screenStep = AuthScreenStep.FirstTimeSetup) }
        }
    }

    private fun submitFirstTimeSetup() {
        val snapshot = _uiState.value
        _uiState.update { it.copy(isSigningIn = true, failure = null) }
        viewModelScope.launch {
            try {
                when (
                    val outcome = signUp(
                        identifier = snapshot.identifier,
                        password = snapshot.password,
                        confirmPassword = snapshot.confirmPassword,
                        displayName = snapshot.newStudentName,
                        faculty = snapshot.newStudentFaculty,
                    )
                ) {
                    is SignUpOutcome.Success -> {
                        if (outcome.needsEmailConfirmation) {
                            _uiState.update {
                                it.copy(
                                    isSigningIn = false,
                                    screenStep = AuthScreenStep.VerifyEmail,
                                    registeredEmail = outcome.email,
                                    verifyEmailCooldownSeconds = 60,
                                )
                            }
                            startVerifyEmailCooldown()
                        } else {
                            _uiState.update {
                                it.copy(
                                    isSigningIn = false,
                                    screenStep = AuthScreenStep.Password,
                                    setupSuccessNotice = true,
                                    password = "",
                                    confirmPassword = "",
                                    failure = null,
                                )
                            }
                        }
                    }

                    is SignUpOutcome.InvalidForm -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
                        outcome.nameError?.let { eventChannel.send(AuthEvent.GeneralMessage(it)) }
                            ?: outcome.passwordError?.let { eventChannel.send(AuthEvent.GeneralMessage(it)) }
                            ?: outcome.confirmError?.let { eventChannel.send(AuthEvent.GeneralMessage(it)) }
                    }

                    is SignUpOutcome.DomainRestriction -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
                        eventChannel.send(AuthEvent.GeneralMessage("Chỉ chấp nhận email sinh viên (@ut.edu.vn hoặc @uth.edu.vn)"))
                    }

                    is SignUpOutcome.Failure -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
                        eventChannel.send(AuthEvent.GeneralMessage(outcome.message))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
            }
        }
    }

    private fun submitForgotPassword() {
        if (_uiState.value.isSendingForgot) return
        val snapshot = _uiState.value
        val email = snapshot.forgotEmail.trim().ifEmpty { snapshot.identifier.trim() }
        _uiState.update { it.copy(isSendingForgot = true, forgotError = null, forgotSuccessNotice = false) }
        viewModelScope.launch {
            try {
                when (val outcome = forgotPassword(email)) {
                    ForgotPasswordOutcome.Success -> _uiState.update {
                        it.copy(isSendingForgot = false, forgotSuccessNotice = true)
                    }

                    is ForgotPasswordOutcome.InvalidEmail -> _uiState.update {
                        it.copy(isSendingForgot = false, forgotError = outcome.message)
                    }

                    ForgotPasswordOutcome.NetworkError -> _uiState.update {
                        it.copy(isSendingForgot = false, failure = AuthFailure.NetworkError)
                    }

                    ForgotPasswordOutcome.RateLimited -> _uiState.update {
                        it.copy(isSendingForgot = false, failure = AuthFailure.RateLimited)
                    }

                    is ForgotPasswordOutcome.Failure -> _uiState.update {
                        it.copy(isSendingForgot = false, forgotError = outcome.message)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSendingForgot = false, failure = AuthFailure.Unexpected) }
            }
        }
    }

    private fun submitVerificationRequest() {
        val snapshot = _uiState.value
        val studentId = snapshot.verificationStudentId.trim()
        if (studentId.isBlank()) return
        _uiState.update { it.copy(isSubmittingVerification = true, verificationNotice = null) }
        viewModelScope.launch {
            try {
                when (val outcome = submitVerification(studentId)) {
                    is SubmitVerificationOutcome.Success -> _uiState.update {
                        it.copy(
                            isSubmittingVerification = false,
                            verificationStatus = VerificationStatus.PENDING,
                            verificationNotice = "Yêu cầu xác minh MSSV $studentId đã được gửi thành công và đang chờ Ban quản trị duyệt.",
                        )
                    }

                    SubmitVerificationOutcome.AlreadyVerified -> _uiState.update {
                        it.copy(
                            isSubmittingVerification = false,
                            verificationNotice = "Tài khoản của bạn đã được xác minh trước đó.",
                        )
                    }

                    SubmitVerificationOutcome.AlreadyTaken -> _uiState.update {
                        it.copy(
                            isSubmittingVerification = false,
                            verificationNotice = "Mã số sinh viên $studentId đã được liên kết với tài khoản khác.",
                        )
                    }

                    is SubmitVerificationOutcome.InvalidStudentId -> _uiState.update {
                        it.copy(isSubmittingVerification = false, verificationNotice = outcome.message)
                    }

                    is SubmitVerificationOutcome.Failure -> _uiState.update {
                        it.copy(isSubmittingVerification = false, verificationNotice = outcome.message)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSubmittingVerification = false, verificationNotice = "Không thể gửi yêu cầu xác minh") }
            }
        }
    }

    private fun submitResetPassword() {
        val snapshot = _uiState.value
        _uiState.update { it.copy(isResettingPassword = true, resetPasswordError = null) }
        viewModelScope.launch {
            try {
                when (val outcome = completePasswordReset(snapshot.resetNewPassword, snapshot.resetConfirmPassword)) {
                    CompletePasswordResetOutcome.Success -> _uiState.update {
                        it.copy(
                            isResettingPassword = false,
                            resetSuccessNotice = true,
                            screenStep = AuthScreenStep.Password,
                            resetNewPassword = "",
                            resetConfirmPassword = "",
                        )
                    }

                    CompletePasswordResetOutcome.TokenExpired -> _uiState.update {
                        it.copy(isResettingPassword = false, failure = AuthFailure.TokenExpired)
                    }

                    is CompletePasswordResetOutcome.InvalidForm -> _uiState.update {
                        it.copy(
                            isResettingPassword = false,
                            resetPasswordError = outcome.passwordError ?: outcome.confirmError,
                        )
                    }

                    is CompletePasswordResetOutcome.Failure -> _uiState.update {
                        it.copy(isResettingPassword = false, resetPasswordError = outcome.message)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isResettingPassword = false, failure = AuthFailure.Unexpected) }
            }
        }
    }

    private fun resendVerificationEmail() {
        val snapshot = _uiState.value
        val targetEmail = snapshot.registeredEmail.ifBlank { snapshot.forgotEmail.ifBlank { snapshot.identifier } }.trim()
        if (targetEmail.isBlank()) {
            viewModelScope.launch {
                eventChannel.send(AuthEvent.GeneralMessage("Chưa có email sinh viên để gửi xác thực"))
            }
            return
        }
        if (snapshot.verifyEmailCooldownSeconds > 0) return
        startVerifyEmailCooldown()
        viewModelScope.launch {
            try {
                when (val outcome = forgotPassword(targetEmail)) {
                    ForgotPasswordOutcome.Success -> Unit

                    is ForgotPasswordOutcome.InvalidEmail -> {
                        _uiState.update { it.copy(verifyEmailCooldownSeconds = 0) }
                        cooldownJob?.cancel()
                        eventChannel.send(AuthEvent.GeneralMessage(outcome.message))
                    }

                    ForgotPasswordOutcome.NetworkError -> {
                        _uiState.update { it.copy(verifyEmailCooldownSeconds = 0) }
                        cooldownJob?.cancel()
                        _uiState.update { it.copy(failure = AuthFailure.NetworkError) }
                    }

                    ForgotPasswordOutcome.RateLimited -> {
                        _uiState.update { it.copy(failure = AuthFailure.RateLimited) }
                    }

                    is ForgotPasswordOutcome.Failure -> {
                        _uiState.update { it.copy(verifyEmailCooldownSeconds = 0) }
                        cooldownJob?.cancel()
                        eventChannel.send(AuthEvent.GeneralMessage(outcome.message))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(verifyEmailCooldownSeconds = 0) }
                cooldownJob?.cancel()
                _uiState.update { it.copy(failure = AuthFailure.Unexpected) }
            }
        }
    }

    private fun startVerifyEmailCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (sec in 60 downTo 1) {
                _uiState.update { it.copy(verifyEmailCooldownSeconds = sec) }
                delay(1000L)
            }
            _uiState.update { it.copy(verifyEmailCooldownSeconds = 0) }
        }
    }

    private fun handleDeepLink(token: String, type: String) {
        if (type == "recovery" && token.isNotBlank()) {
            supabaseRestClient.recoveryToken = token
            _uiState.update {
                it.copy(
                    screenStep = AuthScreenStep.ResetPassword,
                    failure = null,
                    resetSuccessNotice = false,
                )
            }
        }
    }

    private fun googleSignIn(idToken: String, rawNonce: String) {
        val snapshot = _uiState.value
        if (!googleEnabled || snapshot.isSigningIn || snapshot.student != null) return
        _uiState.update {
            it.copy(isSigningInWithGoogle = true, failure = null, googleVerification = null)
        }
        viewModelScope.launch {
            try {
                when (val outcome = signInWithGoogle(idToken, rawNonce)) {
                    is SignInWithGoogleResult.Success -> _uiState.update {
                        it.copy(isSigningInWithGoogle = false, identifier = "", password = "")
                    }

                    is SignInWithGoogleResult.VerificationPending -> _uiState.update {
                        it.copy(
                            isSigningInWithGoogle = false,
                            googleVerification = GoogleVerificationNotice(outcome.email, outcome.displayName),
                            verificationStudentId = "",
                        )
                    }

                    SignInWithGoogleResult.Rejected, SignInWithGoogleResult.Unsupported -> _uiState.update {
                        it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleRejected)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleRejected) }
            }
        }
    }

    private fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isRestoring || snapshot.isSigningIn || snapshot.student != null) return
        _uiState.update { it.copy(isSigningIn = true, failure = null) }
        viewModelScope.launch {
            try {
                when (val outcome = signIn(snapshot.identifier, snapshot.password, snapshot.rememberMe)) {
                    is SignInResult.Success -> _uiState.update {
                        it.copy(isSigningIn = false, password = "", identifierError = null, passwordError = null)
                    }

                    is SignInResult.InvalidForm -> _uiState.update {
                        it.copy(isSigningIn = false, identifierError = outcome.identifier, passwordError = outcome.password)
                    }

                    is SignInResult.VerificationRequired -> _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            failure = AuthFailure.VerificationRequired,
                            registeredEmail = outcome.email,
                        )
                    }

                    SignInResult.InvalidCredentials -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.InvalidCredentials) }
                        eventChannel.send(AuthEvent.InvalidCredentials)
                    }

                    SignInResult.Failure -> _uiState.update {
                        it.copy(isSigningIn = false, failure = AuthFailure.Unexpected)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
            }
        }
    }

    /**
     * Pill VI/EN đổi ngôn ngữ **thật**: ghi vào store rồi báo Route dựng lại activity,
     * vì Locale áp ở `MainActivity.attachBaseContext` chứ không phải trong Compose.
     *
     * Bấm lại đúng ngôn ngữ đang bật thì chỉ giữ nguyên uiState — `recreate()` có giá
     * của nó (mất trạng thái form) nên không phát event thừa.
     */
    private fun selectLanguage(languageCode: String) {
        val tag = if (languageCode == LANGUAGE_CODE_ENGLISH) LANGUAGE_TAG_ENGLISH else LANGUAGE_TAG_VIETNAMESE
        _uiState.update { it.copy(selectedLanguage = languageCode) }
        if (tag == appliedLanguageTag) return
        viewModelScope.launch {
            updateSettings.setLanguageTag(tag)
            appliedLanguageTag = tag
            eventChannel.send(AuthEvent.LanguageChanged)
        }
    }
}

/** Mã hiển thị trên pill ("VI"/"EN") tương ứng với BCP-47 tag đã lưu. */
private fun String.toLanguageCode(): String = when (this) {
    LANGUAGE_TAG_ENGLISH -> LANGUAGE_CODE_ENGLISH
    else -> LANGUAGE_CODE_VIETNAMESE
}

/**
 * Cài đặt không trạng thái, chỉ phục vụ constructor tiện lợi của [AuthViewModel] trong
 * test: đọc trả mặc định, ghi bỏ qua — test assert `uiState`, không cần giá trị đã lưu.
 */
private object NoopSettingsRepository : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> = flowOf(AppSettings())
    override fun currentLanguageTag(): String = LANGUAGE_TAG_VIETNAMESE
    override suspend fun updateTuitionReminder(enabled: Boolean): Unit = Unit
    override suspend fun updateTrainingPointNotif(enabled: Boolean): Unit = Unit
    override suspend fun setLanguageTag(languageTag: String): Unit = Unit
    override suspend fun setThemeMode(themeMode: ThemeMode): Unit = Unit
}
