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
value class FloatMaxVector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<FloatMaxVector> VCLASS = FloatMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxF.class);

    private final VectorPayloadMFMaxF payload;

    FloatMaxVector(Object value) {
        this.payload = (VectorPayloadMFMaxF) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final FloatMaxVector ZERO = new FloatMaxVector(VectorPayloadMF.newVectorInstanceFactory(float.class, 0, true));
    static final FloatMaxVector IOTA = new FloatMaxVector(VectorPayloadMF.createVectPayloadInstanceF(VLENGTH, (float[])(VSPECIES.iotaArray()), true));

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
    public FloatSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Float> elementType() { return float.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Float.SIZE; }

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
    public final FloatMaxVector broadcast(float e) {
        return (FloatMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxVector broadcast(long e) {
        return (FloatMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    FloatMaxMask maskFromPayload(VectorPayloadMF payload) {
        return new FloatMaxMask(payload);
    }

    @Override
    @ForceInline
    FloatMaxShuffle iotaShuffle() { return FloatMaxShuffle.IOTA; }

    @ForceInline
    FloatMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (FloatMaxShuffle)VectorSupport.shuffleIota(ETYPE, FloatMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (FloatMaxShuffle)VectorSupport.shuffleIota(ETYPE, FloatMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromBytes(VectorPayloadMF indexes) { return new FloatMaxShuffle(indexes); }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromArray(int[] indexes, int i) { return new FloatMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new FloatMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    FloatMaxVector vectorFactory(VectorPayloadMF vec) {
        return new FloatMaxVector(vec);
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
    FloatMaxVector uOpMF(FUnOp f) {
        return (FloatMaxVector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector uOpMF(VectorMask<Float> m, FUnOp f) {
        return (FloatMaxVector)
            super.uOpTemplateMF((FloatMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    FloatMaxVector bOpMF(Vector<Float> v, FBinOp f) {
        return (FloatMaxVector) super.bOpTemplateMF((FloatMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector bOpMF(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (FloatMaxVector)
            super.bOpTemplateMF((FloatMaxVector)v, (FloatMaxMask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    FloatMaxVector tOpMF(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (FloatMaxVector)
            super.tOpTemplateMF((FloatMaxVector)v1, (FloatMaxVector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector tOpMF(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (FloatMaxVector)
            super.tOpTemplateMF((FloatMaxVector)v1, (FloatMaxVector)v2,
                                (FloatMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    float rOpMF(float v, VectorMask<Float> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Float,F> conv,
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
    public FloatMaxVector lanewise(Unary op) {
        return (FloatMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector lanewise(Unary op, VectorMask<Float> m) {
        return (FloatMaxVector) super.lanewiseTemplate(op, FloatMaxMask.class, (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector lanewise(Binary op, Vector<Float> v) {
        return (FloatMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector lanewise(Binary op, Vector<Float> v, VectorMask<Float> m) {
        return (FloatMaxVector) super.lanewiseTemplate(op, FloatMaxMask.class, v, (FloatMaxMask) m);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    FloatMaxVector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (FloatMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    FloatMaxVector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2, VectorMask<Float> m) {
        return (FloatMaxVector) super.lanewiseTemplate(op, FloatMaxMask.class, v1, v2, (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    FloatMaxVector addIndex(int scale) {
        return (FloatMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Float> m) {
        return super.reduceLanesTemplate(op, FloatMaxMask.class, (FloatMaxMask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Float> m) {
        return (long) super.reduceLanesTemplate(op, FloatMaxMask.class, (FloatMaxMask) m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Float> toShuffle() {
        return super.toShuffleTemplate(FloatMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final FloatMaxMask test(Test op) {
        return super.testTemplate(FloatMaxMask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask test(Test op, VectorMask<Float> m) {
        return super.testTemplate(FloatMaxMask.class, op, (FloatMaxMask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(FloatMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, float s) {
        return super.compareTemplate(FloatMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(FloatMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, Vector<Float> v, VectorMask<Float> m) {
        return super.compareTemplate(FloatMaxMask.class, op, v, (FloatMaxMask) m);
    }


    @Override
    @ForceInline
    public FloatMaxVector blend(Vector<Float> v, VectorMask<Float> m) {
        return (FloatMaxVector)
            super.blendTemplate(FloatMaxMask.class,
                                (FloatMaxVector) v,
                                (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector slice(int origin, Vector<Float> v) {
        return (FloatMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector slice(int origin) {
        return (FloatMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin, Vector<Float> w, int part) {
        return (FloatMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (FloatMaxVector)
            super.unsliceTemplate(FloatMaxMask.class,
                                  origin, w, part,
                                  (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin) {
        return (FloatMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> s) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    (FloatMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    FloatMaxMask.class,
                                    (FloatMaxShuffle) shuffle,
                                    (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    (FloatMaxShuffle) s,
                                    (FloatMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector compress(VectorMask<Float> m) {
        return (FloatMaxVector)
            super.compressTemplate(FloatMaxMask.class,
                                   (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector expand(VectorMask<Float> m) {
        return (FloatMaxVector)
            super.expandTemplate(FloatMaxMask.class,
                                   (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector selectFrom(Vector<Float> v) {
        return (FloatMaxVector)
            super.selectFromTemplate((FloatMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (FloatMaxVector)
            super.selectFromTemplate((FloatMaxVector) v,
                                     (FloatMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public float lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        int bits = laneHelper(i);
        return Float.intBitsToFloat(bits);
    }

    public int laneHelper(int i) {
        return (int) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                         VectorPayloadMF vecpayload = vec.vec();
                         long start_offset = vecpayload.multiFieldOffset();
                         return (long)Float.floatToIntBits(U.getFloat(vecpayload, start_offset + ix * Float.BYTES));
                     });
    }

    @ForceInline
    @Override
    public FloatMaxVector withLane(int i, float e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public FloatMaxVector withLaneHelper(int i, float e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = U.makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    U.putFloat(tpayload, start_offset + ix * Float.BYTES, Float.intBitsToFloat((int)bits));
                                    tpayload = U.finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class FloatMaxMask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        FloatMaxMask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxIZ) payload;
        }

        private final VectorPayloadMFMaxIZ payload;

        FloatMaxMask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        FloatMaxMask(boolean val) {
            this(prepare(val, VSPECIES));
        }


        @ForceInline
        final @Override
        public FloatSpecies vspecies() {
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
        FloatMaxVector toVector() {
            return (FloatMaxVector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        FloatMaxMask indexPartiallyInUpperRange(long offset, long limit) {
            return (FloatMaxMask) VectorSupport.indexPartiallyInUpperRange(
                FloatMaxMask.class, float.class, VLENGTH, offset, limit,
                (o, l) -> (FloatMaxMask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public FloatMaxMask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public FloatMaxMask compress() {
            return (FloatMaxMask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                FloatMaxVector.class, FloatMaxMask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public FloatMaxMask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, FloatMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (FloatMaxMask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public FloatMaxMask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, FloatMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (FloatMaxMask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public FloatMaxMask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, FloatMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (FloatMaxMask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, FloatMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((FloatMaxMask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, FloatMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((FloatMaxMask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, FloatMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((FloatMaxMask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, FloatMaxMask.class, int.class, VLENGTH, this,
                                                      (m) -> ((FloatMaxMask) m).toLongHelper());
        }

        // laneIsSet

        @Override
        @ForceInline
        public boolean laneIsSet(int i) {
            Objects.checkIndex(i, length());
            return VectorSupport.extract(FloatMaxMask.class, float.class, VLENGTH,
                                         this, i, (m, idx) -> (((FloatMaxMask) m).laneIsSetHelper(idx) ? 1L : 0L)) == 1L;
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, FloatMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((FloatMaxMask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, FloatMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((FloatMaxMask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static FloatMaxMask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(FloatMaxMask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final FloatMaxMask  TRUE_MASK = new FloatMaxMask(true);
        private static final FloatMaxMask FALSE_MASK = new FloatMaxMask(false);

    }

    // Shuffle

    static final value class FloatMaxShuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        private final VectorPayloadMFMaxIB payload;

        FloatMaxShuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxIB) payload;
            assert(VLENGTH == payload.length());
            assert(indexesInRange(payload));
        }

        public FloatMaxShuffle(int[] indexes, int i) {
            this(prepare(indexes, i, VSPECIES));
        }

        public FloatMaxShuffle(IntUnaryOperator fn) {
            this(prepare(fn, VSPECIES));
        }

        public FloatMaxShuffle(int[] indexes) {
            this(indexes, 0);
        }


        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        public FloatSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final FloatMaxShuffle IOTA = new FloatMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public FloatMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, FloatMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((FloatMaxVector)(((AbstractShuffle<Float>)(s)).toVectorTemplate())));
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
        public FloatMaxShuffle rearrange(VectorShuffle<Float> shuffle) {
            FloatMaxShuffle s = (FloatMaxShuffle) shuffle;
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
            return new FloatMaxShuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, VectorMask<Float> m, int offsetInRange) {
        return super.fromArray0Template(FloatMaxMask.class, a, offset, (FloatMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        return super.fromArray0Template(FloatMaxMask.class, a, offset, indexMap, mapOffset, (FloatMaxMask) m);
    }



    @ForceInline
    @Override
    final
    FloatVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Float> m, int offsetInRange) {
        return super.fromMemorySegment0Template(FloatMaxMask.class, ms, offset, (FloatMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset, VectorMask<Float> m) {
        super.intoArray0Template(FloatMaxMask.class, a, offset, (FloatMaxMask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        super.intoArray0Template(FloatMaxMask.class, a, offset, indexMap, mapOffset, (FloatMaxMask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Float> m) {
        super.intoMemorySegment0Template(FloatMaxMask.class, ms, offset, (FloatMaxMask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

