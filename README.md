# NewsHub

Global news platform + API explorer. A full-stack learning project:
**React (Vite) + Spring Boot + MySQL**.

> Migrated from a 5-file Express/vanilla-JS prototype. See
> [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) for the inspection report, the
> security findings from the original code, and phase-by-phase status.

---

## Current state

| Layer | Status |
|---|---|
| Spring Boot backend | Foundation complete — news, auth, categories, saved articles |
| MySQL schema | 10 entities, 8 repositories |
| Swagger | `/swagger-ui.html` |
| React frontend | Dashboard, explore, categories, countries, trending, search, article detail, saved, auth, interests, API explorer |
| Frontend build | Verified — `npm run build` passes |

---

## Tech stack

**Backend** — Java 21, Spring Boot 3.5.6, Spring Web, Spring Data JPA,
Hibernate, MySQL 8, Spring Security 6 + JWT (jjwt), Bean Validation, Caffeine
cache, springdoc-openapi, JUnit 5 + Mockito + MockMvc + H2.

**Frontend** (next phase) — React 18, Vite, Tailwind CSS, React Router, Axios,
Recharts.

---

## Architecture

```
React
  ↓  HTTP + JWT
Controller        request/response, status codes, validation
  ↓
Service           business rules, transactions
  ↓
Repository        Spring Data JPA
  ↓
MySQL
```

For external data:

```
Controller → NewsService → NewsFetcher (cache) → NewsProvider → NewsAPI / GNews
                    ↓
            NewsArticleRepository   (local metadata, enables /article/:id)
```

`NewsProvider` is an interface with two implementations. Changing
`NEWS_PROVIDER` in `.env` swaps the whole upstream source without touching any
other class.

---

## Folder structure

```
backend/
└── src/main/java/com/apihub/
    ├── common/       ApiResult, PageResponse envelopes
    ├── config/       cache, OpenAPI, RestClient timeouts, seeders
    ├── controller/   Auth, User, Category, Country, News
    ├── dto/          request/response records
    ├── entity/       User, Category, NewsArticle, SavedArticle,
    │                 Notification, ApiDefinition, ApiEndpoint,
    │                 ApiKey, RequestHistory, Role
    ├── exception/    GlobalExceptionHandler + typed exceptions
    ├── news/         NewsProvider interface + adapters + Countries
    ├── repository/   Spring Data interfaces
    ├── security/     JwtService, filter, SecurityConfig, CurrentUserProvider
    └── service/      Auth, User, Category, News, SavedArticle, summaries
```

---

## Database setup

```sql
CREATE DATABASE newshub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tables are created by Hibernate (`ddl-auto: update`). That is fine while
learning; switch to `validate` plus Flyway migrations before deploying.

---

## Environment variables

Copy `backend/.env.example` to `backend/.env` and fill in:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | ≥32 chars. `openssl rand -base64 48` |
| `NEWS_PROVIDER` | `newsapi` or `gnews` |
| `NEWS_API_KEY` | newsapi.org key (dev/localhost only) |
| `GNEWS_API_KEY` | gnews.io key (deployable) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | optional first admin account |

`.env` is git-ignored. Never commit real keys.

---

## External API setup

```
API PROVIDER:     NewsAPI.org
SIGNUP REQUIRED:  https://newsapi.org/register  (free, no card)
API KEY REQUIRED: yes
ENV VARIABLE:     NEWS_API_KEY
FREE TIER:        100 req/day, ~24h article delay, localhost/development only
EXAMPLE REQUEST:  GET https://newsapi.org/v2/top-headlines?country=in&category=technology
                      (header: X-Api-Key: <key>)

API PROVIDER:     GNews.io
SIGNUP REQUIRED:  https://gnews.io/register
API KEY REQUIRED: yes
ENV VARIABLE:     GNEWS_API_KEY
FREE TIER:        100 req/day, 10 req/min, works from a deployed server
EXAMPLE REQUEST:  GET https://gnews.io/api/v4/top-headlines?topic=technology&lang=en&apikey=<key>
```

---

## Running the backend

```bash
cd backend
cp .env.example .env     # then edit it
mvn clean test
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## Running the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

Vite proxies `/api/*` to `http://localhost:8080`, so the browser stays on one
origin in development and CORS never gets in the way. To point at a deployed
backend instead, copy `.env.example` to `.env` and set `VITE_API_BASE_URL`.

**Start the backend first** — the frontend loads categories and countries from
it on first paint.

---

## API documentation

Every endpoint is annotated and browsable in Swagger. Highlights:

```
POST   /api/auth/register            201 + JWT
POST   /api/auth/login               200 + JWT

GET    /api/users/me                 auth
PUT    /api/users/me/interests       auth

GET    /api/categories               public, 25 seeded categories
GET    /api/countries                public, 24 regions + Worldwide

GET    /api/news?category=&country=&page=&limit=
GET    /api/news/search?q=
GET    /api/news/category/{slug}
GET    /api/news/country/{code}
GET    /api/news/trending
GET    /api/news/personalized        auth
GET    /api/news/{id}                article detail + related
POST   /api/news/{id}/save           auth, toggles bookmark
GET    /api/news/saved/list          auth
```

---

## Authentication

Register or log in, receive a JWT, then send it on every protected call:

```
Authorization: Bearer <token>
```

Passwords are hashed with BCrypt and never returned in any response. Tokens
last 24 hours. Logout is client-side — a stateless JWT cannot be revoked before
expiry without a denylist, which is a deliberate trade-off documented in
`JwtService`.

Roles: `USER` and `ADMIN`. `/api/admin/**` requires `ADMIN`.

---

## Content and copyright

This project stores and serves article **metadata** only — headline, snippet,
image URL, source, timestamp, and a link to the original. It does not copy
publisher article bodies. The "Summary / Key Points / Why It Matters" block on
the detail page is derived from that metadata and is flagged
`summaryGenerated: true` so the UI can label it as generated rather than
attributing it to the publisher. Every article view links back to the source.

---

## Future improvements

- Notifications, API keys, request history, analytics endpoints
- Admin screens (manage APIs, categories, users)
- Per-user rate limiting returning 429
- Flyway migrations
- Upgrade to Spring Boot 4.x
