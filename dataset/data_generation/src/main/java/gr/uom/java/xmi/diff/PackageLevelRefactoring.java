package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;

public interface PackageLevelRefactoring extends Refactoring {
    public RenamePattern getRenamePattern();

    public UMLClass getOriginalClass();

    public UMLClass getMovedClass();

    public String getOriginalClassName();

    public String getMovedClassName();
}
