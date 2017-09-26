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

import java.util.function.Consumer;

public class ClassBuilder<S, T, C extends ClassBuilder<S, T, C>>
        extends DeclBuilder<S, T, byte[], C> {

    protected TypeHelper<S, T> typeHelper;
    protected S thisClass;
    protected GrowableByteBuffer interfaces = new GrowableByteBuffer();
    protected GrowableByteBuffer fields = new GrowableByteBuffer();
    protected GrowableByteBuffer methods = new GrowableByteBuffer();

    int majorVersion;
    int minorVersion;
    int flags;
    int superclass;
    int nmethods, nfields, ninterfaces;

    public ClassBuilder(PoolHelper<S, T, byte[]> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
        this.typeHelper = typeHelper;
    }

    public C withMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return thisBuilder();
    }

    public C withMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return thisBuilder();
    }

    public C withThisClass(S thisClass) {
        this.thisClass = thisClass;
        return thisBuilder();
    }

    public C withFlags(Flag... flags) {
        for (Flag f : flags) {
            this.flags |= f.flag;
        }
        return thisBuilder();
    }

    public C withSuperclass(S s) {
        this.superclass = poolHelper.putClass(s);
        return thisBuilder();
    }

    public C withSuperinterface(S s) {
        this.interfaces.writeChar(poolHelper.putClass(s));
        ninterfaces++;
        return thisBuilder();
    }

    public final C withField(CharSequence name, T type) {
        return withField(name, type, FB -> {
        });
    }

    public C withField(CharSequence name, T type, Consumer<? super FieldBuilder<S, T, byte[]>> fieldBuilder) {
        FieldBuilder<S, T, byte[]> F = new FieldBuilder<>(name, type, poolHelper, typeHelper);
        fieldBuilder.accept(F);
        F.build(fields);
        nfields++;
        return thisBuilder();
    }

    public final C withMethod(CharSequence name, T type) {
        return withMethod(name, type, MB -> {
        });
    }

    public C withMethod(CharSequence name, T type, Consumer<? super MethodBuilder<S, T, byte[]>> methodBuilder) {
        if (name.toString().contains(".")) {
            throw new IllegalArgumentException("Illegal method name " + name);
        }
        MethodBuilder<S, T, byte[]> M = new MethodBuilder<>(thisClass, name, type, poolHelper, typeHelper);
        methodBuilder.accept(M);
        M.build(methods);
        nmethods++;
        return thisBuilder();
    }

    public byte[] build() {
        addAnnotations();
        int thisClassIdx = poolHelper.putClass(thisClass);
        byte[] poolBytes = poolHelper.entries();
        GrowableByteBuffer buf = new GrowableByteBuffer();
        buf.writeInt(0xCAFEBABE);
        buf.writeChar(minorVersion);
        buf.writeChar(majorVersion);
        buf.writeChar(poolHelper.size() + 1);
        buf.writeBytes(poolBytes);
        buf.writeChar(flags);
        buf.writeChar(thisClassIdx);
        buf.writeChar(superclass);
        buf.writeChar(ninterfaces);
        if (ninterfaces > 0) {
            buf.writeBytes(interfaces);
        }
        buf.writeChar(nfields);
        buf.writeBytes(fields);
        buf.writeChar(nmethods);
        buf.writeBytes(methods);
        buf.writeChar(nattrs);
        buf.writeBytes(attributes);
        return buf.bytes();
    }
}
