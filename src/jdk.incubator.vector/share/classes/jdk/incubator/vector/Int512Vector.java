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

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Objects;
import jdk.internal.misc.Unsafe;
import java.util.function.IntUnaryOperator;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.vector.VectorSupport;

import static jdk.internal.vm.vector.VectorSupport.*;

import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

@SuppressWarnings("cast")  // warning: redundant cast
value class Int512Vector extends IntVector {
    static final IntSpecies VSPECIES =
        (IntSpecies) IntVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Int512Vector> VCLASS = Int512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Integer> ETYPE = int.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF512I.class);

    private final VectorPayloadMF512I payload;

    Int512Vector(Object value) {
        this.payload = (VectorPayloadMF512I) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Int512Vector ZERO = new Int512Vector(VectorPayloadMF.newInstanceFactory(int.class, 16));
    static final Int512Vector IOTA = new Int512Vector(VectorPayloadMF.createVectPayloadInstanceI(16, (int[])(VSPECIES.iotaArray())));

    static {
        // Warm up a few species caches.
        // If we do this too much we will
        // get NPEs from bootstrap circularity.
        VSPECIES.dummyVectorMF();
        VSPECIES.withLanes(LaneType.BYTE);
    }

    // Specialized extractors

    @ForceInline
    final @Override
    public IntSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Integer> elementType() { return int.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Integer.SIZE; }

    @ForceInline
    @Override
    public final VectorShape shape() { return VSHAPE; }

    @ForceInline
    @Override
    public final int length() { return VLENGTH; }

    @ForceInline
    @Override
    public final int bitSize() { return VSIZE; }

    @ForceInline
    @Override
    public final int byteSize() { return VSIZE / Byte.SIZE; }

    @ForceInline
    @Override
    public final long multiFieldOffset() { return MFOFFSET; }

    @Override
    @ForceInline
    public final Int512Vector broadcast(int e) {
        return (Int512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Int512Vector broadcast(long e) {
        return (Int512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Int512Mask maskFromPayload(VectorPayloadMF payload) {
        return new Int512Mask(payload);
    }

    @Override
    @ForceInline
    Int512Shuffle iotaShuffle() { return Int512Shuffle.IOTA; }

    @Override
    @ForceInline
    Int512Shuffle shuffleFromArray(int[] indices, int i) { return new Int512Shuffle(indices, i); }

    @Override
    @ForceInline
    Int512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Int512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Int512Vector vectorFactory(VectorPayloadMF vec) {
        return new Int512Vector(vec);
    }

    @ForceInline
    final @Override
    Byte512Vector asByteVectorRaw() {
        return (Byte512Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Int512Vector uOpMF(FUnOp f) {
        return (Int512Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Int512Vector uOpMF(VectorMask<Integer> m, FUnOp f) {
        return (Int512Vector)
            super.uOpTemplateMF((Int512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Int512Vector bOpMF(Vector<Integer> v, FBinOp f) {
        return (Int512Vector) super.bOpTemplateMF((Int512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Int512Vector bOpMF(Vector<Integer> v,
                     VectorMask<Integer> m, FBinOp f) {
        return (Int512Vector)
            super.bOpTemplateMF((Int512Vector)v, (Int512Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Int512Vector tOpMF(Vector<Integer> v1, Vector<Integer> v2, FTriOp f) {
        return (Int512Vector)
            super.tOpTemplateMF((Int512Vector)v1, (Int512Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Int512Vector tOpMF(Vector<Integer> v1, Vector<Integer> v2,
                     VectorMask<Integer> m, FTriOp f) {
        return (Int512Vector)
            super.tOpTemplateMF((Int512Vector)v1, (Int512Vector)v2,
                                (Int512Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    int rOpMF(int v, VectorMask<Integer> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Integer,F> conv,
                           VectorSpecies<F> rsp, int part) {
        return super.convertShapeTemplate(conv, rsp, part);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> reinterpretShape(VectorSpecies<F> toSpecies, int part) {
        return super.reinterpretShapeTemplate(toSpecies, part);  // specialize
    }

    // Specialized algebraic operations:

    // The following definition forces a specialized version of this
    // crucial method into the v-table of this class.  A call to add()
    // will inline to a call to lanewise(ADD,), at which point the JIT
    // intrinsic will have the opcode of ADD, plus all the metadata
    // for this particular class, enabling it to generate precise
    // code.
    //
    // There is probably no benefit to the JIT to specialize the
    // masked or broadcast versions of the lanewise method.

    @Override
    @ForceInline
    public Int512Vector lanewise(Unary op) {
        return (Int512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector lanewise(Unary op, VectorMask<Integer> m) {
        return (Int512Vector) super.lanewiseTemplate(op, Int512Mask.class, (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector lanewise(Binary op, Vector<Integer> v) {
        return (Int512Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector lanewise(Binary op, Vector<Integer> v, VectorMask<Integer> m) {
        return (Int512Vector) super.lanewiseTemplate(op, Int512Mask.class, v, (Int512Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Int512Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Int512Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Int512Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Integer> m) {
        return (Int512Vector) super.lanewiseShiftTemplate(op, Int512Mask.class, e, (Int512Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Int512Vector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2) {
        return (Int512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Int512Vector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2, VectorMask<Integer> m) {
        return (Int512Vector) super.lanewiseTemplate(op, Int512Mask.class, v1, v2, (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Int512Vector addIndex(int scale) {
        return (Int512Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final int reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final int reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Integer> m) {
        return super.reduceLanesTemplate(op, Int512Mask.class, (Int512Mask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Integer> m) {
        return (long) super.reduceLanesTemplate(op, Int512Mask.class, (Int512Mask) m);  // specialized
    }

    @Override
    @ForceInline
    public final
    <F> VectorShuffle<F> toShuffle(AbstractSpecies<F> dsp) {
        return super.toShuffleTemplate(dsp);
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Int512Mask test(Test op) {
        return super.testTemplate(Int512Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Int512Mask test(Test op, VectorMask<Integer> m) {
        return super.testTemplate(Int512Mask.class, op, (Int512Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Int512Mask compare(Comparison op, Vector<Integer> v) {
        return super.compareTemplate(Int512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Int512Mask compare(Comparison op, int s) {
        return super.compareTemplate(Int512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Int512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Int512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Int512Mask compare(Comparison op, Vector<Integer> v, VectorMask<Integer> m) {
        return super.compareTemplate(Int512Mask.class, op, v, (Int512Mask) m);
    }


    @Override
    @ForceInline
    public Int512Vector blend(Vector<Integer> v, VectorMask<Integer> m) {
        return (Int512Vector)
            super.blendTemplate(Int512Mask.class,
                                (Int512Vector) v,
                                (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector slice(int origin, Vector<Integer> v) {
        return (Int512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector slice(int origin) {
        return (Int512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector unslice(int origin, Vector<Integer> w, int part) {
        return (Int512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector unslice(int origin, Vector<Integer> w, int part, VectorMask<Integer> m) {
        return (Int512Vector)
            super.unsliceTemplate(Int512Mask.class,
                                  origin, w, part,
                                  (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector unslice(int origin) {
        return (Int512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector rearrange(VectorShuffle<Integer> s) {
        return (Int512Vector)
            super.rearrangeTemplate(Int512Shuffle.class,
                                    (Int512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector rearrange(VectorShuffle<Integer> shuffle,
                                  VectorMask<Integer> m) {
        return (Int512Vector)
            super.rearrangeTemplate(Int512Shuffle.class,
                                    Int512Mask.class,
                                    (Int512Shuffle) shuffle,
                                    (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector rearrange(VectorShuffle<Integer> s,
                                  Vector<Integer> v) {
        return (Int512Vector)
            super.rearrangeTemplate(Int512Shuffle.class,
                                    (Int512Shuffle) s,
                                    (Int512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector compress(VectorMask<Integer> m) {
        return (Int512Vector)
            super.compressTemplate(Int512Mask.class,
                                   (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector expand(VectorMask<Integer> m) {
        return (Int512Vector)
            super.expandTemplate(Int512Mask.class,
                                   (Int512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector selectFrom(Vector<Integer> v) {
        return (Int512Vector)
            super.selectFromTemplate((Int512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int512Vector selectFrom(Vector<Integer> v,
                                   VectorMask<Integer> m) {
        return (Int512Vector)
            super.selectFromTemplate((Int512Vector) v,
                                     (Int512Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public int lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
            case 4: return laneHelper(4);
            case 5: return laneHelper(5);
            case 6: return laneHelper(6);
            case 7: return laneHelper(7);
            case 8: return laneHelper(8);
            case 9: return laneHelper(9);
            case 10: return laneHelper(10);
            case 11: return laneHelper(11);
            case 12: return laneHelper(12);
            case 13: return laneHelper(13);
            case 14: return laneHelper(14);
            case 15: return laneHelper(15);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public int laneHelper(int i) {
        return (int) VectorSupport.extract(
                             VCLASS, ETYPE, VLENGTH,
                             this, i,
                             (vec, ix) -> {
                                 VectorPayloadMF vecpayload = vec.vec();
                                 long start_offset = vecpayload.multiFieldOffset();
                                 return (long)Unsafe.getUnsafe().getInt(vecpayload, start_offset + ix * Integer.BYTES);
                             });
    }

    @ForceInline
    @Override
    public Int512Vector withLane(int i, int e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            case 4: return withLaneHelper(4, e);
            case 5: return withLaneHelper(5, e);
            case 6: return withLaneHelper(6, e);
            case 7: return withLaneHelper(7, e);
            case 8: return withLaneHelper(8, e);
            case 9: return withLaneHelper(9, e);
            case 10: return withLaneHelper(10, e);
            case 11: return withLaneHelper(11, e);
            case 12: return withLaneHelper(12, e);
            case 13: return withLaneHelper(13, e);
            case 14: return withLaneHelper(14, e);
            case 15: return withLaneHelper(15, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Int512Vector withLaneHelper(int i, int e) {
       return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = Unsafe.getUnsafe().makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    Unsafe.getUnsafe().putInt(tpayload, start_offset + ix * Integer.BYTES, (int)bits);
                                    tpayload = Unsafe.getUnsafe().finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class Int512Mask extends AbstractMask<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF128Z payload;

        Int512Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF128Z) payload;
        }

        Int512Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Int512Mask(boolean val) {
            this(prepare(val, VLENGTH));
        }

        @ForceInline
        final @Override
        public IntSpecies vspecies() {
            // ISSUE:  This should probably be a @Stable
            // field inside AbstractMask, rather than
            // a megamorphic method.
            return VSPECIES;
        }

        @ForceInline
        @Override
        final VectorPayloadMF getBits() {
            return payload;
        }

        @ForceInline
        @Override
        public final
        Int512Vector toVector() {
            return (Int512Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Int512Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Int512Mask) VectorSupport.indexPartiallyInUpperRange(
                Int512Mask.class, int.class, VLENGTH, offset, limit,
                (o, l) -> (Int512Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Int512Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Int512Mask compress() {
            return (Int512Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Int512Vector.class, Int512Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Int512Mask and(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int512Mask m = (Int512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Int512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int512Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Int512Mask or(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int512Mask m = (Int512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Int512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int512Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Int512Mask xor(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int512Mask m = (Int512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Int512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int512Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Int512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int512Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Int512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int512Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Int512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int512Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Int512Mask.class, int.class, VLENGTH, this,
                                                      (m) -> ((Int512Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Int512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Int512Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Int512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Int512Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Int512Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Int512Mask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Int512Mask  TRUE_MASK = new Int512Mask(true);
        private static final Int512Mask FALSE_MASK = new Int512Mask(false);

    }

    // Shuffle

    static final value class Int512Shuffle extends AbstractShuffle<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF512I payload;

        Int512Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF512I) payload;
            assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Int512Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Int512Shuffle(IntUnaryOperator fn) {
            this(prepare(fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        @ForceInline
        public IntSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Integer.MAX_VALUE);
            assert(Integer.MIN_VALUE <= -VLENGTH);
        }
        static final Int512Shuffle IOTA = new Int512Shuffle(IDENTITY);

        @Override
        @ForceInline
        Int512Vector toBitsVector() {
            return (Int512Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        IntVector toBitsVector0() {
            return Int512Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            toBitsVector().intoArray(a, offset);
        }

        private static VectorPayloadMF prepare(int[] indices, int offset) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(int.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long mfOffset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = indices[offset + i];
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putInt(payload, mfOffset + i * Integer.BYTES, (int) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }

        private static VectorPayloadMF prepare(IntUnaryOperator f) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(int.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long offset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = f.applyAsInt(i);
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putInt(payload, offset + i * Integer.BYTES, (int) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }


        private static boolean indicesInRange(VectorPayloadMF indices) {
            int length = indices.length();
            long offset = indices.multiFieldOffset();
            for (int i = 0; i < length; i++) {
                int si = Unsafe.getUnsafe().getInt(indices, offset + i * Integer.BYTES);
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

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset, VectorMask<Integer> m, int offsetInRange) {
        return super.fromArray0Template(Int512Mask.class, a, offset, (Int512Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        return super.fromArray0Template(Int512Mask.class, a, offset, indexMap, mapOffset, (Int512Mask) m);
    }



    @ForceInline
    @Override
    final
    IntVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Integer> m, int offsetInRange) {
        return super.fromMemorySegment0Template(Int512Mask.class, ms, offset, (Int512Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset, VectorMask<Integer> m) {
        super.intoArray0Template(Int512Mask.class, a, offset, (Int512Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        super.intoArray0Template(Int512Mask.class, a, offset, indexMap, mapOffset, (Int512Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Integer> m) {
        super.intoMemorySegment0Template(Int512Mask.class, ms, offset, (Int512Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

