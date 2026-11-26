# Session Log

## Slice 1: scaffold-ci (branch `slice-1-scaffold-ci`)

- Generated the project with Spring Initializr (Boot 4.2.0, Java 21, groupId
  `io.github.mithunp999`, artifactId `reservation-lab`, package
  `io.github.mithunp999.reservationlab`, dependencies `web,validation,actuator`).
  Deleted only `HELP.md` from the generated zip.
- Added `* text=auto` to the generated `.gitattributes` (kept its
  `mvnw eol=lf` / `*.cmd eol=crlf` rules).
- Pinned `io.rest-assured:rest-assured:6.3.0` as a test dependency in `pom.xml`
  (not managed by the Boot BOM). No other managed versions overridden.
- Marked `mvnw` executable in git (`git update-index --chmod=+x mvnw`) — verified
  `git ls-files -s mvnw` is `100755` and `git ls-files --eol mvnw` is `i/lf`.
- Renamed `application.properties` to `application.yml`; exposed only the
  `health` actuator endpoint (`management.endpoints.web.exposure.include=health`).
- Added `common/ClockConfig.java` with a `Clock.systemUTC()` bean for later
  slices to inject.
- Added `HealthApiTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` +
  REST Assured) asserting `GET /actuator/health` returns 200 and
  `status == "UP"`. Kept the Initializr-generated
  `ReservationLabApplicationTests` smoke test as is.
- Extended `.gitignore` with `CLAUDE.local.md`, `.env`, `.env.*`, `.allure/`,
  `allure-results/`.
- Added `.claude/settings.json` with deny rules on force-push/push-to-main/
  merge/`gh pr merge` (Bash + PowerShell) and an ask rule on any other
  `git push`; denies reading `.env`/`.env.*`.
- Added `.github/pull_request_template.md` with the four required sections.
- Added `.github/workflows/ci.yml` (provided by Max): runs `./mvnw -B verify`
  on push to `main` and on pull requests, Java 21 Temurin via
  `actions/setup-java@v6`.
- Verified locally: `.\mvnw.cmd -B verify` → BUILD SUCCESS, 2/2 tests pass
  (local `JAVA_HOME` had to be overridden to the installed JDK 21 for the
  build command — the machine's default `JAVA_HOME` points at JDK 11).
- Squash-merged to `main` as PR #1 (commit 63e7fd0); branch `slice-1-scaffold-ci` deleted.

## Slice 2: booking-state-machine (branch `slice-2-booking-state-machine`)

- Branched from `main`. Reordered `.gitattributes` so `* text=auto` is first and the specific `/mvnw` and `*.cmd` rules come after it.
- Wrote tests first: `MutableClock` (getZone/withZone/instant/advance), `BookingExpiryTest`, `BookingTest`; `ReservationLabApplicationTests` now asserts `booking.hold-duration` binds to 10m. `BookingStatusTransitionTest` left to Max.
- Added `BookingStatus.canMoveTo` (switch, no default), `IllegalTransitionException`, `Booking` record, `BookingProperties` (+ `booking.hold-duration: 10m`, `@ConfigurationPropertiesScan`), `BookingRepository` + `InMemoryBookingRepository`.
- `.\mvnw.cmd -B verify` -> BUILD SUCCESS, 9 tests pass, no `Thread.sleep` in `src/`. Committed on the branch; nothing pushed.
- Fixed misplaced `junit-jupiter-params` dependency in `pom.xml` (was outside `<dependencies>`); added Max's `BookingStatusTransitionTest`. `verify` green, 34 tests.

## Slice 3: booking-api (branch `slice-3-booking-api`)

- Branched from `main` (b7291c1, slice 2 merged as PR #2). Housekeeping commit: merged `includeCoAuthoredBy: false` into the existing `.claude/settings.json` (kept its deny/ask rules) and added the no-attribution rule to `CLAUDE.md`. `spring-boot-starter-validation` was already in `pom.xml`.
- Test support first: `MutableClock.reset`, `TestClockConfig` (@Primary MutableClock), `ApiTestBase` (RANDOM_PORT, resets clock + repository), `HealthApiTest` moved onto it; wrote `BookingApiTest` (create, get, 404, cancel, cancel twice 409, expiry).
- Added `CreateBookingRequest` (seats `Integer`, @NotNull @Min(1) @Max(6)), `BookingResponse`, `BookingNotFoundException`, `BookingService`, `BookingController`, `InMemoryBookingRepository.deleteAll()`, and `common/ApiExceptionHandler` (VALIDATION_FAILED for bean validation and unreadable bodies, BOOKING_NOT_FOUND, ILLEGAL_TRANSITION; ProblemDetail + `code`).
- `.\mvnw.cmd -B verify` -> BUILD SUCCESS, 40 tests. Committed on the branch; nothing pushed.

## Slice 4: allure-pages (branch `slice-4-allure-pages`)

- Branched from `main` (ac5674a, slice 3 merged as PR #4). Local `main` was stale, fast-forwarded first.
- `pom.xml`: `allure-bom` 3.1.1 in `dependencyManagement`; test deps `allure-jupiter` and `allure-rest-assured` (no versions, no allure-junit5 2.x, no AspectJ); `allure-maven` 3.1.0 with `reportVersion` 3.17.0.
- `src/test/resources/allure.properties`: results go to `target/allure-results`. `.gitignore` already covered `.allure/`, `allure-results/` and `target/`.
- `ApiTestBase`: `AllureRestAssured` filter registered once behind a static guard in `resetState()`.
- Added `@Feature`/`@Story`/`@DisplayName` to `HealthApiTest`, `BookingApiTest`, `BookingValidationApiTest` (class renamed from `BookingValidationAPITest` to match its file name).
- `ci.yml`: kept `build` job and triggers; added workflow `permissions: contents: read`; upload `allure-results` with `if: always()`; on push to main only: `allure:report` + `upload-pages-artifact@v5` (no `always()`); new `deploy` job (needs build, pages: write + id-token: write, github-pages environment, concurrency group `pages` without cancel, configure-pages@v6, deploy-pages@v5).
- `.\mvnw.cmd -B verify` -> BUILD SUCCESS, 48 tests. `allure:report` wrote `target/site/allure-maven-plugin/index.html` (path matches ci.yml, no change needed). Each HTTP call produces exactly one "HTTP exchange" attachment (request + response together), so the filter is not stacked. Committed on the branch; nothing pushed.

## Slice 5: payment-endpoint (branch `slice-5-payment-endpoint`)

- Branched from `main` (2254663, slice 4 merged as PR #6). Housekeeping commit: `Bash(gh pr merge:*)` added to the `deny` list in `.claude/settings.json`.
- Tests first: `FakePaymentGateway` + `TestGatewayConfig` (@Primary), `ApiTestBase` resets the fake, `PaymentApiTest`, `InMemoryBookingRepositoryTest`. The "pay after expiry" test is left to Max.
- Added `gateway/` (`PaymentGateway`, `ChargeRequest`, `ChargeResult`, placeholder bean that throws "gateway not configured") and `payment/` (`POST /bookings/{id}/payments`: refresh expiry, claim HELD -> PAYMENT_PENDING with `replace`, then call the gateway; 201 on PENDING, 402 PAYMENT_DECLINED and back to HELD on DECLINED).
- `BookingRepository.replace` (compare-and-set). `BookingService.get`/`cancel` now use it in a retry loop, so a stale write cannot overwrite a payment claim.
- `spring.jackson.deserialization.accept-float-as-int: false`: without it the `amountMinor: 10.9` test row failed (value was accepted); with it, 400. Also rejects decimal `seats`.
- No Idempotency-Key handling yet (slice 6). Each charge attempt uses its new paymentId as the gateway idempotency key. Payments are not stored.
- Known gap: if the gateway call throws, the booking stays PAYMENT_PENDING and the API returns 500 (with the placeholder bean, every production call); slice 7a resolves this with timeout handling.
- `.\mvnw.cmd -B verify` -> BUILD SUCCESS, 61 tests, no `Thread.sleep` in `src/`. Committed on the branch; nothing pushed.
- Added the pay-after-expiry test (written by Max): 409, no gateway call, booking EXPIRED. `verify` -> BUILD SUCCESS, 62 tests.

## Slice 6: idempotency (branch `slice-6-idempotency`)

- Branched from `main` (1198465, slice 5 merged as PR #8).
- Tests first: `FakePaymentGateway.failNext(n)`; `ApiTestBase` resets the idempotency store and now holds the shared `createHeld`/`pay`/`assertStatus` helpers (`pay` sends an `Idempotency-Key`); `IdempotencyApiTest`, `IdempotencyStoreTest`, `ConcurrentDifferentKeyPaymentTest` (@RepeatedTest(20), 10 threads, one latch). `ConcurrentDoublePaymentTest` (same key) is left to Max.
- `POST /bookings/{id}/payments` requires `Idempotency-Key` (`[A-Za-z0-9_-]{1,64}`), else 400 IDEMPOTENCY_KEY_REQUIRED. An invalid body still returns 400 VALIDATION_FAILED first.
- `payment/IdempotencyStore`: the store's atomicity comes from a single `ConcurrentHashMap.compute` per key, which decides NEW / RESUME / REPLAY / IN_PROGRESS / REUSED. After that, only the request that set IN_PROGRESS changes the entry, with `replace(key, owned, next)`. No get-then-put. Fingerprint = record `(bookingId, amountMinor, currency)`: exact equality in memory, no hashing needed.
- Stored and replayed (with `Idempotent-Replayed: true`): 201, 402 PAYMENT_DECLINED, 409 ILLEGAL_TRANSITION, 404. Not stored: 400, 409 IDEMPOTENCY_IN_PROGRESS, 422 IDEMPOTENCY_KEY_REUSED, 503 GATEWAY_UNAVAILABLE.
- Gateway throws: booking stays PAYMENT_PENDING, entry RETRYABLE, 503. Retry with the same key skips the booking claim and charges again with the same key and paymentId. The gateway now gets the client's key, not the paymentId.
- On decline, the booking is released to HELD before the 402 is stored.
- `PaymentDeclinedException` removed; shared `common/Problems` and `common/ApiProblemException` added.
- Mutation check: with the booking claim changed to a plain `save`, `ConcurrentDifferentKeyPaymentTest` failed 8 of 20 repetitions. Reverted.
- Out of scope: keys never expire or get cleaned up (the map grows without bound), and the store is lost on restart.
- `.\mvnw.cmd -B verify` -> BUILD SUCCESS, 97 tests, no `Thread.sleep` in `src/`. Committed on the branch; nothing pushed.

## Slice 7a: wiremock-retries (branch `slice-7a-wiremock-retries`)

- Branched from `main` (c3fb2c2, slice 6 merged as PR #10).
- `pom.xml`: `spring-boot-starter-restclient`; test deps `wiremock-spring-boot-standalone` 4.4.3 and `awaitility` (no version).
- Test wiring: shared setup moved to `support/ApiTestSupport`; `ApiTestBase` extends it and adds the @Primary fake gateway. `GatewayRetryApiTest` extends `ApiTestSupport`, so the real `HttpPaymentGateway` is its only gateway.
- `gateway/`: `GatewayProperties` (connect 1s, read 2s, 3 attempts), `GatewayClientConfig` (explicit `RestClient` on an HTTP/1.1 `HttpClient`), `HttpPaymentGateway` (replaces the "not configured" placeholder; 402 -> DECLINED), `RetryPolicy` + `Sleeper` (backoff 200ms, 400ms; same Idempotency-Key). `application.yml` has a fictional local `gateway.base-url`.
- `RetryPolicy.isRetryable` (Max's part): retry on 5xx or any `IOException` in the cause chain; never 4xx.
- Retries exhausted: slice 6's path, unchanged (entry RETRYABLE, booking stays PAYMENT_PENDING, 503, not stored). `PaymentService` was not modified.
- The first gateway call in a fresh JVM took about 650ms, above the test's 300ms read timeout, so `GatewayRetryApiTest` warms up the client once (Awaitility) and then resets WireMock.
- Out of scope: callbacks (slice 7b).
- `.\mvnw.cmd -B clean verify` -> BUILD SUCCESS, 121 tests, no `Thread.sleep(...)` calls in `src/` (the production `Sleeper` bean is `Thread::sleep`).
