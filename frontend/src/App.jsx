import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'

import ApiExplorer from './pages/ApiExplorer'
import ArticleDetail from './pages/ArticleDetail'
import Categories from './pages/Categories'
import Countries from './pages/Countries'
import Dashboard from './pages/Dashboard'
import FeedPage from './pages/FeedPage'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import Register from './pages/Register'
import Saved from './pages/Saved'
import SearchResults from './pages/SearchResults'
import Settings from './pages/Settings'
import Trending from './pages/Trending'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />

            <Route path="/explore" element={<FeedPage title="Explore" />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/categories/:slug" element={<FeedPage lockCategory />} />
            <Route path="/countries" element={<Countries />} />
            <Route path="/countries/:code" element={<FeedPage lockCountry />} />
            <Route path="/trending" element={<Trending />} />
            <Route path="/search" element={<SearchResults />} />
            <Route path="/article/:id" element={<ArticleDetail />} />
            <Route path="/api-explorer" element={<ApiExplorer />} />

            <Route path="/saved" element={<ProtectedRoute><Saved /></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />

            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
