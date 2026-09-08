package com.ac.mcp.server.service;

import com.ac.mcp.server.domain.Entity;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EntityService {
    public List<Entity> search(String keyword) { return List.of(new Entity("person-001", keyword, "PERSON")); }
    public Entity get(String id) { return new Entity(id, "示例实体", "PERSON"); }
}
