# gather-service — Cleanup Plan

A sequenced remediation plan for the [findings report](./findings-report.md). Ordered low-risk-first within each wave, and dependency-aware (e.g. tests land before risky refactors). Estimated effort: S ≈ <1h, M ≈ 1–3h, L ≈ half-day+.

> **Hard precondition for safe refactoring:** there are currently **no tests**. Waves 3–4 change behavior in the core weekly flow and auth. Land a minimal test harness (H6) *before* those waves, or accept regression risk.

---

## Wave 0 — Zero-risk cleanup (delete dead code & config)
No behavioral change; shrinks surface area before the real work. Safe to do first.

| Item | Action |
|---|---|
| L1 | Delete the `google.job.*` block from `application.yml` |
| L2 | Delete `CityRepository.findByName()` |
| L3 | Delete `Place.id` and `Place.priceLevel` (and drop `priceLevel` from the Places field mask if unused elsewhere) |
| L4 | Decide `GatheringSpot.phoneNumber`: wire it into the email/notification or remove it |
| L7 | `GooglePlacesApiConfig` → Lombok `@Data` |

**Verify:** `./gradlew compileJava` succeeds; grep confirms no references to deleted members.

---

## Wave 1 — Critical correctness fixes (small, high-value, low-blast-radius)
These are surgical and don't require the security framework.

| Item | Action |
|---|---|
| C3 | Capture `DocumentReference` in `GatheringSpotRepository.save()` and `spot.setId(ref.getId())` so `markNotificationSent` works |
| C4 | `@ConditionalOnProperty(name="firebase.enabled", havingValue="true")` on Firebase beans; fail-fast (throw) on `IOException` |
| H4 | Compensating delete of the Auth user when the Firestore profile write fails in `register()` |
| H7 | Null-coalesce FCM data-map values in `PushNotificationService` |
| M5 | Add `zone = "America/Los_Angeles"` to the weekly `@Scheduled` cron |
| M6 | Split the `InterruptedException | ExecutionException` catches; restore interrupt flag only for `InterruptedException` |

**Verify:** build + targeted unit tests (added in Wave 2) cover C3, H4, H7, M6.

---

## Wave 2 — Test harness (H6) — do this before Waves 3–4
Minimum viable safety net, highest-ROI first:

1. `GatheringSpotSyncJob` — random selection + repeat-avoidance edge cases (empty list, all-recently-used).
2. `AuthService.register/login` — happy path + `EMAIL_ALREADY_EXISTS` + Firestore-failure compensation (H4).
3. `GooglePlaceSearchService.convertToPlace` — null/optional fields (relates to H7).
4. `UserService.updateLocation` — valid/invalid city.
5. `@WebMvcTest` slices for `AuthController`, `UserController`, `CityController` — status codes + validation error shape (pins the M1 contract before changing it).

**Verify:** `./gradlew test` green; establish a baseline coverage number.

---

## Wave 3 — Security hardening (the structural fix)
Do as one coherent change; H1 resolves C1/C2 at the root.

| Item | Action |
|---|---|
| H1 | Add `spring-boot-starter-security`; `OncePerRequestFilter` verifying the Firebase token into the `SecurityContext`; `authorizeHttpRequests` rules |
| C1 | Move `/api/admin/**` behind an `admin` custom-claim rule; default the enabled flag to `false` |
| C2 | Require admin claim on city writes; keep `GET` public |
| H5 | `verifyIdToken(token, true)`; map revoked/disabled → 401 |
| M3 | City write request DTO + `@Valid`; set server-managed fields server-side |
| M4 | Clamp `limit`/`weeks` (`@Min/@Max` + `@Validated`); authenticate `/api/places/search` |
| M11 | `CorsConfigurationSource` allowlisting LoopIn origin(s) |
| M2 | HTML-escape email interpolation; `https://`-only URL |
| M10 | Mask/drop PII in logs; uid → DEBUG |

> **Cross-repo guardrail (CLAUDE.md):** before changing auth/CORS/route protection, `grep` the LoopIn workspace for affected `fetch` calls and confirm the frontend sends a Bearer token to the newly-protected routes. Document any required frontend changes.

**Verify:** `@WebMvcTest` + Spring Security test slices assert 401/403 on protected routes; manual smoke against LoopIn.

---

## Wave 4 — Reliability & design refactors (behavioral; needs Wave 2 tests)

| Item | Action |
|---|---|
| H2 | Make the weekly job synchronous-by-design: `Mono.when(...).block()` across cities with `onErrorResume` |
| H3 | Timeouts on all Firestore `.get(...)` calls |
| M7 | Route the job's data access through services (`GatheringSpotService.save`/`markNotificationSent`, `UserService.findByCityId`) |
| M1 | Introduce the global `@RestControllerAdvice`; strip controller try/catch |
| M8 | Single source of truth for cities (validate against `CityRepository` or load registry from Firestore) |
| M9 | Fix the provider abstraction: resolve by name, `@Qualifier` in the controller, rename `googlePlaceId`→`placeId`. **Schema migration** — see note below |

**M9 migration note:** renaming the Firestore field `googlePlaceId`→`placeId` affects existing `gatheringSpots` documents. Options: (a) keep reading the old field as a fallback during a transition window, or (b) run a one-off backfill. Decide before implementing.

---

## Wave 5 — Modernization & polish (optional, cosmetic)

| Item | Action |
|---|---|
| L5 | Reuse a single `Resend` client |
| L6 | Records for immutable DTOs; `Map.of` in `HealthController`; `.toList()` |
| L8 | `application-dev.yml` for zero-config local startup |

---

## Suggested grouping into PRs
1. **PR-1 "dead-code cleanup"** — Wave 0 (+ L5/L6/L8 if convenient).
2. **PR-2 "critical correctness"** — Wave 1.
3. **PR-3 "test harness"** — Wave 2.
4. **PR-4 "security hardening"** — Wave 3 (largest; coordinate with LoopIn).
5. **PR-5 "job reliability & layering"** — Wave 4.

Each PR independently buildable and (from PR-3 on) test-green.
