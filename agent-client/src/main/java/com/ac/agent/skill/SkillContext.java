package com.ac.agent.skill;

import com.ac.agent.agent.AgentContext;
import java.util.Map;
public record SkillContext(AgentContext agentContext, Map<String, Object> variables) { }
