package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AST visitor to find 'this' references to enclosing instances.
 */
public class EnclosingInstanceReferenceFinder extends AstNodeFinder {

    /**
     * The list of enclosing types
     */
    private final List<ITypeBinding> fEnclosingTypes = new ArrayList<>(3);

    /**
     * Creates a new enclosing instance reference finder.
     *
     * @param binding the declaring type
     */
    public EnclosingInstanceReferenceFinder(final ITypeBinding binding) {
        Assert.isNotNull(binding);
        ITypeBinding declaring = binding.getDeclaringClass();
        while (declaring != null && !Modifier.isStatic(binding.getModifiers())) {
            fEnclosingTypes.add(declaring);
            declaring = declaring.getDeclaringClass();
        }
    }

    @Override
    public boolean visit(final SimpleName node) {
        Assert.isNotNull(node);
        final IBinding binding = node.resolveBinding();
        ITypeBinding declaring = null;
        if (binding instanceof IVariableBinding) {
            final IVariableBinding variable = (IVariableBinding) binding;
            if (Flags.isStatic(variable.getModifiers()))
                return false;
            declaring = variable.getDeclaringClass();
        } else if (binding instanceof IMethodBinding) {
            final IMethodBinding method = (IMethodBinding) binding;
            if (Flags.isStatic(method.getModifiers()))
                return false;
            declaring = method.getDeclaringClass();
        }
        if (declaring != null) {
            declaring = declaring.getTypeDeclaration();
            ITypeBinding enclosing = null;
            for (final Iterator<ITypeBinding> iterator = fEnclosingTypes.iterator(); iterator.hasNext(); ) {
                enclosing = iterator.next();
                if (enclosing.isEqualTo(declaring)) {
                    fStatus.merge(RefactoringStatus.createErrorStatus());
                    fResult.add(node);
                    break;
                }
            }
        }
        return false;
    }

    @Override
    public boolean visit(final ThisExpression node) {
        Assert.isNotNull(node);
        if (node.getQualifier() != null) {
            fStatus.merge(RefactoringStatus.createErrorStatus());
            fResult.add(node);
        }
        return false;
    }
}
