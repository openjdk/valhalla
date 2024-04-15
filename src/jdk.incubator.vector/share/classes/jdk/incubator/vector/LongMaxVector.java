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
import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.vector.VectorSupport;

import static jdk.internal.vm.vector.VectorSupport.*;

import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

@SuppressWarnings("cast")  // warning: redundant cast
value class LongMaxVector extends LongVector {
    static final LongSpecies VSPECIES =
        (LongSpecies) LongVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<LongMaxVector> VCLASS = LongMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Long> ETYPE = long.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxL.class);

    @NullRestricted
    private final VectorPayloadMFMaxL payload;

    LongMaxVector(Object value) {
        this.payload = (VectorPayloadMFMaxL) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final LongMaxVector ZERO = new LongMaxVector(VectorPayloadMF.newVectorInstanceFactory(long.class, 0, true));
    static final LongMaxVector IOTA = new LongMaxVector(VectorPayloadMF.createVectPayloadInstanceL(VLENGTH, (long[])(VSPECIES.iotaArray()), true));

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
    public LongSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Long> elementType() { return long.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Long.SIZE; }

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
    public final LongMaxVector broadcast(long e) {
        return (LongMaxVector) super.broadcastTemplate(e);  // specialize
    }


    @Override
    @ForceInline
    LongMaxMask maskFromPayload(VectorPayloadMF payload) {
        return new LongMaxMask(payload);
    }

    @Override
    @ForceInline
    LongMaxShuffle iotaShuffle() { return LongMaxShuffle.IOTA; }

    @ForceInline
    LongMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (LongMaxShuffle)VectorSupport.shuffleIota(ETYPE, LongMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (LongMaxShuffle)VectorSupport.shuffleIota(ETYPE, LongMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromBytes(VectorPayloadMF indexes) { return new LongMaxShuffle(indexes); }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromArray(int[] indexes, int i) { return new LongMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new LongMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    LongMaxVector vectorFactory(VectorPayloadMF vec) {
        return new LongMaxVector(vec);
    }

    @ForceInline
    final @Override
    ByteMaxVector asByteVectorRaw() {
        return (ByteMaxVector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    LongMaxVector uOpMF(FUnOp f) {
        return (LongMaxVector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector uOpMF(VectorMask<Long> m, FUnOp f) {
        return (LongMaxVector)
            super.uOpTemplateMF((LongMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    LongMaxVector bOpMF(Vector<Long> v, FBinOp f) {
        return (LongMaxVector) super.bOpTemplateMF((LongMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector bOpMF(Vector<Long> v,
                     VectorMask<Long> m, FBinOp f) {
        return (LongMaxVector)
            super.bOpTemplateMF((LongMaxVector)v, (LongMaxMask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    LongMaxVector tOpMF(Vector<Long> v1, Vector<Long> v2, FTriOp f) {
        return (LongMaxVector)
            super.tOpTemplateMF((LongMaxVector)v1, (LongMaxVector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector tOpMF(Vector<Long> v1, Vector<Long> v2,
                     VectorMask<Long> m, FTriOp f) {
        return (LongMaxVector)
            super.tOpTemplateMF((LongMaxVector)v1, (LongMaxVector)v2,
                                (LongMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    long rOpMF(long v, VectorMask<Long> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Long,F> conv,
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
    public LongMaxVector lanewise(Unary op) {
        return (LongMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector lanewise(Unary op, VectorMask<Long> m) {
        return (LongMaxVector) super.lanewiseTemplate(op, LongMaxMask.class, (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector lanewise(Binary op, Vector<Long> v) {
        return (LongMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector lanewise(Binary op, Vector<Long> v, VectorMask<Long> m) {
        return (LongMaxVector) super.lanewiseTemplate(op, LongMaxMask.class, v, (LongMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline LongMaxVector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (LongMaxVector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline LongMaxVector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Long> m) {
        return (LongMaxVector) super.lanewiseShiftTemplate(op, LongMaxMask.class, e, (LongMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    LongMaxVector
    lanewise(Ternary op, Vector<Long> v1, Vector<Long> v2) {
        return (LongMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    LongMaxVector
    lanewise(Ternary op, Vector<Long> v1, Vector<Long> v2, VectorMask<Long> m) {
        return (LongMaxVector) super.lanewiseTemplate(op, LongMaxMask.class, v1, v2, (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    LongMaxVector addIndex(int scale) {
        return (LongMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final long reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Long> m) {
        return super.reduceLanesTemplate(op, LongMaxMask.class, (LongMaxMask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Long> m) {
        return (long) super.reduceLanesTemplate(op, LongMaxMask.class, (LongMaxMask) m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Long> toShuffle() {
        return super.toShuffleTemplate(LongMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final LongMaxMask test(Test op) {
        return super.testTemplate(LongMaxMask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final LongMaxMask test(Test op, VectorMask<Long> m) {
        return super.testTemplate(LongMaxMask.class, op, (LongMaxMask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final LongMaxMask compare(Comparison op, Vector<Long> v) {
        return super.compareTemplate(LongMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final LongMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(LongMaxMask.class, op, s);  // specialize
    }


    @Override
    @ForceInline
    public final LongMaxMask compare(Comparison op, Vector<Long> v, VectorMask<Long> m) {
        return super.compareTemplate(LongMaxMask.class, op, v, (LongMaxMask) m);
    }


    @Override
    @ForceInline
    public LongMaxVector blend(Vector<Long> v, VectorMask<Long> m) {
        return (LongMaxVector)
            super.blendTemplate(LongMaxMask.class,
                                (LongMaxVector) v,
                                (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector slice(int origin, Vector<Long> v) {
        return (LongMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector slice(int origin) {
        return (LongMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin, Vector<Long> w, int part) {
        return (LongMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin, Vector<Long> w, int part, VectorMask<Long> m) {
        return (LongMaxVector)
            super.unsliceTemplate(LongMaxMask.class,
                                  origin, w, part,
                                  (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin) {
        return (LongMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> s) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    (LongMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> shuffle,
                                  VectorMask<Long> m) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    LongMaxMask.class,
                                    (LongMaxShuffle) shuffle,
                                    (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> s,
                                  Vector<Long> v) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    (LongMaxShuffle) s,
                                    (LongMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector compress(VectorMask<Long> m) {
        return (LongMaxVector)
            super.compressTemplate(LongMaxMask.class,
                                   (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector expand(VectorMask<Long> m) {
        return (LongMaxVector)
            super.expandTemplate(LongMaxMask.class,
                                   (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector selectFrom(Vector<Long> v) {
        return (LongMaxVector)
            super.selectFromTemplate((LongMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector selectFrom(Vector<Long> v,
                                   VectorMask<Long> m) {
        return (LongMaxVector)
            super.selectFromTemplate((LongMaxVector) v,
                                     (LongMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public long lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return laneHelper(i);
    }

    public long laneHelper(int i) {
        return (long) VectorSupport.extract(
                             VCLASS, ETYPE, VLENGTH,
                             this, i,
                             (vec, ix) -> {
                                 VectorPayloadMF vecpayload = vec.vec();
                                 long start_offset = vecpayload.multiFieldOffset();
                                 return (long)U.getLong(vecpayload, start_offset + ix * Long.BYTES);
                             });
    }

    @ForceInline
    @Override
    public LongMaxVector withLane(int i, long e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public LongMaxVector withLaneHelper(int i, long e) {
       return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = U.makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    U.putLong(tpayload, start_offset + ix * Long.BYTES, (long)bits);
                                    tpayload = U.finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class LongMaxMask extends AbstractMask<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        LongMaxMask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxLZ) payload;
        }

        @NullRestricted
        private final VectorPayloadMFMaxLZ payload;

        LongMaxMask(VectorPayloadMF payload, int offset) {
            this.payload = (VectorPayloadMFMaxLZ)(prepare(payload, offset, VSPECIES));
        }

        LongMaxMask(boolean val) {
            this.payload = (VectorPayloadMFMaxLZ)(prepare(val, VSPECIES));
        }


        @ForceInline
        final @Override
        public LongSpecies vspecies() {
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
        LongMaxVector toVector() {
            return (LongMaxVector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        LongMaxMask indexPartiallyInUpperRange(long offset, long limit) {
            return (LongMaxMask) VectorSupport.indexPartiallyInUpperRange(
                LongMaxMask.class, long.class, VLENGTH, offset, limit,
                (o, l) -> (LongMaxMask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public LongMaxMask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public LongMaxMask compress() {
            return (LongMaxMask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                LongMaxVector.class, LongMaxMask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public LongMaxMask and(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, LongMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (LongMaxMask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public LongMaxMask or(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, LongMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (LongMaxMask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public LongMaxMask xor(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, LongMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (LongMaxMask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, LongMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((LongMaxMask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, LongMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((LongMaxMask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, LongMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((LongMaxMask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, LongMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> ((LongMaxMask) m).toLongHelper());
        }

        // laneIsSet

        @Override
        @ForceInline
        public boolean laneIsSet(int i) {
            Objects.checkIndex(i, length());
            return VectorSupport.extract(LongMaxMask.class, long.class, VLENGTH,
                                         this, i, (m, idx) -> (((LongMaxMask) m).laneIsSetHelper(idx) ? 1L : 0L)) == 1L;
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, LongMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((LongMaxMask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, LongMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((LongMaxMask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static LongMaxMask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(LongMaxMask.class, long.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final LongMaxMask  TRUE_MASK = new LongMaxMask(true);
        private static final LongMaxMask FALSE_MASK = new LongMaxMask(false);

    }

    // Shuffle

    static final value class LongMaxShuffle extends AbstractShuffle<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        @NullRestricted
        private final VectorPayloadMFMaxLB payload;

        LongMaxShuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxLB) payload;
            assert(VLENGTH == payload.length());
            assert(indexesInRange(payload));
        }

        public LongMaxShuffle(int[] indexes, int i) {
            this.payload = (VectorPayloadMFMaxLB)(prepare(indexes, i, VSPECIES));
        }

        public LongMaxShuffle(IntUnaryOperator fn) {
            this.payload = (VectorPayloadMFMaxLB)(prepare(fn, VSPECIES));
        }
        public LongMaxShuffle(int[] indexes) {
            this.payload = (VectorPayloadMFMaxLB)(prepare(indexes, 0, VSPECIES));
        }



        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        public LongSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final LongMaxShuffle IOTA = new LongMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public LongMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, LongMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((LongMaxVector)(((AbstractShuffle<Long>)(s)).toVectorTemplate())));
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
        public LongMaxShuffle rearrange(VectorShuffle<Long> shuffle) {
            LongMaxShuffle s = (LongMaxShuffle) shuffle;
            VectorPayloadMF indices1 = indices();
            VectorPayloadMF indices2 = s.indices();
            VectorPayloadMF r = VectorPayloadMF.newShuffleInstanceFactory(ETYPE, VLENGTH, true);
            r = U.makePrivateBuffer(r);
            long offset = r.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int ssi = U.getByte(indices2, offset + i * Byte.BYTES);
                int si = U.getByte(indices1, offset + ssi * Byte.BYTES);
                U.putByte(r, offset + i * Byte.BYTES, (byte) si);
            }
            r = U.finishPrivateBuffer(r);
            return new LongMaxShuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    LongVector fromArray0(long[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    LongVector fromArray0(long[] a, int offset, VectorMask<Long> m, int offsetInRange) {
        return super.fromArray0Template(LongMaxMask.class, a, offset, (LongMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    LongVector fromArray0(long[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Long> m) {
        return super.fromArray0Template(LongMaxMask.class, a, offset, indexMap, mapOffset, (LongMaxMask) m);
    }



    @ForceInline
    @Override
    final
    LongVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    LongVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Long> m, int offsetInRange) {
        return super.fromMemorySegment0Template(LongMaxMask.class, ms, offset, (LongMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(long[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(long[] a, int offset, VectorMask<Long> m) {
        super.intoArray0Template(LongMaxMask.class, a, offset, (LongMaxMask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(long[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Long> m) {
        super.intoArray0Template(LongMaxMask.class, a, offset, indexMap, mapOffset, (LongMaxMask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Long> m) {
        super.intoMemorySegment0Template(LongMaxMask.class, ms, offset, (LongMaxMask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

