/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244712
 * @summary Javac should switch to reference projection before walking type hierarchy.
 * @compile AsSuperTests.java
 */

/* The following test "covers"/verifies that the asSuper calls in
   com.sun.tools.javac.comp.Resolve#resolveSelf &&
   com.sun.tools.javac.comp.Lower#visitSelect
   com.sun.tools.javac.comp.Resolve#mostSpecific
   com.sun.tools.javac.comp.Attr#visitSelect
   com.sun.tools.javac.comp.Resolve.UnboundMethodReferenceLookupHelper#UnboundMethodReferenceLookupHelper
   work correctly with primitive types.
*/

interface I {
    default void foo() {
        System.out.println("I.foo");
    }
}

abstract class Base<T> {
    static void goo() {}
    void zoo() {}
    interface SAM {
       String m(Foo f);
    }

    static void test() {
        SAM s = Base::getX;
    }

    String getX() { return null; }

    static primitive class Foo<X> extends Base {}
}

primitive class X extends Base implements I {

    static void goo() {}

    public void foo() {
        I.super.foo();
        X.this.goo(); // covers the asSuper call in com.sun.tools.javac.comp.Resolve#mostSpecific
        super.zoo();  // covers the asSuper call in com.sun.tools.javac.comp.Attr#visitSelect
    }
}
