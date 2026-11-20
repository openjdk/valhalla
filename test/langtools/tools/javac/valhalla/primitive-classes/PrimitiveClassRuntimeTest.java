/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @summary implement primitive classes declaration
 * @enablePreview
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 * @run main PrimitiveClassRuntimeTest
 */

import java.lang.reflect.Modifier;
import java.util.Objects;

import com.sun.tools.javac.util.Assert;

primitive class complex {
    double r;
    double i;

    public complex() {
        this(0, 0);
    }

    complex(int r, int i) {
        this.r = r;
        this.i = i;
    }
}

public class PrimitiveClassRuntimeTest {

    static primitive class nested {
        long n;

        public nested() {
            this(0);
        }

        nested(long n) {
            this.n = n;
        }
    }

    static primitive class empty {
        public empty() {}
    }

    public static void main(String... args) {
        check(new complex(1, 2), new complex(1, 2));
        check(new complex(), new complex(0, 0));
        check(new nested(5), new nested(5));
        check(new empty(), new empty());

        primitive class local {
            double d;
            public local() {
                this(0);
            }
            local(double d) {
                this.d = d;
            }
        }
        check(new local(1.5), new local(1.5));
    }

    /**
     * Checks that {@code a} is an instance of a final value class, and that it is equal to {@code b}.
     */
    static void check(Object a, Object b) {
        Class<?> cls = a.getClass();
        Assert.check(cls.isValue(), cls + " should be a value class");
        Assert.check(Modifier.isFinal(cls.getModifiers()), cls + " should be final");
        Assert.check(!Objects.hasIdentity(a), cls + ": instances should have no identity");
        Assert.check(a == b, cls + ": instances with the same field values should be equal");
        Assert.check(a.equals(b) && a.hashCode() == b.hashCode(), cls + ": equals and hashCode should agree with ==");
        try {
            synchronized (a) {
                throw new AssertionError(cls + ": synchronizing on a value object should fail");
            }
        } catch (IdentityException e) {
            // expected
        }
    }
}
