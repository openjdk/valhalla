/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222634
 * @summary Check various code snippets that were incorrectly failing to compile.
 * @compile AssortedTests.java
 */

inline class MyValue1 {
    final int x = 0;
}

class X {
    static final MyValue1 vField = new MyValue1();

    inline class MyValue2 {
        final MyValue1? vBoxField;

        public MyValue2() {
            vBoxField = new MyValue1();
        }
    }

    public static void main(String[] args) { }
}

inline class MyValue3 {
    final int x = 0;
    public int hash() { return 0; }
}

class Y {

    inline class MyValue4 {
        final MyValue3? vBoxField = null;

        public int test() {
            return vBoxField.hash();
        }
    }

    public static void main(String[] args) { }
}

interface MyInterface {
    public void test(MyValue5? vt);
}

inline class MyValue5 implements MyInterface {
    final int x = 0;

    @Override
    public void test(MyValue5? vt) { }
}
