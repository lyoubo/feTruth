package org.methodinvocation.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class NodeDeclarationVisitor extends ASTVisitor {

    private final List<Initializer> initializers;
    private final List<VariableDeclarationFragment> fragmentDeclarations;
    private final List<MethodDeclaration> methodDeclarations;

    public NodeDeclarationVisitor() {
        initializers = new ArrayList<>();
        fragmentDeclarations = new ArrayList<>();
        methodDeclarations = new ArrayList<>();
    }

    public List<Initializer> getInitializers() {
        return initializers;
    }

    public List<VariableDeclarationFragment> getFragmentDeclarations() {
        return fragmentDeclarations;
    }

    public List<MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    @Override
    public boolean visit(Initializer node) {
        ASTNode parent = node.getParent();
        if (parent instanceof AbstractTypeDeclaration) {
            ITypeBinding typeBinding = ((AbstractTypeDeclaration) parent).resolveBinding();
            if (typeBinding == null) return false;
            initializers.add(node);
        }
        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        ASTNode parent = node.getParent();
        if (parent instanceof AbstractTypeDeclaration) {
            ITypeBinding typeBinding = ((AbstractTypeDeclaration) parent).resolveBinding();
            if (typeBinding == null) return false;
            List<VariableDeclarationFragment> fragments = node.fragments();
            for (VariableDeclarationFragment fragment : fragments) {
                IVariableBinding variableBinding = fragment.resolveBinding();
                if (variableBinding != null)
                    fragmentDeclarations.add(fragment);
            }
        }
        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        ASTNode parent = node.getParent();
        if (parent instanceof AbstractTypeDeclaration) {
            ITypeBinding typeBinding = ((AbstractTypeDeclaration) parent).resolveBinding();
            if (typeBinding == null) return false;
            IMethodBinding methodBinding = node.resolveBinding();
            if (methodBinding == null) return false;
            methodDeclarations.add(node);
        }
        return false;
    }
}
