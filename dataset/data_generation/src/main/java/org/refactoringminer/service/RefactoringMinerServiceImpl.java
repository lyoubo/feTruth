package org.refactoringminer.service;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLClassRenameDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jgit.lib.Repository;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class RefactoringMinerServiceImpl implements RefactoringMinerService {

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
    public void detectAll(List<String> projects) throws Exception {
        final String insertSQL = "INSERT INTO refactoring_miner(project_name, commit_id, refactor_description, " +
                "source_class_name, source_method_name, source_param_types, source_start_line, source_end_line, " +
                "source_start_column, source_end_column, source_file_path, target_class_name, target_method_name, " +
                "target_param_types, target_start_line, target_end_line, target_start_column, target_end_column, " +
                "target_file_path, target_original_class, target_original_path, mapped_element, filter_type, remote_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = rootPath + "dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner detector;
        for (String project : projects) {
            String projectPath = datasetPath + project;
            try (Repository repo = gitService.openRepository(projectPath)) {
                List<String> commits = GitTravellerUtils.getAllCommits(projectPath);
                String remoteUrl = GitTravellerUtils.getRemoteUrl(projectPath);
                detector = new GitHistoryRefactoringMinerImpl();
                for (String commit : commits) {
                    detector.detectAtEachCommit(repo, commit, new RefactoringHandler() {
                        @Override
                        public void handle(String commitId, UMLModelDiff modelDiff, List<Refactoring> refactorings) {
                            for (Refactoring ref : refactorings) {
                                try {
                                    MoveOperationRefactoring refactoring = (MoveOperationRefactoring) ref;
                                    MappedElement mappings = refactoring.getMappings();
                                    UMLOperation sourceMethod = refactoring.getOriginalOperation();
                                    UMLOperation targetMethod = refactoring.getMovedOperation();
                                    insertStmt.setString(1, project);
                                    insertStmt.setString(2, commitId);
                                    insertStmt.setString(3, ref.toString());
                                    insertStmt.setString(4, sourceMethod.getClassName());
                                    insertStmt.setString(5, sourceMethod.getName());
                                    if (sourceMethod.getMethodParamTypes() != null)
                                        insertStmt.setString(6, sourceMethod.getMethodParamTypes());
                                    else
                                        insertStmt.setString(6, sourceMethod.getParameterTypeList().stream().
                                                map(UMLType::toString).collect(Collectors.joining(",")));
                                    insertStmt.setInt(7, sourceMethod.getLocationInfo().getStartLine());
                                    insertStmt.setInt(8, sourceMethod.getLocationInfo().getEndLine());
                                    insertStmt.setInt(9, sourceMethod.getLocationInfo().getStartColumn());
                                    insertStmt.setInt(10, sourceMethod.getLocationInfo().getEndColumn());
                                    insertStmt.setString(11, sourceMethod.getLocationInfo().getFilePath());
                                    insertStmt.setString(12, targetMethod.getClassName());
                                    insertStmt.setString(13, targetMethod.getName());
                                    if (targetMethod.getMethodParamTypes() != null)
                                        insertStmt.setString(14, targetMethod.getMethodParamTypes());
                                    else
                                        insertStmt.setString(14, targetMethod.getParameterTypeList().stream().
                                                map(UMLType::toString).collect(Collectors.joining(",")));
                                    insertStmt.setInt(15, targetMethod.getLocationInfo().getStartLine());
                                    insertStmt.setInt(16, targetMethod.getLocationInfo().getEndLine());
                                    insertStmt.setInt(17, targetMethod.getLocationInfo().getStartColumn());
                                    insertStmt.setInt(18, targetMethod.getLocationInfo().getEndColumn());
                                    insertStmt.setString(19, targetMethod.getLocationInfo().getFilePath());
                                    insertStmt.setString(20, getOriginalClass(modelDiff, targetMethod.getClassName(), targetMethod.getLocationInfo().getFilePath()));
                                    insertStmt.setString(21, getOriginalPath(modelDiff, targetMethod.getLocationInfo().getFilePath()));
                                    insertStmt.setString(22, mappings == null ? "" : mappings.toString());
                                    insertStmt.setString(23, refactoring.getFilterType());
                                    insertStmt.setString(24, remoteUrl + commitId);
                                    insertStmt.addBatch();
                                    insertStmt.executeBatch();
                                } catch (SQLException ignored) {
                                }
                            }
                            try {
                                conn.commit();
                            } catch (SQLException ignored) {
                            }
                        }

                        @Override
                        public void handleException(String commit, Exception e) {
                        }
                    }, 60);
                }
            } catch (Exception ignored) {
            }
        }
        JDBCUtils.close(conn, insertStmt);
    }

    private String getOriginalClass(UMLModelDiff modelDiff, String targetClassName, String targetFilePath) {
        List<UMLClassRenameDiff> classRenameDiffList = modelDiff.getClassRenameDiffList();
        List<UMLClassMoveDiff> classMoveDiffList = modelDiff.getClassMoveDiffList();
        List<UMLClassMoveDiff> innerClassMoveDiffList = modelDiff.getInnerClassMoveDiffList();
        for (UMLClassRenameDiff umlClassRenameDiff : classRenameDiffList)
            if (umlClassRenameDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassRenameDiff.getOriginalClass().getName();
        for (UMLClassMoveDiff umlClassMoveDiff : classMoveDiffList)
            if (umlClassMoveDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassMoveDiff.getOriginalClass().getName();
        for (UMLClassMoveDiff umlClassMoveDiff : innerClassMoveDiffList)
            if (umlClassMoveDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassMoveDiff.getOriginalClass().getName();
        return targetClassName;
    }

    private String getOriginalPath(UMLModelDiff modelDiff, String targetFilePath) {
        List<UMLClassRenameDiff> classRenameDiffList = modelDiff.getClassRenameDiffList();
        List<UMLClassMoveDiff> classMoveDiffList = modelDiff.getClassMoveDiffList();
        List<UMLClassMoveDiff> innerClassMoveDiffList = modelDiff.getInnerClassMoveDiffList();
        for (UMLClassRenameDiff umlClassRenameDiff : classRenameDiffList)
            if (umlClassRenameDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassRenameDiff.getOriginalClass().getLocationInfo().getFilePath();
        for (UMLClassMoveDiff umlClassMoveDiff : classMoveDiffList)
            if (umlClassMoveDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassMoveDiff.getOriginalClass().getLocationInfo().getFilePath();
        for (UMLClassMoveDiff umlClassMoveDiff : innerClassMoveDiffList)
            if (umlClassMoveDiff.getNextClass().getLocationInfo().getFilePath().equals(targetFilePath))
                return umlClassMoveDiff.getOriginalClass().getLocationInfo().getFilePath();
        return targetFilePath;
    }
}
