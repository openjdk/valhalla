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
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseSerialGC -Xmx50M -Xlog:gc*=info runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */
/**
 * @test
 * @summary ensure larval bit is kept by Parallel
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseParallelGC -Xmx50M -Xlog:gc*=info runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */
/**
 * @test
 * @summary ensure larval bit is kept by G1
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseG1GC -Xmx50M -Xlog:gc*=info runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */
/**
 * @test
 * @summary ensure larval bit is kept by Z
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseZGC -Xmx50M -Xlog:gc*=info runtime.valhalla.inlinetypes.LarvalMarkWordTest
 */

package runtime.valhalla.inlinetypes;

import jdk.internal.misc.Unsafe;

public class LarvalMarkWordTest {
  private static final Unsafe UNSAFE = Unsafe.getUnsafe();

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
    // Now we make a bunch of objects.
    // Ideally, we want the object to be relocated, but this isn't guaranteed.
    stressTheGC();
    // Hopefully the GC has already decided to do a GC, but if not we
    // suggest it do one. This suggestion may be ignored.
    System.gc();

    // At this point, the GC's preservation should have seen that val was
    // a larval, and if it did anything with val, preserved the markWord.
    // There is a VM assertion that will fail here if the bit is lost.
    val = UNSAFE.finishPrivateBuffer(val);
    // Sanity check just in case.
    if (val.x != 19) {
      throw new IllegalStateException("something went wrong during larval construction");
    }
  }

  private static void stressTheGC() {
    int ignore = 0;
    for (int i = 0; i < 100_000_000; i++) {
      Object obj = new Object();
      // Arbitrary operation, has no meaning.
      ignore %= obj.getClass().getName().length();
    }
    if (ignore == -1) {
      // Will never be thrown.
      throw new RuntimeException("will never throw");
    }
  }


  private static value class MyValue {
    private final int x;

    private MyValue(int x) {
      this.x = x;
    }
  }
}
