import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Globe } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(form)
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-md pt-10">
      <div className="mb-8 flex items-center justify-center gap-2.5">
        <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-gradient shadow-glow">
          <Globe className="h-5 w-5 text-white" />
        </div>
        <span className="text-xl font-bold gradient-text">NewsHub</span>
      </div>

      <form onSubmit={submit} className="panel space-y-4 p-6">
        <h1 className="text-xl font-bold tracking-tight">Log in</h1>

        {error && (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
            {error}
          </div>
        )}

        <div>
          <label className="mb-1 block text-sm text-ink-500 dark:text-slate-400">Email</label>
          <input
            type="email" required className="input" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </div>

        <div>
          <label className="mb-1 block text-sm text-ink-500 dark:text-slate-400">Password</label>
          <input
            type="password" required className="input" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
        </div>

        <button type="submit" disabled={busy} className="btn-primary w-full">
          {busy ? 'Logging in…' : 'Log in'}
        </button>

        <p className="text-center text-sm text-ink-500 dark:text-slate-400">
          No account? <Link to="/register" className="text-brand-600 dark:text-brand-400 hover:underline">Sign up</Link>
        </p>
      </form>
    </div>
  )
}
