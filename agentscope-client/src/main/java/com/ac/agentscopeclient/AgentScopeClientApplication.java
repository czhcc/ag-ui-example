package com.ac.agentscopeclient;

import com.ac.agentscopeclient.config.AgentScopeMcpProperties;
import com.ac.agui.web.api.AgUiProtocolExceptionHandler;
import com.ac.agui.web.config.AgUiWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(AgentScopeMcpProperties.class)
@Import({AgUiWebConfiguration.class, AgUiProtocolExceptionHandler.class})
public class AgentScopeClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentScopeClientApplication.class, args);
    }
}
