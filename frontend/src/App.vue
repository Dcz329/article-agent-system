<script setup>
import { ref, onMounted } from 'vue'
import {
  register as apiRegister,
  login as apiLogin,
  listSessions,
  sessionDetail,
  streamGenerate,
  setToken,
  getToken,
} from './api.js'

// ===== 认证状态 =====
const authed = ref(!!getToken())
const mode = ref('login') // login | register
const form = ref({ username: '', password: '', nickname: '' })
const user = ref(null)
const authError = ref('')

// ===== 会话状态 =====
const sessions = ref([])
const currentSessionId = ref(null)
const historyMessages = ref([]) // 当前会话历史 [{role, content}]
const savedArticle = ref(null)

// ===== 创作状态 =====
const topic = ref('')
const style = ref('')
const agents = ref('retrieval,writing,review')
const generating = ref(false)
const streamText = ref('')
const agentFlow = ref('')
const errorMsg = ref('')

async function refreshSessions() {
  sessions.value = (await listSessions()).records || []
}

async function submitAuth() {
  authError.value = ''
  try {
    if (mode.value === 'register') {
      await apiRegister(form.value)
      mode.value = 'login'
      authError.value = '注册成功，请登录'
    } else {
      const data = await apiLogin({ username: form.value.username, password: form.value.password })
      setToken(data.token)
      user.value = data.user
      authed.value = true
      await refreshSessions()
    }
  } catch (e) {
    authError.value = e.message
  }
}

function logout() {
  setToken('')
  authed.value = false
  user.value = null
  sessions.value = []
  streamText.value = ''
}

async function openSession(id) {
  currentSessionId.value = id
  const detail = await sessionDetail(id)
  historyMessages.value = (detail.messages || []).map((m) => ({ role: m.role, content: m.content }))
  savedArticle.value = detail.article || null
  streamText.value = savedArticle.value ? savedArticle.value.content : ''
}

async function generate() {
  if (!topic.value.trim() || generating.value) return
  generating.value = true
  errorMsg.value = ''
  streamText.value = ''
  agentFlow.value = ''
  historyMessages.value = []
  try {
    await streamGenerate(
        { sessionId: currentSessionId.value || null, topic: topic.value, style: style.value, agents: agents.value },
        {
          session: (d) => { currentSessionId.value = d.sessionId },
          agents: (d) => { agentFlow.value = d.agents },
          delta: (d) => { streamText.value += d.content },
          done: async () => {
            const detail = await sessionDetail(currentSessionId.value)
            savedArticle.value = detail.article || null
            historyMessages.value = (detail.messages || []).map((m) => ({ role: m.role, content: m.content }))
            await refreshSessions()
          },
          error: (d) => { errorMsg.value = d.message },
        },
    )
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    generating.value = false
  }
}

// 极简 Markdown 渲染（标题/加粗/换行），够展示用
function markdownToHtml(text) {
  if (!text) return ''
  return text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/^### (.*)$/gm, '<h3>$1</h3>')
      .replace(/^## (.*)$/gm, '<h2>$1</h2>')
      .replace(/^# (.*)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\n/g, '<br/>')
}

onMounted(async () => {
  if (authed.value) await refreshSessions()
})
</script>

<template>
  <!-- ===== 登录 / 注册 ===== -->
  <div v-if="!authed" class="auth-wrap">
    <div class="auth-card">
      <h1>多智能体文章创作系统</h1>
      <p class="sub">AI Agent 协作 · SSE 流式输出 · Spring Boot 3</p>
      <div class="tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>
      <input v-model="form.username" placeholder="用户名" />
      <input v-if="mode === 'register'" v-model="form.nickname" placeholder="昵称（可选）" />
      <input v-model="form.password" type="password" placeholder="密码" @keyup.enter="submitAuth" />
      <p v-if="authError" class="msg">{{ authError }}</p>
      <button class="primary" @click="submitAuth">{{ mode === 'login' ? '登 录' : '注 册' }}</button>
    </div>
  </div>

  <!-- ===== 主界面 ===== -->
  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">📝 多智能体创作</div>
      <button class="new-btn" @click="currentSessionId = null; topic = ''; streamText = ''">＋ 新会话</button>
      <ul class="sessions">
        <li v-for="s in sessions" :key="s.id" :class="{ active: s.id === currentSessionId }" @click="openSession(s.id)">
          {{ s.title }}
        </li>
      </ul>
      <div class="user-row">
        <span>{{ user?.nickname || user?.username }}</span>
        <button @click="logout">退出</button>
      </div>
    </aside>

    <main class="workspace">
      <div class="control">
        <input v-model="topic" placeholder="输入文章主题，如：什么是 RAG 检索增强生成？" />
        <input v-model="style" placeholder="风格/要求（可选）" class="style-input" />
        <select v-model="agents" class="agents-select">
          <option value="writing">写作（默认）</option>
          <option value="retrieval,writing">检索 + 写作</option>
          <option value="retrieval,writing,review">检索 + 写作 + 审校</option>
        </select>
        <button class="primary gen" :disabled="generating || !topic.trim()" @click="generate">
          {{ generating ? '生成中…' : '生成文章' }}
        </button>
      </div>
      <p v-if="agentFlow" class="flow">编排链：{{ agentFlow }}</p>
      <p v-if="errorMsg" class="msg error">{{ errorMsg }}</p>

      <div class="content">
        <div v-if="!streamText" class="placeholder">输入主题后点击「生成文章」，内容将以流式方式逐字展示</div>
        <div v-else class="article" v-html="markdownToHtml(streamText)"></div>
      </div>

      <details v-if="historyMessages.length" class="history">
        <summary>本轮对话历史（{{ historyMessages.length }} 条）</summary>
        <div v-for="(m, i) in historyMessages" :key="i" class="hist-item">
          <b>{{ m.role === 'user' ? '用户' : 'AI' }}：</b><span>{{ m.content.slice(0, 120) }}</span>
        </div>
      </details>
    </main>
  </div>
</template>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: #f4f6fa; color: #1f2937; }

/* 认证页 */
.auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; }
.auth-card { width: 380px; background: #fff; border-radius: 14px; padding: 36px; box-shadow: 0 8px 30px rgba(0,0,0,.08); }
.auth-card h1 { font-size: 20px; text-align: center; }
.auth-card .sub { text-align: center; color: #9ca3af; font-size: 12px; margin: 8px 0 22px; }
.tabs { display: flex; gap: 8px; margin-bottom: 18px; }
.tabs button { flex: 1; padding: 9px; border: 1px solid #e5e7eb; background: #fff; border-radius: 8px; cursor: pointer; }
.tabs button.active { background: #2563eb; color: #fff; border-color: #2563eb; }
.auth-card input { width: 100%; padding: 11px 12px; margin-bottom: 12px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; }
.msg { font-size: 13px; color: #16a34a; margin: 8px 0; }
.msg.error { color: #dc2626; }

/* 布局 */
.layout { display: flex; height: 100vh; }
.sidebar { width: 240px; background: #111827; color: #d1d5db; display: flex; flex-direction: column; }
.brand { padding: 18px 16px; font-weight: 600; color: #fff; font-size: 15px; border-bottom: 1px solid #1f2937; }
.new-btn { margin: 12px; padding: 9px; background: #2563eb; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
.sessions { flex: 1; overflow-y: auto; list-style: none; }
.sessions li { padding: 11px 16px; cursor: pointer; font-size: 13px; border-bottom: 1px solid #1f2937; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sessions li:hover { background: #1f2937; }
.sessions li.active { background: #2563eb; color: #fff; }
.user-row { padding: 12px 16px; border-top: 1px solid #1f2937; display: flex; justify-content: space-between; font-size: 13px; }
.user-row button { background: transparent; border: 1px solid #4b5563; color: #d1d5db; border-radius: 6px; padding: 3px 10px; cursor: pointer; }

/* 工作区 */
.workspace { flex: 1; display: flex; flex-direction: column; padding: 18px 22px; gap: 12px; min-width: 0; }
.control { display: flex; gap: 10px; flex-wrap: wrap; }
.control input, .control select { padding: 11px 12px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; }
.control input:first-child { flex: 1; min-width: 220px; }
.style-input { flex: 0 0 200px; }
.agents-select { flex: 0 0 170px; }
.primary { padding: 11px 20px; background: #2563eb; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
.primary:disabled { opacity: .5; cursor: not-allowed; }
.flow { font-size: 12px; color: #2563eb; }
.content { flex: 1; background: #fff; border-radius: 12px; padding: 24px; overflow-y: auto; border: 1px solid #e5e7eb; }
.placeholder { color: #9ca3af; text-align: center; margin-top: 60px; font-size: 14px; }
.article { line-height: 1.9; font-size: 15px; color: #1f2937; }
.article h1 { font-size: 22px; font-weight: 700; color: #111827; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px; }
.article h2 { font-size: 19px; font-weight: 700; color: #1d4ed8; margin-top: 20px; }
.article h3 { font-size: 16px; font-weight: 700; color: #374151; }
.history { font-size: 13px; color: #6b7280; border-top: 1px solid #e5e7eb; padding-top: 10px; }
.hist-item { padding: 4px 0; }
</style>