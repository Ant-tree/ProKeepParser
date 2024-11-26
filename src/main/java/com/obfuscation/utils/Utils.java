package com.obfuscation.utils;

import com.googlecode.d2j.node.DexClassNode;
import com.obfuscation.model.ConsistencyScope;
import com.obfuscation.proconfig.ProConfigAdapter;

import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static String normalizeClassName(String className) {
        String normalizedClassName = className.trim();
        if (normalizedClassName.startsWith("L")) {
            normalizedClassName = normalizedClassName.substring(1);
        }
        if (normalizedClassName.endsWith(";")) {
            normalizedClassName = normalizedClassName.substring(0,
                    normalizedClassName.length() - 1
            );
        }
        return normalizedClassName.replace(".", "/");
    }

    public static void printClassPath(Map<String, DexClassNode> classPath) {
        classPath.forEach((className, dexClassNode) -> {
            Log.important("Processing class : " + className);
            Log.important("superClass : "           + dexClassNode.superClass);
            Log.important("interfaceNames : "       + String.join(", ", dexClassNode.interfaceNames));
            Log.important("fields : " + (dexClassNode.fields == null
                    ? "NULL"
                    : dexClassNode.fields
                    .stream()
                    .map(field -> field.field.getType() + " " + field.field.getName())
                    .collect(Collectors.joining(", "))));
            Log.important("method : " + (dexClassNode.methods == null
                    ? "NULL"
                    : dexClassNode.methods
                    .stream()
                    .map(method -> method.method.getName() + method.method.getDesc())
                    .collect(Collectors.joining(", "))));
            Log.important("anns : " + (dexClassNode.anns == null
                    ? "NULL"
                    : dexClassNode.anns
                    .stream()
                    .map(ann -> ann.type)
                    .collect(Collectors.joining(", "))));
            Log.important("---------------------------------------------");
        });
    }

    public static void printScope(ConsistencyScope scope) {
        Log.important("scope.classNames : " + scope.classNames.size());
        Log.important("scope.keepMethodsAccess : " + scope.keepMethodsAccess.size());
        Log.important("scope.keepFieldsAccess : " + scope.keepFieldsAccess.size());
        Log.important("scope.methodSignatures : " + scope.methodSignatures.size());
        Log.important("scope.fieldSignatures : " + scope.fieldSignatures.size());

        scope.keepMethodsAccess.forEach((key, value)
                -> Log.important("[KeepM] Class : " + key
                + " method access : " + value.stream()
                .map(i -> "[" + i + "]")
                .collect(Collectors.joining(", ")))
        );
        scope.keepFieldsAccess.forEach((key, value)
                -> Log.important("[KeepF] Class : " + key
                + " field access : " + value.stream()
                .map(i -> "[" + i + "]")
                .collect(Collectors.joining(", ")))
        );
        scope.methodSignatures.forEach(s -> Log.important("Method : " + s));
        scope.fieldSignatures.forEach(s -> Log.important("Field : " + s));
    }
}
