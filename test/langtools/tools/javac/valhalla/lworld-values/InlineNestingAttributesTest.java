/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Test nest host - member attributes
 * @bug 8244314
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDallowWithFieldOperator Point.java
 * @run main InlineNestingAttributesTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class InlineNestingAttributesTest {
    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(InlineNestingAttributesTest.class.getResourceAsStream("Point.class"));
        ClassFile clsProj = ClassFile.read(InlineNestingAttributesTest.class.getResourceAsStream("Point$ref.class"));

        NestMembers_attribute nestMembers = (NestMembers_attribute)clsProj.attributes.get(Attribute.NestMembers);
        CONSTANT_Class_info[] members = nestMembers != null ? nestMembers.getChildren(clsProj.constant_pool) : new CONSTANT_Class_info[0];

        if (members.length != 1 || !members[0].getName().equals("Point")) {
            throw new RuntimeException("Nest members not present");
        }

        NestHost_attribute nestHost = (NestHost_attribute)cls.attributes.get(Attribute.NestHost);
        CONSTANT_Class_info host = nestHost != null ? nestHost.getNestTop(cls.constant_pool) : null;

        if (host == null || !host.getName().equals("Point$ref")) {
            throw new RuntimeException("Nest host not present");
        }
    }
}
