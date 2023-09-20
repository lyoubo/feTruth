package org.datageneration.metrics;

import org.datageneration.visitor.MethodEntityVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodEntitySet extends Metric {

    private final MethodDeclaration methodDeclaration;
    private Set<String> methodCallHashSet;
    private List<String> methodCallList;
    private Set<String> methodEntitySet;

    public MethodEntitySet(MethodDeclaration methodDeclaration) {
        super();
        this.methodDeclaration = methodDeclaration;
        name = "MethodEntitySet";
        methodCallHashSet = new HashSet<>();
        methodCallList = new ArrayList<>();
        methodEntitySet = new HashSet<>();
    }

    @Override
    public void calculate() {
        MethodEntityVisitor methodEntityVisitor = new MethodEntityVisitor(methodDeclaration);
        methodDeclaration.accept(methodEntityVisitor);
        methodCallHashSet = methodEntityVisitor.getMethodCallHashSet();
        methodCallList = methodEntityVisitor.getMethodCallList();
        methodEntitySet = methodEntityVisitor.getMethodEntitySet();
        result.put(name, methodEntitySet);
        result.put("MethodCallHashSet", methodCallHashSet);
        result.put("MethodCallList", methodCallList);
    }
}
