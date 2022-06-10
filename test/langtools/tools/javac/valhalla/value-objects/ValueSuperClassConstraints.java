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

/*
 * @test
 * @bug 8287136
 * @summary [lw4] Javac tolerates abstract value classes that violate constraints for qualifying to be value super classes
 * @compile/fail/ref=ValueSuperClassConstraints.out -XDrawDiagnostics ValueSuperClassConstraints.java
 */

public class ValueSuperClassConstraints {

    static abstract class I1 { // has identity since it declares an instance field.
        int f;
    }

    static value class V1 extends I1 {} // Error.

    abstract class I2 { // has identity since is an inner class
    }

    static value class V2 extends I2 {} // Error.

    static abstract class I3 { // has identity since it declared a synchronized instance method.
        synchronized void foo() {
        }
    }

    static value class V3 extends I3 {} // Error.

    static abstract class I4 { // has identity since it declares an instance initializer
        { int f = 42; }
    }

    static value class V4 extends I4 {} // Error.

    static abstract class I5 { // has identity since it declares a non-trivial constructor
        I5(int x) {}
    }

    static value class V5 extends I5 {} // Error.
}
