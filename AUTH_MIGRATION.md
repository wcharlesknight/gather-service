# Auth Migration: Frontend → Backend

## Current State Analysis

### Flow Overview

```
App.tsx
  └── RootNavigator
        ├── auth().onAuthStateChanged  ← Firebase SDK listener
        ├── user == null → AuthStack → WelcomeScreen
        └── user != null → AppStack  → HomeScreen
```

### What the Frontend Currently Does Directly

#### Sign-Up (`WelcomeScreen.handleAuth` when `isSignUp == true`)
1. `auth().createUserWithEmailAndPassword(email, password)` — creates Firebase Auth user
2. `userCredential.user.updateProfile({ displayName })` — sets display name on Auth profile
3. `firestore().collection('users').doc(uid).set({...})` — creates user document in Firestore with:
   - `displayName`
   - `email`
   - `createdAt` (server timestamp)
   - `lastLoginAt` (server timestamp)
   - `hasCompletedOnboarding: false`

#### Sign-In (`WelcomeScreen.handleAuth` when `isSignUp == false`)
1. `auth().signInWithEmailAndPassword(email, password)` — authenticates with Firebase
2. `firestore().collection('users').doc(uid).update({ lastLoginAt })` — updates last login timestamp

#### Sign-Out (`HomeScreen.handleSignOut`)
1. `auth().signOut()` — clears the local Firebase session

#### Session/Profile Listening (`HomeScreen` useEffect)
- `firestore().collection('users').doc(uid).onSnapshot(...)` — real-time profile subscription

### Problems with the Current Approach

- **Business logic in the client**: User profile creation rules (default fields, onboarding flag) are only enforced if the client code runs correctly.
- **Direct Firestore writes**: Firestore security rules must be permissive enough to allow clients to write to `users/{uid}`. Any client can write arbitrary data if rules aren't tight.
- **No server-side validation**: Email format, display name rules, duplicate detection — all client-side.
- **Hard to iterate**: Adding a field to the user profile (e.g., a referral code, a plan tier) requires an app update.
- **No audit trail**: No server-side hook to trigger side effects on registration (welcome email, analytics event, etc.).

---

## Target Architecture

The frontend should only:
1. Collect credentials from the user
2. Call backend API endpoints
3. Use the Firebase custom token returned by the backend to establish a Firebase session

All Firebase Admin SDK calls, Firestore profile writes, and business logic live in the backend.

```
WelcomeScreen
  └── POST /api/auth/register  or  POST /api/auth/login
        └── gather-service (Spring Boot)
              ├── Firebase Admin SDK (create user / verify token)
              ├── Firestore write (user profile doc)
              └── returns { customToken, userProfile }

Frontend receives customToken
  └── auth().signInWithCustomToken(token)  ← establishes Firebase session
        └── RootNavigator.onAuthStateChanged fires → navigates to AppStack
```

---

## Backend Endpoints to Build

### `POST /api/auth/register`

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "s3cur3P@ss",
  "displayName": "Jane Doe"
}
```

**Backend actions:**
1. Validate inputs (non-empty, email format, password length >= 6)
2. Use Firebase Admin SDK to create the Auth user: `FirebaseAuth.getInstance().createUser(...)`
3. Set `displayName` on the Auth user record
4. Write Firestore user doc: `users/{uid}` with `displayName`, `email`, `createdAt`, `lastLoginAt`, `hasCompletedOnboarding: false`
5. Create a Firebase custom token: `FirebaseAuth.getInstance().createCustomToken(uid)`
6. Return the custom token (and optionally the user profile)

**Response:**
```json
{
  "customToken": "<firebase-custom-token>",
  "user": {
    "uid": "...",
    "displayName": "Jane Doe",
    "email": "user@example.com"
  }
}
```

**Error responses:** 400 for validation failures, 409 if email already in use.

---

### `POST /api/auth/login`

Sign-in is trickier because Firebase Auth password verification is not exposed via the Admin SDK — it is intentionally client-only. The two viable patterns are:

**Option A (Recommended) — Client authenticates, backend syncs:**
1. Frontend calls `auth().signInWithEmailAndPassword(email, password)` as today
2. Frontend immediately calls `POST /api/auth/login` with the resulting ID token (not the password)
3. Backend verifies the ID token via Admin SDK, updates `lastLoginAt` in Firestore, returns the user profile

**Option B — Backend proxies to Firebase Auth REST API:**
1. Frontend sends `{email, password}` to backend
2. Backend calls the Firebase Auth REST API (`identitytoolkit.googleapis.com`) with the Web API key to verify credentials and get an ID token
3. Backend then issues a custom token back to the frontend
4. Frontend calls `auth().signInWithCustomToken(token)`

Option A is simpler and keeps password handling fully within Firebase. Option B is viable if you want zero Firebase SDK usage on the frontend long-term.

**For Option A, request body:**
```json
{
  "idToken": "<firebase-id-token>"
}
```

**Backend actions:**
1. Verify ID token: `FirebaseAuth.getInstance().verifyIdToken(idToken)`
2. Update `lastLoginAt` in Firestore for the user
3. Return user profile

---

### `POST /api/auth/logout` (optional)

Sign-out is local state clearing (`auth().signOut()`) and doesn't require a server round-trip for basic use. Add this endpoint if you later need server-side session revocation:

**Backend actions:**
1. Verify the ID token
2. Call `FirebaseAuth.getInstance().revokeRefreshTokens(uid)` to invalidate all sessions

---

## Frontend Changes Required

### `WelcomeScreen.tsx`

Replace the `handleAuth` function body:

**Sign-up path:**
```
// Before: 3 direct Firebase SDK calls
// After:
const response = await fetch('https://<backend>/api/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName }),
});
const { customToken } = await response.json();
await auth().signInWithCustomToken(customToken);
// RootNavigator listener fires automatically → navigates to AppStack
```

Remove imports: `firestore` (no longer needed in this file)
Keep import: `auth` (still needed for `signInWithCustomToken`)

**Sign-in path (Option A):**
```
// Step 1: authenticate with Firebase (unchanged)
const userCredential = await auth().signInWithEmailAndPassword(email, password);
// Step 2: sync with backend
const idToken = await userCredential.user.getIdToken();
await fetch('https://<backend>/api/auth/login', {
  method: 'POST',
  headers: { Authorization: `Bearer ${idToken}` },
});
// Navigation handled by RootNavigator listener
```

**Sign-in path (Option B):**
```
const response = await fetch('https://<backend>/api/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password }),
});
const { customToken } = await response.json();
await auth().signInWithCustomToken(customToken);
```

### `HomeScreen.tsx`

**Sign-out:** No change needed for basic sign-out. If adding server-side revocation:
```
const idToken = await auth().currentUser?.getIdToken();
await fetch('https://<backend>/api/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${idToken}` } });
await auth().signOut();
```

**Profile subscription:** The `onSnapshot` listener can stay for now (real-time profile data is a valid use of Firestore). It can be migrated to a REST poll or WebSocket later if desired.

### `RootNavigator.tsx`

No changes needed. The `onAuthStateChanged` listener handles navigation automatically once `signInWithCustomToken` resolves.

---

## Backend Implementation Notes (gather-service)

### Dependencies to add (`build.gradle`)
```groovy
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

### Config
- Add Firebase service account JSON to resources or configure via env var (`GOOGLE_APPLICATION_CREDENTIALS`)
- Initialize `FirebaseApp` in a `@Configuration` class (similar to `GooglePlacesApiConfig`)

### New files
- `config/FirebaseConfig.java` — initializes `FirebaseApp`
- `controller/AuthController.java` — `@RestController` with `/api/auth/register`, `/api/auth/login`
- `service/AuthService.java` — business logic using `FirebaseAuth` and `Firestore` admin clients
- `model/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java` — request/response DTOs

---

## Migration Sequence

1. Build and test backend endpoints (register + login Option A) with the Firebase Admin SDK
2. Update `WelcomeScreen` to call the backend, keep `signInWithCustomToken`
3. Verify the full sign-up and sign-in flow end-to-end
4. Tighten Firestore security rules: clients should no longer need write access to `users/{uid}`
5. (Optional) Migrate sign-in to Option B to remove the direct `signInWithEmailAndPassword` call
6. (Optional) Add `POST /api/auth/logout` with token revocation
