/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.hotspot;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.graalvm.compiler.core.common.CompressEncoding;
import org.graalvm.compiler.hotspot.nodes.GraalHotSpotVMConfigNode;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Used to access native configuration details.
 */
public class GraalHotSpotVMConfig extends GraalHotSpotVMConfigBase {

    GraalHotSpotVMConfig(HotSpotVMConfigStore store) {
        super(store);

        assert narrowKlassShift <= logKlassAlignment;
        assert narrowOopShift <= logMinObjAlignment();
        oopEncoding = new CompressEncoding(narrowOopBase, narrowOopShift);
        klassEncoding = new CompressEncoding(narrowKlassBase, narrowKlassShift);

        assert check();
    }

    private final CompressEncoding oopEncoding;
    private final CompressEncoding klassEncoding;
    private final String markWord = versioned.markWordFieldType;

    public CompressEncoding getOopEncoding() {
        return oopEncoding;
    }

    public CompressEncoding getKlassEncoding() {
        return klassEncoding;
    }

    public final boolean cAssertions = getConstant("ASSERT", Boolean.class);

    public final int codeEntryAlignment = getFlag("CodeEntryAlignment", Integer.class);
    public final boolean enableContended = getFlag("EnableContended", Boolean.class);
    public final boolean restrictContended = getFlag("RestrictContended", Boolean.class);
    public final int contendedPaddingWidth = getFlag("ContendedPaddingWidth", Integer.class);
    public final int fieldsAllocationStyle = versioned.fieldsAllocationStyle;
    public final boolean compactFields = versioned.compactFields;
    public final boolean verifyOops = getFlag("VerifyOops", Boolean.class);
    public final boolean ciTime = getFlag("CITime", Boolean.class);
    public final boolean ciTimeEach = getFlag("CITimeEach", Boolean.class);
    public final boolean dontCompileHugeMethods = getFlag("DontCompileHugeMethods", Boolean.class);
    public final int hugeMethodLimit = getFlag("HugeMethodLimit", Integer.class);
    public final boolean printInlining = getFlag("PrintInlining", Boolean.class);
    public final boolean inline = getFlag("Inline", Boolean.class);
    public final boolean inlineNotify = versioned.inlineNotify;
    public final boolean useFastLocking = getFlag("JVMCIUseFastLocking", Boolean.class);
    public final boolean forceUnreachable = getFlag("ForceUnreachable", Boolean.class);
    public final int codeSegmentSize = getFlag("CodeCacheSegmentSize", Integer.class);
    public final boolean foldStableValues = getFlag("FoldStableValues", Boolean.class);
    public final int maxVectorSize = getFlag("MaxVectorSize", Integer.class);

    public final boolean verifyBeforeGC = getFlag("VerifyBeforeGC", Boolean.class);
    public final boolean verifyAfterGC = getFlag("VerifyAfterGC", Boolean.class);

    public final boolean useTLAB = getFlag("UseTLAB", Boolean.class);
    public final boolean useBiasedLocking = getFlag("UseBiasedLocking", Boolean.class);
    public final boolean usePopCountInstruction = getFlag("UsePopCountInstruction", Boolean.class);
    public final boolean useAESIntrinsics = getFlag("UseAESIntrinsics", Boolean.class);
    public final boolean useAESCTRIntrinsics = getFlag("UseAESCTRIntrinsics", Boolean.class, false);
    public final boolean useCRC32Intrinsics = getFlag("UseCRC32Intrinsics", Boolean.class);
    public final boolean useCRC32CIntrinsics = versioned.useCRC32CIntrinsics;
    public final boolean threadLocalHandshakes = versioned.threadLocalHandshakes;

    private final boolean useMultiplyToLenIntrinsic = getFlag("UseMultiplyToLenIntrinsic", Boolean.class);
    private final boolean useSHA1Intrinsics = getFlag("UseSHA1Intrinsics", Boolean.class);
    private final boolean useSHA256Intrinsics = getFlag("UseSHA256Intrinsics", Boolean.class);
    private final boolean useSHA512Intrinsics = getFlag("UseSHA512Intrinsics", Boolean.class);
    private final boolean useGHASHIntrinsics = getFlag("UseGHASHIntrinsics", Boolean.class, false);
    private final boolean useBase64Intrinsics = getFlag("UseBASE64Intrinsics", Boolean.class, false);
    private final boolean useMontgomeryMultiplyIntrinsic = getFlag("UseMontgomeryMultiplyIntrinsic", Boolean.class, false);
    private final boolean useMontgomerySquareIntrinsic = getFlag("UseMontgomerySquareIntrinsic", Boolean.class, false);
    private final boolean useMulAddIntrinsic = getFlag("UseMulAddIntrinsic", Boolean.class, false);
    private final boolean useSquareToLenIntrinsic = getFlag("UseSquareToLenIntrinsic", Boolean.class, false);
    public final boolean useVectorizedMismatchIntrinsic = getFlag("UseVectorizedMismatchIntrinsic", Boolean.class, false);
    public final boolean useFMAIntrinsics = getFlag("UseFMA", Boolean.class, false);
    public final int useAVX3Threshold = getFlag("AVX3Threshold", Integer.class, 4096);

    public final boolean preserveFramePointer = getFlag("PreserveFramePointer", Boolean.class, false);

    /*
     * These are methods because in some JDKs the flags are visible but the stubs themselves haven't
     * been exported so we have to check both if the flag is on and if we have the stub.
     */
    public boolean useMultiplyToLenIntrinsic() {
        return useMultiplyToLenIntrinsic && multiplyToLen != 0;
    }

    public boolean useSHA1Intrinsics() {
        return useSHA1Intrinsics && sha1ImplCompress != 0 && sha1ImplCompressMultiBlock != 0;
    }

    public boolean useSHA256Intrinsics() {
        return useSHA256Intrinsics && sha256ImplCompress != 0 && sha256ImplCompressMultiBlock != 0;
    }

    public boolean useSHA512Intrinsics() {
        return useSHA512Intrinsics && sha512ImplCompress != 0 && sha512ImplCompressMultiBlock != 0;
    }

    public boolean useGHASHIntrinsics() {
        return useGHASHIntrinsics && ghashProcessBlocks != 0;
    }

    public boolean useBase64Intrinsics() {
        return useBase64Intrinsics && base64EncodeBlock != 0;
    }

    public boolean useMontgomeryMultiplyIntrinsic() {
        return useMontgomeryMultiplyIntrinsic && montgomeryMultiply != 0;
    }

    public boolean useMontgomerySquareIntrinsic() {
        return useMontgomerySquareIntrinsic && montgomerySquare != 0;
    }

    public boolean useMulAddIntrinsic() {
        return useMulAddIntrinsic && mulAdd != 0;
    }

    public boolean useSquareToLenIntrinsic() {
        return useSquareToLenIntrinsic && squareToLen != 0;
    }

    public boolean inlineNotify() {
        return inlineNotify && notifyAddress != 0;
    }

    public boolean inlineNotifyAll() {
        return inlineNotify && notifyAllAddress != 0;
    }

    public final boolean useG1GC = getFlag("UseG1GC", Boolean.class);
    public final boolean useCMSGC = getFlag("UseConcMarkSweepGC", Boolean.class, false);

    public final int allocatePrefetchStyle = getFlag("AllocatePrefetchStyle", Integer.class);
    public final int allocatePrefetchInstr = getFlag("AllocatePrefetchInstr", Integer.class);
    public final int allocatePrefetchLines = getFlag("AllocatePrefetchLines", Integer.class);
    public final int allocateInstancePrefetchLines = getFlag("AllocateInstancePrefetchLines", Integer.class);
    public final int allocatePrefetchStepSize = getFlag("AllocatePrefetchStepSize", Integer.class);
    public final int allocatePrefetchDistance = getFlag("AllocatePrefetchDistance", Integer.class);

    private final long universeCollectedHeap = getFieldValue("CompilerToVM::Data::Universe_collectedHeap", Long.class, "CollectedHeap*");
    private final int collectedHeapTotalCollectionsOffset = getFieldOffset("CollectedHeap::_total_collections", Integer.class, "unsigned int");

    public long gcTotalCollectionsAddress() {
        return universeCollectedHeap + collectedHeapTotalCollectionsOffset;
    }

    public final boolean useDeferredInitBarriers = getFlag("ReduceInitialCardMarks", Boolean.class);

    // Compressed Oops related values.
    public final boolean useCompressedOops = getFlag("UseCompressedOops", Boolean.class);
    public final boolean useCompressedClassPointers = getFlag("UseCompressedClassPointers", Boolean.class);

    public final long narrowOopBase = getFieldValue("CompilerToVM::Data::Universe_narrow_oop_base", Long.class, "address");
    public final int narrowOopShift = getFieldValue("CompilerToVM::Data::Universe_narrow_oop_shift", Integer.class, "int");
    public final int objectAlignment = getFlag("ObjectAlignmentInBytes", Integer.class);

    public final int minObjAlignment() {
        return objectAlignment / heapWordSize;
    }

    public final int logMinObjAlignment() {
        return (int) (Math.log(objectAlignment) / Math.log(2));
    }

    public final int narrowKlassSize = getFieldValue("CompilerToVM::Data::sizeof_narrowKlass", Integer.class, "int");
    public final long narrowKlassBase = getFieldValue("CompilerToVM::Data::Universe_narrow_klass_base", Long.class, "address");
    public final int narrowKlassShift = getFieldValue("CompilerToVM::Data::Universe_narrow_klass_shift", Integer.class, "int");
    public final int logKlassAlignment = getConstant("LogKlassAlignmentInBytes", Integer.class);

    public final int stackShadowPages = getFlag("StackShadowPages", Integer.class);
    public final int stackReservedPages = getFlag("StackReservedPages", Integer.class, 0);
    public final boolean useStackBanging = getFlag("UseStackBanging", Boolean.class);
    public final int stackBias = getConstant("STACK_BIAS", Integer.class);
    public final int vmPageSize = getFieldValue("CompilerToVM::Data::vm_page_size", Integer.class, "int");

    public final int markOffset = getFieldOffset("oopDesc::_mark", Integer.class, markWord);
    public final int hubOffset = getFieldOffset("oopDesc::_metadata._klass", Integer.class, "Klass*");

    public final int prototypeMarkWordOffset = getFieldOffset("Klass::_prototype_header", Integer.class, markWord);
    public final int subklassOffset = getFieldOffset("Klass::_subklass", Integer.class, "Klass*");
    public final int nextSiblingOffset = getFieldOffset("Klass::_next_sibling", Integer.class, "Klass*");
    public final int superCheckOffsetOffset = getFieldOffset("Klass::_super_check_offset", Integer.class, "juint");
    public final int secondarySuperCacheOffset = getFieldOffset("Klass::_secondary_super_cache", Integer.class, "Klass*");
    public final int secondarySupersOffset = getFieldOffset("Klass::_secondary_supers", Integer.class, "Array<Klass*>*");

    public final boolean classMirrorIsHandle = versioned.classMirrorIsHandle;
    public final int classMirrorOffset = versioned.classMirrorOffset;

    public final int klassSuperKlassOffset = getFieldOffset("Klass::_super", Integer.class, "Klass*");
    public final int klassModifierFlagsOffset = getFieldOffset("Klass::_modifier_flags", Integer.class, "jint");
    public final int klassAccessFlagsOffset = getFieldOffset("Klass::_access_flags", Integer.class, "AccessFlags");
    public final int klassLayoutHelperOffset = getFieldOffset("Klass::_layout_helper", Integer.class, "jint");

    public final int klassLayoutHelperNeutralValue = getConstant("Klass::_lh_neutral_value", Integer.class);
    public final int layoutHelperLog2ElementSizeShift = getConstant("Klass::_lh_log2_element_size_shift", Integer.class);
    public final int layoutHelperLog2ElementSizeMask = getConstant("Klass::_lh_log2_element_size_mask", Integer.class);
    public final int layoutHelperElementTypeShift = getConstant("Klass::_lh_element_type_shift", Integer.class);
    public final int layoutHelperElementTypeMask = getConstant("Klass::_lh_element_type_mask", Integer.class);
    public final int layoutHelperHeaderSizeShift = getConstant("Klass::_lh_header_size_shift", Integer.class);
    public final int layoutHelperHeaderSizeMask = getConstant("Klass::_lh_header_size_mask", Integer.class);
    public final int layoutHelperArrayTagShift = getConstant("Klass::_lh_array_tag_shift", Integer.class);
    public final int layoutHelperArrayTagTypeValue = getConstant("Klass::_lh_array_tag_type_value", Integer.class);
    public final int layoutHelperArrayTagObjectValue = getConstant("Klass::_lh_array_tag_obj_value", Integer.class);

    /**
     * This filters out the bit that differentiates a type array from an object array.
     */
    public int layoutHelperElementTypePrimitiveInPlace() {
        return (layoutHelperArrayTagTypeValue & ~layoutHelperArrayTagObjectValue) << layoutHelperArrayTagShift;
    }

    public final int vtableEntrySize = getFieldValue("CompilerToVM::Data::sizeof_vtableEntry", Integer.class, "int");
    public final int vtableEntryMethodOffset = getFieldOffset("vtableEntry::_method", Integer.class, "Method*");

    public final int instanceKlassInitStateOffset = getFieldOffset("InstanceKlass::_init_state", Integer.class, "u1");
    public final int instanceKlassInitThreadOffset = getFieldOffset("InstanceKlass::_init_thread", Integer.class, "Thread*", -1);
    public final int instanceKlassConstantsOffset = getFieldOffset("InstanceKlass::_constants", Integer.class, "ConstantPool*");
    public final int instanceKlassFieldsOffset = getFieldOffset("InstanceKlass::_fields", Integer.class, "Array<u2>*");
    public final int klassVtableStartOffset = getFieldValue("CompilerToVM::Data::Klass_vtable_start_offset", Integer.class, "int");
    public final int klassVtableLengthOffset = getFieldValue("CompilerToVM::Data::Klass_vtable_length_offset", Integer.class, "int");

    public final int instanceKlassStateLinked = getConstant("InstanceKlass::linked", Integer.class);
    public final int instanceKlassStateBeingInitialized = getConstant("InstanceKlass::being_initialized", Integer.class, -1);
    public final int instanceKlassStateFullyInitialized = getConstant("InstanceKlass::fully_initialized", Integer.class);

    public final int arrayOopDescSize = getFieldValue("CompilerToVM::Data::sizeof_arrayOopDesc", Integer.class, "int");

    /**
     * The offset of the array length word in an array object's header.
     *
     * See {@code arrayOopDesc::length_offset_in_bytes()}.
     */
    public final int arrayOopDescLengthOffset() {
        return useCompressedClassPointers ? hubOffset + narrowKlassSize : arrayOopDescSize;
    }

    public final int arrayU1LengthOffset = getFieldOffset("Array<int>::_length", Integer.class, "int");
    public final int arrayU1DataOffset = getFieldOffset("Array<u1>::_data", Integer.class);
    public final int arrayU2DataOffset = getFieldOffset("Array<u2>::_data", Integer.class);
    public final int metaspaceArrayLengthOffset = getFieldOffset("Array<Klass*>::_length", Integer.class, "int");
    public final int metaspaceArrayBaseOffset = getFieldOffset("Array<Klass*>::_data[0]", Integer.class, "Klass*");

    public final int arrayClassElementOffset = getFieldOffset("ObjArrayKlass::_element_klass", Integer.class, "Klass*");

    public final int fieldInfoAccessFlagsOffset = getConstant("FieldInfo::access_flags_offset", Integer.class);
    public final int fieldInfoNameIndexOffset = getConstant("FieldInfo::name_index_offset", Integer.class);
    public final int fieldInfoSignatureIndexOffset = getConstant("FieldInfo::signature_index_offset", Integer.class);
    public final int fieldInfoInitvalIndexOffset = getConstant("FieldInfo::initval_index_offset", Integer.class);
    public final int fieldInfoLowPackedOffset = getConstant("FieldInfo::low_packed_offset", Integer.class);
    public final int fieldInfoHighPackedOffset = getConstant("FieldInfo::high_packed_offset", Integer.class);
    public final int fieldInfoFieldSlots = getConstant("FieldInfo::field_slots", Integer.class);

    public final int fieldInfoTagSize = getConstant("FIELDINFO_TAG_SIZE", Integer.class);

    public final int jvmAccMonitorMatch = getConstant("JVM_ACC_MONITOR_MATCH", Integer.class);
    public final int jvmAccHasMonitorBytecodes = getConstant("JVM_ACC_HAS_MONITOR_BYTECODES", Integer.class);
    public final int jvmAccHasFinalizer = getConstant("JVM_ACC_HAS_FINALIZER", Integer.class);
    public final int jvmAccFieldInternal = getConstant("JVM_ACC_FIELD_INTERNAL", Integer.class);
    public final int jvmAccFieldStable = getConstant("JVM_ACC_FIELD_STABLE", Integer.class);
    public final int jvmAccFieldHasGenericSignature = getConstant("JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE", Integer.class);
    public final int jvmAccWrittenFlags = getConstant("JVM_ACC_WRITTEN_FLAGS", Integer.class);
    public final int jvmAccSynthetic = getConstant("JVM_ACC_SYNTHETIC", Integer.class);
    public final int jvmAccIsHiddenClass = getConstant("JVM_ACC_IS_HIDDEN_CLASS", Integer.class);

    public final int jvmciCompileStateCanPostOnExceptionsOffset = getJvmciJvmtiCapabilityOffset("_jvmti_can_post_on_exceptions");
    public final int jvmciCompileStateCanPopFrameOffset = getJvmciJvmtiCapabilityOffset("_jvmti_can_pop_frame");
    public final int jvmciCompileStateCanAccessLocalVariablesOffset = getJvmciJvmtiCapabilityOffset("_jvmti_can_access_local_variables");

    // Integer.MIN_VALUE if not available
    private int getJvmciJvmtiCapabilityOffset(String name) {
        int offset = getFieldOffset("JVMCICompileState::" + name, Integer.class, "jbyte", Integer.MIN_VALUE);
        if (offset == Integer.MIN_VALUE) {
            // JDK 12
            offset = getFieldOffset("JVMCIEnv::" + name, Integer.class, "jbyte", Integer.MIN_VALUE);
        }
        return offset;
    }

    public final int threadTlabOffset = getFieldOffset("Thread::_tlab", Integer.class, "ThreadLocalAllocBuffer");
    public final int javaThreadAnchorOffset = getFieldOffset("JavaThread::_anchor", Integer.class, "JavaFrameAnchor");
    public final int javaThreadShouldPostOnExceptionsFlagOffset = getFieldOffset("JavaThread::_should_post_on_exceptions_flag", Integer.class, "int", Integer.MIN_VALUE);
    public final int threadObjectOffset = getFieldOffset("JavaThread::_threadObj", Integer.class, "oop");
    public final int osThreadOffset = getFieldOffset("JavaThread::_osthread", Integer.class, "OSThread*", Integer.MAX_VALUE);
    public final int threadIsMethodHandleReturnOffset = getFieldOffset("JavaThread::_is_method_handle_return", Integer.class, "int");
    public final int threadObjectResultOffset = getFieldOffset("JavaThread::_vm_result", Integer.class, "oop");
    public final int jvmciCountersThreadOffset = getFieldOffset("JavaThread::_jvmci_counters", Integer.class, "jlong*");
    public final int doingUnsafeAccessOffset = getFieldOffset("JavaThread::_doing_unsafe_access", Integer.class, "bool", Integer.MAX_VALUE);
    public final int javaThreadReservedStackActivationOffset = versioned.javaThreadReservedStackActivationOffset;
    public final int jniEnvironmentOffset = getFieldOffset("JavaThread::_jni_environment", Integer.class, "JNIEnv", Integer.MIN_VALUE);

    public boolean requiresReservedStackCheck(List<ResolvedJavaMethod> methods) {
        if (enableStackReservedZoneAddress != 0 && methods != null) {
            for (ResolvedJavaMethod method : methods) {
                if (((HotSpotResolvedJavaMethod) method).hasReservedStackAccess()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * An invalid value for {@link #rtldDefault}.
     */
    public static final long INVALID_RTLD_DEFAULT_HANDLE = 0xDEADFACE;

    /**
     * Address of the library lookup routine. The C signature of this routine is:
     *
     * <pre>
     *     void* (const char *filename, char *ebuf, int ebuflen)
     * </pre>
     */
    public final long dllLoad = getAddress("os::dll_load");

    /**
     * Address of the library lookup routine. The C signature of this routine is:
     *
     * <pre>
     *     void* (void* handle, const char* name)
     * </pre>
     */
    public final long dllLookup = getAddress("os::dll_lookup");

    /**
     * A pseudo-handle which when used as the first argument to {@link #dllLookup} means lookup will
     * return the first occurrence of the desired symbol using the default library search order. If
     * this field is {@value #INVALID_RTLD_DEFAULT_HANDLE}, then this capability is not supported on
     * the current platform.
     */
    public final long rtldDefault = getAddress("RTLD_DEFAULT", osName.equals("bsd") || osName.equals("linux") ? null : INVALID_RTLD_DEFAULT_HANDLE);

    /**
     * This field is used to pass exception objects into and out of the runtime system during
     * exception handling for compiled code.
     */
    public final int threadExceptionOopOffset = getFieldOffset("JavaThread::_exception_oop", Integer.class, "oop");
    public final int threadExceptionPcOffset = getFieldOffset("JavaThread::_exception_pc", Integer.class, "address");
    public final int pendingExceptionOffset = getFieldOffset("ThreadShadow::_pending_exception", Integer.class, "oop");

    public final int pendingDeoptimizationOffset = getFieldOffset("JavaThread::_pending_deoptimization", Integer.class, "int");
    public final int pendingTransferToInterpreterOffset = getFieldOffset("JavaThread::_pending_transfer_to_interpreter", Integer.class, "bool");

    private final int javaFrameAnchorLastJavaSpOffset = getFieldOffset("JavaFrameAnchor::_last_Java_sp", Integer.class, "intptr_t*");
    private final int javaFrameAnchorLastJavaPcOffset = getFieldOffset("JavaFrameAnchor::_last_Java_pc", Integer.class, "address");

    public final int pendingFailedSpeculationOffset;
    {
        String name = "JavaThread::_pending_failed_speculation";
        int offset = -1;
        try {
            offset = getFieldOffset(name, Integer.class, "jlong");
        } catch (JVMCIError e) {
            try {
                offset = getFieldOffset(name, Integer.class, "long");
            } catch (JVMCIError e2) {
            }
        }
        if (offset == -1) {
            throw new JVMCIError("cannot get offset of field " + name + " with type long or jlong");
        }
        pendingFailedSpeculationOffset = offset;
    }

    public int threadLastJavaSpOffset() {
        return javaThreadAnchorOffset + javaFrameAnchorLastJavaSpOffset;
    }

    public int threadLastJavaPcOffset() {
        return javaThreadAnchorOffset + javaFrameAnchorLastJavaPcOffset;
    }

    public int threadLastJavaFpOffset() {
        assert osArch.equals("aarch64") || osArch.equals("amd64");
        return javaThreadAnchorOffset + getFieldOffset("JavaFrameAnchor::_last_Java_fp", Integer.class, "intptr_t*");
    }

    public int threadJavaFrameAnchorFlagsOffset() {
        assert osArch.equals("sparc");
        return javaThreadAnchorOffset + getFieldOffset("JavaFrameAnchor::_flags", Integer.class, "int");
    }

    public final int runtimeCallStackSize = getConstant("frame::arg_reg_save_area_bytes", Integer.class, intRequiredOnAMD64);
    public final int frameInterpreterFrameSenderSpOffset = getConstant("frame::interpreter_frame_sender_sp_offset", Integer.class, intRequiredOnAMD64);
    public final int frameInterpreterFrameLastSpOffset = getConstant("frame::interpreter_frame_last_sp_offset", Integer.class, intRequiredOnAMD64);

    public final int osThreadInterruptedOffset = getFieldOffset("OSThread::_interrupted", Integer.class, "jint", Integer.MAX_VALUE);

    public final long markWordHashShift = getConstant(markWordField("hash_shift"), Long.class);

    public final int biasedLockMaskInPlace = getConstant(markWordField("biased_lock_mask_in_place"), Integer.class);
    public final int ageMaskInPlace = getConstant(markWordField("age_mask_in_place"), Integer.class);
    public final int epochMaskInPlace = getConstant(markWordField("epoch_mask_in_place"), Integer.class);
    public final long markWordHashMask = getConstant(markWordField("hash_mask"), Long.class);
    public final long markWordHashMaskInPlace = getConstant(markWordField("hash_mask_in_place"), Long.class);

    public final int unlockedMask = getConstant(markWordField("unlocked_value"), Integer.class);
    public final int monitorMask = getConstant(markWordField("monitor_value"), Integer.class, -1);
    public final int biasedLockPattern = getConstant(markWordField("biased_lock_pattern"), Integer.class);

    // This field has no type in vmStructs.cpp
    public final int objectMonitorOwner = getFieldOffset("ObjectMonitor::_owner", Integer.class, null, -1);
    public final int objectMonitorRecursions = getFieldOffset("ObjectMonitor::_recursions", Integer.class, "intptr_t", -1);
    public final int objectMonitorCxq = getFieldOffset("ObjectMonitor::_cxq", Integer.class, "ObjectWaiter*", -1);
    public final int objectMonitorEntryList = getFieldOffset("ObjectMonitor::_EntryList", Integer.class, "ObjectWaiter*", -1);
    public final int objectMonitorSucc = getFieldOffset("ObjectMonitor::_succ", Integer.class, "Thread*", -1);

    public final int markWordNoHashInPlace = getConstant(markWordField("no_hash_in_place"), Integer.class);
    public final int markWordNoLockInPlace = getConstant(markWordField("no_lock_in_place"), Integer.class);

    /**
     * See {@code markOopDesc::prototype()}/{@code markWord::prototype()}.
     */
    public long arrayPrototypeMarkWord() {
        return markWordNoHashInPlace | markWordNoLockInPlace;
    }

    /**
     * See {@code markOopDesc::copy_set_hash()}/{@code markWord::copy_set_hash()}.
     */
    public long tlabIntArrayMarkWord() {
        long tmp = arrayPrototypeMarkWord() & (~markWordHashMaskInPlace);
        tmp |= ((0x2 & markWordHashMask) << markWordHashShift);
        return tmp;
    }

    private String markWordField(String simpleName) {
        return versioned.markWordClassName + "::" + simpleName;
    }

    /**
     * Mark word right shift to get identity hash code.
     */
    public final int identityHashCodeShift = getConstant(markWordField("hash_shift"), Integer.class);

    /**
     * Identity hash code value when uninitialized.
     */
    public final int uninitializedIdentityHashCodeValue = getConstant(markWordField("no_hash"), Integer.class);

    public final int methodAccessFlagsOffset = getFieldOffset("Method::_access_flags", Integer.class, "AccessFlags");
    public final int methodConstMethodOffset = getFieldOffset("Method::_constMethod", Integer.class, "ConstMethod*");
    public final int methodIntrinsicIdOffset = versioned.methodIntrinsicIdOffset;
    public final int methodFlagsOffset = versioned.methodFlagsOffset;
    public final int methodVtableIndexOffset = getFieldOffset("Method::_vtable_index", Integer.class, "int");

    public final int methodCountersOffset = getFieldOffset("Method::_method_counters", Integer.class, "MethodCounters*");
    public final int methodDataOffset = getFieldOffset("Method::_method_data", Integer.class, "MethodData*");
    public final int methodCompiledEntryOffset = getFieldOffset("Method::_from_compiled_entry", Integer.class, "address");
    public final int methodCodeOffset = versioned.methodCodeOffset;

    public final int methodFlagsCallerSensitive = getConstant("Method::_caller_sensitive", Integer.class);
    public final int methodFlagsForceInline = getConstant("Method::_force_inline", Integer.class);
    public final int methodFlagsDontInline = getConstant("Method::_dont_inline", Integer.class);
    public final int methodFlagsHidden = getConstant("Method::_hidden", Integer.class);
    public final int nonvirtualVtableIndex = getConstant("Method::nonvirtual_vtable_index", Integer.class);
    public final int invalidVtableIndex = getConstant("Method::invalid_vtable_index", Integer.class);

    public final int invocationCounterOffset = getFieldOffset("MethodCounters::_invocation_counter", Integer.class, "InvocationCounter");
    public final int backedgeCounterOffset = getFieldOffset("MethodCounters::_backedge_counter", Integer.class, "InvocationCounter");
    public final int invocationCounterIncrement = versioned.invocationCounterIncrement;
    public final int invocationCounterShift = versioned.invocationCounterShift;

    public final int nmethodEntryOffset = getFieldOffset("nmethod::_verified_entry_point",
                    Integer.class, "address");
    public final int compilationLevelFullOptimization = getConstant("CompLevel_full_optimization",
                    Integer.class);

    public final int constantPoolSize = getFieldValue("CompilerToVM::Data::sizeof_ConstantPool", Integer.class, "int");
    public final int constantPoolLengthOffset = getFieldOffset("ConstantPool::_length",
                    Integer.class, "int");

    public final int heapWordSize = getConstant("HeapWordSize", Integer.class);

    /**
     * Bit pattern that represents a non-oop. Neither the high bits nor the low bits of this value
     * are allowed to look like (respectively) the high or low bits of a real oop.
     */
    public final long nonOopBits = getFieldValue("CompilerToVM::Data::Universe_non_oop_bits", Long.class, "void*");

    public final long verifyOopCounterAddress = getFieldAddress("StubRoutines::_verify_oop_count", "jint");
    public final long verifyOopMask = getFieldValue("CompilerToVM::Data::Universe_verify_oop_mask", Long.class, "uintptr_t");
    public final long verifyOopBits = getFieldValue("CompilerToVM::Data::Universe_verify_oop_bits", Long.class, "uintptr_t");

    public final int logOfHRGrainBytes = getFieldValue("HeapRegion::LogOfHRGrainBytes", Integer.class, "int");

    public final long cardtableStartAddress = getFieldValue("CompilerToVM::Data::cardtable_start_address", Long.class, "CardTable::CardValue*");
    public final int cardtableShift = getFieldValue("CompilerToVM::Data::cardtable_shift", Integer.class, "int");

    /**
     * This is the largest stack offset encodeable in an OopMapValue. Offsets larger than this will
     * throw an exception during code installation.
     */
    public final int maxOopMapStackOffset = getFieldValue("CompilerToVM::Data::_max_oop_map_stack_offset", Integer.class, "int");

    public final long safepointPollingAddress = getFieldValue("os::_polling_page", Long.class, "address");

    // G1 Collector Related Values.

    public final byte dirtyCardValue = versioned.dirtyCardValue;
    public final byte g1YoungCardValue = versioned.g1YoungCardValue;

    public final int g1SATBQueueMarkingOffset = versioned.g1SATBQueueMarkingOffset;
    public final int g1SATBQueueIndexOffset = versioned.g1SATBQueueIndexOffset;
    public final int g1SATBQueueBufferOffset = versioned.g1SATBQueueBufferOffset;
    public final int g1CardQueueIndexOffset = versioned.g1CardQueueIndexOffset;
    public final int g1CardQueueBufferOffset = versioned.g1CardQueueBufferOffset;

    public final int klassOffset = getFieldValue("java_lang_Class::_klass_offset", Integer.class, "int");
    public final int arrayKlassOffset = getFieldValue("java_lang_Class::_array_klass_offset", Integer.class, "int");

    public final int basicLockSize = getFieldValue("CompilerToVM::Data::sizeof_BasicLock", Integer.class, "int");
    public final int basicLockDisplacedHeaderOffset = getFieldOffset("BasicLock::_displaced_header", Integer.class, markWord);

    public final int threadPollingPageOffset = getFieldOffset("Thread::_polling_page", Integer.class, "volatile void*", -1);
    public final int threadAllocatedBytesOffset = getFieldOffset("Thread::_allocated_bytes", Integer.class, "jlong");

    public final int tlabRefillWasteIncrement = getFlag("TLABWasteIncrement", Integer.class);

    private final int threadLocalAllocBufferStartOffset = getFieldOffset("ThreadLocalAllocBuffer::_start", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferEndOffset = getFieldOffset("ThreadLocalAllocBuffer::_end", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferTopOffset = getFieldOffset("ThreadLocalAllocBuffer::_top", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferPfTopOffset = getFieldOffset("ThreadLocalAllocBuffer::_pf_top", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferSlowAllocationsOffset = getFieldOffset("ThreadLocalAllocBuffer::_slow_allocations", Integer.class, "unsigned");
    private final int threadLocalAllocBufferFastRefillWasteOffset = getFieldOffset("ThreadLocalAllocBuffer::_fast_refill_waste", Integer.class, "unsigned");
    private final int threadLocalAllocBufferNumberOfRefillsOffset = getFieldOffset("ThreadLocalAllocBuffer::_number_of_refills", Integer.class, "unsigned");
    private final int threadLocalAllocBufferRefillWasteLimitOffset = getFieldOffset("ThreadLocalAllocBuffer::_refill_waste_limit", Integer.class, "size_t");
    private final int threadLocalAllocBufferDesiredSizeOffset = getFieldOffset("ThreadLocalAllocBuffer::_desired_size", Integer.class, "size_t");

    public int tlabSlowAllocationsOffset() {
        return threadTlabOffset + threadLocalAllocBufferSlowAllocationsOffset;
    }

    public int tlabFastRefillWasteOffset() {
        return threadTlabOffset + threadLocalAllocBufferFastRefillWasteOffset;
    }

    public int tlabNumberOfRefillsOffset() {
        return threadTlabOffset + threadLocalAllocBufferNumberOfRefillsOffset;
    }

    public int tlabRefillWasteLimitOffset() {
        return threadTlabOffset + threadLocalAllocBufferRefillWasteLimitOffset;
    }

    public int threadTlabSizeOffset() {
        return threadTlabOffset + threadLocalAllocBufferDesiredSizeOffset;
    }

    public int threadTlabStartOffset() {
        return threadTlabOffset + threadLocalAllocBufferStartOffset;
    }

    public int threadTlabEndOffset() {
        return threadTlabOffset + threadLocalAllocBufferEndOffset;
    }

    public int threadTlabTopOffset() {
        return threadTlabOffset + threadLocalAllocBufferTopOffset;
    }

    public int threadTlabPfTopOffset() {
        return threadTlabOffset + threadLocalAllocBufferPfTopOffset;
    }

    public final int tlabAlignmentReserve = getFieldValue("CompilerToVM::Data::ThreadLocalAllocBuffer_alignment_reserve", Integer.class, "size_t");

    public final boolean tlabStats = getFlag("TLABStats", Boolean.class);

    // We set 0x10 as default value to disable DC ZVA if this field is not present in HotSpot.
    // ARMv8-A architecture reference manual D12.2.35 Data Cache Zero ID register says:
    // * BS, bits [3:0] indicate log2 of the DC ZVA block size in (4-byte) words.
    // * DZP, bit [4] of indicates whether use of DC ZVA instruction is prohibited.
    public final int psrInfoDczidValue = getFieldValue("VM_Version::_psr_info.dczid_el0", Integer.class, "uint32_t", 0x10);

    // FIXME This is only temporary until the GC code is changed.
    public final boolean inlineContiguousAllocationSupported = getFieldValue("CompilerToVM::Data::_supports_inline_contig_alloc", Boolean.class);
    public final long heapEndAddress = getFieldValue("CompilerToVM::Data::_heap_end_addr", Long.class, "HeapWord**");
    public final long heapTopAddress = versioned.heapTopAddress;

    public final boolean cmsIncrementalMode = getFlag("CMSIncrementalMode", Boolean.class, false);

    public final long inlineCacheMissStub = getFieldValue("CompilerToVM::Data::SharedRuntime_ic_miss_stub", Long.class, "address");
    public final long handleWrongMethodStub = getFieldValue("CompilerToVM::Data::SharedRuntime_handle_wrong_method_stub", Long.class, "address");

    public final long deoptBlobUnpack = getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_unpack", Long.class, "address");
    public final long deoptBlobUnpackWithExceptionInTLS = getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_unpack_with_exception_in_tls", Long.class, "address", 0L);
    public final long deoptBlobUncommonTrap = getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_uncommon_trap", Long.class, "address");

    public final long codeCacheLowBound = versioned.codeCacheLowBound;
    public final long codeCacheHighBound = versioned.codeCacheHighBound;

    public final long aescryptEncryptBlockStub = getFieldValue("StubRoutines::_aescrypt_encryptBlock", Long.class, "address");
    public final long aescryptDecryptBlockStub = getFieldValue("StubRoutines::_aescrypt_decryptBlock", Long.class, "address");
    public final long cipherBlockChainingEncryptAESCryptStub = getFieldValue("StubRoutines::_cipherBlockChaining_encryptAESCrypt", Long.class, "address");
    public final long cipherBlockChainingDecryptAESCryptStub = getFieldValue("StubRoutines::_cipherBlockChaining_decryptAESCrypt", Long.class, "address");
    public final long updateBytesCRC32Stub = getFieldValue("StubRoutines::_updateBytesCRC32", Long.class, "address");
    public final long crcTableAddress = getFieldValue("StubRoutines::_crc_table_adr", Long.class, "address");

    public final long sha1ImplCompress = getFieldValue("StubRoutines::_sha1_implCompress", Long.class, "address", 0L);
    public final long sha1ImplCompressMultiBlock = getFieldValue("StubRoutines::_sha1_implCompressMB", Long.class, "address", 0L);
    public final long sha256ImplCompress = getFieldValue("StubRoutines::_sha256_implCompress", Long.class, "address", 0L);
    public final long sha256ImplCompressMultiBlock = getFieldValue("StubRoutines::_sha256_implCompressMB", Long.class, "address", 0L);
    public final long sha512ImplCompress = getFieldValue("StubRoutines::_sha512_implCompress", Long.class, "address", 0L);
    public final long sha512ImplCompressMultiBlock = getFieldValue("StubRoutines::_sha512_implCompressMB", Long.class, "address", 0L);
    public final long multiplyToLen = getFieldValue("StubRoutines::_multiplyToLen", Long.class, "address", longRequiredOnAMD64);

    public final long counterModeAESCrypt = getFieldValue("StubRoutines::_counterMode_AESCrypt", Long.class, "address", 0L);
    public final long ghashProcessBlocks = getFieldValue("StubRoutines::_ghash_processBlocks", Long.class, "address", 0L);
    public final long base64EncodeBlock = getFieldValue("StubRoutines::_base64_encodeBlock", Long.class, "address", 0L);
    public final long crc32cTableTddr = getFieldValue("StubRoutines::_crc32c_table_addr", Long.class, "address", 0L);
    public final long updateBytesCRC32C = getFieldValue("StubRoutines::_updateBytesCRC32C", Long.class, "address", 0L);
    public final long updateBytesAdler32 = getFieldValue("StubRoutines::_updateBytesAdler32", Long.class, "address", 0L);
    public final long squareToLen = getFieldValue("StubRoutines::_squareToLen", Long.class, "address", longRequiredOnAMD64);
    public final long mulAdd = getFieldValue("StubRoutines::_mulAdd", Long.class, "address", longRequiredOnAMD64);
    public final long montgomeryMultiply = getFieldValue("StubRoutines::_montgomeryMultiply", Long.class, "address", longRequiredOnAMD64);
    public final long montgomerySquare = getFieldValue("StubRoutines::_montgomerySquare", Long.class, "address", longRequiredOnAMD64);
    public final long vectorizedMismatch = getFieldValue("StubRoutines::_vectorizedMismatch", Long.class, "address", 0L);

    public final long throwDelayedStackOverflowErrorEntry = versioned.throwDelayedStackOverflowErrorEntry;

    public final long jbyteArraycopy = getFieldValue("StubRoutines::_jbyte_arraycopy", Long.class, "address");
    public final long jshortArraycopy = getFieldValue("StubRoutines::_jshort_arraycopy", Long.class, "address");
    public final long jintArraycopy = getFieldValue("StubRoutines::_jint_arraycopy", Long.class, "address");
    public final long jlongArraycopy = getFieldValue("StubRoutines::_jlong_arraycopy", Long.class, "address");
    public final long oopArraycopy = getFieldValue("StubRoutines::_oop_arraycopy", Long.class, "address");
    public final long oopArraycopyUninit = getFieldValue("StubRoutines::_oop_arraycopy_uninit", Long.class, "address");
    public final long jbyteDisjointArraycopy = getFieldValue("StubRoutines::_jbyte_disjoint_arraycopy", Long.class, "address");
    public final long jshortDisjointArraycopy = getFieldValue("StubRoutines::_jshort_disjoint_arraycopy", Long.class, "address");
    public final long jintDisjointArraycopy = getFieldValue("StubRoutines::_jint_disjoint_arraycopy", Long.class, "address");
    public final long jlongDisjointArraycopy = getFieldValue("StubRoutines::_jlong_disjoint_arraycopy", Long.class, "address");
    public final long oopDisjointArraycopy = getFieldValue("StubRoutines::_oop_disjoint_arraycopy", Long.class, "address");
    public final long oopDisjointArraycopyUninit = getFieldValue("StubRoutines::_oop_disjoint_arraycopy_uninit", Long.class, "address");
    public final long jbyteAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jbyte_arraycopy", Long.class, "address");
    public final long jshortAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jshort_arraycopy", Long.class, "address");
    public final long jintAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jint_arraycopy", Long.class, "address");
    public final long jlongAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jlong_arraycopy", Long.class, "address");
    public final long oopAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_oop_arraycopy", Long.class, "address");
    public final long oopAlignedArraycopyUninit = getFieldValue("StubRoutines::_arrayof_oop_arraycopy_uninit", Long.class, "address");
    public final long jbyteAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jbyte_disjoint_arraycopy", Long.class, "address");
    public final long jshortAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jshort_disjoint_arraycopy", Long.class, "address");
    public final long jintAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jint_disjoint_arraycopy", Long.class, "address");
    public final long jlongAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jlong_disjoint_arraycopy", Long.class, "address");
    public final long oopAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy", Long.class, "address");
    public final long oopAlignedDisjointArraycopyUninit = getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit", Long.class, "address");
    public final long checkcastArraycopy = getFieldValue("StubRoutines::_checkcast_arraycopy", Long.class, "address");
    public final long checkcastArraycopyUninit = getFieldValue("StubRoutines::_checkcast_arraycopy_uninit", Long.class, "address");
    public final long unsafeArraycopy = getFieldValue("StubRoutines::_unsafe_arraycopy", Long.class, "address");
    public final long genericArraycopy = getFieldValue("StubRoutines::_generic_arraycopy", Long.class, "address");

    // Allocation stubs that throw an exception when allocation fails
    public final long newInstanceAddress = getAddress("JVMCIRuntime::new_instance");
    public final long newArrayAddress = getAddress("JVMCIRuntime::new_array");
    public final long newMultiArrayAddress = getAddress("JVMCIRuntime::new_multi_array");
    public final long dynamicNewInstanceAddress = getAddress("JVMCIRuntime::dynamic_new_instance");

    // Allocation stubs that return null when allocation fails
    public final long newInstanceOrNullAddress = getAddress("JVMCIRuntime::new_instance_or_null", 0L);
    public final long newArrayOrNullAddress = getAddress("JVMCIRuntime::new_array_or_null", 0L);
    public final long newMultiArrayOrNullAddress = getAddress("JVMCIRuntime::new_multi_array_or_null", 0L);
    public final long dynamicNewInstanceOrNullAddress = getAddress("JVMCIRuntime::dynamic_new_instance_or_null", 0L);

    public boolean areNullAllocationStubsAvailable() {
        return newInstanceOrNullAddress != 0L;
    }

    /**
     * Checks that HotSpot implements all or none of the allocate-or-null stubs.
     */
    private boolean checkNullAllocationStubs() {
        if (newInstanceOrNullAddress == 0L) {
            assert newArrayOrNullAddress == 0L;
            assert newMultiArrayOrNullAddress == 0L;
            assert dynamicNewInstanceOrNullAddress == 0L;
        } else {
            assert newArrayOrNullAddress != 0L;
            assert newMultiArrayOrNullAddress != 0L;
            assert dynamicNewInstanceOrNullAddress != 0L;
        }
        return true;
    }

    public final long vmMessageAddress = getAddress("JVMCIRuntime::vm_message");
    public final long identityHashCodeAddress = getAddress("JVMCIRuntime::identity_hash_code");
    public final long exceptionHandlerForPcAddress = getAddress("JVMCIRuntime::exception_handler_for_pc");
    public final long monitorenterAddress = getAddress("JVMCIRuntime::monitorenter");
    public final long monitorexitAddress = getAddress("JVMCIRuntime::monitorexit");
    public final long notifyAddress = getAddress("JVMCIRuntime::object_notify", 0L);
    public final long notifyAllAddress = getAddress("JVMCIRuntime::object_notifyAll", 0L);
    public final long throwAndPostJvmtiExceptionAddress = getAddress("JVMCIRuntime::throw_and_post_jvmti_exception");
    public final long throwKlassExternalNameExceptionAddress = getAddress("JVMCIRuntime::throw_klass_external_name_exception");
    public final long throwClassCastExceptionAddress = getAddress("JVMCIRuntime::throw_class_cast_exception");
    public final long logPrimitiveAddress = getAddress("JVMCIRuntime::log_primitive");
    public final long logObjectAddress = getAddress("JVMCIRuntime::log_object");
    public final long logPrintfAddress = getAddress("JVMCIRuntime::log_printf");
    public final long vmErrorAddress = getAddress("JVMCIRuntime::vm_error");
    public final long loadAndClearExceptionAddress = getAddress("JVMCIRuntime::load_and_clear_exception");
    public final long writeBarrierPreAddress = getAddress("JVMCIRuntime::write_barrier_pre");
    public final long writeBarrierPostAddress = getAddress("JVMCIRuntime::write_barrier_post");
    public final long validateObject = getAddress("JVMCIRuntime::validate_object");

    public final long testDeoptimizeCallInt = getAddress("JVMCIRuntime::test_deoptimize_call_int");

    public final long registerFinalizerAddress = getAddress("SharedRuntime::register_finalizer");
    public final long exceptionHandlerForReturnAddressAddress = getAddress("SharedRuntime::exception_handler_for_return_address");
    public final long osrMigrationEndAddress = getAddress("SharedRuntime::OSR_migration_end");
    public final long enableStackReservedZoneAddress = versioned.enableStackReservedZoneAddress;

    public final long javaTimeMillisAddress = getAddress("os::javaTimeMillis");
    public final long javaTimeNanosAddress = getAddress("os::javaTimeNanos");
    public final long arithmeticSinAddress = getFieldValue("CompilerToVM::Data::dsin", Long.class, "address");
    public final long arithmeticCosAddress = getFieldValue("CompilerToVM::Data::dcos", Long.class, "address");
    public final long arithmeticTanAddress = getFieldValue("CompilerToVM::Data::dtan", Long.class, "address");
    public final long arithmeticExpAddress = getFieldValue("CompilerToVM::Data::dexp", Long.class, "address");
    public final long arithmeticLogAddress = getFieldValue("CompilerToVM::Data::dlog", Long.class, "address");
    public final long arithmeticLog10Address = getFieldValue("CompilerToVM::Data::dlog10", Long.class, "address");
    public final long arithmeticPowAddress = getFieldValue("CompilerToVM::Data::dpow", Long.class, "address");

    public final long fremAddress = getAddress("SharedRuntime::frem");
    public final long dremAddress = getAddress("SharedRuntime::drem");

    public final int jvmciCountersSize = getFlag("JVMCICounterSize", Integer.class);

    public final long deoptimizationFetchUnrollInfo = getAddress("Deoptimization::fetch_unroll_info");
    public final long deoptimizationUncommonTrap = getAddress("Deoptimization::uncommon_trap");
    public final long deoptimizationUnpackFrames = getAddress("Deoptimization::unpack_frames");

    public final int deoptimizationUnpackDeopt = getConstant("Deoptimization::Unpack_deopt", Integer.class);
    public final int deoptimizationUnpackException = getConstant("Deoptimization::Unpack_exception", Integer.class);
    public final int deoptimizationUnpackUncommonTrap = getConstant("Deoptimization::Unpack_uncommon_trap", Integer.class);
    public final int deoptimizationUnpackReexecute = getConstant("Deoptimization::Unpack_reexecute", Integer.class);

    public final int deoptimizationUnrollBlockSizeOfDeoptimizedFrameOffset = getFieldOffset("Deoptimization::UnrollBlock::_size_of_deoptimized_frame", Integer.class, "int");
    public final int deoptimizationUnrollBlockCallerAdjustmentOffset = getFieldOffset("Deoptimization::UnrollBlock::_caller_adjustment", Integer.class, "int");
    public final int deoptimizationUnrollBlockNumberOfFramesOffset = getFieldOffset("Deoptimization::UnrollBlock::_number_of_frames", Integer.class, "int");
    public final int deoptimizationUnrollBlockTotalFrameSizesOffset = getFieldOffset("Deoptimization::UnrollBlock::_total_frame_sizes", Integer.class, "int");
    public final int deoptimizationUnrollBlockUnpackKindOffset = getFieldOffset("Deoptimization::UnrollBlock::_unpack_kind", Integer.class, "int");
    public final int deoptimizationUnrollBlockFrameSizesOffset = getFieldOffset("Deoptimization::UnrollBlock::_frame_sizes", Integer.class, "intptr_t*");
    public final int deoptimizationUnrollBlockFramePcsOffset = getFieldOffset("Deoptimization::UnrollBlock::_frame_pcs", Integer.class, "address*");
    public final int deoptimizationUnrollBlockInitialInfoOffset = getFieldOffset("Deoptimization::UnrollBlock::_initial_info", Integer.class, "intptr_t");

    public final boolean deoptimizationSupportLargeAccessByteArrayVirtualization = getConstant("Deoptimization::_support_large_access_byte_array_virtualization", Boolean.class, false);

    // Checkstyle: stop
    public final int MARKID_VERIFIED_ENTRY = getConstant("CodeInstaller::VERIFIED_ENTRY", Integer.class);
    public final int MARKID_UNVERIFIED_ENTRY = getConstant("CodeInstaller::UNVERIFIED_ENTRY", Integer.class);
    public final int MARKID_OSR_ENTRY = getConstant("CodeInstaller::OSR_ENTRY", Integer.class);
    public final int MARKID_EXCEPTION_HANDLER_ENTRY = getConstant("CodeInstaller::EXCEPTION_HANDLER_ENTRY", Integer.class);
    public final int MARKID_DEOPT_HANDLER_ENTRY = getConstant("CodeInstaller::DEOPT_HANDLER_ENTRY", Integer.class);
    public final int MARKID_INVOKEINTERFACE = getConstant("CodeInstaller::INVOKEINTERFACE", Integer.class);
    public final int MARKID_INVOKEVIRTUAL = getConstant("CodeInstaller::INVOKEVIRTUAL", Integer.class);
    public final int MARKID_INVOKESTATIC = getConstant("CodeInstaller::INVOKESTATIC", Integer.class);
    public final int MARKID_INVOKESPECIAL = getConstant("CodeInstaller::INVOKESPECIAL", Integer.class);
    public final int MARKID_INLINE_INVOKE = getConstant("CodeInstaller::INLINE_INVOKE", Integer.class);
    public final int MARKID_POLL_NEAR = getConstant("CodeInstaller::POLL_NEAR", Integer.class);
    public final int MARKID_POLL_RETURN_NEAR = getConstant("CodeInstaller::POLL_RETURN_NEAR", Integer.class);
    public final int MARKID_POLL_FAR = getConstant("CodeInstaller::POLL_FAR", Integer.class);
    public final int MARKID_POLL_RETURN_FAR = getConstant("CodeInstaller::POLL_RETURN_FAR", Integer.class);
    public final int MARKID_CARD_TABLE_SHIFT = getConstant("CodeInstaller::CARD_TABLE_SHIFT", Integer.class);
    public final int MARKID_CARD_TABLE_ADDRESS = getConstant("CodeInstaller::CARD_TABLE_ADDRESS", Integer.class);
    public final int MARKID_INVOKE_INVALID = getConstant("CodeInstaller::INVOKE_INVALID", Integer.class);

    /**
     * The following constants are given default values here since they are missing in the native
     * JVMCI-8 code but are still required for {@link GraalHotSpotVMConfigNode#canonical} to work in
     * a JDK8 environment.
     */
    public final int MARKID_HEAP_TOP_ADDRESS = getConstant("CodeInstaller::HEAP_TOP_ADDRESS", Integer.class, 17);
    public final int MARKID_HEAP_END_ADDRESS = getConstant("CodeInstaller::HEAP_END_ADDRESS", Integer.class, 18);
    public final int MARKID_NARROW_KLASS_BASE_ADDRESS = getConstant("CodeInstaller::NARROW_KLASS_BASE_ADDRESS", Integer.class, 19);
    public final int MARKID_NARROW_OOP_BASE_ADDRESS = getConstant("CodeInstaller::NARROW_OOP_BASE_ADDRESS", Integer.class, 20);
    public final int MARKID_CRC_TABLE_ADDRESS = getConstant("CodeInstaller::CRC_TABLE_ADDRESS", Integer.class, 21);
    public final int MARKID_LOG_OF_HEAP_REGION_GRAIN_BYTES = getConstant("CodeInstaller::LOG_OF_HEAP_REGION_GRAIN_BYTES", Integer.class, 22);
    public final int MARKID_INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED = getConstant("CodeInstaller::INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED", Integer.class, 23);

    // Checkstyle: resume

    protected boolean check() {
        for (Field f : getClass().getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                assert Modifier.isFinal(modifiers) : "field should be final: " + f;
            }
        }

        assert codeEntryAlignment > 0 : codeEntryAlignment;
        assert checkNullAllocationStubs();
        return true;
    }
}
