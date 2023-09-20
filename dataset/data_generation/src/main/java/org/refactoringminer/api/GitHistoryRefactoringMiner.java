package org.refactoringminer.api;

import org.eclipse.jgit.lib.Repository;

/**
 * Detect refactorings in the git history.
 */
public interface GitHistoryRefactoringMiner {

    /**
     * Iterate over each commit of a git repository and detect all refactorings performed in the
     * entire repository history. Merge commits are ignored to avoid detecting the same refactoring
     * multiple times.
     *
     * @param repository A git repository (from JGit library).
     * @param branch     A branch to start the log lookup. If null, commits from all branches are analyzed.
     * @param handler    A handler object that is responsible to process the detected refactorings and
     *                   control when to skip a commit.
     * @throws Exception propagated from JGit library.
     */
    void detectAll(Repository repository, String branch, RefactoringHandler handler) throws Exception;

    /**
     * Detect refactorings performed in the specified commit.
     *
     * @param repository A git repository (from JGit library).
     * @param commitId   The SHA key that identifies the commit.
     * @param handler    A handler object that is responsible to process the detected refactorings.
     */
    void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler);

    /**
     * Detect refactorings performed in the specified commit.
     *
     * @param repository A git repository (from JGit library).
     * @param commitId   The SHA key that identifies the commit.
     * @param handler    A handler object that is responsible to process the detected refactorings.
     * @param timeout    A timeout, in seconds. When timeout is reached, the operation stops and returns no refactorings.
     */
    void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout);

    void detectAtEachCommit(Repository repository, String commitId, RefactoringHandler handler);

    void detectAtEachCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout);
}
