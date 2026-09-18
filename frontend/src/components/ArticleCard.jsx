import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, Bookmark, Radio } from 'lucide-react'
import { freshnessLabel, timeAgo } from '../utils/time'
import { newsService } from '../services/newsService'
import { useAuth } from '../context/AuthContext'

/** Small provider pill so it's visible which upstream source produced a story. */
function ProviderTag({ provider }) {
  if (!provider) return null
  const label = provider === 'newsapi' ? 'NewsAPI' : provider === 'gnews' ? 'GNews' : provider
  return (
    <span className="rounded-md bg-surface-sunken px-1.5 py-0.5 font-mono text-[10px]
                     text-ink-400 dark:bg-night-sunken dark:text-slate-500">
      {label}
    </span>
  )
}

export default function ArticleCard({ article, featured = false }) {
  const { isAuthenticated } = useAuth()
  const [saved, setSaved] = useState(article.saved)
  const [busy, setBusy] = useState(false)

  const fresh = freshnessLabel(article.publishedAt)

  async function handleSave(event) {
    // The whole card is a link; stop the bookmark click from navigating.
    event.preventDefault()
    event.stopPropagation()
    if (!isAuthenticated || busy) return

    setBusy(true)
    const previous = saved
    setSaved(!previous)                       // optimistic
    try {
      const result = await newsService.toggleSave(article.id)
      setSaved(result.saved)
    } catch {
      setSaved(previous)                      // roll back on failure
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card-3d-wrap animate-rise">
      <Link to={`/article/${article.id}`} className="card-3d group block h-full">
        <span className="card-sheen" />

        <div className={`card-media ${featured ? 'h-64' : 'h-44'}`}>
          {article.imageUrl ? (
            <img
              src={article.imageUrl}
              alt=""
              loading="lazy"
              className="h-full w-full object-cover"
              onError={(e) => { e.currentTarget.style.visibility = 'hidden' }}
            />
          ) : (
            <div className="grid h-full place-items-center bg-brand-gradient/10 text-4xl opacity-40">📰</div>
          )}

          {/* Gradient scrim so overlay chips stay legible on any image. */}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/45 via-transparent to-black/10" />

          <div className="absolute left-3 top-3 flex gap-2">
            {fresh && (
              <span className={fresh.tone === 'live' ? 'badge-live' : 'badge-fresh'}>
                {fresh.tone === 'live' && <Radio className="h-3 w-3 animate-pulseDot" />}
                {fresh.text}
              </span>
            )}
          </div>

          {isAuthenticated && (
            <button
              onClick={handleSave}
              aria-label={saved ? 'Remove from saved' : 'Save article'}
              className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-xl
                         glass transition-transform hover:scale-110 active:scale-95"
            >
              <Bookmark className={`h-4 w-4 ${saved ? 'fill-brand-500 text-brand-500' : 'text-ink-600 dark:text-slate-300'}`} />
            </button>
          )}

          <span className="absolute bottom-3 right-3 rounded-lg bg-black/55 px-2 py-1
                           text-[11px] font-medium text-white backdrop-blur-sm">
            {timeAgo(article.publishedAt)}
          </span>
        </div>

        <div className="flex flex-col p-5">
          <div className="mb-3 flex items-center gap-2">
            {article.categoryName && <span className="badge-category">{article.categoryName}</span>}
            {article.trendRank && <span className="badge-neutral">#{article.trendRank}</span>}
          </div>

          {/* The headline is the dominant element on the card, by design. */}
          <h3 className={`line-clamp-3 font-semibold leading-snug text-ink-900 dark:text-white
                          transition-colors group-hover:text-brand-600 dark:group-hover:text-brand-400
                          ${featured ? 'text-xl' : 'text-[15px]'}`}>
            {article.title}
          </h3>

          {article.description && (
            <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-ink-500 dark:text-slate-400">
              {article.description}
            </p>
          )}

          <div className="mt-4 flex items-center justify-between gap-3 border-t border-edge pt-3 dark:border-night-edge">
            <div className="flex min-w-0 items-center gap-2">
              <span className="truncate text-xs font-medium text-ink-600 dark:text-slate-400">
                {article.source || 'Unknown source'}
              </span>
              <ProviderTag provider={article.provider} />
            </div>

            <span className="flex shrink-0 items-center gap-1 text-xs font-semibold text-brand-600
                             transition-transform group-hover:translate-x-0.5 dark:text-brand-400">
              Read <ArrowUpRight className="h-3.5 w-3.5" />
            </span>
          </div>
        </div>
      </Link>
    </div>
  )
}
