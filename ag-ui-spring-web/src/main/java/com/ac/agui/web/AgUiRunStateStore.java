package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import reactor.core.publisher.Flux;

/** 按运行归属隔离的 AG-UI 事件存储、重放与实时投递接口。 */
public interface AgUiRunStateStore extends RuntimeEventSink {
    /** 幂等注册运行，并指示调用方是否应执行。 */
    Registration register(RunScope scope, AgUiRunAgentInput input, String fingerprint);
    /** 从指定事件 ID 之后读取历史事件并接收新事件。 */
    Flux<EncodedEvent> open(RunScope scope, String lastEventId);
    /** 尚无终态时补发运行完成事件。 */
    void finishIfMissing(RunScope scope, String status);
    /** 尚无终态时补发运行错误事件。 */
    void failIfMissing(RunScope scope, String code);

    /** 运行注册结果及首次执行标记。 */
    record Registration(RunScope scope, boolean execute) { }
    /** 带序号和 SSE 标识的已编码事件。 */
    record EncodedEvent(String id, long sequence, String data) { }
}
