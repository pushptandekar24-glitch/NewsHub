import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Search } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import CategoryNav from '../components/CategoryNav'
import CountryFilter from '../components/CountryFilter'
import Pagination from '../components/Pagination'
import { EmptyState, ErrorState, SkeletonGrid, WarningBanner } from '../components/States'
import { newsService, DEFAULT_PAGE_SIZE } from '../services/newsService'

const SORTS = [
  { value: 'publishedAt', label: 'Newest' },
  { value: 'relevancy', label: 'Most relevant' },
]

/** Search filters live in the URL too, so a result page is shareable. */
export default function SearchResults() {
  const [searchParams, setSearchParams] = useSearchParams()

  const q = searchParams.get('q') || ''
  const category = searchParams.get('category') || ''
  const country = searchParams.get('country') || ''
  const sort = searchParams.get('sort') || 'publishedAt'
  const from = searchParams.get('from') || ''
  const page = Number(searchParams.get('page') || 0)

  const [feed, setFeed] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    if (!q) { setLoading(false); return }
    setLoading(true)
    setError(null)
    setFeed(null)

    newsService
      .search({ q, category, country, sort, from, page, pageSize: DEFAULT_PAGE_SIZE })
      .then(setFeed)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [q, category, country, sort, from, page])

  useEffect(() => { load() }, [load])

  function update(key, value) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.set('page', '0')
    setSearchParams(next)
  }

  if (!q) {
    return <EmptyState title="Type something to search" hint="Try “OpenAI”, “IPL”, or “climate policy”." />
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="flex items-center gap-3 text-3xl font-bold tracking-tight">
          <Search className="h-6 w-6 text-brand-500" />
          <span>Results for “<span className="gradient-text">{q}</span>”</span>
        </h1>
        <p className="mt-1.5 text-sm text-ink-500 dark:text-slate-400">
          {loading ? 'Searching both providers…' : `${feed?.content.length ?? 0} stories on this page`}
        </p>
      </header>

      <CategoryNav value={category} onChange={(slug) => update('category', slug)} sticky={false} />
      <CountryFilter value={country} onChange={(code) => update('country', code)} limit={8} />

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex gap-2">
          {SORTS.map((option) => (
            <button
              key={option.value}
              className={`chip ${sort === option.value ? 'chip-active' : ''}`}
              onClick={() => update('sort', option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>

        <label className="flex items-center gap-2 text-sm text-ink-500 dark:text-slate-400">
          From
          <input
            type="date"
            className="input h-9 w-auto py-1"
            value={from}
            onChange={(e) => update('from', e.target.value)}
          />
        </label>
      </div>

      {feed && <WarningBanner message={feed.message} />}

      {loading && <SkeletonGrid count={6} />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && feed?.content.length === 0 && (
        <EmptyState
          title={`No stories matched “${q}”`}
          hint="Try a broader keyword, clear the country filter, or widen the date range."
          onRetry={load}
        />
      )}

      {!loading && !error && feed?.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
            {feed.content.map((article) => (
              <ArticleCard key={article.id} article={article} />
            ))}
          </div>
          <Pagination
            page={feed.page}
            totalPages={feed.totalPages}
            onChange={(next) => update('page', String(next))}
          />
        </>
      )}
    </div>
  )
}
