package org.datageneration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.datageneration.util.TestingDataJSON;
import org.parser.TestingDataParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

public class TestingDataGeneration {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        String projectPath = args[0];
        File file = new File(projectPath);
        String projectName = file.getName();
        File directory = new File("./testing_data");
        if (!directory.exists())
            directory.mkdir();
        try (BufferedWriter out = new BufferedWriter(new FileWriter("./testing_data/" + projectName + ".json"))) {
            TestingDataParser parser = new TestingDataParser(projectPath);
            List<Map<String, Object>> testingData = parser.generateTestingData(true);
            TestingDataJSON results = new TestingDataJSON();
            results.populateJSON(projectName, testingData);
            String jsonString = gson.toJson(results, TestingDataJSON.class);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
