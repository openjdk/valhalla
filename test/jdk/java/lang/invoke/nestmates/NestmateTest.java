/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199386
 * @build Invoker
 * @run testng/othervm NestmateTest
 * @summary Lookup::in teleports to a nestmate and produces a private lookup
 */

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static java.lang.invoke.MethodHandles.Lookup.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import static org.testng.Assert.*;

// See also http://mail.openjdk.java.net/pipermail/valhalla-spec-experts/2016-January/000071.html
public class NestmateTest {
    /*
     * Teleporting from a lookup to a nestmate produces a full-power lookup
     */
    private static void assertThreadLocalNestmate(Class<?> member) throws Throwable {
        Lookup lookup = MyThreadLocal.lookup();
        Lookup lookup2 = lookup.in(member);
        assertTrue(lookup.lookupClass().isNestmateOf(member));
        assertTrue(lookup2.hasPrivateAccess());

        // can invoke a private method
        MethodHandle mh = lookup2.findStatic(MyThreadLocal.class, "testNestmateAccess", methodType(void.class));
        mh.invokeExact();

        try {
            // no access to teleport to a protected method in its super class in a different package
            lookup2.findSpecial(ThreadLocal.class, "initialValue",
                                methodType(Object.class),
                                MyThreadLocal.class);
            assertTrue(false);
        } catch (IllegalAccessException e) {}

        // cross-package teleport
        Lookup l = lookup2.in(ThreadLocal.class);
        assertTrue(l.lookupModes() == PUBLIC);
    }

    @Test
    public void test() throws Throwable {
        // not a nestmate of this class
        Lookup L = lookup().in(MyThreadLocal.Inner.class);
        assertTrue(L.lookupClass() == MyThreadLocal.Inner.class);
        assertTrue((L.lookupModes() & PRIVATE) == 0);

        // nestmate of MyThreadLocal
        assertThreadLocalNestmate(MyThreadLocal.Inner.class);
    }

    // dynamic invocation of super::initialValue
    @Test
    public void superInitMethodHandle() throws Throwable {
        MyThreadLocal tl = new MyThreadLocal();
        MethodHandle mh = tl.superInitMH();
        mh = mh.bindTo(tl);
        assertTrue(mh.invokeExact() == null);
    }

    // dynamic invocation of super::initialValue from a nestmate class
    @Test
    public void superInitInnerMethodHandle() throws Throwable {
        MyThreadLocal tl = new MyThreadLocal();
        MethodHandle mh = MyThreadLocal.Inner2.superInitInnerMH();
        invokeMH(tl, mh);
    }

    /*
     * Test the dynamic invocation of ThreadLocal::initialValue where
     * a bridge method may not exist.
     *
     * Spins a hidden nestmate class that invokes ThreadLocal::initialValue.
     * It's a nestmate of MyThreadLocal which may or may not have the
     * bridge method.  Even it has the bridge method, a library may not
     * know about the synthetic method name to invoke.
     */
    private static void invokeMH(ThreadLocal tl, MethodHandle mh) throws Throwable {
        Path path = Paths.get(System.getProperty("test.classes"), "Invoker.class");
        byte[] bytes = Files.readAllBytes(path);
        Lookup lookup = MyThreadLocal.lookup()
                .defineHiddenClassWithClassData(bytes, mh.bindTo(tl), ClassOption.NESTMATE);
        Class<?> hiddenClass = lookup.lookupClass();
        assertTrue(hiddenClass.isHiddenClass());
        assertThreadLocalNestmate(hiddenClass);

        // run it
        MethodHandle ctor = lookup.findConstructor(lookup.lookupClass(), methodType(void.class));
        ctor = ctor.asType(methodType(Runnable.class));
        Runnable r = (Runnable)ctor.invokeExact();
        r.run();
    }
}
