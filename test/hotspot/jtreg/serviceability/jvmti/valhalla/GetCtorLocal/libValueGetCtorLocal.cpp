/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "jni.h"
#include "jvmti_common.hpp"

#ifdef __cplusplus
extern "C" {
#endif

static jvmtiEnv *jvmti = nullptr;
static jobject cached_this = nullptr;

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  jint res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
  if (res != JNI_OK || jvmti == nullptr) {
    LOG("GetEnv failed, res = %d", (int)res);
    return JNI_ERR;
  }

  jvmtiCapabilities caps;
  memset(&caps, 0, sizeof(caps));
  caps.can_access_local_variables = 1;
  jvmtiError err = jvmti->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    LOG("AddCapabilities failed: %s (%d)\n", TranslateError(err), err);
    return JNI_ERR;
  }

  return JNI_OK;
}

static void log_value(JNIEnv *jni, jobject value) {
  jclass cls = jni->GetObjectClass(value);
  if (cls == nullptr) {
    fatal(jni, "Failed: value class is nullptr\n");
    return;
  }

  char* sig = nullptr;
  check_jvmti_error(jvmti->GetClassSignature(cls, &sig, nullptr), "GetClassSignature");

  LOG(" - the value class: %s\n", sig);
  jvmti->Deallocate((unsigned char *)sig);
}

static jobject get_local(JNIEnv *jni, jthread thread, jint depth, jint slot) {
  LOG("GetLocalObject for slot %d\n", (int)slot);
  jobject value = nullptr;
  check_jvmti_error(jvmti->GetLocalObject(thread, depth, slot, &value), "GetLocalObject");

  log_value(jni, value);

  return value;
}

static jobject get_this(JNIEnv *jni, jthread thread, jint depth) {
  LOG("GetLocalInstance\n");
  jobject value = nullptr;
  check_jvmti_error(jvmti->GetLocalInstance(thread, depth, &value), "GetLocalInstance");

  log_value(jni, value);

  return value;
}

JNIEXPORT jboolean JNICALL
Java_ValueGetCtorLocal_nTestCtorThis(JNIEnv *jni, jclass thisClass, jthread thread) {
  const jint depth = 1;

  jmethodID method = get_frame_method(jvmti, jni, thread, depth);
  char* mname = get_method_name(jvmti, jni, method);

  LOG("\nnTestCtorThis: frame method: %s\n", mname);
  jobject obj0 = get_local(jni, thread, depth, 0);
  jobject obj_this = get_this(jni, thread, depth);

  // obj0 is expected to be equal to "this"
  jboolean result = jni->IsSameObject(obj0, obj_this);
  LOG("nTestCtorThis: obj0: %p obj_this: %p objects are equal: %d\n",
      (void*)obj0, (void*)obj_this, result);

  if (!result) {
    fatal(jni, "Failed: obj0 != obj_this\n");
  }
  if (cached_this == nullptr) { // first call to nTestCtorThis
    cached_this = jni->NewGlobalRef(obj_this);
  } else {
    // cached_this must be a snapshot that not mutate with the ctor changes
    if (jni->IsSameObject(cached_this, obj_this)) {
      fatal(jni, "Failed: unexpected: cached_this == obj_this\n");
    }
  }
  deallocate(jvmti, jni, (void*)mname);
  return result;
}

#ifdef __cplusplus
}
#endif
