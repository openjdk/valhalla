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
 */

#include <string.h>
#include "jvmti.h"

extern "C" {

static const char* EXP_INTERF_SIGN= "LMyPackage/Test;";
static const char* SIGN_START = "LMyPackage/HiddenClass/";
static const size_t   SIGN_START_LEN = strlen(SIGN_START);

static jvmtiEnv *jvmti = NULL;
static jint class_load_count = 0;
static bool failed = false;

static void
check_jvmti_status(JNIEnv* jni, jvmtiError err, const char* msg) {
  if (err != JVMTI_ERROR_NONE) {
    printf("check_jvmti_status: JVMTI function returned error: %d\n", err);
    fflush(0);
    failed = true;
    jni->FatalError(msg);
  }
}

static void
check_hidden_class_sig(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  char* sig = NULL;
  char* gsig = NULL;
  jvmtiError err;

  err = jvmti->GetClassSignature(klass, &sig, &gsig);
  check_jvmti_status(jni, err, "check_hidden_class_sig: Error in JVMTI GetClassSignature");

  if (strchr(sig, '+') != NULL) {
    printf("Hidden class signature should not contain a '+' character, sig: %s\n", sig);
    fflush(0);
    failed = true;
  }
}

static void JNICALL
VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
  char* sign = NULL;
  jvmtiError err;

  printf("VMInit event posted\n");
  printf("VMInit event: SIGN_START: %s, SIGN_START_LEN: %d\n", SIGN_START, (int)SIGN_START_LEN);
  fflush(0);

  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL);
  check_jvmti_status(jni, err, "VMInit event: Error in enabling ClassLoad events notification");
}

static void JNICALL
ClassLoad(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread, jclass klass) {
  char* sign = NULL;
  char* gsig = NULL;
  char* src_name = NULL;
  jvmtiError err;

  err = jvmti->GetClassSignature(klass, &sign, &gsig);
  check_jvmti_status(jni, err, "ClassLoad: Error in JVMTI GetClassSignature");

  if (strlen(sign) > strlen(SIGN_START) && strncmp(sign, SIGN_START, SIGN_START_LEN) == 0) {
    class_load_count++;
    if (gsig == NULL) {
      printf("ClassLoad event: FAIL: GetClassSignature returned NULL generic signature for hidden class\n");
      fflush(0);
      failed = true;
    }
    printf("ClassLoad event: hidden class with sign: %s\n", sign); fflush(0);
  }
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  jvmtiEventCallbacks callbacks;
  jvmtiCapabilities caps;
  jvmtiError err;

  if (jvm->GetEnv((void **) (&jvmti), JVMTI_VERSION) != JNI_OK) {
    printf("Agent_OnLoad: Error in GetEnv in obtaining jvmtiEnv*\n");
    failed = true;
    return JNI_ERR;
  }

  printf("Agent_OnLoad: started\n");
  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.ClassLoad = &ClassLoad;
  callbacks.VMInit = &VMInit;

  err = jvmti->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI SetEventCallbacks: %d\n", err);
    failed = true;
    return JNI_ERR;
  }

  memset(&caps, 0, sizeof(caps));
  err = jvmti->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI AddCapabilities: %d\n", err);
    failed = true;
    return JNI_ERR;
  }

  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI SetEventNotificationMode: %d\n", err);
    failed = true;
    return JNI_ERR;
  }

  printf("Agent_OnLoad: finished\n");
  fflush(0);
  return JNI_OK;
}

JNIEXPORT void JNICALL
Java_MyPackage_HiddenClassSigTest_checkHiddenClassSig(JNIEnv *jni, jclass klass, jclass hidden_klass) {
  check_hidden_class_sig(jvmti, jni, hidden_klass);
}

JNIEXPORT jboolean JNICALL
Java_MyPackage_HiddenClassSigTest_checkFailed(JNIEnv *jni, jclass klass) {
  return failed;
}

} // extern "C"
