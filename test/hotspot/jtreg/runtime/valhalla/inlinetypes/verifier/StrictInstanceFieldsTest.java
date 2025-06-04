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
 * @compile BadChild.jasm
 *          BadChild1.jasm
 *          ControlFlowChildBad.jasm
 *          TryCatchChildBad.jasm
 *          NestedEarlyLarval.jcod
 *          EndsInEarlyLarval.jcod
 *          StrictFieldsNotSubset.jcod
 * @compile --add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED -XDgenerateEarlyLarvalFrame -XDnoLocalProxyVars StrictInstanceFieldsTest.java
 * @run main/othervm -Xlog:verification StrictInstanceFieldsTest
 */

import java.lang.reflect.Field;
import jdk.internal.vm.annotation.Strict;

public class StrictInstanceFieldsTest {
    public static void main(String[] args) {

        // --------------
        // POSITIVE TESTS
        // --------------

        // Base case
        Child c = new Child();
        System.out.println(c);

        // Constructor with control flow
        ControlFlowChild c1 = new ControlFlowChild(true, true);
        System.out.println(c1);

        // Constructor with try-catch-finally
        TryCatchChild c2 = new TryCatchChild();
        System.out.println(c2);

        // Constructor with switch case
        SwitchCaseChild c3 = new SwitchCaseChild(2);
        System.out.println(c3);

        // Constructor with strict field assignment in conditional
        AssignedInConditionalChild c4 = new AssignedInConditionalChild();
        System.out.println(c4);

        // Constructor with nested constructor calls
        NestedConstructorChild c5 = new NestedConstructorChild();
        System.out.println(c5);

        // Final stirct fields defined in constructor
        FinalChild fc = new FinalChild();
        System.out.println(fc);

        // --------------
        // NEGATIVE TESTS
        // --------------

        // Field not initialized before super call
        try {
            BadChild child = new BadChild();
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("All strict final fields must be initialized before super()")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Field not initialized before super call
        try {
            BadChild1 child = new BadChild1();
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("All strict final fields must be initialized before super()")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Constructor with control flow but field is not initialized
        try {
            ControlFlowChildBad child = new ControlFlowChildBad(true, false);
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Inconsistent stackmap frames at branch target")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Constructor with try-catch but field is not initialized
        try {
            TryCatchChildBad child = new TryCatchChildBad();
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Inconsistent stackmap frames at branch target")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Early_Larval frame contains another early_larval instead of a base frame
        try {
            NestedEarlyLarval child = new NestedEarlyLarval(true, false);
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Early larval frame must be followed by a base frame")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Stack map table ends in early_larval frame without base frame
        try {
            EndsInEarlyLarval child = new EndsInEarlyLarval(true, false);
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Early larval frame must be followed by a base frame")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        // Early_larval frame includes a strict field not preset in the original set of unset fields
        try {
            StrictFieldsNotSubset child = new StrictFieldsNotSubset(true, false);
            System.out.println(child);
            throw new RuntimeException("Should fail verification");
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Strict fields not a subset of initial strict instance fields")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        System.out.println("Passed");
    }
}

class Parent {
    int z;

    Parent() {
        z = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field f : fields) {
            try {
                sb.append(f.getName() + ": " + f.get(this) + "\n");
           } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
           }
        }
        return sb.toString();
    }
}

class Child extends Parent {

    @Strict
    int x;
    @Strict
    int y;

    Child() {
        x = y = 1;
        super();
    }
}

class ControlFlowChild extends Parent {

    @Strict
    int x;
    @Strict
    int y;

    ControlFlowChild(boolean a, boolean b) {
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
}

class TryCatchChild extends Parent {

    @Strict
    int x;
    @Strict
    int y;

    TryCatchChild() {
        try {
            x = 0;
            int[] a = new int[1];
            System.out.println(a[2]);
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            y = 0;
        } finally {
            x = y = 1;
        }
        super();
    }
}

class AssignedInConditionalChild extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    AssignedInConditionalChild() {
        if ((x=1) == 1) {
            y = 1;
        } else {
            y = 2;
        }
        super();
    }
}

class SwitchCaseChild extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    SwitchCaseChild(int n) {
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
}

class NestedConstructorChild extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    NestedConstructorChild(boolean a, boolean b) {
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

    NestedConstructorChild() {
        this(true, true);
    }
}

class FinalChild extends Parent {

    @Strict
    final int x;
    @Strict
    final int y;

    FinalChild() {
        x = y = 1;
        super();
    }
}
