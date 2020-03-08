package com.fvp.kubeson.common.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class K8SResourceChange<T> {

    private static Logger LOGGER = LogManager.getLogger();

    private List<T> resourceAdded;

    private List<T> resourceRemoved;

    private List<T> resourceUpdated;

    public K8SResourceChange() {
        this.resourceAdded = new ArrayList<>();
        this.resourceRemoved = new ArrayList<>();
        this.resourceUpdated = new ArrayList<>();
    }

    private K8SResourceChange(List<T> resourceAdded, List<T> resourceRemoved, List<T> resourceUpdated) {
        this.resourceAdded = resourceAdded;
        this.resourceRemoved = resourceRemoved;
        this.resourceUpdated = resourceUpdated;
    }

    public K8SResourceChange<T> filter(Predicate<? super T> predicate) {
        return new K8SResourceChange<>(
                resourceAdded.stream().filter(predicate).collect(Collectors.toList()),
                resourceRemoved.stream().filter(predicate).collect(Collectors.toList()),
                resourceUpdated.stream().filter(predicate).collect(Collectors.toList())
        );
    }

    public K8SResourceChange<T> forEachAdded(Consumer<? super T> consumer) {
        resourceAdded.forEach(consumer);
        return this;
    }

    public K8SResourceChange<T> forEachRemoved(Consumer<? super T> consumer) {
        resourceRemoved.forEach(consumer);
        return this;
    }

    public K8SResourceChange<T> forEachUpdated(Consumer<? super T> consumer) {
        resourceUpdated.forEach(consumer);
        return this;
    }

    public boolean hasChanges() {
        return !resourceAdded.isEmpty() || !resourceRemoved.isEmpty() || !resourceUpdated.isEmpty();
    }

    public List<T> getAdded() {
        return resourceAdded;
    }

    public List<T> getRemoved() {
        return resourceRemoved;
    }

    public List<T> getUpdated() {
        return resourceUpdated;
    }

    public boolean wasAdded() {
        return !resourceAdded.isEmpty();
    }

    public boolean wasRemoved() {
        return !resourceRemoved.isEmpty();
    }

    public boolean wasUpdated() {
        return !resourceUpdated.isEmpty();
    }

    void resourceAdded(T resource) {
        LOGGER.debug("Resource added: {} ", resource);
        resourceAdded.add(resource);
    }

    void resourceRemoved(T resource) {
        LOGGER.debug("Resource removed: {} ", resource);
        resourceRemoved.add(resource);
    }

    void resourceUpdated(T resource) {
        LOGGER.debug("Resource updated: {} ", resource);
        resourceUpdated.add(resource);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("K8SResourceChange{");
        sb.append("resourceAdded=").append(resourceAdded);
        sb.append(", resourceRemoved=").append(resourceRemoved);
        sb.append(", resourceUpdated=").append(resourceUpdated);
        sb.append('}');
        return sb.toString();
    }
}
