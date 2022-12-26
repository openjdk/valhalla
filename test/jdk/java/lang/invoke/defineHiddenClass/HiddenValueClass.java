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
 * @library /test/lib
 * @compile ValueImpl.jcod
 * @run testng/othervm HiddenValueClass
 * @summary a hidden value class can only be defined without "<vnew>" static
 *          factory method.  It will have to provide a different instance creation.
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodType.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Utils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class HiddenValueClass {

    value class Impl implements Runnable {
        public void run() {}
    }

    private static final Path CLASSES_DIR = Paths.get(Utils.TEST_CLASSES);

    /*
     * ClassFormatError when defining HiddenValueClass$Impl as a hidden class
     * as it defines "<vnew>()LHiddenValueClass$Impl;" which is illegal.
     *
     * A hidden class cannot have "<vnew>" factory method since it cannot
     * be named.
     */
    @Test
    public void illegalHiddenValueClass() throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("HiddenValueClass$Impl.class"));
        assertThrows(ClassFormatError.class, () -> MethodHandles.lookup().defineHiddenClass(bytes, false));
    }

    /*
     * ValueImpl is a value class without "<vnew>" method but instead
     * a special factory method named "$make" (no bracket) with
     * "()java/lang/Runnable;" descriptor defined.
     */
    @Test
    public void hiddenValueClassWithMakeFactory() throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("ValueImpl.class"));
        Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false);
        MethodHandle mh = lookup.findStatic(lookup.lookupClass(), "$make", methodType(Runnable.class));
        Runnable r = (Runnable)mh.invokeExact();
        r.run();
    }
}
