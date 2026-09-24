package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.SurfaceSpec;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.result.ResultStore;

import java.util.Objects;

/**
 * 在不依赖传输层的情况下完成结果授权、视图校验和 Surface 映射。
 */
public final class DefaultPresentationService implements PresentationService {
    private final ResultStore resultStore;
    private final PresentationValidator validator;
    private final PresentationMapper mapper;

    /**
     * 使用默认校验器和映射器创建展示服务。
     */
    public DefaultPresentationService(ResultStore resultStore) {
        this(resultStore, new PresentationValidator(), new PresentationMapper());
    }

    /**
     * 使用指定结果存储、校验器和映射器创建展示服务。
     */
    public DefaultPresentationService(ResultStore resultStore,
                                      PresentationValidator validator,
                                      PresentationMapper mapper) {
        this.resultStore = Objects.requireNonNull(resultStore, "resultStore must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * 读取已授权结果，校验指定视图并生成 Surface 描述。
     */
    @Override
    public SurfaceSpec render(RunScope scope, AccessSubject subject,
                              ResultReference resultReference, String viewId) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(resultReference, "resultReference must not be null");
        if (viewId == null || viewId.isBlank()) {
            throw new IllegalArgumentException("viewId must not be blank");
        }
        var stored = resultStore.get(resultReference, scope, subject);
        var view = stored.result().presentation().views().stream()
                .filter(candidate -> candidate.id().equals(viewId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown viewId for result: " + viewId));
        validator.validate(view, stored.result());
        return mapper.map(resultReference, view);
    }
}
