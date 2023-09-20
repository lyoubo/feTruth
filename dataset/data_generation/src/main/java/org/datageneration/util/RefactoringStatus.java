package org.datageneration.util;

public class RefactoringStatus {

    public static final int OK = 0;
    public static final int ERROR = 3;
    private int fSeverity = OK;

    public boolean isOK() {
        return fSeverity == OK;
    }

    public boolean hasError() {
        return fSeverity == ERROR;
    }

    public void merge(RefactoringStatus other) {
        if (other == null)
            return;
        fSeverity = Math.max(fSeverity, other.getSeverity());
    }

    public int getSeverity() {
        return fSeverity;
    }

    public static RefactoringStatus createErrorStatus() {
        return createStatus(ERROR);
    }

    public static RefactoringStatus createStatus(int severity) {
        RefactoringStatus result = new RefactoringStatus();
        result.fSeverity = severity;
        return result;
    }
}
