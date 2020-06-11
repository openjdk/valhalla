/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILERDEFINITIONS_HPP
#define SHARE_COMPILER_COMPILERDEFINITIONS_HPP

#include "memory/allocation.hpp"

// The (closed set) of concrete compiler classes.
enum CompilerType {
  compiler_none,
  compiler_c1,
  compiler_c2,
  compiler_jvmci,
  compiler_number_of_types
};

extern const char* compilertype2name_tab[compiler_number_of_types];     // Map CompilerType to its name
inline const char* compilertype2name(CompilerType t) { return (uint)t < compiler_number_of_types ? compilertype2name_tab[t] : NULL; }

// Handy constants for deciding which compiler mode to use.
enum MethodCompilation {
  InvocationEntryBci   = -1,     // i.e., not a on-stack replacement compilation
  BeforeBci            = InvocationEntryBci,
  AfterBci             = -2,
  UnwindBci            = -3,
  AfterExceptionBci    = -4,
  UnknownBci           = -5,
  InvalidFrameStateBci = -6
};

// Enumeration to distinguish tiers of compilation
enum CompLevel {
  CompLevel_any               = -2,
  CompLevel_all               = -2,
  CompLevel_aot               = -1,
  CompLevel_none              = 0,         // Interpreter
  CompLevel_simple            = 1,         // C1
  CompLevel_limited_profile   = 2,         // C1, invocation & backedge counters
  CompLevel_full_profile      = 3,         // C1, invocation & backedge counters + mdo
  CompLevel_full_optimization = 4          // C2 or JVMCI
};

#ifdef TIERED
class CompilationModeFlag : AllStatic {
  static bool _quick_only;
  static bool _high_only;
  static bool _high_only_quick_internal;

public:
  static bool initialize();
  static bool normal()                   { return !quick_only() && !high_only() && !high_only_quick_internal(); }
  static bool quick_only()               { return _quick_only;               }
  static bool high_only()                { return _high_only;                }
  static bool high_only_quick_internal() { return _high_only_quick_internal; }

  static bool disable_intermediate()     { return high_only() || high_only_quick_internal(); }
  static bool quick_internal()           { return !high_only(); }

  static void set_high_only_quick_internal(bool x) { _high_only_quick_internal = x; }
};
#endif

extern CompLevel CompLevel_highest_tier;

enum CompMode {
  CompMode_none = 0,
  CompMode_client = 1,
  CompMode_server = 2
};

extern CompMode Compilation_mode;

inline bool is_server_compilation_mode_vm() {
  return Compilation_mode == CompMode_server;
}

inline bool is_client_compilation_mode_vm() {
  return Compilation_mode == CompMode_client;
}

inline bool is_c1_compile(int comp_level) {
  return comp_level > CompLevel_none && comp_level < CompLevel_full_optimization;
}

inline bool is_c2_compile(int comp_level) {
  return comp_level == CompLevel_full_optimization;
}

inline bool is_highest_tier_compile(int comp_level) {
  return comp_level == CompLevel_highest_tier;
}

inline bool is_compile(int comp_level) {
  return is_c1_compile(comp_level) || is_c2_compile(comp_level);
}

bool is_c1_or_interpreter_only();

// States of Restricted Transactional Memory usage.
enum RTMState {
  NoRTM      = 0x2, // Don't use RTM
  UseRTM     = 0x1, // Use RTM
  ProfileRTM = 0x0  // Use RTM with abort ratio calculation
};

#ifndef INCLUDE_RTM_OPT
#define INCLUDE_RTM_OPT 0
#endif
#if INCLUDE_RTM_OPT
#define RTM_OPT_ONLY(code) code
#else
#define RTM_OPT_ONLY(code)
#endif

class CompilerConfig : public AllStatic {
public:
  // Scale compile thresholds
  // Returns threshold scaled with CompileThresholdScaling
  static intx scaled_compile_threshold(intx threshold, double scale);
  static intx scaled_compile_threshold(intx threshold);

  // Returns freq_log scaled with CompileThresholdScaling
  static intx scaled_freq_log(intx freq_log, double scale);
  static intx scaled_freq_log(intx freq_log);

  static bool check_args_consistency(bool status);

  static void ergo_initialize();

private:
  TIERED_ONLY(static void set_tiered_flags();)
};

#endif // SHARE_COMPILER_COMPILERDEFINITIONS_HPP
