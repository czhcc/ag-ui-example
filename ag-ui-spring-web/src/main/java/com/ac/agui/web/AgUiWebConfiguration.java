package com.ac.agui.web;

import com.ac.agui.protocol.AgUiRunInputDecoder;
import com.ac.richui.core.result.ResultStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgUiWebConfiguration {
    @Bean
    public AgUiRunInputDecoder agUiRunInputDecoder(ObjectMapper mapper) {
        return new AgUiRunInputDecoder(mapper);
    }

    @Bean
    @ConditionalOnMissingBean(AgUiRunStateStore.class)
    public AgUiRunStateStore agUiEventStream(
            ObjectMapper mapper,
            @Value("${ac.ag-ui.retention:30m}") Duration retention,
            @Value("${ac.ag-ui.replay-limit:512}") int replayLimit,
            @Value("${ac.ag-ui.maximum-runs:10000}") int maximumRuns) {
        return new AgUiEventStream(mapper, retention, replayLimit, maximumRuns);
    }

    @Bean
    public AgUiController agUiController(AgUiRunInputDecoder decoder, AgUiRunStateStore events,
                                         AgUiRunHandler handler, AgUiSubjectResolver subjects) {
        return new AgUiController(decoder, events, handler, subjects);
    }

    @Bean
    @ConditionalOnMissingBean(ResultAccessAuditor.class)
    public ResultAccessAuditor resultAccessAuditor() {
        return new LoggingResultAccessAuditor();
    }

    @Bean
    public ResultAccessRateLimiter resultAccessRateLimiter(
            @Value("${ac.result-api.rate-limit.requests:120}") int requests,
            @Value("${ac.result-api.rate-limit.window:1m}") Duration window,
            @Value("${ac.result-api.rate-limit.maximum-subjects:100000}") int maximumSubjects) {
        return new ResultAccessRateLimiter(requests, window, maximumSubjects);
    }

    @Bean
    public ResultApiService resultApiService(ResultStore results, AgUiSubjectResolver subjects,
                                             ResultAccessRateLimiter rateLimiter,
                                             ResultAccessAuditor auditor) {
        return new ResultApiService(results, subjects, rateLimiter, auditor);
    }

    @Bean
    public DistributedStorageGuard distributedStorageGuard(
            @Value("${ac.deployment.distributed:false}") boolean distributed,
            ResultStore results, AgUiRunStateStore runs) {
        return new DistributedStorageGuard(distributed, results, runs);
    }
}
