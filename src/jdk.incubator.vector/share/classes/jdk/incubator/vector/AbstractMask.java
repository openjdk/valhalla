/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import jdk.internal.vm.annotation.ForceInline;

import jdk.internal.misc.Unsafe;

import jdk.internal.vm.vector.VectorSupport;

import static jdk.incubator.vector.VectorOperators.*;

import static jdk.internal.vm.vector.VectorSupport.*;

abstract class AbstractMask<E> extends VectorMask<E> {

    /*package-private*/
    abstract VectorPayloadMF getBits();

    /*package-private*/
    abstract long multiFieldOffset();

    /*package-private*/
    @ForceInline
    static <F> VectorPayloadMF createPayloadInstance(VectorSpecies<F> species) {
        boolean isMaxShape = species.vectorShape() == VectorShape.S_Max_BIT;
        Class<?> etype = isMaxShape ? species.elementType() : boolean.class;
        int length = species.length();
        return VectorPayloadMF.newInstanceFactory(etype, length, isMaxShape, true);
    }

    static <F> VectorPayloadMF prepare(VectorPayloadMF payload, int offset, VectorSpecies<F> species) {
        VectorPayloadMF res = createPayloadInstance(species);
        res = Unsafe.getUnsafe().makePrivateBuffer(res);
        long mOffset = res.multiFieldOffset();
        for (int i = 0; i < species.length(); i++) {
            boolean b = Unsafe.getUnsafe().getBoolean(payload, mOffset + i + offset);
            Unsafe.getUnsafe().putBoolean(res, mOffset + i, b);
        }
        res = Unsafe.getUnsafe().finishPrivateBuffer(res);
        return res;
    }

    static <F> VectorPayloadMF prepare(boolean val, VectorSpecies<F> species) {
        VectorPayloadMF res = createPayloadInstance(species);
        res = Unsafe.getUnsafe().makePrivateBuffer(res);
        long mOffset = res.multiFieldOffset();
        for (int i = 0; i < species.length(); i++) {
            Unsafe.getUnsafe().putBoolean(res, mOffset + i, val);
        }
        res = Unsafe.getUnsafe().finishPrivateBuffer(res);
        return res;
    }

    // Unary operator

    interface MUnOp {
        boolean apply(int i, boolean a);
    }

    AbstractMask<E> uOpMF(MUnOp f) {
        int length = vspecies().laneCount();
        VectorPayloadMF bits = getBits();
        VectorPayloadMF res = createPayloadInstance(vspecies());
        res = Unsafe.getUnsafe().makePrivateBuffer(res);
        long mOffset = res.multiFieldOffset();
        for (int i = 0; i < length; i++) {
            boolean b = Unsafe.getUnsafe().getBoolean(bits, mOffset + i);
            Unsafe.getUnsafe().putBoolean(res, mOffset + i, f.apply(i, b));
        }
        res = Unsafe.getUnsafe().finishPrivateBuffer(res);
        return vspecies().maskFactory(res);
    }

    // Binary operator

    interface MBinOp {
        boolean apply(int i, boolean a, boolean b);
    }

    AbstractMask<E> bOpMF(AbstractMask<E> m, MBinOp f) {
        int length = vspecies().laneCount();
        VectorPayloadMF bits = getBits();
        VectorPayloadMF mbits = m.getBits();
        VectorPayloadMF res = createPayloadInstance(vspecies());
        res = Unsafe.getUnsafe().makePrivateBuffer(res);
        long mOffset = res.multiFieldOffset();
        for (int i = 0; i < length; i++) {
            boolean b = Unsafe.getUnsafe().getBoolean(bits, mOffset + i);
            boolean mb = Unsafe.getUnsafe().getBoolean(mbits, mOffset + i);
            Unsafe.getUnsafe().putBoolean(res, mOffset + i, f.apply(i, b, mb));
        }
        res = Unsafe.getUnsafe().finishPrivateBuffer(res);
        return vspecies().maskFactory(res);
    }

    // Store operator

    void stOpMF(boolean[] arr, int idx) {
        VectorPayloadMF bits = getBits();
        long mOffset = multiFieldOffset();
        for (int i = 0; i < vspecies().laneCount(); i++) {
            arr[idx++] = Unsafe.getUnsafe().getBoolean(bits, mOffset + i);
        }
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
    public <F> VectorMask<F> cast(VectorSpecies<F> dsp) {
        AbstractSpecies<F> species = (AbstractSpecies<F>) dsp;
        if (length() != species.laneCount())
            throw new IllegalArgumentException("VectorMask length and species length differ");

        return VectorSupport.convert(VectorSupport.VECTOR_OP_CAST,
                this.getClass(), vspecies().elementType(), vspecies().laneCount,
                species.maskType(), species.elementType(), vspecies().laneCount,
                this, species,
                (m, s) -> s.maskFactory(m.getBits()).check(s));
    }

    @Override
    @ForceInline
    public boolean laneIsSet(int i) {
        int length = length();
        Objects.checkIndex(i, length);
        if (length <= Long.SIZE) {
            return ((toLong() >>> i) & 1L) == 1;
        } else {
            VectorPayloadMF bits = getBits();
            return Unsafe.getUnsafe().getBoolean(bits, bits.multiFieldOffset() + i);
        }
    }

    @Override
    public void intoArray(boolean[] bits, int i) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) vectorSpecies();
        int laneCount = vsp.laneCount();
        i = VectorIntrinsics.checkFromIndexSize(i, laneCount, bits.length);
        VectorSupport.store(
            vsp.maskType(), vsp.elementType(), laneCount,
            bits, (long) i + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
            this, bits, i,
            (c, idx, s) -> s.stOpMF(c, (int) idx));
    }

    @Override
    public boolean[] toArray() {
        boolean[] arr = new boolean[length()];
        intoArray(arr, 0);
        return arr;
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public
    <F> VectorMask<F> check(Class<F> elementType) {
        if (vectorSpecies().elementType() != elementType) {
            throw AbstractSpecies.checkFailed(this, elementType);
        }
        return (VectorMask<F>) this;
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public
    <F> VectorMask<F> check(VectorSpecies<F> species) {
        if (species != vectorSpecies()) {
            throw AbstractSpecies.checkFailed(this, species);
        }
        return (VectorMask<F>) this;
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    <F> VectorMask<F> check(Class<? extends VectorMask<F>> maskClass, Vector<F> vector) {
        if (!sameSpecies(maskClass, vector)) {
            throw AbstractSpecies.checkFailed(this, vector);
        }
        return (VectorMask<F>) this;
    }

    @ForceInline
    private <F> boolean sameSpecies(Class<? extends VectorMask<F>> maskClass, Vector<F> vector) {
        boolean same = getClass() == maskClass;
        assert (same == (vectorSpecies() == vector.species())) : same;
        return same;
    }

    @Override
    @ForceInline
    public final VectorMask<E> andNot(VectorMask<E> m) {
        return and(m.not());
    }

    @Override
    @ForceInline
    public final VectorMask<E> eq(VectorMask<E> m) {
        return xor(m.not());
    }

    /*package-private*/
    boolean anyTrueHelper() {
        // FIXME: Maybe use toLong() != 0 here.
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = 0; i < length(); i++) {
            if (Unsafe.getUnsafe().getBoolean(bits, mOffset + i)) return true;
        }
        return false;
    }

    /*package-private*/
    boolean allTrueHelper() {
        // FIXME: Maybe use not().toLong() == 0 here.
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = 0; i < length(); i++) {
            if (!Unsafe.getUnsafe().getBoolean(bits, mOffset + i)) return false;
        }
        return true;
    }

    /*package-private*/
    int trueCountHelper() {
        int c = 0;
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = 0; i < length(); i++) {
            if (Unsafe.getUnsafe().getBoolean(bits, mOffset + i)) c++;
        }
        return c;
    }

    /*package-private*/
    int firstTrueHelper() {
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = 0; i < length(); i++) {
            if (Unsafe.getUnsafe().getBoolean(bits, mOffset + i)) return i;
        }
        return length();
    }

    /*package-private*/
    int lastTrueHelper() {
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = length() - 1; i >= 0; i--) {
            if (Unsafe.getUnsafe().getBoolean(bits, mOffset + i)) return i;
        }
        return -1;
    }

    /*package-private*/
    long toLongHelper() {
        long res = 0;
        long set = 1;
        VectorPayloadMF bits = getBits();
        long mOffset = bits.multiFieldOffset();
        for (int i = 0; i < length(); i++) {
            res = Unsafe.getUnsafe().getBoolean(bits, mOffset + i) ? res | set : res;
            set = set << 1;
        }
        return res;
    }

    /*package-private*/
    @ForceInline
    VectorMask<E> indexPartiallyInRange(int offset, int limit) {
        int vlength = length();
        Vector<E> iota = vectorSpecies().zero().addIndex(1);
        VectorMask<E> badMask = checkIndex0(offset, limit, iota, vlength);
        return badMask.not();
    }

    /*package-private*/
    @ForceInline
    VectorMask<E> indexPartiallyInRange(long offset, long limit) {
        int vlength = length();
        Vector<E> iota = vectorSpecies().zero().addIndex(1);
        VectorMask<E> badMask = checkIndex0(offset, limit, iota, vlength);
        return badMask.not();
    }

    @Override
    @ForceInline
    public VectorMask<E> indexInRange(int offset, int limit) {
        if (offset < 0) {
            return this.and(indexPartiallyInRange(offset, limit));
        } else if (offset >= limit) {
            return vectorSpecies().maskAll(false);
        } else if (limit - offset >= length()) {
            return this;
        }
        return this.and(indexPartiallyInUpperRange(offset, limit));
    }

    @ForceInline
    public VectorMask<E> indexInRange(long offset, long limit) {
        if (offset < 0) {
            return this.and(indexPartiallyInRange(offset, limit));
        } else if (offset >= limit) {
            return vectorSpecies().maskAll(false);
        } else if (limit - offset >= length()) {
            return this;
        }
        return this.and(indexPartiallyInUpperRange(offset, limit));
    }

    abstract VectorMask<E> indexPartiallyInUpperRange(long offset, long limit);

    /*package-private*/
    @ForceInline
    AbstractVector<E>
    toVectorTemplate() {
        AbstractSpecies<E> vsp = vspecies();
        Vector<E> zero = vsp.broadcast(0);
        Vector<E> mone = vsp.broadcast(-1);
        // -1 will result in the most significant bit being set in
        // addition to some or all other lane bits.
        // For integral types, *all* lane bits will be set.
        // The bits for -1.0 are like {0b10111*0000*}.
        // FIXME: Use a conversion intrinsic for this operation.
        // https://bugs.openjdk.org/browse/JDK-8225740
        return (AbstractVector<E>) zero.blend(mone, this);
    }

    /**
     * Test if a masked memory access at a given offset into an array
     * of the given length will stay within the array.
     * The per-lane offsets are iota*esize.
     */
    /*package-private*/
    @ForceInline
    void checkIndexByLane(int offset, int length,
                          Vector<E> iota,
                          int esize) {
        if (VectorIntrinsics.VECTOR_ACCESS_OOB_CHECK == 0) {
            return;
        }
        // Although the specification is simple, the implementation is
        // tricky, because the value iota*esize might possibly
        // overflow.  So we calculate our test values as scalars,
        // clipping to the range [-1..VLENGTH], and test them against
        // the unscaled iota vector, whose values are in [0..VLENGTH-1].
        int vlength = length();
        VectorMask<E> badMask;
        if (esize == 1) {
            badMask = checkIndex0(offset, length, iota, vlength);
        } else if (offset >= 0) {
            // Masked access to multi-byte lanes in byte array.
            // It could be aligned anywhere.
            int elemCount = Math.min(vlength, (length - offset) / esize);
            badMask = checkIndex0(0, elemCount, iota, vlength);
        } else {
            int clipOffset = Math.max(offset, -(vlength * esize));
            badMask = checkIndex0(clipOffset, length,
                                  iota.lanewise(VectorOperators.MUL, esize),
                                  vlength * esize);
        }
        badMask = badMask.and(this);
        if (badMask.anyTrue()) {
            int badLane = badMask.firstTrue();
            throw ((AbstractMask<E>)badMask)
                   .checkIndexFailed(offset, badLane, length, esize);
        }
    }

    private
    @ForceInline
    VectorMask<E> checkIndex0(int offset, int length,
                              Vector<E> iota, int vlength) {
        // An active lane is bad if its number is greater than
        // length-offset, since when added to offset it will step off
        // of the end of the array.  To avoid overflow when
        // converting, clip the comparison value to [0..vlength]
        // inclusive.
        int indexLimit = Math.max(0, Math.min(length - offset, vlength));
        VectorMask<E> badMask =
            iota.compare(GE, iota.broadcast(indexLimit));
        if (offset < 0) {
            // An active lane is bad if its number is less than
            // -offset, because when added to offset it will then
            // address an array element at a negative index.  To avoid
            // overflow when converting, clip the comparison value at
            // vlength.  This specific expression works correctly even
            // when offset is Integer.MIN_VALUE.
            int firstGoodIndex = -Math.max(offset, -vlength);
            VectorMask<E> badMask2 =
                iota.compare(LT, iota.broadcast(firstGoodIndex));
            if (indexLimit >= vlength) {
                badMask = badMask2;  // 1st badMask is all true
            } else {
                badMask = badMask.or(badMask2);
            }
        }
        return badMask;
    }

    /**
     * Test if a masked memory access at a given offset into an array
     * of the given length will stay within the array.
     * The per-lane offsets are iota*esize.
     */
    /*package-private*/
    @ForceInline
    void checkIndexByLane(long offset, long length,
                          Vector<E> iota,
                          int esize) {
        if (VectorIntrinsics.VECTOR_ACCESS_OOB_CHECK == 0) {
            return;
        }
        // Although the specification is simple, the implementation is
        // tricky, because the value iota*esize might possibly
        // overflow.  So we calculate our test values as scalars,
        // clipping to the range [-1..VLENGTH], and test them against
        // the unscaled iota vector, whose values are in [0..VLENGTH-1].
        int vlength = length();
        VectorMask<E> badMask;
        if (esize == 1) {
            badMask = checkIndex0(offset, length, iota, vlength);
        } else if (offset >= 0) {
            // Masked access to multi-byte lanes in byte array.
            // It could be aligned anywhere.
            // 0 <= elemCount <= vlength
            int elemCount = (int) Math.min(vlength, (length - offset) / esize);
            badMask = checkIndex0(0, elemCount, iota, vlength);
        } else {
            // -vlength * esize <= clipOffset <= 0
            int clipOffset = (int) Math.max(offset, -(vlength * esize));
            badMask = checkIndex0(clipOffset, length,
                    iota.lanewise(VectorOperators.MUL, esize),
                    vlength * esize);
        }
        badMask = badMask.and(this);
        if (badMask.anyTrue()) {
            int badLane = badMask.firstTrue();
            throw ((AbstractMask<E>)badMask)
                    .checkIndexFailed(offset, badLane, length, esize);
        }
    }

    private
    @ForceInline
    VectorMask<E> checkIndex0(long offset, long length,
                              Vector<E> iota, int vlength) {
        // An active lane is bad if its number is greater than
        // length-offset, since when added to offset it will step off
        // of the end of the array.  To avoid overflow when
        // converting, clip the comparison value to [0..vlength]
        // inclusive.
        // 0 <= indexLimit <= vlength
        int indexLimit = (int) Math.max(0, Math.min(length - offset, vlength));
        VectorMask<E> badMask =
                iota.compare(GE, iota.broadcast(indexLimit));
        if (offset < 0) {
            // An active lane is bad if its number is less than
            // -offset, because when added to offset it will then
            // address an array element at a negative index.  To avoid
            // overflow when converting, clip the comparison value at
            // vlength.  This specific expression works correctly even
            // when offset is Integer.MIN_VALUE.
            // 0 <= firstGoodIndex <= vlength
            int firstGoodIndex = (int) -Math.max(offset, -vlength);
            VectorMask<E> badMask2 =
                    iota.compare(LT, iota.broadcast(firstGoodIndex));
            if (indexLimit >= vlength) {
                badMask = badMask2;  // 1st badMask is all true
            } else {
                badMask = badMask.or(badMask2);
            }
        }
        return badMask;
    }

    private IndexOutOfBoundsException checkIndexFailed(long offset, int lane,
                                                       long length, int esize) {
        String msg = String.format("Masked range check failed: "+
                                   "vector mask %s out of bounds at "+
                                   "index %d+%d for length %d",
                                   this, offset, lane * esize, length);
        if (esize != 1) {
            msg += String.format(" (each lane spans %d elements)", esize);
        }
        throw new IndexOutOfBoundsException(msg);
    }

}
