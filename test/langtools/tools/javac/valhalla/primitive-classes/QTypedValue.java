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

value class QTypedValue {

    QTypedValue! []! f1;
    QTypedValue! []! f2;

    QTypedValue! [][][]! f3;
    QTypedValue! [][][]! f4;

    public QTypedValue(boolean dummy) {
        f1 = new QTypedValue![10];
        f2 = new QTypedValue![10];

        f3 = new QTypedValue![10][10][];
        f4 = new QTypedValue![10][10][];
    }

    void foo(QTypedValue! x, int i) {
        foo(new QTypedValue(), 10);
        QTypedValue! x1, x2, x4, x5, x6;
        if (i == 0) {
            int j = 0; double d = 0.0;
            x1 = new QTypedValue();
            if (j == 0)
                return;
            QTypedValue! x9 = new QTypedValue();
        }
    }

    public implicit QTypedValue();
}