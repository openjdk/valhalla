/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2024, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP
#define CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP

#include "asm/assembler.inline.hpp"
#include "code/vmreg.hpp"
#include "metaprogramming/enableIf.hpp"
#include "oops/compressedOops.hpp"
#include "oops/compressedKlass.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"
#include "runtime/signature.hpp"


class ciInlineKlass;

class OopMap;

// MacroAssembler extends Assembler by frequently used macros.
//
// Instructions for which a 'better' code sequence exists depending
// on arguments should also go in here.

class MacroAssembler: public Assembler {
  friend class LIR_Assembler;

 public:
  using Assembler::mov;
  using Assembler::movi;

 protected:

  // Support for VM calls
  //
  // This is the base routine called by the different versions of call_VM_leaf. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  virtual void call_VM_leaf_base(
    address entry_point,               // the entry point
    int     number_of_arguments,        // the number of arguments to pop after the call
    Label *retaddr = nullptr
  );

  virtual void call_VM_leaf_base(
    address entry_point,               // the entry point
    int     number_of_arguments,        // the number of arguments to pop after the call
    Label &retaddr) {
    call_VM_leaf_base(entry_point, number_of_arguments, &retaddr);
  }

  // This is the base routine called by the different versions of call_VM. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  //
  // If no java_thread register is specified (noreg) than rthread will be used instead. call_VM_base
  // returns the register which contains the thread upon return. If a thread register has been
  // specified, the return value will correspond to that register. If no last_java_sp is specified
  // (noreg) than rsp will be used instead.
  virtual void call_VM_base(           // returns the register containing the thread upon return
    Register oop_result,               // where an oop-result ends up if any; use noreg otherwise
    Register java_thread,              // the thread if computed before     ; use noreg otherwise
    Register last_java_sp,             // to set up last_Java_frame in stubs; use noreg otherwise
    address  entry_point,              // the entry point
    int      number_of_arguments,      // the number of arguments (w/o thread) to pop after the call
    bool     check_exceptions          // whether to check for pending exceptions after return
  );

  void call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions = true);

  enum KlassDecodeMode {
    KlassDecodeNone,
    KlassDecodeZero,
    KlassDecodeXor,
    KlassDecodeMovk
  };

  // Calculate decoding mode based on given parameters, used for checking then ultimately setting.
  static KlassDecodeMode klass_decode_mode(address base, int shift, const size_t range);

 private:
  static KlassDecodeMode _klass_decode_mode;

  // Returns above setting with asserts
  static KlassDecodeMode klass_decode_mode();

 public:
  // Checks the decode mode and returns false if not compatible with preferred decoding mode.
  static bool check_klass_decode_mode(address base, int shift, const size_t range);

  // Sets the decode mode and returns false if cannot be set.
  static bool set_klass_decode_mode(address base, int shift, const size_t range);

 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

 // These routines should emit JVMTI PopFrame and ForceEarlyReturn handling code.
 // The implementation is only non-empty for the InterpreterMacroAssembler,
 // as only the interpreter handles PopFrame and ForceEarlyReturn requests.
 virtual void check_and_handle_popframe(Register java_thread);
 virtual void check_and_handle_earlyret(Register java_thread);

  void safepoint_poll(Label& slow_path, bool at_return, bool acquire, bool in_nmethod, Register tmp = rscratch1);
  void rt_call(address dest, Register tmp = rscratch1);

  // Load Effective Address
  void lea(Register r, const Address &a) {
    InstructionMark im(this);
    a.lea(this, r);
  }

  /* Sometimes we get misaligned loads and stores, usually from Unsafe
     accesses, and these can exceed the offset range. */
  Address legitimize_address(const Address &a, int size, Register scratch) {
    if (a.getMode() == Address::base_plus_offset) {
      if (! Address::offset_ok_for_immed(a.offset(), exact_log2(size))) {
        block_comment("legitimize_address {");
        lea(scratch, a);
        block_comment("} legitimize_address");
        return Address(scratch);
      }
    }
    return a;
  }

  void addmw(Address a, Register incr, Register scratch) {
    ldrw(scratch, a);
    addw(scratch, scratch, incr);
    strw(scratch, a);
  }

  // Add constant to memory word
  void addmw(Address a, int imm, Register scratch) {
    ldrw(scratch, a);
    if (imm > 0)
      addw(scratch, scratch, (unsigned)imm);
    else
      subw(scratch, scratch, (unsigned)-imm);
    strw(scratch, a);
  }

  void bind(Label& L) {
    Assembler::bind(L);
    code()->clear_last_insn();
    code()->set_last_label(pc());
  }

  void membar(Membar_mask_bits order_constraint);

  using Assembler::ldr;
  using Assembler::str;
  using Assembler::ldrw;
  using Assembler::strw;

  void ldr(Register Rx, const Address &adr);
  void ldrw(Register Rw, const Address &adr);
  void str(Register Rx, const Address &adr);
  void strw(Register Rx, const Address &adr);

  // Frame creation and destruction shared between JITs.
  void build_frame(int framesize);
  void remove_frame(int framesize);

  virtual void _call_Unimplemented(address call_site) {
    mov(rscratch2, call_site);
  }

// Microsoft's MSVC team thinks that the __FUNCSIG__ is approximately (sympathy for calling conventions) equivalent to __PRETTY_FUNCTION__
// Also, from Clang patch: "It is very similar to GCC's PRETTY_FUNCTION, except it prints the calling convention."
// https://reviews.llvm.org/D3311

#ifdef _WIN64
#define call_Unimplemented() _call_Unimplemented((address)__FUNCSIG__)
#else
#define call_Unimplemented() _call_Unimplemented((address)__PRETTY_FUNCTION__)
#endif

  // aliases defined in AARCH64 spec

  template<class T>
  inline void cmpw(Register Rd, T imm)  { subsw(zr, Rd, imm); }

  inline void cmp(Register Rd, unsigned char imm8)  { subs(zr, Rd, imm8); }
  inline void cmp(Register Rd, unsigned imm) = delete;

  template<class T>
  inline void cmnw(Register Rd, T imm) { addsw(zr, Rd, imm); }

  inline void cmn(Register Rd, unsigned char imm8)  { adds(zr, Rd, imm8); }
  inline void cmn(Register Rd, unsigned imm) = delete;

  void cset(Register Rd, Assembler::Condition cond) {
    csinc(Rd, zr, zr, ~cond);
  }
  void csetw(Register Rd, Assembler::Condition cond) {
    csincw(Rd, zr, zr, ~cond);
  }

  void cneg(Register Rd, Register Rn, Assembler::Condition cond) {
    csneg(Rd, Rn, Rn, ~cond);
  }
  void cnegw(Register Rd, Register Rn, Assembler::Condition cond) {
    csnegw(Rd, Rn, Rn, ~cond);
  }

  inline void movw(Register Rd, Register Rn) {
    if (Rd == sp || Rn == sp) {
      Assembler::addw(Rd, Rn, 0U);
    } else {
      orrw(Rd, zr, Rn);
    }
  }
  inline void mov(Register Rd, Register Rn) {
    assert(Rd != r31_sp && Rn != r31_sp, "should be");
    if (Rd == Rn) {
    } else if (Rd == sp || Rn == sp) {
      Assembler::add(Rd, Rn, 0U);
    } else {
      orr(Rd, zr, Rn);
    }
  }

  inline void moviw(Register Rd, unsigned imm) { orrw(Rd, zr, imm); }
  inline void movi(Register Rd, unsigned imm) { orr(Rd, zr, imm); }

  inline void tstw(Register Rd, Register Rn) { andsw(zr, Rd, Rn); }
  inline void tst(Register Rd, Register Rn) { ands(zr, Rd, Rn); }

  inline void tstw(Register Rd, uint64_t imm) { andsw(zr, Rd, imm); }
  inline void tst(Register Rd, uint64_t imm) { ands(zr, Rd, imm); }

  inline void bfiw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void bfi(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void bfxilw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void bfxil(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void sbfizw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void sbfiz(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void sbfxw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void sbfx(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void ubfizw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void ubfiz(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void ubfxw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void ubfx(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void asrw(Register Rd, Register Rn, unsigned imm) {
    sbfmw(Rd, Rn, imm, 31);
  }

  inline void asr(Register Rd, Register Rn, unsigned imm) {
    sbfm(Rd, Rn, imm, 63);
  }

  inline void lslw(Register Rd, Register Rn, unsigned imm) {
    ubfmw(Rd, Rn, ((32 - imm) & 31), (31 - imm));
  }

  inline void lsl(Register Rd, Register Rn, unsigned imm) {
    ubfm(Rd, Rn, ((64 - imm) & 63), (63 - imm));
  }

  inline void lsrw(Register Rd, Register Rn, unsigned imm) {
    ubfmw(Rd, Rn, imm, 31);
  }

  inline void lsr(Register Rd, Register Rn, unsigned imm) {
    ubfm(Rd, Rn, imm, 63);
  }

  inline void rorw(Register Rd, Register Rn, unsigned imm) {
    extrw(Rd, Rn, Rn, imm);
  }

  inline void ror(Register Rd, Register Rn, unsigned imm) {
    extr(Rd, Rn, Rn, imm);
  }

  inline void sxtbw(Register Rd, Register Rn) {
    sbfmw(Rd, Rn, 0, 7);
  }
  inline void sxthw(Register Rd, Register Rn) {
    sbfmw(Rd, Rn, 0, 15);
  }
  inline void sxtb(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 7);
  }
  inline void sxth(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 15);
  }
  inline void sxtw(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 31);
  }

  inline void uxtbw(Register Rd, Register Rn) {
    ubfmw(Rd, Rn, 0, 7);
  }
  inline void uxthw(Register Rd, Register Rn) {
    ubfmw(Rd, Rn, 0, 15);
  }
  inline void uxtb(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 7);
  }
  inline void uxth(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 15);
  }
  inline void uxtw(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 31);
  }

  inline void cmnw(Register Rn, Register Rm) {
    addsw(zr, Rn, Rm);
  }
  inline void cmn(Register Rn, Register Rm) {
    adds(zr, Rn, Rm);
  }

  inline void cmpw(Register Rn, Register Rm) {
    subsw(zr, Rn, Rm);
  }
  inline void cmp(Register Rn, Register Rm) {
    subs(zr, Rn, Rm);
  }

  inline void negw(Register Rd, Register Rn) {
    subw(Rd, zr, Rn);
  }

  inline void neg(Register Rd, Register Rn) {
    sub(Rd, zr, Rn);
  }

  inline void negsw(Register Rd, Register Rn) {
    subsw(Rd, zr, Rn);
  }

  inline void negs(Register Rd, Register Rn) {
    subs(Rd, zr, Rn);
  }

  inline void cmnw(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    addsw(zr, Rn, Rm, kind, shift);
  }
  inline void cmn(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    adds(zr, Rn, Rm, kind, shift);
  }

  inline void cmpw(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    subsw(zr, Rn, Rm, kind, shift);
  }
  inline void cmp(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    subs(zr, Rn, Rm, kind, shift);
  }

  inline void negw(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subw(Rd, zr, Rn, kind, shift);
  }

  inline void neg(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    sub(Rd, zr, Rn, kind, shift);
  }

  inline void negsw(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subsw(Rd, zr, Rn, kind, shift);
  }

  inline void negs(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subs(Rd, zr, Rn, kind, shift);
  }

  inline void mnegw(Register Rd, Register Rn, Register Rm) {
    msubw(Rd, Rn, Rm, zr);
  }
  inline void mneg(Register Rd, Register Rn, Register Rm) {
    msub(Rd, Rn, Rm, zr);
  }

  inline void mulw(Register Rd, Register Rn, Register Rm) {
    maddw(Rd, Rn, Rm, zr);
  }
  inline void mul(Register Rd, Register Rn, Register Rm) {
    madd(Rd, Rn, Rm, zr);
  }

  inline void smnegl(Register Rd, Register Rn, Register Rm) {
    smsubl(Rd, Rn, Rm, zr);
  }
  inline void smull(Register Rd, Register Rn, Register Rm) {
    smaddl(Rd, Rn, Rm, zr);
  }

  inline void umnegl(Register Rd, Register Rn, Register Rm) {
    umsubl(Rd, Rn, Rm, zr);
  }
  inline void umull(Register Rd, Register Rn, Register Rm) {
    umaddl(Rd, Rn, Rm, zr);
  }

#define WRAP(INSN)                                                            \
  void INSN(Register Rd, Register Rn, Register Rm, Register Ra) {             \
    if (VM_Version::supports_a53mac() && Ra != zr)                            \
      nop();                                                                  \
    Assembler::INSN(Rd, Rn, Rm, Ra);                                          \
  }

  WRAP(madd) WRAP(msub) WRAP(maddw) WRAP(msubw)
  WRAP(smaddl) WRAP(smsubl) WRAP(umaddl) WRAP(umsubl)
#undef WRAP


  // macro assembly operations needed for aarch64

public:

  enum FpPushPopMode {
    PushPopFull,
    PushPopSVE,
    PushPopNeon,
    PushPopFp
  };

  // first two private routines for loading 32 bit or 64 bit constants
private:

  void mov_immediate64(Register dst, uint64_t imm64);
  void mov_immediate32(Register dst, uint32_t imm32);

  int push(unsigned int bitset, Register stack);
  int pop(unsigned int bitset, Register stack);

  int push_fp(unsigned int bitset, Register stack, FpPushPopMode mode);
  int pop_fp(unsigned int bitset, Register stack, FpPushPopMode mode);

  int push_p(unsigned int bitset, Register stack);
  int pop_p(unsigned int bitset, Register stack);

  void mov(Register dst, Address a);

public:

  void push(RegSet regs, Register stack) { if (regs.bits()) push(regs.bits(), stack); }
  void pop(RegSet regs, Register stack) { if (regs.bits()) pop(regs.bits(), stack); }

  void push_fp(FloatRegSet regs, Register stack, FpPushPopMode mode = PushPopFull) { if (regs.bits()) push_fp(regs.bits(), stack, mode); }
  void pop_fp(FloatRegSet regs, Register stack, FpPushPopMode mode = PushPopFull) { if (regs.bits()) pop_fp(regs.bits(), stack, mode); }

  static RegSet call_clobbered_gp_registers();

  void push_p(PRegSet regs, Register stack) { if (regs.bits()) push_p(regs.bits(), stack); }
  void pop_p(PRegSet regs, Register stack) { if (regs.bits()) pop_p(regs.bits(), stack); }

  // Push and pop everything that might be clobbered by a native
  // runtime call except rscratch1 and rscratch2.  (They are always
  // scratch, so we don't have to protect them.)  Only save the lower
  // 64 bits of each vector register. Additional registers can be excluded
  // in a passed RegSet.
  void push_call_clobbered_registers_except(RegSet exclude);
  void pop_call_clobbered_registers_except(RegSet exclude);

  void push_call_clobbered_registers() {
    push_call_clobbered_registers_except(RegSet());
  }
  void pop_call_clobbered_registers() {
    pop_call_clobbered_registers_except(RegSet());
  }


  // now mov instructions for loading absolute addresses and 32 or
  // 64 bit integers

  inline void mov(Register dst, address addr)             { mov_immediate64(dst, (uint64_t)addr); }

  template<typename T, ENABLE_IF(std::is_integral<T>::value)>
  inline void mov(Register dst, T o)                      { mov_immediate64(dst, (uint64_t)o); }

  inline void movw(Register dst, uint32_t imm32)          { mov_immediate32(dst, imm32); }

  void mov(Register dst, RegisterOrConstant src) {
    if (src.is_register())
      mov(dst, src.as_register());
    else
      mov(dst, src.as_constant());
  }

  void movptr(Register r, uintptr_t imm64);

  void mov(FloatRegister Vd, SIMD_Arrangement T, uint64_t imm64);

  void mov(FloatRegister Vd, SIMD_Arrangement T, FloatRegister Vn) {
    orr(Vd, T, Vn, Vn);
  }

  void flt_to_flt16(Register dst, FloatRegister src, FloatRegister tmp) {
    fcvtsh(tmp, src);
    smov(dst, tmp, H, 0);
  }

  void flt16_to_flt(FloatRegister dst, Register src, FloatRegister tmp) {
    mov(tmp, H, 0, src);
    fcvths(dst, tmp);
  }

  // Generalized Test Bit And Branch, including a "far" variety which
  // spans more than 32KiB.
  void tbr(Condition cond, Register Rt, int bitpos, Label &dest, bool isfar = false) {
    assert(cond == EQ || cond == NE, "must be");

    if (isfar)
      cond = ~cond;

    void (Assembler::* branch)(Register Rt, int bitpos, Label &L);
    if (cond == Assembler::EQ)
      branch = &Assembler::tbz;
    else
      branch = &Assembler::tbnz;

    if (isfar) {
      Label L;
      (this->*branch)(Rt, bitpos, L);
      b(dest);
      bind(L);
    } else {
      (this->*branch)(Rt, bitpos, dest);
    }
  }

  // macro instructions for accessing and updating floating point
  // status register
  //
  // FPSR : op1 == 011
  //        CRn == 0100
  //        CRm == 0100
  //        op2 == 001

  inline void get_fpsr(Register reg)
  {
    mrs(0b11, 0b0100, 0b0100, 0b001, reg);
  }

  inline void set_fpsr(Register reg)
  {
    msr(0b011, 0b0100, 0b0100, 0b001, reg);
  }

  inline void clear_fpsr()
  {
    msr(0b011, 0b0100, 0b0100, 0b001, zr);
  }

  // FPCR : op1 == 011
  //        CRn == 0100
  //        CRm == 0100
  //        op2 == 000

  inline void get_fpcr(Register reg) {
    mrs(0b11, 0b0100, 0b0100, 0b000, reg);
  }

  inline void set_fpcr(Register reg) {
    msr(0b011, 0b0100, 0b0100, 0b000, reg);
  }

  // DCZID_EL0: op1 == 011
  //            CRn == 0000
  //            CRm == 0000
  //            op2 == 111
  inline void get_dczid_el0(Register reg)
  {
    mrs(0b011, 0b0000, 0b0000, 0b111, reg);
  }

  // CTR_EL0:   op1 == 011
  //            CRn == 0000
  //            CRm == 0000
  //            op2 == 001
  inline void get_ctr_el0(Register reg)
  {
    mrs(0b011, 0b0000, 0b0000, 0b001, reg);
  }

  inline void get_nzcv(Register reg) {
    mrs(0b011, 0b0100, 0b0010, 0b000, reg);
  }

  inline void set_nzcv(Register reg) {
    msr(0b011, 0b0100, 0b0010, 0b000, reg);
  }

  // idiv variant which deals with MINLONG as dividend and -1 as divisor
  int corrected_idivl(Register result, Register ra, Register rb,
                      bool want_remainder, Register tmp = rscratch1);
  int corrected_idivq(Register result, Register ra, Register rb,
                      bool want_remainder, Register tmp = rscratch1);

  // Support for null-checks
  //
  // Generates code that causes a null OS exception if the content of reg is null.
  // If the accessed location is M[reg + offset] and the offset is known, provide the
  // offset. No explicit code generation is needed if the offset is within a certain
  // range (0 <= offset <= page_size).

  virtual void null_check(Register reg, int offset = -1);
  static bool needs_explicit_null_check(intptr_t offset);
  static bool uses_implicit_null_check(void* address);

  // markWord tests, kills markWord reg
  void test_markword_is_inline_type(Register markword, Label& is_inline_type);

  // inlineKlass queries, kills temp_reg
  void test_klass_is_inline_type(Register klass, Register temp_reg, Label& is_inline_type);
  void test_oop_is_not_inline_type(Register object, Register tmp, Label& not_inline_type);

  void test_field_is_null_free_inline_type(Register flags, Register temp_reg, Label& is_null_free);
  void test_field_is_not_null_free_inline_type(Register flags, Register temp_reg, Label& not_null_free);
  void test_field_is_flat(Register flags, Register temp_reg, Label& is_flat);
  void test_field_has_null_marker(Register flags, Register temp_reg, Label& has_null_marker);

  // Check oops for special arrays, i.e. flat arrays and/or null-free arrays
  void test_oop_prototype_bit(Register oop, Register temp_reg, int32_t test_bit, bool jmp_set, Label& jmp_label);
  void test_flat_array_oop(Register klass, Register temp_reg, Label& is_flat_array);
  void test_non_flat_array_oop(Register oop, Register temp_reg, Label&is_non_flat_array);
  void test_null_free_array_oop(Register oop, Register temp_reg, Label& is_null_free_array);
  void test_non_null_free_array_oop(Register oop, Register temp_reg, Label&is_non_null_free_array);

  // Check array klass layout helper for flat or null-free arrays...
  void test_flat_array_layout(Register lh, Label& is_flat_array);
  void test_non_flat_array_layout(Register lh, Label& is_non_flat_array);

  static address target_addr_for_insn(address insn_addr, unsigned insn);
  static address target_addr_for_insn_or_null(address insn_addr, unsigned insn);
  static address target_addr_for_insn(address insn_addr) {
    unsigned insn = *(unsigned*)insn_addr;
    return target_addr_for_insn(insn_addr, insn);
  }
  static address target_addr_for_insn_or_null(address insn_addr) {
    unsigned insn = *(unsigned*)insn_addr;
    return target_addr_for_insn_or_null(insn_addr, insn);
  }

  // Required platform-specific helpers for Label::patch_instructions.
  // They _shadow_ the declarations in AbstractAssembler, which are undefined.
  static int pd_patch_instruction_size(address branch, address target);
  static void pd_patch_instruction(address branch, address target, const char* file = nullptr, int line = 0) {
    pd_patch_instruction_size(branch, target);
  }
  static address pd_call_destination(address branch) {
    return target_addr_for_insn(branch);
  }
#ifndef PRODUCT
  static void pd_print_patched_instruction(address branch);
#endif

  static int patch_oop(address insn_addr, address o);
  static int patch_narrow_klass(address insn_addr, narrowKlass n);

  // Return whether code is emitted to a scratch blob.
  virtual bool in_scratch_emit_size() {
    return false;
  }
  address emit_trampoline_stub(int insts_call_instruction_offset, address target);
  static int max_trampoline_stub_size();
  void emit_static_call_stub();
  static int static_call_stub_size();

  // The following 4 methods return the offset of the appropriate move instruction

  // Support for fast byte/short loading with zero extension (depending on particular CPU)
  int load_unsigned_byte(Register dst, Address src);
  int load_unsigned_short(Register dst, Address src);

  // Support for fast byte/short loading with sign extension (depending on particular CPU)
  int load_signed_byte(Register dst, Address src);
  int load_signed_short(Register dst, Address src);

  int load_signed_byte32(Register dst, Address src);
  int load_signed_short32(Register dst, Address src);

  // Support for sign-extension (hi:lo = extend_sign(lo))
  void extend_sign(Register hi, Register lo);

  // Load and store values by size and signed-ness
  void load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed);
  void store_sized_value(Address dst, Register src, size_t size_in_bytes);

  // Support for inc/dec with optimal instruction selection depending on value

  // x86_64 aliases an unqualified register/address increment and
  // decrement to call incrementq and decrementq but also supports
  // explicitly sized calls to incrementq/decrementq or
  // incrementl/decrementl

  // for aarch64 the proper convention would be to use
  // increment/decrement for 64 bit operations and
  // incrementw/decrementw for 32 bit operations. so when porting
  // x86_64 code we can leave calls to increment/decrement as is,
  // replace incrementq/decrementq with increment/decrement and
  // replace incrementl/decrementl with incrementw/decrementw.

  // n.b. increment/decrement calls with an Address destination will
  // need to use a scratch register to load the value to be
  // incremented. increment/decrement calls which add or subtract a
  // constant value greater than 2^12 will need to use a 2nd scratch
  // register to hold the constant. so, a register increment/decrement
  // may trash rscratch2 and an address increment/decrement trash
  // rscratch and rscratch2

  void decrementw(Address dst, int value = 1);
  void decrementw(Register reg, int value = 1);

  void decrement(Register reg, int value = 1);
  void decrement(Address dst, int value = 1);

  void incrementw(Address dst, int value = 1);
  void incrementw(Register reg, int value = 1);

  void increment(Register reg, int value = 1);
  void increment(Address dst, int value = 1);


  // Alignment
  void align(int modulus);
  void align(int modulus, int target);

  // nop
  void post_call_nop();

  // Stack frame creation/removal
  void enter(bool strip_ret_addr = false);
  void leave();

  // ROP Protection
  void protect_return_address();
  void protect_return_address(Register return_reg);
  void authenticate_return_address();
  void authenticate_return_address(Register return_reg);
  void strip_return_address();
  void check_return_address(Register return_reg=lr) PRODUCT_RETURN;

  // Support for getting the JavaThread pointer (i.e.; a reference to thread-local information)
  // The pointer will be loaded into the thread register.
  void get_thread(Register thread);

  // support for argument shuffling
  void move32_64(VMRegPair src, VMRegPair dst, Register tmp = rscratch1);
  void float_move(VMRegPair src, VMRegPair dst, Register tmp = rscratch1);
  void long_move(VMRegPair src, VMRegPair dst, Register tmp = rscratch1);
  void double_move(VMRegPair src, VMRegPair dst, Register tmp = rscratch1);
  void object_move(
                   OopMap* map,
                   int oop_handle_offset,
                   int framesize_in_slots,
                   VMRegPair src,
                   VMRegPair dst,
                   bool is_receiver,
                   int* receiver_offset);


  // Support for VM calls
  //
  // It is imperative that all calls into the VM are handled via the call_VM macros.
  // They make sure that the stack linkage is setup correctly. call_VM's correspond
  // to ENTRY/ENTRY_X entry points while call_VM_leaf's correspond to LEAF entry points.


  void call_VM(Register oop_result,
               address entry_point,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  // Overloadings with last_Java_sp
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               int number_of_arguments = 0,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, bool
               check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  void get_vm_result_oop(Register oop_result, Register thread);
  void get_vm_result_metadata(Register metadata_result, Register thread);

  // These always tightly bind to MacroAssembler::call_VM_base
  // bypassing the virtual implementation
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, int number_of_arguments = 0, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4, bool check_exceptions = true);

  void call_VM_leaf(address entry_point,
                    int number_of_arguments = 0);
  void call_VM_leaf(address entry_point,
                    Register arg_1);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2, Register arg_3);

  // These always tightly bind to MacroAssembler::call_VM_leaf_base
  // bypassing the virtual implementation
  void super_call_VM_leaf(address entry_point);
  void super_call_VM_leaf(address entry_point, Register arg_1);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4);

  // last Java Frame (fills frame anchor)
  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           address last_java_pc,
                           Register scratch);

  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           Label &last_java_pc,
                           Register scratch);

  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           Register last_java_pc,
                           Register scratch);

  void reset_last_Java_frame(Register thread);

  // thread in the default location (rthread)
  void reset_last_Java_frame(bool clear_fp);

  // Stores
  void store_check(Register obj);                // store check for obj - register is destroyed afterwards
  void store_check(Register obj, Address dst);   // same as above, dst is exact store location (reg. is destroyed)

  void resolve_jobject(Register value, Register tmp1, Register tmp2);
  void resolve_global_jobject(Register value, Register tmp1, Register tmp2);

  // C 'boolean' to Java boolean: x == 0 ? 0 : 1
  void c2bool(Register x);

  void load_method_holder_cld(Register rresult, Register rmethod);
  void load_method_holder(Register holder, Register method);

  // oop manipulations
  void load_metadata(Register dst, Register src);

  void load_narrow_klass_compact(Register dst, Register src);
  void load_klass(Register dst, Register src);
  void store_klass(Register dst, Register src);
  void cmp_klass(Register obj, Register klass, Register tmp);
  void cmp_klasses_from_objects(Register obj1, Register obj2, Register tmp1, Register tmp2);

  void resolve_weak_handle(Register result, Register tmp1, Register tmp2);
  void resolve_oop_handle(Register result, Register tmp1, Register tmp2);
  void load_mirror(Register dst, Register method, Register tmp1, Register tmp2);

  void access_load_at(BasicType type, DecoratorSet decorators, Register dst, Address src,
                      Register tmp1, Register tmp2);

  void access_store_at(BasicType type, DecoratorSet decorators, Address dst, Register val,
                       Register tmp1, Register tmp2, Register tmp3);

  void flat_field_copy(DecoratorSet decorators, Register src, Register dst, Register inline_layout_info);

  // inline type data payload offsets...
  void payload_offset(Register inline_klass, Register offset);
  void payload_address(Register oop, Register data, Register inline_klass);
  // get data payload ptr a flat value array at index, kills rcx and index
  void data_for_value_array_index(Register array, Register array_klass,
                                  Register index, Register data);

  void load_heap_oop(Register dst, Address src, Register tmp1,
                     Register tmp2, DecoratorSet decorators = 0);

  void load_heap_oop_not_null(Register dst, Address src, Register tmp1,
                              Register tmp2, DecoratorSet decorators = 0);
  void store_heap_oop(Address dst, Register val, Register tmp1,
                      Register tmp2, Register tmp3, DecoratorSet decorators = 0);

  // currently unimplemented
  // Used for storing null. All other oop constants should be
  // stored using routines that take a jobject.
  void store_heap_oop_null(Address dst);

  void load_prototype_header(Register dst, Register src);

  void store_klass_gap(Register dst, Register src);

  // This dummy is to prevent a call to store_heap_oop from
  // converting a zero (like null) into a Register by giving
  // the compiler two choices it can't resolve

  void store_heap_oop(Address dst, void* dummy);

  void encode_heap_oop(Register d, Register s);
  void encode_heap_oop(Register r) { encode_heap_oop(r, r); }
  void decode_heap_oop(Register d, Register s);
  void decode_heap_oop(Register r) { decode_heap_oop(r, r); }
  void encode_heap_oop_not_null(Register r);
  void decode_heap_oop_not_null(Register r);
  void encode_heap_oop_not_null(Register dst, Register src);
  void decode_heap_oop_not_null(Register dst, Register src);

  void set_narrow_oop(Register dst, jobject obj);

  void encode_klass_not_null(Register r);
  void decode_klass_not_null(Register r);
  void encode_klass_not_null(Register dst, Register src);
  void decode_klass_not_null(Register dst, Register src);

  void set_narrow_klass(Register dst, Klass* k);

  // if heap base register is used - reinit it with the correct value
  void reinit_heapbase();

  DEBUG_ONLY(void verify_heapbase(const char* msg);)

  void push_CPU_state(bool save_vectors = false, bool use_sve = false,
                      int sve_vector_size_in_bytes = 0, int total_predicate_in_bytes = 0);
  void pop_CPU_state(bool restore_vectors = false, bool use_sve = false,
                     int sve_vector_size_in_bytes = 0, int total_predicate_in_bytes = 0);

  void push_cont_fastpath(Register java_thread = rthread);
  void pop_cont_fastpath(Register java_thread = rthread);

  void inc_held_monitor_count(Register tmp);
  void dec_held_monitor_count(Register tmp);

  // Round up to a power of two
  void round_to(Register reg, int modulus);

  // java.lang.Math::round intrinsics
  void java_round_double(Register dst, FloatRegister src, FloatRegister ftmp);
  void java_round_float(Register dst, FloatRegister src, FloatRegister ftmp);

  // allocation

  // Object / value buffer allocation...
  // Allocate instance of klass, assumes klass initialized by caller
  // new_obj prefers to be rax
  // Kills t1 and t2, perserves klass, return allocation in new_obj (rsi on LP64)
  void allocate_instance(Register klass, Register new_obj,
                         Register t1, Register t2,
                         bool clear_fields, Label& alloc_failed);

  void tlab_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void verify_tlab();

  // For field "index" within "klass", return inline_klass ...
  void get_inline_type_field_klass(Register klass, Register index, Register inline_klass);
  void inline_layout_info(Register holder_klass, Register index, Register layout_info);


  // interface method calling
  void lookup_interface_method(Register recv_klass,
                               Register intf_klass,
                               RegisterOrConstant itable_index,
                               Register method_result,
                               Register scan_temp,
                               Label& no_such_interface,
                   bool return_method = true);

  void lookup_interface_method_stub(Register recv_klass,
                                    Register holder_klass,
                                    Register resolved_klass,
                                    Register method_result,
                                    Register temp_reg,
                                    Register temp_reg2,
                                    int itable_index,
                                    Label& L_no_such_interface);

  // virtual method calling
  // n.b. x86 allows RegisterOrConstant for vtable_index
  void lookup_virtual_method(Register recv_klass,
                             RegisterOrConstant vtable_index,
                             Register method_result);

  // Test sub_klass against super_klass, with fast and slow paths.

  // The fast path produces a tri-state answer: yes / no / maybe-slow.
  // One of the three labels can be null, meaning take the fall-through.
  // If super_check_offset is -1, the value is loaded up from super_klass.
  // No registers are killed, except temp_reg.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     Label* L_slow_path,
                                     Register super_check_offset = noreg);

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // The temp_reg and temp2_reg can be noreg, if no temps are available.
  // Updates the sub's secondary super cache as necessary.
  // If set_cond_codes, condition codes will be Z on success, NZ on failure.
  void check_klass_subtype_slow_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Register temp2_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     bool set_cond_codes = false);

  void check_klass_subtype_slow_path_linear(Register sub_klass,
                                            Register super_klass,
                                            Register temp_reg,
                                            Register temp2_reg,
                                            Label* L_success,
                                            Label* L_failure,
                                            bool set_cond_codes = false);

  void check_klass_subtype_slow_path_table(Register sub_klass,
                                           Register super_klass,
                                           Register temp_reg,
                                           Register temp2_reg,
                                           Register temp3_reg,
                                           Register result_reg,
                                           FloatRegister vtemp_reg,
                                           Label* L_success,
                                           Label* L_failure,
                                           bool set_cond_codes = false);

  // If r is valid, return r.
  // If r is invalid, remove a register r2 from available_regs, add r2
  // to regs_to_push, then return r2.
  Register allocate_if_noreg(const Register r,
                             RegSetIterator<Register> &available_regs,
                             RegSet &regs_to_push);

  // Secondary subtype checking
  void lookup_secondary_supers_table_var(Register sub_klass,
                                         Register r_super_klass,
                                         Register temp1,
                                         Register temp2,
                                         Register temp3,
                                         FloatRegister vtemp,
                                         Register result,
                                         Label *L_success);


  // As above, but with a constant super_klass.
  // The result is in Register result, not the condition codes.
  bool lookup_secondary_supers_table_const(Register r_sub_klass,
                                           Register r_super_klass,
                                           Register temp1,
                                           Register temp2,
                                           Register temp3,
                                           FloatRegister vtemp,
                                           Register result,
                                           u1 super_klass_slot,
                                           bool stub_is_near = false);

  void verify_secondary_supers_table(Register r_sub_klass,
                                     Register r_super_klass,
                                     Register temp1,
                                     Register temp2,
                                     Register result);

  void lookup_secondary_supers_table_slow_path(Register r_super_klass,
                                               Register r_array_base,
                                               Register r_array_index,
                                               Register r_bitmap,
                                               Register temp1,
                                               Register result,
                                               bool is_stub = true);

  // Simplified, combined version, good for typical uses.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp_reg,
                           Label& L_success);

  void clinit_barrier(Register klass,
                      Register thread,
                      Label* L_fast_path = nullptr,
                      Label* L_slow_path = nullptr);

  Address argument_address(RegisterOrConstant arg_slot, int extra_slot_offset = 0);

  void verify_sve_vector_length(Register tmp = rscratch1);
  void reinitialize_ptrue() {
    if (UseSVE > 0) {
      sve_ptrue(ptrue, B);
    }
  }
  void verify_ptrue();

  // Debugging

  // only if +VerifyOops
  void _verify_oop(Register reg, const char* s, const char* file, int line);
  void _verify_oop_addr(Address addr, const char * s, const char* file, int line);

  void _verify_oop_checked(Register reg, const char* s, const char* file, int line) {
    if (VerifyOops) {
      _verify_oop(reg, s, file, line);
    }
  }
  void _verify_oop_addr_checked(Address reg, const char* s, const char* file, int line) {
    if (VerifyOops) {
      _verify_oop_addr(reg, s, file, line);
    }
  }

// TODO: verify method and klass metadata (compare against vptr?)
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line){}

#define verify_oop(reg) _verify_oop_checked(reg, "broken oop " #reg, __FILE__, __LINE__)
#define verify_oop_msg(reg, msg) _verify_oop_checked(reg, "broken oop " #reg ", " #msg, __FILE__, __LINE__)
#define verify_oop_addr(addr) _verify_oop_addr_checked(addr, "broken oop addr " #addr, __FILE__, __LINE__)
#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

  // Restore cpu control state after JNI call
  void restore_cpu_control_state_after_jni(Register tmp1, Register tmp2);

  // prints msg, dumps registers and stops execution
  void stop(const char* msg);

  static void debug64(char* msg, int64_t pc, int64_t regs[]);

  void untested()                                { stop("untested"); }

  void unimplemented(const char* what = "");

  void should_not_reach_here()                   { stop("should not reach here"); }

  void _assert_asm(Condition cc, const char* msg);
#define assert_asm0(cc, msg) _assert_asm(cc, FILE_AND_LINE ": " msg)
#define assert_asm(masm, command, cc, msg) DEBUG_ONLY(command; (masm)->_assert_asm(cc, FILE_AND_LINE ": " #command " " #cc ": " msg))

  // Stack overflow checking
  void bang_stack_with_offset(int offset) {
    // stack grows down, caller passes positive offset
    assert(offset > 0, "must bang with negative offset");
    sub(rscratch2, sp, offset);
    str(zr, Address(rscratch2));
  }

  // Writes to stack successive pages until offset reached to check for
  // stack overflow + shadow pages.  Also, clobbers tmp
  void bang_stack_size(Register size, Register tmp);

  // Check for reserved stack access in method being exited (for JIT)
  void reserved_stack_check();

  // Arithmetics

  // Clobber: rscratch1, rscratch2
  void addptr(const Address &dst, int32_t src);

  // Clobber: rscratch1
  void cmpptr(Register src1, Address src2);

  void cmpoop(Register obj1, Register obj2);

  // Various forms of CAS

  void cmpxchg_obj_header(Register oldv, Register newv, Register obj, Register tmp,
                          Label &succeed, Label *fail);
  void cmpxchgptr(Register oldv, Register newv, Register addr, Register tmp,
                  Label &succeed, Label *fail);

  void cmpxchgw(Register oldv, Register newv, Register addr, Register tmp,
                  Label &succeed, Label *fail);

  void atomic_add(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addw(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addal(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addalw(Register prev, RegisterOrConstant incr, Register addr);

  void atomic_xchg(Register prev, Register newv, Register addr);
  void atomic_xchgw(Register prev, Register newv, Register addr);
  void atomic_xchgl(Register prev, Register newv, Register addr);
  void atomic_xchglw(Register prev, Register newv, Register addr);
  void atomic_xchgal(Register prev, Register newv, Register addr);
  void atomic_xchgalw(Register prev, Register newv, Register addr);

  void orptr(Address adr, RegisterOrConstant src) {
    ldr(rscratch1, adr);
    if (src.is_register())
      orr(rscratch1, rscratch1, src.as_register());
    else
      orr(rscratch1, rscratch1, src.as_constant());
    str(rscratch1, adr);
  }

  // A generic CAS; success or failure is in the EQ flag.
  // Clobbers rscratch1
  void cmpxchg(Register addr, Register expected, Register new_val,
               enum operand_size size,
               bool acquire, bool release, bool weak,
               Register result);

#ifdef ASSERT
  // Template short-hand support to clean-up after a failed call to trampoline
  // call generation (see trampoline_call() below),  when a set of Labels must
  // be reset (before returning).
  template<typename Label, typename... More>
  void reset_labels(Label &lbl, More&... more) {
    lbl.reset(); reset_labels(more...);
  }
  template<typename Label>
  void reset_labels(Label &lbl) {
    lbl.reset();
  }
#endif

private:
  void compare_eq(Register rn, Register rm, enum operand_size size);

public:
  // AArch64 OpenJDK uses four different types of calls:
  //   - direct call: bl pc_relative_offset
  //     This is the shortest and the fastest, but the offset has the range:
  //     +/-128MB for the release build, +/-2MB for the debug build.
  //
  //   - far call: adrp reg, pc_relative_offset; add; bl reg
  //     This is longer than a direct call. The offset has
  //     the range +/-4GB. As the code cache size is limited to 4GB,
  //     far calls can reach anywhere in the code cache. If a jump is
  //     needed rather than a call, a far jump 'b reg' can be used instead.
  //     All instructions are embedded at a call site.
  //
  //   - trampoline call:
  //     This is only available in C1/C2-generated code (nmethod). It is a combination
  //     of a direct call, which is used if the destination of a call is in range,
  //     and a register-indirect call. It has the advantages of reaching anywhere in
  //     the AArch64 address space and being patchable at runtime when the generated
  //     code is being executed by other threads.
  //
  //     [Main code section]
  //       bl trampoline
  //     [Stub code section]
  //     trampoline:
  //       ldr reg, pc + 8
  //       br reg
  //       <64-bit destination address>
  //
  //     If the destination is in range when the generated code is moved to the code
  //     cache, 'bl trampoline' is replaced with 'bl destination' and the trampoline
  //     is not used.
  //     The optimization does not remove the trampoline from the stub section.
  //     This is necessary because the trampoline may well be redirected later when
  //     code is patched, and the new destination may not be reachable by a simple BR
  //     instruction.
  //
  //   - indirect call: move reg, address; blr reg
  //     This too can reach anywhere in the address space, but it cannot be
  //     patched while code is running, so it must only be modified at a safepoint.
  //     This form of call is most suitable for targets at fixed addresses, which
  //     will never be patched.
  //
  // The patching we do conforms to the "Concurrent modification and
  // execution of instructions" section of the Arm Architectural
  // Reference Manual, which only allows B, BL, BRK, HVC, ISB, NOP, SMC,
  // or SVC instructions to be modified while another thread is
  // executing them.
  //
  // To patch a trampoline call when the BL can't reach, we first modify
  // the 64-bit destination address in the trampoline, then modify the
  // BL to point to the trampoline, then flush the instruction cache to
  // broadcast the change to all executing threads. See
  // NativeCall::set_destination_mt_safe for the details.
  //
  // There is a benign race in that the other thread might observe the
  // modified BL before it observes the modified 64-bit destination
  // address. That does not matter because the destination method has been
  // invalidated, so there will be a trap at its start.
  // For this to work, the destination address in the trampoline is
  // always updated, even if we're not using the trampoline.

  // Emit a direct call if the entry address will always be in range,
  // otherwise a trampoline call.
  // Supported entry.rspec():
  // - relocInfo::runtime_call_type
  // - relocInfo::opt_virtual_call_type
  // - relocInfo::static_call_type
  // - relocInfo::virtual_call_type
  //
  // Return: the call PC or null if CodeCache is full.
  // Clobbers: rscratch1
  address trampoline_call(Address entry);

  static bool far_branches() {
    return ReservedCodeCacheSize > branch_range;
  }

  // Check if branches to the non nmethod section require a far jump
  static bool codestub_branch_needs_far_jump() {
    return CodeCache::max_distance_to_non_nmethod() > branch_range;
  }

  // Emit a direct call/jump if the entry address will always be in range,
  // otherwise a far call/jump.
  // The address must be inside the code cache.
  // Supported entry.rspec():
  // - relocInfo::external_word_type
  // - relocInfo::runtime_call_type
  // - relocInfo::none
  // In the case of a far call/jump, the entry address is put in the tmp register.
  // The tmp register is invalidated.
  //
  // Far_jump returns the amount of the emitted code.
  void far_call(Address entry, Register tmp = rscratch1);
  int far_jump(Address entry, Register tmp = rscratch1);

  static int far_codestub_branch_size() {
    if (codestub_branch_needs_far_jump()) {
      return 3 * 4;  // adrp, add, br
    } else {
      return 4;
    }
  }

  // Emit the CompiledIC call idiom
  address ic_call(address entry, jint method_index = 0);
  static int ic_check_size();
  int ic_check(int end_alignment);

public:

  // Data

  void mov_metadata(Register dst, Metadata* obj);
  Address allocate_metadata_address(Metadata* obj);
  Address constant_oop_address(jobject obj);

  void movoop(Register dst, jobject obj);

  // CRC32 code for java.util.zip.CRC32::updateBytes() intrinsic.
  void kernel_crc32(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3);
  // CRC32 code for java.util.zip.CRC32C::updateBytes() intrinsic.
  void kernel_crc32c(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3);

  // Stack push and pop individual 64 bit registers
  void push(Register src);
  void pop(Register dst);

  void repne_scan(Register addr, Register value, Register count,
                  Register scratch);
  void repne_scanw(Register addr, Register value, Register count,
                   Register scratch);

  typedef void (MacroAssembler::* add_sub_imm_insn)(Register Rd, Register Rn, unsigned imm);
  typedef void (MacroAssembler::* add_sub_reg_insn)(Register Rd, Register Rn, Register Rm, enum shift_kind kind, unsigned shift);

  // If a constant does not fit in an immediate field, generate some
  // number of MOV instructions and then perform the operation
  void wrap_add_sub_imm_insn(Register Rd, Register Rn, uint64_t imm,
                             add_sub_imm_insn insn1,
                             add_sub_reg_insn insn2, bool is32);
  // Separate vsn which sets the flags
  void wrap_adds_subs_imm_insn(Register Rd, Register Rn, uint64_t imm,
                               add_sub_imm_insn insn1,
                               add_sub_reg_insn insn2, bool is32);

#define WRAP(INSN, is32)                                                \
  void INSN(Register Rd, Register Rn, uint64_t imm) {                   \
    wrap_add_sub_imm_insn(Rd, Rn, imm, &Assembler::INSN, &Assembler::INSN, is32); \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
             enum shift_kind kind, unsigned shift = 0) {                \
    Assembler::INSN(Rd, Rn, Rm, kind, shift);                           \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm) {                    \
    Assembler::INSN(Rd, Rn, Rm);                                        \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
           ext::operation option, int amount = 0) {                     \
    Assembler::INSN(Rd, Rn, Rm, option, amount);                        \
  }

  WRAP(add, false) WRAP(addw, true) WRAP(sub, false) WRAP(subw, true)

#undef WRAP
#define WRAP(INSN, is32)                                                \
  void INSN(Register Rd, Register Rn, uint64_t imm) {                   \
    wrap_adds_subs_imm_insn(Rd, Rn, imm, &Assembler::INSN, &Assembler::INSN, is32); \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
             enum shift_kind kind, unsigned shift = 0) {                \
    Assembler::INSN(Rd, Rn, Rm, kind, shift);                           \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm) {                    \
    Assembler::INSN(Rd, Rn, Rm);                                        \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
           ext::operation option, int amount = 0) {                     \
    Assembler::INSN(Rd, Rn, Rm, option, amount);                        \
  }

  WRAP(adds, false) WRAP(addsw, true) WRAP(subs, false) WRAP(subsw, true)

  void add(Register Rd, Register Rn, RegisterOrConstant increment);
  void addw(Register Rd, Register Rn, RegisterOrConstant increment);
  void sub(Register Rd, Register Rn, RegisterOrConstant decrement);
  void subw(Register Rd, Register Rn, RegisterOrConstant decrement);

  void adrp(Register reg1, const Address &dest, uint64_t &byte_offset);

  void verified_entry(Compile* C, int sp_inc);

  // Inline type specific methods
  #include "asm/macroAssembler_common.hpp"

  int store_inline_type_fields_to_buf(ciInlineKlass* vk, bool from_interpreter = true);
  bool move_helper(VMReg from, VMReg to, BasicType bt, RegState reg_state[]);
  bool unpack_inline_helper(const GrowableArray<SigEntry>* sig, int& sig_index,
                            VMReg from, int& from_index, VMRegPair* to, int to_count, int& to_index,
                            RegState reg_state[]);
  bool pack_inline_helper(const GrowableArray<SigEntry>* sig, int& sig_index, int vtarg_index,
                          VMRegPair* from, int from_count, int& from_index, VMReg to,
                          RegState reg_state[], Register val_array);
  int extend_stack_for_inline_args(int args_on_stack);
  void remove_frame(int initial_framesize, bool needs_stack_repair);
  VMReg spill_reg_for(VMReg reg);
  void save_stack_increment(int sp_inc, int frame_size);

  void tableswitch(Register index, jint lowbound, jint highbound,
                   Label &jumptable, Label &jumptable_end, int stride = 1) {
    adr(rscratch1, jumptable);
    subsw(rscratch2, index, lowbound);
    subsw(zr, rscratch2, highbound - lowbound);
    br(Assembler::HS, jumptable_end);
    add(rscratch1, rscratch1, rscratch2,
        ext::sxtw, exact_log2(stride * Assembler::instruction_size));
    br(rscratch1);
  }

  // Form an address from base + offset in Rd.  Rd may or may not
  // actually be used: you must use the Address that is returned.  It
  // is up to you to ensure that the shift provided matches the size
  // of your data.
  Address form_address(Register Rd, Register base, int64_t byte_offset, int shift);

  // Return true iff an address is within the 48-bit AArch64 address
  // space.
  bool is_valid_AArch64_address(address a) {
    return ((uint64_t)a >> 48) == 0;
  }

  // Load the base of the cardtable byte map into reg.
  void load_byte_map_base(Register reg);

  // Prolog generator routines to support switch between x86 code and
  // generated ARM code

  // routine to generate an x86 prolog for a stub function which
  // bootstraps into the generated ARM code which directly follows the
  // stub
  //

  public:

  address read_polling_page(Register r, relocInfo::relocType rtype);
  void get_polling_page(Register dest, relocInfo::relocType rtype);

  // CRC32 code for java.util.zip.CRC32::updateBytes() intrinsic.
  void update_byte_crc32(Register crc, Register val, Register table);
  void update_word_crc32(Register crc, Register v, Register tmp,
        Register table0, Register table1, Register table2, Register table3,
        bool upper = false);

  address count_positives(Register ary1, Register len, Register result);

  address arrays_equals(Register a1, Register a2, Register result, Register cnt1,
                        Register tmp1, Register tmp2, Register tmp3, int elem_size);

// Ensure that the inline code and the stub use the same registers.
#define ARRAYS_HASHCODE_REGISTERS \
  do {                      \
    assert(result == r0  && \
           ary    == r1  && \
           cnt    == r2  && \
           vdata0 == v3  && \
           vdata1 == v2  && \
           vdata2 == v1  && \
           vdata3 == v0  && \
           vmul0  == v4  && \
           vmul1  == v5  && \
           vmul2  == v6  && \
           vmul3  == v7  && \
           vpow   == v12 && \
           vpowm  == v13, "registers must match aarch64.ad"); \
  } while (0)

  void string_equals(Register a1, Register a2, Register result, Register cnt1);

  void fill_words(Register base, Register cnt, Register value);
  void fill_words(Register base, uint64_t cnt, Register value);

  address zero_words(Register base, uint64_t cnt);
  address zero_words(Register ptr, Register cnt);
  void zero_dcache_blocks(Register base, Register cnt);

  static const int zero_words_block_size;

  address byte_array_inflate(Register src, Register dst, Register len,
                             FloatRegister vtmp1, FloatRegister vtmp2,
                             FloatRegister vtmp3, Register tmp4);

  void char_array_compress(Register src, Register dst, Register len,
                           Register res,
                           FloatRegister vtmp0, FloatRegister vtmp1,
                           FloatRegister vtmp2, FloatRegister vtmp3,
                           FloatRegister vtmp4, FloatRegister vtmp5);

  void encode_iso_array(Register src, Register dst,
                        Register len, Register res, bool ascii,
                        FloatRegister vtmp0, FloatRegister vtmp1,
                        FloatRegister vtmp2, FloatRegister vtmp3,
                        FloatRegister vtmp4, FloatRegister vtmp5);

  void generate_dsin_dcos(bool isCos, address npio2_hw, address two_over_pi,
      address pio2, address dsin_coef, address dcos_coef);
 private:
  // begin trigonometric functions support block
  void generate__ieee754_rem_pio2(address npio2_hw, address two_over_pi, address pio2);
  void generate__kernel_rem_pio2(address two_over_pi, address pio2);
  void generate_kernel_sin(FloatRegister x, bool iyIsOne, address dsin_coef);
  void generate_kernel_cos(FloatRegister x, address dcos_coef);
  // end trigonometric functions support block
  void add2_with_carry(Register final_dest_hi, Register dest_hi, Register dest_lo,
                       Register src1, Register src2);
  void add2_with_carry(Register dest_hi, Register dest_lo, Register src1, Register src2) {
    add2_with_carry(dest_hi, dest_hi, dest_lo, src1, src2);
  }
  void multiply_64_x_64_loop(Register x, Register xstart, Register x_xstart,
                             Register y, Register y_idx, Register z,
                             Register carry, Register product,
                             Register idx, Register kdx);
  void multiply_128_x_128_loop(Register y, Register z,
                               Register carry, Register carry2,
                               Register idx, Register jdx,
                               Register yz_idx1, Register yz_idx2,
                               Register tmp, Register tmp3, Register tmp4,
                               Register tmp7, Register product_hi);
  void kernel_crc32_using_crypto_pmull(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
  void kernel_crc32_using_crc32(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
  void kernel_crc32c_using_crypto_pmull(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
  void kernel_crc32c_using_crc32c(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
  void kernel_crc32_common_fold_using_crypto_pmull(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        size_t table_offset);

  void ghash_modmul (FloatRegister result,
                     FloatRegister result_lo, FloatRegister result_hi, FloatRegister b,
                     FloatRegister a, FloatRegister vzr, FloatRegister a1_xor_a0, FloatRegister p,
                     FloatRegister t1, FloatRegister t2, FloatRegister t3);
  void ghash_load_wide(int index, Register data, FloatRegister result, FloatRegister state);
public:
  void multiply_to_len(Register x, Register xlen, Register y, Register ylen, Register z,
                       Register tmp0, Register tmp1, Register tmp2, Register tmp3,
                       Register tmp4, Register tmp5, Register tmp6, Register tmp7);
  void mul_add(Register out, Register in, Register offs, Register len, Register k);
  void ghash_multiply(FloatRegister result_lo, FloatRegister result_hi,
                      FloatRegister a, FloatRegister b, FloatRegister a1_xor_a0,
                      FloatRegister tmp1, FloatRegister tmp2, FloatRegister tmp3);
  void ghash_multiply_wide(int index,
                           FloatRegister result_lo, FloatRegister result_hi,
                           FloatRegister a, FloatRegister b, FloatRegister a1_xor_a0,
                           FloatRegister tmp1, FloatRegister tmp2, FloatRegister tmp3);
  void ghash_reduce(FloatRegister result, FloatRegister lo, FloatRegister hi,
                    FloatRegister p, FloatRegister z, FloatRegister t1);
  void ghash_reduce_wide(int index, FloatRegister result, FloatRegister lo, FloatRegister hi,
                    FloatRegister p, FloatRegister z, FloatRegister t1);
  void ghash_processBlocks_wide(address p, Register state, Register subkeyH,
                                Register data, Register blocks, int unrolls);


  void aesenc_loadkeys(Register key, Register keylen);
  void aesecb_encrypt(Register from, Register to, Register keylen,
                      FloatRegister data = v0, int unrolls = 1);
  void aesecb_decrypt(Register from, Register to, Register key, Register keylen);
  void aes_round(FloatRegister input, FloatRegister subkey);

  // ChaCha20 functions support block
  void cc20_qr_add4(FloatRegister (&addFirst)[4],
          FloatRegister (&addSecond)[4]);
  void cc20_qr_xor4(FloatRegister (&firstElem)[4],
          FloatRegister (&secondElem)[4], FloatRegister (&result)[4]);
  void cc20_qr_lrot4(FloatRegister (&sourceReg)[4],
          FloatRegister (&destReg)[4], int bits, FloatRegister table);
  void cc20_set_qr_registers(FloatRegister (&vectorSet)[4],
          const FloatRegister (&stateVectors)[16], int idx1, int idx2,
          int idx3, int idx4);

  // Place an ISB after code may have been modified due to a safepoint.
  void safepoint_isb();

private:
  // Return the effective address r + (r1 << ext) + offset.
  // Uses rscratch2.
  Address offsetted_address(Register r, Register r1, Address::extend ext,
                            int offset, int size);

private:
  // Returns an address on the stack which is reachable with a ldr/str of size
  // Uses rscratch2 if the address is not directly reachable
  Address spill_address(int size, int offset, Register tmp=rscratch2);
  Address sve_spill_address(int sve_reg_size_in_bytes, int offset, Register tmp=rscratch2);

  bool merge_alignment_check(Register base, size_t size, int64_t cur_offset, int64_t prev_offset) const;

  // Check whether two loads/stores can be merged into ldp/stp.
  bool ldst_can_merge(Register rx, const Address &adr, size_t cur_size_in_bytes, bool is_store) const;

  // Merge current load/store with previous load/store into ldp/stp.
  void merge_ldst(Register rx, const Address &adr, size_t cur_size_in_bytes, bool is_store);

  // Try to merge two loads/stores into ldp/stp. If success, returns true else false.
  bool try_merge_ldst(Register rt, const Address &adr, size_t cur_size_in_bytes, bool is_store);

public:
  void spill(Register Rx, bool is64, int offset) {
    if (is64) {
      str(Rx, spill_address(8, offset));
    } else {
      strw(Rx, spill_address(4, offset));
    }
  }
  void spill(FloatRegister Vx, SIMD_RegVariant T, int offset) {
    str(Vx, T, spill_address(1 << (int)T, offset));
  }

  void spill_sve_vector(FloatRegister Zx, int offset, int vector_reg_size_in_bytes) {
    sve_str(Zx, sve_spill_address(vector_reg_size_in_bytes, offset));
  }
  void spill_sve_predicate(PRegister pr, int offset, int predicate_reg_size_in_bytes) {
    sve_str(pr, sve_spill_address(predicate_reg_size_in_bytes, offset));
  }

  void unspill(Register Rx, bool is64, int offset) {
    if (is64) {
      ldr(Rx, spill_address(8, offset));
    } else {
      ldrw(Rx, spill_address(4, offset));
    }
  }
  void unspill(FloatRegister Vx, SIMD_RegVariant T, int offset) {
    ldr(Vx, T, spill_address(1 << (int)T, offset));
  }

  void unspill_sve_vector(FloatRegister Zx, int offset, int vector_reg_size_in_bytes) {
    sve_ldr(Zx, sve_spill_address(vector_reg_size_in_bytes, offset));
  }
  void unspill_sve_predicate(PRegister pr, int offset, int predicate_reg_size_in_bytes) {
    sve_ldr(pr, sve_spill_address(predicate_reg_size_in_bytes, offset));
  }

  void spill_copy128(int src_offset, int dst_offset,
                     Register tmp1=rscratch1, Register tmp2=rscratch2) {
    if (src_offset < 512 && (src_offset & 7) == 0 &&
        dst_offset < 512 && (dst_offset & 7) == 0) {
      ldp(tmp1, tmp2, Address(sp, src_offset));
      stp(tmp1, tmp2, Address(sp, dst_offset));
    } else {
      unspill(tmp1, true, src_offset);
      spill(tmp1, true, dst_offset);
      unspill(tmp1, true, src_offset+8);
      spill(tmp1, true, dst_offset+8);
    }
  }
  void spill_copy_sve_vector_stack_to_stack(int src_offset, int dst_offset,
                                            int sve_vec_reg_size_in_bytes) {
    assert(sve_vec_reg_size_in_bytes % 16 == 0, "unexpected sve vector reg size");
    for (int i = 0; i < sve_vec_reg_size_in_bytes / 16; i++) {
      spill_copy128(src_offset, dst_offset);
      src_offset += 16;
      dst_offset += 16;
    }
  }
  void spill_copy_sve_predicate_stack_to_stack(int src_offset, int dst_offset,
                                               int sve_predicate_reg_size_in_bytes) {
    sve_ldr(ptrue, sve_spill_address(sve_predicate_reg_size_in_bytes, src_offset));
    sve_str(ptrue, sve_spill_address(sve_predicate_reg_size_in_bytes, dst_offset));
    reinitialize_ptrue();
  }
  void cache_wb(Address line);
  void cache_wbsync(bool is_pre);

  // Code for java.lang.Thread::onSpinWait() intrinsic.
  void spin_wait();

  void lightweight_lock(Register basic_lock, Register obj, Register t1, Register t2, Register t3, Label& slow);
  void lightweight_unlock(Register obj, Register t1, Register t2, Register t3, Label& slow);

private:
  // Check the current thread doesn't need a cross modify fence.
  void verify_cross_modify_fence_not_required() PRODUCT_RETURN;

};

#ifdef ASSERT
inline bool AbstractAssembler::pd_check_instruction_mark() { return false; }
#endif

struct tableswitch {
  Register _reg;
  int _insn_index; jint _first_key; jint _last_key;
  Label _after;
  Label _branches;
};

#endif // CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP
