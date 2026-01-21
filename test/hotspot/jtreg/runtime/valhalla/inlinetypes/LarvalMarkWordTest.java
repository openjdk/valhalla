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

/**
 * @test
 * @summary ensure larval bit is kept by Serial
 * @library /test/lib /
 * @requires vm.flagless
 * @enablePreview
 * @modules java.base/jdk.internal.misc
            java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+UseSerialGC -Xlog:gc*=info
                     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */

/**
 * @test
 * @summary ensure larval bit is kept by Parallel
 * @library /test/lib /
 * @requires vm.flagless
 * @enablePreview
 * @modules java.base/jdk.internal.misc
            java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+UseParallelGC -Xlog:gc*=info
                     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */

/**
 * @test
 * @summary ensure larval bit is kept by G1
 * @library /test/lib /
 * @requires vm.flagless
 * @enablePreview
 * @modules java.base/jdk.internal.misc
            java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+UseG1GC -Xlog:gc*=info
                     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */

/**
 * @test
 * @summary ensure larval bit is kept by Z
 * @library /test/lib /
 * @requires vm.flagless
 * @enablePreview
 * @modules java.base/jdk.internal.misc
            java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+UseZGC -Xlog:gc*=info
                     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */
package runtime.valhalla.inlinetypes;

import jdk.test.whitebox.WhiteBox;

import jdk.internal.misc.Unsafe;

public class LarvalMarkWordTest {
  private static final Unsafe UNSAFE = Unsafe.getUnsafe();
  private static final WhiteBox WB = WhiteBox.getWhiteBox();

  // We want to ensure that the larval bit in the markWord is preserved
  // accross GC events.
  public static void main(String[] args) throws ReflectiveOperationException {
    // Take a value class with one field and record its field offset.
    MyValue val = new MyValue(0);
    long offset = UNSAFE.objectFieldOffset(val.getClass().getDeclaredField("x"));
    // Create a new larval value object.
    val = UNSAFE.makePrivateBuffer(val);
    // Write a new value to it.
    UNSAFE.putLong(val, offset, 19);

    // Explicitly cause a full GC.
    // This will cause relocations to happen, which is what we want to test.
    WB.fullGC();

    // At this point, the GC's preservation should have seen that val was
    // a larval, and if it did anything with val, preserved the markWord.
    // There is a VM assertion that will fail here if the bit is lost.
    val = UNSAFE.finishPrivateBuffer(val);
    // Sanity check just in case.
    if (val.x != 19) {
      throw new IllegalStateException("something went wrong during larval construction");
    }
  }

  private static value class MyValue {
    private final int x;

    private MyValue(int x) {
      this.x = x;
    }
  }
}
