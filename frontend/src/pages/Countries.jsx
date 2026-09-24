import { useEffect, useState } from 'react'
import CountryGrid from '../components/CountryGrid'
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

      <CountryGrid countries={countries} />
    </div>
  )
}
