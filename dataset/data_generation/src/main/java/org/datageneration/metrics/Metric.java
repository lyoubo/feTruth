package org.datageneration.metrics;

import java.util.HashMap;
import java.util.Map;

public abstract class Metric {

    Map<String, Object> result;
    String name;
    String sourceCode;

    public Metric() {
        result = new HashMap<>();
        name = null;
        sourceCode = null;
    }

    public abstract void calculate();

    public Map<String, Object> getMetrics() {
        return result;
    }

    public String getName() {
        return name;
    }
}
