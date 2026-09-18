import { AlertTriangle, Compass, Inbox, RefreshCw, WifiOff } from 'lucide-react'
import { Link } from 'react-router-dom'

/* Skeletons mirror the real card layout so nothing shifts when data lands. */

export function SkeletonCard() {
  return (
    <div className="panel overflow-hidden">
      <div className="skeleton h-44 rounded-none" />
      <div className="space-y-3 p-5">
        <div className="skeleton h-4 w-24" />
        <div className="skeleton h-5 w-11/12" />
        <div className="skeleton h-5 w-3/5" />
        <div className="skeleton h-3 w-full" />
        <div className="skeleton h-3 w-4/5" />
        <div className="flex items-center justify-between pt-3">
          <div className="skeleton h-3 w-20" />
          <div className="skeleton h-3 w-14" />
        </div>
      </div>
    </div>
  )
}

export function SkeletonGrid({ count = 9 }) {
  return (
    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  )
}

export function SkeletonRail({ count = 4 }) {
  return (
    <div className="scroll-x">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="panel w-72 shrink-0 space-y-3 p-5">
          <div className="skeleton h-3 w-10" />
          <div className="skeleton h-4 w-full" />
          <div className="skeleton h-4 w-2/3" />
          <div className="skeleton h-3 w-24" />
        </div>
      ))}
    </div>
  )
}

/**
 * Error state.
 *
 * `message` comes from the backend, which already translates provider failures
 * into plain language ("NewsAPI temporarily unavailable"). No stack traces
 * reach this component.
 */
export function ErrorState({ message, onRetry, title = 'Unable to load news right now' }) {
  return (
    <div className="panel mx-auto max-w-lg p-10 text-center">
      <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-rose-50 dark:bg-rose-500/10">
        <WifiOff className="h-6 w-6 text-rose-500" />
      </div>
      <p className="text-lg font-semibold text-ink-900 dark:text-white">{title}</p>
      {message && <p className="mt-2 text-sm text-ink-500 dark:text-slate-400">{message}</p>}
      {onRetry && (
        <button onClick={onRetry} className="btn-primary mx-auto mt-6">
          <RefreshCw className="h-4 w-4" /> Retry
        </button>
      )}
    </div>
  )
}

/**
 * Empty state.
 *
 * Deliberately shows nothing rather than padding the page with loosely related
 * articles — an empty Cricket page is more honest than a Cricket page full of
 * baseball.
 */
export function EmptyState({
  title = 'Nothing here yet',
  hint,
  onRetry,
  showBrowse = false,
}) {
  return (
    <div className="panel mx-auto max-w-lg p-10 text-center">
      <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-surface-sunken dark:bg-night-sunken">
        <Inbox className="h-6 w-6 text-ink-400" />
      </div>
      <p className="text-lg font-semibold text-ink-900 dark:text-white">{title}</p>
      {hint && <p className="mt-2 text-sm text-ink-500 dark:text-slate-400">{hint}</p>}

      <div className="mt-6 flex flex-wrap justify-center gap-3">
        {onRetry && (
          <button onClick={onRetry} className="btn-ghost">
            <RefreshCw className="h-4 w-4" /> Refresh
          </button>
        )}
        {showBrowse && (
          <Link to="/categories" className="btn-primary">
            <Compass className="h-4 w-4" /> Try another category
          </Link>
        )}
      </div>
    </div>
  )
}

/** Non-fatal banner: one provider is down, the other answered. */
export function WarningBanner({ warnings }) {
  if (!warnings || warnings.length === 0) return null
  return (
    <div className="mb-5 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3
                    text-sm text-amber-800 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300">
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
      <div>
        <p className="font-medium">Showing partial results</p>
        <p className="mt-0.5 opacity-90">{warnings.join(' · ')}</p>
      </div>
    </div>
  )
}
