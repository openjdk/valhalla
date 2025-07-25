/*
 * Copyright (c) 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/connode.hpp"
#include "opto/graphKit.hpp"
#include "opto/inlinetypenode.hpp"
#include "opto/matcher.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"
#include "castnode.hpp"
#include "utilities/checkedCast.hpp"

//=============================================================================
// If input is already higher or equal to cast type, then this is an identity.
Node* ConstraintCastNode::Identity(PhaseGVN* phase) {
  if (_dependency == UnconditionalDependency) {
    return this;
  }
  Node* dom = dominating_cast(phase, phase);
  if (dom != nullptr) {
    return dom;
  }
  return higher_equal_types(phase, in(1)) ? in(1) : this;
}

//------------------------------Value------------------------------------------
// Take 'join' of input and cast-up type
const Type* ConstraintCastNode::Value(PhaseGVN* phase) const {
  if (in(0) && phase->type(in(0)) == Type::TOP) return Type::TOP;

  const Type* in_type = phase->type(in(1));
  const Type* ft = in_type->filter_speculative(_type);

  // Check if both _type and in_type had a speculative type, but for the just
  // computed ft the speculative type was dropped.
  if (ft->speculative() == nullptr &&
      _type->speculative() != nullptr &&
      in_type->speculative() != nullptr) {
    // Speculative type may have disagreed between cast and input, and was
    // dropped in filtering. Recompute so that ft can take speculative type
    // of in_type. If we did not do it now, a subsequent ::Value call would
    // do it, and violate idempotence of ::Value.
    ft = in_type->filter_speculative(ft);
  }

#ifdef ASSERT
  // Previous versions of this function had some special case logic,
  // which is no longer necessary.  Make sure of the required effects.
  switch (Opcode()) {
    case Op_CastII:
    {
      if (in_type == Type::TOP) {
        assert(ft == Type::TOP, "special case #1");
      }
      const Type* rt = in_type->join_speculative(_type);
      if (rt->empty()) {
        assert(ft == Type::TOP, "special case #2");
      }
      break;
    }
    case Op_CastPP:
    if (in_type == TypePtr::NULL_PTR &&
        _type->isa_ptr() && _type->is_ptr()->_ptr == TypePtr::NotNull) {
      assert(ft == Type::TOP, "special case #3");
      break;
    }
  }
#endif //ASSERT

  return ft;
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node *ConstraintCastNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if (in(0) != nullptr && remove_dead_region(phase, can_reshape)) {
    return this;
  }

  // Push cast through InlineTypeNode
  InlineTypeNode* vt = in(1)->isa_InlineType();
  if (vt != nullptr && phase->type(vt)->filter_speculative(_type) != Type::TOP) {
    Node* cast = clone();
    cast->set_req(1, vt->get_oop());
    vt = vt->clone()->as_InlineType();
    if (!_type->maybe_null()) {
      vt->as_InlineType()->set_null_marker(*phase);
    }
    vt->set_oop(*phase, phase->transform(cast));
    return vt;
  }

  if (in(1) != nullptr && phase->type(in(1)) != Type::TOP) {
    return TypeNode::Ideal(phase, can_reshape);
  }

  return nullptr;
}

uint ConstraintCastNode::hash() const {
  return TypeNode::hash() + (int)_dependency + (_extra_types != nullptr ? _extra_types->hash() : 0);
}

bool ConstraintCastNode::cmp(const Node &n) const {
  if (!TypeNode::cmp(n)) {
    return false;
  }
  ConstraintCastNode& cast = (ConstraintCastNode&) n;
  if (cast._dependency != _dependency) {
    return false;
  }
  if (_extra_types == nullptr || cast._extra_types == nullptr) {
    return _extra_types == cast._extra_types;
  }
  return _extra_types->eq(cast._extra_types);
}

uint ConstraintCastNode::size_of() const {
  return sizeof(*this);
}

Node* ConstraintCastNode::make_cast_for_basic_type(Node* c, Node* n, const Type* t, DependencyType dependency, BasicType bt) {
  switch(bt) {
  case T_INT:
    return new CastIINode(c, n, t, dependency);
  case T_LONG:
    return new CastLLNode(c, n, t, dependency);
  default:
    fatal("Bad basic type %s", type2name(bt));
  }
  return nullptr;
}

TypeNode* ConstraintCastNode::dominating_cast(PhaseGVN* gvn, PhaseTransform* pt) const {
  if (_dependency == UnconditionalDependency) {
    return nullptr;
  }
  Node* val = in(1);
  Node* ctl = in(0);
  int opc = Opcode();
  if (ctl == nullptr) {
    return nullptr;
  }
  // Range check CastIIs may all end up under a single range check and
  // in that case only the narrower CastII would be kept by the code
  // below which would be incorrect.
  if (is_CastII() && as_CastII()->has_range_check()) {
    return nullptr;
  }
  if (type()->isa_rawptr() && (gvn->type_or_null(val) == nullptr || gvn->type(val)->isa_oopptr())) {
    return nullptr;
  }
  for (DUIterator_Fast imax, i = val->fast_outs(imax); i < imax; i++) {
    Node* u = val->fast_out(i);
    if (u != this &&
        u->outcnt() > 0 &&
        u->Opcode() == opc &&
        u->in(0) != nullptr &&
        higher_equal_types(gvn, u)) {
      if (pt->is_dominator(u->in(0), ctl)) {
        return u->as_Type();
      }
      if (is_CheckCastPP() && u->in(1)->is_Proj() && u->in(1)->in(0)->is_Allocate() &&
          u->in(0)->is_Proj() && u->in(0)->in(0)->is_Initialize() &&
          u->in(1)->in(0)->as_Allocate()->initialization() == u->in(0)->in(0)) {
        // CheckCastPP following an allocation always dominates all
        // use of the allocation result
        return u->as_Type();
      }
    }
  }
  return nullptr;
}

bool ConstraintCastNode::higher_equal_types(PhaseGVN* phase, const Node* other) const {
  const Type* t = phase->type(other);
  if (!t->higher_equal_speculative(type())) {
    return false;
  }
  if (_extra_types != nullptr) {
    for (uint i = 0; i < _extra_types->cnt(); ++i) {
      if (!t->higher_equal_speculative(_extra_types->field_at(i))) {
        return false;
      }
    }
  }
  return true;
}

#ifndef PRODUCT
void ConstraintCastNode::dump_spec(outputStream *st) const {
  TypeNode::dump_spec(st);
  if (_extra_types != nullptr) {
    st->print(" extra types: ");
    _extra_types->dump_on(st);
  }
  if (_dependency != RegularDependency) {
    st->print(" %s dependency", _dependency == StrongDependency ? "strong" : "unconditional");
  }
}
#endif

const Type* CastIINode::Value(PhaseGVN* phase) const {
  const Type *res = ConstraintCastNode::Value(phase);
  if (res == Type::TOP) {
    return Type::TOP;
  }
  assert(res->isa_int(), "res must be int");

  // Similar to ConvI2LNode::Value() for the same reasons
  // see if we can remove type assertion after loop opts
  res = widen_type(phase, res, T_INT);

  return res;
}

Node* ConstraintCastNode::find_or_make_integer_cast(PhaseIterGVN* igvn, Node* parent, const TypeInteger* type) const {
  Node* n = clone();
  n->set_req(1, parent);
  n->as_ConstraintCast()->set_type(type);
  Node* existing = igvn->hash_find_insert(n);
  if (existing != nullptr) {
    n->destruct(igvn);
    return existing;
  }
  return igvn->register_new_node_with_optimizer(n);
}

Node *CastIINode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node* progress = ConstraintCastNode::Ideal(phase, can_reshape);
  if (progress != nullptr) {
    return progress;
  }
  if (can_reshape && !phase->C->post_loop_opts_phase()) {
    // makes sure we run ::Value to potentially remove type assertion after loop opts
    phase->C->record_for_post_loop_opts_igvn(this);
  }
  if (!_range_check_dependency || phase->C->post_loop_opts_phase()) {
    return optimize_integer_cast(phase, T_INT);
  }
  phase->C->record_for_post_loop_opts_igvn(this);
  return nullptr;
}

Node* CastIINode::Identity(PhaseGVN* phase) {
  Node* progress = ConstraintCastNode::Identity(phase);
  if (progress != this) {
    return progress;
  }
  return this;
}

bool CastIINode::cmp(const Node &n) const {
  return ConstraintCastNode::cmp(n) && ((CastIINode&)n)._range_check_dependency == _range_check_dependency;
}

uint CastIINode::size_of() const {
  return sizeof(*this);
}

#ifndef PRODUCT
void CastIINode::dump_spec(outputStream* st) const {
  ConstraintCastNode::dump_spec(st);
  if (_range_check_dependency) {
    st->print(" range check dependency");
  }
}
#endif

CastIINode* CastIINode::pin_array_access_node() const {
  assert(_dependency == RegularDependency, "already pinned");
  if (has_range_check()) {
    return new CastIINode(in(0), in(1), bottom_type(), StrongDependency, has_range_check());
  }
  return nullptr;
}

void CastIINode::remove_range_check_cast(Compile* C) {
  if (has_range_check()) {
    // Range check CastII nodes feed into an address computation subgraph. Remove them to let that subgraph float freely.
    // For memory access or integer divisions nodes that depend on the cast, record the dependency on the cast's control
    // as a precedence edge, so they can't float above the cast in case that cast's narrowed type helped eliminate a
    // range check or a null divisor check.
    assert(in(0) != nullptr, "All RangeCheck CastII must have a control dependency");
    ResourceMark rm;
    Unique_Node_List wq;
    wq.push(this);
    for (uint next = 0; next < wq.size(); ++next) {
      Node* m = wq.at(next);
      for (DUIterator_Fast imax, i = m->fast_outs(imax); i < imax; i++) {
        Node* use = m->fast_out(i);
        if (use->is_Mem() || use->is_div_or_mod(T_INT) || use->is_div_or_mod(T_LONG)) {
          use->ensure_control_or_add_prec(in(0));
        } else if (!use->is_CFG() && !use->is_Phi()) {
          wq.push(use);
        }
      }
    }
    subsume_by(in(1), C);
    if (outcnt() == 0) {
      disconnect_inputs(C);
    }
  }
}


const Type* CastLLNode::Value(PhaseGVN* phase) const {
  const Type* res = ConstraintCastNode::Value(phase);
  if (res == Type::TOP) {
    return Type::TOP;
  }
  assert(res->isa_long(), "res must be long");

  return widen_type(phase, res, T_LONG);
}

Node* CastLLNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* progress = ConstraintCastNode::Ideal(phase, can_reshape);
  if (progress != nullptr) {
    return progress;
  }
  if (!phase->C->post_loop_opts_phase()) {
    // makes sure we run ::Value to potentially remove type assertion after loop opts
    phase->C->record_for_post_loop_opts_igvn(this);
  }
  // transform (CastLL (ConvI2L ..)) into (ConvI2L (CastII ..)) if the type of the CastLL is narrower than the type of
  // the ConvI2L.
  Node* in1 = in(1);
  if (in1 != nullptr && in1->Opcode() == Op_ConvI2L) {
    const Type* t = Value(phase);
    const Type* t_in = phase->type(in1);
    if (t != Type::TOP && t_in != Type::TOP) {
      const TypeLong* tl = t->is_long();
      const TypeLong* t_in_l = t_in->is_long();
      assert(tl->_lo >= t_in_l->_lo && tl->_hi <= t_in_l->_hi, "CastLL type should be narrower than or equal to the type of its input");
      assert((tl != t_in_l) == (tl->_lo > t_in_l->_lo || tl->_hi < t_in_l->_hi), "if type differs then this nodes's type must be narrower");
      if (tl != t_in_l) {
        const TypeInt* ti = TypeInt::make(checked_cast<jint>(tl->_lo), checked_cast<jint>(tl->_hi), tl->_widen);
        Node* castii = phase->transform(new CastIINode(in(0), in1->in(1), ti));
        Node* convi2l = in1->clone();
        convi2l->set_req(1, castii);
        return convi2l;
      }
    }
  }
  return optimize_integer_cast(phase, T_LONG);
}

//=============================================================================
//------------------------------Identity---------------------------------------
// If input is already higher or equal to cast type, then this is an identity.
Node* CheckCastPPNode::Identity(PhaseGVN* phase) {
  if (in(1)->is_InlineType() && _type->isa_instptr() && phase->type(in(1))->inline_klass()->is_subtype_of(_type->is_instptr()->instance_klass())) {
    return in(1);
  }
  return ConstraintCastNode::Identity(phase);
}

//------------------------------Value------------------------------------------
// Take 'join' of input and cast-up type, unless working with an Interface
const Type* CheckCastPPNode::Value(PhaseGVN* phase) const {
  if( in(0) && phase->type(in(0)) == Type::TOP ) return Type::TOP;

  const Type *inn = phase->type(in(1));
  if( inn == Type::TOP ) return Type::TOP;  // No information yet

  if (inn->isa_oopptr() && _type->isa_oopptr()) {
    return ConstraintCastNode::Value(phase);
  }

  const TypePtr *in_type = inn->isa_ptr();
  const TypePtr *my_type = _type->isa_ptr();
  const Type *result = _type;
  if (in_type != nullptr && my_type != nullptr) {
    // TODO 8302672
    if (!StressReflectiveCode && my_type->isa_aryptr() && in_type->isa_aryptr()) {
      // Propagate array properties (not flat/null-free)
      // Don't do this when StressReflectiveCode is enabled because it might lead to
      // a dying data path while the corresponding flat/null-free check is not folded.
      my_type = my_type->is_aryptr()->update_properties(in_type->is_aryptr());
      if (my_type == nullptr) {
        return Type::TOP; // Inconsistent properties
      }
    }
    TypePtr::PTR in_ptr = in_type->ptr();
    if (in_ptr == TypePtr::Null) {
      result = in_type;
    } else if (in_ptr != TypePtr::Constant) {
      result = my_type->cast_to_ptr_type(my_type->join_ptr(in_ptr));
    }
  }

  return result;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* CastX2PNode::Value(PhaseGVN* phase) const {
  const Type* t = phase->type(in(1));
  if (t == Type::TOP) return Type::TOP;
  if (t->base() == Type_X && t->singleton()) {
    uintptr_t bits = (uintptr_t) t->is_intptr_t()->get_con();
    if (bits == 0)   return TypePtr::NULL_PTR;
    return TypeRawPtr::make((address) bits);
  }
  return CastX2PNode::bottom_type();
}

//------------------------------Idealize---------------------------------------
static inline bool fits_in_int(const Type* t, bool but_not_min_int = false) {
  if (t == Type::TOP)  return false;
  const TypeX* tl = t->is_intptr_t();
  jint lo = min_jint;
  jint hi = max_jint;
  if (but_not_min_int)  ++lo;  // caller wants to negate the value w/o overflow
  return (tl->_lo >= lo) && (tl->_hi <= hi);
}

static inline Node* addP_of_X2P(PhaseGVN *phase,
                                Node* base,
                                Node* dispX,
                                bool negate = false) {
  if (negate) {
    dispX = phase->transform(new SubXNode(phase->MakeConX(0), dispX));
  }
  return new AddPNode(phase->C->top(),
                      phase->transform(new CastX2PNode(base)),
                      dispX);
}

Node *CastX2PNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // convert CastX2P(AddX(x, y)) to AddP(CastX2P(x), y) if y fits in an int
  int op = in(1)->Opcode();
  Node* x;
  Node* y;
  switch (op) {
    case Op_SubX:
    x = in(1)->in(1);
    // Avoid ideal transformations ping-pong between this and AddP for raw pointers.
    if (phase->find_intptr_t_con(x, -1) == 0)
    break;
    y = in(1)->in(2);
    if (fits_in_int(phase->type(y), true)) {
      return addP_of_X2P(phase, x, y, true);
    }
    break;
    case Op_AddX:
    x = in(1)->in(1);
    y = in(1)->in(2);
    if (fits_in_int(phase->type(y))) {
      return addP_of_X2P(phase, x, y);
    }
    if (fits_in_int(phase->type(x))) {
      return addP_of_X2P(phase, y, x);
    }
    break;
  }
  return nullptr;
}

//------------------------------Identity---------------------------------------
Node* CastX2PNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_CastP2X)  return in(1)->in(1);
  return this;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* CastP2XNode::Value(PhaseGVN* phase) const {
  const Type* t = phase->type(in(1));
  if (t == Type::TOP) return Type::TOP;
  if (t->base() == Type::RawPtr && t->singleton()) {
    uintptr_t bits = (uintptr_t) t->is_rawptr()->get_con();
    return TypeX::make(bits);
  }

  if (t->is_zero_type() || !t->maybe_null()) {
    for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
      Node* u = fast_out(i);
      if (u->Opcode() == Op_OrL) {
        for (DUIterator_Fast jmax, j = u->fast_outs(jmax); j < jmax; j++) {
          Node* cmp = u->fast_out(j);
          if (cmp->Opcode() == Op_CmpL) {
            // Give CmpL a chance to get optimized
            phase->record_for_igvn(cmp);
          }
        }
      }
    }
  }

  return CastP2XNode::bottom_type();
}

Node *CastP2XNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  return (in(0) && remove_dead_region(phase, can_reshape)) ? this : nullptr;
}

//------------------------------Identity---------------------------------------
Node* CastP2XNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_CastX2P)  return in(1)->in(1);
  return this;
}

Node* ConstraintCastNode::make_cast_for_type(Node* c, Node* in, const Type* type, DependencyType dependency,
                                             const TypeTuple* types) {
  if (type->isa_int()) {
    return new CastIINode(c, in, type, dependency, false, types);
  } else if (type->isa_long()) {
    return new CastLLNode(c, in, type, dependency, types);
  } else if (type->isa_half_float()) {
    return new CastHHNode(c, in, type, dependency, types);
  } else if (type->isa_float()) {
    return new CastFFNode(c, in, type, dependency, types);
  } else if (type->isa_double()) {
    return new CastDDNode(c, in, type, dependency, types);
  } else if (type->isa_vect()) {
    return new CastVVNode(c, in, type, dependency, types);
  } else if (type->isa_ptr()) {
    return new CastPPNode(c, in, type, dependency, types);
  }
  fatal("unreachable. Invalid cast type.");
  return nullptr;
}

Node* ConstraintCastNode::optimize_integer_cast(PhaseGVN* phase, BasicType bt) {
  PhaseIterGVN *igvn = phase->is_IterGVN();
  const TypeInteger* this_type = this->type()->is_integer(bt);
  Node* z = in(1);
  const TypeInteger* rx = nullptr;
  const TypeInteger* ry = nullptr;
  // Similar to ConvI2LNode::Ideal() for the same reasons
  if (Compile::push_thru_add(phase, z, this_type, rx, ry, bt, bt)) {
    if (igvn == nullptr) {
      // Postpone this optimization to iterative GVN, where we can handle deep
      // AddI chains without an exponential number of recursive Ideal() calls.
      phase->record_for_igvn(this);
      return nullptr;
    }
    int op = z->Opcode();
    Node* x = z->in(1);
    Node* y = z->in(2);

    Node* cx = find_or_make_integer_cast(igvn, x, rx);
    Node* cy = find_or_make_integer_cast(igvn, y, ry);
    if (op == Op_Add(bt)) {
      return AddNode::make(cx, cy, bt);
    } else {
      assert(op == Op_Sub(bt), "");
      return SubNode::make(cx, cy, bt);
    }
    return nullptr;
  }
  return nullptr;
}

const Type* ConstraintCastNode::widen_type(const PhaseGVN* phase, const Type* res, BasicType bt) const {
  if (!phase->C->post_loop_opts_phase()) {
    return res;
  }

  // At VerifyConstraintCasts == 1, we verify the ConstraintCastNodes that are present during code
  // emission. This allows us detecting possible mis-scheduling due to these nodes being pinned at
  // the wrong control nodes.
  // At VerifyConstraintCasts == 2, we do not perform widening so that we can verify the
  // correctness of more ConstraintCastNodes. This further helps us detect possible
  // mis-transformations that may happen due to these nodes being pinned at the wrong control
  // nodes.
  if (VerifyConstraintCasts > 1) {
    return res;
  }

  const TypeInteger* this_type = res->is_integer(bt);
  const TypeInteger* in_type = phase->type(in(1))->isa_integer(bt);
  if (in_type != nullptr &&
      (in_type->lo_as_long() != this_type->lo_as_long() ||
       in_type->hi_as_long() != this_type->hi_as_long())) {
    jlong lo1 = this_type->lo_as_long();
    jlong hi1 = this_type->hi_as_long();
    int w1 = this_type->_widen;
    if (lo1 >= 0) {
      // Keep a range assertion of >=0.
      lo1 = 0;        hi1 = max_signed_integer(bt);
    } else if (hi1 < 0) {
      // Keep a range assertion of <0.
      lo1 = min_signed_integer(bt); hi1 = -1;
    } else {
      lo1 = min_signed_integer(bt); hi1 = max_signed_integer(bt);
    }
    return TypeInteger::make(MAX2(in_type->lo_as_long(), lo1),
                             MIN2(in_type->hi_as_long(), hi1),
                             MAX2((int)in_type->_widen, w1), bt);
  }
  return res;
}
