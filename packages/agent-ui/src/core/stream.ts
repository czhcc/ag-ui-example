import type {AgUiEvent, ResultFetchContext, ResultPayload, ResultUnavailableReason, RunAgentInput} from './types'

export interface AgentStreamOptions {
    url?: string
    fetchImpl?: typeof fetch
    headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>)
    credentials?: RequestCredentials
    lastEventId?: string
    onEventId?: (eventId: string) => void
}

export interface ResultRequestOptions extends AgentStreamOptions {
    resultUrl?: string
}

export class ResultUnavailableError extends Error {
    constructor(public readonly status: number, public readonly reason: ResultUnavailableReason) {
        super(reason === 'forbidden'
            ? 'You do not have access to this result.'
            : reason === 'expired'
                ? 'This result has expired. Run the query again.'
                : reason === 'not-found'
                    ? 'This result is no longer available.'
                    : `Result request failed (${status}).`)
        this.name = 'ResultUnavailableError'
    }
}

const DEFAULT_URL = '/api/agent'
const MAX_SSE_FRAME_CHARS = 1_000_000
const KNOWN_EVENTS = new Set([
    'RUN_STARTED', 'RUN_FINISHED', 'RUN_ERROR',
    'TEXT_MESSAGE_START', 'TEXT_MESSAGE_CONTENT', 'TEXT_MESSAGE_END',
    'TOOL_CALL_START', 'TOOL_CALL_ARGS', 'TOOL_CALL_END', 'TOOL_CALL_RESULT',
    'STATE_SNAPSHOT', 'STATE_DELTA', 'CUSTOM',
    'REASONING_START', 'REASONING_MESSAGE_CHUNK', 'REASONING_MESSAGE_START', 'REASONING_MESSAGE_CONTENT',
    'REASONING_MESSAGE_END', 'REASONING_END',
])

/** Incremental AG-UI SSE parser. One instance belongs to exactly one HTTP response. */
export class AgUiSseParser {
    private buffer = ''

    constructor(
        private readonly onEvent: (event: AgUiEvent) => void,
        private readonly onEventId?: (eventId: string) => void,
    ) {}

    push(chunk: string): void {
        this.buffer += chunk
        this.consume(false)
    }

    finish(): void {
        this.consume(true)
    }

    private consume(flush: boolean): void {
        // Preserve a trailing CR until the next chunk determines whether it is CRLF.
        const pendingCr = !flush && this.buffer.endsWith('\r')
        const source = pendingCr ? this.buffer.slice(0, -1) : this.buffer
        this.buffer = source.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
        const frames = this.buffer.split('\n\n')
        this.buffer = flush ? '' : frames.pop() ?? ''
        for (const frame of frames) consumeFrame(frame, this.onEvent, this.onEventId)
        if (pendingCr) this.buffer += '\r'
        if (!flush && this.buffer.length > MAX_SSE_FRAME_CHARS) {
            throw new Error('AG-UI SSE frame exceeds the client limit')
        }
    }
}

/** POST one official RunAgentInput and consume the official AG-UI SSE event stream. */
export async function streamAgent(
    request: RunAgentInput,
    onEvent: (event: AgUiEvent) => void,
    signal?: AbortSignal,
    options: AgentStreamOptions = {},
): Promise<void> {
    assertRunInput(request)
    const doFetch = options.fetchImpl ?? fetch
    const headers = new Headers(await resolveHeaders(options.headers))
    headers.set('Accept', 'text/event-stream')
    headers.set('Content-Type', 'application/json')
    if (options.lastEventId) headers.set('Last-Event-ID', options.lastEventId)
    const response = await doFetch(options.url ?? DEFAULT_URL, {
        method: 'POST',
        headers,
        credentials: options.credentials ?? 'same-origin',
        body: JSON.stringify(request),
        signal,
    })

    if (!response.ok) {
        const detail = await response.text()
        throw new Error(detail || `Agent request failed (${response.status})`)
    }
    if (!response.body) throw new Error('AG-UI response did not include a streaming body')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    const parser = new AgUiSseParser(onEvent, options.onEventId)

    while (true) {
        const {done, value} = await reader.read()
        if (done) break
        parser.push(decoder.decode(value, {stream: true}))
    }
    parser.push(decoder.decode())
    parser.finish()
}

/** Fetches a stored result with run ownership and browser credentials. */
export async function fetchResult(
    dataRef: string,
    context: ResultFetchContext = {},
    options: ResultRequestOptions = {},
): Promise<ResultPayload> {
    if (!dataRef) throw new Error('dataRef is required')
    const doFetch = options.fetchImpl ?? fetch
    const base = options.resultUrl ?? '/api/results'
    const query = new URLSearchParams()
    if (context.threadId) query.set('threadId', context.threadId)
    if (context.runId) query.set('runId', context.runId)
    const queryString = query.toString()
    const suffix = queryString ? `?${queryString}` : ''
    const response = await doFetch(`${base.replace(/\/$/, '')}/${encodeURIComponent(dataRef)}${suffix}`, {
        method: 'GET',
        headers: await resolveHeaders(options.headers),
        credentials: options.credentials ?? 'same-origin',
        signal: context.signal,
    })
    if (!response.ok) {
        const reason: ResultUnavailableReason = response.status === 403
            ? 'forbidden'
            : response.status === 410
                ? 'expired'
                : response.status === 404 ? 'not-found' : 'http-error'
        throw new ResultUnavailableError(response.status, reason)
    }
    return (await response.json()) as ResultPayload
}

function consumeFrame(
    frame: string,
    onEvent: (event: AgUiEvent) => void,
    onEventId?: (eventId: string) => void,
) {
    if (!frame.trim()) return
    if (frame.length > MAX_SSE_FRAME_CHARS) throw new Error('AG-UI SSE frame exceeds the client limit')
    const lines = frame.split('\n')
    const id = lines.find((line) => line.startsWith('id:'))?.slice(3).trim()
    if (id) onEventId?.(id)
    const data = lines
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trimStart())
        .join('\n')
    if (!data) return
    const parsed: unknown = JSON.parse(data)
    if (!isObject(parsed) || typeof parsed.type !== 'string') {
        throw new Error('AG-UI event must be a JSON object with a type')
    }
    // Forward compatibility: unknown events are ignored instead of corrupting reducer state.
    if (!KNOWN_EVENTS.has(parsed.type)) return
    onEvent(parsed as unknown as AgUiEvent)
}

async function resolveHeaders(
    source?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>),
): Promise<HeadersInit> {
    return typeof source === 'function' ? await source() : source ?? {}
}

function assertRunInput(input: RunAgentInput) {
    if (!input.threadId?.trim() || !input.runId?.trim()) {
        throw new Error('AG-UI threadId and runId are required')
    }
    if (!Array.isArray(input.messages)) throw new Error('AG-UI messages must be an array')
}

function isObject(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
}
