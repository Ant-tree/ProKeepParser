/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.obfuscation.proconfig;

import com.obfuscation.proconfig.constants.*;
import com.obfuscation.proconfig.reader.FileWordReader;
import com.obfuscation.proconfig.reader.WordReader;
import com.obfuscation.proconfig.specs.ClassSpecification;
import com.obfuscation.proconfig.specs.KeepClassSpecification;
import com.obfuscation.proconfig.specs.MemberSpecification;
import com.obfuscation.proconfig.specs.MemberValueSpecification;
import com.obfuscation.proconfig.utils.ClassUtil;
import com.obfuscation.proconfig.utils.ListUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class parses ProGuard configurations. Configurations can be read from an
 * array of arguments or from a configuration file or URL. External references
 * in file names ('&lt;...&gt;') can be resolved against a given set of properties.
 *
 * @author Eric Lafortune
 */
public class ProConfigKeepParser implements AutoCloseable {
    private final boolean useDalvikVerification = System.getProperty("proguard.use.dalvik.identifier.verification") != null;

    private final WordReader reader;
    private final Properties properties;

    private String nextWord;
    private String lastComments;

    /**
     * Creates a new ConfigurationParser for the given file, with the system
     * Properties.
     */
    public ProConfigKeepParser(File file) throws IOException {
        this(file, System.getProperties());
    }

    /**
     * Creates a new ConfigurationParser for the given file and the given
     * Properties.
     */
    public ProConfigKeepParser(
            File file,
            Properties properties
    ) throws IOException {
        this(new FileWordReader(file), properties);
    }

    /**
     * Creates a new ConfigurationParser for the given word reader and the
     * given Properties.
     */
    public ProConfigKeepParser(
            WordReader reader,
            Properties properties
    ) throws IOException {
        this.reader = reader;
        this.properties = properties;

        readNextWord();
    }
    
    public void parse(ProConfig proConfig) throws IOException {
        parseWord: while (nextWord != null) {
            lastComments = reader.lastComments();

            // First include directives.
            if (ConfigurationConstants.IF_OPTION                                  .startsWith(nextWord)) proConfig.keep      = parseIfCondition(proConfig.keep);
            else if (ConfigurationConstants.KEEP_OPTION                           .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, true,  true,  false, false, null);
            else if (ConfigurationConstants.KEEP_CLASS_MEMBERS_OPTION             .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, false, true,  false, false, null);
            else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBERS_OPTION      .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, false, true,  false, true, null);
            else if (ConfigurationConstants.KEEP_NAMES_OPTION                     .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, true,  true,  false, false, null);
            else if (ConfigurationConstants.KEEP_CLASS_MEMBER_NAMES_OPTION        .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, false, true,  false, false, null);
            else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, false, true,  false, true, null);
            else if (ConfigurationConstants.KEEP_CODE_OPTION                      .startsWith(nextWord)) proConfig.keep      = parseKeepClassSpecificationArguments(proConfig.keep, false, false, true,  false, null);
            else if (ConfigurationConstants.DONT_OBFUSCATE_OPTION                 .startsWith(nextWord)) proConfig.obfuscate = parseNoArgument(false);
            else {
                while (nextWord != null) {
                    readNextWord();
                    if (nextWord != null && nextWord.startsWith("-")) {
                        continue parseWord;
                    }
                }
            }
        }
    }


    /**
     * Closes the configuration.
     * @throws IOException if an IO error occurs while closing the configuration.
     */
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private boolean parseNoArgument(boolean value) throws IOException {
        readNextWord();

        return value;
    }

    /**
     * Parses and adds a conditional class specification to keep other classes
     * and class members.
     * For example: -if "public class SomeClass { void someMethod(); } -keep"
     * @throws RuntimeException if the class specification contains a syntax error.
     * @throws IOException    if an IO error occurs while reading the class
     *                        specification.
     */
    private List<KeepClassSpecification> parseIfCondition(
            List<KeepClassSpecification> keepClassSpecifications
    ) throws RuntimeException, IOException {
        // Read the condition.
        ClassSpecification condition = parseClassSpecificationArguments(true, true, false);

        // Read the corresponding keep option.
        if (nextWord == null) {
            throw new RuntimeException("Expecting '-keep' option after '-if' option, before " + reader.locationDescription());
        }

        if      (ConfigurationConstants.KEEP_OPTION                          .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, true,  true,  false, false, condition);
        else if (ConfigurationConstants.KEEP_CLASS_MEMBERS_OPTION            .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, false, true,  false, false, condition);
        else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBERS_OPTION     .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, false, true,  false, true, condition);
        else if (ConfigurationConstants.KEEP_NAMES_OPTION                    .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, true,  true,  false, false, condition);
        else if (ConfigurationConstants.KEEP_CLASS_MEMBER_NAMES_OPTION       .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, false, true,  false, false, condition);
        else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION.startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, false, true,  false, true, condition);
        else if (ConfigurationConstants.KEEP_CODE_OPTION                     .startsWith(nextWord)) keepClassSpecifications = parseKeepClassSpecificationArguments(keepClassSpecifications, false, false, true,  false, condition);
        else {
            throw new RuntimeException("Expecting '-keep' option after '-if' option, before " + reader.locationDescription());
        }

        return keepClassSpecifications;
    }


    /**
     * Parses and adds a class specification to keep classes and class members.
     * For example: -keep "public class SomeClass { void someMethod(); }"
     * @throws RuntimeException if the class specification contains a syntax error.
     * @throws IOException    if an IO error occurs while reading the class
     *                        specification.
     */
    private List<KeepClassSpecification> parseKeepClassSpecificationArguments(
            List<KeepClassSpecification> keepClassSpecifications,
            boolean markClasses,
            boolean markMembers,
            boolean markCodeAttributes,
            boolean markConditionally,
            ClassSpecification condition
    ) throws RuntimeException, IOException {
        // Create a new List if necessary.
        if (keepClassSpecifications == null) {
            keepClassSpecifications = new ArrayList<>();
        }

        // Read and add the keep configuration.
        keepClassSpecifications.add(parseKeepClassSpecificationArguments(
                markClasses,
                markMembers,
                markCodeAttributes,
                markConditionally,
                condition
        ));
        return keepClassSpecifications;
    }


    /**
     * Parses and returns a class specification to keep classes and class
     * members.
     * For example: -keep "public class SomeClass { void someMethod(); }"
     * @throws RuntimeException if the class specification contains a syntax error.
     * @throws IOException    if an IO error occurs while reading the class
     *                        specification.
     */
    private KeepClassSpecification parseKeepClassSpecificationArguments(
            boolean markClasses,
            boolean markMembers,
            boolean markCodeAttributes,
            boolean markConditionally,
            ClassSpecification condition
    ) throws RuntimeException, IOException {
        boolean markDescriptorClasses = false;
        boolean allowObfuscation      = false;

        // Read the keep modifiers.
        while (true) {
            readNextWord("keyword '" + ConfigurationConstants.CLASS_KEYWORD +
                            "', '" + JavaAccessConstants.INTERFACE +
                            "', or '" + JavaAccessConstants.ENUM + "'",
                    false,
                    true
            );

            if (!ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD.equals(nextWord)) {
                // Not a comma. Stop parsing the keep modifiers.
                break;
            }

            readNextWord("keyword '" + ConfigurationConstants.ALLOW_SHRINKING_SUBOPTION +
                    "', '" + ConfigurationConstants.ALLOW_OPTIMIZATION_SUBOPTION +
                    "', or '" + ConfigurationConstants.ALLOW_OBFUSCATION_SUBOPTION + "'"
            );

            if (ConfigurationConstants.INCLUDE_DESCRIPTOR_CLASSES_SUBOPTION.startsWith(nextWord)) {
                markDescriptorClasses = true;
            } else if (ConfigurationConstants.INCLUDE_CODE_SUBOPTION.startsWith(nextWord)) {
                markCodeAttributes = true;
            } else if (ConfigurationConstants.ALLOW_OBFUSCATION_SUBOPTION.startsWith(nextWord)) {
                allowObfuscation = true;
            } else if (ConfigurationConstants.ALLOW_SHRINKING_SUBOPTION.startsWith(nextWord) ||
                    ConfigurationConstants.ALLOW_OPTIMIZATION_SUBOPTION.startsWith(nextWord)
            ) {
                // Skip
            } else {
                throw new RuntimeException("Expecting keyword '" + ConfigurationConstants.INCLUDE_DESCRIPTOR_CLASSES_SUBOPTION +
                        "', '" + ConfigurationConstants.INCLUDE_CODE_SUBOPTION +
                        "', or '" + ConfigurationConstants.ALLOW_OBFUSCATION_SUBOPTION +
                        "' before " + reader.locationDescription());
            }
        }

        // Read the class configuration.
        ClassSpecification classSpecification = parseClassSpecificationArguments(false, true, false);

        // Create and return the keep configuration.
        return new KeepClassSpecification(markClasses,
                markMembers,
                markConditionally,
                markDescriptorClasses,
                markCodeAttributes,
                allowObfuscation,
                condition,
                classSpecification);
    }

    /**
     * Parses and returns a class specification.
     * For example: "public class SomeClass { public void someMethod(); }"
     * @throws RuntimeException if the class specification contains a syntax error.
     * @throws IOException    if an IO error occurs while reading the class
     *                        specification.
     */
    public ClassSpecification parseClassSpecificationArguments(
            boolean readFirstWord,
            boolean allowClassMembers,
            boolean allowValues
    ) throws RuntimeException, IOException {
        if (readFirstWord) {
            readNextWord("keyword '" + ConfigurationConstants.CLASS_KEYWORD +
                            "', '" + JavaAccessConstants.INTERFACE +
                            "', or '" + JavaAccessConstants.ENUM + "'",
                    false, true);
        }

        // Clear the annotation type.
        String annotationType = null;

        // Clear the class access modifiers.
        int requiredSetClassAccessFlags = 0;
        int requiredUnsetClassAccessFlags = 0;

        // Parse the class annotations and access modifiers until the class keyword.
        while (!ConfigurationConstants.CLASS_KEYWORD.equals(nextWord) && !configurationEnd(true)) {
            // Strip the negating sign, if any.
            boolean negated = nextWord.startsWith(ConfigurationConstants.NEGATOR_KEYWORD);

            String strippedWord = negated
                    ? nextWord.substring(1)
                    : nextWord;

            // Parse the class access modifiers.
            int accessFlag =
                strippedWord.equals(JavaAccessConstants.PUBLIC)     ? AccessConstants.PUBLIC      :
                strippedWord.equals(JavaAccessConstants.FINAL)      ? AccessConstants.FINAL       :
                strippedWord.equals(JavaAccessConstants.INTERFACE)  ? AccessConstants.INTERFACE   :
                strippedWord.equals(JavaAccessConstants.ABSTRACT)   ? AccessConstants.ABSTRACT    :
                strippedWord.equals(JavaAccessConstants.SYNTHETIC)  ? AccessConstants.SYNTHETIC   :
                strippedWord.equals(JavaAccessConstants.ANNOTATION) ? AccessConstants.ANNOTATION  :
                strippedWord.equals(JavaAccessConstants.ENUM)       ? AccessConstants.ENUM        :
                                                                      unknownAccessFlag();

            // Is it an annotation modifier?
            if (accessFlag == AccessConstants.ANNOTATION) {
                readNextWord("annotation type or keyword '" + JavaAccessConstants.INTERFACE + "'",
                        false, false);

                // Is the next word actually an annotation type?
                if (!nextWord.equals(JavaAccessConstants.INTERFACE) &&
                    !nextWord.equals(JavaAccessConstants.ENUM)      &&
                    !nextWord.equals(ConfigurationConstants.CLASS_KEYWORD))
                {
                    // Parse the annotation type.
                    annotationType = ListUtil.commaSeparatedString(parseCommaSeparatedList(
                            "annotation type",
                            false, false, false, true
                    ), false);

                    // Continue parsing the access modifier that we just read
                    // in the next cycle.
                    continue;
                }

                // Otherwise just handle the annotation modifier.
            }

            if (!negated)
            {
                requiredSetClassAccessFlags   |= accessFlag;
            }
            else
            {
                requiredUnsetClassAccessFlags |= accessFlag;
            }

            if ((requiredSetClassAccessFlags &
                 requiredUnsetClassAccessFlags) != 0)
            {
                throw new RuntimeException("Conflicting class access modifiers for '" + strippedWord +
                                         "' before " + reader.locationDescription());
            }

            if (strippedWord.equals(JavaAccessConstants.INTERFACE) ||
                strippedWord.equals(JavaAccessConstants.ENUM)      ||
                strippedWord.equals(ConfigurationConstants.CLASS_KEYWORD))
            {
                // The interface or enum keyword. Stop parsing the class flags.
                break;
            }

            // Should we read the next word?
            if (accessFlag != AccessConstants.ANNOTATION)
            {
                readNextWord("keyword '" + ConfigurationConstants.CLASS_KEYWORD +
                             "', '"      + JavaAccessConstants.INTERFACE +
                             "', or '"   + JavaAccessConstants.ENUM + "'",
                        false, true
                );
            }
        }

       // Parse the class name part.
        String externalClassName = ListUtil.commaSeparatedString(
                parseCommaSeparatedList(
                        "class name or interface name",
                        true, false, false, false
                ), false
        );

        // For backward compatibility, allow a single "*" wildcard to match any
        // class.
        String className = ConfigurationConstants.ANY_CLASS_KEYWORD.equals(externalClassName) ?
            null :
            ClassUtil.internalClassName(externalClassName);

        // Clear the annotation type and the class name of the extends part.
        String extendsAnnotationType = null;
        String extendsClassName      = null;

        if (allowClassMembers && !configurationEnd()) {
            // Parse 'implements ...' or 'extends ...' part, if any.
            if (ConfigurationConstants.IMPLEMENTS_KEYWORD.equals(nextWord) ||
                    ConfigurationConstants.EXTENDS_KEYWORD.equals(nextWord)
            ) {
                readNextWord("class name or interface name", false, true);

                // Parse the annotation type, if any.
                if (ConfigurationConstants.ANNOTATION_KEYWORD.equals(nextWord)) {
                    extendsAnnotationType = ListUtil.commaSeparatedString(
                            parseCommaSeparatedList(
                                    "annotation type",
                                    true, false, false, true
                            ), false
                    );
                }

                String externalExtendsClassName = ListUtil.commaSeparatedString(
                        parseCommaSeparatedList(
                                "class name or interface name",
                                false, false, false, false
                        ), false
                );

                extendsClassName = ConfigurationConstants.ANY_CLASS_KEYWORD.equals(externalExtendsClassName)
                        ? null
                        : ClassUtil.internalClassName(externalExtendsClassName);
            }
        }

        // Create the basic class specification.
        ClassSpecification classSpecification = new ClassSpecification(lastComments,
                requiredSetClassAccessFlags,
                requiredUnsetClassAccessFlags,
                annotationType,
                className,
                extendsAnnotationType,
                extendsClassName
        );


        // Now add any class members to this class specification.
        if (allowClassMembers && !configurationEnd()) {
            // Check the class member opening part.
            if (!ConfigurationConstants.OPEN_KEYWORD.equals(nextWord)) {
                throw new RuntimeException("Expecting opening '" + ConfigurationConstants.OPEN_KEYWORD +
                        "' at " + reader.locationDescription());
            }

            // Parse all class members.
            while (true) {
                readNextWord("class member description" +
                                " or closing '" + ConfigurationConstants.CLOSE_KEYWORD + "'",
                        false, true);

                if (nextWord.equals(ConfigurationConstants.CLOSE_KEYWORD)) {
                    lastComments = reader.lastComments();
                    classSpecification.memberComments = lastComments;

                    // The closing brace. Stop parsing the class members.
                    readNextWord();

                    break;
                }

                parseMemberSpecificationArguments(externalClassName,
                        allowValues,
                        classSpecification);
            }
        }

        return classSpecification;
    }


    /**
     * Parses and adds a class member specification.
     * @throws RuntimeException if the class specification contains a syntax error.
     * @throws IOException    if an IO error occurs while reading the class
     *                        specification.
     */
    private void parseMemberSpecificationArguments(
            String externalClassName,
            boolean allowValues,
            ClassSpecification classSpecification
    ) throws RuntimeException, IOException {
        // Clear the annotation name.
        String annotationType = null;

        // Parse the class member access modifiers, if any.
        int requiredSetMemberAccessFlags   = 0;
        int requiredUnsetMemberAccessFlags = 0;

        while (!configurationEnd(true)) {
            // Parse the annotation type, if any.
            if (ConfigurationConstants.ANNOTATION_KEYWORD.equals(nextWord)) {
                annotationType = ListUtil.commaSeparatedString(parseCommaSeparatedList(
                        "annotation type",
                        true, false, false, true
                ), false);
                continue;
            }

            String strippedWord = nextWord.startsWith("!")
                    ? nextWord.substring(1)
                    : nextWord;

            // Parse the class member access modifiers.
            int accessFlag =
                strippedWord.equals(JavaAccessConstants.PUBLIC)       ? AccessConstants.PUBLIC       :
                strippedWord.equals(JavaAccessConstants.PRIVATE)      ? AccessConstants.PRIVATE      :
                strippedWord.equals(JavaAccessConstants.PROTECTED)    ? AccessConstants.PROTECTED    :
                strippedWord.equals(JavaAccessConstants.STATIC)       ? AccessConstants.STATIC       :
                strippedWord.equals(JavaAccessConstants.FINAL)        ? AccessConstants.FINAL        :
                strippedWord.equals(JavaAccessConstants.SYNCHRONIZED) ? AccessConstants.SYNCHRONIZED :
                strippedWord.equals(JavaAccessConstants.VOLATILE)     ? AccessConstants.VOLATILE     :
                strippedWord.equals(JavaAccessConstants.TRANSIENT)    ? AccessConstants.TRANSIENT    :
                strippedWord.equals(JavaAccessConstants.BRIDGE)       ? AccessConstants.BRIDGE       :
                strippedWord.equals(JavaAccessConstants.VARARGS)      ? AccessConstants.VARARGS      :
                strippedWord.equals(JavaAccessConstants.NATIVE)       ? AccessConstants.NATIVE       :
                strippedWord.equals(JavaAccessConstants.ABSTRACT)     ? AccessConstants.ABSTRACT     :
                strippedWord.equals(JavaAccessConstants.STRICT)       ? AccessConstants.STRICT       :
                strippedWord.equals(JavaAccessConstants.SYNTHETIC)    ? AccessConstants.SYNTHETIC    :
                                                                        0;
            if (accessFlag == 0) {
                // Not a class member access modifier. Stop parsing them.
                break;
            }

            if (strippedWord.equals(nextWord)) {
                requiredSetMemberAccessFlags |= accessFlag;
            } else {
                requiredUnsetMemberAccessFlags |= accessFlag;
            }

            // Make sure the user doesn't try to set and unset the same
            // access flags simultaneously.
            if ((requiredSetMemberAccessFlags & requiredUnsetMemberAccessFlags) != 0) {
                throw new RuntimeException("Conflicting class member access modifiers for " +
                        reader.locationDescription());
            }

            readNextWord("class member description");
        }

        // Parse the class member type and name part.

        // Did we get a special wildcard?
        boolean isStar = ConfigurationConstants.ANY_CLASS_MEMBER_KEYWORD.equals(nextWord);
        boolean isFields = ConfigurationConstants.ANY_FIELD_KEYWORD.equals(nextWord);
        boolean isMethods = ConfigurationConstants.ANY_METHOD_KEYWORD.equals(nextWord);
        boolean isFieldsOrMethods = isFields || isMethods;

        String type = nextWord;
        String typeLocation = reader.locationDescription();

        // Try to read the class member name; we need to do this now so that we can check the nextWord
        // to see if we're parsing a wildcard type.
        readNextWord("class member name", false, false);

        // Is it a wildcard star (short for all members) or is a type wildcard?
        boolean isReallyStar = isStar && ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord);

        if (isFieldsOrMethods || isReallyStar) {
            // Act according to the type of wildcard.
            if (isStar) {
                checkFieldAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );
                checkMethodAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                classSpecification.addField(new MemberSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        null,
                        null
                ));
                classSpecification.addMethod(new MemberSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        null,
                        null
                ));
            } else if (isFields) {
                checkFieldAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                classSpecification.addField(new MemberSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        null,
                        null
                ));
            } else if (isMethods) {
                checkMethodAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                classSpecification.addMethod(new MemberSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        null,
                        null
                ));
            }

            if (!ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord)) {
                throw new RuntimeException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                        "' before " + reader.locationDescription());
            }
        } else {
            String name = nextWord;
            checkJavaIdentifier("java type", type, true);

            // Did we get just one word before the opening parenthesis?
            if (ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD.equals(name)) {
                // This must be an initializer then.
                // Make sure the type is a proper initializer name.
                if (ClassUtil.isInitializer(type)) {
                    name = type; // This is either `<init>` or `<clinit>`.
                    type = JavaTypeConstants.VOID;
                } else if (type.equals(externalClassName) ||
                    type.equals(ClassUtil.externalShortClassName(externalClassName))
                ) {
                    name = ClassConstants.METHOD_NAME_INIT;
                    type = JavaTypeConstants.VOID;
                } else {
                    throw new RuntimeException("Expecting type and name " +
                            "instead of just '" + type +
                            "' before " + reader.locationDescription());
                }
            } else {
                // It's not an initializer.
                // Make sure we have a proper name.
                checkNextWordIsJavaIdentifier("class member name");

                // Read the opening parenthesis or the separating
                // semi-colon.
                readNextWord("opening '" + ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD +
                        "' or separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");
            }

            // Check if the type actually contains the use of generics.
            // Can not do it right away as we also support "<init>" and "<clinit>" as a type (see case above).
            if (containsGenerics(type)) {
                throw new RuntimeException("Generics are not allowed (erased) for java type" + typeLocation);
            }

            // Are we looking at a field, a method, or something else?
            if (ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord)) {
                // It's a field.
                checkFieldAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                // We already have a field descriptor.
                String descriptor = ClassUtil.internalType(type);

                if (ConfigurationConstants.ANY_FIELD_KEYWORD.equals(name)) {
                    throw new RuntimeException("Not expecting field type before with wildcard '" + ConfigurationConstants.ANY_FIELD_KEYWORD +
                            "before " + reader.locationDescription() +
                            " (use '" + ConfigurationConstants.ANY_CLASS_MEMBER_KEYWORD + "' instead)");
                }

                // Add the field.
                classSpecification.addField(new MemberSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        name,
                        descriptor
                ));
            } else if (allowValues &&
                    (ConfigurationConstants.EQUAL_KEYWORD.equals(nextWord) ||
                            ConfigurationConstants.RETURN_KEYWORD.equals(nextWord))) {
                // It's a field with a value.
                checkFieldAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                // We already have a field descriptor.
                String descriptor = ClassUtil.internalType(type);

                // Read the constant.
                Number[] values = parseValues(type, descriptor);

                // Read the separator after the constant.
                readNextWord("separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");

                if (!ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord)) {
                    throw new RuntimeException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                            "' before " + reader.locationDescription());
                }

                // Add the field.
                classSpecification.addField(new MemberValueSpecification(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags,
                        annotationType,
                        name,
                        descriptor,
                        values
                ));
            } else if (ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD.equals(nextWord)) {
                // It's a method.
                checkMethodAccessFlags(
                        requiredSetMemberAccessFlags,
                        requiredUnsetMemberAccessFlags
                );

                // Parse the method arguments.
                String descriptor = ClassUtil.internalMethodDescriptor(
                        type,
                        parseCommaSeparatedList(
                                "argument", true, true, true, false
                        )
                );

                if (!ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD.equals(nextWord)) {
                    throw new RuntimeException("Expecting separating '" + ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD +
                                             "' or closing '" + ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD +
                                             "' before " + reader.locationDescription());
                }

                // Class initializers are not supposed to have any parameters.
                if (ClassConstants.METHOD_NAME_CLINIT.equals(name) &&
                    ClassUtil.internalMethodParameterCount(descriptor) > 0)
                {
                    throw new RuntimeException("Not expecting method parameters with initializer '"
                            + ClassConstants.METHOD_NAME_CLINIT
                            + "' before " + reader.locationDescription());
                }

                // Read the separator after the closing parenthesis.
                readNextWord("separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");

                if (ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord)) {
                    if (ConfigurationConstants.ANY_METHOD_KEYWORD.equals(name)) {
                        throw new RuntimeException("Not expecting method descriptor with wildcard '"
                                + ConfigurationConstants.ANY_METHOD_KEYWORD
                                + "before " + reader.locationDescription()
                                + " (use '" + ConfigurationConstants.ANY_CLASS_MEMBER_KEYWORD + "' instead)");
                    }

                    // Add the plain method.
                    classSpecification.addMethod(new MemberSpecification(
                            requiredSetMemberAccessFlags,
                            requiredUnsetMemberAccessFlags,
                            annotationType,
                            name,
                            descriptor
                    ));
                } else if (allowValues &&
                        (ConfigurationConstants.EQUAL_KEYWORD.equals(nextWord) ||
                        ConfigurationConstants.RETURN_KEYWORD.equals(nextWord))
                ) {
                    // It's a method with a value.
                    checkFieldAccessFlags(
                            requiredSetMemberAccessFlags,
                            requiredUnsetMemberAccessFlags
                    );

                    // Read the constant.
                    Number[] values = parseValues(type, ClassUtil.internalType(type));

                    // Read the separator after the constant.
                    readNextWord("separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");

                    if (!ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord)) {
                        throw new RuntimeException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                                "' before " + reader.locationDescription());
                    }

                    // Add the method.
                    classSpecification.addMethod(new MemberValueSpecification(
                            requiredSetMemberAccessFlags,
                            requiredUnsetMemberAccessFlags,
                            annotationType,
                            name,
                            descriptor,
                            values
                    ));
                } else {
                    throw new RuntimeException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                            "' before " + reader.locationDescription());

                }
            } else {
                // It doesn't look like a field or a method.
                throw new RuntimeException("Expecting opening '" + ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD +
                        "' or separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                        "' before " + reader.locationDescription());
            }
        }
    }


    /**
     * Reads a value or value range of the given primitive type.
     * For example, values "123" or "100..199" of type "int" ("I").
     */
    private Number[] parseValues(
            String externalType,
            String internalType
    ) throws RuntimeException, IOException {
        readNextWord(externalType + " constant");

        int rangeIndex = nextWord.indexOf(ConfigurationConstants.RANGE_KEYWORD);
        return rangeIndex >= 0 ? new Number[]{
                parseValue(externalType, internalType, nextWord.substring(
                        0,
                        rangeIndex
                )),
                parseValue(externalType, internalType, nextWord.substring(
                        rangeIndex + ConfigurationConstants.RANGE_KEYWORD.length()
                ))
        } : new Number[]{
                parseValue(externalType, internalType, nextWord)
        };
    }


    /**
     * Parses the given string as a value of the given primitive type.
     * For example, value "123" of type "int" ("I").
     * For example, value "true" of type "boolean" ("Z"), returned as 1.
     */
    private Number parseValue(
            String externalType,
            String internalType,
            String string
    ) throws RuntimeException {
        try {
            string = replaceSystemProperties(string);

            switch (internalType.charAt(0)) {
                case TypeConstants.BOOLEAN: {
                    return parseBoolean();
                }
                case TypeConstants.BYTE:
                case TypeConstants.CHAR:
                case TypeConstants.SHORT:
                case TypeConstants.INT: {
                    return Integer.decode(string);
                }
                default: {
                    throw new RuntimeException("Can't handle '" + externalType +
                            "' constant " + reader.locationDescription());
                }
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Can't parse " + externalType +
                    " constant " + reader.locationDescription());
        }
    }


    /**
     * Parses the given boolean string as an integer (0 or 1).
     */
    private Integer parseBoolean() throws RuntimeException {
        if (ConfigurationConstants.FALSE_KEYWORD.equals(nextWord)) {
            return 0;
        } else if (ConfigurationConstants.TRUE_KEYWORD.equals(nextWord)) {
            return 1;
        } else {
            throw new RuntimeException("Unknown boolean constant " + reader.locationDescription());
        }
    }

    /**
     * Reads a comma-separated list of java identifiers or of file names.
     * Examples of invocation arguments:
     *
     *   expected           read   allow  expect  is     check  allow   replace replace replace
     *   description        First  empty  Closing File   Java   Generic System  Extern  Extern
     *                      Word   List   Paren   Name   Id             Prop    Class   Types
     *   ----------------------------------------------------------------------------------
     *   ("directory name", true,  true,  false,  true,  false, true,   true,   false,  false, ...)
     *   ("optimization",   true,  false, false,  false, false, true,   false,  false,  false, ...)
     *   ("package name",   true,  true,  false,  false, true,  false,  false,  true,   false, ...)
     *   ("attribute name", true,  true,  false,  false, true,  false,  false,  false,  false, ...)
     *   ("class name",     true,  true,  false,  false, true,  false,  false,  true,   false, ...)
     *   ("filter",         true,  true,  true,   true,  false, true,   true,   false,  false, ...)
     *   ("annotation ",    false, false, false,  false, true,  false,  false,  false,  true,  ...)
     *   ("class name ",    true,  false, false,  false, true,  false,  false,  false,  false, ...)
     *   ("annotation ",    true,  false, false,  false, true,  false,  false,  false,  true,  ...)
     *   ("class name ",    false, false, false,  false, true,  false,  false,  false,  false, ...)
     *   ("annotation ",    true,  false, false,  false, true,  false,  false,  false,  true,  ...)
     *   ("argument",       true,  true,  true,   false, true,  false,  false,  false,  false, ...)
     */
    private List<String> parseCommaSeparatedList(
            String expectedDescription,
            boolean readFirstWord,
            boolean allowEmptyList,
            boolean expectClosingParenthesis,
            boolean replaceExternalTypes
    ) throws RuntimeException, IOException {
        List<String> list = new ArrayList<>();

        if (readFirstWord) {
            if (!allowEmptyList) {
                // Read the first list entry.
                readNextWord(expectedDescription, true, false);
            } else if (expectClosingParenthesis) {
                // Read the first list entry.
                readNextWord(expectedDescription, true, false);

                // Return if the entry is actually empty (an empty file name or
                // a closing parenthesis).
                if (nextWord.length() == 0) {
                    // Read the closing parenthesis
                    readNextWord("closing '" + ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD + "'");
                    return list;
                } else if (nextWord.equals(ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD)) {
                    return list;
                }
            } else {
                // Read the first list entry, if there is any.
                readNextWord(true);

                // Check if the list is empty.
                if (configurationEnd()) {
                    return list;
                }
            }
        }

        while (true) {
            checkNextWordIsJavaIdentifier("java type", false);

            if (replaceExternalTypes) {
                nextWord = ClassUtil.internalType(nextWord);
            }

            list.add(nextWord);

            if (expectClosingParenthesis) {
                // Read a comma (or a closing parenthesis, or a different word).
                readNextWord("separating '" + ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD +
                        "' or closing '" + ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD +
                        "'");
            } else {
                // Read a comma (or a different word).
                readNextWord();
            }

            if (!ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD.equals(nextWord)) {
                return list;
            }

            // Read the next list entry.
            readNextWord(expectedDescription, true, false);
        }
    }


    /**
     * Throws a RuntimeException for an unexpected keyword.
     */
    private int unknownAccessFlag() throws RuntimeException {
        throw new RuntimeException("Unexpected keyword " + reader.locationDescription());
    }

    /**
     * Replaces any properties in the given word by their values.
     * For instance, the substring "&lt;java.home&gt;" is replaced by its value.
     */
    private String replaceSystemProperties(String word) throws RuntimeException {
        int fromIndex = 0;
        while (true) {
            fromIndex = word.indexOf(ConfigurationConstants.OPEN_SYSTEM_PROPERTY, fromIndex);
            if (fromIndex < 0) {
                break;
            }

            int toIndex = word.indexOf(ConfigurationConstants.CLOSE_SYSTEM_PROPERTY, fromIndex + 1);
            if (toIndex < 0) {
                break;
            }

            String propertyName = word.substring(fromIndex + 1, toIndex);
            String propertyValue = properties.getProperty(propertyName);
            if (propertyValue == null) {
                try {
                    // Allow integer names, since they may be references
                    // to wildcards.
                    Integer.parseInt(propertyName);

                    fromIndex = toIndex + 1;
                    continue;
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Value of system property '" + propertyName +
                            "' is undefined in " + reader.locationDescription());
                }
            }

            word = word.substring(0, fromIndex) + propertyValue + word.substring(toIndex + 1);

            fromIndex += propertyValue.length();
        }

        return word;
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field,
     * throwing an exception if there is no next word.
     */
    private void readNextWord(
            String expectedDescription
    ) throws RuntimeException, IOException {
        readNextWord(expectedDescription, false, false);
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field,
     * throwing an exception if there is no next word.
     */
    private void readNextWord(
            String expectedDescription,
            boolean expectSingleFile,
            boolean expectingAtCharacter
    ) throws RuntimeException, IOException {
        readNextWord(expectSingleFile);
        if (configurationEnd(expectingAtCharacter)) {
            throw new RuntimeException("Expecting " + expectedDescription +
                    " before " + reader.locationDescription());
        }
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field.
     */
    private void readNextWord() throws IOException {
        readNextWord(false);
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field.
     */
    private void readNextWord(boolean expectSingleFile) throws IOException {
        nextWord = reader.nextWord(false, expectSingleFile);
    }


    /**
     * Returns whether the end of the configuration has been reached.
     */
    private boolean configurationEnd() {
        return configurationEnd(false);
    }


    /**
     * Returns whether the end of the configuration has been reached.
     */
    private boolean configurationEnd(boolean expectingAtCharacter) {
        boolean currentEnds  = (nextWord == null || nextWord.startsWith(ConfigurationConstants.OPTION_PREFIX));
        boolean atDirectives = (!expectingAtCharacter && ConfigurationConstants.AT_DIRECTIVE.equals(nextWord));
        return  currentEnds || atDirectives;
    }


    /**
     * Checks whether the given word is a valid Java identifier and throws
     * a RuntimeException if it isn't. Wildcard characters are accepted.
     */
    private void checkNextWordIsJavaIdentifier(
            String expectedDescription
    ) throws RuntimeException {
        checkNextWordIsJavaIdentifier(expectedDescription, true);
    }

    private void checkNextWordIsJavaIdentifier(
            String expectedDescription,
            boolean allowGenerics
    ) throws RuntimeException {
        checkJavaIdentifier(expectedDescription, nextWord, allowGenerics);
    }

    /**
     * Checks whether the given word is a valid Java identifier and throws
     * a RuntimeException if it isn't. Wildcard characters are accepted.
     */
    private void checkJavaIdentifier(
            String expectedDescription,
            String identifier,
            boolean allowGenerics
    ) throws RuntimeException {
        if (!isValidIdentifier(identifier)) {
            throw new RuntimeException("Expecting " + expectedDescription +
                    " before " + reader.locationDescription());
        }

        if (!allowGenerics && containsGenerics(identifier)) {
            throw new RuntimeException("Generics are not allowed (erased) in " + expectedDescription +
                    " " + reader.locationDescription());
        }
    }

    private boolean isValidIdentifier(String word) {
        return useDalvikVerification ? isDexIdentifier(word) : isJavaIdentifier(word);
    }

    /**
     * Returns whether the given word is a valid Java identifier.
     * Wildcard characters are accepted.
     */
    private boolean isJavaIdentifier(String word) {
        if (word.length() == 0) {
            return false;
        }

        for (int index = 0; index < word.length(); index++) {
            char c = word.charAt(index);
            if (!(Character.isJavaIdentifierPart(c) ||
                    c == '.' ||
                    c == '[' ||
                    c == ']' ||
                    c == '<' ||
                    c == '>' ||
                    c == '-' ||
                    c == '!' ||
                    c == '*' ||
                    c == '?' ||
                    c == '%')) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether the given word is a valid DEX identifier. Special wildcard characters for
     * ProGuard class specifiction syntaxs are accepted. The list of valid identifier can be
     * found at <a href="https://source.android.com/docs/core/runtime/dex-format#simplename">...</a>
     */
    private boolean isDexIdentifier(String word) {
        if (word.isEmpty()) {
            return false;
        }

        int[] codePoints = word.codePoints().toArray();

        for (int index = 0; index < codePoints.length; index++) {
            int c = codePoints[index];

            boolean isLetterOrNumber = Character.isLetterOrDigit(c);
            boolean isValidSymbol = c == '$' || c == '-' || c == '_';
            boolean isWithinSupportedUnicodeRanges =
                    (c >= 0x00a1 && c <= 0x1fff)
                            || (c >= 0x2010 && c <= 0x2027)
                            || (c >= 0x2030 && c <= 0xd7ff)
                            || (c >= 0xe000 && c <= 0xffef)
                            || (c >= 0x10000 && c <= 0x10ffff);
            boolean isProGuardSymbols =
                    c == '.' || c == '[' || c == ']' || c == '<' || c == '>' || c == '-' || c == '!'
                            || c == '*' || c == '?' || c == '%';

            if (!(isLetterOrNumber
                    || isValidSymbol
                    || isWithinSupportedUnicodeRanges
                    || isProGuardSymbols)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether the given word contains angle brackets around
     * a non-digit string.
     */
    private boolean containsGenerics(String word) {
        int index = 0;

        while (true) {
            // Can we find an opening angular bracket?
            int openIndex = word.indexOf(TypeConstants.GENERIC_START, index);
            if (openIndex < 0) {
                return false;
            }

            // Can we find a corresponding closing angular bracket?
            int closeIndex = word.indexOf(TypeConstants.GENERIC_END, openIndex + 1);
            if (closeIndex < 0) {
                return false;
            }

            try {
                // Is it just a reference to a wildcard?
                Integer.parseInt(word.substring(openIndex + 1, closeIndex));
            } catch (NumberFormatException e) {
                // It's not; it's really a generic type.
                return true;
            }

            index = closeIndex + 1;
        }
    }


    /**
     * Checks whether the given access flags are valid field access flags,
     * throwing a RuntimeException if they aren't.
     */
    private void checkFieldAccessFlags(
            int requiredSetMemberAccessFlags,
            int requiredUnsetMemberAccessFlags
    ) throws RuntimeException {
        int access = (requiredSetMemberAccessFlags | requiredUnsetMemberAccessFlags);
        if ((access & ~AccessConstants.VALID_FLAGS_FIELD) != 0) {
            throw new RuntimeException("Invalid method access modifier for field before " +
                    reader.locationDescription());
        }
    }


    /**
     * Checks whether the given access flags are valid method access flags,
     * throwing a RuntimeException if they aren't.
     */
    private void checkMethodAccessFlags(
            int requiredSetMemberAccessFlags,
            int requiredUnsetMemberAccessFlags
    ) throws RuntimeException {
        int access = (requiredSetMemberAccessFlags | requiredUnsetMemberAccessFlags);
        if ((access & ~AccessConstants.VALID_FLAGS_METHOD) != 0) {
            throw new RuntimeException("Invalid field access modifier for method before " +
                    reader.locationDescription());
        }
    }

}
