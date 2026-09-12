# CLAUDE.md

Personal finance PWA. Design decisions and phase order live in `PLAN.md`.
How to run, test, contribute and use IntelliJ: `README.md`, `backend/README.md`,
`frontend/README.md` — keep them updated when setup steps change.

## Layout

- `backend/` — Spring Boot 4.1 / Java 21, Maven wrapper, Flyway, Spring Modulith
- `frontend/` — React + Vite + TypeScript 7, react-i18next (English only for now)
- `.github/` — CI (`ci.yml`), Claude review (`claude-review.yml`, `claude.yml`), Dependabot
- `sonar-project.properties` — SonarQube Cloud config for both apps
- `.run/` — shared IntelliJ run configurations

## Commands

```bash
# backend (from backend/)
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify            # tests + JaCoCo report
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run   # starts Postgres via compose.yaml

# frontend (from frontend/)
npm ci --ignore-scripts
npm run dev      # proxies /api to localhost:8080
npm run lint && npm test -- --coverage && npm run build
```

## Git workflow

- `main` is protected by a ruleset: never commit or push to it directly, never force-push it.
  Work on a branch (`feat/`, `fix/`, `chore/`, `docs/`) and open a PR with `gh pr create`.
- Merges are squash-only. Required checks: `backend`, `frontend`, `sonar`, CodeQL, resolved
  review threads. The Claude review is automatic but not required.
- Run the same checks locally before pushing (commands above).
- The repo is public: commit author must be the GitHub noreply address (already the global
  git config).

## Local environment quirks

- Shell `java` is 17; always set `JAVA_HOME` to 21 for Maven.
- Docker is Colima (`colima start` once per boot). The `colima` Maven profile auto-activates
  when the Colima socket exists and points Testcontainers at it. IntelliJ's JUnit runner does
  not use it — see `backend/README.md` for the JUnit template fix.
- The IntelliJ module for `backend/` resolves to the repo root, so run configs must set the
  working directory explicitly. Shared configs live in `.run/`.
- GUI-launched IntelliJ only sees `/usr/local/bin`, not `/opt/homebrew/bin`. CLI tools it
  needs (docker, etc.) are symlinked into `/usr/local/bin`.

## Conventions

- Flyway owns the schema; `ddl-auto=validate`. Migrations `V<n>__<snake_case>.sql`, numbered
  in build order (never reserve numbers for later phases); never edit a merged migration.
  Plural table names, singular entities.
- Money is `amount_minor BIGINT` + `currency CHAR(3)`, never floating point.
- All user-facing strings go through i18n keys in `frontend/src/i18n/locales/en.json`.
- New code needs tests: SonarQube's gate requires ≥ 80% coverage on new code. Only framework
  bootstrap files are excluded from coverage — don't add exclusions to pass the gate.

## CI and security rules

- Third-party GitHub Actions are pinned to a full commit SHA with the version in a comment
  (`uses: owner/action@<sha> # vX.Y.Z`). GitHub-owned `actions/*` may use major tags.
- `npm ci` always with `--ignore-scripts`.
- Workflow logs, PR comments and SonarQube are public: tests and fixtures use made-up data;
  never print secrets or real amounts.
- Spreadsheets, exports and `.env` files are personal data — ignored by git, never commit them.
