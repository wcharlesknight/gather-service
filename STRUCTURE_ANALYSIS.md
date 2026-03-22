# Codebase Structure Analysis

## Backend — gather-service

### Current Layout

```
com.gather/
├── Application.java
├── config/
│   ├── CityRegistry.java          ← not really a config
│   ├── FirebaseConfig.java
│   ├── GooglePlacesApiConfig.java
│   ├── PlaceSearchServiceConfig.java
│   └── WebClientConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CityController.java
│   ├── GatheringSpotController.java
│   ├── GooglePlacesController.java
│   └── HealthController.java
│   └── UserController.java
├── job/
│   └── GatheringSpotSyncJob.java
├── model/
│   ├── AuthResponse.java          ← response DTO
│   ├── City.java                  ← job config domain model
│   ├── GatheringSpot.java         ← stored record domain model
│   ├── GooglePlace.java           ← provider-specific model
│   ├── GooglePlacesSearchRequest.java
│   ├── GooglePlacesSearchResponse.java
│   ├── LocationResponse.java      ← response DTO
│   ├── Place.java                 ← generic search result model
│   ├── RegisterRequest.java       ← request DTO
│   └── UpdateLocationRequest.java ← request DTO
├── repository/
│   ├── CityRepository.java
│   └── GatheringSpotRepository.java
└── service/
    ├── AuthService.java
    ├── GooglePlaceSearchService.java
    ├── GooglePlacesApiService.java
    ├── PlaceSearchService.java     ← interface
    ├── PushNotificationService.java
    └── UserService.java
```

### Issues

#### 1. `CityRegistry` belongs in `service/`, not `config/`

`config/CityRegistry.java` is a data lookup component, not a configuration class. The `config` package should contain classes that wire up beans and read external configuration (`@Configuration`, `@ConfigurationProperties`). `CityRegistry` holds business data and has a `find()` method — that's a service or repository concern. It should live in `service/` or a new `registry/` package.

#### 2. The `model/` package mixes four distinct kinds of types

Everything lives in one flat package:

| File | Kind |
|------|------|
| `City`, `GatheringSpot`, `Place` | Domain models |
| `GooglePlace`, `GooglePlacesSearchRequest`, `GooglePlacesSearchResponse` | Provider-specific internal models |
| `RegisterRequest`, `UpdateLocationRequest` | Inbound request DTOs |
| `AuthResponse`, `LocationResponse` | Outbound response DTOs |

These should be separated so it's immediately clear what each type is for:

```
model/
├── domain/       City, GatheringSpot, Place
├── dto/
│   ├── request/  RegisterRequest, UpdateLocationRequest
│   └── response/ AuthResponse, LocationResponse
└── provider/
    └── google/   GooglePlace, GooglePlacesSearchRequest, GooglePlacesSearchResponse
```

#### 3. Two different "City" concepts collide

- `model/City.java` — a Firestore document for the weekly job (fields: `topic`, `cronSchedule`, `searchTerm`, `searchLimit`, `enabled`). This is really a *job configuration* per city, not a city.
- `config/CityRegistry` + `model/LocationResponse` — represents a city for user location purposes (coordinates, state).

These should have names that reflect what they actually are: `City` → `CityJobConfig` (or similar), making it immediately clear it's a configuration record for the sync job, not a geographic concept.

#### 4. `CityController` and `GatheringSpotController` bypass the service layer

`CityController` calls `CityRepository` directly. `GatheringSpotController` calls `GatheringSpotRepository` directly. Every other controller (`AuthController`, `UserController`) goes through a service. The inconsistency means business logic will inevitably creep into controllers. Both need thin service wrappers (`CityService`, `GatheringSpotService`) inserted between them.

#### 5. `GooglePlacesController` is a raw debug proxy in production code

`GooglePlacesController` returns the raw Google API response as an untyped `String`. It exists as a development/debugging tool but is indistinguishable from production endpoints. At minimum it should be clearly separated or behind an internal-only route; ideally it lives in a `debug/` controller subpackage or is removed.

#### 6. `GatheringSpot` has misleading legacy field names

`yelpUrl` is documented in code as "also used for Google Maps URL". `yelpBusinessId` is a legacy field kept for backward compatibility. These names are actively misleading — a new developer reading the model would assume it's Yelp-specific. Suggest renaming to `url` and `legacyYelpBusinessId` (or just removing the legacy field if Firestore data can be migrated).

#### 7. `AuthService.InvalidTokenException` is used across services

`UserService` imports and throws `AuthService.InvalidTokenException`. This creates a cross-service dependency solely for a shared exception type. A small `exception/` package with shared exception classes (`InvalidTokenException`, `UnknownCityException`) would break the coupling.

#### 8. Lombok is used inconsistently

`AuthResponse` and `LocationResponse` (written recently) use `@Data` / `@Builder`. `City`, `GatheringSpot`, and `Place` have hundreds of lines of hand-written getters/setters despite Lombok being a declared dependency. This is just legacy — but the two styles coexisting makes the codebase feel inconsistent.

### Proposed Backend Layout

```
com.gather/
├── Application.java
├── config/                        ← only wiring & external config
│   ├── FirebaseConfig.java
│   ├── GooglePlacesApiConfig.java
│   ├── PlaceSearchServiceConfig.java
│   └── WebClientConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CityController.java
│   ├── GatheringSpotController.java
│   ├── HealthController.java
│   └── UserController.java
│   (GooglePlacesController removed or moved to internal/)
├── exception/                     ← NEW: shared exception types
│   ├── InvalidTokenException.java
│   └── UnknownCityException.java
├── job/
│   └── GatheringSpotSyncJob.java
├── model/
│   ├── domain/
│   │   ├── CityJobConfig.java     ← renamed from City
│   │   ├── GatheringSpot.java
│   │   └── Place.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   └── UpdateLocationRequest.java
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       └── LocationResponse.java
│   └── provider/
│       └── google/
│           ├── GooglePlace.java
│           ├── GooglePlacesSearchRequest.java
│           └── GooglePlacesSearchResponse.java
├── repository/
│   ├── CityRepository.java
│   └── GatheringSpotRepository.java
└── service/
    ├── AuthService.java
    ├── CityService.java           ← NEW: wraps CityRepository
    ├── CityRegistry.java          ← moved from config/
    ├── GatheringSpotService.java  ← NEW: wraps GatheringSpotRepository
    ├── GooglePlaceSearchService.java
    ├── GooglePlacesApiService.java
    ├── PlaceSearchService.java
    ├── PushNotificationService.java
    └── UserService.java
```

---

## Frontend — LoopIn

### Current Layout

```
LoopIn/
├── App.tsx
├── constants/
│   └── cities.ts
├── navigation/
│   ├── AppStack.tsx
│   ├── AuthStack.tsx
│   └── RootNavigator.tsx
├── screens/
│   ├── HomeScreen.tsx
│   ├── LocationPickerScreen.tsx
│   └── WelcomeScreen.tsx
├── types/
│   └── index.ts
└── utils/
    └── saveUserLocation.ts
```

### Issues

#### 1. `BACKEND_URL` is defined in three separate files

`WelcomeScreen.tsx`, `saveUserLocation.ts`, and `AppStack.tsx` each define:
```ts
const BACKEND_URL = 'http://localhost:8080';
```
Changing the backend URL requires finding and updating three files. This should be a single export from `constants/api.ts`.

#### 2. No API client layer

All backend calls are raw `fetch()` calls with manual `Authorization: Bearer` header construction and `user.getIdToken()` calls at the point of use. As more endpoints are added, this pattern will be copy-pasted everywhere. A thin API client module (`api/auth.ts`, `api/user.ts`) would centralize token injection, base URL, and error handling.

#### 3. `utils/` has one file doing what should be an API call

`utils/saveUserLocation.ts` is really an API call to the backend, not a utility. As more Firestore logic moves to the backend, there will be more of these. The `utils/` folder isn't the right home — it implies local helper logic, not network calls. An `api/` directory makes the intent clear.

#### 4. User profile state is fetched in two places independently

Both `AppStack.tsx` and `HomeScreen.tsx` independently subscribe to `firestore().collection('users').doc(uid).onSnapshot(...)`. This means:
- Two active Firestore listeners for the same document
- Profile state is duplicated in two component subtrees
- If the profile shape changes, two places need updating

The profile should be managed once, in a `UserProfileContext`, and consumed by both components via `useContext`.

#### 5. `AppStack.tsx` does too much

As a navigation file, `AppStack` should only concern itself with which screens exist and how they connect. It currently also:
- Manages `userProfile` state
- Manages `loading` state
- Subscribes to Firestore
- Makes a backend API call (`ensure-profile`)
- Contains routing logic based on profile data

The data fetching and profile state should move to a context provider or a dedicated hook (`useUserProfile`), leaving `AppStack` to just read from context and decide which initial route to use.

#### 6. `types/index.ts` couples the type layer to Firebase

`UserProfile` imports `FirebaseFirestoreTypes.Timestamp` for `createdAt`, `lastLoginAt`, and `location.savedAt`. As the backend takes over Firestore writes and the frontend moves toward reading data via REST rather than direct Firestore access, this coupling will become a problem. Consider using `string` (ISO date) or `number` (epoch ms) for timestamps in the type definitions, which are provider-agnostic.

### Proposed Frontend Layout

```
LoopIn/
├── App.tsx
├── api/                           ← NEW: one module per backend domain
│   ├── auth.ts                    ← register, login calls
│   └── user.ts                    ← updateLocation, ensureProfile calls
├── constants/
│   ├── api.ts                     ← NEW: BACKEND_URL and other API constants
│   └── cities.ts
├── context/                       ← NEW: shared state
│   └── UserProfileContext.tsx     ← single Firestore listener, exposes profile
├── hooks/                         ← NEW: reusable logic
│   └── useUserProfile.ts          ← convenience hook wrapping the context
├── navigation/
│   ├── AppStack.tsx               ← reads from context, no data fetching
│   ├── AuthStack.tsx
│   └── RootNavigator.tsx
├── screens/
│   ├── HomeScreen.tsx             ← reads from context instead of own listener
│   ├── LocationPickerScreen.tsx
│   └── WelcomeScreen.tsx
└── types/
    └── index.ts                   ← decouple from FirebaseFirestoreTypes
```

---

## Summary of Most Impactful Changes

| Priority | Change | Impact |
|----------|--------|--------|
| High | Extract `BACKEND_URL` to `constants/api.ts` | Eliminates the most immediate maintenance pain |
| High | Create `api/auth.ts` and `api/user.ts` | Centralises token injection; makes adding endpoints trivial |
| High | `UserProfileContext` + remove duplicate Firestore listeners | Eliminates redundant network calls, one source of truth for profile |
| Medium | Separate `model/` into `domain/`, `dto/`, `provider/` subpackages | Makes intent of every type immediately obvious |
| Medium | Add `CityService` and `GatheringSpotService` | Restores consistent layering across all controllers |
| Medium | Move `CityRegistry` to `service/` | Puts it with other service-layer components |
| Medium | Create `exception/` package with shared exceptions | Removes cross-service coupling on `AuthService.InvalidTokenException` |
| Low | Rename `City` → `CityJobConfig` | Removes the naming collision between the two "city" concepts |
| Low | Move `AppStack` data-fetching into a hook | Makes the navigation file readable and the logic testable |
| Low | Add Lombok to `City`, `GatheringSpot`, `Place` | Consistency; removes ~200 lines of boilerplate |
