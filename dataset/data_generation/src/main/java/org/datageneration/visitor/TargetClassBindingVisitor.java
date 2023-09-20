package org.datageneration.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TargetClassBindingVisitor extends ASTVisitor {

    private final String targetClassName;
    private ITypeBinding targetClass;

    public TargetClassBindingVisitor(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public ITypeBinding getTargetClass() {
        return targetClass;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ITypeBinding typeBinding = node.resolveBinding();
        if (typeBinding == null) return true;
        if (typeBinding.getQualifiedName().equals(targetClassName))
            targetClass = typeBinding;
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        ITypeBinding typeBinding = node.resolveBinding();
        if (typeBinding == null) return true;
        if (typeBinding.getQualifiedName().equals(targetClassName))
            targetClass = typeBinding;
        return true;
    }
}
