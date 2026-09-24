import { Link } from 'react-router-dom'

/**
 * Card grid of categories, shared by the full Categories page (all 25) and the
 * Explore discovery hub (a compact subset + "view all" link). Extracted so the
 * card markup exists in exactly one place.
 */
export default function CategoryGrid({ categories, limit }) {
  const visible = limit ? categories.slice(0, limit) : categories

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {visible.map((category) => (
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
  )
}
