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
 * This class consists of {@code static} utility methods for checking
 * if any of their arguments is {@code null}.
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
     * public Foo(Obj obj) {
     *     Checks.nullCheck(obj);
     *     this.obj = obj;
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

    /**
     * Checks that the specified object references are not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Obj1 obj1, Obj2 obj2) {
     *     Checks.nullCheck(obj1, obj2);
     *     this.obj1 = obj1;
     *     this.obj2 = obj2;
     * }
     * </pre></blockquote>
     *
     * @param obj1 an object reference to check for nullity
     * @param obj2 an object reference to check for nullity
     * @throws NullPointerException if any of {@code obj1} or {@code obj2} is {@code null}
     */
    @ForceInline
    public static void nullCheck(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null)
            throw new NullPointerException();
    }

    /**
     * Checks that the specified object references are not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Obj1 obj1, Obj2 obj2, Obj3 obj3) {
     *     Checks.nullCheck(obj1, obj2, obj3);
     *     this.obj1 = obj1;
     *     this.obj2 = obj2;
     *     this.obj3 = obj3;
     * }
     * </pre></blockquote>
     *
     * @param obj1 an object reference to check for nullity
     * @param obj2 an object reference to check for nullity
     * @param obj3 an object reference to check for nullity
     * @throws NullPointerException if any of {@code obj1}, {@code obj2} or
     * {@code obj3} is {@code null}
     */
    @ForceInline
    public static void nullCheck(Object obj1, Object obj2, Object obj3) {
        if (obj1 == null || obj2 == null || obj3 == null)
            throw new NullPointerException();
    }

    /**
     * Checks that the specified object references are not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Obj1 obj1, Obj2 obj2, Obj3 obj3, Obj4 obj4) {
     *     Checks.nullCheck(obj1, obj2, obj3, obj4);
     *     this.obj1 = obj1;
     *     this.obj2 = obj2;
     *     this.obj3 = obj3;
     *     this.obj4 = obj4;
     * }
     * </pre></blockquote>
     *
     * @param obj1 an object reference to check for nullity
     * @param obj2 an object reference to check for nullity
     * @param obj3 an object reference to check for nullity
     * @param obj4 an object reference to check for nullity
     * @throws NullPointerException if any of {@code obj1}, {@code obj2},
     * {@code obj3} or {@code obj4} is {@code null}
     */
    @ForceInline
    public static void nullCheck(Object obj1, Object obj2, Object obj3, Object obj4) {
        if (obj1 == null || obj2 == null || obj3 == null || obj4 == null)
            throw new NullPointerException();
    }

    /**
     * Checks that the specified object references are not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Obj1 obj1, Obj2 obj2, Obj3 obj3, Obj4 obj4, Obj5 obj5) {
     *     Checks.nullCheck(obj1, obj2, obj3, obj4, obj5);
     *     this.obj1 = obj1;
     *     this.obj2 = obj2;
     *     this.obj3 = obj3;
     *     this.obj4 = obj4;
     *     this.obj5 = obj5;
     * }
     * </pre></blockquote>
     *
     * @param obj1 an object reference to check for nullity
     * @param obj2 an object reference to check for nullity
     * @param obj3 an object reference to check for nullity
     * @param obj4 an object reference to check for nullity
     * @param obj5 an object reference to check for nullity
     * @throws NullPointerException if any of {@code obj1}, {@code obj2},
     * {@code obj3}, {@code obj4} or {@code obj5} is {@code null}
     */
    @ForceInline
    public static void nullCheck(Object obj1, Object obj2, Object obj3, Object obj4, Object obj5) {
        if (obj1 == null || obj2 == null || obj3 == null || obj4 == null || obj5 == null)
            throw new NullPointerException();
    }
}
