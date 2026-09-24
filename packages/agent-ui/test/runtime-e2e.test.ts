import test from 'node:test'
import assert from 'node:assert/strict'
import {randomUUID} from 'node:crypto'
import {streamAgent, fetchResult} from '../src/core/stream.ts'
import type {AgUiEvent} from '../src/core/types.ts'

// Explicit opt-in: these tests call a real model and MCP tools and may incur charges.
const enabled = process.env.AC_LIVE_E2E === '1'
const headers = {
    'X-Tenant-Id': process.env.AC_TEST_TENANT ?? 'local-tenant',
    'X-User-Id': process.env.AC_TEST_USER ?? 'local-user',
}

for (const [runtime, base] of [
    ['SAA', process.env.AC_SAA_URL ?? 'http://localhost:8080'],
    ['AgentScope', process.env.AC_AGENTSCOPE_URL ?? 'http://localhost:8082'],
] as const) {
    test(`${runtime}: real text, MCP tool, surface, authorized result`, {
        skip: !enabled, timeout: 180_000,
    }, async () => {
        const threadId = `e2e-${randomUUID()}`
        const runId = `e2e-${randomUUID()}`
        const events: AgUiEvent[] = []
        const signal = AbortSignal.timeout(170_000)
        await streamAgent({
            threadId, runId, state: {}, tools: [], context: [], forwardedProps: {},
            messages: [{id: randomUUID(), role: 'user', content: process.env.AC_E2E_PROMPT
                ?? '调用 kg_activity_statistics 查询 person-001 的活动城市统计，然后调用 ui_render 展示结果并给出文字总结。'}],
        }, event => events.push(event), signal, {url: `${base}/api/agent`, headers})

        assert.equal(events[0]?.type, 'RUN_STARTED')
        assert.equal(events.filter(e => e.type === 'RUN_FINISHED' || e.type === 'RUN_ERROR').length, 1)
        assert.equal(events.at(-1)?.type, 'RUN_FINISHED')
        assert.ok(events.some(e => e.type === 'TEXT_MESSAGE_CONTENT' && e.delta.length > 0))
        const calls = events.filter(e => e.type === 'TOOL_CALL_START')
        assert.ok(calls.some(e => e.toolCallName.includes('kg_activity_statistics')))
        assert.ok(calls.some(e => e.toolCallName.includes('ui_render')))
        const surfaces = events.filter(e => e.type === 'CUSTOM' && e.name === 'ui.surface.create')
        assert.ok(surfaces.length > 0)
        for (const event of surfaces) {
            if (event.type !== 'CUSTOM') continue
            const surface = event.value as {profile: string; profileVersion: string; dataRef: string; components: unknown[]}
            assert.equal(surface.profile, 'ac.rich-ui')
            assert.equal(surface.profileVersion, '1.0')
            assert.ok(surface.components.length > 0)
            const result = await fetchResult(surface.dataRef, {threadId, runId, signal}, {
                resultUrl: `${base}/api/results`, headers,
            })
            assert.equal(result.resultRef, surface.dataRef)
            assert.ok(result.data != null)
            const denied = await fetch(`${base}/api/results/${encodeURIComponent(surface.dataRef)}?`
                + new URLSearchParams({threadId, runId}), {
                headers: {...headers, 'X-User-Id': `intruder-${randomUUID()}`}, signal,
            })
            assert.equal(denied.status, 403)
        }
    })
}
