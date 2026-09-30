# LLM + MCP 富可视化流式响应技术方案

## 1. 背景

系统采用基于 LLM 的智能体进行自然语言交互，业务数据主要通过 MCP Tool 获取。

当前典型调用链路如下：

```text
用户
  ↓
LLM / Agent
  ↓
MCP Tool
  ↓
JSON 数据
  ↓
LLM
  ↓
文本回复
  ↓
SSE
  ↓
浏览器
```

MCP Tool 当前通常返回 JSON 对象或者 JSON 数组，例如：

```json
[
  {
    "sourceId": "P001",
    "sourceName": "张三",
    "targetId": "P002",
    "targetName": "李四",
    "relation": "同事"
  },
  {
    "sourceId": "P001",
    "sourceName": "张三",
    "targetId": "P003",
    "targetName": "王五",
    "relation": "同行"
  }
]
```

LLM 获取结果后，通常直接组织为自然语言回复。

对于简单事实查询，这种模式足够。

但对于以下类型的数据，仅使用文字表达会明显降低分析效果：

- 实体关系网络
- 时间序列数据
- 活动时间线
- 分类统计
- 趋势统计
- 多指标分析
- 行为轨迹
- 资金流
- 事件链
- 证据链
- 复杂表格
- 多实体关联分析

系统希望最终实现：

```text
文字说明

[统计图]

继续分析说明

[关系图]

进一步解释

[时间线]

最终总结
```

并且整个过程仍保持浏览器端的流式响应体验。

---

# 2. 设计目标

本方案主要解决以下问题。

## 2.1 支持文字和 UI 组件混合流式输出

Assistant 回复不能再简单建模为：

```text
content: string
```

而应建模为：

```text
Message
  └── Parts / Blocks
```

一条 Assistant 消息可以动态包含：

```text
Text
Chart
RelationGraph
Timeline
Table
Metric
EntityCard
CustomView
```

并且这些 Block 不需要在回答开始时全部确定，而是在 Agent 执行过程中动态生成。

---

## 2.2 MCP 不应全部采用同一种 UI 机制

不同 MCP Tool 的性质不同。

需要区分两类 MCP Tool：

```text
Data Tool
App Tool
```

### Data Tool

主要负责：

> 返回业务数据。

例如：

```text
kg.search_entity
kg.get_entity_core
kg.list_activity
kg.list_accounts
kg.expand_relations
```

这类工具返回的数据可能存在多种表达方式。

例如 `kg.list_activity` 的结果可能用于：

- 回答最后一次活动
- 生成城市统计
- 展示行为时间线
- 分析同行关系
- 展示趋势图

因此 Data Tool 不应该强制绑定某一种 UI。

---

### App Tool

主要负责：

> 返回一种具有明确业务含义、天然对应某种 UI 的结果。

例如：

```text
kg.relationship_network
kg.activity_timeline_analysis
kg.fund_flow_graph
kg.evidence_chain
```

这类 Tool 的结果天然适合：

```text
Relation Graph
Timeline
Fund Flow Graph
Evidence Chain
```

可以考虑直接采用 MCP Apps。

---

# 3. 核心架构原则

总体架构采用：

```text
MCP Data Tool / MCP App Tool
        +
Business Skill
        +
LLM Runtime Decision
        +
A2UI-like Presentation Spec
        +
AG-UI-compatible Event Stream
        +
Vue Renderer
```

整体关系如下：

```text
                         User
                          │
                          ▼
                   LLM / Agent
                          │
                 Business Skill
                          │
                ┌─────────┴─────────┐
                │                   │
                ▼                   ▼
          MCP Data Tool         MCP App Tool
                │                   │
                │                   │
       structuredContent        MCP App UI
                │                   │
                ▼                   │
           Agent Runtime            │
                │                   │
       Presentation Decision        │
                │                   │
                ▼                   │
          UI Presentation           │
                │                   │
                └─────────┬─────────┘
                          │
                          ▼
                AG-UI Compatible Events
                          │
                         SSE
                          │
                          ▼
                         Vue3
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
       Text            Renderer         MCP App
                       Registry          iframe
                         │
          ┌──────────────┼───────────────┐
          ▼              ▼               ▼
       ECharts         AntV G6       Custom Vue
```

---

# 4. 职责边界

建议严格规定各层职责。

## 4.1 MCP

MCP 负责：

```text
我能提供什么业务能力？
我返回的数据语义是什么？
这个 Tool 是否天然拥有专属 UI？
```

MCP 不负责：

```text
整个 Assistant 回答如何组织
LLM 最终说什么
聊天窗口中图放在哪里
页面最终使用哪种前端技术
```

---

## 4.2 Skill

Skill 负责：

```text
这类问题应该怎样分析？
需要哪些业务维度？
应该调用哪些 MCP？
多个 MCP 结果怎样组合？
什么情况下需要继续深入分析？
```

例如：

```text
activity-analysis
relationship-analysis
communication-analysis
fund-flow-analysis
risk-analysis
person-profile-analysis
```

Skill 不负责：

```text
看到任何 JSON 都猜应该画什么图
生成 ECharts 配置
生成 G6 配置
操作前端组件
```

---

## 4.3 LLM

LLM 负责：

```text
理解用户当前意图
决定下一步调用什么能力
结合 MCP 结果判断是否需要可视化
选择合适的推荐视图
组织文字解释
决定文字和 UI 的相对顺序
```

---

## 4.4 Presentation Runtime

负责：

```text
PresentationSpec
      ↓
UI Block / UI Event
```

同时负责：

- 数据引用解析
- 数据转换
- Mapping 校验
- Renderer 路由
- Schema 校验
- 安全检查

---

## 4.5 Vue Renderer

Vue 只负责：

> 最终具体怎样显示。

例如：

```text
chart
  → ECharts

relation_graph
  → AntV G6

timeline
  → Vue Timeline / ECharts Custom

table
  → Vue Table

entity_card
  → Vue Component
```

---

# 5. 两类 MCP Tool

## 5.1 MCP Data Tool

Data Tool 是最基础、数量最多的一类 MCP Tool。

例如：

```text
kg.search_entity
kg.get_entity_core
kg.list_activity
kg.list_accounts
kg.expand_relations
kg.get_evidence
```

返回：

```text
structuredContent
```

内容以业务数据为主。

Data Tool 默认：

```text
不绑定 UI Resource
```

因为：

> 数据怎么展示，取决于当前用户问题。

---

## 5.2 MCP App Tool

当一个 Tool 的输出天然属于一个业务 UI 时，可以使用 MCP Apps。

例如：

```text
kg.relationship_network
```

天然就是：

```text
RelationGraph
```

或者：

```text
kg.fund_flow_analysis
```

天然就是：

```text
FundFlowGraph
```

这种 Tool 可以声明对应：

```text
ui://...
```

由 MCP Host 加载专属 UI。

---

# 6. MCP Data Tool 与 App Tool 的选择原则

满足以下情况优先使用 Data Tool：

```text
同一结果存在多种可能的表达方式

结果可能只需要文字回答

结果经常作为其他分析的中间数据

Tool 主要职责是查询

Tool 是基础能力
```

满足以下情况可以考虑 App Tool：

```text
Tool 输出天然对应一种固定业务视图

UI 本身就是 Tool 能力的一部分

结果高度交互

结果需要复杂专属 UI

Tool 不依赖聊天上下文决定展示形式
```

---

# 7. 推荐设计：Data Tool + Render Capability 分离

对于大多数通用 MCP，推荐：

```text
Data Tool
      ↓
structuredContent
      ↓
LLM / Agent
      ↓
判断是否可视化
      ↓
Presentation Request
```

例如：

```text
kg.expand_relations
      ↓
关系数据
      ↓
LLM判断当前关系较复杂
      ↓
请求 relation_graph
```

而不是：

```text
每调用一次 kg.expand_relations
都自动弹一个关系图
```

---

# 8. MCP 通用返回规范

Data Tool 建议统一返回：

```text
McpResult<T>
```

采用 Object Envelope，而不是直接裸数组。

基础结构：

```json
{
  "specVersion": "1.1",
  "success": true,
  "data": {},
  "summary": {},
  "presentation": {},
  "resultMeta": {}
}
```

---

# 9. McpResult<T>

## 9.1 specVersion

```json
{
  "specVersion": "1.1"
}
```

表示平台自身定义的 MCP Business Result Schema Version。

它与 MCP Protocol Version 无关。

---

## 9.2 success

```json
{
  "success": true
}
```

表示业务操作是否成功。

支持：

```text
true
false
```

注意：

业务失败与 MCP Transport / Protocol Error 应区分。

---

# 10. data

`data` 表示真实业务数据。

例如：

```json
{
  "data": [
    {
      "city": "北京",
      "count": 18
    },
    {
      "city": "上海",
      "count": 11
    }
  ]
}
```

可以为：

```text
object
array
string
number
boolean
null
```

业务查询 Tool 推荐优先：

```text
Object
Array<Object>
```

---

# 11. summary

`summary` 用于告诉 Agent：

> 当前返回结果大致是什么。

示例：

```json
{
  "summary": {
    "count": 3,
    "description": "查询到3个主要活动城市",
    "truncated": false
  }
}
```

推荐字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| count | integer | 当前返回数量 |
| total | integer | 完整数量 |
| truncated | boolean | 是否截断 |
| description | string | 简单结果描述 |
| highlights | array | MCP 确定性计算出的重要事实 |

例如：

```json
{
  "summary": {
    "count": 100,
    "total": 583,
    "truncated": true,
    "description": "共查询到583条活动记录，当前返回前100条",
    "highlights": [
      "北京活动次数最多"
    ]
  }
}
```

---

# 12. presentation 的新定位

上一版方案中：

```text
presentation
```

承担了比较重的 UI Contract 作用。

调整后的方案中，`presentation` 应定位为：

> Data Tool 对数据表达方式提供的轻量建议。

它不是：

```text
完整 UI 描述协议
```

更不是：

```text
MCP 强制 UI 指令
```

基本结构：

```json
{
  "presentation": {
    "mode": "RECOMMENDED",
    "views": [ViewHint]
  }
}
```

---

# 13. Presentation Mode

支持：

```text
NONE
RECOMMENDED
REQUIRED
```

## NONE

```json
{
  "presentation": {
    "mode": "NONE",
    "views": []
  }
}
```

表示没有特殊展示建议。

---

## RECOMMENDED

```json
{
  "presentation": {
    "mode": "RECOMMENDED",
    "views": [ViewHint]
  }
}
```

表示：

> 如果当前问题需要展示完整数据，推荐使用指定 View。

最终是否采用仍由 LLM 决定。

这是最常见模式。

---

## REQUIRED

```json
{
  "presentation": {
    "mode": "REQUIRED",
    "views": [ViewHint]
  }
}
```

仅用于：

```text
不使用专属 UI 很难正确表达结果
```

普通：

```text
bar
line
table
relation_graph
timeline
```

原则上不要使用 `required`。

---

# 14. ViewHint

Data Tool 中的 View 不应设计成完整 UI。

而是：

```text
ViewHint
```

例如：

```json
{
  "id": "city-stat",
  "type": "chart",
  "subType": "bar",
  "title": "城市活动次数",
  "mapping": {
    "category": "city",
    "value": "count"
  }
}
```

它表达：

> 这批数据天然适合怎样映射。

而不是：

> 前端具体如何绘制。

---

# 15. 第一版 ViewHint 类型

建议支持：

```text
table
metric
entity_card
chart
relation_graph
timeline
```

后续：

```text
tree
sankey
heatmap
map
trajectory
relationship_path
evidence_chain
fund_flow
process
custom
```

---

# 16. Chart ViewHint

```json
{
  "id": "city-stat",
  "type": "chart",
  "subType": "bar",
  "title": "城市活动次数",
  "mapping": {
    "category": "city",
    "value": "count"
  }
}
```

建议第一版支持：

```text
bar
line
area
pie
scatter
```

---

# 17. RelationGraph ViewHint

```json
{
  "id": "person-relation",
  "type": "relation_graph",
  "subType": "network",
  "title": "人员关系",
  "mapping": {
    "source": "sourceId",
    "target": "targetId",
    "label": "relationName",
    "sourceLabel": "sourceName"
  }
}
```

这里只表达：

```text
数据字段和图语义的对应关系
```

而不包含：

```text
G6 Layout
颜色
节点大小
Canvas
WebGL
动画
```

---

# 18. Timeline ViewHint

```json
{
  "id": "activity-timeline",
  "type": "timeline",
  "subType": "vertical",
  "title": "活动时间线",
  "mapping": {
    "time": "eventTime",
    "title": "eventName",
    "description": "description",
    "group": "eventType"
  }
}
```

---

# 19. Table ViewHint

```json
{
  "id": "activity-list",
  "type": "table",
  "subType": "standard",
  "title": "活动记录",
  "mapping": {
    "columns": [
      {
        "field": "eventTime",
        "label": "时间"
      },
      {
        "field": "city",
        "label": "城市"
      },
      {
        "field": "companion",
        "label": "同行人"
      }
    ]
  }
}
```

---

# 20. 完整 Data Tool 返回示例

例如：

```text
kg.activity_statistics
```

返回：

```json
{
  "specVersion": "1.1",

  "success": true,

  "data": [
    {
      "city": "北京",
      "count": 18
    },
    {
      "city": "上海",
      "count": 11
    },
    {
      "city": "杭州",
      "count": 6
    }
  ],

  "summary": {
    "count": 3,
    "description": "共涉及3个主要活动城市"
  },

  "presentation": {
    "mode": "RECOMMENDED",

    "views": [
      {
        "id": "city-stat",
        "type": "chart",
        "subType": "bar",
        "title": "各城市活动次数",

        "mapping": {
          "category": "city",
          "value": "count"
        }
      }
    ]
  },

  "resultMeta": {
    "partial": false
  },

  "error": null
}
```

---

# 21. structuredContent / content / _meta 分工

推荐：

```text
content
structuredContent
_meta
```

分别承担不同职责。

## content

给 LLM 的简短描述。

例如：

```text
查询到3个主要活动城市，北京18次、上海11次、杭州6次。
```

---

## structuredContent

包含完整：

```text
McpResult<T>
```

例如：

```text
data
summary
presentation
```

---

## _meta

保存不需要模型理解的 Runtime Metadata：

```text
traceId
resultRef
cacheKey
executionTime
serverId
debug
```

例如：

```json
{
  "_meta": {
    "resultRef": "result_17",
    "traceId": "trace_xxx"
  }
}
```

---

# 22. ResultStore

推荐 Agent Host 增加：

```text
ResultStore
```

MCP Result 返回以后：

```text
MCP Result
    ↓
ResultStore
    ↓
result_17
```

不要让 LLM 在 UI 调用中重新复制几百条 JSON。

---

# 23. ResultRef

后续 UI 只引用：

```text
result_17
```

例如：

```json
{
  "resultRef": "result_17",
  "viewId": "city-stat"
}
```

而不是：

```text
MCP 返回500条 JSON
 ↓
LLM重新生成500条 JSON
 ↓
UI Tool
```

这样可以避免：

- Token 浪费
- 数据被模型改写
- ID 丢失
- 数值错误
- Hallucination
- 大数据无法处理

---

# 24. ObservationBuilder

MCP Result 不建议直接完整塞给 LLM。

增加：

```text
ObservationBuilder
```

流程：

```text
MCP Result
     ↓
ResultStore
     ↓
ObservationBuilder
     ↓
LLM Observation
```

例如转换成：

```text
工具 kg.activity_statistics 执行成功。

结果：
北京18次，上海11次，杭州6次。

resultRef:
result_17

该结果提供一个推荐视图：

viewId: city-stat
type: chart
subType: bar
title: 各城市活动次数

如果该图有助于回答当前问题，可以使用对应 Presentation；
如果简单文字已经足够，则不需要为了展示而展示。
```

---

# 25. PresentationSpec 的新定位

如果 Agent 决定：

> 需要生成 UI。

则进入真正的：

```text
PresentationSpec
```

这一层。

PresentationSpec 不再属于某个 MCP Tool 的完整输出，而属于：

> Agent UI Runtime。

推荐将其定位为：

> A2UI-like 的受控子集。

---

# 26. A2UI-like Presentation Model

建议采用三层模型：

```text
Component
Data
Renderer
```

即：

```text
Component Tree
       │
       ├────引用────→ Data Model
       │
       ▼
Renderer Registry
```

不要把 UI、数据、Renderer 全塞到一个 JSON 中。

---

# 27. Presentation Surface

一次生成的 UI 可以定义为：

```json
{
  "surfaceId": "surface_01",
  "components": [],
  "dataRef": "result_17"
}
```

例如：

```json
{
  "surfaceId": "surface_01",

  "dataRef": "result_17",

  "components": [
    {
      "id": "component_01",

      "type": "Chart",

      "props": {
        "subType": "bar",
        "title": "各城市活动次数",

        "encoding": {
          "category": "city",
          "value": "count"
        }
      }
    }
  ]
}
```

---

# 28. Component Catalog

系统维护受控组件目录：

```text
Text
Table
Metric
EntityCard
Chart
RelationGraph
Timeline
```

第二阶段：

```text
Map
Tree
Heatmap
RelationshipPath
EvidenceChain
FundFlow
Trajectory
```

这相当于：

```text
A2UI Component Catalog
```

的受控子集。

---

# 29. Vue Component Registry

Vue 建立：

```text
ComponentRegistry
```

例如：

```text
Chart
    → ChartRenderer.vue

RelationGraph
    → RelationGraphRenderer.vue

Timeline
    → TimelineRenderer.vue

Table
    → TableRenderer.vue

Metric
    → MetricRenderer.vue
```

具体 Renderer：

```text
ChartRenderer
      ↓
ECharts

RelationGraphRenderer
      ↓
AntV G6
```

---

# 30. 禁止生成任意 UI 代码

Presentation 层禁止返回：

```text
HTML
JavaScript
Vue Template
React Component
ECharts Option
G6 Config
eval
function
```

只允许：

```text
受控 Component Catalog
+
受控 Props
+
字段映射
```

---

# 31. UI 决策机制

Agent 获取 MCP Result 后，可以动态决定：

```text
不展示 UI
```

或者：

```text
展示一个或多个 UI
```

例如：

```text
用户：
张三最后一次去了哪里？
```

即使结果包含 Timeline Hint，也可能只返回：

```text
张三最后一次活动记录是在上海。
```

不生成 Timeline。

---

另一种情况：

```text
用户：
分析张三最近三个月的活动规律。
```

MCP 返回：

```text
time
city
companion
count
```

此时可以动态生成：

```text
Chart
Timeline
RelationGraph
```

---

# 32. UI 不是一次规划完成

系统不应要求 LLM 在回答开始时提前确定：

```text
我要输出两个图
三个段落
一个时间线
```

真正执行过程应该是：

```text
LLM
 ↓
Text

 ↓

MCP Tool

 ↓

Observation

 ↓

LLM 动态判断

 ├─继续 Text
 └─生成 UI

 ↓

继续分析

 ↓

再调用 MCP

 ↓

再决定是否生成 UI
```

因此：

> RichMessage 是在 Agent 执行过程中动态生长出来的。

---

# 33. Skill 的新定位

采用 Data Tool / App Tool + Presentation Runtime 后：

Visualization Skill 不再负责：

```text
解析任意 JSON
猜测字段语义
猜测应该用什么图
```

这部分应尽可能由：

```text
MCP ViewHint
+
业务数据 Schema
```

解决。

Skill 主要负责：

```text
分析策略
```

例如：

```text
activity-analysis Skill
```

定义：

```text
分析时间分布
分析地点分布
分析同行关系
分析异常模式
```

但不规定：

```text
一定出柱状图
一定出关系图
```

最终仍由 Agent 根据数据和用户问题决定。

---

# 34. MCP App Tool

对于天然拥有专属 UI 的能力，可以直接采用：

```text
MCP Apps
```

例如：

```text
kg.fund_flow_graph
```

可以对应：

```text
ui://kg/fund-flow.html
```

适合：

```text
复杂资金网络
高度交互关系图
复杂事件链
多步骤分析面板
专属业务 Dashboard
```

---

# 35. MCP App Tool 与普通 Presentation 的区别

普通 Presentation：

```text
Agent
 ↓
选择 Component
 ↓
Vue Renderer
```

MCP App：

```text
MCP Tool
 ↓
ui:// resource
 ↓
MCP App iframe
```

因此：

| 场景 | 推荐 |
|---|---|
| 普通柱状图 | Vue Renderer |
| 普通关系图 | Vue Renderer |
| 时间线 | Vue Renderer |
| 普通表格 | Vue Renderer |
| 复杂资金分析应用 | MCP App |
| 高交互证据链 | MCP App |
| Tool 专属 Dashboard | MCP App |

---

# 36. 流式协议：兼容 AG-UI 思想

浏览器流式协议不建议完全自定义为一套封闭协议。

推荐：

> 参考 AG-UI Event Model。

第一版至少在概念上兼容：

```text
RUN
TEXT_MESSAGE
TOOL_CALL
STATE
ACTIVITY
CUSTOM
```

---

# 37. Text Message Events

文字流：

```text
TEXT_MESSAGE_START
TEXT_MESSAGE_CONTENT
TEXT_MESSAGE_END
```

例如：

```json
{
  "type": "TEXT_MESSAGE_START",
  "messageId": "m1"
}
```

```json
{
  "type": "TEXT_MESSAGE_CONTENT",
  "messageId": "m1",
  "delta": "张三最近三个月的活动主要集中在"
}
```

```json
{
  "type": "TEXT_MESSAGE_CONTENT",
  "messageId": "m1",
  "delta": "北京、上海和杭州。"
}
```

---

# 38. Tool Call Events

可以保留：

```text
TOOL_CALL_START
TOOL_CALL_ARGS
TOOL_CALL_END
TOOL_CALL_RESULT
```

用于浏览器展示：

```text
正在查询活动数据……
```

或者用于 Debug / Agent Execution Panel。

---

# 39. UI Events

对于自定义 UI，可以第一版使用：

```text
CUSTOM
```

例如：

```json
{
  "type": "CUSTOM",
  "name": "ui.surface.create",

  "value": {
    "surfaceId": "surface_01",
    "dataRef": "result_17",

    "components": [
      {
        "id": "chart_01",
        "type": "Chart",

        "props": {
          "subType": "bar",
          "title": "各城市活动次数",

          "encoding": {
            "category": "city",
            "value": "count"
          }
        }
      }
    ]
  }
}
```

---

# 40. UI 增量更新

参考 A2UI 的思想，UI 不需要只能：

```text
一次性完整返回
```

可以支持：

```text
surface.create
surface.update
data.update
surface.remove
```

例如：

```text
先创建图区域
 ↓
显示 loading
 ↓
数据准备完成
 ↓
update
```

---

# 41. 第一版可以简化

第一版实际上只需要：

```text
TEXT_MESSAGE_START
TEXT_MESSAGE_CONTENT
TEXT_MESSAGE_END

TOOL_CALL_START
TOOL_CALL_END

UI_CREATE
UI_UPDATE

RUN_FINISHED
```

即可。

没有必要第一版完整实现全部 AG-UI。

原则是：

> Event Model 尽量兼容 AG-UI，而不是复制全部规范。

---

# 42. 浏览器消息模型

前端不再使用：

```ts
interface Message {
  content: string
}
```

改成：

```ts
interface Message {
  id: string
  role: 'user' | 'assistant'
  parts: MessagePart[]
}
```

例如：

```text
Message
 ├── TextPart
 ├── UIPart
 ├── TextPart
 ├── UIPart
 └── TextPart
```

---

# 43. UI Part

例如：

```ts
interface UiPart {
  type: 'ui'
  surfaceId: string
  status: 'loading' | 'ready' | 'error'
}
```

对应：

```text
Presentation Surface
```

前端通过 Surface Store 获取实际组件配置。

---

# 44. 数据和 UI 分离

推荐：

```text
ResultStore

UI Surface Store
```

分别保存：

```text
数据
UI
```

例如：

```text
ResultStore

result_17
 └── MCP Data
```

```text
SurfaceStore

surface_01
 └── Chart → result_17
```

从而：

```text
UI 不复制数据
UI 只引用 dataRef
```

---

# 45. 数据转换

不要让 LLM 做：

```text
583条活动记录
 ↓
自己统计城市数量
```

大规模：

```text
过滤
聚合
统计
排序
TopN
```

应该尽量由：

```text
数据库
MCP
Backend
```

执行。

例如：

```text
kg.activity_statistics
```

直接返回：

```json
[
  {
    "city": "北京",
    "count": 18
  }
]
```

而不是让 UI 从 100000 条活动记录自己统计。

---

# 46. Presentation Transform

只有轻量转换可以在 Presentation Runtime 完成，例如：

```text
字段映射
排序
TopN
格式化
单位转换
简单分组
```

大量数据处理必须下沉 MCP / Backend。

---

# 47. Agent 基础规则

建议 Agent System Instruction 增加：

```text
MCP 工具可能返回 presentation 建议。

presentation 是数据提供方给出的推荐表达方式，不代表必须使用。

根据用户当前问题判断 UI 是否真正有助于理解。

如果简单文字已经能够充分回答，则无需为了展示而生成 UI。

当需要生成 UI 时，应引用 MCP resultRef，而不是重新复制完整 MCP 数据。

禁止自行重新计算大量业务数据。

禁止自行生成 HTML、JavaScript、Vue、ECharts 或 G6 配置。

对于具有专属 MCP App UI 的工具，应优先使用工具自身提供的 App UI。
```

---

# 48. 完整执行示例

用户：

```text
分析一下张三最近三个月的活动情况。
```

执行：

```text
User
 ↓
Agent
 ↓
命中 activity-analysis Skill
 ↓
kg.list_activity
 ↓
MCP Result
 ↓
ResultStore → result_01
 ↓
ObservationBuilder
 ↓
Agent
```

Agent发现需要进一步统计：

```text
kg.activity_statistics
```

返回：

```text
北京18
上海11
杭州6
```

并带：

```text
ViewHint:
bar chart
```

Agent判断：

```text
统计图有价值
```

于是：

```text
TEXT_MESSAGE_START

“从整体活动分布来看，北京明显最为集中。”

TEXT_MESSAGE_END
```

然后：

```text
UI_CREATE

Chart
dataRef=result_02
```

浏览器显示：

```text
[城市活动次数柱状图]
```

Agent继续：

```text
TEXT_MESSAGE_START

“除了地点集中之外，活动时间也表现出明显的阶段性。”
```

然后调用：

```text
kg.activity_timeline
```

如果该 Tool 是普通 Data Tool：

```text
Agent
 ↓
创建 Timeline Surface
```

如果它本身是 MCP App Tool：

```text
直接展示 MCP App Timeline
```

最终形成：

```text
文字

[柱状图]

文字

[时间线]

文字

[关系网络]

总结
```

---

# 49. 推荐的第一阶段实现

第一阶段只实现最小闭环。

## MCP

支持：

```text
McpResult<T>

data
summary
presentation
```

---

## ViewHint

只支持：

```text
table
chart
relation_graph
timeline
```

---

## ResultStore

支持：

```text
resultRef
```

---

## Presentation Component

支持：

```text
Table
Chart
RelationGraph
Timeline
```

---

## Stream Event

支持：

```text
TEXT_MESSAGE_START
TEXT_MESSAGE_CONTENT
TEXT_MESSAGE_END

TOOL_CALL_START
TOOL_CALL_END

UI_CREATE
UI_UPDATE

RUN_FINISHED
```

---

## Renderer

支持：

```text
ECharts
AntV G6
Vue Timeline
Vue Table
```

---

# 50. 第二阶段

具体落地范围、契约和模块改动见《[富可视化流式响应第二阶段实施设计](富可视化流式响应第二阶段实施设计.md)》。

增加：

```text
Metric
EntityCard
Tree
Heatmap
RelationshipPath
EvidenceChain
```

增加：

```text
UI Surface Update
Data Model Update
Drill Down
节点展开
图表点击
```

---

# 51. 第三阶段

进一步引入：

```text
MCP Apps
```

用于：

```text
复杂专属分析 UI
资金流分析
证据链
复杂时序应用
交互 Dashboard
```

并支持：

```text
UI → MCP Tool
UI → Agent
UI → Data Query
```

形成真正双向交互。

---

# 52. 第四阶段

根据实际需求评估是否：

```text
正式兼容 AG-UI
```

以及：

```text
正式兼容 A2UI
```

而不是仅采用兼容思想。

如果未来 Agent 平台需要支持：

```text
Spring AI Alibaba
LangGraph
CopilotKit
其他 Agent Runtime
```

则 AG-UI 兼容价值会明显提高。

---

# 53. 推荐模块划分

后端建议增加：

```text
agent-runtime
│
├── result
│   ├── ResultStore
│   ├── McpResult
│   └── ObservationBuilder
│
├── presentation
│   ├── PresentationSpec
│   ├── ComponentSpec
│   ├── PresentationPlanner
│   ├── PresentationValidator
│   └── PresentationRuntime
│
├── streaming
│   ├── AgentEvent
│   ├── TextMessageEvent
│   ├── ToolCallEvent
│   └── UiEvent
│
└── skill
    └── SkillRuntime
```

---

# 54. Vue 前端模块

```text
src/
├── chat/
│   ├── MessageRenderer.vue
│   ├── TextPart.vue
│   └── UiPart.vue
│
├── presentation/
│   ├── ComponentRegistry.ts
│   │
│   ├── ChartRenderer.vue
│   ├── RelationGraphRenderer.vue
│   ├── TimelineRenderer.vue
│   └── TableRenderer.vue
│
├── stores/
│   ├── messageStore.ts
│   ├── surfaceStore.ts
│   └── resultStore.ts
│
└── streaming/
    └── agentEventHandler.ts
```

---

# 55. 安全规范

Presentation Runtime 必须严格白名单。

允许：

```text
component.type

mapping

encoding

display hints

排序

布局语义
```

禁止：

```text
JavaScript

HTML

CSS Code

Vue Template

function

eval

动态 URL Script

ECharts 原始 option

G6 原始 runtime config
```

---

# 56. Presentation Options 白名单

例如允许：

```text
orientation

order

limit

showLegend

showLabels

group

sort
```

禁止：

```text
任意 JavaScript Expression
```

---

# 57. MCP 通用返回错误结构

```json
{
  "specVersion": "1.1",

  "success": false,

  "data": null,

  "presentation": {
    "mode": "NONE",
    "views": []
  },

  "error": {
    "code": "ENTITY_NOT_FOUND",
    "message": "未找到指定人员",
    "retryable": false
  }
}
```

错误字段：

```text
code
message
retryable
details
```

禁止在 details 中返回：

```text
数据库密码
Access Token
内部 StackTrace
内部连接字符串
敏感系统信息
```

---

# 58. 最终标准化职责

建议长期固定下面这套边界：

```text
MCP Data Tool
=
数据能力

MCP App Tool
=
专属业务能力 + 专属 UI

Skill
=
分析方法

LLM
=
当前语义决策

ViewHint
=
数据提供方的表达建议

PresentationSpec
=
Agent 生成的受控 UI 描述

AG-UI-compatible Event
=
Agent 与浏览器之间的事件协议

Vue Renderer
=
最终 UI 实现
```

---

# 59. 最终推荐架构

最终形态：

```text
                         User
                           │
                           ▼
                      LLM Agent
                           │
                    Business Skill
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
        MCP Data Tool              MCP App Tool
              │                         │
        McpResult<T>                MCP Apps
              │                         │
              ▼                         │
         ResultStore                    │
              │                         │
              ▼                         │
      ObservationBuilder                │
              │                         │
              ▼                         │
            Agent                       │
              │                         │
       Presentation Decision            │
              │                         │
       ┌──────┴──────┐                  │
       │             │                  │
       ▼             ▼                  │
     Text      PresentationSpec         │
       │             │                  │
       │      A2UI-like Components      │
       │             │                  │
       └─────────────┼──────────────────┘
                     │
                     ▼
           AG-UI-compatible Events
                     │
                    SSE
                     │
                     ▼
                    Vue3
                     │
          ┌──────────┼────────────┐
          ▼          ▼            ▼
       Markdown   Renderer      MCP App
                   Registry      iframe
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
       ECharts      G6       Custom Vue
```

---

# 60. 核心设计结论

整个方案最终可以归纳为六句话：

```text
MCP Data Tool 知道“数据是什么”。

MCP App Tool 知道“这个能力本身应该怎样交互”。

Skill 知道“这类问题应该怎样分析”。

LLM 决定“当前用户问题到底需要怎样表达”。

PresentationSpec 描述“应该展示什么组件”。

Vue Renderer 决定“这个组件最终怎样画出来”。
```

同时：

```text
AG-UI-compatible Event
```

负责解决：

> Agent 的执行过程怎样实时传递给浏览器。

而：

```text
A2UI-like PresentationSpec
```

负责解决：

> Agent 动态产生的 UI 应该怎样用安全、声明式、与前端技术无关的方式描述。

最终实现：

```text
MCP Tool
 ↓
Observation
 ↓
Text Stream
 ↓
MCP Tool
 ↓
动态 UI
 ↓
继续 Text Stream
 ↓
另一个 MCP Tool
 ↓
另一个 UI
 ↓
最终总结
```

即：

> RichMessage 不是 LLM 在开始回答时提前规划好的一棵静态 UI 树，而是在 Agent 执行过程中，根据用户意图、Skill 分析策略以及 MCP Observation 动态生长出来的对话式分析界面。
