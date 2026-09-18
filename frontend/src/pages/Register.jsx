import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Globe } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { categoryService } from '../services/categoryService'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [categories, setCategories] = useState([])
  const [interests, setInterests] = useState([])
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
  }, [])

  function toggleInterest(slug) {
    setInterests((current) =>
      current.includes(slug) ? current.filter((s) => s !== slug) : [...current, slug]
    )
  }

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await register({ ...form, interestSlugs: interests })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl pt-10">
      <div className="mb-8 flex items-center justify-center gap-2.5">
        <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-gradient shadow-glow">
          <Globe className="h-5 w-5 text-white" />
        </div>
        <span className="text-xl font-bold gradient-text">NewsHub</span>
      </div>

      <form onSubmit={submit} className="panel space-y-5 p-6">
        <h1 className="text-xl font-bold tracking-tight">Create your account</h1>

        {error && (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
            {error}
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm text-ink-500 dark:text-slate-400">Name</label>
            <input required className="input" value={form.name}
                   onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div>
            <label className="mb-1 block text-sm text-ink-500 dark:text-slate-400">Email</label>
            <input type="email" required className="input" value={form.email}
                   onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm text-ink-500 dark:text-slate-400">Password</label>
          <input type="password" required minLength={8} className="input" value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <p className="mt-1 text-xs text-ink-400">At least 8 characters.</p>
        </div>

        <div>
          <label className="mb-2 block text-sm text-ink-500 dark:text-slate-400">
            What are you interested in? <span className="text-slate-600">(optional, editable later)</span>
          </label>
          <div className="flex flex-wrap gap-2">
            {categories.map((c) => (
              <button
                key={c.slug}
                type="button"
                onClick={() => toggleInterest(c.slug)}
                className={`chip ${interests.includes(c.slug) ? 'chip-active' : ''}`}
              >
                <span>{c.icon}</span> {c.name}
              </button>
            ))}
          </div>
        </div>

        <button type="submit" disabled={busy} className="btn-primary w-full">
          {busy ? 'Creating account…' : 'Create account'}
        </button>

        <p className="text-center text-sm text-ink-500 dark:text-slate-400">
          Already registered? <Link to="/login" className="text-brand-600 dark:text-brand-400 hover:underline">Log in</Link>
        </p>
      </form>
    </div>
  )
}
