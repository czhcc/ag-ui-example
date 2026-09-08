package com.ac.mcp.server.service;

import com.ac.mcp.server.domain.Relation;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RelationService {
    public List<Relation> expand(String entityId, int depth) { return List.of(new Relation(entityId, "person-002", "KNOWS", 8)); }
    public List<Relation> statistics(String entityId) { return expand(entityId, 1); }
}
