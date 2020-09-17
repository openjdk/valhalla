/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_JVMFLAG_HPP
#define SHARE_RUNTIME_FLAGS_JVMFLAG_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class outputStream;

// function type that will construct default range string
typedef const char* (*RangeStrFunc)(void);

struct JVMFlag {
  enum Flags : int {
    // latest value origin
    DEFAULT          = 0,
    COMMAND_LINE     = 1,
    ENVIRON_VAR      = 2,
    CONFIG_FILE      = 3,
    MANAGEMENT       = 4,
    ERGONOMIC        = 5,
    ATTACH_ON_DEMAND = 6,
    INTERNAL         = 7,
    JIMAGE_RESOURCE  = 8,

    LAST_VALUE_ORIGIN = JIMAGE_RESOURCE,
    VALUE_ORIGIN_BITS = 4,
    VALUE_ORIGIN_MASK = right_n_bits(VALUE_ORIGIN_BITS),

    // flag kind
    KIND_PRODUCT            = 1 << 4,
    KIND_MANAGEABLE         = 1 << 5,
    KIND_DIAGNOSTIC         = 1 << 6,
    KIND_EXPERIMENTAL       = 1 << 7,
    KIND_NOT_PRODUCT        = 1 << 8,
    KIND_DEVELOP            = 1 << 9,
    KIND_PLATFORM_DEPENDENT = 1 << 10,
    KIND_C1                 = 1 << 11,
    KIND_C2                 = 1 << 12,
    KIND_ARCH               = 1 << 13,
    KIND_LP64_PRODUCT       = 1 << 14,
    KIND_JVMCI              = 1 << 15,

    // set this bit if the flag was set on the command line
    ORIG_COMMAND_LINE       = 1 << 17,

    KIND_MASK = ~(VALUE_ORIGIN_MASK | ORIG_COMMAND_LINE)
  };

  enum Error {
    // no error
    SUCCESS = 0,
    // flag name is missing
    MISSING_NAME,
    // flag value is missing
    MISSING_VALUE,
    // error parsing the textual form of the value
    WRONG_FORMAT,
    // flag is not writable
    NON_WRITABLE,
    // flag value is outside of its bounds
    OUT_OF_BOUNDS,
    // flag value violates its constraint
    VIOLATES_CONSTRAINT,
    // there is no flag with the given name
    INVALID_FLAG,
    // the flag can only be set only on command line during invocation of the VM
    COMMAND_LINE_ONLY,
    // the flag may only be set once
    SET_ONLY_ONCE,
    // the flag is not writable in this combination of product/debug build
    CONSTANT,
    // other, unspecified error related to setting the flag
    ERR_OTHER
  };

  enum MsgType {
    NONE = 0,
    DIAGNOSTIC_FLAG_BUT_LOCKED,
    EXPERIMENTAL_FLAG_BUT_LOCKED,
    DEVELOPER_FLAG_BUT_PRODUCT_BUILD,
    NOTPRODUCT_FLAG_BUT_PRODUCT_BUILD
  };

  const char* _type;
  const char* _name;
  void* _addr;
  Flags _flags;
  NOT_PRODUCT(const char* _doc;)

  // points to all Flags static array
  static JVMFlag* flags;

  // number of flags
  static size_t numFlags;

private:
  static JVMFlag* find_flag(const char* name, size_t length, bool allow_locked, bool return_flag);

public:
  constexpr JVMFlag() : _type(), _name(), _addr(), _flags() NOT_PRODUCT(COMMA _doc()) {}

  constexpr JVMFlag(int flag_enum, const char* type, const char* name,
                    void* addr, int flags, int extra_flags, const char* doc);

  constexpr JVMFlag(int flag_enum,  const char* type, const char* name,
                    void* addr, int flags, const char* doc);

  static JVMFlag* find_flag(const char* name) {
    return find_flag(name, strlen(name), false, false);
  }
  static JVMFlag* find_declared_flag(const char* name, size_t length) {
    return find_flag(name, length, true, true);
  }
  static JVMFlag* find_declared_flag(const char* name) {
    return find_declared_flag(name, strlen(name));
  }

  static JVMFlag* fuzzy_match(const char* name, size_t length, bool allow_locked = false);

  static const char* get_int_default_range_str();
  static const char* get_uint_default_range_str();
  static const char* get_intx_default_range_str();
  static const char* get_uintx_default_range_str();
  static const char* get_uint64_t_default_range_str();
  static const char* get_size_t_default_range_str();
  static const char* get_double_default_range_str();

  static void assert_valid_flag_enum(int i) NOT_DEBUG_RETURN;
  static void check_all_flag_declarations() NOT_DEBUG_RETURN;

  inline int flag_enum() const {
    int i = this - JVMFlag::flags;
    assert_valid_flag_enum(i);
    return i;
  }

  static JVMFlag* flag_from_enum(int flag_enum) {
    assert_valid_flag_enum(flag_enum);
    return &JVMFlag::flags[flag_enum];
  }

  bool is_bool() const;
  bool get_bool() const                       { return *((bool*) _addr); }
  void set_bool(bool value) const             { *((bool*) _addr) = value; }

  bool is_int() const;
  int get_int() const                         { return *((int*) _addr); }
  void set_int(int value) const               { *((int*) _addr) = value; }

  bool is_uint() const;
  uint get_uint() const                       { return *((uint*) _addr); }
  void set_uint(uint value) const             { *((uint*) _addr) = value; }

  bool is_intx() const;
  intx get_intx() const                       { return *((intx*) _addr); }
  void set_intx(intx value) const             { *((intx*) _addr) = value; }

  bool is_uintx() const;
  uintx get_uintx() const                     { return *((uintx*) _addr); }
  void set_uintx(uintx value) const           { *((uintx*) _addr) = value; }

  bool is_uint64_t() const;
  uint64_t get_uint64_t() const               { return *((uint64_t*) _addr); }
  void set_uint64_t(uint64_t value) const     { *((uint64_t*) _addr) = value; }

  bool is_size_t() const;
  size_t get_size_t() const                   { return *((size_t*) _addr); }
  void set_size_t(size_t value) const         { *((size_t*) _addr) = value; }

  bool is_double() const;
  double get_double() const                   { return *((double*) _addr); }
  void set_double(double value) const         { *((double*) _addr) = value; }

  bool is_ccstr() const;
  bool ccstr_accumulates() const;
  ccstr get_ccstr() const                     { return *((ccstr*) _addr); }
  void set_ccstr(ccstr value) const           { *((ccstr*) _addr) = value; }

  Flags get_origin() const;
  void set_origin(Flags origin);

  bool is_default() const;
  bool is_ergonomic() const;
  bool is_jimage_resource() const;
  bool is_command_line() const;
  void set_command_line();

  bool is_product() const;
  bool is_manageable() const;
  bool is_diagnostic() const;
  bool is_experimental() const;
  bool is_notproduct() const;
  bool is_develop() const;

  bool is_constant_in_binary() const;

  bool is_unlocker() const;
  bool is_unlocked() const;
  bool is_writeable() const;
  bool is_external() const;

  void clear_diagnostic();
  void clear_experimental();
  void set_product();

  JVMFlag::MsgType get_locked_message(char*, int) const;
  JVMFlag::MsgType get_locked_message_ext(char*, int) const;

  // printRanges will print out flags type, name and range values as expected by -XX:+PrintFlagsRanges
  void print_on(outputStream* st, bool withComments = false, bool printRanges = false) const;
  void print_kind(outputStream* st, unsigned int width) const;
  void print_origin(outputStream* st, unsigned int width) const;
  void print_as_flag(outputStream* st) const;

  static const char* flag_error_str(JVMFlag::Error error);

public:
  static JVMFlag::Error boolAt(const JVMFlag* flag, bool* value);
  static JVMFlag::Error boolAtPut(JVMFlag* flag, bool* value, JVMFlag::Flags origin);

  static JVMFlag::Error intAt(const JVMFlag* flag, int* value);
  static JVMFlag::Error intAtPut(JVMFlag* flag, int* value, JVMFlag::Flags origin);

  static JVMFlag::Error uintAt(const JVMFlag* flag, uint* value);
  static JVMFlag::Error uintAtPut(JVMFlag* flag, uint* value, JVMFlag::Flags origin);

  static JVMFlag::Error intxAt(const JVMFlag* flag, intx* value);
  static JVMFlag::Error intxAtPut(JVMFlag* flag, intx* value, JVMFlag::Flags origin);

  static JVMFlag::Error uintxAt(const JVMFlag* flag, uintx* value);
  static JVMFlag::Error uintxAtPut(JVMFlag* flag, uintx* value, JVMFlag::Flags origin);

  static JVMFlag::Error size_tAt(const JVMFlag* flag, size_t* value);
  static JVMFlag::Error size_tAtPut(JVMFlag* flag, size_t* value, JVMFlag::Flags origin);

  static JVMFlag::Error uint64_tAt(const JVMFlag* flag, uint64_t* value);
  static JVMFlag::Error uint64_tAtPut(JVMFlag* flag, uint64_t* value, JVMFlag::Flags origin);

  static JVMFlag::Error doubleAt(const JVMFlag* flag, double* value);
  static JVMFlag::Error doubleAtPut(JVMFlag* flag, double* value, JVMFlag::Flags origin);

  static JVMFlag::Error ccstrAt(const JVMFlag* flag, ccstr* value);
  // Contract:  JVMFlag will make private copy of the incoming value.
  // Outgoing value is always malloc-ed, and caller MUST call free.
  static JVMFlag::Error ccstrAtPut(JVMFlag* flag, ccstr* value, JVMFlag::Flags origin);

  static void printSetFlags(outputStream* out);

  // printRanges will print out flags type, name and range values as expected by -XX:+PrintFlagsRanges
  static void printFlags(outputStream* out, bool withComments, bool printRanges = false, bool skipDefaults = false);
  static void printError(bool verbose, const char* msg, ...) ATTRIBUTE_PRINTF(2, 3);

  static void verify() PRODUCT_RETURN;
};

#define DECLARE_CONSTRAINT(type, func) JVMFlag::Error func(type value, bool verbose);

#endif // SHARE_RUNTIME_FLAGS_JVMFLAG_HPP
