# NewsHub — Update: aggregation, category accuracy, premium UI

Everything below modifies the existing project. No architecture was replaced:
Controller → Service → Repository on the backend, and the existing service-layer
+ component split on the frontend, are both intact. Authentication, JWT, saved
articles and the API Explorer all still work.

---

## 1. Files changed

### Backend — new (12)

| File | Purpose |
|---|---|
| `news/CategoryQuery.java` | Query + relevance rules for one category |
| `news/CategoryQueryRegistry.java` | All 25 category definitions, one place |
| `news/ArticleRelevanceFilter.java` | Drops off-topic articles (the Cricket fix) |
| `news/ArticleDeduplicator.java` | Canonical-URL and title+source de-duplication |
| `news/AggregatedFeed.java` | Merged multi-provider result + per-provider errors |
| `service/NewsAggregationService.java` | Parallel fan-out, merge, filter, dedupe, sort |
| `service/TrendingService.java` | Trend scoring (recency + coverage + clicks) |
| `test/.../CricketCategoryTest.java` | Regression tests for the Cricket bug |
| `test/.../ArticleDeduplicatorTest.java` | De-duplication tests |
| `test/.../TrendingServiceTest.java` | Recency-curve tests |

### Backend — modified (14)

`news/NewsQuery.java` (category strategy, `from`/`to`, over-fetch, page size 20/100) ·
`news/NormalizedArticle.java` (+`provider`, `category`, `country`, `language`) ·
`news/ProviderResult.java` (failures are data, not exceptions) ·
`news/NewsProvider.java` (+`displayName`, `isConfigured`) ·
`news/NewsApiOrgProvider.java` (`/everything` + `sortBy=publishedAt` + `from`) ·
`news/GNewsProvider.java` (`sortby`, `in=title,description`, `from`, query truncation) ·
`service/NewsService.java` (aggregation, trending, pagination, freshness) ·
`service/NewsFetcher.java` (caches `AggregatedFeed`) ·
`controller/NewsController.java` (`pageSize`, `from`, `to`, `sort`, `language`) ·
`config/CacheConfig.java` (per-cache TTLs) ·
`config/CategorySeeder.java` (reads queries from the registry) ·
`entity/NewsArticle.java` (+`provider`) ·
`dto/news/ArticleResponse.java` (+`provider`, `trendRank`, `sourceCoverage`) ·
`common/PageResponse.java` (+`warnings`) ·
`repository/NewsArticleRepository.java` (+`findRecentSince`) ·
`.env.example`, `application.yml`

### Backend — deleted (1)
`service/NewsProviderRegistry.java` — replaced by `NewsAggregationService`, which
uses every configured provider instead of selecting one.

### Frontend — new (5)
`components/CategoryNav.jsx` · `components/CountryFilter.jsx` ·
`components/Hero.jsx` · `components/TrendingRail.jsx` · `context/ThemeContext.jsx`

### Frontend — modified (17)
`tailwind.config.js` · `src/index.css` · `index.html` · `main.jsx` ·
`utils/time.js` · `services/newsService.js` · `components/Layout.jsx` ·
`components/ArticleCard.jsx` · `components/States.jsx` · `components/Pagination.jsx` ·
`pages/Dashboard.jsx` · `pages/FeedPage.jsx` · `pages/Trending.jsx` ·
`pages/SearchResults.jsx` · `pages/Categories.jsx` · `pages/Countries.jsx` ·
`pages/Saved.jsx` · `pages/ArticleDetail.jsx` · `pages/ApiExplorer.jsx` ·
`pages/Login.jsx` · `pages/Register.jsx` · `pages/Settings.jsx` · `pages/NotFound.jsx`

---

## 2. Backend news flow

```
GET /api/news?category=cricket&country=in&page=0&pageSize=20
        │
NewsController          validates country, resolves pageSize/limit, parses from/to
        │
NewsService             slug -> CategoryQuery (registry, DB override, or name)
        │                builds NewsQuery with a freshness floor
NewsFetcher             @Cacheable("newsFeed", key = NewsQuery)   3 min TTL
        │
NewsAggregationService  ├─ CompletableFuture ─> NewsApiOrgProvider
                        └─ CompletableFuture ─> GNewsProvider
        │                (parallel; each returns ProviderResult, never throws)
        │
        ├─ 1. collect failures as data
        ├─ 2. ArticleRelevanceFilter   drop off-topic articles
        ├─ 3. sort by publishedAt DESC
        ├─ 4. ArticleDeduplicator      canonical URL + title|source
        └─ 5. AggregatedFeed
        │
NewsService             upsert metadata into news_articles (SHA-256 of URL as id)
        │                slice to pageSize, attach saved-state per user
        │
PageResponse<ArticleResponse>  { content, page, totalPages, warnings }
```

If one provider fails, its message lands in `warnings` and the other provider's
results are still returned. Only if **every** provider fails does the request
become a 502 with a combined message.

## 3. Frontend news flow

```
Page (FeedPage / Dashboard / SearchResults / Trending)
  │  reads filters from the URL (useSearchParams / useParams)
newsService.feed({ category, country, page, pageSize })
  │  strips empty params
services/api.js   axios instance: attaches JWT, unwraps { data }, normalises errors
  │
Spring Boot :8080
  │
PageResponse -> setFeed(...) -> ArticleCard grid + Pagination
```

Loading shows shimmer skeletons, failure shows `ErrorState` with Retry, and an
empty result shows `EmptyState` — never a blank screen.

## 4. API key configuration

All keys live on the backend, in `backend/.env`, read through
`application.yml` placeholders. The React bundle contains none of them; the
frontend's only configured URL is the backend's own.

```
backend/.env
  NEWS_API_KEY=...     -> app.news.newsapi.api-key -> NewsApiOrgProvider
  GNEWS_API_KEY=...    -> app.news.gnews.api-key   -> GNewsProvider
  AI_API_KEY=...       -> app.ai.api-key           -> reserved, not yet read
```

`.env` is git-ignored; `.env.example` documents every variable with no real values.

## 5. Category filtering

`CategoryQueryRegistry` holds one `CategoryQuery` per slug:

- `providerCategory` — the provider's native category, used only as a coarse hint
- `query` — a boolean search string, which is what actually drives results
- `mustMatchAny` — terms an article must contain to be kept
- `excludeAny` — terms that get an article rejected outright
- `strict` — whether `mustMatchAny` filters or merely ranks

`CategorySeeder` copies `query` and `providerCategory` into the `categories`
table on startup, so an admin can override them in the DB; `NewsService` falls
back to the DB value when a slug is not in the registry.

## 6. Cricket filtering specifically

Cricket is **not** mapped to "sports and hope for the best". Three defences:

1. **Targeted query** — `cricket OR IPL OR ICC OR BCCI OR "Test cricket" OR ODI OR T20 OR "county cricket" OR "Ranji Trophy" OR "Big Bash" OR wicket OR batsman`
2. **Strict inclusion** — an article must mention one of ~30 cricket-specific
   terms (cricket, IPL, BCCI, wicket, batsman, bowler, Ranji, the Ashes, Kohli,
   Bumrah, crease, …) in its title or description.
3. **Explicit exclusion** — any article mentioning NFL, MLB, NBA, NHL, American
   football, baseball, basketball, quarterback, touchdown, home run, Super Bowl,
   World Series, golf, tennis, F1 or NASCAR is dropped, even if it also says
   "cricket" — mixed round-ups are not cricket coverage.

If that leaves nothing, the page shows "Nothing fresh in Cricket right now"
rather than padding with unrelated sport. `CricketCategoryTest` pins all of this.

## 7. Trending calculation

`TrendingService` scores every article collected in the last 48 hours:

```
trendScore = 5.0 * recency + 2.0 * sourceCoverage + 1.5 * engagement

recency         0.5 ^ (hoursOld / 6)      exponential, 6-hour half-life
sourceCoverage  log10(distinct publishers covering the same topic), capped at 1
engagement      log1p(clicks) / log(50),  capped at 1   (our own click counters)
```

Recency is weighted highest deliberately: a 20-minute-old story scores ~0.96 on
that term, a 24-hour-old story ~0.06. Topic grouping uses the first four
significant words of the headline, which groups the same story across outlets
without grouping unrelated ones. Every input is real data — timestamps, source
names, click counts. Nothing is synthesised.

## 8. Pagination

Server-side. `page` is zero-based, `pageSize` defaults to 20 and caps at 100
(`limit` still works as an alias). Providers are asked for page N directly, so
each page is a distinct upstream window and pages never overlap. The backend
over-fetches 3× (`NewsQuery.fetchSize`) because relevance filtering and
de-duplication discard results, then slices down to `pageSize`. Total pages are
capped at 10 so the UI never advertises a page that cannot be filled. The
frontend renders a windowed numeric pager and keeps `page` in the URL.

## 9. UI architecture

- **Light-first.** `#F5F7FB` page, `#FFFFFF` cards, `#111827` text, `#E2E8F0`
  borders, indigo→violet→blue gradient accent. Dark mode is a `class` toggle
  in the header, persisted to localStorage.
- **Depth without noise.** Four shadow tiers (`surface`, `lift`, `float`,
  `glow`) plus 1px borders. Glassmorphism is used only for elements that float
  over content: the header, the sticky category bar, image overlay chips.
- **3D cards.** `.card-3d-wrap` sets `perspective: 1200px`; `.card-3d` does
  `translateY(-6px) rotateX(1.5deg) scale(1.012)` on hover. Only `transform`,
  `opacity` and `box-shadow` animate, so it stays on the GPU. Motion is disabled
  entirely under `prefers-reduced-motion`.
- **Components.** `Hero`, `TrendingRail`, `CategoryNav`, `CountryFilter`,
  `ArticleCard`, `Pagination`, and the `States` family
  (`SkeletonCard`/`SkeletonGrid`/`SkeletonRail`/`ErrorState`/`EmptyState`/`WarningBanner`).
  `FeedPage` is shared by `/explore`, `/categories/:slug` and `/countries/:code`.

---

## 10. Run the backend

```bash
cd backend
cp .env.example .env      # then edit it — see section 12
mvn clean test
mvn spring-boot:run       # http://localhost:8080
```

Swagger: http://localhost:8080/swagger-ui.html

## 11. Run the frontend

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173
```

Vite proxies `/api` to `:8080`, so no CORS configuration is needed in dev.

## 12. Where your API keys go

**One file: `backend/.env`.** Nowhere else. Never in the frontend.

```env
NEWS_API_KEY=your_newsapi_key      # https://newsapi.org/register
GNEWS_API_KEY=your_gnews_key       # https://gnews.io/register
AI_API_KEY=                        # optional, unused for now
```

Set **both** news keys to get aggregation, cross-provider de-duplication and
automatic fallback. One key alone works — the app uses whatever is configured
and tells you in the UI when a provider is unavailable.

After editing `.env`, restart the backend. Caffeine caches survive nothing but
the process, so a restart also clears stale feeds.
