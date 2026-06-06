# gather-service — Codebase Audit: Findings Report

**Date:** 2026-06-06
**Scope:** `src/main/java/com/gather/**` (39 files, ~1,886 LOC), `src/main/resources/application.yml`, `build.gradle`
**Method:** Four parallel specialist reviews (bugs/correctness, dead code, security, quality/design), synthesized and de-duplicated. Highest-severity findings independently verified against source.
**Tests in repo:** none (`src/test` is empty).

## Severity summary

| Severity | Count | Theme |
|---|---|---|
| CRITICAL | 4 | Remote-exploitable endpoints + two correctness defects in the core weekly flow |
| HIGH | 7 | Structural auth gap, async job reliability, no tests |
| MEDIUM | 11 | Error handling, validation, abstraction, hygiene |
| LOW | 9 | Dead code, modernization, config cleanup |

Each finding lists the reviewer(s) that raised it. Items raised by more than one reviewer are higher-confidence.

---

## CRITICAL

### C1 — Admin job-trigger endpoint is unauthenticated and on by default
**`controller/AdminJobController.java:16,26`** · _security + bugs_ · **Verified**
`@ConditionalOnProperty(..., matchIfMissing = true)` and `application.yml` default `PLACE_SERVICE_ADMIN_ENABLED=true` mean `POST /api/admin/jobs/weekly-gather` is live with no auth in any environment that doesn't explicitly disable it. A single unauthenticated request runs the weekly job: billable Google Places calls, Firestore writes, and FCM push + Resend emails to **all** users.
**Fix:** Require an authenticated admin (verified Firebase token + `admin` custom claim). Default the flag to `false`. Config is not a security control.

### C2 — City create/update/delete endpoints are unauthenticated
**`controller/CityController.java:44–62`** · _security_ · **Verified**
`POST`/`PUT`/`DELETE /api/cities/**` have no token check. Anyone can create, overwrite, or delete city job configs in Firestore — disabling or hijacking the weekly job (arbitrary `cronSchedule`, `searchTerm`, `topic`, `location`).
**Fix:** Gate writes behind verified token + admin claim. `GET` may remain public.

### C3 — Saved spot ID is never captured → `markNotificationSent` is a permanent no-op
**`repository/GatheringSpotRepository.java:30`**, **`job/GatheringSpotSyncJob.java` (notify path)** · _bugs_ · **Verified**
`collection.add(spot).get()` returns a `DocumentReference` whose `getId()` is the new document key; the return value is discarded, so `spot.id` stays `null`. The job then guards `markNotificationSent(...)` on `spot.getId() != null`, which is always false. Result: `notificationSent` / `notificationSentAt` are never written for any spot.
**Fix:** `DocumentReference ref = ...add(spot).get(); spot.setId(ref.getId());`

### C4 — `FirebaseConfig` returns `null` beans → NPE cascade
**`config/FirebaseConfig.java:32,50,58,72`** · _bugs + quality_ · **Verified**
When `firebase.enabled=false` or credential load throws `IOException`, the `FirebaseApp`/`Firestore`/`FirebaseAuth` beans are registered as `null`. They are injected into `AuthService`, `UserService`, and all repositories without guards, so the first call throws `NullPointerException` instead of a controlled error. Affects local dev (`FIREBASE_ENABLED=false`) and any startup credential misconfiguration.
**Fix:** `@ConditionalOnProperty(name="firebase.enabled", havingValue="true")` on the beans and fail-fast (throw) on `IOException` when Firebase is required; document that `enabled=false` means the service does not serve Firebase-backed routes.

---

## HIGH

### H1 — No `SecurityFilterChain`; authentication is enforced ad-hoc per controller
**`build.gradle` (no `spring-boot-starter-security`); app-wide** · _security_
Only `AuthController` and `UserController` manually parse the `Authorization` header. `AdminJobController`, `CityController` writes, `GatheringSpotController`, and `PlaceSearchController` enforce nothing — "deny by forgetting." This is the **root cause of C1 and C2**, and any new controller is public by default.
**Fix:** Add Spring Security with a `OncePerRequestFilter` that verifies the Firebase ID token once and populates the `SecurityContext`; declare `authorizeHttpRequests` rules (public: `/api/health`, `/api/auth/**`; authenticated: `/api/users/**`, `/api/places/**`; admin-claim: `/api/admin/**`, city writes).

### H2 — Weekly job uses fire-and-forget `subscribe()` — completion not guaranteed, errors swallowed
**`job/GatheringSpotSyncJob.java` (processCity / processDefaultCity)** · _bugs + quality_
`subscribe()` returns immediately; `selectWeeklyGatheringSpot()` finishes while the reactive pipeline is still running on another thread. The outer `try/catch` cannot catch async errors, the admin endpoint returns `202` before any work completes, and a `@Scheduled` run is considered "done" prematurely.
**Fix:** `.block()` (acceptable on the scheduler thread) or `Mono.when(...).block()` across cities, with `doOnError`/`onErrorResume`.

### H3 — Firestore `ApiFuture.get()` called with no timeout
**All repositories + `AuthService`, `UserService`** · _bugs_
Every blocking `.get()` uses the zero-arg overload and can block indefinitely if Firestore is slow/unreachable, exhausting `boundedElastic` worker threads in the job path.
**Fix:** `.get(30, TimeUnit.SECONDS)` everywhere, catching `TimeoutException`.

### H4 — `register()` leaves orphaned Auth user if Firestore write fails
**`service/AuthService.java:41–50`** · _bugs + quality_
The Firebase Auth user is created first; if the subsequent Firestore profile write throws, the user exists in Auth with no profile. Retrying yields `EMAIL_ALREADY_EXISTS` (409) — the user is permanently stuck.
**Fix:** On Firestore failure, `firebaseAuth.deleteUser(uid)` (compensating action) before re-throwing.

### H5 — Firebase ID tokens not checked for revocation
**`service/AuthService.java:76`, `service/UserService.java:85`** · _security_
`verifyIdToken(idToken)` validates signature/expiry but not revocation. A stolen-but-unexpired token (up to 1h) still authenticates after sign-out / account disable / session revoke.
**Fix:** `verifyIdToken(idToken, true)`; handle `REVOKED_ID_TOKEN` / `USER_DISABLED` as 401. (Adds a network round-trip.)

### H6 — Zero automated tests
**`src/test` empty; `build.gradle:45–49` test deps unused** · _quality_
No safety net for any of the refactors below. Highest-ROI first targets: `GatheringSpotSyncJob` selection/repeat-avoidance logic, `GooglePlaceSearchService.convertToPlace`, `AuthService.register/login`, `UserService.updateLocation`, plus `@WebMvcTest` slices for controllers (also pins down error-response shapes).

### H7 — `PushNotificationService` passes potentially-null values into the FCM data map
**`service/PushNotificationService.java:70–75`** · _bugs_
`place.getUrl()` / `getProviderId()` / `getProvider()` can be null (e.g. `googleMapsUri` absent from field mask); FCM rejects null data-map values at runtime (`IllegalArgumentException`).
**Fix:** Coalesce to `""` (or omit) for each value.

---

## MEDIUM

### M1 — No global `@RestControllerAdvice`; duplicated, inconsistent error handling
**Project-wide** · _quality_
Each controller hand-rolls `Map.of("error", ...)`; `AuthController` uses `HttpStatus` constants, `UserController` uses raw int literals, `CityController`/`GatheringSpotController` catch nothing, and `@Valid` failures produce Spring's default body shape. Token extraction is duplicated in `AuthController` and `UserController`.
**Fix:** One `@RestControllerAdvice` mapping `InvalidTokenException`→401, `UnknownCityException`→404, `EmailAlreadyExistsException`→409, `MethodArgumentNotValidException`→400, `RuntimeException`→500; remove controller try/catch.

### M2 — Email HTML built by string concatenation (injection / XSS-in-email)
**`service/EmailService.java:59–66`** · _security + bugs_
`name` (user `displayName`, attacker-controlled) and Google Places `name`/`address`/`url` are concatenated into HTML/`href` with no escaping. Display name is also written unescaped to Firestore.
**Fix:** HTML-escape all interpolated values (`HtmlUtils.htmlEscape`) or use an auto-escaping template; allow only `https://` for `url`.

### M3 — Mass-assignment via domain object as request body
**`controller/CityController.java:45,51`** · _security + quality_
`CityJobConfig` (the Firestore entity) is bound directly from JSON with no `@Valid`; every field is client-settable (`createdAt`, `enabled`, `cronSchedule`, …). With C2 this is fully open.
**Fix:** Dedicated request DTO with only client-editable fields + `@Valid` constraints; set server-managed fields server-side.

### M4 — Unbounded `limit` / `weeks` query params
**`controller/GatheringSpotController.java:36,44`; `controller/PlaceSearchController.java`** · _security_
`?limit=1000000` / huge `weeks` force large/full-scan Firestore reads (cost + latency) on unauthenticated endpoints; negative `limit` throws at the Firestore layer; `/api/places/search` is an unauthenticated proxy to a billable API.
**Fix:** Clamp (`limit` ≤ 50, `weeks` ≤ 52), reject negatives via `@Min/@Max` + `@Validated`; authenticate `/api/places/search`.

### M5 — Weekly cron has no timezone
**`application.yml:31`, `job/GatheringSpotSyncJob.java` `@Scheduled`** · _bugs_
`@Scheduled(cron=...)` defaults to the JVM timezone; on UTC infrastructure "Thursday 09:00" fires at ~01:00–02:00 Pacific. Silent time-of-day bug.
**Fix:** `zone = "America/Los_Angeles"` (or externalize per-city).

### M6 — Interrupt flag mishandled in multi-catch blocks
**`repository/CityRepository.java:39–43` (and `findByName`, `save`, `delete`); `service/AuthService.java:68,93`** · _bugs + quality_
`Thread.currentThread().interrupt()` is called inside `catch (InterruptedException | ExecutionException e)` — i.e. also on `ExecutionException`, spuriously setting the interrupt flag (can disrupt the scheduled executor); conversely `AuthService` never restores it.
**Fix:** Split the catches; restore the flag only for `InterruptedException`.

### M7 — Weekly job bypasses the service layer
**`job/GatheringSpotSyncJob.java:36–38`** · _quality_
Injects `CityRepository`/`GatheringSpotRepository`/`UserRepository` directly (6 deps, ~227 lines), duplicating what `GatheringSpotService`/`CityService`/`UserService` should own; hurts testability.
**Fix:** Route data access through services; add `GatheringSpotService.save` + `markNotificationSent`, `UserService.findByCityId`.

### M8 — `CityRegistry` hard-codes cities; diverges from Firestore
**`service/CityRegistry.java:11–28`** · _quality_
Seattle/Tacoma are a hard-coded `Map.of`; `UserService.updateLocation` validates against it, so a city added to the Firestore `cities` collection silently fails user location validation. Two sources of truth drift.
**Fix:** Validate against `CityRepository`, or load the registry from Firestore at startup.

### M9 — Provider abstraction is broken end-to-end
**`config/PlaceSearchServiceConfig.java:25–33`, `controller/PlaceSearchController.java:22`, `model/domain/GatheringSpot.java:13,27–29`, `repository/GatheringSpotRepository.java:84–92`** · _quality + bugs_
Both `if/else` branches of `activePlaceSearchService` return the same bean; `PlaceSearchController` injects the concrete `GooglePlaceSearchService`; the domain field is named `googlePlaceId` with `if ("google".equals(...))` branches; `findRecentPlaceIds` returns an empty list for any non-Google provider — silently **disabling repeat-avoidance** for future providers.
**Fix:** Resolve provider from `List<PlaceSearchService>` by name (fail loudly if missing); inject via `@Qualifier("activePlaceSearchService")`; rename `googlePlaceId`→`placeId` (provider-agnostic) — note this is a Firestore schema change requiring a migration plan.

### M10 — PII (email / uid) logged at INFO
**`service/EmailService.java`, `service/AuthService.java:54,83`, `service/UserService.java:58`** · _security_
Emails and uids land in INFO logs.
**Fix:** Drop/mask email; log uid at DEBUG; ensure log sinks are access-controlled.

### M11 — No CORS configuration for the LoopIn frontend
**App-wide** · _security_
No `@CrossOrigin`/`CorsConfigurationSource`. The intended LoopIn origin is implicit, not allowlisted (per the CLAUDE.md cross-repo guardrail).
**Fix:** Configure a `CorsConfigurationSource` allowlisting the specific LoopIn origin(s) when adding Spring Security; never `*` with credentials.

---

## LOW — dead code, config & modernization

### L1 — Dead `google.job.*` config block
**`application.yml:15–21`** · _dead code + quality_ · **Confirmed unused**
No `@Value`/`@ConfigurationProperties` binds `google.job.*`; the job reads only `place-service.job.*`. The two blocks even disagree (`google.job.search-limit=20` vs the `place-service` default of 50), which is misleading. **Delete.**

### L2 — Unused method `CityRepository.findByName()`
**`repository/CityRepository.java:61–79`** · _dead code_ · **Confirmed unused (no callers)** · **Delete.**

### L3 — Unused fields `Place.id`, `Place.priceLevel`
**`model/domain/Place.java:11,22`** · _dead code_ · `id` never read (`providerId` is used instead); `priceLevel` is set from the API but never read. **Delete** (and drop `priceLevel` from the field mask if nothing else needs it).

### L4 — `GatheringSpot.phoneNumber` set but never read
**`model/domain/GatheringSpot.java:21`** · _dead code_ · **Investigate** (intended for display?) then keep or remove.

### L5 — `Resend` client instantiated per email send
**`service/EmailService.java:36`** · _quality_ · Create once (constructor / field); reuse for connection pooling.

### L6 — Java 21 modernization
**Multiple** · _quality_ · Convert immutable DTOs (`AuthResponse`, `LocationResponse`, Google request/response) to `record`s; `Map.of(...)` in `HealthController:17–19`; `.toList()` instead of `Collectors.toList()` (`GooglePlaceSearchService`, `GatheringSpotSyncJob`, `GatheringSpotRepository`). Keep Firestore-mapped domain classes as no-arg-constructor beans.

### L7 — `GooglePlacesApiConfig` hand-written getters/setters
**`config/GooglePlacesApiConfig.java`** · _quality_ · Lombok is already on the classpath — `@Data`, or constructor-bound `@ConfigurationProperties`.

### L8 — No dev/prod profile separation
**`application.yml`** · _quality_ · Single file; local startup defaults to `FIREBASE_ENABLED=true` (crashes without creds). Add `application-dev.yml` (`firebase.enabled=false`, `resend.enabled=false`, `admin.enabled=true`) for zero-config local runs.

---

## Verified non-issues (good news)
- **No committed secrets:** `.env` and `firebase-service-account.json` are gitignored and absent from `git log --all`; `.env.example` holds placeholders; no hardcoded keys (all `${ENV}`).
- **uid sourced correctly:** location/profile updates derive uid from the verified token, not client input — no profile IDOR.
- **No commented-out code or TODO/FIXME** markers found.
- Dependencies (Spring Boot 3.2.1, firebase-admin 9.2.0) are dated but not flagged CVE-critical from the manifest; a Spring Boot 3.x patch bump is advisable.
