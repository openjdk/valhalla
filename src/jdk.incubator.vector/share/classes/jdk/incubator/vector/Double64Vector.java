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
value class Double64Vector extends DoubleVector {
    static final DoubleSpecies VSPECIES =
        (DoubleSpecies) DoubleVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Double64Vector> VCLASS = Double64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Double> ETYPE = double.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF64D.class);

    private final VectorPayloadMF64D payload;

    Double64Vector(Object value) {
        this.payload = (VectorPayloadMF64D) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Double64Vector ZERO = new Double64Vector(VectorPayloadMF.newInstanceFactory(double.class, 1));
    static final Double64Vector IOTA = new Double64Vector(VectorPayloadMF.createVectPayloadInstanceD(1, (double[])(VSPECIES.iotaArray())));

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
    public final Double64Vector broadcast(double e) {
        return (Double64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Double64Vector broadcast(long e) {
        return (Double64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Double64Mask maskFromPayload(VectorPayloadMF payload) {
        return new Double64Mask(payload);
    }

    @Override
    @ForceInline
    Double64Shuffle iotaShuffle() { return Double64Shuffle.IOTA; }

    @Override
    @ForceInline
    Double64Shuffle shuffleFromArray(int[] indices, int i) { return new Double64Shuffle(indices, i); }

    @Override
    @ForceInline
    Double64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Double64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Double64Vector vectorFactory(VectorPayloadMF vec) {
        return new Double64Vector(vec);
    }

    @ForceInline
    final @Override
    Byte64Vector asByteVectorRaw() {
        return (Byte64Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Double64Vector uOpMF(FUnOp f) {
        return (Double64Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Double64Vector uOpMF(VectorMask<Double> m, FUnOp f) {
        return (Double64Vector)
            super.uOpTemplateMF((Double64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Double64Vector bOpMF(Vector<Double> v, FBinOp f) {
        return (Double64Vector) super.bOpTemplateMF((Double64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Double64Vector bOpMF(Vector<Double> v,
                     VectorMask<Double> m, FBinOp f) {
        return (Double64Vector)
            super.bOpTemplateMF((Double64Vector)v, (Double64Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Double64Vector tOpMF(Vector<Double> v1, Vector<Double> v2, FTriOp f) {
        return (Double64Vector)
            super.tOpTemplateMF((Double64Vector)v1, (Double64Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Double64Vector tOpMF(Vector<Double> v1, Vector<Double> v2,
                     VectorMask<Double> m, FTriOp f) {
        return (Double64Vector)
            super.tOpTemplateMF((Double64Vector)v1, (Double64Vector)v2,
                                (Double64Mask)m, f);  // specialize
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
    public Double64Vector lanewise(Unary op) {
        return (Double64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector lanewise(Unary op, VectorMask<Double> m) {
        return (Double64Vector) super.lanewiseTemplate(op, Double64Mask.class, (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector lanewise(Binary op, Vector<Double> v) {
        return (Double64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector lanewise(Binary op, Vector<Double> v, VectorMask<Double> m) {
        return (Double64Vector) super.lanewiseTemplate(op, Double64Mask.class, v, (Double64Mask) m);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Double64Vector
    lanewise(Ternary op, Vector<Double> v1, Vector<Double> v2) {
        return (Double64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Double64Vector
    lanewise(Ternary op, Vector<Double> v1, Vector<Double> v2, VectorMask<Double> m) {
        return (Double64Vector) super.lanewiseTemplate(op, Double64Mask.class, v1, v2, (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Double64Vector addIndex(int scale) {
        return (Double64Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Double64Mask.class, (Double64Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Double64Mask.class, (Double64Mask) m);  // specialized
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
    public final Double64Mask test(Test op) {
        return super.testTemplate(Double64Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Double64Mask test(Test op, VectorMask<Double> m) {
        return super.testTemplate(Double64Mask.class, op, (Double64Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Double64Mask compare(Comparison op, Vector<Double> v) {
        return super.compareTemplate(Double64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Double64Mask compare(Comparison op, double s) {
        return super.compareTemplate(Double64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Double64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Double64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Double64Mask compare(Comparison op, Vector<Double> v, VectorMask<Double> m) {
        return super.compareTemplate(Double64Mask.class, op, v, (Double64Mask) m);
    }


    @Override
    @ForceInline
    public Double64Vector blend(Vector<Double> v, VectorMask<Double> m) {
        return (Double64Vector)
            super.blendTemplate(Double64Mask.class,
                                (Double64Vector) v,
                                (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector slice(int origin, Vector<Double> v) {
        return (Double64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector slice(int origin) {
        return (Double64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector unslice(int origin, Vector<Double> w, int part) {
        return (Double64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector unslice(int origin, Vector<Double> w, int part, VectorMask<Double> m) {
        return (Double64Vector)
            super.unsliceTemplate(Double64Mask.class,
                                  origin, w, part,
                                  (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector unslice(int origin) {
        return (Double64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector rearrange(VectorShuffle<Double> s) {
        return (Double64Vector)
            super.rearrangeTemplate(Double64Shuffle.class,
                                    (Double64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector rearrange(VectorShuffle<Double> shuffle,
                                  VectorMask<Double> m) {
        return (Double64Vector)
            super.rearrangeTemplate(Double64Shuffle.class,
                                    Double64Mask.class,
                                    (Double64Shuffle) shuffle,
                                    (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector rearrange(VectorShuffle<Double> s,
                                  Vector<Double> v) {
        return (Double64Vector)
            super.rearrangeTemplate(Double64Shuffle.class,
                                    (Double64Shuffle) s,
                                    (Double64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector compress(VectorMask<Double> m) {
        return (Double64Vector)
            super.compressTemplate(Double64Mask.class,
                                   (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector expand(VectorMask<Double> m) {
        return (Double64Vector)
            super.expandTemplate(Double64Mask.class,
                                   (Double64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector selectFrom(Vector<Double> v) {
        return (Double64Vector)
            super.selectFromTemplate((Double64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Double64Vector selectFrom(Vector<Double> v,
                                   VectorMask<Double> m) {
        return (Double64Vector)
            super.selectFromTemplate((Double64Vector) v,
                                     (Double64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public double lane(int i) {
        long bits;
        switch(i) {
            case 0: bits = laneHelper(0); break;
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return Double.longBitsToDouble(bits);
    }

    public long laneHelper(int i) {
        return (long) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                         VectorPayloadMF vecpayload = vec.vec();
                         long start_offset = vecpayload.multiFieldOffset();
                         return (long)Double.doubleToLongBits(Unsafe.getUnsafe().getDouble(vecpayload, start_offset + ix * Double.BYTES));
                     });
    }

    @ForceInline
    @Override
    public Double64Vector withLane(int i, double e) {
        switch(i) {
            case 0: return withLaneHelper(0, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Double64Vector withLaneHelper(int i, double e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Double.doubleToLongBits(e),
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = Unsafe.getUnsafe().makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    Unsafe.getUnsafe().putDouble(tpayload, start_offset + ix * Double.BYTES, Double.longBitsToDouble((long)bits));
                                    tpayload = Unsafe.getUnsafe().finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class Double64Mask extends AbstractMask<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        private final VectorPayloadMF8Z payload;

        Double64Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF8Z) payload;
        }

        Double64Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Double64Mask(boolean val) {
            this(prepare(val, VLENGTH));
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
        Double64Vector toVector() {
            return (Double64Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Double64Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Double64Mask) VectorSupport.indexPartiallyInUpperRange(
                Double64Mask.class, double.class, VLENGTH, offset, limit,
                (o, l) -> (Double64Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Double64Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Double64Mask compress() {
            return (Double64Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Double64Vector.class, Double64Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Double64Mask and(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double64Mask m = (Double64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Double64Mask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Double64Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Double64Mask or(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double64Mask m = (Double64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Double64Mask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Double64Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Double64Mask xor(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double64Mask m = (Double64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Double64Mask.class, null,
                                          long.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Double64Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Double64Mask.class, long.class, VLENGTH, this,
                                                            (m) -> ((Double64Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Double64Mask.class, long.class, VLENGTH, this,
                                                            (m) -> ((Double64Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Double64Mask.class, long.class, VLENGTH, this,
                                                            (m) -> ((Double64Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Double64Mask.class, long.class, VLENGTH, this,
                                                      (m) -> ((Double64Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Double64Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Double64Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Double64Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Double64Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Double64Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Double64Mask.class, long.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Double64Mask  TRUE_MASK = new Double64Mask(true);
        private static final Double64Mask FALSE_MASK = new Double64Mask(false);

    }

    // Shuffle

    static final value class Double64Shuffle extends AbstractShuffle<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        private final VectorPayloadMF64L payload;

        Double64Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF64L) payload;
            assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Double64Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Double64Shuffle(IntUnaryOperator fn) {
            this(prepare(fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        @ForceInline
        public DoubleSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Long.MAX_VALUE);
            assert(Long.MIN_VALUE <= -VLENGTH);
        }
        static final Double64Shuffle IOTA = new Double64Shuffle(IDENTITY);

        @Override
        @ForceInline
        Long64Vector toBitsVector() {
            return (Long64Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        LongVector toBitsVector0() {
            return Long64Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            switch (length()) {
                case 1 -> a[offset] = laneSource(0);
                case 2 -> toBitsVector()
                        .convertShape(VectorOperators.L2I, IntVector.SPECIES_64, 0)
                        .reinterpretAsInts()
                        .intoArray(a, offset);
                case 4 -> toBitsVector()
                        .convertShape(VectorOperators.L2I, IntVector.SPECIES_128, 0)
                        .reinterpretAsInts()
                        .intoArray(a, offset);
                case 8 -> toBitsVector()
                        .convertShape(VectorOperators.L2I, IntVector.SPECIES_256, 0)
                        .reinterpretAsInts()
                        .intoArray(a, offset);
                case 16 -> toBitsVector()
                        .convertShape(VectorOperators.L2I, IntVector.SPECIES_512, 0)
                        .reinterpretAsInts()
                        .intoArray(a, offset);
                default -> {
                    VectorIntrinsics.checkFromIndexSize(offset, length(), a.length);
                    for (int i = 0; i < length(); i++) {
                        a[offset + i] = laneSource(i);
                    }
                }
            }
        }

        private static VectorPayloadMF prepare(int[] indices, int offset) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(long.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long mfOffset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = indices[offset + i];
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putLong(payload, mfOffset + i * Long.BYTES, (long) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }

        private static VectorPayloadMF prepare(IntUnaryOperator f) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(long.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long offset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = f.applyAsInt(i);
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putLong(payload, offset + i * Long.BYTES, (long) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }


        private static boolean indicesInRange(VectorPayloadMF indices) {
            int length = indices.length();
            long offset = indices.multiFieldOffset();
            for (int i = 0; i < length; i++) {
                long si = Unsafe.getUnsafe().getLong(indices, offset + i * Long.BYTES);
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
    DoubleVector fromArray0(double[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset, VectorMask<Double> m, int offsetInRange) {
        return super.fromArray0Template(Double64Mask.class, a, offset, (Double64Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Double> m) {
        return super.fromArray0Template(Double64Mask.class, a, offset, indexMap, mapOffset, (Double64Mask) m);
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
        return super.fromMemorySegment0Template(Double64Mask.class, ms, offset, (Double64Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Double64Mask.class, a, offset, (Double64Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(double[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Double> m) {
        super.intoArray0Template(Double64Mask.class, a, offset, indexMap, mapOffset, (Double64Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Double> m) {
        super.intoMemorySegment0Template(Double64Mask.class, ms, offset, (Double64Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

