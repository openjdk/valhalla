/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023 SAP SE. All rights reserved.
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
package jdk.internal.foreign.abi.ppc64;

import jdk.internal.foreign.Utils;
import jdk.internal.foreign.abi.ABIDescriptor;
import jdk.internal.foreign.abi.AbstractLinker.UpcallStubFactory;
import jdk.internal.foreign.abi.Binding;
import jdk.internal.foreign.abi.CallingSequence;
import jdk.internal.foreign.abi.CallingSequenceBuilder;
import jdk.internal.foreign.abi.DowncallLinker;
import jdk.internal.foreign.abi.LinkerOptions;
import jdk.internal.foreign.abi.SharedUtils;
import jdk.internal.foreign.abi.VMStorage;
import jdk.internal.foreign.abi.ppc64.ABIv2CallArranger;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

import static jdk.internal.foreign.abi.ppc64.PPC64Architecture.*;
import static jdk.internal.foreign.abi.ppc64.PPC64Architecture.Regs.*;

/**
 * For the PPC64 C ABI specifically, this class uses CallingSequenceBuilder
 * to translate a C FunctionDescriptor into a CallingSequence, which can then be turned into a MethodHandle.
 *
 * This includes taking care of synthetic arguments like pointers to return buffers for 'in-memory' returns.
 *
 * There are minor differences between the ABIs implemented on Linux and AIX
 * which are handled in sub-classes. Clients should access these through the provided
 * public constants CallArranger.ABIv1/2.
 */
public abstract class CallArranger {
    final boolean useABIv2 = (this instanceof ABIv2CallArranger);

    private static final int STACK_SLOT_SIZE = 8;
    private static final int MAX_COPY_SIZE = 8;
    public static final int MAX_REGISTER_ARGUMENTS = 8;
    public static final int MAX_FLOAT_REGISTER_ARGUMENTS = 13;

    // This is derived from the 64-Bit ELF v2 ABI spec, restricted to what's
    // possible when calling to/from C code. (v1 is compatible, but uses fewer output registers.)
    private final ABIDescriptor C = abiFor(
        new VMStorage[] { r3, r4, r5, r6, r7, r8, r9, r10 }, // GP input
        new VMStorage[] { f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13 }, // FP intput
        new VMStorage[] { r3, r4 }, // GP output
        new VMStorage[] { f1, f2, f3, f4, f5, f6, f7, f8 }, // FP output
        new VMStorage[] { r0, r2, r11, r12 }, // volatile GP (excluding argument registers)
        new VMStorage[] { f0 }, // volatile FP (excluding argument registers)
        16, // Stack is always 16 byte aligned on PPC64
        useABIv2 ? 32 : 48, // ABI header (excluding argument register spill slots)
        r11, // scratch reg
        r12  // target addr reg, otherwise used as scratch reg
    );

    public record Bindings(CallingSequence callingSequence, boolean isInMemoryReturn) {}

    private record HfaRegs(VMStorage[] first, VMStorage[] second) {}

    protected CallArranger() {}

    public static final CallArranger ABIv2 = new ABIv2CallArranger();

    public Bindings getBindings(MethodType mt, FunctionDescriptor cDesc, boolean forUpcall) {
        return getBindings(mt, cDesc, forUpcall, LinkerOptions.empty());
    }

    public Bindings getBindings(MethodType mt, FunctionDescriptor cDesc, boolean forUpcall, LinkerOptions options) {
        CallingSequenceBuilder csb = new CallingSequenceBuilder(C, forUpcall, options);

        BindingCalculator argCalc = forUpcall ? new BoxBindingCalculator(true) : new UnboxBindingCalculator(true);
        BindingCalculator retCalc = forUpcall ? new UnboxBindingCalculator(false) : new BoxBindingCalculator(false);

        boolean returnInMemory = isInMemoryReturn(cDesc.returnLayout());
        if (returnInMemory) {
            Class<?> carrier = MemorySegment.class;
            MemoryLayout layout = SharedUtils.C_POINTER;
            csb.addArgumentBindings(carrier, layout, argCalc.getBindings(carrier, layout));
        } else if (cDesc.returnLayout().isPresent()) {
            Class<?> carrier = mt.returnType();
            MemoryLayout layout = cDesc.returnLayout().get();
            csb.setReturnBindings(carrier, layout, retCalc.getBindings(carrier, layout));
        }

        for (int i = 0; i < mt.parameterCount(); i++) {
            Class<?> carrier = mt.parameterType(i);
            MemoryLayout layout = cDesc.argumentLayouts().get(i);
            if (options.isVarargsIndex(i)) {
                argCalc.storageCalculator.adjustForVarArgs();
            }
            csb.addArgumentBindings(carrier, layout, argCalc.getBindings(carrier, layout));
        }

        return new Bindings(csb.build(), returnInMemory);
    }

    public MethodHandle arrangeDowncall(MethodType mt, FunctionDescriptor cDesc, LinkerOptions options) {
        Bindings bindings = getBindings(mt, cDesc, false, options);

        MethodHandle handle = new DowncallLinker(C, bindings.callingSequence).getBoundMethodHandle();

        if (bindings.isInMemoryReturn) {
            handle = SharedUtils.adaptDowncallForIMR(handle, cDesc, bindings.callingSequence);
        }

        return handle;
    }

    public UpcallStubFactory arrangeUpcall(MethodType mt, FunctionDescriptor cDesc, LinkerOptions options) {
        Bindings bindings = getBindings(mt, cDesc, true, options);

        final boolean dropReturn = true; /* drop return, since we don't have bindings for it */
        return SharedUtils.arrangeUpcallHelper(mt, bindings.isInMemoryReturn, dropReturn, C,
                bindings.callingSequence);
    }

    private boolean isInMemoryReturn(Optional<MemoryLayout> returnLayout) {
        return returnLayout
            .filter(GroupLayout.class::isInstance)
            .filter(layout -> !TypeClass.isStructHFAorReturnRegisterAggregate(layout, useABIv2))
            .isPresent();
    }

    class StorageCalculator {
        private final boolean forArguments;

        private final int[] nRegs = new int[] { 0, 0 };
        private long stackOffset = 0;

        public StorageCalculator(boolean forArguments) {
            this.forArguments = forArguments;
        }

        VMStorage stackAlloc(long size, long alignment) {
            long alignedStackOffset = Utils.alignUp(stackOffset, alignment);

            short encodedSize = (short) size;
            assert (encodedSize & 0xFFFF) == size;

            VMStorage storage = PPC64Architecture.stackStorage(encodedSize, (int) alignedStackOffset);
            stackOffset = alignedStackOffset + size;
            return storage;
        }

        VMStorage regAlloc(int type) {
            // GP regs always need to get reserved even when float regs are used.
            int gpRegCnt = 1;
            int fpRegCnt = (type == StorageType.INTEGER) ? 0 : 1;

            // Use stack if not enough registers available.
            if (type == StorageType.FLOAT && nRegs[StorageType.FLOAT] + fpRegCnt > MAX_FLOAT_REGISTER_ARGUMENTS) {
                type = StorageType.INTEGER; // Try gp reg.
            }
            if (type == StorageType.INTEGER && nRegs[StorageType.INTEGER] + gpRegCnt > MAX_REGISTER_ARGUMENTS) return null;

            VMStorage[] source = (forArguments ? C.inputStorage : C.outputStorage)[type];
            VMStorage result = source[nRegs[type]];

            nRegs[StorageType.INTEGER] += gpRegCnt;
            nRegs[StorageType.FLOAT] += fpRegCnt;
            return result;
        }

        // Integers need size for int to long conversion (required by ABI).
        // FP loads and stores must use the correct IEEE 754 precision format (32/64 bit).
        // Note: Can return a GP reg for a float!
        VMStorage nextStorage(int type, boolean is32Bit) {
            VMStorage reg = regAlloc(type);
            // Stack layout computation: We need to count all arguments in order to get the correct
            // offset for the next argument which will really use the stack.
            // The reserved space for the Parameter Save Area is determined by the DowncallStubGenerator.
            VMStorage stack;
            if (!useABIv2 && is32Bit) {
                stackAlloc(4, STACK_SLOT_SIZE); // Skip first half of stack slot.
                stack = stackAlloc(4, 4);
            } else {
                stack = stackAlloc(is32Bit ? 4 : 8, STACK_SLOT_SIZE);
            }
            if (reg == null) return stack;
            if (is32Bit) {
                reg = new VMStorage(reg.type(), PPC64Architecture.REG32_MASK, reg.indexOrOffset());
            }
            return reg;
        }

        // Regular struct, no HFA.
        VMStorage[] structAlloc(MemoryLayout layout) {
            // TODO: Big Endian can't pass partially used slots correctly in some cases with:
            // !useABIv2 && layout.byteSize() > 8 && layout.byteSize() % 8 != 0

            // Allocate enough gp slots (regs and stack) such that the struct fits in them.
            int numChunks = (int) Utils.alignUp(layout.byteSize(), MAX_COPY_SIZE) / MAX_COPY_SIZE;
            VMStorage[] result = new VMStorage[numChunks];
            for (int i = 0; i < numChunks; i++) {
                result[i] = nextStorage(StorageType.INTEGER, false);
            }
            return result;
        }

        HfaRegs hfaAlloc(List<MemoryLayout> scalarLayouts) {
            // Determine count and type.
            int count = scalarLayouts.size();
            Class<?> elementCarrier = ((ValueLayout) (scalarLayouts.get(0))).carrier();
            int elementSize = (elementCarrier == float.class) ? 4 : 8;

            // Allocate registers.
            int fpRegCnt = count;
            // Rest will get put into a struct. Compute number of 64 bit slots.
            int structSlots = 0;
            boolean needOverlapping = false; // See "no partial DW rule" below.

            int availableFpRegs = MAX_FLOAT_REGISTER_ARGUMENTS - nRegs[StorageType.FLOAT];
            if (count > availableFpRegs) {
                fpRegCnt = availableFpRegs;
                int remainingElements = count - availableFpRegs;
                if (elementCarrier == float.class) {
                    if ((fpRegCnt & 1) != 0) {
                        needOverlapping = true;
                        remainingElements--; // After overlapped one.
                    }
                    structSlots = (remainingElements + 1) / 2;
                } else {
                    structSlots = remainingElements;
                }
            }

            VMStorage[] source = (forArguments ? C.inputStorage : C.outputStorage)[StorageType.FLOAT];
            VMStorage[] result  = new VMStorage[fpRegCnt + structSlots],
                        result2 = new VMStorage[fpRegCnt + structSlots]; // For overlapping.
            if (elementCarrier == float.class) {
                // Mark elements as single precision (32 bit).
                for (int i = 0; i < fpRegCnt; i++) {
                    VMStorage sourceReg = source[nRegs[StorageType.FLOAT] + i];
                    result[i] = new VMStorage(StorageType.FLOAT, PPC64Architecture.REG32_MASK,
                                              sourceReg.indexOrOffset());
                }
            } else {
                for (int i = 0; i < fpRegCnt; i++) {
                    result[i] = source[nRegs[StorageType.FLOAT] + i];
                }
            }

            nRegs[StorageType.FLOAT] += fpRegCnt;
            // Reserve GP regs and stack slots for the packed HFA (when using single precision).
            int gpRegCnt = (elementCarrier == float.class) ? ((fpRegCnt + 1) / 2)
                                                           : fpRegCnt;
            nRegs[StorageType.INTEGER] += gpRegCnt;
            stackAlloc(fpRegCnt * elementSize, STACK_SLOT_SIZE);

            if (needOverlapping) {
                // "no partial DW rule": Put GP reg or stack slot into result2.
                // Note: Can only happen with forArguments = true.
                VMStorage overlappingReg;
                if (nRegs[StorageType.INTEGER] <= MAX_REGISTER_ARGUMENTS) {
                    VMStorage allocatedGpReg = C.inputStorage[StorageType.INTEGER][nRegs[StorageType.INTEGER] - 1];
                    overlappingReg = new VMStorage(StorageType.INTEGER,
                                                   PPC64Architecture.REG64_MASK, allocatedGpReg.indexOrOffset());
                } else {
                    overlappingReg = new VMStorage(StorageType.STACK,
                                                   (short) STACK_SLOT_SIZE, (int) stackOffset - 4);
                    stackOffset += 4; // We now have a 64 bit slot, but reserved only 32 bit before.
                }
                result2[fpRegCnt - 1] = overlappingReg;
            }

            // Allocate rest as struct.
            for (int i = 0; i < structSlots; i++) {
                result[fpRegCnt + i] = nextStorage(StorageType.INTEGER, false);
            }

            return new HfaRegs(result, result2);
        }

        void adjustForVarArgs() {
            // PPC64 can pass VarArgs in GP regs. But we're not using FP regs.
            nRegs[StorageType.FLOAT] = MAX_FLOAT_REGISTER_ARGUMENTS;
        }
    }

    abstract class BindingCalculator {
        protected final StorageCalculator storageCalculator;

        protected BindingCalculator(boolean forArguments) {
            this.storageCalculator = new StorageCalculator(forArguments);
        }

        abstract List<Binding> getBindings(Class<?> carrier, MemoryLayout layout);
    }

    // Compute recipe for transfering arguments / return values to C from Java.
    class UnboxBindingCalculator extends BindingCalculator {
        UnboxBindingCalculator(boolean forArguments) {
            super(forArguments);
        }

        @Override
        List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.classifyLayout(layout, useABIv2);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass) {
                case STRUCT_REGISTER -> {
                    assert carrier == MemorySegment.class;
                    VMStorage[] regs = storageCalculator.structAlloc(layout);
                    long offset = 0;
                    for (VMStorage storage : regs) {
                        // Last slot may be partly used.
                        final long size = Math.min(layout.byteSize() - offset, MAX_COPY_SIZE);
                        Class<?> type = SharedUtils.primitiveCarrierForSize(size, false);
                        if (offset + size < layout.byteSize()) {
                            bindings.dup();
                        }
                        bindings.bufferLoad(offset, type, (int) size)
                                .vmStore(storage, type);
                        offset += size;
                    }
                }
                case STRUCT_HFA -> {
                    assert carrier == MemorySegment.class;
                    List<MemoryLayout> scalarLayouts = TypeClass.scalarLayouts((GroupLayout) layout);
                    HfaRegs regs = storageCalculator.hfaAlloc(scalarLayouts);
                    final long baseSize = scalarLayouts.get(0).byteSize();
                    long offset = 0;
                    for (int index = 0; index < regs.first().length; index++) {
                        VMStorage storage = regs.first()[index];
                        // Floats are 4 Bytes, Double, GP reg and stack slots 8 Bytes (except maybe last slot).
                        long size = (baseSize == 4 &&
                                     (storage.type() == StorageType.FLOAT || layout.byteSize() - offset < 8)) ? 4 : 8;
                        Class<?> type = SharedUtils.primitiveCarrierForSize(size, storage.type() == StorageType.FLOAT);
                        if (offset + size < layout.byteSize()) {
                            bindings.dup();
                        }
                        bindings.bufferLoad(offset, type)
                                .vmStore(storage, type);
                        VMStorage storage2 = regs.second()[index];
                        if (storage2 != null) {
                            // We have a second slot to fill (always 64 bit GP reg or stack slot).
                            size = 8;
                            if (offset + size < layout.byteSize()) {
                                bindings.dup();
                            }
                            bindings.bufferLoad(offset, long.class)
                                    .vmStore(storage2, long.class);
                        }
                        offset += size;
                    }
                }
                case POINTER -> {
                    VMStorage storage = storageCalculator.nextStorage(StorageType.INTEGER, false);
                    bindings.unboxAddress()
                            .vmStore(storage, long.class);
                }
                case INTEGER -> {
                    // ABI requires all int types to get extended to 64 bit.
                    VMStorage storage = storageCalculator.nextStorage(StorageType.INTEGER, false);
                    bindings.vmStore(storage, carrier);
                }
                case FLOAT -> {
                    VMStorage storage = storageCalculator.nextStorage(StorageType.FLOAT, carrier == float.class);
                    bindings.vmStore(storage, carrier);
                }
                default -> throw new UnsupportedOperationException("Unhandled class " + argumentClass);
            }
            return bindings.build();
        }
    }

    // Compute recipe for transfering arguments / return values from C to Java.
    class BoxBindingCalculator extends BindingCalculator {
        BoxBindingCalculator(boolean forArguments) {
            super(forArguments);
        }

        @Override
        List<Binding> getBindings(Class<?> carrier, MemoryLayout layout) {
            TypeClass argumentClass = TypeClass.classifyLayout(layout, useABIv2);
            Binding.Builder bindings = Binding.builder();
            switch (argumentClass) {
                case STRUCT_REGISTER -> {
                    assert carrier == MemorySegment.class;
                    bindings.allocate(layout);
                    VMStorage[] regs = storageCalculator.structAlloc(layout);
                    long offset = 0;
                    for (VMStorage storage : regs) {
                        // Last slot may be partly used.
                        final long size = Math.min(layout.byteSize() - offset, MAX_COPY_SIZE);
                        Class<?> type = SharedUtils.primitiveCarrierForSize(size, false);
                        bindings.dup()
                                .vmLoad(storage, type)
                                .bufferStore(offset, type, (int) size);
                        offset += size;
                    }
                }
                case STRUCT_HFA -> {
                    assert carrier == MemorySegment.class;
                    bindings.allocate(layout);
                    List<MemoryLayout> scalarLayouts = TypeClass.scalarLayouts((GroupLayout) layout);
                    HfaRegs regs = storageCalculator.hfaAlloc(scalarLayouts);
                    final long baseSize = scalarLayouts.get(0).byteSize();
                    long offset = 0;
                    for (int index = 0; index < regs.first().length; index++) {
                        // Use second if available since first one only contains one 32 bit value.
                        VMStorage storage = regs.second()[index] == null ? regs.first()[index] : regs.second()[index];
                        // Floats are 4 Bytes, Double, GP reg and stack slots 8 Bytes (except maybe last slot).
                        final long size = (baseSize == 4 &&
                                           (storage.type() == StorageType.FLOAT || layout.byteSize() - offset < 8)) ? 4 : 8;
                        Class<?> type = SharedUtils.primitiveCarrierForSize(size, storage.type() == StorageType.FLOAT);
                        bindings.dup()
                                .vmLoad(storage, type)
                                .bufferStore(offset, type);
                        offset += size;
                    }
                }
                case POINTER -> {
                    AddressLayout addressLayout = (AddressLayout) layout;
                    VMStorage storage = storageCalculator.nextStorage(StorageType.INTEGER, false);
                    bindings.vmLoad(storage, long.class)
                            .boxAddressRaw(Utils.pointeeByteSize(addressLayout), Utils.pointeeByteAlign(addressLayout));
                }
                case INTEGER -> {
                    // We could use carrier != long.class for BoxBindingCalculator, but C always uses 64 bit slots.
                    VMStorage storage = storageCalculator.nextStorage(StorageType.INTEGER, false);
                    bindings.vmLoad(storage, carrier);
                }
                case FLOAT -> {
                    VMStorage storage = storageCalculator.nextStorage(StorageType.FLOAT, carrier == float.class);
                    bindings.vmLoad(storage, carrier);
                }
                default -> throw new UnsupportedOperationException("Unhandled class " + argumentClass);
            }
            return bindings.build();
        }
    }
}
