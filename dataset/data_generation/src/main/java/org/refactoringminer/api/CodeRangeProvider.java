package org.refactoringminer.api;

import gr.uom.java.xmi.diff.CodeRange;

import java.util.List;

public interface CodeRangeProvider {
    List<CodeRange> leftSide();

    List<CodeRange> rightSide();
}
