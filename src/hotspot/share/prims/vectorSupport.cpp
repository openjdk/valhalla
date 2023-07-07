/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmClassMacros.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/location.hpp"
#include "jni.h"
#include "jvm.h"
#include "oops/fieldStreams.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/vectorSupport.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/stackValue.hpp"
#include "runtime/deoptimization.hpp"
#include "utilities/debug.hpp"
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif // COMPILER2

#ifdef COMPILER2
const char* VectorSupport::svmlname[VectorSupport::NUM_SVML_OP] = {
    "tan",
    "tanh",
    "sin",
    "sinh",
    "cos",
    "cosh",
    "asin",
    "acos",
    "atan",
    "atan2",
    "cbrt",
    "log",
    "log10",
    "log1p",
    "pow",
    "exp",
    "expm1",
    "hypot",
};
#endif

bool VectorSupport::is_vector(Klass* klass) {
  return klass->is_subclass_of(vmClasses::vector_VectorPayload_klass());
}

bool VectorSupport::is_vector_payload_mf(Klass* klass) {
  return klass->is_subclass_of(vmClasses::vector_VectorPayloadMF_klass());
}

bool VectorSupport::skip_value_scalarization(Klass* klass) {
  return VectorSupport::is_vector(klass) ||
         VectorSupport::is_vector_payload_mf(klass);
}

#ifdef COMPILER2
int VectorSupport::vop2ideal(jint id, BasicType bt) {
  VectorOperation vop = (VectorOperation)id;
  switch (vop) {
    case VECTOR_OP_ADD: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_AddI;
        case T_LONG:   return Op_AddL;
        case T_FLOAT:  return Op_AddF;
        case T_DOUBLE: return Op_AddD;
        default: fatal("ADD: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_SUB: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_SubI;
        case T_LONG:   return Op_SubL;
        case T_FLOAT:  return Op_SubF;
        case T_DOUBLE: return Op_SubD;
        default: fatal("SUB: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MUL: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_MulI;
        case T_LONG:   return Op_MulL;
        case T_FLOAT:  return Op_MulF;
        case T_DOUBLE: return Op_MulD;
        default: fatal("MUL: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_DIV: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_DivI;
        case T_LONG:   return Op_DivL;
        case T_FLOAT:  return Op_DivF;
        case T_DOUBLE: return Op_DivD;
        default: fatal("DIV: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MIN: {
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:    return Op_MinI;
        case T_LONG:   return Op_MinL;
        case T_FLOAT:  return Op_MinF;
        case T_DOUBLE: return Op_MinD;
        default: fatal("MIN: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MAX: {
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:    return Op_MaxI;
        case T_LONG:   return Op_MaxL;
        case T_FLOAT:  return Op_MaxF;
        case T_DOUBLE: return Op_MaxD;
        default: fatal("MAX: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_ABS: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_AbsI;
        case T_LONG:   return Op_AbsL;
        case T_FLOAT:  return Op_AbsF;
        case T_DOUBLE: return Op_AbsD;
        default: fatal("ABS: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_NEG: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_NegI;
        case T_LONG:   return Op_NegL;
        case T_FLOAT:  return Op_NegF;
        case T_DOUBLE: return Op_NegD;
        default: fatal("NEG: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_AND: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_AndI;
        case T_LONG:   return Op_AndL;
        default: fatal("AND: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_OR: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_OrI;
        case T_LONG:   return Op_OrL;
        default: fatal("OR: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_XOR: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    return Op_XorI;
        case T_LONG:   return Op_XorL;
        default: fatal("XOR: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_SQRT: {
      switch (bt) {
        case T_FLOAT:  return Op_SqrtF;
        case T_DOUBLE: return Op_SqrtD;
        default: fatal("SQRT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_FMA: {
      switch (bt) {
        case T_FLOAT:  return Op_FmaF;
        case T_DOUBLE: return Op_FmaD;
        default: fatal("FMA: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_LSHIFT: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:  return Op_LShiftI;
        case T_LONG: return Op_LShiftL;
        default: fatal("LSHIFT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_RSHIFT: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:  return Op_RShiftI;
        case T_LONG: return Op_RShiftL;
        default: fatal("RSHIFT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_URSHIFT: {
      switch (bt) {
        case T_BYTE:  return Op_URShiftB;
        case T_SHORT: return Op_URShiftS;
        case T_INT:   return Op_URShiftI;
        case T_LONG:  return Op_URShiftL;
        default: fatal("URSHIFT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_LROTATE: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    // fall-through
        case T_LONG:  return Op_RotateLeft;
        default: fatal("LROTATE: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_RROTATE: {
      switch (bt) {
        case T_BYTE:   // fall-through
        case T_SHORT:  // fall-through
        case T_INT:    // fall-through
        case T_LONG:  return Op_RotateRight;
        default: fatal("RROTATE: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MASK_LASTTRUE: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_VectorMaskLastTrue;
        default: fatal("MASK_LASTTRUE: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MASK_FIRSTTRUE: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_VectorMaskFirstTrue;
        default: fatal("MASK_FIRSTTRUE: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MASK_TRUECOUNT: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_VectorMaskTrueCount;
        default: fatal("MASK_TRUECOUNT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MASK_TOLONG: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_VectorMaskToLong;
        default: fatal("MASK_TOLONG: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_EXPAND: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_ExpandV;
        default: fatal("EXPAND: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_COMPRESS: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_CompressV;
        default: fatal("COMPRESS: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_MASK_COMPRESS: {
      switch (bt) {
        case T_BYTE:  // fall-through
        case T_SHORT: // fall-through
        case T_INT:   // fall-through
        case T_LONG:  // fall-through
        case T_FLOAT: // fall-through
        case T_DOUBLE: return Op_CompressM;
        default: fatal("MASK_COMPRESS: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_BIT_COUNT: {
      switch (bt) {
        case T_BYTE:  // Returning Op_PopCountI
        case T_SHORT: // for byte and short types temporarily
        case T_INT:   return Op_PopCountI;
        case T_LONG:  return Op_PopCountL;
        default: fatal("BIT_COUNT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_TZ_COUNT: {
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:   return Op_CountTrailingZerosI;
        case T_LONG:  return Op_CountTrailingZerosL;
        default: fatal("TZ_COUNT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_LZ_COUNT: {
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:   return Op_CountLeadingZerosI;
        case T_LONG:  return Op_CountLeadingZerosL;
        default: fatal("LZ_COUNT: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_REVERSE: {
      switch (bt) {
        case T_BYTE:  // Temporarily returning
        case T_SHORT: // Op_ReverseI for byte and short
        case T_INT:   return Op_ReverseI;
        case T_LONG:  return Op_ReverseL;
        default: fatal("REVERSE: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_REVERSE_BYTES: {
      switch (bt) {
        case T_SHORT: return Op_ReverseBytesS;
        // Superword requires type consistency between the ReverseBytes*
        // node and the data. But there's no ReverseBytesB node because
        // no reverseBytes() method in Java Byte class. T_BYTE can only
        // appear in VectorAPI calls. We reuse Op_ReverseBytesI for this
        // to ensure vector intrinsification succeeds.
        case T_BYTE:  // Intentionally fall-through
        case T_INT:   return Op_ReverseBytesI;
        case T_LONG:  return Op_ReverseBytesL;
        default: fatal("REVERSE_BYTES: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_COMPRESS_BITS: {
      switch (bt) {
        case T_INT:
        case T_LONG: return Op_CompressBits;
        default: fatal("COMPRESS_BITS: %s", type2name(bt));
      }
      break;
    }
    case VECTOR_OP_EXPAND_BITS: {
      switch (bt) {
        case T_INT:
        case T_LONG: return Op_ExpandBits;
        default: fatal("EXPAND_BITS: %s", type2name(bt));
      }
      break;
    }

    case VECTOR_OP_TAN:
    case VECTOR_OP_TANH:
    case VECTOR_OP_SIN:
    case VECTOR_OP_SINH:
    case VECTOR_OP_COS:
    case VECTOR_OP_COSH:
    case VECTOR_OP_ASIN:
    case VECTOR_OP_ACOS:
    case VECTOR_OP_ATAN:
    case VECTOR_OP_ATAN2:
    case VECTOR_OP_CBRT:
    case VECTOR_OP_LOG:
    case VECTOR_OP_LOG10:
    case VECTOR_OP_LOG1P:
    case VECTOR_OP_POW:
    case VECTOR_OP_EXP:
    case VECTOR_OP_EXPM1:
    case VECTOR_OP_HYPOT:
      return Op_CallLeafVector;
    default: fatal("unknown op: %d", vop);
  }
  return 0; // Unimplemented
}
#endif // COMPILER2

/**
 * Implementation of the jdk.internal.vm.vector.VectorSupport class
 */

JVM_ENTRY(jint, VectorSupport_GetMaxLaneCount(JNIEnv *env, jclass vsclazz, jobject clazz)) {
#ifdef COMPILER2
  oop mirror = JNIHandles::resolve_non_null(clazz);
  if (java_lang_Class::is_primitive(mirror)) {
    BasicType bt = java_lang_Class::primitive_type(mirror);
    return Matcher::max_vector_size(bt);
  }
#endif // COMPILER2
  return -1;
} JVM_END

// JVM_RegisterVectorSupportMethods

#define LANG "Ljava/lang/"
#define CLS LANG "Class;"

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

static JNINativeMethod jdk_internal_vm_vector_VectorSupport_methods[] = {
    {CC "getMaxLaneCount",   CC "(" CLS ")I", FN_PTR(VectorSupport_GetMaxLaneCount)}
};

#undef CC
#undef FN_PTR

#undef LANG
#undef CLS

// This function is exported, used by NativeLookup.

JVM_ENTRY(void, JVM_RegisterVectorSupportMethods(JNIEnv* env, jclass vsclass)) {
  ThreadToNativeFromVM ttnfv(thread);

  int ok = env->RegisterNatives(vsclass, jdk_internal_vm_vector_VectorSupport_methods, sizeof(jdk_internal_vm_vector_VectorSupport_methods)/sizeof(JNINativeMethod));
  guarantee(ok == 0, "register jdk.internal.vm.vector.VectorSupport natives");
} JVM_END
