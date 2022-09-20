/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

public class SuperclassCollections {

    public static class BadSuper {}

    public interface GoodSuperInterface {}
    public static abstract class GoodSuper extends Object {}
    public static abstract class Integer extends Number {
        public double doubleValue() { return 0; }
        public float floatValue() { return 0; }
        public long longValue() { return 0; }
        public int intValue() { return 0; }
    }
    public static abstract class SuperWithInstanceField {
        int x;
    }
    public static abstract class SuperWithInstanceField_01 extends SuperWithInstanceField {}

    public static abstract class SuperWithStaticField {
        static int x;
    }
    public static abstract class SuperWithEmptyNoArgCtor {
        public SuperWithEmptyNoArgCtor() {
            // Programmer supplied ctor but injected super call
        }
    }
    public static abstract class SuperWithEmptyNoArgCtor_01 extends SuperWithEmptyNoArgCtor {
        public SuperWithEmptyNoArgCtor_01() {
            super();  // programmer coded chaining no-arg constructor
        }
    }
    public static abstract class SuperWithEmptyNoArgCtor_02 extends SuperWithEmptyNoArgCtor_01 {
        // Synthesized chaining no-arg constructor
    }

    public static abstract class SuperWithNonEmptyNoArgCtor {
        public SuperWithNonEmptyNoArgCtor() {
            System.out.println("Non-Empty");
        }
    }
    public static abstract class SuperWithNonEmptyNoArgCtor_01 extends SuperWithNonEmptyNoArgCtor {}

    public static abstract class SuperWithArgedCtor {
        public SuperWithArgedCtor() {}
        public SuperWithArgedCtor(String s) {
        }
    }
    public static abstract class SuperWithArgedCtor_01 extends SuperWithArgedCtor {}

    public static abstract class SuperWithInstanceInit {
        {
            System.out.println("Disqualified from being super");
        }
    }
    public static abstract class SuperWithInstanceInit_01 extends SuperWithInstanceInit {
        {
            // Not disqualified since it is a meaningless empty block.
        }
    }

    public static abstract class SuperWithSynchronizedMethod {
        synchronized void foo() {}
    }
    public static abstract class SuperWithSynchronizedMethod_1 extends SuperWithSynchronizedMethod {
    }

    public abstract class InnerSuper {}
}
