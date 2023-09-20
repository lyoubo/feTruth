package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.List;

public class PullUpOperationRefactoring extends MoveOperationRefactoring {

    public PullUpOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
        super(bodyMapper);
    }

    public PullUpOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
        super(originalOperation, movedOperation);
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.PULL_UP_OPERATION;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<CodeRange>();
        ranges.add(movedOperation.codeRange()
                .setDescription("pulled up method declaration")
                .setCodeElement(movedOperation.toString()));
        return ranges;
    }
}
