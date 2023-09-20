package org.datageneration.metrics;

import java.util.Collection;

public class CouplingBetweenObject extends Metric {

    private final Collection<String> entitySet1;
    private final Collection<String> entitySet2;

    public CouplingBetweenObject(Collection<String> entitySet1, Collection<String> entitySet2) {
        super();
        name = "cbmc";
        this.entitySet1 = entitySet1;
        this.entitySet2 = entitySet2;
    }

    @Override
    public void calculate() {
        int cbmc = 0;
        for (String element : entitySet1)
            if (entitySet2.contains(element)) {
                cbmc++;
            }
        double value = 0.0;
        if (entitySet1.size() > 0)
            value = cbmc * 1.0 / entitySet1.size();
        result.put(name, value);
    }
}
