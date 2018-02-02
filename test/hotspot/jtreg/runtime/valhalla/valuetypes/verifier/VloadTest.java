/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.valuetypes.verifier;

import java.lang.invoke.*;

import jdk.experimental.value.MethodHandleBuilder;
import sun.hotspot.WhiteBox;

/**
 * @test VloadTest
 * @summary Test vload opcode
 * @modules java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @library /test/lib
 * @compile -XDenableValueTypes VloadTest.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint -XX:+UseG1GC -Xmx128m -XX:+EnableMVT
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.verifier.VloadTest
 */
public class VloadTest {

    public static void main(String[] args) throws Throwable{
        testBytecodes();
    }

    // Test that vload cannot load an integer.
    public static void testBytecodes() throws Throwable {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Object array = MethodHandleBuilder.loadCode(
                lookup,
                "vloadAnInteger",
                MethodType.methodType(Object.class, Integer.TYPE),
                CODE->{
                CODE
                .vload(0)      // Attempt to vload an Integer
                .aconst_null()
                .areturn();
            }).invoke(1000);
        } catch (Throwable t) {
            if (!t.getMessage().contains("java.lang.VerifyError: Bad local variable type")) {
                throw t;
            } else {
                System.out.println("Successful detection of vload verification error");
            }
        }
    }
}
