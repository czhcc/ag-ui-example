package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;

/** Validates a requested view and creates a transport-independent surface. */
@FunctionalInterface
public interface PresentationService {

    SurfaceSpec render(
            RunScope scope,
            AccessSubject subject,
            ResultReference resultReference,
            String viewId);
}
