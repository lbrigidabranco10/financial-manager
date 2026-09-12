# Frontend

React 19 · Vite 8 · TypeScript 7 · react-i18next · Vitest · oxlint.

Requires Node.js 24. See the [root README](../README.md).

## Setup

```bash
npm ci --ignore-scripts
```

`npm ci` installs exactly what `package-lock.json` pins; `--ignore-scripts` stops dependencies
from running install scripts, same as CI. Use `npm install <package>` only when adding or
upgrading a dependency.

## Run

### IntelliJ

- Start: select **Frontend** in the run dropdown (top right) → ▶.
- Stop: red ■ in the Run tool window, or `Cmd+F2`.

The **Frontend** configuration (`../.run/Frontend.run.xml`) runs `npm run dev -- --host`, so
the app is also reachable from the iPhone on the same Wi-Fi.

### Terminal

```bash
npm run dev              # http://localhost:5173
npm run dev -- --host    # also on the local network, for the iPhone
```

Stop with `Ctrl+C`.

In development, requests to `/api/*` are proxied to the backend on `http://localhost:8080`
(see `vite.config.ts`), so start the backend too when working on anything that loads data.

## Scripts

| Command | What |
| --- | --- |
| `npm run dev` | Dev server with hot reload |
| `npm run lint` | oxlint |
| `npm test` | Vitest, single run |
| `npm test -- --coverage` | Same, plus coverage in the terminal and `coverage/` (as CI runs it). `src/main.tsx` is excluded — it only mounts React |
| `npm run build` | Type-check and production build into `dist/` |
| `npm run preview` | Serve the production build locally |

## Translations

All user-facing text goes through i18n keys — no hard-coded strings in components.

- Strings: `src/i18n/locales/en.json`
- Setup: `src/i18n/index.ts`
- Usage: `const { t } = useTranslation()` → `t('app.name')`

Adding Portuguese later means adding `pt.json` and registering it in `src/i18n/index.ts`.
