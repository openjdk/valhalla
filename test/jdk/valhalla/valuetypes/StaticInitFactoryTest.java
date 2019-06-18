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
 * @summary test MethodHandle of static init factories
 * @run main/othervm StaticInitFactoryTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class StaticInitFactoryTest {
    static interface Cons {};

    static inline class DefaultConstructor implements Cons {
        int x = 0;
    }
    static inline class ConstructorWithArgs implements Cons {
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

    public static void main(String... args) throws Throwable {
        // test default static init factory
        MethodType mtype0 = MethodType.methodType(DefaultConstructor.class);
        MethodHandle mh0 = staticInitFactory(DefaultConstructor.class, mtype0);
        DefaultConstructor o0 = (DefaultConstructor)mh0.invokeExact();
        assertEquals(o0, new DefaultConstructor());
        assertEquals(o0, newInstance(DefaultConstructor.class, 0, mh0.type().parameterArray()));

        // test 1-arg static init factory
        MethodType mtype1 = MethodType.methodType(ConstructorWithArgs.class, int.class);
        MethodHandle mh1 = staticInitFactory(ConstructorWithArgs.class, mtype1);
        ConstructorWithArgs o1 = (ConstructorWithArgs)mh1.invokeExact(1);
        assertEquals(o1, new ConstructorWithArgs(1));
        assertEquals(o1, newInstance(ConstructorWithArgs.class, Modifier.PUBLIC, mh1.type().parameterArray(), 1));


        // test 2-arg static init factory
        MethodType mtype2 = MethodType.methodType(ConstructorWithArgs.class, int.class, int.class);
        MethodHandle mh2 = staticInitFactory(ConstructorWithArgs.class, mtype2);
        ConstructorWithArgs o2 = (ConstructorWithArgs)mh2.invokeExact(1, 2);
        assertEquals(o2, new ConstructorWithArgs(1, 2));
        assertEquals(o2, newInstance(ConstructorWithArgs.class, 0, mh2.type().parameterArray(), 1, 2));


        // test 3-arg static init factory
        MethodType mtype3 = MethodType.methodType(ConstructorWithArgs.class, int.class, int.class, int.class);
        MethodHandle mh3 = staticInitFactory(ConstructorWithArgs.class, mtype3);
        ConstructorWithArgs o3 = (ConstructorWithArgs)mh3.invokeExact(1, 2, 3);
        assertEquals(o3, new ConstructorWithArgs(1, 2, 3));
        assertEquals(o3, newInstance(ConstructorWithArgs.class, Modifier.PRIVATE, mh3.type().parameterArray(), 1, 2, 3));
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
            lookup.findConstructor(DefaultConstructor.class, mtype);
            throw new RuntimeException("findConstructor should not find the static init factory");
        } catch (NoSuchMethodException e) {
        }

        // crack method handle
        MethodHandleInfo minfo = lookup.revealDirect(mh);
        assertEquals(minfo.getDeclaringClass(), c);
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

    static void assertTrue(boolean v) {
        if (!v) {
            throw new AssertionError("expected true");
        }
    }

    static void assertEquals(Object o1, Object o2) {
        if (o1 == o2) return;

        if (!o1.equals(o2)) {
            throw new AssertionError(o1 + " != " + o2);
        }
    }
}
