package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.StringDistance;

import java.util.*;

public class LeafMapping extends AbstractCodeMapping implements Comparable<LeafMapping> {

    public LeafMapping(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
                       VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
        super(statement1, statement2, operation1, operation2);
    }

    private static String removeGenericTypeAfterDot(String s) {
        if (s.contains(".<")) {
            int indexOfGenericTypeStart = s.indexOf(".<");
            int indexOfGenericTypeEnd = s.indexOf(">", indexOfGenericTypeStart);
            if (indexOfGenericTypeStart < indexOfGenericTypeEnd) {
                s = s.substring(0, indexOfGenericTypeStart) + "." + s.substring(indexOfGenericTypeEnd + 1, s.length());
            }
        }
        return s;
    }

    @Override
    public int compareTo(LeafMapping o) {
        CompositeReplacement compositeReplacement1 = this.containsCompositeReplacement();
        CompositeReplacement compositeReplacement2 = o.containsCompositeReplacement();
        boolean concatenationReplacement1 = this.containsReplacement(ReplacementType.CONCATENATION);
        boolean concatenationReplacement2 = o.containsReplacement(ReplacementType.CONCATENATION);
        if (compositeReplacement1 != null || compositeReplacement2 != null) {
            if (compositeReplacement1 != null && compositeReplacement2 == null) {
                return -1;
            } else if (compositeReplacement1 == null && compositeReplacement2 != null) {
                return 1;
            } else {
                return -Integer.compare(compositeReplacement1.getTotalAdditionallyMatchedStatements(),
                        compositeReplacement2.getTotalAdditionallyMatchedStatements());
            }
        } else if (concatenationReplacement1 != concatenationReplacement2) {
            if (concatenationReplacement1 && !concatenationReplacement2) {
                return 1;
            } else {
                return -1;
            }
        } else {
            double distance1;
            double distance2;
            if (this.getFragment1().getString().equals(this.getFragment2().getString())) {
                distance1 = 0;
            } else {
                String s1 = removeGenericTypeAfterDot(this.getFragment1().getString().toLowerCase());
                String s2 = removeGenericTypeAfterDot(this.getFragment2().getString().toLowerCase());
                int distance = StringDistance.editDistance(s1, s2);
                distance1 = (double) distance / (double) Math.max(s1.length(), s2.length());
            }

            if (o.getFragment1().getString().equals(o.getFragment2().getString())) {
                distance2 = 0;
            } else {
                String s1 = removeGenericTypeAfterDot(o.getFragment1().getString().toLowerCase());
                String s2 = removeGenericTypeAfterDot(o.getFragment2().getString().toLowerCase());
                int distance = StringDistance.editDistance(s1, s2);
                distance2 = (double) distance / (double) Math.max(s1.length(), s2.length());
            }

            if (distance1 != distance2) {
                if (this.isIdenticalWithExtractedVariable() && !o.isIdenticalWithExtractedVariable()) {
                    return -1;
                } else if (!this.isIdenticalWithExtractedVariable() && o.isIdenticalWithExtractedVariable()) {
                    return 1;
                }
                if (this.isIdenticalWithInlinedVariable() && !o.isIdenticalWithInlinedVariable()) {
                    return -1;
                } else if (!this.isIdenticalWithInlinedVariable() && o.isIdenticalWithInlinedVariable()) {
                    return 1;
                }
                if (this.identicalDepthIndexAndParentType() && !o.identicalDepthIndexAndParentType()) {
                    return -1;
                } else if (!this.identicalDepthIndexAndParentType() && o.identicalDepthIndexAndParentType()) {
                    return 1;
                }
                if (this.referencesMapping(o)) {
                    return 1;
                } else if (o.referencesMapping(this)) {
                    return -1;
                }
                Set<ReplacementType> thisReplacementTypes = this.getReplacementTypes();
                Set<ReplacementType> otherReplacementTypes = o.getReplacementTypes();
                Set<ReplacementType> intersection = new LinkedHashSet<>(thisReplacementTypes);
                intersection.retainAll(otherReplacementTypes);
                if (intersection.size() > 0 && (intersection.equals(thisReplacementTypes) || intersection.equals(otherReplacementTypes))) {
                    Set<ReplacementType> thisReplacementTypesWithoutCommon = new LinkedHashSet<>(thisReplacementTypes);
                    thisReplacementTypesWithoutCommon.removeAll(intersection);
                    Set<ReplacementType> otherReplacementTypesWithoutCommon = new LinkedHashSet<>(otherReplacementTypes);
                    otherReplacementTypesWithoutCommon.removeAll(intersection);
                    int sameReplacementCount = 0;
                    for (ReplacementType type : intersection) {
                        String before = null, after = null;
                        for (Replacement r : this.getReplacements()) {
                            if (r.getType().equals(type)) {
                                before = r.getBefore();
                                after = r.getAfter();
                                break;
                            }
                        }
                        for (Replacement r : o.getReplacements()) {
                            if (r.getType().equals(type)) {
                                if (before.equals(r.getBefore()) || after.equals(r.getAfter())) {
                                    sameReplacementCount++;
                                    break;
                                }
                            }
                        }
                    }
                    int identicalNodesCount = 0;
                    for (ReplacementType type : thisReplacementTypesWithoutCommon) {
                        if (type.equals(ReplacementType.STRING_LITERAL) && o.getFragment1().getStringLiterals().equals(o.getFragment2().getStringLiterals()) && o.getFragment1().getStringLiterals().size() > 0) {
                            identicalNodesCount++;
                        } else if (type.equals(ReplacementType.NUMBER_LITERAL) && o.getFragment1().getNumberLiterals().equals(o.getFragment2().getNumberLiterals()) && o.getFragment1().getNumberLiterals().size() > 0) {
                            identicalNodesCount++;
                        }
                    }
                    for (ReplacementType type : otherReplacementTypesWithoutCommon) {
                        if (type.equals(ReplacementType.STRING_LITERAL) && this.getFragment1().getStringLiterals().equals(this.getFragment2().getStringLiterals()) && this.getFragment1().getStringLiterals().size() > 0) {
                            identicalNodesCount++;
                        } else if (type.equals(ReplacementType.NUMBER_LITERAL) && this.getFragment1().getNumberLiterals().equals(this.getFragment2().getNumberLiterals()) && this.getFragment1().getNumberLiterals().size() > 0) {
                            identicalNodesCount++;
                        }
                    }
                    boolean identicalNodes = false;
                    if (thisReplacementTypesWithoutCommon.size() > 0) {
                        identicalNodes = identicalNodesCount == thisReplacementTypesWithoutCommon.size();
                    } else if (otherReplacementTypesWithoutCommon.size() > 0) {
                        identicalNodes = identicalNodesCount == otherReplacementTypesWithoutCommon.size();
                    }
                    if (sameReplacementCount == intersection.size() && identicalNodes) {
                        if (intersection.equals(thisReplacementTypes) && !intersection.equals(otherReplacementTypes)) {
                            return -1;
                        } else if (intersection.equals(otherReplacementTypes) && !intersection.equals(thisReplacementTypes)) {
                            return 1;
                        }
                    }
                }
                return Double.compare(distance1, distance2);
            } else {
                int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
                int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());

                if (depthDiff1 != depthDiff2) {
                    return Integer.valueOf(depthDiff1).compareTo(Integer.valueOf(depthDiff2));
                } else {
                    int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
                    int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
                    if (indexDiff1 != indexDiff2) {
                        return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
                    } else {
                        boolean sameVariableDeclarationTypeInParent1 = this.sameVariableDeclarationTypeInParent();
                        boolean sameVariableDeclarationTypeInParent2 = o.sameVariableDeclarationTypeInParent();
                        double parentEditDistance1 = this.parentEditDistance();
                        double parentEditDistance2 = o.parentEditDistance();
                        Set<String> set1 = this.parentVariableTokenIntersection();
                        Set<String> set2 = o.parentVariableTokenIntersection();
                        if (parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && sameVariableDeclarationTypeInParent1 != sameVariableDeclarationTypeInParent2) {
                            if (sameVariableDeclarationTypeInParent1 && !sameVariableDeclarationTypeInParent2) {
                                return -1;
                            }
                            if (!sameVariableDeclarationTypeInParent1 && sameVariableDeclarationTypeInParent2) {
                                return 1;
                            }
                        } else if (parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && set1.size() != set2.size()) {
                            if (set1.size() > set2.size()) {
                                return -1;
                            } else if (set1.size() < set2.size()) {
                                return 1;
                            }
                        }
                        if (parentEditDistance1 == parentEditDistance2) {
                            int locationSum1 = this.getFragment1().getLocationInfo().getStartLine() + this.getFragment2().getLocationInfo().getStartLine();
                            int locationSum2 = o.getFragment1().getLocationInfo().getStartLine() + o.getFragment2().getLocationInfo().getStartLine();
                            return Integer.valueOf(locationSum1).compareTo(Integer.valueOf(locationSum2));
                        }
                        return Double.compare(parentEditDistance1, parentEditDistance2);
                    }
                }
            }
        }
    }

    private boolean referencesMapping(LeafMapping o) {
        if (getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
                getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
                o.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
                o.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
                this.getFragment1().equals(o.getFragment1()) &&
                o.getFragment2().getLocationInfo().getEndOffset() < this.getFragment2().getLocationInfo().getStartOffset()) {
            List<VariableDeclaration> variableDeclarations2 = o.getFragment2().getVariableDeclarations();
            Map<String, List<ObjectCreation>> creationMap2 = this.getFragment2().getCreationMap();
            for (VariableDeclaration declaration2 : variableDeclarations2) {
                for (String key : creationMap2.keySet()) {
                    List<ObjectCreation> creations = creationMap2.get(key);
                    for (ObjectCreation creation : creations) {
                        if (creation.getAnonymousClassDeclaration() != null) {
                            return false;
                        }
                        List<String> arguments = creation.getArguments();
                        if (arguments.size() == 1 && arguments.contains(declaration2.getVariableName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean identicalDepthIndexAndParentType() {
        if (getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
                getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT)) {
            return false;
        }
        CompositeStatementObject parent1 = getFragment1().getParent();
        while (parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent1 = parent1.getParent();
        }
        CompositeStatementObject parent2 = getFragment2().getParent();
        while (parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent2 = parent2.getParent();
        }
        if (parent1 != null && parent2 != null) {
            return parent1.getLocationInfo().getCodeElementType().equals(parent2.getLocationInfo().getCodeElementType()) &&
                    !parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                    getFragment1().getDepth() == getFragment2().getDepth() && getFragment1().getIndex() == getFragment2().getIndex();
        }
        return false;
    }

    private boolean sameVariableDeclarationTypeInParent() {
        CompositeStatementObject parent1 = getFragment1().getParent();
        while (parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent1 = parent1.getParent();
        }
        CompositeStatementObject parent2 = getFragment2().getParent();
        while (parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent2 = parent2.getParent();
        }
        if (parent1 != null && parent2 != null) {
            List<VariableDeclaration> declarations1 = parent1.getVariableDeclarations();
            List<VariableDeclaration> declarations2 = parent2.getVariableDeclarations();
            if (declarations1.size() == declarations2.size()) {
                for (int i = 0; i < declarations1.size(); i++) {
                    VariableDeclaration declaration1 = declarations1.get(i);
                    VariableDeclaration declaration2 = declarations2.get(i);
                    if (!declaration1.equalType(declaration2)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private Set<String> parentVariableTokenIntersection() {
        CompositeStatementObject parent1 = getFragment1().getParent();
        while (parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent1 = parent1.getParent();
        }
        CompositeStatementObject parent2 = getFragment2().getParent();
        while (parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent2 = parent2.getParent();
        }
        if (parent1 == null && parent2 == null) {
            //method signature is the parent
            return Collections.emptySet();
        } else if (parent1 == null && parent2 != null) {
            return Collections.emptySet();
        } else if (parent1 != null && parent2 == null) {
            return Collections.emptySet();
        }
        List<String> variables1 = parent1.getVariables();
        List<String> variables2 = parent2.getVariables();
        if (variables1.size() == 1 && variables2.size() == 1) {
            Set<String> tokens1 = new LinkedHashSet<>();
            for (String variable : variables1) {
                String[] array = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variable);
                for (String s : array) {
                    tokens1.add(s.toLowerCase());
                }
            }
            Set<String> tokens2 = new LinkedHashSet<>();
            for (String variable : variables2) {
                String[] array = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variable);
                for (String s : array) {
                    tokens2.add(s.toLowerCase());
                }
            }
            tokens1.retainAll(tokens2);
            return tokens1;
        }
        return Collections.emptySet();
    }

    private double parentEditDistance() {
        CompositeStatementObject parent1 = getFragment1().getParent();
        while (parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent1 = parent1.getParent();
        }
        CompositeStatementObject parent2 = getFragment2().getParent();
        while (parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            parent2 = parent2.getParent();
        }
        if (parent1 == null && parent2 == null) {
            //method signature is the parent
            return 0;
        } else if (parent1 == null && parent2 != null) {
            String s2 = parent2.getString();
            int distance = StringDistance.editDistance("{", s2);
            double normalized = (double) distance / (double) Math.max(1, s2.length());
            return normalized;
        } else if (parent1 != null && parent2 == null) {
            String s1 = parent1.getString();
            int distance = StringDistance.editDistance(s1, "{");
            double normalized = (double) distance / (double) Math.max(s1.length(), 1);
            return normalized;
        }
        String s1 = parent1.getString();
        String s2 = parent2.getString();
        int distance = StringDistance.editDistance(s1, s2);
        double normalized = (double) distance / (double) Math.max(s1.length(), s2.length());
        return normalized;
    }

    public Set<String> callChainIntersection() {
        AbstractCall invocation1 = this.getFragment1().invocationCoveringEntireFragment();
        AbstractCall invocation2 = this.getFragment2().invocationCoveringEntireFragment();
        return invocation1.callChainIntersection(invocation2);
    }
}
