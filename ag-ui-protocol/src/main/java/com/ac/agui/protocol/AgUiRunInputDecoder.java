package com.ac.agui.protocol;

import com.agui.community.core.agent.Context;
import com.agui.community.core.interrupt.Resume;
import com.agui.community.core.interrupt.ResumeStatus;
import com.agui.community.core.message.AssistantMessage;
import com.agui.community.core.message.DeveloperMessage;
import com.agui.community.core.message.FunctionCall;
import com.agui.community.core.message.Message;
import com.agui.community.core.message.ReasoningMessage;
import com.agui.community.core.message.SystemMessage;
import com.agui.community.core.message.ToolCall;
import com.agui.community.core.message.ToolMessage;
import com.agui.community.core.message.UserMessage;
import com.agui.community.core.tool.Tool;
import com.agui.community.core.tool.ToolParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Strict decoder for the official AG-UI JSON request shape. */
public final class AgUiRunInputDecoder {
    private static final int MAX_BODY_CHARS = 256_000;
    private static final int MAX_MESSAGES = 200;
    private static final int MAX_TOOLS = 64;
    private static final int MAX_CONTEXT = 64;
    private final ObjectMapper mapper;

    public AgUiRunInputDecoder(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public AgUiRunAgentInput decode(JsonNode root) {
        if (root == null || !root.isObject()) throw invalid("Request body must be a JSON object");
        if (root.toString().length() > MAX_BODY_CHARS) throw invalid("Request body is too large");
        String threadId = requiredText(root, "threadId", 128);
        String runId = requiredText(root, "runId", 128);
        String parentRunId = optionalText(root, "parentRunId", 128);
        List<Message> messages = messages(root.path("messages"));
        List<Tool> tools = tools(root.path("tools"));
        List<Context> context = context(root.path("context"));
        List<Resume> resume = resume(root.path("resume"));
        Object state = value(root.get("state"));
        Object forwardedProps = value(root.get("forwardedProps"));
        return new AgUiRunAgentInput(threadId, runId, parentRunId, state, messages, tools,
                context, forwardedProps, resume);
    }

    private List<Message> messages(JsonNode node) {
        JsonNode array = arrayOrEmpty(node, "messages", MAX_MESSAGES);
        List<Message> result = new ArrayList<>();
        for (JsonNode item : array) {
            String id = requiredText(item, "id", 128);
            String role = requiredText(item, "role", 32);
            String content = optionalText(item, "content", 64_000);
            String name = optionalText(item, "name", 128);
            switch (role) {
                case "user" -> result.add(new UserMessage(id, required(content, "user message content"), name));
                case "system" -> result.add(new SystemMessage(id, required(content, "system message content"), name));
                case "developer" -> result.add(new DeveloperMessage(id, required(content, "developer message content"), name));
                case "reasoning" -> result.add(new ReasoningMessage(id,
                        required(content, "reasoning message content"), name,
                        optionalText(item, "encryptedValue", 64_000)));
                case "assistant" -> result.add(new AssistantMessage(id, content, name, toolCalls(item.path("toolCalls"))));
                case "tool" -> result.add(new ToolMessage(id,
                        required(content, "tool message content"),
                        requiredText(item, "toolCallId", 128), optionalText(item, "error", 2_000)));
                default -> throw invalid("Unsupported message role: " + role);
            }
        }
        return result;
    }

    private List<ToolCall> toolCalls(JsonNode node) {
        JsonNode array = arrayOrEmpty(node, "toolCalls", MAX_TOOLS);
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode item : array) {
            String type = optionalText(item, "type", 32);
            if (type != null && !ToolCall.TOOL_CALL_TYPE.equals(type)) {
                throw invalid("tool call type must be function");
            }
            JsonNode function = item.path("function");
            calls.add(new ToolCall(requiredText(item, "id", 128), new FunctionCall(
                    requiredText(function, "name", 128), requiredText(function, "arguments", 32_000))));
        }
        return calls;
    }

    private List<Tool> tools(JsonNode node) {
        JsonNode array = arrayOrEmpty(node, "tools", MAX_TOOLS);
        List<Tool> result = new ArrayList<>();
        for (JsonNode item : array) {
            JsonNode schema = item.path("parameters");
            if (!schema.isObject()) throw invalid("tool parameters must be a JSON Schema object");
            String schemaType = optionalText(schema, "type", 32);
            if (schemaType != null && !"object".equals(schemaType)) {
                throw invalid("tool parameters type must be object");
            }
            Map<String, Object> properties = schema.path("properties").isObject()
                    ? mapper.convertValue(schema.path("properties"), Map.class) : Map.of();
            List<String> required = new ArrayList<>();
            if (schema.path("required").isArray()) {
                schema.path("required").forEach(value -> required.add(value.asText()));
            }
            result.add(new Tool(requiredText(item, "name", 128),
                    optionalText(item, "description", 2_000) == null ? "" : item.path("description").asText(),
                    new ToolParameters("object", properties, required)));
        }
        return result;
    }

    private List<Context> context(JsonNode node) {
        JsonNode array = arrayOrEmpty(node, "context", MAX_CONTEXT);
        List<Context> result = new ArrayList<>();
        for (JsonNode item : array) {
            result.add(new Context(requiredText(item, "description", 512),
                    requiredText(item, "value", 8_000)));
        }
        return result;
    }

    private List<Resume> resume(JsonNode node) {
        JsonNode array = arrayOrEmpty(node, "resume", MAX_TOOLS);
        List<Resume> result = new ArrayList<>();
        for (JsonNode item : array) {
            try {
                result.add(new Resume(requiredText(item, "interruptId", 128),
                        ResumeStatus.fromValue(requiredText(item, "status", 32)), value(item.get("payload"))));
            } catch (IllegalArgumentException exception) {
                throw invalid("resume status must be resolved or cancelled");
            }
        }
        return result;
    }

    private JsonNode arrayOrEmpty(JsonNode node, String name, int max) {
        if (node == null || node.isMissingNode() || node.isNull()) return mapper.createArrayNode();
        if (!node.isArray()) throw invalid(name + " must be an array");
        if (node.size() > max) throw invalid(name + " exceeds limit " + max);
        return node;
    }

    private Object value(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode()
                ? null : mapper.convertValue(node, Object.class);
    }

    private String requiredText(JsonNode node, String name, int max) {
        String value = optionalText(node, name, max);
        return required(value, name);
    }

    private String optionalText(JsonNode node, String name, int max) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalid(name + " must be a string");
        String text = value.textValue();
        if (text.length() > max) throw invalid(name + " exceeds limit " + max);
        return text;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) throw invalid(name + " must not be blank");
        return value;
    }

    private AgUiProtocolException invalid(String message) {
        return new AgUiProtocolException("INVALID_RUN_AGENT_INPUT", message);
    }
}
