import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { SlidersHorizontal } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import CategoryNav from '../components/CategoryNav'
import CountryFilter from '../components/CountryFilter'
import Pagination from '../components/Pagination'
import { EmptyState, ErrorState, SkeletonGrid, WarningBanner } from '../components/States'
import { categoryService } from '../services/categoryService'
import { newsService, DEFAULT_PAGE_SIZE } from '../services/newsService'

/**
 * One page powers /explore, /categories/:slug and /countries/:code.
 *
 * FILTER STATE LIVES IN THE URL. Every change goes through setSearchParams or
 * navigate(), which means refresh, back/forward and link sharing all work, and
 * there is exactly one source of truth for what the backend should be asked.
 *
 *   /explore?category=cricket&country=in&page=0
 */
export default function FeedPage({ title, lockCategory = false, lockCountry = false }) {
  const params = useParams()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const [categories, setCategories] = useState([])
  const [countries, setCountries] = useState([])
  const [feed, setFeed] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Locked routes take the value from the path; /explore takes it from the query.
  const category = lockCategory ? params.slug : searchParams.get('category') || ''
  const country = lockCountry ? params.code : searchParams.get('country') || ''
  const page = Number(searchParams.get('page') || 0)
  const pageSize = Number(searchParams.get('pageSize') || DEFAULT_PAGE_SIZE)

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
    categoryService.countries().then(setCountries).catch(() => {})
  }, [])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    setFeed(null)                                  // clear the previous feed immediately

    newsService
      .feed({ category, country, page, pageSize })
      .then(setFeed)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [category, country, page, pageSize])

  useEffect(() => {
    load()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [load])

  /** Category changes navigate to the canonical /categories/:slug URL. */
  function changeCategory(slug) {
    const query = country ? `?country=${country}&page=0` : '?page=0'
    navigate(slug ? `/categories/${slug}${query}` : `/explore${query}`)
  }

  function changeCountry(code) {
    if (lockCountry) {
      navigate(code ? `/countries/${code}?page=0` : '/explore?page=0')
      return
    }
    updateParam('country', code)
  }

  function updateParam(key, value) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.set('page', '0')      // any filter change resets pagination
    setSearchParams(next)
  }

  const activeCategory = useMemo(
    () => categories.find((c) => c.slug === category),
    [categories, category]
  )
  const activeCountry = useMemo(
    () => countries.find((c) => c.code === country),
    [countries, country]
  )

  const heading = activeCategory
    ? `${activeCategory.icon} ${activeCategory.name}`
    : activeCountry
      ? `${activeCountry.flag} ${activeCountry.name}`
      : title || 'Explore'

  const subheading = [
    activeCategory ? activeCategory.name : 'All topics',
    activeCountry ? activeCountry.name : 'Worldwide',
  ].join(' · ')

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">{heading}</h1>
        <p className="mt-1.5 flex items-center gap-2 text-sm text-ink-500 dark:text-slate-400">
          <SlidersHorizontal className="h-3.5 w-3.5" />
          {subheading}
          {feed && !loading && (
            <>
              <span aria-hidden="true">·</span>
              <span>{feed.content.length} stories on this page</span>
            </>
          )}
        </p>
      </header>

      {!lockCategory && <CategoryNav value={category} onChange={changeCategory} />}
      {lockCategory && <CategoryNav value={category} onChange={changeCategory} />}

      <CountryFilter value={country} onChange={changeCountry} />

      {feed && <WarningBanner warnings={feed.warnings} />}

      {loading && <SkeletonGrid count={pageSize > 12 ? 9 : 6} />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && feed?.content.length === 0 && (
        <EmptyState
          title={activeCategory
            ? `Nothing fresh in ${activeCategory.name} right now.`
            : 'No articles found.'}
          hint="We only show stories that actually match this topic, so the page stays empty rather than filling up with unrelated news."
          onRetry={load}
          showBrowse
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
            onChange={(next) => updateParam('page', String(next))}
          />
        </>
      )}
    </div>
  )
}
