package com.ac.agui.web;

import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.result.SharedResultStore;

/** Prevents multi-replica mode from silently using process-local result or replay state. */
public final class DistributedStorageGuard {
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
