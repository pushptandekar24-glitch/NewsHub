import { useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { categoryService } from '../services/categoryService'

/**
 * Horizontally scrollable category bar with arrow affordances on desktop.
 *
 * It is a controlled component: it renders `value` as active and calls
 * `onChange(slug)`. Owning the URL is the PAGE's job, not this component's, so
 * the same bar works on /explore, /categories/:slug and /search.
 */
export default function CategoryNav({ value, onChange, sticky = true }) {
  const [categories, setCategories] = useState([])
  const [canScrollLeft, setCanScrollLeft] = useState(false)
  const [canScrollRight, setCanScrollRight] = useState(false)
  const railRef = useRef(null)

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
  }, [])

  function updateArrows() {
    const el = railRef.current
    if (!el) return
    setCanScrollLeft(el.scrollLeft > 8)
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 8)
  }

  useEffect(() => {
    updateArrows()
    const el = railRef.current
    if (!el) return
    el.addEventListener('scroll', updateArrows, { passive: true })
    window.addEventListener('resize', updateArrows)
    return () => {
      el.removeEventListener('scroll', updateArrows)
      window.removeEventListener('resize', updateArrows)
    }
  }, [categories])

  function scrollBy(delta) {
    railRef.current?.scrollBy({ left: delta, behavior: 'smooth' })
  }

  return (
    <div className={`relative ${sticky ? 'sticky top-[4.25rem] z-20' : ''}`}>
      <div className={`${sticky ? 'glass rounded-2xl px-2 py-2' : ''}`}>
        {canScrollLeft && (
          <button
            onClick={() => scrollBy(-320)}
            aria-label="Scroll categories left"
            className="absolute -left-3 top-1/2 z-10 hidden -translate-y-1/2 rounded-full border border-edge
                       bg-surface-raised p-2 shadow-lift hover:scale-105 lg:block
                       dark:border-night-edge dark:bg-night-raised"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
        )}

        <div ref={railRef} className="scroll-x">
          <button
            className={`chip ${!value ? 'chip-active' : ''}`}
            onClick={() => onChange('')}
          >
            All topics
          </button>

          {categories.map((category) => (
            <button
              key={category.slug}
              className={`chip ${value === category.slug ? 'chip-active' : ''}`}
              onClick={() => onChange(category.slug)}
            >
              <span aria-hidden="true">{category.icon}</span>
              {category.name}
            </button>
          ))}
        </div>

        {canScrollRight && (
          <button
            onClick={() => scrollBy(320)}
            aria-label="Scroll categories right"
            className="absolute -right-3 top-1/2 z-10 hidden -translate-y-1/2 rounded-full border border-edge
                       bg-surface-raised p-2 shadow-lift hover:scale-105 lg:block
                       dark:border-night-edge dark:bg-night-raised"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  )
}
