// API 封装：登录注册 / 会话管理 / SSE 流式创作
// 后端不可用（如 GitHub Pages 纯静态托管）时自动降级为「演示模式」，用内置数据跑通界面全流程

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

// ===== 演示模式数据 =====
const DEMO_USER = { id: 1, username: 'demo', nickname: '演示用户' }
const DEMO_TOKEN = 'demo-token'

function demoFallback(path) {
  if (path === '/auth/login' || path === '/auth/register') {
    return { token: DEMO_TOKEN, user: DEMO_USER }
  }
  if (path.startsWith('/session/list')) {
    return {
      records: [
        { id: 1, title: '示例会话：什么是 RAG 检索增强生成？', userId: 1, updatedAt: '2026-08-12T10:00:00' },
        { id: 2, title: '示例会话：Java 虚拟线程入门', userId: 1, updatedAt: '2026-08-12T09:30:00' },
      ],
      total: 2,
    }
  }
  if (path.startsWith('/session/')) {
    const id = path.split('/').pop()
    return {
      session: { id: Number(id), userId: 1, title: '示例会话' },
      messages: [
        { id: 1, sessionId: Number(id), role: 'user', content: '请写一篇通俗易懂的技术文章', createdAt: '2026-08-12T10:00:00' },
        { id: 2, sessionId: Number(id), role: 'assistant', content: '（演示模式）这是一篇示例文章……连接本地后端后可体验真实 DeepSeek 流式生成。', createdAt: '2026-08-12T10:01:00' },
      ],
      article: {
        id: 1, sessionId: Number(id), userId: 1,
        title: '示例文章', agentFlow: 'retrieval->writing->review',
        content: '（演示模式）这是一篇示例文章……连接本地后端后可体验真实 DeepSeek 流式生成。',
        createdAt: '2026-08-12T10:01:00',
      },
    }
  }
  return null
}

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(BASE + path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
      },
    })
  } catch (e) {
    // 网络不通（后端未启动）→ 演示模式
    const demo = demoFallback(path)
    if (demo) return demo
    throw new Error('请求失败：后端服务不可用')
  }
  if (res.status === 401) {
    setToken('')
    throw new Error('登录已过期，请重新登录')
  }
  const body = await res.json().catch(() => ({}))
  if (body.code !== 200) {
    // 后端返回业务错误；若后端根本不存在（404/405）则降级演示
    const demo = demoFallback(path)
    if (demo) return demo
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

export const register = (data) => request('/auth/register', { method: 'POST', body: JSON.stringify(data) })
export const login = (data) => request('/auth/login', { method: 'POST', body: JSON.stringify(data) })
export const listSessions = (page = 1, size = 30) => request(`/session/list?page=${page}&size=${size}`)
export const sessionDetail = (id) => request(`/session/${id}`)

/** 演示模式的模拟流式输出 */
function demoStream(payload, handlers) {
  const chunks = [
    '这是一篇由「演示模式」生成的示例文章（连接本地后端后可体验真实 DeepSeek v4 流式生成）。\n\n',
    '## 一、引言\n',
    `${payload.topic} 是当前 AI 技术栈中的热门话题，本文用通俗语言介绍其核心思想。\n\n`,
    '## 二、核心概念\n',
    '系统通过多智能体协作完成任务拆解，各 Agent 独立解耦、按编排链依次执行，结果可审计。\n\n',
    '## 三、总结\n',
    '原型已验证完整链路：登录鉴权 → 会话管理 → Agent 编排 → SSE 流式输出。',
  ]
  handlers.session?.({ sessionId: 999 })
  handlers.agents?.({ agents: payload.agents || 'writing' })
  let i = 0
  const timer = setInterval(() => {
    if (i < chunks.length) {
      handlers.delta?.({ content: chunks[i++] })
    } else {
      clearInterval(timer)
      handlers.done?.({ articleId: 999 })
    }
  }, 120)
}

/**
 * 流式创作：解析 SSE 事件并回调
 * 事件：session / agents / delta / done / error
 */
export async function streamGenerate(payload, handlers) {
  let res
  try {
    res = await fetch(BASE + '/agent/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(payload),
    })
  } catch (e) {
    return demoStream(payload, handlers)
  }
  if (!res.ok || !res.body) {
    return demoStream(payload, handlers)
  }
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
