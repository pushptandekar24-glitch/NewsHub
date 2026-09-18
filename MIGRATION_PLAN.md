# NewsHub — Phase 1 Report & Migration Plan

## 1. What was actually in the ZIP

The uploaded project is far smaller than the master prompt assumes. Excluding
`node_modules`, it contains **five files**:

```
Basic-API-main/
├── backend/
│   ├── package.json        Express + cors + axios + nodemon
│   ├── package-lock.json
│   └── server.js           148 lines — the entire backend
└── frontend/
    ├── index.html          344 lines — the entire frontend
    └── package-lock.json   empty ("packages": {})
```

### Backend — `server.js`

A thin Express proxy in front of NewsAPI.org. Four routes:

| Route                  | Upstream                   | Notes |
|------------------------|----------------------------|-------|
| `GET /`                | —                          | health check |
| `GET /api/news`        | `/v2/top-headlines`        | `category`, `country`, `page`, `pageSize` |
| `GET /api/search`      | `/v2/everything`           | `q` required, `sortBy` |
| `GET /api/sources`     | `/v2/sources`              | `category`, `language`, `country` |
| `GET /api/news-by-source` | `/v2/top-headlines`     | `sources` required |

Each handler forwards the upstream JSON verbatim, so the frontend is tightly
coupled to NewsAPI's response shape.

### Frontend — `index.html`

A single static page: Tailwind via CDN `<script>`, vanilla JS, no build step.
Implements a category tab bar (7 categories), a news grid, a skeleton loader,
an error banner, an empty state, a refresh button, and relative timestamps.
Hardcodes `API_BASE = 'http://localhost:3001'`.

**There is no React, no Vite, no `src/`, no components, no router, no database,
no authentication, no `.env`, no `.gitignore`, no README, and no tests.** The
master prompt's instruction to "keep the frontend in React/Vite" is therefore a
*build*, not a *preserve* — there is nothing React to keep.

---

## 2. Security findings (act on these first)

| # | Finding | Severity | Action |
|---|---------|----------|--------|
| 1 | A live NewsAPI key is hardcoded on line 13 of `server.js` and committed to the repo | **High** | **Revoke and regenerate the key at newsapi.org now.** Anything pushed to GitHub must be treated as public forever; deleting the line does not remove it from git history. |
| 2 | `app.use(cors())` allows every origin | Medium | Replaced with an explicit allow-list (`app.cors.allowed-origins`). |
| 3 | No timeouts on outbound axios calls | Medium | A hung provider could exhaust the server. Timeouts added (5s connect / 10s read). |
| 4 | Upstream error bodies forwarded to the client | Low | Could leak provider internals. Now mapped to our own error shape. |
| 5 | No `.gitignore` | Medium | Added; `.env` and `target/` are now ignored. |

---

## 3. News provider reality check

Researched September 2026. This matters because it changes what the project can
become.

| Provider | Free tier | Production use? |
|---|---|---|
| **NewsAPI.org** (currently used) | 100 req/day, articles delayed ~24h | **No** — development/localhost only by ToS; first paid plan is ~$449/mo |
| **GNews.io** | 100 req/day, 10/min | Yes (non-commercial) |
| **NewsData.io** | ~200 credits/day | Yes, commercial allowed |
| **The Guardian** | Genuinely free | Yes, but a single publisher |

**Decision:** keep NewsAPI.org for local development (it is what the key was
issued for and the existing code targets it), but put every provider behind a
`NewsProvider` interface so switching is a one-line config change. `GNewsProvider`
is implemented alongside it so the project is deployable without a rewrite.

---

## 4. What gets reused, migrated, or rebuilt

### Reused (concepts, not code)
- The four route shapes and their query parameters → become `NewsController`
- The category-tab UX and skeleton/error/empty states → become React components
- Relative-time formatting ("2h ago") → ported to a frontend util
- The dark theme instinct → kept, but rebuilt with a real palette

### Migrated JS → Java
| From `server.js` | To |
|---|---|
| `axios.get(top-headlines)` | `NewsApiOrgProvider.fetchTopHeadlines()` |
| `axios.get(everything)` | `NewsApiOrgProvider.search()` |
| `/api/sources` | folded into `ArticleResponse.source` |
| `/api/news-by-source` | `GET /api/news/search?q=<source>` |
| hardcoded key | `NEWS_API_KEY` env var |
| raw JSON passthrough | `NormalizedArticle` → `ArticleResponse` DTOs |

### Rebuilt from scratch
Everything else: persistence, auth, categories-in-DB, article detail pages,
saved articles, notifications, API explorer, API keys, request history,
analytics, and the entire React frontend.

---

## 5. Phase status

| Phase | Status |
|---|---|
| 1 — Inspect existing project | **Done** (this document) |
| 2 — React frontend foundation | Not started |
| 3 — Spring Boot backend | **Done** (skeleton + news/auth/category slice) |
| 4 — Migrate JS API functionality | **Done** |
| 5 — MySQL + JPA/Hibernate | **Done** (10 entities, 8 repositories) |
| 6 — CRUD APIs | Partial (news, categories, users, saved) |
| 7 — Global news integration | **Done** |
| 8 — 25 categories | **Done** (seeded, DB-driven) |
| 9 — Country filtering | **Done** (24 regions + Worldwide) |
| 10 — Article detail pages | Backend done; UI pending |
| 11 — Search/filter/pagination | **Done** |
| 12 — Auth + JWT | **Done** |
| 13 — Interests/personalization | Backend done |
| 14 — Notifications | Entity + repository only |
| 15 — Saved articles | **Done** |
| 16 — API keys | Entity + repository only |
| 17 — Request history | Entity + repository only |
| 18 — Analytics | Queries written; no endpoint yet |
| 19 — Swagger | **Done** |
| 20 — Validation + exceptions | **Done** |
| 21 — Security | Done except rate limiting |
| 22 — Caching | **Done** (Caffeine, 5 min) |
| 23 — Testing | Started (3 test classes) |
| 24 — Deployment prep | Not started |

---

## 6. Honest caveat about verification

This environment has a Java **runtime** but no JDK compiler, no Maven, and no
access to Maven Central, so **the backend has not been compiled or run here**.
The code was written against Spring Boot 3.5.6 APIs and hand-checked, but the
first `mvn clean test` on your machine is the real verification step — treat any
compile errors as expected first-pass cleanup, not as a sign something is
fundamentally wrong.

Run this first:

```bash
cd backend
cp .env.example .env     # fill in DB_PASSWORD, JWT_SECRET, NEWS_API_KEY
mvn clean test
mvn spring-boot:run
```

Then open http://localhost:8080/swagger-ui.html

---

## 7. Why Spring Boot 3.5, not 4.1

Spring Boot 4.1 is current (Aug 2026) and 3.5 reached open-source EOL on
30 June 2026. 3.5.6 was chosen anyway because this is a **learning** project:
essentially every tutorial, StackOverflow answer and blog post you will hit
while debugging targets 3.x, and Spring Security 7 (bundled with Boot 4) changed
enough of the configuration DSL that mismatched examples become a constant
source of confusion. Once the project is working end-to-end, upgrading to 4.x is
a worthwhile exercise in its own right. Say the word and I'll do the upgrade.
