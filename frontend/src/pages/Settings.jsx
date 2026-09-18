import { useEffect, useState } from 'react'
import { categoryService } from '../services/categoryService'
import { userService } from '../services/userService'
import { useAuth } from '../context/AuthContext'

export default function Settings() {
  const { user, setUser } = useAuth()
  const [categories, setCategories] = useState([])
  const [interests, setInterests] = useState(user?.interests || [])
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
  }, [])

  function toggle(slug) {
    setInterests((current) =>
      current.includes(slug) ? current.filter((s) => s !== slug) : [...current, slug]
    )
  }

  async function save() {
    setBusy(true)
    setStatus(null)
    try {
      const updated = await userService.updateInterests(interests)
      setUser(updated)
      setStatus({ ok: true, message: 'Interests updated' })
    } catch (err) {
      setStatus({ ok: false, message: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-bold tracking-tight">My interests</h1>
      <p className="mt-1 text-sm text-ink-500 dark:text-slate-400">
        These drive your personalised feed on the home page.
      </p>

      <div className="panel mt-6 p-6">
        <div className="mb-4 text-sm text-ink-500 dark:text-slate-400">
          Signed in as <span className="font-medium text-ink-900 dark:text-white">{user?.email}</span> ({user?.role})
        </div>

        <div className="flex flex-wrap gap-2">
          {categories.map((c) => (
            <button
              key={c.slug}
              onClick={() => toggle(c.slug)}
              className={`chip ${interests.includes(c.slug) ? 'chip-active' : ''}`}
            >
              <span>{c.icon}</span> {c.name}
            </button>
          ))}
        </div>

        <div className="mt-6 flex items-center gap-4">
          <button onClick={save} disabled={busy} className="btn-primary">
            {busy ? 'Saving…' : 'Save interests'}
          </button>
          {status && (
            <span className={`text-sm ${status.ok ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
              {status.message}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
