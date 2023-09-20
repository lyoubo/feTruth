package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;

/**
 * AST visitor to find recursive calls to the method.
 */
public class RecursiveCallFinder extends AstNodeFinder {

    /**
     * The method binding
     */
    protected final IMethodBinding fBinding;

    /**
     * Creates a new recursive call finder.
     *
     * @param declaration the method declaration
     */
    public RecursiveCallFinder(final MethodDeclaration declaration) {
        Assert.isNotNull(declaration);
        fBinding = declaration.resolveBinding();
    }

    @Override
    public boolean visit(final MethodInvocation node) {
        Assert.isNotNull(node);
        final Expression expression = node.getExpression();
        final IMethodBinding binding = node.resolveMethodBinding();
        if (binding == null || !Modifier.isStatic(binding.getModifiers()) && binding.isEqualTo(fBinding) && (expression == null || expression instanceof ThisExpression)) {
            fStatus.merge(RefactoringStatus.createErrorStatus());
            fResult.add(node);
            return false;
        }
        return true;
    }
}
