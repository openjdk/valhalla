/*
 * Copyright (c) 2007, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <stdio.h>
#include <time.h>
#include "ExceptionCheckingJniEnv.hpp"
#include "jni_tools.h"

extern "C" {

static jfieldID objFieldId = nullptr;

/*
 * Class:     nsk_share_gc_lock_jni_DoubleArrayCriticalLocker
 * Method:    criticalNative
 * Signature: ([Z)Z
 */
JNIEXPORT jdouble JNICALL Java_nsk_share_gc_lock_jni_DoubleArrayCriticalLocker_criticalNative
(JNIEnv *jni_env, jobject o, jlong enterTime, jlong sleepTime) {
  ExceptionCheckingJniEnvPtr ec_jni(jni_env);

  jsize size, i;
  jdoubleArray arr;
  jdouble *pa;
  jdouble hash = 0;
  time_t start_time, current_time;

  if (objFieldId == nullptr) {
    jclass klass = ec_jni->GetObjectClass(o, TRACE_JNI_CALL);
    objFieldId = ec_jni->GetFieldID(klass, "obj", "Ljava/lang/Object;", TRACE_JNI_CALL);
  }
  arr = (jdoubleArray) ec_jni->GetObjectField(o, objFieldId, TRACE_JNI_CALL);
  ec_jni->SetObjectField(o, objFieldId, nullptr, TRACE_JNI_CALL);

  size = ec_jni->GetArrayLength(arr, TRACE_JNI_CALL);
  start_time = time(nullptr);
  enterTime /= 1000;
  current_time = 0;
  while (difftime(current_time, start_time) < enterTime) {
    hash = 0;
    pa = (jdouble*) ec_jni->GetPrimitiveArrayCritical(arr, nullptr, TRACE_JNI_CALL);
    if (pa != nullptr) {
      for (i = 0; i < size; ++i) {
        hash += pa[i];
      }
    } else {
      jni_env->FatalError("GetPrimitiveArrayCritical returned null");
    }
    mssleep((long) sleepTime);
    ec_jni->ReleasePrimitiveArrayCritical(arr, pa, 0, TRACE_JNI_CALL);
    mssleep((long) sleepTime);
    current_time = time(nullptr);
  }
  ec_jni->SetObjectField(o, objFieldId, arr, TRACE_JNI_CALL);
  return hash;
}

}
