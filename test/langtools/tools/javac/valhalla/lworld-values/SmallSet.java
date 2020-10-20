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

public inline class SmallSet {

  final int value;

  public SmallSet(int value) {
    this.value = value;
  }

  private static int checkRange(final int i) {
    if (i > 31)
      throw new IllegalArgumentException("out of range: i>31");
    if (i < 0)
      throw new IllegalArgumentException("out of range: i<0");
    return i;
  }

  public static SmallSet of(final int... i) {
    int set = 0;
    for (final int integer : i)
      set |= (1 << checkRange(integer));
    return new SmallSet(set);
  }

  public boolean contains(final int element) {
    return (this.value & (1 << checkRange(element))) != 0;
  }

  public SmallSet add(final int element) {
    return new SmallSet(this.value  | (1 << checkRange(element)));
  }

  static final class MutableInt {
    int value = 0;
  }
}
