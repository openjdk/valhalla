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
value class Float512Vector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Float512Vector> VCLASS = Float512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF512F.class);

    private final VectorPayloadMF512F payload;

    Float512Vector(Object value) {
        this.payload = (VectorPayloadMF512F) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Float512Vector ZERO = new Float512Vector(VectorPayloadMF.newInstanceFactory(float.class, 16));
    static final Float512Vector IOTA = new Float512Vector(VectorPayloadMF.createVectPayloadInstanceF(16, (float[])(VSPECIES.iotaArray())));

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
    public final Float512Vector broadcast(float e) {
        return (Float512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Vector broadcast(long e) {
        return (Float512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Float512Mask maskFromPayload(VectorPayloadMF payload) {
        return new Float512Mask(payload);
    }

    @Override
    @ForceInline
    Float512Shuffle iotaShuffle() { return Float512Shuffle.IOTA; }

    @Override
    @ForceInline
    Float512Shuffle shuffleFromArray(int[] indices, int i) { return new Float512Shuffle(indices, i); }

    @Override
    @ForceInline
    Float512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Float512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Float512Vector vectorFactory(VectorPayloadMF vec) {
        return new Float512Vector(vec);
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
    Float512Vector uOpMF(FUnOp f) {
        return (Float512Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector uOpMF(VectorMask<Float> m, FUnOp f) {
        return (Float512Vector)
            super.uOpTemplateMF((Float512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Float512Vector bOpMF(Vector<Float> v, FBinOp f) {
        return (Float512Vector) super.bOpTemplateMF((Float512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector bOpMF(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (Float512Vector)
            super.bOpTemplateMF((Float512Vector)v, (Float512Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Float512Vector tOpMF(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (Float512Vector)
            super.tOpTemplateMF((Float512Vector)v1, (Float512Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector tOpMF(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (Float512Vector)
            super.tOpTemplateMF((Float512Vector)v1, (Float512Vector)v2,
                                (Float512Mask)m, f);  // specialize
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
    public Float512Vector lanewise(Unary op) {
        return (Float512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector lanewise(Unary op, VectorMask<Float> m) {
        return (Float512Vector) super.lanewiseTemplate(op, Float512Mask.class, (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector lanewise(Binary op, Vector<Float> v) {
        return (Float512Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector lanewise(Binary op, Vector<Float> v, VectorMask<Float> m) {
        return (Float512Vector) super.lanewiseTemplate(op, Float512Mask.class, v, (Float512Mask) m);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Float512Vector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (Float512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float512Vector
    lanewise(Ternary op, Vector<Float> v1, Vector<Float> v2, VectorMask<Float> m) {
        return (Float512Vector) super.lanewiseTemplate(op, Float512Mask.class, v1, v2, (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float512Vector addIndex(int scale) {
        return (Float512Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Float512Mask.class, (Float512Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Float512Mask.class, (Float512Mask) m);  // specialized
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
    public final Float512Mask test(Test op) {
        return super.testTemplate(Float512Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask test(Test op, VectorMask<Float> m) {
        return super.testTemplate(Float512Mask.class, op, (Float512Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(Float512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, float s) {
        return super.compareTemplate(Float512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Float512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, Vector<Float> v, VectorMask<Float> m) {
        return super.compareTemplate(Float512Mask.class, op, v, (Float512Mask) m);
    }


    @Override
    @ForceInline
    public Float512Vector blend(Vector<Float> v, VectorMask<Float> m) {
        return (Float512Vector)
            super.blendTemplate(Float512Mask.class,
                                (Float512Vector) v,
                                (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector slice(int origin, Vector<Float> v) {
        return (Float512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector slice(int origin) {
        return (Float512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin, Vector<Float> w, int part) {
        return (Float512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (Float512Vector)
            super.unsliceTemplate(Float512Mask.class,
                                  origin, w, part,
                                  (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin) {
        return (Float512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> s) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    (Float512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    Float512Mask.class,
                                    (Float512Shuffle) shuffle,
                                    (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    (Float512Shuffle) s,
                                    (Float512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector compress(VectorMask<Float> m) {
        return (Float512Vector)
            super.compressTemplate(Float512Mask.class,
                                   (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector expand(VectorMask<Float> m) {
        return (Float512Vector)
            super.expandTemplate(Float512Mask.class,
                                   (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector selectFrom(Vector<Float> v) {
        return (Float512Vector)
            super.selectFromTemplate((Float512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (Float512Vector)
            super.selectFromTemplate((Float512Vector) v,
                                     (Float512Mask) m);  // specialize
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
            case 4: bits = laneHelper(4); break;
            case 5: bits = laneHelper(5); break;
            case 6: bits = laneHelper(6); break;
            case 7: bits = laneHelper(7); break;
            case 8: bits = laneHelper(8); break;
            case 9: bits = laneHelper(9); break;
            case 10: bits = laneHelper(10); break;
            case 11: bits = laneHelper(11); break;
            case 12: bits = laneHelper(12); break;
            case 13: bits = laneHelper(13); break;
            case 14: bits = laneHelper(14); break;
            case 15: bits = laneHelper(15); break;
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
    public Float512Vector withLane(int i, float e) {
        switch(i) {
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

    public Float512Vector withLaneHelper(int i, float e) {
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

    static final value class Float512Mask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        private final VectorPayloadMF128Z payload;

        Float512Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF128Z) payload;
        }

        Float512Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Float512Mask(boolean val) {
            this(prepare(val, VLENGTH));
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
        Float512Vector toVector() {
            return (Float512Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Float512Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Float512Mask) VectorSupport.indexPartiallyInUpperRange(
                Float512Mask.class, float.class, VLENGTH, offset, limit,
                (o, l) -> (Float512Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Float512Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Float512Mask compress() {
            return (Float512Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Float512Vector.class, Float512Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Float512Mask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Float512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float512Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float512Mask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Float512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float512Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Float512Mask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Float512Mask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Float512Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Float512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float512Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Float512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float512Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Float512Mask.class, int.class, VLENGTH, this,
                                                            (m) -> ((Float512Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Float512Mask.class, int.class, VLENGTH, this,
                                                      (m) -> ((Float512Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Float512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Float512Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Float512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Float512Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Float512Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Float512Mask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Float512Mask  TRUE_MASK = new Float512Mask(true);
        private static final Float512Mask FALSE_MASK = new Float512Mask(false);

    }

    // Shuffle

    static final value class Float512Shuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMF512I payload;

        Float512Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF512I) payload;
            assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Float512Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Float512Shuffle(IntUnaryOperator fn) {
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
        static final Float512Shuffle IOTA = new Float512Shuffle(IDENTITY);

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
    FloatVector fromArray0(float[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, VectorMask<Float> m, int offsetInRange) {
        return super.fromArray0Template(Float512Mask.class, a, offset, (Float512Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        return super.fromArray0Template(Float512Mask.class, a, offset, indexMap, mapOffset, (Float512Mask) m);
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
        return super.fromMemorySegment0Template(Float512Mask.class, ms, offset, (Float512Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Float512Mask.class, a, offset, (Float512Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Float> m) {
        super.intoArray0Template(Float512Mask.class, a, offset, indexMap, mapOffset, (Float512Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Float> m) {
        super.intoMemorySegment0Template(Float512Mask.class, ms, offset, (Float512Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

