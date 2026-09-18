# Quick start

Two terminals. Backend first.

## 0. Prerequisites

- **JDK 21** — `java -version` should show 21
- **Maven 3.9+** — `mvn -version`
- **MySQL 8** running locally
- **Node 18+** — `node -v`

## 1. Database

```sql
CREATE DATABASE newshub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tables are created automatically by Hibernate on first run.

## 2. Get a news API key

Free, no card: https://newsapi.org/register

(If you already have one from the old project, **generate a new one** — the old
key was committed to the repo and must be treated as public.)

## 3. Backend

```bash
cd backend
cp .env.example .env
```

Edit `.env` and set at minimum:

```
DB_PASSWORD=your_mysql_password
JWT_SECRET=<run: openssl rand -base64 48>
NEWS_API_KEY=<your newsapi.org key>
```

Then:

```bash
mvn clean test          # runs the test suite
mvn spring-boot:run     # starts on :8080
```

Check it: http://localhost:8080/swagger-ui.html

On first startup the log should show `Seeded 25 news categories`.

## 4. Frontend

```bash
cd frontend
npm install
npm run dev             # starts on :5173
```

Open http://localhost:5173

## 5. Try it

1. The dashboard loads a general feed.
2. Click **Sign up**, pick a few interest categories.
3. The dashboard switches to a personalised feed.
4. Click any card → article detail with summary, key points and related stories.
5. Tap the heart on a card → it appears under **Saved**.
6. Open **API Explorer** → send a request, see status code, response time, JSON.

---

## Troubleshooting

**`mvn` not found** — install Maven, or use your IDE's bundled one
(IntelliJ: right-click `pom.xml` → Maven → Reload).

**`Communications link failure`** — MySQL isn't running, or `DB_PASSWORD` is wrong.

**`app.jwt.secret must be at least 32 characters`** — `JWT_SECRET` is missing
or too short in `.env`. This failure is deliberate and happens at startup
rather than at first login.

**News endpoints return 502 / "provider rejected our API key"** — `NEWS_API_KEY`
is missing or invalid. Categories and countries will still load; only the feed
needs the upstream key.

**News endpoints return 429** — NewsAPI's free tier is 100 requests/day. The
backend caches feeds for 5 minutes (`NEWS_CACHE_TTL`) to stretch that, but it
is easy to exhaust while developing. Raise the TTL, or switch to GNews:
set `NEWS_PROVIDER=gnews` and `GNEWS_API_KEY=...`.

**Frontend loads but every request fails** — the backend isn't running. Start it
first; the frontend fetches categories on first paint.

**Port 8080 in use** — set `SERVER_PORT` in `backend/.env`, and update the proxy
target in `frontend/vite.config.js` to match.
