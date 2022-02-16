/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test MethodHandle of static init factories
 * @run testng/othervm StaticFactoryMethodHandleTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodType.*;
import static org.testng.Assert.*;

public class StaticFactoryMethodHandleTest {
    static interface Cons {};

    static primitive class DefaultConstructor implements Cons {
        int x = 0;
    }
    static primitive class ConstructorWithArgs implements Cons {
        int x;
        int y;
        public ConstructorWithArgs(int x) {
            this.x = x;
            this.y = 100;
        }

        /* package-private */ ConstructorWithArgs(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private ConstructorWithArgs(int x, int y, int z) {
            this.x = x+z;
            this.y = y+z;
        }
    }
    static value class Value implements Cons {
        int x;
        public Value(int x) {
            this.x = x;
        }
    }

    /*
     * Test no-arg static factory
     */
    @Test
    public void testNoArgStaticFactory() throws Throwable {
        // test default static init factory
        Class<? extends Cons> cls = (Class<? extends Cons>)DefaultConstructor.class.asValueType();
        MethodHandle mh = staticInitFactory(cls, methodType(cls));
        DefaultConstructor o = (DefaultConstructor)mh.invokeExact();
        assertEquals(o, new DefaultConstructor());
        assertEquals(o, newInstance(cls, 0, new Class<?>[0]));
    }

    @DataProvider(name="ctorWithArgs")
    static Object[][] ctorWithArgs() {
        Class<? extends Cons> cls = (Class<? extends Cons>)ConstructorWithArgs.class.asValueType();
        return new Object[][]{
                new Object[] { cls, methodType(cls, int.class), Modifier.PUBLIC, new ConstructorWithArgs(1) },
                new Object[] { cls, methodType(cls, int.class, int.class), 0, new ConstructorWithArgs(1, 2) },
                new Object[] { cls, methodType(cls, int.class, int.class, int.class), Modifier.PRIVATE, new ConstructorWithArgs(1, 2, 3) },
        };
    }

    /*
     * Test static factory with args
     */
    @Test(dataProvider="ctorWithArgs")
    public void testStaticFactoryWithArgs(Class<? extends Cons> c, MethodType mtype, int modifiers, ConstructorWithArgs o) throws Throwable {
        MethodHandle mh = staticInitFactory(c, mtype);
        ConstructorWithArgs o1;
        Object o2;
        switch (mtype.parameterCount()) {
            case 1: o1 = (ConstructorWithArgs)mh.invokeExact(1);
                    o2 = newInstance(c, modifiers, mtype.parameterArray(), 1);
                    break;
            case 2: o1 = (ConstructorWithArgs)mh.invokeExact(1, 2);
                    o2 = newInstance(c, modifiers, mtype.parameterArray(), 1, 2);
                    break;
            case 3: o1 = (ConstructorWithArgs)mh.invokeExact(1, 2, 3);
                    o2 = newInstance(c, modifiers, mtype.parameterArray(), 1, 2, 3);
                    break;
            default:
                    throw new IllegalArgumentException(c + " " + mtype);
        }

        assertEquals(o1, o);
        assertEquals(o1, o2);
    }

    @Test
    public void testValueClasstaticFactory() throws Throwable {
        // test default static init factory
        MethodType mtype = methodType(Value.class, int.class);
        MethodHandle mh = staticInitFactory(Value.class, mtype);
        Value o = (Value)mh.invokeExact(10);
        assertEquals(o, new Value(10));
        assertEquals(o, newInstance(Value.class, Modifier.PUBLIC, mtype.parameterArray(), 10));
    }

    /*
     * Test the following API when looking up a static init factory
     *
     * 1. Lookup::findStatic accepts "<init>" to lookup a static init factory
     * 2. Lookup::findConstructor is for invokespecial <init> constructor bytecode pattern
     *    i.e. the instance <init> constructor.
     *    Hence it won't find the static <init> factory.
     * 3. Lookup::revealDirect cracks the method handle info
     */
    static MethodHandle staticInitFactory(Class<? extends Cons> c, MethodType mtype) throws Throwable {
        Lookup lookup = MethodHandles.lookup();
        //
        MethodHandle mh = lookup.findStatic(c, "<init>", mtype);
        try {
            lookup.findConstructor(DefaultConstructor.class.asValueType(), mtype);
            throw new RuntimeException("findConstructor should not find the static init factory");
        } catch (NoSuchMethodException e) {
        }

        // crack method handle
        MethodHandleInfo minfo = lookup.revealDirect(mh);
        assertEquals(minfo.getDeclaringClass(), c.asPrimaryType());
        assertEquals(minfo.getName(), "<init>");
        assertEquals(minfo.getReferenceKind(), MethodHandleInfo.REF_invokeStatic);
        assertEquals(minfo.getMethodType(), mtype);
        return mh;
    }

    /*
     * Test unreflectConstructor and also generic invoke
     */
    static Cons newInstance(Class<? extends Cons> c, int mods, Class<?>[] params, int... args) throws Throwable {
        // get Constructor from getConstructor/getDeclaredConstructor
        Constructor<? extends Cons> ctor;
        if (Modifier.isPublic(mods)) {
            ctor = c.getConstructor(params);
        } else {
            ctor = c.getDeclaredConstructor(params);
        }

        // unreflect constructor
        MethodHandle mh = MethodHandles.lookup().unreflectConstructor(ctor);
        assertEquals(params.length, args.length);
        Cons o1, o2;
        switch (args.length) {
            case 0:
                o1 = (Cons) mh.invoke();
                o2 = ctor.newInstance();
                break;
            case 1:
                o1 = (Cons) mh.invoke(args[0]);
                o2 = ctor.newInstance(args[0]);
                break;
            case 2:
                o1 = (Cons) mh.invoke(args[0], args[1]);
                o2 = ctor.newInstance(args[0], args[1]);
                break;
            case 3:
                o1 = (Cons) mh.invoke(args[0], args[1], args[2]);
                o2 = ctor.newInstance(args[0], args[1], args[2]);
                break;
            default:
                throw new IllegalArgumentException(Arrays.toString(args));
        }
        assertEquals(o1, o2);
        return o1;
    }
}
