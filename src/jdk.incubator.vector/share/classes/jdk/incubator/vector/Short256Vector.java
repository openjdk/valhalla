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
value class Short256Vector extends ShortVector {
    static final ShortSpecies VSPECIES =
        (ShortSpecies) ShortVector.SPECIES_256;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Short256Vector> VCLASS = Short256Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Short> ETYPE = short.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF256S.class);

    private final VectorPayloadMF256S payload;

    Short256Vector(Object value) {
        this.payload = (VectorPayloadMF256S) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Short256Vector ZERO = new Short256Vector(createPayloadInstance(VSPECIES));

    static final Short256Vector IOTA = new Short256Vector(VectorPayloadMF.createVectPayloadInstanceS(16, (short[])(VSPECIES.iotaArray()), false));

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
    public final Short256Vector broadcast(short e) {
        return (Short256Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Short256Vector broadcast(long e) {
        return (Short256Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Short256Mask maskFromPayload(VectorPayloadMF payload) {
        return new Short256Mask(payload);
    }

    @Override
    @ForceInline
    Short256Shuffle iotaShuffle() { return Short256Shuffle.IOTA; }

    @Override
    @ForceInline
    Short256Shuffle shuffleFromArray(int[] indices, int i) { return new Short256Shuffle(indices, i); }

    @Override
    @ForceInline
    Short256Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Short256Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Short256Vector vectorFactory(VectorPayloadMF vec) {
        return new Short256Vector(vec);
    }

    @ForceInline
    final @Override
    Byte256Vector asByteVectorRaw() {
        return (Byte256Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Short256Vector uOpMF(FUnOp f) {
        return (Short256Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Short256Vector uOpMF(VectorMask<Short> m, FUnOp f) {
        return (Short256Vector)
            super.uOpTemplateMF((Short256Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Short256Vector bOpMF(Vector<Short> v, FBinOp f) {
        return (Short256Vector) super.bOpTemplateMF((Short256Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Short256Vector bOpMF(Vector<Short> v,
                     VectorMask<Short> m, FBinOp f) {
        return (Short256Vector)
            super.bOpTemplateMF((Short256Vector)v, (Short256Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Short256Vector tOpMF(Vector<Short> v1, Vector<Short> v2, FTriOp f) {
        return (Short256Vector)
            super.tOpTemplateMF((Short256Vector)v1, (Short256Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Short256Vector tOpMF(Vector<Short> v1, Vector<Short> v2,
                     VectorMask<Short> m, FTriOp f) {
        return (Short256Vector)
            super.tOpTemplateMF((Short256Vector)v1, (Short256Vector)v2,
                                (Short256Mask)m, f);  // specialize
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
    public Short256Vector lanewise(Unary op) {
        return (Short256Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector lanewise(Unary op, VectorMask<Short> m) {
        return (Short256Vector) super.lanewiseTemplate(op, Short256Mask.class, (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector lanewise(Binary op, Vector<Short> v) {
        return (Short256Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector lanewise(Binary op, Vector<Short> v, VectorMask<Short> m) {
        return (Short256Vector) super.lanewiseTemplate(op, Short256Mask.class, v, (Short256Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short256Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Short256Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short256Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Short> m) {
        return (Short256Vector) super.lanewiseShiftTemplate(op, Short256Mask.class, e, (Short256Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Short256Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2) {
        return (Short256Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short256Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2, VectorMask<Short> m) {
        return (Short256Vector) super.lanewiseTemplate(op, Short256Mask.class, v1, v2, (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short256Vector addIndex(int scale) {
        return (Short256Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Short256Mask.class, (Short256Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Short256Mask.class, (Short256Mask) m);  // specialized
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
    public final Short256Mask test(Test op) {
        return super.testTemplate(Short256Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Short256Mask test(Test op, VectorMask<Short> m) {
        return super.testTemplate(Short256Mask.class, op, (Short256Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Short256Mask compare(Comparison op, Vector<Short> v) {
        return super.compareTemplate(Short256Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Short256Mask compare(Comparison op, short s) {
        return super.compareTemplate(Short256Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short256Mask compare(Comparison op, long s) {
        return super.compareTemplate(Short256Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short256Mask compare(Comparison op, Vector<Short> v, VectorMask<Short> m) {
        return super.compareTemplate(Short256Mask.class, op, v, (Short256Mask) m);
    }


    @Override
    @ForceInline
    public Short256Vector blend(Vector<Short> v, VectorMask<Short> m) {
        return (Short256Vector)
            super.blendTemplate(Short256Mask.class,
                                (Short256Vector) v,
                                (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector slice(int origin, Vector<Short> v) {
        return (Short256Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector slice(int origin) {
        return (Short256Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector unslice(int origin, Vector<Short> w, int part) {
        return (Short256Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m) {
        return (Short256Vector)
            super.unsliceTemplate(Short256Mask.class,
                                  origin, w, part,
                                  (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector unslice(int origin) {
        return (Short256Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector rearrange(VectorShuffle<Short> s) {
        return (Short256Vector)
            super.rearrangeTemplate(Short256Shuffle.class,
                                    (Short256Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector rearrange(VectorShuffle<Short> shuffle,
                                  VectorMask<Short> m) {
        return (Short256Vector)
            super.rearrangeTemplate(Short256Shuffle.class,
                                    Short256Mask.class,
                                    (Short256Shuffle) shuffle,
                                    (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector rearrange(VectorShuffle<Short> s,
                                  Vector<Short> v) {
        return (Short256Vector)
            super.rearrangeTemplate(Short256Shuffle.class,
                                    (Short256Shuffle) s,
                                    (Short256Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector compress(VectorMask<Short> m) {
        return (Short256Vector)
            super.compressTemplate(Short256Mask.class,
                                   (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector expand(VectorMask<Short> m) {
        return (Short256Vector)
            super.expandTemplate(Short256Mask.class,
                                   (Short256Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector selectFrom(Vector<Short> v) {
        return (Short256Vector)
            super.selectFromTemplate((Short256Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short256Vector selectFrom(Vector<Short> v,
                                   VectorMask<Short> m) {
        return (Short256Vector)
            super.selectFromTemplate((Short256Vector) v,
                                     (Short256Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public short lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
            case 4: return laneHelper(4);
            case 5: return laneHelper(5);
            case 6: return laneHelper(6);
            case 7: return laneHelper(7);
            case 8: return laneHelper(8);
            case 9: return laneHelper(9);
            case 10: return laneHelper(10);
            case 11: return laneHelper(11);
            case 12: return laneHelper(12);
            case 13: return laneHelper(13);
            case 14: return laneHelper(14);
            case 15: return laneHelper(15);
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
    public Short256Vector withLane(int i, short e) {
        switch (i) {
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

    public Short256Vector withLaneHelper(int i, short e) {
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

    static final value class Short256Mask extends AbstractMask<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF128Z.class);

        private final VectorPayloadMF128Z payload;

        Short256Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF128Z) payload;
        }

        Short256Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VSPECIES));
        }

        Short256Mask(boolean val) {
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
        Short256Vector toVector() {
            return (Short256Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Short256Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Short256Mask) VectorSupport.indexPartiallyInUpperRange(
                Short256Mask.class, short.class, VLENGTH, offset, limit,
                (o, l) -> (Short256Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Short256Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Short256Mask compress() {
            return (Short256Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Short256Vector.class, Short256Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Short256Mask and(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short256Mask m = (Short256Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Short256Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short256Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Short256Mask or(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short256Mask m = (Short256Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Short256Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short256Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Short256Mask xor(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short256Mask m = (Short256Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Short256Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short256Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Short256Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short256Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Short256Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short256Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Short256Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short256Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Short256Mask.class, short.class, VLENGTH, this,
                                                      (m) -> ((Short256Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Short256Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short256Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Short256Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short256Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Short256Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Short256Mask.class, short.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Short256Mask  TRUE_MASK = new Short256Mask(true);
        private static final Short256Mask FALSE_MASK = new Short256Mask(false);

    }

    // Shuffle

    static final value class Short256Shuffle extends AbstractShuffle<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        private final VectorPayloadMF256S payload;

        Short256Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF256S) payload;
            //assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Short256Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Short256Shuffle(IntUnaryOperator fn) {
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
        static final Short256Shuffle IOTA = new Short256Shuffle(IDENTITY);

        @Override
        @ForceInline
        Short256Vector toBitsVector() {
            return (Short256Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        ShortVector toBitsVector0() {
            return Short256Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            VectorSpecies<Integer> species = IntVector.SPECIES_256;
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
        return super.fromArray0Template(Short256Mask.class, a, offset, (Short256Mask) m, offsetInRange);  // specialize
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
        return super.fromCharArray0Template(Short256Mask.class, a, offset, (Short256Mask) m, offsetInRange);  // specialize
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
        return super.fromMemorySegment0Template(Short256Mask.class, ms, offset, (Short256Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Short256Mask.class, a, offset, (Short256Mask) m);
    }



    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Short> m) {
        super.intoMemorySegment0Template(Short256Mask.class, ms, offset, (Short256Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoCharArray0(char[] a, int offset, VectorMask<Short> m) {
        super.intoCharArray0Template(Short256Mask.class, a, offset, (Short256Mask) m);
    }

    // End of specialized low-level memory operations.

    // ================================================

}

