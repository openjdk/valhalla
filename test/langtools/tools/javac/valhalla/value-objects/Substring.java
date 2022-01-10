/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8279368
 * @summary Add parser support for declaration of value classes
 * @compile Substring.java
 */

public value class Substring implements CharSequence {
    private String str;
    private int start;
    private int end;

    public Substring(String str, int start, int end) {
        checkBounds(start, end, str.length());
        this.str = str;
        this.start = start;
        this.end = end;
    }

    public int length() {
        return end - start;
    }

    public char charAt(int i) {
        checkBounds(0, i, length());
        return str.charAt(start + i);
    }

    public Substring subSequence(int s, int e) {
        checkBounds(s, e, length());
        return new Substring(str, start + s, start + e);
    }

    public String toString() {
        return str.substring(start, end);
    }

    private static void checkBounds(int start, int end, int length) {
        if (start < 0 || end < start || length < end)
            throw new IndexOutOfBoundsException();
    }
}
