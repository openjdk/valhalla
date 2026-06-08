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
package jdk.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Indicates the anotated class or record is a value class whenever preview
 * features are enabled.
 *
 * The annotation should be used like the {@code value} modifier and placed
 * immediately before the {@code class} or {@code record} keyword in the
 * declaration.
 *
 * At JDK build time: an alternative source file is generated, adding the
 * {@code value} modifier to the class or record. For this to work properly,
 * the source filename must be listed at
 * {@code make/modules/java.base/gensrc/GensrcValueClasses.gmk},
 * and the annotation must be spelled {@code @jdk.internal.PreviewValue} and
 * must be immediately followed on the same line by {@code class} or
 * {@code record}.
 *
 * At compile time: javac recognizes annotated JDK classes as value classes
 * whenever preview features are enabled. To work properly with
 * {@code --release older-release}, the annotation requires special handling in
 * {@code make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java} and
 * {@code src/jdk.compiler/share/classes/com/sun/tools/javac/jvm/ClassReader.java}.
 *
 * At run time: other non-preview JDK classes that references preview value
 * classes cannot have {@code LoadableDescriptors} attributes. To enable
 * optimizations in these referencing non-preview JDK class files, the preview
 * value classes should be manually listed for special-case treatment in
 * {@code src/hotspot/share/classfile/vmSymbols.hpp},
 * {@code src/hotspot/share/classfile/vmSymbols.cpp}, and
 * {@code src/hotspot/share/classfile/vmClassMacros.hpp}.
 *
 * @since Valhalla
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={TYPE})
public @interface PreviewValue {
}
