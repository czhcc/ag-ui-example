package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;

/**
 * 校验请求的视图并生成与传输方式无关的 Surface。
 */
@FunctionalInterface
public interface PresentationService {

    /**
     * 在访问授权和字段校验通过后创建 Surface。
     */
    SurfaceSpec render(
            RunScope scope,
            AccessSubject subject,
            ResultReference resultReference,
            String viewId);
}
