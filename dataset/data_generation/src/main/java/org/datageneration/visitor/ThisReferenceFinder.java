package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;


public class ThisReferenceFinder extends AstNodeFinder {

    protected final IVariableBinding fTarget;

    public ThisReferenceFinder(IVariableBinding fTarget) {
        this.fTarget = fTarget;
    }

    @Override
    public boolean visit(final MethodInvocation node) {
        Assert.isNotNull(node);
        final IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null && !isStatic(binding) && node.getExpression() == null) {
            fResult.add(node);
            fStatus.merge(RefactoringStatus.createErrorStatus());
        }
        return true;
    }

    @Override
    public boolean visit(final SimpleName node) {
        Assert.isNotNull(node);
        if (isFieldAccess(node) && !isTargetAccess(node)) {
            fResult.add(node);
            fStatus.merge(RefactoringStatus.createErrorStatus());
        }
        return false;
    }

    @Override
    public boolean visit(final ThisExpression node) {
        Assert.isNotNull(node);
        fResult.add(node);
        fStatus.merge(RefactoringStatus.createErrorStatus());
        return false;
    }

    public static boolean isStatic(IMethodBinding methodBinding) {
        return Modifier.isStatic(methodBinding.getModifiers());
    }

    /**
     * Is the specified name a field access?
     *
     * @param name the name to check
     * @return <code>true</code> if this name is a field access,
     * <code>false</code> otherwise
     */
    protected boolean isFieldAccess(final SimpleName name) {
        Assert.isNotNull(name);
        final IBinding binding = name.resolveBinding();
        if (!(binding instanceof IVariableBinding))
            return false;
        final IVariableBinding variable = (IVariableBinding) binding;
        if (!variable.isField())
            return false;
        if ("length".equals(name.getIdentifier())) { //$NON-NLS-1$
            final ASTNode parent = name.getParent();
            if (parent instanceof QualifiedName) {
                final QualifiedName qualified = (QualifiedName) parent;
                final ITypeBinding type = qualified.getQualifier().resolveTypeBinding();
                if (type != null && type.isArray())
                    return false;
            }
        }
        return !Modifier.isStatic(variable.getModifiers());
    }

    /**
     * Is the specified name a target access?
     *
     * @param name the name to check
     * @return <code>true</code> if this name is a target access,
     * <code>false</code> otherwise
     */
    protected boolean isTargetAccess(final Name name) {
        Assert.isNotNull(name);
        final IBinding binding = name.resolveBinding();
        if (fTarget.isEqualTo(binding))
            return true;
        if (name.getParent() instanceof FieldAccess) {
            final FieldAccess access = (FieldAccess) name.getParent();
            final Expression expression = access.getExpression();
            if (expression instanceof Name)
                return isTargetAccess((Name) expression);
        } else if (name instanceof QualifiedName) {
            final QualifiedName qualified = (QualifiedName) name;
            if (qualified.getQualifier() != null)
                return isTargetAccess(qualified.getQualifier());
        }
        return false;
    }
}
