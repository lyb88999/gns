package com.gns.notification.service.strategy;

import lombok.Getter;

@Getter
public class DispatchResult {
    private final boolean success;
    private final String recipient;
    private final String errorMessage;

    public DispatchResult(boolean success, String recipient, String errorMessage) {
        this.success = success;
        this.recipient = recipient;
        this.errorMessage = errorMessage;
    }

    public static DispatchResult success(String recipient) {
        return new DispatchResult(true, recipient, null);
    }

    public static DispatchResult failure(String recipient, String errorMessage) {
        return new DispatchResult(false, recipient, errorMessage);
    }

    public static DispatchResult ignored() {
        return new DispatchResult(true, null, null);
    }

}
