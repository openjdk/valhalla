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

package compiler.valhalla.inlinetypes;

import compiler.lib.ir_framework.*;
import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

/*
 * @test
 * @key randomness
 * @summary Test intrinsic support for value classes.
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      compiler.valhalla.inlinetypes.TestCompareAndExchange
 */

/*
 * My command line: java -cp /home/chagedor/JTwork/classes/compiler/valhalla/inlinetypes/TestCompareAndExchange.d:/home/chagedor/valhalla3/open/test/hotspot/jtreg/compiler/valhalla/inlinetypes:/home/chagedor/JTwork/classes/compiler/valhalla/inlinetypes/TestCompareAndExchange.d/test/lib:/home/chagedor/valhalla3/open/test/lib:/home/chagedor/valhalla3/open/test/hotspot/jtreg:/home/chagedor/jtreg/lib/javatest.jar:/home/chagedor/jtreg/lib/jtreg.jar -Djava.library.path=. -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Dir.framework.server.port=35391 --enable-preview --add-exports java.base/jdk.internal.value=ALL-UNNAMED --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED -XX:-BackgroundCompilation -DWarmup=10000 -XX:CompileCommand=dontinline,*Unsafe*::getFlatValue* -XX:CompileCommand=dontinline,*Unsafe*::putFlatValue* -DIgnoreCompilerControls=true -XX:-TieredCompilation -XX:CompileOnly=*Small*::*,*::test70 -XX:CompileCommand=dontinline,*Unsafe::array* -XX:DisableIntrinsic=_compareAndSetLong,_compareAndSetInt,_compareAndSetByte,_compareAndSetShort -DReproduce=true compiler.lib.ir_framework.test.TestVM compiler.valhalla.inlinetypes.TestCompareAndExchange
 */
@ForceCompileClassInitializer
public class TestCompareAndExchange {


    public static void main(String[] args) {
        InlineTypes.getFramework()
                .addFlags("--enable-preview",
                          "--add-exports", "java.base/jdk.internal.value=ALL-UNNAMED",
                          "--add-exports", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED",
                          "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
                .addFlags("-XX:-BackgroundCompilation -DWarmup=10000 -XX:CompileCommand=dontinline,*Unsafe*::getFlatValue* -XX:CompileCommand=dontinline,*Unsafe*::putFlatValue* -DIgnoreCompilerControls=true -XX:-TieredCompilation -XX:CompileOnly=*Small*::*,*::test70 -XX:CompileCommand=dontinline,*Unsafe::array* -XX:DisableIntrinsic=_compareAndSetLong,_compareAndSetInt,_compareAndSetByte,_compareAndSetShort -DReproduce=true".split(" "))
                .start();
    }

    private static final Unsafe U = Unsafe.getUnsafe();

    static public value class SmallValue {
        byte a = (byte)0x12;
        byte b = (byte)0x34;
        byte c = (byte)0x56;
        byte d = (byte)0x78;
        byte e = (byte)0x9a;

        @ForceInline
        static SmallValue create() {
            return new SmallValue();
        }

        @Override
        public String toString() {
            return "a: " + a + ", b: " + b;
        }
    }

    SmallValue test63_vt;
    private static final long TEST63_VT_OFFSET;
    static {
        try {
            Field test63_vt_Field = TestCompareAndExchange.class.getDeclaredField("test63_vt");
            TEST63_VT_OFFSET = U.objectFieldOffset(test63_vt_Field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // compareAndExchange to flattened field in object, non-inline arguments to compare and set
    @Test
    public Object test70(Object expected, Object x) {
        return U.compareAndExchangeFlatValue(this, TEST63_VT_OFFSET, 4, SmallValue.class, expected, x);
    }

    @Run(test = "test70")
    public void test70_verifier() {
        test63_vt = SmallValue.create();
        test70(SmallValue.create(), SmallValue.create());
    }
}