package com.fvp.kubeson.metrics.model;

import java.util.Map;

public class Metrics {

    private Map<String, Metric> metrics;

    public Metrics(Map<String, Metric> metrics) {
        this.metrics = metrics;
    }

    public Map<String, Metric> getMetrics() {
        return this.metrics;
    }

    public Metric getMetric(String metricName) {
        return this.metrics.get(metricName);
    }

    public boolean isEmpty() {
        return metrics.isEmpty();
    }
}
