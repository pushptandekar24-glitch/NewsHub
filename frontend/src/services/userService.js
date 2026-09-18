import api, { unwrap } from './api'

export const userService = {
  async me() {
    return unwrap(await api.get('/api/users/me'))
  },

  async updateInterests(interestSlugs) {
    return unwrap(await api.put('/api/users/me/interests', { interestSlugs }))
  },
}
