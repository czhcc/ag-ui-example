package com.ac.agent.skill;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SkillManager {
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();
    public void register(SkillDefinition definition) { skills.put(definition.code(), definition); }
    public Optional<SkillDefinition> find(String code) { return Optional.ofNullable(skills.get(code)); }
}
