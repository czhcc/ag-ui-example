package com.ac.agent.mcp.result;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ResultRefGenerator {
    public String next() { return "result_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}
