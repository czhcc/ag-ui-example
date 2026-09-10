export type Role = 'user' | 'assistant' | 'system'

export interface SurfaceComponent {
    id: string
    type: string
    props: {
        subType?: string
        title?: string
        description?: string
        encoding?: Record<string, unknown>
        options?: Record<string, unknown>
    }
}

export interface UiSurface {
    surfaceId: string
    dataRef: string
    components: SurfaceComponent[]
}

export type MessagePart =
    | { kind: 'text'; text: string }
    | { kind: 'ui'; surface: UiSurface }

export interface RichMessage {
    id: string
    role: Role
    parts: MessagePart[]
    streaming?: boolean
    failed?: boolean
}

export interface ResultPayload {
    resultRef: string
    serverCode?: string
    toolName?: string
    data: unknown
    summary: { count?: number; description?: string } | null
    expiresAtEpochMs?: number | null
}

export type StreamEvent =
    | { type: 'delta'; conversationId: string; runId: string; content: string }
    | { type: 'done'; conversationId: string; runId: string; content: null }
    | { type: 'error'; conversationId: string; runId: string; content: string }
    | ({ type: 'ui'; conversationId: string; runId: string } & UiSurface)
    | ({ conversationId: string; runId: string } & ToolEvent)

export interface ToolEvent {
    type: 'tool_start' | 'tool_end'
    toolCallId: string
    toolName: string
    arguments: string | null
    success: boolean | null
    resultRef: string | null
    detail: string | null
}

export type RendererProps = {
    component: SurfaceComponent
    result: ResultPayload
}

export type RendererComponent = abstract new (...args: never[]) => unknown

export interface AgentUiOptions {
    fetchResult?: (dataRef: string) => Promise<ResultPayload>
    renderers?: Record<string, RendererComponent>
}
