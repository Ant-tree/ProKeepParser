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

/**
 * This class represents a keep option with class specification.
 *
 * @author Eric Lafortune
 */
public class KeepClassSpecification extends ClassSpecification {
    public final boolean markClasses;
    public final boolean markClassMembers;
    public final boolean markConditionally;
    public final boolean markDescriptorClasses;
    public final boolean markCodeAttributes;
    public final boolean allowObfuscation;
    public final ClassSpecification condition;


    /**
     * Creates a new KeepClassSpecification.
     *
     * @param markClasses           specifies whether to mark the classes.
     * @param markClassMembers      specifies whether to mark the class
     *                              members.
     * @param markConditionally     specifies whether to mark the classes and
     *                              class members conditionally. If true,
     *                              classes and class members are marked, on
     *                              the condition that all specified class
     *                              members are present.
     * @param markDescriptorClasses specifies whether to mark the classes in
     *                              the descriptors of the marked class members.
     * @param markCodeAttributes    specified whether to mark the code attributes
     *                              of the marked class methods.
     * @param allowObfuscation      specifies whether obfuscation is allowed.
     * @param condition             an optional extra condition.
     * @param classSpecification    the specification of classes and class
     *                              members.
     */
    public KeepClassSpecification(
            boolean markClasses,
            boolean markClassMembers,
            boolean markConditionally,
            boolean markDescriptorClasses,
            boolean markCodeAttributes,
            boolean allowObfuscation,
            ClassSpecification condition,
            ClassSpecification classSpecification
    ) {
        super(classSpecification);

        this.markClasses = markClasses;
        this.markClassMembers = markClassMembers;
        this.markConditionally = markConditionally;
        this.markDescriptorClasses = markDescriptorClasses;
        this.markCodeAttributes = markCodeAttributes;
        this.allowObfuscation = allowObfuscation;
        this.condition = condition;
    }
}
