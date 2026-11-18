# Project: UTH Connect Web Portal — Vercel Preview Deployment

## Architecture
- Framework: Next.js 14.2.35 (App Router, React 18.3.1, TypeScript 5.7.3).
- Architecture & State: Refine (`@refinedev/core` 4.58.0, `@refinedev/supabase` 5.9.8, `@tanstack/react-query` 4.36.1).
- Backend & Session: Supabase SSR (`@supabase/ssr` 0.5.2, `@supabase/supabase-js` 2.49.1).
- Design System: Anti-AI Design System (`docs/web/ANTI_AI_DESIGN_SYSTEM.md`), TailwindCSS 3.4.17, Lucide Icons.
- Deployment Platform: Vercel (Preview deployment, Root Directory: `web`, Framework Preset: `Next.js`).
- Monorepo Isolation: Android app at workspace root (`d:\APP\UTH-Connect`), Web Portal under `web/`.

## Feature Inventory
| # | Feature | Description | Milestone | Source | Status |
|---|---------|-------------|-----------|--------|--------|
| F1 | Vercel Monorepo Root Directory | Configured in Vercel Project Settings (`uth-connect-web` -> `web`) | M1 | Survey (spec_miner_docs, explorer_vercel_env) | DONE |
| F2 | Environment Variables Provisioning | Configured `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` on Vercel | M1 | Survey (explorer_vercel_env, ORIGINAL_REQUEST) | DONE |
| F3 | Web Production Build Integrity | Executed `npm run build` in `web/` generating 21 routes with exit code 0 | M1 | Survey (explorer_web_codebase, ORIGINAL_REQUEST) | DONE |
| F4 | Git & Artifact Sanitation | Ensured `.vercel` is ignored in root and `web/.gitignore`, 0 secrets leaked | M1 | Survey (explorer_vercel_env) | DONE |
| F5 | Vercel Preview Deployment | Executed preview deployment via Vercel MCP plugin (`prj_KiOeJtlZcQVKJ88sYzxX1oLkHAJQ`) | M1 | Survey (explorer_vercel_env, ORIGINAL_REQUEST) | DONE |
| F6 | Preview Root Route HTTP 200 Check | Verified HTTP GET `/` returns HTTP 200 OK | M1 | Survey (ORIGINAL_REQUEST, explorer_vercel_env) | DONE |
| F7 | Preview Login Route HTTP 200 Check | Verified HTTP GET `/login` returns HTTP 200 OK | M1 | Survey (ORIGINAL_REQUEST, explorer_vercel_env) | DONE |
| F8 | Supabase SSR Cookie & Auth Invariant | Verified Supabase SSR middleware and fail-fast environment validation | M1 | Survey (spec_miner_docs, explorer_web_codebase) | DONE |
| F9 | Anti-AI Design System Compliance | Verified clean identifier-first login form without demo account box or hardcoded passwords | M1 | Survey (spec_miner_docs) | DONE |
| F10 | Deployment & Session Continuity Record | Documented in `docs/plan/PLAN_OBSERVABILITY_AND_WEB_DEPLOYMENT.md` and `docs/web/VERCEL_DEPLOYMENT_GUIDE.md` | M1 | Survey (spec_miner_docs) | DONE |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Vercel Preview Deployment & Health Verification | Features F1-F10: Root Directory configuration, environment variable provisioning, local build validation, preview deployment, and post-deployment HTTP 200 health checks | none | DONE |

## Interface Contracts
### Vercel Deployment Settings
- **Project Name**: `uth-connect-web` (`prj_KiOeJtlZcQVKJ88sYzxX1oLkHAJQ`)
- **Root Directory**: `web`
- **Framework Preset**: `Next.js`
- **Node.js Runtime**: `24.x`
- **Environment Variables**:
  - `NEXT_PUBLIC_SUPABASE_URL`: `https://bvxilfcctwmgmrwrncvd.supabase.co`
  - `NEXT_PUBLIC_SUPABASE_ANON_KEY`: `sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo`

### Live Deployment URLs
- **Develop Preview**: `https://uth-connect-4ynxduvvn-nguyenthinhk52005-5982.vercel.app`
- **Main Preview**: `https://uth-connect-4l9sn55uk-nguyenthinhk52005-5982.vercel.app`
- **Branch Alias (develop)**: `https://uth-connect-web-git-develop-nguyenthinhk52005-5982.vercel.app`
- **Branch Alias (main)**: `https://uth-connect-web-git-main-nguyenthinhk52005-5982.vercel.app`

### Health Check Verification Contract
- `GET /` -> HTTP 200 OK. Content includes HTML with Refine and Next.js hydration scripts.
- `GET /login` -> HTTP 200 OK. Content includes Identifier-first authentication form.

## Code Layout
- Web Application: `web/`
  - App Router: `web/src/app/`
  - Components: `web/src/components/`
  - Providers: `web/src/providers/`
  - Utilities: `web/src/utils/`
  - Configuration: `web/next.config.mjs`, `web/package.json`, `web/tsconfig.json`
  - Environment: `web/.env.local`
- Deployment Documentation:
  - `docs/web/ANTI_AI_DESIGN_SYSTEM.md`
  - `docs/plan/PLAN_OBSERVABILITY_AND_WEB_DEPLOYMENT.md`
  - `docs/web/VERCEL_DEPLOYMENT_GUIDE.md`
  - `web/README.md`
