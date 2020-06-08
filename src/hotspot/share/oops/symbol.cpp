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


#include "precompiled.hpp"
#include "classfile/altHashing.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/metaspaceShared.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/symbol.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "utilities/utf8.hpp"

uint32_t Symbol::pack_hash_and_refcount(short hash, int refcount) {
  STATIC_ASSERT(PERM_REFCOUNT == ((1 << 16) - 1));
  assert(refcount >= 0, "negative refcount");
  assert(refcount <= PERM_REFCOUNT, "invalid refcount");
  uint32_t hi = hash;
  uint32_t lo = refcount;
  return (hi << 16) | lo;
}

Symbol::Symbol(const u1* name, int length, int refcount) {
  _hash_and_refcount =  pack_hash_and_refcount((short)os::random(), refcount);
  _length = length;
  _body[0] = 0;  // in case length == 0
  for (int i = 0; i < length; i++) {
    byte_at_put(i, name[i]);
  }
}

void* Symbol::operator new(size_t sz, int len) throw() {
#if INCLUDE_CDS
 if (DumpSharedSpaces) {
    // To get deterministic output from -Xshare:dump, we ensure that Symbols are allocated in
    // increasing addresses. When the symbols are copied into the archive, we preserve their
    // relative address order (see SortedSymbolClosure in metaspaceShared.cpp)
    //
    // We cannot use arena because arena chunks are allocated by the OS. As a result, for example,
    // the archived symbol of "java/lang/Object" may sometimes be lower than "java/lang/String", and
    // sometimes be higher. This would cause non-deterministic contents in the archive.
   DEBUG_ONLY(static void* last = 0);
   void* p = (void*)MetaspaceShared::symbol_space_alloc(size(len)*wordSize);
   assert(p > last, "must increase monotonically");
   DEBUG_ONLY(last = p);
   return p;
 }
#endif
  int alloc_size = size(len)*wordSize;
  address res = (address) AllocateHeap(alloc_size, mtSymbol);
  return res;
}

void* Symbol::operator new(size_t sz, int len, Arena* arena) throw() {
  int alloc_size = size(len)*wordSize;
  address res = (address)arena->Amalloc_4(alloc_size);
  return res;
}

void Symbol::operator delete(void *p) {
  assert(((Symbol*)p)->refcount() == 0, "should not call this");
  FreeHeap(p);
}

#if INCLUDE_CDS
void Symbol::update_identity_hash() {
  // This is called at a safepoint during dumping of a static CDS archive. The caller should have
  // called os::init_random() with a deterministic seed and then iterate all archived Symbols in
  // a deterministic order.
  assert(SafepointSynchronize::is_at_safepoint(), "must be at a safepoint");
  _hash_and_refcount =  pack_hash_and_refcount((short)os::random(), PERM_REFCOUNT);
}

void Symbol::set_permanent() {
  // This is called at a safepoint during dumping of a dynamic CDS archive.
  assert(SafepointSynchronize::is_at_safepoint(), "must be at a safepoint");
  _hash_and_refcount =  pack_hash_and_refcount(extract_hash(_hash_and_refcount), PERM_REFCOUNT);
}
#endif

bool Symbol::is_Q_signature() const {
  int len = utf8_length();
  return len > 2 && char_at(0) == JVM_SIGNATURE_VALUETYPE && char_at(len - 1) == JVM_SIGNATURE_ENDCLASS;
}

bool Symbol::is_Q_array_signature() const {
  int l = utf8_length();
  if (l < 2 || char_at(0) != JVM_SIGNATURE_ARRAY || char_at(l - 1) != JVM_SIGNATURE_ENDCLASS) {
    return false;
  }
  for (int i = 1; i < (l - 2); i++) {
    char c = char_at(i);
    if (c == JVM_SIGNATURE_VALUETYPE) {
      return true;
    }
    if (c != JVM_SIGNATURE_ARRAY) {
      return false;
    }
  }
  return false;
}

bool Symbol::is_Q_method_signature() const {
  assert(SignatureVerifier::is_valid_method_signature(this), "must be");
  int len = utf8_length();
  if (len > 4 && char_at(0) == JVM_SIGNATURE_FUNC) {
    for (int i=1; i<len-3; i++) { // Must end with ")Qx;", where x is at least one character or more.
      if (char_at(i) == JVM_SIGNATURE_ENDFUNC && char_at(i+1) == JVM_SIGNATURE_VALUETYPE) {
        return true;
      }
    }
  }
  return false;
}

bool Symbol::is_Q_singledim_array_signature() const {
  int len = utf8_length();
  return len > 3 && char_at(0) == JVM_SIGNATURE_ARRAY && char_at(1) == JVM_SIGNATURE_VALUETYPE &&
                    char_at(len - 1) == JVM_SIGNATURE_ENDCLASS;
}

Symbol* Symbol::fundamental_name(TRAPS) {
  if ((char_at(0) == JVM_SIGNATURE_VALUETYPE || char_at(0) == JVM_SIGNATURE_CLASS) && ends_with(JVM_SIGNATURE_ENDCLASS)) {
    return SymbolTable::new_symbol(this, 1, utf8_length() - 1);
  } else {
    // reference count is incremented to be consistent with the behavior with
    // the SymbolTable::new_symbol() call above
    this->increment_refcount();
    return this;
  }
}

bool Symbol::is_same_fundamental_type(Symbol* s) const {
  if (this == s) return true;
  if (utf8_length() < 3) return false;
  int offset1, offset2, len;
  if (ends_with(JVM_SIGNATURE_ENDCLASS)) {
    if (char_at(0) != JVM_SIGNATURE_VALUETYPE && char_at(0) != JVM_SIGNATURE_CLASS) return false;
    offset1 = 1;
    len = utf8_length() - 2;
  } else {
    offset1 = 0;
    len = utf8_length();
  }
  if (ends_with(JVM_SIGNATURE_ENDCLASS)) {
    if (s->char_at(0) != JVM_SIGNATURE_VALUETYPE && s->char_at(0) != JVM_SIGNATURE_CLASS) return false;
    offset2 = 1;
  } else {
    offset2 = 0;
  }
  if ((offset2 + len) > s->utf8_length()) return false;
  if ((utf8_length() - offset1 * 2) != (s->utf8_length() - offset2 * 2))
    return false;
  int l = len;
  while (l-- > 0) {
    if (char_at(offset1 + l) != s->char_at(offset2 + l))
      return false;
  }
  return true;
}

// ------------------------------------------------------------------
// Symbol::index_of
//
// Finds if the given string is a substring of this symbol's utf8 bytes.
// Return -1 on failure.  Otherwise return the first index where str occurs.
int Symbol::index_of_at(int i, const char* str, int len) const {
  assert(i >= 0 && i <= utf8_length(), "oob");
  if (len <= 0)  return 0;
  char first_char = str[0];
  address bytes = (address) ((Symbol*)this)->base();
  address limit = bytes + utf8_length() - len;  // inclusive limit
  address scan = bytes + i;
  if (scan > limit)
    return -1;
  for (; scan <= limit; scan++) {
    scan = (address) memchr(scan, first_char, (limit + 1 - scan));
    if (scan == NULL)
      return -1;  // not found
    assert(scan >= bytes+i && scan <= limit, "scan oob");
    if (len <= 2
        ? (char) scan[len-1] == str[len-1]
        : memcmp(scan+1, str+1, len-1) == 0) {
      return (int)(scan - bytes);
    }
  }
  return -1;
}


char* Symbol::as_C_string(char* buf, int size) const {
  if (size > 0) {
    int len = MIN2(size - 1, utf8_length());
    for (int i = 0; i < len; i++) {
      buf[i] = char_at(i);
    }
    buf[len] = '\0';
  }
  return buf;
}

char* Symbol::as_C_string() const {
  int len = utf8_length();
  char* str = NEW_RESOURCE_ARRAY(char, len + 1);
  return as_C_string(str, len + 1);
}

void Symbol::print_utf8_on(outputStream* st) const {
  st->print("%s", as_C_string());
}

void Symbol::print_symbol_on(outputStream* st) const {
  char *s;
  st = st ? st : tty;
  {
    // ResourceMark may not affect st->print(). If st is a string
    // stream it could resize, using the same resource arena.
    ResourceMark rm;
    s = as_quoted_ascii();
    s = os::strdup(s);
  }
  if (s == NULL) {
    st->print("(null)");
  } else {
    st->print("%s", s);
    os::free(s);
  }
}

char* Symbol::as_quoted_ascii() const {
  const char *ptr = (const char *)&_body[0];
  int quoted_length = UTF8::quoted_ascii_length(ptr, utf8_length());
  char* result = NEW_RESOURCE_ARRAY(char, quoted_length + 1);
  UTF8::as_quoted_ascii(ptr, utf8_length(), result, quoted_length + 1);
  return result;
}

jchar* Symbol::as_unicode(int& length) const {
  Symbol* this_ptr = (Symbol*)this;
  length = UTF8::unicode_length((char*)this_ptr->bytes(), utf8_length());
  jchar* result = NEW_RESOURCE_ARRAY(jchar, length);
  if (length > 0) {
    UTF8::convert_to_unicode((char*)this_ptr->bytes(), result, length);
  }
  return result;
}

const char* Symbol::as_klass_external_name(char* buf, int size) const {
  if (size > 0) {
    char* str    = as_C_string(buf, size);
    int   length = (int)strlen(str);
    // Turn all '/'s into '.'s (also for array klasses)
    for (int index = 0; index < length; index++) {
      if (str[index] == JVM_SIGNATURE_SLASH) {
        str[index] = JVM_SIGNATURE_DOT;
      }
    }
    return str;
  } else {
    return buf;
  }
}

const char* Symbol::as_klass_external_name() const {
  char* str    = as_C_string();
  int   length = (int)strlen(str);
  // Turn all '/'s into '.'s (also for array klasses)
  for (int index = 0; index < length; index++) {
    if (str[index] == JVM_SIGNATURE_SLASH) {
      str[index] = JVM_SIGNATURE_DOT;
    }
  }
  return str;
}

static void print_class(outputStream *os, const SignatureStream& ss) {
  int sb = ss.raw_symbol_begin(), se = ss.raw_symbol_end();
  for (int i = sb; i < se; ++i) {
    int ch = ss.raw_char_at(i);
    if (ch == JVM_SIGNATURE_SLASH) {
      os->put(JVM_SIGNATURE_DOT);
    } else {
      os->put(ch);
    }
  }
}

static void print_array(outputStream *os, SignatureStream& ss) {
  int dimensions = ss.skip_array_prefix();
  assert(dimensions > 0, "");
  if (ss.is_reference()) {
    print_class(os, ss);
  } else {
    os->print("%s", type2name(ss.type()));
  }
  for (int i = 0; i < dimensions; ++i) {
    os->print("[]");
  }
}

void Symbol::print_as_signature_external_return_type(outputStream *os) {
  for (SignatureStream ss(this); !ss.is_done(); ss.next()) {
    if (ss.at_return_type()) {
      if (ss.is_array()) {
        print_array(os, ss);
      } else if (ss.is_reference()) {
        print_class(os, ss);
      } else {
        os->print("%s", type2name(ss.type()));
      }
    }
  }
}

void Symbol::print_as_signature_external_parameters(outputStream *os) {
  bool first = true;
  for (SignatureStream ss(this); !ss.is_done(); ss.next()) {
    if (ss.at_return_type()) break;
    if (!first) { os->print(", "); }
    if (ss.is_array()) {
      print_array(os, ss);
    } else if (ss.is_reference()) {
      print_class(os, ss);
    } else {
      os->print("%s", type2name(ss.type()));
    }
    first = false;
  }
}

// Increment refcount while checking for zero.  If the Symbol's refcount becomes zero
// a thread could be concurrently removing the Symbol.  This is used during SymbolTable
// lookup to avoid reviving a dead Symbol.
bool Symbol::try_increment_refcount() {
  uint32_t found = _hash_and_refcount;
  while (true) {
    uint32_t old_value = found;
    int refc = extract_refcount(old_value);
    if (refc == PERM_REFCOUNT) {
      return true;  // sticky max or created permanent
    } else if (refc == 0) {
      return false; // dead, can't revive.
    } else {
      found = Atomic::cmpxchg(&_hash_and_refcount, old_value, old_value + 1);
      if (found == old_value) {
        return true; // successfully updated.
      }
      // refcount changed, try again.
    }
  }
}

// The increment_refcount() is called when not doing lookup. It is assumed that you
// have a symbol with a non-zero refcount and it can't become zero while referenced by
// this caller.
void Symbol::increment_refcount() {
  if (!try_increment_refcount()) {
#ifdef ASSERT
    print();
    fatal("refcount has gone to zero");
#endif
  }
#ifndef PRODUCT
  if (refcount() != PERM_REFCOUNT) { // not a permanent symbol
    NOT_PRODUCT(Atomic::inc(&_total_count);)
  }
#endif
}

// Decrement refcount potentially while racing increment, so we need
// to check the value after attempting to decrement so that if another
// thread increments to PERM_REFCOUNT the value is not decremented.
void Symbol::decrement_refcount() {
  uint32_t found = _hash_and_refcount;
  while (true) {
    uint32_t old_value = found;
    int refc = extract_refcount(old_value);
    if (refc == PERM_REFCOUNT) {
      return;  // refcount is permanent, permanent is sticky
    } else if (refc == 0) {
#ifdef ASSERT
      print();
      fatal("refcount underflow");
#endif
      return;
    } else {
      found = Atomic::cmpxchg(&_hash_and_refcount, old_value, old_value - 1);
      if (found == old_value) {
        return;  // successfully updated.
      }
      // refcount changed, try again.
    }
  }
}

void Symbol::make_permanent() {
  uint32_t found = _hash_and_refcount;
  while (true) {
    uint32_t old_value = found;
    int refc = extract_refcount(old_value);
    if (refc == PERM_REFCOUNT) {
      return;  // refcount is permanent, permanent is sticky
    } else if (refc == 0) {
#ifdef ASSERT
      print();
      fatal("refcount underflow");
#endif
      return;
    } else {
      int hash = extract_hash(old_value);
      found = Atomic::cmpxchg(&_hash_and_refcount, old_value, pack_hash_and_refcount(hash, PERM_REFCOUNT));
      if (found == old_value) {
        return;  // successfully updated.
      }
      // refcount changed, try again.
    }
  }
}

void Symbol::metaspace_pointers_do(MetaspaceClosure* it) {
  if (log_is_enabled(Trace, cds)) {
    LogStream trace_stream(Log(cds)::trace());
    trace_stream.print("Iter(Symbol): %p ", this);
    print_value_on(&trace_stream);
    trace_stream.cr();
  }
}

void Symbol::print_on(outputStream* st) const {
  st->print("Symbol: '");
  print_symbol_on(st);
  st->print("'");
  st->print(" count %d", refcount());
}

void Symbol::print() const { print_on(tty); }

// The print_value functions are present in all builds, to support the
// disassembler and error reporting.
void Symbol::print_value_on(outputStream* st) const {
  st->print("'");
  for (int i = 0; i < utf8_length(); i++) {
    st->print("%c", char_at(i));
  }
  st->print("'");
}

void Symbol::print_value() const { print_value_on(tty); }

bool Symbol::is_valid(Symbol* s) {
  if (!is_aligned(s, sizeof(MetaWord))) return false;
  if ((size_t)s < os::min_page_size()) return false;

  if (!os::is_readable_range(s, s + 1)) return false;

  // Symbols are not allocated in Java heap.
  if (Universe::heap()->is_in(s)) return false;

  int len = s->utf8_length();
  if (len < 0) return false;

  jbyte* bytes = (jbyte*) s->bytes();
  return os::is_readable_range(bytes, bytes + len);
}

void Symbol::print_Qvalue_on(outputStream* st) const {
  if (this == NULL) {
    st->print("NULL");
  } else {
    st->print("'Q");
    for (int i = 0; i < utf8_length(); i++) {
      st->print("%c", char_at(i));
    }
    st->print(";'");
  }
}

// SymbolTable prints this in its statistics
NOT_PRODUCT(size_t Symbol::_total_count = 0;)
