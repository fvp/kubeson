package com.fvp.kubeson.common.controller;

public class K8SRequestResult {

    private boolean successful;

    private Throwable throwable;

    private K8SRequestResult(boolean successful, Throwable throwable) {
        this.successful = successful;
        this.throwable = throwable;
    }

    public static void apply(K8SRequestCallback requestCallback, boolean successful) {
        apply(requestCallback, successful, null);
    }

    public static void apply(K8SRequestCallback requestCallback, boolean successful, Throwable throwable) {
        if (requestCallback != null) {
            requestCallback.callback(new K8SRequestResult(successful, throwable));
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
