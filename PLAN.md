# PLAN.md — Financial Manager

Status: **Phase 0 done** (2026-09-12). Next: Phase 1.

---

## 1. Decisions

| Topic | Decision |
| --- | --- |
| Users | Single user, Google OAuth, email allowlist |
| Accounts | None — one global total |
| Currency | EUR only in the UI; schema is multi-currency from day one |
| Language | English first, i18n wired from day one (`react-i18next`), Portuguese added later |
| Hosting | One Cloud Run container serves API + SPA; no Cloudflare Pages |
| Income | Configured through forms on first login, with effective-dated amounts (§3.2) |
| Spreadsheet | Reference only — its numbers are typed into the income forms, never imported |
| Repo | Private GitHub repo |

## 2. Architecture

```
iPhone (PWA, Home Screen)
   │  HTTPS, same origin
   ▼
Cloud Run  ── one container: Spring Boot API + built React SPA as static files
   │
   ▼
Neon Postgres (free tier)
```

**SPA served by Spring Boot, not Cloudflare Pages.** Safari blocks
third-party cookies; with the SPA on `*.pages.dev` and the API on `*.run.app`
the session cookie would be cross-site and login would silently fail on the
iPhone. One origin removes that and CORS. The service worker caches the app
shell, so the PWA opens instantly even while Cloud Run cold-starts.

**Sessions in Postgres (Spring Session JDBC).** With `min-instances=0` an
in-memory session dies with the instance. 30-day sliding session.

**Neon:** pooled connection string, Hikari `maximum-pool-size: 3`.

**Why Neon.** Free tier 0.5 GB, autosuspends and resumes in under a second. Supabase
also has a free tier but pauses projects after 7 days idle and needs a manual unpause.
Avoid Cloud SQL (~$10/month) and Memorystore (~$35/month): no free tier.

**Cold start.** Scale-to-zero means the first request after idle takes 2–5 s on a plain
JVM. Spring Boot's AOT + CDS with a training run in the Docker build brings that to about
a second. GraalVM native would reach ~100 ms, but the Hibernate reflection configuration
is not worth it at this size. Decision: **CDS, accept the second.**

**Images in Artifact Registry, not GHCR.** Cloud Run
cannot pull from `ghcr.io` directly. Artifact Registry's free tier is 0.5 GB;
a cleanup policy keeping the last 3 images stays under it.

**GitHub Actions on a private repo:** GitHub Free includes 2,000 Linux runner
minutes/month for private repos. A build here is ~5 minutes, so roughly 400
builds/month. Use `ubuntu-latest` only (macOS runners bill at 10×).

### Stack

- Backend: Spring Boot 4.1.x, Java 21, Maven wrapper, Spring Security OAuth2
  Login, Spring Data JPA, Flyway, Spring Session JDBC, Spring Modulith.
- Frontend: React + Vite + TypeScript, `vite-plugin-pwa`, TanStack Query,
  `react-i18next`, `idb` (offline queue), Recharts.
- Tests: JUnit 5 + Testcontainers Postgres, Vitest.
- CI/CD: GitHub Actions → Artifact Registry → Cloud Run.

### Backend modules

| Package | Responsibility |
| --- | --- |
| `category` | Categories, recent-use ordering |
| `transaction` | Actual expenses and incomes, idempotent upsert |
| `recurring` | Recurring items (salary, subsidies, rent, subscriptions), effective-dated amounts, expected-per-month calculation, auto-generation |
| `analytics` | Read-only SQL aggregations, expected vs actual |
| `currency` | Enabled currencies, `Money` value object |
| `security` | Google login, allowlist, API tokens for Siri Shortcut |

## 3. Data model

Money is **integer minor units** (`amount_minor BIGINT`) plus a `currency`
code — never floats. No FX conversion until a second currency exists.

### 3.1 Core

```
currencies            categories                 transactions
──────────            ──────────                 ────────────
code  CHAR(3) PK      id          UUID PK        id                UUID PK  ← client-generated
enabled BOOL          kind        VARCHAR        kind              VARCHAR  EXPENSE | INCOME
                      name        VARCHAR        amount_minor      BIGINT   > 0
                      icon        VARCHAR        currency          CHAR(3)  FK
                      color       VARCHAR        category_id       UUID     FK
                      sort_order  INT            occurred_on       DATE
                      archived    BOOL           note              VARCHAR NULL
                      UNIQUE(kind,name)          recurring_item_id UUID NULL FK
                                                 period            DATE NULL  first day of the month it belongs to
                                                 created_at, updated_at
                                                 UNIQUE(recurring_item_id, period)
```

Client-generated UUIDs make offline replay safe: `PUT /api/transactions/{id}`
twice is a no-op, not a duplicate.

### 3.2 Recurring items — income configuration and fixed expenses

One concept covers salary, subsidies, bonus, meal card, capitalization, rent
and subscriptions.

```
recurring_items                              recurring_item_amounts
───────────────                              ──────────────────────
id            UUID PK                        id               UUID PK
kind          VARCHAR  INCOME | EXPENSE      recurring_item_id UUID FK
name          VARCHAR  "Salary (bank)"       amount_minor     BIGINT
category_id   UUID FK                        currency         CHAR(3) FK
months        SMALLINT[] NULL                effective_from   DATE     first day of a month
              NULL = every month;            UNIQUE(recurring_item_id, effective_from)
              {6,11} = June and November
day_of_month  SMALLINT
mode          VARCHAR  CONFIRM | AUTO
start_on      DATE
end_on        DATE NULL
created_at, updated_at
```

**Salary raises.** An amount applies from `effective_from` until the next row.
A raise in September 2027 is a new row `effective_from = 2027-09-01`: past
months keep the old amount, future months use the new one, and analytics for
2026 does not change. The form shows the history ("€1,000 since Sep 2026 →
€1,100 from Sep 2027") and lets you schedule a future change in advance.

**Expected amount for a month** = the item is active that month (start/end,
`months`), using the latest amount row with `effective_from <= month`. Computed
on the fly; nothing is stored for the future.

**Mode:**

- `CONFIRM` (incomes, variable bills): the month's expected entries appear in
  a *To confirm* list, pre-filled. Tap to accept or correct the amount from the
  payslip → creates the real transaction linked by `(recurring_item_id, period)`.
  The unique constraint prevents confirming twice.
- `AUTO` (rent, subscriptions): transactions are created automatically. The
  first request of each day runs a catch-up for due occurrences (a Cloud Run
  service at zero instances cannot run `@Scheduled` jobs); the same unique
  constraint makes concurrent catch-ups safe.

The meal card follows working days and the bonus varies, so the configured
amount is only an estimate; confirming with the real number is the point of
`CONFIRM`.

### 3.3 Security

```
api_tokens
──────────
id, name, token_hash (SHA-256), last_used_at, revoked_at, created_at
```

### 3.4 Flyway migrations

```
V1__create_currencies.sql               seed EUR
V2__create_categories.sql               seed default categories
V3__create_recurring_items.sql          items + amounts
V4__create_transactions.sql
V5__create_spring_session.sql           Spring's schema — the one exception to plural naming
V6__create_api_tokens.sql
```

### 3.5 Default categories (renameable)

- **Expense:** Groceries, Restaurants, Transport, Car, Home, Utilities, Health,
  Leisure, Shopping, Subscriptions, Travel, Other.
- **Income:** Salary, Holiday & Christmas subsidies, Bonus, Meal card,
  Capitalization insurance, Other.

Category names are user data in the database, so translating the UI does not
touch them.

## 4. API

Under `/api`, session cookie unless noted.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/me` | includes `onboardingCompleted` |
| GET | `/categories?kind=` | ordered by last 30 days' use |
| POST / PATCH | `/categories[/{id}]` | archive instead of delete |
| PUT | `/transactions/{id}` | idempotent create-or-update |
| GET | `/transactions?from&to&kind&categoryId&cursor` | keyset pagination |
| DELETE | `/transactions/{id}` | |
| GET / POST / PATCH / DELETE | `/recurring-items[/{id}]` | |
| POST | `/recurring-items/{id}/amounts` | schedule a new amount from a date |
| DELETE | `/recurring-items/{id}/amounts/{amountId}` | only future ones |
| GET | `/recurring-items/pending?until=` | expected `CONFIRM` entries not yet confirmed |
| POST | `/recurring-items/{id}/confirm` | `{period, amount?}` → creates transaction |
| GET | `/analytics/summary?from&to` | income, expenses, net, savings rate |
| GET | `/analytics/by-category?kind&from&to` | totals + % share |
| GET | `/analytics/monthly?months=12` | actual income vs expense per month, plus expected income |
| POST / DELETE | `/tokens[/{id}]` | raw token returned once |
| POST | `/quick/expense` | **Bearer token**; `{amount, category?, note?}` — Siri Shortcut |

## 5. Screens (mobile-first)

0. **Onboarding** (first login, reachable later from Settings) — step-by-step
   forms with templates prefilled from the spreadsheet's structure: salary to
   bank, meal card, capitalization, holiday + Christmas subsidies (June /
   November), optional bonus (September). Each asks amount + "since when".
   Skippable.
1. **Add** (home) — amount + keypad → category chips (recent first) → Save.
   Expense by default, one tap for income. Banner when there are entries *to
   confirm*. Badge for unsynced offline entries.
2. **History** — grouped by day, filter by month/category, edit, swipe delete.
3. **Analytics** — month picker; income / expenses / net / savings-rate tiles;
   expenses by category; 12-month trend; expected vs actual income.
4. **Settings** — recurring items (with amount history), categories, Shortcut
   tokens, sign out.

Offline: entries go to IndexedDB first. iOS PWAs have no Background Sync API,
so the queue flushes when the app opens or comes back online.

## 6. Phases

| # | Phase | Done when |
| --- | --- | --- |
| 0 | **Skeleton** — git, Maven backend, Vite frontend with i18n, docker-compose Postgres, Testcontainers base test, Modulith verify test, GitHub Actions build | `./mvnw verify` and `npm run build` green locally and in CI |
| 1 | **Core** — V1, V2, V4, V5; categories + transactions API; Google login + allowlist + JDBC sessions | integration tests pass; login works locally |
| 2 | **Entry + deploy** — Add + History, PWA, offline queue; Dockerfile with CDS; Neon + Artifact Registry + Cloud Run | expense added from the iPhone Home Screen, including in airplane mode |
| 3 | **Income & recurring** — V3, recurring items + amounts, onboarding forms, *To confirm* flow, AUTO catch-up | a scheduled raise changes only months after its date |
| 4 | **Analytics** — endpoints + screen, expected vs actual | month and 12-month views correct against test data |
| 5 | **Siri** — V6, tokens, `/quick/expense`, Shortcut setup notes | "Hey Siri, add expense" creates a transaction |
| 6 | **Later** — Portuguese translation, budgets per category, CSV export, second currency, `k8s/` + Helm on kind | — |

**Kubernetes practice (Phase 6).** Production stays on Cloud Run. Write real `k8s/`
manifests and a Helm chart, run them locally on kind or k3s for free, and create a GKE
cluster only for an afternoon. The first zonal cluster has no management fee, but nodes
cost ~$25/month if left running — delete the cluster the same day.

## 7. Costs

**€0/month**: Cloud Run free tier, Neon free tier, Artifact Registry ≤ 0.5 GB,
GitHub Free private repo with 2,000 Action minutes. Optional domain ~€10/year.
Set a GCP budget alert at €1 as a safety net.
