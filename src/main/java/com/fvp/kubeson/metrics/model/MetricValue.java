package com.fvp.kubeson.metrics.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import com.fvp.kubeson.Configuration;

public class MetricValue {

    private static final DecimalFormat decimalFormat;

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        dfs.setGroupingSeparator(' ');

        decimalFormat = new DecimalFormat("###,###,###,###.###", dfs);
    }

    private int dimensionsHash;

    private List<KeyValue> dimensions;

    private List<String> values;

    public MetricValue(int dimensionsHash, List<KeyValue> dimensions, String value) {
        this.dimensionsHash = dimensionsHash;
        this.dimensions = dimensions;
        this.values = new ArrayList<>(Configuration.MAX_METRICS_VALUE_HISTORY);
        this.values.add(formatValue(value));
    }

    public int getDimensionsHash() {
        return dimensionsHash;
    }

    public List<KeyValue> getDimensions() {
        return dimensions;
    }

    public List<String> getValues() {
        return values;
    }

    public void addValue(String value) {
        values.add(0, formatValue(value));
        if (values.size() > Configuration.MAX_METRICS_VALUE_HISTORY) {
            values.remove(values.size() - 1);
        }
    }

    private String formatValue(String value) {
        try {
            return decimalFormat.format(Double.valueOf(value));
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MetricValue{");
        sb.append("dimensions=").append(dimensions);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }
}
