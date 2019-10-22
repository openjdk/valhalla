/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test interacting with inline types through jdb
 *
 * @library /test/lib
 * @compile -g JdbInlineTypesTest.java
 * @run main/othervm JdbInlineTypesTest
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;

class JdbInlineTypesTestTarg {
    static MyValue static_v = new MyValue(16,'s', (byte)3, (byte)(byte)9);
    
    static inline class SmallValue {
        byte b0,b1;

        public SmallValue(byte b0, byte b1) {
            this.b0 = b0;
            this.b1 = b1;
        }
    }
    
    static inline class MyValue {
        int a;
        char b;
        SmallValue small;

        public MyValue(int a, char b, byte b0, byte b1) {
            this.a = a;
            this.b = b;
            this.small = new SmallValue(b0, b1);
        }
    }

    public static class ObjectContainer {
        int i;
        MyValue value;
    }
    
    public static void bkpt() {
	MyValue v = new MyValue(12,'c', (byte)5, (byte)(byte)7);
        Object b = new Object();
        b = v;
	Object o = new Object();
        MyValue v2 = new MyValue(12,'c', (byte)5, (byte)7);
        MyValue v3 = new MyValue(11,'c', (byte)5, (byte)7);
        ObjectContainer oc = new ObjectContainer();
	MyValue[] array = new MyValue[10];
        System.out.println("v == v " + (b == b));
        int i = 0;     //@1 breakpoint
    }

    public static void main(String[] args) {
        bkpt();
    }
}

public class JdbInlineTypesTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbInlineTypesTest().run();
    }

    private JdbInlineTypesTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbInlineTypesTestTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("JdbInlineTypesTest.java", 1);
        // Run to breakpoint #1
        execCommand(JdbCommand.run())
                .shouldContain("Breakpoint hit");

	// Printing a local variable containing an instance of an inline type
	execCommand(JdbCommand.print("v"))
                .shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Printing an instance of an inline type stored in a local variable of type Object
	execCommand(JdbCommand.print("b"))
                .shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Trying to set a local variable containing an instance of an inline types to null
	execCommand(JdbCommand.set("v", "null")).shouldContain("Can't set an inline type to null");

	// Storing an instance of an inline type into a local variable of type Object
	execCommand(JdbCommand.set("o", "v")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Printing a field of an instance of an inline type
	execCommand(JdbCommand.print("v.a")).shouldContain(" = 12");

	// Print a flattened field of an instance of an inline type
	execCommand(JdbCommand.print("v.small")).shouldContain(" = \"[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]\"");

	// Print a flattened field of an instance of an identity class
	// Note field b has a not printible value (character 0);
	execCommand(JdbCommand.print("oc.value")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=0 b=\u0000 small=[JdbInlineTypesTestTarg$SmallValue b0=0 b1=0]]\"");

	// Updating a flattened field of an instance of an identity class
	execCommand(JdbCommand.set("oc.value", "v")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Trying set a flattened field to null
	execCommand(JdbCommand.set("oc.value", "null")).shouldContain("Can't set an inline type to null");

	// Print a static inline field
	execCommand(JdbCommand.print("JdbInlineTypesTestTarg.static_v"))
                .shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=16 b=s small=[JdbInlineTypesTestTarg$SmallValue b0=3 b1=9]]\"");

	// Updating a static inline field
	execCommand(JdbCommand.set("JdbInlineTypesTestTarg.static_v", "v")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Trying set a inline field to null
	execCommand(JdbCommand.set("JdbInlineTypesTestTarg.static_v", "null")).shouldContain("Can't set an inline type to null");
	
	// Printing an element of an inline type array
	execCommand(JdbCommand.print("array[0]")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=0 b=\u0000 small=[JdbInlineTypesTestTarg$SmallValue b0=0 b1=0]]\"");

	// Setting an element of an inline type array
	execCommand(JdbCommand.set("array[0]", "v")).shouldContain(" = \"[JdbInlineTypesTestTarg$MyValue a=12 b=c small=[JdbInlineTypesTestTarg$SmallValue b0=5 b1=7]]\"");

	// Trying to set an element of an inline type array to null
	execCommand(JdbCommand.set("array[1]", "null")).shouldContain("Can't set an inline type to null");

	// Testing substitutability test 
	execCommand(JdbCommand.print("v == v2")).shouldContain(" = true");
	execCommand(JdbCommand.print("v == v3")).shouldContain(" = false");
    }
}
