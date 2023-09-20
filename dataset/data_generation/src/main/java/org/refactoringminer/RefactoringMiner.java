package org.refactoringminer;

import org.refactoringminer.service.RefactoringMinerService;
import org.refactoringminer.service.RefactoringMinerServiceImpl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RefactoringMiner {

    public static void main(String[] args) throws Exception {
        RefactoringMinerService service = new RefactoringMinerServiceImpl();
        ClassLoader classLoader = RefactoringMiner.class.getClassLoader();
        URL resource = classLoader.getResource("projects.txt");
        String filePath = resource.getFile();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
        String project;
        List<String> projects = new ArrayList<>();
        while ((project = reader.readLine()) != null)
            projects.add(project);
        service.detectAll(projects);
    }
}
