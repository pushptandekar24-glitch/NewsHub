import axios from 'axios'

/**
 * One axios instance for the whole app.
 *
 * WHY a service layer instead of axios calls inside components: the token
 * header, the error shape and the base URL are decided in exactly one place.
 * Components deal in data, never in HTTP.
 */
const api = axios.create({
  // Empty base URL -> relative /api/... -> Vite's dev proxy handles it.
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
})

export const TOKEN_KEY = 'newshub_token'

// Attach the JWT to every outgoing request if we have one.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * Normalise every failure into a plain Error with a readable message, so pages
 * can render `err.message` without unpacking axios internals.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
    }
    const data = error.response?.data
    const message =
      data?.message ||
      (data?.fieldErrors && Object.values(data.fieldErrors)[0]) ||
      error.message ||
      'Something went wrong'
    return Promise.reject(new Error(message))
  }
)

/** The backend wraps everything in { success, message, data }. Unwrap once, here. */
export const unwrap = (response) => response.data?.data

export default api
