package com.ac.agent.presentation.model;

import java.util.List;
public record SurfaceSpec(String surfaceId, String dataRef, List<ComponentSpec> components) { }
