# Location Migration: Frontend → Backend

## Current State Analysis

### Files Involved

| File | Role |
|------|------|
| `screens/LocationPickerScreen.tsx` | UI — city dropdown + "Continue" button |
| `utils/saveUserLocation.ts` | Writes location + onboarding flag to Firestore |
| `constants/cities.ts` | Static city list with coordinates |
| `navigation/AppStack.tsx` | Reads profile from Firestore, gates routing on `hasCompletedOnboarding` |

### Flow

```
LocationPickerScreen
  └── handleContinue()
        └── saveUserLocation(selectedCity)   ← util/saveUserLocation.ts
              ├── auth().currentUser          ← gets uid
              └── firestore().collection('users').doc(uid).update({
                    location: {
                      cityId, cityName, state, country,
                      latitude, longitude,
                      savedAt: serverTimestamp()
                    },
                    hasCompletedOnboarding: true
                  })
```

```
AppStack (on mount)
  └── firestore().collection('users').doc(uid).onSnapshot(...)
        ├── reads hasCompletedOnboarding + location
        ├── sets initialRouteName → 'LocationPicker' or 'Home'
        └── MIGRATION: if hasCompletedOnboarding == undefined
              └── firestore().collection('users').doc(uid).update({
                    hasCompletedOnboarding: false
                  })                          ← second direct Firestore write
```

### What the Frontend Currently Writes to Firestore

**`saveUserLocation`** — triggered when user taps "Continue":
```
users/{uid}: {
  location: {
    cityId:    string   (e.g. "seattle")
    cityName:  string   (e.g. "Seattle")
    state:     string   (e.g. "WA")
    country:   string   (e.g. "USA")
    latitude:  number
    longitude: number
    savedAt:   serverTimestamp
  },
  hasCompletedOnboarding: true
}
```

**`AppStack` migration** — triggered once on load if field is absent:
```
users/{uid}: {
  hasCompletedOnboarding: false
}
```

### What Stays on the Frontend (No Change Needed)

- The `LocationPickerScreen` UI (picker, modal, button)
- The `CITIES` constant (used to populate the dropdown — stays as the source of truth for displayed options)
- The `onSnapshot` listener in `AppStack` (read-only, real-time profile subscription — acceptable client-side read)

---

## Problems with the Current Approach

**City data is fully trusted from the client.** `saveUserLocation` writes whatever the client passes — including latitude, longitude, and city name. A malicious or buggy client could write arbitrary coordinates or strings into user documents.

**Two separate files make direct Firestore writes.** `saveUserLocation.ts` and the inline migration in `AppStack.tsx` both write to `users/{uid}`. Firestore security rules must allow client writes, and there's no central place to audit or validate those writes.

**The migration logic is client-side business logic.** Whether `hasCompletedOnboarding` should default to `false` is a data concern — if the field schema changes, every installed version of the app needs to be updated.

**`onSnapshot` returning `undefined` triggers a write.** The migration in `AppStack` fires a Firestore `.update()` from within a snapshot listener — a read triggering a write is fragile and hard to reason about.

---

## Target Architecture

The frontend sends only a `cityId` (a simple string identifier). The backend owns the city registry, validates the ID, looks up the authoritative city data (name, coordinates), and performs all Firestore writes.

```
LocationPickerScreen
  └── handleContinue()
        └── PUT /api/users/location  { cityId: "seattle" }
              └── gather-service
                    ├── verifies Bearer ID token
                    ├── looks up city in server-side registry
                    ├── writes location + hasCompletedOnboarding: true to Firestore
                    └── returns { location }

AppStack (on mount)
  └── POST /api/users/ensure-profile   ← replaces the inline migration
        └── gather-service
              ├── verifies Bearer ID token
              ├── checks if hasCompletedOnboarding exists in Firestore doc
              └── sets it to false if missing
  └── firestore().onSnapshot(...)       ← unchanged, reads only
```

---

## Backend Endpoints to Build

### `PUT /api/users/location`

Saves the user's chosen city and marks onboarding complete.

**Request:**
```
Authorization: Bearer <firebase-id-token>

{
  "cityId": "seattle"
}
```

**Backend actions:**
1. Verify ID token → extract `uid`
2. Look up `cityId` in the server-side city registry; return 404 if not found
3. Write to Firestore `users/{uid}`:
   ```json
   {
     "location": {
       "cityId": "seattle",
       "cityName": "Seattle",
       "state": "WA",
       "country": "USA",
       "latitude": 47.6062,
       "longitude": -122.3321,
       "savedAt": "<server timestamp>"
     },
     "hasCompletedOnboarding": true
   }
   ```
4. Return the resolved location object

**Response:**
```json
{
  "location": {
    "cityId": "seattle",
    "cityName": "Seattle",
    "state": "WA",
    "country": "USA",
    "latitude": 47.6062,
    "longitude": -122.3321
  }
}
```

**Errors:** 400 for missing/blank `cityId`, 401 for invalid token, 404 for unknown city.

---

### `POST /api/users/ensure-profile`

Replaces the inline migration in `AppStack`. Called once after the Firebase session is established. Ensures the Firestore user doc has all required fields with sensible defaults.

**Request:**
```
Authorization: Bearer <firebase-id-token>
(no body)
```

**Backend actions:**
1. Verify ID token → extract `uid`
2. Fetch `users/{uid}` from Firestore
3. If `hasCompletedOnboarding` is absent, set it to `false`
4. Return 204 No Content

This is idempotent — safe to call on every app launch.

---

## New Backend Files

### `model/UpdateLocationRequest.java`
```java
@Data
public class UpdateLocationRequest {
    @NotBlank(message = "cityId is required")
    private String cityId;
}
```

### `model/LocationResponse.java`
```java
@Data @Builder
public class LocationResponse {
    private String cityId;
    private String cityName;
    private String state;
    private String country;
    private double latitude;
    private double longitude;
}
```

### `config/CityRegistry.java`
A `@Component` that holds the authoritative city list (mirroring `constants/cities.ts`). Returns a `Map<String, LocationResponse>` keyed by `cityId`. This is the single place to add new cities.

### `service/UserService.java`
- `updateLocation(String uid, String cityId)` — validates city, writes Firestore, returns `LocationResponse`
- `ensureProfile(String uid)` — checks and backfills `hasCompletedOnboarding` if absent

### `controller/UserController.java`
- `PUT /api/users/location` — reads Bearer token from `Authorization` header, delegates to `UserService.updateLocation`
- `POST /api/users/ensure-profile` — reads Bearer token, delegates to `UserService.ensureProfile`

Both endpoints verify the ID token using the injected `FirebaseAuth` bean (already set up).

---

## Frontend Changes Required

### `utils/saveUserLocation.ts`

Replace the Firestore write with a backend call:

```typescript
// Before: direct firestore write with all city fields
// After:
export async function saveUserLocation(city: City): Promise<void> {
  const user = auth().currentUser;
  if (!user) throw new Error('No authenticated user');

  const idToken = await user.getIdToken();
  const res = await fetch(`${BACKEND_URL}/api/users/location`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({ cityId: city.id }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || 'Failed to save location');
  }
}
```

Remove imports: `firestore`
Keep imports: `auth`

### `navigation/AppStack.tsx`

Remove the inline migration block (the `firestore().update({ hasCompletedOnboarding: false })` call inside the `onSnapshot` callback). Replace it with a one-time call to `POST /api/users/ensure-profile` in a separate `useEffect` that fires on mount.

Remove imports: `firestore`
Keep imports: `auth` (still needed for `auth().currentUser`)

---

## Migration Sequence

1. Add `CityRegistry`, `UserService`, `UserController` to gather-service
2. Update `saveUserLocation.ts` to call `PUT /api/users/location`
3. Update `AppStack.tsx` to call `POST /api/users/ensure-profile` and remove the inline Firestore write
4. Test the full onboarding flow end-to-end (new user → location picker → home)
5. Tighten Firestore security rules: `users/{uid}` should now be read-only for the authenticated client (no client writes needed)
