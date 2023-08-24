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
value class Float128Vector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_128;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Float128Vector> VCLASS = Float128Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF128F.class);

    private final VectorPayloadMF128F payload;

    Float128Vector(Object value) {
        this.payload = (VectorPayloadMF128F) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Float128Vector ZERO = new Float128Vector(createPayloadInstance(VSPECIES));

    static final Float128Vector IOTA = new Float128Vector(VectorPayloadMF.createVectPayloadInstanceF(4, (float[])(VSPECIES.iotaArray()), false));

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
    public final Float128Vector broadcast(float e) {
        return (Float128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Vector broadcast(long e) {
        return (Float128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Float128Mask maskFromPayload(VectorPayloadMF payload) {
        return new Float128Mask(payload);
    }

    @Override
    @ForceInline
    Float128Shuffle iotaShuffle() { return Float128Shuffle.IOTA; }

    @Override
    @ForceInline
    Float128Shuffle shuffleFromArray(int[] indices, int i) { return new Float128Shuffle(indices, i); }

    @Override
    @ForceInline
    Float128Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Float128Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Float128Vector vectorFactory(VectorPayloadMF vec) {
        return new Float128Vector(vec);
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
    Float128Vector uOpMF(FUnOp f) {
        return (Float128Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector uOpMF(VectorMask<Float> m, FUnOp f) {
        return (Float128Vector)
            super.uOpTemplateMF((Float128Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Float128Vector bOpMF(Vector<Float> v, FBinOp f) {
        return (Float128Vector) super.bOpTemplateMF((Float128Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector bOpMF(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (Float128Vector)
            super.bOpTemplateMF((Float128Vector)v, (Float128Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Float128Vector tOpMF(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (Float128Vector)
            super.tOpTemplateMF((Float128Vector)v1, (Float128Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector tOpMF(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (Float128Vector)
            super.tOpTemplateMF((Float128Vector)v1, (Float128Vector)v2,
                                (Float128Mask)m, f);  // specialize
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
    public Float128Vector lanewise(Unary op) {
        return (Float128Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector lanewise(Unary op, VectorMask<Float> m) {
        return (Float128Vector) super.lanewiseTemplate(op, Float128Mask.class, (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector lanewise(Binary op, Vector<Float> v) {
        return (Float128Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector lanewise(Binary op, Vector<Float> v, VectorMask<Float> m) {
        return (Float128Vector) super.lanewiseTemplate(op, Float128Mask.class, v, (Float128Mask) m);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Float128Vector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (Float128Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float128Vector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2, VectorMask<Float> m) {
        return (Float128Vector) super.lanewiseTemplate(op, Float128Mask.class, v1, v2, (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float128Vector addIndex(int scale) {
        return (Float128Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Float128Mask.class, (Float128Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Float128Mask.class, (Float128Mask) m);  // specialized
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
    public final Float128Mask test(Test op) {
        return super.testTemplate(Float128Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask test(Test op, VectorMask<Float> m) {
        return super.testTemplate(Float128Mask.class, op, (Float128Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(Float128Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, float s) {
        return super.compareTemplate(Float128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, long s) {
        return super.compareTemplate(Float128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, Vector<Float> v, VectorMask<Float> m) {
        return super.compareTemplate(Float128Mask.class, op, v, (Float128Mask) m);
    }


    @Override
    @ForceInline
    public Float128Vector blend(Vector<Float> v, VectorMask<Float> m) {
        return (Float128Vector)
            super.blendTemplate(Float128Mask.class,
                                (Float128Vector) v,
                                (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector slice(int origin, Vector<Float> v) {
        return (Float128Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector slice(int origin) {
        return (Float128Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector unslice(int origin, Vector<Float> w, int part) {
        return (Float128Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (Float128Vector)
            super.unsliceTemplate(Float128Mask.class,
                                  origin, w, part,
                                  (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector unslice(int origin) {
        return (Float128Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> s) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    (Float128Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    Float128Mask.class,
                                    (Float128Shuffle) shuffle,
                                    (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    (Float128Shuffle) s,
                                    (Float128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector compress(VectorMask<Float> m) {
        return (Float128Vector)
            super.compressTemplate(Float128Mask.class,
                                   (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector expand(VectorMask<Float> m) {
        return (Float128Vector)
            super.expandTemplate(Float128Mask.class,
                                   (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector selectFrom(Vector<Float> v) {
        return (Float128Vector)
            super.selectFromTemplate((Float128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (Float128Vector)
            super.selectFromTemplate((Float128Vector) v,
                                     (Float128Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public float lane(int i) {
        int bits;
        switch(i) {
            case 0: bits = laneHelper(0); break;
            case 1: bits = laneHelper(1); break;
            case 2: bits = laneHelper(2); break;
            case 3: bits = laneHelper(3); break;
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return Float.intBitsToFloat(bits);
    }

    public int laneHelper(int i) {
        return (int) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                         VectorPayloadMF vecpayload = vec.vec();
                         long start_offset = vecpayload.multiFieldOffset();
                         return (long)Float.floatToIntBits(Unsafe.getUnsafe().getFloat(vecpayload, start_offset + ix * Float.BYTES));
                     });
    }

    @ForceInline
    @Override
    public Float128Vector withLane(int i, float e) {
        switch(i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Float128Vector withLaneHelper(int i, float e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = Unsafe.getUnsafe().makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    Unsafe.getUnsafe().putFloat(tpayload, start_offset + ix * Float.BYTES, Float.intBitsToFloat((int)bits));
                                    tpayload = Unsafe.getUnsafe().finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class Float128Mask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF32Z.class);

        private final VectorPayloadMF32Z payload;

        Float128Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF32Z) payload;
        }

        Float128Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        Float128Mask(boolean val) {
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
        public final long multiFieldOffset() { return MFOFFSET; }

        @ForceInline
        @Override
        public final
        Float128Vector toVector() {
            return (Float128Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Float128Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Float128Mask) VectorSupport.indexPartiallyInUpperRange(
                Float128Mask.class, float.class, VLENGTH, offset, limit,
                (o, l) -> (Float128Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Float128Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Float128Mask compress() {
            return (Float128Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Float128Vector.class, Float128Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Float128Mask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float128Mask m = (Float128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Float128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float128Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float128Mask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float128Mask m = (Float128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Float128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float128Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Float128Mask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float128Mask m = (Float128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Float128Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float128Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Float128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float128Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Float128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float128Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Float128Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float128Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Float128Mask.class, int.class, VLENGTH, this,
                                                      (m) -> ((Float128Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Float128Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Float128Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Float128Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Float128Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Float128Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Float128Mask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Float128Mask  TRUE_MASK = new Float128Mask(true);
        private static final Float128Mask FALSE_MASK = new Float128Mask(false);

    }

    // Shuffle

    static final value class Float128Shuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF128I payload;

        Float128Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF128I) payload;
            //assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Float128Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Float128Shuffle(IntUnaryOperator fn) {
            this(prepare(fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        @ForceInline
        public FloatSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Integer.MAX_VALUE);
            assert(Integer.MIN_VALUE <= -VLENGTH);
        }
        static final Float128Shuffle IOTA = new Float128Shuffle(IDENTITY);

        @Override
        @ForceInline
        Int128Vector toBitsVector() {
            return (Int128Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        IntVector toBitsVector0() {
            return Int128Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
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
            VectorPayloadMF payload = createPayloadInstance(VSPECIES);
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
            VectorPayloadMF payload = createPayloadInstance(VSPECIES);
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
            int length = VLENGTH;
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
    FloatVector fromArray0(float[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, VectorMask<Float> m, int offsetInRange) {
        return super.fromArray0Template(Float128Mask.class, a, offset, (Float128Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        return super.fromArray0Template(Float128Mask.class, a, offset, indexMap, mapOffset, (Float128Mask) m);
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
        return super.fromMemorySegment0Template(Float128Mask.class, ms, offset, (Float128Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Float128Mask.class, a, offset, (Float128Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        super.intoArray0Template(Float128Mask.class, a, offset, indexMap, mapOffset, (Float128Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Float> m) {
        super.intoMemorySegment0Template(Float128Mask.class, ms, offset, (Float128Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

