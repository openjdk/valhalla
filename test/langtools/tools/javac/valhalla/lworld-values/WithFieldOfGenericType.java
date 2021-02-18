/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8205686 8215109
 * @summary __WithField seems to have trouble if the value type is a generic type.
 * @compile -XDrawDiagnostics -XDdev -XDallowWithFieldOperator WithFieldOfGenericType.java
 * @run main/othervm WithFieldOfGenericType
 */

public final primitive class WithFieldOfGenericType<E> {
  private final boolean value;

  public static <E> WithFieldOfGenericType<E> create() {
    WithFieldOfGenericType<E> bug = WithFieldOfGenericType.default;
    bug = __WithField(bug.value, true);
    return bug;
  }

  private WithFieldOfGenericType() {
    value = false;
    throw new AssertionError();
  }

  public static void main(String[] args) {
     WithFieldOfGenericType<String> w = create();
     if (!w.toString().equals("[WithFieldOfGenericType value=true]"))
        throw new AssertionError("Withfield didn't work!");
  }
}
