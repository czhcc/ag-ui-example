package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import reactor.core.publisher.Flux;

/** Storage and live-delivery boundary for owner-scoped AG-UI run events. */
public interface AgUiRunStateStore extends RuntimeEventSink {
    Registration register(RunScope scope, AgUiRunAgentInput input, String fingerprint);
    Flux<EncodedEvent> open(RunScope scope, String lastEventId);
    void finishIfMissing(RunScope scope, String status);
    void failIfMissing(RunScope scope, String code);

    record Registration(RunScope scope, boolean execute) { }
    record EncodedEvent(String id, long sequence, String data) { }
}
