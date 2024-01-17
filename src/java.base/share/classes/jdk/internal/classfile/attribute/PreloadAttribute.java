/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public sealed interface PreloadAttribute
        extends Attribute<PreloadAttribute>, ClassElement
        permits BoundAttribute.BoundPreloadAttribute, UnboundAttribute.UnboundPreloadAttribute {

    /**
     * {@return the list of preload classes}
     */
    List<ClassEntry> preloads();

    /**
     * {@return a {@code Preload} attribute}
     * @param preloads the preload classes
     */
    static PreloadAttribute of(List<ClassEntry> preloads) {
        return new UnboundAttribute.UnboundPreloadAttribute(preloads);
    }

    /**
     * {@return a {@code Preload} attribute}
     * @param preloads the preload classes
     */
    static PreloadAttribute of(ClassEntry... preloads) {
        return of(List.of(preloads));
    }

    /**
     * {@return a {@code Preload} attribute}
     * @param preloads the preload classes
     */
    static PreloadAttribute ofSymbols(List<ClassDesc> preloads) {
        return of(Util.entryList(preloads));
    }

    /**
     * {@return a {@code Preload} attribute}
     * @param preloads the preload classes
     */
    static PreloadAttribute ofSymbols(ClassDesc... preloads) {
        return ofSymbols(Arrays.asList(preloads));
    }
}
