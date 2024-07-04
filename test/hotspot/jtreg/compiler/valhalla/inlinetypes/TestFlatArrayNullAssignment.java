/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
* @bug 8333852
* @summary Test NullPointerException generation on null value assignment to flat arrays.
* @enablePreview
* @library /test/lib /
* @compile --add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED TestFlatArrayNullAssignment.java
* @run main/othervm --enable-preview -Xint compiler.valhalla.inlinetypes.TestFlatArrayNullAssignment
* @run main/othervm --enable-preview -XX:TieredStopAtLevel=3 -Xbatch compiler.valhalla.inlinetypes.TestFlatArrayNullAssignment
* @run main/othervm --enable-preview -XX:-TieredCompilation -Xbatch compiler.valhalla.inlinetypes.TestFlatArrayNullAssignment
*/
package compiler.valhalla.inlinetypes;

import compiler.lib.ir_framework.*;
import java.util.Random;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestrictedArray;
import jdk.internal.vm.annotation.LooselyConsistentValue;

@NullRestrictedArray
@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue {
   public int field;
   MyValue(int field_val) {
      field = field_val;
   }
}

public class TestFlatArrayNullAssignment {
  public static void test(int i) {
      MyValue [] varr = new MyValue[16];
      // C2 will treat it as UCT and de-optimize, Interpreter will throw NPE.
      if (i == 15000) {
          varr[1] = null;
      }
  }
  public static void main(String [] args) {
     try {
         for (int i = 0; i < 100000; i++) {
           test(i);
         }
         throw new AssertionError("NullPointerException Expected");
     } catch (NullPointerException e) {
         System.out.println(e);
         System.out.println("PASSED");
     }
  }
}
