/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package jdk.incubator.vector;

import java.util.function.IntUnaryOperator;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;

import static jdk.internal.vm.vector.VectorSupport.*;

abstract class AbstractShuffle<E> extends VectorShuffle<E> {
    static final IntUnaryOperator IDENTITY = i -> i;

    // Internal representation allows for a maximum index of 256
    // Values are clipped to [-VLENGTH..VLENGTH-1].

    /*package-private*/
    abstract VectorPayloadMF indices();

    static <F> VectorPayloadMF prepare(int[] indices, int offset, VectorSpecies<F> species) {
        int length = species.length();
        boolean isMaxShape  = species.vectorShape() == VectorShape.S_Max_BIT;
        VectorPayloadMF payload = VectorPayloadMF.newShuffleInstanceFactory(species.elementType(), length, isMaxShape);
        payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
        long mf_offset = payload.multiFieldOffset();
        for (int i = 0; i < length; i++) {
            int si = indices[offset + i];
            si = partiallyWrapIndex(si, length);
            Unsafe.getUnsafe().putByte(payload, mf_offset + i * Byte.BYTES, (byte) si);
        }
        payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
        return payload;
    }

    static <F> VectorPayloadMF prepare(IntUnaryOperator f, VectorSpecies<F> species) {
        int length = species.length();
        boolean isMaxShape  = species.vectorShape() == VectorShape.S_Max_BIT;
        VectorPayloadMF payload = VectorPayloadMF.newShuffleInstanceFactory(species.elementType(), length, isMaxShape);
        payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
        long offset = payload.multiFieldOffset();
        for (int i = 0; i < length; i++) {
            int si = f.applyAsInt(i);
            si = partiallyWrapIndex(si, length);
            Unsafe.getUnsafe().putByte(payload, offset + i * Byte.BYTES, (byte) si);
        }
        payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
        return payload;
    }

    /*package-private*/
    abstract AbstractSpecies<E> vspecies();

    @Override
    @ForceInline
    public final VectorSpecies<E> vectorSpecies() {
        return vspecies();
    }

    @Override
    @ForceInline
    public void intoArray(int[] a, int offset) {
        VectorPayloadMF indices = indices();
        int vlen = indices.length();
        long mf_offset = indices.multiFieldOffset();
        for (int i = 0; i < vlen; i++) {
            int sourceIndex = Unsafe.getUnsafe().getByte(indices, mf_offset + i * Byte.BYTES);
            assert(sourceIndex >= -vlen && sourceIndex < vlen);
            a[offset + i] = sourceIndex;
        }
    }

    @Override
    @ForceInline
    public int[] toArray() {
        VectorPayloadMF indices = indices();
        int[] a = new int[indices.length()];
        intoArray(a, 0);
        return a;
    }

    /*package-private*/
    @ForceInline
    final
    AbstractVector<E>
    toVectorTemplate() {
        // Note that the values produced by laneSource
        // are already clipped.  At this point we convert
        // them from internal ints (or bytes) into the ETYPE.
        // FIXME: Use a conversion intrinsic for this operation.
        // https://bugs.openjdk.org/browse/JDK-8225740
        return (AbstractVector<E>) vspecies().fromIntValues(toArray());
    }

    @ForceInline
    public final VectorShuffle<E> checkIndexes() {
        if (VectorIntrinsics.VECTOR_ACCESS_OOB_CHECK == 0) {
            return this;
        }
        Vector<E> shufvec = this.toVector();
        VectorMask<E> vecmask = shufvec.compare(VectorOperators.LT, vspecies().zero());
        if (vecmask.anyTrue()) {
            VectorPayloadMF indices = indices();
            long offset = indices.multiFieldOffset();
            throw checkIndexFailed(Unsafe.getUnsafe().getByte(indices, offset + vecmask.firstTrue() * Byte.BYTES), length());
        }
        return this;
    }

    @ForceInline
    public final VectorShuffle<E> wrapIndexes() {
        Vector<E> shufvec = this.toVector();
        VectorMask<E> vecmask = shufvec.compare(VectorOperators.LT, vspecies().zero());
        if (vecmask.anyTrue()) {
            // FIXME: vectorize this
            VectorPayloadMF indices = indices();
            return wrapAndRebuild(indices);
        }
        return this;
    }

    @ForceInline
    public final VectorShuffle<E> wrapAndRebuild(VectorPayloadMF oldIndices) {
        int length = oldIndices.length();
        boolean is_max_species = ((AbstractSpecies)(vspecies())).is_max_species();
        VectorPayloadMF indices = VectorPayloadMF.newShuffleInstanceFactory(vspecies().elementType(), length, is_max_species);
        long offset = oldIndices.multiFieldOffset();
        indices = Unsafe.getUnsafe().makePrivateBuffer(indices);
        for (int i = 0; i < length; i++) {
            int si = Unsafe.getUnsafe().getByte(oldIndices, offset + i * Byte.BYTES);
            // FIXME: This does not work unless it's a power of 2.
            if ((length & (length - 1)) == 0) {
                si += si & length;  // power-of-two optimization
            } else if (si < 0) {
                // non-POT code requires a conditional add
                si += length;
            }
            assert(si >= 0 && si < length);
            Unsafe.getUnsafe().putByte(indices, offset + i * Byte.BYTES, (byte) si);
        }
        indices = Unsafe.getUnsafe().finishPrivateBuffer(indices);
        return vspecies().dummyVectorMF().shuffleFromBytes(indices);
    }

    @ForceInline
    public final VectorMask<E> laneIsValid() {
        Vector<E> shufvec = this.toVector();
        return shufvec.compare(VectorOperators.GE, vspecies().zero());
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public final
    <F> VectorShuffle<F> check(VectorSpecies<F> species) {
        if (species != vectorSpecies()) {
            throw AbstractSpecies.checkFailed(this, species);
        }
        return (VectorShuffle<F>) this;
    }

    @Override
    @ForceInline
    public final int checkIndex(int index) {
        return checkIndex0(index, length(), (byte)1);
    }

    @Override
    @ForceInline
    public final int wrapIndex(int index) {
        return checkIndex0(index, length(), (byte)0);
    }

    /** Return invalid indexes partially wrapped
     * mod VLENGTH to negative values.
     */
    /*package-private*/
    @ForceInline
    static
    int partiallyWrapIndex(int index, int laneCount) {
        return checkIndex0(index, laneCount, (byte)-1);
    }

    /*package-private*/
    @ForceInline
    static int checkIndex0(int index, int laneCount, byte mode) {
        int wrapped = VectorIntrinsics.wrapToRange(index, laneCount);
        if (mode == 0 || wrapped == index) {
            return wrapped;
        }
        if (mode < 0) {
            return wrapped - laneCount;  // special mode for internal storage
        }
        throw checkIndexFailed(index, laneCount);
    }

    private static IndexOutOfBoundsException checkIndexFailed(int index, int laneCount) {
        int max = laneCount - 1;
        String msg = "required an index in [0.."+max+"] but found "+index;
        return new IndexOutOfBoundsException(msg);
    }

    static boolean indexesInRange(VectorPayloadMF indices) {
        int length = indices.length();
        long offset = indices.multiFieldOffset();
        for (int i = 0; i < length; i++) {
            byte si = Unsafe.getUnsafe().getByte(indices, offset + i * Byte.BYTES);
            if (si >= length || si < -length) {
                boolean assertsEnabled = false;
                assert(assertsEnabled = true);
                if (assertsEnabled) {
                    String msg = ("index "+si+"out of range ["+length+"] in "+
                            indices.toString());
                    throw new AssertionError(msg);
                }
                return false;
            }
        }
        return true;
    }
}
