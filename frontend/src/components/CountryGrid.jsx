import { Link } from 'react-router-dom'

/**
 * Card grid of countries, shared by the full Countries page and the Explore
 * discovery hub's compact preview.
 */
export default function CountryGrid({ countries, limit }) {
  const visible = limit ? countries.slice(0, limit) : countries

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
      {visible.map((country) => (
        <div key={country.code} className="card-3d-wrap">
          <Link
            to={country.code === 'world' ? '/browse' : `/countries/${country.code}`}
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
  )
}
