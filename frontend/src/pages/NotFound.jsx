import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="py-24 text-center">
      <p className="gradient-text font-mono text-6xl font-bold">404</p>
      <h1 className="mt-4 text-xl font-semibold">Page not found</h1>
      <p className="mt-2 text-sm text-ink-500 dark:text-slate-400">
        That route does not exist.
      </p>
      <Link to="/dashboard" className="btn-primary mx-auto mt-8">Back to dashboard</Link>
    </div>
  )
}
