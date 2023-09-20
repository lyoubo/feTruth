package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * AST visitor to find 'super' references.
 */
public class SuperReferenceFinder extends AstNodeFinder {

    private List<ITypeBinding> superClasses;

    public SuperReferenceFinder(List<ITypeBinding> superClasses) {
        this.superClasses = superClasses;
    }

    @Override
    public boolean visit(final AnnotationTypeDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(final AnonymousClassDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(final EnumDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(final SuperFieldAccess node) {
        fStatus.merge(RefactoringStatus.createErrorStatus());
        fResult.add(node);
        return false;
    }

    @Override
    public boolean visit(final SuperMethodInvocation node) {
        Assert.isNotNull(node);
        fStatus.merge(RefactoringStatus.createErrorStatus());
        fResult.add(node);
        return false;
    }

    @Override
    public boolean visit(final SuperMethodReference node) {
        Assert.isNotNull(node);
        fStatus.merge(RefactoringStatus.createErrorStatus());
        fResult.add(node);
        return false;
    }

    @Override
    public boolean visit(final TypeDeclaration node) {
        return false;
    }

    public boolean visit(MethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding == null) return true;
        IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
        if (methodDeclaration == null) return true;
        ITypeBinding typeBinding = methodDeclaration.getDeclaringClass();
        if (typeBinding == null) return true;
        if (superClasses.contains(typeBinding)) {
            fStatus.merge(RefactoringStatus.createErrorStatus());
            fResult.add(node);
            return false;
        }
        return true;
    }

    public boolean visit(FieldAccess node) {
        IVariableBinding variableBinding = node.resolveFieldBinding();
        if (variableBinding == null) return true;
        IVariableBinding variableDeclaration = variableBinding.getVariableDeclaration();
        if (variableDeclaration == null) return true;
        ITypeBinding typeBinding = variableDeclaration.getDeclaringClass();
        if (typeBinding == null) return true;
        if (superClasses.contains(typeBinding)) {
            fStatus.merge(RefactoringStatus.createErrorStatus());
            fResult.add(node);
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding iBinding = node.resolveBinding();
        if (iBinding == null) return true;
        if (iBinding instanceof IVariableBinding) {
            IVariableBinding variableBinding = (IVariableBinding) node.resolveBinding();
            if (variableBinding == null) return true;
            IVariableBinding variableDeclaration = variableBinding.getVariableDeclaration();
            if (variableDeclaration == null) return true;
            if (variableDeclaration.isField()) {
                ITypeBinding typeBinding = variableDeclaration.getDeclaringClass();
                if (typeBinding == null)
                    return true;
                if (superClasses.contains(typeBinding)) {
                    fStatus.merge(RefactoringStatus.createErrorStatus());
                    fResult.add(node);
                    return false;
                }
            }
        }
        return true;
    }
}
