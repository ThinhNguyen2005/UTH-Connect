# Phụ lục Phase 1: baseline tại a54f5b6

Dữ liệu từ config/detekt/baseline.xml là số ID, không phải số finding hay dòng nguồn. Một ID có thể áp dụng nhiều vị trí hoặc đã lỗi thời.

## Theo tầng

- presentation khác/Compose: 177 ID
- data: 117 ID
- test: 68 ID
- ViewModel: 59 ID
- domain: 52 ID
- khác: 2 ID
- DI: 1 ID

## 15 file có nhiều ID nhất

| File | ID | Đường dẫn tương ứng |
| --- | ---: | --- |
| AuthViewModel.kt | 30 | app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt |
| BooksScreen.kt | 24 | app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksScreen.kt |
| TicketsScreen.kt | 23 | app/src/main/java/com/giathinh/uthconnect/presentation/tickets/TicketsScreen.kt |
| SupabaseBookListingRepository.kt | 21 | app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt |
| FirestoreEventSeeder.kt | 19 | app/src/main/java/com/giathinh/uthconnect/data/seed/FirestoreEventSeeder.kt |
| EventsScreen.kt | 16 | app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsScreen.kt |
| ProfileScreen.kt | 15 | app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt |
| UtilitiesScreen.kt | 13 | app/src/main/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesScreen.kt |
| HomeScreen.kt | 12 | app/src/main/java/com/giathinh/uthconnect/presentation/home/HomeScreen.kt |
| SupabaseRestClient.kt | 12 | app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseRestClient.kt |
| AnnouncementScreen.kt | 12 | app/src/main/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementScreen.kt |
| SupabaseAuthRepository.kt | 10 | app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt |
| AuthScreen.kt | 10 | app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt |
| ScheduleViewModel.kt | 9 | app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt |
| InMemoryLostFoundRepository.kt | 9 | app/src/main/java/com/giathinh/uthconnect/data/repository/InMemoryLostFoundRepository.kt |

## Toàn bộ ID baseline

Các ID được sao chép nguyên văn để rà soát; không có thông tin số dòng.

```
ComplexCondition:BookListingUseCases.kt$BookListingUseCases$listing.id.isBlank() || listing.title.isBlank() || listing.title.length > 160 || listing.subject.isBlank() || listing.subject.length > 100 || listing.sellerName.isBlank() || listing.sellerName.length > 100 || listing.description.length > 2000 || listing.contact.length > 254 || !isValidContact(listing.contact) || (listing.dealType == BookDealType.SALE && (listing.priceVnd ?: 0) <= 0) || (listing.dealType == BookDealType.EXCHANGE && listing.priceVnd != null)
ComplexCondition:EventsScreen.kt$validationError == null && start != null && end != null && count != null
ComplexCondition:LostFoundViewModel.kt$LostFoundViewModel$titleError != null || descriptionError != null || locationError != null || phoneError != null
ComplexCondition:ScheduleViewModel.kt$ScheduleViewModel$state.value.isRegistrationsLoading || state.value.registrationsLoadFailed || event.status != EventStatus.PUBLISHED || now >= event.startAt || now >= event.endAt
ComplexCondition:SignUpUseCase.kt$SignUpUseCase$nameError != null || emailError != null || passwordError != null || confirmError != null
CyclomaticComplexMethod:AuthViewModel.kt$AuthViewModel$fun onAction(action: AuthAction)
CyclomaticComplexMethod:BookListingUseCases.kt$BookListingUseCases$suspend fun save(listing: BookListing): Result<Unit>
CyclomaticComplexMethod:CompletePasswordResetUseCase.kt$CompletePasswordResetUseCase$suspend operator fun invoke(email: String, otp: String, password: String, confirmPassword: String): CompletePasswordResetOutcome
CyclomaticComplexMethod:CreateHousingPostViewModel.kt$CreateHousingPostViewModel$private fun handleFieldChange(action: CreateHousingPostAction)
CyclomaticComplexMethod:FirestoreEventRepository.kt$FirestoreEventRepository$private fun DocumentSnapshot.toEvent(): Event?
CyclomaticComplexMethod:LostFoundViewModel.kt$LostFoundViewModel$fun onAction(action: LostFoundAction)
CyclomaticComplexMethod:PasswordChangeValidator.kt$PasswordChangeValidator$fun validate(old: String, updated: String, confirmation: String): PasswordValidation
CyclomaticComplexMethod:ProfileViewModel.kt$ProfileViewModel$fun onAction(action: ProfileAction)
CyclomaticComplexMethod:ScheduleViewModel.kt$ScheduleViewModel$fun onAction(action: ScheduleAction)
CyclomaticComplexMethod:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$override suspend fun saveListing(listing: BookListing): Result<Unit>
CyclomaticComplexMethod:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$internal fun validateImageBytes(bytes: ByteArray): Pair<ByteArray, String>
LongMethod:AuthViewModel.kt$AuthViewModel$fun onAction(action: AuthAction)
LongMethod:AuthViewModel.kt$AuthViewModel$private fun submitFirstTimeSetup()
LongMethod:FirestoreEventSeeder.kt$FirestoreEventSeeder$suspend fun seedSampleEvents(firestore: FirebaseFirestore): Result<Int>
LongMethod:HomeScreen.kt$private fun LazyListScope.homeFeed( state: HomeUiState, onOpen: (String) -> Unit, onOpenFilter: () -> Unit, onOpenArchive: () -> Unit, onSelectCategory: (AnnouncementCategory?) -> Unit, )
LongMethod:LostFoundViewModel.kt$LostFoundViewModel$fun onAction(action: LostFoundAction)
LongMethod:LostFoundViewModel.kt$LostFoundViewModel$private fun submitReport()
LongMethod:ScheduleViewModel.kt$ScheduleViewModel$fun onAction(action: ScheduleAction)
LongMethod:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$override suspend fun saveListing(listing: BookListing): Result<Unit>
LongParameterList:AuthViewModel.kt$AuthViewModel$( private val signIn: SignInUseCase, private val sessions: AccessStudentSessionUseCase, private val signInWithGoogle: SignInWithGoogleUseCase, private val signUp: SignUpUseCase, private val forgotPassword: ForgotPasswordUseCase, private val completePasswordReset: CompletePasswordResetUseCase, private val submitVerification: SubmitVerificationRequestUseCase, private val supabaseRestClient: SupabaseRestClient, private val getSettings: GetSettingsUseCase, private val updateSettings: UpdateSettingsUseCase, )
MagicNumber:AuthViewModel.kt$AuthViewModel$1000L
MagicNumber:AuthViewModel.kt$AuthViewModel$12
MagicNumber:AuthViewModel.kt$AuthViewModel$1_000
MagicNumber:AuthViewModel.kt$AuthViewModel$429
MagicNumber:AuthViewModel.kt$AuthViewModel$60
MagicNumber:AuthViewModel.kt$AuthViewModel$8
MagicNumber:BookListingUseCases.kt$BookListingUseCases$100
MagicNumber:BookListingUseCases.kt$BookListingUseCases$160
MagicNumber:BookListingUseCases.kt$BookListingUseCases$2000
MagicNumber:BookListingUseCases.kt$BookListingUseCases$254
MagicNumber:BookListingUseCases.kt$BookListingUseCases$8
MagicNumber:BooksScreen.kt$8
MagicNumber:CompletePasswordResetUseCase.kt$CompletePasswordResetUseCase$64
MagicNumber:CompletePasswordResetUseCase.kt$CompletePasswordResetUseCase$8
MagicNumber:DemoAuthRepository.kt$DemoAuthRepository$16
MagicNumber:DemoAuthRepository.kt$DemoAuthRepository$180_000
MagicNumber:DemoAuthRepository.kt$DemoAuthRepository$256
MagicNumber:EventsScreen.kt$120
MagicNumber:EventsScreen.kt$5L
MagicNumber:EventsScreen.kt$60
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$11
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$17
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$18
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$200
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$21
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$3
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$30
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$42
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$5
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$58
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$60
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$7
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$72
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$80
MagicNumber:FirestoreEventSeeder.kt$FirestoreEventSeeder$9
MagicNumber:ForgotPasswordUseCase.kt$ForgotPasswordUseCase$12
MagicNumber:ForgotPasswordUseCase.kt$ForgotPasswordUseCase$8
MagicNumber:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$6
MagicNumber:InMemoryTimetableRepository.kt$InMemoryTimetableRepository$6
MagicNumber:InMemoryTimetableRepository.kt$InMemoryTimetableRepository$7
MagicNumber:PasswordChangeValidator.kt$PasswordChangeValidator$64
MagicNumber:PasswordChangeValidator.kt$PasswordChangeValidator$8
MagicNumber:QrFrameDecoder.kt$QrFrameDecoder$0xFF
MagicNumber:QrFrameDecoder.kt$QrFrameDecoder$16
MagicNumber:QrFrameDecoder.kt$QrFrameDecoder$24
MagicNumber:QrFrameDecoder.kt$QrFrameDecoder$8
MagicNumber:ScheduleUiState.kt$SchedulePeriod.Companion$12
MagicNumber:ScheduleUiState.kt$SchedulePeriod.Companion$18
MagicNumber:ScheduleViewModel.kt$ScheduleViewModel$60_000
MagicNumber:SecureSessionStore.kt$SecureSessionStore$128
MagicNumber:SignUpUseCase.kt$SignUpUseCase$12
MagicNumber:SignUpUseCase.kt$SignUpUseCase$6
MagicNumber:SignUpUseCase.kt$SignUpUseCase$8
MagicNumber:StaticTuitionRepository.kt$StaticTuitionRepository$3
MagicNumber:SubmitVerificationRequestUseCase.kt$SubmitVerificationRequestUseCase$32
MagicNumber:SubmitVerificationRequestUseCase.kt$SubmitVerificationRequestUseCase$5
MagicNumber:SupabaseAuthRepository.kt$SupabaseAuthRepository$12
MagicNumber:SupabaseAuthRepository.kt$SupabaseAuthRepository$400
MagicNumber:SupabaseAuthRepository.kt$SupabaseAuthRepository$401
MagicNumber:SupabaseAuthRepository.kt$SupabaseAuthRepository$8
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$10
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$11
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$12
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$3
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$8
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$8192
MagicNumber:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$9
MagicNumber:SupabaseLostFoundRepository.kt$SupabaseLostFoundRepository$20
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$1000L
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$15_000
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$200
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$299
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$30_000L
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$3600L
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$400
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$401
MagicNumber:SupabaseRestClient.kt$SupabaseRestClient$404
MagicNumber:SupabaseUniversityRepositories.kt$SupabaseAnnouncementRepository$5
MagicNumber:TuitionUiState.kt$TuitionUiState$3
MatchingDeclarationName:AnnouncementNavigation.kt$AnnouncementDetailDestination
MatchingDeclarationName:AttendanceNavigation.kt$AttendanceDestination
MatchingDeclarationName:Dimens.kt$UthDimens
MatchingDeclarationName:EventsNavigation.kt$EventsDestination
MatchingDeclarationName:LostFoundNavigation.kt$LostFoundDestination
MatchingDeclarationName:ScannerRoute.kt$ScannerDestination
MatchingDeclarationName:UthStatusBadge.kt$BadgeTone
MatchingDeclarationName:UtilitiesNavigation.kt$UtilitiesDestination
MaxLineLength:AnnouncementScreen.kt$private
MaxLineLength:AnnouncementViewModelTest.kt$AnnouncementViewModelTest.FakeRepository$override suspend fun homeFeed(): Result<HomeFeed>
MaxLineLength:AttendanceRosterDataModule.kt$AttendanceRosterDataModule$@Binds abstract fun bindRosterRepository(implementation: SupabaseAttendanceRosterRepository): AttendanceRosterRepository
MaxLineLength:AttendanceScreen.kt$Text(entry.displayName.ifBlank { entry.studentId }, style = MaterialTheme.typography.titleMedium)
MaxLineLength:AttendanceScreen.kt$Text(stringResource(if (exporting) R.string.attendance_exporting else R.string.attendance_export))
MaxLineLength:AttendanceScreen.kt$color = if (entry.checkedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
MaxLineLength:AttendanceScreen.kt$stringResource(if (entry.checkedIn) R.string.attendance_present else R.string.attendance_missing)
MaxLineLength:AttendanceViewModelTest.kt$AttendanceViewModelTest$val vm = AttendanceViewModel(GetAttendanceRosterUseCase(FakeRepository(Result.success(roster))), ExportAttendanceCsvUseCase())
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest$// ───────────────────────────────────────── 5. DOUBLE SUBMIT & RACE CONDITION RESILIENCE ─────────────────────────────────────────
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest$// ───────────────────────────────────────── 6. BIOMETRIC STATE TRANSITIONS & RESET ─────────────────────────────────────────
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest$// ───────────────────────────────────────── 7. ACCESSIBILITY CONTRACT (MIN 48DP) ─────────────────────────────────────────
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest$assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1234567890123"))
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest$fun
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest.CountingAuthRepository$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:AuthAdversarialStressTest.kt$AuthAdversarialStressTest.CountingAuthRepository$val valid = (identifier.trim() == student.studentId || identifier.trim().equals(student.email, ignoreCase = true)) && password == "correct_password"
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest$// ───────────────────────────────────────── TIER 1: DOMAIN VALIDATION & CRYPTO ─────────────────────────────────────────
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest$// ───────────────────────────────────────── TIER 2: VIEWMODEL STATE MACHINE ─────────────────────────────────────────
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest$// ───────────────────────────────────────── TIER 3: UI SEMANTICS & ACCESSIBILITY CONTRACT ─────────────────────────────────────────
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest$// ───────────────────────────────────────── TIER 4: HERMETIC E2E USER JOURNEYS ─────────────────────────────────────────
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest.FakeAuthRepository$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest.FakeAuthRepository$override suspend fun findStudent(studentId: String): Result<StudentProfile?>
MaxLineLength:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest.FakeAuthRepository$val valid = (identifier.trim() == student.studentId || normalizedEmail.equals(student.email, ignoreCase = true)) && password == "secret"
MaxLineLength:AuthRepository.kt$AuthRepository$suspend
MaxLineLength:AuthScreen.kt$border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthOutlineButtonBorder))
MaxLineLength:AuthScreen.kt$border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.SubtleDivider))
MaxLineLength:AuthScreen.kt$border = BorderStroke(UthDimens.Size.Stroke, extended.info.copy(alpha = UthDimens.Opacity.AuthInfoBannerBorder))
MaxLineLength:AuthScreen.kt$border = BorderStroke(UthDimens.Size.Stroke, extended.success.copy(alpha = UthDimens.Opacity.AuthSuccessBannerBorder))
MaxLineLength:AuthScreen.kt$unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthInputUnfocusedBorder)
MaxLineLength:AuthScreen.kt$visualTransformation = if (uiState.resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
MaxLineLength:AuthSecurityAdversarialChallengeTest.kt$AuthSecurityAdversarialChallengeTest$fun
MaxLineLength:AuthSecurityAdversarialChallengeTest.kt$AuthSecurityAdversarialChallengeTest$val passedDomainCheck = email.endsWith("@ut.edu.vn", ignoreCase = true) || email.endsWith("@uth.edu.vn", ignoreCase = true)
MaxLineLength:AuthSecurityAdversarialChallengeTest.kt$AuthSecurityAdversarialChallengeTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String)
MaxLineLength:AuthSecurityAdversarialChallengeTest.kt$AuthSecurityAdversarialChallengeTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:AuthViewModel.kt$AuthViewModel$AuthAction.DismissForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = false, forgotError = null) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$AuthAction.ToggleResetPasswordVisibility -> _uiState.update { it.copy(resetPasswordVisible = !it.resetPasswordVisible) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$_uiState.update { it.copy(isSubmittingVerification = false, verificationNotice = "Không thể gửi yêu cầu xác minh") }
MaxLineLength:AuthViewModel.kt$AuthViewModel$completePasswordReset
MaxLineLength:AuthViewModel.kt$AuthViewModel$eventChannel.send(AuthEvent.GeneralMessage("Chỉ chấp nhận email sinh viên (@ut.edu.vn hoặc @uth.edu.vn)"))
MaxLineLength:AuthViewModel.kt$AuthViewModel$eventChannel.send(AuthEvent.GeneralMessage("Đã yêu cầu gửi lại email xác minh. Vui lòng kiểm tra hộp thư."))
MaxLineLength:AuthViewModel.kt$AuthViewModel$is AuthAction.ForgotEmailChanged -> _uiState.update { it.copy(forgotEmail = action.value, forgotError = null) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$is AuthAction.ResetConfirmPasswordChanged -> _uiState.update { it.copy(resetConfirmPassword = action.value, resetPasswordError = null) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$is AuthAction.ResetNewPasswordChanged -> _uiState.update { it.copy(resetNewPassword = action.value, resetPasswordError = null) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$is AuthAction.VerificationStudentIdChanged -> _uiState.update { it.copy(verificationStudentId = action.value) }
MaxLineLength:AuthViewModel.kt$AuthViewModel$it.copy(isResettingPassword = false, resetPasswordError = "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới.")
MaxLineLength:AuthViewModel.kt$AuthViewModel$it.copy(isSendingForgot = false, forgotError = "Đã gửi yêu cầu quá nhiều lần. Vui lòng thử lại sau.")
MaxLineLength:AuthViewModel.kt$AuthViewModel$it.copy(isSigningIn = false, identifierError = outcome.identifier, passwordError = outcome.password)
MaxLineLength:AuthViewModel.kt$AuthViewModel$resetConfirmPassword = if (action.step == AuthScreenStep.ResetPassword) it.resetConfirmPassword else ""
MaxLineLength:AuthViewModel.kt$AuthViewModel$submitVerification
MaxLineLength:AuthViewModel.kt$AuthViewModel$val isUtEmail = (id.endsWith("@ut.edu.vn", ignoreCase = true) || id.endsWith("@uth.edu.vn", ignoreCase = true)) && id.indexOf('@') > 0
MaxLineLength:AuthViewModel.kt$AuthViewModel$val targetEmail = snapshot.registeredEmail.ifBlank { snapshot.forgotEmail.ifBlank { snapshot.identifier } }.trim()
MaxLineLength:AuthViewModel.kt$AuthViewModel$verificationNotice = "Yêu cầu xác minh MSSV $studentId đã được gửi thành công và đang chờ Ban quản trị duyệt."
MaxLineLength:AuthViewModel.kt$AuthViewModel.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:AuthViewModelTest.kt$AuthViewModelTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
MaxLineLength:AuthViewModelTest.kt$AuthViewModelTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:BooksScreen.kt$AsyncImage(it, listing.title, modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
MaxLineLength:BooksScreen.kt$Button
MaxLineLength:BooksScreen.kt$FilterChip(condition == value, { condition = value }, label = { Text(stringResource(value.label)) })
MaxLineLength:BooksScreen.kt$Text(listing.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
MaxLineLength:BooksScreen.kt$Text(stringResource(if (listing.status == BookListingStatus.AVAILABLE) R.string.books_mark_sold else R.string.books_mark_available))
MaxLineLength:BooksScreen.kt$Text(stringResource(if (value == BookDealType.SALE) R.string.books_filter_sale else R.string.books_filter_exchange))
MaxLineLength:BooksScreen.kt$actionLabel = if (uiState.listings.isEmpty()) null else stringResource(R.string.books_clear_filters)
MaxLineLength:BooksScreen.kt$body = stringResource(if (uiState.currentUserUid == null) R.string.books_auth_required_body else R.string.books_error_body)
MaxLineLength:BooksScreen.kt$body = stringResource(if (uiState.listings.isEmpty()) R.string.books_empty_body else R.string.books_no_results_body)
MaxLineLength:BooksScreen.kt$confirmButton = { TextButton(onClick = { showContactError = false }) { Text(stringResource(R.string.books_close)) } }
MaxLineLength:BooksScreen.kt$dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.books_cancel)) } }
MaxLineLength:BooksScreen.kt$dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.books_cancel)) } }
MaxLineLength:BooksScreen.kt$errorMessage = if (descriptionError) stringResource(R.string.books_error_description_invalid) else null
MaxLineLength:BooksScreen.kt$if (listing.dealType == BookDealType.SALE) priceText(listing.priceVnd) else stringResource(R.string.books_exchange)
MaxLineLength:BooksScreen.kt$if (photoTooLarge) Text(stringResource(R.string.books_photo_too_large), color = MaterialTheme.colorScheme.error)
MaxLineLength:BooksScreen.kt$private
MaxLineLength:BooksScreen.kt$text = stringResource(if (listing.status == BookListingStatus.AVAILABLE) R.string.books_available else R.string.books_sold)
MaxLineLength:BooksScreen.kt$title = stringResource(if (uiState.currentUserUid == null) R.string.books_auth_required_title else R.string.books_error_title)
MaxLineLength:BooksScreen.kt$title = stringResource(if (uiState.listings.isEmpty()) R.string.books_empty_title else R.string.books_no_results_title)
MaxLineLength:BooksScreen.kt$uriHandler.openUri(if ('@' in contact) "mailto:${Uri.encode(contact)}" else "tel:${Uri.encode(contact)}")
MaxLineLength:BooksScreen.kt$val stableFormId = editingId ?: (newListingId ?: rememberSaveable { UUID.randomUUID().toString() }.also { newListingId = it })
MaxLineLength:BooksScreen.kt$val target = if (listing.status == BookListingStatus.AVAILABLE) BookListingStatus.SOLD else BookListingStatus.AVAILABLE
MaxLineLength:BooksUiState.kt$BooksUiState$(maximumPrice.toLongOrNull()?.let { maximum -> listing.priceVnd?.let { it <= maximum } ?: false } != false)
MaxLineLength:BooksUiState.kt$BooksUiState$(minimumPrice.toLongOrNull()?.let { minimum -> listing.priceVnd?.let { it >= minimum } ?: false } != false)
MaxLineLength:BooksUiState.kt$BooksUiState$filter == BooksFilter.ALL || (filter == BooksFilter.SALE && listing.dealType == BookDealType.SALE)
MaxLineLength:BooksUiStateTest.kt$BooksUiStateTest$private
MaxLineLength:BooksViewModel.kt$BooksViewModel$is BooksAction.SetPriceRange -> _uiState.update { it.copy(minimumPrice = action.minimum, maximumPrice = action.maximum) }
MaxLineLength:BooksViewModelTest.kt$BooksViewModelTest.<no name provided>$override suspend fun deleteListing(id: String)
MaxLineLength:BooksViewModelTest.kt$BooksViewModelTest.<no name provided>$override suspend fun saveListing(listing: BookListing)
MaxLineLength:BooksViewModelTest.kt$BooksViewModelTest.<no name provided>$override suspend fun updateStatus(id: String, status: BookListingStatus)
MaxLineLength:CampusReference.kt$CampusReference$private
MaxLineLength:CampusesUiState.kt$CampusesUiState$DepartmentInfo("Trung tâm Thông tin - Thư viện", "028 3899 1373", "thuvien@ut.edu.vn", "Tòa nhà Thư viện - CS1")
MaxLineLength:CheckInUseCaseTest.kt$CheckInUseCaseTest.Companion$val profile = StudentProfile("organizer-1", "demo@ut.edu.vn", "Demo", "A", "IT", "IT", role = UserRole.ORGANIZER)
MaxLineLength:CompletePasswordResetUseCase.kt$CompletePasswordResetUseCase$suspend operator
MaxLineLength:CompletePasswordResetUseCaseTest.kt$CompletePasswordResetUseCaseTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
MaxLineLength:CompletePasswordResetUseCaseTest.kt$CompletePasswordResetUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:CompletePasswordResetUseCaseTest.kt$CompletePasswordResetUseCaseTest.<no name provided>$override suspend fun completePasswordReset(email: String, otp: String, newPassword: String): Result<Unit>
MaxLineLength:DemoAccountStore.kt$SharedPreferencesDemoAccountStore$override fun putAll(values: Map<String, String>): Boolean
MaxLineLength:DemoAuthRepository.kt$DemoAuthRepository$override suspend
MaxLineLength:DemoAuthRepository.kt$DemoAuthRepository$private
MaxLineLength:Event.kt$EventStatus.Companion$fun fromString(value: String?): EventStatus
MaxLineLength:EventRepository.kt$EventRepository$suspend
MaxLineLength:EventRepository.kt$EventRepository$suspend fun cancelCurrentRegistration(eventId: String): Result<Unit>
MaxLineLength:EventRepository.kt$EventRepository$suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit>
MaxLineLength:EventRepository.kt$EventRepository$suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit>
MaxLineLength:EventsScreen.kt$EventStatus.HAPPENING
MaxLineLength:EventsScreen.kt$EventStatus.PUBLISHED
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = campus, onValueChange = { campus = it }, label = { Text("Cơ sở") }, singleLine = true)
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Sức chứa (0 = không giới hạn)") }, singleLine = true)
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = endsAt, onValueChange = { endsAt = it }, label = { Text("Kết thúc (dd/MM/yyyy HH:mm)") }, singleLine = true)
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Địa điểm") }, singleLine = true)
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = startsAt, onValueChange = { startsAt = it }, label = { Text("Bắt đầu (dd/MM/yyyy HH:mm)") }, singleLine = true)
MaxLineLength:EventsScreen.kt$OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên sự kiện") }, singleLine = true)
MaxLineLength:EventsScreen.kt$Text("Trạng thái: ${event.status.name} · Đã đăng ký: ${event.registeredCount}${if (event.capacity > 0) "/${event.capacity}" else ""}")
MaxLineLength:EventsScreen.kt$internal fun editableDateTime(millis: Long, zone: ZoneId): String
MaxLineLength:EventsScreen.kt$internal fun parseEditableDateTime(text: String): LocalDateTime?
MaxLineLength:EventsScreen.kt$modifier = Modifier.fillMaxWidth().heightIn(max = UthDimens.Spacing.Xxl * 15).verticalScroll(rememberScrollState())
MaxLineLength:EventsViewModel.kt$EventsViewModel$fun saveEvent(eventId: String?, draft: EventDraft)
MaxLineLength:EventsViewModel.kt$EventsViewModel$fun setStatus(eventId: String, status: EventStatus)
MaxLineLength:FakeNotificationRepository.kt$FakeNotificationRepository$summary = "Khoa CNTT tổ chức workshop 'Nghề nghiệp CNTT 2025' với sự tham gia của doanh nghiệp FPT, Viettel."
MaxLineLength:FakeScheduleRepository.kt$FakeScheduleRepository$override
MaxLineLength:FirestoreEventSeeder.kt$FirestoreEventSeeder$"description" to "Buổi giao lưu chia sẻ về các hoạt động thiện nguyện, chiến dịch Mùa hè xanh và rèn luyện kỹ năng mềm."
MaxLineLength:FirestoreEventSeeder.kt$FirestoreEventSeeder$"description" to "Học cách xây dựng ứng dụng với Gemini API và Firebase Cloud Functions cùng diễn giả GDG."
MaxLineLength:FirestoreEventSeeder.kt$FirestoreEventSeeder$"description" to "Hội thao truyền thống sinh viên toàn trường. Thi đấu bóng đá, cầu lông, kéo co và cờ vua."
MaxLineLength:ForgotPasswordUseCase.kt$ForgotPasswordUseCase$// Anti-enumeration: treat unknown backend responses as Success unless explicit network/rate limit
MaxLineLength:ForgotPasswordUseCase.kt$ForgotPasswordUseCase$return ForgotPasswordOutcome.InvalidEmail("Vui lòng nhập đúng email sinh viên (@ut.edu.vn hoặc @uth.edu.vn)")
MaxLineLength:ForgotPasswordUseCaseTest.kt$ForgotPasswordUseCaseTest$val repo = createAuthRepo { Result.failure(IllegalStateException("429 Too Many Requests (rate limit exceeded)")) }
MaxLineLength:ForgotPasswordUseCaseTest.kt$ForgotPasswordUseCaseTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
MaxLineLength:ForgotPasswordUseCaseTest.kt$ForgotPasswordUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:GoogleIdTokenSource.kt$GoogleIdTokenSource$"Explicit sign-in failed [${failure.javaClass.simpleName}]: ${failure.message}; trying GetGoogleIdOption fallback"
MaxLineLength:GoogleNonce.kt$internal
MaxLineLength:HomeScreen.kt$private
MaxLineLength:HomeViewModelTest.kt$HomeViewModelTest.FakeRepository$override suspend fun homeFeed(): Result<HomeFeed>
MaxLineLength:HousingDetailScreen.kt$private
MaxLineLength:HousingViewModelTest.kt$HousingViewModelTest$private
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Bên trong có CCCD mang tên Phạm Quốc Hưng, một ít tiền mặt và thẻ ATM Vietcombank. Ai nhặt được liên hệ giúp mình, xin cảm ơn và hậu tạ."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Bóp viết vải màu xám bên trong có 3 cây bút bi, 1 bút chì và 1 thẻ sinh viên tên Đặng Ngọc Ánh. Mình đang giữ tại phòng CTSV."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Chùm chìa khóa xe máy có gắn móc khóa gấu bông màu nâu và thẻ chung cư, rơi dọc đoạn đường từ nhà xe sinh viên ra cổng số 2 CS2 Thủ Đức."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Máy tính bỏ túi còn dán sticker UTH Connect ở mặt lưng, để quên ở bàn cuối phòng E102 cơ sở 3 sau ca thi giữa kỳ môn Toán cao cấp."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Mình để quên thẻ sinh viên mang tên Nguyễn Văn An, MSSV 2251050012 ở bàn dãy 3 phòng B204 cơ sở 1 Võ Oanh sau giờ học Kỹ thuật phần mềm."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Nhặt được thẻ gửi xe máy sinh viên rơi tại bãi xe tầng trệt khu C Võ Oanh. Đã gửi lại bác bảo vệ chốt cổng chính quản lý."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Sạc màu trắng 67W Apple kèm dây cáp C to C, để quên ở khu tự học tầng 2 thư viện CS1 khoảng 11h trưa nay. Ai thấy cho mình xin lại với ạ."
MaxLineLength:InMemoryLostFoundRepository.kt$InMemoryLostFoundRepository$description = "Tai nghe nhặt được tại dãy ghế chờ hành lang lầu 3 nhà A CS1 sau tiết học sáng. Ai là chủ nhân vui lòng liên hệ xác nhận tên bluetooth."
MaxLineLength:InMemoryTimetableRepository.kt$InMemoryTimetableRepository$state.update { it.copy(deadlines = it.deadlines + Deadline(UUID.randomUUID().toString(), course, title.trim(), dueAt)) }
MaxLineLength:MockTicketRepository.kt$MockTicketRepository$override suspend
MaxLineLength:PasswordChangeValidatorTest.kt$PasswordChangeValidatorTest$@Test fun `accept strong distinct password`()
MaxLineLength:PasswordChangeValidatorTest.kt$PasswordChangeValidatorTest$@Test fun `reject whitespace and excessive password`()
MaxLineLength:PostNotification.kt$NotificationCategory.Companion$fun fromString(value: String?): NotificationCategory
MaxLineLength:ProfileScreen.kt$)
MaxLineLength:ProfileScreen.kt$if (student.currentAddress.isBlank()) stringResource(R.string.auth_contact_not_set) else student.currentAddress
MaxLineLength:ProfileScreen.kt$if (student.personalEmail.isBlank()) stringResource(R.string.auth_contact_not_set) else student.personalEmail
MaxLineLength:ProfileScreen.kt$onClick = { onAction(if (state.biometricEnabled) ProfileAction.DisableBiometric else ProfileAction.RequestBiometricEnroll) }
MaxLineLength:ProfileScreen.kt$private
MaxLineLength:ProfileViewModel.kt$ProfileViewModel$ProfileAction.Back -> _uiState.update { it.copy(page = ProfilePage.Overview, passwordForm = PasswordForm(), failure = null) }
MaxLineLength:ProfileViewModel.kt$ProfileViewModel$_uiState.update { it.copy(isLoggingOut = false, confirmLogout = true, failure = ProfileFailure.LogoutFailed) }
MaxLineLength:ProfileViewModel.kt$ProfileViewModel$_uiState.update { it.copy(notice = if (enable) ProfileNotice.BiometricEnabled else ProfileNotice.BiometricDisabled) }
MaxLineLength:ProfileViewModel.kt$ProfileViewModel$it.copy(page = ProfilePage.Overview, passwordForm = PasswordForm(), notice = ProfileNotice.PasswordChanged)
MaxLineLength:ProfileViewModelTest.kt$ProfileViewModelTest.FakeAuth$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:ProfileViewModelTest.kt$ProfileViewModelTest.FakeAuth$override suspend fun updateContact(studentId: String, phone: String, personalEmail: String, currentAddress: String)
MaxLineLength:ReportHousingListingUseCase.kt$ReportHousingListingUseCase$suspend operator fun invoke(listingId: String, reason: String): Result<Unit>
MaxLineLength:ScannerRoute.kt$ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
MaxLineLength:ScannerScreen.kt$TextButton(onClick = { onAction(ScannerAction.ClearScan) }) { Text(stringResource(R.string.scanner_scan_again)) }
MaxLineLength:ScannerViewModel.kt$ScannerViewModel$is ScannerAction.CameraPermissionChanged -> state.update { it.copy(cameraPermissionGranted = action.granted, cameraError = false) }
MaxLineLength:ScannerViewModel.kt$ScannerViewModel$state.update { it.copy(code = "", result = null, cameraError = false, scanSession = it.scanSession + 1) }
MaxLineLength:ScannerViewModelTest.kt$ScannerViewModelTest.Companion$val profile = StudentProfile("organizer-1", "demo@ut.edu.vn", "Demo", "A", "IT", "IT", role = UserRole.ORGANIZER)
MaxLineLength:ScheduleScreen.kt$1 -> rawEvents.filter { it.title.contains("Workshop", ignoreCase = true) || it.title.contains("Hội thảo", ignoreCase = true) || it.title.contains("Seminar", ignoreCase = true) || it.title.contains("AI", ignoreCase = true) }
MaxLineLength:ScheduleScreen.kt$2 -> rawEvents.filter { it.title.contains("Hội thao", ignoreCase = true) || it.title.contains("Thể thao", ignoreCase = true) || it.title.contains("Sports", ignoreCase = true) || it.title.contains("Festival", ignoreCase = true) }
MaxLineLength:ScheduleScreen.kt$3 -> rawEvents.filter { it.title.contains("Doanh nghiệp", ignoreCase = true) || it.title.contains("Logistics", ignoreCase = true) || it.title.contains("Career", ignoreCase = true) || it.title.contains("Đoàn", ignoreCase = true) }
MaxLineLength:ScheduleScreen.kt$eventStartsAt = "${startAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${startAt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
MaxLineLength:ScheduleViewModel.kt$ScheduleViewModel$it.copy(showForm = true, date = it.now.toLocalDate().plusDays(1).toString(), titleError = false, dateError = false, timeError = false)
MaxLineLength:ScheduleViewModel.kt$ScheduleViewModel$registration = if (event.id in registeredEventIds) EventRegistration.REGISTERED else EventRegistration.NOT_REGISTERED
MaxLineLength:ScheduleViewModel.kt$ScheduleViewModel$state.update { it.copy(showForm = !valid, dateError = !valid, timeError = !valid, title = if (valid) "" else it.title) }
MaxLineLength:ScheduleViewModelTest.kt$EmptyEventRepository$override suspend fun registerEvent(eventId: String, uid: String, studentId: String, studentName: String): Result<Unit>
MaxLineLength:ScheduleViewModelTest.kt$EmptyTicketRepository$override suspend fun getTickets(studentId: String)
MaxLineLength:ScheduleViewModelTest.kt$ScheduleViewModelTest$val vm = ScheduleViewModel(TimetableUseCase(repository), EmptyEventRepository, EmptyTicketRepository, EmptySessionRepository, clock)
MaxLineLength:ScheduleViewModelTest.kt$ScheduleViewModelTest.<no name provided>$override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit>
MaxLineLength:ScheduleViewModelTest.kt$ScheduleViewModelTest.<no name provided>$override suspend fun registerEvent(eventId: String, uid: String, studentId: String, studentName: String): Result<Unit>
MaxLineLength:SecureSessionStore.kt$SecureSessionStore$val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal("$userId\n$refreshToken".toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
MaxLineLength:SecureSessionStore.kt$SecureSessionStore$val parts = cipher.doFinal(bytes.copyOfRange(IV_LENGTH, bytes.size)).toString(Charsets.UTF_8).split('\n', limit = 2)
MaxLineLength:ServerErrorCode.kt$ServerErrorCode.Companion$fun
MaxLineLength:SignInUseCaseTest.kt$SignInUseCaseTest.<no name provided>$override suspend
MaxLineLength:SignInUseCaseTest.kt$SignInUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:SignInWithGoogleUseCase.kt$SignInWithGoogleUseCase$suspend operator
MaxLineLength:SignInWithGoogleUseCaseTest.kt$SignInWithGoogleUseCaseTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String)
MaxLineLength:SignInWithGoogleUseCaseTest.kt$SignInWithGoogleUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:SignInWithGoogleUseCaseTest.kt$SignInWithGoogleUseCaseTest.FakeAuth$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String)
MaxLineLength:SignUpUseCaseTest.kt$SignUpUseCaseTest$Result.failure(IllegalStateException("DOMAIN_RESTRICTION: Only @ut.edu.vn or @uth.edu.vn addresses are allowed"))
MaxLineLength:SignUpUseCaseTest.kt$SignUpUseCaseTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
MaxLineLength:SignUpUseCaseTest.kt$SignUpUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:SignUpUseCaseTest.kt$SignUpUseCaseTest.<no name provided>$override suspend fun signUp(email: String, password: String, displayName: String, faculty: String): Result<SignUpResult>
MaxLineLength:StudentContactValidator.kt$StudentContactValidator$emailError = if (email.isNotBlank() && (email.length > 254 || !emailPattern.matches(email.trim()))) ContactFieldError.InvalidEmail else null
MaxLineLength:StudentContactValidator.kt$StudentContactValidator$phoneError = if (phone.isNotBlank() && !phonePattern.matches(phone.trim())) ContactFieldError.InvalidPhone else null
MaxLineLength:StudentContactValidatorTest.kt$StudentContactValidatorTest$@Test fun `accept vietnam phone and personal email`()
MaxLineLength:SubmitVerificationRequestUseCaseTest.kt$SubmitVerificationRequestUseCaseTest.<no name provided>$override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
MaxLineLength:SubmitVerificationRequestUseCaseTest.kt$SubmitVerificationRequestUseCaseTest.<no name provided>$override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
MaxLineLength:SubmitVerificationRequestUseCaseTest.kt$SubmitVerificationRequestUseCaseTest.<no name provided>$override suspend fun submitVerificationRequest(studentId: String): Result<VerificationRequestResult>
MaxLineLength:SupabaseAuthRepository.kt$SupabaseAuthRepository$"profiles?select=student_id,student_id_verified,email,display_name,class_name,major,faculty,avatar_url,phone_number,personal_email,current_address,role&id=eq.${urlEncode(uuid(client.userId))}"
MaxLineLength:SupabaseAuthRepository.kt$SupabaseAuthRepository$override suspend
MaxLineLength:SupabaseAuthRepository.kt$SupabaseAuthRepository$val isUtEmail = email.endsWith("@ut.edu.vn", ignoreCase = true) || email.endsWith("@uth.edu.vn", ignoreCase = true)
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$Log.w("SupabaseBookListing", "Compensating storage cleanup failed for $newlyUploadedPath", cleanupFailure)
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$condition = runCatching { BookCondition.valueOf(string("condition").orEmpty()) }.getOrDefault(BookCondition.GOOD)
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$dealType = runCatching { BookDealType.valueOf(string("deal_type").orEmpty()) }.getOrDefault(BookDealType.SALE)
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$override suspend
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$status = runCatching { BookListingStatus.valueOf(string("status").orEmpty()) }.getOrDefault(BookListingStatus.AVAILABLE)
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$bytes[8] == 'W'.code.toByte()
MaxLineLength:SupabaseBookListingRepository.kt$SupabaseBookListingRepository.Companion$internal suspend
MaxLineLength:SupabaseBookListingRepositoryTest.kt$SupabaseBookListingRepositoryTest$val pngBytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(), 0, 0, 0, 0)
MaxLineLength:SupabaseEventRepository.kt$SupabaseEventRepository$client.table("registrations?select=status&event_id=eq.${urlEncode(uuid(eventId))}&user_id=eq.${urlEncode(uuid(uid))}")
MaxLineLength:SupabaseEventRepository.kt$SupabaseEventRepository$override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit>
MaxLineLength:SupabaseRestClient.kt$SupabaseRestClient$suspend
MaxLineLength:SupabaseRestClient.kt$SupabaseRestClient$val response = send(if (redirectTo.isBlank()) "/auth/v1/signup" else "/auth/v1/signup?redirect_to=$encodedRedirect", "POST", payload, bearer = null).jsonObject
MaxLineLength:SupabaseUniversityRepositories.kt$SupabaseAnnouncementRepository$blocks = notice.content.takeIf { it.isNotBlank() }?.let { listOf(AnnouncementBlock.Paragraph(it)) }.orEmpty()
MaxLineLength:SupabaseUniversityRepositories.kt$SupabaseStudentGradeRepository$emit(if (totalCredits == 0) 0f else grades.sumOf { it.totalScore4.toDouble() * it.credits }.div(totalCredits).toFloat())
MaxLineLength:SupabaseUniversityRepositories.kt$private
MaxLineLength:TicketRepository.kt$TicketRepository$suspend fun checkIn(identifier: String, organizerId: String): Result<TicketCheckInOutcome>
MaxLineLength:TicketRepository.kt$TicketRepository$suspend fun checkIn(token: String): Result<Unit>
MaxLineLength:TicketRepository.kt$TicketRepository$suspend fun issueCheckInToken(eventId: String): Result<String>
MaxLineLength:TicketsScreen.kt$.
MaxLineLength:TicketsScreen.kt$item { Button(onClick = { onAction(TicketsAction.OpenScanner) }) { Text(stringResource(R.string.scanner_open)) } }
MaxLineLength:TicketsScreen.kt$private
MaxLineLength:TicketsViewModel.kt$TicketsViewModel$TicketsAction.OpenScanner -> if (_uiState.value.isOrganizer) viewModelScope.launch { _events.send(TicketsEvent.OpenScanner) }
MaxLineLength:TicketsViewModel.kt$TicketsViewModel$_uiState.update { it.copy(studentName = profile.displayName, isOrganizer = profile.role == UserRole.ORGANIZER) }
MaxLineLength:TicketsViewModelTest.kt$TicketsViewModelTest$Ticket("t2", STUDENT_ID, "e2", "Giao lưu CLB", "Nguyễn Tất Thành", "15/11/2026", TicketStatus.CHECKED_IN, "654 321")
MaxLineLength:TicketsViewModelTest.kt$TicketsViewModelTest.FakeSessionRepository$StudentSession(isRestoring = false, profile = StudentProfile(STUDENT_ID, "s@uth.edu.vn", "Sinh viên", "CN23A", "CNTT", "CNTT"))
MaxLineLength:TicketsViewModelTest.kt$TicketsViewModelTest.FakeTicketRepository$override suspend fun checkIn(identifier: String, organizerId: String): Result<TicketCheckInOutcome>
MaxLineLength:TimetableUseCase.kt$TimetableUseCase.Companion$fun isUrgent(dueAt: LocalDateTime, now: LocalDateTime): Boolean
MaxLineLength:TimetableUseCase.kt$TimetableUseCase.Companion$fun tomorrow(value: Timetable, today: LocalDate)
MaxLineLength:TrainingPointScreen.kt$text = stringResource(R.string.training_point_criteria_score, criteria.score, criteria.maxScore)
MaxLineLength:UnavailableBookListingRepository.kt$UnavailableBookListingRepository$override suspend fun deleteListing(id: String): Result<Unit>
MaxLineLength:UnavailableBookListingRepository.kt$UnavailableBookListingRepository$override suspend fun saveListing(listing: BookListing): Result<Unit>
MaxLineLength:UnavailableBookListingRepository.kt$UnavailableBookListingRepository$override suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit>
MaxLineLength:UnavailableUniversityRepositories.kt$UnavailableTrainingPointRepository$override suspend fun getTrainingPoint(): Result<TrainingPointInfo>
MaxLineLength:UnavailableUniversityRepositories.kt$UnavailableTuitionRepository$override suspend fun getTuition(): Result<TuitionInfo>
MaxLineLength:UthDestination.kt$UthDestination.Companion$fun
MaxLineLength:UtilitiesScreen.kt$private
MaxLineLength:UtilitiesViewModelTest.kt$UtilitiesViewModelTest.FakeUtilityRepository$override suspend fun modules(): Result<List<UtilityModule>>
ReturnCount:AuthE2EWorkflowTest.kt$AuthE2EWorkflowTest.FakeAuthRepository$override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile>
ReturnCount:ChangePasswordUseCase.kt$ChangePasswordUseCase$suspend operator fun invoke(old: String, updated: String, confirmed: String): ChangePasswordResult
ReturnCount:CheckInUseCase.kt$CheckInUseCase$suspend operator fun invoke(rawCode: String): CheckInResult
ReturnCount:CompletePasswordResetUseCase.kt$CompletePasswordResetUseCase$suspend operator fun invoke(email: String, otp: String, password: String, confirmPassword: String): CompletePasswordResetOutcome
ReturnCount:FirebaseAuthRepository.kt$FirebaseAuthRepository$private suspend fun resolveEmail(identifier: String): String?
ReturnCount:GoogleIdTokenSource.kt$GoogleIdTokenSource$suspend fun request(activity: Activity, webClientId: String): GoogleIdTokenResult
ReturnCount:QrFrameDecoder.kt$QrFrameDecoder$fun compactLuminance(plane: ByteBuffer, width: Int, height: Int, rowStride: Int): ByteArray
ReturnCount:QrScannerRoute.kt$private fun decodeFrame(frame: ImageProxy): String?
ReturnCount:ReportItemBottomSheet.kt$private fun compressRawBytes( rawBytes: ByteArray, maxDimension: Int, quality: Int, ): ImageProcessResult
ReturnCount:ScheduleViewModel.kt$ScheduleViewModel$fun onAction(action: ScheduleAction)
ReturnCount:SupabaseMappers.kt$internal fun JsonObject.toDomainTicket( studentId: String, userId: String, attendedEventIds: Set<String>, ): Ticket?
ReturnCount:UpdateStudentContactUseCase.kt$UpdateStudentContactUseCase$suspend operator fun invoke(phone: String, email: String, address: String): UpdateContactResult
SwallowedException:GoogleIdTokenSource.kt$GoogleIdTokenSource$cancelled: GetCredentialCancellationException
SwallowedException:SecureSessionStore.kt$SecureSessionStore$error: Exception
TooGenericExceptionCaught:AttendanceViewModel.kt$AttendanceViewModel$error: Exception
TooGenericExceptionCaught:EventsViewModel.kt$EventsViewModel$error: Exception
TooGenericExceptionCaught:ScheduleViewModel.kt$ScheduleViewModel$error: Exception
TooGenericExceptionCaught:SecureSessionStore.kt$SecureSessionStore$error: Exception
TooGenericExceptionCaught:SupabaseAttendanceRosterRepository.kt$SupabaseAttendanceRosterRepository$error: Exception
TooGenericExceptionCaught:SupabaseAuthRepository.kt$SupabaseAuthRepository$e: Exception
TooGenericExceptionCaught:SupabaseAuthRepository.kt$SupabaseAuthRepository$error: Exception
TooGenericExceptionCaught:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$cleanupFailure: Exception
TooGenericExceptionCaught:SupabaseBookListingRepository.kt$SupabaseBookListingRepository$error: Exception
TooGenericExceptionCaught:SupabaseGoogleExchange.kt$SupabaseGoogleExchange$error: Exception
TooGenericExceptionCaught:SupabaseHousingRepository.kt$SupabaseHousingRepository$error: Exception
TooGenericExceptionCaught:SuspendResults.kt$error: Exception
TooManyFunctions:AnnouncementScreen.kt$com.giathinh.uthconnect.presentation.announcement.AnnouncementScreen.kt
TooManyFunctions:AuthScreen.kt$com.giathinh.uthconnect.presentation.auth.AuthScreen.kt
TooManyFunctions:AuthViewModel.kt$AuthViewModel : ViewModel
TooManyFunctions:CreateHousingPostScreen.kt$com.giathinh.uthconnect.presentation.housing.create.CreateHousingPostScreen.kt
TooManyFunctions:CreateHousingPostViewModel.kt$CreateHousingPostViewModel : ViewModel
TooManyFunctions:DemoAuthRepository.kt$DemoAuthRepository : AuthRepositoryStudentContactRepository
TooManyFunctions:EventRepository.kt$EventRepository
TooManyFunctions:HomeScreen.kt$com.giathinh.uthconnect.presentation.home.HomeScreen.kt
TooManyFunctions:HousingDetailScreen.kt$com.giathinh.uthconnect.presentation.housing.detail.HousingDetailScreen.kt
TooManyFunctions:HousingScreen.kt$com.giathinh.uthconnect.presentation.housing.HousingScreen.kt
TooManyFunctions:ProfileScreen.kt$com.giathinh.uthconnect.presentation.profile.ProfileScreen.kt
TooManyFunctions:QrScannerScreen.kt$com.giathinh.uthconnect.presentation.scanner.QrScannerScreen.kt
TooManyFunctions:ScheduleScreen.kt$com.giathinh.uthconnect.presentation.schedule.ScheduleScreen.kt
TooManyFunctions:SupabaseAuthRepository.kt$SupabaseAuthRepository : AuthRepositoryStudentContactRepository
TooManyFunctions:SupabaseEventRepository.kt$SupabaseEventRepository : EventRepository
TooManyFunctions:SupabaseHousingRepository.kt$SupabaseHousingRepository : HousingRepository
TooManyFunctions:SupabaseRestClient.kt$SupabaseRestClient
TooManyFunctions:TicketsScreen.kt$com.giathinh.uthconnect.presentation.tickets.TicketsScreen.kt
TooManyFunctions:UtilitiesScreen.kt$com.giathinh.uthconnect.presentation.utilities.UtilitiesScreen.kt
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "content dark", showBackground = true) @Composable private fun AnnouncementContentDarkPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "content fontScale 130%", showBackground = true, fontScale = 1.3f) @Composable private fun AnnouncementContentFontScalePreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "content light", showBackground = true) @Composable private fun AnnouncementContentPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "error dark", showBackground = true) @Composable private fun AnnouncementErrorDarkPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "error light", showBackground = true) @Composable private fun AnnouncementErrorPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "loading dark", showBackground = true) @Composable private fun AnnouncementLoadingDarkPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "loading light", showBackground = true) @Composable private fun AnnouncementLoadingPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "missing dark", showBackground = true) @Composable private fun AnnouncementMissingDarkPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "missing light", showBackground = true) @Composable private fun AnnouncementMissingPreview()
UnusedPrivateMember:AnnouncementScreen.kt$@Preview(name = "short content", showBackground = true) @Composable private fun AnnouncementShortContentPreview()
UnusedPrivateMember:AttendanceScreen.kt$@Preview(showBackground = true) @Composable private fun AttendancePreview()
UnusedPrivateMember:AuthScreen.kt$@Preview(name = "Dark Mode - Password (Hello Again)", showBackground = true) @Composable private fun PreviewAuthDarkPassword()
UnusedPrivateMember:AuthScreen.kt$@Preview(name = "Light Mode - Identifier Step", showBackground = true) @Composable private fun PreviewAuthIdentifierStep()
UnusedPrivateMember:AuthScreen.kt$@Preview(name = "Light Mode - Password (Hello Again)", showBackground = true) @Composable private fun PreviewAuthLightPassword()
UnusedPrivateMember:BooksScreen.kt$@Preview(showBackground = true, widthDp = 360) @Composable private fun BooksScreenPreview()
UnusedPrivateMember:CampusesScreen.kt$@Preview(name = "CampusesScreen Dark", showBackground = true) @Composable private fun CampusesScreenDarkPreview()
UnusedPrivateMember:CampusesScreen.kt$@Preview(name = "CampusesScreen Light", showBackground = true) @Composable private fun CampusesScreenPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "content dark", showBackground = true) @Composable private fun HomeContentDarkPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "content fontScale 130%", showBackground = true, fontScale = 1.3f) @Composable private fun HomeContentFontScalePreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "content light", showBackground = true) @Composable private fun HomeContentPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "empty dark", showBackground = true) @Composable private fun HomeEmptyDarkPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "empty light", showBackground = true) @Composable private fun HomeEmptyPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "error dark", showBackground = true) @Composable private fun HomeErrorDarkPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "error light", showBackground = true) @Composable private fun HomeErrorPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "loading dark", showBackground = true) @Composable private fun HomeLoadingDarkPreview()
UnusedPrivateMember:HomeScreen.kt$@Preview(name = "loading light", showBackground = true) @Composable private fun HomeLoadingPreview()
UnusedPrivateMember:HousingDetailScreen.kt$@Preview(name = "detail content dark", showBackground = true) @Composable private fun DetailContentDarkPreview()
UnusedPrivateMember:HousingDetailScreen.kt$@Preview(name = "detail content light", showBackground = true) @Composable private fun DetailContentLightPreview()
UnusedPrivateMember:HousingLocationPickerScreen.kt$@Preview @Composable private fun PickerBottomPanelPreview()
UnusedPrivateMember:HousingScreen.kt$@Preview @Composable private fun HousingScreenContentPreview()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile contact dark", showBackground = true) @Composable private fun ProfileContactDark()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile contact light", showBackground = true) @Composable private fun ProfileContactLight()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile content dark", showBackground = true) @Composable private fun ProfileContentDark()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile content light", showBackground = true) @Composable private fun ProfileContentLight()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile empty light", showBackground = true) @Composable private fun ProfileEmptyLight()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile logout confirmation", showBackground = true) @Composable private fun ProfileLogoutDialog()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile organizer light", showBackground = true) @Composable private fun ProfileOrganizerLight()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile password dark", showBackground = true) @Composable private fun ProfilePasswordDark()
UnusedPrivateMember:ProfileScreen.kt$@Preview(name = "Profile password light", showBackground = true) @Composable private fun ProfilePasswordLight()
UnusedPrivateMember:QrScannerScreen.kt$@Preview(name = "Scanner live light", showBackground = true) @Composable private fun ScannerLivePreview()
UnusedPrivateMember:QrScannerScreen.kt$@Preview(name = "Scanner permission light", showBackground = true) @Composable private fun ScannerPermissionPreview()
UnusedPrivateMember:QrScannerScreen.kt$@Preview(name = "Scanner success light", showBackground = true) @Composable private fun ScannerSuccessPreview()
UnusedPrivateMember:ScannerScreen.kt$@Preview(showBackground = true) @Composable private fun ScannerPreview()
UnusedPrivateMember:ScheduleScreen.kt$@Preview(showBackground = true) @Composable private fun DarkPreview()
UnusedPrivateMember:ScheduleScreen.kt$@Preview(showBackground = true) @Composable private fun LightPreview()
UnusedPrivateMember:SettingsScreen.kt$@Preview(name = "SettingsScreen Dark", showBackground = true) @Composable private fun SettingsScreenDarkPreview()
UnusedPrivateMember:SettingsScreen.kt$@Preview(name = "SettingsScreen Light", showBackground = true) @Composable private fun SettingsScreenPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR checked-in dark", showBackground = true) @Composable private fun TicketQrCheckedInDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR checked-in light", showBackground = true) @Composable private fun TicketQrCheckedInPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR dark", showBackground = true) @Composable private fun TicketQrDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR expired dark", showBackground = true) @Composable private fun TicketQrExpiredDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR expired light", showBackground = true) @Composable private fun TicketQrExpiredPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR font scale 1.3", showBackground = true, fontScale = 1.3f) @Composable private fun TicketQrFontScalePreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR missing dark", showBackground = true) @Composable private fun TicketQrMissingDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "QR missing light", showBackground = true) @Composable private fun TicketQrMissingPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets empty dark", showBackground = true) @Composable private fun TicketsEmptyDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets empty light", showBackground = true) @Composable private fun TicketsEmptyPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets error dark", showBackground = true) @Composable private fun TicketsErrorDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets error light", showBackground = true) @Composable private fun TicketsErrorPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets font scale 1.3", showBackground = true, fontScale = 1.3f) @Composable private fun TicketsFontScalePreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets loading dark", showBackground = true) @Composable private fun TicketsLoadingDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets loading light", showBackground = true) @Composable private fun TicketsLoadingPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets past dark", showBackground = true) @Composable private fun TicketsPastDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets past light", showBackground = true) @Composable private fun TicketsPastPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets upcoming dark", showBackground = true) @Composable private fun TicketsDarkPreview()
UnusedPrivateMember:TicketsScreen.kt$@Preview(name = "Tickets upcoming light", showBackground = true) @Composable private fun TicketsPreview()
UnusedPrivateMember:TrainingPointScreen.kt$@Preview(name = "TrainingPointScreen Dark", showBackground = true) @Composable private fun TrainingPointScreenDarkPreview()
UnusedPrivateMember:TrainingPointScreen.kt$@Preview(name = "TrainingPointScreen Light", showBackground = true) @Composable private fun TrainingPointScreenPreview()
UnusedPrivateMember:TuitionScreen.kt$@Preview(name = "TuitionScreen Dark", showBackground = true) @Composable private fun TuitionScreenDarkPreview()
UnusedPrivateMember:TuitionScreen.kt$@Preview(name = "TuitionScreen Light", showBackground = true) @Composable private fun TuitionScreenPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "content dark", showBackground = true) @Composable private fun UtilitiesContentDarkPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "content large text", showBackground = true, fontScale = 1.5f) @Composable private fun UtilitiesContentLargeTextPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "content light", showBackground = true) @Composable private fun UtilitiesContentPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "content narrow large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f) @Composable private fun UtilitiesContentNarrowLargeTextPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "content narrow", showBackground = true, widthDp = 320, heightDp = 640) @Composable private fun UtilitiesContentNarrowPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "empty dark", showBackground = true) @Composable private fun UtilitiesEmptyDarkPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "empty light", showBackground = true) @Composable private fun UtilitiesEmptyPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "error dark", showBackground = true) @Composable private fun UtilitiesErrorDarkPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "error light", showBackground = true) @Composable private fun UtilitiesErrorPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "loading dark", showBackground = true) @Composable private fun UtilitiesLoadingDarkPreview()
UnusedPrivateMember:UtilitiesScreen.kt$@Preview(name = "loading light", showBackground = true) @Composable private fun UtilitiesLoadingPreview()
UnusedPrivateProperty:ScheduleViewModelTest.kt$ScheduleViewModelTest$private val emptyEvents = object : EventRepository { override fun getEvents(campus: String?): Flow<List<Event>> = flowOf(emptyList()) override suspend fun getEventDetail(eventId: String): Result<Event?> = Result.success(null) override suspend fun registerEvent(eventId: String, uid: String, studentId: String, studentName: String): Result<Unit> = Result.failure(UnsupportedOperationException()) override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = Result.failure(UnsupportedOperationException()) override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = flowOf(false) }
UnusedPrivateProperty:SupabaseMappersTest.kt$SupabaseMappersTest$val base = """ { "event_id": "33333333-3333-4333-8333-333333333333", "events": { "title": "Sự kiện" } } """
UseCheckOrError:FirestoreEventRepository.kt$FirestoreEventRepository$throw IllegalStateException("Sự kiện không tồn tại")
UseCheckOrError:FirestoreEventRepository.kt$FirestoreEventRepository$throw IllegalStateException("Sự kiện đã hết số lượng đăng ký")
UseCheckOrError:ProfileViewModelTest.kt$ProfileViewModelTest.FakeSession$throw IllegalStateException("Simulated profile storage failure")
UseCheckOrError:ProfileViewModelTest.kt$ProfileViewModelTest.FakeSession$throw IllegalStateException("Simulated storage failure")
```

## Hướng xử lý

Ưu tiên AuthViewModel, BooksScreen, TicketsScreen và SupabaseBookListingRepository theo số ID; đối chiếu test cùng khu vực trước khi refactor. Xác minh vị trí và baseline lỗi thời bằng lần quét không baseline trước khi chốt số cảnh báo sửa theo từng lô.
