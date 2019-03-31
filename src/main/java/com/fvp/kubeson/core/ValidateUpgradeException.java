package com.fvp.kubeson.core;

public class ValidateUpgradeException extends Exception {

    public ValidateUpgradeException(String msg) {
        super(msg);
    }

    public ValidateUpgradeException(String msg, Throwable t) {
        super(msg, t);
    }
}
