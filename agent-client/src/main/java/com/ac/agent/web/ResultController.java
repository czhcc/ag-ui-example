package com.ac.agent.web;

import com.ac.agui.web.ResultApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/results")
public final class ResultController {
    private final ResultApiService results;

    public ResultController(ResultApiService results) {
        this.results = results;
    }

    @GetMapping("/{resultRef}")
    public ResponseEntity<?> get(
            @PathVariable("resultRef") String resultRef,
            @RequestParam("threadId") String threadId,
            @RequestParam("runId") String runId,
            ServerHttpRequest request) {
        return results.get(resultRef, threadId, runId, request);
    }
}
