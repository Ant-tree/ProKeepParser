package com.obfuscation.proconfig.utils;

import java.util.Iterator;
import java.util.List;

public class ClassUtil {

    public static String internalType(String externalType) {
        int dimensionCount = externalArrayTypeDimensionCount(externalType);
        if (dimensionCount > 0) {
            externalType = externalType.substring(0, externalType.length() - dimensionCount * "[]".length());
        }

        int internalTypeChar = externalType.equals("void") ? 86
                : (externalType.equals("boolean") ? 90
                : (externalType.equals("byte") ? 66
                : (externalType.equals("char") ? 67
                : (externalType.equals("short") ? 83
                : (externalType.equals("int") ? 73
                : (externalType.equals("float") ? 70
                : (externalType.equals("long") ? 74
                : (externalType.equals("double") ? 68
                : (externalType.equals("%") ? 37
                : 0)))))))));
        String internalType = internalTypeChar != 0
                ? String.valueOf((char)internalTypeChar)
                : 'L' + internalClassName(externalType) + ';';

        for(int count = 0; count < dimensionCount; ++count) {
            internalType = '[' + internalType;
        }

        return internalType;
    }

    public static String internalClassName(String externalClassName) {
        return externalClassName.replace('.', '/');
    }

    public static boolean isInitializer(String internalMethodName) {
        return internalMethodName.equals("<clinit>") || internalMethodName.equals("<init>");
    }

    public static String externalShortClassName(String externalClassName) {
        int index = externalClassName.lastIndexOf(46);
        return externalClassName.substring(index + 1);
    }

    public static int externalArrayTypeDimensionCount(String externalType) {
        int dimensions = 0;
        int length = "[]".length();

        for(int offset = externalType.length() - length; externalType.regionMatches(offset, "[]", 0, length); offset -= length) {
            ++dimensions;
        }

        return dimensions;
    }

    public static String internalMethodDescriptor(String externalReturnType, List<String> externalArguments) {
        StringBuilder internalMethodDescriptor = new StringBuilder();
        internalMethodDescriptor.append('(');
        Iterator var3 = externalArguments.iterator();

        while(var3.hasNext()) {
            String externalArgument = (String)var3.next();
            internalMethodDescriptor.append(internalType(externalArgument));
        }

        internalMethodDescriptor.append(')');
        internalMethodDescriptor.append(internalType(externalReturnType));
        return internalMethodDescriptor.toString();
    }

    public static int internalMethodParameterCount(String internalMethodDescriptor) {
        return internalMethodParameterCount(internalMethodDescriptor, true);
    }

    public static int internalMethodParameterCount(String internalMethodDescriptor, int accessFlags) {
        return internalMethodParameterCount(internalMethodDescriptor, (accessFlags & 8) != 0);
    }

    public static int internalMethodParameterCount(String internalMethodDescriptor, boolean isStatic) {
        int counter = isStatic ? 0 : 1;
        int index = 1;

        while(true) {
            char c = internalMethodDescriptor.charAt(index++);
            switch (c) {
                case ')':
                    return counter;
                case 'L':
                    ++counter;
                    index = internalMethodDescriptor.indexOf(59, index) + 1;
                    if (index == 0) {
                        throw new IllegalStateException("No matching semicolon found for class start character");
                    }
                case '[':
                    break;
                default:
                    ++counter;
            }
        }
    }
}
