package org.methodinvocation.dto;

public class MethodCaller {

    private String callerClassName;
    private CallerType callerType;
    private String callerSignature;
    private String callerName;
    private LocationInfo locationInfo;
    private String callerFilePath;
    private String calleeClassName;
    private String calleeMethodName;
    private String calleeParamTypes;
    private String calleeFilePath;
    private int callerFrequency;

    public String getCallerClassName() {
        return callerClassName;
    }

    public void setCallerClassName(String callerClassName) {
        this.callerClassName = callerClassName;
    }

    public String getCallerType() {
        return callerType.getName();
    }

    public void setCallerType(String callerType) {
        switch (callerType) {
            case "field":
                this.callerType = CallerType.FIELD;
                break;
            case "method":
                this.callerType = CallerType.METHOD;
                break;
            case "initializer":
                this.callerType = CallerType.INITIALIZER;
                break;
        }
    }

    public void setCallerType(CallerType callerType) {
        this.callerType = callerType;
    }

    public String getCallerSignature() {
        return callerSignature;
    }

    public void setCallerSignature(String callerSignature) {
        this.callerSignature = callerSignature;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getCallerFilePath() {
        return callerFilePath;
    }

    public void setCallerFilePath(String callerFilePath) {
        this.callerFilePath = callerFilePath;
    }

    public String getCalleeClassName() {
        return calleeClassName;
    }

    public void setCalleeClassName(String calleeClassName) {
        this.calleeClassName = calleeClassName;
    }

    public String getCalleeMethodName() {
        return calleeMethodName;
    }

    public void setCalleeMethodName(String calleeMethodName) {
        this.calleeMethodName = calleeMethodName;
    }

    public String getCalleeParamTypes() {
        return calleeParamTypes;
    }

    public void setCalleeParamTypes(String calleeParamTypes) {
        this.calleeParamTypes = calleeParamTypes;
    }

    public String getCalleeFilePath() {
        return calleeFilePath;
    }

    public void setCalleeFilePath(String calleeFilePath) {
        this.calleeFilePath = calleeFilePath;
    }

    public int getCallerFrequency() {
        return callerFrequency;
    }

    public void setCallerFrequency(int callerFrequency) {
        this.callerFrequency = callerFrequency;
    }

    public boolean equals(MethodCaller caller) {
        return this.callerType.equals(caller.callerType) &&
                this.callerClassName.equals(caller.callerClassName) &&
                this.callerSignature.equals(caller.callerSignature) &&
                this.callerFilePath.equals(caller.callerFilePath);
    }

    public boolean equalsByRefactoring(MethodCaller caller) {
        return this.callerType.equals(caller.callerType) &&
                this.callerClassName.equals(caller.callerClassName) &&
                this.locationInfo.getStartLine() == caller.getLocationInfo().getStartLine() &&
                this.locationInfo.getEndLine() == caller.getLocationInfo().getEndLine();
    }

    public boolean equalsOnlyBySignature(MethodCaller caller) {
        return this.callerType.equals(caller.callerType) &&
                this.callerSignature.equals(caller.callerSignature);
    }

    public enum CallerType {

        METHOD("method"),

        FIELD("field"),

        INITIALIZER("initializer");

        private final String name;

        CallerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
