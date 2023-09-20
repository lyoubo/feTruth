package org.refactoringminer.rm1;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.refactoringminer.api.*;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GitHistoryRefactoringMinerImpl implements GitHistoryRefactoringMiner {

    private final static Logger logger = LoggerFactory.getLogger(GitHistoryRefactoringMinerImpl.class);
    private Set<RefactoringType> refactoringTypesToConsider = null;

    public GitHistoryRefactoringMinerImpl() {
        this.setRefactoringTypesToConsider(RefactoringType.ALL);
    }

    public static List<MoveSourceFolderRefactoring> processIdenticalFiles(Map<String, String> fileContentsBefore, Map<String, String> fileContentsCurrent,
                                                                          Map<String, String> renamedFilesHint) throws IOException {
        Map<String, String> identicalFiles = new HashMap<String, String>();
        Map<Pair<String, String>, Integer> consistentSourceFolderChanges = new HashMap<>();
        Map<String, String> nonIdenticalFiles = new HashMap<String, String>();
        for (String key : fileContentsBefore.keySet()) {
            //take advantage of renamed file hints, if available
            if (renamedFilesHint.containsKey(key)) {
                String renamedFile = renamedFilesHint.get(key);
                String fileBefore = fileContentsBefore.get(key);
                String fileAfter = fileContentsCurrent.get(renamedFile);
                if (fileBefore.equals(fileAfter) || StringDistance.trivialCommentChange(fileBefore, fileAfter)) {
                    identicalFiles.put(key, renamedFile);
                    if (key.contains("/") && renamedFile.contains("/")) {
                        String prefix1 = key.substring(0, key.indexOf("/"));
                        String prefix2 = renamedFile.substring(0, renamedFile.indexOf("/"));
                        Pair<String, String> p = Pair.of(prefix1, prefix2);
                        if (consistentSourceFolderChanges.containsKey(p)) {
                            consistentSourceFolderChanges.put(p, consistentSourceFolderChanges.get(p) + 1);
                        } else {
                            consistentSourceFolderChanges.put(p, 1);
                        }
                    }
                } else {
                    nonIdenticalFiles.put(key, renamedFile);
                }
            }
            if (fileContentsCurrent.containsKey(key)) {
                String fileBefore = fileContentsBefore.get(key);
                String fileAfter = fileContentsCurrent.get(key);
                if (fileBefore.equals(fileAfter) || StringDistance.trivialCommentChange(fileBefore, fileAfter)) {
                    identicalFiles.put(key, key);
                } else {
                    nonIdenticalFiles.put(key, key);
                }
            }
        }
        fileContentsBefore.keySet().removeAll(identicalFiles.keySet());
        fileContentsCurrent.keySet().removeAll(identicalFiles.values());
        //second iteration to find renamed/moved files with identical contents
        for (String key1 : fileContentsBefore.keySet()) {
            if (!identicalFiles.containsKey(key1) && !nonIdenticalFiles.containsKey(key1)) {
                String prefix1 = key1.substring(0, key1.indexOf("/"));
                String fileBefore = fileContentsBefore.get(key1);
                boolean matchWithConsistentSourceFolderChangeFound = false;
                List<String> matches = new ArrayList<String>();
                for (String key2 : fileContentsCurrent.keySet()) {
                    if (!identicalFiles.containsValue(key2) && !nonIdenticalFiles.containsValue(key2)) {
                        String prefix2 = key2.substring(0, key2.indexOf("/"));
                        String fileAfter = fileContentsCurrent.get(key2);
                        if (fileBefore.equals(fileAfter) || StringDistance.trivialCommentChange(fileBefore, fileAfter)) {
                            if (consistentSourceFolderChanges.containsKey(Pair.of(prefix1, prefix2))) {
                                identicalFiles.put(key1, key2);
                                matchWithConsistentSourceFolderChangeFound = true;
                                break;
                            } else {
                                matches.add(key2);
                            }
                        }
                    }
                }
                if (!matchWithConsistentSourceFolderChangeFound) {
                    if (matches.size() == 1) {
                        identicalFiles.put(key1, matches.get(0));
                    } else if (matches.size() > 1) {
                        int minEditDistance = key1.length();
                        String bestMatch = null;
                        for (int i = 0; i < matches.size(); i++) {
                            String key2 = matches.get(i);
                            int editDistance = StringDistance.editDistance(key1, key2);
                            if (editDistance < minEditDistance) {
                                minEditDistance = editDistance;
                                bestMatch = key2;
                            }
                        }
                        if (bestMatch != null) {
                            identicalFiles.put(key1, bestMatch);
                        }
                    }
                }
            }
        }
        fileContentsBefore.keySet().removeAll(identicalFiles.keySet());
        fileContentsCurrent.keySet().removeAll(identicalFiles.values());

        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = new ArrayList<MoveSourceFolderRefactoring>();
        for (String key : identicalFiles.keySet()) {
            String originalPath = key;
            String movedPath = identicalFiles.get(key);
            String originalPathPrefix = "";
            if (originalPath.contains("/")) {
                originalPathPrefix = originalPath.substring(0, originalPath.lastIndexOf('/'));
            }
            String movedPathPrefix = "";
            if (movedPath.contains("/")) {
                movedPathPrefix = movedPath.substring(0, movedPath.lastIndexOf('/'));
            }
            if (!originalPathPrefix.equals(movedPathPrefix) && !key.endsWith("package-info.java")) {
                MovedClassToAnotherSourceFolder refactoring = new MovedClassToAnotherSourceFolder(null, null, originalPathPrefix, movedPathPrefix);
                RenamePattern renamePattern = refactoring.getRenamePattern();
                boolean foundInMatchingMoveSourceFolderRefactoring = false;
                for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : moveSourceFolderRefactorings) {
                    if (moveSourceFolderRefactoring.getPattern().equals(renamePattern)) {
                        moveSourceFolderRefactoring.putIdenticalFilePaths(originalPath, movedPath);
                        foundInMatchingMoveSourceFolderRefactoring = true;
                        break;
                    }
                }
                if (!foundInMatchingMoveSourceFolderRefactoring) {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = new MoveSourceFolderRefactoring(renamePattern);
                    moveSourceFolderRefactoring.putIdenticalFilePaths(originalPath, movedPath);
                    moveSourceFolderRefactorings.add(moveSourceFolderRefactoring);
                }
            }
        }
        return moveSourceFolderRefactorings;
    }

    public static void populateFileContents(Repository repository, RevCommit commit,
                                            Set<String> filePaths, Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
        logger.info("Processing {} {} ...", repository.getDirectory().getParent().toString(), commit.getName());
        RevTree parentTree = commit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(parentTree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String pathString = treeWalk.getPathString();
                if (filePaths.contains(pathString)) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(loader.openStream(), writer);
                    fileContents.put(pathString, writer.toString());
                }
                if (pathString.endsWith(".java") && pathString.contains("/")) {
                    String directory = pathString.substring(0, pathString.lastIndexOf("/"));
                    repositoryDirectories.add(directory);
                    //include sub-directories
                    String subDirectory = new String(directory);
                    while (subDirectory.contains("/")) {
                        subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
                        repositoryDirectories.add(subDirectory);
                    }
                }
            }
        }
    }

    public static UMLModel createModel(Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
        return new UMLModelASTReader(fileContents, repositoryDirectories).getUmlModel();
    }

    public void setRefactoringTypesToConsider(RefactoringType... types) {
        this.refactoringTypesToConsider = new HashSet<RefactoringType>();
        Collections.addAll(this.refactoringTypesToConsider, types);
    }

    private void detect(GitService gitService, Repository repository, final RefactoringHandler handler, Iterator<RevCommit> i) {
        int commitsCount = 0;
        int errorCommitsCount = 0;
        int refactoringsCount = 0;

        File metadataFolder = repository.getDirectory();
        File projectFolder = metadataFolder.getParentFile();
        String projectName = projectFolder.getName();

        long time = System.currentTimeMillis();
        while (i.hasNext()) {
            RevCommit currentCommit = i.next();
            try {
                List<Refactoring> refactoringsAtRevision = detectRefactorings(gitService, repository, handler, currentCommit);
                refactoringsCount += refactoringsAtRevision.size();

            } catch (Exception e) {
                logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
                handler.handleException(currentCommit.getId().getName(), e);
                errorCommitsCount++;
            }

            commitsCount++;
            long time2 = System.currentTimeMillis();
            if ((time2 - time) > 20000) {
                time = time2;
                logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
            }
        }

        handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
        logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
    }

    protected List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, RevCommit currentCommit) throws Exception {
        List<Refactoring> refactoringsAtRevision;
        String commitId = currentCommit.getId().getName();
        Set<String> filePathsBefore = new LinkedHashSet<String>();
        Set<String> filePathsCurrent = new LinkedHashSet<String>();
        Map<String, String> renamedFilesHint = new HashMap<String, String>();
        gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
        try (RevWalk walk = new RevWalk(repository)) {
            // If no java files changed, there is no refactoring. Also, if there are
            // only ADD's or only REMOVE's there is no refactoring
            if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParentCount() > 0) {
                RevCommit parentCommit = currentCommit.getParent(0);
                populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
                populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
                List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
                UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
                UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

                UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
                refactoringsAtRevision = modelDiff.getRefactorings();
                refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
                refactoringsAtRevision = filter(refactoringsAtRevision);
            } else {
                //logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
                refactoringsAtRevision = Collections.emptyList();
            }
            handler.handle(commitId, refactoringsAtRevision);

            walk.dispose();
        }
        return refactoringsAtRevision;
    }

    protected List<Refactoring> filter(List<Refactoring> refactoringsAtRevision) {
        if (this.refactoringTypesToConsider == null) {
            return refactoringsAtRevision;
        }
        List<Refactoring> filteredList = new ArrayList<Refactoring>();
        for (Refactoring ref : refactoringsAtRevision) {
            if (this.refactoringTypesToConsider.contains(ref.getRefactoringType())) {
                filteredList.add(ref);
            }
        }
        return filteredList;
    }

    private List<Refactoring> refine(List<Refactoring> refactoringsAtRevision, UMLModelDiff modelDiff) {
        List<Refactoring> refinedList = new ArrayList<Refactoring>();
        for (Refactoring ref : refactoringsAtRevision) {
            if (ref.getRefactoringType() != RefactoringType.MOVE_OPERATION &&
                    ref.getRefactoringType() != RefactoringType.MOVE_AND_RENAME_OPERATION)
                continue;
            MoveOperationRefactoring refactoring = (MoveOperationRefactoring) ref;
            UMLOperation source = refactoring.getOriginalOperation();
            UMLOperation target = refactoring.getMovedOperation();
            // 1. 检查是否存在source类被删除或者target类是新增的情况
            List<UMLClass> removedClasses = modelDiff.getRemovedClasses();
            List<UMLClass> addedClasses = modelDiff.getAddedClasses();
            if (removedClasses.stream().map(UMLClass::getName).collect(Collectors.toSet()).contains(source.getClassName()) &&
                    addedClasses.stream().map(UMLClass::getName).collect(Collectors.toSet()).contains(target.getClassName())) {
                refactoring.setFilterType("deletedSourceClass&addedTargetClass");
                refinedList.add(refactoring);
                continue;
            } else if (removedClasses.stream().map(UMLClass::getName).collect(Collectors.toSet()).contains(source.getClassName())) {
                refactoring.setFilterType("deletedSourceClass");
                refinedList.add(refactoring);
                continue;
            } else if (addedClasses.stream().map(UMLClass::getName).collect(Collectors.toSet()).contains(target.getClassName())) {
                refactoring.setFilterType("addedTargetClass");
                refinedList.add(refactoring);
                continue;
            }
            // 2.检查源类或目标类是否是rename或move操作
            boolean renamedClass = false;
            List<UMLClassRenameDiff> classRenameDiffList = modelDiff.getClassRenameDiffList();
            for (UMLClassRenameDiff umlClassRenameDiff : classRenameDiffList) {
                if (umlClassRenameDiff.getOriginalClass().getName().equals(source.getClassName()) &&
                        umlClassRenameDiff.getNextClass().getName().equals(target.getClassName())) {
                    renamedClass = true;
                    break;
                }
            }
            if (renamedClass) {
                refactoring.setFilterType("renamedClass");
                refinedList.add(refactoring);
                continue;
            }
            boolean movedClass = false;
            List<UMLClassMoveDiff> classMoveDiffList = modelDiff.getClassMoveDiffList();
            for (UMLClassMoveDiff umlClassMoveDiff : classMoveDiffList) {
                if (umlClassMoveDiff.getOriginalClass().getName().equals(source.getClassName()) &&
                        umlClassMoveDiff.getNextClass().getName().equals(target.getClassName())) {
                    movedClass = true;
                    break;
                }
            }
            if (movedClass) {
                refactoring.setFilterType("movedClass");
                refinedList.add(refactoring);
                continue;
            }
            boolean movedInnerClass = false;
            List<UMLClassMoveDiff> innerClassMoveDiffList = modelDiff.getInnerClassMoveDiffList();
            for (UMLClassMoveDiff umlClassMoveDiff : innerClassMoveDiffList) {
                if (umlClassMoveDiff.getOriginalClass().getName().equals(source.getClassName()) &&
                        umlClassMoveDiff.getNextClass().getName().equals(target.getClassName())) {
                    movedInnerClass = true;
                    break;
                }
            }
            if (movedInnerClass) {
                refactoring.setFilterType("movedInnerClass");
                refinedList.add(refactoring);
                continue;
            }
            // 3.检查move method是否是一个构造方法
            if (source.isConstructor() || target.isConstructor()) {
                refactoring.setFilterType("constructor");
                refinedList.add(refactoring);
                continue;
            }
            // 4.检查move method是否存在@Test注解
            if (source.hasTestAnnotation() || target.hasTestAnnotation()) {
                refactoring.setFilterType("testingMethod");
                refinedList.add(refactoring);
                continue;
            }
            List<UMLClassDiff> commonClassDiffList = modelDiff.getCommonClassDiffList();
            boolean testedClass = false;
            for (UMLClassDiff diff : commonClassDiffList) {
                UMLClass originalClass = diff.getOriginalClass();
                UMLClass nextClass = diff.getNextClass();
                if ((originalClass.getName().equals(source.getClassName()) && originalClass.isTestClass()) ||
                        nextClass.getName().equals(target.getClassName()) && nextClass.isTestClass()) {
                    testedClass = true;
                    break;
                }
            }
            if (testedClass) {
                refactoring.setFilterType("testingMethod");
                refinedList.add(refactoring);
                continue;
            }
            // 5.检查move method是否存在@Override注解
            if (source.hasOverrideAnnotation() || target.hasOverrideAnnotation() ||
                    modelDiff.isSubclassOf(source.getClassName(), target.getClassName()) ||
                    modelDiff.isSubclassOf(target.getClassName(), source.getClassName())) {
                refactoring.setFilterType("overridingMethod");
                refinedList.add(refactoring);
                continue;
            }
            // 6.检查move method是否是空方法体
            if (source.getBody() == null || target.getBody() == null) {
                refactoring.setFilterType("overriddenMethod");
                refinedList.add(refactoring);
                continue;
            }
            refinedList.add(refactoring);
        }
        return refinedList;
    }

    @Override
    public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
        GitService gitService = new GitServiceImpl() {
            @Override
            public boolean isCommitAnalyzed(String sha1) {
                return handler.skipCommit(sha1);
            }
        };
        RevWalk walk = gitService.createAllRevsWalk(repository, branch);
        try {
            detect(gitService, repository, handler, walk.iterator());
        } finally {
            walk.dispose();
        }
    }

    @Override
    public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
        GitService gitService = new GitServiceImpl();
        RevWalk walk = new RevWalk(repository);
        try {
            RevCommit commit = walk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                walk.parseCommit(commit.getParent(0));
                this.detectRefactorings(gitService, repository, handler, commit);
            } else {
                logger.warn(String.format("Ignored revision %s because it has no parent", commitId));
            }
        } catch (MissingObjectException moe) {
            logger.warn(String.format("Ignored revision %s due to missing object", commitId), moe);
        } catch (RefactoringMinerTimedOutException e) {
            logger.warn(String.format("Ignored revision %s due to timeout", commitId), e);
        } catch (Exception e) {
            logger.warn(String.format("Ignored revision %s due to error", commitId), e);
            handler.handleException(commitId, e);
        } finally {
            walk.close();
            walk.dispose();
        }
    }

    public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = null;
        try {
            Runnable r = () -> detectAtCommit(repository, commitId, handler);
            f = service.submit(r);
            f.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    @Override
    public void detectAtEachCommit(Repository repository, String commitId, RefactoringHandler handler) {
        GitService gitService = new GitServiceImpl();
        RevWalk walk = new RevWalk(repository);
        try {
            RevCommit commit = walk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                walk.parseCommit(commit.getParent(0));
                this.detectMoveMethodRefactorings(gitService, repository, handler, commit);
            } else {
                logger.warn(String.format("Ignored revision %s because it has no parent", commitId));
            }
        } catch (MissingObjectException moe) {
            logger.warn(String.format("Ignored revision %s due to missing object", commitId), moe);
        } catch (RefactoringMinerTimedOutException e) {
            logger.warn(String.format("Ignored revision %s due to timeout", commitId), e);
        } catch (Exception e) {
            logger.warn(String.format("Ignored revision %s due to error", commitId), e);
            handler.handleException(commitId, e);
        } finally {
            walk.close();
            walk.dispose();
        }
    }

    public void detectAtEachCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = null;
        try {
            Runnable r = () -> detectAtEachCommit(repository, commitId, handler);
            f = service.submit(r);
            f.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    protected List<Refactoring> detectMoveMethodRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, RevCommit currentCommit) throws Exception {
        List<Refactoring> refactoringsAtRevision;
        String commitId = currentCommit.getId().getName();
        Set<String> filePathsBefore = new LinkedHashSet<String>();
        Set<String> filePathsCurrent = new LinkedHashSet<String>();
        Map<String, String> renamedFilesHint = new HashMap<String, String>();
        gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
        try (RevWalk walk = new RevWalk(repository)) {
            // If no java files changed, there is no refactoring. Also, if there are
            // only ADD's or only REMOVE's there is no refactoring
            UMLModelDiff modelDiff = null;
            if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParentCount() > 0) {
                RevCommit parentCommit = currentCommit.getParent(0);
                populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
                populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
                List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
                UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
                UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

                modelDiff = parentUMLModel.diff(currentUMLModel);
                refactoringsAtRevision = modelDiff.getMoveMethodRefactorings();
                refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
                refactoringsAtRevision = filter(refactoringsAtRevision);
                refactoringsAtRevision = refine(refactoringsAtRevision, modelDiff);
            } else {
                //logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
                refactoringsAtRevision = Collections.emptyList();
            }
            handler.handle(commitId, modelDiff, refactoringsAtRevision);

            walk.dispose();
        }
        return refactoringsAtRevision;
    }
}
