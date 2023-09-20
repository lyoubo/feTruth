package org.parser;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.*;
import org.methodinvocation.dto.LocationInfo;
import org.methodinvocation.dto.MethodCaller;
import org.methodinvocation.visitor.MethodInvocationVisitor;
import org.methodinvocation.visitor.NodeDeclarationVisitor;
import org.util.ASTParserUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MethodInvocationParser {

    private final String projectPath;
    private final List<String> relatedJavaFiles;
    private String[] sourcepathEntries;
    private String[] encodings;

    public MethodInvocationParser(String projectPath, List<String> relatedJavaFiles) {
        this.projectPath = projectPath;
        this.relatedJavaFiles = relatedJavaFiles;
        populateSourcepathEntries();
    }

    public List<MethodCaller> findInvocation(String projectName, String className, String methodName, String paramTypes,
                                             String filePath, int startLine, int endLine) {
        try {
            ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
            String code = FileUtils.readFileToString(new File(projectPath + "/" + filePath), StandardCharsets.UTF_8);
            parser.setSource(code.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            final MethodDeclaration[] methodDeclaration = new MethodDeclaration[1];
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    LocationInfo locationInfo = new LocationInfo(cu, node);
                    if (locationInfo.getStartLine() == startLine && locationInfo.getEndLine() == endLine) {
                        IMethodBinding methodBinding = node.resolveBinding();
                        if (methodBinding == null) return true;
                        methodDeclaration[0] = node;
                    }
                    return true;
                }
            });
            if (methodDeclaration[0] != null) {
                IMethodBinding methodBinding = methodDeclaration[0].resolveBinding();
                ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
                paramTypes = Arrays.stream(parameterTypes).map(ITypeBinding::getName).collect(Collectors.joining(","));
            }
        } catch (Exception ignored) {
        }
        List<MethodCaller> callers = new ArrayList<>();
        for (String file : relatedJavaFiles) {
            try {
                ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
                String code = FileUtils.readFileToString(new File(projectPath + "/" + file), StandardCharsets.UTF_8);
                parser.setSource(code.toCharArray());
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                NodeDeclarationVisitor visitor = new NodeDeclarationVisitor();
                cu.accept(visitor);
                List<MethodDeclaration> methodDeclarations = visitor.getMethodDeclarations();
                List<VariableDeclarationFragment> fragmentDeclarations = visitor.getFragmentDeclarations();
                List<Initializer> initializers = visitor.getInitializers();
                MethodInvocationVisitor miVisitor = new MethodInvocationVisitor(className, methodName, paramTypes);
                for (MethodDeclaration md : methodDeclarations) {
                    miVisitor.clear();
                    md.accept(miVisitor);
                    if (miVisitor.getInvokedMethods().size() > 0) {
                        LocationInfo locationInfo = new LocationInfo(cu, md);
                        populateData(callers, md, locationInfo, getRelativeFilePath(file, projectName), className,
                                methodName, paramTypes, filePath, miVisitor.getInvokedMethods().size());
                    }
                }
                for (VariableDeclarationFragment fd : fragmentDeclarations) {
                    miVisitor.clear();
                    fd.accept(miVisitor);
                    if (miVisitor.getInvokedMethods().size() > 0) {
                        LocationInfo locationInfo = new LocationInfo(cu, fd);
                        populateData(callers, fd, locationInfo, getRelativeFilePath(file, projectName), className,
                                methodName, paramTypes, filePath, miVisitor.getInvokedMethods().size());
                    }
                }
                for (Initializer in : initializers) {
                    miVisitor.clear();
                    in.accept(miVisitor);
                    if (miVisitor.getInvokedMethods().size() > 0) {
                        LocationInfo locationInfo = new LocationInfo(cu, in);
                        populateData(callers, in, locationInfo, getRelativeFilePath(file, projectName), className,
                                methodName, paramTypes, filePath, miVisitor.getInvokedMethods().size());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return callers;
    }

    private void populateSourcepathEntries() {
        Set<String> sourceRootSet = new HashSet<>();
        for (String filePath : relatedJavaFiles) {
            ASTParser parser = ASTParserUtils.getASTParser();
            try {
                String code = FileUtils.readFileToString(new File(projectPath + "/" + filePath), StandardCharsets.UTF_8);
                parser.setSource(code.toCharArray());
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                if (cu.getPackage() == null) continue;
                String rootPath = parseRootPath(projectPath + "/" + filePath, cu.getPackage().getName().toString());
                if (!rootPath.equals("") && Paths.get(rootPath).toFile().exists()) sourceRootSet.add(rootPath);
            } catch (Exception ignored) {
            }
        }
        sourcepathEntries = new String[sourceRootSet.size()];
        encodings = new String[sourceRootSet.size()];
        int index = 0;
        for (String sourceRoot : sourceRootSet) {
            sourcepathEntries[index] = sourceRoot;
            encodings[index] = "utf-8";
            index++;
        }
    }

    private String parseRootPath(String filePath, String packageName) {
        String path = packageName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        Path relativePath = Paths.get(path);
        Path absolutePath = Paths.get(filePath).resolveSibling("");
        int end = absolutePath.toString().lastIndexOf(relativePath.toString());
        if (end == -1) return "";
        return absolutePath.toString().substring(0, end).replace("\\", "/");
    }

    private void populateData(List<MethodCaller> methodCallers, MethodDeclaration md, LocationInfo locationInfo, String callerFilePath,
                              String className, String methodName, String paramTypes, String calleeFilePath, int frequency) {
        MethodCaller caller = new MethodCaller();
        IMethodBinding methodBinding = md.resolveBinding();
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        caller.setCallerClassName(declaringClass.getQualifiedName());
        caller.setCallerType(MethodCaller.CallerType.METHOD);
        caller.setCallerSignature(methodBinding.toString().strip());
        caller.setCallerName(md.getName().getFullyQualifiedName());
        caller.setLocationInfo(locationInfo);
        caller.setCallerFilePath(callerFilePath);
        caller.setCalleeClassName(className);
        caller.setCalleeMethodName(methodName);
        caller.setCalleeParamTypes(paramTypes);
        caller.setCalleeFilePath(calleeFilePath);
        caller.setCallerFrequency(frequency);
        methodCallers.add(caller);
    }

    private void populateData(List<MethodCaller> methodCallers, VariableDeclarationFragment fd, LocationInfo locationInfo, String callerFilePath,
                              String className, String methodName, String paramTypes, String calleeFilePath, int frequency) {
        MethodCaller caller = new MethodCaller();
        IVariableBinding variableBinding = fd.resolveBinding();
        ITypeBinding declaringClass = variableBinding.getDeclaringClass();
        caller.setCallerClassName(declaringClass.getQualifiedName());
        caller.setCallerType(MethodCaller.CallerType.FIELD);
        caller.setCallerSignature(variableBinding.toString().strip());
        caller.setCallerName(fd.getName().getFullyQualifiedName());
        caller.setLocationInfo(locationInfo);
        caller.setCallerFilePath(callerFilePath);
        caller.setCalleeClassName(className);
        caller.setCalleeMethodName(methodName);
        caller.setCalleeParamTypes(paramTypes);
        caller.setCalleeFilePath(calleeFilePath);
        caller.setCallerFrequency(frequency);
        methodCallers.add(caller);
    }

    private void populateData(List<MethodCaller> methodCallers, Initializer in, LocationInfo locationInfo, String callerFilePath,
                              String className, String methodName, String paramTypes, String calleeFilePath, int frequency) {
        MethodCaller caller = new MethodCaller();
        ASTNode parent = in.getParent();
        ITypeBinding typeBinding = ((AbstractTypeDeclaration) parent).resolveBinding();
        caller.setCallerClassName(typeBinding.getQualifiedName());
        caller.setCallerType(MethodCaller.CallerType.INITIALIZER);
        caller.setCallerSignature(Flags.isStatic(in.getModifiers()) ? "static" : "instance");
        caller.setCallerName("initializer");
        caller.setLocationInfo(locationInfo);
        caller.setCallerFilePath(callerFilePath);
        caller.setCalleeClassName(className);
        caller.setCalleeMethodName(methodName);
        caller.setCalleeParamTypes(paramTypes);
        caller.setCalleeFilePath(calleeFilePath);
        caller.setCallerFrequency(frequency);
        methodCallers.add(caller);
    }

    private String getRelativeFilePath(String filePath, String projectName) {
        Pattern pattern = Pattern.compile(projectName);
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            int start = matcher.end() + 1;
            return filePath.substring(start);
        }
        return filePath;
    }

    public String[] getSourcepathEntries() {
        return sourcepathEntries;
    }

    public String[] getEncodings() {
        return encodings;
    }
}
