package org.datageneration;

import org.datageneration.service.TestingDataService;
import org.datageneration.service.TestingDataServiceImpl;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestingData {

    public static void main(String[] args) throws IOException, SQLException {
        TestingDataService service = new TestingDataServiceImpl();
        ClassLoader classLoader = TestingData.class.getClassLoader();
        URL resource = classLoader.getResource("testingApps.txt");
        File file = new File(resource.getFile());
        InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inputReader);
        String project;
        List<String> projects = new ArrayList<>();
        while ((project = reader.readLine()) != null)
            projects.add(project);
        service.generateData(projects);
    }
}
