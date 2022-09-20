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
 * @summary Verify that primitive class declarations can be annotated
 * @bug 8244713
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main UnannotatedProjection
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.tools.classfile.*;

public class UnannotatedProjection {

    @interface DA {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface DARR {}

    @Target(ElementType.TYPE_PARAMETER)
    @interface TA {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_PARAMETER)
    @interface TARR {}

    @DA @DARR
    public primitive class V<@TA @TARR T> {}

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(UnannotatedProjection.class.getResourceAsStream("UnannotatedProjection$V.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        RuntimeInvisibleAnnotations_attribute inv = (RuntimeInvisibleAnnotations_attribute) cls.attributes.get(Attribute.RuntimeInvisibleAnnotations);
        if (inv == null || inv.annotations == null || inv.annotations.length != 1) {
            throw new AssertionError("Missing annotations");
        }

        String aName = cls.constant_pool.getUTF8Value(inv.annotations[0].type_index);
        if (!aName.equals("LUnannotatedProjection$DA;")) {
            throw new AssertionError("Unexpected annotation: " + aName);
        }

        RuntimeInvisibleTypeAnnotations_attribute invta = (RuntimeInvisibleTypeAnnotations_attribute) cls.attributes.get(Attribute.RuntimeInvisibleTypeAnnotations);
        if (invta == null || invta.annotations == null || invta.annotations.length != 1) {
            throw new AssertionError("Missing annotations");
        }

        aName = cls.constant_pool.getUTF8Value(invta.annotations[0].annotation.type_index);
        if (!aName.equals("LUnannotatedProjection$TA;")) {
            throw new AssertionError("Unexpected annotation: " + aName);
        }

        RuntimeVisibleAnnotations_attribute v = (RuntimeVisibleAnnotations_attribute) cls.attributes.get(Attribute.RuntimeVisibleAnnotations);
        if (v == null || v.annotations == null || v.annotations.length != 1) {
            throw new AssertionError("Missing annotations");
        }

        aName = cls.constant_pool.getUTF8Value(v.annotations[0].type_index);
        if (!aName.equals("LUnannotatedProjection$DARR;")) {
            throw new AssertionError("Unexpected annotation: " + aName);
        }

        RuntimeVisibleTypeAnnotations_attribute vta = (RuntimeVisibleTypeAnnotations_attribute) cls.attributes.get(Attribute.RuntimeVisibleTypeAnnotations);
        if (vta == null || vta.annotations == null || vta.annotations.length != 1) {
            throw new AssertionError("Missing annotations");
        }

        aName = cls.constant_pool.getUTF8Value(vta.annotations[0].annotation.type_index);
        if (!aName.equals("LUnannotatedProjection$TARR;")) {
            throw new AssertionError("Unexpected annotation: " + aName);
        }
    }
}
