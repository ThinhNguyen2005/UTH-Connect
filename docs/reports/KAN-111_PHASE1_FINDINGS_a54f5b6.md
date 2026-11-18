# KAN-111 Phase 1 - Detekt findings

Commit a54f5b6. Detekt 1.23.8 CLI, Compose plugin 0.6.8, project config and default rules, without baseline. Inputs: app/src/main/java, app/src/test/java, app/src/androidTest/java. No type resolution.
CLI exit code: 2. Finding lines: 566. Baseline-backed Gradle task: exit 0, no unbaselined findings. Baseline contains 476 IDs; IDs are not equivalent to finding lines.

## Rule counts

- MaxLineLength: 269
- MagicNumber: 123
- UnusedPrivateMember: 81
- TooGenericExceptionCaught: 21
- TooManyFunctions: 19
- CyclomaticComplexMethod: 11
- ReturnCount: 11
- LongMethod: 8
- MatchingDeclarationName: 8
- UseCheckOrError: 5
- ComplexCondition: 4
- SwallowedException: 3
- UnusedPrivateProperty: 2
- LongParameterList: 1

## Source area counts

- presentation other: 201
- data: 161
- test: 70
- ViewModel: 66
- domain: 58
- androidTest: 7
- other: 2
- data DI: 1

## Top 15 files

- 35: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt
- 34: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt
- 25: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt
- 23: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt
- 22: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt
- 21: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt
- 19: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt
- 19: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt
- 18: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt
- 16: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt
- 15: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt
- 14: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt
- 12: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt
- 11: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt
- 9: G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt

## All findings

```text
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:22:7: Class 'DemoAuthRepository' with '11' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FirestoreEventRepository.kt:132:34: The function toEvent appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:22:7: Class 'SupabaseAuthRepository' with '14' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:56:26: The function saveListing is too long (64). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:56:26: The function saveListing appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:198:22: The function validateImageBytes appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseEventRepository.kt:19:7: Class 'SupabaseEventRepository' with '12' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:78:7: Class 'SupabaseHousingRepository' with '28' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:26:7: Class 'SupabaseRestClient' with '29' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:12:17: The function seedSampleEvents is too long (76). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\EventRepository.kt:11:11: Interface 'EventRepository' with '12' functions detected. Defined threshold inside interfaces is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:15:17: The function save appears to be too complex based on Cyclomatic Complexity (complexity: 16). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:16:13: This condition is too complex (14). Defined complexity threshold for conditions is set to '4' [ComplexCondition]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCase.kt:16:26: The function invoke appears to be too complex based on Cyclomatic Complexity (complexity: 17). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCase.kt:63:13: This condition is too complex (4). Defined complexity threshold for conditions is set to '4' [ComplexCondition]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\validation\PasswordChangeValidator.kt:15:9: The function validate appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt' with '34' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt' with '16' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:46:40: The constructor(signIn: SignInUseCase, sessions: AccessStudentSessionUseCase, signInWithGoogle: SignInWithGoogleUseCase, signUp: SignUpUseCase, forgotPassword: ForgotPasswordUseCase, completePasswordReset: CompletePasswordResetUseCase, submitVerification: SubmitVerificationRequestUseCase, supabaseRestClient: SupabaseRestClient, getSettings: GetSettingsUseCase, updateSettings: UpdateSettingsUseCase) has too many parameters. The current threshold is set to 7. [LongParameterList]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:141:9: The function onAction is too long (79). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:296:17: The function submitFirstTimeSetup is too long (60). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:141:9: The function onAction appears to be too complex based on Cyclomatic Complexity (complexity: 40). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:46:7: Class 'AuthViewModel' with '13' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:339:21: This condition is too complex (4). Defined complexity threshold for conditions is set to '4' [ComplexCondition]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:138:27: The function homeFeed is too long (60). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt' with '38' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\create\CreateHousingPostScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\create\CreateHousingPostScreen.kt' with '19' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\create\CreateHousingPostViewModel.kt:129:17: The function handleFieldChange appears to be too complex based on Cyclomatic Complexity (complexity: 15). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\create\CreateHousingPostViewModel.kt:50:7: Class 'CreateHousingPostViewModel' with '15' functions detected. Defined threshold inside classes is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\detail\HousingDetailScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\detail\HousingDetailScreen.kt' with '28' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\HousingScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\HousingScreen.kt' with '32' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\lostfound\LostFoundViewModel.kt:47:9: The function onAction is too long (146). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\lostfound\LostFoundViewModel.kt:233:17: The function submitReport is too long (71). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\lostfound\LostFoundViewModel.kt:47:9: The function onAction appears to be too complex based on Cyclomatic Complexity (complexity: 28). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt' with '20' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModel.kt:46:9: The function onAction appears to be too complex based on Cyclomatic Complexity (complexity: 20). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerScreen.kt' with '11' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt' with '13' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:146:9: The function onAction is too long (117). The maximum length is 60. [LongMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:146:9: The function onAction appears to be too complex based on Cyclomatic Complexity (complexity: 37). Defined complexity threshold for methods is set to '15' [CyclomaticComplexMethod]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:167:21: This condition is too complex (5). Defined complexity threshold for conditions is set to '4' [ComplexCondition]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt' with '53' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:1:1: File 'G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt' with '23' functions detected. Defined threshold inside files is set to '11' [TooManyFunctions]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\GoogleIdTokenSource.kt:64:18: The caught exception is swallowed. The original exception could be lost. [SwallowedException]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\GoogleIdTokenSource.kt:92:18: The caught exception is swallowed. The original exception could be lost. [SwallowedException]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SecureSessionStore.kt:43:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SecureSessionStore.kt:43:18: The caught exception is swallowed. The original exception could be lost. [SwallowedException]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAttendanceRosterRepository.kt:20:14: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:54:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:95:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:134:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:119:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:124:30: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseGoogleExchange.kt:57:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:121:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:132:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:150:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:171:18: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:246:14: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:369:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:406:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseHousingRepository.kt:481:14: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SuspendResults.kt:10:10: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceViewModel.kt:48:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsViewModel.kt:117:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsViewModel.kt:141:22: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:192:30: The caught exception is too generic. Prefer catching specific exceptions to the case that is currently handled. [TooGenericExceptionCaught]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\core\designsystem\component\UthStatusBadge.kt:31:12: The file name 'UthStatusBadge' does not match the name of the single top-level declaration 'BadgeTone'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\core\designsystem\theme\Dimens.kt:14:8: The file name 'Dimens' does not match the name of the single top-level declaration 'UthDimens'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementNavigation.kt:15:12: The file name 'AnnouncementNavigation' does not match the name of the single top-level declaration 'AnnouncementDetailDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceNavigation.kt:8:26: The file name 'AttendanceNavigation' does not match the name of the single top-level declaration 'AttendanceDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsNavigation.kt:15:27: The file name 'EventsNavigation' does not match the name of the single top-level declaration 'EventsDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\lostfound\LostFoundNavigation.kt:10:13: The file name 'LostFoundNavigation' does not match the name of the single top-level declaration 'LostFoundDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerRoute.kt:29:27: The file name 'ScannerRoute' does not match the name of the single top-level declaration 'ScannerDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesNavigation.kt:17:18: The file name 'UtilitiesNavigation' does not match the name of the single top-level declaration 'UtilitiesDestination'. [MatchingDeclarationName]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\di\AttendanceRosterDataModule.kt:13:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAccountStore.kt:26:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:56:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:85:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:77:61: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:77:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\DemoAuthRepository.kt:109:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FakeNotificationRepository.kt:115:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FakeScheduleRepository.kt:160:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FakeScheduleRepository.kt:163:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FirebaseAuthRepository.kt:88:25: Function resolveEmail has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FirestoreEventRepository.kt:66:17: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\FirestoreEventRepository.kt:80:17: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\GoogleIdTokenSource.kt:46:17: Function request has 4 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\GoogleIdTokenSource.kt:74:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\GoogleNonce.kt:11:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:25:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:36:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:47:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:58:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:70:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:81:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:92:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:103:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryLostFoundRepository.kt:129:81: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryTimetableRepository.kt:37:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryTimetableRepository.kt:23:40: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\InMemoryTimetableRepository.kt:23:58: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\MockTicketRepository.kt:25:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SecureSessionStore.kt:28:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SecureSessionStore.kt:40:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SecureSessionStore.kt:39:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:22:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:23:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:24:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:25:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:26:67: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\StaticTuitionRepository.kt:27:75: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:76:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:79:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:83:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:144:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:148:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:189:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:32:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:32:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:41:29: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:41:48: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:195:29: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseAuthRepository.kt:195:48: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:125:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:133:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:180:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:181:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:188:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:207:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:210:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:211:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:232:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:200:35: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:207:110: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:210:122: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:211:27: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:211:60: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:211:93: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:211:127: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepository.kt:219:36: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseEventRepository.kt:54:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseEventRepository.kt:65:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseMappers.kt:107:25: Function toDomainTicket has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:136:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:243:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:59:37: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:59:60: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:268:41: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:269:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:276:43: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:276:48: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:278:28: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:278:33: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:300:41: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:301:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:305:28: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:305:33: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:305:50: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:335:30: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:346:28: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:346:33: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:390:41: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:400:28: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:400:33: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:412:68: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:415:71: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:424:37: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:424:60: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:457:90: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:458:90: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:481:41: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:482:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:492:43: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:492:48: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:494:28: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseRestClient.kt:494:33: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseUniversityRepositories.kt:42:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseUniversityRepositories.kt:88:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseUniversityRepositories.kt:197:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\SupabaseUniversityRepositories.kt:181:59: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\UnavailableBookListingRepository.kt:15:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\UnavailableBookListingRepository.kt:16:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\UnavailableBookListingRepository.kt:17:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\UnavailableUniversityRepositories.kt:11:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\repository\UnavailableUniversityRepositories.kt:15:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:19:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:41:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:63:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:25:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:26:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:30:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:33:31: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:34:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:47:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:48:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:49:42: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:52:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:55:31: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:56:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:69:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:70:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:74:47: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:75:42: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:77:31: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\data\seed\FirestoreEventSeeder.kt:78:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\model\CampusReference.kt:81:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\model\Event.kt:42:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\model\PostNotification.kt:30:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\model\ServerErrorCode.kt:69:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:35:9: Function compactLuminance has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:66:53: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:67:30: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:67:39: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:67:56: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\qr\QrFrameDecoder.kt:67:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\AuthRepository.kt:26:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\AuthRepository.kt:29:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\AuthRepository.kt:34:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\AuthRepository.kt:39:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\AuthRepository.kt:51:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\EventRepository.kt:33:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\EventRepository.kt:35:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\EventRepository.kt:43:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\EventRepository.kt:45:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\TicketRepository.kt:8:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\TicketRepository.kt:9:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\repository\TicketRepository.kt:10:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:16:87: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:17:67: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:18:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:19:42: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:19:75: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\BookListingUseCases.kt:42:104: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ChangePasswordUseCase.kt:22:26: Function invoke has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CheckInUseCase.kt:16:26: Function invoke has 4 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCase.kt:16:26: Function invoke has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCase.kt:16:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCase.kt:23:50: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCase.kt:23:53: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCase.kt:32:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCase.kt:47:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCase.kt:25:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCase.kt:25:73: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\ReportHousingListingUseCase.kt:9:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SignInWithGoogleUseCase.kt:22:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCase.kt:39:90: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCase.kt:39:93: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCase.kt:56:38: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SubmitVerificationRequestUseCase.kt:19:34: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\SubmitVerificationRequestUseCase.kt:19:37: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\TimetableUseCase.kt:33:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\TimetableUseCase.kt:35:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\usecase\UpdateStudentContactUseCase.kt:21:26: Function invoke has 3 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\validation\PasswordChangeValidator.kt:16:40: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\validation\PasswordChangeValidator.kt:16:43: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\validation\StudentContactValidator.kt:18:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\domain\validation\StudentContactValidator.kt:19:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:811:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:823:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:860:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:811:13: Private function `AnnouncementContentPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:815:13: Private function `AnnouncementContentDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:823:13: Private function `AnnouncementShortContentPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:827:13: Private function `AnnouncementLoadingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:831:13: Private function `AnnouncementLoadingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:835:13: Private function `AnnouncementErrorPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:841:13: Private function `AnnouncementErrorDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:848:13: Private function `AnnouncementMissingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:852:13: Private function `AnnouncementMissingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementScreen.kt:860:13: Private function `AnnouncementContentFontScalePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceScreen.kt:77:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceScreen.kt:136:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceScreen.kt:139:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceScreen.kt:140:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\attendance\AttendanceScreen.kt:159:13: Private function `AttendancePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:211:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:352:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:440:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:473:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:509:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:682:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:715:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:855:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:894:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1153:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1187:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1441:13: Private function `PreviewAuthLightPassword` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1456:13: Private function `PreviewAuthDarkPassword` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthScreen.kt:1471:13: Private function `PreviewAuthIdentifierStep` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:70:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:75:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:77:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:80:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:82:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:85:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:179:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:181:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:209:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:233:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:242:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:244:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:246:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:263:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:344:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:396:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:435:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:464:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:495:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:519:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:531:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:605:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:239:54: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:262:67: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:262:70: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:415:29: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:417:23: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:535:37: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:552:25: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\auth\AuthViewModel.kt:554:23: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:314:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:315:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:328:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:329:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:330:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:407:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:437:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:446:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:459:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:472:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:489:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:530:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:540:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:546:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:560:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:564:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:662:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:668:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:688:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:702:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:705:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:755:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:792:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:788:100: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksScreen.kt:805:13: Private function `BooksScreenPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksUiState.kt:39:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksUiState.kt:43:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksUiState.kt:44:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\books\BooksViewModel.kt:52:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:160:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:177:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:181:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:227:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:229:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:271:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:274:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:276:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:277:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:278:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:279:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:280:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:222:52: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:223:35: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:223:54: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:223:60: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsScreen.kt:224:39: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsViewModel.kt:100:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\events\EventsViewModel.kt:102:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1315:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1319:13: Private function `HomeContentPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1325:13: Private function `HomeContentDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1332:13: Private function `HomeLoadingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1336:13: Private function `HomeLoadingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1340:13: Private function `HomeErrorPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1346:13: Private function `HomeErrorDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1353:13: Private function `HomeEmptyPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1357:13: Private function `HomeEmptyDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\home\HomeScreen.kt:1362:13: Private function `HomeContentFontScalePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\detail\HousingDetailScreen.kt:1396:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\detail\HousingDetailScreen.kt:1402:13: Private function `DetailContentLightPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\detail\HousingDetailScreen.kt:1408:13: Private function `DetailContentDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\HousingScreen.kt:1370:13: Private function `HousingScreenContentPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\housing\location\HousingLocationPickerScreen.kt:271:13: Private function `PickerBottomPanelPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\navigation\UthDestination.kt:74:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:213:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:217:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:241:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:244:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:524:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:536:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:540:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:544:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:552:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:528:13: Private function `ProfileContentLight` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:532:13: Private function `ProfileContentDark` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:536:13: Private function `ProfileOrganizerLight` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:540:13: Private function `ProfilePasswordLight` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:544:13: Private function `ProfilePasswordDark` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:548:13: Private function `ProfileContactLight` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:552:13: Private function `ProfileContactDark` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:556:13: Private function `ProfileEmptyLight` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileScreen.kt:560:13: Private function `ProfileLogoutDialog` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModel.kt:67:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModel.kt:150:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModel.kt:206:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModel.kt:236:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerRoute.kt:287:13: Function decodeFrame has 4 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerScreen.kt:399:13: Private function `ScannerLivePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerScreen.kt:405:13: Private function `ScannerSuccessPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\QrScannerScreen.kt:419:13: Private function `ScannerPermissionPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerRoute.kt:45:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerScreen.kt:63:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerScreen.kt:138:13: Private function `ScannerPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerViewModel.kt:40:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\scanner\ScannerViewModel.kt:52:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:157:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:158:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:159:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:966:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:928:13: Private function `LightPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleScreen.kt:945:13: Private function `DarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleUiState.kt:86:20: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleUiState.kt:87:20: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:146:9: Function onAction has 5 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:246:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:273:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:289:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:98:23: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:120:23: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModel.kt:136:23: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:123:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:353:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1325:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1219:13: Private function `TicketsPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1223:13: Private function `TicketsDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1230:13: Private function `TicketsPastPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1236:13: Private function `TicketsPastDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1243:13: Private function `TicketsFontScalePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1249:13: Private function `TicketsLoadingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1253:13: Private function `TicketsLoadingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1257:13: Private function `TicketsErrorPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1263:13: Private function `TicketsErrorDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1270:13: Private function `TicketsEmptyPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1274:13: Private function `TicketsEmptyDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1285:13: Private function `TicketQrDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1289:13: Private function `TicketQrCheckedInPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1296:13: Private function `TicketQrCheckedInDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1304:13: Private function `TicketQrExpiredPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1308:13: Private function `TicketQrExpiredDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1312:13: Private function `TicketQrFontScalePreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1318:13: Private function `TicketQrMissingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsScreen.kt:1322:13: Private function `TicketQrMissingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsViewModel.kt:41:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\tickets\TicketsViewModel.kt:54:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\campuses\CampusesScreen.kt:261:13: Private function `CampusesScreenPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\campuses\CampusesScreen.kt:269:13: Private function `CampusesScreenDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\campuses\CampusesUiState.kt:46:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\settings\SettingsScreen.kt:337:13: Private function `SettingsScreenPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\settings\SettingsScreen.kt:345:13: Private function `SettingsScreenDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\trainingpoint\TrainingPointScreen.kt:159:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\trainingpoint\TrainingPointScreen.kt:192:13: Private function `TrainingPointScreenPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\trainingpoint\TrainingPointScreen.kt:200:13: Private function `TrainingPointScreenDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionScreen.kt:248:13: Private function `TuitionScreenPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionScreen.kt:256:13: Private function `TuitionScreenDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:25:62: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:26:65: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:27:65: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:28:65: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:29:59: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\tuition\TuitionUiState.kt:30:67: This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:478:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:487:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:491:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:497:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:503:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:507:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:523:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:487:13: Private function `UtilitiesContentPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:491:13: Private function `UtilitiesContentDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:497:13: Private function `UtilitiesContentLargeTextPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:503:13: Private function `UtilitiesContentNarrowPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:507:13: Private function `UtilitiesContentNarrowLargeTextPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:511:13: Private function `UtilitiesLoadingPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:515:13: Private function `UtilitiesLoadingDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:519:13: Private function `UtilitiesEmptyPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:523:13: Private function `UtilitiesEmptyDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:527:13: Private function `UtilitiesErrorPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\main\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesScreen.kt:533:13: Private function `UtilitiesErrorDarkPreview` is unused. [UnusedPrivateMember]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\AuthSecurityAdversarialChallengeTest.kt:172:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\AuthSecurityAdversarialChallengeTest.kt:236:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\AuthSecurityAdversarialChallengeTest.kt:241:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\AuthSecurityAdversarialChallengeTest.kt:243:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepositoryTest.kt:24:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\SupabaseBookListingRepositoryTest.kt:65:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\data\repository\SupabaseMappersTest.kt:120:13: Private property `base` is unused. [UnusedPrivateProperty]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\CheckInUseCaseTest.kt:66:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCaseTest.kt:15:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCaseTest.kt:17:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\CompletePasswordResetUseCaseTest.kt:18:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCaseTest.kt:16:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCaseTest.kt:18:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\ForgotPasswordUseCaseTest.kt:65:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignInUseCaseTest.kt:25:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignInUseCaseTest.kt:31:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignInWithGoogleUseCaseTest.kt:53:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignInWithGoogleUseCaseTest.kt:84:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignInWithGoogleUseCaseTest.kt:86:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCaseTest.kt:18:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCaseTest.kt:20:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCaseTest.kt:21:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SignUpUseCaseTest.kt:119:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SubmitVerificationRequestUseCaseTest.kt:19:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SubmitVerificationRequestUseCaseTest.kt:21:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\usecase\SubmitVerificationRequestUseCaseTest.kt:22:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\validation\PasswordChangeValidatorTest.kt:11:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\validation\PasswordChangeValidatorTest.kt:28:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\domain\validation\StudentContactValidatorTest.kt:12:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\announcement\AnnouncementViewModelTest.kt:118:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\attendance\AttendanceViewModelTest.kt:33:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\attendance\AttendanceViewModelTest.kt:42:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:68:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:73:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:207:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:256:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:329:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:373:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthAdversarialStressTest.kt:397:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:129:30: Function authenticateWithGoogle has 4 return statements which exceeds the limit of 2. [ReturnCount]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:121:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:125:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:127:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:159:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:229:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:296:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthE2EWorkflowTest.kt:334:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthViewModelTest.kt:60:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\auth\AuthViewModelTest.kt:62:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\books\BooksUiStateTest.kt:43:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\books\BooksViewModelTest.kt:49:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\books\BooksViewModelTest.kt:140:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\books\BooksViewModelTest.kt:165:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\home\HomeViewModelTest.kt:140:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\housing\HousingViewModelTest.kt:52:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModelTest.kt:77:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModelTest.kt:78:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModelTest.kt:46:21: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\profile\ProfileViewModelTest.kt:67:17: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\scanner\ScannerViewModelTest.kt:71:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:37:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:153:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:154:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:162:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:169:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\schedule\ScheduleViewModelTest.kt:150:17: Private property `emptyEvents` is unused. [UnusedPrivateProperty]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\tickets\TicketsViewModelTest.kt:29:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\tickets\TicketsViewModelTest.kt:108:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\tickets\TicketsViewModelTest.kt:113:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\test\java\com\giathinh\uthconnect\presentation\utilities\UtilitiesViewModelTest.kt:163:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\BooksContactUiTest.kt:46:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\BooksContactUiTest.kt:66:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\BooksContactUiTest.kt:37:17: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\presentation\housing\HousingScreenUiTest.kt:48:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\presentation\housing\HousingScreenUiTest.kt:90:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\presentation\profile\ProfileScreenUiTest.kt:33:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
G:\HKDN -\UTH-Connect\.worktrees\KAN-111-detekt-cleanup\app\src\androidTest\java\com\giathinh\uthconnect\presentation\profile\ProfileScreenUiTest.kt:45:1: Line detected, which is longer than the defined maximum line length in the code style. [MaxLineLength]
```

## Limits
Baseline staleness requires comparing detekt issue IDs generated from this exact analysis; it is not inferred from file names or line numbers. Per-finding test coverage has not been measured. Gradle baseline scan and the earlier full build, Spotless and 414 unit tests are documented in KAN-111_PHASE1_AUDIT_a54f5b6.md. No source or configuration was edited.

## Files by rule for planning

Counts are finding locations, not baseline IDs. Run the relevant module tests after every batch; review changes affecting validation, exceptions and coroutines for behavior preservation.

### ComplexCondition (4)

- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/BookListingUseCases.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 1

### CyclomaticComplexMethod (11)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirestoreEventRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/BookListingUseCases.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CompletePasswordResetUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/validation/PasswordChangeValidator.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/create/CreateHousingPostViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/lostfound/LostFoundViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 1

### LongMethod (8)

- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/lostfound/LostFoundViewModel.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/seed/FirestoreEventSeeder.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/home/HomeScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 1

### LongParameterList (1)

- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 1

### MagicNumber (123)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseRestClient.kt`: 31
- `app/src/main/java/com/giathinh/uthconnect/data/seed/FirestoreEventSeeder.kt`: 17
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt`: 8
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 8
- `app/src/main/java/com/giathinh/uthconnect/data/repository/StaticTuitionRepository.kt`: 6
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt`: 6
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/BookListingUseCases.kt`: 6
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/tuition/TuitionUiState.kt`: 6
- `app/src/main/java/com/giathinh/uthconnect/domain/qr/QrFrameDecoder.kt`: 5
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsScreen.kt`: 5
- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAuthRepository.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCase.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/data/repository/InMemoryTimetableRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CompletePasswordResetUseCase.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ForgotPasswordUseCase.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SubmitVerificationRequestUseCase.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/validation/PasswordChangeValidator.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleUiState.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/InMemoryLostFoundRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SecureSessionStore.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseUniversityRepositories.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksScreen.kt`: 1

### MatchingDeclarationName (8)

- `app/src/main/java/com/giathinh/uthconnect/core/designsystem/component/UthStatusBadge.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/core/designsystem/theme/Dimens.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementNavigation.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/attendance/AttendanceNavigation.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsNavigation.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/lostfound/LostFoundNavigation.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/ScannerRoute.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesNavigation.kt`: 1

### MaxLineLength (269)

- `app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksScreen.kt`: 23
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 22
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsScreen.kt`: 12
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`: 11
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt`: 9
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`: 9
- `app/src/main/java/com/giathinh/uthconnect/data/repository/InMemoryLostFoundRepository.kt`: 8
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesScreen.kt`: 7
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthAdversarialStressTest.kt`: 7
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthE2EWorkflowTest.kt`: 7
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt`: 6
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/AuthRepository.kt`: 5
- `app/src/test/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModelTest.kt`: 5
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/EventRepository.kt`: 4
- `app/src/main/java/com/giathinh/uthconnect/presentation/attendance/AttendanceScreen.kt`: 4
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModel.kt`: 4
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleScreen.kt`: 4
- `app/src/test/java/com/giathinh/uthconnect/data/repository/AuthSecurityAdversarialChallengeTest.kt`: 4
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SignUpUseCaseTest.kt`: 4
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseUniversityRepositories.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/data/repository/UnavailableBookListingRepository.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/data/seed/FirestoreEventSeeder.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/TicketRepository.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementScreen.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksUiState.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/tickets/TicketsScreen.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/CompletePasswordResetUseCaseTest.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/ForgotPasswordUseCaseTest.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SignInWithGoogleUseCaseTest.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SubmitVerificationRequestUseCaseTest.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/presentation/books/BooksViewModelTest.kt`: 3
- `app/src/test/java/com/giathinh/uthconnect/presentation/tickets/TicketsViewModelTest.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAuthRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FakeScheduleRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SecureSessionStore.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseEventRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseRestClient.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/UnavailableUniversityRepositories.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ForgotPasswordUseCase.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/TimetableUseCase.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/domain/validation/StudentContactValidator.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsViewModel.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/ScannerViewModel.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/tickets/TicketsViewModel.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepositoryTest.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/SignInUseCaseTest.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/PasswordChangeValidatorTest.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/presentation/attendance/AttendanceViewModelTest.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthViewModelTest.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModelTest.kt`: 2
- `app/src/androidTest/java/com/giathinh/uthconnect/BooksContactUiTest.kt`: 2
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/housing/HousingScreenUiTest.kt`: 2
- `app/src/androidTest/java/com/giathinh/uthconnect/presentation/profile/ProfileScreenUiTest.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/di/AttendanceRosterDataModule.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAccountStore.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/FakeNotificationRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/GoogleIdTokenSource.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/GoogleNonce.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/InMemoryTimetableRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/MockTicketRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/model/CampusReference.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/model/Event.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/model/PostNotification.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/model/ServerErrorCode.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CompletePasswordResetUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ReportHousingListingUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/SignInWithGoogleUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/home/HomeScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/detail/HousingDetailScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/navigation/UthDestination.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/ScannerRoute.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/ScannerScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/campuses/CampusesUiState.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/trainingpoint/TrainingPointScreen.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/domain/usecase/CheckInUseCaseTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/domain/validation/StudentContactValidatorTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementViewModelTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/books/BooksUiStateTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/home/HomeViewModelTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/housing/HousingViewModelTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/scanner/ScannerViewModelTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesViewModelTest.kt`: 1

### ReturnCount (11)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirebaseAuthRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/GoogleIdTokenSource.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseMappers.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/qr/QrFrameDecoder.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/ChangePasswordUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CheckInUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/CompletePasswordResetUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/usecase/UpdateStudentContactUseCase.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/QrScannerRoute.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/auth/AuthE2EWorkflowTest.kt`: 1

### SwallowedException (3)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/GoogleIdTokenSource.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SecureSessionStore.kt`: 1

### TooGenericExceptionCaught (21)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseHousingRepository.kt`: 8
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseBookListingRepository.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/events/EventsViewModel.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SecureSessionStore.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAttendanceRosterRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseGoogleExchange.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SuspendResults.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/attendance/AttendanceViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModel.kt`: 1

### TooManyFunctions (19)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/DemoAuthRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseAuthRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseEventRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseHousingRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/data/repository/SupabaseRestClient.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/domain/repository/EventRepository.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/home/HomeScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/create/CreateHousingPostScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/create/CreateHousingPostViewModel.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/detail/HousingDetailScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/HousingScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/QrScannerScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/tickets/TicketsScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesScreen.kt`: 1

### UnusedPrivateMember (81)

- `app/src/main/java/com/giathinh/uthconnect/presentation/tickets/TicketsScreen.kt`: 19
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/UtilitiesScreen.kt`: 11
- `app/src/main/java/com/giathinh/uthconnect/presentation/announcement/AnnouncementScreen.kt`: 10
- `app/src/main/java/com/giathinh/uthconnect/presentation/home/HomeScreen.kt`: 9
- `app/src/main/java/com/giathinh/uthconnect/presentation/profile/ProfileScreen.kt`: 9
- `app/src/main/java/com/giathinh/uthconnect/presentation/auth/AuthScreen.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/QrScannerScreen.kt`: 3
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/detail/HousingDetailScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/schedule/ScheduleScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/campuses/CampusesScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/settings/SettingsScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/trainingpoint/TrainingPointScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/utilities/tuition/TuitionScreen.kt`: 2
- `app/src/main/java/com/giathinh/uthconnect/presentation/attendance/AttendanceScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/books/BooksScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/HousingScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/housing/location/HousingLocationPickerScreen.kt`: 1
- `app/src/main/java/com/giathinh/uthconnect/presentation/scanner/ScannerScreen.kt`: 1

### UnusedPrivateProperty (2)

- `app/src/test/java/com/giathinh/uthconnect/data/repository/SupabaseMappersTest.kt`: 1
- `app/src/test/java/com/giathinh/uthconnect/presentation/schedule/ScheduleViewModelTest.kt`: 1

### UseCheckOrError (5)

- `app/src/main/java/com/giathinh/uthconnect/data/repository/FirestoreEventRepository.kt`: 2
- `app/src/test/java/com/giathinh/uthconnect/presentation/profile/ProfileViewModelTest.kt`: 2
- `app/src/androidTest/java/com/giathinh/uthconnect/BooksContactUiTest.kt`: 1
