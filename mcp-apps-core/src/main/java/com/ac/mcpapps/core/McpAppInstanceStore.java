package com.ac.mcpapps.core;

import java.util.function.Function;

/** Updates must be atomic across replicas. Shared implementations must preserve this contract. */
public interface McpAppInstanceStore {
    void put(McpAppInstance instance);
    <T> T update(String id, Function<McpAppInstance, T> operation);
    default boolean shared() { return false; }
}
