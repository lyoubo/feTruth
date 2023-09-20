package org.datageneration.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MoveStaticMembersProcessor {

    private IMethodBinding fSourceBinding;

    private ITypeBinding fDestinationType;

    public boolean checkInitialConditions(final MethodDeclaration declaration) {
        RefactoringStatus result = new RefactoringStatus();
        result.merge(checkIfCuBroken(declaration));
        result.merge(checkIfBindingBroken(declaration));
        result.merge(checkDeclaringType());
        if (isNull(declaration) || isEmpty(declaration) || isMain(declaration))
            result.merge(RefactoringStatus.createErrorStatus());
        return result.isOK();
    }

    private RefactoringStatus checkIfCuBroken(MethodDeclaration declaration) {
        CompilationUnit cu = (CompilationUnit) declaration.getRoot();
        if (cu == null) return RefactoringStatus.createErrorStatus();
        return null;
    }

    private RefactoringStatus checkIfBindingBroken(MethodDeclaration declaration) {
        fSourceBinding = declaration.resolveBinding();
        if (fSourceBinding == null) return RefactoringStatus.createErrorStatus();
        return null;
    }

    protected boolean isNull(MethodDeclaration declaration) {
        return declaration == null || declaration.getBody() == null;
    }

    protected static boolean isEmpty(MethodDeclaration method) {
        return method.getBody().statements().size() == 0;
    }

    protected boolean isMain(MethodDeclaration method) {
        return method.getName().toString().equals("main") && isStatic(method) && isPublic(method) &&
                method.getReturnType2().toString().equals("void");
    }

    protected boolean isStatic(MethodDeclaration method) {
        return Flags.isStatic(method.getModifiers());
    }

    protected boolean isPublic(MethodDeclaration method) {
        return Flags.isPublic(method.getModifiers());
    }

    private RefactoringStatus checkDeclaringType() {
        ITypeBinding declaringType = getDeclaringType();
        if (declaringType == null) {
            return RefactoringStatus.createErrorStatus();
        }
        return null;
    }

    public ITypeBinding getDeclaringType() {
        return fSourceBinding.getDeclaringClass();
    }

    public boolean checkFinalConditions(ITypeBinding fDestinationType) {
        this.fDestinationType = fDestinationType;
        RefactoringStatus result = new RefactoringStatus();
        result.merge(checkDestinationType());
        if (result.hasError())
            return result.isOK();
        result.merge(checkMembersInDestinationType());
        if (result.hasError())
            return result.isOK();
        result.merge(checkNativeMovedMethods());
        if (result.hasError())
            return result.isOK();
        return result.isOK();
    }

    private RefactoringStatus checkDestinationType() {
        if (fDestinationType == null) {
            return RefactoringStatus.createErrorStatus();
        }
        if (fDestinationType.equals(getDeclaringType())) {
            return RefactoringStatus.createErrorStatus();
        }
        RefactoringStatus result = new RefactoringStatus();
        if (fDestinationType.isInterface())
            result.merge(checkMoveToInterface());
        if (result.hasError())
            return result;
        if (!Flags.isStatic(fDestinationType.getModifiers()) && fDestinationType.getDeclaringClass() != null) {
            return RefactoringStatus.createErrorStatus();
        }
        return result;
    }

    private RefactoringStatus checkMoveToInterface() {
        RefactoringStatus result = new RefactoringStatus();
        boolean declaringIsInterface = getDeclaringType().isInterface();
        if (declaringIsInterface)
            return result;
        if (!canMoveToInterface()) {
            return RefactoringStatus.createErrorStatus();
        } else if (!Flags.isPublic(fSourceBinding.getModifiers())) {
            return RefactoringStatus.createErrorStatus();
        }
        return result;
    }

    private boolean canMoveToInterface() {
        int flags = fSourceBinding.getModifiers();
        return Flags.isStatic(flags);
    }

    public RefactoringStatus checkMembersInDestinationType() {
        RefactoringStatus result = new RefactoringStatus();
        checkMethodInType(result);
        return result;
    }

    private void checkMethodInType(RefactoringStatus result) {
        IMethodBinding[] destinationTypeMethods = fDestinationType.getDeclaredMethods();
        IMethodBinding found = findMethod(fSourceBinding, destinationTypeMethods);
        if (found != null) {
            result.merge(RefactoringStatus.createErrorStatus());
        } else {
            IMethodBinding similar = findMethod(fSourceBinding.getName(), fSourceBinding.getParameterTypes().length, fSourceBinding.isConstructor(), destinationTypeMethods);
            if (similar != null) {
                result.merge(RefactoringStatus.createErrorStatus());
            }
        }
    }

    public IMethodBinding findMethod(IMethodBinding method, IMethodBinding[] allMethods) {
        String name = method.getName();
        ITypeBinding[] paramTypes = method.getParameterTypes();
        boolean isConstructor = method.isConstructor();
        for (IMethodBinding m : allMethods) {
            if (isSameMethodSignature(name, paramTypes, isConstructor, m))
                return m;
        }
        return null;
    }

    public boolean isSameMethodSignature(String name, ITypeBinding[] paramTypes, boolean isConstructor, IMethodBinding curr) {
        if (isConstructor || name.equals(curr.getName())) {
            if (isConstructor == curr.isConstructor()) {
                ITypeBinding[] currParamTypes = curr.getParameterTypes();
                if (paramTypes.length == currParamTypes.length) {
                    for (int i = 0; i < paramTypes.length; i++) {
                        String t1 = paramTypes[i].getName();
                        String t2 = currParamTypes[i].getName();
                        if (!t1.equals(t2)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public IMethodBinding findMethod(String name, int parameters, boolean isConstructor, IMethodBinding[] methods) {
        for (int i = methods.length - 1; i >= 0; i--) {
            IMethodBinding curr = methods[i];
            if (name.equals(curr.getName())) {
                if (isConstructor == curr.isConstructor()) {
                    if (parameters == curr.getParameterTypes().length) {
                        return curr;
                    }
                }
            }
        }
        return null;
    }

    private RefactoringStatus checkNativeMovedMethods() {
        RefactoringStatus result = new RefactoringStatus();
        if (Flags.isNative(fSourceBinding.getModifiers()))
            return RefactoringStatus.createErrorStatus();
        return result;
    }
}
