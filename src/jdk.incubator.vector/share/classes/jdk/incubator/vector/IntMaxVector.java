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
value class IntMaxVector extends IntVector {
    static final IntSpecies VSPECIES =
        (IntSpecies) IntVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<IntMaxVector> VCLASS = IntMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Integer> ETYPE = int.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxI.class);

    private final VectorPayloadMFMaxI payload;

    IntMaxVector(Object value) {
        this.payload = (VectorPayloadMFMaxI) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final IntMaxVector ZERO = new IntMaxVector(createPayloadInstance(VSPECIES));

    static final IntMaxVector IOTA = new IntMaxVector(VectorPayloadMF.createVectPayloadInstanceI(VLENGTH, (int[])(VSPECIES.iotaArray()), true));

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
    public final IntMaxVector broadcast(int e) {
        return (IntMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final IntMaxVector broadcast(long e) {
        return (IntMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    IntMaxMask maskFromPayload(VectorPayloadMF payload) {
        return new IntMaxMask(payload);
    }

    @Override
    @ForceInline
    IntMaxShuffle iotaShuffle() { return IntMaxShuffle.IOTA; }

    @Override
    @ForceInline
    IntMaxShuffle shuffleFromArray(int[] indices, int i) { return new IntMaxShuffle(indices, i); }

    @Override
    @ForceInline
    IntMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new IntMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    IntMaxVector vectorFactory(VectorPayloadMF vec) {
        return new IntMaxVector(vec);
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
    IntMaxVector uOpMF(FUnOp f) {
        return (IntMaxVector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    IntMaxVector uOpMF(VectorMask<Integer> m, FUnOp f) {
        return (IntMaxVector)
            super.uOpTemplateMF((IntMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    IntMaxVector bOpMF(Vector<Integer> v, FBinOp f) {
        return (IntMaxVector) super.bOpTemplateMF((IntMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    IntMaxVector bOpMF(Vector<Integer> v,
                     VectorMask<Integer> m, FBinOp f) {
        return (IntMaxVector)
            super.bOpTemplateMF((IntMaxVector)v, (IntMaxMask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    IntMaxVector tOpMF(Vector<Integer> v1, Vector<Integer> v2, FTriOp f) {
        return (IntMaxVector)
            super.tOpTemplateMF((IntMaxVector)v1, (IntMaxVector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    IntMaxVector tOpMF(Vector<Integer> v1, Vector<Integer> v2,
                     VectorMask<Integer> m, FTriOp f) {
        return (IntMaxVector)
            super.tOpTemplateMF((IntMaxVector)v1, (IntMaxVector)v2,
                                (IntMaxMask)m, f);  // specialize
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
    public IntMaxVector lanewise(Unary op) {
        return (IntMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector lanewise(Unary op, VectorMask<Integer> m) {
        return (IntMaxVector) super.lanewiseTemplate(op, IntMaxMask.class, (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector lanewise(Binary op, Vector<Integer> v) {
        return (IntMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector lanewise(Binary op, Vector<Integer> v, VectorMask<Integer> m) {
        return (IntMaxVector) super.lanewiseTemplate(op, IntMaxMask.class, v, (IntMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline IntMaxVector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (IntMaxVector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline IntMaxVector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Integer> m) {
        return (IntMaxVector) super.lanewiseShiftTemplate(op, IntMaxMask.class, e, (IntMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    IntMaxVector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2) {
        return (IntMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    IntMaxVector
    lanewise(Ternary op, Vector<Integer> v1, Vector<Integer> v2, VectorMask<Integer> m) {
        return (IntMaxVector) super.lanewiseTemplate(op, IntMaxMask.class, v1, v2, (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    IntMaxVector addIndex(int scale) {
        return (IntMaxVector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, IntMaxMask.class, (IntMaxMask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, IntMaxMask.class, (IntMaxMask) m);  // specialized
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
    public final IntMaxMask test(Test op) {
        return super.testTemplate(IntMaxMask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final IntMaxMask test(Test op, VectorMask<Integer> m) {
        return super.testTemplate(IntMaxMask.class, op, (IntMaxMask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final IntMaxMask compare(Comparison op, Vector<Integer> v) {
        return super.compareTemplate(IntMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final IntMaxMask compare(Comparison op, int s) {
        return super.compareTemplate(IntMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final IntMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(IntMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final IntMaxMask compare(Comparison op, Vector<Integer> v, VectorMask<Integer> m) {
        return super.compareTemplate(IntMaxMask.class, op, v, (IntMaxMask) m);
    }


    @Override
    @ForceInline
    public IntMaxVector blend(Vector<Integer> v, VectorMask<Integer> m) {
        return (IntMaxVector)
            super.blendTemplate(IntMaxMask.class,
                                (IntMaxVector) v,
                                (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector slice(int origin, Vector<Integer> v) {
        return (IntMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector slice(int origin) {
        return (IntMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector unslice(int origin, Vector<Integer> w, int part) {
        return (IntMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector unslice(int origin, Vector<Integer> w, int part, VectorMask<Integer> m) {
        return (IntMaxVector)
            super.unsliceTemplate(IntMaxMask.class,
                                  origin, w, part,
                                  (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector unslice(int origin) {
        return (IntMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector rearrange(VectorShuffle<Integer> s) {
        return (IntMaxVector)
            super.rearrangeTemplate(IntMaxShuffle.class,
                                    (IntMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector rearrange(VectorShuffle<Integer> shuffle,
                                  VectorMask<Integer> m) {
        return (IntMaxVector)
            super.rearrangeTemplate(IntMaxShuffle.class,
                                    IntMaxMask.class,
                                    (IntMaxShuffle) shuffle,
                                    (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector rearrange(VectorShuffle<Integer> s,
                                  Vector<Integer> v) {
        return (IntMaxVector)
            super.rearrangeTemplate(IntMaxShuffle.class,
                                    (IntMaxShuffle) s,
                                    (IntMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector compress(VectorMask<Integer> m) {
        return (IntMaxVector)
            super.compressTemplate(IntMaxMask.class,
                                   (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector expand(VectorMask<Integer> m) {
        return (IntMaxVector)
            super.expandTemplate(IntMaxMask.class,
                                   (IntMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector selectFrom(Vector<Integer> v) {
        return (IntMaxVector)
            super.selectFromTemplate((IntMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public IntMaxVector selectFrom(Vector<Integer> v,
                                   VectorMask<Integer> m) {
        return (IntMaxVector)
            super.selectFromTemplate((IntMaxVector) v,
                                     (IntMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public int lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return laneHelper(i);
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
    public IntMaxVector withLane(int i, int e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public IntMaxVector withLaneHelper(int i, int e) {
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

    static final value class IntMaxMask extends AbstractMask<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxIZ.class);

        private final VectorPayloadMFMaxIZ payload;

        IntMaxMask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxIZ) payload;
        }

        IntMaxMask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        IntMaxMask(boolean val) {
            this(prepare(val, VSPECIES));
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
        public final long multiFieldOffset() { return MFOFFSET; }

        @ForceInline
        @Override
        public final
        IntMaxVector toVector() {
            return (IntMaxVector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        IntMaxMask indexPartiallyInUpperRange(long offset, long limit) {
            return (IntMaxMask) VectorSupport.indexPartiallyInUpperRange(
                IntMaxMask.class, int.class, VLENGTH, offset, limit,
                (o, l) -> (IntMaxMask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public IntMaxMask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public IntMaxMask compress() {
            return (IntMaxMask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                IntMaxVector.class, IntMaxMask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public IntMaxMask and(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            IntMaxMask m = (IntMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, IntMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (IntMaxMask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public IntMaxMask or(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            IntMaxMask m = (IntMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, IntMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (IntMaxMask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public IntMaxMask xor(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            IntMaxMask m = (IntMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, IntMaxMask.class, null,
                                          int.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (IntMaxMask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, IntMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((IntMaxMask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, IntMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((IntMaxMask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, IntMaxMask.class, int.class, VLENGTH, this,
                                                            (m) -> ((IntMaxMask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, IntMaxMask.class, int.class, VLENGTH, this,
                                                      (m) -> ((IntMaxMask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, IntMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((IntMaxMask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, IntMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((IntMaxMask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static IntMaxMask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(IntMaxMask.class, int.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final IntMaxMask  TRUE_MASK = new IntMaxMask(true);
        private static final IntMaxMask FALSE_MASK = new IntMaxMask(false);


        static VectorPayloadMF maskLowerHalf() {
            VectorPayloadMF newObj = createPayloadInstance(VSPECIES);
            newObj = Unsafe.getUnsafe().makePrivateBuffer(newObj);
            long mf_offset = newObj.multiFieldOffset();
            int len = VLENGTH >> 1;
            for (int i = 0; i < len; i++) {
                Unsafe.getUnsafe().putBoolean(newObj, mf_offset + i, true);
            }
            newObj = Unsafe.getUnsafe().finishPrivateBuffer(newObj);
            return newObj;
        }

        static final IntMaxMask LOWER_HALF_TRUE_MASK = new IntMaxMask(maskLowerHalf());
    }

    // Shuffle

    static final value class IntMaxShuffle extends AbstractShuffle<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        private final VectorPayloadMFMaxI payload;

        IntMaxShuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxI) payload;
            //assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        IntMaxShuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        IntMaxShuffle(IntUnaryOperator fn) {
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
        static final IntMaxShuffle IOTA = new IntMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        IntMaxVector toBitsVector() {
            return (IntMaxVector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        IntVector toBitsVector0() {
            return IntMaxVector.VSPECIES.dummyVectorMF().vectorFactory(indices());
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
    IntVector fromArray0(int[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset, VectorMask<Integer> m, int offsetInRange) {
        return super.fromArray0Template(IntMaxMask.class, a, offset, (IntMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        return super.fromArray0Template(IntMaxMask.class, a, offset, indexMap, mapOffset, (IntMaxMask) m);
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
        return super.fromMemorySegment0Template(IntMaxMask.class, ms, offset, (IntMaxMask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(IntMaxMask.class, a, offset, (IntMaxMask) m);
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset, int[] indexMap, int mapOffset, VectorMask<Integer> m) {
        super.intoArray0Template(IntMaxMask.class, a, offset, indexMap, mapOffset, (IntMaxMask) m);
    }


    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Integer> m) {
        super.intoMemorySegment0Template(IntMaxMask.class, ms, offset, (IntMaxMask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

