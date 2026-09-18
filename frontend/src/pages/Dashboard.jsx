import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import Hero from '../components/Hero'
import TrendingRail from '../components/TrendingRail'
import { EmptyState, ErrorState, SkeletonGrid, SkeletonRail, WarningBanner } from '../components/States'
import { categoryService } from '../services/categoryService'
import { newsService } from '../services/newsService'
import { useAuth } from '../context/AuthContext'

export default function Dashboard() {
  const { isAuthenticated, user } = useAuth()

  const [featured, setFeatured] = useState(null)
  const [latest, setLatest] = useState([])
  const [warnings, setWarnings] = useState([])
  const [trending, setTrending] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)

    // Personalised feed when signed in; fall back to the general feed if the
    // user has no interests yet or the call fails.
    const feedRequest = isAuthenticated
      ? newsService.personalized({ pageSize: 19 }).catch(() => newsService.feed({ pageSize: 19 }))
      : newsService.feed({ pageSize: 19 })

    Promise.all([
      feedRequest,
      newsService.trending(8).catch(() => ({ content: [] })),
      categoryService.list().catch(() => []),
    ])
      .then(([feedData, trendingData, categoryData]) => {
        const [first, ...rest] = feedData.content
        setFeatured(first || null)
        setLatest(rest)
        setWarnings(feedData.warnings || [])
        setTrending(trendingData.content)
        setCategories(categoryData)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [isAuthenticated])

  useEffect(() => { load() }, [load])

  if (error) return <ErrorState message={error} onRetry={load} />

  return (
    <div className="space-y-12">
      <Hero name={isAuthenticated ? user.name.split(' ')[0] : null} />

      <WarningBanner warnings={warnings} />

      {/* ------------------------------------------------------ trending */}
      {loading ? (
        <section>
          <div className="skeleton mb-4 h-6 w-40" />
          <SkeletonRail />
        </section>
      ) : (
        <TrendingRail articles={trending} />
      )}

      {/* ------------------------------------------------------ featured */}
      {!loading && featured && (
        <section>
          <h2 className="section-title mb-4">Breaking</h2>
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <ArticleCard article={featured} featured />
            </div>

            <div className="panel flex flex-col justify-center p-6">
              <h3 className="text-sm font-semibold">Why this is first</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-500 dark:text-slate-400">
                Stories are merged from every configured provider, de-duplicated by
                canonical URL and headline, then sorted by publication time. The most
                recently published story leads the page.
              </p>
              <Link to="/trending" className="btn-ghost mt-5 self-start">
                See trending ranking <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </section>
      )}

      {/* ---------------------------------------------------- categories */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="section-title">Explore categories</h2>
          <Link to="/categories" className="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400">
            View all {categories.length || 25}
          </Link>
        </div>
        <div className="flex flex-wrap gap-2">
          {categories.slice(0, 16).map((category) => (
            <Link key={category.slug} to={`/categories/${category.slug}`} className="chip">
              <span aria-hidden="true">{category.icon}</span> {category.name}
            </Link>
          ))}
        </div>
      </section>

      {/* -------------------------------------------------------- latest */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="section-title">Latest stories</h2>
          <Link to="/explore" className="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400">
            Browse all
          </Link>
        </div>

        {loading && <SkeletonGrid count={6} />}

        {!loading && latest.length === 0 && (
          <EmptyState title="No further articles right now" onRetry={load} />
        )}

        {!loading && latest.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
            {latest.map((article) => (
              <ArticleCard key={article.id} article={article} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
