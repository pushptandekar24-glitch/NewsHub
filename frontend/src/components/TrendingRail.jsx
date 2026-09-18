import { Link } from 'react-router-dom'
import { Flame, TrendingUp } from 'lucide-react'
import { timeAgo } from '../utils/time'

/**
 * Horizontal trending rail.
 *
 * `trendRank` and `sourceCoverage` both come from the backend's TrendingService
 * — the rank is its scoring position and the coverage count is how many
 * distinct publishers are running the story. Neither is generated in the UI.
 */
export default function TrendingRail({ articles }) {
  if (!articles || articles.length === 0) return null

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          <span className="grid h-8 w-8 place-items-center rounded-xl bg-rose-50 dark:bg-rose-500/10">
            <Flame className="h-4 w-4 text-rose-500" />
          </span>
          Trending Now
        </h2>
        <Link to="/trending" className="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400">
          See all
        </Link>
      </div>

      <div className="scroll-x snap-x">
        {articles.map((article, index) => (
          <Link
            key={article.id}
            to={`/article/${article.id}`}
            className="card-3d group w-[19rem] shrink-0 snap-start p-5"
          >
            <span className="card-sheen" />

            <div className="flex items-start gap-3">
              <span className="gradient-text text-2xl font-bold leading-none">
                #{article.trendRank ?? index + 1}
              </span>

              <div className="min-w-0 flex-1">
                <h3 className="line-clamp-3 text-sm font-semibold leading-snug
                               transition-colors group-hover:text-brand-600 dark:group-hover:text-brand-400">
                  {article.title}
                </h3>

                <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-ink-500 dark:text-slate-400">
                  <span className="truncate font-medium">{article.source}</span>
                  <span aria-hidden="true">•</span>
                  <span>{timeAgo(article.publishedAt)}</span>
                </div>

                {article.sourceCoverage > 1 && (
                  <span className="mt-3 inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5
                                   text-[11px] font-semibold text-emerald-600
                                   dark:bg-emerald-500/10 dark:text-emerald-400">
                    <TrendingUp className="h-3 w-3" />
                    {article.sourceCoverage} outlets covering
                  </span>
                )}
              </div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
