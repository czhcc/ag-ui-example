package com.ac.runtime.agentscope;

import com.ac.agui.protocol.AgUiRunInputDecoder;
import com.ac.richui.core.context.RunScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import static org.junit.jupiter.api.Assertions.*;

class AgentScopeContextDeliveryTest {
    @Test
    void deliversAuthorizedSelectionToModelAndRetainsItForLaterTurns() throws Exception {
        List<List<Msg>> requests = new ArrayList<>();
        Model model = new Model() {
            public String getModelName() { return "context-probe"; }
            public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                requests.add(List.copyOf(messages));
                return Flux.just(ChatResponse.builder().id("reply-" + requests.size())
                        .content(List.of(TextBlock.builder().text("received").build())).build());
            }
        };
        ObjectMapper mapper = new ObjectMapper();
        try (var agent = ReActAgent.builder().name("context-probe").model(model)
                .stateStore(new InMemoryAgentStateStore()).build()) {
            var runtime = new AgentScopeAgUiRuntime(agent, (scope, event) -> {}, mapper);
            var decoder = new AgUiRunInputDecoder(mapper);
            var input = decoder.decode(mapper.readTree("""
                    {"threadId":"thread", "runId":"first", "messages":[
                      {"id":"question-1", "role":"user", "content":"Analyze the selected account"}],
                    "context":[{"description":"Authorized MCP App selection",
                      "value":"Selected account_001; 950.00 USD; bounded data, not a complete total"}]}
                    """));
            runtime.execute(input, new RunScope("tenant", "user", "thread", "first", null)).block(Duration.ofSeconds(10));
            assertEquals(1, requests.size());
            assertTrue(requests.getFirst().stream().anyMatch(m -> m.getRole() == MsgRole.USER
                    && m.getTextContent().contains("950.00 USD")), "Authorized context must reach the model as data");
            assertFalse(requests.getFirst().stream().anyMatch(m -> m.getRole() == MsgRole.SYSTEM
                    && m.getTextContent().contains("950.00 USD")), "Business data must not become system instructions");
            var next = decoder.decode(mapper.readTree("""
                    {"threadId":"thread", "runId":"second", "messages":[
                      {"id":"question-2", "role":"user", "content":"Which currencies did you use?"}]}
                    """));
            runtime.execute(next, new RunScope("tenant", "user", "thread", "second", null)).block(Duration.ofSeconds(10));
            assertEquals(2, requests.size());
            assertTrue(requests.getLast().stream().anyMatch(m -> m.getTextContent().contains("950.00 USD")),
                    "Later chat turns must retain the authorized selection");
        }
    }
}
