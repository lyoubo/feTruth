package org.datageneration.util;

import org.apache.commons.lang3.StringUtils;
import org.datageneration.visitor.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class MoveInstanceMethodProcessor {

    /**
     * The method to move
     */
    private IMethodBinding fMethod;

    /**
     * The name of the new method to generate
     */
    private String fMethodName;

    /**
     * The possible targets
     */
    private IVariableBinding[] fPossibleTargets = new IVariableBinding[0];

    /**
     * The candidate targets
     */
    private IVariableBinding[] fCandidateTargets = new IVariableBinding[0];

    /**
     * The new target
     */
    private IVariableBinding fTarget = null;

    /**
     * The name of the new target
     */
    private String fTargetName;

    /**
     * The target type
     */
    private ITypeBinding fTargetType = null;

    /**
     * Does the move method need a target node?
     */
    private boolean fTargetNode = true;

    public boolean checkInitialConditions(final MethodDeclaration declaration) {
        if (declaration == null) return false;
        final RefactoringStatus status = new RefactoringStatus();
        status.merge(checkIfCuBroken(declaration));
        status.merge(checkIfBindingBroken(declaration));
        if (!status.hasError()) {
            checkMethodDeclaration(status);
            checkMethodDeclaration(declaration, status);
            /*if (status.isOK()) {
                checkGenericTypes(declaration, status);
                checkMethodBody(declaration, status);
                checkPossibleTargets(declaration, status);
            }*/
        }
        return status.isOK();
    }

    public boolean checkInitialConditions2(final MethodDeclaration declaration) {
        final RefactoringStatus status = new RefactoringStatus();
        checkGenericTypes(declaration, status);
        checkMethodBody(declaration, status);
        checkPossibleTargets(declaration, status);
        return status.isOK();
    }

    private RefactoringStatus checkIfCuBroken(MethodDeclaration declaration) {
        CompilationUnit cu = (CompilationUnit) declaration.getRoot();
        if (cu == null) return RefactoringStatus.createErrorStatus();
        return new RefactoringStatus();
    }

    private RefactoringStatus checkIfBindingBroken(MethodDeclaration declaration) {
        fMethod = declaration.resolveBinding();
        if (fMethod == null) return RefactoringStatus.createErrorStatus();
        fMethodName = fMethod.getName();
        return new RefactoringStatus();
    }

    protected void checkMethodDeclaration(final RefactoringStatus status) {
        Assert.isNotNull(status);
        final int flags = fMethod.getModifiers();
        if (Flags.isStatic(flags))
            status.merge(RefactoringStatus.createErrorStatus());
        if (Flags.isAbstract(flags))
            status.merge(RefactoringStatus.createErrorStatus());
        if (Flags.isNative(flags))
            status.merge(RefactoringStatus.createErrorStatus());
        if (Flags.isSynchronized(flags))
            status.merge(RefactoringStatus.createErrorStatus());
        if (fMethod.isConstructor())
            status.merge(RefactoringStatus.createErrorStatus());
        if (fMethod.getDeclaringClass().isAnnotation())
            status.merge(RefactoringStatus.createErrorStatus());
        else if (fMethod.getDeclaringClass().isInterface() && !Flags.isDefaultMethod(flags))
            status.merge(RefactoringStatus.createErrorStatus());
    }

    /**
     * 新增的一些与feature envy smells相关的move method限制
     */
    protected void checkMethodDeclaration(final MethodDeclaration declaration, final RefactoringStatus status) {
        if (isNull(declaration) || isEmpty(declaration) || isTest(declaration) || isOverride())
            status.merge(RefactoringStatus.createErrorStatus());
    }

    protected void checkGenericTypes(final MethodDeclaration declaration, final RefactoringStatus status) {
        Assert.isNotNull(declaration);
        Assert.isNotNull(status);
        final AstNodeFinder finder = new GenericReferenceFinder(declaration);
        declaration.accept(finder);
        if (!finder.getStatus().isOK())
            status.merge(finder.getStatus());
    }

    protected List<ITypeBinding> getSuperClasses(IMethodBinding method) {
        List<ITypeBinding> superClasses = new ArrayList<>();
        if (method == null)
            return superClasses;
        ITypeBinding declaringClass = method.getDeclaringClass();
        if (declaringClass == null)
            return superClasses;
        ITypeBinding superclass = declaringClass.getSuperclass();
        while (superclass != null) {
            superClasses.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return superClasses;
    }

    protected void checkMethodBody(final MethodDeclaration declaration, final RefactoringStatus status) {
        Assert.isNotNull(declaration);
        Assert.isNotNull(status);
        final List<ITypeBinding> superClasses = getSuperClasses(fMethod);
        AstNodeFinder finder = new SuperReferenceFinder(superClasses);
        declaration.accept(finder);
        if (!finder.getStatus().isOK())
            status.merge(finder.getStatus());
        finder = null;
        final IMethodBinding binding = declaration.resolveBinding();
        if (binding != null) {
            final ITypeBinding declaring = binding.getDeclaringClass();
            if (declaring != null)
                finder = new EnclosingInstanceReferenceFinder(declaring);
        }
        if (finder != null) {
            declaration.accept(finder);
            if (!finder.getStatus().isOK())
                status.merge(finder.getStatus());
            finder = new RecursiveCallFinder(declaration);
            declaration.accept(finder);
            if (!finder.getStatus().isOK())
                status.merge(finder.getStatus());
        }
    }

    protected void checkPossibleTargets(final MethodDeclaration declaration, final RefactoringStatus status) {
        Assert.isNotNull(declaration);
        Assert.isNotNull(status);
        if (computeTargetCategories(declaration).length < 1)
            status.merge(RefactoringStatus.createErrorStatus());
    }

    protected boolean isNull(MethodDeclaration declaration) {
        return declaration == null || declaration.getBody() == null;
    }

    protected static boolean isEmpty(MethodDeclaration method) {
        return method.getBody().statements().size() == 0;
    }

    protected boolean isTest(MethodDeclaration declaration) {
        List<IExtendedModifier> modifiers = declaration.modifiers();
        for (IExtendedModifier modifier : modifiers) {
            if (modifier.isAnnotation()) {
                Annotation annotation = (Annotation) modifier;
                if (annotation.getTypeName().getFullyQualifiedName().equals("Test"))
                    return true;
            }
        }
        ITypeBinding declaringClass = fMethod.getDeclaringClass();
        if (declaringClass != null) {
            IMethodBinding[] declaredMethods = declaringClass.getDeclaredMethods();
            for (IMethodBinding declaredMethod : declaredMethods) {
                IAnnotationBinding[] annotations = declaredMethod.getAnnotations();
                for (IAnnotationBinding annotation : annotations) {
                    if (annotation.getName().equals("Test"))
                        return true;
                }
            }
        }
        return false;
    }

    protected boolean isOverride() {
        final List<ITypeBinding> superClasses = getSuperClasses(fMethod);
        IAnnotationBinding[] annotations = fMethod.getAnnotations();
        for (IAnnotationBinding annotation : annotations) {
            if (annotation.getName().equals("Override"))
                return true;
        }
        if (fMethod.getDeclaringClass() == null)
            return false;
        for (ITypeBinding anInterface : fMethod.getDeclaringClass().getInterfaces()) {
            while (anInterface != null) {
                superClasses.add(anInterface);
                anInterface = anInterface.getSuperclass();
            }
        }
        for (ITypeBinding typeBinding : superClasses) {
            for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
                if (couldMethodOverride(fMethod, declaredMethod) &&
                        areParametersEqual(fMethod, declaredMethod)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean couldMethodOverride(IMethodBinding binding1, IMethodBinding binding2) {
        if (!StringUtils.isEmpty(binding1.getName()) && !binding1.getName().equals(binding2.getName())) {
            return false;
        } else if (binding1 != binding2 && !Flags.isStatic(binding1.getModifiers()) && !Flags.isStatic(binding2.getModifiers())) {
            if (Flags.isPrivate(binding2.getModifiers())) {
                return false;
            } else if (Flags.isDefaultMethod(binding2.getModifiers()) && binding1.getDeclaringClass().getPackage() != binding2.getDeclaringClass().getPackage()) {
                return false;
            } else {
                if (!Flags.isPublic(binding1.getModifiers())) {
                    if (Flags.isPublic(binding2.getModifiers())) {
                        return false;
                    }
                    return !Flags.isProtected(binding2.getModifiers()) || Flags.isProtected(binding1.getModifiers());
                }
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean areParametersEqual(IMethodBinding binding1, IMethodBinding binding2) {
        ITypeBinding[] parameterTypes1 = binding1.getParameterTypes();
        ITypeBinding[] parameterTypes2 = binding2.getParameterTypes();
        if (parameterTypes1.length != parameterTypes2.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes1.length; i++) {
            if (!areTypesEqual(parameterTypes1[i], parameterTypes2[i])) {
                return false;
            }
        }
        return true;
    }

    static boolean areTypesEqual(ITypeBinding binding1, ITypeBinding binding2) {
        String name1 = binding1.getName();
        String name2 = binding2.getName();
        if (StringUtils.isEmpty(name1) && StringUtils.isEmpty(name2)) {
            return true;
        }
        if (StringUtils.isEmpty(name1) || StringUtils.isEmpty(name2)) {
            return false;
        }
        return name1.equals(name2);
    }

    protected IVariableBinding[] computeTargetCategories(final MethodDeclaration declaration) {
        Assert.isNotNull(declaration);
        if (fPossibleTargets.length == 0 || fCandidateTargets.length == 0) {
            final List<IVariableBinding> possibleTargets = new ArrayList<>(16);
            final List<IVariableBinding> candidateTargets = new ArrayList<>(16);
            final IMethodBinding method = declaration.resolveBinding();
            if (method != null) {
                final ITypeBinding declaring = method.getDeclaringClass();
                ITypeBinding binding = null;
                for (IVariableBinding binding2 : getArgumentBindings(declaration)) {
                    binding = binding2.getType();
                    if ((binding.isClass() || binding.isEnum() || is18OrHigherInterface(binding)) && binding.isFromSource()) {
                        possibleTargets.add(binding2);
                        candidateTargets.add(binding2);
                    }
                }
                final ReadyOnlyFieldFinder visitor = new ReadyOnlyFieldFinder(declaring);
                declaration.accept(visitor);
                for (IVariableBinding binding2 : visitor.getReadOnlyFields()) {
                    binding = binding2.getType();
                    if ((binding.isClass() || is18OrHigherInterface(binding)) && binding.isFromSource())
                        possibleTargets.add(binding2);
                }
                for (IVariableBinding binding2 : visitor.getDeclaredFields()) {
                    binding = binding2.getType();
                    if ((binding.isClass() || is18OrHigherInterface(binding)) && binding.isFromSource())
                        candidateTargets.add(binding2);
                }
                /** newly added variable declaration */
                final VariableDeclarationFinder visitor2 = new VariableDeclarationFinder();
                declaration.accept(visitor2);
                for (IVariableBinding binding2 : visitor2.getDeclaredVariables()) {
                    binding = binding2.getType();
                    if ((binding.isClass() || is18OrHigherInterface(binding)) && binding.isFromSource()) {
                        possibleTargets.add(binding2);
                        candidateTargets.add(binding2);
                    }
                }
            }
            fPossibleTargets = new IVariableBinding[possibleTargets.size()];
            possibleTargets.toArray(fPossibleTargets);
            fCandidateTargets = new IVariableBinding[candidateTargets.size()];
            candidateTargets.toArray(fCandidateTargets);
        }
        return fPossibleTargets;
    }

    private boolean is18OrHigherInterface(ITypeBinding binding) {
        if (!binding.isInterface() || binding.isAnnotation())
            return false;
        return true;
    }

    private static IVariableBinding[] getArgumentBindings(MethodDeclaration declaration) {
        final List<IVariableBinding> parameters = new ArrayList<>(declaration.parameters().size());
        for (VariableDeclaration variable : (List<VariableDeclaration>) declaration.parameters()) {
            IVariableBinding binding = variable.resolveBinding();
            if (binding == null)
                return new IVariableBinding[0];
            parameters.add(binding);
        }
        final IVariableBinding[] result = new IVariableBinding[parameters.size()];
        parameters.toArray(result);
        return result;
    }

    public boolean checkFinalConditions(final MethodDeclaration declaration) {
        Assert.isNotNull(fTarget);
        final RefactoringStatus status = new RefactoringStatus();
        status.merge(checkIfCuBroken(declaration));
        if (!status.hasError()) {
            checkGenericTarget(status);
            if (status.isOK()) {
                final ITypeBinding type = fTarget.getType();
                if (type != null) {
                    checkConflictingTarget(declaration, status);
                    checkConflictingMethod(status);
                } else
                    status.merge(RefactoringStatus.createErrorStatus());
            }
        }
        return status.isOK();
    }

    protected void checkGenericTarget(final RefactoringStatus status) {
        Assert.isNotNull(status);
        final ITypeBinding binding = fTarget.getType();
        if (binding == null || binding.isTypeVariable())
            status.merge(RefactoringStatus.createErrorStatus());
    }

    protected void checkConflictingTarget(final MethodDeclaration declaration, final RefactoringStatus status) {
        Assert.isNotNull(status);
        VariableDeclaration variable = null;
        final List<SingleVariableDeclaration> parameters = declaration.parameters();
        for (SingleVariableDeclaration singleVariableDeclaration : parameters) {
            variable = singleVariableDeclaration;
            if (fTargetName.equals(variable.getName().getIdentifier())) {
                status.merge(RefactoringStatus.createErrorStatus());
                break;
            }
        }
    }

    protected void checkConflictingMethod(final RefactoringStatus status) {
        Assert.isNotNull(status);
        final IMethodBinding[] methods = fTargetType.getDeclaredMethods();
        int newParamCount = fMethod.getParameterTypes().length;
        if (!fTarget.isField())
            newParamCount--; // moving to a parameter
        if (needsTargetNode())
            newParamCount++; // will add a parameter for the old 'this'
        for (IMethodBinding method : methods) {
            if (method.getName().equals(fMethodName) && method.getParameterTypes().length == newParamCount)
                status.merge(RefactoringStatus.createErrorStatus());
        }
        if (fMethodName.equals(fTargetType.getName()))
            status.merge(RefactoringStatus.createErrorStatus());
    }

    public boolean needsTargetNode() {
        return fTargetNode;
    }

    public void setTarget(final MethodDeclaration declaration, final IVariableBinding target) {
        Assert.isNotNull(target);
        fTarget = target;
        fTargetType = target.getType().getTypeDeclaration();
        fTargetName = fTargetType.getName();
        if (declaration != null) {
            final AstNodeFinder finder = new ThisReferenceFinder(fTarget);
            declaration.accept(finder);
            fTargetNode = !finder.getResult().isEmpty();
            return;
        }
        fTargetNode = true;
    }

    public IVariableBinding[] getPossibleTargets() {
        return fPossibleTargets;
    }
}
