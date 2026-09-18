import api, { unwrap } from './api'

export const categoryService = {
  async list() {
    return unwrap(await api.get('/api/categories'))
  },

  async countries() {
    return unwrap(await api.get('/api/countries'))
  },
}
