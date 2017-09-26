/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/systemDictionary.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayKlass.hpp"
#include "runtime/fieldType.hpp"
#include "runtime/signature.hpp"

void FieldType::skip_optional_size(Symbol* signature, int* index) {
  jchar c = signature->byte_at(*index);
  while (c >= '0' && c <= '9') {
    *index = *index + 1;
    c = signature->byte_at(*index);
  }
}

BasicType FieldType::basic_type(Symbol* signature) {
  return char2type(signature->byte_at(0));
}

// Check if it is a valid array signature
bool FieldType::is_valid_array_signature(Symbol* sig) {
  assert(sig->utf8_length() > 1, "this should already have been checked");
  assert(sig->byte_at(0) == '[', "this should already have been checked");
  // The first character is already checked
  int i = 1;
  int len = sig->utf8_length();
  // First skip all '['s
  while(i < len - 1 && sig->byte_at(i) == '[') i++;

  // Check type
  switch(sig->byte_at(i)) {
    case 'B': // T_BYTE
    case 'C': // T_CHAR
    case 'D': // T_DOUBLE
    case 'F': // T_FLOAT
    case 'I': // T_INT
    case 'J': // T_LONG
    case 'S': // T_SHORT
    case 'Z': // T_BOOLEAN
      // If it is an array, the type is the last character
      return (i + 1 == len);
    case 'L':
    case 'Q':
      // If it is an object or a value type, the last character must be a ';'
      return sig->byte_at(len - 1) == ';';
  }

  return false;
}

static const char dvt_postfix[] = "$Value";
static const int dvt_postfix_len = 6;

bool FieldType::is_dvt_postfix(Symbol* signature) {
  assert(strlen(dvt_postfix) == dvt_postfix_len, "Invariant");
  int sig_length = signature->utf8_length();
  int pos = sig_length - dvt_postfix_len;
  if (pos <= 0) {
    return false;
  }
  for (int i = 0; i < dvt_postfix_len; i++) {
    if (signature->byte_at(pos) != dvt_postfix[i]) {
      return false;
    }
    pos++;
  }
  return true;
}

char* FieldType::dvt_unmangle_vcc(Symbol* signature) {
  assert(is_dvt_postfix(signature), "Unmangle that which is not managled");
  char* str = signature->as_C_string();
  str[signature->utf8_length() -dvt_postfix_len] = '\0';
  return str;
}

BasicType FieldType::get_array_info(Symbol* signature, FieldArrayInfo& fd, TRAPS) {
  assert(basic_type(signature) == T_ARRAY, "must be array");
  int index = 1;
  int dim   = 1;
  skip_optional_size(signature, &index);
  while (signature->byte_at(index) == '[') {
    index++;
    dim++;
    skip_optional_size(signature, &index);
  }
  ResourceMark rm;
  char *element = signature->as_C_string() + index;
  BasicType element_type = char2type(element[0]);
  if (element_type == T_OBJECT || element_type == T_VALUETYPE) {
    int len = (int)strlen(element);
    assert(element[len-1] == ';', "last char should be a semicolon");
    element[len-1] = '\0';        // chop off semicolon
    fd._object_key = SymbolTable::new_symbol(element + 1, CHECK_(T_BYTE));
  }
  // Pass dimension back to caller
  fd._dimension = dim;
  return element_type;
}
