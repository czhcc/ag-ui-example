package com.ac.richui.core;

import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.observation.DefaultObservationBuilder;
import com.ac.richui.core.presentation.PresentationValidator;
import com.ac.richui.core.result.InMemoryResultStore;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Runs on the core-only test classpath; runtime frameworks must remain absent. */
/** 检查公共核心模块不依赖特定 Runtime、Web 或存储实现。 */
class PortableCoreBoundaryTest {
    @Test
    void coreLoadsWithoutRuntimeFrameworks() {
        new RunScope("tenant", "user", "thread", "run", null);
        new DefaultObservationBuilder();
        new PresentationValidator();
        new InMemoryResultStore(Duration.ofMinutes(1), 10);

        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.alibaba.cloud.ai.graph.StateGraph"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.agentscope.agent.ReActAgent"));
    }
}
