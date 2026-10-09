import test from 'node:test'
import assert from 'node:assert/strict'
import {createAgentUiState, reduceAgentUiEvent} from '../src/core/reducer.ts'

const surface = (revision: number, dataRef: string) => ({
    profile: 'ac.rich-ui', profileVersion: '1.1', surfaceId: 'surface-1', revision, dataRef,
    components: [{id: 'metric-1', type: 'Metric', props: {
        subType: 'single', encoding: {label: 'label', value: 'value'}, options: {limit: 12},
    }}],
})

test('metric encoding rejects explicit null and accepts omitted optional fields', () => {
    const state = createAgentUiState()
    reduceAgentUiEvent(state, {type: 'RUN_STARTED', threadId: 'thread', runId: 'run'}, 'run')
    const invalid = surface(1, 'result-1')
    Object.assign(invalid.components[0].props.encoding, {change: null})
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.create', value: invalid}, 'run')
    assert.equal(state.surfaces['surface-1'].validationIssue?.code, 'INVALID_SURFACE')
    const valid = {...surface(1, 'result-2'), surfaceId: 'surface-2'}
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.create', value: valid}, 'run')
    assert.equal(state.surfaces['surface-2'].validationIssue, undefined)
})

test('phase-two update requires the creating run and next revision', () => {
    const state = createAgentUiState()
    reduceAgentUiEvent(state, {type: 'RUN_STARTED', threadId: 'thread', runId: 'run-1'}, 'run-1')
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.create', value: surface(1, 'result-1')}, 'run-1')
    assert.equal(state.surfaces['surface-1'].revision, 1)
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.update', value: surface(2, 'result-2')}, 'run-1')
    assert.equal(state.surfaces['surface-1'].dataRef, 'result-2')
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.update', value: surface(2, 'stale')}, 'run-1')
    assert.equal(state.surfaces['surface-1'].dataRef, 'result-2')
    reduceAgentUiEvent(state, {type: 'RUN_STARTED', threadId: 'thread', runId: 'run-2'}, 'run-2')
    reduceAgentUiEvent(state, {type: 'CUSTOM', name: 'ui.surface.update', value: surface(3, 'wrong-run')}, 'run-2')
    assert.equal(state.surfaces['surface-1'].dataRef, 'result-2')
})
