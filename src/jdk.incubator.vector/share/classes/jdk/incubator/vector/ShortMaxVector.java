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
value class ShortMaxVector extends ShortVector {
    static final ShortSpecies VSPECIES =
        (ShortSpecies) ShortVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<ShortMaxVector> VCLASS = ShortMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Short> ETYPE = short.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxS.class);

    private final VectorPayloadMFMaxS payload;

    ShortMaxVector(Object value) {
        this.payload = (VectorPayloadMFMaxS) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final ShortMaxVector ZERO = new ShortMaxVector(createPayloadInstance(VSPECIES));

    static final ShortMaxVector IOTA = new ShortMaxVector(VectorPayloadMF.createVectPayloadInstanceS(VLENGTH, (short[])(VSPECIES.iotaArray()), true));

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
    public ShortSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Short> elementType() { return short.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Short.SIZE; }

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
    public final ShortMaxVector broadcast(short e) {
        return (ShortMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final ShortMaxVector broadcast(long e) {
        return (ShortMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    ShortMaxMask maskFromPayload(VectorPayloadMF payload) {
        return new ShortMaxMask(payload);
    }

    @Override
    @ForceInline
    ShortMaxShuffle iotaShuffle() { return ShortMaxShuffle.IOTA; }

    @Override
    @ForceInline
    ShortMaxShuffle shuffleFromArray(int[] indices, int i) { return new ShortMaxShuffle(indices, i); }

    @Override
    @ForceInline
    ShortMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new ShortMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    ShortMaxVector vectorFactory(VectorPayloadMF vec) {
        return new ShortMaxVector(vec);
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
    ShortMaxVector uOpMF(FUnOp f) {
        return (ShortMaxVector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    ShortMaxVector uOpMF(VectorMask<Short> m, FUnOp f) {
        return (ShortMaxVector)
            super.uOpTemplateMF((ShortMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    ShortMaxVector bOpMF(Vector<Short> v, FBinOp f) {
        return (ShortMaxVector) super.bOpTemplateMF((ShortMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    ShortMaxVector bOpMF(Vector<Short> v,
                     VectorMask<Short> m, FBinOp f) {
        return (ShortMaxVector)
            super.bOpTemplateMF((ShortMaxVector)v, (ShortMaxMask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    ShortMaxVector tOpMF(Vector<Short> v1, Vector<Short> v2, FTriOp f) {
        return (ShortMaxVector)
            super.tOpTemplateMF((ShortMaxVector)v1, (ShortMaxVector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    ShortMaxVector tOpMF(Vector<Short> v1, Vector<Short> v2,
                     VectorMask<Short> m, FTriOp f) {
        return (ShortMaxVector)
            super.tOpTemplateMF((ShortMaxVector)v1, (ShortMaxVector)v2,
                                (ShortMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    short rOpMF(short v, VectorMask<Short> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Short,F> conv,
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
    public ShortMaxVector lanewise(Unary op) {
        return (ShortMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector lanewise(Unary op, VectorMask<Short> m) {
        return (ShortMaxVector) super.lanewiseTemplate(op, ShortMaxMask.class, (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector lanewise(Binary op, Vector<Short> v) {
        return (ShortMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector lanewise(Binary op, Vector<Short> v, VectorMask<Short> m) {
        return (ShortMaxVector) super.lanewiseTemplate(op, ShortMaxMask.class, v, (ShortMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline ShortMaxVector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (ShortMaxVector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline ShortMaxVector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Short> m) {
        return (ShortMaxVector) super.lanewiseShiftTemplate(op, ShortMaxMask.class, e, (ShortMaxMask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    ShortMaxVector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2) {
        return (ShortMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    ShortMaxVector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2, VectorMask<Short> m) {
        return (ShortMaxVector) super.lanewiseTemplate(op, ShortMaxMask.class, v1, v2, (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    ShortMaxVector addIndex(int scale) {
        return (ShortMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final short reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final short reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Short> m) {
        return super.reduceLanesTemplate(op, ShortMaxMask.class, (ShortMaxMask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Short> m) {
        return (long) super.reduceLanesTemplate(op, ShortMaxMask.class, (ShortMaxMask) m);  // specialized
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
    public final ShortMaxMask test(Test op) {
        return super.testTemplate(ShortMaxMask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final ShortMaxMask test(Test op, VectorMask<Short> m) {
        return super.testTemplate(ShortMaxMask.class, op, (ShortMaxMask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final ShortMaxMask compare(Comparison op, Vector<Short> v) {
        return super.compareTemplate(ShortMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final ShortMaxMask compare(Comparison op, short s) {
        return super.compareTemplate(ShortMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final ShortMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(ShortMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final ShortMaxMask compare(Comparison op, Vector<Short> v, VectorMask<Short> m) {
        return super.compareTemplate(ShortMaxMask.class, op, v, (ShortMaxMask) m);
    }


    @Override
    @ForceInline
    public ShortMaxVector blend(Vector<Short> v, VectorMask<Short> m) {
        return (ShortMaxVector)
            super.blendTemplate(ShortMaxMask.class,
                                (ShortMaxVector) v,
                                (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector slice(int origin, Vector<Short> v) {
        return (ShortMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector slice(int origin) {
        return (ShortMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector unslice(int origin, Vector<Short> w, int part) {
        return (ShortMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m) {
        return (ShortMaxVector)
            super.unsliceTemplate(ShortMaxMask.class,
                                  origin, w, part,
                                  (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector unslice(int origin) {
        return (ShortMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector rearrange(VectorShuffle<Short> s) {
        return (ShortMaxVector)
            super.rearrangeTemplate(ShortMaxShuffle.class,
                                    (ShortMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector rearrange(VectorShuffle<Short> shuffle,
                                  VectorMask<Short> m) {
        return (ShortMaxVector)
            super.rearrangeTemplate(ShortMaxShuffle.class,
                                    ShortMaxMask.class,
                                    (ShortMaxShuffle) shuffle,
                                    (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector rearrange(VectorShuffle<Short> s,
                                  Vector<Short> v) {
        return (ShortMaxVector)
            super.rearrangeTemplate(ShortMaxShuffle.class,
                                    (ShortMaxShuffle) s,
                                    (ShortMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector compress(VectorMask<Short> m) {
        return (ShortMaxVector)
            super.compressTemplate(ShortMaxMask.class,
                                   (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector expand(VectorMask<Short> m) {
        return (ShortMaxVector)
            super.expandTemplate(ShortMaxMask.class,
                                   (ShortMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector selectFrom(Vector<Short> v) {
        return (ShortMaxVector)
            super.selectFromTemplate((ShortMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public ShortMaxVector selectFrom(Vector<Short> v,
                                   VectorMask<Short> m) {
        return (ShortMaxVector)
            super.selectFromTemplate((ShortMaxVector) v,
                                     (ShortMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public short lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return laneHelper(i);
    }

    public short laneHelper(int i) {
        return (short) VectorSupport.extract(
                             VCLASS, ETYPE, VLENGTH,
                             this, i,
                             (vec, ix) -> {
                                 VectorPayloadMF vecpayload = vec.vec();
                                 long start_offset = vecpayload.multiFieldOffset();
                                 return (long)Unsafe.getUnsafe().getShort(vecpayload, start_offset + ix * Short.BYTES);
                             });
    }

    @ForceInline
    @Override
    public ShortMaxVector withLane(int i, short e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public ShortMaxVector withLaneHelper(int i, short e) {
       return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = Unsafe.getUnsafe().makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    Unsafe.getUnsafe().putShort(tpayload, start_offset + ix * Short.BYTES, (short)bits);
                                    tpayload = Unsafe.getUnsafe().finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class ShortMaxMask extends AbstractMask<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMFMaxSZ.class);

        private final VectorPayloadMFMaxSZ payload;

        ShortMaxMask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxSZ) payload;
        }

        ShortMaxMask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        ShortMaxMask(boolean val) {
            this(prepare(val, VSPECIES));
        }

        @ForceInline
        final @Override
        public ShortSpecies vspecies() {
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
        ShortMaxVector toVector() {
            return (ShortMaxVector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        ShortMaxMask indexPartiallyInUpperRange(long offset, long limit) {
            return (ShortMaxMask) VectorSupport.indexPartiallyInUpperRange(
                ShortMaxMask.class, short.class, VLENGTH, offset, limit,
                (o, l) -> (ShortMaxMask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public ShortMaxMask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public ShortMaxMask compress() {
            return (ShortMaxMask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                ShortMaxVector.class, ShortMaxMask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public ShortMaxMask and(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            ShortMaxMask m = (ShortMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, ShortMaxMask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (ShortMaxMask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public ShortMaxMask or(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            ShortMaxMask m = (ShortMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, ShortMaxMask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (ShortMaxMask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public ShortMaxMask xor(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            ShortMaxMask m = (ShortMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, ShortMaxMask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (ShortMaxMask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, ShortMaxMask.class, short.class, VLENGTH, this,
                                                            (m) -> ((ShortMaxMask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, ShortMaxMask.class, short.class, VLENGTH, this,
                                                            (m) -> ((ShortMaxMask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, ShortMaxMask.class, short.class, VLENGTH, this,
                                                            (m) -> ((ShortMaxMask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, ShortMaxMask.class, short.class, VLENGTH, this,
                                                      (m) -> ((ShortMaxMask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, ShortMaxMask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((ShortMaxMask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, ShortMaxMask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((ShortMaxMask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static ShortMaxMask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(ShortMaxMask.class, short.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final ShortMaxMask  TRUE_MASK = new ShortMaxMask(true);
        private static final ShortMaxMask FALSE_MASK = new ShortMaxMask(false);

    }

    // Shuffle

    static final value class ShortMaxShuffle extends AbstractShuffle<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        private final VectorPayloadMFMaxS payload;

        ShortMaxShuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMFMaxS) payload;
            //assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        ShortMaxShuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        ShortMaxShuffle(IntUnaryOperator fn) {
            this(prepare(fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        @ForceInline
        public ShortSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Short.MAX_VALUE);
            assert(Short.MIN_VALUE <= -VLENGTH);
        }
        static final ShortMaxShuffle IOTA = new ShortMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        ShortMaxVector toBitsVector() {
            return (ShortMaxVector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        ShortVector toBitsVector0() {
            return ShortMaxVector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            VectorSpecies<Integer> species = IntVector.SPECIES_MAX;
            Vector<Short> v = toBitsVector();
            v.convertShape(VectorOperators.S2I, species, 0)
                    .reinterpretAsInts()
                    .intoArray(a, offset);
            v.convertShape(VectorOperators.S2I, species, 1)
                    .reinterpretAsInts()
                    .intoArray(a, offset + species.length());
        }

        private static VectorPayloadMF prepare(int[] indices, int offset) {
            VectorPayloadMF payload = createPayloadInstance(VSPECIES);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long mfOffset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = indices[offset + i];
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putShort(payload, mfOffset + i * Short.BYTES, (short) si);
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
                Unsafe.getUnsafe().putShort(payload, offset + i * Short.BYTES, (short) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }


        private static boolean indicesInRange(VectorPayloadMF indices) {
            int length = VLENGTH;
            long offset = indices.multiFieldOffset();
            for (int i = 0; i < length; i++) {
                short si = Unsafe.getUnsafe().getShort(indices, offset + i * Short.BYTES);
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
    ShortVector fromArray0(short[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ShortVector fromArray0(short[] a, int offset, VectorMask<Short> m, int offsetInRange) {
        return super.fromArray0Template(ShortMaxMask.class, a, offset, (ShortMaxMask) m, offsetInRange);  // specialize
    }


    @ForceInline
    @Override
    final
    ShortVector fromCharArray0(char[] a, int offset) {
        return super.fromCharArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ShortVector fromCharArray0(char[] a, int offset, VectorMask<Short> m, int offsetInRange) {
        return super.fromCharArray0Template(ShortMaxMask.class, a, offset, (ShortMaxMask) m, offsetInRange);  // specialize
    }


    @ForceInline
    @Override
    final
    ShortVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ShortVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Short> m, int offsetInRange) {
        return super.fromMemorySegment0Template(ShortMaxMask.class, ms, offset, (ShortMaxMask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(short[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(short[] a, int offset, VectorMask<Short> m) {
        super.intoArray0Template(ShortMaxMask.class, a, offset, (ShortMaxMask) m);
    }



    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Short> m) {
        super.intoMemorySegment0Template(ShortMaxMask.class, ms, offset, (ShortMaxMask) m);
    }

    @ForceInline
    @Override
    final
    void intoCharArray0(char[] a, int offset, VectorMask<Short> m) {
        super.intoCharArray0Template(ShortMaxMask.class, a, offset, (ShortMaxMask) m);
    }

    // End of specialized low-level memory operations.

    // ================================================

}

