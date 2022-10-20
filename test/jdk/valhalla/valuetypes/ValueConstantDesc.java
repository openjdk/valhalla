/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test ConstantDesc for primitive classes
 * @compile --enable-preview --source ${jdk.version} -XDenablePrimitiveClasses Point.java ValueConstantDesc.java
 * @run testng/othervm --enable-preview -XX:+EnableValhalla -XX:+EnablePrimitiveClasses ValueConstantDesc
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import jdk.internal.value.PrimitiveClass;

import static org.testng.Assert.*;

public class ValueConstantDesc {
    private static final String NAME = "Point";
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @DataProvider(name="descs")
    static Object[][] descs() {
        return new Object[][]{
            new Object[] { PrimitiveClass.asValueType(Point.class),     ClassDesc.ofDescriptor("Q" + NAME + ";"), NAME},
            new Object[] { Point.ref.class, ClassDesc.ofDescriptor("L" + NAME + ";"), NAME},
            new Object[] { Point[].class,   ClassDesc.ofDescriptor("[Q" + NAME + ";"), NAME + "[]"},
            new Object[] { Point.ref[][].class, ClassDesc.ofDescriptor("[[L" + NAME + ";"), NAME + "[][]"},
            new Object[] { ValueOptional.class, ClassDesc.ofDescriptor("LValueOptional;"), "ValueOptional"},
            new Object[] { ValueOptional[].class, ClassDesc.ofDescriptor("[LValueOptional;"), "ValueOptional[]"},
            new Object[] { ValueOptional[][].class, ClassDesc.ofDescriptor("[[LValueOptional;"), "ValueOptional[][]"},
        };
    }

    @Test(dataProvider="descs")
    public void classDesc(Class<?> type, ClassDesc expected, String displayName) {
        ClassDesc cd = type.describeConstable().orElseThrow();
        if (type.isArray()) {
            assertTrue(cd.isArray());
            assertFalse(cd.isClassOrInterface());
        } else {
            assertFalse(cd.isArray());
            assertTrue(cd.isClassOrInterface());
        }
        assertEquals(cd, expected);
        assertEquals(cd.displayName(), displayName);
        assertEquals(cd.descriptorString(), type.descriptorString());
    }

    @DataProvider(name="componentTypes")
    static Object[][] componentTypes() {
        return new Object[][]{
            new Object[] { PrimitiveClass.asValueType(Point.class) },
            new Object[] { Point.ref.class },
            new Object[] { ValueOptional.class }
        };
    }

    @Test(dataProvider="componentTypes")
    public void arrayType(Class<?> componentType) {
        ClassDesc cd = componentType.describeConstable().orElseThrow();
        ClassDesc arrayDesc = cd.arrayType();
        ClassDesc arrayDesc2 = cd.arrayType(2);

        assertTrue(arrayDesc.isArray());
        assertEquals(arrayDesc.componentType(), cd);
        assertTrue(arrayDesc2.isArray());
        assertEquals(arrayDesc2.componentType(), arrayDesc);
    }

    @DataProvider(name="valueDesc")
    static Object[][] valueDesc() {
        return new Object[][]{
                new Object[] { PrimitiveClass.asValueType(Point.class),         "Q" + NAME + ";"},
                new Object[] { Point.ref.class,     "L" + NAME + ";"},
                new Object[] { Point[].class,       "[Q" + NAME + ";"},
                new Object[] { Point.ref[][].class, "[[L" + NAME + ";"},
                new Object[] { ValueOptional.class, "LValueOptional;"},
                new Object[] { ValueOptional[].class, "[LValueOptional;"},
        };
    }
    @Test(dataProvider="valueDesc")
    public void asValueType(Class<?> type, String descriptor) throws ReflectiveOperationException {
        ClassDesc cd = type.describeConstable().orElseThrow();
        ClassDesc valueDesc = ClassDesc.ofDescriptor(descriptor);
        assertEquals(cd, valueDesc);
        Class<?> c = (Class<?>) cd.resolveConstantDesc(LOOKUP);
        assertTrue(c == type);
        assertTrue(PrimitiveClass.isPrimitiveValueClassDesc(cd) == PrimitiveClass.isPrimitiveValueType(type));
    }
    @DataProvider(name="classes")
    static Object[][] classes() {
        Class<?> valType = PrimitiveClass.asValueType(Point.class);
        Class<?> refType = Point.class;

        return new Object[][]{
            new Object[] { ValueOptional.class, "(Ljava/lang/Object;)LValueOptional;" },
            new Object[] { valType, "(II)QPoint;" },
            new Object[] { refType, "(II)QPoint;" },
        };
    }
    @Test(dataProvider="classes")
    public void directMethodHandleDesc(Class<?> type, String methodDescriptor) throws Throwable {
        ClassDesc cd = type.describeConstable().orElseThrow();
        MethodTypeDesc methodTypeDesc = MethodTypeDesc.ofDescriptor(methodDescriptor);

        DirectMethodHandleDesc dmhDesc = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, cd, "<vnew>", methodTypeDesc);
        MethodHandle mh = (MethodHandle) dmhDesc.resolveConstantDesc(LOOKUP);
        MethodType methodType = (MethodType) methodTypeDesc.resolveConstantDesc(LOOKUP);
        MethodHandle vnew = LOOKUP.findStatic(type, "<vnew>", methodType);
        assertMethodHandleEquals(mh, vnew);
    }

    private static void assertMethodHandleEquals(MethodHandle mh1, MethodHandle mh2) {
        MethodHandleInfo minfo1 = LOOKUP.revealDirect(mh1);
        MethodHandleInfo minfo2 = LOOKUP.revealDirect(mh2);
        assertEquals(minfo1.getDeclaringClass(), minfo2.getDeclaringClass());
        assertEquals(minfo1.getName(), minfo2.getName());
        assertEquals(minfo1.getMethodType(), minfo2.getMethodType());
    }

    @Test(expectedExceptions = {LinkageError.class})
    public void illegalDescriptor() throws ReflectiveOperationException {
        // ValueConstantDesc is not a primitive class
        ClassDesc cd = ClassDesc.ofDescriptor("QValueConstantDesc;");
        Class<?> c = (Class<?>) cd.resolveConstantDesc(LOOKUP);
    }
}
