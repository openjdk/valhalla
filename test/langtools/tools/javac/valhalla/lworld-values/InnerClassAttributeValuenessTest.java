/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @bug 8221330
 * @summary Javac adds InnerClass attribute missing value flag
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main/othervm InnerClassAttributeValuenessTest
 */

import com.sun.tools.classfile.AccessFlags;

public class InnerClassAttributeValuenessTest {

    static inline class Inner {
        int f;
        private Inner() { f=0; }
        private Inner(int v) { f=v; }

        public static Inner create(int v) {
            return new Inner(v);
        }

        // Uncomment the next line, and Inner ceases to be a value type
        public static final Inner? ZERO = Inner.create(0);
        public static final Inner? ZERO2 = Inner.create(0);
    }

    public static void main(String[] args) {
        if ((Inner.class.getModifiers() & AccessFlags.ACC_VALUE) == 0)
            throw new AssertionError("Value flag missing");
    }
}
