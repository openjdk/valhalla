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
value class Short64Vector extends ShortVector {
    static final ShortSpecies VSPECIES =
        (ShortSpecies) ShortVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Short64Vector> VCLASS = Short64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Short> ETYPE = short.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF64S.class);

    private final VectorPayloadMF64S payload;

    Short64Vector(Object value) {
        this.payload = (VectorPayloadMF64S) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Short64Vector ZERO = new Short64Vector(createPayloadInstance(VSPECIES));

    static final Short64Vector IOTA = new Short64Vector(VectorPayloadMF.createVectPayloadInstanceS(4, (short[])(VSPECIES.iotaArray()), false));

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
    public final Short64Vector broadcast(short e) {
        return (Short64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Short64Vector broadcast(long e) {
        return (Short64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Short64Mask maskFromPayload(VectorPayloadMF payload) {
        return new Short64Mask(payload);
    }

    @Override
    @ForceInline
    Short64Shuffle iotaShuffle() { return Short64Shuffle.IOTA; }

    @Override
    @ForceInline
    Short64Shuffle shuffleFromArray(int[] indices, int i) { return new Short64Shuffle(indices, i); }

    @Override
    @ForceInline
    Short64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Short64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Short64Vector vectorFactory(VectorPayloadMF vec) {
        return new Short64Vector(vec);
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
    Short64Vector uOpMF(FUnOp f) {
        return (Short64Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Short64Vector uOpMF(VectorMask<Short> m, FUnOp f) {
        return (Short64Vector)
            super.uOpTemplateMF((Short64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Short64Vector bOpMF(Vector<Short> v, FBinOp f) {
        return (Short64Vector) super.bOpTemplateMF((Short64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Short64Vector bOpMF(Vector<Short> v,
                     VectorMask<Short> m, FBinOp f) {
        return (Short64Vector)
            super.bOpTemplateMF((Short64Vector)v, (Short64Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Short64Vector tOpMF(Vector<Short> v1, Vector<Short> v2, FTriOp f) {
        return (Short64Vector)
            super.tOpTemplateMF((Short64Vector)v1, (Short64Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Short64Vector tOpMF(Vector<Short> v1, Vector<Short> v2,
                     VectorMask<Short> m, FTriOp f) {
        return (Short64Vector)
            super.tOpTemplateMF((Short64Vector)v1, (Short64Vector)v2,
                                (Short64Mask)m, f);  // specialize
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
    public Short64Vector lanewise(Unary op) {
        return (Short64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector lanewise(Unary op, VectorMask<Short> m) {
        return (Short64Vector) super.lanewiseTemplate(op, Short64Mask.class, (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector lanewise(Binary op, Vector<Short> v) {
        return (Short64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector lanewise(Binary op, Vector<Short> v, VectorMask<Short> m) {
        return (Short64Vector) super.lanewiseTemplate(op, Short64Mask.class, v, (Short64Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short64Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Short64Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short64Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Short> m) {
        return (Short64Vector) super.lanewiseShiftTemplate(op, Short64Mask.class, e, (Short64Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Short64Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2) {
        return (Short64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short64Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2, VectorMask<Short> m) {
        return (Short64Vector) super.lanewiseTemplate(op, Short64Mask.class, v1, v2, (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short64Vector addIndex(int scale) {
        return (Short64Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Short64Mask.class, (Short64Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Short64Mask.class, (Short64Mask) m);  // specialized
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
    public final Short64Mask test(Test op) {
        return super.testTemplate(Short64Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Short64Mask test(Test op, VectorMask<Short> m) {
        return super.testTemplate(Short64Mask.class, op, (Short64Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Short64Mask compare(Comparison op, Vector<Short> v) {
        return super.compareTemplate(Short64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Short64Mask compare(Comparison op, short s) {
        return super.compareTemplate(Short64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Short64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short64Mask compare(Comparison op, Vector<Short> v, VectorMask<Short> m) {
        return super.compareTemplate(Short64Mask.class, op, v, (Short64Mask) m);
    }


    @Override
    @ForceInline
    public Short64Vector blend(Vector<Short> v, VectorMask<Short> m) {
        return (Short64Vector)
            super.blendTemplate(Short64Mask.class,
                                (Short64Vector) v,
                                (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector slice(int origin, Vector<Short> v) {
        return (Short64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector slice(int origin) {
        return (Short64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector unslice(int origin, Vector<Short> w, int part) {
        return (Short64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m) {
        return (Short64Vector)
            super.unsliceTemplate(Short64Mask.class,
                                  origin, w, part,
                                  (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector unslice(int origin) {
        return (Short64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector rearrange(VectorShuffle<Short> s) {
        return (Short64Vector)
            super.rearrangeTemplate(Short64Shuffle.class,
                                    (Short64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector rearrange(VectorShuffle<Short> shuffle,
                                  VectorMask<Short> m) {
        return (Short64Vector)
            super.rearrangeTemplate(Short64Shuffle.class,
                                    Short64Mask.class,
                                    (Short64Shuffle) shuffle,
                                    (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector rearrange(VectorShuffle<Short> s,
                                  Vector<Short> v) {
        return (Short64Vector)
            super.rearrangeTemplate(Short64Shuffle.class,
                                    (Short64Shuffle) s,
                                    (Short64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector compress(VectorMask<Short> m) {
        return (Short64Vector)
            super.compressTemplate(Short64Mask.class,
                                   (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector expand(VectorMask<Short> m) {
        return (Short64Vector)
            super.expandTemplate(Short64Mask.class,
                                   (Short64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector selectFrom(Vector<Short> v) {
        return (Short64Vector)
            super.selectFromTemplate((Short64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short64Vector selectFrom(Vector<Short> v,
                                   VectorMask<Short> m) {
        return (Short64Vector)
            super.selectFromTemplate((Short64Vector) v,
                                     (Short64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public short lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
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
    public Short64Vector withLane(int i, short e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Short64Vector withLaneHelper(int i, short e) {
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

    static final value class Short64Mask extends AbstractMask<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF32Z.class);

        private final VectorPayloadMF32Z payload;

        Short64Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF32Z) payload;
        }

        Short64Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        Short64Mask(boolean val) {
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
        Short64Vector toVector() {
            return (Short64Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Short64Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Short64Mask) VectorSupport.indexPartiallyInUpperRange(
                Short64Mask.class, short.class, VLENGTH, offset, limit,
                (o, l) -> (Short64Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Short64Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Short64Mask compress() {
            return (Short64Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Short64Vector.class, Short64Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Short64Mask and(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short64Mask m = (Short64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Short64Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short64Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Short64Mask or(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short64Mask m = (Short64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Short64Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short64Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Short64Mask xor(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short64Mask m = (Short64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Short64Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short64Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Short64Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short64Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Short64Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short64Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Short64Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short64Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Short64Mask.class, short.class, VLENGTH, this,
                                                      (m) -> ((Short64Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Short64Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short64Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Short64Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short64Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Short64Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Short64Mask.class, short.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Short64Mask  TRUE_MASK = new Short64Mask(true);
        private static final Short64Mask FALSE_MASK = new Short64Mask(false);

    }

    // Shuffle

    static final value class Short64Shuffle extends AbstractShuffle<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        private final VectorPayloadMF64S payload;

        Short64Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF64S) payload;
            //assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Short64Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Short64Shuffle(IntUnaryOperator fn) {
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
        static final Short64Shuffle IOTA = new Short64Shuffle(IDENTITY);

        @Override
        @ForceInline
        Short64Vector toBitsVector() {
            return (Short64Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        ShortVector toBitsVector0() {
            return Short64Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            VectorSpecies<Integer> species = IntVector.SPECIES_64;
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
        return super.fromArray0Template(Short64Mask.class, a, offset, (Short64Mask) m, offsetInRange);  // specialize
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
        return super.fromCharArray0Template(Short64Mask.class, a, offset, (Short64Mask) m, offsetInRange);  // specialize
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
        return super.fromMemorySegment0Template(Short64Mask.class, ms, offset, (Short64Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Short64Mask.class, a, offset, (Short64Mask) m);
    }



    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Short> m) {
        super.intoMemorySegment0Template(Short64Mask.class, ms, offset, (Short64Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoCharArray0(char[] a, int offset, VectorMask<Short> m) {
        super.intoCharArray0Template(Short64Mask.class, a, offset, (Short64Mask) m);
    }

    // End of specialized low-level memory operations.

    // ================================================

}

