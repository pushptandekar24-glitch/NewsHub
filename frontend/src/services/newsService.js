import api, { unwrap } from './api'

/** Strips null/undefined/empty params so we never send `?country=undefined`. */
const clean = (params) =>
  Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  )

/**
 * All news calls go through here.
 *
 * `pageSize` is the documented backend parameter (the backend also accepts
 * `limit` as an alias). Default is 20 per page — the old default of 12 was the
 * main reason the feed felt sparse.
 */
export const DEFAULT_PAGE_SIZE = 20

export const newsService = {
  async feed({ category, country, language = 'en', page = 0, pageSize = DEFAULT_PAGE_SIZE, from, to } = {}) {
    return unwrap(await api.get('/api/news', {
      params: clean({ category, country, language, page, pageSize, from, to }),
    }))
  },

  async search({ q, category, country, language = 'en', sort = 'publishedAt',
                 page = 0, pageSize = DEFAULT_PAGE_SIZE, from, to }) {
    return unwrap(await api.get('/api/news/search', {
      params: clean({ q, category, country, language, sort, page, pageSize, from, to }),
    }))
  },

  async byCategory({ slug, country, page = 0, pageSize = DEFAULT_PAGE_SIZE }) {
    return unwrap(await api.get(`/api/news/category/${slug}`, {
      params: clean({ country, page, pageSize }),
    }))
  },

  async byCountry({ code, category, page = 0, pageSize = DEFAULT_PAGE_SIZE }) {
    return unwrap(await api.get(`/api/news/country/${code}`, {
      params: clean({ category, page, pageSize }),
    }))
  },

  async trending(limit = 12) {
    return unwrap(await api.get('/api/news/trending', { params: { limit } }))
  },

  async personalized({ country, page = 0, pageSize = DEFAULT_PAGE_SIZE } = {}) {
    return unwrap(await api.get('/api/news/personalized', { params: clean({ country, page, pageSize }) }))
  },

  async detail(id) {
    return unwrap(await api.get(`/api/news/${id}`))
  },

  async toggleSave(id) {
    return unwrap(await api.post(`/api/news/${id}/save`))
  },

  async saved({ page = 0, pageSize = DEFAULT_PAGE_SIZE } = {}) {
    return unwrap(await api.get('/api/news/saved/list', { params: { page, pageSize } }))
  },
}
