package com.ac.agui.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.agui.community.core.message.UserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgUiRunInputDecoderTest {
    @Test
    void decodesCompleteWireModelIntoOfficialTypes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var input = new AgUiRunInputDecoder(mapper).decode(mapper.readTree("""
                {
                  "threadId":"thread-1",
                  "runId":"run-2",
                  "parentRunId":"run-1",
                  "state":{"route":"/sales"},
                  "messages":[{"id":"m1","role":"user","content":"hello"}],
                  "tools":[{"name":"client_lookup","description":"lookup",
                    "parameters":{"type":"object","properties":{},"required":[]}}],
                  "context":[{"description":"page","value":"sales"}],
                  "forwardedProps":{"locale":"zh-CN","tenantId":"must-not-win"},
                  "resume":[]
                }
                """));

        assertEquals("run-1", input.parentRunId());
        assertInstanceOf(UserMessage.class, input.messages().getFirst());
        assertEquals(Map.of("locale", "zh-CN"), input.trustedForwardedProps());
        assertEquals("run-2", input.officialInput().runId());
    }
}
