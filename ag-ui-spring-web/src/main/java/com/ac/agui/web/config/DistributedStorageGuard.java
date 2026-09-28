package com.ac.agui.web.config;

import com.ac.agui.web.stream.AgUiRunStateStore;
import com.ac.agui.web.stream.SharedAgUiRunStateStore;
import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.result.SharedResultStore;

/** 阻止多副本部署继续使用仅在单进程内有效的结果或事件存储。 */
public final class DistributedStorageGuard {
    /** 分布式模式下校验结果存储和运行状态存储均为共享实现。 */
    public DistributedStorageGuard(boolean distributed, ResultStore results, AgUiRunStateStore runs) {
        if (!distributed) return;
        if (!(results instanceof SharedResultStore)) {
            throw new IllegalStateException(
                    "Distributed mode requires a SharedResultStore (Redis/database implementation)");
        }
        if (!(runs instanceof SharedAgUiRunStateStore)) {
            throw new IllegalStateException(
                    "Distributed mode requires a SharedAgUiRunStateStore (shared replay and live delivery)");
        }
    }
}
