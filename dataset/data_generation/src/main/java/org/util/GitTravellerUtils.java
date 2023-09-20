package org.util;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GitTravellerUtils {

    public static void checkoutCurrent(String projectPath, String commitId) throws GitAPIException, IOException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            CheckoutCommand checkout = git.checkout().setForced(true).setName(commitId);
            checkout.call();
        }
    }

    public static void checkoutParent(String projectPath, String commitId) throws GitAPIException, IOException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            RevCommit currentCommit = getRevCommit(git.getRepository(), commitId);
            CheckoutCommand checkout = git.checkout().setForced(true).setName(currentCommit.getParent(0).getName());
            checkout.call();
        }
    }

    private static RevCommit getRevCommit(Repository repository, String commitId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                walk.parseCommit(commit.getParent(0));
                return commit;
            }
        }
        throw new RuntimeException(String.format("Ignored revision %s because it has no parent", commitId));
    }

    public static List<String> getAllCommits(String projectPath) throws IOException, GitAPIException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            Iterable<RevCommit> logs = git.log().setRevFilter(RevFilter.NO_MERGES).call();
            List<String> commits = new ArrayList<>();
            for (RevCommit log : logs)
                commits.add(log.getName());
            return commits;
        }
    }

    public static String getRemoteUrl(String projectPath) throws IOException, GitAPIException {
        if (dotGitFound(projectPath))
            try (Git git = Git.open(new File(projectPath))) {
                List<RemoteConfig> configs = git.remoteList().call();
                for (RemoteConfig config : configs) {
                    List<URIish> urIs = config.getURIs();
                    for (URIish uri : urIs)
                        return uri.toString().startsWith("git@github.com") ? uri.toString().replace(":", "/").
                                replace("git@", "https://").replace(".git", "/commit/") :
                                uri.toString().replace(".git", "/commit/");
                }
            }
        return "";
    }

    public static String checkoutLatest(String projectPath) throws IOException, GitAPIException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            List<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                if (ref.getName().startsWith("refs/heads/")) {
                    String commitId = ref.getObjectId().getName();
                    CheckoutCommand checkout = git.checkout().setForced(true).setName(commitId);
                    checkout.call();
                    return commitId;
                }
            }
        }
        throw new RuntimeException("Failed to checkout its latest commit");
    }

    private static boolean dotGitFound(String projectPath) {
        File folder = new File(projectPath);
        if (folder.exists()) {
            String[] contents = folder.list();
            for (String content : contents)
                if (content.equals(".git"))
                    return true;
        }
        return false;
    }

    public static void resetHard(String projectPath) throws GitAPIException, IOException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            ResetCommand reset = git.reset().setMode(ResetCommand.ResetType.HARD);
            reset.call();
        }
    }

    public static void fileTreeDiff(String projectPath, String commitId, Set<String> addedFiles, Set<String> deletedFiles,
                                    Set<String> modifiedFiles, Map<String, String> renamedFiles) throws IOException, CanceledException {
        if (!dotGitFound(projectPath))
            throw new FileNotFoundException(projectPath);
        try (Git git = Git.open(new File(projectPath))) {
            Repository repository = git.getRepository();
            RevCommit currentCommit = getRevCommit(repository, commitId);
            if (currentCommit.getParentCount() > 0) {
                ObjectId oldTree = currentCommit.getParent(0).getTree();
                ObjectId newTree = currentCommit.getTree();
                final TreeWalk tw = new TreeWalk(repository);
                tw.setRecursive(true);
                tw.addTree(oldTree);
                tw.addTree(newTree);
                RenameDetector rd = new RenameDetector(repository);
                rd.addAll(DiffEntry.scan(tw));
                for (DiffEntry diff : rd.compute(tw.getObjectReader(), null)) {
                    DiffEntry.ChangeType changeType = diff.getChangeType();
                    String oldPath = diff.getOldPath();
                    String newPath = diff.getNewPath();
                    if (changeType == DiffEntry.ChangeType.ADD) {
                        if (isJavaFile(newPath))
                            addedFiles.add(newPath);
                    } else if (changeType == DiffEntry.ChangeType.DELETE) {
                        if (isJavaFile(oldPath))
                            deletedFiles.add(oldPath);
                    } else if (changeType == DiffEntry.ChangeType.MODIFY) {
                        if (isJavaFile(oldPath) && isJavaFile(newPath))
                            modifiedFiles.add(oldPath);
                    } else if (changeType == DiffEntry.ChangeType.RENAME) {
                        if (isJavaFile(oldPath) && isJavaFile(newPath))
                            renamedFiles.put(oldPath, newPath);
                    } else if (changeType == DiffEntry.ChangeType.COPY) {
                        if (isJavaFile(newPath))
                            addedFiles.add(newPath);
                    }
                }
            }
        }
    }

    private static boolean isJavaFile(String path) {
        return path.endsWith(".java");
    }
}
