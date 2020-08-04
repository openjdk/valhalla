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
 * @summary Check to see if the reference projection is a sealed class
 * @bug 8244315
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDallowWithFieldOperator Point.java
 * @run main ProjectionSealed
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class ProjectionSealed {
    public static void main(String[] args) throws Exception {
        ClassFile clsProj = ClassFile.read(ProjectionSealed.class.getResourceAsStream("Point$ref.class"));

        PermittedSubclasses_attribute permitted = (PermittedSubclasses_attribute)clsProj.attributes.get(Attribute.PermittedSubclasses);
        CONSTANT_Class_info[] infos = permitted != null ? permitted.getSubtypes(clsProj.constant_pool) : new CONSTANT_Class_info[0];

        if (infos.length != 1 || !infos[0].getName().equals("Point")) {
            throw new RuntimeException("Sealed classes not present");
        }
    }
}
