/*
 * Copyright (c) 2019, 2019 Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#if !defined(_WIN32) && !defined(_WIN64)

JNIEXPORT jint JNICALL
Java_TestJNIArrays_GetFlattenedArrayElementSizeWrapper(JNIEnv* env, jobject receiver, jarray array) {
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  return (jint)elm_sz;
}

JNIEXPORT jclass JNICALL
Java_TestJNIArrays_GetFlattenedArrayElementClassWrapper(JNIEnv* env, jobject receiver, jarray array) {
  jclass elm_class = (*env)->GetFlattenedArrayElementClass(env, array);
  return elm_class;
}

JNIEXPORT jint JNICALL
Java_TestJNIArrays_GetFieldOffsetInFlattenedLayoutWrapper(JNIEnv* env, jobject receiver, jclass clazz, jstring name, jstring signature, jboolean expectFlattened) {
  jboolean flattened;
  const char *name_ptr = (*env)->GetStringUTFChars(env, name, NULL);
  const char *signature_ptr = (*env)->GetStringUTFChars(env, signature, NULL);
  int offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, name_ptr,signature_ptr, &flattened);
  (*env)->ReleaseStringUTFChars(env, name, name_ptr);
  (*env)->ReleaseStringUTFChars(env, signature, signature_ptr);
  if ((*env)->ExceptionCheck(env)) {
    return -1;
  }
  if (flattened != expectFlattened) {
    jclass RE = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, RE, "Flattening mismatch");
    return -1;
  }
  return offset;
}

JNIEXPORT jlong JNICALL
Java_TestJNIArrays_GetFlattenedArrayElementsWrapper(JNIEnv* env, jobject receiver, jarray array) {
  jboolean isCopy;
  void* addr = (*env)->GetFlattenedArrayElements(env, array, &isCopy);
  return (jlong)addr;
}

JNIEXPORT void JNICALL
Java_TestJNIArrays_ReleaseFlattenedArrayElementsWrapper(JNIEnv* env, jobject receiver, jarray array, jlong addr, jint mode) {
  (*env)->ReleaseFlattenedArrayElements(env, array, (void*)addr, mode);
}

JNIEXPORT jint JNICALL
Java_TestJNIArrays_getIntFieldAtIndex(JNIEnv* env, jobject receiver, jarray array, jint index, jstring name, jstring signature) {
  jint array_length = (*env)->GetArrayLength(env, array);
  if (index < 0 || index >= array_length) {
    jclass AIOOBE = (*env)->FindClass(env, "java.lang.ArrayIndexOutOfBoundsException");
    (*env)->ThrowNew(env, AIOOBE, "Bad index");
    return -1;
  }
  jobject element = (*env)->GetObjectArrayElement(env, array, index);
  // should add protection against null element here (could happen if array is not a flattened array
  jclass element_class = (*env)->GetObjectClass(env, element);
  const char *name_ptr = (*env)->GetStringUTFChars(env, name, NULL);
  const char *signature_ptr = (*env)->GetStringUTFChars(env, signature, NULL);
  jfieldID field_id = (*env)->GetFieldID(env, element_class, (const char*)name_ptr, (const char *)signature_ptr);
  (*env)->ReleaseStringUTFChars(env, name, name_ptr);
  (*env)->ReleaseStringUTFChars(env, signature, signature_ptr);
  jint value = (*env)->GetIntField(env, element, field_id);
  return value;
}

JNIEXPORT void JNICALL
Java_TestJNIArrays_printArrayInformation(JNIEnv* env, jobject receiver, jarray array) {
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  void* base = (*env)->GetFlattenedArrayElements(env, array, NULL);
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}

JNIEXPORT void JNICALL
Java_TestJNIArrays_initializeIntIntArrayBuffer(JNIEnv* env, jobject receiver, jarray array, int i0, int i1) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  int i0_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i0", "I", NULL);
  int i1_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i1", "I", NULL);
  char* buffer = (char*)malloc(elm_sz);
  if (buffer == NULL) {
    jclass OOM = (*env)->FindClass(env, "java/lang/OutOfMemoryException");
    (*env)->ThrowNew(env, OOM, "Malloc failed");
    return;
  }
  *(int*)(buffer + i0_offset) = i0;
  *(int*)(buffer + i1_offset) = i1;
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
  for (int i = 0; i < len; i++) {
    memcpy((char*)base + i * elm_sz, buffer, elm_sz); 
  }
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
  free(buffer);
}

JNIEXPORT void JNICALL
Java_TestJNIArrays_initializeIntIntArrayFields(JNIEnv* env, jobject receiver, jarray array, int i0, int i1) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  int i0_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i0", "I", NULL);
  int i1_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i1", "I", NULL);
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
  char* elm_ptr = base;
  for (int i = 0; i < len; i++) {
    *(int*)(elm_ptr + i0_offset) = i0;
    *(int*)(elm_ptr + i1_offset) = i1;
    elm_ptr += elm_sz;
  }
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}

struct IntInt_offsets {
  int i0_offset;
  int i1_offset;
};

#ifdef __APPLE__
static int compare_IntInt(void* offsets, const void* x, const void* y)  {
#endif // __APPLE__
#ifdef __linux__
static int compare_IntInt(const void* x, const void* y, void* offsets)  {
#endif // __linux__  
  int i0_offset = ((struct IntInt_offsets*)offsets)->i0_offset;
  int x_i0 = *(int*)((char*)x + i0_offset);
  int y_i0 = *(int*)((char*)y + i0_offset);
  if (x_i0 < y_i0) return -1;
  if (x_i0 > y_i0) return 1;
  int i1_offset = ((struct IntInt_offsets*)offsets)->i1_offset;
  int x_i1 = *(int*)((char*)x + i1_offset);
  int y_i1 = *(int*)((char*)y + i1_offset );
  if (x_i1 < y_i1) return -1;
  if (x_i1 > y_i1) return 1;
  return 0;
}

JNIEXPORT void JNICALL
Java_TestJNIArrays_sortIntIntArray(JNIEnv* env, jobject receiver, jarray array) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  struct IntInt_offsets offsets;
  offsets.i0_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i0", "I", NULL);
  offsets.i1_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "i1", "I", NULL);
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
#ifdef __APPLE__
  qsort_r(base, len, elm_sz, (void*) &offsets, compare_IntInt);
#endif // __APPLE__
#ifdef __linux__
  qsort_r(base, len, elm_sz,  compare_IntInt, (void*) &offsets);
#endif // __linux__
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}


JNIEXPORT void JNICALL
Java_TestJNIArrays_initializeContainerArray(JNIEnv* env, jobject receiver, jarray array,
					       jdouble d, jfloat f, jshort s, jbyte b) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  int d_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "d", "D", NULL);
  int b_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "b", "B", NULL);
  jboolean flattened;
  int c_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "c", "QTestJNIArrays$Containee;", &flattened);
  if (!flattened) {
    jclass RE = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, RE, "Incompatible layout");
    return;
  }
  jclass clazz2 = (*env)->FindClass(env, "TestJNIArrays$Containee");
  int f_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz2, "f", "F", NULL);
  int s_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz2, "s", "S", NULL);
  f_offset += c_offset;
  s_offset += c_offset;
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
  char* elm_ptr = base;
  for (int i = 0; i < len; i++) {
    *(jdouble*)(elm_ptr + d_offset) = d;
    *(jfloat*)(elm_ptr + f_offset) = f;
    *(jshort*)(elm_ptr + s_offset) = s;
    *(jbyte*)(elm_ptr + b_offset) = b;
    elm_ptr += elm_sz;
  }
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}


JNIEXPORT void JNICALL
Java_TestJNIArrays_updateContainerArray(JNIEnv* env, jobject receiver, jarray array,
					       jfloat f, jshort s) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  jboolean flattened;
  int c_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "c", "QTestJNIArrays$Containee;", &flattened);
  if (!flattened) {
    jclass RE = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, RE, "Incompatible layout");
    return;
  }
  jclass clazz2 = (*env)->FindClass(env, "TestJNIArrays$Containee");
  int f_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz2, "f", "F", NULL);
  int s_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz2, "s", "S", NULL);
  f_offset += c_offset;
  s_offset += c_offset;
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
  char* elm_ptr = base;
  for (int i = 0; i < len; i++) {
    *(jfloat*)(elm_ptr + f_offset) = f;
    *(jshort*)(elm_ptr + s_offset) = s;
    elm_ptr += elm_sz;
  }
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}


 JNIEXPORT void JNICALL
 Java_TestJNIArrays_initializeLongLongLongLongArray(JNIEnv* env, jobject receiver, jarray array, jlong l0, jlong l1, jlong l2, jlong l3) {
  int len = (*env)->GetArrayLength(env, array);
  jsize elm_sz = (*env)->GetFlattenedArrayElementSize(env, array);
  jclass clazz = (*env)->GetFlattenedArrayElementClass(env, array);
  int l0_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "l0", "J", NULL);
  int l1_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "l1", "J", NULL);
  int l2_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "l2", "J", NULL);
  int l3_offset = (*env)->GetFieldOffsetInFlattenedLayout(env, clazz, "l3", "J", NULL);
  void* base = (void*)(*env)->GetFlattenedArrayElements(env, array, NULL);
  char* elm_ptr = base;
  for (int i = 0; i < len; i++) {
    *(jlong*)(elm_ptr + l0_offset) = l0;
    *(jlong*)(elm_ptr + l1_offset) = l1;
    *(jlong*)(elm_ptr + l2_offset) = l2;
    *(jlong*)(elm_ptr + l3_offset) = l3;
    elm_ptr += elm_sz;
  }
  (*env)->ReleaseFlattenedArrayElements(env, array, base, 0);
}

JNIEXPORT jobject JNICALL
Java_TestJNIArrays_createSubElementSelector(JNIEnv* env, jobject receiver, jarray array) {
  return (*env)->CreateSubElementSelector(env, array);
}

JNIEXPORT jobject JNICALL
  Java_TestJNIArrays_getSubElementSelector(JNIEnv* env, jobject receiver, jobject selector, jclass klass, jstring name, jstring signature) {
  const char *name_ptr = (*env)->GetStringUTFChars(env, name, NULL);
  const char *signature_ptr = (*env)->GetStringUTFChars(env, signature, NULL);
  jfieldID fieldID = (*env)->GetFieldID(env, klass, name_ptr, signature_ptr);
  jobject res = (*env)->GetSubElementSelector(env, selector, fieldID);
  (*env)->ReleaseStringUTFChars(env, name, name_ptr);
  (*env)->ReleaseStringUTFChars(env, signature, signature_ptr);
  return res;
}

JNIEXPORT jobject JNICALL
Java_TestJNIArrays_getObjectSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index) {
  return (*env)->GetObjectSubElement(env, array, selector, index);
}

JNIEXPORT void JNICALL
  Java_TestJNIArrays_setObjectSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index, jobject value) {
  (*env)->SetObjectSubElement(env, array, selector, index, value);
}

JNIEXPORT jshort JNICALL
Java_TestJNIArrays_getShortSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index) {
  return (*env)->GetShortSubElement(env, array, selector, index);
}

JNIEXPORT void JNICALL
  Java_TestJNIArrays_setShortSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index, short value) {
  (*env)->SetShortSubElement(env, array, selector, index, value);
}

JNIEXPORT jint JNICALL
Java_TestJNIArrays_getIntSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index) {
  return (*env)->GetIntSubElement(env, array, selector, index);
}

JNIEXPORT void JNICALL
  Java_TestJNIArrays_setIntSubElement(JNIEnv* env, jobject receiver, jarray array, jobject selector, jint index, jint value) {
  (*env)->SetIntSubElement(env, array, selector, index, value);
}

#endif // !defined(_WIN32) && !defined(_WIN64)
