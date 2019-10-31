package com.fvp.kubeson.metrics.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fvp.kubeson.metrics.model.KeyValue;
import com.fvp.kubeson.metrics.model.Metric;
import com.fvp.kubeson.metrics.model.MetricType;
import com.fvp.kubeson.metrics.model.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrometheusMetricsParser {

    private static final Pattern HELP_PATTERN = Pattern.compile("^# HELP (?<name>[^ ]+) (?<data>.*)$");

    private static final Pattern TYPE_PATTERN = Pattern.compile("^# TYPE (?<name>[^ ]+) (?<type>.*)$");

    private static final Pattern METRIC_PATTERN = Pattern.compile("^(?<name>[^{]+)\\{?(?<dimentions>[^}]*)\\}? (?<value>.*)$");

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(?<key>[^=]+)=(?<value>[^,]+),? ?");

    private static Logger LOGGER = LogManager.getLogger();

    private PrometheusMetricsParser() {

    }

    public static Metrics parse(InputStream data) {
        return parse(data, new TreeMap<>());
    }

    public static Metrics parseAndUpdateValues(InputStream data, Metrics currentMetrics) {
        return parse(data, currentMetrics.getMetrics());
    }

    public static Metrics parse(InputStream data, Map<String, Metric> metrics) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(data))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.charAt(0) == '#') {
                    // # HELP Line
                    Matcher helpLine = HELP_PATTERN.matcher(line);
                    if (helpLine.matches()) {
                        Map<String, String> dataMap = parseKeyValueMap(helpLine.group("data"));
                        getMetric(metrics, helpLine.group("name")).setDescription(dataMap.get("description"));
                        continue;
                    }

                    // # TYPE Line
                    Matcher typeLine = TYPE_PATTERN.matcher(line);
                    if (typeLine.matches()) {
                        getMetric(metrics, typeLine.group("name")).setType(MetricType.getType(typeLine.group("type")));
                    }
                } else {
                    // Metric Line
                    Matcher metricLine = METRIC_PATTERN.matcher(line);
                    if (metricLine.matches()) {
                        String dimentions = metricLine.group("dimentions");
                        if (dimentions != null && dimentions.endsWith(",")) {
                            dimentions = dimentions.substring(0, dimentions.lastIndexOf(','));
                        }
                        List<KeyValue> dimensionsList = parseKeyValueList(dimentions);
                        getMetric(metrics, metricLine.group("name")).addValue(Objects.hash(dimentions), dimensionsList, metricLine.group("value"));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse prometheus metrics", e);
        }

        //metrics.forEach((key, value) -> {
        //System.out.println(key+" -> "+value);
        //});

        return new Metrics(metrics);
    }

    private static Metric getMetric(Map<String, Metric> metrics, String name) {
        Metric metric = metrics.get(name);
        if (metric == null) {
            metric = new Metric(name);
            metrics.put(name, metric);
        }

        return metric;
    }

    private static Map<String, String> parseKeyValueMap(String data) {
        Map<String, String> keyValues = new HashMap<>();
        Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(data);
        while (keyValueMatcher.find()) {
            keyValues.put(keyValueMatcher.group("key"), keyValueMatcher.group("value"));
        }

        return keyValues;
    }

    private static List<KeyValue> parseKeyValueList(String data) {
        List<KeyValue> keyValues = new ArrayList<>();
        Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(data);
        while (keyValueMatcher.find()) {
            keyValues.add(new KeyValue(keyValueMatcher.group("key"), keyValueMatcher.group("value")));
        }

        return keyValues;
    }
}
