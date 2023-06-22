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

import java.util.List;

import jdk.internal.classfile.Attribute;
import jdk.internal.classfile.BootstrapMethodEntry;
import jdk.internal.classfile.constantpool.ConstantPool;
import jdk.internal.classfile.impl.BoundAttribute;
import jdk.internal.classfile.impl.UnboundAttribute;

/**
 * Models the {@code BootstrapMethods} attribute {@jvms 4.7.23}, which serves as
 * an extension to the constant pool of a classfile.  Elements of the bootstrap
 * method table are accessed through {@link ConstantPool}.
 */
public sealed interface BootstrapMethodsAttribute
        extends Attribute<BootstrapMethodsAttribute>
        permits BoundAttribute.BoundBootstrapMethodsAttribute,
                UnboundAttribute.EmptyBootstrapAttribute {

    /**
     * {@return the elements of the bootstrap method table}
     */
    List<BootstrapMethodEntry> bootstrapMethods();

    /**
     * {@return the size of the bootstrap methods table}.  Calling this method
     * does not necessarily inflate the entire table.
     */
    int bootstrapMethodsSize();

    // No factories; BMA is generated as part of constant pool
}
