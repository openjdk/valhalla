/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

public class AttributeBuilder<S, T, E, D extends AttributeBuilder<S, T, E, D>>
        extends AbstractBuilder<S, T, E, D> {

    protected int nattrs;
    protected GrowableByteBuffer attributes = new GrowableByteBuffer();

    public AttributeBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
    }

    public D withAttribute(CharSequence name, byte[] bytes) {
        attributes.writeChar(poolHelper.putUtf8(name));
        attributes.writeInt(bytes.length);
        attributes.writeBytes(bytes);
        nattrs++;
        return thisBuilder();
    }

    public <Z> D withAttribute(CharSequence name, Z attr, AttributeWriter<S, T, E, Z> attrWriter) {
        attributes.writeChar(poolHelper.putUtf8(name));
        int offset = attributes.offset;
        attributes.writeInt(0);
        attrWriter.write(attr, poolHelper, attributes);
        int len = attributes.offset - offset - 4;
        attributes.withOffset(offset, buf -> buf.writeInt(len));
        nattrs++;
        return thisBuilder();
    }

    public interface AttributeWriter<S, T, E, A> {
        void write(A attr, PoolHelper<S, T, E> poolHelper, GrowableByteBuffer buf);
    }
}
