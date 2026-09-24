package com.ac.mcp.contract.presentation;

import java.util.List;

/**
 * 汇总结果的展示模式及候选视图。
 */
public record PresentationHint(PresentationMode mode, List<ViewHint> views) {
    /**
     * 规范化空值，并校验展示模式与视图列表是否匹配。
     */
    public PresentationHint {
        mode = mode == null ? PresentationMode.NONE : mode;
        views = views == null ? List.of() : List.copyOf(views);
        if (mode == PresentationMode.NONE && !views.isEmpty()) {
            throw new IllegalArgumentException("NONE presentation must not contain views");
        }
        if (mode != PresentationMode.NONE && views.isEmpty()) {
            throw new IllegalArgumentException(mode + " presentation requires at least one view");
        }
    }

    /**
     * 创建不展示任何视图的建议。
     */
    public static PresentationHint none() {
        return new PresentationHint(PresentationMode.NONE, List.of());
    }

    /**
     * 创建包含指定视图的推荐展示建议。
     */
    public static PresentationHint recommended(ViewHint... views) {
        return new PresentationHint(PresentationMode.RECOMMENDED, List.of(views));
    }
}
