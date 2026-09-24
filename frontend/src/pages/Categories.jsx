import { useEffect, useState } from 'react'
import CategoryGrid from '../components/CategoryGrid'
import { ErrorState } from '../components/States'
import { categoryService } from '../services/categoryService'

export default function Categories() {
  const [categories, setCategories] = useState([])
  const [error, setError] = useState(null)

  function load() {
    categoryService.list().then(setCategories).catch((err) => setError(err.message))
  }
  useEffect(load, [])

  if (error) return <ErrorState message={error} onRetry={load} title="Unable to load categories" />

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">Categories</h1>
        <p className="mt-1.5 text-sm text-ink-500 dark:text-slate-400">
          {categories.length} topics. Each one has its own targeted provider query, so
          results stay on-topic rather than falling back to a broad category.
        </p>
      </header>

      <CategoryGrid categories={categories} />
    </div>
  )
}
