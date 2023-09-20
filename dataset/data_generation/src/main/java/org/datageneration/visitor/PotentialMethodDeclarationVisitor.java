package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class PotentialMethodDeclarationVisitor extends ASTVisitor {

    private final List<MethodDeclaration> methods;

    public PotentialMethodDeclarationVisitor() {
        methods = new ArrayList<>();
    }

    public List<MethodDeclaration> getAllMethods() {
        return methods;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        IMethodBinding methodBinding = node.resolveBinding();
        if (methodBinding == null) return true;
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass == null) return true;
        if (!declaringClass.isAnonymous())
            methods.add(node);
        return true;
    }
}
