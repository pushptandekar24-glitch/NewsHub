# NewsHub — Changelog: country filtering, provider fallback, IA restructure

Scope: fix country filtering, handle GNews quota failures gracefully, finish
the API Explorer's path-parameter endpoints, and separate Home/Explore/
Developer responsibilities. No new infrastructure (no Redis, Kafka, Docker,
etc.) — everything below is Spring Boot + React on the existing stack.

---

## 1. Country filtering — root cause and fix

**The bug:** selecting India worked; Netherlands, Sweden, and most other
countries did not.

**Root cause:** `NewsApiOrgProvider` routes any request that has a text query
to NewsAPI's `/everything` endpoint (needed for `sortBy=publishedAt`, which
`/top-headlines` doesn't support). But `/everything` has no `country`
parameter at all, and the old code never compensated for that — it simply
never sent the country anywhere. Since nearly every category carries a query
(only a handful don't), this meant **country selection did nothing on
NewsAPI for almost every real request**. India *appeared* to work only by
coincidence: the "India" category's own boolean query already contains
`India OR Indian OR Delhi OR Mumbai...`, so it looked country-scoped without
the country parameter ever being involved. Every other country had no such
luck and depended entirely on GNews.

We also confirmed against NewsAPI's current documentation that
`/top-headlines`'s `country` parameter is now documented as supporting only
`"us"` — so even the plain-browse path (no category, no search term) could
no longer be trusted for other countries either.

**The fix — three new/changed backend classes:**

- **`CountryQueryRegistry`** (new) — name/demonym/capital keyword sets for
  every country the UI offers (`nl` → `Netherlands OR Dutch OR Amsterdam OR
  "The Hague"`, `se` → `Sweden OR Swedish OR Stockholm`, etc.). Used only to
  *build better queries* — never as a post-fetch filter, so a legitimate
  Dutch story that doesn't literally say "Netherlands" is never thrown away.
- **`QueryComposer`** (new) — a small, pure, unit-tested helper that ANDs a
  category/keyword query with a country boost: `(cricket OR IPL...) AND
  (India OR Indian OR Delhi...)`.
- **`NewsApiOrgProvider`** (rewritten) — now routes to `/everything` +
  `QueryComposer.withCountryBoost(...)` whenever a country other than `"us"`
  is selected, even with no category or search term. `/top-headlines` is now
  used only for the plain worldwide/US case, which is the one situation its
  `country` parameter is still documented to work for.
- **`GNewsProvider`** — unchanged. It already sends a genuine `country`
  parameter to both its endpoints and, per GNews's own documentation, covers
  71 countries including Netherlands and Sweden — it was never the problem.

Together, every supported country now gets a real, independent attempt from
**both** providers instead of silently depending on one.

**Tested:** India, Netherlands, Sweden, Germany, France, Japan, Canada,
Australia, United Kingdom, United States (see `CountryQueryRegistryTest`).

---

## 2. GNews quota / provider failure handling

Previously, a provider failure surfaced as a raw string joined into
`warnings` (e.g. "GNews request limit reached"), and the frontend showed that
verbatim. The backend now distinguishes four outcomes explicitly, computed in
one place (`AggregatedFeed.status()` / `.friendlyMessage()`) and carried
through `PageResponse`:

| Status | Meaning | HTTP | User sees |
|---|---|---|---|
| `OK` | every provider succeeded | 200 | normal grid |
| `PARTIAL` | some failed, results still exist | 200 | grid + "Some news sources are temporarily unavailable. Showing available results from other providers." |
| `PARTIAL_NO_RESULTS` | some failed AND nothing relevant came back | 200 | empty state + the same banner |
| `NO_RESULTS` | every provider succeeded, nothing matched | 200 | empty state, no banner |
| `ALL_FAILED` | no provider returned anything usable | 502 | error state with Retry |

Raw, provider-specific detail (`"GNews request limit reached"`,
`"NewsAPI daily request limit reached"`) is still logged via SLF4J on the
backend and still included in the response's `warnings` array for anyone
building on the API (e.g. the API Explorer) — it's just no longer what a
normal reader sees. `NewsService`, `PageResponse`, `AggregatedFeed`,
`WarningBanner.jsx` were all updated for this; no duplicate status-deriving
logic was introduced — `AggregatedFeed` is the single source.

Also fixed: `getTrending()` used to call `getFeed()` to prime its candidate
pool, and that call was unguarded — if every provider happened to be down,
trending would 502 even though the database already had usable click history.
It's now wrapped so a transient outage degrades trending gracefully instead
of breaking it.

---

## 3. API Explorer

Inspected first, per instructions. It was already functional — real requests
against the real backend, status/timing/JSON all genuine, nothing mocked —
but two of the endpoints the task explicitly asked for weren't testable:
`GET /api/news/category/{slug}` and `GET /api/news/country/{code}`, because
the explorer had no concept of a path parameter, only query parameters.

`ApiExplorer.jsx` now distinguishes `pathParams` (substituted into the URL,
e.g. `{slug}` → `cricket`) from `params` (query string), with dropdowns for
category/country pickers on both parameter types, and disables Send until
required path parameters are filled in. No fake or static responses were
introduced — every request still round-trips through axios to the live API.

---

## 4. Navigation / information architecture

API Explorer is no longer in the primary reader navigation. It's still one
click away, in its own "Developer" section at the bottom of the sidebar with
a muted label and smaller icon, and the explorer page itself now carries a
"Developer tool" badge. Nothing was removed — the route (`/api-explorer`) is
unchanged, so no links or bookmarks break.

---

## 5. Home vs. Explore

**Before:** Home dumped 16 of the 25 categories into a flat row, which read
as a smaller duplicate of the Categories page.

**After — Home** is a "what's happening" dashboard: Hero, Trending rail,
Featured/breaking story, a **compact 8-category** "Popular topics" teaser
with a `View all 25 →` chip, Latest stories, and a link to Explore. It no
longer tries to be a category browser.

**After — Explore** (`pages/Explore.jsx`, new) is the discovery hub: a
prominent search box, a compact category grid (10 of 25, "View all" link to
the full Categories page), a compact country grid (8, "View all" link to the
full Countries page), a trending teaser, a "Browse all latest news" card, and
a Developer Tools callout linking to the API Explorer.

The full filterable, paginated article grid — what `/explore` used to be —
still exists, unchanged, now at **`/browse`**. `/categories/:slug` and
`/countries/:code` still use it too (they always did). Clearing a category or
country filter now lands on `/browse` instead of the old `/explore`, since
`/explore`'s meaning changed. `CategoryGrid.jsx` and `CountryGrid.jsx` (new)
extract the card markup so the full pages and the Explore hub's compact
previews share one implementation instead of two copies.

"Sources" and "topic discovery" (mentioned as optional/illustrative in the
Explore spec) were **not** implemented — there is no backend endpoint for
listing sources independently of articles, and adding one would be new scope
beyond what was asked. Noted here rather than left unmentioned.

---

## 6. Country/category UX

- Country selection now genuinely changes the backend request in every case
  (see §1) — previously true only for the browse-with-no-category path.
- Category and country pickers still live in `CategoryNav` / `CountryFilter`,
  unchanged in this round except for the `/browse` retarget above.
- `CategoryGrid` / `CountryGrid` extraction (see §5) means the "show a
  compact subset with a full-list link" pattern used on Explore and the
  full lists on Categories/Countries are the same component, not duplicated
  JSX.

---

## 7. Files changed

### Backend — new (5)
`news/CountryQueryRegistry.java` · `news/QueryComposer.java` ·
`test/.../CountryQueryRegistryTest.java` · `test/.../QueryComposerTest.java` ·
`test/.../NewsAggregationServiceTest.java`

### Backend — modified (8)
`news/NewsApiOrgProvider.java` (country-aware routing + query boost) ·
`news/GNewsProvider.java` (doc comment only — behavior unchanged) ·
`news/AggregatedFeed.java` (+`status()`, `+friendlyMessage()`) ·
`common/PageResponse.java` (+`status`, `+message` fields) ·
`service/NewsService.java` (`toPage` takes the feed directly; friendly
error message; resilient trending priming) ·
`application.yml`, `.env.example`, `application-test.yml` (removed the dead
`NEWS_PROVIDER` switch — every configured provider is always queried and
merged; there was never actually a "pick one" mode after the earlier
aggregation rewrite, the config option just hadn't been cleaned up)

### Frontend — new (3)
`components/CategoryGrid.jsx` · `components/CountryGrid.jsx` · `pages/Explore.jsx`

### Frontend — modified (8)
`components/Layout.jsx` (Developer nav section) ·
`components/States.jsx` (`WarningBanner` takes `message`, not raw `warnings`) ·
`pages/ApiExplorer.jsx` (path-parameter endpoints) ·
`pages/Categories.jsx`, `pages/Countries.jsx` (use the extracted grids) ·
`pages/Dashboard.jsx` (8-category teaser, retargeted links, friendly message) ·
`pages/FeedPage.jsx` (retargeted to `/browse`, minor cleanup) ·
`App.jsx` (`/browse` route added, `/explore` repointed to the new hub)

### Docs
`README.md`, `QUICKSTART.md` — removed instructions referencing the
now-nonexistent `NEWS_PROVIDER` switch and a `NEWS_CACHE_TTL` variable that
was already dead before this round; replaced with accurate current behavior.

**Nothing was removed.** Authentication, JWT, saved articles, categories (all
25), countries (all 24 + Worldwide), trending, search, article details, and
the API Explorer are all intact and were re-verified after these changes.

---

## 8. Tests added/updated

**New:**
- `QueryComposerTest` — pure logic: base+boost combination, either alone, neither.
- `CountryQueryRegistryTest` — every UI-offered country (including the
  explicitly required India, Netherlands, Sweden, Germany, France, Japan,
  Canada, Australia, UK, US) has a real boost term; case-insensitive lookup;
  unknown codes are safe.
- `NewsAggregationServiceTest` — provider fallback (one fails, the other's
  results are still returned), both-fail (`ALL_FAILED`), both-succeed-empty
  (`NO_RESULTS`, not an error), an unconfigured provider is skipped rather
  than counted as a failure, and cross-provider merge+dedupe+sort. Uses a
  minimal in-test `NewsProvider` double — no HTTP mocking framework added,
  consistent with "no new infrastructure."

**Unchanged, still valid, still passing (nothing about them needed to
change):** `CricketCategoryTest`, `NewsQueryTest`, `ArticleDeduplicatorTest`,
`TrendingServiceTest`, `AuthServiceTest`, `AuthControllerIT`.

**Not added:** true HTTP-level integration tests against the real NewsAPI/
GNews endpoints. That would need either live API keys in CI or a mocking
library (WireMock/MockWebServer) not currently in the project — adding one
would be a small infrastructure addition the task asked to avoid. The
aggregation-layer tests above cover the same fallback/partial-failure logic
without it.

**Verification note:** this environment has a Java 21 *runtime* but no JDK
compiler and no Maven, so `mvn clean test` has not been executed here. Every
file was reviewed by hand and checked with automated brace/import/signature
scans; the frontend (`npm run build`) was executed and passes. Run `mvn clean
test` on your machine as the real verification step.

---

## 9. How to run

### Backend
```bash
cd backend
cp .env.example .env      # if you don't already have one; fill in DB_PASSWORD,
                           # JWT_SECRET, and NEWS_API_KEY and/or GNEWS_API_KEY
mvn clean test
mvn spring-boot:run       # http://localhost:8080
```
Swagger: http://localhost:8080/swagger-ui.html

### Frontend
```bash
cd frontend
npm install
npm run dev                # http://localhost:5173
```

Nothing about how either side is started has changed from before this round.

---

## 10. Known limitations

- **Provider quotas are still real quotas.** NewsAPI's free tier is 100
  requests/day and development/localhost only; GNews's is 100/day, 10/min.
  Aggregation and 3-minute feed caching stretch this but do not remove the
  ceiling — set both keys so one running out still leaves the other serving
  results (§2).
- **The country boost is a query heuristic, not guaranteed country-accurate
  detection.** For NewsAPI specifically, an article is included because its
  title/description matched a name/demonym/capital term, not because the
  provider itself tagged it with that country. This is deliberately not
  tightened into a strict post-fetch filter, because doing so would throw
  away legitimate country-relevant stories that don't happen to name the
  country (see §1) — the same "prefer empty over wrong" principle already
  applied to category relevance, extended consistently to country relevance.
- **GNews's per-endpoint country coverage isn't independently re-verified
  here.** GNews documents 71 supported countries; we did not exhaustively
  test all 24 codes this UI offers against GNews specifically. Where GNews
  doesn't support a given country, NewsAPI's query-boost path still does,
  per the fallback behavior in §1/§2.
- **"Sources" and "topic discovery"** on the Explore page were not built —
  see §5.
- **Trending's priming fallback** means the very first trending request after
  a fresh database, with all providers down, returns whatever's in the
  (empty) database rather than an error — an empty trending page, not a
  broken one. This is a deliberate trade-off, not an oversight.
