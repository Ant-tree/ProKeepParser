package com.obfuscation.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsistencyScope implements Serializable {
    public static final String CLASS_NAME_SECTION_HEADER                    = "[CLASS-NAMES]";
    public static final String KEEP_METHOD_ACCESS_SECTION_HEADER            = "[KEEP-METHOD-ACCESS]";
    public static final String KEEP_FIELD_ACCESS_SECTION_HEADER             = "[KEEP-FIELD-ACCESS]";
    public static final String KEEP_METHOD_SIGNATURE_SECTION_HEADER         = "[KEEP-METHOD-SIGNATURE]";
    public static final String KEEP_FIELD_SIGNATURE_SECTION_HEADER          = "[KEEP-FIELD-SIGNATURE]";
    public static final String ACCESS_SEPARATOR                             = " -> ";

    public Set<String> classNames;
    public Map<String, Set<Integer>> keepMethodsAccess;
    public Map<String, Set<Integer>> keepFieldsAccess;
    public Set<String> methodSignatures;
    public Set<String> fieldSignatures;

    public void initialize() {
        classNames              = new HashSet<>();
        keepMethodsAccess       = new HashMap<>();
        keepFieldsAccess        = new HashMap<>();
        methodSignatures        = new HashSet<>();
        fieldSignatures         = new HashSet<>();
    }

    @Override
    public String toString() {
        return CLASS_NAME_SECTION_HEADER
                + "\n"
                + String.join("\n", classNames)
                + "\n"
                + KEEP_METHOD_ACCESS_SECTION_HEADER
                + "\n"
                + keepMethodsAccess.entrySet()
                .stream()
                .map(e -> e.getKey() + " -> " + e.getValue()
                        .stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(","))
                ).collect(Collectors.joining("\n"))
                + "\n"
                + KEEP_FIELD_ACCESS_SECTION_HEADER
                + "\n"
                + keepFieldsAccess.entrySet()
                .stream()
                .map(e -> e.getKey() + ACCESS_SEPARATOR + e.getValue()
                        .stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(","))
                ).collect(Collectors.joining("\n"))
                + "\n"
                + KEEP_METHOD_SIGNATURE_SECTION_HEADER
                + "\n"
                + String.join("\n", methodSignatures)
                + "\n"
                + KEEP_FIELD_SIGNATURE_SECTION_HEADER
                + "\n"
                + String.join("\n", fieldSignatures);
    }
}
