package com.ac.agent.config;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;

/** Prevents distributed SAA deployments from retaining resumable state in one process. */
public final class SaaDistributedStateGuard {
    public SaaDistributedStateGuard(boolean distributed, BaseCheckpointSaver saver) {
        if (distributed && !(saver instanceof SharedCheckpointSaver)) {
            throw new IllegalStateException(
                    "Distributed mode requires a SharedCheckpointSaver implementation");
        }
    }
}
