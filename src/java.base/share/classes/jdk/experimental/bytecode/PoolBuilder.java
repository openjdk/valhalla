/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Interface for low-level building of constant pool. Indicies are used rather
 * than strings and thus component parts aren't added automatically.
 *
 * @param <R> the type of the constant pool representation that is built
 */
public interface PoolBuilder<R> {

    int putClass(int utf8_idx);

    int putConstantDynamic(int bsmIndex, int nameAndType_idx);

    int putDouble(double d);

    int putFieldRef(int owner_idx, int nameAndType_idx);

    int putFloat(float f);

    int putMethodHandle(int refKind, int ref_idx);

    int putInt(int i);

    int putInvokeDynamic(int bsmIndex, int nameAndType_idx);

    int putLong(long l);

    int putMemberRef(PoolTag tag, int owner_idx, int nameAndType_idx);

    int putMethodRef(int owner_idx, int nameAndType_idx, boolean isInterface);

    int putMethodType(int desc_idx);

    int putNameAndType(int name_idx, int type_idx);

    int putString(int utf8_index);

    int putUtf8(CharSequence s);

    /**
     * 
     * @return count of constant pool indicies
     */
    int size();

    /**
     * 
     * @return the representation of the constant pool
     */
    R representation();
    
}
