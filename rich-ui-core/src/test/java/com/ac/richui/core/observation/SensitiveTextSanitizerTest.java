package com.ac.richui.core.observation;

import com.ac.richui.core.text.SensitiveTextSanitizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证敏感文本脱敏规则。 */
class SensitiveTextSanitizerTest {
    @Test
    void removesSecretAndPersonalDataCanariesBeforePublication() {
        String original = "password=canary-password api_key=canary-key Bearer canary-token "
                + "person@example.org 13812345678 110101199001011234";
        String safe = new SensitiveTextSanitizer().sanitize(original);
        for (String canary : new String[]{"canary-password", "canary-key", "canary-token",
                "person@example.org", "13812345678", "110101199001011234"}) {
            assertFalse(safe.contains(canary), canary);
        }
        assertTrue(safe.contains("[REDACTED]"));
    }
}
