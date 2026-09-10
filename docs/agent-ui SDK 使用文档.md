# @ac/agent-ui 前端组件化 SDK 使用文档

> 对应代码：`packages/agent-ui/`（pnpm workspace 包）
> 适用场景：任何需要"对话流中内嵌可视化组件"的 Vue3 前端

---

## 1. SDK 是什么

把 ai-mcp-platform 前端的富可视化流式机制沉淀为独立组件库：

- **消息模型**：`Message.parts[]`，TextPart 与 UiSurfacePart 交错 → 图表出现在文字中间
- **事件协议**：消费 AG-UI 风格 SSE 流（delta / ui / tool / done / error）
- **渲染器注册表**：内置 Chart / Timeline / RelationGraph / Table 四类，支持第三方注册新类型、覆盖替换渲染实现
- **数据端点可配置**：默认 `GET /api/results/{dataRef}`，可替换为任意网关

## 2. 快速接入（五步）

### 2.1 安装

workspace 内：

```bash
pnpm --filter your-app add '@ac/agent-ui@workspace:*'
```

独立发布后：

```bash
pnpm add @ac/agent-ui
```

### 2.2 注册内置渲染器

```ts
// main.ts
import {createApp} from 'vue'
import App from './App.vue'
import {registerBuiltinRenderers} from '@ac/agent-ui/renderers'

registerBuiltinRenderers()
createApp(App).mount('#app')
```

### 2.3 发起会话并渲染

```vue
<script setup lang="ts">
import {reactive, ref} from 'vue'
import {streamChat} from '@ac/agent-ui'
import type {RichMessage, UiSurface} from '@ac/agent-ui'
import UiSurfacePart from '@ac/agent-ui/components/UiSurfacePart.vue'

const messages = ref<RichMessage[]>([])

function appendText(message: RichMessage, text: string) {
    const last = message.parts[message.parts.length - 1]
    if (last && last.kind === 'text') last.text += text
    else message.parts.push({kind: 'text', text})
}

async function send(text: string) {
    const reply = reactive<RichMessage>({
        id: crypto.randomUUID(), role: 'assistant',
        parts: [{kind: 'text', text: ''}], streaming: true,
    })
    messages.value.push(reply)
    await streamChat({conversationId: 'c1', message: text}, (event) => {
        if (event.type === 'delta' && event.content) {
            appendText(reply, event.content)
        } else if (event.type === 'ui') {
            const {type: _t, conversationId: _c, runId: _r, ...surface} = event
            reply.parts.push({kind: 'ui', surface: surface as UiSurface})
        } else if (event.type === 'done') {
            reply.streaming = false
        } else if (event.type === 'error') {
            reply.streaming = false
            reply.failed = true
        }
    })
}
</script>

<template>
  <div v-for="message in messages" :key="message.id">
    <template v-for="(part, index) in message.parts" :key="index">
      <div v-if="part.kind === 'text'">{{ part.text }}</div>
      <UiSurfacePart v-else :surface="part.surface"/>
    </template>
  </div>
</template>
```

### 2.4 最简方案：useConversation

不想自己维护 parts 逻辑时：

```ts
import {useConversation, streamChat} from '@ac/agent-ui'

const {conversationId, messages, send} = useConversation({send: streamChat})
```

配合 `RichMessageView` 组件一行渲染整条消息（含流式光标）：

```vue
<RichMessageView v-for="m in messages" :key="m.id" :message="m"/>
```

### 2.5 自定义后端地址

```ts
import {streamChat} from '@ac/agent-ui'

await streamChat(req, onEvent, signal, {url: 'https://my-host/v1/chat/messages'})
```

## 3. 第三方扩展渲染器

### 3.1 Renderer 契约

任意接收以下 props 的 Vue 组件：

```ts
interface RendererProps {
    component: SurfaceComponent
    // {id, type, props: {subType?, title?, description?, encoding?, options?}}
    result: ResultPayload
    // {resultRef, data: unknown, summary, expiresAtEpochMs}
}
```

从 `result.data`（推荐 `Array<Object>` 行式数据）按 `component.props.encoding` 字段映射取数渲染。

### 3.2 注册新统计图类型

```ts
// HeatmapRenderer.vue
<script setup lang="ts">
import type {RendererProps} from '@ac/agent-ui'
const props = defineProps<RendererProps>()
// encoding: {x: 'month', y: 'city', value: 'count'}
</script>
```

```ts
import {registerRenderer} from '@ac/agent-ui'
import HeatmapRenderer from './HeatmapRenderer.vue'

registerRenderer('Heatmap', HeatmapRenderer)
```

后端 ViewHint 配套返回（详见 mcp-result.schema.json）：

```json
{
  "id": "city-heatmap",
  "type": "heatmap",
  "mapping": {"x": "month", "y": "city", "value": "count"}
}
```

> 类型匹配链：后端 ViewHint `type: "heatmap"` → agent-client `PresentationMapper` 映射为
> `UiComponentType`（新类型需在后端枚举中登记，大写驼峰）→ SSE components[].type →
> 前端注册表键名。

### 3.3 替换内置渲染实现

同名注册即覆盖：

```ts
registerRenderer('Chart', MyChartRenderer)      // 替换 ECharts 实现
registerRenderer('RelationGraph', MyGraphRenderer)  // 替换 G6 实现
```

管理 API：

```ts
unregisterRenderer('Chart')   // 移除
listRenderers()               // 枚举 ['Chart', 'Timeline', ...]
```

### 3.4 替换数据端点

```ts
import {setFetchResult} from '@ac/agent-ui'

setFetchResult(async (dataRef) => {
    const res = await fetch(`https://my-gateway/v1/results/${dataRef}`)
    if (!res.ok) throw new Error(`expired (${res.status})`)
    return res.json()
})
```

### 3.5 一站式配置

```ts
import {configure} from '@ac/agent-ui'

configure({
    fetchResult,
    renderers: {Heatmap: HeatmapRenderer, Chart: MyChartRenderer},
})
```

## 4. Renderer 开发规范（重要）

1. **encoding 白名单**：subType 等枚举值必须白名单校验，未知值降级到安全默认，不要透传给图形库
2. **让出主线程**：重图形库初始化（ECharts/G6）建议双 `requestAnimationFrame` 延迟，保证文字流不卡顿（参考内置 ChartRenderer）
3. **容器宽度兜底**：宿主布局可能晚于组件挂载稳定，用 `ResizeObserver` + `chart.resize()` 兜底；只设 `barMaxWidth` 类上限，不设下限（窄容器下强制最小宽度会导致重叠）
4. **卸载清理**：dispose 图形实例、断开 observer、移除 window 监听
5. **不做网络请求**：数据由 UiSurfacePart 统一拉取注入，Renderer 保持纯渲染
6. **禁止执行代码**：不 eval、不 innerHTML 拼接、不渲染 encoding/options 之外的任意配置（安全白名单原则，见方案 §55）

## 5. SSE 事件协议参考

| 事件名 | 载荷 | 说明 |
|---|---|---|
| `delta` | `{conversationId, runId, content}` | 文字增量（TEXT_MESSAGE_CONTENT） |
| `ui` | `{conversationId, runId, surfaceId, dataRef, components[]}` | SurfaceSpec（ui.surface.create） |
| `tool` | `{type: tool_start/tool_end, toolCallId, toolName, arguments, success, resultRef, detail}` | 工具调用过程（执行日志用） |
| `done` | — | 本轮结束 |
| `error` | `{content}` | 降级提示 |

内置 ViewHint 类型与 mapping 必填字段：

| type | mapping 必填 | 内置实现 |
|---|---|---|
| chart | category, value | ECharts（bar/line/area/pie/scatter） |
| timeline | time, title | Vue 垂直时间线 |
| relation_graph | source, target | AntV G6 |
| table | columns（可省略自动推导） | Vue 表格 |

完整数据契约：`mcp-contract/src/main/resources/schema/mcp-result.schema.json`

## 6. 参考实现

本仓库 `frontend/` 即完整消费示例：

- `frontend/src/main.ts`：注册内置渲染器
- `frontend/src/App.vue`：自有气泡样式 + SDK 的 streamChat/UiSurfacePart + 业务日志面板
- `packages/agent-ui/README.md`：API 速查

## 7. 已知边界

- `useConversation` 不含执行日志/错误横幅等业务 UI，需要时参照 frontend/App.vue 自行扩展
- 后端新增 ViewHint type 时，需同步在 agent-client `UiComponentType` 枚举与 `PresentationMapper` 登记映射
- MCP server 重启后 agent-client 缓存 session 会失效（需重启 agent-client），SDK 侧表现为 error 事件
