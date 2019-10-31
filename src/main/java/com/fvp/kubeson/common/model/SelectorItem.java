package com.fvp.kubeson.common.model;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

public class SelectorItem implements Comparable<SelectorItem> {

    private String key;

    private String text;

    private ItemType type;

    private K8SPod pod;

    private K8SConfigMap configMap;

    private String style;

    private Property<Boolean> checked;

    private Property<Boolean> disabled;

    public SelectorItem(K8SPod pod, String key, String text, ItemType type) {
        this.disabled = new SimpleBooleanProperty();
        this.key = key;
        this.text = text;
        this.type = type;
        this.pod = pod;
        setFailedStateColor();
    }

    public SelectorItem(K8SPod pod, String text, ItemType type) {
        this.disabled = new SimpleBooleanProperty();
        this.text = text;
        this.type = type;
        this.pod = pod;
        setFailedStateColor();
    }

    public SelectorItem(K8SConfigMap configMap, String text, ItemType type) {
        this.disabled = new SimpleBooleanProperty();
        this.text = text;
        this.type = type;
        this.configMap = configMap;
    }

    public SelectorItem(String text) {
        this.disabled = new SimpleBooleanProperty();
        this.text = text;
        this.type = ItemType.TEXT;
        this.style = "-fx-font-weight: bolder;-fx-font-size: 14px;";
    }

    private void setFailedStateColor() {
        if (K8SPod.STATUS_SUCCEEDED.equals(pod.getState())) {
            this.style = "-fx-fill: #004d1a;";
        } else if (K8SPod.STATUS_PENDING.equals(pod.getState())) {
            this.style = "-fx-fill: #666600;";
        } else if (!K8SPod.STATUS_RUNNING.equals(pod.getState())) {
            this.style = "-fx-fill: firebrick;";
        }
    }

    public String getKey() {
        if (key == null) {
            return getText();
        }
        return key;
    }

    public String getText() {
        return text;
    }

    public String getContainer() {
        if (this.type == ItemType.CONTAINER) {
            return getKey();
        }
        return null;
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

    public String getStyle() {
        return style;
    }

    public boolean isChecked() {
        if (checked != null) {
            return checked.getValue();
        }
        return false;
    }

    public void setChecked(Boolean value) {
        checked.setValue(value);
    }

    public Property<Boolean> getCheckedProperty() {
        return checked;
    }

    public void setCheckedProperty(Property<Boolean> property) {
        checked = property;
    }

    public Property<Boolean> getDisabledProperty() {
        return disabled;
    }

    public void setDisabledProperty(Boolean disabled) {
        this.disabled.setValue(disabled);
    }

    @Override
    public int compareTo(SelectorItem o) {
        return this.getText().compareTo(o.getText());
    }
}
