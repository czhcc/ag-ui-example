import test from 'node:test'
import assert from 'node:assert/strict'
import {AgUiSseParser} from '../src/core/stream.ts'
import type {AgUiEvent} from '../src/core/types.ts'

test('parses frames split across arbitrary chunks', () => {
    const events: AgUiEvent[] = []
    const ids: string[] = []
    const parser = new AgUiSseParser((event) => events.push(event), (id) => ids.push(id))
    parser.push('id: run-1:1\r\nda')
    parser.push('ta: {"type":"TEXT_MESSAGE_CONTENT","messageId":"m1",')
    parser.push('"delta":"hel')
    parser.push('lo"}\r\n\r\n')
    parser.finish()

    assert.deepEqual(ids, ['run-1:1'])
    assert.equal(events.length, 1)
    assert.equal(events[0].type, 'TEXT_MESSAGE_CONTENT')
})

test('parses merged frames, blank lines, comments, and multiline data', () => {
    const events: AgUiEvent[] = []
    const parser = new AgUiSseParser((event) => events.push(event))
    parser.push('\n\n: keep-alive\n\n'
        + 'data: {"type":"RUN_STARTED",\n'
        + 'data: "threadId":"thread","runId":"run"}\n\n'
        + 'data: {"type":"RUN_FINISHED","threadId":"thread","runId":"run"}\n\n')
    parser.finish()

    assert.deepEqual(events.map((event) => event.type), ['RUN_STARTED', 'RUN_FINISHED'])
})

test('ignores unknown event types for forward compatibility', () => {
    const events: AgUiEvent[] = []
    const parser = new AgUiSseParser((event) => events.push(event))
    parser.push('data: {"type":"FUTURE_EVENT","value":1}\n\n')
    parser.finish()
    assert.deepEqual(events, [])
})

test('every split position preserves CRLF and a comment preceding data', () => {
    const frame = ': comment\r\ndata: {"type":"RUN_STARTED",\r\ndata: "threadId":"t","runId":"r"}\r\n\r\n'
    for (let split = 0; split <= frame.length; split++) {
        const events: AgUiEvent[] = []
        const parser = new AgUiSseParser((event) => events.push(event))
        parser.push(frame.slice(0, split))
        parser.push(frame.slice(split))
        parser.finish()
        assert.equal(events.length, 1, `split=${split}`)
    }
})

test('rejects malformed JSON and oversized unterminated frames', () => {
    assert.throws(() => {
        const parser = new AgUiSseParser(() => undefined)
        parser.push('data: {not-json}\n\n')
    }, SyntaxError)

    assert.throws(() => {
        const parser = new AgUiSseParser(() => undefined)
        parser.push(`data: ${'x'.repeat(1_000_001)}`)
    }, /exceeds the client limit/)
})
