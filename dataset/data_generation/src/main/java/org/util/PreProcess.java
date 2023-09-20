package org.util;

import org.apache.commons.io.FileUtils;
import org.datageneration.TestingData;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PreProcess {

    public static void main(String[] args) {
        String fileName = "E:/methodAndClassName.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            String datasetPath = "E:/dataset/";
            ClassLoader classLoader = TestingData.class.getClassLoader();
            URL resource = classLoader.getResource("projects.txt");
            File file = new File(resource.getFile());
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputReader);
            String project;
            List<String> projects = new ArrayList<>();
            while ((project = reader.readLine()) != null)
                projects.add(project);
            int i = 1;
            for (String name : projects) {
                System.out.println("第 " + i + " 个项目： " + name);
                String projectPath = datasetPath + name;
                File projectDir = new File(projectPath);
                List<String> filePaths = new ArrayList<>();
                traverseJavaFiles(projectDir, filePaths);
                for (String filePath : filePaths) {
                    ASTParser parser = ASTParserUtils.getASTParser();
                    try {
                        String code = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
                        parser.setSource(code.toCharArray());
                        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                        cu.accept(new ASTVisitor() {
                            @Override
                            public boolean visit(MethodDeclaration node) {
                                if (node.isConstructor())
                                    return true;
                                if (node.getBody() != null) {
                                    String methodDeclarationStatement = node.toString().replace(node.getBody().toString(), "");
                                    if (methodDeclarationStatement.contains("@Override")) {
                                        return true;
                                    }
                                }
                                ASTNode parent = node.getParent();
                                if (parent instanceof AbstractTypeDeclaration) {
                                    AbstractTypeDeclaration atd = (AbstractTypeDeclaration) parent;
//                                System.out.print(node.getName().getIdentifier() + " ");
//                                System.out.println(atd.getName().getIdentifier());
                                    try {
                                        List<String> methodNames = tokenize(node.getName().getIdentifier());
                                        List<String> classNames = tokenize(atd.getName().getIdentifier());
                                        List<String> combinedList = new ArrayList<>();
                                        combinedList.addAll(methodNames);
                                        combinedList.addAll(classNames);
                                        String names = combinedList.stream()
                                                .collect(Collectors.joining(" "));
                                        writer.write(names + "\n");
                                    } catch (IOException e) {
                                        System.out.println(e.getMessage());
                                    }
                                }
                                return true;
                            }
                        });
                    } catch (Exception ignored) {
                    }
                }
                writer.flush();
                i++;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void traverseJavaFiles(File directory, List<String> filePaths) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    traverseJavaFiles(file, filePaths);
                } else if (file.isFile() && file.getName().endsWith(".java")) {
                    filePaths.add(file.getAbsolutePath());
                }
            }
        }
    }

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        // 根据下划线和大写字母进行分词，并转换为小写
        String[] words = input.split("(?=[A-Z])|_");
        List<String> filteredWords = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                filteredWords.add(word.toLowerCase());
            }
        }

        // 输出分词并转换为小写后的结果
        for (String word : filteredWords)
            tokens.add(word);
        return tokens;
    }
}
