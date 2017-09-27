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

// We compile two versions of the nested StaticIface which contains a call
// to a private interface method. Both versions are compiled to use
// invokeinterface, but one is modified to declare a lower classfile version,
// which makes the use of invokeinterface illegal in that context.
// We run the test twice, each time using a different version of the
// StaticIface class.

/*
 * @test
 * @bug 8046171
 * @summary Test use of invokeinterface versus invokespecial for private
 *          interface method invocation
 * @compile TestInvokeInterface.java
 * @compile StaticIfaceGood.jcod
 * @run main TestInvokeInterface pass
 * @compile StaticIfaceError.jcod
 * @run main TestInvokeInterface fail
 */

public class TestInvokeInterface {

    static interface StaticIface {

        private void priv_invoke() {
            System.out.println("StaticIface::priv_invoke");
        }

        default void access_priv(StaticIface o) {
            o.priv_invoke();
        }
    }

    public static void main(String[] args) {
        boolean shouldFail = args[0].equals("fail");
        String icce = "IncompatibleClassChangeError: private interface method requires invokespecial";
        StaticIface intf = new StaticIface() {};
        try {
            intf.access_priv(new StaticIface(){});
            if (shouldFail)
                throw new Error("Do not get expected exception: " + icce);
            else
                System.out.println("Invocation succeeded as expected");
        }
        catch (IncompatibleClassChangeError e) {
            if (shouldFail && e.toString().contains(icce))
                System.out.println("Got expected exception: " + e);
            else
                throw new Error("Unexpected cause of exception: " + e);
        }
    }
}
