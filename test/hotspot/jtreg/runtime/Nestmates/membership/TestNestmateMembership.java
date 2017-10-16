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

/*
 * @test
 * @bug 8046171
 * @summary Test the various rules for nest members and nest-hosts
 * @compile TestNestmateMembership.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 *          NotAMember2.java
 * @compile MissingNestHost.jcod
 *          ArrayNestHost.jcod
 *          NotAMember.jcod
 *          NotAMember2.jcod
 *          PackagedNestHost.jcod
 *          PackagedNestHost2Member.jcod
 * @run main/othervm TestNestmateMembership
 */

// We test all the "illegal" relationships between a nest member and its nest-host
// except for the case where the name of the nest-member matches the name listed
// in the nest-host, but resolves to a different class. There doesn't seem to
// be a way to construct that scenario.
// For each nested class below there is a corresponding .jcod file which breaks one
// of the rules regarding nest membership. For the package related tests we have
// additional PackageNestHost*.java sources. We also have NotAMember2.java.
// Note that all the .java files must be compiled in the same step, while all
// .jcod files must be compiled in a later step.

public class TestNestmateMembership {

    // jcod modified to have non-existent nest-host
    static class MissingNestHost {
        private static void m() {
            System.out.println("MissingNestHost.m() - java version");
        }
    }

    // jcod modified to have non-instance class Object[] as nest-host
    static class ArrayNestHost {
        Object[] oa; // create CP entry
        private static void m() {
            System.out.println("ArrayNestHost.m() - java version");
        }
    }

    // jcod modified to have Object as nest-host, which has no nest-members
    static class NotAMember {
        private static void m() {
            System.out.println("NotAMember.m() - java version");
        }
    }

    public static void main(String[] args) throws Throwable {
        test_MissingNestHost();
        test_ArrayNestHost();
        test_WrongPackageForNestMember();
        test_NotAMember();
        test_NotAMember2();
    }

    static void test_WrongPackageForNestMember() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Class P2.PackagedNestHost2$Member is in a different" +
                     " package to its nest-host class P1.PackagedNestHost";
        try {
            P1.PackagedNestHost.doAccess();
            throw new Error("Missing IncompatibleClassChangeError: " + msg);
        }
        catch (IncompatibleClassChangeError expected) {
            if (!expected.getMessage().contains(msg))
                throw new Error("Wrong IncompatibleClassChangeError: \"" +
                                expected.getMessage() + "\" does not contain \"" +
                                msg + "\"");
            System.out.println("OK - got expected exception: " + expected);
        }
    }

    static void test_MissingNestHost() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "NoSuchClass";
        try {
            MissingNestHost.m();
            throw new Error("Missing NoClassDefFoundError: " + msg);
        }
        catch (NoClassDefFoundError expected) {
            if (!expected.getMessage().contains(msg))
                throw new Error("Wrong NoClassDefFoundError: \"" +
                                expected.getMessage() + "\" does not contain \"" +
                                msg + "\"");
            System.out.println("OK - got expected exception: " + expected);
        }
    }

    static void test_ArrayNestHost() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "ArrayNestHost has non-instance class [Ljava.lang.Object; as nest-host";
        try {
            ArrayNestHost.m();
            throw new Error("Missing IncompatibleClassChangeError: " + msg);
        }
        catch (IncompatibleClassChangeError expected) {
            if (!expected.getMessage().contains(msg))
                throw new Error("Wrong IncompatibleClassChangeError: \"" +
                                expected.getMessage() + "\" does not contain \"" +
                                msg + "\"");
            System.out.println("OK - got expected exception: " + expected);
        }
    }

    static void test_NotAMember() throws Throwable {
        System.out.println("Testing for nest-host class that has no nest");
        String msg = "NotAMember is not a nest member of java.lang.Object";
        try {
            NotAMember.m();
            throw new Error("Missing IncompatibleClassChangeError: " + msg);
        }
        catch (IncompatibleClassChangeError expected) {
            if (!expected.getMessage().contains(msg))
                throw new Error("Wrong IncompatibleClassChangeError: \"" +
                                expected.getMessage() + "\" does not contain \"" +
                                msg + "\"");
            System.out.println("OK - got expected exception: " + expected);
        }
    }

    static void test_NotAMember2() throws Throwable {
        System.out.println("Testing for nest-host class that doesn't list this class as a member");
        String msg = "NotAMember2$Member is not a nest member of TestNestmateMembership";
        try {
            NotAMember2.Member.m();
            throw new Error("Missing IncompatibleClassChangeError: " + msg);
        }
        catch (IncompatibleClassChangeError expected) {
            if (!expected.getMessage().contains(msg))
                throw new Error("Wrong IncompatibleClassChangeError: \"" +
                                expected.getMessage() + "\" does not contain \"" +
                                msg + "\"");
            System.out.println("OK - got expected exception: " + expected);
        }
    }
}
