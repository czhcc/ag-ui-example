package com.ac.agent.presentation.model;

import java.util.Map;
public record ComponentSpec(String id, UiComponentType type, Map<String, Object> props) { }
