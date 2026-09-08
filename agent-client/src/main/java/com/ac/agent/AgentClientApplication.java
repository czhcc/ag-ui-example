package com.ac.agent;

import com.ac.agent.mcp.config.McpServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(McpServerProperties.class)
public class AgentClientApplication {
    public static void main(String[] args) { SpringApplication.run(AgentClientApplication.class, args); }
}
