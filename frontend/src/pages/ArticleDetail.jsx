import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Bookmark, ExternalLink, Sparkles } from 'lucide-react'
import ArticleCard from '../components/ArticleCard'
import { ErrorState, SkeletonGrid } from '../components/States'
import { newsService } from '../services/newsService'
import { useAuth } from '../context/AuthContext'
import { formatDate, timeAgo } from '../utils/time'

export default function ArticleDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const [detail, setDetail] = useState(null)
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    newsService
      .detail(id)
      .then((data) => { setDetail(data); setSaved(data.article.saved) })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { load(); window.scrollTo(0, 0) }, [load])

  async function toggleSave() {
    if (!isAuthenticated) return navigate('/login')
    const previous = saved
    setSaved(!previous)
    try {
      const result = await newsService.toggleSave(id)
      setSaved(result.saved)
    } catch {
      setSaved(previous)
    }
  }

  if (loading) return <SkeletonGrid count={3} />
  if (error) return <ErrorState message={error} onRetry={load} title="Unable to load this article" />

  const { article, summary, keyPoints, whyItMatters, summaryGenerated, related } = detail

  return (
    <article className="mx-auto max-w-4xl space-y-8">
      <button onClick={() => navigate(-1)} className="btn-ghost">
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      <header>
        <div className="flex flex-wrap items-center gap-2">
          {article.categoryName && (
            <Link to={`/categories/${article.categorySlug}`} className="badge-category hover:opacity-80">
              {article.categoryName}
            </Link>
          )}
          <span className="badge-neutral">{timeAgo(article.publishedAt)}</span>
        </div>

        <h1 className="mt-4 text-3xl font-bold leading-tight tracking-tight sm:text-4xl">
          {article.title}
        </h1>

        <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-ink-500 dark:text-slate-400">
          <span>Source: <strong className="text-ink-900 dark:text-white">{article.source || 'Unknown'}</strong></span>
          {article.author && <span>By {article.author}</span>}
          {article.publishedAt && <span>{formatDate(article.publishedAt)}</span>}
          {article.country && <span className="uppercase">{article.country}</span>}
          {article.provider && (
            <span className="font-mono text-xs">via {article.provider === 'gnews' ? 'GNews' : 'NewsAPI'}</span>
          )}
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          <a href={article.articleUrl} target="_blank" rel="noopener noreferrer" className="btn-primary">
            Read original article <ExternalLink className="h-4 w-4" />
          </a>
          <button onClick={toggleSave} className="btn-ghost">
            <Bookmark className={`h-4 w-4 ${saved ? 'fill-brand-500 text-brand-500' : ''}`} />
            {saved ? 'Saved' : 'Save'}
          </button>
        </div>
      </header>

      {article.imageUrl && (
        <img
          src={article.imageUrl}
          alt=""
          loading="lazy"
          className="w-full rounded-2xl border border-edge object-cover shadow-lift dark:border-night-edge"
        />
      )}

      <section className="panel p-7">
        {summaryGenerated && (
          <div className="mb-5 inline-flex items-center gap-2 rounded-full bg-violet-50 px-3 py-1.5
                          text-xs font-semibold text-violet-600 dark:bg-violet-500/10 dark:text-violet-400">
            <Sparkles className="h-3.5 w-3.5" />
            AI-generated summary — derived from article metadata, not written by the publisher
          </div>
        )}

        <h2 className="text-lg font-semibold">Summary</h2>
        <p className="mt-2 leading-relaxed text-ink-600 dark:text-slate-300">{summary}</p>

        {keyPoints?.length > 0 && (
          <>
            <h2 className="mt-7 text-lg font-semibold">Key points</h2>
            <ul className="mt-3 space-y-2.5">
              {keyPoints.map((point, index) => (
                <li key={index} className="flex gap-3 text-sm text-ink-600 dark:text-slate-300">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-gradient" />
                  {point}
                </li>
              ))}
            </ul>
          </>
        )}

        {whyItMatters && (
          <>
            <h2 className="mt-7 text-lg font-semibold">Why this matters</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-600 dark:text-slate-300">{whyItMatters}</p>
          </>
        )}

        <p className="mt-7 border-t border-edge pt-5 text-xs text-ink-400 dark:border-night-edge">
          Full article text remains with the publisher. Open the original for complete coverage.
        </p>
      </section>

      {related?.length > 0 && (
        <section>
          <h2 className="section-title mb-4">Related stories</h2>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
            {related.map((item) => (
              <ArticleCard key={item.id} article={item} />
            ))}
          </div>
        </section>
      )}
    </article>
  )
}
