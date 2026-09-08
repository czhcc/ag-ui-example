package com.ac.agent.skill;

import java.util.List;
public record SkillDefinition(String code, String description, List<String> recommendedTools) { }
