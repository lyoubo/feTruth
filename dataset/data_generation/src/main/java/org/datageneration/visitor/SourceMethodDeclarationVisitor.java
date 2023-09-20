package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class SourceMethodDeclarationVisitor extends ASTVisitor {

    private final String className;
    private final String methodName;
    private final List<MethodDeclaration> sourceMethods;

    public SourceMethodDeclarationVisitor(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        sourceMethods = new ArrayList<>();
    }

    public List<MethodDeclaration> getSourceMethods() {
        return sourceMethods;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        IMethodBinding methodBinding = node.resolveBinding();
        if (methodBinding == null) return true;
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass == null) return true;
        if (methodBinding.getName().equals(methodName) && declaringClass.getQualifiedName().equals(className))
            sourceMethods.add(node);
        return true;
    }
}
