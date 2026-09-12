# Backend

Spring Boot 4.1.1 · Java 21 · Maven wrapper · Postgres 17 · Flyway · Spring Data JPA ·
Spring Modulith · Testcontainers.

Requires JDK 21 and a running Colima (`colima start`). See the [root README](../README.md).

## Run

### IntelliJ

**One-time setup**

1. If IntelliJ did not detect the backend: right-click `backend/pom.xml` → **Add as Maven Project**.
2. **File → Project Structure → Project → SDK**: `corretto-21`.
3. **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE**: 21.
   This is a separate setting from the project SDK and both must be 21.

**Start / stop**

- Start: select **Backend** in the run dropdown (top right) → ▶.
- Stop: red ■ in the Run tool window, or `Cmd+F2`.

The **Backend** configuration lives in `../.run/Backend.run.xml` and sets the working
directory to `backend/`, where `compose.yaml` is. Don't use the ▶ next to `main()`: IntelliJ
runs that from the repo root and startup fails with `No Docker Compose file found`.

### Terminal

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

Stop with `Ctrl+C`.

The shell's default `java` is 17, so `JAVA_HOME` must be set on every `./mvnw` call. To avoid
typing it, add to `~/.zshrc`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## Local database

No setup needed. On startup, Spring Boot's Docker Compose support reads `compose.yaml`,
starts a `postgres:17` container and wires the datasource to it. When the app stops, the
container stops too; data survives until the container is removed.

To connect with IntelliJ's Database tool window, find the mapped port with `docker ps`
(e.g. `0.0.0.0:32770->5432`) and use:

| Field | Value |
| --- | --- |
| Host | `localhost` |
| Port | the mapped port from `docker ps` |
| Database | `financial_manager` |
| User | `financial_manager` |
| Password | `local-only` |

To reset the database completely:

```bash
docker compose down -v
```

## Test

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify
```

- `FinancialManagerApplicationTests` starts the full app against a throwaway Postgres
  container (Testcontainers).
- `ModularityTests` fails the build if one module reaches into another module's internals.
- Coverage: `verify` writes a JaCoCo report to `target/site/jacoco/index.html` (open it in a
  browser). CI sends `jacoco.xml` to SonarQube, whose quality gate needs ≥ 80% coverage on
  new code. `FinancialManagerApplication` is excluded — it only boots Spring.

**Colima and Testcontainers.** The `colima` Maven profile activates automatically when
`~/.colima/default/docker.sock` exists and tells Testcontainers where Docker is. It only
applies when *Maven* runs the tests. For the ▶ button next to a test in IntelliJ, add these
once under **Run → Edit Configurations → Edit configuration templates → JUnit →
Environment variables**:

```
DOCKER_HOST=unix:///Users/<you>/.colima/default/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

`TestFinancialManagerApplication` (in `src/test`) runs the app against a Testcontainers
database instead of `compose.yaml`: useful when you want a throwaway database.

## Conventions

- Flyway owns the schema (`ddl-auto=validate`). Migrations go in
  `src/main/resources/db/migration` as `V<n>__<snake_case>.sql`, numbered in the order they
  are written. Never edit a migration that has been merged — add a new one.
- Plural table names, singular entity names.
- Money: `amount_minor BIGINT` + `currency CHAR(3)`, never floating point.
- One top-level package per module (`category`, `transaction`, `recurring`, `analytics`, …).
