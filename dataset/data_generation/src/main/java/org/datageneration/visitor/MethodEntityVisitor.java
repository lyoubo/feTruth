package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodEntityVisitor extends ASTVisitor {

    private final IMethodBinding methodBinding;
    private final Set<String> methodCallHashSet;
    private final List<String> methodCallList;
    private final Set<String> methodEntitySet;

    public MethodEntityVisitor(MethodDeclaration methodDeclaration) {
        methodBinding = methodDeclaration.resolveBinding();
        methodCallHashSet = new HashSet<>();
        methodCallList = new ArrayList<>();
        methodEntitySet = new HashSet<>();
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return visit(node.resolveMethodBinding());
    }

    private boolean visit(IMethodBinding methodBinding) {
        if (methodBinding == null) return true;
        IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
        if (methodDeclaration == null) return true;
        /**
         * recursive call method
         */
        if (methodDeclaration.equals(this.methodBinding)) return true;
        ITypeBinding declaringClass = methodDeclaration.getDeclaringClass();
        if (declaringClass == null || declaringClass.isAnonymous() || !declaringClass.isFromSource()) return true;
        String entityName = declaringClass.getQualifiedName() + "." + methodDeclaration.toString().strip();
        methodCallHashSet.add(entityName);
        methodCallList.add(entityName);
        methodEntitySet.add(entityName);
        return true;
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
        ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
        if (declaringClass == null || declaringClass.isAnonymous() || !declaringClass.isFromSource()) return true;
        String entityName = declaringClass.getQualifiedName() + "." + variableDeclaration.toString().strip();
        methodEntitySet.add(entityName);
        return true;
    }

    public List<String> getMethodCallList() {
        return methodCallList;
    }

    public Set<String> getMethodCallHashSet() {
        return methodCallHashSet;
    }

    public Set<String> getMethodEntitySet() {
        return methodEntitySet;
    }
}
