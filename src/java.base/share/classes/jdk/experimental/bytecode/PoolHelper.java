/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.ToIntBiFunction;

/**
 * An interface for building and tracking constant pools.
 *
 * @param <S> the type of the symbol representation
 * @param <T> the type of type descriptors representation
 * @param <R> the type of pool entries
 */
public interface PoolHelper<S, T, R> {
    int putClass(S symbol);

    int putValueClass(S symbol);

    int putFieldRef(S owner, CharSequence name, T type);

    int putMethodRef(S owner, CharSequence name, T type, boolean isInterface);

    int putUtf8(CharSequence s);

    int putInt(int i);

    int putFloat(float f);

    int putLong(long l);

    int putDouble(double d);

    int putString(String s);

    int putValue(Object v);

    int putType(T t);

    int putMethodType(T t);

    int putMethodHandle(int refKind, S owner, CharSequence name, T type);

    int putMethodHandle(int refKind, S owner, CharSequence name, T type, boolean isInterface);

    int putInvokeDynamic(CharSequence invokedName, T invokedType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, R>> staticArgs);

    int putConstantDynamic(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, R>> staticArgs);

    int size();

    R representation();

    interface StaticArgListBuilder<S, T, E> {
        StaticArgListBuilder<S, T, E> add(int i);
        StaticArgListBuilder<S, T, E> add(float f);
        StaticArgListBuilder<S, T, E> add(long l);
        StaticArgListBuilder<S, T, E> add(double d);
        StaticArgListBuilder<S, T, E> add(String s);
        StaticArgListBuilder<S, T, E> add(int refKind, S owner, CharSequence name, T type);
        <Z> StaticArgListBuilder<S, T, E> add(Z z, ToIntBiFunction<PoolHelper<S, T, E>, Z> poolFunc);
        StaticArgListBuilder<S, T, E> add(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, E>> staticArgList);
    }
}
