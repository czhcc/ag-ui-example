package com.ac.agent;

import com.ac.agent.mcp.config.McpServerProperties;
import com.ac.agui.web.api.AgUiProtocolExceptionHandler;
import com.ac.agui.web.config.AgUiWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(McpServerProperties.class)
@Import({AgUiWebConfiguration.class, AgUiProtocolExceptionHandler.class})
public class AgentClientApplication {
    public static void main(String[] args) { SpringApplication.run(AgentClientApplication.class, args); }
}
