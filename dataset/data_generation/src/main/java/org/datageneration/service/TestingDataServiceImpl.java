package org.datageneration.service;

import org.parser.TestingDataParser;
import org.refactoringminer.RefactoringMiner;
import org.util.JDBCUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class TestingDataServiceImpl implements TestingDataService {

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
    public void generateData(List<String> projects) throws SQLException {
        final String insertTestingSQL = "INSERT INTO testing_data_without_filtering(project_name, source_class_name, " +
                "method_name, method_signature, source_dist, source_cbmc, source_mcmc, target_class_list, target_dist_list, " +
                "target_cbmc_list, target_mcmc_list) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String insertHeuristicSQL = "INSERT INTO testing_data_with_filtering(project_name, source_class_name, " +
                "method_name, method_signature, source_dist, source_cbmc, source_mcmc, target_class_list, target_dist_list, " +
                "target_cbmc_list, target_mcmc_list) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String datasetPath = rootPath + "dataset/";
        Connection conn = JDBCUtils.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement insertTestingStmt = conn.prepareStatement(insertTestingSQL);
        PreparedStatement insertHeuristicStmt = conn.prepareStatement(insertHeuristicSQL);
        for (String project : projects) {
            try {
                String projectPath = datasetPath + project;
                TestingDataParser parser = new TestingDataParser(projectPath);
                List<Map<String, Object>> testingData = parser.generateTestingData(false);
                writeCandidatesIntoSQL(project, insertTestingStmt, testingData);
                conn.commit();
                parser.clear();
                List<Map<String, Object>> heuristicData = parser.generateTestingData(true);
                writeCandidatesIntoSQL(project, insertHeuristicStmt, heuristicData);
                conn.commit();
            } catch (Exception ignored) {
            }
        }
        JDBCUtils.close(conn, insertTestingStmt, insertHeuristicStmt);
    }

    private void writeCandidatesIntoSQL(String projectName, PreparedStatement insertStmt, List<Map<String, Object>> candidateFeatures) throws SQLException {
        for (Map<String, Object> candidateFeature : candidateFeatures) {
            insertStmt.setString(1, projectName);
            insertStmt.setString(2, candidateFeature.get("sourceClassName").toString());
            insertStmt.setString(3, candidateFeature.get("methodName").toString());
            insertStmt.setString(4, candidateFeature.get("methodSignature").toString());
            insertStmt.setString(5, candidateFeature.get("sourceDist").toString());
            insertStmt.setString(6, candidateFeature.get("sourceCBMC").toString());
            insertStmt.setString(7, candidateFeature.get("sourceMCMC").toString());
            insertStmt.setString(8, iterator2String(candidateFeature.get("targetClassList")));
            insertStmt.setString(9, iterator2String(candidateFeature.get("targetDistList")));
            insertStmt.setString(10, iterator2String(candidateFeature.get("targetCBMCList")));
            insertStmt.setString(11, iterator2String(candidateFeature.get("targetMCMCList")));
            insertStmt.addBatch();
            insertStmt.executeBatch();
        }
    }

    private String iterator2String(Object obj) {
        List<Object> list = (List<Object>) obj;
        return list.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
}
