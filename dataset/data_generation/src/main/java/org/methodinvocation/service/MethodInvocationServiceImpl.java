package org.methodinvocation.service;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.methodinvocation.dto.LocationInfo;
import org.methodinvocation.dto.MethodCaller;
import org.parser.MethodInvocationParser;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.util.GitTravellerUtils;
import org.util.JDBCUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MethodInvocationServiceImpl implements MethodInvocationService {

    private final String rootPath;

    {
        ClassLoader classLoader = RefactoringMiner.class.getClassLoader();
        URL resource = classLoader.getResource("config.properties");
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(resource.getFile()));
        } catch (IOException ignored) {
            throw new RuntimeException("can't locate config.properties");
        }
        rootPath = props.getProperty("rootPath").endsWith("/") ? props.getProperty("rootPath") : props.getProperty("rootPath") + "/";
    }

    @Override
    public void findInvocation() throws SQLException, GitAPIException, IOException {
        final String selectSQL = "SELECT * FROM refactoring_miner WHERE filter_type = ''";
        final String insertSQL = "INSERT INTO method_invocation(project_name, commit_id, caller_class_name, caller_type, " +
                "caller_signature, caller_name, caller_start_line, caller_end_line, caller_start_column, caller_end_column, " +
                "caller_file_path, callee_class_name, callee_method_name, callee_param_types, callee_file_path, caller_frequency, " +
                "move_identity, refactoring_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = rootPath + "dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
        PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
        ResultSet rs = selectStmt.executeQuery();
        while (rs.next()) {
            int refactoringId = rs.getInt("id");
            String projectName = rs.getString("project_name");
            String projectPath = datasetPath + projectName;
            String commitId = rs.getString("commit_id");
            Set<String> addedFiles = new LinkedHashSet<>();
            Set<String> deletedFiles = new LinkedHashSet<>();
            Set<String> modifiedFiles = new LinkedHashSet<>();
            Map<String, String> renamedFiles = new LinkedHashMap<>();
            GitTravellerUtils.fileTreeDiff(projectPath, commitId, addedFiles, deletedFiles, modifiedFiles, renamedFiles);
            try {
                List<String> relatedJavaFiles = new ArrayList<>();
                relatedJavaFiles.addAll(addedFiles);
                relatedJavaFiles.addAll(modifiedFiles);
                relatedJavaFiles.addAll(renamedFiles.values());
                GitTravellerUtils.resetHard(projectPath);
                GitTravellerUtils.checkoutCurrent(projectPath, commitId);
                MethodInvocationParser targetParser = new MethodInvocationParser(projectPath, relatedJavaFiles);
                List<MethodCaller> targetInvocation = targetParser.findInvocation(projectName,
                        rs.getString("target_class_name"), rs.getString("target_method_name"),
                        rs.getString("target_param_types"), rs.getString("target_file_path"),
                        rs.getInt("target_start_line"), rs.getInt("target_end_line"));
                insertSQL(conn, insertStmt, targetInvocation, 1, refactoringId, projectName, commitId);
                relatedJavaFiles.clear();
                relatedJavaFiles.addAll(deletedFiles);
                relatedJavaFiles.addAll(modifiedFiles);
                relatedJavaFiles.addAll(renamedFiles.keySet());
                GitTravellerUtils.resetHard(projectPath);
                GitTravellerUtils.checkoutParent(projectPath, commitId);
                MethodInvocationParser sourceParser = new MethodInvocationParser(projectPath, relatedJavaFiles);
                List<MethodCaller> sourceInvocation = sourceParser.findInvocation(projectName,
                        rs.getString("source_class_name"), rs.getString("source_method_name"),
                        rs.getString("source_param_types"), rs.getString("source_file_path"),
                        rs.getInt("source_start_line"), rs.getInt("source_end_line"));
                insertSQL(conn, insertStmt, sourceInvocation, 0, refactoringId, projectName, commitId);
            } catch (Exception ignored) {
            } finally {
                GitTravellerUtils.resetHard(projectPath);
            }
        }
        JDBCUtils.close(conn, selectStmt, insertStmt);
    }

    @Override
    public void matchInvocation() throws SQLException {
        final String selectSQL = "SELECT * FROM refactoring_miner WHERE filter_type = ''";
        final String invocationSQL = "SELECT * FROM method_invocation WHERE refactoring_id = ?";
        final String insertSQL = "INSERT INTO decision_feature(source_invocation_nums, target_invocation_nums, " +
                "matched_invocation_nums, source_code_elements, target_code_elements, matched_code_elements, " +
                "refactoring_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = rootPath + "dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
        PreparedStatement invocationStmt = conn.prepareStatement(invocationSQL);
        PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
        ResultSet rs = selectStmt.executeQuery();
        while (rs.next()) {
            int refactoringId = rs.getInt("id");
            String projectName = rs.getString("project_name");
            String commitId = rs.getString("commit_id");
            int[] methodInvocations = countMethodInvocation(invocationStmt, refactoringId, projectName, commitId, datasetPath);
            MappedElement mappings = new MappedElement(rs.getString("mapped_element"));
            insertStmt.setInt(1, methodInvocations[0]);
            insertStmt.setInt(2, methodInvocations[1]);
            insertStmt.setInt(3, methodInvocations[2]);
            insertStmt.setInt(4, mappings.getSourceLOE());
            insertStmt.setInt(5, mappings.getTargetLOE());
            insertStmt.setInt(6, mappings.getMappedLOE());
            insertStmt.setInt(7, refactoringId);
            insertStmt.addBatch();
            insertStmt.executeBatch();
            conn.commit();
        }
        JDBCUtils.close(conn, selectStmt, invocationStmt, insertStmt);
    }

    private void insertSQL(Connection conn, PreparedStatement insertStmt, List<MethodCaller> callers, int moveIdentity,
                           int refactoringId, String projectName, String commitId) throws SQLException {
        for (MethodCaller caller : callers) {
            insertStmt.setString(1, projectName);
            insertStmt.setString(2, commitId);
            insertStmt.setString(3, caller.getCallerClassName());
            insertStmt.setString(4, caller.getCallerType());
            insertStmt.setString(5, caller.getCallerSignature());
            insertStmt.setString(6, caller.getCallerName());
            insertStmt.setInt(7, caller.getLocationInfo().getStartLine());
            insertStmt.setInt(8, caller.getLocationInfo().getEndLine());
            insertStmt.setInt(9, caller.getLocationInfo().getStartColumn());
            insertStmt.setInt(10, caller.getLocationInfo().getEndColumn());
            insertStmt.setString(11, caller.getCallerFilePath());
            insertStmt.setString(12, caller.getCalleeClassName());
            insertStmt.setString(13, caller.getCalleeMethodName());
            insertStmt.setString(14, caller.getCalleeParamTypes());
            insertStmt.setString(15, caller.getCalleeFilePath());
            insertStmt.setInt(16, caller.getCallerFrequency());
            insertStmt.setInt(17, moveIdentity);  // 0: source   1: target
            insertStmt.setInt(18, refactoringId);
            insertStmt.addBatch();
            insertStmt.executeBatch();
        }
        conn.commit();
    }

    private int[] countMethodInvocation(PreparedStatement invocationStmt, int refactoringId, String projectName, String commitId, String datasetPath) throws SQLException {
        List<MethodCaller> sourceInvocation = new ArrayList<>();
        List<MethodCaller> targetInvocation = new ArrayList<>();
        invocationStmt.setInt(1, refactoringId);
        ResultSet rs = invocationStmt.executeQuery();
        while (rs.next()) {
            MethodCaller caller = new MethodCaller();
            caller.setCallerClassName(rs.getString("caller_class_name"));
            caller.setCallerType(rs.getString("caller_type"));
            caller.setCallerSignature(rs.getString("caller_signature"));
            caller.setCallerName(rs.getString("caller_name"));
            caller.setCallerFilePath(rs.getString("caller_file_path"));
            caller.setCallerFrequency(rs.getInt("caller_frequency"));
            int startLine = rs.getInt("caller_start_line");
            int endLine = rs.getInt("caller_end_line");
            int startColumn = rs.getInt("caller_end_column");
            int endColumn = rs.getInt("caller_end_column");
            caller.setLocationInfo(new LocationInfo(startLine, endLine, startColumn, endColumn));
            caller.setCalleeClassName(rs.getString("callee_class_name"));
            caller.setCalleeMethodName(rs.getString("callee_method_name"));
            caller.setCalleeParamTypes(rs.getString("callee_param_types"));
            caller.setCalleeFilePath(rs.getString("callee_file_path"));
            int moved = rs.getInt("move_identity");
            if (moved == 0)
                sourceInvocation.add(caller);
            if (moved == 1)
                targetInvocation.add(caller);
        }
        String projectPath = datasetPath + projectName;
        return countMethodInvocation(sourceInvocation, targetInvocation, commitId, projectPath);
    }

    public int[] countMethodInvocation(List<MethodCaller> sourceInvocation, List<MethodCaller> targetInvocation, String commitId, String projectPath) {
        List<MethodCaller> matchedSourceInvocation = new ArrayList<>();
        List<MethodCaller> matchedTargetInvocation = new ArrayList<>();
        for (MethodCaller source : new ArrayList<>(sourceInvocation)) {
            for (MethodCaller target : new ArrayList<>(targetInvocation)) {
                if (source.equals(target)) {
                    matchedSourceInvocation.add(source);
                    matchedTargetInvocation.add(target);
                    sourceInvocation.remove(source);
                    targetInvocation.remove(target);
                    break;
                }
            }
        }
        GitService gitService = new GitServiceImpl();
        Map<MethodCaller, MethodCaller> methodRefactors = new HashMap<>();
        Map<MethodCaller, MethodCaller> fieldRefactors = new HashMap<>();
        Map<String, String> classRefactors = new HashMap<>();
        if (sourceInvocation.size() != 0 && targetInvocation.size() != 0) {
            try (Repository repo = gitService.openRepository(projectPath)) {
                GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
                detector.detectAtCommit(repo, commitId, new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
                        for (Refactoring ref : refactorings) {
                            RefactoringType refactoringType = ref.getRefactoringType();
                            /** at method-level refactoring */
                            if (refactoringType == RefactoringType.ADD_PARAMETER) {
                                UMLOperation operationBefore = ((AddParameterRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((AddParameterRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.REMOVE_PARAMETER) {
                                UMLOperation operationBefore = ((RemoveParameterRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((RemoveParameterRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.RENAME_PARAMETER ||
                                    refactoringType == RefactoringType.PARAMETERIZE_VARIABLE ||
                                    refactoringType == RefactoringType.LOCALIZE_PARAMETER) {
                                VariableDeclarationContainer operationBefore = ((RenameVariableRefactoring) ref).getOperationBefore();
                                VariableDeclarationContainer operationAfter = ((RenameVariableRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.REORDER_PARAMETER) {
                                UMLOperation operationBefore = ((ReorderParameterRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((ReorderParameterRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.CHANGE_PARAMETER_TYPE) {
                                VariableDeclarationContainer operationBefore = ((ChangeVariableTypeRefactoring) ref).getOperationBefore();
                                VariableDeclarationContainer operationAfter = ((ChangeVariableTypeRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MERGE_PARAMETER) {
                                VariableDeclarationContainer operationBefore = ((MergeVariableRefactoring) ref).getOperationBefore();
                                VariableDeclarationContainer operationAfter = ((MergeVariableRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.SPLIT_PARAMETER) {
                                VariableDeclarationContainer operationBefore = ((SplitVariableRefactoring) ref).getOperationBefore();
                                VariableDeclarationContainer operationAfter = ((SplitVariableRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.EXTRACT_OPERATION ||
                                    refactoringType == RefactoringType.EXTRACT_AND_MOVE_OPERATION) {
                                VariableDeclarationContainer operationBefore = ((ExtractOperationRefactoring) ref).getSourceOperationBeforeExtraction();
                                UMLOperation operationAfter = ((ExtractOperationRefactoring) ref).getExtractedOperation();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.RENAME_METHOD) {
                                UMLOperation operationBefore = ((RenameOperationRefactoring) ref).getOriginalOperation();
                                UMLOperation operationAfter = ((RenameOperationRefactoring) ref).getRenamedOperation();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.INLINE_OPERATION ||
                                    refactoringType == RefactoringType.MOVE_AND_INLINE_OPERATION) {
                                UMLOperation operationBefore = ((InlineOperationRefactoring) ref).getInlinedOperation();
                                VariableDeclarationContainer operationAfter = ((InlineOperationRefactoring) ref).getTargetOperationBeforeInline();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MOVE_OPERATION ||
                                    refactoringType == RefactoringType.MOVE_AND_RENAME_OPERATION ||
                                    refactoringType == RefactoringType.PULL_UP_OPERATION ||
                                    refactoringType == RefactoringType.PUSH_DOWN_OPERATION) {
                                UMLOperation operationBefore = ((MoveOperationRefactoring) ref).getOriginalOperation();
                                UMLOperation operationAfter = ((MoveOperationRefactoring) ref).getMovedOperation();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.CHANGE_RETURN_TYPE) {
                                UMLOperation operationBefore = ((ChangeReturnTypeRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((ChangeReturnTypeRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.ADD_THROWN_EXCEPTION_TYPE) {
                                UMLOperation operationBefore = ((AddThrownExceptionTypeRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((AddThrownExceptionTypeRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.REMOVE_THROWN_EXCEPTION_TYPE) {
                                UMLOperation operationBefore = ((RemoveThrownExceptionTypeRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((RemoveThrownExceptionTypeRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.CHANGE_THROWN_EXCEPTION_TYPE) {
                                UMLOperation operationBefore = ((ChangeThrownExceptionTypeRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((ChangeThrownExceptionTypeRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.CHANGE_OPERATION_ACCESS_MODIFIER) {
                                UMLOperation operationBefore = ((ChangeOperationAccessModifierRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((ChangeOperationAccessModifierRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.ADD_METHOD_MODIFIER) {
                                UMLOperation operationBefore = ((AddMethodModifierRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((AddMethodModifierRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.REMOVE_METHOD_MODIFIER) {
                                UMLOperation operationBefore = ((RemoveMethodModifierRefactoring) ref).getOperationBefore();
                                UMLOperation operationAfter = ((RemoveMethodModifierRefactoring) ref).getOperationAfter();
                                setRefactor(methodRefactors, operationBefore, operationAfter);
                            }
                            /** at field-level refactoring */
                            else if (refactoringType == RefactoringType.MOVE_ATTRIBUTE ||
                                    refactoringType == RefactoringType.MOVE_RENAME_ATTRIBUTE ||
                                    refactoringType == RefactoringType.REPLACE_ATTRIBUTE ||
                                    refactoringType == RefactoringType.PULL_UP_ATTRIBUTE ||
                                    refactoringType == RefactoringType.PUSH_DOWN_ATTRIBUTE) {
                                UMLAttribute operationBefore = ((MoveAttributeRefactoring) ref).getOriginalAttribute();
                                UMLAttribute operationAfter = ((MoveAttributeRefactoring) ref).getMovedAttribute();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.EXTRACT_ATTRIBUTE) {
                                UMLAttribute operationBefore = ((ExtractAttributeRefactoring) ref).getVariableDeclaration();
                                UMLAttribute operationAfter = ((ExtractAttributeRefactoring) ref).getVariableDeclaration();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.RENAME_ATTRIBUTE) {
                                UMLAttribute operationBefore = ((RenameAttributeRefactoring) ref).getOriginalAttribute();
                                UMLAttribute operationAfter = ((RenameAttributeRefactoring) ref).getRenamedAttribute();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MERGE_ATTRIBUTE) {
                                Set<UMLAttribute> operationBeforeSet = ((MergeAttributeRefactoring) ref).getMergedAttributes();
                                UMLAttribute operationAfter = ((MergeAttributeRefactoring) ref).getNewAttribute();
                                for (UMLAttribute operationBefore : operationBeforeSet) {
                                    setRefactor(fieldRefactors, operationBefore, operationAfter);
                                }
                            } else if (refactoringType == RefactoringType.SPLIT_ATTRIBUTE) {
                                UMLAttribute operationBefore = ((SplitAttributeRefactoring) ref).getOldAttribute();
                                Set<UMLAttribute> operationAfterSet = ((SplitAttributeRefactoring) ref).getSplitAttributes();
                                for (UMLAttribute operationAfter : operationAfterSet) {
                                    setRefactor(fieldRefactors, operationBefore, operationAfter);
                                }
                            } else if (refactoringType == RefactoringType.CHANGE_ATTRIBUTE_TYPE) {
                                UMLAttribute operationBefore = ((ChangeAttributeTypeRefactoring) ref).getOriginalAttribute();
                                UMLAttribute operationAfter = ((ChangeAttributeTypeRefactoring) ref).getChangedTypeAttribute();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.CHANGE_ATTRIBUTE_ACCESS_MODIFIER) {
                                UMLAttribute operationBefore = ((ChangeAttributeAccessModifierRefactoring) ref).getAttributeBefore();
                                UMLAttribute operationAfter = ((ChangeAttributeAccessModifierRefactoring) ref).getAttributeAfter();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.ADD_ATTRIBUTE_MODIFIER) {
                                UMLAttribute operationBefore = ((AddAttributeModifierRefactoring) ref).getAttributeBefore();
                                UMLAttribute operationAfter = ((AddAttributeModifierRefactoring) ref).getAttributeAfter();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.REMOVE_ATTRIBUTE_MODIFIER) {
                                UMLAttribute operationBefore = ((RemoveAttributeModifierRefactoring) ref).getAttributeBefore();
                                UMLAttribute operationAfter = ((RemoveAttributeModifierRefactoring) ref).getAttributeAfter();
                                setRefactor(fieldRefactors, operationBefore, operationAfter);
                            }
                            /** at class-level refactoring */
                            else if (refactoringType == RefactoringType.RENAME_CLASS) {
                                String operationBefore = ((RenameClassRefactoring) ref).getOriginalClassName();
                                String operationAfter = ((RenameClassRefactoring) ref).getRenamedClassName();
                                setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MOVE_CLASS) {
                                String operationBefore = ((MoveClassRefactoring) ref).getOriginalClassName();
                                String operationAfter = ((MoveClassRefactoring) ref).getMovedClassName();
                                setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MOVE_RENAME_CLASS) {
                                String operationBefore = ((MoveAndRenameClassRefactoring) ref).getOriginalClassName();
                                String operationAfter = ((MoveAndRenameClassRefactoring) ref).getRenamedClassName();
                                setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.MERGE_CLASS) {
                                Set<String> operationBeforeSet = ((MergeClassRefactoring) ref).getMergedClasses().stream().map(UMLClass::getName).collect(Collectors.toSet());
                                String operationAfter = ((MergeClassRefactoring) ref).getNewClass().getName();
                                for (String operationBefore : operationBeforeSet)
                                    setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.SPLIT_CLASS) {
                                String operationBefore = ((SplitClassRefactoring) ref).getOriginalClass().getName();
                                Set<String> operationAfterSet = ((SplitClassRefactoring) ref).getSplitClasses().stream().map(UMLClass::getName).collect(Collectors.toSet());
                                for (String operationAfter : operationAfterSet)
                                    setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.EXTRACT_SUPERCLASS ||
                                    refactoringType == RefactoringType.EXTRACT_INTERFACE) {
                                Set<String> operationBeforeSet = ((ExtractSuperclassRefactoring) ref).getSubclassSetBefore();
                                String operationAfter = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getName();
                                for (String operationBefore : operationBeforeSet)
                                    setRefactor(classRefactors, operationBefore, operationAfter);
                            } else if (refactoringType == RefactoringType.EXTRACT_CLASS ||
                                    refactoringType == RefactoringType.EXTRACT_SUBCLASS) {
                                Map<UMLOperation, UMLOperation> extractedOperations = ((ExtractClassRefactoring) ref).getExtractedOperations();
                                Map<UMLAttribute, UMLAttribute> extractedAttributes = ((ExtractClassRefactoring) ref).getExtractedAttributes();
                                Set<String> operationAfterSet = extractedOperations.keySet().stream().map(UMLOperation::getClassName).collect(Collectors.toSet());
                                String operationBefore = ((ExtractClassRefactoring) ref).getExtractedClass().getName();
                                for (String operationAfter : operationAfterSet)
                                    setRefactor(classRefactors, operationBefore, operationAfter);
                                for (UMLOperation operation : extractedOperations.keySet())
                                    setRefactor(methodRefactors, operation, extractedOperations.get(operation));
                                for (UMLAttribute attribute : extractedAttributes.keySet())
                                    setRefactor(fieldRefactors, attribute, extractedAttributes.get(attribute));
                            }
                            /** at package-level refactoring */
                            else if (refactoringType == RefactoringType.RENAME_PACKAGE ||
                                    refactoringType == RefactoringType.MOVE_PACKAGE) {
                                List<PackageLevelRefactoring> moveClassRefactorings = ((RenamePackageRefactoring) ref).getMoveClassRefactorings();
                                for (PackageLevelRefactoring refactoring : moveClassRefactorings) {
                                    String operationBefore = refactoring.getOriginalClassName();
                                    String operationAfter = refactoring.getMovedClassName();
                                    setRefactor(classRefactors, operationBefore, operationAfter);
                                }
                            } else if (refactoringType == RefactoringType.MERGE_PACKAGE) {
                                Set<RenamePackageRefactoring> renamePackageRefactorings = ((MergePackageRefactoring) ref).getRenamePackageRefactorings();
                                for (RenamePackageRefactoring packageRefactoring : renamePackageRefactorings) {
                                    for (PackageLevelRefactoring refactoring : packageRefactoring.getMoveClassRefactorings()) {
                                        String operationBefore = refactoring.getOriginalClassName();
                                        String operationAfter = refactoring.getMovedClassName();
                                        setRefactor(classRefactors, operationBefore, operationAfter);
                                    }
                                }
                            } else if (refactoringType == RefactoringType.SPLIT_PACKAGE) {
                                Set<RenamePackageRefactoring> renamePackageRefactorings = ((SplitPackageRefactoring) ref).getRenamePackageRefactorings();
                                for (RenamePackageRefactoring packageRefactoring : renamePackageRefactorings) {
                                    for (PackageLevelRefactoring refactoring : packageRefactoring.getMoveClassRefactorings()) {
                                        String operationBefore = refactoring.getOriginalClassName();
                                        String operationAfter = refactoring.getMovedClassName();
                                        setRefactor(classRefactors, operationBefore, operationAfter);
                                    }
                                }
                            } else if (refactoringType == RefactoringType.MOVE_SOURCE_FOLDER) {
                                List<MovedClassToAnotherSourceFolder> folders = ((MoveSourceFolderRefactoring) ref).getMovedClassesToAnotherSourceFolder();
                                for (MovedClassToAnotherSourceFolder folder : folders)
                                    setRefactor(classRefactors, folder.getOriginalClassName(), folder.getMovedClassName());
                            }
                        }
                    }
                }, 60);
            } catch (Exception ignored) {
            }
            for (MethodCaller source : new ArrayList<>(sourceInvocation)) {
                boolean matched = false;
                /** match between the method before refactoring and the method after refactoring */
                for (MethodCaller refactor : methodRefactors.keySet()) {
                    if (source.equalsByRefactoring(refactor))
                        for (MethodCaller target : new ArrayList<>(targetInvocation)) {
                            if (target.equalsByRefactoring(methodRefactors.get(refactor))) {
                                matchedSourceInvocation.add(source);
                                matchedTargetInvocation.add(target);
                                sourceInvocation.remove(source);
                                targetInvocation.remove(target);
                                matched = true;
                                break;
                            }
                        }
                    if (matched) break;
                }
                if (matched) continue;
                /** match between the field before refactoring and the field after refactoring */
                for (MethodCaller refactor : fieldRefactors.keySet()) {
                    if (source.equalsByRefactoring(refactor))
                        for (MethodCaller target : new ArrayList<>(targetInvocation)) {
                            if (target.equalsByRefactoring(fieldRefactors.get(refactor))) {
                                sourceInvocation.remove(source);
                                targetInvocation.remove(target);
                                matchedSourceInvocation.add(source);
                                matchedTargetInvocation.add(target);
                                matched = true;
                                break;
                            }
                        }
                    if (matched) break;
                }
                if (matched) continue;
                /** match between the initializer/constructor before refactoring and the initializer/constructor after refactoring */
                for (MethodCaller target : new ArrayList<>(targetInvocation)) {
                    String sourceClassName = source.getCallerClassName();
                    String targetClassName = target.getCallerClassName();
                    if (classRefactors.containsKey(sourceClassName) &&
                            classRefactors.get(sourceClassName).equals(targetClassName)) {
                        if (source.equalsOnlyBySignature(target)) {
                            sourceInvocation.remove(source);
                            targetInvocation.remove(target);
                            matchedSourceInvocation.add(source);
                            matchedTargetInvocation.add(target);
                            break;
                        } else {
                            String[] split1 = sourceClassName.split("\\.");
                            String[] split2 = targetClassName.split("\\.");
                            String lastSourceClassName = split1[split1.length - 1];
                            String lastTargetClassName = split2[split2.length - 1];
                            if (source.getCallerType().equals("method") && target.getCallerType().equals("method") &&
                                    source.getCallerName().equalsIgnoreCase(lastSourceClassName) &&
                                    target.getCallerName().equalsIgnoreCase(lastTargetClassName)) {
                                sourceInvocation.remove(source);
                                targetInvocation.remove(target);
                                matchedSourceInvocation.add(source);
                                matchedTargetInvocation.add(target);
                                break;
                            }
                            if (source.getCallerType().equals("method") && target.getCallerType().equals("initializer") &&
                                    source.getCallerName().equalsIgnoreCase(lastSourceClassName)) {
                                sourceInvocation.remove(source);
                                targetInvocation.remove(target);
                                matchedSourceInvocation.add(source);
                                matchedTargetInvocation.add(target);
                                break;
                            }
                            if (source.getCallerType().equals("initializer") && target.getCallerType().equals("method") &&
                                    target.getCallerName().equalsIgnoreCase(lastTargetClassName)) {
                                sourceInvocation.remove(source);
                                targetInvocation.remove(target);
                                matchedSourceInvocation.add(source);
                                matchedTargetInvocation.add(target);
                                break;
                            }
                        }
                    }
                }
            }
        }
        sourceInvocation.removeIf(source -> source.getCallerFilePath().contains("/src/test/"));
        targetInvocation.removeIf(target -> target.getCallerFilePath().contains("/src/test/"));

        int sourceCallerNum = calculateFrequency(sourceInvocation, matchedSourceInvocation);
        int targetCallerNum = calculateFrequency(targetInvocation, matchedTargetInvocation);
        int matchedCallerNum = calculateMatchedFrequency(matchedSourceInvocation, matchedTargetInvocation);
        return new int[]{sourceCallerNum, targetCallerNum, matchedCallerNum};
    }

    private void setRefactor(Map<MethodCaller, MethodCaller> refactors, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
        MethodCaller before = new MethodCaller();
        MethodCaller after = new MethodCaller();
        before.setCallerClassName(operationBefore.getClassName());
        before.setLocationInfo(new LocationInfo(operationBefore.getLocationInfo().getStartLine(), operationBefore.getLocationInfo().getEndLine(),
                operationBefore.getLocationInfo().getStartColumn(), operationBefore.getLocationInfo().getEndColumn()));
        before.setCallerType(MethodCaller.CallerType.METHOD);
        if (operationBefore instanceof UMLOperation)
            before.setCallerSignature(((UMLOperation) operationBefore).getMethodParamTypes());
        after.setCallerClassName(operationAfter.getClassName());
        after.setCallerType(MethodCaller.CallerType.METHOD);
        after.setLocationInfo(new LocationInfo(operationAfter.getLocationInfo().getStartLine(), operationAfter.getLocationInfo().getEndLine(),
                operationAfter.getLocationInfo().getStartColumn(), operationAfter.getLocationInfo().getEndColumn()));
        if (operationAfter instanceof UMLOperation)
            after.setCallerSignature(((UMLOperation) operationAfter).getMethodParamTypes());
        refactors.put(before, after);
    }

    private void setRefactor(Map<MethodCaller, MethodCaller> refactors, UMLAttribute operationBefore, UMLAttribute operationAfter) {
        MethodCaller before = new MethodCaller();
        MethodCaller after = new MethodCaller();
        before.setCallerClassName(operationBefore.getClassName());
        before.setLocationInfo(new LocationInfo(operationBefore.getLocationInfo().getStartLine(), operationBefore.getLocationInfo().getEndLine(),
                operationBefore.getLocationInfo().getStartColumn(), operationBefore.getLocationInfo().getEndColumn()));
        before.setCallerType(MethodCaller.CallerType.FIELD);
        before.setCallerSignature(operationBefore.getFieldSignature());
        after.setCallerClassName(operationAfter.getClassName());
        after.setCallerType(MethodCaller.CallerType.FIELD);
        after.setLocationInfo(new LocationInfo(operationAfter.getLocationInfo().getStartLine(), operationAfter.getLocationInfo().getEndLine(),
                operationAfter.getLocationInfo().getStartColumn(), operationAfter.getLocationInfo().getEndColumn()));
        after.setCallerSignature(operationAfter.getFieldSignature());
        refactors.put(before, after);
    }

    private void setRefactor(Map<String, String> refactors, String operationBefore, String operationAfter) {
        refactors.put(operationBefore, operationAfter);
    }

    private int calculateFrequency(List<MethodCaller> invocations, List<MethodCaller> matchedInvocations) {
        int frequency = 0;
        for (MethodCaller methodCaller : invocations)
            frequency += methodCaller.getCallerFrequency();
        for (MethodCaller methodCaller : matchedInvocations)
            frequency += methodCaller.getCallerFrequency();
        return frequency;
    }

    private int calculateMatchedFrequency(List<MethodCaller> matchedSourceInvocations, List<MethodCaller> matchedTargetInvocations) {
        int frequency = 0;
        for (int i = 0; i < matchedSourceInvocations.size(); i++)
            frequency += Math.min(matchedSourceInvocations.get(i).getCallerFrequency(), matchedTargetInvocations.get(i).getCallerFrequency());
        return frequency;
    }
}
