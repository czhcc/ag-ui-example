package com.ac.mcp.server.domain;

public record Relation(String sourceId, String targetId, String relationType, int weight) { }
