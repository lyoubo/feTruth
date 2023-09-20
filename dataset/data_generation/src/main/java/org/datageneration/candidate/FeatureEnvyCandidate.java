package org.datageneration.candidate;

import org.apache.commons.lang3.StringUtils;
import org.datageneration.metrics.*;
import org.datageneration.util.GetterSetterUtils;
import org.datageneration.util.MoveInstanceMethodProcessor;
import org.datageneration.util.MoveStaticMembersProcessor;
import org.datageneration.visitor.PotentialTargetClassVisitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.*;
import java.util.stream.Collectors;

public class FeatureEnvyCandidate extends SmellCandidate {

    private MethodDeclaration methodDeclaration;
    private ITypeBinding sourceClassBinding;
    private Set<String> methodCallSet;
    private List<String> methodCallList;
    private Set<String> methodEntitySet;
    private MoveInstanceMethodProcessor moveInstanceMethodProcessor;
    private MoveStaticMembersProcessor moveStaticMembersProcessor;
    private List<ITypeBinding> typeBindings;

    /**
     * true:training generation  false:testing generation
     */
    private boolean trainingGeneration;

    public FeatureEnvyCandidate(MethodDeclaration methodDeclaration, MoveInstanceMethodProcessor moveInstanceMethodProcessor,
                                boolean heuristic, boolean trainingGeneration) {
        super();
        this.methodDeclaration = methodDeclaration;
        this.trainingGeneration = trainingGeneration;
        this.moveInstanceMethodProcessor = moveInstanceMethodProcessor;
        methodEntitySet = new HashSet<>();
        methodCallSet = new HashSet<>();
        methodCallList = new ArrayList<>();
        valid = true;
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
        sourceClassBinding = methodBinding.getDeclaringClass();
        calculateFeatures(heuristic);
    }

    public FeatureEnvyCandidate(MethodDeclaration methodDeclaration, MoveStaticMembersProcessor moveStaticMembersProcessor,
                                List<ITypeBinding> typeBindings, boolean heuristic, boolean trainingGeneration) {
        super();
        this.methodDeclaration = methodDeclaration;
        this.trainingGeneration = trainingGeneration;
        this.moveStaticMembersProcessor = moveStaticMembersProcessor;
        this.typeBindings = typeBindings;
        methodEntitySet = new HashSet<>();
        methodCallSet = new HashSet<>();
        methodCallList = new ArrayList<>();
        valid = true;
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
        sourceClassBinding = methodBinding.getDeclaringClass();
        calculateFeatures(heuristic);
    }

    public FeatureEnvyCandidate(MethodDeclaration methodDeclaration, ITypeBinding targetClassBinding, boolean trainingGeneration) {
        super();
        if (methodDeclaration == null || targetClassBinding == null) {
            valid = false;
            return;
        }
        this.methodDeclaration = methodDeclaration;
        this.trainingGeneration = trainingGeneration;
        methodEntitySet = new HashSet<>();
        methodCallSet = new HashSet<>();
        methodCallList = new ArrayList<>();
        valid = true;
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
        sourceClassBinding = methodBinding.getDeclaringClass();
        calculateSourceFeatures();
        calculateTargetFeatures(Set.of(targetClassBinding));
    }

    private void calculateFeatures(boolean heuristic) {
        Set<ITypeBinding> targetClassBindings = parseTargetCandidate();
        Iterator<ITypeBinding> iterator = targetClassBindings.iterator();
        while (iterator.hasNext()) {
            ITypeBinding targetClassBinding = iterator.next();
            String targetClassName = targetClassBinding.getQualifiedName();
            if (StringUtils.isEmpty(targetClassName)) {
                iterator.remove();
                continue;
            }
            if (heuristic) {
                IMethodBinding[] declaredMethods = targetClassBinding.getDeclaredMethods();
                IVariableBinding[] declaredFields = targetClassBinding.getDeclaredFields();
                int static_cnt = 0;
                int method_cnt = 0;
                int sum = declaredFields.length;
                for (IMethodBinding method : declaredMethods) {
                    if (!method.isConstructor())
                        sum += 1;
                }
                for (IMethodBinding declaredMethod : declaredMethods) {
                    if (!declaredMethod.isConstructor() && Flags.isStatic(declaredMethod.getModifiers())) {
                        static_cnt += 1;
                    }
                    if (!declaredMethod.isConstructor() && !GetterSetterUtils.isGetterOrSetter(declaredMethod, declaredFields))
                        method_cnt++;
                }
                for (IVariableBinding declaredField : declaredFields) {
                    if (Flags.isStatic(declaredField.getModifiers())) {
                        static_cnt += 1;
                    }
                }
                if (method_cnt == 0) {
                    iterator.remove();
                    continue;
                }
                if (static_cnt == sum && !Flags.isStatic(methodDeclaration.getModifiers())) {
                    iterator.remove();
                    continue;
                }
                if (isSubClass(sourceClassBinding, targetClassBinding) || isSubClass(targetClassBinding, sourceClassBinding)) {
                    iterator.remove();
                }
            }
        }
        if (targetClassBindings.size() == 0) {
            valid = false;
            return;
        }
        calculateSourceFeatures();
        if (trainingGeneration) {
            Random random = new Random(1337);
            ITypeBinding[] array = targetClassBindings.toArray(new ITypeBinding[0]);
            int n = random.nextInt(array.length);
            ITypeBinding randomElement = array[n];
            calculateTargetFeatures(Set.of(randomElement));
        } else {
            calculateTargetFeatures(targetClassBindings);
        }
    }

    private boolean isSubClass(ITypeBinding sourceClass, ITypeBinding targetClass) {
        while (sourceClass.getSuperclass() != null) {
            sourceClass = sourceClass.getSuperclass();
            if (sourceClass.getQualifiedName().equals(targetClass.getQualifiedName()))
                return true;
        }
        return false;
    }

    private void calculateSourceFeatures() {
        String sourceClassName = sourceClassBinding.getQualifiedName();

        /** class-level entity set */
        TypeEntitySet sourceClassMetric = new TypeEntitySet(sourceClassBinding);
        sourceClassMetric.calculate();
        Set<String> sourceClassEntitySet = (Set<String>) sourceClassMetric.getMetrics().get(sourceClassMetric.getName());

        /** method-level entity set */
        MethodEntitySet methodMetric = new MethodEntitySet(methodDeclaration);
        methodMetric.calculate();

        methodCallSet = (Set<String>) methodMetric.getMetrics().get("MethodCallHashSet");
        methodCallList = (List<String>) methodMetric.getMetrics().get("MethodCallList");
        methodEntitySet = (Set<String>) methodMetric.getMetrics().get(methodMetric.getName());

        /** calculate Jaccard distance (DIST), CBMC, MCMC*/
        JDeodorantDistance sourceDist = new JDeodorantDistance(methodEntitySet, sourceClassEntitySet);
        sourceDist.calculate();
        CouplingBetweenObject sourceCBMC = new CouplingBetweenObject(methodCallSet, sourceClassEntitySet);
        sourceCBMC.calculate();
        MessagePassingCoupling sourceMCMC = new MessagePassingCoupling(methodCallList, sourceClassEntitySet);
        sourceMCMC.calculate();

        features.put("sourceClassName", sourceClassName);
        features.put("methodName", methodDeclaration.getName().getFullyQualifiedName());
        IMethodBinding iMethodBinding = methodDeclaration.resolveBinding().getMethodDeclaration();
        String methodSignature = iMethodBinding.getName() + "(";
        ITypeBinding[] parameterTypes = iMethodBinding.getParameterTypes();
        methodSignature += Arrays.stream(parameterTypes).map(ITypeBinding::getQualifiedName).collect(Collectors.joining(", "));
        methodSignature += "):";
        methodSignature += iMethodBinding.getReturnType().getQualifiedName();
        features.put("methodSignature", methodSignature);
        features.put("sourceDist", sourceDist.getMetrics().get(sourceDist.getName()));
        features.put("sourceCBMC", sourceCBMC.getMetrics().get(sourceCBMC.getName()));
        features.put("sourceMCMC", sourceMCMC.getMetrics().get(sourceMCMC.getName()));
    }

    private void calculateTargetFeatures(Set<ITypeBinding> targetClassBindings) {
        List<String> targetClassList = new ArrayList<>();
        List<Double> targetDistList = new ArrayList<>();
        List<Double> targetCBMCList = new ArrayList<>();
        List<Double> targetMCMCList = new ArrayList<>();
        for (ITypeBinding targetClassBinding : targetClassBindings) {
            targetClassList.add(targetClassBinding.getQualifiedName());

            /** class-level entity set */
            TypeEntitySet targetClassMetric = new TypeEntitySet(targetClassBinding);
            targetClassMetric.calculate();
            Set<String> targetClassEntitySet = (Set<String>) targetClassMetric.getMetrics().get(targetClassMetric.getName());

            /** calculate Jaccard distance, CBMC, MCMC*/
            JDeodorantDistance targetDist = new JDeodorantDistance(methodEntitySet, targetClassEntitySet);
            targetDist.calculate();
            targetDistList.add((Double) targetDist.getMetrics().get(targetDist.getName()));

            CouplingBetweenObject targetCBMC = new CouplingBetweenObject(methodCallSet, targetClassEntitySet);
            targetCBMC.calculate();
            targetCBMCList.add((Double) targetCBMC.getMetrics().get(targetCBMC.getName()));

            MessagePassingCoupling targetMCMC = new MessagePassingCoupling(methodCallList, targetClassEntitySet);
            targetMCMC.calculate();
            targetMCMCList.add((Double) targetMCMC.getMetrics().get(targetMCMC.getName()));
        }

        if (trainingGeneration) {
            features.put("targetClassList", targetClassList.get(0));
            features.put("targetDistList", targetDistList.get(0));
            features.put("targetCBMCList", targetCBMCList.get(0));
            features.put("targetMCMCList", targetMCMCList.get(0));
        } else {
            features.put("targetClassList", targetClassList);
            features.put("targetDistList", targetDistList);
            features.put("targetCBMCList", targetCBMCList);
            features.put("targetMCMCList", targetMCMCList);
        }
    }

    private Set<ITypeBinding> parseTargetCandidate() {
        Set<ITypeBinding> targetClassBindings = new LinkedHashSet<>();
        if (trainingGeneration) {
            if (Flags.isStatic(methodDeclaration.getModifiers())) {
                for (ITypeBinding fDestinationType : typeBindings) {
                    if (moveStaticMembersProcessor.checkFinalConditions(fDestinationType))
                        targetClassBindings.add(fDestinationType);
                }
            } else {
                if (moveInstanceMethodProcessor.checkInitialConditions2(methodDeclaration)) {
                    for (IVariableBinding fTarget : moveInstanceMethodProcessor.getPossibleTargets()) {
                        ITypeBinding typeBinding = fTarget.getType().getTypeDeclaration();
                        moveInstanceMethodProcessor.setTarget(methodDeclaration, fTarget);
                        if (moveInstanceMethodProcessor.checkFinalConditions(methodDeclaration))
                            targetClassBindings.add(typeBinding);
                    }
                }
            }
        } else {
            PotentialTargetClassVisitor visitor = new PotentialTargetClassVisitor(sourceClassBinding);
            methodDeclaration.accept(visitor);
            Set<ITypeBinding> typeBindings = visitor.getTargetClasses();
            if (Flags.isStatic(methodDeclaration.getModifiers())) {
                for (ITypeBinding fDestinationType : typeBindings) {
                    if (moveStaticMembersProcessor.checkFinalConditions(fDestinationType))
                        targetClassBindings.add(fDestinationType);
                }
            } else {
                if (moveInstanceMethodProcessor.checkInitialConditions2(methodDeclaration)) {
                    for (IVariableBinding fTarget : moveInstanceMethodProcessor.getPossibleTargets()) {
                        ITypeBinding typeBinding = fTarget.getType().getTypeDeclaration();
                        if (typeBindings.contains(typeBinding)) {
                            moveInstanceMethodProcessor.setTarget(methodDeclaration, fTarget);
                            if (moveInstanceMethodProcessor.checkFinalConditions(methodDeclaration))
                                targetClassBindings.add(typeBinding);
                        }
                    }
                }
            }
        }
        return targetClassBindings;
    }

    @Override
    public boolean isValidCandidate() {
        return valid;
    }
}
