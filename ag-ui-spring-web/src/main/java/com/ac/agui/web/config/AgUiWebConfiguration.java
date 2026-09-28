package com.ac.agui.web.config;

import com.ac.agui.protocol.AgUiRunInputDecoder;
import com.ac.agui.web.api.AgUiController;
import com.ac.agui.web.result.LoggingResultAccessAuditor;
import com.ac.agui.web.result.ResultAccessAuditor;
import com.ac.agui.web.result.ResultAccessRateLimiter;
import com.ac.agui.web.result.ResultApiService;
import com.ac.agui.web.spi.AgUiRunHandler;
import com.ac.agui.web.spi.AgUiSubjectResolver;
import com.ac.agui.web.stream.AgUiEventStream;
import com.ac.agui.web.stream.AgUiRunStateStore;
import com.ac.richui.core.result.ResultStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 装配 AG-UI Web 层的默认解码器、事件流、结果服务及存储检查组件。 */
@Configuration
public class AgUiWebConfiguration {
    /** 创建 AG-UI 请求解码器。 */
    @Bean
    public AgUiRunInputDecoder agUiRunInputDecoder(ObjectMapper mapper) {
        return new AgUiRunInputDecoder(mapper);
    }

    /** 未提供自定义事件存储时创建进程内事件流。 */
    @Bean
    @ConditionalOnMissingBean(AgUiRunStateStore.class)
    public AgUiRunStateStore agUiEventStream(
            ObjectMapper mapper,
            @Value("${ac.ag-ui.retention:30m}") Duration retention,
            @Value("${ac.ag-ui.replay-limit:512}") int replayLimit,
            @Value("${ac.ag-ui.maximum-runs:10000}") int maximumRuns) {
        return new AgUiEventStream(mapper, retention, replayLimit, maximumRuns);
    }

    /** 使用应用注入的运行处理器和身份解析器创建控制器。 */
    @Bean
    public AgUiController agUiController(AgUiRunInputDecoder decoder, AgUiRunStateStore events,
                                         AgUiRunHandler handler, AgUiSubjectResolver subjects) {
        return new AgUiController(decoder, events, handler, subjects);
    }

    /** 未提供审计器时创建默认日志审计器。 */
    @Bean
    @ConditionalOnMissingBean(ResultAccessAuditor.class)
    public ResultAccessAuditor resultAccessAuditor() {
        return new LoggingResultAccessAuditor();
    }

    /** 根据配置创建结果读取限流器。 */
    @Bean
    public ResultAccessRateLimiter resultAccessRateLimiter(
            @Value("${ac.result-api.rate-limit.requests:120}") int requests,
            @Value("${ac.result-api.rate-limit.window:1m}") Duration window,
            @Value("${ac.result-api.rate-limit.maximum-subjects:100000}") int maximumSubjects) {
        return new ResultAccessRateLimiter(requests, window, maximumSubjects);
    }

    /** 创建带授权、限流及审计的结果读取服务。 */
    @Bean
    public ResultApiService resultApiService(ResultStore results, AgUiSubjectResolver subjects,
                                             ResultAccessRateLimiter rateLimiter,
                                             ResultAccessAuditor auditor) {
        return new ResultApiService(results, subjects, rateLimiter, auditor);
    }

    /** 创建多副本模式下的共享存储检查组件。 */
    @Bean
    public DistributedStorageGuard distributedStorageGuard(
            @Value("${ac.deployment.distributed:false}") boolean distributed,
            ResultStore results, AgUiRunStateStore runs) {
        return new DistributedStorageGuard(distributed, results, runs);
    }
}
