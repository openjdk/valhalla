/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @enablePreview
 * @library ../ /test/lib
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestCaptureCallState
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.testng.Assert.assertEquals;

public class TestCaptureCallState extends NativeTestHelper {

    static {
        System.loadLibrary("CaptureCallState");
        if (IS_WINDOWS) {
            String system32 = System.getenv("SystemRoot") + "\\system32";
            System.load(system32 + "\\Kernel32.dll");
            System.load(system32 + "\\Ws2_32.dll");
        }
    }

    private record SaveValuesCase(String nativeTarget, FunctionDescriptor nativeDesc, String threadLocalName, Consumer<Object> resultCheck) {}

    @Test(dataProvider = "cases")
    public void testSavedThreadLocal(SaveValuesCase testCase) throws Throwable {
        Linker.Option.CaptureCallState stl = Linker.Option.captureCallState(testCase.threadLocalName());
        MethodHandle handle = downcallHandle(testCase.nativeTarget(), testCase.nativeDesc(), stl);

        VarHandle errnoHandle = stl.layout().varHandle(groupElement(testCase.threadLocalName()));

        try (Arena arena = Arena.openConfined()) {
            MemorySegment saveSeg = arena.allocate(stl.layout());
            int testValue = 42;
            boolean needsAllocator = testCase.nativeDesc().returnLayout().map(StructLayout.class::isInstance).orElse(false);
            Object result = needsAllocator
                ? handle.invoke(arena, saveSeg, testValue)
                : handle.invoke(saveSeg, testValue);
            testCase.resultCheck().accept(result);
            int savedErrno = (int) errnoHandle.get(saveSeg);
            assertEquals(savedErrno, testValue);
        }
    }

    @DataProvider
    public static Object[][] cases() {
        List<SaveValuesCase> cases = new ArrayList<>();

        cases.add(new SaveValuesCase("set_errno_V", FunctionDescriptor.ofVoid(JAVA_INT), "errno", o -> {}));
        cases.add(new SaveValuesCase("set_errno_I", FunctionDescriptor.of(JAVA_INT, JAVA_INT), "errno", o -> assertEquals((int) o, 42)));
        cases.add(new SaveValuesCase("set_errno_D", FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT), "errno", o -> assertEquals((double) o, 42.0)));

        cases.add(structCase("SL",  Map.of(JAVA_LONG.withName("x"), 42L)));
        cases.add(structCase("SLL", Map.of(JAVA_LONG.withName("x"), 42L,
                                           JAVA_LONG.withName("y"), 42L)));
        cases.add(structCase("SLLL", Map.of(JAVA_LONG.withName("x"), 42L,
                                            JAVA_LONG.withName("y"), 42L,
                                            JAVA_LONG.withName("z"), 42L)));
        cases.add(structCase("SD",  Map.of(JAVA_DOUBLE.withName("x"), 42D)));
        cases.add(structCase("SDD", Map.of(JAVA_DOUBLE.withName("x"), 42D,
                                           JAVA_DOUBLE.withName("y"), 42D)));
        cases.add(structCase("SDDD", Map.of(JAVA_DOUBLE.withName("x"), 42D,
                                            JAVA_DOUBLE.withName("y"), 42D,
                                            JAVA_DOUBLE.withName("z"), 42D)));

        if (IS_WINDOWS) {
            cases.add(new SaveValuesCase("SetLastError", FunctionDescriptor.ofVoid(JAVA_INT), "GetLastError", o -> {}));
            cases.add(new SaveValuesCase("WSASetLastError", FunctionDescriptor.ofVoid(JAVA_INT), "WSAGetLastError", o -> {}));
        }

        return cases.stream().map(tc -> new Object[] {tc}).toArray(Object[][]::new);
    }

    static SaveValuesCase structCase(String name, Map<MemoryLayout, Object> fields) {
        StructLayout layout = MemoryLayout.structLayout(fields.keySet().toArray(MemoryLayout[]::new));

        Consumer<Object> check = o -> {};
        for (var field : fields.entrySet()) {
            MemoryLayout fieldLayout = field.getKey();
            VarHandle fieldHandle = layout.varHandle(MemoryLayout.PathElement.groupElement(fieldLayout.name().get()));
            Object value = field.getValue();
            check = check.andThen(o -> assertEquals(fieldHandle.get(o), value));
        }

        return new SaveValuesCase("set_errno_" + name, FunctionDescriptor.of(layout, JAVA_INT), "errno", check);
    }

}

