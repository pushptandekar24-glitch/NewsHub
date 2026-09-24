import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, Compass, Flame, Search, Terminal } from 'lucide-react'
import CategoryGrid from '../components/CategoryGrid'
import CountryGrid from '../components/CountryGrid'
import TrendingRail from '../components/TrendingRail'
import { SkeletonRail } from '../components/States'
import { categoryService } from '../services/categoryService'
import { newsService } from '../services/newsService'

/**
 * The discovery hub.
 *
 * This is deliberately NOT another paginated article grid — that page still
 * exists at /browse (and at /categories/:slug, /countries/:code, /search).
 * Explore's job is narrower and different: help the person figure out WHAT
 * they want to look at next.
 *
 *   Home    = "what is happening right now"
 *   Explore = "help me find what I want"
 *
 * so this page is a set of entry points (search, a compact category grid, a
 * compact country grid, a trending teaser, a pointer to developer tools) — not
 * a feed of its own.
 */
export default function Explore() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [categories, setCategories] = useState([])
  const [countries, setCountries] = useState([])
  const [trending, setTrending] = useState([])
  const [loadingTrending, setLoadingTrending] = useState(true)

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
    categoryService.countries().then(setCountries).catch(() => {})
    newsService.trending(6)
      .then((data) => setTrending(data.content))
      .catch(() => {})
      .finally(() => setLoadingTrending(false))
  }, [])

  function submitSearch(event) {
    event.preventDefault()
    if (!query.trim()) return
    navigate(`/search?q=${encodeURIComponent(query.trim())}&page=0`)
  }

  return (
    <div className="space-y-12">
      <header>
        <span className="inline-flex items-center gap-2 rounded-full border border-brand-200/60
                         bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700
                         dark:border-brand-500/20 dark:bg-brand-500/10 dark:text-brand-400">
          <Compass className="h-3.5 w-3.5" /> Discovery
        </span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">
          Find what you want to read
        </h1>
        <p className="mt-2 max-w-xl text-sm text-ink-500 dark:text-slate-400">
          Search by keyword, browse by topic, or narrow down to a region — all 25
          categories and every supported country are one click away.
        </p>

        <form onSubmit={submitSearch} className="mt-6 max-w-xl">
          <div className="relative">
            <Search className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-400" />
            <input
              className="input py-3.5 pl-12 text-base shadow-lift"
              placeholder="Search news, topics, countries…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoFocus
            />
          </div>
        </form>
      </header>

      {/* ------------------------------------------------------- categories */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="section-title">Browse by topic</h2>
          <Link to="/categories" className="flex items-center gap-1 text-sm font-medium
                                            text-brand-600 hover:underline dark:text-brand-400">
            View all {categories.length || 25} <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <CategoryGrid categories={categories} limit={10} />
      </section>

      {/* -------------------------------------------------------- countries */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="section-title">Browse by country</h2>
          <Link to="/countries" className="flex items-center gap-1 text-sm font-medium
                                           text-brand-600 hover:underline dark:text-brand-400">
            View all regions <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <CountryGrid countries={countries} limit={8} />
      </section>

      {/* --------------------------------------------------------- trending */}
      <section>
        {loadingTrending ? (
          <>
            <div className="skeleton mb-4 h-6 w-40" />
            <SkeletonRail count={3} />
          </>
        ) : (
          <TrendingRail articles={trending} />
        )}
      </section>

      {/* ------------------------------------------------- browse everything */}
      <Link to="/browse" className="card-3d group flex items-center justify-between p-6">
        <span className="card-sheen" />
        <div>
          <p className="font-semibold">Browse all latest news</p>
          <p className="mt-1 text-sm text-ink-500 dark:text-slate-400">
            Every story, filterable by topic and country, newest first.
          </p>
        </div>
        <ArrowRight className="h-5 w-5 shrink-0 text-brand-500 transition-transform group-hover:translate-x-1" />
      </Link>

      {/* ------------------------------------------------------- developer */}
      <div className="panel flex items-center justify-between gap-4 p-6">
        <div className="flex items-center gap-4">
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-surface-sunken dark:bg-night-sunken">
            <Terminal className="h-5 w-5 text-ink-500 dark:text-slate-400" />
          </span>
          <div>
            <p className="font-semibold">Developer tools</p>
            <p className="mt-0.5 text-sm text-ink-500 dark:text-slate-400">
              NewsHub is also a REST API. Test its endpoints directly.
            </p>
          </div>
        </div>
        <Link to="/api-explorer" className="btn-ghost shrink-0">
          API Explorer <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  )
}
