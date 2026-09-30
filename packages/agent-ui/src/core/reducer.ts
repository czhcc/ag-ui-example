import type {
    AgentRunState,
    AgentUiState,
    AgUiEvent,
    JsonPatchOperation,
    MessagePart,
    RichMessage,
    SafeToolResult,
    SurfaceComponent,
    SurfaceValidationIssue,
    UiSurface,
} from './types'

const PROFILE = 'ac.rich-ui'
const PROFILE_VERSION = '1.0'
const PROFILE_VERSION_NEXT = '1.1'
const MAX_TEXT = 256_000
const MAX_REASONING = 16_000
const MAX_TOOL_ARGS = 8_000
const MAX_COMPONENTS = 10

export function createAgentUiState(): AgentUiState {
    return {
        runs: {},
        runOrder: [],
        messages: {},
        toolCalls: {},
        surfaces: {},
        reasoning: {},
        subagents: {},
        warnings: [],
    }
}

export function reduceAgentUiEvent(
    state: AgentUiState,
    event: AgUiEvent,
    expectedRunId?: string,
): AgentUiState {
    const runId = eventRunId(state, event, expectedRunId)
    const run = ensureRun(state, runId, event.type === 'RUN_STARTED' ? event.threadId : 'unknown')

    switch (event.type) {
        case 'RUN_STARTED':
            run.threadId = event.threadId
            run.parentRunId = event.parentRunId
            run.status = 'running'
            return state
        case 'RUN_FINISHED':
            if (event.outcome?.type === 'interrupt') {
                run.status = 'interrupted'
                run.interrupts = event.outcome.interrupts ? [...event.outcome.interrupts] : []
            } else {
                run.status = 'finished'
            }
            finishMessages(state, run)
            return state
        case 'RUN_ERROR':
            run.status = 'error'
            run.error = {code: event.code, message: bounded(event.message, 2_000)}
            finishMessages(state, run, true)
            return state
        case 'TEXT_MESSAGE_START': {
            const message: RichMessage = {
                id: event.messageId,
                runId,
                role: event.role,
                parts: [{kind: 'text', text: ''}],
                streaming: true,
            }
            state.messages[event.messageId] = message
            addOnce(run.messageIds, event.messageId)
            return state
        }
        case 'TEXT_MESSAGE_CONTENT': {
            const message = ensureMessage(state, run, event.messageId)
            appendText(message, bounded(event.delta, MAX_TEXT))
            return state
        }
        case 'TEXT_MESSAGE_END':
            ensureMessage(state, run, event.messageId).streaming = false
            return state
        case 'TOOL_CALL_START':
            state.toolCalls[event.toolCallId] = {
                id: event.toolCallId,
                runId,
                name: bounded(event.toolCallName, 256),
                parentMessageId: event.parentMessageId,
                arguments: '',
                status: 'running',
            }
            addOnce(run.toolCallIds, event.toolCallId)
            return state
        case 'TOOL_CALL_ARGS': {
            const tool = ensureTool(state, run, event.toolCallId)
            tool.arguments = bounded(tool.arguments + event.delta, MAX_TOOL_ARGS)
            return state
        }
        case 'TOOL_CALL_END':
            ensureTool(state, run, event.toolCallId).status = 'finished'
            return state
        case 'TOOL_CALL_RESULT': {
            const tool = ensureTool(state, run, event.toolCallId)
            tool.result = safeToolResult(event.content)
            tool.status = tool.result.kind?.includes('ERROR') ? 'failed' : 'finished'
            return state
        }
        case 'STATE_SNAPSHOT':
            run.state = cloneJson(event.snapshot)
            return state
        case 'STATE_DELTA':
            run.state = applyJsonPatch(run.state, event.delta)
            return state
        case 'CUSTOM':
            reduceCustom(state, run, event.name, event.value, event.timestamp)
            return state
        case 'REASONING_START':
        case 'REASONING_MESSAGE_START': {
            const id = event.messageId ?? `${runId}:reasoning`
            state.reasoning[id] = {id, runId, content: '', streaming: true}
            addOnce(run.reasoningIds, id)
            return state
        }
        case 'REASONING_MESSAGE_CHUNK': {
            const id = event.messageId ?? `${runId}:reasoning`
            const reasoning = state.reasoning[id] ?? {id, runId, content: '', streaming: true}
            reasoning.content = bounded(reasoning.content + (event.delta ?? ''), MAX_REASONING)
            reasoning.streaming = true
            state.reasoning[id] = reasoning
            addOnce(run.reasoningIds, id)
            return state
        }
        case 'REASONING_MESSAGE_CONTENT': {
            const reasoning = state.reasoning[event.messageId] ?? {
                id: event.messageId, runId, content: '', streaming: true,
            }
            reasoning.content = bounded(reasoning.content + event.delta, MAX_REASONING)
            state.reasoning[event.messageId] = reasoning
            addOnce(run.reasoningIds, event.messageId)
            return state
        }
        case 'REASONING_MESSAGE_END':
        case 'REASONING_END': {
            const id = event.messageId ?? run.reasoningIds[run.reasoningIds.length - 1]
            if (id && state.reasoning[id]) state.reasoning[id].streaming = false
            return state
        }
    }
    return state
}

export function runMessages(state: AgentUiState, runId: string): RichMessage[] {
    const run = state.runs[runId]
    if (!run) return []
    return run.messageIds.map((id) => state.messages[id]).filter(Boolean)
}

export function latestAssistantMessage(state: AgentUiState, runId: string): RichMessage | undefined {
    return [...runMessages(state, runId)].reverse().find((message) => message.role === 'assistant')
}

export function validateSurface(value: unknown, threadId?: string, runId?: string): UiSurface | null {
    if (!isObject(value)) return null
    const surfaceId = stringValue(value.surfaceId)
    if (!surfaceId) return null
    const profile = stringValue(value.profile) ?? ''
    const profileVersion = stringValue(value.profileVersion) ?? ''
    const dataRef = stringValue(value.dataRef) ?? ''
    const componentValues = Array.isArray(value.components) ? value.components : []
    const surfaceKeysValid = hasOnlyKeys(value, ['profile', 'profileVersion', 'surfaceId', 'dataRef', 'components', 'revision'])
    const components = componentValues.slice(0, MAX_COMPONENTS)
        .map(parseComponent).filter((item): item is SurfaceComponent => item !== null)
    let issue: SurfaceValidationIssue | undefined
    if (profile !== PROFILE) {
        issue = {code: 'UNKNOWN_PROFILE', message: `Unsupported UI profile: ${profile || 'missing'}`}
    } else if (profileVersion !== PROFILE_VERSION && profileVersion !== PROFILE_VERSION_NEXT) {
        issue = {
            code: 'UNSUPPORTED_PROFILE_VERSION',
            message: `Unsupported ${PROFILE} version: ${profileVersion || 'missing'}`,
        }
    } else if (!surfaceKeysValid || !validId(surfaceId) || !validId(dataRef) || components.length === 0
        || components.length !== componentValues.length
        || (profileVersion === PROFILE_VERSION_NEXT
            && (!Number.isInteger(value.revision) || (value.revision as number) < 1))
        || (profileVersion === PROFILE_VERSION && hasOwn(value, 'revision'))
        || components.some((component) => !validKnownComponent(component, profileVersion))) {
        issue = {code: 'INVALID_SURFACE', message: 'The UI surface does not match the ac.rich-ui schema'}
    }
    return {
        profile,
        profileVersion,
        surfaceId,
        dataRef,
        components,
        revision: profileVersion === PROFILE_VERSION_NEXT ? value.revision as number : undefined,
        threadId,
        runId,
        validationIssue: issue,
    }
}

export function safeToolResult(content: string | undefined): SafeToolResult {
    if (!content) return {}
    try {
        const parsed: unknown = JSON.parse(content)
        if (!isObject(parsed)) return {summary: 'Tool completed'}
        return {
            kind: stringValue(parsed.kind),
            summary: bounded(stringValue(parsed.summary) ?? 'Tool completed', 2_000),
            resultRef: bounded(stringValue(parsed.resultRef) ?? '', 512) || undefined,
        }
    } catch {
        // Native tool output can contain sensitive/full data. Never retain it in the UI state.
        return {summary: 'Tool completed'}
    }
}

function reduceCustom(
    state: AgentUiState,
    run: AgentRunState,
    name: string,
    value: unknown,
    timestamp?: number,
) {
    if (name === 'ui.surface.create') {
        const surface = validateSurface(value, run.threadId, run.id)
        if (!surface) {
            warn(state, 'Ignored ui.surface.create without a valid surfaceId')
            return
        }
        if (state.surfaces[surface.surfaceId]) {
            warn(state, `Ignored duplicate surface ${surface.surfaceId}`)
            return
        }
        state.surfaces[surface.surfaceId] = surface
        addOnce(run.surfaceIds, surface.surfaceId)
        const message = latestAssistantMessage(state, run.id) ?? ensureMessage(
            state, run, `${run.id}:assistant`)
        upsertSurfacePart(message, surface)
        return
    }
    if (name === 'ui.surface.update') {
        if (!isObject(value)
            || !hasOnlyKeys(value, ['profile', 'profileVersion', 'surfaceId', 'dataRef', 'components', 'revision'])
            || !stringValue(value.profile) || !stringValue(value.profileVersion)
            || !validId(value.surfaceId)
            || (!hasOwn(value, 'dataRef') && !hasOwn(value, 'components'))) {
            warn(state, 'Ignored ui.surface.update that does not match the profile schema')
            return
        }
        const id = String(value.surfaceId)
        const current = state.surfaces[id]
        if (!current) {
            warn(state, `Ignored update for unknown surface ${id}`)
            return
        }
        if (current.runId !== run.id || current.threadId !== run.threadId
            || value.profile !== current.profile || value.profileVersion !== current.profileVersion) {
            warn(state, `Ignored cross-run or cross-profile update for ${id}`)
            return
        }
        if (current.profileVersion === PROFILE_VERSION_NEXT
            && (!Number.isInteger(value.revision) || value.revision !== (current.revision ?? 0) + 1)) {
            warn(state, `Ignored out-of-order update for ${id}`)
            return
        }
        const merged = validateSurface({
            profile: value.profile,
            profileVersion: value.profileVersion,
            surfaceId: id,
            dataRef: hasOwn(value, 'dataRef') ? value.dataRef : current.dataRef,
            components: hasOwn(value, 'components') ? value.components : current.components,
            ...(current.profileVersion === PROFILE_VERSION_NEXT ? {revision: value.revision} : {}),
        }, run.threadId, run.id)
        if (!merged) return
        state.surfaces[id] = merged
        for (const message of Object.values(state.messages)) upsertSurfacePart(message, merged, false)
        return
    }
    if (name === 'ui.surface.remove') {
        if (!isObject(value)
            || !hasOnlyKeys(value, ['profile', 'profileVersion', 'surfaceId', 'revision'])
            || value.profile !== PROFILE
            || ![PROFILE_VERSION, PROFILE_VERSION_NEXT].includes(String(value.profileVersion))
            || !validId(value.surfaceId)) {
            warn(state, 'Ignored ui.surface.remove that does not match the profile schema')
            return
        }
        const id = value.surfaceId
        const current = state.surfaces[id]
        if (!current || current.runId !== run.id || current.threadId !== run.threadId
            || current.profileVersion !== value.profileVersion
            || (current.profileVersion === PROFILE_VERSION_NEXT
                && (!Number.isInteger(value.revision) || value.revision !== (current.revision ?? 0) + 1))) {
            warn(state, `Ignored invalid remove for ${id}`)
            return
        }
        delete state.surfaces[id]
        run.surfaceIds = run.surfaceIds.filter((surfaceId) => surfaceId !== id)
        for (const message of Object.values(state.messages)) {
            message.parts = message.parts.filter((part) => part.kind !== 'ui' || part.surfaceId !== id)
        }
        return
    }
    if (name.startsWith('subagent.')) {
        const payload = isObject(value) ? value : {}
        const id = stringValue(payload.taskId) ?? stringValue(payload.agentId)
            ?? `${run.id}:subagent:${Object.keys(state.subagents).length + 1}`
        state.subagents[id] = {id, runId: run.id, name, value: payload, timestamp}
    }
}

function ensureRun(state: AgentUiState, runId: string, threadId: string): AgentRunState {
    if (!state.runs[runId]) {
        state.runs[runId] = {
            id: runId,
            threadId,
            status: 'idle',
            messageIds: [],
            toolCallIds: [],
            surfaceIds: [],
            reasoningIds: [],
            interrupts: [],
            state: null,
        }
        state.runOrder.push(runId)
    }
    return state.runs[runId]
}

function ensureMessage(state: AgentUiState, run: AgentRunState, id: string): RichMessage {
    if (!state.messages[id]) {
        state.messages[id] = {
            id,
            runId: run.id,
            role: 'assistant',
            parts: [{kind: 'text', text: ''}],
            streaming: true,
        }
        addOnce(run.messageIds, id)
    }
    return state.messages[id]
}

function ensureTool(state: AgentUiState, run: AgentRunState, id: string) {
    if (!state.toolCalls[id]) {
        state.toolCalls[id] = {id, runId: run.id, name: 'unknown', arguments: '', status: 'running'}
        addOnce(run.toolCallIds, id)
    }
    return state.toolCalls[id]
}

function appendText(message: RichMessage, text: string) {
    const last = message.parts[message.parts.length - 1]
    if (last?.kind === 'text') last.text = bounded(last.text + text, MAX_TEXT)
    else message.parts.push({kind: 'text', text})
}

function upsertSurfacePart(message: RichMessage, surface: UiSurface, append = true) {
    const index = message.parts.findIndex((part) => part.kind === 'ui' && part.surfaceId === surface.surfaceId)
    const part: MessagePart = {kind: 'ui', surfaceId: surface.surfaceId, surface}
    if (index >= 0) message.parts[index] = part
    else if (append) message.parts.push(part)
}

function finishMessages(state: AgentUiState, run: AgentRunState, failed = false) {
    run.messageIds.forEach((id) => {
        state.messages[id].streaming = false
        if (failed) state.messages[id].failed = true
    })
}

function eventRunId(state: AgentUiState, event: AgUiEvent, expected?: string): string {
    if ('runId' in event && typeof event.runId === 'string') return event.runId
    if (expected) return expected
    return state.runOrder[state.runOrder.length - 1] ?? 'unscoped-run'
}

function parseComponent(value: unknown): SurfaceComponent | null {
    if (!isObject(value)) return null
    const id = stringValue(value.id)
    const type = stringValue(value.type)
    if (!hasOnlyKeys(value, ['id', 'type', 'props'])
        || !validId(id) || !validId(type) || !isObject(value.props)) return null
    return {id, type, props: {...value.props}}
}

function validKnownComponent(component: SurfaceComponent, version: string): boolean {
    const {type, props} = component
    if (!['Chart', 'Table', 'Timeline', 'RelationGraph'].includes(type)) {
        if (version !== PROFILE_VERSION_NEXT) return false
        return validPhaseTwoComponent(component)
    }
    if (!hasOnlyKeys(props, ['subType', 'title', 'description', 'encoding', 'options', 'drillDown'])
        || !optionalString(props.title, 200)
        || !optionalString(props.description, 2_000)
        || !isObject(props.encoding)
        || !validDrillDown(props.drillDown)) return false

    if (type === 'Chart') {
        return typeof props.subType === 'string'
            && ['bar', 'line', 'area', 'pie', 'scatter'].includes(props.subType)
            && validFieldMap(props.encoding, ['category', 'value'], ['category', 'value'])
            && validOptions(props.options, {
                orientation: ['vertical', 'horizontal'], order: ['none', 'asc', 'desc'],
                limit: [1, 1_000], showLegend: 'boolean', showLabels: 'boolean',
            })
    }
    if (type === 'Table') {
        const columns = props.encoding.columns
        return (props.subType === undefined || props.subType === 'standard')
            && hasOnlyKeys(props.encoding, ['columns'])
            && (columns === undefined || (Array.isArray(columns) && columns.length >= 1 && columns.length <= 50
                && columns.every((column) => isObject(column)
                    && hasOnlyKeys(column, ['field', 'label'])
                    && validField(column.field) && validString(column.label, 100))))
            && validOptions(props.options, {limit: [1, 1_000], sort: 'field', order: ['asc', 'desc']})
    }
    if (type === 'Timeline') {
        return props.subType === 'vertical'
            && validFieldMap(props.encoding, ['time', 'title', 'description', 'group'], ['time', 'title'])
            && validOptions(props.options, {limit: [1, 500], order: ['asc', 'desc'], group: 'boolean'})
    }
    return props.subType === 'network'
        && validFieldMap(props.encoding, ['source', 'target', 'label', 'sourceLabel'], ['source', 'target'])
        && validOptions(props.options, {limit: [1, 1_000], showLabels: 'boolean'})
}

function validPhaseTwoComponent(component: SurfaceComponent): boolean {
    const {type, props} = component
    if (!hasOnlyKeys(props, ['subType', 'title', 'description', 'encoding', 'options', 'drillDown'])
        || !optionalString(props.title, 200) || !optionalString(props.description, 2_000)
        || !isObject(props.encoding) || !validDrillDown(props.drillDown)) return false
    const shapes: Record<string, {subType: string; allowed: string[]; required: string[];
        limit: number; flag?: string}> = {
        Metric: {subType: 'single', allowed: ['label', 'value', 'unit', 'change'], required: ['label', 'value'], limit: 12, flag: 'showChange'},
        EntityCard: {subType: 'standard', allowed: ['id', 'name', 'kind', 'summary'], required: ['id', 'name'], limit: 50, flag: 'showSummary'},
        Tree: {subType: 'hierarchy', allowed: ['id', 'parentId', 'label', 'hasChildren'], required: ['id', 'parentId', 'label'], limit: 500},
        Heatmap: {subType: 'matrix', allowed: ['x', 'y', 'value'], required: ['x', 'y', 'value'], limit: 1000, flag: 'showLabels'},
        RelationshipPath: {subType: 'ordered', allowed: ['step', 'source', 'target', 'label'], required: ['step', 'source', 'target'], limit: 30, flag: 'showLabels'},
        EvidenceChain: {subType: 'ordered', allowed: ['step', 'id', 'title', 'source', 'summary', 'evidenceRef'], required: ['step', 'id', 'title', 'source'], limit: 50, flag: 'showSource'},
    }
    const shape = shapes[type]
    if (!shape || props.subType !== shape.subType
        || !validFieldMap(props.encoding, shape.allowed, shape.required)) return false
    const rules: Record<string, OptionRule> = {limit: [1, shape.limit]}
    if (shape.flag) rules[shape.flag] = 'boolean'
    return validOptions(props.options, rules)
}

type OptionRule = readonly string[] | readonly [number, number] | 'boolean' | 'field'

function validOptions(value: unknown, rules: Record<string, OptionRule>): boolean {
    if (value === undefined) return true
    if (!isObject(value) || !hasOnlyKeys(value, Object.keys(rules))) return false
    return Object.entries(value).every(([key, item]) => {
        const rule = rules[key]
        if (rule === 'boolean') return typeof item === 'boolean'
        if (rule === 'field') return validField(item)
        if (typeof rule[0] === 'number') {
            const [minimum, maximum] = rule as readonly [number, number]
            return typeof item === 'number' && Number.isInteger(item) && item >= minimum && item <= maximum
        }
        return typeof item === 'string' && (rule as readonly string[]).includes(item)
    })
}

function validFieldMap(
    value: Record<string, unknown>,
    allowed: string[],
    required: string[],
): boolean {
    return hasOnlyKeys(value, allowed)
        && required.every((key) => validField(value[key]))
        && Object.values(value).every((item) => validField(item))
}

function validDrillDown(value: unknown): boolean {
    if (value === undefined) return true
    return isObject(value)
        && hasOnlyKeys(value, ['enabled', 'dimension', 'label', 'promptTemplate'])
        && value.enabled === true
        && validField(value.dimension)
        && optionalString(value.label, 100)
        && validString(value.promptTemplate, 2_000)
}

function hasOnlyKeys(value: Record<string, unknown>, allowed: string[]): boolean {
    return Object.keys(value).every((key) => allowed.includes(key))
}

function hasOwn(value: Record<string, unknown>, key: string): boolean {
    return Object.prototype.hasOwnProperty.call(value, key)
}

function validId(value: unknown): value is string { return validString(value, 200) }
function validField(value: unknown): value is string { return validString(value, 200) }
function validString(value: unknown, max: number): value is string {
    return typeof value === 'string' && value.length >= 1 && value.length <= max
}
function optionalString(value: unknown, max: number): boolean {
    return value === undefined || (typeof value === 'string' && value.length <= max)
}

function applyJsonPatch(snapshot: unknown, operations: JsonPatchOperation[]): unknown {
    let target: unknown = cloneJson(snapshot ?? {})
    for (const operation of operations.slice(0, 256)) {
        if (!['add', 'replace', 'remove'].includes(operation.op)) continue
        const segments = operation.path.split('/').slice(1).map(unescapePointer)
        if (segments.length === 0) {
            if (operation.op === 'remove') target = null
            else target = cloneJson(operation.value)
            continue
        }
        if (!isObject(target) && !Array.isArray(target)) target = {}
        let parent: unknown = target
        for (const segment of segments.slice(0, -1)) {
            if (!isObject(parent) && !Array.isArray(parent)) break
            const record = parent as Record<string, unknown>
            if (!isObject(record[segment]) && !Array.isArray(record[segment])) record[segment] = {}
            parent = record[segment]
        }
        if (!isObject(parent) && !Array.isArray(parent)) continue
        const record = parent as Record<string, unknown>
        const key = segments[segments.length - 1]
        if (operation.op === 'remove') delete record[key]
        else record[key] = cloneJson(operation.value)
    }
    return target
}

function cloneJson<T>(value: T): T {
    try { return structuredClone(value) } catch { return value }
}

function unescapePointer(value: string) { return value.replace(/~1/g, '/').replace(/~0/g, '~') }
function addOnce(items: string[], value: string) { if (!items.includes(value)) items.push(value) }
function bounded(value: string, limit: number) { return value.length <= limit ? value : value.slice(0, limit) }
function stringValue(value: unknown) { return typeof value === 'string' ? value : undefined }
function isObject(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
}
function warn(state: AgentUiState, message: string) {
    state.warnings.push(message)
    if (state.warnings.length > 100) state.warnings.shift()
}
