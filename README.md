# Financial Manager

[![CI](https://github.com/lbrigidabranco10/financial-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/lbrigidabranco10/financial-manager/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=lbrigidabranco10_financial-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lbrigidabranco10_financial-manager)

Personal finance app: fast expense entry from the iPhone (installed as a PWA) and analytics
on where income goes.

| Folder | What | Details |
| --- | --- | --- |
| [`backend/`](backend/README.md) | Spring Boot 4.1 API, Java 21, Postgres + Flyway | [backend/README.md](backend/README.md) |
| [`frontend/`](frontend/README.md) | React + Vite + TypeScript SPA | [frontend/README.md](frontend/README.md) |

Design, data model and roadmap: [`PLAN.md`](PLAN.md).

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| JDK | 21 | `/usr/libexec/java_home -v 21` |
| Node.js | 24 | `node -v` |
| Docker runtime | Colima | `colima status` |

Colima must be running before the backend starts (local Postgres and the tests use Docker):

```bash
colima start      # once per boot
```

## Running everything

| | URL |
| --- | --- |
| App (frontend) | http://localhost:5173 |
| API (backend) | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |

### From IntelliJ

Run configurations are shared in [`.run/`](.run) and appear automatically in the run
dropdown (top right):

| Configuration | What |
| --- | --- |
| `App` | Backend + Frontend together |
| `Backend` | Spring Boot, working directory `backend/` |
| `Frontend` | `npm run dev -- --host` |

Select one, ▶ to start, ■ (or `Cmd+F2`) to stop.

Use these instead of the ▶ next to `main()` in `FinancialManagerApplication`: that one
creates a configuration whose working directory is the repo root, and startup fails with
`No Docker Compose file found`.

The **Services** tool window (`Cmd+8`) shows everything that is running, including the
Postgres container.

### From the terminal

```bash
# terminal 1
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run

# terminal 2
cd frontend && npm run dev
```

Stop each with `Ctrl+C`.

### Opening it on the iPhone (same Wi-Fi)

Start the frontend with `npm run dev -- --host` and open the `Network:` URL it prints
(e.g. `http://192.168.x.x:5173`) in Safari.

## Making changes

`main` is protected: no direct pushes, no force pushes. Every change goes through a pull
request.

```bash
git switch main && git pull
git switch -c feat/short-description      # or fix/…, chore/…, docs/…
# commit, then:
git push -u origin feat/short-description
gh pr create
```

A PR can be merged (squash only) when:

| Check | What it does |
| --- | --- |
| `backend` | `./mvnw verify` — tests against Testcontainers Postgres, Modulith boundaries, JaCoCo coverage |
| `frontend` | lint, Vitest with coverage, production build |
| `sonar` | [SonarQube Cloud](https://sonarcloud.io/summary/new_code?id=lbrigidabranco10_financial-manager) analysis with coverage; fails on a red quality gate (≥ 80% coverage on new code, no new issues) |
| CodeQL | Security scanning for Java, TypeScript and workflows; blocks on high-severity alerts. Runs through GitHub's code scanning *default setup* (Settings → Code security), so there is no workflow file for it |
| Review threads | All review comments resolved |

Not required, but automatic:

- **Claude review** — runs on every push to a non-draft PR and comments on bugs, security,
  money handling, migrations and tests. Write `@claude` in a PR comment to ask it something.
  On a PR that edits the Claude workflow files, the review is skipped: GitHub runs the
  edited workflow, but the Claude action refuses to authenticate unless the file is
  identical to the one on `main` ("Workflow validation failed" in the job log).
- **Dependabot** — weekly grouped update PRs for Maven, npm and GitHub Actions. Its PRs get
  no secrets, so the Sonar scan and Claude review are skipped on them.

The branch is deleted automatically after merge; locally run `git switch main && git pull`.

## Recommended IntelliJ plugins

Bundled with Ultimate (just enable): Lombok (also enable *Settings → Build → Compiler →
Annotation Processors → Enable annotation processing*), Docker, Maven, Database Tools,
HTTP Client, Kubernetes/Helm.

Worth installing:

| Plugin | Why |
| --- | --- |
| JPA Buddy | Diffs entities against the schema and generates Flyway migrations |
| SonarQube for IDE | Inline bug detection for Java and TypeScript. Bind it to the SonarQube Cloud project (connected mode) to get the same rules as CI |
| Maven Helper | Dependency conflict tree |
| Claude Code [Beta] (vendor: anthropic) | Official plugin; diffs in the IntelliJ viewer, shares selection and diagnostics |

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Port 8080 / 5173 already in use` | Another instance is running. Stop it, or find it with `lsof -i :8080` |
| Backend fails with a Docker / compose error | `colima start` |
| Works in the terminal but IntelliJ says Docker is not installed | Apps opened from the Dock/Toolbox don't read `~/.zshrc`, so they can't see `/opt/homebrew/bin`. Link Docker into `/usr/local/bin`: `sudo ln -sf /opt/homebrew/bin/docker /usr/local/bin/docker` and the same for `docker-credential-osxkeychain` |
| `invalid target release: 21` or class version errors | Maven is running on Java 17 — see *Java 21* in `backend/README.md` |

## Personal data

This repository is public. Real financial data lives only in the production database.

- Spreadsheets and exports (`*.xlsx`, `*.xls`, `*.csv`) and `.env` files are ignored by git.
  Never commit them.
- GitHub Actions logs, PR comments and the SonarQube dashboard are public too — tests and
  fixtures use made-up amounts only.
- Credentials go in GitHub secrets / Cloud Run configuration, never in files. Push
  protection blocks known secret formats.
