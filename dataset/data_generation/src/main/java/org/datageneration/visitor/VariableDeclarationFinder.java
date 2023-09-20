package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.LinkedList;
import java.util.List;

public class VariableDeclarationFinder extends ASTVisitor {

    protected final List<IVariableBinding> fBindings = new LinkedList<>();

    public final IVariableBinding[] getDeclaredVariables() {
        final IVariableBinding[] result = new IVariableBinding[fBindings.size()];
        fBindings.toArray(result);
        return result;
    }

    @Override
    public final boolean visit(final VariableDeclarationFragment node) {
        final IVariableBinding binding = node.resolveBinding();
        if (binding != null)
            fBindings.add(binding);
        return true;
    }
}
