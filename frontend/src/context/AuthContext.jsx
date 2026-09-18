import { createContext, useContext, useEffect, useState } from 'react'
import { authService } from '../services/authService'
import { userService } from '../services/userService'

const AuthContext = createContext(null)

/**
 * Holds the signed-in user for the whole tree.
 *
 * The token lives in localStorage; the user object is re-fetched on mount so a
 * refresh does not lose the session and a revoked/expired token is detected
 * immediately rather than on the first protected click.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authService.hasToken()) {
      setLoading(false)
      return
    }
    userService
      .me()
      .then(setUser)
      .catch(() => authService.logout())
      .finally(() => setLoading(false))
  }, [])

  const value = {
    user,
    loading,
    isAuthenticated: Boolean(user),
    isAdmin: user?.role === 'ADMIN',
    async login(credentials) {
      setUser(await authService.login(credentials))
    },
    async register(payload) {
      setUser(await authService.register(payload))
    },
    logout() {
      authService.logout()
      setUser(null)
    },
    setUser,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
