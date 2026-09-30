import test from 'node:test'
import assert from 'node:assert/strict'
import {randomUUID} from 'node:crypto'
import {readFileSync} from 'node:fs'
import {streamAgent, fetchResult} from '../src/core/stream.ts'
import type {AgUiEvent} from '../src/core/types.ts'

const enabled = process.env.AC_LIVE_PHASE_TWO === '1'
const base = process.env.AC_SAA_URL ?? 'http://localhost:8080'
const headers = {'X-Tenant-Id': 'local-tenant', 'X-User-Id': 'local-user'}
const source = readFileSync(new URL('../../../docs/测试问句.md', import.meta.url), 'utf8')
const section = source.slice(source.indexOf('## 15.'))
const prompts = new Map([...section.matchAll(/### 15\.(\d+)[^\r\n]*[\s\S]*?\r?\n> ([^\r\n]+)/g)]
    .map(match => [Number(match[1]), match[2]]))
const cases = [
    {number: 1, tool: 'kg_activity_metrics', component: 'Metric', count: 2},
    {number: 2, tool: 'kg_entity_cards', component: 'EntityCard', count: 2},
    {number: 3, tool: 'kg_relation_tree', component: 'Tree', count: 2},
    {number: 4, tool: 'kg_activity_heatmap', component: 'Heatmap', count: 9},
    {number: 5, tool: 'kg_relationship_path', component: 'RelationshipPath', count: 2},
    {number: 6, tool: 'kg_evidence_chain', component: 'EvidenceChain', count: 2},
]

for (const item of cases) {
    test(`phase-two question 15.${item.number}: ${item.component}`, {skip: !enabled, timeout: 150_000}, async () => {
        const question = prompts.get(item.number)
        assert.ok(question, `Missing test question 15.${item.number}`)
        const threadId = `phase-two-${randomUUID()}`
        const runId = `phase-two-${randomUUID()}`
        const events: AgUiEvent[] = []
        const signal = AbortSignal.timeout(140_000)
        await streamAgent({threadId, runId, state: {}, tools: [], context: [], forwardedProps: {},
            messages: [{id: randomUUID(), role: 'user', content: question}]},
        event => {
            events.push(event)
            if (process.env.AC_TRACE_PHASE_TWO === '1'
                && ['TOOL_CALL_START', 'TOOL_CALL_ARGS', 'TOOL_CALL_RESULT', 'CUSTOM'].includes(event.type))
                process.stdout.write(`${item.number}: ${JSON.stringify(event).slice(0, 700)}\n`)
        }, signal, {url: `${base}/api/agent`, headers})
        assert.equal(events.at(-1)?.type, 'RUN_FINISHED', `Last event: ${JSON.stringify(events.at(-1))}`)
        assert.ok(events.some(event => event.type === 'TOOL_CALL_START'
            && event.toolCallName.includes(item.tool)), `Missing ${item.tool}`)
        const surfaces = events.filter(event => event.type === 'CUSTOM' && event.name === 'ui.surface.create')
        const matching = surfaces.find(event => event.type === 'CUSTOM'
            && (event.value as {components?: Array<{type: string}>})?.components?.[0]?.type === item.component)
        assert.ok(matching && matching.type === 'CUSTOM', `Missing ${item.component} surface`)
        const surface = matching.value as {dataRef: string; profileVersion: string}
        assert.equal(surface.profileVersion, '1.1')
        const result = await fetchResult(surface.dataRef, {threadId, runId, signal},
            {resultUrl: `${base}/api/results`, headers})
        assert.equal((result.data as unknown[]).length, item.count)
    })
}

test('phase-two question 15.7: same-run tree update', {skip: !enabled, timeout: 150_000}, async () => {
    const question = prompts.get(7)
    assert.ok(question)
    const threadId = `phase-two-${randomUUID()}`
    const runId = `phase-two-${randomUUID()}`
    const events: AgUiEvent[] = []
    const signal = AbortSignal.timeout(140_000)
    await streamAgent({threadId, runId, state: {}, tools: [], context: [], forwardedProps: {},
        messages: [{id: randomUUID(), role: 'user', content: question}]},
    event => {
        events.push(event)
        if (process.env.AC_TRACE_PHASE_TWO === '1'
            && ['TOOL_CALL_START', 'TOOL_CALL_ARGS', 'TOOL_CALL_RESULT', 'CUSTOM'].includes(event.type))
            process.stdout.write(`7: ${JSON.stringify(event).slice(0, 700)}\n`)
    }, signal, {url: `${base}/api/agent`, headers})
    assert.equal(events.at(-1)?.type, 'RUN_FINISHED')
    const created = events.find(event => event.type === 'CUSTOM' && event.name === 'ui.surface.create')
    const updated = events.find(event => event.type === 'CUSTOM' && event.name === 'ui.surface.update')
    assert.ok(created && created.type === 'CUSTOM')
    assert.ok(updated && updated.type === 'CUSTOM')
    const before = created.value as {surfaceId: string; dataRef: string}
    const after = updated.value as {surfaceId: string; dataRef: string; revision: number}
    assert.equal(after.surfaceId, before.surfaceId)
    assert.notEqual(after.dataRef, before.dataRef)
    assert.equal(after.revision, 2)
    const result = await fetchResult(after.dataRef, {threadId, runId, signal},
        {resultUrl: `${base}/api/results`, headers})
    assert.equal((result.data as unknown[]).length, 3)
})
