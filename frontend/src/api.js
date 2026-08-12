// API 封装：登录注册 / 会话管理 / SSE 流式创作

const BASE = '/api'
let token = localStorage.getItem('token') || ''

export function setToken(t) {
  token = t || ''
  if (t) localStorage.setItem('token', t)
  else localStorage.removeItem('token')
}

export function getToken() {
  return token
}

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  })
  if (res.status === 401) {
    setToken('')
    throw new Error('登录已过期，请重新登录')
  }
  const body = await res.json().catch(() => ({}))
  if (body.code !== 200) throw new Error(body.message || '请求失败')
  return body.data
}

export const register = (data) => request('/auth/register', { method: 'POST', body: JSON.stringify(data) })
export const login = (data) => request('/auth/login', { method: 'POST', body: JSON.stringify(data) })
export const listSessions = (page = 1, size = 30) => request(`/session/list?page=${page}&size=${size}`)
export const sessionDetail = (id) => request(`/session/${id}`)

/**
 * 流式创作：解析 SSE 事件并回调
 * 事件：session / agents / delta / done / error
 */
export async function streamGenerate(payload, handlers) {
  const res = await fetch(BASE + '/agent/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(payload),
  })
  if (!res.ok || !res.body) throw new Error('创作请求失败')
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buf.indexOf('\n\n')) !== -1) {
      const raw = buf.slice(0, idx)
      buf = buf.slice(idx + 2)
      const lines = raw.split('\n')
      const ev = (lines.find((l) => l.startsWith('event:')) || '').slice(6).trim()
      const dataLine = (lines.find((l) => l.startsWith('data:')) || '').slice(5).trim()
      if (!dataLine) continue
      let data
      try { data = JSON.parse(dataLine) } catch { continue }
      if (handlers[ev]) handlers[ev](data)
    }
  }
}
