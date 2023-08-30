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
value class Int128Vector extends IntVector {
    static final IntSpecies VSPECIES =
        (IntSpecies) IntVector.SPECIES_128;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Int128Vector> VCLASS = Int128Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Integer> ETYPE = int.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF128I.class);

    private final VectorPayloadMF128I payload;

    Int128Vector(Object value) {
        this.payload = (VectorPayloadMF128I) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Int128Vector ZERO = new Int128Vector(VectorPayloadMF.newInstanceFactory(int.class, 4));
    static final Int128Vector IOTA = new Int128Vector(VectorPayloadMF.createVectPayloadInstanceI(4, (int[])(VSPECIES.iotaArray())));

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
    public final Int128Vector broadcast(int e) {
        return (Int128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Int128Vector broadcast(long e) {
        return (Int128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Int128Mask maskFromPayload(VectorPayloadMF payload) {
        return new Int128Mask(payload);
    }

    @Override
    @ForceInline
    Int128Shuffle iotaShuffle() { return Int128Shuffle.IOTA; }

    @ForceInline
    Int128Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Int128Shuffle)VectorSupport.shuffleIota(ETYPE, Int128Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Int128Shuffle)VectorSupport.shuffleIota(ETYPE, Int128Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Int128Shuffle shuffleFromBytes(VectorPayloadMF indexes) { return new Int128Shuffle(indexes); }

    @Override
    @ForceInline
    Int128Shuffle shuffleFromArray(int[] indexes, int i) { return new Int128Shuffle(indexes, i); }

    @Override
    @ForceInline
    Int128Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Int128Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Int128Vector vectorFactory(VectorPayloadMF vec) {
        return new Int128Vector(vec);
    }

    @ForceInline
    final @Override
    Byte128Vector asByteVectorRaw() {
        return (Byte128Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Int128Vector uOpMF(FUnOp f) {
        return (Int128Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Int128Vector uOpMF(VectorMask<Integer> m, FUnOp f) {
        return (Int128Vector)
            super.uOpTemplateMF((Int128Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Int128Vector bOpMF(Vector<Integer> v, FBinOp f) {
        return (Int128Vector) super.bOpTemplateMF((Int128Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Int128Vector bOpMF(Vector<Integer> v,
                     VectorMask<Integer> m, FBinOp f) {
        return (Int128Vector)
            super.bOpTemplateMF((Int128Vector)v, (Int128Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Int128Vector tOpMF(Vector<Integer> v1, Vector<Integer> v2, FTriOp f) {
        return (Int128Vector)
            super.tOpTemplateMF((Int128Vector)v1, (Int128Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Int128Vector tOpMF(Vector<Integer> v1, Vector<Integer> v2,
                     VectorMask<Integer> m, FTriOp f) {
        return (Int128Vector)
            super.tOpTemplateMF((Int128Vector)v1, (Int128Vector)v2,
                                (Int128Mask)m, f);  // specialize
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
    public Int128Vector lanewise(Unary op) {
        return (Int128Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector lanewise(Unary op, VectorMask<Integer> m) {
        return (Int128Vector) super.lanewiseTemplate(op, Int128Mask.class, (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector lanewise(Binary op, Vector<Integer> v) {
        return (Int128Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector lanewise(Binary op, Vector<Integer> v, VectorMask<Integer> m) {
        return (Int128Vector) super.lanewiseTemplate(op, Int128Mask.class, v, (Int128Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Int128Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Int128Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Int128Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Integer> m) {
        return (Int128Vector) super.lanewiseShiftTemplate(op, Int128Mask.class, e, (Int128Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Int128Vector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2) {
        return (Int128Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Int128Vector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2, VectorMask<Integer> m) {
        return (Int128Vector) super.lanewiseTemplate(op, Int128Mask.class, v1, v2, (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Int128Vector addIndex(int scale) {
        return (Int128Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Int128Mask.class, (Int128Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Int128Mask.class, (Int128Mask) m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Integer> toShuffle() {
        return super.toShuffleTemplate(Int128Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Int128Mask test(Test op) {
        return super.testTemplate(Int128Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Int128Mask test(Test op, VectorMask<Integer> m) {
        return super.testTemplate(Int128Mask.class, op, (Int128Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Int128Mask compare(Comparison op, Vector<Integer> v) {
        return super.compareTemplate(Int128Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Int128Mask compare(Comparison op, int s) {
        return super.compareTemplate(Int128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Int128Mask compare(Comparison op, long s) {
        return super.compareTemplate(Int128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Int128Mask compare(Comparison op, Vector<Integer> v, VectorMask<Integer> m) {
        return super.compareTemplate(Int128Mask.class, op, v, (Int128Mask) m);
    }


    @Override
    @ForceInline
    public Int128Vector blend(Vector<Integer> v, VectorMask<Integer> m) {
        return (Int128Vector)
            super.blendTemplate(Int128Mask.class,
                                (Int128Vector) v,
                                (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector slice(int origin, Vector<Integer> v) {
        return (Int128Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector slice(int origin) {
        return (Int128Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector unslice(int origin, Vector<Integer> w, int part) {
        return (Int128Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector unslice(int origin, Vector<Integer> w, int part, VectorMask<Integer> m) {
        return (Int128Vector)
            super.unsliceTemplate(Int128Mask.class,
                                  origin, w, part,
                                  (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector unslice(int origin) {
        return (Int128Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector rearrange(VectorShuffle<Integer> s) {
        return (Int128Vector)
            super.rearrangeTemplate(Int128Shuffle.class,
                                    (Int128Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector rearrange(VectorShuffle<Integer> shuffle,
                                  VectorMask<Integer> m) {
        return (Int128Vector)
            super.rearrangeTemplate(Int128Shuffle.class,
                                    Int128Mask.class,
                                    (Int128Shuffle) shuffle,
                                    (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector rearrange(VectorShuffle<Integer> s,
                                  Vector<Integer> v) {
        return (Int128Vector)
            super.rearrangeTemplate(Int128Shuffle.class,
                                    (Int128Shuffle) s,
                                    (Int128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector compress(VectorMask<Integer> m) {
        return (Int128Vector)
            super.compressTemplate(Int128Mask.class,
                                   (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector expand(VectorMask<Integer> m) {
        return (Int128Vector)
            super.expandTemplate(Int128Mask.class,
                                   (Int128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector selectFrom(Vector<Integer> v) {
        return (Int128Vector)
            super.selectFromTemplate((Int128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int128Vector selectFrom(Vector<Integer> v,
                                   VectorMask<Integer> m) {
        return (Int128Vector)
            super.selectFromTemplate((Int128Vector) v,
                                     (Int128Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public int lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
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
    public Int128Vector withLane(int i, int e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Int128Vector withLaneHelper(int i, int e) {
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

    static final value class Int128Mask extends AbstractMask<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF32Z payload;

        Int128Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF32Z) payload;
        }

        Int128Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Int128Mask(boolean val) {
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
        Int128Vector toVector() {
            return (Int128Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Int128Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Int128Mask) VectorSupport.indexPartiallyInUpperRange(
                Int128Mask.class, int.class, VLENGTH, offset, limit,
                (o, l) -> (Int128Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Int128Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Int128Mask compress() {
            return (Int128Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Int128Vector.class, Int128Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Int128Mask and(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int128Mask m = (Int128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Int128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int128Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Int128Mask or(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int128Mask m = (Int128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Int128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int128Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Int128Mask xor(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int128Mask m = (Int128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Int128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Int128Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Int128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int128Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Int128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int128Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Int128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Int128Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Int128Mask.class, int.class, VLENGTH, this,
                                                      (m) -> ((Int128Mask) m).toLongHelper());
        }

        // laneIsSet

        @Override
        @ForceInline
        public boolean laneIsSet(int i) {
            Objects.checkIndex(i, length());
            return VectorSupport.extract(Int128Mask.class, int.class, VLENGTH,
                                         this, i, (m, idx) -> (((Int128Mask) m).laneIsSetHelper(idx) ? 1L : 0L)) == 1L;
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Int128Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Int128Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Int128Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Int128Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Int128Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Int128Mask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Int128Mask  TRUE_MASK = new Int128Mask(true);
        private static final Int128Mask FALSE_MASK = new Int128Mask(false);

    }

    // Shuffle

    static final value class Int128Shuffle extends AbstractShuffle<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF32B payload;

        Int128Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF32B) payload;
            assert(VLENGTH == payload.length());
            assert(indexesInRange(payload));
        }

        public Int128Shuffle(int[] indexes) {
            this(indexes, 0);
        }

        public Int128Shuffle(int[] indexes, int i) {
            this(prepare(VLENGTH, indexes, i));
        }

        public Int128Shuffle(IntUnaryOperator fn) {
            this(prepare(VLENGTH, fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        public IntSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Int128Shuffle IOTA = new Int128Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Int128Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Int128Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Int128Vector)(((AbstractShuffle<Integer>)(s)).toVectorTemplate())));
        }

        @Override
        @ForceInline
        public <F> VectorShuffle<F> cast(VectorSpecies<F> s) {
            AbstractSpecies<F> species = (AbstractSpecies<F>) s;
            if (length() != species.laneCount())
                throw new IllegalArgumentException("VectorShuffle length and species length differ");
            int[] shuffleArray = toArray();
            return s.shuffleFromArray(shuffleArray, 0).check(s);
        }

        @ForceInline
        @Override
        public Int128Shuffle rearrange(VectorShuffle<Integer> shuffle) {
            Int128Shuffle s = (Int128Shuffle) shuffle;
            VectorPayloadMF indices1 = indices();
            VectorPayloadMF indices2 = s.indices();
            VectorPayloadMF r = VectorPayloadMF.newInstanceFactory(byte.class, VLENGTH);
            r = Unsafe.getUnsafe().makePrivateBuffer(r);
            long offset = r.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int ssi = Unsafe.getUnsafe().getByte(indices2, offset + i * Byte.BYTES);
                int si = Unsafe.getUnsafe().getByte(indices1, offset + ssi * Byte.BYTES);
                Unsafe.getUnsafe().putByte(r, offset + i * Byte.BYTES, (byte) si);
            }
            r = Unsafe.getUnsafe().finishPrivateBuffer(r);
            return new Int128Shuffle(r);
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
        return super.fromArray0Template(Int128Mask.class, a, offset, (Int128Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        return super.fromArray0Template(Int128Mask.class, a, offset, indexMap, mapOffset, (Int128Mask) m);
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
        return super.fromMemorySegment0Template(Int128Mask.class, ms, offset, (Int128Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Int128Mask.class, a, offset, (Int128Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        super.intoArray0Template(Int128Mask.class, a, offset, indexMap, mapOffset, (Int128Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Integer> m) {
        super.intoMemorySegment0Template(Int128Mask.class, ms, offset, (Int128Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

