# AI MCP Platform 前端设计说明

> 适用范围：`frontend/` 与 `packages/agent-ui/` 当前实现。
>
> 本文描述现有界面的视觉语言、信息架构、交互状态、富可视化组件和扩展约束，并说明迁移到正式 AG-UI Client 后应保持的设计边界。运行服务 `http://localhost:5173/` 在分析时可正常返回 HTTP 200；由于浏览器视图连接未成功，本次没有取得运行态截图，尺寸、颜色和状态结论均以当前源码为准，最终视觉验收仍需在真实浏览器中补做。

相关架构方案见：[可移植方案.md](./可移植方案.md)。

## 1. 设计定位

当前界面是一套面向分析型 Agent 的单会话工作台，核心体验不是传统后台管理系统，也不是纯聊天气泡，而是：

> 低饱和雾白背景上的轻量玻璃工作台，以深色结构、蓝色操作和大面积留白承载“文字流 + 工具过程 + 富可视化结果”。

它具有以下特征：

- 视觉克制：正文和容器以 Slate 中性色为主，品牌蓝只用于发送、流式状态、选中反馈和数据强调。
- 对话优先：主区只保留标题、消息流和输入框，不引入复杂导航。
- 分析结果内联：Chart、Table、Timeline、RelationGraph 作为消息的一部分出现，而不是跳转到独立报表页。
- 过程可追溯：工具、LLM 和 UI 事件收纳在右侧执行日志抽屉中，不持续占用主对话空间。
- 桌面工作台、移动对话页双形态：大屏显示侧栏，小屏收起侧栏并将新建操作移入标题栏。

## 2. 页面信息架构

```text
页面背景（mist + 蓝/青环境光）
└── 工作区容器（最大宽度 1480px，高度 100vh）
    ├── 左侧栏（仅 lg 及以上，宽 288px）
    │   ├── 产品标识：Agent Workspace / AI MCP Platform
    │   ├── 新建对话
    │   ├── 当前会话状态与 conversationId
    │   └── Agent Client 说明卡
    └── 主对话面板
        ├── 标题栏：智能分析助手 / 连接状态 / 移动端新建按钮
        ├── 消息滚动区
        │   ├── Assistant/User 消息
        │   ├── TextPart 与 UiSurfacePart 交错内容
        │   ├── 复制与执行日志入口
        │   └── 首屏建议问题
        └── 输入区
            ├── 错误横幅
            ├── 自动增高输入框 + 发送按钮
            └── 快捷键与内容声明
```

页面采用固定标题栏和固定输入区，中间消息区独立滚动。对话正文宽度限制为 `max-w-3xl`，避免宽屏下单行文本过长；整个工作区限制为 `1480px`，保证侧栏与主区在大屏上仍保持聚合关系。

## 3. 视觉语言

### 3.1 色彩

核心 Token 定义于 `frontend/tailwind.config.ts`：

| Token | 色值 | 用途 |
|---|---:|---|
| `mist` | `#f5f7fb` | 页面底色 |
| `ink` | `#182032` | Logo 底、主要深色按钮、Assistant 头像、正文基准色 |
| `brand-50` | `#eef5ff` | 品牌浅背景、提示卡 Hover |
| `brand-100` | `#dceaff` | Focus Ring、用户头像浅底 |
| `brand-500` | `#3b76f6` | 活跃状态、光标、动效强调 |
| `brand-600` | `#295fd8` | 用户气泡、发送按钮 |
| `brand-700` | `#234fb3` | Hover、强调文字 |

辅助语义色使用 Tailwind 默认色系：

| 语义 | 色系 | 当前用途 |
|---|---|---|
| 正常/完成 | Emerald | 在线状态、UI 日志 |
| 工具调用 | Amber | TOOL 日志标签 |
| 错误 | Rose | 请求错误、Renderer 错误 |
| 次要信息 | Slate | 边框、说明、时间、禁用态 |

背景上叠加两个低透明度径向光斑：右上蓝色 `rgba(91, 139, 255, 0.17)`，左下青色 `rgba(114, 199, 187, 0.14)`。光斑只承担空间氛围，不承载信息。

### 3.2 色彩一致性问题

应用层品牌蓝是 `#3b76f6/#295fd8/#234fb3`，SDK 内的富组件却使用了另一组 Indigo：

- `UiSurfacePart` 加载图标、`ChartRenderer` 柱形：`#6366f1`
- `RelationGraphRenderer` 节点：`#4f46e5`

这会让图表看起来像嵌入的第三方组件。后续应把 SDK 颜色改成可注入的 CSS Variables 或主题对象，并让默认值与应用层 Brand Token 对齐。

### 3.3 字体与排版

- 字体栈：`Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`。
- 会话 ID、日志时间和日志详情使用等宽字体。
- 页面没有大标题，主要文字集中在 `10px`～`16px`，形成工具型产品的紧凑层级。
- 消息正文为 `14px`，`sm` 以上为 `15px`，行高 `28px`，适合连续阅读。
- 标题通常为 `14px`～`16px`、Semibold；说明文字通常为 `10px`～`12px`、Slate 400/500。
- Assistant 文本使用 `white-space: pre-wrap`，当前不解析 Markdown。

中文环境实际会回退到系统中文字体。若未来需要跨 Windows/macOS 保持更稳定的中文观感，应显式补充中文字体栈，而不是只依赖 `system-ui`。

### 3.4 圆角、边框与阴影

页面通过较大圆角建立柔和、低压力的工作台形象：

| 层级 | 典型圆角 |
|---|---|
| 主面板/侧栏 | `26px`～`30px` |
| 输入框 | `22px` |
| 用户气泡 | `20px`，右上角收紧 |
| 卡片/富组件 | `16px` |
| 普通按钮/建议问题 | `12px`～`16px` |

主面板使用白色半透明背景、白色高光边框和 `backdrop-blur-xl`，构成轻度玻璃拟态。阴影保持扩散、低对比：

- Panel：`0 24px 80px -36px rgba(38, 58, 105, 0.38)`
- Composer：`0 12px 34px -16px rgba(29, 55, 102, 0.26)`

富结果卡只使用细边框和极浅阴影，视觉权重低于对话容器。

## 4. 布局与响应式规则

### 4.1 桌面端

- 外层工作区最大宽度 `1480px`，大屏内边距 `28px`，两列间距 `20px`。
- 左侧栏固定宽度 `288px`，只在 Tailwind `lg` 断点及以上显示。
- 主区占剩余空间，并通过 `min-w-0` 避免富组件撑破 Flex 布局。
- 标题栏固定高度 `72px`。
- 消息及输入区内容最大宽度 `768px`。
- 消息间距约 `28px`，优先保持呼吸感，而不是追求信息密度。

### 4.2 移动端和平板

- `lg` 以下完全隐藏侧栏；品牌图标和新建对话按钮出现在主标题栏。
- 页面外边距由 `12px` 起步，`sm` 后提升至 `20px`。
- 主面板圆角从 `26px` 提升到 `30px`。
- 消息宽度小屏最大为容器的 `85%`，`sm` 后为 `78%`。
- 日志抽屉在窄屏占满宽度，在宽屏限制为 `max-w-xl`。

当前使用 `h-screen`。移动浏览器地址栏收放时可能出现高度跳动或底部输入区遮挡，后续应评估 `100dvh`/`min-h-dvh`。

## 5. 核心组件规范

### 5.1 左侧工作区

侧栏的职责是展示产品身份、本地会话状态和新建入口，不是完整的历史会话导航。

- Logo 使用 `ink` 深色方块和线性对话图标。
- “新建对话”是侧栏唯一主按钮，深色底而非品牌蓝，避免与发送动作竞争。
- 当前会话卡显示状态点和截断的 `conversationId`。
- 发送中状态由蓝色实心点叠加 `animate-ping` 表达；空闲状态为绿色。
- 底部 Agent Client 卡以浅蓝背景解释流式和 MCP 工具行为。

设计约束：除非真正支持历史会话，不应把当前区域扩展成伪造的会话列表；新增历史能力时应同时定义选中、悬停、重命名、删除和加载失败状态。

### 5.2 标题栏

标题栏只表达三个信息：当前 Agent 名称、连接/驱动状态、移动端新建入口。它不承担模型切换、配置或用户账户功能。

当前状态文案是静态的“由 Agent Client 驱动”，绿色点也不代表真实健康检查。若后续对接两个 Runtime，应把这里改为运行时可配置名称，并让颜色反映真实连接状态：

| 状态 | 建议表现 |
|---|---|
| 正常 | 绿色点 + Runtime 名称 |
| 连接中/重连 | 蓝色动画点 + 简短状态 |
| 断开 | Rose 点 + 可操作重试入口 |

### 5.3 消息

Assistant 与 User 使用不对称表达：

- Assistant：左对齐，深色星形头像，正文无气泡底色，让长文本更像分析报告。
- User：右对齐，浅蓝“你”头像，品牌蓝实心气泡，白色文字。
- 两者都显示小号角色标签；Assistant 流式时附加“正在回复”。
- TextPart 与 UiSurfacePart 按事件到达顺序交错渲染，因此图表可以出现在解释文字之间。
- Assistant 完成后才出现“复制”和“日志”操作。

这套不对称设计适合“用户短问、Agent 长答”的分析场景。不要给 Assistant 长文本整体套实心大气泡，否则会削弱可读性并与富组件卡片形成多层嵌套。

### 5.4 首屏建议问题

只有消息数为 1 时显示建议问题。按钮是白底细边框胶囊卡，Hover 时轻微上浮并转为品牌浅色。

点击后只把问题填入输入框并聚焦，不立即发送。这一行为给用户保留了编辑机会，适合可能包含实体 ID、时间范围等参数的分析问题。

### 5.5 输入区

- 输入框单行起步，随内容自动增高，最高 `160px`。
- 最大长度 `8000`。
- Enter 发送，Shift + Enter 换行。
- 空内容或发送中禁用发送按钮。
- Focus 时边框转品牌蓝并出现浅蓝 Ring。
- 发送中按钮显示旋转圆环，同时禁用 Textarea。
- 请求级错误在输入框上方显示 Rose 浅底横幅。

当前一轮发送期间不能输入下一条消息，也没有显式“停止生成”按钮。正式 AG-UI 接入取消能力后，发送按钮应在运行中切换为停止按钮，并保留清晰的可访问名称。

### 5.6 消息操作

复制与日志入口默认透明度为 0，鼠标 Hover 消息或键盘 Focus 时显示。这保持了界面洁净，但触屏用户没有 Hover，操作发现性较弱。

建议规则：

- 桌面可继续按 Hover 显示。
- 触摸设备或窄屏应常显低对比图标，或由“更多”按钮承载。
- 复制后应给出短暂成功反馈，目前没有反馈。
- 所有操作按钮补齐一致的 Focus Ring。

## 6. 运行状态模型

### 6.1 会话状态

```text
Idle
  └── Send
      └── Connecting/Running
          ├── Text streaming
          ├── Tool running
          ├── UI loading → ready/error
          ├── Done → Idle
          ├── Error → Idle
          └── Abort/New conversation → Idle(new thread)
```

当前状态由 `isSending`、消息 `streaming/failed` 和 UiSurface 内部 `loading/ready/error` 共同表达。

### 6.2 可见反馈

| 状态 | 当前可见反馈 |
|---|---|
| 空闲 | 绿色状态点、可编辑输入框 |
| 已提交、首个文本前 | Assistant 三个跳动圆点 |
| 文本流式 | “正在回复” + 行尾脉冲光标 |
| 整轮运行中 | 侧栏蓝色 Ping、发送按钮 Spinner、输入框禁用 |
| UI 数据加载 | 类型文案 + 五列 Shimmer 骨架 |
| UI 不支持 | “暂不支持的组件类型” |
| UI 加载失败 | 卡片内 Rose 错误文案 |
| Run 失败 | Assistant 错误文字 + 输入区错误横幅 |
| 完成 | 移除流式标记，Hover 后可复制/查看日志 |

工具正在执行时，主对话区没有独立的实时 Tool 状态，只能在完成后打开日志查看。正式 AG-UI 接入后，可在 Assistant 消息中加入低权重的 Tool Activity 行，但不应把完整参数和结果直接铺在主消息中。

## 7. 执行日志抽屉

日志从右侧覆盖进入，背景使用 `slate-900/30` 加轻度模糊。抽屉宽度最大 `576px`，窄屏全宽。

事件按垂直时间线展示：

| 类别 | 标签/颜色 |
|---|---|
| Run | `RUN` / Slate |
| 模型 | `LLM` / Brand |
| 工具 | `TOOL` / Amber |
| 富 UI | `UI` / Emerald |
| 错误 | `ERROR` / Rose |

详情少于等于 60 字时直接展开，较长时由用户展开；JSON 会格式化并以等宽字体显示。抽屉淡入 `200ms`，内容向左滑入 `250ms`。

当前可访问性不足：虽有 `role="dialog"` 和关闭按钮，但没有 `aria-modal`、焦点陷阱、打开后聚焦、Esc 关闭及关闭后焦点恢复。实施日志面板重构时应把这些作为完成条件。

## 8. 富可视化 Surface

### 8.1 容器生命周期

`UiSurfacePart` 接收 `surface` 后：

1. 取 `components[0]` 作为主组件。
2. 根据 `dataRef` 异步获取完整结果。
3. 加载时显示占位骨架，文字流继续。
4. 数据到达后等待两个 Animation Frame，再切换到 Renderer。
5. 根据组件 `type` 从 Registry 解析 Renderer。
6. 失败或不支持时原位降级，不影响消息中的其他 Part。

Surface 是白底、Slate 边框、`16px` 圆角的内联卡片。加载骨架高度 `240px`，与常见图表接近，目的是降低布局跳变。

当前 `components` 类型为数组，但实际只渲染第一项。设计与协议必须统一：如果 Profile 允许多个组件，就应定义网格/堆叠布局；若短期只支持单组件，则 Schema 应将这一限制显式化。

### 8.2 Chart

- 使用 ECharts Canvas，固定高度 `288px`。
- 支持 bar、line、area、pie、scatter；未知 `subType` 降级为 bar。
- 标题居中，坐标图保留 Tooltip；柱形最大宽度 `64px`。
- 支持声明式 Drill Down；可下钻时鼠标变为 Pointer，点击数据项后发起新一轮自然语言追问。
- 使用 ResizeObserver 和 Window Resize 双重兜底。

当前问题：只有非饼图系列显式设色，Pie 和其他 ECharts 默认色可能偏离品牌；Canvas 缺少等价数据摘要；空数据没有专门空态。

### 8.3 Table

- 最大高度 `384px`，内部滚动。
- 表头吸顶，行使用浅色斑马纹。
- 优先使用 `encoding.columns`，否则按第一行字段自动推导列。
- 最多显示 100 行，并给出截断提示。

建议补充表格 Caption、表头 `scope`、空数据状态、横向溢出提示，以及对超长对象值的折叠策略。

### 8.4 Timeline

- 使用竖直细线和品牌色节点。
- 时间使用等宽字体，Group 使用浅蓝标签。
- 按时间字符串降序排列，最多 50 条。
- 有明确的空数据和截断提示。

时间格式当前只做字符串替换，没有本地化和时区转换。若面向多时区用户，应由契约明确时间语义并在 UI 统一格式化。

### 8.5 RelationGraph

- 使用 AntV G6 Force 布局，固定高度 `320px`。
- 节点支持拖拽，画布支持缩放和平移。
- 节点为 Indigo，边为 Slate，并显示方向箭头。

关系图需要补充空数据状态、操作提示、缩放复位、节点/边 Focus 或列表替代视图；当前只有 Source Label 补全逻辑，Target Label 可能退化为 ID。

## 9. 动效原则

现有动效都用于表达状态：

- Ping：整轮运行中。
- 三点跳动：尚未收到首个文本 Chunk。
- 脉冲光标：文本仍在继续。
- Shimmer：富结果正在获取。
- Spinner：发送按钮处于不可重复提交状态。
- Drawer Slide：建立侧层与主层关系。
- Prompt 上浮：表达可点击性。

全局已实现 `prefers-reduced-motion: reduce`，会将动画缩短并取消平滑滚动。新增动效也必须服从该媒体查询。禁止加入没有状态含义的循环装饰动画。

## 10. 可访问性基线

已经具备：

- HTML 使用 `lang="zh-CN"`。
- 关键图标按钮、输入框和发送按钮有 `aria-label`。
- Loading 使用 `role="status"` 和 `aria-live="polite"`。
- 日志容器声明为 Dialog。
- 装饰性 SVG 多数设置 `aria-hidden`。
- Reduced Motion 全局兜底。

需要补齐：

- 为日志抽屉实现完整 Modal 焦点管理。
- 为复制、日志、关闭、建议问题提供统一的 `focus-visible` 样式。
- 为 Chart 和 RelationGraph 提供摘要或表格替代内容。
- 发送错误应使用合适的 Live Region，避免仅靠颜色表达。
- 流式内容要避免每个 Token 都被读屏重复播报，可在完成或分段时宣布。
- 触屏环境不能依赖 Hover 才能发现关键操作。
- 验证品牌蓝按钮、浅色标签与小字号文本的 WCAG 对比度。

## 11. 前端代码结构

| 文件 | 当前职责 | 设计层级 |
|---|---|---|
| `frontend/src/App.vue` | 页面布局、会话状态、SSE 事件归并、消息渲染、输入与日志接线 | 应用壳与业务交互 |
| `frontend/src/style.css` | 全局字体、背景、环境光、滚动条、Typing 动效、Reduced Motion | 全局基础样式 |
| `frontend/tailwind.config.ts` | Brand Token、阴影、字体 | 应用主题 |
| `frontend/src/chat/RunLogPanel.vue` | 单轮执行日志抽屉 | 业务组件 |
| `packages/agent-ui/src/components/UiSurfacePart.vue` | 富结果加载、错误降级、Renderer 解析 | 可移植 UI SDK |
| `packages/agent-ui/src/components/RichMessageView.vue` | 通用 Text/UI Part 交错渲染 | 可移植 UI SDK |
| `packages/agent-ui/src/renderers/*` | Chart、Table、Timeline、RelationGraph | 可插拔 Renderer |
| `packages/agent-ui/src/core/registry.ts` | Renderer 与结果获取函数注册 | SDK 扩展点 |
| `packages/agent-ui/src/core/stream.ts` | 当前私有 SSE 解析 | Transport Adapter |
| `packages/agent-ui/src/core/useConversation.ts` | SDK 内置简化会话状态 | Conversation Adapter |

### 11.1 当前分层评价

值得保留的边界：

- 应用壳决定对话布局和业务操作，SDK 只负责富 Surface。
- Renderer 不自行请求数据，由 `UiSurfacePart` 统一获取并注入。
- Renderer Registry 允许宿主增加或覆盖实现。
- 文本与 UI 使用统一 Part 序列，可自然保持到达顺序。

需要收敛的边界：

- `App.vue` 自己实现了一套比 `useConversation` 更完整的状态机，两者会逐渐产生语义漂移。
- `RichMessageView` 已存在，但主应用仍重复实现 Part 渲染。
- 应用 Token 与 SDK 硬编码颜色分裂。
- SDK Renderer 使用 Tailwind Class，但 `frontend/tailwind.config.ts` 的扫描范围只有 `frontend/src`，没有覆盖 `packages/agent-ui/src`。生产 CSS 可能依赖“应用恰好也使用了相同 Class”，新增 SDK 样式尤其容易缺失。
- Transport、Conversation Reducer 和视觉组件仍有耦合到当前私有 `delta/ui/tool/done` 事件的部分。

## 12. AG-UI 迁移下的设计边界

正式兼容 AG-UI 时，应替换 Transport 和 Reducer，不应重做视觉层。推荐结构：

```text
标准 AG-UI Client / Transport
           ↓
按 runId、messageId、toolCallId、surfaceId 归并的 Reducer
           ↓
应用视图模型（MessagePart / RunStatus / ToolStatus / SurfaceState）
           ↓
App Shell + Message Components + @ac/agent-ui Renderers
```

事件到可见状态的映射建议为：

| AG-UI 语义 | UI 表现 |
|---|---|
| `RUN_STARTED` | 进入运行态、显示全局运行反馈 |
| `TEXT_MESSAGE_START` | 创建 Assistant Message/文本 Part |
| `TEXT_MESSAGE_CONTENT` | 追加到指定 messageId，而非默认最后一条消息 |
| `TEXT_MESSAGE_END` | 结束该消息的流式光标 |
| `TOOL_CALL_START/ARGS` | 增加低权重工具活动及日志项 |
| `TOOL_CALL_END/RESULT` | 更新指定 toolCallId；只显示摘要，不展示完整敏感结果 |
| `CUSTOM ui.surface.create` | 插入或创建指定 surfaceId |
| `CUSTOM ui.surface.update` | 原位更新 Surface，不新增重复卡片 |
| `CUSTOM ui.surface.remove` | 安全卸载 Renderer 并回收图形实例 |
| `RUN_FINISHED` | 退出运行态，开放复制/日志操作 |
| `RUN_ERROR` | 保留已有内容，显示局部错误和可重试入口 |

视觉层不得依赖 Spring AI Alibaba 或 AgentScope 类型。无论后端选择 `agent-client` 还是独立 `agentscope-client`，只要输出相同 AG-UI 事件和 `ac.rich-ui` Profile，前端表现必须一致。

## 13. 扩展新组件的设计规范

新增 Renderer 时必须满足：

1. 放在标准 Surface 容器中，默认不改变消息列宽。
2. 使用统一的主题变量，不直接引入第三套品牌色。
3. 定义 Loading、Empty、Ready、Error、Unsupported 五类状态。
4. 数据量超限时给出明确截断或聚合提示。
5. 重计算和图形初始化让出主线程，不阻塞文本流。
6. 响应容器宽度变化，并在卸载时销毁 Observer、事件和图形实例。
7. 交互由受控 Schema 声明，不执行服务端返回的 HTML/JavaScript/任意图形配置。
8. 提供键盘路径；Canvas/SVG 图形应提供文本摘要或数据表替代。
9. 不在 Renderer 内自行访问结果接口。
10. 未识别的 `subType/options` 降级到安全默认值。

## 14. 当前设计风险与优先级

| 优先级 | 问题 | 影响 | 建议 |
|---|---|---|---|
| P0 | SDK Renderer 的 Tailwind 源码不在 Content 扫描范围 | 发布/新增组件时样式可能缺失 | SDK 输出独立 CSS，或将 Package 源码加入扫描白名单 |
| P0 | AG-UI 迁移前状态按“最后一条回复”隐式归并 | 并发/多消息事件可能错位 | 先建立 ID 驱动的 Reducer |
| P1 | App 与 SDK 两套 Conversation/Part 渲染逻辑 | 交互和状态逐渐漂移 | 定义唯一 View Model 与 Reducer，应用只扩展业务日志 |
| P1 | SDK Indigo 与应用 Brand Blue 不一致 | 富组件像第三方嵌入 | 建立 CSS Variables/Theme Provider |
| P1 | `components[]` 只渲染第一项 | 契约与视觉能力不一致 | 明确单组件限制或实现布局协议 |
| P1 | Canvas 图表缺少替代内容 | 无障碍和导出体验不足 | 提供摘要/数据表切换 |
| P1 | 日志 Modal 缺少焦点管理 | 键盘和读屏体验不完整 | 实现标准 Dialog 行为 |
| P1 | 错误态无重试、运行态无停止 | 弱网和长任务可控性不足 | AG-UI 取消/重试接入后补齐操作 |
| P2 | 操作依赖 Hover | 移动端发现性弱 | 小屏常显或使用更多菜单 |
| P2 | `h-screen` | 移动浏览器高度可能跳变 | 评估 Dynamic Viewport Unit |
| P2 | 文本只做 `pre-wrap` | 复杂分析缺少标题、列表、代码样式 | 引入受控 Markdown Renderer，并做好净化 |
| P2 | 连接状态为静态文案 | 不能反映真实 Runtime 健康 | 接入配置和真实连接状态 |

## 15. 设计验收清单

### 15.1 视觉

- [ ] 1440px 左右桌面宽度下，侧栏与主面板比例稳定，消息列不超过 `768px`。
- [ ] 1024px 断点切换前后没有重叠、横向滚动和操作丢失。
- [ ] 375px/390px 宽度下输入区不被浏览器工具栏遮挡。
- [ ] App 与所有 SDK Renderer 使用同一品牌 Token。
- [ ] Loading 到 Ready 的替换没有明显布局跳动。
- [ ] 长文本、长表格、长日志和长 conversationId 均不会撑破容器。

### 15.2 交互

- [ ] Enter/Shift+Enter 行为符合提示，中文输入法组合输入不会误发送。
- [ ] 运行中可停止，失败后可重试，新建会话能取消旧 Run。
- [ ] 文本、Tool 和 Surface 更新严格归属对应 ID。
- [ ] Drill Down 有明确可点击提示，并将追问作为新 Run 展示。
- [ ] 复制成功有反馈；触屏可发现复制与日志入口。
- [ ] Surface Update/Remove 不产生重复卡片或资源泄漏。

### 15.3 可访问性

- [ ] 所有功能可仅使用键盘完成。
- [ ] Dialog 具备焦点陷阱、Esc 关闭和焦点恢复。
- [ ] Loading、错误、完成状态可由读屏理解。
- [ ] 图表和关系图具有文本摘要或表格替代。
- [ ] Reduced Motion 下无持续闪烁或不必要位移。
- [ ] 关键文字和控件通过 WCAG AA 对比度检查。

### 15.4 跨 Runtime 一致性

- [ ] `agent-client`（Spring AI Alibaba）与 `agentscope-client`（AgentScope v2 Java）使用相同前端构建物。
- [ ] 相同 AG-UI Golden Trace 在两个后端下产生相同消息、工具和 Surface 状态。
- [ ] Runtime 名称只影响标题栏配置，不影响消息和富组件视觉。
- [ ] 未识别的 AG-UI Custom Event 或 Profile 能安全忽略/降级。

## 16. 结论

当前前端已经形成了清晰且适合分析型 Agent 的基础风格：轻玻璃工作台、长文本友好的 Assistant 排版、克制的品牌蓝、内联富可视化以及按需展开的执行日志。其视觉壳可以继续保留。

下一阶段的重点不是重新设计页面，而是把视觉组件建立在稳定的 Design Token 和标准状态模型上：由正式 AG-UI Client/Reducer 管理运行事件，由 `@ac/agent-ui` 保持 Runtime 无关的 Surface 渲染，并用同一主题覆盖 Spring AI Alibaba 与 AgentScope v2 Java 两条后端链路。
