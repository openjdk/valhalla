/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.foreigntest;

import java.lang.foreign.*;
import java.lang.foreign.Arena;
import java.lang.invoke.*;

public class PanamaMainInvoke {
    public static void main(String[] args) throws Throwable {
       testInvokenativeLinker();
       testInvokeMemorySegment();
    }

    public static void testInvokenativeLinker() throws Throwable {
        Linker linker = Linker.nativeLinker();
        System.out.println("Trying to obtain a downcall handle");
        var mh = MethodHandles.lookup().findVirtual(Linker.class, "downcallHandle",
                MethodType.methodType(MethodHandle.class, FunctionDescriptor.class, Linker.Option[].class));
        var handle = (MethodHandle)mh.invokeExact(linker, FunctionDescriptor.ofVoid(), new Linker.Option[0]);
        System.out.println("Got downcall handle");
    }

    public static void testInvokeMemorySegment() throws Throwable {
        System.out.println("Trying to get MemorySegment");
        var mh = MethodHandles.lookup().findVirtual(MemorySegment.class, "reinterpret",
                MethodType.methodType(MemorySegment.class, long.class));
        var seg = (MemorySegment)mh.invokeExact(MemorySegment.NULL, 10L);
        System.out.println("Got MemorySegment");
    }
}
