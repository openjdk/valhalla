/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2013, 2023 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_TEMPLATETABLE_PPC_HPP
#define CPU_PPC_TEMPLATETABLE_PPC_HPP

  static void prepare_invoke(Register Rcache, Register Rret_addr, Register Rrecv, Register Rscratch);
  static void invokevfinal_helper(Register Rcache, Register Rscratch1, Register Rscratch2, Register Rscratch3, Register Rscratch4);
  static void generate_vtable_call(Register Rrecv_klass, Register Rindex, Register Rret, Register Rtemp);
  static void invokeinterface_object_method(Register Rrecv_klass, Register Rret, Register Rflags, Register Rcache, Register Rtemp, Register Rtemp2);

  // Branch_conditional which takes TemplateTable::Condition.
  static void branch_conditional(ConditionRegister crx, TemplateTable::Condition cc, Label& L, bool invert = false);
  static void if_cmp_common(Register Rfirst, Register Rsecond, Register Rscratch1, Register Rscratch2, Condition cc, bool is_jint, bool cmp0);

#endif // CPU_PPC_TEMPLATETABLE_PPC_HPP
