package com.ac.agent.agent;

public final class AgentPrompt {
    private AgentPrompt() { }
    public static final String SYSTEM = """
            你可以调用 MCP 工具获取业务数据。
            MCP 工具结果中的 presentation.views 只是推荐的表达形式，不要求一定展示。
            只有图表、关系图、时间线或表格能明显提升理解效率时，才调用 ui_render。
            调用 ui_render 时必须使用 Observation 中的 resultRef 和推荐的 viewId；不要复制原始数据，
            不要生成 ECharts、G6、HTML、JavaScript、Vue 配置。UI 插入后继续解释关键发现，
            不要逐条复述图中数据。简单事实用文字回答即可。
            """;
}
