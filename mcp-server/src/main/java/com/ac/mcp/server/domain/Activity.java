package com.ac.mcp.server.domain;

import java.time.Instant;
public record Activity(String id, String personId, String city, Instant occurredAt) { }
