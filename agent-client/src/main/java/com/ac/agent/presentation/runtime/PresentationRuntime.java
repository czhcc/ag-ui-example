package com.ac.agent.presentation.runtime;

import com.ac.agent.mcp.result.ResultStore;
import com.ac.agent.presentation.model.SurfaceSpec;
import com.ac.agent.streaming.AgentEventBus;
import com.ac.agent.streaming.event.UiCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PresentationRuntime {
    private static final Logger log = LoggerFactory.getLogger(PresentationRuntime.class);
    private final ResultStore store;
    private final PresentationValidator validator;
    private final PresentationMapper mapper;
    private final AgentEventBus eventBus;
    public PresentationRuntime(ResultStore store, PresentationValidator validator, PresentationMapper mapper, AgentEventBus eventBus) {
        this.store = store; this.validator = validator; this.mapper = mapper; this.eventBus = eventBus;
    }
    public SurfaceSpec render(String resultRef, String viewId) {
        var stored = store.get(resultRef);
        var view = stored.result().presentation().views().stream().filter(it -> it.id().equals(viewId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown viewId for result: " + viewId));
        validator.validate(view, stored.result());
        var surface = mapper.map(resultRef, view);
        eventBus.emit(new UiCreateEvent(surface));
        log.info("Published UI_CREATE resultRef={} viewId={} surfaceId={}", resultRef, viewId, surface.surfaceId());
        return surface;
    }
}
