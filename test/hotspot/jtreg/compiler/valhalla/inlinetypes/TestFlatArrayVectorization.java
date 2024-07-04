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
* @summary Allow flat array layout for implicitly constructible value classes.
* @requires vm.compiler2.enabled
* @enablePreview
* @library /test/lib /
* @compile --add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED TestFlatArrayVectorization.java
* @run main/othervm --enable-preview compiler.valhalla.inlinetypes.TestFlatArrayVectorization
*/

package compiler.valhalla.inlinetypes;
import compiler.lib.ir_framework.*;
import java.util.Random;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestrictedArray;
import jdk.internal.vm.annotation.LooselyConsistentValue;

@ImplicitlyConstructible
@NullRestrictedArray
@LooselyConsistentValue
value class SimpleValue {
   int field;
   public SimpleValue(int field) {
      this.field = field;
   }
}

public class TestFlatArrayVectorization {
   public static SimpleValue [] varr;
   public static final int SIZE = 2048;
   public static final int SIZE_M1 = SIZE - 1;
   public static int res = 0;
   public static Random rd = new Random(2048);

   //public static int test(int ctr) {
   @Test
   @IR(counts = {IRNode.POPULATE_INDEX, " > 0"}, applyIf = {"EnableValhalla", "true"})
   public static int test() {
      varr = new SimpleValue [SIZE];    // ANEWARRAY
      for (int i = 0; i < varr.length; i++) {
          varr[i] = new SimpleValue(i); // AASTORE
      }
      return varr[rd.nextInt(2047)].field;
   }

   public static void main(String [] args) {
      TestFramework.runWithFlags("--enable-preview", "-Xbatch", "-XX:-TieredCompilation", "-XX:-UseOnStackReplacement", "--add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED");
   }
}
