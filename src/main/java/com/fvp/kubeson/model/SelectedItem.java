package com.fvp.kubeson.model;

public class SelectedItem {

    private String text;

    private ItemType type;

    private Pod pod;

    private String container;

    public SelectedItem(SelectorItem selectorItem) {
        this.text = selectorItem.getText();
        this.type = selectorItem.getType();
        this.pod = selectorItem.getPod();
        this.container = selectorItem.getContainer();
    }

    public String getText() {
        return text;
    }

    public ItemType getType() {
        return type;
    }

    public Pod getPod() {
        return pod;
    }

    public void setPod(Pod pod) {
        this.pod = pod;
    }

    public String getContainer() {
        return container;
    }

    public boolean isRunning() {
        return this.pod.isRunning(container);
    }
}
