package com.obfuscation.proconfig;

import com.googlecode.d2j.node.DexClassNode;
import com.obfuscation.model.ConsistencyScope;
import com.obfuscation.constants.ResultCode;
import com.obfuscation.proconfig.specs.KeepClassSpecification;
import com.obfuscation.proconfig.specs.MemberSpecification;
import com.obfuscation.utils.Log;
import com.obfuscation.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ProConfigAdapter {
    private static final String PATTERN_ALLOW_SUBPACKAGES = "**";
    private static final String PATTERN_ALL_IN_THIS_PACKAGE = "*";
    public static final String PATTERN_WILDCARD = "*";
    private final Map<String, DexClassNode> classPath;

    private final ConsistencyScope scope;

    public ConsistencyScope getScope() {
        return scope;
    }

    public int writeAsFile(String filePath) {
        //Serialize list object into byte array
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(scope.toString());
            return ResultCode.SUCCESS;
        }  catch (Exception e) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            return ResultCode.FAILED;
        }
    }

    public ProConfigAdapter(Map<String, DexClassNode> classPath) {
        this.scope = new ConsistencyScope();
        this.classPath = classPath;
    }

    /**
     * Adapts the proguard configuration file specified by the given path and returns a set of class names
     * that should not be obfuscated based on the configuration.
     *
     * @param proConfigPath the path to the proguard configuration file. If null or empty, an empty set is returned.
     *
     */
    public int adapt(String proConfigPath) {
        scope.initialize();
        ProConfig proConfig = new ProConfig();

        if (proConfigPath == null || proConfigPath.isEmpty()) {
            return ResultCode.FAILED;
        }

        try (ProConfigKeepParser parser = new ProConfigKeepParser(
                new File(proConfigPath)
        )) {
            parser.parse(proConfig);
        } catch (IOException e) {
            return ResultCode.FAILED;
        }

        if (!proConfig.obfuscate) {
            return ResultCode.NO_OBF_SUPPORT;
        }

        for (KeepClassSpecification spec : proConfig.keep) {
            adaptProConfigKeeps(spec);
            adaptProConfigKeepMembers(spec);
        }

        return ResultCode.SUCCESS;
    }
    
    /**
     * Adapts the proguard configuration keeps based on the given specification.
     * This method filters class names that should not be obfuscated according to
     * the specified keep class rules.
     *
     * @param spec the {@link KeepClassSpecification} containing the rules for
     *             which classes should be kept from obfuscation. The specification
     *             includes conditions such as class name patterns, access flags,
     *             annotations, and inheritance.
     */
    private void adaptProConfigKeeps(KeepClassSpecification spec) {
        if (spec.allowObfuscation) {
            return;
        }
        if (!spec.markClasses && !spec.markConditionally) {
            return;
        }

        if (spec.className == null &&
                spec.requiredSetAccessFlags == 0 &&
                spec.annotationType == null &&
                spec.extendsClassName == null
        ) {
            return;
        }

        Map<String, DexClassNode> filtered = new HashMap<>(classPath);

        if (spec.className != null) {
            if (spec.className.endsWith(PATTERN_ALLOW_SUBPACKAGES)) {
                filtered = filterPatternWithSubpackages(
                        spec.className,
                        filtered
                );
            } else if (spec.className.endsWith(PATTERN_ALL_IN_THIS_PACKAGE)) {
                filtered = filterPatternWithClassName(
                        spec.className,
                        filtered
                );
            } else {
                scope.classNames.add(spec.className);
                return;
            }
        }

        if (spec.requiredSetAccessFlags != 0) {
            filtered = filterWithAccessFlags(
                    spec.requiredSetAccessFlags,
                    filtered
            );
        }

        if (spec.annotationType != null) {
            filtered = filterWithAnnotationType(
                    spec.annotationType,
                    filtered
            );
        }

        if (spec.extendsClassName != null) {
            filtered = filterWithExtendingClassName(
                    spec.extendsClassName,
                    filtered
            );
        }

        if (spec.methodSpecifications != null) {
            filtered = filterWithMethodSpecs(spec, filtered);
        }

        if (spec.fieldSpecifications != null) {
            filtered = filterWithFieldSpecs(spec, filtered);
        }

        //Not filtered
        if (filtered.size() ==  classPath.size()) {
            return;
        }

        scope.classNames.addAll(filtered.keySet());
    }

    /**
     * Adapts the proguard configuration to keep class members based on the given specification.
     * This method filters class members (methods and fields) that should not be obfuscated
     * according to the specified keep class member rules.
     *
     * @param spec the {@link KeepClassSpecification} containing the rules for
     *             which class members should be kept from obfuscation. The specification
     *             includes conditions such as class name patterns, access flags,
     *             annotations, and inheritance.
     */
    private void adaptProConfigKeepMembers(KeepClassSpecification spec) {
        if (spec.allowObfuscation) {
            return;
        }
        if (!spec.markClassMembers) {
            return;
        }

        if (spec.className == null &&
                spec.requiredSetAccessFlags == 0 &&
                spec.annotationType == null &&
                spec.extendsClassName == null &&
                spec.methodSpecifications == null &&
                spec.fieldSpecifications == null
        ) {
            return;
        }

        Map<String, DexClassNode> filtered = new HashMap<>(classPath);

        if (spec.className != null) {
            if (spec.className.endsWith(PATTERN_ALLOW_SUBPACKAGES)) {
                filtered = filterPatternWithSubpackages(
                        spec.className,
                        filtered
                );
            } else if (spec.className.endsWith(PATTERN_ALL_IN_THIS_PACKAGE)) {
                filtered = filterPatternWithClassName(
                        spec.className,
                        filtered
                );
            } else {
                scope.classNames.add(spec.className);
                return;
            }
        }

        if (spec.requiredSetAccessFlags != 0) {
            filtered = filterWithAccessFlags(
                    spec.requiredSetAccessFlags,
                    filtered
            );
        }

        if (spec.annotationType != null) {
            filtered = filterWithAnnotationType(
                    spec.annotationType,
                    filtered
            );
        }

        if (spec.extendsClassName != null) {
            filtered = filterWithExtendingClassName(
                    spec.extendsClassName,
                    filtered
            );
        }

        /// -keepclassmembers class * {
        ///   public static <fields>;
        ///   public *;
        /// }
        /// Keep the classes members from every class that,
        /// 1. fields that are public static
        /// 2. All public methods and fields
        if (spec.methodSpecifications != null) {
            filterMethodSpecs(spec, filtered);
        }

        if (spec.fieldSpecifications != null) {
            filterFieldSpecs(spec, filtered);
        }
    }

    /**
     * Accumulates access flags into a nested map structure, where each class name
     * is associated with a set of access flags.
     *
     * @param nestedMap the map where class names are keys and sets of access flags are values.
     *                  If the class name is not already present in the map, a new entry is created.
     * @param className the name of the class for which the access flags are being accumulated.
     * @param accessFlags the access flags to be added to the set associated with the class name.
     */
    private void accumulateIntoNestedMap(
            Map<String, Set<Integer>> nestedMap,
            String className,
            int accessFlags
    ) {
        if (!nestedMap.containsKey(className)) {
            nestedMap.put(className, new HashSet<>());
        }
        nestedMap.get(className).add(accessFlags);
    }

    private Map<String, DexClassNode> filterWithMethodSpecs(
            KeepClassSpecification spec,
            Map<String, DexClassNode> filtered
    ) {
        if (spec.methodSpecifications == null || spec.methodSpecifications.isEmpty()) {
            return filtered;
        }
        return filtered.entrySet().stream().filter(e -> {
            if (e.getValue().methods == null || e.getValue().methods.isEmpty()) {
                return false;
            }
            for (MemberSpecification methodSpec : spec.methodSpecifications) {
                boolean accessMatch;
                boolean methodNameMatch;
                boolean methodDescMatch;

                int requiredAccess = methodSpec.requiredSetAccessFlags;
                accessMatch = requiredAccess == 0 || e.getValue().methods
                        .stream()
                        .anyMatch(m -> (m.access & requiredAccess) == requiredAccess);

                methodNameMatch = methodSpec.name == null || e.getValue().methods
                        .stream()
                        .anyMatch(m -> methodSpec.name.equals(m.method.getName()));

                methodDescMatch = methodSpec.descriptor == null || e.getValue().methods
                        .stream()
                        .anyMatch(m -> methodSpec.descriptor.equals(m.method.getDesc()));

                if (accessMatch && methodNameMatch && methodDescMatch) {
                    return true;
                }
            }
            return false;

        }).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));
    }

    /**
     * Filters the given map of class wrappers based on field specifications.
     * This method evaluates each class in the map to determine if it contains
     * fields that match the criteria specified in the {@link KeepClassSpecification}.
     *
     * @param spec the {@link KeepClassSpecification} containing field specifications
     *             that define the conditions for filtering. Each field specification
     *             includes required access flags and field names that determine which
     *             fields should be considered.
     * @param filtered a map of class names to {@link DexClassNode} objects representing
     *                 the classes to be filtered. The map is filtered based on the field
     *                 specifications provided in the spec parameter.
     * @return a map containing only the entries from the input map whose fields match
     *         the specified field specifications. If no field specifications are provided,
     *         the original map is returned unmodified.
     */
    private Map<String, DexClassNode> filterWithFieldSpecs(
            KeepClassSpecification spec,
            Map<String, DexClassNode> filtered
    ) {
        if (spec.fieldSpecifications == null || spec.fieldSpecifications.isEmpty()) {
            return filtered;
        }
        return filtered.entrySet().stream().filter(e -> {
            if (e.getValue().fields == null || e.getValue().fields.isEmpty()) {
                return false;
            }
            for (MemberSpecification fieldSpec : spec.fieldSpecifications) {
                boolean accessMatch;
                boolean fieldNameMatch;

                int requiredAccess = fieldSpec.requiredSetAccessFlags;
                accessMatch = requiredAccess == 0 || e.getValue().fields
                        .stream()
                        .anyMatch(f -> (f.access & requiredAccess) == requiredAccess);

                fieldNameMatch = fieldSpec.name == null || e.getValue().fields
                        .stream()
                        .anyMatch(f -> fieldSpec.name.equals(f.field.getName()));

                if (accessMatch && fieldNameMatch) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));
    }

    /**
     * Filters the given map of class wrappers based on method specifications and accumulates
     * the required access flags into a nested map structure.
     *
     * @param spec     the {@link KeepClassSpecification} containing method specifications
     *                 that define the conditions for filtering. Each method specification
     *                 includes required access flags that determine which methods should
     *                 be considered.
     * @param filtered a map of class names to {@link DexClassNode} objects representing
     *                 the classes to be filtered. The map is filtered based on the method
     *                 specifications provided in the spec parameter.
     */
    private void filterMethodSpecs(
            KeepClassSpecification spec,
            Map<String, DexClassNode> filtered
    ) {
        for (MemberSpecification methodSpec : spec.methodSpecifications) {
            //If no access flags are specified
            if (methodSpec.requiredSetAccessFlags == 0) {
                if (methodSpec.name != null && methodSpec.descriptor != null) {
                    filtered.values().stream()
                            .filter(n ->    n.methods != null)
                            .flatMap(n ->   n.methods.stream())
                            .filter(m ->    m.method.getName().equals(methodSpec.name) &&
                                            m.method.getDesc().equals(methodSpec.descriptor))
                            .forEach(m ->   scope.methodSignatures.add(m.method.getOwner()
                                            + "."
                                            + m.method.getName()
                                            + m.method.getDesc())
                            );
                }
                continue;
            }
            if (methodSpec.name != null && methodSpec.descriptor != null) {
                filtered.values().stream()
                        .filter(n ->    n.methods != null)
                        .flatMap(n ->   n.methods.stream())
                        .filter(m ->    m.method.getName().equals(methodSpec.name) &&
                                        m.method.getDesc().equals(methodSpec.descriptor) &&
                                        (m.access & methodSpec.requiredSetAccessFlags) != methodSpec.requiredSetAccessFlags
                        ).forEach(m ->  scope.methodSignatures.add(Utils.normalizeClassName(m.method.getOwner())
                                        + "."
                                        + m.method.getName()
                                        + m.method.getDesc())
                        );
            } else {
                if (filtered.size() == classPath.size()) {
                    accumulateIntoNestedMap(
                            scope.keepMethodsAccess,
                            PATTERN_WILDCARD,
                            methodSpec.requiredSetAccessFlags
                    );
                } else {
                    filtered.keySet().forEach(className -> accumulateIntoNestedMap(
                            scope.keepMethodsAccess,
                            className,
                            methodSpec.requiredSetAccessFlags
                    ));
                }
            }
        }
    }

    /**
     * Filters the given map of class wrappers based on field specifications and accumulates
     * the required access flags into a nested map structure.
     *
     * @param spec     the {@link KeepClassSpecification} containing field specifications
     *                 that define the conditions for filtering. Each field specification
     *                 includes required access flags that determine which fields should
     *                 be considered.
     * @param filtered a map of class names to {@link DexClassNode} objects representing
     *                 the classes to be filtered. The map is filtered based on the field
     *                 specifications provided in the spec parameter.
     */
    private void filterFieldSpecs(
            KeepClassSpecification spec,
            Map<String, DexClassNode> filtered
    ) {
        for (MemberSpecification fieldSpec : spec.fieldSpecifications) {
            //If no access flags are specified
            if (fieldSpec.requiredSetAccessFlags == 0) {
                if (fieldSpec.name != null && fieldSpec.descriptor != null) {
                    filtered.values().stream()
                            .filter(n ->    n.fields != null)
                            .flatMap(n ->   n.fields.stream())
                            .filter(f ->    f.field.getName().equals(fieldSpec.name) &&
                                            f.field.getType().equals(fieldSpec.descriptor)
                            ).forEach(f ->  scope.fieldSignatures.add(f.field.getOwner()
                                            + "."
                                            + f.field.getName())
                            );
                }
                continue;
            }

            if (fieldSpec.name != null && fieldSpec.descriptor != null) {
                filtered.values().stream()
                        .filter(n ->    n.fields != null)
                        .flatMap(n ->   n.fields.stream())
                        .filter(f ->    f.field.getName().equals(fieldSpec.name) &&
                                        f.field.getType().equals(fieldSpec.descriptor) &&
                                        (f.access & fieldSpec.requiredSetAccessFlags) != fieldSpec.requiredSetAccessFlags
                        ).forEach(f ->  scope.fieldSignatures.add(f.field.getOwner()
                                        + "."
                                        + f.field.getName())
                        );
            } else {
                if (filtered.size() == classPath.size()) {
                    accumulateIntoNestedMap(
                            scope.keepFieldsAccess,
                            PATTERN_WILDCARD,
                            fieldSpec.requiredSetAccessFlags
                    );
                } else {
                    filtered.keySet().forEach(className -> accumulateIntoNestedMap(
                            scope.keepFieldsAccess,
                            className,
                            fieldSpec.requiredSetAccessFlags
                    ));
                }
            }
        }
    }

    /**
     * Filters the given map of class wrappers to include only those classes that extend
     * a specified superclass.
     *
     * @param extendsClassName the name of the superclass to filter by. Only classes that
     *                         directly extend this superclass will be included in the result.
     * @param filtered         the map of class names to DexClassNode objects to be filtered.
     * @return a map containing only the entries from the input map whose classes extend
     *         the specified superclass.
     */
    private static Map<String, DexClassNode> filterWithExtendingClassName(
            String extendsClassName,
            Map<String, DexClassNode> filtered
    ) {
        return filtered.entrySet().stream().filter(e -> {
            String superClassName = Utils.normalizeClassName(e.getValue().superClass);
            return superClassName.equals(extendsClassName)
                || stream(e.getValue().interfaceNames)
                    .map(Utils::normalizeClassName)
                    .anyMatch(s -> s.equals(extendsClassName));
        }).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));
    }

    /**
     * Filters the given map of class wrappers to include only those classes that have
     * annotations matching the specified annotation type.
     *
     * @param annotationType the type of annotation to filter by. Only classes with this
     *                       annotation type will be included in the result.
     * @param filtered       the map of class names to DexClassNode objects to be filtered.
     * @return a map containing only the entries from the input map whose classes have
     *         annotations matching the specified annotation type.
     */
    private static Map<String, DexClassNode> filterWithAnnotationType(
            String annotationType,
            Map<String, DexClassNode> filtered
    ) {
        return filtered.entrySet().stream().filter(e -> e.getValue().anns != null &&
                e.getValue().anns.stream()
                        .map(annotationNode -> Utils.normalizeClassName(annotationNode.type))
                        .anyMatch(annotationNodeDesc -> annotationNodeDesc.contains(annotationType))
        ).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));
    }

    /**
     * Filters the given map of class wrappers to include only those whose access flags
     * match the specified access flags.
     *
     * @param access the access flags to filter by. Only classes with these access flags
     *               set will be included in the result.
     * @param filtered the map of class names to DexClassNode objects to be filtered.
     * @return a map containing only the entries from the input map whose class access flags
     *         match the specified access flags.
     */
    private static Map<String, DexClassNode> filterWithAccessFlags(
            int access,
            Map<String, DexClassNode> filtered
    ) {
        return filtered.entrySet().stream()
                .filter(e -> (e.getValue().access & access) == access)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue
                ));
    }

    /**
     * Filters the given map of class wrappers to include only those whose class names
     * start with the specified package name, allowing for subpackages.
     *
     * @param className the class name pattern to filter by, ending with a double asterisk (**)
     *                  to indicate all classes in the specified package and its subpackages.
     * @param filtered  the map of class names to DexClassNode objects to be filtered.
     * @return a map containing only the entries from the input map whose class names
     *         start with the specified package name, including subpackages.
     */
    private static Map<String, DexClassNode> filterPatternWithSubpackages(
            String className,
            Map<String, DexClassNode> filtered
    ) {
        String packageName = className.substring(
                0, className.length() - PATTERN_ALLOW_SUBPACKAGES.length()
        );
        return filtered.entrySet().stream()
                .filter(e -> e.getKey().startsWith(packageName))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue
                ));
    }

    /**
     * Filters the given map of class wrappers to include only those whose class names
     * match the specified package pattern, excluding subpackages.
     *
     * @param className the class name pattern to filter by, ending with a single asterisk (*)
     *                  to indicate all classes in the specified package.
     * @param filtered  the map of class names to DexClassNode objects to be filtered.
     * @return a map containing only the entries from the input map whose class names
     *         start with the specified package name and do not include subpackages.
     */
    private static Map<String, DexClassNode> filterPatternWithClassName(
            String className,
            Map<String, DexClassNode> filtered
    ) {
        String packageName = className.substring(
                0, className.length() - PATTERN_ALL_IN_THIS_PACKAGE.length()
        );
        return filtered.entrySet().stream()
                .filter(e -> e.getKey().startsWith(packageName) &&
                        !e.getKey().substring(packageName.length()).contains("/")
                ).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private void printSpecs(ProConfig proConfig) {
        int index = 0;
        for (KeepClassSpecification spec : proConfig.keep) {

            Log.important("====> Keep [" + index + "] : " + spec.className + " :: " + spec.allowObfuscation
                    + ", markClasses : " + spec.markClasses
                    + ", markClassMembers : " + spec.markClassMembers
                    + ", markConditionally : " + spec.markConditionally
                    + ", requiredSetAccessFlags : " + String.format("0x%x", spec.requiredSetAccessFlags)
                    + ", requiredUnsetAccessFlags : " + String.format("0x%x", spec.requiredUnsetAccessFlags)
                    + ", annotationType : " + spec.annotationType
                    + ", extendsAnnotationType : " + spec.extendsAnnotationType
                    + ", extendsClassName : " + spec.extendsClassName
            );
            index ++;
            if (spec.methodSpecifications != null) {
                for (MemberSpecification specification : spec.methodSpecifications) {
                    Log.important("Method : " + "[n]"     + specification.name
                            + ", [d]"   + specification.descriptor
                            + ", [r]"   + specification.requiredSetAccessFlags
                            + ", [a]"   + specification.annotationType);
                }
            }
            if (spec.fieldSpecifications != null) {
                for (MemberSpecification specification : spec.fieldSpecifications) {
                    Log.important("Field : " + "[n]"     + specification.name
                            + ", [d]"   + specification.descriptor
                            + ", [r]"   + specification.requiredSetAccessFlags
                            + ", [a]"   + specification.annotationType);
                }
            }

            if(spec.condition != null) {
                Log.important("[Cond] className : " + spec.condition.className);
                Log.important("[Cond] Extends : " + spec.condition.extendsClassName);
                Log.important("[Cond] extendsAnnotationType : " + spec.condition.extendsAnnotationType);
                Log.important("[Cond] annotationType : " + spec.condition.annotationType);
                if (spec.condition.methodSpecifications != null) {
                    for (MemberSpecification specification : spec.condition.methodSpecifications) {
                        Log.important("[Cond] Method : " + "[n]"    + specification.name
                                                         + ", [d]"  + specification.descriptor
                                                         + ", [r]"  + specification.requiredSetAccessFlags);
                    }
                }
                if (spec.condition.fieldSpecifications != null) {
                    for (MemberSpecification specification : spec.condition.fieldSpecifications) {
                        Log.important("[Cond] Field : " + "[n]"     + specification.name
                                                        + ", [d]"   + specification.descriptor
                                                        + ", [r]"   + specification.requiredSetAccessFlags);
                    }
                }
            }
        }
    }

}
