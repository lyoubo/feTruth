package org.datageneration.util;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.HashMap;
import java.util.Map;

public class GetterSetterUtils {

    public static boolean isGetterOrSetter(IMethodBinding declaredMethod, IVariableBinding[] declaredFields) {
        Map<String, String> fields = new HashMap<>();
        for (IVariableBinding field : declaredFields) {
            String fieldName = field.getName();
            String fieldType = field.getType().getName();
            if (Character.isLowerCase(fieldName.charAt(0)))
                fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            fields.put(fieldName, fieldType);
        }
        String methodName = declaredMethod.getName();
        ITypeBinding[] parameterTypes = declaredMethod.getParameterTypes();
        if (parameterTypes.length > 1)
            return false;
        String parameter = null;
        if (parameterTypes.length == 1) {
            parameter = parameterTypes[0].getName();
        }
        if (methodName.startsWith("is") && declaredMethod.getReturnType() != null && "boolean".equals(declaredMethod.getReturnType().getName())) {
            if (fields.containsKey(methodName.substring(2)) && parameterTypes.length == 0 && fields.get(methodName.substring(2)).equals("boolean")) {
                return true;
            }
        }
        if (methodName.startsWith("get") && declaredMethod.getReturnType() != null) {
            if (fields.containsKey(methodName.substring(3)) && parameterTypes.length == 0 &&
                    fields.get(methodName.substring(3)).equals(declaredMethod.getReturnType().getName())) {
                return true;
            }
        }
        if (methodName.startsWith("set") && declaredMethod.getReturnType() != null && "void".equals(declaredMethod.getReturnType().getName())) {
            if (fields.containsKey(methodName.substring(3)) && parameterTypes.length == 1 &&
                    fields.get(methodName.substring(3)).equals(parameter)) {
                return true;
            }
        }
        return false;
    }
}
