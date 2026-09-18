import { useEffect, useState } from 'react'
import { categoryService } from '../services/categoryService'

/**
 * Country selector. Emits a country code (or '' for worldwide); the page turns
 * that into a URL param and a backend request. Nothing here filters client-side
 * — the code is sent to Spring Boot, which passes it to the providers.
 */
export default function CountryFilter({ value, onChange, limit = 12 }) {
  const [countries, setCountries] = useState([])

  useEffect(() => {
    categoryService.countries().then(setCountries).catch(() => {})
  }, [])

  const visible = countries.slice(0, limit)
  const rest = countries.slice(limit)

  return (
    <div className="flex flex-wrap items-center gap-2">
      {visible.map((country) => {
        const isWorld = country.code === 'world'
        const active = isWorld ? !value : value === country.code
        return (
          <button
            key={country.code}
            className={`chip ${active ? 'chip-active' : ''}`}
            onClick={() => onChange(isWorld ? '' : country.code)}
          >
            <span aria-hidden="true">{country.flag}</span>
            {country.name}
          </button>
        )
      })}

      {rest.length > 0 && (
        <select
          className="input h-9 w-auto py-1 text-sm"
          value={rest.some((c) => c.code === value) ? value : ''}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">More regions…</option>
          {rest.map((country) => (
            <option key={country.code} value={country.code}>
              {country.flag} {country.name}
            </option>
          ))}
        </select>
      )}
    </div>
  )
}
