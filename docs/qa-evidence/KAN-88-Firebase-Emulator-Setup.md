# KAN-88 Firebase Emulator setup

This QA setup uses Firebase Emulator Suite only. The project ID is `demo-uthconnect`; it is a Firebase demo project ID, so the emulator refuses non-emulated service calls. No `google-services.json` or production credentials are used.

## Start local services

From the feature worktree, start Auth, Firestore, and Storage:

```powershell
npx --yes firebase-tools emulators:start --only auth,firestore,storage --project demo-uthconnect
```

The checked-in `firebase.json` configures the existing Firestore and Storage rules and local ports 9099, 8080, and 9199. Emulator UI runs at `http://127.0.0.1:4000`.

## Build for the Android emulator

In a second terminal:

```powershell
.\gradlew.bat -PuseMockData=false -PuseFirebaseEmulator=true assembleDebug
```

This build initializes Firebase with emulator-only options and connects Android's `10.0.2.2` host alias to the local services. Cleartext HTTP is enabled only when `useFirebaseEmulator=true`. The default build remains in mock mode. For a real Firebase QA project, omit the emulator flag and provide the approved `app/google-services.json` locally; do not commit it.

## Test accounts and data

Create disposable email/password accounts in the Auth Emulator UI or with its local Auth REST API. The app currently accepts UTH-style email addresses ending in `@ut.edu.vn`; for example, `kan88.qa.owner@ut.edu.vn` and `kan88.qa.other@ut.edu.vn`. Use generated passwords and keep them in a local password manager or encrypted temporary storage. Emulator accounts and test listings are disposable and are removed when the emulator data is reset.

Do not upload local account passwords, ID tokens, emulator auth tokens, or private configuration into Jira, reports, logs, or commits. Do not use a production project for this task.
