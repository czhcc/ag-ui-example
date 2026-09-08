package com.ac.agent.mcp.observation;

import com.ac.mcp.contract.result.McpResult;
import org.springframework.stereotype.Component;
import java.util.StringJoiner;

@Component
public class DefaultObservationBuilder implements ObservationBuilder {
    @Override public String build(String toolName, String resultRef, McpResult<?> result) {
        if (!result.success()) {
            var error = result.error();
            return "工具 " + toolName + " 执行业务失败。\n错误码: " + (error == null ? "UNKNOWN" : error.code())
                    + "\n错误说明: " + (error == null ? "未提供" : error.message()) + "\nresultRef: " + resultRef;
        }
        var text = new StringJoiner("\n");
        text.add("工具 " + toolName + " 执行成功。");
        if (result.summary() != null) {
            text.add("结果摘要:");
            if (result.summary().description() != null) text.add(result.summary().description());
            if (result.summary().count() != null) text.add("结果数量: " + result.summary().count());
            result.summary().highlights().stream().limit(5).forEach(text::add);
        }
        text.add("resultRef: " + resultRef);
        if (!result.presentation().views().isEmpty()) {
            text.add("推荐展示（按需选择，不要为了展示而展示）:");
            result.presentation().views().stream().limit(3).forEach(view -> text.add(
                    "viewId=" + view.id() + ", type=" + view.type() + ", subType=" + view.subType() + ", title=" + view.title()));
            text.add("需要展示时调用 ui_render；文字已足够时不要调用。");
        }
        return text.toString();
    }
}
