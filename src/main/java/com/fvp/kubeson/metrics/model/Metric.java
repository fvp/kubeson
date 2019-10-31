package com.fvp.kubeson.metrics.model;

import java.util.ArrayList;
import java.util.List;

public class Metric {

    private String name;

    private MetricType type;

    private String description;

    private List<MetricValue> metricValues;

    public Metric(String name) {
        this.name = name;
        this.metricValues = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public MetricType getType() {
        return type;
    }

    public Metric setType(MetricType type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Metric setDescription(String description) {
        this.description = description;
        return this;
    }

    public void addValue(int dimensionsHash, List<KeyValue> dimensions, String value) {
        for (MetricValue currentMetricValue : this.metricValues) {
            if (currentMetricValue.getDimensionsHash() == dimensionsHash) {
                currentMetricValue.addValue(value);
                return;
            }
        }
        metricValues.add(new MetricValue(dimensionsHash, dimensions, value));
    }

    public List<MetricValue> getMetricValues() {
        return metricValues;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Metric{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", description='").append(description).append('\'');
        sb.append(", metricValues=").append(metricValues);
        sb.append('}');
        return sb.toString();
    }
}
