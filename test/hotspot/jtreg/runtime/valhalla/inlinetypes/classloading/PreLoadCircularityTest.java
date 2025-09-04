
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
 *
 */

/*
 * @test
 * @summary Ensures circularity through inheritance does not crash.
            Class Gen10 will have 30 fields and Field0 will inherit from Gen15.
            This forms a cycle through field preloading and inheritance.
 * @enablePreview
 * @compile BigClassTreeClassLoader.java
 * @run main PreLoadCircularityTest 30 10 java.lang.Object Gen15
 */

/*
 * @test
 * @summary Ensures circularity through fields does not crash.
            Class Gen10 will have 30 fields and Field0 will refer to Gen10.
            This forms a cycle through field preloading.
 * @enablePreview
 * @compile BigClassTreeClassLoader.java
 * @run main PreLoadCircularityTest 30 10 Gen10
 */

/*
 * @test
 * @summary Ensures circularity through fields and inheritance does not crash.
            Class Gen10 will have 30 fields and Field0 will inherit from
            Gen15 and refer to Gen13.
            This forms a cycle through field and inheritance preloading.
 * @enablePreview
 * @compile BigClassTreeClassLoader.java
 * @run main PreLoadCircularityTest 30 10 Gen13 Gen15
 */

 // TOOD: should i do some asserts on log msgs?

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

// This test makes use of BigClassTreeClassLoader. Please refer to its documentation
// for a better understanding of e.g. what Gen10 refers to.
public class PreLoadCircularityTest {
  // Order of parameters:
  // depthLimit, fieldIndex[, innermostFieldClass[, innermostFieldParent]]
  public static void main(String[] args) {
    try {
      // Set up parameters.
      int depthLimit = Integer.valueOf(args[0]);
      int fieldIndex = Integer.valueOf(args[1]);
      Optional<String> fieldClass = args.length > 2 ? Optional.of(args[2]) : Optional.empty();
      Optional<String> parentClass = args.length > 3 ? Optional.of(args[3]) : Optional.empty();
      // Create the generator.
      BigClassTreeClassLoader.FieldGeneration fg =
        new BigClassTreeClassLoader.FieldGeneration(fieldIndex, fieldClass, parentClass);
      BigClassTreeClassLoader cl = new BigClassTreeClassLoader(depthLimit, fg);
      // Generate the classes!
      Class<?> clazz = Class.forName("Gen" + (depthLimit - 1), false, cl);
      Object instance = clazz.getDeclaredConstructor().newInstance();
      System.out.println(instance);
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("test not parameterized properly", e);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("generated class could not be found, likely VM bug", e);
    } catch (NoSuchMethodException |
             IllegalAccessException |
             InstantiationException |
             InvocationTargetException e) {
      throw new RuntimeException("test bug: setup of test saw error", e);
    }
  }
}
