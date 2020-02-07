/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
static const char* SIGN_START = "LMyPackage/HiddenClassSig/";
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
check_hidden_class_loader(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass, jobject loader) {
  jint count = 0;
  jclass* loader_classes = NULL;
  jboolean found = false;
  jvmtiError err;

  err = jvmti->GetClassLoaderClasses(loader, &count, &loader_classes);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI GetClassLoaderClasses");

  for (int idx = 0; idx < count; idx++) {
    char* sign = NULL;

    err = jvmti->GetClassSignature(loader_classes[idx], &sign, NULL);
    check_jvmti_status(jni, err, "ClassLoad: Error in JVMTI GetClassSignature");

    if (jni->IsSameObject(loader_classes[idx], klass)) {
      found = true;
      break;
    }
  }
  if (found) {
    printf("check_hidden_class: FAIL: unexpectedly found hidden class in its loader classes\n");
    failed = true;
  } else {
    printf("check_hidden_class: not found hidden class in its loader classes as expected\n");
  }
  fflush(0);
}

static void
check_hidden_class_flags(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  jboolean flag = false;
  jvmtiError err;

  err = jvmti->IsInterface(klass, &flag);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI IsInterface");
  if (flag) {
    printf("check_hidden_class: FAIL: given hidden class is not expected to be an interface\n");
    fflush(0);
    failed = true;
  }

  err = jvmti->IsArrayClass(klass, &flag);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI IsArrayClass");
  if (flag) {
    printf("check_hidden_class: FAIL: given hidden class is not expected to be an array\n");
    fflush(0);
    failed = true;
  }

  err = jvmti->IsModifiableClass(klass, &flag);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI IsModifiableClass");
  if (flag) {
    printf("check_hidden_class: FAIL: given hidden class is not expected to be modifiable\n");
    fflush(0);
    failed = true;
  }
}

static void
check_hidden_class_impl_interf(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  char* sign = NULL;
  jint count = 0;
  jclass* interfaces = NULL;
  jvmtiError err;

  err = jvmti->GetImplementedInterfaces(klass, &count, &interfaces);
  check_jvmti_status(jni, err, "check_hidden_class_impl_interf: Error in JVMTI GetImplementedInterfaces");
  if (count != 1) {
    printf("check_hidden_class: FAIL: implemented interfaces count: %d, expected to be 1\n", count);
    fflush(0);
    failed = true;
  }

  err = jvmti->GetClassSignature(interfaces[0], &sign, NULL);
  check_jvmti_status(jni, err, "check_hidden_class_impl_interf: Error in JVMTI GetClassSignature for implemented interface");
  if (strcmp(sign, EXP_INTERF_SIGN) != 0) {
    printf("check_hidden_class_impl_interf: FAIL: implemented interface signature: %s, expected to be: %s\n",
           sign, EXP_INTERF_SIGN);
    fflush(0);
    failed = true;
  }
}

static void
check_hidden_class(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  jint class_modifiers = 0;
  char* source_file_name = NULL;
  jobject loader = NULL;
  jboolean is_array = false;
  char* sign = NULL;
  char* gsig = NULL;
  jvmtiError err;

  err = jvmti->GetClassModifiers(klass, &class_modifiers);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI GetClassModifiers");

  err = jvmti->IsArrayClass(klass, &is_array);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI IsArrayClass");

  printf("check_hidden_class: modifiers of hidden class 0x%x, isArray: %d\n", class_modifiers, is_array);
  fflush(0);

  err = jvmti->GetClassSignature(klass, &sign, &gsig);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI GetClassSignature");

  printf("check_hidden_class: hidden class with sign: %s\n", sign); fflush(0);
  printf("check_hidden_class: hidden class with gsig: %s\n", gsig); fflush(0);

  if (strchr(sign, '+') != NULL) {
    printf("Hidden class signature should not contain a '+' character, sign: %s\n", sign);
    fflush(0);
    failed = true;
  }

  if (is_array) {
    return;
  }

  err = jvmti->GetClassLoader(klass, &loader);
  check_jvmti_status(jni, err, "check_hidden_class: Error in JVMTI GetClassLoader");
  printf("check_hidden_class: loader of hidden class: %p\n", loader);
  fflush(0);

  check_hidden_class_loader(jvmti, jni, klass, loader);
  check_hidden_class_flags(jvmti, jni, klass);
  check_hidden_class_impl_interf(jvmti, jni, klass);
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
    printf("ClassLoad event: hidden class with gsig: %s\n", gsig); fflush(0);
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
  caps.can_get_source_file_name = 1;

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
Java_MyPackage_HiddenClassSigTest_checkHiddenClass(JNIEnv *jni, jclass klass, jclass hidden_klass) {
  check_hidden_class(jvmti, jni, hidden_klass);
}

JNIEXPORT jboolean JNICALL
Java_MyPackage_HiddenClassSigTest_checkFailed(JNIEnv *jni, jclass klass) {
  return failed;
}

} // extern "C"
