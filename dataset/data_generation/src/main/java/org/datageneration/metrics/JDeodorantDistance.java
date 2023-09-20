package org.datageneration.metrics;

import org.datageneration.util.MetricUtils;

import java.util.Collection;

public class JDeodorantDistance extends Metric {

    private final Collection<String> collection1;
    private final Collection<String> collection2;

    public JDeodorantDistance(Collection<String> collection1, Collection<String> collection2) {
        super();
        name = "dist";
        this.collection1 = collection1;
        this.collection2 = collection2;
    }

    @Override
    public void calculate() {
        double dist = 0.0;
        Collection<String> union = MetricUtils.getUnion(collection1, collection2);
        if (union.size() > 0) {
            Collection<String> intersection = MetricUtils.getIntersection(collection1, collection2);
            dist = 1 - intersection.size() * 1.0 / union.size();
        }
        result.put(name, dist);
    }
}
