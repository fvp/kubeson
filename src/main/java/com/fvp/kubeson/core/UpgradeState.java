package com.fvp.kubeson.core;

public enum UpgradeState {

    NO_UPGRADE_AVAILABLE("You are using the latest version"),
    UPGRADE_AVAILABLE("Upgrade to version %s available"),
    DOWNLOADING("Downloading version %s"),
    VALIDATING("Validating upgrade version %s"),
    UNPACKING("Unpacking upgrade version %s"),
    UPGRADE_ERROR("Upgrade to version %s failed with error: %s"),
    UPGRADE_SUCCESSFUL("Upgraded successfully to version %s, please restart to take effect");

    private String message;

    UpgradeState(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
