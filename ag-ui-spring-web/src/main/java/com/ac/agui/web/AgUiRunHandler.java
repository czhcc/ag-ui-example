package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import reactor.core.publisher.Mono;

/** 由应用组装层提供的 Runtime 执行与取消接口。 */
public interface AgUiRunHandler {
    /** 在指定运行范围内执行 AG-UI 请求。 */
    Mono<Void> execute(AgUiRunAgentInput input, RunScope scope);
    /** 取消指定运行，并返回是否找到可取消的运行。 */
    boolean cancel(RunScope scope);
}
