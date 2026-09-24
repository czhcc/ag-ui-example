package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import reactor.core.publisher.Mono;

/** Runtime adapter supplied by the composing application. */
public interface AgUiRunHandler {
    Mono<Void> execute(AgUiRunAgentInput input, RunScope scope);
    boolean cancel(RunScope scope);
}
