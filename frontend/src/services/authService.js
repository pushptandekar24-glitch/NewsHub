import api, { unwrap, TOKEN_KEY } from './api'

export const authService = {
  async register({ name, email, password, interestSlugs = [] }) {
    const data = unwrap(await api.post('/api/auth/register', { name, email, password, interestSlugs }))
    localStorage.setItem(TOKEN_KEY, data.token)
    return data.user
  },

  async login({ email, password }) {
    const data = unwrap(await api.post('/api/auth/login', { email, password }))
    localStorage.setItem(TOKEN_KEY, data.token)
    return data.user
  },

  logout() {
    localStorage.removeItem(TOKEN_KEY)
  },

  hasToken() {
    return Boolean(localStorage.getItem(TOKEN_KEY))
  },
}
