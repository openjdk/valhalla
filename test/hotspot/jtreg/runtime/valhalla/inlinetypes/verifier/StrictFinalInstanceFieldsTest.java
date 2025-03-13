/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @enablePreview
 * @compile --add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED -XDgenerateAssertUnsetFieldsFrame StrictFinalInstanceFieldsTest.java
 * @run main/othervm -Xlog:verification StrictFinalInstanceFieldsTest
 */

import jdk.internal.vm.annotation.Strict;

public class StrictFinalInstanceFieldsTest {
    public static void main(String[] args) {
        // Base case
        Child c = new Child();
        System.out.println(c);

        // Field not initialized before super call
        /*
        // javac is flagging the error at compile time
        try {
            BadChild0 bc0 = new BadChild0();
            System.out.println(bc0);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("All strict final fields must be initialized before super()")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Field not initialized before super call
        try {
            BadChild1 bc1 = new BadChild1();
            System.out.println(bc1);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("All strict final fields must be initialized before super()")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }
        */

        // Test constructor with control flow. Should pass
        Child1 c1 = new Child1(true, false);
        System.out.println(c1);

        // Test constructor with control flow and nested constructor calls. Should pass
        Child1 c1_2 = new Child1();
        System.out.println(c1_2);

        // Test assignment in conditional. Should pass
        Child2 c2 = new Child2();
        System.out.println(c2);

        // Test constructor with control flow in switch case. Should pass
        Child3 c3 = new Child3(2);
        System.out.println(c3);

        System.out.println("Passed");
    }
}

class Parent {
    int z;

    Parent() {
        z = 0;
    }

    int get_z() { return z; }

    @Override
    public String toString() {
        return "z: " + get_z();
    }
}

class Child extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    Child() {
        x = y = 1;
        super();
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}

class BadChild0 extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    // Should fail with "All strict final fields must be initialized before super()"
    BadChild0() {
        x = 1;
        y = 1;
        super();
        // was y = 1;
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}

class BadChild1 extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    // Should fail with "All strict final fields must be initialized before super()"
    BadChild1() {
        y = 1;
        x = 1;
        super();
        // was x = 1;
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}

class Child1 extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    Child1(boolean a, boolean b) {
        if (a) {
            x = 1;
            if (b) {
                y = 1;
            } else {
                y = 2;
            }
        } else {
            x = y = 3;
        }
        super();
    }

    Child1() {
        this(true, true);
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}

class Child2 extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    Child2() {
        if ((x=1) == 1) {
            y = 1;
        } else {
            y = 2;
        }
        super();
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}

class Child3 extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    Child3(int n) {
        switch(n) {
            case 0:
                x = y = 0;
                break;
            case 1:
                x = y = 1;
                break;
            case 2:
                x = y = 2;
                break;
            default:
                x = y = 100;
                break;
        }
        super();
    }

    int get_x() { return x; }
    int get_y() { return y; }

    @Override
    public String toString() {
        return "x: " + get_x() + "\n" + "y: " + get_y() + "\n" + super.toString();
    }
}
