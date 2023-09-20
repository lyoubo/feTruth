package org.datageneration.metrics;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.HashSet;
import java.util.Set;

public class TypeEntitySet extends Metric {

    private final ITypeBinding currentClass;

    public TypeEntitySet(ITypeBinding currentClass) {
        super();
        this.currentClass = currentClass;
        name = "TypeEntitySet";
    }

    @Override
    public void calculate() {
        Set<String> entitySet = new HashSet<>();
        if (currentClass == null) {
            result.put(name, entitySet);
            return;
        }
        for (IMethodBinding method : currentClass.getDeclaredMethods())
            if (!method.isDefaultConstructor())
                entitySet.add(currentClass.getQualifiedName() + "." + method.toString().strip());
        for (IVariableBinding field : currentClass.getDeclaredFields())
            entitySet.add(currentClass.getQualifiedName() + "." + field.toString().strip());
        result.put(name, entitySet);
    }
}
