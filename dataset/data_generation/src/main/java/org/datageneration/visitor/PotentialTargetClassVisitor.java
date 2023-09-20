package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class PotentialTargetClassVisitor extends ASTVisitor {

    private final ITypeBinding currentClass;
    private final Set<ITypeBinding> targetClasses;

    public PotentialTargetClassVisitor(ITypeBinding currentClass) {
        this.currentClass = currentClass;
        targetClasses = new LinkedHashSet<>();
    }

    public Set<ITypeBinding> getTargetClasses() {
        return targetClasses;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return visit(node.resolveMethodBinding());
    }

    private boolean visit(IMethodBinding methodBinding) {
        if (methodBinding == null) return true;
        IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
        if (methodDeclaration == null) return true;
        return visit(methodDeclaration.getDeclaringClass());
    }

    @Override
    public boolean visit(FieldAccess node) {
        IVariableBinding variableBinding = node.resolveFieldBinding();
        if (variableBinding == null) return true;
        return visit(variableBinding);
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding iBinding = node.resolveBinding();
        if (iBinding == null) return true;
        if (iBinding instanceof IVariableBinding) {
            IVariableBinding variableBinding = (IVariableBinding) iBinding;
            if (variableBinding.isField() || variableBinding.isEnumConstant())
                return visit(variableBinding);
        }
        return true;
    }

    private boolean visit(IVariableBinding variableBinding) {
        IVariableBinding variableDeclaration = variableBinding.getVariableDeclaration();
        if (variableDeclaration == null) return true;
        return visit(variableDeclaration.getDeclaringClass());
    }

    private boolean visit(ITypeBinding declaringClass) {
        if (declaringClass == null || declaringClass.isTypeVariable() || declaringClass.getQualifiedName().equals(currentClass.getQualifiedName()))
            return true;
        if ((declaringClass.isClass() || declaringClass.isEnum() || is18OrHigherInterface(declaringClass)) &&
                !declaringClass.isAnonymous() && declaringClass.isFromSource())
            targetClasses.add(declaringClass);
        return true;
    }

    private boolean is18OrHigherInterface(ITypeBinding binding) {
        if (!binding.isInterface() || binding.isAnnotation())
            return false;
        return true;
    }
}
