package com.fvp.kubeson.gui;

public interface SearchBoxListener {

    void onUpButton();

    void onDownButton();

    void onLeftButton();

    void onRightButton();

    void onClearButton();

    void onChange(String text);
}
