/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Random;
import java.util.stream.Stream;

import org.testng.annotations.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @run testng/othervm -XX:+EnableMVT TestPoint
 */

@Test
public class TestPoint {

    @DataProvider(name = "createPoints")
    public Object[][] createPoints() {
        Object[][] data = new Object[10 * 10][];
        int n = 0;
        for (int i = 0 ; i < 10 ; i++) {
            for (int j = 0 ; j < 10 ; j++) {
                data[n++] = new Object[] { new Point(i, j) };
            }
        }
        return data;
    }

    @DataProvider(name = "createPointArrays")
    public Object[][] createPointArrays() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        Object[][] data = new Object[10][];
        Random rand = new Random();
        for (int n = 0 ; n < 10 ; n++) {
            int length = rand.nextInt(10);
            Point[] boxes = new Point[length];
            Object arr = MethodHandles.arrayConstructor(vt.arrayValueClass()).invoke(length);
            for (int i = 0; i < length; i++) {
                 Point p = new Point(i, i);
                 boxes[i] = p;
                 MethodHandles.arrayElementSetter(vt.arrayValueClass()).invoke(arr, i, p);
            }
            data[n] = new Object[] { boxes, arr };
        }
        return data;
    }

    @Test(dataProvider = "createPoints")
    public void testMakePoint(Point p) throws Throwable {
        assertEquals(p, makePoint().invoke(p.x, p.y));
    }

    static MethodHandle makePoint() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        lookup = MethodHandles.privateLookupIn(Point.class, lookup);
        MethodHandle impl = vt.findWither(lookup, "y", int.class); // (QPoint,int)->QPoint
        impl = MethodHandles.collectArguments(impl, 0, vt.findWither(lookup, "x", int.class)); // (QPoint,int,int)->QPoint
        impl = MethodHandles.collectArguments(impl, 0, vt.defaultValueConstant()); // (int,int)->QPoint
        return impl;
    }

    @Test(dataProvider = "createPoints")
    public void testMakePoint_bytecode(Point p) throws Throwable {
        assertEquals(p, makePoint_bytecode().invoke(p.x, p.y));
    }

    static MethodHandle makePoint_bytecode() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandle mh = MethodHandleBuilder.loadCode(MethodHandles.privateLookupIn(vt.valueClass(), MethodHandles.lookup()),
                "makePoint", MethodType.methodType(vt.valueClass(), int.class, int.class),
                C -> {
                    C.vdefault(vt.valueClass())
                            .iload_0()
                            .vwithfield(vt.valueClass(), "x", "I")
                            .iload_1()
                            .vwithfield(vt.valueClass(), "y", "I")
                            .vreturn();
                });
        return mh;
    }

    @Test(dataProvider = "createPoints")
    public void testNorm(Point p) throws Throwable {
        assertEquals(p.norm(), norm().invoke(p));
    }

    static MethodHandle norm() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle id = vt.identity();
        MethodHandle norm = lookup.findVirtual(Point.class, "norm", MethodType.methodType(double.class));
        MethodHandle impl = MethodHandles.filterReturnValue(id, vt.box());
        impl = MethodHandles.filterReturnValue(impl, norm);
        return impl;
    }

    @Test(dataProvider = "createPoints")
    public void testNorm_bytecode(Point p) throws Throwable {
        assertEquals(p.norm(), norm_bytecode().invoke(p));
    }

    static MethodHandle norm_bytecode() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandle mh = MethodHandleBuilder.loadCode(MethodHandles.lookup(), "norm", MethodType.methodType(double.class, vt.valueClass()),
                C -> {
                    C.vload(0)
                            .vbox(Point.class)
                            .invokevirtual(Point.class, "norm", "()D", false)
                            .dreturn();
                });
        return mh;

    }

    @Test(dataProvider = "createPointArrays")
    public void testTotalNorm(Point[] boxes, Object arr) throws Throwable {
        assertTrue(Math.abs(totalNorm(boxes) - (double)totalNorm().invoke(arr)) < 0.0001d);
    }

    private double totalNorm(Point[] parr) {
        return Stream.of(parr).mapToDouble(Point::norm).sum();
    }

    private static MethodHandle totalNorm() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType loopLocals = MethodType.methodType(double.class, vt.arrayValueClass());
        MethodType loopParams = loopLocals.insertParameterTypes(0, double.class, int.class); // (int,double,T)->double
        MethodHandle init = MethodHandles.permuteArguments(MethodHandles.constant(double.class, 0.0), loopLocals); // (T)->double
        MethodHandle body = lookup.findStatic(Double.class, "sum", MethodType.methodType(double.class, double.class, double.class)); // (double,double)->double
        body = MethodHandles.collectArguments(body, 1, norm()); // (double,QPoint)->double
        body = MethodHandles.collectArguments(body, 1, vt.arrayGetter()); // (double,T,int)->double
        body = MethodHandles.permuteArguments(body, loopParams, 0, 2, 1); // (int,double,T)->double
        return MethodHandles.countedLoop(vt.arrayLength(), init, body);
    }

    @Test(dataProvider = "createPointArrays")
    public void testTotalNorm_bytecode(Point[] boxes, Object arr) throws Throwable {
        assertTrue(Math.abs(totalNorm(boxes) - (double)totalNorm_bytecode().invoke(arr)) < 0.0001d);
    }

    static MethodHandle totalNorm_bytecode() throws Throwable {
        ValueType vt = ValueType.forClass(Point.class);
        MethodHandle mh = MethodHandleBuilder.loadCode(MethodHandles.privateLookupIn(vt.valueClass(), MethodHandles.lookup()), "totalNorm", MethodType.methodType(double.class, vt.arrayValueClass()),
                C -> {
                    C.iconst_0()
                            .istore_1()
                            .dconst_0()
                            .dstore_2()
                            .label("loop")
                            .aload_0()
                            .arraylength()
                            .iload_1()
                            .ifcmp(TypeTag.I, CondKind.LE, "end")
                            .aload_0()
                            .iload_1()
                            .vaload()
                            .vbox(Point.class)
                            .invokevirtual(Point.class, "norm", "()D", false)
                            .dload_2()
                            .dadd()
                            .dstore_2()
                            .iinc(1, 1)
                            .goto_("loop")
                            .label("end")
                            .dload_2()
                            .dreturn();
                });
        return mh;
    }

    public void guardWithTest() throws Throwable {
        MethodHandle point1 = MethodHandles.insertArguments(makePoint(), 0, 1, 1);
        MethodHandle point2 = MethodHandles.insertArguments(makePoint(), 0, 2, 2);
        MethodHandle predicate_T = MethodHandles.constant(boolean.class, true);
        MethodHandle predicate_F = MethodHandles.constant(boolean.class, false);
        MethodHandle gwt_T = MethodHandles.guardWithTest(predicate_T, point1, point2);
        MethodHandle gwt_F = MethodHandles.guardWithTest(predicate_F, point1, point2);
        assertEquals(gwt_T.invoke(), new Point(1, 1));
        assertEquals(gwt_F.invoke(), new Point(2, 2));
    }
}
