export type Role = 'user' | 'assistant' | 'system' | 'developer' | 'tool' | 'reasoning'

export interface AgUiMessage {
    id: string
    role: Role
    content?: string | null
    name?: string | null
    toolCallId?: string | null
    toolCalls?: Array<{
        id: string
        type: 'function'
        function: {name: string; arguments: string}
    }>
}

export interface AgUiTool {
    name: string
    description?: string
    parameters: Record<string, unknown>
}

export interface AgUiContext {
    description: string
    value: string
}

export interface AgUiResume {
    interruptId: string
    status: 'resolved' | 'cancelled'
    payload?: unknown
}

/** Official AG-UI RunAgentInput wire shape. */
export interface RunAgentInput {
    threadId: string
    runId: string
    parentRunId?: string | null
    state?: unknown
    messages: AgUiMessage[]
    tools?: AgUiTool[]
    context?: AgUiContext[]
    forwardedProps?: Record<string, unknown>
    resume?: AgUiResume[]
}

interface EventBase {
    type: string
    timestamp?: number
    rawEvent?: unknown
}

export type AgUiEvent =
    | (EventBase & {type: 'RUN_STARTED'; threadId: string; runId: string; parentRunId?: string; input?: RunAgentInput})
    | (EventBase & {type: 'RUN_FINISHED'; threadId: string; runId: string; outcome?: RunOutcome; result?: unknown})
    | (EventBase & {type: 'RUN_ERROR'; message: string; code?: string})
    | (EventBase & {type: 'TEXT_MESSAGE_START'; messageId: string; role: Role})
    | (EventBase & {type: 'TEXT_MESSAGE_CONTENT'; messageId: string; delta: string})
    | (EventBase & {type: 'TEXT_MESSAGE_END'; messageId: string})
    | (EventBase & {type: 'TOOL_CALL_START'; toolCallId: string; toolCallName: string; parentMessageId?: string})
    | (EventBase & {type: 'TOOL_CALL_ARGS'; toolCallId: string; delta: string})
    | (EventBase & {type: 'TOOL_CALL_END'; toolCallId: string})
    | (EventBase & {type: 'TOOL_CALL_RESULT'; messageId: string; toolCallId: string; content?: string; role?: string})
    | (EventBase & {type: 'STATE_SNAPSHOT'; snapshot: unknown})
    | (EventBase & {type: 'STATE_DELTA'; delta: JsonPatchOperation[]})
    | (EventBase & {type: 'CUSTOM'; name: string; value?: unknown})
    | (EventBase & {type: 'REASONING_START'; messageId?: string})
    | (EventBase & {type: 'REASONING_MESSAGE_CHUNK'; threadId?: string; runId?: string; messageId?: string; delta?: string})
    | (EventBase & {type: 'REASONING_MESSAGE_START'; messageId: string; role?: string})
    | (EventBase & {type: 'REASONING_MESSAGE_CONTENT'; messageId: string; delta: string})
    | (EventBase & {type: 'REASONING_MESSAGE_END'; messageId: string})
    | (EventBase & {type: 'REASONING_END'; messageId?: string})

export interface RunOutcome {
    type: 'success' | 'interrupt'
    interrupts?: AgUiInterrupt[]
}

export interface AgUiInterrupt {
    id: string
    reason: string
    message?: string
    toolCallId?: string
    responseSchema?: Record<string, unknown>
    expiresAt?: string
    metadata?: Record<string, unknown>
}

export interface JsonPatchOperation {
    op: 'add' | 'replace' | 'remove' | 'copy' | 'move' | 'test'
    path: string
    value?: unknown
    from?: string
}

export interface SurfaceComponent {
    id: string
    type: string
    props: {
        subType?: string
        title?: string
        description?: string
        encoding?: Record<string, unknown>
        options?: Record<string, unknown>
        drillDown?: DrillDownHint
        [key: string]: unknown
    }
}

export type SurfaceValidationCode =
    | 'UNKNOWN_PROFILE'
    | 'UNSUPPORTED_PROFILE_VERSION'
    | 'INVALID_SURFACE'

export interface SurfaceValidationIssue {
    code: SurfaceValidationCode
    message: string
}

export interface UiSurface {
    profile: string
    profileVersion: string
    surfaceId: string
    dataRef: string
    components: SurfaceComponent[]
    threadId?: string
    runId?: string
    validationIssue?: SurfaceValidationIssue
}

export interface DrillDownHint {
    enabled: boolean
    dimension: string
    label?: string
    promptTemplate: string
}

export interface DrillDownEvent {
    question: string
    surfaceId: string
    dimension: string
    value: unknown
}

export type MessagePart =
    | {kind: 'text'; text: string}
    | {kind: 'ui'; surfaceId: string; surface: UiSurface}

export interface RichMessage {
    id: string
    runId: string
    role: Role
    parts: MessagePart[]
    streaming?: boolean
    failed?: boolean
}

export interface SafeToolResult {
    kind?: string
    summary?: string
    resultRef?: string
}

export interface ToolCallState {
    id: string
    runId: string
    name: string
    parentMessageId?: string
    arguments: string
    status: 'running' | 'finished' | 'failed'
    result?: SafeToolResult
}

export interface ReasoningState {
    id: string
    runId: string
    content: string
    streaming: boolean
}

export interface SubagentActivity {
    id: string
    runId: string
    name: string
    value: Record<string, unknown>
    timestamp?: number
}

export interface AgentRunState {
    id: string
    threadId: string
    status: 'idle' | 'running' | 'finished' | 'interrupted' | 'error'
    parentRunId?: string
    messageIds: string[]
    toolCallIds: string[]
    surfaceIds: string[]
    reasoningIds: string[]
    interrupts: AgUiInterrupt[]
    state: unknown
    error?: {code?: string; message: string}
}

export interface AgentUiState {
    runs: Record<string, AgentRunState>
    runOrder: string[]
    messages: Record<string, RichMessage>
    toolCalls: Record<string, ToolCallState>
    surfaces: Record<string, UiSurface>
    reasoning: Record<string, ReasoningState>
    subagents: Record<string, SubagentActivity>
    warnings: string[]
}

export interface ResultPayload {
    resultRef: string
    serverCode?: string
    toolName?: string
    data: unknown
    summary: {count?: number; description?: string} | null
    resultMeta?: {requestId?: string; generatedAt?: string; attributes?: Record<string, unknown>} | null
    expiresAtEpochMs?: number | null
}

export type ResultUnavailableReason = 'forbidden' | 'not-found' | 'expired' | 'http-error'

export interface ResultFetchContext {
    threadId?: string
    runId?: string
    signal?: AbortSignal
}

export type RendererProps = {
    component: SurfaceComponent
    result: ResultPayload
}

export type RendererComponent = abstract new (...args: never[]) => unknown

export interface AgentUiOptions {
    fetchResult?: (dataRef: string, context?: ResultFetchContext) => Promise<ResultPayload>
    resultUrl?: string
    resultHeaders?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>)
    credentials?: RequestCredentials
    renderers?: Record<string, RendererComponent>
}
