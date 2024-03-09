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
 *
 */
/*
 * @ignore
 * @test PrimitiveUsers
 * @summary test that if a class file uses primitive classes, -XX:+EnablePrimitiveClasses must be set.
 * @compile PrimitiveUsers.jcod
 * @run main/othervm -XX:+EnableValhalla -XX:+EnablePrimitiveClasses PrimitiveUsers true
 * @run main/othervm -XX:+EnableValhalla -XX:-EnablePrimitiveClasses PrimitiveUsers false
 */

public class PrimitiveUsers {

    static final String[][] TEST_CASE_ERROR = {
        { "PrimitiveUsersField",         "Field \"p\" in class PrimitiveUsersField has illegal signature \"QMyPrimitive;\"" },
        { "PrimitiveUsersStaticQArg",    "Class name contains illegal Q-signature in descriptor in class file PrimitiveUsersStaticQArg, requires option -XX:+EnablePrimitiveClasses" },
        { "PrimitiveUsersStaticQReturn", "Class name contains illegal Q-signature in descriptor in class file PrimitiveUsersStaticQReturn, requires option -XX:+EnablePrimitiveClasses" },
        { "PrimitiveUsersQArg",    "Class name contains illegal Q-signature in descriptor in class file PrimitiveUsersQArg, requires option -XX:+EnablePrimitiveClasses" },
        { "PrimitiveUsersQReturn", "Class name contains illegal Q-signature in descriptor in class file PrimitiveUsersQReturn, requires option -XX:+EnablePrimitiveClasses" }
    };

    public static void testLoadCasesEnabled() throws Throwable {
        for (String[] caseAndError : TEST_CASE_ERROR) {
            Class.forName(caseAndError[0]);
        }
    }

    public static void testLoadCasesDisabled() throws Throwable {
        for (String[] caseAndError : TEST_CASE_ERROR) {
            try {
                Class.forName(caseAndError[0]);
                throw new RuntimeException("Test case " + caseAndError[0] + " loaded with out failure");
            } catch (Throwable t) {
                if (!(t instanceof ClassFormatError)) {
                    t.printStackTrace();
                    throw t;
                }
                if (!t.getMessage().equals(caseAndError[1])) {
                    t.printStackTrace();
                    throw new RuntimeException("Wrong CFE error: " +  t.getMessage() + " expected: " + caseAndError[1]);
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args[0].equals("true")) {
            testLoadCasesEnabled();
        } else {
            // Test correct error message for disabled primitive class feature
            testLoadCasesDisabled();
        }
    }
}
