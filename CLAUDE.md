# Rules for Claude Code in this repo

- This repo is public and fully fictional (a made-up event/workshop seat booking service). Never add client code, client data, real company names, real endpoints, tokens or credentials.
- Stack: Java 21 (Temurin), Spring Boot 4.2.0, Maven 3.10.1 via the wrapper (`./mvnw`, `.\mvnw.cmd` on Windows), JUnit Jupiter (Boot-managed), AssertJ, REST Assured 6.3.0, WireMock (`wiremock-spring-boot-standalone` 4.4.3), Awaitility (Boot-managed), Allure Java 3.1.1 (`allure-bom` → `allure-jupiter` and `allure-rest-assured`), `allure-maven` 3.1.0, GitHub Actions.
- Pin versions only for libraries Spring Boot doesn't manage (REST Assured, WireMock Spring Boot, Allure BOM, plugins). Never override a Boot-managed version.
- Boot 4 names: `spring-boot-starter-webmvc`, `spring-boot-starter-restclient` (needed for `RestClient.Builder`), module test starters, Jackson 3 (`tools.jackson`), `@MockitoBean` (not `@MockBean`). For the gateway client, use `JdkClientHttpRequestFactory` on an `HttpClient` pinned to `HTTP_1_1` with explicit connect and read timeouts.
- Package by feature under `io.github.mithunp999.reservationlab` (booking, payment, gateway, webhook, common). Java records, no Lombok.
- One slice = one issue = one branch (`slice-N-short-name`). Ask Max before every push, and push only a feature branch after he says yes. Never commit or push to `main`. Never merge. Max reviews and squash-merges every pull request by hand.
- Start every slice in plan mode and wait for approval. Leave the piece marked "I write this myself" to Max.
- Before stopping, run `./mvnw -B verify` and make sure it is green.
- Tests first for each transition and failure case. No `Thread.sleep`: inject `java.time.Clock` (`MutableClock` in tests), use a no-op sleeper for retries and Awaitility for async.
- Money is `long` minor units, never `double`.
- Static imports: WireMock's `equalTo` clashes with Hamcrest's. In REST Assured assertions use `org.hamcrest.Matchers.equalTo`.
- Test state: every API test extends `ApiTestBase`, which resets the clock, fake gateway, repository and idempotency store before each test. Set the Allure REST Assured filter once (a shared `RequestSpecification` or a static guard), not in every `@BeforeEach`.
- Retry rule (gateway): retry on 5xx, or when any cause in the exception chain is an `IOException` (this covers `SocketTimeoutException` and connection refused). Never retry 4xx, especially not 402.
- PR description in plain words: what changed, how to run it, what it tests, what you did not do.
- If a requirement is unclear, ask instead of guessing.
- Never add Co-Authored-By or 'Generated with Claude' lines to commits, PR titles or PR bodies.
