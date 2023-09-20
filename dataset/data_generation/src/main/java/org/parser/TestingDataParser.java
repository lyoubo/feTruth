package org.parser;

import org.apache.commons.io.FileUtils;
import org.datageneration.candidate.FeatureEnvyCandidate;
import org.datageneration.util.MoveInstanceMethodProcessor;
import org.datageneration.util.MoveStaticMembersProcessor;
import org.datageneration.visitor.PotentialMethodDeclarationVisitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.util.ASTParserUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestingDataParser extends ProjectParser {

    private final List<FeatureEnvyCandidate> featureEnvyCandidates;

    public TestingDataParser(String projectPath) {
        super(projectPath);
        featureEnvyCandidates = new ArrayList<>();
    }

    public List<Map<String, Object>> generateTestingData(boolean heuristic) throws IOException {
        for (String filePath : allJavaFiles) {
            if (filePath.contains("/src/test/")) continue;
            ASTParser parser = ASTParserUtils.getASTParser(sourcepathEntries, encodings);
            String code = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
            parser.setSource(code.toCharArray());
            try {
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                PotentialMethodDeclarationVisitor visitor = new PotentialMethodDeclarationVisitor();
                cu.accept(visitor);
                List<MethodDeclaration> methods = visitor.getAllMethods();
                for (MethodDeclaration methodDeclaration : methods) {
                    MoveStaticMembersProcessor moveStaticMembersProcessor = new MoveStaticMembersProcessor();
                    MoveInstanceMethodProcessor moveInstanceMethodProcessor = new MoveInstanceMethodProcessor();
                    FeatureEnvyCandidate candidate;
                    if (Flags.isStatic(methodDeclaration.getModifiers())) {
                        if (!moveStaticMembersProcessor.checkInitialConditions(methodDeclaration)) continue;
                        candidate = new FeatureEnvyCandidate(methodDeclaration, moveStaticMembersProcessor, null, heuristic, false);
                    } else {
                        if (!moveInstanceMethodProcessor.checkInitialConditions(methodDeclaration)) continue;
                        candidate = new FeatureEnvyCandidate(methodDeclaration, moveInstanceMethodProcessor, heuristic, false);
                    }
                    if (candidate.isValidCandidate())
                        featureEnvyCandidates.add(candidate);
                }
            } catch (Exception ignored) {
            }
        }
        List<Map<String, Object>> candidatesFeatures = new ArrayList<>();
        for (FeatureEnvyCandidate candidate : featureEnvyCandidates) {
            candidatesFeatures.add(candidate.getFeatures());
        }
        return candidatesFeatures;
    }

    public void clear() {
        featureEnvyCandidates.clear();
    }
}
