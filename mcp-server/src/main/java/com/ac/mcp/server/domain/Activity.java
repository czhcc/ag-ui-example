package com.ac.mcp.server.domain;

import java.time.Instant;
public record Activity(String id, String personId, String city, Instant occurredAt,
                       String eventType, String description, String companion) {
    public Activity(String id, String personId, String city, Instant occurredAt) {
        this(id, personId, city, occurredAt, "BUSINESS", "商务活动", null);
    }
}
