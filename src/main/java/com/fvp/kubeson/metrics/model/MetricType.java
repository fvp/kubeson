package com.fvp.kubeson.metrics.model;

public enum MetricType {

    GAUGE, SUMMARY, COUNTER, HISTOGRAM;

    public static MetricType getType(String metricType) {
        for (MetricType type : MetricType.values()) {
            if (type.name().equalsIgnoreCase(metricType)) {
                return type;
            }
        }

        return null;
    }
}
