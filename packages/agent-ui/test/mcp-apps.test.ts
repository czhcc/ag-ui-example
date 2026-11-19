import test from 'node:test'
import assert from 'node:assert/strict'
import {createAgentUiState, reduceAgentUiEvent} from '../src/core/reducer.ts'
const app = {profile: 'ac.mcp-app', profileVersion: '1.0', appInstanceId: 'app_1', messageId: 'run:assistant',
  threadId: 'thread', originRunId: 'run', toolCallId: 'call', title: '资金流', resourceHash: 'sha256:' + 'a'.repeat(64), resultRef: 'result', revision: 1}
test('App mixes with text in order, deduplicates creation and preserves existing rich-ui validation', () => {
  const s = createAgentUiState()
  const reduce = (e: any) => reduceAgentUiEvent(s, e, 'run')
  reduce({type: 'RUN_STARTED', threadId: 'thread', runId: 'run'})
  reduce({type: 'TEXT_MESSAGE_START', messageId: app.messageId, role: 'assistant'})
  reduce({type: 'TEXT_MESSAGE_CONTENT', messageId: app.messageId, delta: '之前'})
  reduce({type: 'CUSTOM', name: 'mcp.app.create', value: app})
  reduce({type: 'CUSTOM', name: 'mcp.app.create', value: app})
  reduce({type: 'TEXT_MESSAGE_CONTENT', messageId: app.messageId, delta: '之后'})
  assert.deepEqual(s.messages[app.messageId].parts.map(p => p.kind), ['text', 'mcp_app', 'text'])
  reduce({type: 'CUSTOM', name: 'mcp.app.update', value: {appInstanceId: 'app_1', revision: 2, resultRef: 'new'}})
  reduce({type: 'CUSTOM', name: 'mcp.app.update', value: {appInstanceId: 'app_1', revision: 1, resultRef: 'old'}})
  assert.equal(s.apps.app_1.resultRef, 'new')
})
test('App rejects forged scope, unknown versions, URLs and prototype keys', () => {
  for (const change of [{threadId: 'other'}, {originRunId: 'other'}, {profileVersion: '9'}, {resourceHash: 'url'}, {url: 'https://evil'}, {appInstanceId: '__proto__'}]) {
    const s = createAgentUiState()
    reduceAgentUiEvent(s, {type: 'RUN_STARTED', threadId: 'thread', runId: 'run'}, 'run')
    reduceAgentUiEvent(s, {type: 'CUSTOM', name: 'mcp.app.create', value: {...app, ...change}}, 'run')
    assert.equal(Object.keys(s.apps).length, 0)
  }
})
test('App updates cannot cross creating runs and gaps require snapshot sync', () => {
  const s = createAgentUiState()
  reduceAgentUiEvent(s, {type: 'RUN_STARTED', threadId: 'thread', runId: 'run'}, 'run')
  reduceAgentUiEvent(s, {type: 'CUSTOM', name: 'mcp.app.create', value: app}, 'run')
  reduceAgentUiEvent(s, {type: 'CUSTOM', name: 'mcp.app.remove', value: {appInstanceId: 'app_1', revision: 2}}, 'other')
  assert.equal(s.apps.app_1.status, undefined)
  reduceAgentUiEvent(s, {type: 'CUSTOM', name: 'mcp.app.update', value: {appInstanceId: 'app_1', revision: 3, resultRef: 'new'}}, 'run')
  assert.equal(s.apps.app_1.needsSync, true)
})
