package org.datageneration.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.parser.TrainingDataParser;
import org.refactoringminer.RefactoringMiner;
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

public class TrainingDataServiceImpl implements TrainingDataService {

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
    public void generateTP() throws SQLException, GitAPIException, IOException {
        final String selectSQL = "SELECT DISTINCT project_name, commit_id FROM refactoring_miner";
        final String detailSQL = "SELECT * FROM refactoring_miner where project_name = ? AND commit_id = ?";
        final String insertTPSQL = "INSERT INTO positive_examples(project_name, commit_id, source_class_name, method_name, source_dist, " +
                "source_cbmc, source_mcmc, target_class_name, target_dist, target_cbmc, target_mcmc, " +
                "refactoring_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = "E:/dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
        PreparedStatement detailStmt = conn.prepareStatement(detailSQL);
        PreparedStatement insertTPStmt = conn.prepareStatement(insertTPSQL);
        ResultSet rs = selectStmt.executeQuery();
        while (rs.next()) {
            String projectName = rs.getString("project_name");
            String projectPath = datasetPath + projectName;
            String commitId = rs.getString("commit_id");
            try {
                GitTravellerUtils.resetHard(projectPath);
                GitTravellerUtils.checkoutParent(projectPath, commitId);
                TrainingDataParser parser = new TrainingDataParser(projectPath);
                detailStmt.setString(1, projectName);
                detailStmt.setString(2, commitId);
                ResultSet detailRs = detailStmt.executeQuery();
                while (detailRs.next()) {
                    int refactoringId = detailRs.getInt("id");
                    String sourceMethodName = detailRs.getString("source_method_name");
                    int sourceStartLine = detailRs.getInt("source_start_line");
                    int sourceEndLine = detailRs.getInt("source_end_line");
                    String sourceClassName = detailRs.getString("source_class_name");
                    String targetClassName = detailRs.getString("target_original_class");
                    String sourceFilePath = projectPath + "/" + detailRs.getString("source_file_path");
                    String targetFilePath = projectPath + "/" + detailRs.getString("target_original_path");
                    parser.populateData(sourceFilePath, targetFilePath, sourceMethodName, sourceStartLine, sourceEndLine, sourceClassName, targetClassName);
                    Map<String, Object> tpTrainingData = parser.generateTPTrainingData();
                    if (tpTrainingData == null) continue;
                    writeCandidatesIntoSql(insertTPStmt, tpTrainingData, refactoringId, projectName, commitId);
                }
            } catch (Exception ignored) {
            } finally {
                GitTravellerUtils.resetHard(projectPath);
            }
            conn.commit();
        }
        JDBCUtils.close(conn, selectStmt, insertTPStmt);
    }

    @Override
    public void generateTN() throws SQLException, GitAPIException, IOException {
        final String selectSQL = "SELECT DISTINCT project_name, commit_id FROM true_positives";
        String selectNameAndRefactoringIdSql = "SELECT method_name, refactoring_id FROM true_positives where project_name = ? AND commit_id = ?";
        final String insertTNSQL = "INSERT INTO negative_examples(project_name, commit_id, source_class_name, method_name, " +
                "source_dist, source_cbmc, source_mcmc, target_class_name, target_dist, target_cbmc, target_mcmc, " +
                "refactoring_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = rootPath + "dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
        PreparedStatement selectNameAndRefactoringIdStmt = conn.prepareStatement(selectNameAndRefactoringIdSql);
        PreparedStatement insertTNStmt = conn.prepareStatement(insertTNSQL);
        ResultSet rs = selectStmt.executeQuery();
        while (rs.next()) {
            String projectName = rs.getString(1);
            String commitId = rs.getString(2);
            selectNameAndRefactoringIdStmt.setString(1, projectName);
            selectNameAndRefactoringIdStmt.setString(2, commitId);
            ResultSet nameAndRefactoringIdRs = selectNameAndRefactoringIdStmt.executeQuery();
            Set<String> methodNames = new HashSet<>();
            List<Integer> refactoringIds = new ArrayList<>();
            int total = 0;
            while (nameAndRefactoringIdRs.next()) {
                total += 1;
                String methodName = nameAndRefactoringIdRs.getString(1);
                int refactoringId = nameAndRefactoringIdRs.getInt(2);
                methodNames.add(methodName);
                refactoringIds.add(refactoringId);
            }
            String projectPath = datasetPath + projectName;
            try {
                GitTravellerUtils.checkoutCurrent(projectPath, commitId);
                TrainingDataParser parser = new TrainingDataParser(projectPath);
                List<Map<String, Object>> tnTrainingData = parser.generateTNTrainingData(methodNames, total);
                for (int i = 0; i < tnTrainingData.size(); i++)
                    writeCandidatesIntoSql(insertTNStmt, tnTrainingData.get(i), refactoringIds.get(i), projectName, commitId);
                conn.commit();
                if (tnTrainingData.size() < refactoringIds.size()) {
                    String latestCommit = GitTravellerUtils.checkoutLatest(projectPath);
                    parser = new TrainingDataParser(projectPath);
                    List<Map<String, Object>> tnTrainingData2 = parser.generateTNTrainingData(methodNames, refactoringIds.size() - tnTrainingData.size());
                    for (int i = 0; i < tnTrainingData2.size(); i++)
                        writeCandidatesIntoSql(insertTNStmt, tnTrainingData2.get(i), refactoringIds.get(i + tnTrainingData.size()), projectName, latestCommit);
                    conn.commit();
                }
            } catch (Exception ignored) {
            } finally {
                GitTravellerUtils.resetHard(projectPath);
            }
        }
        JDBCUtils.close(conn, selectStmt, insertTNStmt, insertTNStmt);
    }

    private void writeCandidatesIntoSql(PreparedStatement insertStmt, Map<String, Object> candidateFeature, int refactoringId, String projectName, String commitId) throws SQLException {
        insertStmt.setString(1, projectName);
        insertStmt.setString(2, commitId);
        insertStmt.setString(3, candidateFeature.get("sourceClassName").toString());
        insertStmt.setString(4, candidateFeature.get("methodName").toString());
        insertStmt.setString(5, candidateFeature.get("sourceDist").toString());
        insertStmt.setString(6, candidateFeature.get("sourceCBMC").toString());
        insertStmt.setString(7, candidateFeature.get("sourceMCMC").toString());
        insertStmt.setString(8, candidateFeature.get("targetClassList").toString());
        insertStmt.setString(9, candidateFeature.get("targetDistList").toString());
        insertStmt.setString(10, candidateFeature.get("targetCBMCList").toString());
        insertStmt.setString(11, candidateFeature.get("targetMCMCList").toString());
        insertStmt.setInt(12, refactoringId);
        insertStmt.addBatch();
        insertStmt.executeBatch();
    }
}
