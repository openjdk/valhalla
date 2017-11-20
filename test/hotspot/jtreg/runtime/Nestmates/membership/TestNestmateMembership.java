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
 *
 * @compile TargetNoHost.jcod
 *          CallerNoHost.jcod
 *          TargetMissingHost.jcod
 *          CallerMissingHost.jcod
 *          CallerNotInstanceHost.jcod
 *          TargetNotInstanceHost.jcod
 *          CallerNotOurHost.jcod
 *          TargetNotOurHost.jcod
 *          PackagedNestHost.jcod
 *          PackagedNestHost2Member.jcod
 *          PackagedNestHostMember.jcod
 *
 * @run main/othervm TestNestmateMembership
 */

// We test all the "illegal" relationships between a nest member and its nest-host
// except for the case where the name of the nest-member matches the name listed
// in the nest-host, but resolves to a different class. There doesn't seem to
// be a way to construct that scenario.
// For each nested class below there is a corresponding .jcod file which breaks one
// of the rules regarding nest membership. For the package related tests we have
// additional PackageNestHost*.java sources.
// Note that all the .java files must be compiled in the same step, while all
// .jcod files must be compiled in a later step.

// As access checking requires resolution and validation of the nest-host of
// both the caller class and the target class, we must check that all
// combinations of good/bad caller/target are checked for each of the
// possible errors:
// - no nest-host attribute
// - nest-host class can not be found
// - nest-host class is not an instance class
// - class is not a member of nest-host's nest
// - class and nest-host are in different packages

public class TestNestmateMembership {

    static class Caller {
        private static void m() {
            System.out.println("Caller.m()");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNoHost() {
            TargetNoHost.m();
        }
        public static void invokeTargetMissingHost() {
            TargetMissingHost.m();
        }
        public static void invokeTargetNotInstanceHost() {
            TargetNotInstanceHost.m();
        }
        public static void invokeTargetNotOurHost() {
            TargetNotOurHost.m();
        }
    }

    static class CallerNoHost {
        private static void m() {
            System.out.println("CallerNoHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNoHost() {
            TargetNoHost.m();
        }
    }

    static class CallerMissingHost {
        String msg = "NoCallerMissingHost"; // for cp entry
        private static void m() {
            System.out.println("CallerMissingHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetMissingHost() {
            TargetMissingHost.m();
        }
    }

    static class CallerNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change
        private static void m() {
            System.out.println("CallerNotInstanceHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNotInstanceHost() {
            TargetNotInstanceHost.m();
        }
    }

    static class CallerNotOurHost {
        private static void m() {
            System.out.println("CallerNotOurHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNotOurHost() {
            TargetNotOurHost.m();
        }
    }

    static class Target {
        private static void m() {
            System.out.println("Target.m()");
        }
    }

    static class TargetNoHost {
        private static void m() {
            System.out.println("TargetNoHost.m() - java version");
        }
    }

    static class TargetMissingHost {
        String msg = "NoTargetMissingHost";  // for cp entry
        private static void m() {
            System.out.println("TargetMissingHost.m() - java version");
        }
    }

    static class TargetNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change
        private static void m() {
            System.out.println("TargetNotInstanceHost.m() - java version");
        }
    }

    static class TargetNotOurHost {
        private static void m() {
            System.out.println("TargetNotOurHost.m() - java version");
        }
    }

    public static void main(String[] args) throws Throwable {
        test_GoodCalls();
        test_NoHost();
        test_MissingHost();
        test_NotInstanceHost();
        test_NotOurHost();
        test_WrongPackageHost();
    }

    static void test_GoodCalls(){
        try {
            Caller.invokeTarget();
        }
        catch (Exception e) {
            throw new Error("Unexpected exception on good calls: " + e);
        }
    }

    static void test_NoHost() throws Throwable {
        System.out.println("Testing for missing nest-host attribute");
        String msg = "tried to access method " +
            "TestNestmateMembership$TargetNoHost.m()V from class " +
            "TestNestmateMembership$Caller";
        try {
            Caller.invokeTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "tried to access method TestNestmateMembership$Target.m()V" +
            " from class TestNestmateMembership$CallerNoHost";
        try {
            CallerNoHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "tried to access method TestNestmateMembership$TargetNoHost.m()V" +
            " from class TestNestmateMembership$CallerNoHost";
        try {
            CallerNoHost.invokeTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_MissingHost() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "Unable to load nest-host class (NoTargetMissingHost) of " +
            "TestNestmateMembership$TargetMissingHost";
        String cause_msg = "NoTargetMissingHost";
        try {
            Caller.invokeTargetMissingHost();
            throw new Error("Missing NoClassDefFoundError: " + msg);
        }
        catch (NoClassDefFoundError expected) {
            check_expected(expected, msg);
            Throwable cause = expected.getCause();
            if (cause instanceof NoClassDefFoundError) {
                check_expected(cause, cause_msg);
            }
            else throw new Error("Unexpected NoClassDefFoundError", expected);
        }
        msg = "Unable to load nest-host class (NoCallerMissingHost) of " +
            "TestNestmateMembership$CallerMissingHost";
        cause_msg = "NoCallerMissingHost";
        try {
            CallerMissingHost.invokeTarget();
            throw new Error("Missing NoClassDefFoundError: " + msg);
        }
        catch (NoClassDefFoundError expected) {
            check_expected(expected, msg);
            Throwable cause = expected.getCause();
            if (cause instanceof NoClassDefFoundError) {
                check_expected(cause, cause_msg);
            }
            else throw new Error("Unexpected NoClassDefFoundError", expected);
        }
        msg = "Unable to load nest-host class (NoCallerMissingHost) of "+
            "TestNestmateMembership$CallerMissingHost";
        cause_msg = "NoCallerMissingHost";
        try {
            CallerMissingHost.invokeTargetMissingHost();
            throw new Error("Missing NoClassDefFoundError: " + msg);
        }
        catch (NoClassDefFoundError expected) {
            check_expected(expected, msg);
            Throwable cause = expected.getCause();
            if (cause instanceof NoClassDefFoundError) {
                check_expected(cause, cause_msg);
            }
            else throw new Error("Unexpected NoClassDefFoundError", expected);
        }
    }

    static void test_NotInstanceHost() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "Type TestNestmateMembership$TargetNotInstanceHost is not a "+
            "nest member of [Ljava.lang.Object;: nest-host is not an instance class";
        try {
            Caller.invokeTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotInstanceHost is not a "+
            "nest member of [Ljava.lang.Object;: nest-host is not an instance class";
        try {
            CallerNotInstanceHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotInstanceHost is not a "+
            "nest member of [Ljava.lang.Object;: nest-host is not an instance class";
        try {
            CallerNotInstanceHost.invokeTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotOurHost() throws Throwable {
        System.out.println("Testing for nest-host class that does not list us in its nest");
        String msg = "Type TestNestmateMembership$TargetNotOurHost is not a nest member" +
            " of java.lang.Object: current type is not listed as a nest member";
        try {
            Caller.invokeTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotOurHost is not a nest member" +
            " of java.lang.Object: current type is not listed as a nest member";
        try {
            CallerNotOurHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotOurHost is not a nest member" +
            " of java.lang.Object: current type is not listed as a nest member";
        try {
            CallerNotOurHost.invokeTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_WrongPackageHost() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Type P2.PackagedNestHost2$Member is not a nest member of " +
            "P1.PackagedNestHost: types are in different packages";
        try {
            P1.PackagedNestHost.doAccess();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            P2.PackagedNestHost2.Member.doAccess();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void check_expected(Throwable expected, String msg) {
        if (!expected.getMessage().contains(msg))
            throw new Error("Wrong " + expected.getClass().getSimpleName() +": \"" +
                            expected.getMessage() + "\" does not contain \"" +
                            msg + "\"");
        System.out.println("OK - got expected exception: " + expected);
    }
}
