/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @test
 * @modules java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.value
 * @compile -XDenablePrimitiveClasses NullRestrictedTest.java
 * @run junit/othervm -XX:+EnableValhalla NullRestrictedTest
 * @run junit/othervm -XX:+EnableValhalla -XX:InlineFieldMaxFlatSize=0 NullRestrictedTest
 */
public class NullRestrictedTest {
    @ImplicitlyConstructible
    static value class MyValueEmpty {
    }

    @ImplicitlyConstructible
    static value class EmptyContainer {
        @NullRestricted
        MyValueEmpty empty = new MyValueEmpty();
    }

    @Test
    public void lazyInitializedDefaultValue() {
        // VM lazily sets the null-restricted non-flat field to zero default
        assertTrue(new EmptyContainer() == ValueClass.zeroInstance(EmptyContainer.class));
    }

    @Test
    public void testMethodHandle() throws Throwable {
        var mh = MethodHandles.lookup().findGetter(EmptyContainer.class, "empty", MyValueEmpty.class);
        assertTrue(mh.invoke(new EmptyContainer()) == ValueClass.zeroInstance(MyValueEmpty.class));
    }

    @Test
    public void testVarHandle() throws Throwable {
        var vh = MethodHandles.lookup().findVarHandle(EmptyContainer.class, "empty", MyValueEmpty.class);
        assertTrue(vh.get(new EmptyContainer()) == ValueClass.zeroInstance(MyValueEmpty.class));
    }

    @Test
    public void testField() throws Throwable {
        var f = EmptyContainer.class.getDeclaredField("empty");
        assertTrue(f.get(new EmptyContainer()) == ValueClass.zeroInstance(MyValueEmpty.class));
    }
}
