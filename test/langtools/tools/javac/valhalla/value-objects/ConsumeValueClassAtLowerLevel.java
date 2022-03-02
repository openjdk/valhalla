/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8282107
 * @summary Check that javac can compile against value classes at lower source levels
 * @compile GenericPoint.java
 * @compile --source 16 ConsumeValueClassAtLowerLevel.java
 * @run main ConsumeValueClassAtLowerLevel
 */

public class ConsumeValueClassAtLowerLevel {
    public static void main(String [] args) {

        /* Attempting to instantiate a value/primitive class in earlier source level should
         * result in an InstantiationError since javac's class reader downgrades
         * the value class to an identity class if current source level does not
         * support value/primitive class. And so rather than invoke the factory method,
         * the source code of earlier vintage would attempt to invoke the constructor
         * and so should crash and burn with an InstantiationError.
         *
         * This behavior is subject to change - see JDK-8282525
         */
        boolean gotIE = false;
        try {
            GenericPoint<Integer> gl = new GenericPoint<>(0, 0);
        } catch (java.lang.InstantiationError ie) {
            gotIE = true;
        }
        if (!gotIE) {
            throw new AssertionError("Did not see instantiation error!");
        }
    }
}
