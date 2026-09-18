import { useCallback, useEffect, useState } from 'react'
import { Flame } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import { EmptyState, ErrorState, SkeletonGrid } from '../components/States'
import { newsService } from '../services/newsService'

export default function Trending() {
  const [articles, setArticles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    newsService
      .trending(24)
      .then((data) => setArticles(data.content))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="space-y-6">
      <header className="flex items-start gap-4">
        <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-rose-50 dark:bg-rose-500/10">
          <Flame className="h-6 w-6 text-rose-500" />
        </span>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Trending Now</h1>
          <p className="mt-1.5 max-w-2xl text-sm text-ink-500 dark:text-slate-400">
            Ranked by a score combining how recently a story was published, how many
            distinct publishers are covering it, and how often it has been opened here.
            Recency dominates, so a story from twenty minutes ago outranks one from yesterday.
          </p>
        </div>
      </header>

      {loading && <SkeletonGrid count={6} />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && articles.length === 0 && (
        <EmptyState
          title="Nothing trending yet"
          hint="Trending is built from stories collected in the last 48 hours. Browse a few categories and it will fill in."
          onRetry={load}
          showBrowse
        />
      )}

      {!loading && !error && articles.length > 0 && (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          {articles.map((article) => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </div>
      )}
    </div>
  )
}
