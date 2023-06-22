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
value class Short512Vector extends ShortVector {
    static final ShortSpecies VSPECIES =
        (ShortSpecies) ShortVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Short512Vector> VCLASS = Short512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Short> ETYPE = short.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF512S.class);

    private final VectorPayloadMF512S payload;

    Short512Vector(Object value) {
        this.payload = (VectorPayloadMF512S) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Short512Vector ZERO = new Short512Vector(VectorPayloadMF.newInstanceFactory(short.class, 32));
    static final Short512Vector IOTA = new Short512Vector(VectorPayloadMF.createVectPayloadInstanceS(32, (short[])(VSPECIES.iotaArray())));

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
    public final Short512Vector broadcast(short e) {
        return (Short512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Short512Vector broadcast(long e) {
        return (Short512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Short512Mask maskFromPayload(VectorPayloadMF payload) {
        return new Short512Mask(payload);
    }

    @Override
    @ForceInline
    Short512Shuffle iotaShuffle() { return Short512Shuffle.IOTA; }

    @Override
    @ForceInline
    Short512Shuffle shuffleFromArray(int[] indices, int i) { return new Short512Shuffle(indices, i); }

    @Override
    @ForceInline
    Short512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Short512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Short512Vector vectorFactory(VectorPayloadMF vec) {
        return new Short512Vector(vec);
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
    Short512Vector uOpMF(FUnOp f) {
        return (Short512Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Short512Vector uOpMF(VectorMask<Short> m, FUnOp f) {
        return (Short512Vector)
            super.uOpTemplateMF((Short512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Short512Vector bOpMF(Vector<Short> v, FBinOp f) {
        return (Short512Vector) super.bOpTemplateMF((Short512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Short512Vector bOpMF(Vector<Short> v,
                     VectorMask<Short> m, FBinOp f) {
        return (Short512Vector)
            super.bOpTemplateMF((Short512Vector)v, (Short512Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Short512Vector tOpMF(Vector<Short> v1, Vector<Short> v2, FTriOp f) {
        return (Short512Vector)
            super.tOpTemplateMF((Short512Vector)v1, (Short512Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Short512Vector tOpMF(Vector<Short> v1, Vector<Short> v2,
                     VectorMask<Short> m, FTriOp f) {
        return (Short512Vector)
            super.tOpTemplateMF((Short512Vector)v1, (Short512Vector)v2,
                                (Short512Mask)m, f);  // specialize
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
    public Short512Vector lanewise(Unary op) {
        return (Short512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector lanewise(Unary op, VectorMask<Short> m) {
        return (Short512Vector) super.lanewiseTemplate(op, Short512Mask.class, (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector lanewise(Binary op, Vector<Short> v) {
        return (Short512Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector lanewise(Binary op, Vector<Short> v, VectorMask<Short> m) {
        return (Short512Vector) super.lanewiseTemplate(op, Short512Mask.class, v, (Short512Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short512Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Short512Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short512Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Short> m) {
        return (Short512Vector) super.lanewiseShiftTemplate(op, Short512Mask.class, e, (Short512Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Short512Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2) {
        return (Short512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short512Vector
    lanewise(Ternary op, Vector<Short> v1, Vector<Short> v2, VectorMask<Short> m) {
        return (Short512Vector) super.lanewiseTemplate(op, Short512Mask.class, v1, v2, (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short512Vector addIndex(int scale) {
        return (Short512Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.reduceLanesTemplate(op, Short512Mask.class, (Short512Mask) m);  // specialized
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
        return (long) super.reduceLanesTemplate(op, Short512Mask.class, (Short512Mask) m);  // specialized
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
    public final Short512Mask test(Test op) {
        return super.testTemplate(Short512Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Short512Mask test(Test op, VectorMask<Short> m) {
        return super.testTemplate(Short512Mask.class, op, (Short512Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Short512Mask compare(Comparison op, Vector<Short> v) {
        return super.compareTemplate(Short512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Short512Mask compare(Comparison op, short s) {
        return super.compareTemplate(Short512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Short512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short512Mask compare(Comparison op, Vector<Short> v, VectorMask<Short> m) {
        return super.compareTemplate(Short512Mask.class, op, v, (Short512Mask) m);
    }


    @Override
    @ForceInline
    public Short512Vector blend(Vector<Short> v, VectorMask<Short> m) {
        return (Short512Vector)
            super.blendTemplate(Short512Mask.class,
                                (Short512Vector) v,
                                (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector slice(int origin, Vector<Short> v) {
        return (Short512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector slice(int origin) {
        return (Short512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector unslice(int origin, Vector<Short> w, int part) {
        return (Short512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m) {
        return (Short512Vector)
            super.unsliceTemplate(Short512Mask.class,
                                  origin, w, part,
                                  (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector unslice(int origin) {
        return (Short512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector rearrange(VectorShuffle<Short> s) {
        return (Short512Vector)
            super.rearrangeTemplate(Short512Shuffle.class,
                                    (Short512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector rearrange(VectorShuffle<Short> shuffle,
                                  VectorMask<Short> m) {
        return (Short512Vector)
            super.rearrangeTemplate(Short512Shuffle.class,
                                    Short512Mask.class,
                                    (Short512Shuffle) shuffle,
                                    (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector rearrange(VectorShuffle<Short> s,
                                  Vector<Short> v) {
        return (Short512Vector)
            super.rearrangeTemplate(Short512Shuffle.class,
                                    (Short512Shuffle) s,
                                    (Short512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector compress(VectorMask<Short> m) {
        return (Short512Vector)
            super.compressTemplate(Short512Mask.class,
                                   (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector expand(VectorMask<Short> m) {
        return (Short512Vector)
            super.expandTemplate(Short512Mask.class,
                                   (Short512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector selectFrom(Vector<Short> v) {
        return (Short512Vector)
            super.selectFromTemplate((Short512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short512Vector selectFrom(Vector<Short> v,
                                   VectorMask<Short> m) {
        return (Short512Vector)
            super.selectFromTemplate((Short512Vector) v,
                                     (Short512Mask) m);  // specialize
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
            case 16: return laneHelper(16);
            case 17: return laneHelper(17);
            case 18: return laneHelper(18);
            case 19: return laneHelper(19);
            case 20: return laneHelper(20);
            case 21: return laneHelper(21);
            case 22: return laneHelper(22);
            case 23: return laneHelper(23);
            case 24: return laneHelper(24);
            case 25: return laneHelper(25);
            case 26: return laneHelper(26);
            case 27: return laneHelper(27);
            case 28: return laneHelper(28);
            case 29: return laneHelper(29);
            case 30: return laneHelper(30);
            case 31: return laneHelper(31);
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
    public Short512Vector withLane(int i, short e) {
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
            case 16: return withLaneHelper(16, e);
            case 17: return withLaneHelper(17, e);
            case 18: return withLaneHelper(18, e);
            case 19: return withLaneHelper(19, e);
            case 20: return withLaneHelper(20, e);
            case 21: return withLaneHelper(21, e);
            case 22: return withLaneHelper(22, e);
            case 23: return withLaneHelper(23, e);
            case 24: return withLaneHelper(24, e);
            case 25: return withLaneHelper(25, e);
            case 26: return withLaneHelper(26, e);
            case 27: return withLaneHelper(27, e);
            case 28: return withLaneHelper(28, e);
            case 29: return withLaneHelper(29, e);
            case 30: return withLaneHelper(30, e);
            case 31: return withLaneHelper(31, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Short512Vector withLaneHelper(int i, short e) {
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

    static final value class Short512Mask extends AbstractMask<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        private final VectorPayloadMF256Z payload;

        Short512Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF256Z) payload;
        }

        Short512Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Short512Mask(boolean val) {
            this(prepare(val, VLENGTH));
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
        public final
        Short512Vector toVector() {
            return (Short512Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Short512Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Short512Mask) VectorSupport.indexPartiallyInUpperRange(
                Short512Mask.class, short.class, VLENGTH, offset, limit,
                (o, l) -> (Short512Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Short512Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Short512Mask compress() {
            return (Short512Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Short512Vector.class, Short512Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Short512Mask and(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short512Mask m = (Short512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Short512Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short512Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Short512Mask or(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short512Mask m = (Short512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Short512Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short512Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Short512Mask xor(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short512Mask m = (Short512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Short512Mask.class, null,
                                          short.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Short512Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Short512Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short512Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Short512Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short512Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Short512Mask.class, short.class, VLENGTH, this,
                                                            (m) -> ((Short512Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Short512Mask.class, short.class, VLENGTH, this,
                                                      (m) -> ((Short512Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Short512Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short512Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Short512Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Short512Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Short512Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Short512Mask.class, short.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Short512Mask  TRUE_MASK = new Short512Mask(true);
        private static final Short512Mask FALSE_MASK = new Short512Mask(false);

    }

    // Shuffle

    static final value class Short512Shuffle extends AbstractShuffle<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        private final VectorPayloadMF512S payload;

        Short512Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF512S) payload;
            assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Short512Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Short512Shuffle(IntUnaryOperator fn) {
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
        static final Short512Shuffle IOTA = new Short512Shuffle(IDENTITY);

        @Override
        @ForceInline
        Short512Vector toBitsVector() {
            return (Short512Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        ShortVector toBitsVector0() {
            return Short512Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
        }

        @Override
        @ForceInline
        public int laneSource(int i) {
            return (int)toBitsVector().lane(i);
        }

        @Override
        @ForceInline
        public void intoArray(int[] a, int offset) {
            VectorSpecies<Integer> species = IntVector.SPECIES_512;
            Vector<Short> v = toBitsVector();
            v.convertShape(VectorOperators.S2I, species, 0)
                    .reinterpretAsInts()
                    .intoArray(a, offset);
            v.convertShape(VectorOperators.S2I, species, 1)
                    .reinterpretAsInts()
                    .intoArray(a, offset + species.length());
        }

        private static VectorPayloadMF prepare(int[] indices, int offset) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(short.class, VLENGTH);
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
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(short.class, VLENGTH);
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
            int length = indices.length();
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
        return super.fromArray0Template(Short512Mask.class, a, offset, (Short512Mask) m, offsetInRange);  // specialize
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
        return super.fromCharArray0Template(Short512Mask.class, a, offset, (Short512Mask) m, offsetInRange);  // specialize
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
        return super.fromMemorySegment0Template(Short512Mask.class, ms, offset, (Short512Mask) m, offsetInRange);  // specialize
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
        super.intoArray0Template(Short512Mask.class, a, offset, (Short512Mask) m);
    }



    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Short> m) {
        super.intoMemorySegment0Template(Short512Mask.class, ms, offset, (Short512Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoCharArray0(char[] a, int offset, VectorMask<Short> m) {
        super.intoCharArray0Template(Short512Mask.class, a, offset, (Short512Mask) m);
    }

    // End of specialized low-level memory operations.

    // ================================================

}

