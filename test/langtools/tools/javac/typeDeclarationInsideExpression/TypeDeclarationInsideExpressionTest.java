/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8295019
 * @summary Cannot call a method with a parameter of a local class declared in a lambda
 * @compile TypeDeclarationInsideExpressionTest.java
 */

class TypeDeclarationInsideExpressionTest {
    class LambdaTest {
        void run(Runnable r) {}

        void m() {
            run(() -> {
                class C {
                    static void takeC(C c) {}
                    static C giveC() {
                        return null;
                    }
                }
                C.takeC(C.giveC());

                record R() {
                    static void takeR(R r) {}
                    static R giveR() { return null; }
                }
                R.takeR(R.giveR());

                interface I {
                    static void takeI(I i) {}
                    static I giveI() { return null; }
                }
                I.takeI(I.giveI());

                enum E {
                    A;
                    static void takeE(E e) {}
                    static E giveE() { return null; }
                }
                E.takeE(E.giveE());
            });
        }
    }

    class SwitchExprTest {
        void run(int i) {}
        void m(int o) {
            run(switch(o) {
                case 1 -> {
                    class C {
                        static int takeC(C c) { return 0; }
                        static C giveC() { return null; }
                    }
                    yield C.takeC(C.giveC());
                }
                case 2 -> {
                    record R() {
                        static int takeR(R r) { return 0; }
                        static R giveR() { return null; }
                    }
                    yield R.takeR(R.giveR());
                }
                case 3 -> {
                    interface I {
                        static int takeI(I i) { return 0; }
                        static I giveI() { return null; }
                    }
                    yield I.takeI(I.giveI());
                }
                case 4 -> {
                    enum E {
                        A;
                        static int takeE(E e) { return 0; }
                        static E giveE() { return null; }
                    }
                    yield E.takeE(E.giveE());
                }
                default -> throw new AssertionError();
            });
        }
    }
}
