import { ChevronLeft, ChevronRight } from 'lucide-react'

/**
 * Numbered server-side pagination.
 *
 * Only ever renders a small window of page buttons — the backend caps results
 * at 10 pages, and rendering every page number for a large set is noise.
 */
export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null

  const windowSize = 5
  let start = Math.max(0, page - Math.floor(windowSize / 2))
  const end = Math.min(totalPages, start + windowSize)
  start = Math.max(0, end - windowSize)

  const pages = Array.from({ length: end - start }, (_, i) => start + i)

  return (
    <nav className="mt-10 flex flex-wrap items-center justify-center gap-2" aria-label="Pagination">
      <button className="btn-ghost" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft className="h-4 w-4" />
        <span className="hidden sm:inline">Previous</span>
      </button>

      {start > 0 && (
        <>
          <button className="btn-quiet h-10 w-10 !px-0" onClick={() => onChange(0)}>1</button>
          <span className="px-1 text-ink-400">…</span>
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
          className={p === page
            ? 'btn h-10 w-10 !px-0 bg-brand-gradient text-white shadow-glow'
            : 'btn-ghost h-10 w-10 !px-0'}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages && (
        <>
          <span className="px-1 text-ink-400">…</span>
          <button className="btn-quiet h-10 w-10 !px-0" onClick={() => onChange(totalPages - 1)}>
            {totalPages}
          </button>
        </>
      )}

      <button className="btn-ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        <span className="hidden sm:inline">Next</span>
        <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  )
}
