import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  Bookmark, Compass, Globe, Grid3x3, Home, LogOut, Menu, Moon, Search,
  Settings, Sun, Terminal, TrendingUp, X,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

// Primary reader navigation. API Explorer is intentionally NOT in this list —
// it is a developer tool, not something a normal reader needs, and is shown
// in its own de-emphasised section below (requirement: keep it available but
// visually separate so it doesn't dominate the reading experience).
const NAV = [
  { to: '/dashboard', label: 'Home', icon: Home },
  { to: '/explore', label: 'Explore', icon: Compass },
  { to: '/categories', label: 'Categories', icon: Grid3x3 },
  { to: '/countries', label: 'Countries', icon: Globe },
  { to: '/trending', label: 'Trending', icon: TrendingUp },
  { to: '/saved', label: 'Saved', icon: Bookmark, auth: true },
]

function navClass({ isActive }) {
  return `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
    isActive
      ? 'bg-brand-gradient text-white shadow-glow'
      : 'text-ink-600 hover:bg-surface-sunken hover:text-ink-900 dark:text-slate-400 dark:hover:bg-night-sunken dark:hover:text-white'
  }`
}

export default function Layout() {
  const { user, isAuthenticated, logout } = useAuth()
  const { theme, toggle } = useTheme()
  const [query, setQuery] = useState('')
  const [mobileOpen, setMobileOpen] = useState(false)
  const navigate = useNavigate()

  function submitSearch(event) {
    event.preventDefault()
    if (!query.trim()) return
    navigate(`/search?q=${encodeURIComponent(query.trim())}&page=0`)
    setMobileOpen(false)
  }

  const links = NAV.filter((item) => !item.auth || isAuthenticated)

  return (
    <div className="min-h-screen">
      {/* ---------------------------------------------------------- topbar */}
      <header className="sticky top-0 z-40 glass border-b">
        <div className="flex h-[4.25rem] items-center gap-4 px-4 lg:px-8">
          <button
            className="btn-quiet !px-2 lg:hidden"
            onClick={() => setMobileOpen((open) => !open)}
            aria-label="Toggle navigation"
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>

          <Link to="/dashboard" className="flex shrink-0 items-center gap-2.5">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-brand-gradient shadow-glow">
              <Globe className="h-5 w-5 text-white" />
            </div>
            <div className="hidden sm:block">
              <div className="text-lg font-bold leading-none tracking-tight">NewsHub</div>
              <div className="mt-0.5 text-[10px] font-medium uppercase tracking-[.16em] text-ink-400">
                Global Intelligence
              </div>
            </div>
          </Link>

          <form onSubmit={submitSearch} className="mx-auto hidden w-full max-w-lg md:block">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
              <input
                className="input pl-10"
                placeholder="Search news, topics, countries…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </form>

          <div className="ml-auto flex items-center gap-2">
            <button onClick={toggle} className="btn-quiet !px-2" aria-label="Toggle theme">
              {theme === 'dark' ? <Sun className="h-4.5 w-4.5" /> : <Moon className="h-4 w-4" />}
            </button>

            {isAuthenticated ? (
              <>
                <Link
                  to="/settings"
                  className="hidden items-center gap-2 rounded-xl px-2 py-1.5 hover:bg-surface-sunken
                             dark:hover:bg-night-sunken sm:flex"
                >
                  <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-gradient
                                   text-xs font-bold text-white">
                    {user.name.charAt(0).toUpperCase()}
                  </span>
                  <span className="text-sm font-medium">{user.name.split(' ')[0]}</span>
                </Link>
                <button onClick={logout} className="btn-ghost !px-3" title="Log out">
                  <LogOut className="h-4 w-4" />
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-ghost">Log in</Link>
                <Link to="/register" className="btn-primary">Sign up</Link>
              </>
            )}
          </div>
        </div>

        {/* mobile search */}
        <form onSubmit={submitSearch} className="px-4 pb-3 md:hidden">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
            <input
              className="input pl-10"
              placeholder="Search news…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
        </form>
      </header>

      <div className="flex">
        {/* -------------------------------------------------------- sidebar */}
        <aside
          className={`fixed inset-y-[4.25rem] left-0 z-30 w-64 shrink-0 overflow-y-auto border-r border-edge
                      bg-surface-raised p-4 transition-transform duration-300
                      dark:border-night-edge dark:bg-night-raised
                      lg:sticky lg:top-[4.25rem] lg:h-[calc(100vh-4.25rem)] lg:translate-x-0
                      ${mobileOpen ? 'translate-x-0' : '-translate-x-full'}`}
        >
          <nav className="space-y-1">
            {links.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} onClick={() => setMobileOpen(false)} className={navClass}>
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>

          {isAuthenticated && (
            <>
              <div className="my-4 border-t border-edge dark:border-night-edge" />
              <NavLink to="/settings" onClick={() => setMobileOpen(false)} className={navClass}>
                <Settings className="h-4 w-4" /> My Interests
              </NavLink>
            </>
          )}

          {/* Developer tools — visually separated from the reading nav above:
              smaller label, muted icon color, its own divider. Still one click
              away, never removed, just not competing with Home/Explore/etc. */}
          <div className="my-4 border-t border-edge dark:border-night-edge" />
          <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-[.14em] text-ink-400">
            Developer
          </p>
          <NavLink
            to="/api-explorer"
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2 text-sm transition-colors ${
                isActive
                  ? 'bg-surface-sunken text-ink-900 dark:bg-night-sunken dark:text-white'
                  : 'text-ink-400 hover:bg-surface-sunken hover:text-ink-700 dark:hover:bg-night-sunken dark:hover:text-slate-300'
              }`
            }
          >
            <Terminal className="h-3.5 w-3.5" /> API Explorer
          </NavLink>

          <div className="mt-6 rounded-2xl border border-edge bg-surface-muted p-4
                          dark:border-night-edge dark:bg-night-sunken">
            <p className="text-xs font-semibold text-ink-900 dark:text-white">Aggregated coverage</p>
            <p className="mt-1.5 text-xs leading-relaxed text-ink-500 dark:text-slate-400">
              Stories are merged from multiple providers, de-duplicated and sorted newest first.
            </p>
          </div>
        </aside>

        {mobileOpen && (
          <div
            className="fixed inset-0 z-20 bg-ink-900/40 backdrop-blur-sm lg:hidden"
            onClick={() => setMobileOpen(false)}
          />
        )}

        <main className="min-w-0 flex-1 px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
