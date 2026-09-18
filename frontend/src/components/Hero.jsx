import { Link } from 'react-router-dom'
import { Compass, Flame, Newspaper, Sparkles } from 'lucide-react'

/**
 * Dashboard hero.
 *
 * Depth here comes from one soft radial glow and two blurred orbs on a
 * translate-only float animation — no per-frame layout, so it stays cheap.
 */
export default function Hero({ name }) {
  return (
    <section className="relative overflow-hidden rounded-3xl border border-edge bg-surface-raised
                        px-6 py-12 shadow-lift sm:px-10 sm:py-16
                        dark:border-night-edge dark:bg-night-raised">
      <div className="pointer-events-none absolute inset-0 bg-hero-glow" />
      <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full
                      bg-brand-500/15 blur-3xl animate-floaty" />
      <div className="pointer-events-none absolute -bottom-24 left-1/4 h-56 w-56 rounded-full
                      bg-violet-500/10 blur-3xl animate-floaty"
           style={{ animationDelay: '2s' }} />

      <div className="relative">
        <span className="inline-flex items-center gap-2 rounded-full border border-brand-200/60
                         bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700
                         dark:border-brand-500/20 dark:bg-brand-500/10 dark:text-brand-400">
          <Sparkles className="h-3.5 w-3.5" />
          Aggregated from multiple providers, newest first
        </span>

        <h1 className="mt-5 max-w-3xl text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
          {name ? (
            <>Welcome back, <span className="gradient-text">{name}</span></>
          ) : (
            <><span className="gradient-text">Global News</span> Intelligence</>
          )}
        </h1>

        <p className="mt-4 max-w-xl text-base text-ink-500 dark:text-slate-400">
          Stay ahead of what matters. Live coverage across 25 topics and 24 regions,
          de-duplicated and ranked by freshness.
        </p>

        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/trending" className="btn-primary">
            <Flame className="h-4 w-4" /> Trending now
          </Link>
          <Link to="/explore" className="btn-ghost">
            <Newspaper className="h-4 w-4" /> Latest stories
          </Link>
          <Link to="/countries" className="btn-ghost">
            <Compass className="h-4 w-4" /> Explore the world
          </Link>
        </div>
      </div>
    </section>
  )
}
