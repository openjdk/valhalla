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
 * @summary Test incorrect use of Nestmate related attributes
 * @compile TwoMemberOfNest.jcod
 *          TwoNestMembers.jcod
 *          ConflictingAttributesInNestTop.jcod
 *          ConflictingAttributesInNestMember.jcod
 *          BadNestMembersLength.jcod
 *          BadNestMembersEntry.jcod
 *          DuplicateNestMemberEntry.jcod
 *          BadNestTop.jcod
 * @run main TestNestmateAttributes
 */

public class TestNestmateAttributes {
    public static void main(String args[]) throws Throwable {
        String[] badClasses = new String[] {
            "NestmateAttributeHolder$TwoMemberOfNest",
            "NestmateAttributeHolder",
            "ConflictingAttributesInNestTop",
            "NestmateAttributeHolder$ConflictingAttributesInNestMember",
            "BadNestMembersLength",
            "BadNestMembersEntry",
            "DuplicateNestMemberEntry",
            "NestmateAttributeHolder$BadNestTop",
        };

        String[] messages = new String[] {
            "Multiple MemberOfNest attributes in class file",
            "Multiple NestMembers attributes in class file",
            "Conflicting NestMembers and MemberOfNest attributes",
            "Conflicting MemberOfNest and NestMembers attributes",
            "Wrong NestMembers attribute length", 
            "Nest member class_info_index 9 has bad constant type",
            "Duplicate entry in NestMembers ",
            "Nest top class_info_index 10 has bad constant type",
        };
        
        for (int i = 0; i < badClasses.length; i++ ) {
            try {
                Class c = Class.forName(badClasses[i]);
                throw new Error("Missing ClassFormatError: " + messages[i]);
            }
            catch (ClassFormatError expected) {
                if (!expected.getMessage().contains(messages[i]))
                   throw new Error("Wrong ClassFormatError message: \"" +
                                   expected.getMessage() + "\" does not contain \"" +
                                   messages[i] + "\"");
                System.out.println("OK - got expected exception: " + expected);
            }
        }
    }
}