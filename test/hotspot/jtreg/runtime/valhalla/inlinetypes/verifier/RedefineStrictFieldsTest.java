/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Redefinition tests that larval_frame stackmaps are adjusted and other tests that I might add.
 * @enablePreview
 * @library /test/lib
 * @run main RedefineClassHelper
 * @modules java.base/jdk.internal.vm.annotation
 * @compile -XDgenerateEarlyLarvalFrame -XDnoLocalProxyVars StrictFieldsOld.java StrictFieldsNew.java
 * @run main/othervm -Xverify:remote -javaagent:redefineagent.jar RedefineStrictFieldsTest
 */

import java.io.InputStream;
import static jdk.test.lib.Asserts.assertTrue;

public class RedefineStrictFieldsTest {

    // All of this should be moved to RedefineClassHelper
    private static byte[] getBytecodes(Class<?> thisClass, String name) throws Exception {
        InputStream is = thisClass.getClassLoader().getResourceAsStream(name + ".class");
        byte[] buf = is.readAllBytes();
        System.out.println("sizeof(" + name + ".class) == " + buf.length);
        return buf;
    }

    private static int getStringIndex(String needle, byte[] buf) {
        return getStringIndex(needle, buf, 0);
    }

    private static int getStringIndex(String needle, byte[] buf, int offset) {
        outer:
        for (int i = offset; i < buf.length - offset - needle.length(); i++) {
            for (int j = 0; j < needle.length(); j++) {
                if (buf[i + j] != (byte)needle.charAt(j)) continue outer;
            }
            return i;
        }
        return 0;
    }

    private static void replaceString(byte[] buf, String name, int index) {
        for (int i = index; i < index + name.length(); i++) {
            buf[i] = (byte)name.charAt(i - index);
        }
    }

    private static byte[] replaceAllStrings(String oldString, String newString) throws Exception {
        byte [] buf = getBytecodes(RedefineStrictFieldsTest.class, oldString);
        assertTrue(oldString.length() == newString.length(), "must have same length");
        int index = -1;
        while ((index = getStringIndex(oldString, buf, index + 1)) != 0) {
            replaceString(buf, newString, index);
        }
        return buf;
    }

    public static void main(java.lang.String[] unused) throws Exception {

        StrictFieldsOld old = new StrictFieldsOld(true, false);
        old.foo();

        // Rename class "StrictFieldsNew" to "StrictFieldsOld" so we can redefine it.
        byte [] buf = replaceAllStrings("StrictFieldsNew", "StrictFieldsOld");
        // Now redine the original version.
        // If the stackmaps aren't rewritten to point to new constant pool indices, this should get a VerifyError
        RedefineClassHelper.redefineClass(StrictFieldsOld.class, buf);
        StrictFieldsOld new_old = new StrictFieldsOld(true, false);
        new_old.foo();  // should call the new foo
    }
}
