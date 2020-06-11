/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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



package org.graalvm.compiler.asm.aarch64;

import static jdk.vm.ci.aarch64.AArch64.CPU;
import static jdk.vm.ci.aarch64.AArch64.rscratch1;
import static jdk.vm.ci.aarch64.AArch64.rscratch2;
import static jdk.vm.ci.aarch64.AArch64.sp;
import static jdk.vm.ci.aarch64.AArch64.zr;
import static org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode.BASE_REGISTER_ONLY;
import static org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode.EXTENDED_REGISTER_OFFSET;
import static org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode.IMMEDIATE_SCALED;
import static org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode.IMMEDIATE_UNSCALED;
import static org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode.REGISTER_OFFSET;
import static org.graalvm.compiler.asm.aarch64.AArch64Assembler.Instruction.LDP;
import static org.graalvm.compiler.asm.aarch64.AArch64Assembler.Instruction.STP;
import static org.graalvm.compiler.asm.aarch64.AArch64MacroAssembler.AddressGenerationPlan.WorkPlan.ADD_TO_BASE;
import static org.graalvm.compiler.asm.aarch64.AArch64MacroAssembler.AddressGenerationPlan.WorkPlan.ADD_TO_INDEX;
import static org.graalvm.compiler.asm.aarch64.AArch64MacroAssembler.AddressGenerationPlan.WorkPlan.NO_WORK;

import org.graalvm.compiler.asm.BranchTargetOutOfBoundsException;
import org.graalvm.compiler.asm.Label;
import org.graalvm.compiler.asm.aarch64.AArch64MacroAssembler.MovSequenceAnnotation.MovAction;
import org.graalvm.compiler.core.common.NumUtil;
import org.graalvm.compiler.debug.GraalError;

import jdk.vm.ci.aarch64.AArch64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;

public class AArch64MacroAssembler extends AArch64Assembler {

    private final ScratchRegister[] scratchRegister = new ScratchRegister[]{new ScratchRegister(rscratch1), new ScratchRegister(rscratch2)};

    // Points to the next free scratch register
    private int nextFreeScratchRegister = 0;

    // Last immediate ldr/str instruction, which is a candidate to be merged.
    private AArch64MemoryEncoding lastImmLoadStoreEncoding;
    private boolean isImmLoadStoreMerged = false;

    public AArch64MacroAssembler(TargetDescription target) {
        super(target);
    }

    public class ScratchRegister implements AutoCloseable {
        private final Register register;

        public ScratchRegister(Register register) {
            this.register = register;
        }

        public Register getRegister() {
            return register;
        }

        @Override
        public void close() {
            assert nextFreeScratchRegister > 0 : "Close called too often";
            nextFreeScratchRegister--;
        }
    }

    public ScratchRegister getScratchRegister() {
        return scratchRegister[nextFreeScratchRegister++];
    }

    @Override
    public void bind(Label l) {
        super.bind(l);
        // Clear last ldr/str instruction to prevent the labeled ldr/str being merged.
        lastImmLoadStoreEncoding = null;
    }

    private static class AArch64MemoryEncoding {
        private AArch64Address address;
        private Register result;
        private int sizeInBytes;
        private int position;
        private boolean isStore;

        AArch64MemoryEncoding(int sizeInBytes, Register result, AArch64Address address, boolean isStore, int position) {
            this.sizeInBytes = sizeInBytes;
            this.result = result;
            this.address = address;
            this.isStore = isStore;
            this.position = position;
            AArch64Address.AddressingMode addressingMode = address.getAddressingMode();
            assert addressingMode == IMMEDIATE_SCALED || addressingMode == IMMEDIATE_UNSCALED : "Invalid address mode" +
                            "to merge: " + addressingMode;
        }

        Register getBase() {
            return address.getBase();
        }

        int getOffset() {
            if (address.getAddressingMode() == IMMEDIATE_UNSCALED) {
                return address.getImmediateRaw();
            }
            return address.getImmediate() * sizeInBytes;
        }
    }

    /**
     * Specifies what actions have to be taken to turn an arbitrary address of the form
     * {@code base + displacement [+ index [<< scale]]} into a valid AArch64Address.
     */
    public static class AddressGenerationPlan {
        public final WorkPlan workPlan;
        public final AArch64Address.AddressingMode addressingMode;
        public final boolean needsScratch;

        public enum WorkPlan {
            /**
             * Can be used as-is without extra work.
             */
            NO_WORK,
            /**
             * Add scaled displacement to index register.
             */
            ADD_TO_INDEX,
            /**
             * Add unscaled displacement to base register.
             */
            ADD_TO_BASE,
        }

        /**
         * @param workPlan Work necessary to generate a valid address.
         * @param addressingMode Addressing mode of generated address.
         * @param needsScratch True if generating address needs a scatch register, false otherwise.
         */
        public AddressGenerationPlan(WorkPlan workPlan, AArch64Address.AddressingMode addressingMode, boolean needsScratch) {
            this.workPlan = workPlan;
            this.addressingMode = addressingMode;
            this.needsScratch = needsScratch;
        }
    }

    /**
     * Generates an addressplan for an address of the form
     * {@code base + displacement [+ index [<< log2(transferSize)]]} with the index register and
     * scaling being optional.
     *
     * @param displacement an arbitrary displacement.
     * @param hasIndexRegister true if the address uses an index register, false otherwise. non null
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much
     *            the index register is scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @return AddressGenerationPlan that specifies the actions necessary to generate a valid
     *         AArch64Address for the given parameters.
     */
    public static AddressGenerationPlan generateAddressPlan(long displacement, boolean hasIndexRegister, int transferSize) {
        assert transferSize == 0 || transferSize == 1 || transferSize == 2 || transferSize == 4 || transferSize == 8;
        boolean indexScaled = transferSize != 0;
        int log2Scale = NumUtil.log2Ceil(transferSize);
        long scaledDisplacement = displacement >> log2Scale;
        boolean displacementScalable = indexScaled && (displacement & (transferSize - 1)) == 0;
        if (displacement == 0) {
            // register offset without any work beforehand.
            return new AddressGenerationPlan(NO_WORK, REGISTER_OFFSET, false);
        } else {
            if (hasIndexRegister) {
                if (displacementScalable) {
                    boolean needsScratch = !isArithmeticImmediate(scaledDisplacement);
                    return new AddressGenerationPlan(ADD_TO_INDEX, REGISTER_OFFSET, needsScratch);
                } else {
                    boolean needsScratch = !isArithmeticImmediate(displacement);
                    return new AddressGenerationPlan(ADD_TO_BASE, REGISTER_OFFSET, needsScratch);
                }
            } else {
                if (displacementScalable && NumUtil.isUnsignedNbit(12, scaledDisplacement)) {
                    return new AddressGenerationPlan(NO_WORK, IMMEDIATE_SCALED, false);
                } else if (NumUtil.isSignedNbit(9, displacement)) {
                    return new AddressGenerationPlan(NO_WORK, IMMEDIATE_UNSCALED, false);
                } else {
                    boolean needsScratch = !isArithmeticImmediate(displacement);
                    return new AddressGenerationPlan(ADD_TO_BASE, REGISTER_OFFSET, needsScratch);
                }
            }
        }
    }

    /**
     * Returns an AArch64Address pointing to
     * {@code base + displacement + index << log2(transferSize)}.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param index general purpose register. May not be null or the stack pointer.
     * @param signExtendIndex if true consider index register a word register that should be
     *            sign-extended before being added.
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much
     *            the index register is scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @param additionalReg additional register used either as a scratch register or as part of the
     *            final address, depending on whether allowOverwrite is true or not. May not be null
     *            or stackpointer.
     * @param allowOverwrite if true allows to change value of base or index register to generate
     *            address.
     * @return AArch64Address pointing to memory at
     *         {@code base + displacement + index << log2(transferSize)}.
     */
    public AArch64Address makeAddress(Register base, long displacement, Register index, boolean signExtendIndex, int transferSize, Register additionalReg, boolean allowOverwrite) {
        AddressGenerationPlan plan = generateAddressPlan(displacement, !index.equals(zr), transferSize);
        assert allowOverwrite || !zr.equals(additionalReg) || plan.workPlan == NO_WORK;
        assert !plan.needsScratch || !zr.equals(additionalReg);
        int log2Scale = NumUtil.log2Ceil(transferSize);
        long scaledDisplacement = displacement >> log2Scale;
        Register newIndex = index;
        Register newBase = base;
        int immediate;
        switch (plan.workPlan) {
            case NO_WORK:
                if (plan.addressingMode == IMMEDIATE_SCALED) {
                    immediate = (int) scaledDisplacement;
                } else {
                    immediate = (int) displacement;
                }
                break;
            case ADD_TO_INDEX:
                newIndex = allowOverwrite ? index : additionalReg;
                assert !newIndex.equals(sp) && !newIndex.equals(zr);
                if (plan.needsScratch) {
                    mov(additionalReg, scaledDisplacement);
                    add(signExtendIndex ? 32 : 64, newIndex, index, additionalReg);
                } else {
                    add(signExtendIndex ? 32 : 64, newIndex, index, (int) scaledDisplacement);
                }
                immediate = 0;
                break;
            case ADD_TO_BASE:
                newBase = allowOverwrite ? base : additionalReg;
                assert !newBase.equals(sp) && !newBase.equals(zr);
                if (plan.needsScratch) {
                    mov(additionalReg, displacement);
                    add(64, newBase, base, additionalReg);
                } else {
                    add(64, newBase, base, (int) displacement);
                }
                immediate = 0;
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        AArch64Address.AddressingMode addressingMode = plan.addressingMode;
        ExtendType extendType = null;
        if (addressingMode == REGISTER_OFFSET) {
            if (newIndex.equals(zr)) {
                addressingMode = BASE_REGISTER_ONLY;
            } else if (signExtendIndex) {
                addressingMode = EXTENDED_REGISTER_OFFSET;
                extendType = ExtendType.SXTW;
            }
        }
        return AArch64Address.createAddress(addressingMode, newBase, newIndex, immediate, transferSize != 0, extendType);
    }

    /**
     * Returns an AArch64Address pointing to {@code base + displacement}. Specifies the memory
     * transfer size to allow some optimizations when building the address.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param transferSize the memory transfer size in bytes.
     * @param additionalReg additional register used either as a scratch register or as part of the
     *            final address, depending on whether allowOverwrite is true or not. May not be
     *            null, zero register or stackpointer.
     * @param allowOverwrite if true allows to change value of base or index register to generate
     *            address.
     * @return AArch64Address pointing to memory at {@code base + displacement}.
     */
    public AArch64Address makeAddress(Register base, long displacement, Register additionalReg, int transferSize, boolean allowOverwrite) {
        assert additionalReg.getRegisterCategory().equals(CPU);
        return makeAddress(base, displacement, zr, /* sign-extend */false, transferSize, additionalReg, allowOverwrite);
    }

    /**
     * Returns an AArch64Address pointing to {@code base + displacement}. Fails if address cannot be
     * represented without overwriting base register or using a scratch register.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much
     *            the index register is scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @return AArch64Address pointing to memory at {@code base + displacement}.
     */
    public AArch64Address makeAddress(Register base, long displacement, int transferSize) {
        return makeAddress(base, displacement, zr, /* signExtend */false, //
                        transferSize, zr, /* allowOverwrite */false);
    }

    /**
     * Loads memory address into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param address address whose value is loaded into dst. May not be null,
     *            {@link org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode#IMMEDIATE_POST_INDEXED
     *            POST_INDEXED} or
     *            {@link org.graalvm.compiler.asm.aarch64.AArch64Address.AddressingMode#IMMEDIATE_PRE_INDEXED
     *            IMMEDIATE_PRE_INDEXED}
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much
     *            the index register is scaled. Can be 1, 2, 4 or 8.
     */
    public void loadAddress(Register dst, AArch64Address address, int transferSize) {
        assert transferSize == 1 || transferSize == 2 || transferSize == 4 || transferSize == 8;
        assert dst.getRegisterCategory().equals(CPU);
        int shiftAmt = NumUtil.log2Ceil(transferSize);
        switch (address.getAddressingMode()) {
            case IMMEDIATE_SCALED:
                int scaledImmediate = address.getImmediateRaw() << shiftAmt;
                int lowerBits = scaledImmediate & NumUtil.getNbitNumberInt(12);
                int higherBits = scaledImmediate & ~NumUtil.getNbitNumberInt(12);
                boolean firstAdd = true;
                if (lowerBits != 0) {
                    add(64, dst, address.getBase(), lowerBits);
                    firstAdd = false;
                }
                if (higherBits != 0) {
                    Register src = firstAdd ? address.getBase() : dst;
                    add(64, dst, src, higherBits);
                }
                break;
            case IMMEDIATE_UNSCALED:
                int immediate = address.getImmediateRaw();
                add(64, dst, address.getBase(), immediate);
                break;
            case REGISTER_OFFSET:
                add(64, dst, address.getBase(), address.getOffset(), ShiftType.LSL, address.isScaled() ? shiftAmt : 0);
                break;
            case EXTENDED_REGISTER_OFFSET:
                add(64, dst, address.getBase(), address.getOffset(), address.getExtendType(), address.isScaled() ? shiftAmt : 0);
                break;
            case PC_LITERAL: {
                addressOf(dst);
                break;
            }
            case BASE_REGISTER_ONLY:
                movx(dst, address.getBase());
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private boolean tryMerge(int sizeInBytes, Register rt, AArch64Address address, boolean isStore) {
        isImmLoadStoreMerged = false;
        if (lastImmLoadStoreEncoding == null) {
            return false;
        }

        // Only immediate scaled/unscaled address can be merged.
        // Pre-index and post-index mode can't be merged.
        AArch64Address.AddressingMode addressMode = address.getAddressingMode();
        if (addressMode != IMMEDIATE_SCALED && addressMode != IMMEDIATE_UNSCALED) {
            return false;
        }

        // Only the two adjacent ldrs/strs can be merged.
        int lastPosition = position() - 4;
        if (lastPosition < 0 || lastPosition != lastImmLoadStoreEncoding.position) {
            return false;
        }

        if (isStore != lastImmLoadStoreEncoding.isStore) {
            return false;
        }

        // Only merge ldr/str with the same size of 32bits or 64bits.
        if (sizeInBytes != lastImmLoadStoreEncoding.sizeInBytes || (sizeInBytes != 4 && sizeInBytes != 8)) {
            return false;
        }

        // Base register must be the same one.
        Register curBase = address.getBase();
        Register preBase = lastImmLoadStoreEncoding.getBase();
        if (!curBase.equals(preBase)) {
            return false;
        }

        // If the two ldrs have the same rt register, they can't be merged.
        // If the two ldrs have dependence, they can't be merged.
        Register curRt = rt;
        Register preRt = lastImmLoadStoreEncoding.result;
        if (!isStore && (curRt.equals(preRt) || preRt.equals(curBase))) {
            return false;
        }

        // Offset checking. Offsets of the two ldrs/strs must be continuous.
        int curOffset = address.getImmediateRaw();
        if (addressMode == IMMEDIATE_SCALED) {
            curOffset = curOffset * sizeInBytes;
        }
        int preOffset = lastImmLoadStoreEncoding.getOffset();
        if (Math.abs(curOffset - preOffset) != sizeInBytes) {
            return false;
        }

        // Offset must be in ldp/stp instruction's range.
        int offset = curOffset > preOffset ? preOffset : curOffset;
        int minOffset = -64 * sizeInBytes;
        int maxOffset = 63 * sizeInBytes;
        if (offset < minOffset || offset > maxOffset) {
            return false;
        }

        // Alignment checking.
        if (isFlagSet(AArch64.Flag.AvoidUnalignedAccesses)) {
            // AArch64 sp is 16-bytes aligned.
            if (curBase.equals(sp)) {
                long pairMask = sizeInBytes * 2 - 1;
                if ((offset & pairMask) != 0) {
                    return false;
                }
            } else {
                // If base is not sp, we can't guarantee the access is aligned.
                return false;
            }
        } else {
            // ldp/stp only supports sizeInBytes aligned offset.
            long mask = sizeInBytes - 1;
            if ((curOffset & mask) != 0 || (preOffset & mask) != 0) {
                return false;
            }
        }

        // Merge two ldrs/strs to ldp/stp.
        Register rt1 = preRt;
        Register rt2 = curRt;
        if (curOffset < preOffset) {
            rt1 = curRt;
            rt2 = preRt;
        }
        int immediate = offset / sizeInBytes;
        Instruction instruction = isStore ? STP : LDP;
        int size = sizeInBytes * Byte.SIZE;
        insertLdpStp(size, instruction, rt1, rt2, curBase, immediate, lastPosition);
        lastImmLoadStoreEncoding = null;
        isImmLoadStoreMerged = true;
        return true;
    }

    /**
     * Try to merge two continuous ldr/str to one ldp/stp. If this current ldr/str is not merged,
     * save it as the last ldr/str.
     */
    private boolean tryMergeLoadStore(int srcSize, Register rt, AArch64Address address, boolean isStore) {
        int sizeInBytes = srcSize / Byte.SIZE;
        if (tryMerge(sizeInBytes, rt, address, isStore)) {
            return true;
        }

        // Save last ldr/str if it is not merged.
        AArch64Address.AddressingMode addressMode = address.getAddressingMode();
        if (addressMode == IMMEDIATE_SCALED || addressMode == IMMEDIATE_UNSCALED) {
            if (addressMode == IMMEDIATE_UNSCALED) {
                long mask = sizeInBytes - 1;
                int offset = address.getImmediateRaw();
                if ((offset & mask) != 0) {
                    return false;
                }
            }
            lastImmLoadStoreEncoding = new AArch64MemoryEncoding(sizeInBytes, rt, address, isStore, position());
        }
        return false;
    }

    public boolean isImmLoadStoreMerged() {
        return isImmLoadStoreMerged;
    }

    public void movx(Register dst, Register src) {
        mov(64, dst, src);
    }

    public void mov(int size, Register dst, Register src) {
        if (dst.equals(sp) || src.equals(sp)) {
            add(size, dst, src, 0);
        } else {
            or(size, dst, zr, src);
        }
    }

    /**
     * Generates a 32-bit immediate move code sequence.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm the value to move into the register.
     * @param needsImmAnnotation Flag denoting if annotation should be added.
     */
    private void mov32(Register dst, int imm, boolean needsImmAnnotation) {
        MovAction[] includeSet = {MovAction.SKIPPED, MovAction.SKIPPED};
        int pos = position();

        // Split 32-bit imm into low16 and high16 parts.
        int low16 = imm & 0xFFFF;
        int high16 = (imm >>> 16) & 0xFFFF;

        // Generate code sequence with a combination of MOVZ or MOVN with MOVK.
        if (high16 == 0) {
            movz(32, dst, low16, 0);
            includeSet[0] = MovAction.USED;
        } else if (high16 == 0xFFFF) {
            movn(32, dst, low16 ^ 0xFFFF, 0);
            includeSet[0] = MovAction.NEGATED;
        } else if (low16 == 0) {
            movz(32, dst, high16, 16);
            includeSet[1] = MovAction.USED;
        } else if (low16 == 0xFFFF) {
            movn(32, dst, high16 ^ 0xFFFF, 16);
            includeSet[1] = MovAction.NEGATED;
        } else {
            // Neither of the 2 parts is all-0s or all-1s. Generate 2 instructions.
            movz(32, dst, low16, 0);
            movk(32, dst, high16, 16);
            includeSet[0] = MovAction.USED;
            includeSet[1] = MovAction.USED;
        }
        if (needsImmAnnotation) {
            annotateImmediateMovSequence(pos, includeSet);
        }
    }

    /**
     * Generates a 64-bit immediate move code sequence.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm the value to move into the register
     * @param needsImmAnnotation Flag denoting if annotation should be added.
     */
    private void mov64(Register dst, long imm, boolean needsImmAnnotation) {
        MovAction[] includeSet = {MovAction.SKIPPED, MovAction.SKIPPED, MovAction.SKIPPED, MovAction.SKIPPED};
        int pos = position();
        int[] chunks = new int[4];
        int zeroCount = 0;
        int negCount = 0;

        // Split 64-bit imm into 4 chunks and count the numbers of all-0 and all-1 chunks.
        for (int i = 0; i < 4; i++) {
            int chunk = (int) ((imm >>> (i * 16)) & 0xFFFFL);
            if (chunk == 0) {
                zeroCount++;
            } else if (chunk == 0xFFFF) {
                negCount++;
            }
            chunks[i] = chunk;
        }

        // Generate code sequence with a combination of MOVZ or MOVN with MOVK.
        if (zeroCount == 4) {
            // Generate only one MOVZ.
            movz(64, dst, 0, 0);
            includeSet[0] = MovAction.USED;
        } else if (negCount == 4) {
            // Generate only one MOVN.
            movn(64, dst, 0, 0);
            includeSet[0] = MovAction.NEGATED;
        } else if (zeroCount == 3) {
            // Generate only one MOVZ.
            for (int i = 0; i < 4; i++) {
                if (chunks[i] != 0) {
                    movz(64, dst, chunks[i], i * 16);
                    includeSet[i] = MovAction.USED;
                    break;
                }
            }
        } else if (negCount == 3) {
            // Generate only one MOVN.
            for (int i = 0; i < 4; i++) {
                if (chunks[i] != 0xFFFF) {
                    movn(64, dst, chunks[i] ^ 0xFFFF, i * 16);
                    includeSet[i] = MovAction.NEGATED;
                    break;
                }
            }
        } else if (zeroCount == 2) {
            // Generate one MOVZ and one MOVK.
            int i;
            for (i = 0; i < 4; i++) {
                if (chunks[i] != 0) {
                    movz(64, dst, chunks[i], i * 16);
                    includeSet[i] = MovAction.USED;
                    break;
                }
            }
            for (int k = i + 1; k < 4; k++) {
                if (chunks[k] != 0) {
                    movk(64, dst, chunks[k], k * 16);
                    includeSet[k] = MovAction.USED;
                    break;
                }
            }
        } else if (negCount == 2) {
            // Generate one MOVN and one MOVK.
            int i;
            for (i = 0; i < 4; i++) {
                if (chunks[i] != 0xFFFF) {
                    movn(64, dst, chunks[i] ^ 0xFFFF, i * 16);
                    includeSet[i] = MovAction.NEGATED;
                    break;
                }
            }
            for (int k = i + 1; k < 4; k++) {
                if (chunks[k] != 0xFFFF) {
                    movk(64, dst, chunks[k], k * 16);
                    includeSet[k] = MovAction.USED;
                    break;
                }
            }
        } else if (zeroCount == 1) {
            // Generate one MOVZ and two MOVKs.
            int i;
            for (i = 0; i < 4; i++) {
                if (chunks[i] != 0) {
                    movz(64, dst, chunks[i], i * 16);
                    includeSet[i] = MovAction.USED;
                    break;
                }
            }
            int numMovks = 0;
            for (int k = i + 1; k < 4; k++) {
                if (chunks[k] != 0) {
                    movk(64, dst, chunks[k], k * 16);
                    includeSet[k] = MovAction.USED;
                    numMovks++;
                }
            }
            assert numMovks == 2;
        } else if (negCount == 1) {
            // Generate one MOVN and two MOVKs.
            int i;
            for (i = 0; i < 4; i++) {
                if (chunks[i] != 0xFFFF) {
                    movn(64, dst, chunks[i] ^ 0xFFFF, i * 16);
                    includeSet[i] = MovAction.NEGATED;
                    break;
                }
            }
            int numMovks = 0;
            for (int k = i + 1; k < 4; k++) {
                if (chunks[k] != 0xFFFF) {
                    movk(64, dst, chunks[k], k * 16);
                    includeSet[k] = MovAction.USED;
                    numMovks++;
                }
            }
            assert numMovks == 2;
        } else {
            // Generate one MOVZ and three MOVKs
            movz(64, dst, chunks[0], 0);
            movk(64, dst, chunks[1], 16);
            movk(64, dst, chunks[2], 32);
            movk(64, dst, chunks[3], 48);
            includeSet[0] = MovAction.USED;
            includeSet[1] = MovAction.USED;
            includeSet[2] = MovAction.USED;
            includeSet[3] = MovAction.USED;
        }
        if (needsImmAnnotation) {
            annotateImmediateMovSequence(pos, includeSet);
        }
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     */
    public void mov(Register dst, int imm) {
        mov(dst, imm, false);
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     */
    public void mov(Register dst, long imm) {
        mov(dst, imm, false);
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     * @param needsImmAnnotation Flag to signal of the immediate value should be annotated.
     */
    public void mov(Register dst, int imm, boolean needsImmAnnotation) {
        if (imm == 0) {
            mov(32, dst, zr);
        } else if (isLogicalImmediate(imm)) {
            or(32, dst, zr, imm);
        } else {
            mov32(dst, imm, needsImmAnnotation);
        }
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     * @param needsImmAnnotation Flag to signal of the immediate value should be annotated.
     */
    public void mov(Register dst, long imm, boolean needsImmAnnotation) {
        assert dst.getRegisterCategory().equals(CPU);
        if (imm == 0L) {
            movx(dst, zr);
        } else if (isLogicalImmediate(imm)) {
            or(64, dst, zr, imm);
        } else if (imm >> 32 == -1L && (int) imm < 0 && LogicalImmediateTable.isRepresentable((int) imm) != LogicalImmediateTable.Representable.NO) {
            // If the higher 32-bit are 1s and the sign bit of the lower 32-bits is set *and* we can
            // represent the lower 32 bits as a logical immediate we can create the lower 32-bit and
            // then sign extend
            // them. This allows us to cover immediates like ~1L with 2 instructions.
            mov(dst, (int) imm);
            sxt(64, 32, dst, dst);
        } else {
            mov64(dst, imm, needsImmAnnotation);
        }
    }

    /**
     * Generates a 48-bit immediate move code sequence. The immediate may later be updated by
     * HotSpot.
     *
     * In AArch64 mode the virtual address space is 48-bits in size, so we only need three
     * instructions to create a patchable instruction sequence that can reach anywhere.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm
     */
    public void movNativeAddress(Register dst, long imm) {
        movNativeAddress(dst, imm, false);
    }

    /**
     * Generates a 48-bit immediate move code sequence. The immediate may later be updated by
     * HotSpot.
     *
     * In AArch64 mode the virtual address space is 48-bits in size, so we only need three
     * instructions to create a patchable instruction sequence that can reach anywhere.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm The immediate address
     * @param needsImmAnnotation Flag to signal of the immediate value should be annotated.
     */
    public void movNativeAddress(Register dst, long imm, boolean needsImmAnnotation) {
        assert (imm & 0xFFFF_0000_0000_0000L) == 0;
        // We have to move all non zero parts of the immediate in 16-bit chunks
        boolean firstMove = true;
        int pos = position();
        for (int offset = 0; offset < 48; offset += 16) {
            int chunk = (int) (imm >> offset) & NumUtil.getNbitNumberInt(16);
            if (firstMove) {
                movz(64, dst, chunk, offset);
                firstMove = false;
            } else {
                movk(64, dst, chunk, offset);
            }
        }
        if (needsImmAnnotation) {
            MovAction[] includeSet = {MovAction.USED, MovAction.USED, MovAction.USED};
            annotateImmediateMovSequence(pos, includeSet);
        }
        assert !firstMove;
    }

    /**
     * Generates a 32-bit immediate move code sequence. The immediate may later be updated by
     * HotSpot.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm
     */
    public void movNarrowAddress(Register dst, long imm) {
        assert (imm & 0xFFFF_FFFF_0000_0000L) == 0;
        movz(64, dst, (int) (imm >>> 16), 16);
        movk(64, dst, (int) (imm & 0xffff), 0);
    }

    /**
     * @return Number of instructions necessary to load immediate into register.
     */
    public static int nrInstructionsToMoveImmediate(long imm) {
        if (imm == 0L || LogicalImmediateTable.isRepresentable(true, imm) != LogicalImmediateTable.Representable.NO) {
            return 1;
        }
        if (imm >> 32 == -1L && (int) imm < 0 && LogicalImmediateTable.isRepresentable((int) imm) != LogicalImmediateTable.Representable.NO) {
            // If the higher 32-bit are 1s and the sign bit of the lower 32-bits is set *and* we can
            // represent the lower 32 bits as a logical immediate we can create the lower 32-bit and
            // then sign extend
            // them. This allows us to cover immediates like ~1L with 2 instructions.
            return 2;
        }
        int nrInstructions = 0;
        for (int offset = 0; offset < 64; offset += 16) {
            int part = (int) (imm >> offset) & NumUtil.getNbitNumberInt(16);
            if (part != 0) {
                nrInstructions++;
            }
        }
        return nrInstructions;
    }

    /**
     * Loads a srcSize value from address into rt sign-extending it if necessary.
     *
     * @param targetSize size of target register in bits. Must be 32 or 64.
     * @param srcSize size of memory read in bits. Must be 8, 16 or 32 and smaller or equal to
     *            targetSize.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    @Override
    public void ldrs(int targetSize, int srcSize, Register rt, AArch64Address address) {
        assert targetSize == 32 || targetSize == 64;
        assert srcSize <= targetSize;
        if (targetSize == srcSize) {
            ldr(srcSize, rt, address);
        } else {
            super.ldrs(targetSize, srcSize, rt, address);
        }
    }

    /**
     * Loads a srcSize value from address into rt zero-extending it if necessary.
     *
     * @param srcSize size of memory read in bits. Must be 8, 16 or 32 and smaller or equal to
     *            targetSize.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    @Override
    public void ldr(int srcSize, Register rt, AArch64Address address) {
        // Try to merge two adjacent loads into one ldp.
        if (!tryMergeLoadStore(srcSize, rt, address, false)) {
            super.ldr(srcSize, rt, address);
        }
    }

    /**
     * Stores register rt into memory pointed by address.
     *
     * @param destSize number of bits written to memory. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    @Override
    public void str(int destSize, Register rt, AArch64Address address) {
        // Try to merge two adjacent stores into one stp.
        if (!tryMergeLoadStore(destSize, rt, address, true)) {
            super.str(destSize, rt, address);
        }
    }

    /**
     * Conditional move. dst = src1 if condition else src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param result general purpose register. May not be null or the stackpointer.
     * @param trueValue general purpose register. May not be null or the stackpointer.
     * @param falseValue general purpose register. May not be null or the stackpointer.
     * @param cond any condition flag. May not be null.
     */
    public void cmov(int size, Register result, Register trueValue, Register falseValue, ConditionFlag cond) {
        super.csel(size, result, trueValue, falseValue, cond);
    }

    /**
     * Conditional set. dst = 1 if condition else 0.
     *
     * @param dst general purpose register. May not be null or stackpointer.
     * @param condition any condition. May not be null.
     */
    public void cset(int size, Register dst, ConditionFlag condition) {
        super.csinc(size, dst, zr, zr, condition.negate());
    }

    /**
     * dst = src1 + src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null.
     * @param src1 general purpose register. May not be null.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void add(int size, Register dst, Register src1, Register src2) {
        if (dst.equals(sp) || src1.equals(sp)) {
            super.add(size, dst, src1, src2, ExtendType.UXTX, 0);
        } else {
            super.add(size, dst, src1, src2, ShiftType.LSL, 0);
        }
    }

    /**
     * dst = src1 + src2 and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null.
     * @param src1 general purpose register. May not be null.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void adds(int size, Register dst, Register src1, Register src2) {
        if (dst.equals(sp) || src1.equals(sp)) {
            super.adds(size, dst, src1, src2, ExtendType.UXTX, 0);
        } else {
            super.adds(size, dst, src1, src2, ShiftType.LSL, 0);
        }
    }

    /**
     * dst = src1 - src2 and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null.
     * @param src1 general purpose register. May not be null.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void subs(int size, Register dst, Register src1, Register src2) {
        if (dst.equals(sp) || src1.equals(sp)) {
            super.subs(size, dst, src1, src2, ExtendType.UXTX, 0);
        } else {
            super.subs(size, dst, src1, src2, ShiftType.LSL, 0);
        }
    }

    /**
     * dst = src1 - src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null.
     * @param src1 general purpose register. May not be null.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void sub(int size, Register dst, Register src1, Register src2) {
        if (dst.equals(sp) || src1.equals(sp)) {
            super.sub(size, dst, src1, src2, ExtendType.UXTX, 0);
        } else {
            super.sub(size, dst, src1, src2, ShiftType.LSL, 0);
        }
    }

    /**
     * dst = src1 + shiftType(src2, shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param shiftAmt arbitrary shift amount.
     */
    @Override
    public void add(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.add(size, dst, src1, src2, shiftType, shift);
    }

    /**
     * dst = src1 + shiftType(src2, shiftAmt & (size-1)) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param shiftAmt arbitrary shift amount.
     */
    @Override
    public void sub(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.sub(size, dst, src1, src2, shiftType, shift);
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void neg(int size, Register dst, Register src) {
        sub(size, dst, zr, src);
    }

    /**
     * dst = src + immediate.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or zero-register.
     * @param immediate 32-bit signed int
     */
    @Override
    public void add(int size, Register dst, Register src, int immediate) {
        assert (!dst.equals(zr) && !src.equals(zr));
        if (immediate < 0) {
            sub(size, dst, src, -immediate);
        } else if (isAimm(immediate)) {
            if (!(dst.equals(src) && immediate == 0)) {
                super.add(size, dst, src, immediate);
            }
        } else if (immediate >= -(1 << 24) && immediate < (1 << 24)) {
            super.add(size, dst, src, immediate & -(1 << 12));
            super.add(size, dst, dst, immediate & ((1 << 12) - 1));
        } else {
            assert !dst.equals(src);
            mov(dst, immediate);
            add(size, src, dst, dst);
        }
    }

    /**
     * dst = src + immediate.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or zero-register.
     * @param immediate 64-bit signed int
     */
    public void add(int size, Register dst, Register src, long immediate) {
        if (NumUtil.isInt(immediate)) {
            add(size, dst, src, (int) immediate);
        } else {
            assert (!dst.equals(zr) && !src.equals(zr));
            assert !dst.equals(src);
            assert size == 64;
            mov(dst, immediate);
            add(size, src, dst, dst);
        }
    }

    /**
     * dst = src + aimm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or zero-register.
     * @param immediate arithmetic immediate.
     */
    @Override
    public void adds(int size, Register dst, Register src, int immediate) {
        assert (!dst.equals(sp) && !src.equals(zr));
        if (immediate < 0) {
            subs(size, dst, src, -immediate);
        } else if (!(dst.equals(src) && immediate == 0)) {
            super.adds(size, dst, src, immediate);
        }
    }

    /**
     * dst = src - immediate.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or zero-register.
     * @param immediate 32-bit signed int
     */
    @Override
    public void sub(int size, Register dst, Register src, int immediate) {
        assert (!dst.equals(zr) && !src.equals(zr));
        if (immediate < 0) {
            add(size, dst, src, -immediate);
        } else if (isAimm(immediate)) {
            if (!(dst.equals(src) && immediate == 0)) {
                super.sub(size, dst, src, immediate);
            }
        } else if (immediate >= -(1 << 24) && immediate < (1 << 24)) {
            super.sub(size, dst, src, immediate & -(1 << 12));
            super.sub(size, dst, dst, immediate & ((1 << 12) - 1));
        } else {
            assert !dst.equals(src);
            mov(dst, immediate);
            sub(size, src, dst, dst);
        }
    }

    /**
     * dst = src - aimm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or zero-register.
     * @param immediate arithmetic immediate.
     */
    @Override
    public void subs(int size, Register dst, Register src, int immediate) {
        assert (!dst.equals(sp) && !src.equals(zr));
        if (immediate < 0) {
            adds(size, dst, src, -immediate);
        } else if (!dst.equals(src) || immediate != 0) {
            super.subs(size, dst, src, immediate);
        }
    }

    /**
     * dst = src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void mul(int size, Register dst, Register src1, Register src2) {
        super.madd(size, dst, src1, src2, zr);
    }

    /**
     * dst = src3 + src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param src3 general purpose register. May not be null or the stackpointer.
     */
    @Override
    public void madd(int size, Register dst, Register src1, Register src2, Register src3) {
        super.madd(size, dst, src1, src2, src3);
    }

    /**
     * dst = src3 - src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param src3 general purpose register. May not be null or the stackpointer.
     */
    @Override
    public void msub(int size, Register dst, Register src1, Register src2, Register src3) {
        super.msub(size, dst, src1, src2, src3);
    }

    /**
     * dst = 0 - src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void mneg(int size, Register dst, Register src1, Register src2) {
        super.msub(size, dst, src1, src2, zr);
    }

    /**
     * Unsigned multiply high. dst = (src1 * src2) >> size
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void umulh(int size, Register dst, Register src1, Register src2) {
        assert (!dst.equals(sp) && !src1.equals(sp) && !src2.equals(sp));
        assert size == 32 || size == 64;
        if (size == 64) {
            super.umulh(dst, src1, src2);
        } else {
            // xDst = wSrc1 * wSrc2
            super.umaddl(dst, src1, src2, zr);
            // xDst = xDst >> 32
            lshr(64, dst, dst, 32);
        }
    }

    /**
     * Signed multiply high. dst = (src1 * src2) >> size
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void smulh(int size, Register dst, Register src1, Register src2) {
        assert (!dst.equals(sp) && !src1.equals(sp) && !src2.equals(sp));
        assert size == 32 || size == 64;
        if (size == 64) {
            super.smulh(dst, src1, src2);
        } else {
            // xDst = wSrc1 * wSrc2
            super.smaddl(dst, src1, src2, zr);
            // xDst = xDst >> 32
            lshr(64, dst, dst, 32);
        }
    }

    /**
     * Signed multiply long. xDst = wSrc1 * wSrc2
     *
     * @param size destination register size. Has to be 64.
     * @param dst 64-bit general purpose register. May not be null or the stackpointer.
     * @param src1 32-bit general purpose register. May not be null or the stackpointer.
     * @param src2 32-bit general purpose register. May not be null or the stackpointer.
     */
    public void smull(int size, Register dst, Register src1, Register src2) {
        this.smaddl(size, dst, src1, src2, zr);
    }

    /**
     * Signed multiply-negate long. xDst = -(wSrc1 * wSrc2)
     *
     * @param size destination register size. Has to be 64.
     * @param dst 64-bit general purpose register. May not be null or the stackpointer.
     * @param src1 32-bit general purpose register. May not be null or the stackpointer.
     * @param src2 32-bit general purpose register. May not be null or the stackpointer.
     */
    public void smnegl(int size, Register dst, Register src1, Register src2) {
        this.smsubl(size, dst, src1, src2, zr);
    }

    /**
     * Signed multiply-add long. xDst = xSrc3 + (wSrc1 * wSrc2)
     *
     * @param size destination register size. Has to be 64.
     * @param dst 64-bit general purpose register. May not be null or the stackpointer.
     * @param src1 32-bit general purpose register. May not be null or the stackpointer.
     * @param src2 32-bit general purpose register. May not be null or the stackpointer.
     * @param src3 64-bit general purpose register. May not be null or the stackpointer.
     */
    public void smaddl(int size, Register dst, Register src1, Register src2, Register src3) {
        assert (!dst.equals(sp) && !src1.equals(sp) && !src2.equals(sp) && !src3.equals(sp));
        assert size == 64;
        super.smaddl(dst, src1, src2, src3);
    }

    /**
     * Signed multiply-sub long. xDst = xSrc3 - (wSrc1 * wSrc2)
     *
     * @param size destination register size. Has to be 64.
     * @param dst 64-bit general purpose register. May not be null or the stackpointer.
     * @param src1 32-bit general purpose register. May not be null or the stackpointer.
     * @param src2 32-bit general purpose register. May not be null or the stackpointer.
     * @param src3 64-bit general purpose register. May not be null or the stackpointer.
     */
    public void smsubl(int size, Register dst, Register src1, Register src2, Register src3) {
        assert (!dst.equals(sp) && !src1.equals(sp) && !src2.equals(sp) && !src3.equals(sp));
        assert size == 64;
        super.smsubl(dst, src1, src2, src3);
    }

    /**
     * dst = src1 % src2. Signed.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param n numerator. General purpose register. May not be null or the stackpointer.
     * @param d denominator. General purpose register. Divisor May not be null or the stackpointer.
     */
    public void rem(int size, Register dst, Register n, Register d) {
        assert (!dst.equals(sp) && !n.equals(sp) && !d.equals(sp));
        // There is no irem or similar instruction. Instead we use the relation:
        // n % d = n - Floor(n / d) * d if nd >= 0
        // n % d = n - Ceil(n / d) * d else
        // Which is equivalent to n - TruncatingDivision(n, d) * d
        super.sdiv(size, dst, n, d);
        super.msub(size, dst, dst, d, n);
    }

    /**
     * dst = src1 % src2. Unsigned.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param n numerator. General purpose register. May not be null or the stackpointer.
     * @param d denominator. General purpose register. Divisor May not be null or the stackpointer.
     */
    public void urem(int size, Register dst, Register n, Register d) {
        // There is no irem or similar instruction. Instead we use the relation:
        // n % d = n - Floor(n / d) * d
        // Which is equivalent to n - TruncatingDivision(n, d) * d
        super.udiv(size, dst, n, d);
        super.msub(size, dst, dst, d, n);
    }

    /**
     * Add/subtract instruction encoding supports 12-bit immediate values.
     *
     * @param imm immediate value to be tested.
     * @return true if immediate can be used directly for arithmetic instructions (add/sub), false
     *         otherwise.
     */
    public static boolean isArithmeticImmediate(long imm) {
        // If we have a negative immediate we just use the opposite operator. I.e.: x - (-5) == x +
        // 5.
        return NumUtil.isInt(Math.abs(imm)) && isAimm((int) Math.abs(imm));
    }

    /**
     * Compare instructions are add/subtract instructions and so support 12-bit immediate values.
     *
     * @param imm immediate value to be tested.
     * @return true if immediate can be used directly with comparison instructions, false otherwise.
     */
    public static boolean isComparisonImmediate(long imm) {
        return isArithmeticImmediate(imm);
    }

    /**
     * Move wide immediate instruction encoding supports 16-bit immediate values which can be
     * optionally-shifted by multiples of 16 (i.e. 0, 16, 32, 48).
     *
     * @return true if immediate can be moved directly into a register, false otherwise.
     */
    public static boolean isMovableImmediate(long imm) {
        // // Positions of first, respectively last set bit.
        // int start = Long.numberOfTrailingZeros(imm);
        // int end = 64 - Long.numberOfLeadingZeros(imm);
        // int length = end - start;
        // if (length > 16) {
        // return false;
        // }
        // // We can shift the necessary part of the immediate (i.e. everything between the first
        // and
        // // last set bit) by as much as 16 - length around to arrive at a valid shift amount
        // int tolerance = 16 - length;
        // int prevMultiple = NumUtil.roundDown(start, 16);
        // int nextMultiple = NumUtil.roundUp(start, 16);
        // return start - prevMultiple <= tolerance || nextMultiple - start <= tolerance;
        /*
         * This is a bit optimistic because the constant could also be for an arithmetic instruction
         * which only supports 12-bits. That case needs to be handled in the backend.
         */
        return NumUtil.isInt(Math.abs(imm)) && NumUtil.isUnsignedNbit(16, (int) Math.abs(imm));
    }

    /**
     * dst = src << (shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void shl(int size, Register dst, Register src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.ubfm(size, dst, src, (size - shift) & (size - 1), size - 1 - shift);
    }

    /**
     * dst = src1 << (src2 & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void shl(int size, Register dst, Register src, Register shift) {
        super.lsl(size, dst, src, shift);
    }

    /**
     * dst = src >>> (shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void lshr(int size, Register dst, Register src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.ubfm(size, dst, src, shift, size - 1);
    }

    /**
     * dst = src1 >>> (src2 & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void lshr(int size, Register dst, Register src, Register shift) {
        super.lsr(size, dst, src, shift);
    }

    /**
     * dst = src >> (shiftAmt & log2(size)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void ashr(int size, Register dst, Register src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.sbfm(size, dst, src, shift, size - 1);
    }

    /**
     * dst = src1 >> (src2 & log2(size)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void ashr(int size, Register dst, Register src, Register shift) {
        super.asr(size, dst, src, shift);
    }

    /**
     * Rotate right (register). dst = rotateRight(src1, (src2 & (size - 1))).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. It holds a shift amount from 0 to (size - 1) in its
     *            bottom 5 bits. May not be null or stackpointer.
     */
    @Override
    public void rorv(int size, Register dst, Register src1, Register src2) {
        super.rorv(size, dst, src1, src2);
    }

    /**
     * Rotate right (immediate). dst = rotateRight(src1, shift).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift amount by which src is rotated. The value depends on the instruction variant, it
     *            can be 0 to (size - 1).
     */
    public void ror(int size, Register dst, Register src, int shift) {
        assert (0 <= shift && shift <= (size - 1));
        super.extr(size, dst, src, src, shift);
    }

    /**
     * Clamps shiftAmt into range 0 <= shiftamt < size according to JLS.
     *
     * @param size size of operation.
     * @param shiftAmt arbitrary shift amount.
     * @return value between 0 and size - 1 inclusive that is equivalent to shiftAmt according to
     *         JLS.
     */
    private static int clampShiftAmt(int size, long shiftAmt) {
        return (int) (shiftAmt & (size - 1));
    }

    /**
     * dst = src1 & src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void and(int size, Register dst, Register src1, Register src2) {
        super.and(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 ^ src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void eor(int size, Register dst, Register src1, Register src2) {
        super.eor(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 | src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void or(int size, Register dst, Register src1, Register src2) {
        super.orr(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src | bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate. See {@link AArch64Assembler.LogicalImmediateTable} for exact
     *            definition.
     */
    public void or(int size, Register dst, Register src, long bimm) {
        super.orr(size, dst, src, bimm);
    }

    /**
     * dst = src1 & (~src2).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void bic(int size, Register dst, Register src1, Register src2) {
        super.bic(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 ^ (~src2).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void eon(int size, Register dst, Register src1, Register src2) {
        super.eon(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 | (~src2).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void orn(int size, Register dst, Register src1, Register src2) {
        super.orn(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = ~src.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void not(int size, Register dst, Register src) {
        super.orn(size, dst, zr, src, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 & shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    @Override
    public void and(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.and(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 ^ shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    @Override
    public void eor(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.eor(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 | shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void or(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.orr(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 & ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    @Override
    public void bic(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.bic(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 ^ ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    @Override
    public void eon(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.eon(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 | ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    @Override
    public void orn(int size, Register dst, Register src1, Register src2, ShiftType shiftType, int shiftAmt) {
        super.orn(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * Sign-extend value from src into dst.
     *
     * @param destSize destination register size. Must be 32 or 64.
     * @param srcSize source register size. Must be smaller than destSize.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     */
    public void sxt(int destSize, int srcSize, Register dst, Register src) {
        assert (srcSize < destSize && srcSize > 0);
        super.sbfm(destSize, dst, src, 0, srcSize - 1);
    }

    /**
     * dst = src if condition else -src.
     *
     * @param size register size. Must be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src general purpose register. May not be null or the stackpointer.
     * @param condition any condition except AV or NV. May not be null.
     */
    public void csneg(int size, Register dst, Register src, ConditionFlag condition) {
        super.csneg(size, dst, src, src, condition.negate());
    }

    /**
     * @return True if the immediate can be used directly for logical 64-bit instructions.
     */
    public static boolean isLogicalImmediate(long imm) {
        return LogicalImmediateTable.isRepresentable(true, imm) != LogicalImmediateTable.Representable.NO;
    }

    /**
     * @return True if the immediate can be used directly for logical 32-bit instructions.
     */
    public static boolean isLogicalImmediate(int imm) {
        return LogicalImmediateTable.isRepresentable(imm) == LogicalImmediateTable.Representable.YES;
    }

    /* Float instructions */

    /**
     * Moves integer to float, float to integer, or float to float. Does not support integer to
     * integer moves.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst Either floating-point or general-purpose register. If general-purpose register may
     *            not be stackpointer or zero register. Cannot be null in any case.
     * @param src Either floating-point or general-purpose register. If general-purpose register may
     *            not be stackpointer. Cannot be null in any case.
     */
    @Override
    public void fmov(int size, Register dst, Register src) {
        assert !(dst.getRegisterCategory().equals(CPU) && src.getRegisterCategory().equals(CPU)) : "src and dst cannot both be integer registers.";
        if (dst.getRegisterCategory().equals(CPU)) {
            super.fmovFpu2Cpu(size, dst, src);
        } else if (src.getRegisterCategory().equals(CPU)) {
            super.fmovCpu2Fpu(size, dst, src);
        } else {
            super.fmov(size, dst, src);
        }
    }

    /**
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating point register. May not be null.
     * @param imm immediate that is loaded into dst. If size is 32 only float immediates can be
     *            loaded, i.e. (float) imm == imm must be true. In all cases
     *            {@code isFloatImmediate}, respectively {@code #isDoubleImmediate} must be true
     *            depending on size.
     */
    @Override
    public void fmov(int size, Register dst, double imm) {
        if (imm == 0.0) {
            assert Double.doubleToRawLongBits(imm) == 0L : "-0.0 is no valid immediate.";
            super.fmovCpu2Fpu(size, dst, zr);
        } else {
            super.fmov(size, dst, imm);
        }
    }

    /**
     *
     * @return true if immediate can be loaded directly into floating-point register, false
     *         otherwise.
     */
    public static boolean isDoubleImmediate(double imm) {
        return Double.doubleToRawLongBits(imm) == 0L || AArch64Assembler.isDoubleImmediate(imm);
    }

    /**
     *
     * @return true if immediate can be loaded directly into floating-point register, false
     *         otherwise.
     */
    public static boolean isFloatImmediate(float imm) {
        return Float.floatToRawIntBits(imm) == 0 || AArch64Assembler.isFloatImmediate(imm);
    }

    /**
     * Conditional move. dst = src1 if condition else src2.
     *
     * @param size register size.
     * @param result floating point register. May not be null.
     * @param trueValue floating point register. May not be null.
     * @param falseValue floating point register. May not be null.
     * @param condition every condition allowed. May not be null.
     */
    public void fcmov(int size, Register result, Register trueValue, Register falseValue, ConditionFlag condition) {
        super.fcsel(size, result, trueValue, falseValue, condition);
    }

    /**
     * dst = src1 % src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating-point register. May not be null.
     * @param n numerator. Floating-point register. May not be null.
     * @param d denominator. Floating-point register. May not be null.
     */
    public void frem(int size, Register dst, Register n, Register d) {
        // There is no frem instruction, instead we compute the remainder using the relation:
        // rem = n - Truncating(n / d) * d
        super.fdiv(size, dst, n, d);
        super.frintz(size, dst, dst);
        super.fmsub(size, dst, dst, d, n);
    }

    /**
     * dst = src1 * src2 + src3.
     *
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     * @param src3 floating point register. May not be null.
     */
    @Override
    public void fmadd(int size, Register dst, Register src1, Register src2, Register src3) {
        super.fmadd(size, dst, src1, src2, src3);
    }

    /* Branches */

    /**
     * Compares x and y and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y general purpose register. May not be null or stackpointer.
     */
    public void cmp(int size, Register x, Register y) {
        assert size == 32 || size == 64;
        super.subs(size, zr, x, y, ShiftType.LSL, 0);
    }

    /**
     * Compares x to y and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y comparison immediate, {@link #isComparisonImmediate(long)} has to be true for it.
     */
    public void cmp(int size, Register x, int y) {
        assert size == 32 || size == 64;
        if (y < 0) {
            super.adds(size, zr, x, -y);
        } else {
            super.subs(size, zr, x, y);
        }
    }

    /**
     * Sets condition flags according to result of x & y.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stack-pointer.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y general purpose register. May not be null or stackpointer.
     */
    public void ands(int size, Register dst, Register x, Register y) {
        super.ands(size, dst, x, y, ShiftType.LSL, 0);
    }

    /**
     * Sets overflow flag according to result of x * y.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stack-pointer.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y general purpose register. May not be null or stackpointer.
     */
    public void mulvs(int size, Register dst, Register x, Register y) {
        try (ScratchRegister sc1 = getScratchRegister();
                        ScratchRegister sc2 = getScratchRegister()) {
            switch (size) {
                case 64: {
                    // Be careful with registers: it's possible that x, y, and dst are the same
                    // register.
                    Register temp1 = sc1.getRegister();
                    Register temp2 = sc2.getRegister();
                    mul(64, temp1, x, y);     // Result bits 0..63
                    smulh(64, temp2, x, y);  // Result bits 64..127
                    // Top is pure sign ext
                    subs(64, zr, temp2, temp1, ShiftType.ASR, 63);
                    // Copy all 64 bits of the result into dst
                    mov(64, dst, temp1);
                    mov(temp1, 0x80000000);
                    // Develop 0 (EQ), or 0x80000000 (NE)
                    cmov(32, temp1, temp1, zr, ConditionFlag.NE);
                    cmp(32, temp1, 1);
                    // 0x80000000 - 1 => VS
                    break;
                }
                case 32: {
                    Register temp1 = sc1.getRegister();
                    smaddl(temp1, x, y, zr);
                    // Copy the low 32 bits of the result into dst
                    mov(32, dst, temp1);
                    subs(64, zr, temp1, temp1, ExtendType.SXTW, 0);
                    // NE => overflow
                    mov(temp1, 0x80000000);
                    // Develop 0 (EQ), or 0x80000000 (NE)
                    cmov(32, temp1, temp1, zr, ConditionFlag.NE);
                    cmp(32, temp1, 1);
                    // 0x80000000 - 1 => VS
                    break;
                }
            }
        }
    }

    /**
     * When patching up Labels we have to know what kind of code to generate.
     */
    public enum PatchLabelKind {
        BRANCH_CONDITIONALLY(0x0),
        BRANCH_UNCONDITIONALLY(0x1),
        BRANCH_NONZERO(0x2),
        BRANCH_ZERO(0x3),
        BRANCH_BIT_NONZERO(0x4),
        BRANCH_BIT_ZERO(0x5),
        JUMP_ADDRESS(0x6),
        ADR(0x7);

        /**
         * Offset by which additional information for branch conditionally, branch zero and branch
         * non zero has to be shifted.
         */
        public static final int INFORMATION_OFFSET = 5;

        public final int encoding;

        PatchLabelKind(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return PatchLabelKind with given encoding.
         */
        private static PatchLabelKind fromEncoding(int encoding) {
            return values()[encoding & NumUtil.getNbitNumberInt(INFORMATION_OFFSET)];
        }

    }

    public void adr(Register dst, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - position();
            super.adr(dst, offset);
        } else {
            label.addPatchAt(position(), this);
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.ADR.encoding | dst.encoding << PatchLabelKind.INFORMATION_OFFSET);
        }
    }

    /**
     * Compare register and branch if non-zero.
     *
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbnz(int size, Register cmp, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - position();
            super.cbnz(size, cmp, offset);
        } else {
            label.addPatchAt(position(), this);
            int regEncoding = cmp.encoding << (PatchLabelKind.INFORMATION_OFFSET + 1);
            int sizeEncoding = (size == 64 ? 1 : 0) << PatchLabelKind.INFORMATION_OFFSET;
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_NONZERO.encoding | regEncoding | sizeEncoding);
        }
    }

    /**
     * Compare register and branch if zero.
     *
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbz(int size, Register cmp, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - position();
            super.cbz(size, cmp, offset);
        } else {
            label.addPatchAt(position(), this);
            int regEncoding = cmp.encoding << (PatchLabelKind.INFORMATION_OFFSET + 1);
            int sizeEncoding = (size == 64 ? 1 : 0) << PatchLabelKind.INFORMATION_OFFSET;
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_ZERO.encoding | regEncoding | sizeEncoding);
        }
    }

    /**
     * Test a single bit and branch if the bit is nonzero.
     *
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param uimm6 Unsigned 6-bit bit index.
     * @param label Can only handle 16-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void tbnz(Register cmp, int uimm6, Label label) {
        assert NumUtil.isUnsignedNbit(6, uimm6);
        if (label.isBound()) {
            int offset = label.position() - position();
            super.tbnz(cmp, uimm6, offset);
        } else {
            label.addPatchAt(position(), this);
            int indexEncoding = uimm6 << PatchLabelKind.INFORMATION_OFFSET;
            int regEncoding = cmp.encoding << (PatchLabelKind.INFORMATION_OFFSET + 6);
            emitInt(PatchLabelKind.BRANCH_BIT_NONZERO.encoding | indexEncoding | regEncoding);
        }
    }

    /**
     * Test a single bit and branch if the bit is zero.
     *
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param uimm6 Unsigned 6-bit bit index.
     * @param label Can only handle 16-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void tbz(Register cmp, int uimm6, Label label) {
        assert NumUtil.isUnsignedNbit(6, uimm6);
        if (label.isBound()) {
            int offset = label.position() - position();
            super.tbz(cmp, uimm6, offset);
        } else {
            label.addPatchAt(position(), this);
            int indexEncoding = uimm6 << PatchLabelKind.INFORMATION_OFFSET;
            int regEncoding = cmp.encoding << (PatchLabelKind.INFORMATION_OFFSET + 6);
            emitInt(PatchLabelKind.BRANCH_BIT_ZERO.encoding | indexEncoding | regEncoding);
        }
    }

    /**
     * Branches to label if condition is true.
     *
     * @param condition any condition value allowed. Non null.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void branchConditionally(ConditionFlag condition, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - position();
            super.b(condition, offset);
        } else {
            label.addPatchAt(position(), this);
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_CONDITIONALLY.encoding | condition.encoding << PatchLabelKind.INFORMATION_OFFSET);
        }
    }

    /**
     * Branches if condition is true. Address of jump is patched up by HotSpot c++ code.
     *
     * @param condition any condition value allowed. Non null.
     */
    public void branchConditionally(ConditionFlag condition) {
        // Correct offset is fixed up by HotSpot later.
        super.b(condition, 0);
    }

    /**
     * Jumps to label.
     *
     * param label Can only handle signed 28-bit offsets. May be unbound. Non null.
     */
    @Override
    public void jmp(Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - position();
            super.b(offset);
        } else {
            label.addPatchAt(position(), this);
            emitInt(PatchLabelKind.BRANCH_UNCONDITIONALLY.encoding);
        }
    }

    /**
     * Jump to address in dest.
     *
     * @param dest General purpose register. May not be null, zero-register or stackpointer.
     */
    public void jmp(Register dest) {
        super.br(dest);
    }

    /**
     * Immediate jump instruction fixed up by HotSpot c++ code.
     */
    public void jmp() {
        // Offset has to be fixed up by c++ code.
        super.b(0);
    }

    /**
     *
     * @return true if immediate offset can be used in a single branch instruction.
     */
    public static boolean isBranchImmediateOffset(long imm) {
        return NumUtil.isSignedNbit(28, imm);
    }

    /* system instructions */

    /**
     * Exception codes used when calling hlt instruction.
     */
    public enum AArch64ExceptionCode {
        NO_SWITCH_TARGET(0x0),
        BREAKPOINT(0x1);

        public final int encoding;

        AArch64ExceptionCode(int encoding) {
            this.encoding = encoding;
        }
    }

    /**
     * Halting mode software breakpoint: Enters halting mode debug state if enabled, else treated as
     * UNALLOCATED instruction.
     *
     * @param exceptionCode exception code specifying why halt was called. Non null.
     */
    public void hlt(AArch64ExceptionCode exceptionCode) {
        super.hlt(exceptionCode.encoding);
    }

    /**
     * Monitor mode software breakpoint: exception routed to a debug monitor executing in a higher
     * exception level.
     *
     * @param exceptionCode exception code specifying why break was called. Non null.
     */
    public void brk(AArch64ExceptionCode exceptionCode) {
        super.brk(exceptionCode.encoding);
    }

    public void pause() {
        super.hint(SystemHint.YIELD);
    }

    /**
     * Executes no-op instruction. No registers or flags are updated, except for PC.
     */
    public void nop() {
        super.hint(SystemHint.NOP);
    }

    /**
     * Consumption of Speculative Data Barrier. This is a memory barrier that controls speculative
     * execution and data value prediction.
     */
    public void csdb() {
        super.hint(SystemHint.CSDB);
    }

    /**
     * Ensures current execution state is committed before continuing.
     */
    public void fullSystemBarrier() {
        super.dsb(BarrierKind.SYSTEM);
        super.isb();
    }

    /**
     * Same as {@link #nop()}.
     */
    @Override
    public void ensureUniquePC() {
        nop();
    }

    /**
     * Aligns PC.
     *
     * @param modulus Has to be positive multiple of 4.
     */
    @Override
    public void align(int modulus) {
        assert modulus > 0 && (modulus & 0x3) == 0 : "Modulus has to be a positive multiple of 4.";
        if (position() % modulus == 0) {
            return;
        }
        int offset = modulus - position() % modulus;
        for (int i = 0; i < offset; i += 4) {
            nop();
        }
    }

    /**
     * Patches jump targets when label gets bound.
     */
    @Override
    protected void patchJumpTarget(int branch, int jumpTarget) {
        int instruction = getInt(branch);
        int branchOffset = jumpTarget - branch;
        PatchLabelKind type = PatchLabelKind.fromEncoding(instruction);
        switch (type) {
            case BRANCH_CONDITIONALLY:
                ConditionFlag cf = ConditionFlag.fromEncoding(instruction >>> PatchLabelKind.INFORMATION_OFFSET);
                super.b(cf, branchOffset, branch);
                break;
            case BRANCH_UNCONDITIONALLY:
                super.b(branchOffset, branch);
                break;
            case JUMP_ADDRESS:
                int offset = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
                emitInt(jumpTarget - offset, branch);
                break;
            case BRANCH_NONZERO:
            case BRANCH_ZERO: {
                int information = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
                int sizeEncoding = information & 1;
                int regEncoding = information >>> 1;
                Register reg = AArch64.cpuRegisters.get(regEncoding);
                // 1 => 64; 0 => 32
                int size = sizeEncoding * 32 + 32;
                if (!NumUtil.isSignedNbit(21, branchOffset)) {
                    throw new BranchTargetOutOfBoundsException(true, "Branch target %d out of bounds", branchOffset);
                }
                switch (type) {
                    case BRANCH_NONZERO:
                        super.cbnz(size, reg, branchOffset, branch);
                        break;
                    case BRANCH_ZERO:
                        super.cbz(size, reg, branchOffset, branch);
                        break;
                }
                break;
            }
            case BRANCH_BIT_NONZERO:
            case BRANCH_BIT_ZERO: {
                int information = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
                int sizeEncoding = information & NumUtil.getNbitNumberInt(6);
                int regEncoding = information >>> 6;
                Register reg = AArch64.cpuRegisters.get(regEncoding);
                if (!NumUtil.isSignedNbit(16, branchOffset)) {
                    throw new BranchTargetOutOfBoundsException(true, "Branch target %d out of bounds", branchOffset);
                }
                switch (type) {
                    case BRANCH_BIT_NONZERO:
                        super.tbnz(reg, sizeEncoding, branchOffset, branch);
                        break;
                    case BRANCH_BIT_ZERO:
                        super.tbz(reg, sizeEncoding, branchOffset, branch);
                        break;
                }
                break;
            }
            case ADR: {
                int information = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
                int regEncoding = information;
                Register reg = AArch64.cpuRegisters.get(regEncoding);
                super.adr(reg, branchOffset, branch);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    /**
     * Generates an address of the form {@code base + displacement}.
     *
     * Does not change base register to fulfill this requirement. Will fail if displacement cannot
     * be represented directly as address.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @return AArch64Address referencing memory at {@code base + displacement}.
     */
    @Override
    public AArch64Address makeAddress(Register base, int displacement) {
        return makeAddress(base, displacement, zr, /* signExtend */false, /* transferSize */0, //
                        zr, /* allowOverwrite */false);
    }

    @Override
    public AArch64Address getPlaceholder(int instructionStartPosition) {
        return AArch64Address.PLACEHOLDER;
    }

    public void addressOf(Register dst) {
        if (codePatchingAnnotationConsumer != null) {
            codePatchingAnnotationConsumer.accept(new AdrpAddMacroInstruction(position()));
        }
        super.adrp(dst);
        super.add(64, dst, dst, 0);
    }

    /**
     * Loads an address into Register d.
     *
     * @param d general purpose register. May not be null.
     * @param a AArch64Address the address of an operand.
     */
    public void lea(Register d, AArch64Address a) {
        a.lea(this, d);
    }

    /**
     * Count the set bits of src register.
     *
     * @param size src register size. Has to be 32 or 64.
     * @param dst general purpose register. Should not be null or zero-register.
     * @param src general purpose register. Should not be null.
     * @param vreg SIMD register. Should not be null.
     */
    public void popcnt(int size, Register dst, Register src, Register vreg) {
        assert 32 == size || 64 == size : "Invalid data size";
        fmov(size, vreg, src);
        final int fixedSize = 64;
        cnt(fixedSize, vreg, vreg);
        addv(fixedSize, SIMDElementSize.Byte, vreg, vreg);
        umov(fixedSize, dst, 0, vreg);
    }

    /**
     * Emits elf patchable adrp ldr sequence.
     */
    public void adrpLdr(int srcSize, Register result, AArch64Address a) {
        if (codePatchingAnnotationConsumer != null) {
            codePatchingAnnotationConsumer.accept(new AdrpLdrMacroInstruction(position(), srcSize));
        }
        super.adrp(a.getBase());
        this.ldr(srcSize, result, a);
    }

    public static class AdrpLdrMacroInstruction extends AArch64Assembler.PatchableCodeAnnotation {
        public final int srcSize;

        public AdrpLdrMacroInstruction(int position, int srcSize) {
            super(position);
            this.srcSize = srcSize;
        }

        @Override
        public String toString() {
            return "ADRP_LDR";
        }

        @Override
        public void patch(int codePos, int relative, byte[] code) {
            int shiftSize = 0;
            switch (srcSize) {
                case 64:
                    shiftSize = 3;
                    break;
                case 32:
                    shiftSize = 2;
                    break;
                case 16:
                    shiftSize = 1;
                    break;
                case 8:
                    shiftSize = 0;
                    break;
                default:
                    assert false : "srcSize must be either 8, 16, 32, or 64";
            }

            int pos = instructionPosition;

            int targetAddress = pos + relative;
            assert shiftSize == 0 || (targetAddress & ((1 << shiftSize) - 1)) == 0 : "shift bits must be zero";

            int relativePageDifference = PatcherUtil.computeRelativePageDifference(targetAddress, pos, 1 << 12);

            // adrp imm_hi bits
            int curValue = (relativePageDifference >> 2) & 0x7FFFF;
            int[] adrHiBits = {3, 8, 8};
            int[] adrHiOffsets = {5, 0, 0};
            PatcherUtil.writeBitSequence(code, pos, curValue, adrHiBits, adrHiOffsets);
            // adrp imm_lo bits
            curValue = relativePageDifference & 0x3;
            int[] adrLoBits = {2};
            int[] adrLoOffsets = {5};
            PatcherUtil.writeBitSequence(code, pos + 3, curValue, adrLoBits, adrLoOffsets);
            // ldr bits
            curValue = (targetAddress >> shiftSize) & 0x1FF;
            int[] ldrBits = {6, 6};
            int[] ldrOffsets = {2, 0};
            PatcherUtil.writeBitSequence(code, pos + 5, curValue, ldrBits, ldrOffsets);
        }
    }

    public static class AdrpAddMacroInstruction extends AArch64Assembler.PatchableCodeAnnotation {
        public AdrpAddMacroInstruction(int position) {
            super(position);
        }

        @Override
        public String toString() {
            return "ADRP_ADD";
        }

        @Override
        public void patch(int codePos, int relative, byte[] code) {
            int pos = instructionPosition;
            int targetAddress = pos + relative;
            int relativePageDifference = PatcherUtil.computeRelativePageDifference(targetAddress, pos, 1 << 12);
            // adrp imm_hi bits
            int curValue = (relativePageDifference >> 2) & 0x7FFFF;
            int[] adrHiBits = {3, 8, 8};
            int[] adrHiOffsets = {5, 0, 0};
            PatcherUtil.writeBitSequence(code, pos, curValue, adrHiBits, adrHiOffsets);
            // adrp imm_lo bits
            curValue = relativePageDifference & 0x3;
            int[] adrLoBits = {2};
            int[] adrLoOffsets = {5};
            PatcherUtil.writeBitSequence(code, pos + 3, curValue, adrLoBits, adrLoOffsets);
            // add bits
            curValue = targetAddress & 0xFFF;
            int[] addBits = {6, 6};
            int[] addOffsets = {2, 0};
            PatcherUtil.writeBitSequence(code, pos + 5, curValue, addBits, addOffsets);
        }
    }

    private void annotateImmediateMovSequence(int pos, MovSequenceAnnotation.MovAction[] includeSet) {
        if (codePatchingAnnotationConsumer != null) {
            codePatchingAnnotationConsumer.accept(new MovSequenceAnnotation(pos, includeSet));
        }
    }

    public static class MovSequenceAnnotation extends AArch64Assembler.PatchableCodeAnnotation {

        /**
         * An enum to indicate how each 16-bit immediate chunk is represented within a sequence of
         * mov instructions.
         */
        public enum MovAction {
            USED, // mov instruction is in place for this chunk.
            SKIPPED, // no mov instruction is in place for this chunk.
            NEGATED; // movn instruction is in place for this chunk.
        }

        /**
         * The size of the operand, in bytes.
         */
        public final MovAction[] includeSet;

        MovSequenceAnnotation(int instructionPosition, MovAction[] includeSet) {
            super(instructionPosition);
            this.includeSet = includeSet;
        }

        @Override
        public String toString() {
            return "MOV_SEQ";
        }

        @Override
        public void patch(int codePos, int relative, byte[] code) {
            /*
             * Each move has a 16 bit immediate operand. We use a series of shifted moves to
             * represent immediate values larger than 16 bits.
             */
            int curValue = relative;
            int[] bitsUsed = {3, 8, 5};
            int[] offsets = {5, 0, 0};
            int siteOffset = 0;
            boolean containsNegatedMov = false;
            for (MovAction include : includeSet) {
                if (include == MovAction.NEGATED) {
                    containsNegatedMov = true;
                    break;
                }
            }
            for (int i = 0; i < includeSet.length; i++) {
                int value = curValue & 0xFFFF;
                curValue = curValue >> 16;
                switch (includeSet[i]) {
                    case USED:
                        break;
                    case SKIPPED:
                        assert value == (containsNegatedMov ? 0xFFFF : 0) : "Unable to patch this value.";
                        continue;
                    case NEGATED:
                        value = value ^ 0xFFFF;
                        break;
                }
                int bytePosition = instructionPosition + siteOffset;
                PatcherUtil.writeBitSequence(code, bytePosition, value, bitsUsed, offsets);
                siteOffset += 4;
            }
        }
    }
}
