package org.methodinvocation.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodInvocationVisitor extends ASTVisitor {

    private final String className;
    private final String methodName;
    private final String paramTypes;
    private final List<String> invokedMethods;

    public MethodInvocationVisitor(String className, String methodName, String paramTypes) {
        this.className = className;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        invokedMethods = new LinkedList<>();
    }

    public List<String> getInvokedMethods() {
        return invokedMethods;
    }

    public void clear() {
        invokedMethods.clear();
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return visit(node.resolveMethodBinding());
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        return visit(node.resolveMethodBinding());
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        return visit(node.resolveMethodBinding());
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        return visit(node.resolveMethodBinding());
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        return visit(node.resolveMethodBinding());
    }

    private boolean visit(IMethodBinding methodBinding) {
        if (methodBinding == null) return true;
        IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
        if (methodDeclaration == null) return true;
        ITypeBinding declaringClass = methodDeclaration.getDeclaringClass();
        if (declaringClass == null) return true;
        ITypeBinding[] parameterTypes = methodDeclaration.getParameterTypes();
        String params = Arrays.stream(parameterTypes).map(ITypeBinding::getName).collect(Collectors.joining(","));
        if (methodDeclaration.getName().equals(methodName) && params.equals(paramTypes) && declaringClass.getQualifiedName().equals(className))
            invokedMethods.add("");
        return true;
    }
}
