# @ac/agent-ui

Rich streaming conversation UI SDK：文字/工具/UI 交错的消息流渲染 + 可插拔渲染器注册表。

任何对话型前端（Vue3）接完 SSE 后即可获得"文字中间插入图表"的能力；统计图类型可由第三方注册扩展，渲染方式可整体替换。

## 快速接入

```ts
// main.ts
import {createApp} from 'vue'
import App from './App.vue'
import {registerBuiltinRenderers} from '@ac/agent-ui/renderers'

registerBuiltinRenderers()   // Chart / Timeline / RelationGraph / Table
createApp(App).mount('#app')
```

```vue
<script setup lang="ts">
import {ref} from 'vue'
import {streamChat} from '@ac/agent-ui'
import UiSurfacePart from '@ac/agent-ui/components/UiSurfacePart.vue'

const messages = ref([])

async function send(text: string) {
    const reply = reactive({id: crypto.randomUUID(), parts: [{kind: 'text', text: ''}], streaming: true})
    messages.value.push(reply)
    await streamChat({conversationId: 'c1', message: text}, (event) => {
        if (event.type === 'delta' && event.content) appendText(reply, event.content)
        else if (event.type === 'ui') reply.parts.push({kind: 'ui', surface: stripMeta(event)})
        else if (event.type === 'done') reply.streaming = false
    })
}
</script>

<template>
  <div v-for="m in messages" :key="m.id">
    <template v-for="(part, i) in m.parts" :key="i">
      <span v-if="part.kind === 'text'">{{ part.text }}</span>
      <UiSurfacePart v-else :surface="part.surface"/>
    </template>
  </div>
</template>
```

## 第三方注册新统计图类型

Renderer 是任意接收 `{component, result}` props 的 Vue 组件。`component.props.encoding` 是
ViewHint 的字段映射，`result.data` 是行式数据。

```ts
import {registerRenderer} from '@ac/agent-ui'
import HeatmapRenderer from './HeatmapRenderer.vue'

registerRenderer('Heatmap', HeatmapRenderer)
```

后端 ViewHint 返回 `type: "heatmap"`（UiComponentType 大写驼峰会由 PresentationMapper 转换）即可触发。

## 第三方替换渲染方式

同名注册即覆盖内置实现：

```ts
registerRenderer('Chart', MyCustomChartRenderer)   // 替换 ECharts 实现
```

先 `unregisterRenderer('Chart')` 再注册亦可。`listRenderers()` 可枚举当前注册表。

## 替换数据端点

默认 `GET /api/results/{dataRef}`。私有部署/网关不同时：

```ts
import {setFetchResult} from '@ac/agent-ui'

setFetchResult(async (ref) => {
    const res = await fetch(`https://my-host/v1/results/${ref}`)
    if (!res.ok) throw new Error(`gone (${res.status})`)
    return res.json()
})
```

## 一站式配置

```ts
configure({
    fetchResult,
    renderers: {Chart, Timeline},
})
```

## 自带但可选的完整状态管理

不想自己维护 parts 逻辑时用 `useConversation`：

```ts
const {messages, send} = useConversation({send: streamChat})
```

## Renderer 契约

```ts
interface RendererProps {
    component: SurfaceComponent   // {id, type, props: {subType, title, encoding, options}}
    result: ResultPayload         // {data: unknown, summary, ...}
}
```

- 从 `result.data`（推荐 `Array<Object>`）按 `encoding` 取数
- subType 等枚举做白名单校验，未知值降级
- 重图形库初始化建议让出主线程（双 requestAnimationFrame），保证文字流不卡顿
- 容器初始宽度可能未稳定：ResizeObserver + resize 兜底

## SSE 事件协议

| 事件 | 说明 |
|---|---|
| `delta` | 文字增量 |
| `ui` | SurfaceSpec `{surfaceId, dataRef, components}` |
| `tool` | tool_start / tool_end（参数/结果 JSON） |
| `done` / `error` | 收尾 |

数据契约见 `mcp-contract/src/main/resources/schema/mcp-result.schema.json`。
