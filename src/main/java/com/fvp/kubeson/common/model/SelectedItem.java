package com.fvp.kubeson.common.model;

public class SelectedItem {

    private String text;

    private ItemType type;

    private K8SPod pod;

    private K8SConfigMap configMap;

    private String container;

    public SelectedItem(SelectorItem selectorItem) {
        this.text = selectorItem.getText();
        this.type = selectorItem.getType();
        this.pod = selectorItem.getPod();
        this.configMap = selectorItem.getConfigMap();
        this.container = selectorItem.getContainer();
    }

    public String getText() {
        return text;
    }

    public ItemType getType() {
        return type;
    }

    public K8SPod getPod() {
        return pod;
    }

    public void setPod(K8SPod pod) {
        this.pod = pod;
    }

    public K8SConfigMap getConfigMap() {
        return configMap;
    }

    public String getContainer() {
        return container;
    }

    public boolean isRunning() {
        return this.pod.isRunning(container);
    }
}
