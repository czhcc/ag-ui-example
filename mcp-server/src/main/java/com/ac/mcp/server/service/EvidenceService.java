package com.ac.mcp.server.service;

import com.ac.mcp.server.domain.Evidence;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EvidenceService {
    public List<Evidence> query(String entityId) { return List.of(new Evidence("ev-1", entityId, "mock-source", "示例证据摘要")); }
}
