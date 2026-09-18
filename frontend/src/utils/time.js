/**
 * Relative time formatting.
 *
 * The old version collapsed everything past 24 hours into "1d ago", which made
 * a 25-hour-old story and a 6-day-old story look identical. This version keeps
 * minute precision for the first hour, hour precision for the first day, then
 * switches to "Yesterday" and finally an absolute date.
 */
export function timeAgo(value) {
  if (!value) return ''
  const published = new Date(value)
  if (Number.isNaN(published.getTime())) return ''

  const seconds = Math.floor((Date.now() - published.getTime()) / 1000)

  if (seconds < 0) return 'Just now'          // clock skew between client and provider
  if (seconds < 60) return 'Just now'
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`

  const days = Math.floor(seconds / 86400)
  if (days === 1) return 'Yesterday'
  if (days < 7) return `${days}d ago`

  return published.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    ...(published.getFullYear() !== new Date().getFullYear() ? { year: 'numeric' } : {}),
  })
}

/**
 * Freshness tier, used to decide whether a card gets a coloured badge.
 *
 *   breaking — under 30 minutes
 *   fresh    — under 3 hours
 *   recent   — under 24 hours
 *   older    — everything else
 */
export function freshness(value) {
  if (!value) return 'older'
  const minutes = (Date.now() - new Date(value).getTime()) / 60000

  if (minutes < 30) return 'breaking'
  if (minutes < 180) return 'fresh'
  if (minutes < 1440) return 'recent'
  return 'older'
}

/** Short label for the freshness badge, or null when none should be shown. */
export function freshnessLabel(value) {
  const tier = freshness(value)
  if (tier === 'breaking') return { text: 'Just in', tone: 'live' }
  if (tier === 'fresh') return { text: 'Fresh', tone: 'fresh' }
  return null
}

export function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString(undefined, {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}
