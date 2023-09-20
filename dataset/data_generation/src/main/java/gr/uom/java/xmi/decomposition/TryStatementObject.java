package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class TryStatementObject extends CompositeStatementObject {
    private List<CompositeStatementObject> catchClauses;
    private CompositeStatementObject finallyClause;

    public TryStatementObject(CompilationUnit cu, String filePath, Statement statement, int depth) {
        super(cu, filePath, statement, depth, CodeElementType.TRY_STATEMENT);
        this.catchClauses = new ArrayList<CompositeStatementObject>();
    }

    public void addCatchClause(CompositeStatementObject catchClause) {
        catchClauses.add(catchClause);
    }

    public List<CompositeStatementObject> getCatchClauses() {
        return catchClauses;
    }

    public CompositeStatementObject getFinallyClause() {
        return finallyClause;
    }

    public void setFinallyClause(CompositeStatementObject finallyClause) {
        this.finallyClause = finallyClause;
    }

    public boolean isTryWithResources() {
        return getExpressions().size() > 0;
    }
}
