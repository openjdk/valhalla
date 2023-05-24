/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.internal.classfile.attribute;

import java.lang.constant.ClassDesc;
import java.util.Arrays;
import java.util.List;

import jdk.internal.classfile.Attribute;
import jdk.internal.classfile.ClassElement;
import jdk.internal.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.BoundAttribute;
import jdk.internal.classfile.impl.UnboundAttribute;
import jdk.internal.classfile.impl.Util;

/**
 * Models the {@code NestMembers} attribute {@jvms 4.7.29}, which can
 * appear on classes to indicate that this class is the host of a nest.
 * Delivered as a {@link jdk.internal.classfile.ClassElement} when
 * traversing the elements of a {@link jdk.internal.classfile.ClassModel}.
 */
public sealed interface NestMembersAttribute extends Attribute<NestMembersAttribute>, ClassElement
        permits BoundAttribute.BoundNestMembersAttribute, UnboundAttribute.UnboundNestMembersAttribute {

    /**
     * {@return the classes belonging to the nest hosted by this class}
     */
    List<ClassEntry> nestMembers();

    /**
     * {@return a {@code NestMembers} attribute}
     * @param nestMembers the member classes of the nest
     */
    static NestMembersAttribute of(List<ClassEntry> nestMembers) {
        return new UnboundAttribute.UnboundNestMembersAttribute(nestMembers);
    }

    /**
     * {@return a {@code NestMembers} attribute}
     * @param nestMembers the member classes of the nest
     */
    static NestMembersAttribute of(ClassEntry... nestMembers) {
        return of(List.of(nestMembers));
    }

    /**
     * {@return a {@code NestMembers} attribute}
     * @param nestMembers the member classes of the nest
     */
    static NestMembersAttribute ofSymbols(List<ClassDesc> nestMembers) {
        return of(Util.entryList(nestMembers));
    }

    /**
     * {@return a {@code NestMembers} attribute}
     * @param nestMembers the member classes of the nest
     */
    static NestMembersAttribute ofSymbols(ClassDesc... nestMembers) {
        // List view, since ref to nestMembers is temporary
        return ofSymbols(Arrays.asList(nestMembers));
    }
}
