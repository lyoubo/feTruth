package org.datageneration.visitor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class ReadyOnlyFieldFinder extends ASTVisitor {

    /**
     * The list of found bindings
     */
    protected final List<IVariableBinding> fBindings = new LinkedList<>();
    /**
     * The keys of the found binding keys
     */
    protected final Set<String> fFound = new HashSet<>();
    /**
     * The keys of the written binding keys
     */
    protected final Set<String> fWritten = new HashSet<>();

    /**
     * Creates a new read only field finder.
     *
     * @param binding The declaring class of the method declaring to find fields
     *                for
     */
    public ReadyOnlyFieldFinder(final ITypeBinding binding) {
        for (IVariableBinding variable : binding.getDeclaredFields()) {
            if (!variable.isSynthetic() && !fFound.contains(variable.getKey())) {
                fFound.add(variable.getKey());
                fBindings.add(variable);
            }
        }
    }

    /**
     * Returns the field binding associated with this expression.
     *
     * @param expression the expression to get the field binding for
     * @return the field binding, if the expression denotes a field access
     * or a field name, <code>null</code> otherwise
     */
    protected static IVariableBinding getFieldBinding(Expression expression) {
        if (expression instanceof FieldAccess)
            return (IVariableBinding) ((FieldAccess) expression).getName().resolveBinding();
        if (expression instanceof Name) {
            final IBinding binding = ((Name) expression).resolveBinding();
            if (binding instanceof IVariableBinding) {
                final IVariableBinding variable = (IVariableBinding) binding;
                if (variable.isField())
                    return variable;
            }
        }
        return null;
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
     * Is the specified name a qualified entity, e.g. preceded by 'this',
     * 'super' or part of a method invocation?
     *
     * @param name the name to check
     * @return <code>true</code> if this entity is qualified,
     * <code>false</code> otherwise
     */
    protected static boolean isQualifiedEntity(final Name name) {
        final ASTNode parent = name.getParent();
        if (parent instanceof QualifiedName && ((QualifiedName) parent).getName().equals(name) || parent instanceof FieldAccess && ((FieldAccess) parent).getName().equals(name) || parent instanceof SuperFieldAccess)
            return true;
        else if (parent instanceof MethodInvocation) {
            final MethodInvocation invocation = (MethodInvocation) parent;
            return invocation.getExpression() != null && invocation.getName().equals(name);
        }
        return false;
    }

    /**
     * Returns all fields of the declaring class plus the ones references in
     * the visited method declaration.
     *
     * @return all fields of the declaring class plus the references ones
     */
    public final IVariableBinding[] getDeclaredFields() {
        final IVariableBinding[] result = new IVariableBinding[fBindings.size()];
        fBindings.toArray(result);
        return result;
    }

    /**
     * Returns all fields of the declaring class which are not written by
     * the visited method declaration.
     *
     * @return all fields which are not written
     */
    public final IVariableBinding[] getReadOnlyFields() {
        IVariableBinding binding;
        final List<IVariableBinding> list = new LinkedList<>(fBindings);
        for (final Iterator<IVariableBinding> iterator = list.iterator(); iterator.hasNext(); ) {
            binding = iterator.next();
            if (fWritten.contains(binding.getKey()))
                iterator.remove();
        }
        final IVariableBinding[] result = new IVariableBinding[list.size()];
        list.toArray(result);
        return result;
    }

    @Override
    public final boolean visit(final Assignment node) {
        final IVariableBinding binding = getFieldBinding(node.getLeftHandSide());
        if (binding != null)
            fWritten.add(binding.getKey());
        return true;
    }

    @Override
    public final boolean visit(final FieldAccess node) {
        if (node.getExpression() instanceof ThisExpression) {
            final IVariableBinding binding = (IVariableBinding) node.getName().resolveBinding();
            if (binding != null) {
                final String key = binding.getKey();
                if (!fFound.contains(key)) {
                    fFound.add(key);
                    fBindings.add(binding);
                }
            }
        }
        return true;
    }

    @Override
    public final boolean visit(final PostfixExpression node) {
        final IVariableBinding binding = getFieldBinding(node.getOperand());
        if (binding != null)
            fWritten.add(binding.getKey());
        return true;
    }

    @Override
    public final boolean visit(final PrefixExpression node) {
        final IVariableBinding binding = getFieldBinding(node.getOperand());
        if (binding != null)
            fWritten.add(binding.getKey());
        return false;
    }

    @Override
    public final boolean visit(final SimpleName node) {
        final IBinding binding = node.resolveBinding();
        if (binding != null)
            if (isFieldAccess(node) && !isQualifiedEntity(node)) {
                final IVariableBinding variable = (IVariableBinding) binding;
                final String key = variable.getKey();
                if (!fFound.contains(key)) {
                    fFound.add(key);
                    fBindings.add(variable);
                }
            }
        return false;
    }
}
