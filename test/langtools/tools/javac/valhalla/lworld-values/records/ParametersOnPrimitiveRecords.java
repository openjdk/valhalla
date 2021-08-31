/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary [lworld] test for equal treatment of primitive records from reflection
 * @bug 8273202
 * @run main ParametersOnPrimitiveRecords
 */
import java.lang.reflect.Parameter;

public class ParametersOnPrimitiveRecords {

    public record Simple(int i, String s) {
    }

    public primitive record PrimitiveSimple(int i, String s) {
    }

    public static void main(String[] args) throws Throwable {
        checkSimpleRecordClass(Simple.class);
        checkSimpleRecordClass(PrimitiveSimple.class);
    }

    private static void checkSimpleRecordClass(Class<? extends Record> recordClass) throws Throwable {
        // Test that a class can be introspected and constructed.
        // If it's a primitive class, it will be wrapped in a .ref flavour implicitly.
        var r1 = checkAndConstructRecord(recordClass);
        var r2 = checkAndConstructRecord(recordClass);
        if (! r1.equals(r2)) {
            throw new AssertionError(recordClass.getCanonicalName() + ": " + r1 + " should be equal to " + r2);
        }
    }

    private static Object checkAndConstructRecord(Class<? extends Record> recordClass) throws Throwable {
        var className = recordClass.getCanonicalName();
        if (recordClass.getConstructors().length != 1) {
            throw new AssertionError(className
                    + ": Expected 1 constructor, got " + recordClass.getConstructors().length);
        }
        var ctor = recordClass.getConstructors()[0];
        var parameters = ctor.getParameters();
        if (parameters.length != 2) {
            throw new AssertionError(className + ": Expected 2 parameters on <init>");
        }
        checkParamName(className, parameters, 0, "i");
        checkParamName(className, parameters, 1, "s");
        return ctor.newInstance(123, "One two three");
    }

    private static void checkParamName(String className, Parameter[] parameters, int position, String name) throws AssertionError {
        if (! parameters[0].getName().equals("i")) {
            throw new AssertionError(
                    "%s: Parameter %d should be '%s' but was '%s'".formatted(
                            className,
                            position,
                            name,
                            parameters[position].getName()));
        }
    }
}
