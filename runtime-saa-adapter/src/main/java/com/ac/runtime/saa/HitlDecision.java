package com.ac.runtime.saa;

public record HitlDecision(
        String toolCallId,
        Action action,
        String editedArguments,
        String reason) {

    public enum Action {
        APPROVE, REJECT, EDIT
    }
}
