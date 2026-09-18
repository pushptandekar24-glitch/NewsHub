import { useEffect, useState } from 'react'
import { Clock, Play, Terminal } from 'lucide-react'
import api from '../services/api'
import { categoryService } from '../services/categoryService'

/**
 * API Explorer for this platform's own endpoints.
 *
 * Response time is measured client-side around the request, so it includes
 * network latency — which is the number a developer actually cares about when
 * testing an endpoint from a browser.
 */
const ENDPOINTS = [
  { label: 'List categories', method: 'GET', path: '/api/categories', params: [] },
  { label: 'List countries', method: 'GET', path: '/api/countries', params: [] },
  { label: 'News feed', method: 'GET', path: '/api/news', params: ['category', 'country', 'page', 'pageSize'] },
  { label: 'Search news', method: 'GET', path: '/api/news/search', params: ['q', 'country', 'sort', 'pageSize'] },
  { label: 'Trending news', method: 'GET', path: '/api/news/trending', params: ['limit'] },
  { label: 'My profile', method: 'GET', path: '/api/users/me', params: [] },
]

const METHOD_TONE = {
  GET: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400',
  POST: 'bg-sky-50 text-sky-700 dark:bg-sky-500/15 dark:text-sky-400',
}

export default function ApiExplorer() {
  const [selected, setSelected] = useState(ENDPOINTS[0])
  const [values, setValues] = useState({})
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [categories, setCategories] = useState([])

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => {})
  }, [])

  const requestUrl = (() => {
    const query = new URLSearchParams(
      Object.entries(values).filter(([k, v]) => selected.params.includes(k) && v !== '')
    ).toString()
    return selected.path + (query ? `?${query}` : '')
  })()

  async function send() {
    setBusy(true)
    const started = performance.now()
    try {
      const params = Object.fromEntries(
        Object.entries(values).filter(([k, v]) => selected.params.includes(k) && v !== '')
      )
      const response = await api.get(selected.path, { params })
      setResult({
        status: response.status,
        statusText: response.statusText || 'OK',
        ms: Math.round(performance.now() - started),
        body: response.data,
      })
    } catch (err) {
      setResult({
        status: 'ERROR',
        statusText: err.message,
        ms: Math.round(performance.now() - started),
        body: { message: err.message },
      })
    } finally {
      setBusy(false)
    }
  }

  const ok = typeof result?.status === 'number' && result.status < 400

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <header className="flex items-start gap-4">
        <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-brand-50 dark:bg-brand-500/10">
          <Terminal className="h-6 w-6 text-brand-500" />
        </span>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">API Explorer</h1>
          <p className="mt-1.5 text-sm text-ink-500 dark:text-slate-400">
            Send live requests to the NewsHub backend and inspect status, timing and JSON.
          </p>
        </div>
      </header>

      <div className="grid gap-6 lg:grid-cols-5">
        {/* ------------------------------------------------------ request */}
        <div className="panel space-y-5 p-6 lg:col-span-2">
          <div>
            <label className="mb-1.5 block text-sm font-medium">Endpoint</label>
            <select
              className="input"
              value={selected.path}
              onChange={(e) => {
                setSelected(ENDPOINTS.find((x) => x.path === e.target.value))
                setValues({})
                setResult(null)
              }}
            >
              {ENDPOINTS.map((endpoint) => (
                <option key={endpoint.path} value={endpoint.path}>{endpoint.label}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2 rounded-xl border border-edge bg-surface-sunken px-3 py-2.5
                          dark:border-night-edge dark:bg-night-sunken">
            <span className={`rounded-md px-2 py-1 font-mono text-[11px] font-bold ${METHOD_TONE[selected.method]}`}>
              {selected.method}
            </span>
            <code className="truncate font-mono text-xs text-ink-600 dark:text-slate-300">{requestUrl}</code>
          </div>

          {selected.params.length > 0 && (
            <div className="space-y-3">
              <p className="text-sm font-medium">Parameters</p>
              {selected.params.map((param) => (
                <div key={param}>
                  <label className="mb-1 block font-mono text-[11px] text-ink-400">{param}</label>
                  {param === 'category' ? (
                    <select
                      className="input"
                      value={values[param] || ''}
                      onChange={(e) => setValues({ ...values, [param]: e.target.value })}
                    >
                      <option value="">(none)</option>
                      {categories.map((c) => <option key={c.slug} value={c.slug}>{c.name}</option>)}
                    </select>
                  ) : param === 'sort' ? (
                    <select
                      className="input"
                      value={values[param] || ''}
                      onChange={(e) => setValues({ ...values, [param]: e.target.value })}
                    >
                      <option value="">publishedAt (default)</option>
                      <option value="relevancy">relevancy</option>
                    </select>
                  ) : (
                    <input
                      className="input"
                      placeholder={
                        param === 'q' ? 'artificial intelligence'
                        : param === 'country' ? 'in'
                        : param === 'pageSize' ? '20'
                        : ''
                      }
                      value={values[param] || ''}
                      onChange={(e) => setValues({ ...values, [param]: e.target.value })}
                    />
                  )}
                </div>
              ))}
            </div>
          )}

          <button onClick={send} disabled={busy} className="btn-primary w-full">
            <Play className="h-4 w-4" /> {busy ? 'Sending…' : 'Send request'}
          </button>
        </div>

        {/* ----------------------------------------------------- response */}
        <div className="panel flex flex-col p-6 lg:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm font-medium">Response</p>
            {result && (
              <div className="flex items-center gap-3 text-xs">
                <span className={`rounded-md px-2 py-1 font-mono font-semibold ${
                  ok ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400'
                     : 'bg-rose-50 text-rose-700 dark:bg-rose-500/15 dark:text-rose-400'}`}>
                  {result.status} {result.statusText}
                </span>
                <span className="flex items-center gap-1 text-ink-500 dark:text-slate-400">
                  <Clock className="h-3.5 w-3.5" /> {result.ms}ms
                </span>
              </div>
            )}
          </div>

          {!result ? (
            <div className="grid flex-1 place-items-center rounded-xl border border-dashed border-edge
                            py-16 text-sm text-ink-400 dark:border-night-edge">
              Send a request to see the response
            </div>
          ) : (
            <pre className="max-h-[32rem] flex-1 overflow-auto rounded-xl bg-ink-900 p-4
                            font-mono text-xs leading-relaxed text-slate-200 dark:bg-night-sunken">
              {JSON.stringify(result.body, null, 2)}
            </pre>
          )}
        </div>
      </div>
    </div>
  )
}
