/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes.verifier;

import java.lang.invoke.*;
import jdk.incubator.mvt.ValueType;
import jdk.experimental.value.MethodHandleBuilder;

import static jdk.test.lib.Asserts.*;

/*
 * @test VunboxErrorIndex
 * @summary Exercise the verifier for vunbox bytecode with a non-value type CP index.
 * @modules java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @library /test/lib
 * @build runtime.valhalla.valuetypes.verifier.ValueCapableClass
 * @run main/othervm -Xint -verify -XX:+EnableMVT runtime.valhalla.valuetypes.verifier.VunboxErrorIndex
 */
public class VunboxErrorIndex {

    public static void main(String[] args) {
        testCorrectBoxing();
        testIncorrectBoxing();
    }

    public static void testCorrectBoxing() {
        Class<?> vcc = ValueCapableClass.class;
        Class<?> dvt = ValueType.forClass(vcc).valueClass();

        MethodHandle newDvt = newDvtMh(dvt);
        MethodHandle box = boxMh(dvt, vcc);
        MethodHandle unbox = unboxMh(vcc, dvt);

        ValueCapableClass newObject = ValueCapableClass.create(3, (short)7, (short)11);
        ValueCapableClass newBoxed = null;

        try {
            ValueCapableClass boxed = (ValueCapableClass) MethodHandles.filterReturnValue(newDvt, box).invokeExact();
            // Assuming MVT1.0, where source has no way of holding an actual value at the language level, so: box(unbox(Object))
            newBoxed = (ValueCapableClass) MethodHandles.filterReturnValue(unbox, box).invokeExact(newObject);
        }
        catch (Throwable t) { fail("Invokation Exception", t); }

        assertTrue(newObject.getX() == newBoxed.getX());
        assertTrue(newObject.getY() == newBoxed.getY());
        assertTrue(newObject.getZ() == newBoxed.getZ());
    }

    public static void testIncorrectBoxing() {
        Class<?> vcc = ValueCapableClass.class;
        Class<?> dvt = ValueType.forClass(vcc).valueClass();

        MethodHandle newDvt = newDvtMh(dvt);
        MethodHandle box = boxMh(dvt, String.class); // Illegal box type
        try {
            MethodHandles.filterReturnValue(newDvt, box).invoke();
            fail("Expected ClassCastException");
        }
        catch (ClassCastException cce) {}
        catch (Throwable t) { fail("Invokation Exception", t); }

        // With -verify, this test will yield a VerifyError
        // since the constant pool entry at the index is not a
        // symbolic reference to a direct value class type.
        try {
            MethodHandle unbox = unboxMh(vcc, String.class); // Illegal unbox type
            unbox.invoke(ValueCapableClass.create());
            fail("Expected VerifyError");
        }
        catch (Throwable t) {
            if (!t.getMessage().contains("java.lang.VerifyError")) {
                fail("Invokation Exception", t);
            }
        }
    }

    /*
       Create new DVT via loading a value array element. Why this workaround:
       1) to avoid "ValueType.defaultValueConstant()" which may or may not be implemented with vunbox
    */
    public static MethodHandle newDvtMh(Class<?> dvt) {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(), "newDvt", MethodType.methodType(dvt), CODE->{
                CODE.iconst_1().anewarray(dvt).iconst_0().vaload().vreturn();
            });
    }

    public static MethodHandle boxMh(Class<?> inClass, Class<?> outClass) {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(), "box", MethodType.methodType(outClass, inClass), CODE->{
                CODE.vload(0).vbox(outClass).areturn();
            });
    }

    public static MethodHandle unboxMh(Class<?> inClass, Class<?> outClass)  {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(), "box", MethodType.methodType(outClass, inClass), CODE->{
                CODE.aload(0).vunbox(outClass).vreturn();
            });
    }

}
