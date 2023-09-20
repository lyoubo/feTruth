package org.datageneration.visitor;

import org.datageneration.util.RefactoringStatus;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;

import java.util.HashSet;
import java.util.Set;

/**
 * Partial implementation of an ast node finder.
 */
public class AstNodeFinder extends ASTVisitor {

    /**
     * The found ast nodes
     */
    protected final Set<Expression> fResult = new HashSet<>();

    /**
     * The status of the find operation
     */
    protected final RefactoringStatus fStatus = new RefactoringStatus();

    /**
     * Returns the result set.
     *
     * @return the result set
     */
    public final Set<Expression> getResult() {
        return fResult;
    }

    /**
     * Returns the status of the find operation.
     *
     * @return the status of the operation
     */
    public final RefactoringStatus getStatus() {
        return fStatus;
    }
}
