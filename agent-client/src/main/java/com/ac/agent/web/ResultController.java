package com.ac.agent.web;

import com.ac.agent.mcp.result.StoredMcpResult;
import com.ac.agent.mcp.result.ResultStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/results")
public class ResultController {
    private final ResultStore resultStore;
    public ResultController(ResultStore resultStore) { this.resultStore = resultStore; }

    public record ResultPayload(String resultRef, String serverCode, String toolName,
                                Object data, Object summary, Long expiresAtEpochMs) { }

    @GetMapping("/{resultRef}")
    public ResponseEntity<ResultPayload> get(@PathVariable("resultRef") String resultRef) {
        try {
            StoredMcpResult stored = resultStore.get(resultRef);
            return ResponseEntity.ok(new ResultPayload(
                    stored.resultRef(), stored.serverCode(), stored.toolName(),
                    stored.result().data(), stored.result().summary(),
                    stored.expiresAt().toEpochMilli()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
