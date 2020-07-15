/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_SIGNATURE_CC_HPP
#define SHARE_RUNTIME_SIGNATURE_CC_HPP

// Handling of scalarized Calling Convention
#include "runtime/signature.hpp"

class ScalarizedValueArgsStream : public StackObj {
  const GrowableArray<SigEntry>* _sig_cc;
  int _sig_cc_index;
  const VMRegPair* _regs_cc;
  int _regs_cc_count;
  int _regs_cc_index;
  int _vt;
  DEBUG_ONLY(bool _finished);
public:
  ScalarizedValueArgsStream(const GrowableArray<SigEntry>* sig_cc, int sig_cc_index, VMRegPair* regs_cc, int regs_cc_count, int regs_cc_index) :
    _sig_cc(sig_cc), _sig_cc_index(sig_cc_index), _regs_cc(regs_cc), _regs_cc_count(regs_cc_count), _regs_cc_index(regs_cc_index) {
    assert(_sig_cc->at(_sig_cc_index)._bt == T_INLINE_TYPE, "should be at end delimiter");
    _vt = 1;
    DEBUG_ONLY(_finished = false);
  }

  bool next(VMRegPair& pair, BasicType& bt) {
    assert(!_finished, "sanity");
    do {
      _sig_cc_index++;
      bt = _sig_cc->at(_sig_cc_index)._bt;
      if (bt == T_INLINE_TYPE) {
        _vt++;
      } else if (bt == T_VOID &&
                 _sig_cc->at(_sig_cc_index-1)._bt != T_LONG &&
                 _sig_cc->at(_sig_cc_index-1)._bt != T_DOUBLE) {
        _vt--;
      } else if (SigEntry::is_reserved_entry(_sig_cc, _sig_cc_index)) {
        _regs_cc_index++;
      } else {
        assert(_regs_cc_index < _regs_cc_count, "must be");
        pair = _regs_cc[_regs_cc_index++];
        VMReg r1 = pair.first();
        VMReg r2 = pair.second();

        if (!r1->is_valid()) {
          assert(!r2->is_valid(), "must be invalid");
        } else {
          return true;
        }
      }
    } while (_vt != 0);

    DEBUG_ONLY(_finished = true);
    return false;
  }

  int sig_cc_index() {return _sig_cc_index;}
  int regs_cc_index() {return _regs_cc_index;}
};

#endif // SHARE_RUNTIME_SIGNATURE_CC_HPP
