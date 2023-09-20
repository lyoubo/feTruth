package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

import java.util.Set;

public interface ReferenceBasedRefactoring {
    Set<AbstractCodeMapping> getReferences();
}
