package org.parser;

import org.apache.commons.io.FileUtils;
import org.datageneration.candidate.FeatureEnvyCandidate;
import org.datageneration.util.MoveInstanceMethodProcessor;
import org.datageneration.util.MoveStaticMembersProcessor;
import org.datageneration.visitor.PotentialMethodDeclarationVisitor;
import org.datageneration.visitor.SourceMethodDeclarationVisitor;
import org.datageneration.visitor.TargetClassBindingVisitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.*;
import org.methodinvocation.dto.LocationInfo;
import org.util.ASTParserUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TrainingDataParser extends ProjectParser {

    private String sourceFilePath;
    private String targetFilePath;
    private String sourceMethodName;
    private int sourceStartLine;
    private int sourceEndLine;
    private String sourceClassName;
    private String targetClassName;

    public TrainingDataParser(String projectPath) {
        super(projectPath);
    }

    public void populateData(String sourceFilePath, String targetFilePath, String sourceMethodName,
                             int sourceStartLine, int sourceEndLine, String sourceClassName, String targetClassName) {
        this.sourceFilePath = sourceFilePath;
        this.targetFilePath = targetFilePath;
        this.sourceMethodName = sourceMethodName;
        this.sourceStartLine = sourceStartLine;
        this.sourceEndLine = sourceEndLine;
        this.sourceClassName = sourceClassName;
        this.targetClassName = targetClassName;
    }

    public Map<String, Object> generateTPTrainingData() throws IOException {
        ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
        String code = FileUtils.readFileToString(new File(sourceFilePath), StandardCharsets.UTF_8);
        parser.setSource(code.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        MethodDeclaration sourceMethod = getMethodDeclaration(cu);
        ITypeBinding targetClass = getITypeBinding();
        FeatureEnvyCandidate featureEnvyCandidate = new FeatureEnvyCandidate(sourceMethod, targetClass, true);
        if (featureEnvyCandidate.isValidCandidate())
            return featureEnvyCandidate.getFeatures();
        return null;
    }

    public List<Map<String, Object>> generateTNTrainingData(Set<String> methodNames, long total) throws IOException {
        List<Map<String, Object>> candidatesFeatures = new ArrayList<>();
        Random random = new Random(1337);
        Collections.shuffle(allJavaFiles, random);
        Set<String> selected = new HashSet<>();
        int i = 0;
        while (candidatesFeatures.size() < total && i < 5) {
            i++;
            for (String filePath : allJavaFiles) {
                if (candidatesFeatures.size() == total) break;
                if (filePath.contains("/src/test/")) continue;
                String code = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
                ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
                parser.setSource(code.toCharArray());
                try {
                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                    PotentialMethodDeclarationVisitor visitor = new PotentialMethodDeclarationVisitor();
                    cu.accept(visitor);
                    List<MethodDeclaration> methods = visitor.getAllMethods();
                    Collections.shuffle(methods, random);
                    for (MethodDeclaration methodDeclaration : methods) {
                        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
                        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
                        ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
                        String params = Arrays.stream(parameterTypes).map(ITypeBinding::getName).collect(Collectors.joining(","));
                        String identity = declaringClass.getQualifiedName() + "@" + methodDeclaration.getName().getFullyQualifiedName() + ":" + params;
                        if (methodNames.contains(methodDeclaration.getName().getFullyQualifiedName()) ||
                                selected.contains(identity))
                            continue;
                        selected.add(identity);
                        MoveStaticMembersProcessor moveStaticMembersProcessor = new MoveStaticMembersProcessor();
                        MoveInstanceMethodProcessor moveInstanceMethodProcessor = new MoveInstanceMethodProcessor();
                        FeatureEnvyCandidate featureEnvyCandidate;
                        if (Flags.isStatic(methodDeclaration.getModifiers())) {
                            if (!moveStaticMembersProcessor.checkInitialConditions(methodDeclaration)) continue;
                            int n = random.nextInt(allJavaFiles.size());
                            String targetFile = allJavaFiles.get(n);
                            List<ITypeBinding> typeBindings = getITypeBindings(targetFile);
                            featureEnvyCandidate = new FeatureEnvyCandidate(methodDeclaration, moveStaticMembersProcessor, typeBindings, false, true);
                        } else {
                            if (!moveInstanceMethodProcessor.checkInitialConditions(methodDeclaration)) continue;
                            featureEnvyCandidate = new FeatureEnvyCandidate(methodDeclaration, moveInstanceMethodProcessor, false, true);
                        }
                        if (featureEnvyCandidate.isValidCandidate()) {
                            candidatesFeatures.add(featureEnvyCandidate.getFeatures());
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return candidatesFeatures;
    }

    private MethodDeclaration getMethodDeclaration(CompilationUnit cu) {
        SourceMethodDeclarationVisitor visitor = new SourceMethodDeclarationVisitor(sourceClassName, sourceMethodName);
        cu.accept(visitor);
        List<MethodDeclaration> sourceMethods = visitor.getSourceMethods();
        for (MethodDeclaration sourceMethod : sourceMethods) {
            LocationInfo locationInfo = new LocationInfo(cu, sourceMethod);
            if (locationInfo.getStartLine() == sourceStartLine && locationInfo.getEndLine() == sourceEndLine)
                return sourceMethod;
        }
        return null;
    }

    private ITypeBinding getITypeBinding() throws IOException {
        ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
        String code = FileUtils.readFileToString(new File(targetFilePath), StandardCharsets.UTF_8);
        parser.setSource(code.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        TargetClassBindingVisitor visitor = new TargetClassBindingVisitor(targetClassName);
        cu.accept(visitor);
        return visitor.getTargetClass();
    }

    private List<ITypeBinding> getITypeBindings(String targetFile) throws IOException {
        ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
        String code = FileUtils.readFileToString(new File(targetFile), StandardCharsets.UTF_8);
        parser.setSource(code.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        List<ITypeBinding> typeBindings = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                ITypeBinding typeBinding = node.resolveBinding();
                if (typeBinding == null) return true;
                if (typeBinding.isTopLevel() || Flags.isStatic(typeBinding.getModifiers()))
                    typeBindings.add(typeBinding);
                return true;
            }
        });
        return typeBindings;
    }
}
