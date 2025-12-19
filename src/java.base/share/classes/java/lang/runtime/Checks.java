/*
 * Copyright (c) 2009, 2025, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import jdk.internal.vm.annotation.ForceInline;

/**
 * This class consists of {@code static} utility methods for operating
 * on objects, or checking certain conditions before operation.  These utilities
 * include {@code null}-safe or {@code null}-tolerant methods for computing the
 * hash code of an object, returning a string for an object, comparing two
 * objects, and checking if indexes or sub-range values are out of bounds.
 *
 * @since valhalla
 */
public final class Checks {
    private Checks() {
        throw new AssertionError("can't instantiate java.lang.runtime.Checks");
    }

    /**
     * Checks that the specified object reference is not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar) {
     *     Checks.nullCheck(bar);
     *     this.bar = bar;
     * }
     * </pre></blockquote>
     *
     * @param obj the object reference to check for nullity
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @ForceInline
    public static void nullCheck(Object obj) {
        if (obj == null)
            throw new NullPointerException();
    }

    /*public static void nullCheck(Object o1, Object o2) {}
    public static void nullCheck(Object o1, Object o2, Object o3) {}
    public static void nullCheck(Object o1, Object o2, Object o3, Object o4) {}
    public static void nullCheck(Object o1, Object o2, Object o3, Object o4, Object o5) {}*/
}
