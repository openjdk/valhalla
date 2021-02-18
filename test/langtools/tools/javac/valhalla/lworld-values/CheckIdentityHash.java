/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237071
 * @summary Totalize System.identityHashCode for inline types.
 * @compile CheckIdentityHash.java 
 */

final primitive class CheckIdentityHash {
    int identityHashCode(CheckIdentityHash x) {
        return 0;
    }
    void test(CheckIdentityHash v) {
        this.identityHashCode(v);
        System.identityHashCode(v);
        System.identityHashCode(this);
        java.lang.System.identityHashCode(v);
        java.lang.System.identityHashCode(this);
    }
    int x = 10;
}
