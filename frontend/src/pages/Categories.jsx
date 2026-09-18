import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
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

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {categories.map((category) => (
          <div key={category.slug} className="card-3d-wrap">
            <Link to={`/categories/${category.slug}`} className="card-3d group block p-6 text-center">
              <span className="card-sheen" />
              <div className="text-3xl transition-transform duration-300 group-hover:scale-110">
                {category.icon}
              </div>
              <div className="mt-3 text-sm font-semibold leading-tight">{category.name}</div>
            </Link>
          </div>
        ))}
      </div>
    </div>
  )
}
