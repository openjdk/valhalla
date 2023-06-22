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
value class Byte64Vector extends ByteVector {
    static final ByteSpecies VSPECIES =
        (ByteSpecies) ByteVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Byte64Vector> VCLASS = Byte64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Byte> ETYPE = byte.class; // used by the JVM

    static final long MFOFFSET = VectorPayloadMF.multiFieldOffset(VectorPayloadMF64B.class);

    private final VectorPayloadMF64B payload;

    Byte64Vector(Object value) {
        this.payload = (VectorPayloadMF64B) value;
    }

    @ForceInline
    @Override
    final VectorPayloadMF vec() {
        return payload;
    }

    static final Byte64Vector ZERO = new Byte64Vector(VectorPayloadMF.newInstanceFactory(byte.class, 8));
    static final Byte64Vector IOTA = new Byte64Vector(VectorPayloadMF.createVectPayloadInstanceB(8, (byte[])(VSPECIES.iotaArray())));

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
    public ByteSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Byte> elementType() { return byte.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Byte.SIZE; }

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
    public final Byte64Vector broadcast(byte e) {
        return (Byte64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Vector broadcast(long e) {
        return (Byte64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Byte64Mask maskFromPayload(VectorPayloadMF payload) {
        return new Byte64Mask(payload);
    }

    @Override
    @ForceInline
    Byte64Shuffle iotaShuffle() { return Byte64Shuffle.IOTA; }

    @Override
    @ForceInline
    Byte64Shuffle shuffleFromArray(int[] indices, int i) { return new Byte64Shuffle(indices, i); }

    @Override
    @ForceInline
    Byte64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Byte64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Byte64Vector vectorFactory(VectorPayloadMF vec) {
        return new Byte64Vector(vec);
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
    Byte64Vector uOpMF(FUnOp f) {
        return (Byte64Vector) super.uOpTemplateMF(f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector uOpMF(VectorMask<Byte> m, FUnOp f) {
        return (Byte64Vector)
            super.uOpTemplateMF((Byte64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Byte64Vector bOpMF(Vector<Byte> v, FBinOp f) {
        return (Byte64Vector) super.bOpTemplateMF((Byte64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector bOpMF(Vector<Byte> v,
                     VectorMask<Byte> m, FBinOp f) {
        return (Byte64Vector)
            super.bOpTemplateMF((Byte64Vector)v, (Byte64Mask)m,
                                f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Byte64Vector tOpMF(Vector<Byte> v1, Vector<Byte> v2, FTriOp f) {
        return (Byte64Vector)
            super.tOpTemplateMF((Byte64Vector)v1, (Byte64Vector)v2,
                                f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector tOpMF(Vector<Byte> v1, Vector<Byte> v2,
                     VectorMask<Byte> m, FTriOp f) {
        return (Byte64Vector)
            super.tOpTemplateMF((Byte64Vector)v1, (Byte64Vector)v2,
                                (Byte64Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    byte rOpMF(byte v, VectorMask<Byte> m, FBinOp f) {
        return super.rOpTemplateMF(v, m, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Byte,F> conv,
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
    public Byte64Vector lanewise(Unary op) {
        return (Byte64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector lanewise(Unary op, VectorMask<Byte> m) {
        return (Byte64Vector) super.lanewiseTemplate(op, Byte64Mask.class, (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector lanewise(Binary op, Vector<Byte> v) {
        return (Byte64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector lanewise(Binary op, Vector<Byte> v, VectorMask<Byte> m) {
        return (Byte64Vector) super.lanewiseTemplate(op, Byte64Mask.class, v, (Byte64Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Byte64Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Byte64Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Byte64Vector
    lanewiseShift(VectorOperators.Binary op, int e, VectorMask<Byte> m) {
        return (Byte64Vector) super.lanewiseShiftTemplate(op, Byte64Mask.class, e, (Byte64Mask) m);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Byte64Vector
    lanewise(Ternary op, Vector<Byte> v1, Vector<Byte> v2) {
        return (Byte64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Byte64Vector
    lanewise(Ternary op, Vector<Byte> v1, Vector<Byte> v2, VectorMask<Byte> m) {
        return (Byte64Vector) super.lanewiseTemplate(op, Byte64Mask.class, v1, v2, (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public final
    Byte64Vector addIndex(int scale) {
        return (Byte64Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final byte reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final byte reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Byte> m) {
        return super.reduceLanesTemplate(op, Byte64Mask.class, (Byte64Mask) m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Byte> m) {
        return (long) super.reduceLanesTemplate(op, Byte64Mask.class, (Byte64Mask) m);  // specialized
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
    public final Byte64Mask test(Test op) {
        return super.testTemplate(Byte64Mask.class, op);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask test(Test op, VectorMask<Byte> m) {
        return super.testTemplate(Byte64Mask.class, op, (Byte64Mask) m);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, Vector<Byte> v) {
        return super.compareTemplate(Byte64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, byte s) {
        return super.compareTemplate(Byte64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Byte64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, Vector<Byte> v, VectorMask<Byte> m) {
        return super.compareTemplate(Byte64Mask.class, op, v, (Byte64Mask) m);
    }


    @Override
    @ForceInline
    public Byte64Vector blend(Vector<Byte> v, VectorMask<Byte> m) {
        return (Byte64Vector)
            super.blendTemplate(Byte64Mask.class,
                                (Byte64Vector) v,
                                (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector slice(int origin, Vector<Byte> v) {
        return (Byte64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector slice(int origin) {
        return (Byte64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin, Vector<Byte> w, int part) {
        return (Byte64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin, Vector<Byte> w, int part, VectorMask<Byte> m) {
        return (Byte64Vector)
            super.unsliceTemplate(Byte64Mask.class,
                                  origin, w, part,
                                  (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin) {
        return (Byte64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> s) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    (Byte64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> shuffle,
                                  VectorMask<Byte> m) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    Byte64Mask.class,
                                    (Byte64Shuffle) shuffle,
                                    (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> s,
                                  Vector<Byte> v) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    (Byte64Shuffle) s,
                                    (Byte64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector compress(VectorMask<Byte> m) {
        return (Byte64Vector)
            super.compressTemplate(Byte64Mask.class,
                                   (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector expand(VectorMask<Byte> m) {
        return (Byte64Vector)
            super.expandTemplate(Byte64Mask.class,
                                   (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector selectFrom(Vector<Byte> v) {
        return (Byte64Vector)
            super.selectFromTemplate((Byte64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector selectFrom(Vector<Byte> v,
                                   VectorMask<Byte> m) {
        return (Byte64Vector)
            super.selectFromTemplate((Byte64Vector) v,
                                     (Byte64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public byte lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
            case 4: return laneHelper(4);
            case 5: return laneHelper(5);
            case 6: return laneHelper(6);
            case 7: return laneHelper(7);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public byte laneHelper(int i) {
        return (byte) VectorSupport.extract(
                             VCLASS, ETYPE, VLENGTH,
                             this, i,
                             (vec, ix) -> {
                                 VectorPayloadMF vecpayload = vec.vec();
                                 long start_offset = vecpayload.multiFieldOffset();
                                 return (long)Unsafe.getUnsafe().getByte(vecpayload, start_offset + ix * Byte.BYTES);
                             });
    }

    @ForceInline
    @Override
    public Byte64Vector withLane(int i, byte e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            case 4: return withLaneHelper(4, e);
            case 5: return withLaneHelper(5, e);
            case 6: return withLaneHelper(6, e);
            case 7: return withLaneHelper(7, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Byte64Vector withLaneHelper(int i, byte e) {
       return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    VectorPayloadMF vec = v.vec();
                                    VectorPayloadMF tpayload = Unsafe.getUnsafe().makePrivateBuffer(vec);
                                    long start_offset = tpayload.multiFieldOffset();
                                    Unsafe.getUnsafe().putByte(tpayload, start_offset + ix * Byte.BYTES, (byte)bits);
                                    tpayload = Unsafe.getUnsafe().finishPrivateBuffer(tpayload);
                                    return v.vectorFactory(tpayload);
                                });
    }

    // Mask

    static final value class Byte64Mask extends AbstractMask<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        private final VectorPayloadMF64Z payload;

        Byte64Mask(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF64Z) payload;
        }

        Byte64Mask(VectorPayloadMF payload, int offset) {
            this(prepare(payload, offset, VLENGTH));
        }

        Byte64Mask(boolean val) {
            this(prepare(val, VLENGTH));
        }

        @ForceInline
        final @Override
        public ByteSpecies vspecies() {
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
        Byte64Vector toVector() {
            return (Byte64Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        /*package-private*/
        Byte64Mask indexPartiallyInUpperRange(long offset, long limit) {
            return (Byte64Mask) VectorSupport.indexPartiallyInUpperRange(
                Byte64Mask.class, byte.class, VLENGTH, offset, limit,
                (o, l) -> (Byte64Mask) TRUE_MASK.indexPartiallyInRange(o, l));
        }

        // Unary operations

        @Override
        @ForceInline
        public Byte64Mask not() {
            return xor(maskAll(true));
        }

        @Override
        @ForceInline
        public Byte64Mask compress() {
            return (Byte64Mask) VectorSupport.compressExpandOp(VectorSupport.VECTOR_OP_MASK_COMPRESS,
                Byte64Vector.class, Byte64Mask.class, ETYPE, VLENGTH, null, this,
                (v1, m1) -> VSPECIES.iota().compare(VectorOperators.LT, m1.trueCount()));
        }


        // Binary operations

        @Override
        @ForceInline
        public Byte64Mask and(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Byte64Mask.class, null,
                                          byte.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Byte64Mask) m1.bOpMF(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Byte64Mask or(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Byte64Mask.class, null,
                                          byte.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Byte64Mask) m1.bOpMF(m2, (i, a, b) -> a | b));
        }

        @Override
        @ForceInline
        public Byte64Mask xor(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Byte64Mask.class, null,
                                          byte.class, VLENGTH, this, m, null,
                                          (m1, m2, vm) -> (Byte64Mask) m1.bOpMF(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Byte64Mask.class, byte.class, VLENGTH, this,
                                                            (m) -> ((Byte64Mask) m).trueCountHelper());
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Byte64Mask.class, byte.class, VLENGTH, this,
                                                            (m) -> ((Byte64Mask) m).firstTrueHelper());
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return (int) VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Byte64Mask.class, byte.class, VLENGTH, this,
                                                            (m) -> ((Byte64Mask) m).lastTrueHelper());
        }

        @Override
        @ForceInline
        public long toLong() {
            if (length() > Long.SIZE) {
                throw new UnsupportedOperationException("too many lanes for one long");
            }
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TOLONG, Byte64Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> ((Byte64Mask) m).toLongHelper());
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Byte64Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Byte64Mask) m).anyTrueHelper());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Byte64Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> ((Byte64Mask) m).allTrueHelper());
        }

        @ForceInline
        /*package-private*/
        static Byte64Mask maskAll(boolean bit) {
            return VectorSupport.fromBitsCoerced(Byte64Mask.class, byte.class, VLENGTH,
                                                 (bit ? -1 : 0), MODE_BROADCAST, null,
                                                 (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Byte64Mask  TRUE_MASK = new Byte64Mask(true);
        private static final Byte64Mask FALSE_MASK = new Byte64Mask(false);

    }

    // Shuffle

    static final value class Byte64Shuffle extends AbstractShuffle<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        private final VectorPayloadMF64B payload;

        Byte64Shuffle(VectorPayloadMF payload) {
            this.payload = (VectorPayloadMF64B) payload;
            assert(VLENGTH == payload.length());
            assert(indicesInRange(payload));
        }

        Byte64Shuffle(int[] indices, int i) {
            this(prepare(indices, i));
        }

        Byte64Shuffle(IntUnaryOperator fn) {
            this(prepare(fn));
        }

        @ForceInline
        @Override
        protected final VectorPayloadMF indices() {
            return payload;
        }

        @Override
        @ForceInline
        public ByteSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Byte64Shuffle IOTA = new Byte64Shuffle(IDENTITY);

        @Override
        @ForceInline
        Byte64Vector toBitsVector() {
            return (Byte64Vector) super.toBitsVectorTemplate();
        }

        @Override
        @ForceInline
        ByteVector toBitsVector0() {
            return Byte64Vector.VSPECIES.dummyVectorMF().vectorFactory(indices());
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
            Vector<Byte> v = toBitsVector();
            v.convertShape(VectorOperators.B2I, species, 0)
                    .reinterpretAsInts()
                    .intoArray(a, offset);
            v.convertShape(VectorOperators.B2I, species, 1)
                    .reinterpretAsInts()
                    .intoArray(a, offset + species.length());
            v.convertShape(VectorOperators.B2I, species, 2)
                    .reinterpretAsInts()
                    .intoArray(a, offset + species.length() * 2);
            v.convertShape(VectorOperators.B2I, species, 3)
                    .reinterpretAsInts()
                    .intoArray(a, offset + species.length() * 3);
        }

        private static VectorPayloadMF prepare(int[] indices, int offset) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(byte.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long mfOffset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = indices[offset + i];
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putByte(payload, mfOffset + i * Byte.BYTES, (byte) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }

        private static VectorPayloadMF prepare(IntUnaryOperator f) {
            VectorPayloadMF payload = VectorPayloadMF.newInstanceFactory(byte.class, VLENGTH);
            payload = Unsafe.getUnsafe().makePrivateBuffer(payload);
            long offset = payload.multiFieldOffset();
            for (int i = 0; i < VLENGTH; i++) {
                int si = f.applyAsInt(i);
                si = partiallyWrapIndex(si, VLENGTH);
                Unsafe.getUnsafe().putByte(payload, offset + i * Byte.BYTES, (byte) si);
            }
            payload = Unsafe.getUnsafe().finishPrivateBuffer(payload);
            return payload;
        }


        private static boolean indicesInRange(VectorPayloadMF indices) {
            int length = indices.length();
            long offset = indices.multiFieldOffset();
            for (int i = 0; i < length; i++) {
                byte si = Unsafe.getUnsafe().getByte(indices, offset + i * Byte.BYTES);
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
    ByteVector fromArray0(byte[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromArray0(byte[] a, int offset, VectorMask<Byte> m, int offsetInRange) {
        return super.fromArray0Template(Byte64Mask.class, a, offset, (Byte64Mask) m, offsetInRange);  // specialize
    }



    @ForceInline
    @Override
    final
    ByteVector fromBooleanArray0(boolean[] a, int offset) {
        return super.fromBooleanArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromBooleanArray0(boolean[] a, int offset, VectorMask<Byte> m, int offsetInRange) {
        return super.fromBooleanArray0Template(Byte64Mask.class, a, offset, (Byte64Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromMemorySegment0(MemorySegment ms, long offset) {
        return super.fromMemorySegment0Template(ms, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromMemorySegment0(MemorySegment ms, long offset, VectorMask<Byte> m, int offsetInRange) {
        return super.fromMemorySegment0Template(Byte64Mask.class, ms, offset, (Byte64Mask) m, offsetInRange);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(byte[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(byte[] a, int offset, VectorMask<Byte> m) {
        super.intoArray0Template(Byte64Mask.class, a, offset, (Byte64Mask) m);
    }


    @ForceInline
    @Override
    final
    void intoBooleanArray0(boolean[] a, int offset, VectorMask<Byte> m) {
        super.intoBooleanArray0Template(Byte64Mask.class, a, offset, (Byte64Mask) m);
    }

    @ForceInline
    @Override
    final
    void intoMemorySegment0(MemorySegment ms, long offset, VectorMask<Byte> m) {
        super.intoMemorySegment0Template(Byte64Mask.class, ms, offset, (Byte64Mask) m);
    }


    // End of specialized low-level memory operations.

    // ================================================

}

