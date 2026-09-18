import { useCallback, useEffect, useState } from 'react'
import { Bookmark } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import Pagination from '../components/Pagination'
import { EmptyState, ErrorState, SkeletonGrid } from '../components/States'
import { newsService } from '../services/newsService'

export default function Saved() {
  const [feed, setFeed] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    newsService
      .saved({ page })
      .then(setFeed)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { load() }, [load])

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-4">
        <span className="grid h-12 w-12 place-items-center rounded-2xl bg-brand-50 dark:bg-brand-500/10">
          <Bookmark className="h-6 w-6 text-brand-500" />
        </span>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Saved articles</h1>
          <p className="mt-1 text-sm text-ink-500 dark:text-slate-400">
            Your bookmarks, newest first.
          </p>
        </div>
      </header>

      {loading && <SkeletonGrid count={6} />}
      {!loading && error && <ErrorState message={error} onRetry={load} title="Unable to load saved articles" />}

      {!loading && !error && feed?.content.length === 0 && (
        <EmptyState
          title="No saved articles yet"
          hint="Tap the bookmark icon on any article card to save it here."
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
          <Pagination page={feed.page} totalPages={feed.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
