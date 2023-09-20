package org.datageneration.candidate;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SmellCandidate {

    Map<String, Object> features;
    boolean valid;

    public SmellCandidate() {
        features = new LinkedHashMap<>();
        valid = false;
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public abstract boolean isValidCandidate();
}
