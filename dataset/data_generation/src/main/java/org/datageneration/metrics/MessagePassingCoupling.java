package org.datageneration.metrics;

import java.util.Collection;

public class MessagePassingCoupling extends Metric {

    private final Collection<String> entitySet1;
    private final Collection<String> entitySet2;

    public MessagePassingCoupling(Collection<String> entitySet1, Collection<String> entitySet2) {
        super();
        name = "mcmc";
        this.entitySet1 = entitySet1;
        this.entitySet2 = entitySet2;
    }

    @Override
    public void calculate() {
        int mcmc = 0;
        double value = 0.0;
        for (String element : entitySet1) {
            if (entitySet2.contains(element)) {
                mcmc++;
            }
        }
        if (entitySet1.size() > 0)
            value = mcmc * 1.0 / entitySet1.size();
        result.put(name, value);
    }
}
