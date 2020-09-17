/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTLIST_HPP
#define SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTLIST_HPP

#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"

/*
 * Here we have a mechanism for extracting constraints (as custom functions) for flags,
 * which otherwise can not be expressed via simple range check, specified in flag macro tables.
 *
 * An example of a constraint is "flag1 < flag2" where both flag1 and flag2 can change.
 *
 * See runtime "runtime/flags/jvmFlagConstraintsCompiler.hpp",
 * "runtime/flags/jvmFlagConstraintsGC.hpp" and
 * "runtime/flags/jvmFlagConstraintsRuntime.hpp" for the functions themselves.
 */

typedef JVMFlag::Error (*JVMFlagConstraintFunc_bool)(bool value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_int)(int value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_intx)(intx value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uint)(uint value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uintx)(uintx value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uint64_t)(uint64_t value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_size_t)(size_t value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_double)(double value, bool verbose);

class JVMFlagConstraint : public CHeapObj<mtArguments> {
public:
  // During VM initialization, constraint validation will be done order of ConstraintType.
  enum ConstraintType {
    // Will be validated during argument processing (Arguments::parse_argument).
    AtParse         = 0,
    // Will be validated inside Threads::create_vm(), right after Arguments::apply_ergo().
    AfterErgo       = 1,
    // Will be validated inside universe_init(), right after Metaspace::global_initialize().
    AfterMemoryInit = 2
  };

protected:
  const JVMFlag* const _flag;

private:
  ConstraintType _validate_type;

public:
  // the "name" argument must be a string literal
  JVMFlagConstraint(const JVMFlag* flag, ConstraintType type) : _flag(flag), _validate_type(type) {}
  ~JVMFlagConstraint() {}
  const JVMFlag* flag() const { return _flag; }
  ConstraintType type() const { return _validate_type; }
  virtual JVMFlag::Error apply(bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_bool(bool value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_int(int value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_intx(intx value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_uint(uint value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_uintx(uintx value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_uint64_t(uint64_t value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_size_t(size_t value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
  virtual JVMFlag::Error apply_double(double value, bool verbose = true) { ShouldNotReachHere(); return JVMFlag::ERR_OTHER; };
};

class JVMFlagConstraintChecker {
  const JVMFlag* _flag;
  const JVMFlagLimit* _limit;

public:
  JVMFlagConstraintChecker(const JVMFlag* flag, const JVMFlagLimit* limit) : _flag(flag), _limit(limit) {}
  bool exists() const { return _limit != NULL; }
  JVMFlag::Error apply(bool verbose = true) const;

  JVMFlagConstraint::ConstraintType type() const { return (JVMFlagConstraint::ConstraintType)_limit->phase(); }
#define DECLARE_CONSTRAINT_APPLY(T) JVMFlag::Error apply_ ## T(T new_value, bool verbose = true) const;
  ALL_CONSTRAINT_TYPES(DECLARE_CONSTRAINT_APPLY)
};

class JVMFlagConstraintList : public AllStatic {
private:
  // Latest constraint validation type.
  static JVMFlagConstraint::ConstraintType _validating_type;
public:
  static void init();
  static JVMFlagConstraintChecker find(const JVMFlag* flag) { return JVMFlagConstraintChecker(flag, JVMFlagLimit::get_constraint(flag)); }
  static JVMFlagConstraintChecker find_if_needs_check(const JVMFlag* flag);
  // True if 'AfterErgo' or later constraint functions are validated.
  static bool validated_after_ergo() { return _validating_type >= JVMFlagConstraint::AfterErgo; };
  static bool check_constraints(JVMFlagConstraint::ConstraintType type);
};

#endif // SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTLIST_HPP
