/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OOPS_VALUEKLASS_HPP
#define SHARE_VM_OOPS_VALUEKLASS_HPP

#include "classfile/javaClasses.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"

// A ValueKlass is a specialized InstanceKlass for value types.


class ValueKlass: public InstanceKlass {
  friend class VMStructs;
  friend class InstanceKlass;

 private:

  // Constructor
  ValueKlass(const ClassFileParser& parser)
    : InstanceKlass(parser, InstanceKlass::_misc_kind_value_type, InstanceKlass::ID) {
    _adr_valueklass_fixed_block = valueklass_static_block();
    // Addresses used for value type calling convention
    *((Array<SigEntry>**)adr_extended_sig()) = NULL;
    *((Array<VMRegPair>**)adr_return_regs()) = NULL;
    *((address*)adr_pack_handler()) = NULL;
    *((address*)adr_unpack_handler()) = NULL;
    assert(pack_handler() == NULL, "pack handler not null");
    *((int*)adr_default_value_offset()) = 0;
    set_prototype_header(markOopDesc::always_locked_prototype());
  }

  ValueKlassFixedBlock* valueklass_static_block() const {
    address adr_jf = adr_value_fields_klasses();
    if (adr_jf != NULL) {
      return (ValueKlassFixedBlock*)(adr_jf + this->java_fields_count() * sizeof(Klass*));
    }

    address adr_fing = adr_fingerprint();
    if (adr_fing != NULL) {
      return (ValueKlassFixedBlock*)(adr_fingerprint() + sizeof(u8));
    }

    InstanceKlass** adr_host = adr_unsafe_anonymous_host();
    if (adr_host != NULL) {
      return (ValueKlassFixedBlock*)(adr_host + 1);
    }

    Klass** adr_impl = adr_implementor();
    if (adr_impl != NULL) {
      return (ValueKlassFixedBlock*)(adr_impl + 1);
    }

    return (ValueKlassFixedBlock*)end_of_nonstatic_oop_maps();
  }

  address adr_extended_sig() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _extended_sig));
  }

  address adr_return_regs() const {
    ValueKlassFixedBlock* vkst = valueklass_static_block();
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _return_regs));
  }

  // pack and unpack handlers for value types return
  address adr_pack_handler() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _pack_handler));
  }

  address adr_unpack_handler() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _unpack_handler));
  }

  address pack_handler() const {
    return *(address*)adr_pack_handler();
  }

  address unpack_handler() const {
    return *(address*)adr_unpack_handler();
  }

  address adr_default_value_offset() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(default_value_offset_offset());
  }

  int collect_fields(GrowableArray<SigEntry>* sig, int base_off = 0) const;

  void cleanup_blobs();

 protected:
  // Returns the array class for the n'th dimension
  Klass* array_klass_impl(bool or_null, int n, TRAPS);

  // Returns the array class with this class as element type
  Klass* array_klass_impl(bool or_null, TRAPS);

 public:
  // Type testing
  bool is_value_slow() const        { return true; }

  oop value_mirror() const {
    return java_lang_Class::value_mirror(java_mirror());
  }
  /*
   * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

  package compiler.valhalla.valuetypes;

  import jdk.test.lib.Asserts;

  import java.lang.reflect.Method;

  /*
   * @test
   * @summary Test value type calling convention optimizations
   * @library /testlibrary /test/lib /compiler/whitebox /
   * @requires os.simpleArch == "x64"
   * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator -XDallowFlattenabilityModifiers TestCallingConvention.java
   * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
   * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
   *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla
   *                               compiler.valhalla.valuetypes.ValueTypeTest
   *                               compiler.valhalla.valuetypes.TestCallingConvention
   */
  public class TestCallingConvention extends ValueTypeTest {
      // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
      @Override
      public String[] getExtraVMParameters(int scenario) {
          switch (scenario) {
          case 3: return new String[] {"-XX:-ValueArrayFlatten"};
          }
          return null;
      }

      public static void main(String[] args) throws Throwable {
          TestCallingConvention test = new TestCallingConvention();
          test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue4.class);
      }

      // Test interpreter to compiled code with various signatures
      @Test(failOn = ALLOC + STORE + TRAP)
      public long test1(MyValue2 v) {
          return v.hash();
      }

      @DontCompile
      public void test1_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test1(v);
          Asserts.assertEQ(result, v.hashInterpreted());
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test2(int i1, MyValue2 v, int i2) {
          return v.hash() + i1 - i2;
      }

      @DontCompile
      public void test2_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test2(rI, v, 2*rI);
          Asserts.assertEQ(result, v.hashInterpreted() - rI);
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test3(long l1, MyValue2 v, long l2) {
          return v.hash() + l1 - l2;
      }

      @DontCompile
      public void test3_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test3(rL, v, 2*rL);
          Asserts.assertEQ(result, v.hashInterpreted() - rL);
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test4(int i, MyValue2 v, long l) {
          return v.hash() + i + l;
      }

      @DontCompile
      public void test4_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test4(rI, v, rL);
          Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test5(long l, MyValue2 v, int i) {
          return v.hash() + i + l;
      }

      @DontCompile
      public void test5_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test5(rL, v, rI);
          Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test6(long l, MyValue1 v1, int i, MyValue2 v2) {
          return v1.hash() + i + l + v2.hash();
      }

      @DontCompile
      public void test6_verifier(boolean warmup) {
          MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
          MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
          long result = test6(rL, v1, rI, v2);
          Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
      }

      // Test compiled code to interpreter with various signatures
      @DontCompile
      public long test7_interp(MyValue2 v) {
          return v.hash();
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test7(MyValue2 v) {
          return test7_interp(v);
      }

      @DontCompile
      public void test7_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test7(v);
          Asserts.assertEQ(result, v.hashInterpreted());
      }

      @DontCompile
      public long test8_interp(int i1, MyValue2 v, int i2) {
          return v.hash() + i1 - i2;
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test8(int i1, MyValue2 v, int i2) {
          return test8_interp(i1, v, i2);
      }

      @DontCompile
      public void test8_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test8(rI, v, 2*rI);
          Asserts.assertEQ(result, v.hashInterpreted() - rI);
      }

      @DontCompile
      public long test9_interp(long l1, MyValue2 v, long l2) {
          return v.hash() + l1 - l2;
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test9(long l1, MyValue2 v, long l2) {
          return test9_interp(l1, v, l2);
      }

      @DontCompile
      public void test9_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test9(rL, v, 2*rL);
          Asserts.assertEQ(result, v.hashInterpreted() - rL);
      }

      @DontCompile
      public long test10_interp(int i, MyValue2 v, long l) {
          return v.hash() + i + l;
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test10(int i, MyValue2 v, long l) {
          return test10_interp(i, v, l);
      }

      @DontCompile
      public void test10_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test10(rI, v, rL);
          Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
      }

      @DontCompile
      public long test11_interp(long l, MyValue2 v, int i) {
          return v.hash() + i + l;
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test11(long l, MyValue2 v, int i) {
          return test11_interp(l, v, i);
      }

      @DontCompile
      public void test11_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          long result = test11(rL, v, rI);
          Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
      }

      @DontCompile
      public long test12_interp(long l, MyValue1 v1, int i, MyValue2 v2) {
          return v1.hash() + i + l + v2.hash();
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test12(long l, MyValue1 v1, int i, MyValue2 v2) {
          return test12_interp(l, v1, i, v2);
      }

      @DontCompile
      public void test12_verifier(boolean warmup) {
          MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
          MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
          long result = test12(rL, v1, rI, v2);
          Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
      }

      // Test that debug info at a call is correct
      @DontCompile
      public long test13_interp(MyValue2 v, MyValue1[] va, boolean deopt) {
          if (deopt) {
              // uncommon trap
              WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test13"));
          }
          return v.hash() + va[0].hash() + va[1].hash();
      }

      @Test(failOn = ALLOC + STORE + TRAP)
      public long test13(MyValue2 v, MyValue1[] va, boolean flag, long l) {
          return test13_interp(v, va, flag) + l;
      }

      @DontCompile
      public void test13_verifier(boolean warmup) {
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          MyValue1[] va = new MyValue1[2];
          va[0] = MyValue1.createWithFieldsDontInline(rI, rL);
          va[1] = MyValue1.createWithFieldsDontInline(rI, rL);
          long result = test13(v, va, !warmup, rL);
          Asserts.assertEQ(result, v.hashInterpreted() + va[0].hash() + va[1].hash() + rL);
      }

      // Test deoptimization at call return with return value in registers
      @DontCompile
      public MyValue2 test14_interp(boolean deopt) {
          if (deopt) {
              // uncommon trap
              WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test14"));
          }
          return MyValue2.createWithFieldsInline(rI, true);
      }

      @Test()
      public MyValue2 test14(boolean flag) {
          return test14_interp(flag);
      }

      @DontCompile
      public void test14_verifier(boolean warmup) {
          MyValue2 result = test14(!warmup);
          MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
          Asserts.assertEQ(result.hash(), v.hash());
      }

      // Return value types in registers from interpreter -> compiled
      final MyValue3 test15_vt = MyValue3.create();
      @DontCompile
      public MyValue3 test15_interp() {
          return test15_vt;
      }

      MyValue3 test15_vt2;
      @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
      @Test(valid = ValueTypeReturnedAsFieldsOff)
      public void test15() {
          test15_vt2 = test15_interp();
      }

      @DontCompile
      public void test15_verifier(boolean warmup) {
          test15();
          test15_vt.verify(test15_vt2);
      }

      // Return value types in registers from compiled -> interpreter
      final MyValue3 test16_vt = MyValue3.create();
      @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + TRAP)
      @Test(valid = ValueTypeReturnedAsFieldsOff)
      public MyValue3 test16() {
          return test16_vt;
      }

      @DontCompile
      public void test16_verifier(boolean warmup) {
          MyValue3 vt = test16();
          test16_vt.verify(vt);
      }

      // Return value types in registers from compiled -> compiled
      final MyValue3 test17_vt = MyValue3.create();
      @DontInline
      public MyValue3 test17_comp() {
          return test17_vt;
      }

      MyValue3 test17_vt2;
      @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
      @Test(valid = ValueTypeReturnedAsFieldsOff)
      public void test17() {
          test17_vt2 = test17_comp();
      }

      @DontCompile
      public void test17_verifier(boolean warmup) throws Exception {
          Method helper_m = getClass().getDeclaredMethod("test17_comp");
          if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
              WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
              Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test17_comp not compiled");
          }
          test17();
          test17_vt.verify(test17_vt2);
      }

      // Same tests as above but with a value type that cannot be returned in registers

      // Return value types in registers from interpreter -> compiled
      final MyValue4 test18_vt = MyValue4.create();
      @DontCompile
      public MyValue4 test18_interp() {
          return test18_vt;
      }

      MyValue4 test18_vt2;
      @Test
      public void test18() {
          test18_vt2 = test18_interp();
      }

      @DontCompile
      public void test18_verifier(boolean warmup) {
          test18();
          test18_vt.verify(test18_vt2);
      }

      // Return value types in registers from compiled -> interpreter
      final MyValue4 test19_vt = MyValue4.create();
      @Test
      public MyValue4 test19() {
          return test19_vt;
      }

      @DontCompile
      public void test19_verifier(boolean warmup) {
          MyValue4 vt = test19();
          test19_vt.verify(vt);
      }

      // Return value types in registers from compiled -> compiled
      final MyValue4 test20_vt = MyValue4.create();
      @DontInline
      public MyValue4 test20_comp() {
          return test20_vt;
      }

      MyValue4 test20_vt2;
      @Test
      public void test20() {
          test20_vt2 = test20_comp();
      }

      @DontCompile
      public void test20_verifier(boolean warmup) throws Exception {
          Method helper_m = getClass().getDeclaredMethod("test20_comp");
          if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
              WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
              Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test20_comp not compiled");
          }
          test20();
          test20_vt.verify(test20_vt2);
      }

      // Test no result from inlined method for incremental inlining
      final MyValue3 test21_vt = MyValue3.create();
      public MyValue3 test21_inlined() {
          throw new RuntimeException();
      }

      @Test
      public MyValue3 test21() {
          try {
              return test21_inlined();
          } catch (RuntimeException ex) {
              return test21_vt;
          }
      }

      @DontCompile
      public void test21_verifier(boolean warmup) {
          MyValue3 vt = test21();
          test21_vt.verify(vt);
      }

      // Test returning a non-flattened value type as fields
      MyValue3.box test22_vt = MyValue3.create();

      @Test
      public MyValue3 test22() {
          return test22_vt;
      }

      @DontCompile
      public void test22_verifier(boolean warmup) {
          MyValue3 vt = test22();
          test22_vt.verify(vt);
      }

      // Test calling a method that has circular register/stack dependencies when unpacking value type arguments
      value class TestValue23 {
          final double f1;
          TestValue23(double val) {
              f1 = val;
          }
      }

      static double test23Callee(int i1, int i2, int i3, int i4, int i5, int i6,
                                 TestValue23 v1, TestValue23 v2, TestValue23 v3, TestValue23 v4, TestValue23 v5, TestValue23 v6, TestValue23 v7, TestValue23 v8,
                                 double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
          return i1 + i2 + i3 + i4 + i5 + i6 + v1.f1 + v2.f1 + v3.f1 + v4.f1 + v5.f1 + v6.f1 + v7.f1 + v8.f1 + d1 + d2 + d3 + d4 + d5 + d6 + d7 + d8;
      }

      @Test
      public double test23(int i1, int i2, int i3, int i4, int i5, int i6,
                           TestValue23 v1, TestValue23 v2, TestValue23 v3, TestValue23 v4, TestValue23 v5, TestValue23 v6, TestValue23 v7, TestValue23 v8,
                           double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
          return test23Callee(i1, i2, i3, i4, i5, i6,
                              v1, v2, v3, v4, v5, v6, v7, v8,
                              d1, d2, d3, d4, d5, d6, d7, d8);
      }

      @DontCompile
      public void test23_verifier(boolean warmup) {
          TestValue23 vt = new TestValue23(rI);
          double res1 = test23(rI, rI, rI, rI, rI, rI,
                              vt, vt, vt, vt, vt, vt, vt, vt,
                              rI, rI, rI, rI, rI, rI, rI, rI);
          double res2 = test23Callee(rI, rI, rI, rI, rI, rI,
                                     vt, vt, vt, vt, vt, vt, vt, vt,
                                     rI, rI, rI, rI, rI, rI, rI, rI);
          double res3 = 6*rI + 8*rI + 8*rI;
          Asserts.assertEQ(res1, res2);
          Asserts.assertEQ(res2, res3);
      }
  }
  // Casting from Klass*
  static ValueKlass* cast(Klass* k) {
    assert(k->is_value(), "cast to ValueKlass");
    return (ValueKlass*) k;
  }

  // Use this to return the size of an instance in heap words
  // Implementation is currently simple because all value types are allocated
  // in Java heap like Java objects.
  virtual int size_helper() const {
    return layout_helper_to_size_helper(layout_helper());
  }

  // allocate_instance() allocates a stand alone value in the Java heap
  instanceOop allocate_instance(TRAPS);

  // minimum number of bytes occupied by nonstatic fields, HeapWord aligned or pow2
  int raw_value_byte_size() const;

  int first_field_offset() const;

  address data_for_oop(oop o) const {
    return ((address) (void*) o) + first_field_offset();
  }

  oop oop_for_data(address data) const {
    oop o = (oop) (data - first_field_offset());
    assert(oopDesc::is_oop(o, false), "Not an oop");
    return o;
  }

  // Query if h/w provides atomic load/store
  bool is_atomic();

  bool flatten_array();

  bool contains_oops() const { return nonstatic_oop_map_count() > 0; }
  int nonstatic_oop_count();

  // Prototype general store methods...

  // copy the fields, with no concern for GC barriers
  void raw_field_copy(void* src, void* dst, size_t raw_byte_size);

  void value_store(void* src, void* dst, bool dst_is_heap, bool dst_uninitialized) {
    value_store(src, dst, nonstatic_field_size() << LogBytesPerHeapOop, dst_is_heap, dst_uninitialized);
  }

  // store the value of this klass contained with src into dst, raw data ptr
  void value_store(void* src, void* dst, size_t raw_byte_size, bool dst_is_heap, bool dst_uninitialized);

  // GC support...

  void iterate_over_inside_oops(OopClosure* f, oop value);

  // oop iterate raw value type data pointer (where oop_addr may not be an oop, but backing/array-element)
  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized(const address oop_addr, OopClosureType* closure);

  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized_bounded(const address oop_addr, OopClosureType* closure, void* lo, void* hi);

  // calling convention support
  void initialize_calling_convention(TRAPS);
  Array<SigEntry>* extended_sig() const {
    return *((Array<SigEntry>**)adr_extended_sig());
  }
  Array<VMRegPair>* return_regs() const {
    return *((Array<VMRegPair>**)adr_return_regs());
  }
  bool can_be_returned_as_fields() const;
  void save_oop_fields(const RegisterMap& map, GrowableArray<Handle>& handles) const;
  void restore_oop_results(RegisterMap& map, GrowableArray<Handle>& handles) const;
  oop realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS);
  static ValueKlass* returned_value_klass(const RegisterMap& reg_map);

  // pack and unpack handlers. Need to be loadable from generated code
  // so at a fixed offset from the base of the klass pointer.
  static ByteSize pack_handler_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _pack_handler);
  }

  static ByteSize unpack_handler_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _unpack_handler);
  }

  static ByteSize default_value_offset_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _default_value_offset);
  }

  void set_default_value_offset(int offset) {
    *((int*)adr_default_value_offset()) = offset;
  }

  int default_value_offset() {
    int offset = *((int*)adr_default_value_offset());
    assert(offset != 0, "must not be called if not initialized");
    return offset;
  }

  void set_default_value(oop val) {
    java_mirror()->obj_field_put(default_value_offset(), val);
  }

  oop default_value() {
    oop val = java_mirror()->obj_field_acquire(default_value_offset());
    assert(oopDesc::is_oop(val), "Sanity check");
    assert(val->is_value(), "Sanity check");
    assert(val->klass() == this, "sanity check");
    return val;
  }

  void deallocate_contents(ClassLoaderData* loader_data);
  static void cleanup(ValueKlass* ik) ;

  // Verification
  void verify_on(outputStream* st);
  void oop_verify_on(oop obj, outputStream* st);

};

#endif /* SHARE_VM_OOPS_VALUEKLASS_HPP */
