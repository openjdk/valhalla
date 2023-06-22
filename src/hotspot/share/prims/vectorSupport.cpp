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

bool VectorSupport::is_vector_mask(Klass* klass) {
  return klass->is_subclass_of(vmClasses::vector_VectorMask_klass());
}

bool VectorSupport::is_vector_shuffle(Klass* klass) {
  return klass->is_subclass_of(vmClasses::vector_VectorShuffle_klass());
}

bool VectorSupport::skip_value_scalarization(Klass* klass) {
  return VectorSupport::is_vector(klass) ||
         VectorSupport::is_vector_payload_mf(klass);
}

BasicType VectorSupport::klass2bt(InstanceKlass* ik) {
  assert(ik->is_subclass_of(vmClasses::vector_VectorPayload_klass()), "%s not a VectorPayload", ik->name()->as_C_string());
  fieldDescriptor fd; // find_field initializes fd if found
  // static final Class<?> ETYPE;
  Klass* holder = ik->find_field(vmSymbols::ETYPE_name(), vmSymbols::class_signature(), &fd);

  assert(holder != nullptr, "sanity");
  assert(fd.is_static(), "");
  assert(fd.offset() > 0, "");

  if (is_vector_mask(ik)) {
    return T_BOOLEAN;
  } else { // vector and mask
    oop value = ik->java_mirror()->obj_field(fd.offset());
    BasicType elem_bt = java_lang_Class::as_BasicType(value);
    return elem_bt;
  }
}

jint VectorSupport::klass2length(InstanceKlass* ik) {
  fieldDescriptor fd; // find_field initializes fd if found
  // static final int VLENGTH;
  Klass* holder = ik->find_field(vmSymbols::VLENGTH_name(), vmSymbols::int_signature(), &fd);

  assert(holder != nullptr, "sanity");
  assert(fd.is_static(), "");
  assert(fd.offset() > 0, "");

  jint vlen = ik->java_mirror()->int_field(fd.offset());
  assert(vlen > 0, "");
  return vlen;
}

Handle VectorSupport::allocate_vector_payload_helper(InstanceKlass* ik, int num_elem, BasicType elem_bt, int larval, TRAPS) {
  // On-heap vector values are represented as primitive class instances with a multi-field payload.
  InstanceKlass* payload_kls = get_vector_payload_klass(elem_bt, num_elem);
  assert(payload_kls->is_inline_klass(), "");
  instanceOop obj = InlineKlass::cast(payload_kls)->allocate_instance(THREAD);
  if (larval) obj->set_mark(obj->mark().enter_larval_state());

  fieldDescriptor fd;
  Klass* def = payload_kls->find_field(vmSymbols::mfield_name(), vmSymbols::type_signature(elem_bt), false, &fd);
  assert(fd.is_multifield_base() && fd.secondary_fields_count(fd.index()) == num_elem, "");
  return Handle(THREAD, obj);
}

Symbol* VectorSupport::get_vector_payload_field_signature(BasicType elem_bt, int num_elem) {
  switch(elem_bt) {
    case T_BOOLEAN:
      switch(num_elem) {
        case  1: return vmSymbols::vector_VectorPayloadMF8Z_signature();
        case  2: return vmSymbols::vector_VectorPayloadMF16Z_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF32Z_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF64Z_signature();
        case 16: return vmSymbols::vector_VectorPayloadMF128Z_signature();
        case 32: return vmSymbols::vector_VectorPayloadMF256Z_signature();
        case 64: return vmSymbols::vector_VectorPayloadMF512Z_signature();
        default: ShouldNotReachHere();
      } break;
    case T_BYTE:
      switch(num_elem) {
        case  1: return vmSymbols::vector_VectorPayloadMF8B_signature();
        case  2: return vmSymbols::vector_VectorPayloadMF16B_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF32B_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF64B_signature();
        case 16: return vmSymbols::vector_VectorPayloadMF128B_signature();
        case 32: return vmSymbols::vector_VectorPayloadMF256B_signature();
        case 64: return vmSymbols::vector_VectorPayloadMF512B_signature();
        default: ShouldNotReachHere();
      } break;
    case T_SHORT:
      switch(num_elem) {
        case  4: return vmSymbols::vector_VectorPayloadMF64S_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF128S_signature();
        case 16: return vmSymbols::vector_VectorPayloadMF256S_signature();
        case 32: return vmSymbols::vector_VectorPayloadMF512S_signature();
        default: ShouldNotReachHere();
      } break;
    case T_INT:
      switch(num_elem) {
        case  2: return vmSymbols::vector_VectorPayloadMF64I_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF128I_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF256I_signature();
        case 16: return vmSymbols::vector_VectorPayloadMF512I_signature();
        default: ShouldNotReachHere();
      } break;
    case T_LONG:
      switch(num_elem) {
        case  1: return vmSymbols::vector_VectorPayloadMF64L_signature();
        case  2: return vmSymbols::vector_VectorPayloadMF128L_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF256L_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF512L_signature();
        default: ShouldNotReachHere();
      } break;
    case T_FLOAT:
      switch(num_elem) {
        case  2: return vmSymbols::vector_VectorPayloadMF64F_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF128F_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF256F_signature();
        case 16: return vmSymbols::vector_VectorPayloadMF512F_signature();
        default: ShouldNotReachHere();
      } break;
    case T_DOUBLE:
      switch(num_elem) {
        case  1: return vmSymbols::vector_VectorPayloadMF64D_signature();
        case  2: return vmSymbols::vector_VectorPayloadMF128D_signature();
        case  4: return vmSymbols::vector_VectorPayloadMF256D_signature();
        case  8: return vmSymbols::vector_VectorPayloadMF512D_signature();
        default: ShouldNotReachHere();
      } break;
     default:
        ShouldNotReachHere();
  }
  return NULL;
}

InstanceKlass* VectorSupport::get_vector_payload_klass(BasicType elem_bt, int num_elem) {
  switch(elem_bt) {
    case T_BOOLEAN:
      switch(num_elem) {
        case  1: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF8Z_klass));
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF16Z_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF32Z_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64Z_klass));
        case 16: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128Z_klass));
        case 32: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256Z_klass));
        case 64: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512Z_klass));
        default: ShouldNotReachHere();
      } break;
    case T_BYTE:
      switch(num_elem) {
        case  1: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF8B_klass));
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF16B_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF32B_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64B_klass));
        case 16: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128B_klass));
        case 32: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256B_klass));
        case 64: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512B_klass));
        default: ShouldNotReachHere();
      } break;
    case T_SHORT:
      switch(num_elem) {
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64S_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128S_klass));
        case 16: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256S_klass));
        case 32: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512S_klass));
        default: ShouldNotReachHere();
      } break;
    case T_INT:
      switch(num_elem) {
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64I_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128I_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256I_klass));
        case 16: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512I_klass));
        default: ShouldNotReachHere();
      } break;
    case T_LONG:
      switch(num_elem) {
        case  1: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64L_klass));
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128L_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256L_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512L_klass));
        default: ShouldNotReachHere();
      } break;
    case T_FLOAT:
      switch(num_elem) {
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64F_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128F_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256F_klass));
        case 16: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512F_klass));
        default: ShouldNotReachHere();
      } break;
    case T_DOUBLE:
      switch(num_elem) {
        case  1: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF64D_klass));
        case  2: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF128D_klass));
        case  4: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF256D_klass));
        case  8: return vmClasses::klass_at(VM_CLASS_ID(vector_VectorPayloadMF512D_klass));
        default: ShouldNotReachHere();
      } break;
    default:
        ShouldNotReachHere();
  }
  return NULL;
}

Handle VectorSupport::allocate_vector_payload(InstanceKlass* ik, int num_elem, BasicType elem_bt, frame* fr, RegisterMap* reg_map, ObjectValue* ov, TRAPS) {
  ScopeValue* payload = ov->field_at(0);
  intptr_t is_larval = StackValue::create_stack_value(fr, reg_map, ov->is_larval())->get_int();
  jint larval = (jint)*((jint*)&is_larval);

  if (payload->is_location()) {
    // Vector payload value in an aligned adjacent tuple (8, 16, 32 or 64 bytes).
    return allocate_vector_payload_helper(ik, num_elem, elem_bt, larval, THREAD); // safepoint
  } else if (!payload->is_object() && !payload->is_constant_oop()) {
    stringStream ss;
    payload->print_on(&ss);
    assert(false, "expected 'object' value for scalar-replaced boxed vector but got: %s", ss.freeze());
  }
  return Handle(THREAD, nullptr);
}

instanceOop VectorSupport::allocate_vector_payload(InstanceKlass* ik, frame* fr, RegisterMap* reg_map, ObjectValue* ov, TRAPS) {
  assert(is_vector_payload_mf(ik), "%s not a vector payload", ik->name()->as_C_string());
  assert(ik->is_inline_klass(), "");

  int num_elem = 0;
  BasicType elem_bt = T_ILLEGAL;
  for (JavaFieldStream fs(ik); !fs.done(); fs.next()) {
    fieldDescriptor& fd = fs.field_descriptor();
    if (fd.is_multifield_base()) {
      elem_bt = fd.field_type();
      num_elem = fd.secondary_fields_count(fd.index());
      break;
    }
  }
  assert(num_elem != 0, "");
  Handle payload_instance = VectorSupport::allocate_vector_payload(ik, num_elem, elem_bt, fr, reg_map, ov, CHECK_NULL);
  return (instanceOop)payload_instance();
}

instanceOop VectorSupport::allocate_vector(InstanceKlass* ik, frame* fr, RegisterMap* reg_map, ObjectValue* ov, TRAPS) {
  assert(is_vector(ik), "%s not a vector", ik->name()->as_C_string());
  assert(ik->is_inline_klass(), "");

  int num_elem = klass2length(ik);
  BasicType elem_bt = klass2bt(ik);
  Handle payload_instance = VectorSupport::allocate_vector_payload(ik, num_elem, elem_bt, fr, reg_map, ov, CHECK_NULL);

  InstanceKlass* payload_class = InstanceKlass::cast(payload_instance()->klass());
  Deoptimization::reassign_fields_by_klass(payload_class, fr, reg_map, ov, 0, payload_instance(), true, 0, CHECK_NULL);

  instanceOop vbox = ik->allocate_instance(THREAD);
  Handle vbox_h = Handle(THREAD, vbox);

  fieldDescriptor fd;
  Symbol* payload_sig = VectorSupport::get_vector_payload_field_signature(elem_bt, num_elem);
  Klass* def = ik->find_field(vmSymbols::payload_name(), payload_sig, false, &fd);
  assert(def != NULL, "");

  if (fd.is_inlined()) {
    InlineKlass* field_ik = InlineKlass::cast(ik->get_inline_type_field_klass(fd.index()));
    field_ik->write_inlined_field(vbox_h(), fd.offset(), payload_instance(), THREAD);
  } else {
    vbox_h()->obj_field_put(fd.offset(), payload_instance());
  }
  return vbox;
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
