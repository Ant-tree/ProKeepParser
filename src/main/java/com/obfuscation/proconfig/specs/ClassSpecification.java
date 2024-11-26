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
package com.obfuscation.proconfig.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores a specification of classes and possibly class members.
 * The specification is template-based: the class names and class member names
 * and descriptors can contain wildcards. Classes can be specified explicitly,
 * or as extensions or implementations in the class hierarchy.
 *
 * @author Eric Lafortune
 */
public class ClassSpecification implements Serializable {
    public final String comments;
    public String memberComments;
    public int requiredSetAccessFlags;
    public int requiredUnsetAccessFlags;
    public final String annotationType;
    public String className;
    public final String extendsAnnotationType;
    public final String extendsClassName;

    public List<MemberSpecification> fieldSpecifications;
    public List<MemberSpecification> methodSpecifications;

    /**
     * Creates a new ClassSpecification that is a copy of the given specification.
     */
    public ClassSpecification(ClassSpecification classSpecification) {
        this(classSpecification.comments,
                classSpecification.requiredSetAccessFlags,
                classSpecification.requiredUnsetAccessFlags,
                classSpecification.annotationType,
                classSpecification.className,
                classSpecification.extendsAnnotationType,
                classSpecification.extendsClassName,
                classSpecification.fieldSpecifications,
                classSpecification.methodSpecifications);
    }


    /**
     * Creates a new ClassSpecification for the specified class(es), without
     * class members.
     *
     * @param comments                 provides optional comments on this
     *                                 specification.
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be
     *                                 unset in order for the class to apply.
     * @param annotationType           the name of the class that must be an
     *                                 annotation of the class in order for it
     *                                 to apply. The name may be null to
     *                                 specify that no annotation is required.
     * @param className                the class name. The name may be null to
     *                                 specify any class, or it may contain
     *                                 "**", "*", or "?" wildcards.
     * @param extendsAnnotationType    the name of the class of that must be
     *                                 an annotation of the class that the
     *                                 class must extend or implement in order
     *                                 to apply. The name may be null to
     *                                 specify that no annotation is required.
     * @param extendsClassName         the name of the class that the class
     *                                 must extend or implement in order to
     *                                 apply. The name may be null to specify
     *                                 any class.
     */
    public ClassSpecification(
            String comments,
            int requiredSetAccessFlags,
            int requiredUnsetAccessFlags,
            String annotationType,
            String className,
            String extendsAnnotationType,
            String extendsClassName
    ) {
        this(comments,
                requiredSetAccessFlags,
                requiredUnsetAccessFlags,
                annotationType,
                className,
                extendsAnnotationType,
                extendsClassName,
                null,
                null);
    }


    /**
     * Creates a new ClassSpecification for the specified classes and class
     * members.
     *
     * @param comments                 provides optional comments on this
     *                                 specification.
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be
     *                                 unset in order for the class to apply.
     * @param annotationType           the name of the class that must be an
     *                                 annotation of the class in order for it
     *                                 to apply. The name may be null to
     *                                 specify that no annotation is required.
     * @param className                the class name. The name may be null to
     *                                 specify any class, or it may contain
     *                                 "**", "*", or "?" wildcards.
     * @param extendsAnnotationType    the name of the class of that must be
     *                                 an annotation of the class that the
     *                                 class must extend or implement in order
     *                                 to apply. The name may be null to
     *                                 specify that no annotation is required.
     * @param extendsClassName         the name of the class that the class
     *                                 must extend or implement in order to
     *                                 apply. The name may be null to specify
     *                                 any class.
     * @param fieldSpecifications      the field specifications.
     * @param methodSpecifications     the method specifications.
     */
    public ClassSpecification(
            String comments,
            int requiredSetAccessFlags,
            int requiredUnsetAccessFlags,
            String annotationType,
            String className,
            String extendsAnnotationType,
            String extendsClassName,
            List<MemberSpecification> fieldSpecifications,
            List<MemberSpecification> methodSpecifications
    ) {
        this.comments = comments;
        this.requiredSetAccessFlags = requiredSetAccessFlags;
        this.requiredUnsetAccessFlags = requiredUnsetAccessFlags;
        this.annotationType = annotationType;
        this.className = className;
        this.extendsAnnotationType = extendsAnnotationType;
        this.extendsClassName = extendsClassName;
        this.fieldSpecifications = fieldSpecifications;
        this.methodSpecifications = methodSpecifications;
    }


    /**
     * Specifies to keep the specified field(s) of this option's class(es).
     *
     * @param fieldSpecification the field specification.
     */
    public void addField(MemberSpecification fieldSpecification) {
        if (fieldSpecifications == null) {
            fieldSpecifications = new ArrayList<>();
        }

        fieldSpecifications.add(fieldSpecification);
    }


    /**
     * Specifies to keep the specified method(s) of this option's class(es).
     *
     * @param methodSpecification the method specification.
     */
    public void addMethod(MemberSpecification methodSpecification) {
        if (methodSpecifications == null) {
            methodSpecifications = new ArrayList<>();
        }

        methodSpecifications.add(methodSpecification);
    }
}
