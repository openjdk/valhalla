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
value class DoubleMaxVector extends DoubleVector {
    static final DoubleSpecies VSPECIES =
        (DoubleSpecies) DoubleVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<DoubleMaxVector> VCLASS = DoubleMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Double> ETYPE = double.class; // used by the JVM

    static final Unsafe U = Unsafe.getUnsafe();

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxD.class);

    private final VectorPayloadMFMaxD payload;

    DoubleMaxVector(Object value) {
        this.payload = (VectorPayloadMFMaxD) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final DoubleMaxVector ZERO = new DoubleMaxVector(VectorPayloadMF.newVectorInstanceFactory(double.class, 0, true));
    static final DoubleMaxVector IOTA = new DoubleMaxVector(VectorPayloadMF.createVectPayloadInstanceD(VLENGTH, (double[])(VSPECIES.iotaArray()), true));

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
    public DoubleSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Double> elementType() { return double.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Double.SIZE; }

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
    public final DoubleMaxVector broadcast(double e) {
        return (DoubleMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxVector broadcast(long e) {
        return (DoubleMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    DoubleMaxMask maskFromPayload(VectorPayloadMF payload) {
        return new DoubleMaxMask(payload);
    }

    @Override
    @ForceInline
    DoubleMaxShuffle iotaShuffle() { return DoubleMaxShuffle.IOTA; }

    @ForceInline
    DoubleMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (DoubleMaxShuffle)VectorSupport.shuffleIota(ETYPE, DoubleMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (DoubleMaxShuffle)VectorSupport.shuffleIota(ETYPE, DoubleMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromBytes(VectorPayloadMF indexes) { return new DoubleMaxShuffle(indexes); }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromArray(int[] indexes, int i) { return new DoubleMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new DoubleMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    DoubleMaxVector vectorFactory(VectorPayloadMF vec) {
        return new DoubleMaxVector(vec);
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
    DoubleMaxVector uOpMF(FUnOp f) {
        return (DoubleMaxVector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector uOpMF(VectorMask<Double> m, FUnOp f) {
        return (DoubleMaxVector)
            super.uOpTemplateMF((DoubleMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    DoubleMaxVector bOpMF(Vector<Double> v, FBinOp f) {
        return (DoubleMaxVector) super.bOpTemplateMF((DoubleMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector bOpMF(Vector<Double> v,
                     VectorMask<Double> m, FBinOp f) {
        return (DoubleMaxVector)
            super.bOpTemplateMF((DoubleMaxVector)v, (DoubleMaxMask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    DoubleMaxVector tOpMF(Vector<Double> v1, Vector<Double> v2, FTriOp f) {
        return (DoubleMaxVector)
            super.tOpTemplateMF((DoubleMaxVector)v1, (DoubleMaxVector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector tOpMF(Vector<Double> v1, Vector<Double> v2,
                     VectorMask<Double> m, FTriOp f) {
        return (DoubleMaxVector)
            super.tOpTemplateMF((DoubleMaxVector)v1, (DoubleMaxVector)v2,
                                (DoubleMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    double rOpMF(double v, VectorMask<Double> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Double,F> conv,
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
    public DoubleMaxVector lanewise(Unary op) {
        return (DoubleMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector lanewise(Unary op, VectorMask<Double> m) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, DoubleMaxMask.class, (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector lanewise(Binary op, Vector<Double> v) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector lanewise(Binary op, Vector<Double> v, VectorMask<Double> m) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, DoubleMaxMask.class, v, (DoubleMaxMask) m);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    DoubleMaxVector
    lanewise(Ternary op, Vector<Double> v1, Vector<Double> v2) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    DoubleMaxVector
    lanewise(Ternary op, Vector<Double> v1, Vector<Double> v2, VectorMask<Double> m) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, DoubleMaxMask.class, v1, v2, (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    DoubleMaxVector addIndex(int scale) {
        return (DoubleMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final double reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final double reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Double> m) {
        return super.reduceLanesTemplate(op, DoubleMaxMask.class, (DoubleMaxMask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Double> m) {
        return (long) super.reduceLanesTemplate(op, DoubleMaxMask.class, (DoubleMaxMask) m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Double> toShuffle() {
        return super.toShuffleTemplate(DoubleMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final DoubleMaxMask test(Test op) {
        return super.testTemplate(DoubleMaxMask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask test(Test op, VectorMask<Double> m) {
        return super.testTemplate(DoubleMaxMask.class, op, (DoubleMaxMask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, Vector<Double> v) {
        return super.compareTemplate(DoubleMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, double s) {
        return super.compareTemplate(DoubleMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(DoubleMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, Vector<Double> v, VectorMask<Double> m) {
        return super.compareTemplate(DoubleMaxMask.class, op, v, (DoubleMaxMask) m);
    }


    @Override
    @ForceInline
    public DoubleMaxVector blend(Vector<Double> v, VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.blendTemplate(DoubleMaxMask.class,
                                (DoubleMaxVector) v,
                                (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector slice(int origin, Vector<Double> v) {
        return (DoubleMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector slice(int origin) {
        return (DoubleMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin, Vector<Double> w, int part) {
        return (DoubleMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin, Vector<Double> w, int part, VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.unsliceTemplate(DoubleMaxMask.class,
                                  origin, w, part,
                                  (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin) {
        return (DoubleMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> s) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    (DoubleMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> shuffle,
                                  VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    DoubleMaxMask.class,
                                    (DoubleMaxShuffle) shuffle,
                                    (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> s,
                                  Vector<Double> v) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    (DoubleMaxShuffle) s,
                                    (DoubleMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector compress(VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.compressTemplate(DoubleMaxMask.class,
                                   (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector expand(VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.expandTemplate(DoubleMaxMask.class,
                                   (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector selectFrom(Vector<Double> v) {
        return (DoubleMaxVector)
            super.selectFromTemplate((DoubleMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector selectFrom(Vector<Double> v,
                                   VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.selectFromTemplate((DoubleMaxVector) v,
                                     (DoubleMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public double lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        long bits = laneHelper(i);
        return Double.longBitsToDouble(bits);
    }

    public long laneHelper(int i) {
        return (long) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                         VectorPayloadMF vecpayload = vec.vec();
                         long start_offset = vecpayload.multiFieldOffset();
                         return (long)Double.doubleToLongBits(U.getDouble(vecpayload, start_offset + ix * Double.BYTES));
                     });
    }

    @ForceInline
    @Override
    public DoubleMaxVector withLane(int i, double e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public DoubleMaxVector withLaneHelper(int i, double e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Double.doubleToLongBits(e),
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = U.makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    U.putDouble(tpayload, start_offset + ix * Double.BYTES, Double.longBitsToDouble((long)bits));
                                    tpayload = U.finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class DoubleMaxMask extends AbstractMask<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        DoubleMaxMask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxLZ) payload;
        }

        private final VectorPayloadMFMaxLZ payload;

        DoubleMaxMask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        DoubleMaxMask(boolean val) {
            this(prepare(val, VSPECIES));
        }


        @ForceInline
        final @Override
        public DoubleSpecies vspecies() {
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
        DoubleMaxVector toVector() {
            return (DoubleMaxVector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        DoubleMaxMask indexPartiallyInUpperRange(long offset, long limit) {
            return (DoubleMaxMask) VectorSupport.indexPartiallyInUpperRange(
                DoubleMaxMask.class, double.class, VLENGTH, offset, limit,
                (o, l) -> (DoubleMaxMask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public DoubleMaxMask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public DoubleMaxMask compress() {
            return (DoubleMaxMask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                DoubleMaxVector.class, DoubleMaxMask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public DoubleMaxMask and(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, DoubleMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (DoubleMaxMask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public DoubleMaxMask or(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, DoubleMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (DoubleMaxMask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public DoubleMaxMask xor(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, DoubleMaxMask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (DoubleMaxMask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((DoubleMaxMask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((DoubleMaxMask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                            (m) -> ((DoubleMaxMask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> ((DoubleMaxMask) m).toLongHelper());
        }

        // laneIsSet

        @Override
        @ForceInline
        public boolean laneIsSet(int i) {
            Objects.checkIndex(i, length());
            return VectorSupport.extract(DoubleMaxMask.class, double.class, VLENGTH,
                                         this, i, (m, idx) -> (((DoubleMaxMask) m).laneIsSetHelper(idx) ? 1L : 0L)) == 1L;
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, DoubleMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((DoubleMaxMask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, DoubleMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((DoubleMaxMask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static DoubleMaxMask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(DoubleMaxMask.class, long.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final DoubleMaxMask  TRUE_MASK = new DoubleMaxMask(true);
        private static final DoubleMaxMask FALSE_MASK = new DoubleMaxMask(false);

    }

    // Shuffle

    static final value class DoubleMaxShuffle extends AbstractShuffle<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        private final VectorPayloadMFMaxLB payload;

        DoubleMaxShuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxLB) payload;
            assert(VLENGTH == payload.length());
            assert(indexesInRange(payload));
        }

        public DoubleMaxShuffle(int[] indexes, int i) {
            this(prepare(indexes, i, VSPECIES));
        }

        public DoubleMaxShuffle(IntUnaryOperator fn) {
            this(prepare(fn, VSPECIES));
        }

        public DoubleMaxShuffle(int[] indexes) {
            this(indexes, 0);
        }


        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        public DoubleSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final DoubleMaxShuffle IOTA = new DoubleMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public DoubleMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, DoubleMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((DoubleMaxVector)(((AbstractShuffle<Double>)(s)).toVectorTemplate())));
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
        public DoubleMaxShuffle rearrange(VectorShuffle<Double> shuffle) {
            DoubleMaxShuffle s = (DoubleMaxShuffle) shuffle;
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
            return new DoubleMaxShuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset, VectorMask<Double> m, int offsetInRange) {
        return super.fromArray0Template(DoubleMaxMask.class, a, offset, (DoubleMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Double> m) {
        return super.fromArray0Template(DoubleMaxMask.class, a, offset, indexMap, mapOffset, (DoubleMaxMask) m);
    }



    @ForceInline
    @Override
    final
    DoubleVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Double> m, int offsetInRange) {
        return super.fromMemorySegment0Template(DoubleMaxMask.class, ms, offset, (DoubleMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(double[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(double[] a, int offset, VectorMask<Double> m) {
        super.intoArray0Template(DoubleMaxMask.class, a, offset, (DoubleMaxMask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(double[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Double> m) {
        super.intoArray0Template(DoubleMaxMask.class, a, offset, indexMap, mapOffset, (DoubleMaxMask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Double> m) {
        super.intoMemorySegment0Template(DoubleMaxMask.class, ms, offset, (DoubleMaxMask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

