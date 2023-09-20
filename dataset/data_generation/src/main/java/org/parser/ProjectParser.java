package org.parser;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.util.ASTParserUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public abstract class ProjectParser {

    List<String> allJavaFiles;
    String[] sourcepathEntries;
    String[] encodings;

    public ProjectParser(String projectPath) {
        allJavaFiles = new ArrayList<>();
        traverseFile(Paths.get(projectPath).toFile());
        populateSourcepathEntries();
    }

    private void traverseFile(File root) {
        if (root.isFile()) {
            if (root.getName().endsWith(".java"))
                allJavaFiles.add(root.getAbsolutePath().replace("\\", "/"));
        } else if (root.isDirectory()) {
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files)
                    traverseFile(f);
            }
        }
    }

    private void populateSourcepathEntries() {
        Set<String> sourceRootSet = new HashSet<>();
        for (String filePath : allJavaFiles) {
            ASTParser parser = ASTParserUtils.getASTParser();
            try {
                String code = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
                parser.setSource(code.toCharArray());
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                if (cu.getPackage() == null) continue;
                String rootPath = parseRootPath(filePath, cu.getPackage().getName().toString());
                if (!rootPath.equals("") && Paths.get(rootPath).toFile().exists())
                    sourceRootSet.add(rootPath);
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
}
