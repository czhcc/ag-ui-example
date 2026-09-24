# MCP Rich Result 契约

本文是 `mcp-contract` 的生产者、消费者和 Runtime Adapter 共同遵守的规范。
规范文件为：

- `schema/mcp-result.schema.json`：MCP Rich Result envelope。
- `schema/ac-rich-ui-profile.schema.json`：AG-UI `CUSTOM ui.surface.*` Profile。

## 版本和枚举

- 当前生产版本是 `specVersion=1.1`。
- 消费者在迁移期接受 `1.0` 和 `1.1`；生产者只能生成 `1.1`。
- 未知版本必须拒绝为 `UNSUPPORTED_SPEC_VERSION`，不得猜测字段语义。
- `1.0` 输入先通过兼容 Schema，再由适配层归一化成 `1.1` 内部模型。
- `presentation.mode` 在线路上只允许大写 `NONE`、`RECOMMENDED`、`REQUIRED`。
- View `type`、`subType` 和 options 中的排序值使用 Schema 规定的小写值。
- `1.1` 的不兼容收紧包括：空值改为省略、成功/失败条件约束、强类型 mapping、subType 枚举和 options 白名单。

## Envelope 不变量

`1.1` 总是包含 `specVersion`、`success`、`data` 和 `presentation`。

- `success=true`：必须有 `data` 和 `presentation`，且不得出现 `error`。
- `success=false`：必须有 `error`，`data` 必须为 `null`。
- 可选对象 `summary`、`resultMeta`、`error` 不使用 `null` 占位；不适用时省略。
- `mode=NONE` 时 `views` 必须为空。
- `mode=RECOMMENDED` 或 `mode=REQUIRED` 时至少包含一个 view。
- MCP 协议、传输或工具执行失败使用 `CallToolResult.isError=true`，不能伪装成业务失败。

## View 映射和选项

| View | subType | mapping | options 白名单 |
|---|---|---|---|
| `chart` | `bar/line/area/pie/scatter` | `category`、`value` | `orientation/order/limit/showLegend/showLabels` |
| `table` | 缺省或 `standard` | `columns[{field,label}]`，可用 `{}` 自动推导 | `limit/sort/order` |
| `timeline` | `vertical` | `time/title/description?/group?` | `limit/order/group` |
| `relation_graph` | `network` | `source/target/label?/sourceLabel?` | `limit/showLabels` |

所有 mapping 引用的是 `data` 行字段名。mapping 和 options 都禁止额外属性，
不得携带 HTML、JavaScript、ECharts option、G6 配置或可执行表达式。

## `ac.rich-ui` Profile

Profile 名称固定为 `ac.rich-ui`，当前 `profileVersion` 为 `1.0`。支持：

- `CUSTOM ui.surface.create`：创建 surface，必须包含 `dataRef` 和非空 components。
- `CUSTOM ui.surface.update`：按 `surfaceId` 更新，至少提供 `dataRef` 或 components。
- `CUSTOM ui.surface.remove`：按 `surfaceId` 删除。

同一 thread 内 `surfaceId` 必须唯一，create 必须先于 update/remove。`dataRef` 只能由
当前认证用户在所属 thread 中读取。未知 Profile、未知版本或未知组件必须安全降级为
文字提示，不能尝试动态执行 payload。

## 第三方非 Rich Tool 降级

按以下顺序判定，前面的规则优先：

| 输入 | 处理 |
|---|---|
| MCP `isError=true` | 产生协议/执行错误；不进入 Rich Result 和 Presentation 流程 |
| 已配置的 `serverCode/toolName` Adapter | Adapter 转为 `1.1` Rich Result，再执行 Schema 校验 |
| structuredContent 同时声明 `specVersion` 和 `success` | 视为主动声明 Rich Result；校验失败必须报告契约错误，不能静默降级绕过校验 |
| 未声明 Rich Result 的 structuredContent | 作为普通结构化结果；生成受限摘要，不自动生成 UI |
| 仅有 text content | 作为普通文本 Tool Result 返回 Agent |
| 内容超过阈值 | 原始内容存入受授权存储，只返回截断摘要和 opaque resultRef |
| 无任何可用 content | 产生 `EMPTY_TOOL_RESULT` 执行错误 |

普通结果不得伪造 `PresentationHint`。无法识别并不导致整个 Agent run 失败；只有
`isError`、主动声明但非法的 Rich Result、空结果等明确错误按错误路径处理。

## 测试样例

`mcp-contract/src/test/resources/schema/manifest.json` 列出了合法、非法和 `1.0`
兼容样例及预期结果。契约验证器和两个 Runtime Adapter 必须消费同一组样例。
