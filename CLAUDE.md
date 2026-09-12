# CLAUDE.md

Personal finance PWA. Design decisions and phase order live in `PLAN.md`.
How to run, test and use IntelliJ: `README.md`, `backend/README.md`, `frontend/README.md` —
keep them updated when setup steps change.

## Layout

- `backend/` — Spring Boot 4.1 / Java 21, Maven wrapper, Flyway, Spring Modulith
- `frontend/` — React + Vite + TypeScript, react-i18next (English only for now)

## Commands

```bash
# backend (from backend/)
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run   # starts Postgres via compose.yaml

# frontend (from frontend/)
npm run dev      # proxies /api to localhost:8080
npm run lint && npm test && npm run build
```

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

- Flyway owns the schema; `ddl-auto=validate`. Migrations `V<n>__<snake_case>.sql`, plural
  table names, singular entities.
- Money is `amount_minor BIGINT` + `currency CHAR(3)`, never floating point.
- All user-facing strings go through i18n keys in `frontend/src/i18n/locales/en.json`.
- Spreadsheets and exports are personal data — ignored by git, never commit them.
