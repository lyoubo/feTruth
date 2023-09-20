package org.refactoringminer.api;

public class MappedElement {

    private int sourceLOE;
    private int targetLOE;
    private int mappedLOE;

    public MappedElement() {
    }

    public MappedElement(String mappings) {
        String[] elements = mappings.split(", ");
        for (String element : elements) {
            String[] map = element.split("=");
            switch (map[0]) {
                case "sourceLOE":
                    sourceLOE = Integer.parseInt(map[1]);
                    break;
                case "targetLOE":
                    targetLOE = Integer.parseInt(map[1]);
                    break;
                case "mappedLOE":
                    mappedLOE = Integer.parseInt(map[1]);
                    break;
            }
        }
    }

    public int getSourceLOE() {
        return sourceLOE;
    }

    public void setSourceLOE(int sourceLOE) {
        this.sourceLOE = sourceLOE;
    }

    public int getTargetLOE() {
        return targetLOE;
    }

    public void setTargetLOE(int targetLOE) {
        this.targetLOE = targetLOE;
    }

    public int getMappedLOE() {
        return mappedLOE;
    }

    public void setMappedLOE(int mappedLOE) {
        this.mappedLOE = mappedLOE;
    }

    @Override
    public String toString() {
        return "sourceLOE=" + sourceLOE +
                ", targetLOE=" + targetLOE +
                ", mappedLOE=" + mappedLOE;
    }
}
