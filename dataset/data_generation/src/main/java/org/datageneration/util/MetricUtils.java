package org.datageneration.util;

import java.util.Collection;
import java.util.HashSet;

public class MetricUtils {

    static public <E> Collection<E> getIntersection(Collection<E> a, Collection<E> b) {
        Collection<E> res = new HashSet<>(a);
        res.retainAll(b);
        return res;
    }

    static public <E> Collection<E> getUnion(Collection<E> a, Collection<E> b) {
        Collection<E> res = new HashSet<>();
        res.addAll(a);
        res.addAll(b);
        return res;
    }
}
