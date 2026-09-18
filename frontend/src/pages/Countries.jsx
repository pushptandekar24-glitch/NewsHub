import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../components/States'
import { categoryService } from '../services/categoryService'

export default function Countries() {
  const [countries, setCountries] = useState([])
  const [error, setError] = useState(null)

  function load() {
    categoryService.countries().then(setCountries).catch((err) => setError(err.message))
  }
  useEffect(load, [])

  if (error) return <ErrorState message={error} onRetry={load} title="Unable to load countries" />

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">Countries & regions</h1>
        <p className="mt-1.5 text-sm text-ink-500 dark:text-slate-400">
          Selecting a region changes the query sent to the news providers, not just the UI.
        </p>
      </header>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {countries.map((country) => (
          <div key={country.code} className="card-3d-wrap">
            <Link
              to={country.code === 'world' ? '/explore' : `/countries/${country.code}`}
              className="card-3d group flex items-center gap-3 p-5"
            >
              <span className="card-sheen" />
              <span className="text-2xl transition-transform duration-300 group-hover:scale-110">
                {country.flag}
              </span>
              <span className="text-sm font-semibold leading-tight">{country.name}</span>
            </Link>
          </div>
        ))}
      </div>
    </div>
  )
}
