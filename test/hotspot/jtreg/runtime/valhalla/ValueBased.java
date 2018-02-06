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

import java.time.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/*
 * @test ValueBased
 * @summary Verify classes may defined as "Value-based"
 * @library /
 * @modules java.base
 * @run testng/othervm -XX:ValueBasedClasses=java/time/LocalTime,java/time/LocalDateTime,ValueBased$Value ValueBased
 */
public class ValueBased {

    static final class Value {
        final int foo;

        private Value(int foo) { this.foo = foo;  }

        public String toString() {
            return "Value foo=" + foo;
        }

        public static Value make(int foo) {
            return new Value(foo);
        }
    }

    public static boolean testAcmp(Object obj1, Object obj2) {
        return obj1 == obj2;
    }

    @Test
    public void testValue() {
        // Sanity check user defined class
        Value v1 = Value.make(47);
        testAcmp(v1, v1);
    }

    @Test
    public void testJdkValueBasedClasses() {
        // Sanity cfp is not broken on JDK class
        LocalTime time1 = LocalTime.of(0, 0, 0, 999);
        LocalTime time2 = LocalTime.of(0, 0, 0, 999);
        testAcmp(time1, time2);
    }
}
