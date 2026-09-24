package com.ac.agent.config;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;

/** Marker for a Spring AI Alibaba checkpoint saver shared by all replicas. */
public interface SharedCheckpointSaver extends BaseCheckpointSaver { }
