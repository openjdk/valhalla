/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.classfile.impl;

import jdk.internal.classfile.*;
import jdk.internal.classfile.constantpool.*;

import java.lang.constant.ConstantDesc;
import java.util.List;

public final class AnnotationImpl implements Annotation {
    private final Utf8Entry className;
    private final List<AnnotationElement> elements;

    public AnnotationImpl(Utf8Entry className,
                          List<AnnotationElement> elems) {
        this.className = className;
        this.elements = List.copyOf(elems);
    }

    @Override
    public Utf8Entry className() {
        return className;
    }

    @Override
    public List<AnnotationElement> elements() {
        return elements;
    }

    @Override
    public void writeTo(BufWriter buf) {
        buf.writeIndex(className());
        buf.writeList(elements());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Annotation[");
        sb.append(className().stringValue());
        List<AnnotationElement> evps = elements();
        if (!evps.isEmpty())
            sb.append(" [");
        for (AnnotationElement evp : evps) {
            sb.append(evp.name().stringValue())
                    .append("=")
                    .append(evp.value().toString())
                    .append(", ");
        }
        if (!evps.isEmpty()) {
            sb.delete(sb.length()-1, sb.length());
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public record AnnotationElementImpl(Utf8Entry name,
                                        AnnotationValue value)
            implements AnnotationElement {

        @Override
        public void writeTo(BufWriter buf) {
            buf.writeIndex(name());
            value().writeTo(buf);
        }
    }

    public sealed interface OfConstantImpl extends AnnotationValue.OfConstant
            permits AnnotationImpl.OfStringImpl, AnnotationImpl.OfDoubleImpl,
                    AnnotationImpl.OfFloatImpl, AnnotationImpl.OfLongImpl,
                    AnnotationImpl.OfIntegerImpl, AnnotationImpl.OfShortImpl,
                    AnnotationImpl.OfCharacterImpl, AnnotationImpl.OfByteImpl,
                    AnnotationImpl.OfBooleanImpl {

        @Override
        default void writeTo(BufWriter buf) {
            buf.writeU1(tag());
            buf.writeIndex(constant());
        }

        @Override
        default ConstantDesc constantValue() {
            return constant().constantValue();
        }

    }

    public record OfStringImpl(Utf8Entry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfString {

        @Override
        public char tag() {
            return 's';
        }

        @Override
        public String stringValue() {
            return constant().stringValue();
        }
    }

    public record OfDoubleImpl(DoubleEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfDouble {

        @Override
        public char tag() {
            return 'D';
        }

        @Override
        public double doubleValue() {
            return constant().doubleValue();
        }
    }

    public record OfFloatImpl(FloatEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfFloat {

        @Override
        public char tag() {
            return 'F';
        }

        @Override
        public float floatValue() {
            return constant().floatValue();
        }
    }

    public record OfLongImpl(LongEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfLong {

        @Override
        public char tag() {
            return 'J';
        }

        @Override
        public long longValue() {
            return constant().longValue();
        }
    }

    public record OfIntegerImpl(IntegerEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfInteger {

        @Override
        public char tag() {
            return 'I';
        }

        @Override
        public int intValue() {
            return constant().intValue();
        }
    }

    public record OfShortImpl(IntegerEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfShort {

        @Override
        public char tag() {
            return 'S';
        }

        @Override
        public short shortValue() {
            return (short)constant().intValue();
        }
    }

    public record OfCharacterImpl(IntegerEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfCharacter {

        @Override
        public char tag() {
            return 'C';
        }

        @Override
        public char charValue() {
            return (char)constant().intValue();
        }
    }

    public record OfByteImpl(IntegerEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfByte {

        @Override
        public char tag() {
            return 'B';
        }

        @Override
        public byte byteValue() {
            return (byte)constant().intValue();
        }
    }

    public record OfBooleanImpl(IntegerEntry constant)
            implements AnnotationImpl.OfConstantImpl, AnnotationValue.OfBoolean {

        @Override
        public char tag() {
            return 'Z';
        }

        @Override
        public boolean booleanValue() {
            return constant().intValue() == 1;
        }
    }

    public record OfArrayImpl(List<AnnotationValue> values)
            implements AnnotationValue.OfArray {

        public OfArrayImpl(List<AnnotationValue> values) {
            this.values = List.copyOf(values);
        }

        @Override
        public char tag() {
            return '[';
        }

        @Override
        public void writeTo(BufWriter buf) {
            buf.writeU1(tag());
            buf.writeList(values);
        }

    }

    public record OfEnumImpl(Utf8Entry className, Utf8Entry constantName)
            implements AnnotationValue.OfEnum {
        @Override
        public char tag() {
            return 'e';
        }

        @Override
        public void writeTo(BufWriter buf) {
            buf.writeU1(tag());
            buf.writeIndex(className);
            buf.writeIndex(constantName);
        }

    }

    public record OfAnnotationImpl(Annotation annotation)
            implements AnnotationValue.OfAnnotation {
        @Override
        public char tag() {
            return '@';
        }

        @Override
        public void writeTo(BufWriter buf) {
            buf.writeU1(tag());
            annotation.writeTo(buf);
        }

    }

    public record OfClassImpl(Utf8Entry className)
            implements AnnotationValue.OfClass {
        @Override
        public char tag() {
            return 'c';
        }

        @Override
        public void writeTo(BufWriter buf) {
            buf.writeU1(tag());
            buf.writeIndex(className);
        }

    }
}
