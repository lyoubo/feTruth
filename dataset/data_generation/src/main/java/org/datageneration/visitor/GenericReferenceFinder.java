package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * AST visitor to find references to type variables or generic types.
 */
public class GenericReferenceFinder extends AstNodeFinder {

    /**
     * The type parameter binding keys
     */
    protected final Set<String> fBindings = new HashSet<>();

    /**
     * Creates a new generic reference finder.
     *
     * @param declaration the method declaration
     */
    public GenericReferenceFinder(final MethodDeclaration declaration) {
        Assert.isNotNull(declaration);
        ITypeBinding binding = null;
        TypeParameter parameter = null;
        for (final Iterator<TypeParameter> iterator = declaration.typeParameters().iterator(); iterator.hasNext(); ) {
            parameter = iterator.next();
            binding = parameter.resolveBinding();
            if (binding != null)
                fBindings.add(binding.getKey());
        }
    }

    /*
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
     */
    @Override
    public boolean visit(final SimpleName node) {
        Assert.isNotNull(node);
        final IBinding binding = node.resolveBinding();
        if (binding instanceof ITypeBinding) {
            final ITypeBinding type = (ITypeBinding) binding;
            if (!fBindings.contains(type.getKey()) && type.isTypeVariable()) {
                fResult.add(node);
                fStatus.merge(RefactoringStatus.createErrorStatus());
                return false;
            }
        }
        return true;
    }
}
