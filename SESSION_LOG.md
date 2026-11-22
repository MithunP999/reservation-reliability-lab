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
- Commits so far on `slice-1-scaffold-ci`: scaffold + health test + repo
  hygiene, then the CI workflow. Nothing pushed, nothing merged, `main`
  untouched.
