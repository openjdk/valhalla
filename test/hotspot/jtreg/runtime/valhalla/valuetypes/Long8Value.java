/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

public final __ByValue class Long8Value {

    final long longField1;
    final long longField2;
    final long longField3;
    final long longField4;
    final long longField5;
    final long longField6;
    final long longField7;
    final long longField8;

    private Long8Value() {
	this.longField1 = 0;
	this.longField2 = 0;
	this.longField3 = 0;
	this.longField4 = 0;
	this.longField5 = 0;
	this.longField6 = 0;
	this.longField7 = 0;
	this.longField8 = 0;
    }

    public long getLongField1() { return longField1; }
    public long getLongField2() { return longField2; }
    public long getLongField3() { return longField3; }
    public long getLongField4() { return longField4; }
    public long getLongField5() { return longField5; }
    public long getLongField6() { return longField6; }
    public long getLongField7() { return longField7; }
    public long getLongField8() { return longField8; }

    __ValueFactory public static Long8Value create(long long1,
                                    long long2,
                                    long long3,
                                    long long4,
                                    long long5,
                                    long long6,
                                    long long7,
                                    long long8) {
        Long8Value l8v = __MakeDefault Long8Value();
	l8v.longField1 = long1;
	l8v.longField2 = long2;
	l8v.longField3 = long3;
	l8v.longField4 = long4;
	l8v.longField5 = long5;
	l8v.longField6 = long6;
	l8v.longField7 = long7;
	l8v.longField8 = long8;
	return l8v;
    }

    static void check(Long8Value value,
                      long       long1,
                      long       long2,
                      long       long3,
                      long       long4,
                      long       long5,
                      long       long6,
                      long       long7,
                      long       long8) {
        Asserts.assertEquals(value.getLongField1(), long1, "Field 1 incorrect");
        Asserts.assertEquals(value.getLongField2(), long2, "Field 2 incorrect");
        Asserts.assertEquals(value.getLongField3(), long3, "Field 3 incorrect");
        Asserts.assertEquals(value.getLongField4(), long4, "Field 4 incorrect");
        Asserts.assertEquals(value.getLongField5(), long5, "Field 5 incorrect");
        Asserts.assertEquals(value.getLongField6(), long6, "Field 6 incorrect");
        Asserts.assertEquals(value.getLongField7(), long7, "Field 7 incorrect");
        Asserts.assertEquals(value.getLongField8(), long8, "Field 8 incorrect");
    }

}
