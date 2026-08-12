# 多智能体文章创作系统

> 基于 Spring Boot 3 + Vue 3 的 AI 创作平台：通过多智能体（Agent）协作完成"资料检索 → 写作 → 审校"的完整创作流程，SSE 流式输出，前端逐字展示。

**在线演示（前端）**：https://dcz329.github.io/article-agent-system/
> 说明：GitHub Pages 为纯静态托管，无 Java 后端。打开后前端自动进入**演示模式**（内置数据跑通登录/创作/流式展示全流程）；本地启动后端后即为真实功能（DeepSeek v4 流式生成）。

## ✨ 亮点

- **多 Agent 编排**：检索 / 写作 / 审校三个 Agent 独立解耦，基于策略模式 + 接口注入实现可插拔扩展，编排链实时记录入库（`agent_flow` 字段可审计）
- **SSE 流式输出**：DeepSeek v4 大模型生成内容通过 `SseEmitter` 逐段推送给前端，首字响应 < 1s，前端逐字展示（对比同步返回体验显著提升）
- **JWT 无状态鉴权**：HS256 签名 + 拦截器统一鉴权，BCrypt 密码加密，防撞库统一错误提示
- **事务保证一致性**：`@Transactional` 保证"消息落库 + 文章落库"原子性
- **配置驱动切换**：`DEEPSEEK_API_KEY` 未配置时自动降级 Mock 模式，演示/CI 不依赖外部服务
- **CI/CD 自动部署**：GitHub Actions 自动构建前端并部署 GitHub Pages

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.5 · MyBatis-Plus 3.5 · MySQL 8 |
| 认证 | JWT（jjwt 0.12）+ 拦截器 · BCrypt |
| AI | DeepSeek v4 API（OpenAI 兼容）· JDK HttpClient 流式解析 SSE |
| 前端 | Vue 3 · Vite |
| 工程 | Maven · Git · GitHub Actions · 环境变量注入密钥（不落盘） |

## 📁 项目结构

```
article-agent-system/
├── sql/schema.sql              # 建表脚本（user/chat_session/message/article）
├── src/main/java/com/deng/article/
│   ├── agent/                  # Agent 编排层：AgentContext / Agent 接口 / 编排器 / 三 Agent
│   ├── llm/                    # LLM 抽象：DeepSeekClient（流式）/ MockLlmClient（降级）
│   ├── config/                 # JwtUtil / JwtInterceptor / WebConfig / LlmConfig
│   ├── controller/             # auth / user / session / agent 接口
│   ├── service/                # UserService / SessionService / AgentService
│   ├── entity/ mapper/ dto/ common/
└── frontend/                   # Vue 3 + Vite（SSE 流式展示）
```

## 🚀 本地运行

```bash
# 1. 建库
mysql -u root -p -e "CREATE DATABASE ai_article DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p ai_article < sql/schema.sql

# 2. 环境变量（不配置 DEEPSEEK_API_KEY 则自动 Mock 演示模式）
export MYSQL_PASSWORD=你的密码
export DEEPSEEK_API_KEY=sk-xxx

# 3. 后端（端口 8080）
mvn spring-boot:run

# 4. 前端（端口 5173，已配置 /api 代理）
cd frontend && npm install && npm run dev
```

## 🔌 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | 注册（BCrypt 加密） |
| POST | `/api/auth/login` | 登录，返回 JWT token |
| GET | `/api/user/me` | 当前用户信息（需 token） |
| POST | `/api/session?title=` | 创建会话 |
| GET | `/api/session/list` | 会话分页列表 |
| GET | `/api/session/{id}` | 会话详情（消息 + 文章） |
| POST | `/api/agent/generate` | 创作（SSE 流式） |

### SSE 事件协议

```
event: session   → {"sessionId": 1}            新建会话
event: agents    → {"agents": "retrieval->writing->review"}  编排链
event: delta     → {"content": "..."}          内容片段（多次）
event: done      → {"articleId": 1}            生成完成
event: error     → {"message": "..."}          失败
```

## 🌐 CI/CD

推送 `master` 分支后，GitHub Actions 自动执行：安装依赖 → `vite build` → 部署 GitHub Pages。
