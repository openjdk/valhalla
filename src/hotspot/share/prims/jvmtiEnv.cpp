/*
 * Copyright (c) 2003, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/classLoaderExt.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/modules.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "interpreter/interpreter.hpp"
#include "jfr/jfrEvents.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "logging/logConfiguration.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jniCheck.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiAgentThread.hpp"
#include "prims/jvmtiClassFileReconstituter.hpp"
#include "prims/jvmtiCodeBlobEvents.hpp"
#include "prims/jvmtiExtensions.hpp"
#include "prims/jvmtiGetLoadedClasses.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiManageCapabilities.hpp"
#include "prims/jvmtiRawMonitor.hpp"
#include "prims/jvmtiRedefineClasses.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "prims/jvmtiUtil.hpp"
#include "runtime/arguments.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/javaThread.inline.hpp"
#include "runtime/jfieldIDWorkaround.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/osThread.hpp"
#include "runtime/reflectionUtils.hpp"
#include "runtime/signature.hpp"
#include "runtime/threadHeapSampler.hpp"
#include "runtime/threads.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vmThread.hpp"
#include "services/threadService.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/preserveException.hpp"
#include "utilities/utf8.hpp"


#define FIXLATER 0 // REMOVE this when completed.

 // FIXLATER: hook into JvmtiTrace
#define TraceJVMTICalls false

JvmtiEnv::JvmtiEnv(jint version) : JvmtiEnvBase(version) {
}

JvmtiEnv::~JvmtiEnv() {
}

JvmtiEnv*
JvmtiEnv::create_a_jvmti(jint version) {
  return new JvmtiEnv(version);
}

// VM operation class to copy jni function table at safepoint.
// More than one java threads or jvmti agents may be reading/
// modifying jni function tables. To reduce the risk of bad
// interaction b/w these threads it is copied at safepoint.
class VM_JNIFunctionTableCopier : public VM_Operation {
 private:
  const struct JNINativeInterface_ *_function_table;
 public:
  VM_JNIFunctionTableCopier(const struct JNINativeInterface_ *func_tbl) {
    _function_table = func_tbl;
  };

  VMOp_Type type() const { return VMOp_JNIFunctionTableCopier; }
  void doit() {
    copy_jni_function_table(_function_table);
  };
};

//
// Do not change the "prefix" marker below, everything above it is copied
// unchanged into the filled stub, everything below is controlled by the
// stub filler (only method bodies are carried forward, and then only for
// functionality still in the spec).
//
// end file prefix

  //
  // Memory Management functions
  //

// mem_ptr - pre-checked for null
jvmtiError
JvmtiEnv::Allocate(jlong size, unsigned char** mem_ptr) {
  return allocate(size, mem_ptr);
} /* end Allocate */


// mem - null is a valid value, must be checked
jvmtiError
JvmtiEnv::Deallocate(unsigned char* mem) {
  return deallocate(mem);
} /* end Deallocate */

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetThreadLocalStorage(jthread thread, const void* data) {
  JavaThread* current = JavaThread::current();
  JvmtiThreadState* state = nullptr;
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  if (thread == nullptr) {
    java_thread = current;
    state = java_thread->jvmti_thread_state();
  } else {
    jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current, &java_thread, &thread_obj);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    state = java_lang_Thread::jvmti_thread_state(thread_obj);
  }
  if (state == nullptr) {
    if (data == nullptr) {
      // leaving state unset same as data set to null
      return JVMTI_ERROR_NONE;
    }
    // otherwise, create the state
    HandleMark hm(current);
    Handle thread_handle(current, thread_obj);
    state = JvmtiThreadState::state_for(java_thread, thread_handle);
    if (state == nullptr) {
      return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }
  }
  state->env_thread_state(this)->set_agent_thread_local_storage_data((void*)data);
  return JVMTI_ERROR_NONE;
} /* end SetThreadLocalStorage */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// data_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadLocalStorage(jthread thread, void** data_ptr) {
  JavaThread* current_thread = JavaThread::current();
  if (thread == nullptr) {
    JvmtiThreadState* state = current_thread->jvmti_thread_state();
    *data_ptr = (state == nullptr) ? nullptr :
      state->env_thread_state(this)->get_agent_thread_local_storage_data();
  } else {
    // jvmti_GetThreadLocalStorage is "in native" and doesn't transition
    // the thread to _thread_in_vm. However, when the TLS for a thread
    // other than the current thread is required we need to transition
    // from native so as to resolve the jthread.

    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, current_thread));
    ThreadInVMfromNative __tiv(current_thread);
    VM_ENTRY_BASE(jvmtiError, JvmtiEnv::GetThreadLocalStorage , current_thread)
    DEBUG_ONLY(VMNativeEntryWrapper __vew;)

    JvmtiVTMSTransitionDisabler disabler(thread);
    ThreadsListHandle tlh(current_thread);

    JavaThread* java_thread = nullptr;
    oop thread_obj = nullptr;
    jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }

    HandleMark hm(current_thread);
    Handle thread_handle(current_thread, thread_obj);
    JvmtiThreadState* state = JvmtiThreadState::state_for(java_thread, thread_handle);
    *data_ptr = (state == nullptr) ? nullptr :
      state->env_thread_state(this)->get_agent_thread_local_storage_data();
  }
  return JVMTI_ERROR_NONE;
} /* end GetThreadLocalStorage */

  //
  // Module functions
  //

// module_count_ptr - pre-checked for null
// modules_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetAllModules(jint* module_count_ptr, jobject** modules_ptr) {
    JvmtiModuleClosure jmc;

    return jmc.get_all_modules(this, module_count_ptr, modules_ptr);
} /* end GetAllModules */


// class_loader - null is a valid value, must be pre-checked
// package_name - pre-checked for null
// module_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetNamedModule(jobject class_loader, const char* package_name, jobject* module_ptr) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  ResourceMark rm(THREAD);

  Handle h_loader (THREAD, JNIHandles::resolve(class_loader));
  // Check that loader is a subclass of java.lang.ClassLoader.
  if (h_loader.not_null() && !java_lang_ClassLoader::is_subclass(h_loader->klass())) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }
  oop module = Modules::get_named_module(h_loader, package_name);
  *module_ptr = module != nullptr ? JNIHandles::make_local(THREAD, module) : nullptr;
  return JVMTI_ERROR_NONE;
} /* end GetNamedModule */


// module - pre-checked for null
// to_module - pre-checked for null
jvmtiError
JvmtiEnv::AddModuleReads(jobject module, jobject to_module) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.

  // check module
  Handle h_module(THREAD, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  // check to_module
  Handle h_to_module(THREAD, JNIHandles::resolve(to_module));
  if (!java_lang_Module::is_instance(h_to_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  return JvmtiExport::add_module_reads(h_module, h_to_module, THREAD);
} /* end AddModuleReads */


// module - pre-checked for null
// pkg_name - pre-checked for null
// to_module - pre-checked for null
jvmtiError
JvmtiEnv::AddModuleExports(jobject module, const char* pkg_name, jobject to_module) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  Handle h_pkg = java_lang_String::create_from_str(pkg_name, THREAD);

  // check module
  Handle h_module(THREAD, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  // check to_module
  Handle h_to_module(THREAD, JNIHandles::resolve(to_module));
  if (!java_lang_Module::is_instance(h_to_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  return JvmtiExport::add_module_exports(h_module, h_pkg, h_to_module, THREAD);
} /* end AddModuleExports */


// module - pre-checked for null
// pkg_name - pre-checked for null
// to_module - pre-checked for null
jvmtiError
JvmtiEnv::AddModuleOpens(jobject module, const char* pkg_name, jobject to_module) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  Handle h_pkg = java_lang_String::create_from_str(pkg_name, THREAD);

  // check module
  Handle h_module(THREAD, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  // check to_module
  Handle h_to_module(THREAD, JNIHandles::resolve(to_module));
  if (!java_lang_Module::is_instance(h_to_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  return JvmtiExport::add_module_opens(h_module, h_pkg, h_to_module, THREAD);
} /* end AddModuleOpens */


// module - pre-checked for null
// service - pre-checked for null
jvmtiError
JvmtiEnv::AddModuleUses(jobject module, jclass service) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.

  // check module
  Handle h_module(THREAD, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  // check service
  Handle h_service(THREAD, JNIHandles::resolve_external_guard(service));
  if (!java_lang_Class::is_instance(h_service()) ||
      java_lang_Class::is_primitive(h_service())) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  return JvmtiExport::add_module_uses(h_module, h_service, THREAD);
} /* end AddModuleUses */


// module - pre-checked for null
// service - pre-checked for null
// impl_class - pre-checked for null
jvmtiError
JvmtiEnv::AddModuleProvides(jobject module, jclass service, jclass impl_class) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.

  // check module
  Handle h_module(THREAD, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }
  // check service
  Handle h_service(THREAD, JNIHandles::resolve_external_guard(service));
  if (!java_lang_Class::is_instance(h_service()) ||
      java_lang_Class::is_primitive(h_service())) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  // check impl_class
  Handle h_impl_class(THREAD, JNIHandles::resolve_external_guard(impl_class));
  if (!java_lang_Class::is_instance(h_impl_class()) ||
      java_lang_Class::is_primitive(h_impl_class())) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  return JvmtiExport::add_module_provides(h_module, h_service, h_impl_class, THREAD);
} /* end AddModuleProvides */

// module - pre-checked for null
// is_modifiable_class_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsModifiableModule(jobject module, jboolean* is_modifiable_module_ptr) {
  JavaThread* current = JavaThread::current();

  // check module
  Handle h_module(current, JNIHandles::resolve(module));
  if (!java_lang_Module::is_instance(h_module())) {
    return JVMTI_ERROR_INVALID_MODULE;
  }

  *is_modifiable_module_ptr = JNI_TRUE;
  return JVMTI_ERROR_NONE;
} /* end IsModifiableModule */


  //
  // Class functions
  //

// class_count_ptr - pre-checked for null
// classes_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLoadedClasses(jint* class_count_ptr, jclass** classes_ptr) {
  return JvmtiGetLoadedClasses::getLoadedClasses(this, class_count_ptr, classes_ptr);
} /* end GetLoadedClasses */


// initiating_loader - null is a valid value, must be checked
// class_count_ptr - pre-checked for null
// classes_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassLoaderClasses(jobject initiating_loader, jint* class_count_ptr, jclass** classes_ptr) {
  return JvmtiGetLoadedClasses::getClassLoaderClasses(this, initiating_loader,
                                                  class_count_ptr, classes_ptr);
} /* end GetClassLoaderClasses */

// k_mirror - may be primitive, this must be checked
// is_modifiable_class_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsModifiableClass(oop k_mirror, jboolean* is_modifiable_class_ptr) {
  *is_modifiable_class_ptr = VM_RedefineClasses::is_modifiable_class(k_mirror)?
                                                       JNI_TRUE : JNI_FALSE;
  return JVMTI_ERROR_NONE;
} /* end IsModifiableClass */

// class_count - pre-checked to be greater than or equal to 0
// classes - pre-checked for null
jvmtiError
JvmtiEnv::RetransformClasses(jint class_count, const jclass* classes) {
//TODO: add locking

  int index;
  JavaThread* current_thread = JavaThread::current();
  ResourceMark rm(current_thread);

  jvmtiClassDefinition* class_definitions =
                            NEW_RESOURCE_ARRAY(jvmtiClassDefinition, class_count);
  NULL_CHECK(class_definitions, JVMTI_ERROR_OUT_OF_MEMORY);

  for (index = 0; index < class_count; index++) {
    HandleMark hm(current_thread);

    jclass jcls = classes[index];
    oop k_mirror = JNIHandles::resolve_external_guard(jcls);
    if (k_mirror == nullptr) {
      return JVMTI_ERROR_INVALID_CLASS;
    }
    if (!k_mirror->is_a(vmClasses::Class_klass())) {
      return JVMTI_ERROR_INVALID_CLASS;
    }

    if (!VM_RedefineClasses::is_modifiable_class(k_mirror)) {
      return JVMTI_ERROR_UNMODIFIABLE_CLASS;
    }

    Klass* klass = java_lang_Class::as_Klass(k_mirror);

    jint status = klass->jvmti_class_status();
    if (status & (JVMTI_CLASS_STATUS_ERROR)) {
      return JVMTI_ERROR_INVALID_CLASS;
    }

    InstanceKlass* ik = InstanceKlass::cast(klass);
    if (ik->get_cached_class_file_bytes() == nullptr) {
      // Not cached, we need to reconstitute the class file from the
      // VM representation. We don't attach the reconstituted class
      // bytes to the InstanceKlass here because they have not been
      // validated and we're not at a safepoint.
      JvmtiClassFileReconstituter reconstituter(ik);
      if (reconstituter.get_error() != JVMTI_ERROR_NONE) {
        return reconstituter.get_error();
      }

      class_definitions[index].class_byte_count = (jint)reconstituter.class_file_size();
      class_definitions[index].class_bytes      = (unsigned char*)
                                                       reconstituter.class_file_bytes();
    } else {
      // it is cached, get it from the cache
      class_definitions[index].class_byte_count = ik->get_cached_class_file_len();
      class_definitions[index].class_bytes      = ik->get_cached_class_file_bytes();
    }
    class_definitions[index].klass              = jcls;
  }
  EventRetransformClasses event;
  VM_RedefineClasses op(class_count, class_definitions, jvmti_class_load_kind_retransform);
  VMThread::execute(&op);
  jvmtiError error = op.check_error();
  if (error == JVMTI_ERROR_NONE) {
    event.set_classCount(class_count);
    event.set_redefinitionId(op.id());
    event.commit();
  }
  return error;
} /* end RetransformClasses */


// class_count - pre-checked to be greater than or equal to 0
// class_definitions - pre-checked for null
jvmtiError
JvmtiEnv::RedefineClasses(jint class_count, const jvmtiClassDefinition* class_definitions) {
//TODO: add locking
  EventRedefineClasses event;
  VM_RedefineClasses op(class_count, class_definitions, jvmti_class_load_kind_redefine);
  VMThread::execute(&op);
  jvmtiError error = op.check_error();
  if (error == JVMTI_ERROR_NONE) {
    event.set_classCount(class_count);
    event.set_redefinitionId(op.id());
    event.commit();
  }
  return error;
} /* end RedefineClasses */


  //
  // Object functions
  //

// size_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetObjectSize(jobject object, jlong* size_ptr) {
  oop mirror = JNIHandles::resolve_external_guard(object);
  NULL_CHECK(mirror, JVMTI_ERROR_INVALID_OBJECT);
  *size_ptr = (jlong)mirror->size() * wordSize;
  return JVMTI_ERROR_NONE;
} /* end GetObjectSize */

  //
  // Method functions
  //

// prefix - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetNativeMethodPrefix(const char* prefix) {
  return prefix == nullptr?
              SetNativeMethodPrefixes(0, nullptr) :
              SetNativeMethodPrefixes(1, (char**)&prefix);
} /* end SetNativeMethodPrefix */


// prefix_count - pre-checked to be greater than or equal to 0
// prefixes - pre-checked for null
jvmtiError
JvmtiEnv::SetNativeMethodPrefixes(jint prefix_count, char** prefixes) {
  // Have to grab JVMTI thread state lock to be sure that some thread
  // isn't accessing the prefixes at the same time we are setting them.
  // No locks during VM bring-up.
  if (Threads::number_of_threads() == 0) {
    return set_native_method_prefixes(prefix_count, prefixes);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    return set_native_method_prefixes(prefix_count, prefixes);
  }
} /* end SetNativeMethodPrefixes */

  //
  // Event Management functions
  //

// callbacks - null is a valid value, must be checked
// size_of_callbacks - pre-checked to be greater than or equal to 0
jvmtiError
JvmtiEnv::SetEventCallbacks(const jvmtiEventCallbacks* callbacks, jint size_of_callbacks) {
  JvmtiVTMSTransitionDisabler disabler;
  JvmtiEventController::set_event_callbacks(this, callbacks, size_of_callbacks);
  return JVMTI_ERROR_NONE;
} /* end SetEventCallbacks */


// event_thread - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetEventNotificationMode(jvmtiEventMode mode, jvmtiEvent event_type, jthread event_thread,   ...) {
  bool enabled = (mode == JVMTI_ENABLE);

  // event_type must be valid
  if (!JvmtiEventController::is_valid_event_type(event_type)) {
    return JVMTI_ERROR_INVALID_EVENT_TYPE;
  }

  // assure that needed capabilities are present
  if (enabled && !JvmtiUtil::has_event_capability(event_type, get_capabilities())) {
    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }

  if (event_type == JVMTI_EVENT_CLASS_FILE_LOAD_HOOK && enabled) {
    record_class_file_load_hook_enabled();
  }
  JvmtiVTMSTransitionDisabler disabler;

  if (event_thread == nullptr) {
    // Can be called at Agent_OnLoad() time with event_thread == nullptr
    // when Thread::current() does not work yet so we cannot create a
    // ThreadsListHandle that is common to both thread-specific and
    // global code paths.

    JvmtiEventController::set_user_enabled(this, nullptr, (oop) nullptr, event_type, enabled);
  } else {
    // We have a specified event_thread.
    JavaThread* current = JavaThread::current();
    ThreadsListHandle tlh(current);

    JavaThread* java_thread = nullptr;
    oop thread_obj = nullptr;
    jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), event_thread, current, &java_thread, &thread_obj);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }

    // global events cannot be controlled at thread level.
    if (JvmtiEventController::is_global_event(event_type)) {
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    JvmtiEventController::set_user_enabled(this, java_thread, thread_obj, event_type, enabled);
  }

  return JVMTI_ERROR_NONE;
} /* end SetEventNotificationMode */

  //
  // Capability functions
  //

// capabilities_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetPotentialCapabilities(jvmtiCapabilities* capabilities_ptr) {
  JvmtiManageCapabilities::get_potential_capabilities(get_capabilities(),
                                                      get_prohibited_capabilities(),
                                                      capabilities_ptr);
  return JVMTI_ERROR_NONE;
} /* end GetPotentialCapabilities */


// capabilities_ptr - pre-checked for null
jvmtiError
JvmtiEnv::AddCapabilities(const jvmtiCapabilities* capabilities_ptr) {
  return JvmtiManageCapabilities::add_capabilities(get_capabilities(),
                                                   get_prohibited_capabilities(),
                                                   capabilities_ptr,
                                                   get_capabilities());
} /* end AddCapabilities */


// capabilities_ptr - pre-checked for null
jvmtiError
JvmtiEnv::RelinquishCapabilities(const jvmtiCapabilities* capabilities_ptr) {
  JvmtiManageCapabilities::relinquish_capabilities(get_capabilities(), capabilities_ptr, get_capabilities());
  return JVMTI_ERROR_NONE;
} /* end RelinquishCapabilities */


// capabilities_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetCapabilities(jvmtiCapabilities* capabilities_ptr) {
  JvmtiManageCapabilities::copy_capabilities(get_capabilities(), capabilities_ptr);
  return JVMTI_ERROR_NONE;
} /* end GetCapabilities */

  //
  // Class Loader Search functions
  //

// segment - pre-checked for null
jvmtiError
JvmtiEnv::AddToBootstrapClassLoaderSearch(const char* segment) {
  jvmtiPhase phase = get_phase();
  if (phase == JVMTI_PHASE_ONLOAD) {
    Arguments::append_sysclasspath(segment);
    return JVMTI_ERROR_NONE;
  } else if (use_version_1_0_semantics()) {
    // This JvmtiEnv requested version 1.0 semantics and this function
    // is only allowed in the ONLOAD phase in version 1.0 so we need to
    // return an error here.
    return JVMTI_ERROR_WRONG_PHASE;
  } else if (phase == JVMTI_PHASE_LIVE) {
    // The phase is checked by the wrapper that called this function,
    // but this thread could be racing with the thread that is
    // terminating the VM so we check one more time.

    // create the zip entry
    ClassPathZipEntry* zip_entry = ClassLoader::create_class_path_zip_entry(segment);
    if (zip_entry == nullptr) {
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    // add the jar file to the bootclasspath
    log_info(class, load)("opened: %s", zip_entry->name());
#if INCLUDE_CDS
    ClassLoaderExt::append_boot_classpath(zip_entry);
#else
    ClassLoader::add_to_boot_append_entries(zip_entry);
#endif
    return JVMTI_ERROR_NONE;
  } else {
    return JVMTI_ERROR_WRONG_PHASE;
  }

} /* end AddToBootstrapClassLoaderSearch */


// segment - pre-checked for null
jvmtiError
JvmtiEnv::AddToSystemClassLoaderSearch(const char* segment) {
  jvmtiPhase phase = get_phase();

  if (phase == JVMTI_PHASE_ONLOAD) {
    for (SystemProperty* p = Arguments::system_properties(); p != nullptr; p = p->next()) {
      if (strcmp("java.class.path", p->key()) == 0) {
        p->append_value(segment);
        break;
      }
    }
    return JVMTI_ERROR_NONE;
  } else if (phase == JVMTI_PHASE_LIVE) {
    // The phase is checked by the wrapper that called this function,
    // but this thread could be racing with the thread that is
    // terminating the VM so we check one more time.
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    HandleMark hm(THREAD);

    // create the zip entry (which will open the zip file and hence
    // check that the segment is indeed a zip file).
    ClassPathZipEntry* zip_entry = ClassLoader::create_class_path_zip_entry(segment);
    if (zip_entry == nullptr) {
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    delete zip_entry;   // no longer needed

    Handle loader(THREAD, SystemDictionary::java_system_loader());

    // need the path as java.lang.String
    Handle path = java_lang_String::create_from_platform_dependent_str(segment, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
      return JVMTI_ERROR_INTERNAL;
    }

    // Invoke the appendToClassPathForInstrumentation method - if the method
    // is not found it means the loader doesn't support adding to the class path
    // in the live phase.
    {
      JavaValue res(T_VOID);
      JavaCalls::call_special(&res,
                              loader,
                              loader->klass(),
                              vmSymbols::appendToClassPathForInstrumentation_name(),
                              vmSymbols::appendToClassPathForInstrumentation_signature(),
                              path,
                              THREAD);
      if (HAS_PENDING_EXCEPTION) {
        Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
        CLEAR_PENDING_EXCEPTION;

        if (ex_name == vmSymbols::java_lang_NoSuchMethodError()) {
          return JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED;
        } else {
          return JVMTI_ERROR_INTERNAL;
        }
      }
    }

    return JVMTI_ERROR_NONE;
  } else {
    return JVMTI_ERROR_WRONG_PHASE;
  }
} /* end AddToSystemClassLoaderSearch */

  //
  // General functions
  //

// phase_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetPhase(jvmtiPhase* phase_ptr) {
  *phase_ptr = phase();
  return JVMTI_ERROR_NONE;
} /* end GetPhase */


jvmtiError
JvmtiEnv::DisposeEnvironment() {
  dispose();
  return JVMTI_ERROR_NONE;
} /* end DisposeEnvironment */


// data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetEnvironmentLocalStorage(const void* data) {
  set_env_local_storage(data);
  return JVMTI_ERROR_NONE;
} /* end SetEnvironmentLocalStorage */


// data_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetEnvironmentLocalStorage(void** data_ptr) {
  *data_ptr = (void*)get_env_local_storage();
  return JVMTI_ERROR_NONE;
} /* end GetEnvironmentLocalStorage */

// version_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetVersionNumber(jint* version_ptr) {
  *version_ptr = JVMTI_VERSION;
  return JVMTI_ERROR_NONE;
} /* end GetVersionNumber */


// name_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetErrorName(jvmtiError error, char** name_ptr) {
  if (error < JVMTI_ERROR_NONE || error > JVMTI_ERROR_MAX) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }
  const char *name = JvmtiUtil::error_name(error);
  if (name == nullptr) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }
  size_t len = strlen(name) + 1;
  jvmtiError err = allocate(len, (unsigned char**)name_ptr);
  if (err == JVMTI_ERROR_NONE) {
    memcpy(*name_ptr, name, len);
  }
  return err;
} /* end GetErrorName */


jvmtiError
JvmtiEnv::SetVerboseFlag(jvmtiVerboseFlag flag, jboolean value) {
  LogLevelType level = value == 0 ? LogLevel::Off : LogLevel::Info;
  switch (flag) {
  case JVMTI_VERBOSE_OTHER:
    // ignore
    break;
  case JVMTI_VERBOSE_CLASS:
    LogConfiguration::configure_stdout(level, false, LOG_TAGS(class, unload));
    LogConfiguration::configure_stdout(level, false, LOG_TAGS(class, load));
    break;
  case JVMTI_VERBOSE_GC:
    LogConfiguration::configure_stdout(level, true, LOG_TAGS(gc));
    break;
  case JVMTI_VERBOSE_JNI:
    level = value == 0 ? LogLevel::Off : LogLevel::Debug;
    LogConfiguration::configure_stdout(level, true, LOG_TAGS(jni, resolve));
    break;
  default:
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  };
  return JVMTI_ERROR_NONE;
} /* end SetVerboseFlag */


// format_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetJLocationFormat(jvmtiJlocationFormat* format_ptr) {
  *format_ptr = JVMTI_JLOCATION_JVMBCI;
  return JVMTI_ERROR_NONE;
} /* end GetJLocationFormat */

  //
  // Thread functions
  //

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// thread_state_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadState(jthread thread, jint* thread_state_ptr) {
  JavaThread* current_thread = JavaThread::current();
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_oop);
  if (err != JVMTI_ERROR_NONE && err != JVMTI_ERROR_THREAD_NOT_ALIVE) {
    // We got an error code so we don't have a JavaThread*, but only
    // return an error from here if the error is not because the thread
    // is a virtual thread.
    return err;
  }
  *thread_state_ptr = JvmtiEnvBase::get_thread_or_vthread_state(thread_oop, java_thread);
  return JVMTI_ERROR_NONE;
} /* end GetThreadState */


// thread_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetCurrentThread(jthread* thread_ptr) {
  JavaThread* cur_thread = JavaThread::current();
  oop thread_oop = get_vthread_or_thread_oop(cur_thread);

  *thread_ptr = (jthread)JNIHandles::make_local(cur_thread, thread_oop);
  return JVMTI_ERROR_NONE;
} /* end GetCurrentThread */


// threads_count_ptr - pre-checked for null
// threads_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetAllThreads(jint* threads_count_ptr, jthread** threads_ptr) {
  int nthreads        = 0;
  Handle *thread_objs = nullptr;
  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);

  // enumerate threads (including agent threads)
  ThreadsListEnumerator tle(current_thread, true);
  nthreads = tle.num_threads();
  *threads_count_ptr = nthreads;

  if (nthreads == 0) {
    *threads_ptr = nullptr;
    return JVMTI_ERROR_NONE;
  }

  thread_objs = NEW_RESOURCE_ARRAY(Handle, nthreads);
  NULL_CHECK(thread_objs, JVMTI_ERROR_OUT_OF_MEMORY);

  for (int i = 0; i < nthreads; i++) {
    thread_objs[i] = Handle(tle.get_threadObj(i));
  }

  jthread *jthreads  = new_jthreadArray(nthreads, thread_objs);
  NULL_CHECK(jthreads, JVMTI_ERROR_OUT_OF_MEMORY);

  *threads_ptr = jthreads;
  return JVMTI_ERROR_NONE;
} /* end GetAllThreads */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::SuspendThread(jthread thread) {
  JavaThread* current = JavaThread::current();
  HandleMark hm(current);
  Handle self_tobj;

  jvmtiError err;
  {
    JvmtiVTMSTransitionDisabler disabler(true);
    ThreadsListHandle tlh(current);
    JavaThread* java_thread = nullptr;
    oop thread_oop = nullptr;

    err = get_threadOop_and_JavaThread(tlh.list(), thread, current, &java_thread, &thread_oop);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }

    // Do not use JvmtiVTMSTransitionDisabler in context of self suspend to avoid deadlocks.
    if (java_thread != current) {
      err = suspend_thread(thread_oop, java_thread, /* single_suspend */ true);
      return err;
    }
    // protect thread_oop as a safepoint can be reached in disabler destructor
    self_tobj = Handle(current, thread_oop);
  }
  // Do self suspend for current JavaThread.
  err = suspend_thread(self_tobj(), current, /* single_suspend */ true);
  return err;
} /* end SuspendThread */


// request_count - pre-checked to be greater than or equal to 0
// request_list - pre-checked for null
// results - pre-checked for null
jvmtiError
JvmtiEnv::SuspendThreadList(jint request_count, const jthread* request_list, jvmtiError* results) {
  JavaThread* current = JavaThread::current();
  HandleMark hm(current);
  Handle self_tobj;
  int self_idx = -1;

  {
    JvmtiVTMSTransitionDisabler disabler(true);
    ThreadsListHandle tlh(current);

    for (int i = 0; i < request_count; i++) {
      JavaThread *java_thread = nullptr;
      oop thread_oop = nullptr;
      jthread thread = request_list[i];
      jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), thread, &java_thread, &thread_oop);

      if (thread_oop != nullptr &&
          java_lang_VirtualThread::is_instance(thread_oop) &&
          !JvmtiEnvBase::is_vthread_alive(thread_oop)) {
        err = JVMTI_ERROR_THREAD_NOT_ALIVE;
      }
      if (err != JVMTI_ERROR_NONE) {
        if (thread_oop == nullptr || err != JVMTI_ERROR_INVALID_THREAD) {
          results[i] = err;
          continue;
        }
      }
      if (java_thread == current) {
        self_idx = i;
        self_tobj = Handle(current, thread_oop);
        continue; // self suspend after all other suspends
      }
      results[i] = suspend_thread(thread_oop, java_thread, /* single_suspend */ true);
    }
  }
  // Self suspend after all other suspends if necessary.
  // Do not use JvmtiVTMSTransitionDisabler in context of self suspend to avoid deadlocks.
  if (self_tobj() != nullptr) {
    // there should not be any error for current java_thread
    results[self_idx] = suspend_thread(self_tobj(), current, /* single_suspend */ true);
  }
  // per-thread suspend results returned via results parameter
  return JVMTI_ERROR_NONE;
} /* end SuspendThreadList */


jvmtiError
JvmtiEnv::SuspendAllVirtualThreads(jint except_count, const jthread* except_list) {
  if (get_capabilities()->can_support_virtual_threads == 0) {
    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }
  JavaThread* current = JavaThread::current();
  HandleMark hm(current);
  Handle self_tobj;

  {
    ResourceMark rm(current);
    JvmtiVTMSTransitionDisabler disabler(true);
    ThreadsListHandle tlh(current);
    GrowableArray<jthread>* elist = new GrowableArray<jthread>(except_count);

    jvmtiError err = JvmtiEnvBase::check_thread_list(except_count, except_list);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }

    // Collect threads from except_list for which resumed status must be restored (only for VirtualThread case)
    for (int idx = 0; idx < except_count; idx++) {
      jthread thread = except_list[idx];
      oop thread_oop = JNIHandles::resolve_external_guard(thread);
      if (java_lang_VirtualThread::is_instance(thread_oop) && !JvmtiVTSuspender::is_vthread_suspended(thread_oop)) {
          // is not suspended, so its resumed status must be restored
          elist->append(except_list[idx]);
      }
    }

    for (JavaThreadIteratorWithHandle jtiwh; JavaThread *java_thread = jtiwh.next(); ) {
      oop vt_oop = java_thread->jvmti_vthread();
      if (!java_thread->is_exiting() &&
          !java_thread->is_jvmti_agent_thread() &&
          !java_thread->is_hidden_from_external_view() &&
          vt_oop != nullptr &&
          ((java_lang_VirtualThread::is_instance(vt_oop) &&
            JvmtiEnvBase::is_vthread_alive(vt_oop) &&
            !JvmtiVTSuspender::is_vthread_suspended(vt_oop)) ||
            (vt_oop->is_a(vmClasses::BoundVirtualThread_klass()) && !java_thread->is_suspended())) &&
          !is_in_thread_list(except_count, except_list, vt_oop)
         ) {
        if (java_thread == current) {
          self_tobj = Handle(current, vt_oop);
          continue; // self suspend after all other suspends
        }
        suspend_thread(vt_oop, java_thread, /* single_suspend */ false);
      }
    }
    JvmtiVTSuspender::register_all_vthreads_suspend();

    // Restore resumed state for threads from except list that were not suspended before.
    for (int idx = 0; idx < elist->length(); idx++) {
      jthread thread = elist->at(idx);
      oop thread_oop = JNIHandles::resolve_external_guard(thread);
      if (JvmtiVTSuspender::is_vthread_suspended(thread_oop)) {
        JvmtiVTSuspender::register_vthread_resume(thread_oop);
      }
    }
  }
  // Self suspend after all other suspends if necessary.
  // Do not use JvmtiVTMSTransitionDisabler in context of self suspend to avoid deadlocks.
  if (self_tobj() != nullptr) {
    suspend_thread(self_tobj(), current, /* single_suspend */ false);
  }
  return JVMTI_ERROR_NONE;
} /* end SuspendAllVirtualThreads */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ResumeThread(jthread thread) {
  JvmtiVTMSTransitionDisabler disabler(true);
  JavaThread* current = JavaThread::current();
  ThreadsListHandle tlh(current);

  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current, &java_thread, &thread_oop);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  err = resume_thread(thread_oop, java_thread, /* single_resume */ true);
  return err;
} /* end ResumeThread */


// request_count - pre-checked to be greater than or equal to 0
// request_list - pre-checked for null
// results - pre-checked for null
jvmtiError
JvmtiEnv::ResumeThreadList(jint request_count, const jthread* request_list, jvmtiError* results) {
  oop thread_oop = nullptr;
  JavaThread* java_thread = nullptr;
  JvmtiVTMSTransitionDisabler disabler(true);
  ThreadsListHandle tlh;

  for (int i = 0; i < request_count; i++) {
    jthread thread = request_list[i];
    jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), thread, &java_thread, &thread_oop);

    if (thread_oop != nullptr &&
        java_lang_VirtualThread::is_instance(thread_oop) &&
        !JvmtiEnvBase::is_vthread_alive(thread_oop)) {
      err = JVMTI_ERROR_THREAD_NOT_ALIVE;
    }
    if (err != JVMTI_ERROR_NONE) {
      if (thread_oop == nullptr || err != JVMTI_ERROR_INVALID_THREAD) {
        results[i] = err;
        continue;
      }
    }
    results[i] = resume_thread(thread_oop, java_thread, /* single_resume */ true);
  }
  // per-thread resume results returned via results parameter
  return JVMTI_ERROR_NONE;
} /* end ResumeThreadList */


jvmtiError
JvmtiEnv::ResumeAllVirtualThreads(jint except_count, const jthread* except_list) {
  if (get_capabilities()->can_support_virtual_threads == 0) {
    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }
  jvmtiError err = JvmtiEnvBase::check_thread_list(except_count, except_list);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  ResourceMark rm;
  JvmtiVTMSTransitionDisabler disabler(true);
  GrowableArray<jthread>* elist = new GrowableArray<jthread>(except_count);

  // Collect threads from except_list for which suspended status must be restored (only for VirtualThread case)
  for (int idx = 0; idx < except_count; idx++) {
    jthread thread = except_list[idx];
    oop thread_oop = JNIHandles::resolve_external_guard(thread);
    if (java_lang_VirtualThread::is_instance(thread_oop) && JvmtiVTSuspender::is_vthread_suspended(thread_oop)) {
      // is suspended, so its suspended status must be restored
      elist->append(except_list[idx]);
    }
  }

  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *java_thread = jtiwh.next(); ) {
    oop vt_oop = java_thread->jvmti_vthread();
    if (!java_thread->is_exiting() &&
        !java_thread->is_jvmti_agent_thread() &&
        !java_thread->is_hidden_from_external_view() &&
        vt_oop != nullptr &&
        ((java_lang_VirtualThread::is_instance(vt_oop) &&
          JvmtiEnvBase::is_vthread_alive(vt_oop) &&
          JvmtiVTSuspender::is_vthread_suspended(vt_oop)) ||
          (vt_oop->is_a(vmClasses::BoundVirtualThread_klass()) && java_thread->is_suspended())) &&
        !is_in_thread_list(except_count, except_list, vt_oop)
    ) {
      resume_thread(vt_oop, java_thread, /* single_resume */ false);
    }
  }
  JvmtiVTSuspender::register_all_vthreads_resume();

  // Restore suspended state for threads from except list that were suspended before.
  for (int idx = 0; idx < elist->length(); idx++) {
    jthread thread = elist->at(idx);
    oop thread_oop = JNIHandles::resolve_external_guard(thread);
    if (!JvmtiVTSuspender::is_vthread_suspended(thread_oop)) {
      JvmtiVTSuspender::register_vthread_suspend(thread_oop);
    }
  }
  return JVMTI_ERROR_NONE;
} /* end ResumeAllVirtualThreads */


jvmtiError
JvmtiEnv::StopThread(jthread thread, jobject exception) {
  JavaThread* current_thread = JavaThread::current();

  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);
  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;

  NULL_CHECK(thread, JVMTI_ERROR_INVALID_THREAD);

  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_oop);

  bool is_virtual = thread_oop != nullptr && thread_oop->is_a(vmClasses::BaseVirtualThread_klass());

  if (is_virtual && !is_JavaThread_current(java_thread, thread_oop)) {
    if (!is_vthread_suspended(thread_oop, java_thread)) {
      return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }
    if (java_thread == nullptr) { // unmounted virtual thread
      return JVMTI_ERROR_OPAQUE_FRAME;
    }
  }
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  oop e = JNIHandles::resolve_external_guard(exception);
  NULL_CHECK(e, JVMTI_ERROR_NULL_POINTER);

  JavaThread::send_async_exception(java_thread, e);

  return JVMTI_ERROR_NONE;

} /* end StopThread */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::InterruptThread(jthread thread) {
  JavaThread* current_thread = JavaThread::current();
  HandleMark hm(current_thread);

  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    // For virtual threads we have to call into Java to interrupt:
    Handle obj(current_thread, thread_obj);
    JvmtiJavaUpcallMark jjum(current_thread); // hide JVMTI events for Java upcall
    JavaValue result(T_VOID);
    JavaCalls::call_virtual(&result,
                            obj,
                            vmClasses::Thread_klass(),
                            vmSymbols::interrupt_method_name(),
                            vmSymbols::void_method_signature(),
                            current_thread);

    return JVMTI_ERROR_NONE;
  }

  // Really this should be a Java call to Thread.interrupt to ensure the same
  // semantics, however historically this has not been done for some reason.
  // So we continue with that (which means we don't interact with any Java-level
  // Interruptible object) but we must set the Java-level interrupted state.
  java_lang_Thread::set_interrupted(thread_obj, true);
  java_thread->interrupt();

  return JVMTI_ERROR_NONE;
} /* end InterruptThread */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadInfo(jthread thread, jvmtiThreadInfo* info_ptr) {
  JavaThread* current_thread = JavaThread::current();
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;

  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  // if thread is null the current thread is used
  if (thread == nullptr) {
    java_thread = JavaThread::current();
    thread_oop = get_vthread_or_thread_oop(java_thread);
    if (thread_oop == nullptr || !thread_oop->is_a(vmClasses::Thread_klass())) {
      return JVMTI_ERROR_INVALID_THREAD;
    }
  } else {
    jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), thread, &java_thread, &thread_oop);
    if (err != JVMTI_ERROR_NONE) {
      // We got an error code so we don't have a JavaThread *, but
      // only return an error from here if we didn't get a valid
      // thread_oop.
      // In the virtual thread case the cv_external_thread_to_JavaThread is expected to correctly set
      // the thread_oop and return JVMTI_ERROR_INVALID_THREAD which we ignore here.
      if (thread_oop == nullptr) {
        return err;
      }
    }
  }
  // We have a valid thread_oop so we can return some thread info.

  Handle thread_obj(current_thread, thread_oop);
  Handle name;
  ThreadPriority priority;
  Handle     thread_group;
  Handle context_class_loader;
  bool          is_daemon;

  name = Handle(current_thread, java_lang_Thread::name(thread_obj()));

  if (java_lang_VirtualThread::is_instance(thread_obj())) {
    priority = (ThreadPriority)JVMTI_THREAD_NORM_PRIORITY;
    is_daemon = true;
    if (java_lang_VirtualThread::state(thread_obj()) == java_lang_VirtualThread::TERMINATED) {
      thread_group = Handle(current_thread, nullptr);
    } else {
      thread_group = Handle(current_thread, java_lang_Thread_Constants::get_VTHREAD_GROUP());
    }
  } else {
    priority = java_lang_Thread::priority(thread_obj());
    is_daemon = java_lang_Thread::is_daemon(thread_obj());
    if (java_lang_Thread::get_thread_status(thread_obj()) == JavaThreadStatus::TERMINATED) {
      thread_group = Handle(current_thread, nullptr);
    } else {
      thread_group = Handle(current_thread, java_lang_Thread::threadGroup(thread_obj()));
    }
  }

  oop loader = java_lang_Thread::context_class_loader(thread_obj());
  context_class_loader = Handle(current_thread, loader);

  { const char *n;

    if (name() != nullptr) {
      n = java_lang_String::as_utf8_string(name());
    } else {
      size_t utf8_length = 0;
      n = UNICODE::as_utf8((jchar*) nullptr, utf8_length);
    }

    info_ptr->name = (char *) jvmtiMalloc(strlen(n)+1);
    if (info_ptr->name == nullptr)
      return JVMTI_ERROR_OUT_OF_MEMORY;

    strcpy(info_ptr->name, n);
  }
  info_ptr->is_daemon = is_daemon;
  info_ptr->priority  = priority;

  info_ptr->context_class_loader = (context_class_loader.is_null()) ? nullptr :
                                    jni_reference(context_class_loader);
  info_ptr->thread_group = jni_reference(thread_group);

  return JVMTI_ERROR_NONE;
} /* end GetThreadInfo */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// owned_monitor_count_ptr - pre-checked for null
// owned_monitors_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetOwnedMonitorInfo(jthread thread, jint* owned_monitor_count_ptr, jobject** owned_monitors_ptr) {
  JavaThread* calling_thread = JavaThread::current();
  HandleMark hm(calling_thread);

  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(calling_thread);

  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, calling_thread, &java_thread, &thread_oop);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  if (LockingMode == LM_LEGACY && java_thread == nullptr) {
    *owned_monitor_count_ptr = 0;
    return JVMTI_ERROR_NONE;
  }

  // growable array of jvmti monitors info on the C-heap
  GrowableArray<jvmtiMonitorStackDepthInfo*> *owned_monitors_list =
      new (mtServiceability) GrowableArray<jvmtiMonitorStackDepthInfo*>(1, mtServiceability);

  Handle thread_handle(calling_thread, thread_oop);
  EscapeBarrier eb(java_thread != nullptr, calling_thread, java_thread);
  if (!eb.deoptimize_objects(MaxJavaStackTraceDepth)) {
    delete owned_monitors_list;
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }
  // get owned monitors info with handshake
  GetOwnedMonitorInfoClosure op(this, calling_thread, owned_monitors_list);
  JvmtiHandshake::execute(&op, &tlh, java_thread, thread_handle);
  err = op.result();

  jint owned_monitor_count = owned_monitors_list->length();
  if (err == JVMTI_ERROR_NONE) {
    if ((err = allocate(owned_monitor_count * sizeof(jobject *),
                      (unsigned char**)owned_monitors_ptr)) == JVMTI_ERROR_NONE) {
      // copy into the returned array
      for (int i = 0; i < owned_monitor_count; i++) {
        (*owned_monitors_ptr)[i] =
          ((jvmtiMonitorStackDepthInfo*)owned_monitors_list->at(i))->monitor;
      }
      *owned_monitor_count_ptr = owned_monitor_count;
    }
  }
  // clean up.
  for (int i = 0; i < owned_monitor_count; i++) {
    deallocate((unsigned char*)owned_monitors_list->at(i));
  }
  delete owned_monitors_list;

  return err;
} /* end GetOwnedMonitorInfo */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// monitor_info_count_ptr - pre-checked for null
// monitor_info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetOwnedMonitorStackDepthInfo(jthread thread, jint* monitor_info_count_ptr, jvmtiMonitorStackDepthInfo** monitor_info_ptr) {
  JavaThread* calling_thread = JavaThread::current();
  HandleMark hm(calling_thread);

  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(calling_thread);

  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, calling_thread, &java_thread, &thread_oop);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  if (LockingMode == LM_LEGACY && java_thread == nullptr) {
    *monitor_info_count_ptr = 0;
    return JVMTI_ERROR_NONE;
  }

  // growable array of jvmti monitors info on the C-heap
  GrowableArray<jvmtiMonitorStackDepthInfo*> *owned_monitors_list =
      new (mtServiceability) GrowableArray<jvmtiMonitorStackDepthInfo*>(1, mtServiceability);

  Handle thread_handle(calling_thread, thread_oop);
  EscapeBarrier eb(java_thread != nullptr, calling_thread, java_thread);
  if (!eb.deoptimize_objects(MaxJavaStackTraceDepth)) {
    delete owned_monitors_list;
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }
  // get owned monitors info with handshake
  GetOwnedMonitorInfoClosure op(this, calling_thread, owned_monitors_list);
  JvmtiHandshake::execute(&op, &tlh, java_thread, thread_handle);
  err = op.result();

  jint owned_monitor_count = owned_monitors_list->length();
  if (err == JVMTI_ERROR_NONE) {
    if ((err = allocate(owned_monitor_count * sizeof(jvmtiMonitorStackDepthInfo),
                        (unsigned char**)monitor_info_ptr)) == JVMTI_ERROR_NONE) {
      // copy to output array.
      for (int i = 0; i < owned_monitor_count; i++) {
        (*monitor_info_ptr)[i].monitor =
          ((jvmtiMonitorStackDepthInfo*)owned_monitors_list->at(i))->monitor;
        (*monitor_info_ptr)[i].stack_depth =
          ((jvmtiMonitorStackDepthInfo*)owned_monitors_list->at(i))->stack_depth;
      }
    }
    *monitor_info_count_ptr = owned_monitor_count;
  }

  // clean up.
  for (int i = 0; i < owned_monitor_count; i++) {
    deallocate((unsigned char*)owned_monitors_list->at(i));
  }
  delete owned_monitors_list;

  return err;
} /* end GetOwnedMonitorStackDepthInfo */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// monitor_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetCurrentContendedMonitor(jthread thread, jobject* monitor_ptr) {
  JavaThread* current = JavaThread::current();

  *monitor_ptr = nullptr;

  // get contended monitor information with handshake
  GetCurrentContendedMonitorClosure op(this, current, monitor_ptr);
  JvmtiHandshake::execute(&op, thread);
  return op.result();
} /* end GetCurrentContendedMonitor */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// proc - pre-checked for null
// arg - null is a valid value, must be checked
jvmtiError
JvmtiEnv::RunAgentThread(jthread thread, jvmtiStartFunction proc, const void* arg, jint priority) {
  JavaThread* current_thread = JavaThread::current();

  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;
  ThreadsListHandle tlh(current_thread);
  jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), thread, &java_thread, &thread_oop);
  if (err != JVMTI_ERROR_NONE) {
    // We got an error code so we don't have a JavaThread *, but
    // only return an error from here if we didn't get a valid
    // thread_oop.
    if (thread_oop == nullptr) {
      return err;
    }
    // We have a valid thread_oop.
  }

  if (thread_oop->is_a(vmClasses::BaseVirtualThread_klass())) {
    // No support for virtual threads.
    return JVMTI_ERROR_UNSUPPORTED_OPERATION;
  }
  if (java_thread != nullptr) {
    // 'thread' refers to an existing JavaThread.
    return JVMTI_ERROR_INVALID_THREAD;
  }

  if (priority < JVMTI_THREAD_MIN_PRIORITY || priority > JVMTI_THREAD_MAX_PRIORITY) {
    return JVMTI_ERROR_INVALID_PRIORITY;
  }

  Handle thread_hndl(current_thread, thread_oop);

  JvmtiAgentThread* new_thread = new JvmtiAgentThread(this, proc, arg);

  // At this point it may be possible that no osthread was created for the
  // JavaThread due to lack of resources.
  if (new_thread->osthread() == nullptr) {
    // The new thread is not known to Thread-SMR yet so we can just delete.
    delete new_thread;
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }

  JavaThread::start_internal_daemon(current_thread, new_thread, thread_hndl,
                                    (ThreadPriority)priority);

  return JVMTI_ERROR_NONE;
} /* end RunAgentThread */

  //
  // Thread Group functions
  //

// group_count_ptr - pre-checked for null
// groups_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetTopThreadGroups(jint* group_count_ptr, jthreadGroup** groups_ptr) {
  JavaThread* current_thread = JavaThread::current();

  // Only one top level thread group now.
  *group_count_ptr = 1;

  // Allocate memory to store global-refs to the thread groups.
  // Assume this area is freed by caller.
  *groups_ptr = (jthreadGroup *) jvmtiMalloc((sizeof(jthreadGroup)) * (*group_count_ptr));

  NULL_CHECK(*groups_ptr, JVMTI_ERROR_OUT_OF_MEMORY);

  // Convert oop to Handle, then convert Handle to global-ref.
  {
    HandleMark hm(current_thread);
    Handle system_thread_group(current_thread, Universe::system_thread_group());
    *groups_ptr[0] = jni_reference(system_thread_group);
  }

  return JVMTI_ERROR_NONE;
} /* end GetTopThreadGroups */


// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadGroupInfo(jthreadGroup group, jvmtiThreadGroupInfo* info_ptr) {
  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);

  Handle group_obj (current_thread, JNIHandles::resolve_external_guard(group));
  NULL_CHECK(group_obj(), JVMTI_ERROR_INVALID_THREAD_GROUP);

  const char* name;
  Handle parent_group;
  bool is_daemon;
  ThreadPriority max_priority;

  name         = java_lang_ThreadGroup::name(group_obj());
  parent_group = Handle(current_thread, java_lang_ThreadGroup::parent(group_obj()));
  is_daemon    = java_lang_ThreadGroup::is_daemon(group_obj());
  max_priority = java_lang_ThreadGroup::maxPriority(group_obj());

  info_ptr->is_daemon    = is_daemon;
  info_ptr->max_priority = max_priority;
  info_ptr->parent       = jni_reference(parent_group);

  if (name != nullptr) {
    info_ptr->name = (char*)jvmtiMalloc(strlen(name)+1);
    NULL_CHECK(info_ptr->name, JVMTI_ERROR_OUT_OF_MEMORY);
    strcpy(info_ptr->name, name);
  } else {
    info_ptr->name = nullptr;
  }

  return JVMTI_ERROR_NONE;
} /* end GetThreadGroupInfo */

// thread_count_ptr - pre-checked for null
// threads_ptr - pre-checked for null
// group_count_ptr - pre-checked for null
// groups_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadGroupChildren(jthreadGroup group, jint* thread_count_ptr, jthread** threads_ptr, jint* group_count_ptr, jthreadGroup** groups_ptr) {
  jvmtiError err;
  JavaThread* current_thread = JavaThread::current();
  oop group_obj = JNIHandles::resolve_external_guard(group);
  NULL_CHECK(group_obj, JVMTI_ERROR_INVALID_THREAD_GROUP);

  Handle *thread_objs = nullptr;
  objArrayHandle group_objs;
  jint nthreads = 0;
  jint ngroups = 0;
  int hidden_threads = 0;

  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);

  Handle group_hdl(current_thread, group_obj);

  err = get_live_threads(current_thread, group_hdl, &nthreads, &thread_objs);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  err = get_subgroups(current_thread, group_hdl, &ngroups, &group_objs);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  *group_count_ptr  = ngroups;
  *thread_count_ptr = nthreads;
  *threads_ptr     = new_jthreadArray(nthreads, thread_objs);
  *groups_ptr      = new_jthreadGroupArray(ngroups, group_objs);
  if (nthreads > 0 && *threads_ptr == nullptr) {
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }
  if (ngroups > 0 && *groups_ptr == nullptr) {
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }

  return JVMTI_ERROR_NONE;
} /* end GetThreadGroupChildren */


  //
  // Stack Frame functions
  //

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// max_frame_count - pre-checked to be greater than or equal to 0
// frame_buffer - pre-checked for null
// count_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetStackTrace(jthread thread, jint start_depth, jint max_frame_count, jvmtiFrameInfo* frame_buffer, jint* count_ptr) {
  GetStackTraceClosure op(this, start_depth, max_frame_count, frame_buffer, count_ptr);
  JvmtiHandshake::execute(&op, thread);
  return op.result();
} /* end GetStackTrace */


// max_frame_count - pre-checked to be greater than or equal to 0
// stack_info_ptr - pre-checked for null
// thread_count_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetAllStackTraces(jint max_frame_count, jvmtiStackInfo** stack_info_ptr, jint* thread_count_ptr) {
  jvmtiError err = JVMTI_ERROR_NONE;
  JavaThread* calling_thread = JavaThread::current();

  // JVMTI get stack traces at safepoint.
  VM_GetAllStackTraces op(this, calling_thread, max_frame_count);
  VMThread::execute(&op);
  *thread_count_ptr = op.final_thread_count();
  *stack_info_ptr = op.stack_info();
  err = op.result();
  return err;
} /* end GetAllStackTraces */


// thread_count - pre-checked to be greater than or equal to 0
// thread_list - pre-checked for null
// max_frame_count - pre-checked to be greater than or equal to 0
// stack_info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadListStackTraces(jint thread_count, const jthread* thread_list, jint max_frame_count, jvmtiStackInfo** stack_info_ptr) {
  jvmtiError err = JVMTI_ERROR_NONE;

  if (thread_count == 1) {
    // Use direct handshake if we need to get only one stack trace.
    JavaThread *current_thread = JavaThread::current();

    jthread thread = thread_list[0];

    GetSingleStackTraceClosure op(this, current_thread, thread, max_frame_count);
    JvmtiHandshake::execute(&op, thread);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *stack_info_ptr = op.stack_info();
    }
  } else {
    JvmtiVTMSTransitionDisabler disabler;

    // JVMTI get stack traces at safepoint.
    VM_GetThreadListStackTraces op(this, thread_count, thread_list, max_frame_count);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *stack_info_ptr = op.stack_info();
    }
  }
  return err;
} /* end GetThreadListStackTraces */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// count_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetFrameCount(jthread thread, jint* count_ptr) {
  GetFrameCountClosure op(this, count_ptr);
  JvmtiHandshake::execute(&op, thread);
  return op.result();
} /* end GetFrameCount */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::PopFrame(jthread thread) {
  JavaThread* current_thread = JavaThread::current();
  HandleMark hm(current_thread);

  if (thread == nullptr) {
    return JVMTI_ERROR_INVALID_THREAD;
  }
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  Handle thread_handle(current_thread, thread_obj);

  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = java_thread == current_thread;

  err = check_non_suspended_or_opaque_frame(java_thread, thread_obj, self);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  // retrieve or create the state
  JvmtiThreadState* state = JvmtiThreadState::state_for(java_thread);
  if (state == nullptr) {
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  // Eagerly reallocate scalar replaced objects.
  EscapeBarrier eb(true, current_thread, java_thread);
  if (!eb.deoptimize_objects(1)) {
    // Reallocation of scalar replaced objects failed -> return with error
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }

  MutexLocker mu(JvmtiThreadState_lock);
  UpdateForPopTopFrameClosure op(state);
  JvmtiHandshake::execute(&op, &tlh, java_thread, thread_handle);
  return op.result();
} /* end PopFrame */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// method_ptr - pre-checked for null
// location_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetFrameLocation(jthread thread, jint depth, jmethodID* method_ptr, jlocation* location_ptr) {
  GetFrameLocationClosure op(this, depth, method_ptr, location_ptr);
  JvmtiHandshake::execute(&op, thread);
  return op.result();
} /* end GetFrameLocation */


// Threads_lock NOT held, java_thread not protected by lock
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::NotifyFramePop(jthread thread, jint depth) {
  ResourceMark rm;
  JvmtiVTMSTransitionDisabler disabler(thread);
  JavaThread* current = JavaThread::current();
  ThreadsListHandle tlh(current);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  HandleMark hm(current);
  Handle thread_handle(current, thread_obj);
  JvmtiThreadState *state = JvmtiThreadState::state_for(java_thread, thread_handle);
  if (state == nullptr) {
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  SetOrClearFramePopClosure op(this, state, true /* set */, depth);
  MutexLocker mu(current, JvmtiThreadState_lock);
  JvmtiHandshake::execute(&op, &tlh, java_thread, thread_handle);
  return op.result();
} /* end NotifyFramePop */

// Threads_lock NOT held, java_thread not protected by lock
jvmtiError
JvmtiEnv::ClearAllFramePops(jthread thread) {
  ResourceMark rm;
  JvmtiVTMSTransitionDisabler disabler(thread);
  JavaThread* current = JavaThread::current();
  ThreadsListHandle tlh(current);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  HandleMark hm(current);
  Handle thread_handle(current, thread_obj);
  JvmtiThreadState *state = JvmtiThreadState::state_for(java_thread, thread_handle);
  if (state == nullptr) {
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  SetOrClearFramePopClosure op(this, state, false /* clear all frame pops*/);
  MutexLocker mu(current, JvmtiThreadState_lock);
  JvmtiHandshake::execute(&op, &tlh, java_thread, thread_handle);
  return op.result();
} /* end ClearAllFramePops */

  //
  // Force Early Return functions
  //

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnObject(jthread thread, jobject value) {
  jvalue val;
  val.l = value;
  return force_early_return(thread, val, atos);
} /* end ForceEarlyReturnObject */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnInt(jthread thread, jint value) {
  jvalue val;
  val.i = value;
  return force_early_return(thread, val, itos);
} /* end ForceEarlyReturnInt */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnLong(jthread thread, jlong value) {
  jvalue val;
  val.j = value;
  return force_early_return(thread, val, ltos);
} /* end ForceEarlyReturnLong */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnFloat(jthread thread, jfloat value) {
  jvalue val;
  val.f = value;
  return force_early_return(thread, val, ftos);
} /* end ForceEarlyReturnFloat */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnDouble(jthread thread, jdouble value) {
  jvalue val;
  val.d = value;
  return force_early_return(thread, val, dtos);
} /* end ForceEarlyReturnDouble */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
jvmtiError
JvmtiEnv::ForceEarlyReturnVoid(jthread thread) {
  jvalue val;
  val.j = 0L;
  return force_early_return(thread, val, vtos);
} /* end ForceEarlyReturnVoid */


  //
  // Heap functions
  //

// klass - null is a valid value, must be checked
// initial_object - null is a valid value, must be checked
// callbacks - pre-checked for null
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::FollowReferences(jint heap_filter, jclass klass, jobject initial_object, const jvmtiHeapCallbacks* callbacks, const void* user_data) {
  // check klass if provided
  Klass* k = nullptr;
  if (klass != nullptr) {
    oop k_mirror = JNIHandles::resolve_external_guard(klass);
    if (k_mirror == nullptr) {
      return JVMTI_ERROR_INVALID_CLASS;
    }
    if (java_lang_Class::is_primitive(k_mirror)) {
      return JVMTI_ERROR_NONE;
    }
    k = java_lang_Class::as_Klass(k_mirror);
    if (klass == nullptr) {
      return JVMTI_ERROR_INVALID_CLASS;
    }
  }

  if (initial_object != nullptr) {
    oop init_obj = JNIHandles::resolve_external_guard(initial_object);
    if (init_obj == nullptr) {
      return JVMTI_ERROR_INVALID_OBJECT;
    }
  }

  Thread *thread = Thread::current();
  HandleMark hm(thread);

  TraceTime t("FollowReferences", TRACETIME_LOG(Debug, jvmti, objecttagging));
  JvmtiTagMap::tag_map_for(this)->follow_references(heap_filter, k, initial_object, callbacks, user_data);
  return JVMTI_ERROR_NONE;
} /* end FollowReferences */


// klass - null is a valid value, must be checked
// callbacks - pre-checked for null
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::IterateThroughHeap(jint heap_filter, jclass klass, const jvmtiHeapCallbacks* callbacks, const void* user_data) {
  // check klass if provided
  Klass* k = nullptr;
  if (klass != nullptr) {
    oop k_mirror = JNIHandles::resolve_external_guard(klass);
    if (k_mirror == nullptr) {
      return JVMTI_ERROR_INVALID_CLASS;
    }
    if (java_lang_Class::is_primitive(k_mirror)) {
      return JVMTI_ERROR_NONE;
    }
    k = java_lang_Class::as_Klass(k_mirror);
    if (k == nullptr) {
      return JVMTI_ERROR_INVALID_CLASS;
    }
  }

  TraceTime t("IterateThroughHeap", TRACETIME_LOG(Debug, jvmti, objecttagging));
  JvmtiTagMap::tag_map_for(this)->iterate_through_heap(heap_filter, k, callbacks, user_data);
  return JVMTI_ERROR_NONE;
} /* end IterateThroughHeap */


// tag_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetTag(jobject object, jlong* tag_ptr) {
  oop o = JNIHandles::resolve_external_guard(object);
  NULL_CHECK(o, JVMTI_ERROR_INVALID_OBJECT);
  *tag_ptr = JvmtiTagMap::tag_map_for(this)->get_tag(object);
  return JVMTI_ERROR_NONE;
} /* end GetTag */


jvmtiError
JvmtiEnv::SetTag(jobject object, jlong tag) {
  oop o = JNIHandles::resolve_external_guard(object);
  NULL_CHECK(o, JVMTI_ERROR_INVALID_OBJECT);
  JvmtiTagMap::tag_map_for(this)->set_tag(object, tag);
  return JVMTI_ERROR_NONE;
} /* end SetTag */


// tag_count - pre-checked to be greater than or equal to 0
// tags - pre-checked for null
// count_ptr - pre-checked for null
// object_result_ptr - null is a valid value, must be checked
// tag_result_ptr - null is a valid value, must be checked
jvmtiError
JvmtiEnv::GetObjectsWithTags(jint tag_count, const jlong* tags, jint* count_ptr, jobject** object_result_ptr, jlong** tag_result_ptr) {
  TraceTime t("GetObjectsWithTags", TRACETIME_LOG(Debug, jvmti, objecttagging));
  return JvmtiTagMap::tag_map_for(this)->get_objects_with_tags((jlong*)tags, tag_count, count_ptr, object_result_ptr, tag_result_ptr);
} /* end GetObjectsWithTags */


jvmtiError
JvmtiEnv::ForceGarbageCollection() {
  Universe::heap()->collect(GCCause::_jvmti_force_gc);
  return JVMTI_ERROR_NONE;
} /* end ForceGarbageCollection */


  //
  // Heap (1.0) functions
  //

// object_reference_callback - pre-checked for null
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::IterateOverObjectsReachableFromObject(jobject object, jvmtiObjectReferenceCallback object_reference_callback, const void* user_data) {
  oop o = JNIHandles::resolve_external_guard(object);
  NULL_CHECK(o, JVMTI_ERROR_INVALID_OBJECT);
  JvmtiTagMap::tag_map_for(this)->iterate_over_objects_reachable_from_object(object, object_reference_callback, user_data);
  return JVMTI_ERROR_NONE;
} /* end IterateOverObjectsReachableFromObject */


// heap_root_callback - null is a valid value, must be checked
// stack_ref_callback - null is a valid value, must be checked
// object_ref_callback - null is a valid value, must be checked
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::IterateOverReachableObjects(jvmtiHeapRootCallback heap_root_callback, jvmtiStackReferenceCallback stack_ref_callback, jvmtiObjectReferenceCallback object_ref_callback, const void* user_data) {
  TraceTime t("IterateOverReachableObjects", TRACETIME_LOG(Debug, jvmti, objecttagging));
  JvmtiTagMap::tag_map_for(this)->iterate_over_reachable_objects(heap_root_callback, stack_ref_callback, object_ref_callback, user_data);
  return JVMTI_ERROR_NONE;
} /* end IterateOverReachableObjects */


// heap_object_callback - pre-checked for null
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::IterateOverHeap(jvmtiHeapObjectFilter object_filter, jvmtiHeapObjectCallback heap_object_callback, const void* user_data) {
  TraceTime t("IterateOverHeap", TRACETIME_LOG(Debug, jvmti, objecttagging));
  Thread *thread = Thread::current();
  HandleMark hm(thread);
  JvmtiTagMap::tag_map_for(this)->iterate_over_heap(object_filter, nullptr, heap_object_callback, user_data);
  return JVMTI_ERROR_NONE;
} /* end IterateOverHeap */


// k_mirror - may be primitive, this must be checked
// heap_object_callback - pre-checked for null
// user_data - null is a valid value, must be checked
jvmtiError
JvmtiEnv::IterateOverInstancesOfClass(oop k_mirror, jvmtiHeapObjectFilter object_filter, jvmtiHeapObjectCallback heap_object_callback, const void* user_data) {
  if (java_lang_Class::is_primitive(k_mirror)) {
    // DO PRIMITIVE CLASS PROCESSING
    return JVMTI_ERROR_NONE;
  }
  Klass* klass = java_lang_Class::as_Klass(k_mirror);
  if (klass == nullptr) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  TraceTime t("IterateOverInstancesOfClass", TRACETIME_LOG(Debug, jvmti, objecttagging));
  JvmtiTagMap::tag_map_for(this)->iterate_over_heap(object_filter, klass, heap_object_callback, user_data);
  return JVMTI_ERROR_NONE;
} /* end IterateOverInstancesOfClass */


  //
  // Local Variable functions
  //

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalObject(jthread thread, jint depth, jint slot, jobject* value_ptr) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     current_thread, depth, slot, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().l;
    }
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, current_thread, depth, slot, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().l;
    }
  }
  return err;
} /* end GetLocalObject */

// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalInstance(jthread thread, jint depth, jobject* value_ptr){
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetReceiver op(this, Handle(current_thread, thread_obj),
                                   current_thread, depth, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().l;
    }
  } else {
    // Support for ordinary threads
    VM_GetReceiver op(java_thread, current_thread, depth, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().l;
    }
  }
  return err;
} /* end GetLocalInstance */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalInt(jthread thread, jint depth, jint slot, jint* value_ptr) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_INT, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().i;
    }
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_INT, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().i;
    }
  }
  return err;
} /* end GetLocalInt */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalLong(jthread thread, jint depth, jint slot, jlong* value_ptr) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_LONG, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().j;
    }
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_LONG, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().j;
    }
  }
  return err;
} /* end GetLocalLong */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalFloat(jthread thread, jint depth, jint slot, jfloat* value_ptr) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_FLOAT, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().f;
    }
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_FLOAT, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().f;
    }
  }
  return err;
} /* end GetLocalFloat */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalDouble(jthread thread, jint depth, jint slot, jdouble* value_ptr) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_DOUBLE, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().d;
    }
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_DOUBLE, self);
    VMThread::execute(&op);
    err = op.result();
    if (err == JVMTI_ERROR_NONE) {
      *value_ptr = op.value().d;
    }
  }
  return err;
} /* end GetLocalDouble */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::SetLocalObject(jthread thread, jint depth, jint slot, jobject value) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);
  jvalue val;
  val.l = value;

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_OBJECT, val, self);
    VMThread::execute(&op);
    err = op.result();
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_OBJECT, val, self);
    VMThread::execute(&op);
    err = op.result();
  }
  return err;
} /* end SetLocalObject */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::SetLocalInt(jthread thread, jint depth, jint slot, jint value) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);
  jvalue val;
  val.i = value;

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_INT, val, self);
    VMThread::execute(&op);
    err = op.result();
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_INT, val, self);
    VMThread::execute(&op);
    err = op.result();
  }
  return err;
} /* end SetLocalInt */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::SetLocalLong(jthread thread, jint depth, jint slot, jlong value) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);
  jvalue val;
  val.j = value;

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_LONG, val, self);
    VMThread::execute(&op);
    err = op.result();
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_LONG, val, self);
    VMThread::execute(&op);
    err = op.result();
  }
  return err;
} /* end SetLocalLong */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::SetLocalFloat(jthread thread, jint depth, jint slot, jfloat value) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);
  jvalue val;
  val.f = value;

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_FLOAT, val, self);
    VMThread::execute(&op);
    err = op.result();
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_FLOAT, val, self);
    VMThread::execute(&op);
    err = op.result();
  }
  return err;
} /* end SetLocalFloat */


// thread - NOT protected by ThreadsListHandle and NOT pre-checked
// depth - pre-checked as non-negative
jvmtiError
JvmtiEnv::SetLocalDouble(jthread thread, jint depth, jint slot, jdouble value) {
  JavaThread* current_thread = JavaThread::current();
  // rm object is created to clean up the javaVFrame created in
  // doit_prologue(), but after doit() is finished with it.
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  JvmtiVTMSTransitionDisabler disabler(thread);
  ThreadsListHandle tlh(current_thread);

  JavaThread* java_thread = nullptr;
  oop thread_obj = nullptr;
  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_obj);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  bool self = is_JavaThread_current(java_thread, thread_obj);
  jvalue val;
  val.d = value;

  if (java_lang_VirtualThread::is_instance(thread_obj)) {
    VM_VirtualThreadGetOrSetLocal op(this, Handle(current_thread, thread_obj),
                                     depth, slot, T_DOUBLE, val, self);
    VMThread::execute(&op);
    err = op.result();
  } else {
    // Support for ordinary threads
    VM_GetOrSetLocal op(java_thread, depth, slot, T_DOUBLE, val, self);
    VMThread::execute(&op);
    err = op.result();
  }
  return err;
} /* end SetLocalDouble */


  //
  // Breakpoint functions
  //

// method - pre-checked for validity, but may be null meaning obsolete method
jvmtiError
JvmtiEnv::SetBreakpoint(Method* method, jlocation location) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  if (location < 0) {   // simple invalid location check first
    return JVMTI_ERROR_INVALID_LOCATION;
  }
  // verify that the breakpoint is not past the end of the method
  if (location >= (jlocation) method->code_size()) {
    return JVMTI_ERROR_INVALID_LOCATION;
  }

  ResourceMark rm;
  JvmtiBreakpoint bp(method, location);
  JvmtiBreakpoints& jvmti_breakpoints = JvmtiCurrentBreakpoints::get_jvmti_breakpoints();
  if (jvmti_breakpoints.set(bp) == JVMTI_ERROR_DUPLICATE)
    return JVMTI_ERROR_DUPLICATE;

  if (TraceJVMTICalls) {
    jvmti_breakpoints.print();
  }

  return JVMTI_ERROR_NONE;
} /* end SetBreakpoint */


// method - pre-checked for validity, but may be null meaning obsolete method
jvmtiError
JvmtiEnv::ClearBreakpoint(Method* method, jlocation location) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);

  if (location < 0) {   // simple invalid location check first
    return JVMTI_ERROR_INVALID_LOCATION;
  }

  // verify that the breakpoint is not past the end of the method
  if (location >= (jlocation) method->code_size()) {
    return JVMTI_ERROR_INVALID_LOCATION;
  }

  JvmtiBreakpoint bp(method, location);

  JvmtiBreakpoints& jvmti_breakpoints = JvmtiCurrentBreakpoints::get_jvmti_breakpoints();
  if (jvmti_breakpoints.clear(bp) == JVMTI_ERROR_NOT_FOUND)
    return JVMTI_ERROR_NOT_FOUND;

  if (TraceJVMTICalls) {
    jvmti_breakpoints.print();
  }

  return JVMTI_ERROR_NONE;
} /* end ClearBreakpoint */


  //
  // Watched Field functions
  //

jvmtiError
JvmtiEnv::SetFieldAccessWatch(fieldDescriptor* fdesc_ptr) {
  JvmtiVTMSTransitionDisabler disabler;
  // make sure we haven't set this watch before
  if (fdesc_ptr->is_field_access_watched()) return JVMTI_ERROR_DUPLICATE;
  fdesc_ptr->set_is_field_access_watched(true);

  JvmtiEventController::change_field_watch(JVMTI_EVENT_FIELD_ACCESS, true);

  return JVMTI_ERROR_NONE;
} /* end SetFieldAccessWatch */


jvmtiError
JvmtiEnv::ClearFieldAccessWatch(fieldDescriptor* fdesc_ptr) {
  JvmtiVTMSTransitionDisabler disabler;
  // make sure we have a watch to clear
  if (!fdesc_ptr->is_field_access_watched()) return JVMTI_ERROR_NOT_FOUND;
  fdesc_ptr->set_is_field_access_watched(false);

  JvmtiEventController::change_field_watch(JVMTI_EVENT_FIELD_ACCESS, false);

  return JVMTI_ERROR_NONE;
} /* end ClearFieldAccessWatch */


jvmtiError
JvmtiEnv::SetFieldModificationWatch(fieldDescriptor* fdesc_ptr) {
  JvmtiVTMSTransitionDisabler disabler;
  // make sure we haven't set this watch before
  if (fdesc_ptr->is_field_modification_watched()) return JVMTI_ERROR_DUPLICATE;
  fdesc_ptr->set_is_field_modification_watched(true);

  JvmtiEventController::change_field_watch(JVMTI_EVENT_FIELD_MODIFICATION, true);

  return JVMTI_ERROR_NONE;
} /* end SetFieldModificationWatch */


jvmtiError
JvmtiEnv::ClearFieldModificationWatch(fieldDescriptor* fdesc_ptr) {
  JvmtiVTMSTransitionDisabler disabler;
   // make sure we have a watch to clear
  if (!fdesc_ptr->is_field_modification_watched()) return JVMTI_ERROR_NOT_FOUND;
  fdesc_ptr->set_is_field_modification_watched(false);

  JvmtiEventController::change_field_watch(JVMTI_EVENT_FIELD_MODIFICATION, false);

  return JVMTI_ERROR_NONE;
} /* end ClearFieldModificationWatch */

  //
  // Class functions
  //


// k_mirror - may be primitive, this must be checked
// signature_ptr - null is a valid value, must be checked
// generic_ptr - null is a valid value, must be checked
jvmtiError
JvmtiEnv::GetClassSignature(oop k_mirror, char** signature_ptr, char** generic_ptr) {
  ResourceMark rm;
  bool isPrimitive = java_lang_Class::is_primitive(k_mirror);
  Klass* k = nullptr;
  if (!isPrimitive) {
    k = java_lang_Class::as_Klass(k_mirror);
    NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);
  }
  if (signature_ptr != nullptr) {
    char* result = nullptr;
    if (isPrimitive) {
      char tchar = type2char(java_lang_Class::primitive_type(k_mirror));
      result = (char*) jvmtiMalloc(2);
      result[0] = tchar;
      result[1] = '\0';
    } else {
      const char* class_sig = k->signature_name();
      result = (char *) jvmtiMalloc(strlen(class_sig)+1);
      strcpy(result, class_sig);
    }
    *signature_ptr = result;
  }
  if (generic_ptr != nullptr) {
    *generic_ptr = nullptr;
    if (!isPrimitive && k->is_instance_klass()) {
      Symbol* soo = InstanceKlass::cast(k)->generic_signature();
      if (soo != nullptr) {
        const char *gen_sig = soo->as_C_string();
        if (gen_sig != nullptr) {
          char* gen_result;
          jvmtiError err = allocate(strlen(gen_sig) + 1,
                                    (unsigned char **)&gen_result);
          if (err != JVMTI_ERROR_NONE) {
            return err;
          }
          strcpy(gen_result, gen_sig);
          *generic_ptr = gen_result;
        }
      }
    }
  }
  return JVMTI_ERROR_NONE;
} /* end GetClassSignature */


// k_mirror - may be primitive, this must be checked
// status_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassStatus(oop k_mirror, jint* status_ptr) {
  jint result = 0;
  if (java_lang_Class::is_primitive(k_mirror)) {
    result |= JVMTI_CLASS_STATUS_PRIMITIVE;
  } else {
    Klass* k = java_lang_Class::as_Klass(k_mirror);
    NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);
    result = k->jvmti_class_status();
  }
  *status_ptr = result;

  return JVMTI_ERROR_NONE;
} /* end GetClassStatus */


// k_mirror - may be primitive, this must be checked
// source_name_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetSourceFileName(oop k_mirror, char** source_name_ptr) {
  if (java_lang_Class::is_primitive(k_mirror)) {
     return JVMTI_ERROR_ABSENT_INFORMATION;
  }
  Klass* k_klass = java_lang_Class::as_Klass(k_mirror);
  NULL_CHECK(k_klass, JVMTI_ERROR_INVALID_CLASS);

  if (!k_klass->is_instance_klass()) {
    return JVMTI_ERROR_ABSENT_INFORMATION;
  }

  Symbol* sfnOop = InstanceKlass::cast(k_klass)->source_file_name();
  NULL_CHECK(sfnOop, JVMTI_ERROR_ABSENT_INFORMATION);
  {
    JavaThread* current_thread  = JavaThread::current();
    ResourceMark rm(current_thread);
    const char* sfncp = (const char*) sfnOop->as_C_string();
    *source_name_ptr = (char *) jvmtiMalloc(strlen(sfncp)+1);
    strcpy(*source_name_ptr, sfncp);
  }

  return JVMTI_ERROR_NONE;
} /* end GetSourceFileName */


// k_mirror - may be primitive, this must be checked
// modifiers_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassModifiers(oop k_mirror, jint* modifiers_ptr) {
  jint result = java_lang_Class::modifiers(k_mirror);
  *modifiers_ptr = result;

  return JVMTI_ERROR_NONE;
} /* end GetClassModifiers */


// k_mirror - may be primitive, this must be checked
// method_count_ptr - pre-checked for null
// methods_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassMethods(oop k_mirror, jint* method_count_ptr, jmethodID** methods_ptr) {
  JavaThread* current_thread  = JavaThread::current();
  HandleMark hm(current_thread);

  if (java_lang_Class::is_primitive(k_mirror)) {
    *method_count_ptr = 0;
    *methods_ptr = (jmethodID*) jvmtiMalloc(0 * sizeof(jmethodID));
    return JVMTI_ERROR_NONE;
  }
  Klass* k = java_lang_Class::as_Klass(k_mirror);
  NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);

  // Return CLASS_NOT_PREPARED error as per JVMTI spec.
  if (!(k->jvmti_class_status() & (JVMTI_CLASS_STATUS_PREPARED|JVMTI_CLASS_STATUS_ARRAY) )) {
    return JVMTI_ERROR_CLASS_NOT_PREPARED;
  }

  if (!k->is_instance_klass()) {
    *method_count_ptr = 0;
    *methods_ptr = (jmethodID*) jvmtiMalloc(0 * sizeof(jmethodID));
    return JVMTI_ERROR_NONE;
  }
  InstanceKlass* ik = InstanceKlass::cast(k);
  // Allocate the result and fill it in
  int result_length = ik->methods()->length();
  jmethodID* result_list = (jmethodID*)jvmtiMalloc(result_length * sizeof(jmethodID));
  int index;
  bool jmethodids_found = true;
  int skipped = 0;  // skip overpass methods

  for (index = 0; index < result_length; index++) {
    Method* m = ik->methods()->at(index);
    // Depending on can_maintain_original_method_order capability use the original
    // method ordering indices stored in the class, so we can emit jmethodIDs in
    // the order they appeared in the class file or just copy in current order.
    int result_index = JvmtiExport::can_maintain_original_method_order() ? ik->method_ordering()->at(index) : index;
    assert(result_index >= 0 && result_index < result_length, "invalid original method index");
    if (m->is_overpass()) {
      result_list[result_index] = nullptr;
      skipped++;
      continue;
    }
    jmethodID id;
    if (jmethodids_found) {
      id = m->find_jmethod_id_or_null();
      if (id == nullptr) {
        // If we find an uninitialized value, make sure there is
        // enough space for all the uninitialized values we might
        // find.
        ik->ensure_space_for_methodids(index);
        jmethodids_found = false;
        id = m->jmethod_id();
      }
    } else {
      id = m->jmethod_id();
    }
    result_list[result_index] = id;
  }

  // Fill in return value.
  if (skipped > 0) {
    // copy results skipping null methodIDs
    *methods_ptr = (jmethodID*)jvmtiMalloc((result_length - skipped) * sizeof(jmethodID));
    *method_count_ptr = result_length - skipped;
    for (index = 0, skipped = 0; index < result_length; index++) {
      if (result_list[index] == nullptr) {
        skipped++;
      } else {
        (*methods_ptr)[index - skipped] = result_list[index];
      }
    }
    deallocate((unsigned char *)result_list);
  } else {
    *method_count_ptr = result_length;
    *methods_ptr = result_list;
  }

  return JVMTI_ERROR_NONE;
} /* end GetClassMethods */


// k_mirror - may be primitive, this must be checked
// field_count_ptr - pre-checked for null
// fields_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassFields(oop k_mirror, jint* field_count_ptr, jfieldID** fields_ptr) {
  if (java_lang_Class::is_primitive(k_mirror)) {
    *field_count_ptr = 0;
    *fields_ptr = (jfieldID*) jvmtiMalloc(0 * sizeof(jfieldID));
    return JVMTI_ERROR_NONE;
  }
  JavaThread* current_thread = JavaThread::current();
  HandleMark hm(current_thread);
  Klass* k = java_lang_Class::as_Klass(k_mirror);
  NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);

  // Return CLASS_NOT_PREPARED error as per JVMTI spec.
  if (!(k->jvmti_class_status() & (JVMTI_CLASS_STATUS_PREPARED|JVMTI_CLASS_STATUS_ARRAY) )) {
    return JVMTI_ERROR_CLASS_NOT_PREPARED;
  }

  if (!k->is_instance_klass()) {
    *field_count_ptr = 0;
    *fields_ptr = (jfieldID*) jvmtiMalloc(0 * sizeof(jfieldID));
    return JVMTI_ERROR_NONE;
  }

  InstanceKlass* ik = InstanceKlass::cast(k);

  FilteredJavaFieldStream flds(ik);

  int result_count = flds.field_count();

  // Allocate the result and fill it in.
  jfieldID* result_list = (jfieldID*)jvmtiMalloc(result_count * sizeof(jfieldID));
  for (int i = 0; i < result_count; i++, flds.next()) {
    result_list[i] = jfieldIDWorkaround::to_jfieldID(ik, flds.offset(),
                                                     flds.access_flags().is_static(),
                                                     flds.field_descriptor().is_flat());
  }
  assert(flds.done(), "just checking");

  // Fill in the results
  *field_count_ptr = result_count;
  *fields_ptr = result_list;

  return JVMTI_ERROR_NONE;
} /* end GetClassFields */


// k_mirror - may be primitive, this must be checked
// interface_count_ptr - pre-checked for null
// interfaces_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetImplementedInterfaces(oop k_mirror, jint* interface_count_ptr, jclass** interfaces_ptr) {
  {
    if (java_lang_Class::is_primitive(k_mirror)) {
      *interface_count_ptr = 0;
      *interfaces_ptr = (jclass*) jvmtiMalloc(0 * sizeof(jclass));
      return JVMTI_ERROR_NONE;
    }
    JavaThread* current_thread = JavaThread::current();
    HandleMark hm(current_thread);
    Klass* k = java_lang_Class::as_Klass(k_mirror);
    NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);

    // Return CLASS_NOT_PREPARED error as per JVMTI spec.
    if (!(k->jvmti_class_status() & (JVMTI_CLASS_STATUS_PREPARED|JVMTI_CLASS_STATUS_ARRAY) ))
      return JVMTI_ERROR_CLASS_NOT_PREPARED;

    if (!k->is_instance_klass()) {
      *interface_count_ptr = 0;
      *interfaces_ptr = (jclass*) jvmtiMalloc(0 * sizeof(jclass));
      return JVMTI_ERROR_NONE;
    }

    InstanceKlass* ik = InstanceKlass::cast(k);
    Array<InstanceKlass*>* interface_list = ik->local_interfaces();
    int result_length = (interface_list == nullptr ? 0 : interface_list->length());
    jclass* result_list = (jclass*) jvmtiMalloc(result_length * sizeof(jclass));
    for (int i_index = 0; i_index < result_length; i_index += 1) {
      InstanceKlass* klass_at = interface_list->at(i_index);
      assert(klass_at->is_klass(), "interfaces must be Klass*s");
      assert(klass_at->is_interface(), "interfaces must be interfaces");
      oop mirror_at = klass_at->java_mirror();
      Handle handle_at = Handle(current_thread, mirror_at);
      result_list[i_index] = (jclass) jni_reference(handle_at);
    }
    *interface_count_ptr = result_length;
    *interfaces_ptr = result_list;
  }

  return JVMTI_ERROR_NONE;
} /* end GetImplementedInterfaces */


// k_mirror - may be primitive, this must be checked
// minor_version_ptr - pre-checked for null
// major_version_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassVersionNumbers(oop k_mirror, jint* minor_version_ptr, jint* major_version_ptr) {
  if (java_lang_Class::is_primitive(k_mirror)) {
    return JVMTI_ERROR_ABSENT_INFORMATION;
  }
  Klass* klass = java_lang_Class::as_Klass(k_mirror);

  jint status = klass->jvmti_class_status();
  if (status & (JVMTI_CLASS_STATUS_ERROR)) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  if (status & (JVMTI_CLASS_STATUS_ARRAY)) {
    return JVMTI_ERROR_ABSENT_INFORMATION;
  }

  InstanceKlass* ik = InstanceKlass::cast(klass);
  *minor_version_ptr = ik->minor_version();
  *major_version_ptr = ik->major_version();

  return JVMTI_ERROR_NONE;
} /* end GetClassVersionNumbers */


// k_mirror - may be primitive, this must be checked
// constant_pool_count_ptr - pre-checked for null
// constant_pool_byte_count_ptr - pre-checked for null
// constant_pool_bytes_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetConstantPool(oop k_mirror, jint* constant_pool_count_ptr, jint* constant_pool_byte_count_ptr, unsigned char** constant_pool_bytes_ptr) {
  if (java_lang_Class::is_primitive(k_mirror)) {
    return JVMTI_ERROR_ABSENT_INFORMATION;
  }

  Klass* klass = java_lang_Class::as_Klass(k_mirror);
  Thread *thread = Thread::current();
  ResourceMark rm(thread);

  jint status = klass->jvmti_class_status();
  if (status & (JVMTI_CLASS_STATUS_ERROR)) {
    return JVMTI_ERROR_INVALID_CLASS;
  }
  if (status & (JVMTI_CLASS_STATUS_ARRAY)) {
    return JVMTI_ERROR_ABSENT_INFORMATION;
  }

  InstanceKlass* ik = InstanceKlass::cast(klass);
  JvmtiConstantPoolReconstituter reconstituter(ik);
  if (reconstituter.get_error() != JVMTI_ERROR_NONE) {
    return reconstituter.get_error();
  }

  unsigned char *cpool_bytes;
  int cpool_size = reconstituter.cpool_size();
  if (reconstituter.get_error() != JVMTI_ERROR_NONE) {
    return reconstituter.get_error();
  }
  jvmtiError res = allocate(cpool_size, &cpool_bytes);
  if (res != JVMTI_ERROR_NONE) {
    return res;
  }
  reconstituter.copy_cpool_bytes(cpool_bytes);
  if (reconstituter.get_error() != JVMTI_ERROR_NONE) {
    return reconstituter.get_error();
  }

  constantPoolHandle  constants(thread, ik->constants());
  *constant_pool_count_ptr      = constants->length();
  *constant_pool_byte_count_ptr = cpool_size;
  *constant_pool_bytes_ptr      = cpool_bytes;

  return JVMTI_ERROR_NONE;
} /* end GetConstantPool */


// k_mirror - may be primitive, this must be checked
// is_interface_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsInterface(oop k_mirror, jboolean* is_interface_ptr) {
  {
    bool result = false;
    if (!java_lang_Class::is_primitive(k_mirror)) {
      Klass* k = java_lang_Class::as_Klass(k_mirror);
      if (k != nullptr && k->is_interface()) {
        result = true;
      }
    }
    *is_interface_ptr = result;
  }

  return JVMTI_ERROR_NONE;
} /* end IsInterface */


// k_mirror - may be primitive, this must be checked
// is_array_class_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsArrayClass(oop k_mirror, jboolean* is_array_class_ptr) {
  {
    bool result = false;
    if (!java_lang_Class::is_primitive(k_mirror)) {
      Klass* k = java_lang_Class::as_Klass(k_mirror);
      if (k != nullptr && k->is_array_klass()) {
        result = true;
      }
    }
    *is_array_class_ptr = result;
  }

  return JVMTI_ERROR_NONE;
} /* end IsArrayClass */


// k_mirror - may be primitive, this must be checked
// classloader_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetClassLoader(oop k_mirror, jobject* classloader_ptr) {
  {
    if (java_lang_Class::is_primitive(k_mirror)) {
      *classloader_ptr = (jclass) jni_reference(Handle());
      return JVMTI_ERROR_NONE;
    }
    JavaThread* current_thread = JavaThread::current();
    HandleMark hm(current_thread);
    Klass* k = java_lang_Class::as_Klass(k_mirror);
    NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);

    oop result_oop = k->class_loader();
    if (result_oop == nullptr) {
      *classloader_ptr = (jclass) jni_reference(Handle());
      return JVMTI_ERROR_NONE;
    }
    Handle result_handle = Handle(current_thread, result_oop);
    jclass result_jnihandle = (jclass) jni_reference(result_handle);
    *classloader_ptr = result_jnihandle;
  }
  return JVMTI_ERROR_NONE;
} /* end GetClassLoader */


// k_mirror - may be primitive, this must be checked
// source_debug_extension_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetSourceDebugExtension(oop k_mirror, char** source_debug_extension_ptr) {
  {
    if (java_lang_Class::is_primitive(k_mirror)) {
      return JVMTI_ERROR_ABSENT_INFORMATION;
    }
    Klass* k = java_lang_Class::as_Klass(k_mirror);
    NULL_CHECK(k, JVMTI_ERROR_INVALID_CLASS);
    if (!k->is_instance_klass()) {
      return JVMTI_ERROR_ABSENT_INFORMATION;
    }
    const char* sde = InstanceKlass::cast(k)->source_debug_extension();
    NULL_CHECK(sde, JVMTI_ERROR_ABSENT_INFORMATION);

    {
      *source_debug_extension_ptr = (char *) jvmtiMalloc(strlen(sde)+1);
      strcpy(*source_debug_extension_ptr, sde);
    }
  }

  return JVMTI_ERROR_NONE;
} /* end GetSourceDebugExtension */

  //
  // Object functions
  //

// hash_code_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetObjectHashCode(jobject object, jint* hash_code_ptr) {
  oop mirror = JNIHandles::resolve_external_guard(object);
  NULL_CHECK(mirror, JVMTI_ERROR_INVALID_OBJECT);
  NULL_CHECK(hash_code_ptr, JVMTI_ERROR_NULL_POINTER);

  if (mirror->is_inline_type()) {
    // For inline types, use the klass as a hash code.
    // TBD to improve this (see also JvmtiTagMapKey::get_hash for similar case).
    *hash_code_ptr = (jint)((int64_t)mirror->klass() >> 3);
  } else {
    *hash_code_ptr = (jint)mirror->identity_hash();
  }
  return JVMTI_ERROR_NONE;
} /* end GetObjectHashCode */


// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetObjectMonitorUsage(jobject object, jvmtiMonitorUsage* info_ptr) {
  // This needs to be performed at a safepoint to gather stable data
  // because monitor owner / waiters might not be suspended.
  VM_GetObjectMonitorUsage op(this, JavaThread::current(), object, info_ptr);
  VMThread::execute(&op);
  return op.result();
} /* end GetObjectMonitorUsage */


  //
  // Field functions
  //

// name_ptr - null is a valid value, must be checked
// signature_ptr - null is a valid value, must be checked
// generic_ptr - null is a valid value, must be checked
jvmtiError
JvmtiEnv::GetFieldName(fieldDescriptor* fdesc_ptr, char** name_ptr, char** signature_ptr, char** generic_ptr) {
  JavaThread* current_thread  = JavaThread::current();
  ResourceMark rm(current_thread);
  if (name_ptr == nullptr) {
    // just don't return the name
  } else {
    const char* fieldName = fdesc_ptr->name()->as_C_string();
    *name_ptr =  (char*) jvmtiMalloc(strlen(fieldName) + 1);
    if (*name_ptr == nullptr)
      return JVMTI_ERROR_OUT_OF_MEMORY;
    strcpy(*name_ptr, fieldName);
  }
  if (signature_ptr== nullptr) {
    // just don't return the signature
  } else {
    const char* fieldSignature = fdesc_ptr->signature()->as_C_string();
    *signature_ptr = (char*) jvmtiMalloc(strlen(fieldSignature) + 1);
    if (*signature_ptr == nullptr)
      return JVMTI_ERROR_OUT_OF_MEMORY;
    strcpy(*signature_ptr, fieldSignature);
  }
  if (generic_ptr != nullptr) {
    *generic_ptr = nullptr;
    Symbol* soop = fdesc_ptr->generic_signature();
    if (soop != nullptr) {
      const char* gen_sig = soop->as_C_string();
      if (gen_sig != nullptr) {
        jvmtiError err = allocate(strlen(gen_sig) + 1, (unsigned char **)generic_ptr);
        if (err != JVMTI_ERROR_NONE) {
          return err;
        }
        strcpy(*generic_ptr, gen_sig);
      }
    }
  }
  return JVMTI_ERROR_NONE;
} /* end GetFieldName */


// declaring_class_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetFieldDeclaringClass(fieldDescriptor* fdesc_ptr, jclass* declaring_class_ptr) {
  // As for the GetFieldDeclaringClass method, the XSL generated C++ code that calls it has
  // a jclass of the relevant class or a subclass of it, which is fine in terms of ensuring
  // the holder is kept alive.
  *declaring_class_ptr = get_jni_class_non_null(fdesc_ptr->field_holder());
  return JVMTI_ERROR_NONE;
} /* end GetFieldDeclaringClass */


// modifiers_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetFieldModifiers(fieldDescriptor* fdesc_ptr, jint* modifiers_ptr) {

  AccessFlags resultFlags = fdesc_ptr->access_flags();
  jint result = resultFlags.as_field_flags();
  *modifiers_ptr = result;

  return JVMTI_ERROR_NONE;
} /* end GetFieldModifiers */


// is_synthetic_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsFieldSynthetic(fieldDescriptor* fdesc_ptr, jboolean* is_synthetic_ptr) {
  *is_synthetic_ptr = fdesc_ptr->is_synthetic();
  return JVMTI_ERROR_NONE;
} /* end IsFieldSynthetic */


  //
  // Method functions
  //

// method - pre-checked for validity, but may be null meaning obsolete method
// name_ptr - null is a valid value, must be checked
// signature_ptr - null is a valid value, must be checked
// generic_ptr - null is a valid value, must be checked
jvmtiError
JvmtiEnv::GetMethodName(Method* method, char** name_ptr, char** signature_ptr, char** generic_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  JavaThread* current_thread  = JavaThread::current();

  ResourceMark rm(current_thread); // get the utf8 name and signature
  if (name_ptr == nullptr) {
    // just don't return the name
  } else {
    const char* utf8_name = (const char *) method->name()->as_utf8();
    *name_ptr = (char *) jvmtiMalloc(strlen(utf8_name)+1);
    strcpy(*name_ptr, utf8_name);
  }
  if (signature_ptr == nullptr) {
    // just don't return the signature
  } else {
    const char* utf8_signature = (const char *) method->signature()->as_utf8();
    *signature_ptr = (char *) jvmtiMalloc(strlen(utf8_signature) + 1);
    strcpy(*signature_ptr, utf8_signature);
  }

  if (generic_ptr != nullptr) {
    *generic_ptr = nullptr;
    Symbol* soop = method->generic_signature();
    if (soop != nullptr) {
      const char* gen_sig = soop->as_C_string();
      if (gen_sig != nullptr) {
        jvmtiError err = allocate(strlen(gen_sig) + 1, (unsigned char **)generic_ptr);
        if (err != JVMTI_ERROR_NONE) {
          return err;
        }
        strcpy(*generic_ptr, gen_sig);
      }
    }
  }
  return JVMTI_ERROR_NONE;
} /* end GetMethodName */


// method - pre-checked for validity, but may be null meaning obsolete method
// declaring_class_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetMethodDeclaringClass(Method* method, jclass* declaring_class_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  Klass* k = method->method_holder();
  Handle holder(Thread::current(), k->klass_holder()); // keep the klass alive
  (*declaring_class_ptr) = get_jni_class_non_null(k);
  return JVMTI_ERROR_NONE;
} /* end GetMethodDeclaringClass */


// method - pre-checked for validity, but may be null meaning obsolete method
// modifiers_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetMethodModifiers(Method* method, jint* modifiers_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  (*modifiers_ptr) = method->access_flags().as_method_flags();
  return JVMTI_ERROR_NONE;
} /* end GetMethodModifiers */


// method - pre-checked for validity, but may be null meaning obsolete method
// max_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetMaxLocals(Method* method, jint* max_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  // get max stack
  (*max_ptr) = method->max_locals();
  return JVMTI_ERROR_NONE;
} /* end GetMaxLocals */


// method - pre-checked for validity, but may be null meaning obsolete method
// size_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetArgumentsSize(Method* method, jint* size_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  // get size of arguments

  (*size_ptr) = method->size_of_parameters();
  return JVMTI_ERROR_NONE;
} /* end GetArgumentsSize */


// method - pre-checked for validity, but may be null meaning obsolete method
// entry_count_ptr - pre-checked for null
// table_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLineNumberTable(Method* method, jint* entry_count_ptr, jvmtiLineNumberEntry** table_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  if (!method->has_linenumber_table()) {
    return (JVMTI_ERROR_ABSENT_INFORMATION);
  }

  // The line number table is compressed so we don't know how big it is until decompressed.
  // Decompression is really fast so we just do it twice.

  // Compute size of table
  jint num_entries = 0;
  CompressedLineNumberReadStream stream(method->compressed_linenumber_table());
  while (stream.read_pair()) {
    num_entries++;
  }
  jvmtiLineNumberEntry *jvmti_table =
            (jvmtiLineNumberEntry *)jvmtiMalloc(num_entries * (sizeof(jvmtiLineNumberEntry)));

  // Fill jvmti table
  if (num_entries > 0) {
    int index = 0;
    CompressedLineNumberReadStream stream(method->compressed_linenumber_table());
    while (stream.read_pair()) {
      jvmti_table[index].start_location = (jlocation) stream.bci();
      jvmti_table[index].line_number = (jint) stream.line();
      index++;
    }
    assert(index == num_entries, "sanity check");
  }

  // Set up results
  (*entry_count_ptr) = num_entries;
  (*table_ptr) = jvmti_table;

  return JVMTI_ERROR_NONE;
} /* end GetLineNumberTable */


// method - pre-checked for validity, but may be null meaning obsolete method
// start_location_ptr - pre-checked for null
// end_location_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetMethodLocation(Method* method, jlocation* start_location_ptr, jlocation* end_location_ptr) {

  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  // get start and end location
  (*end_location_ptr) = (jlocation) (method->code_size() - 1);
  if (method->code_size() == 0) {
    // there is no code so there is no start location
    (*start_location_ptr) = (jlocation)(-1);
  } else {
    (*start_location_ptr) = (jlocation)(0);
  }

  return JVMTI_ERROR_NONE;
} /* end GetMethodLocation */


// method - pre-checked for validity, but may be null meaning obsolete method
// entry_count_ptr - pre-checked for null
// table_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetLocalVariableTable(Method* method, jint* entry_count_ptr, jvmtiLocalVariableEntry** table_ptr) {

  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  JavaThread* current_thread  = JavaThread::current();

  // does the klass have any local variable information?
  InstanceKlass* ik = method->method_holder();
  if (!ik->has_localvariable_table()) {
    return (JVMTI_ERROR_ABSENT_INFORMATION);
  }

  ConstantPool* constants = method->constants();
  NULL_CHECK(constants, JVMTI_ERROR_ABSENT_INFORMATION);

  // in the vm localvariable table representation, 6 consecutive elements in the table
  // represent a 6-tuple of shorts
  // [start_pc, length, name_index, descriptor_index, signature_index, index]
  jint num_entries = method->localvariable_table_length();
  jvmtiLocalVariableEntry *jvmti_table = (jvmtiLocalVariableEntry *)
                jvmtiMalloc(num_entries * (sizeof(jvmtiLocalVariableEntry)));

  if (num_entries > 0) {
    LocalVariableTableElement* table = method->localvariable_table_start();
    for (int i = 0; i < num_entries; i++) {
      // get the 5 tuple information from the vm table
      jlocation start_location = (jlocation) table[i].start_bci;
      jint length = (jint) table[i].length;
      int name_index = (int) table[i].name_cp_index;
      int signature_index = (int) table[i].descriptor_cp_index;
      int generic_signature_index = (int) table[i].signature_cp_index;
      jint slot = (jint) table[i].slot;

      // get utf8 name and signature
      char *name_buf = nullptr;
      char *sig_buf = nullptr;
      char *gen_sig_buf = nullptr;
      {
        ResourceMark rm(current_thread);

        const char *utf8_name = (const char *) constants->symbol_at(name_index)->as_utf8();
        name_buf = (char *) jvmtiMalloc(strlen(utf8_name)+1);
        strcpy(name_buf, utf8_name);

        const char *utf8_signature = (const char *) constants->symbol_at(signature_index)->as_utf8();
        sig_buf = (char *) jvmtiMalloc(strlen(utf8_signature)+1);
        strcpy(sig_buf, utf8_signature);

        if (generic_signature_index > 0) {
          const char *utf8_gen_sign = (const char *)
                                       constants->symbol_at(generic_signature_index)->as_utf8();
          gen_sig_buf = (char *) jvmtiMalloc(strlen(utf8_gen_sign)+1);
          strcpy(gen_sig_buf, utf8_gen_sign);
        }
      }

      // fill in the jvmti local variable table
      jvmti_table[i].start_location = start_location;
      jvmti_table[i].length = length;
      jvmti_table[i].name = name_buf;
      jvmti_table[i].signature = sig_buf;
      jvmti_table[i].generic_signature = gen_sig_buf;
      jvmti_table[i].slot = slot;
    }
  }

  // set results
  (*entry_count_ptr) = num_entries;
  (*table_ptr) = jvmti_table;

  return JVMTI_ERROR_NONE;
} /* end GetLocalVariableTable */


// method - pre-checked for validity, but may be null meaning obsolete method
// bytecode_count_ptr - pre-checked for null
// bytecodes_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetBytecodes(Method* method, jint* bytecode_count_ptr, unsigned char** bytecodes_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);

  methodHandle mh(Thread::current(), method);
  jint size = (jint)mh->code_size();
  jvmtiError err = allocate(size, bytecodes_ptr);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  (*bytecode_count_ptr) = size;
  // get byte codes
  JvmtiClassFileReconstituter::copy_bytecodes(mh, *bytecodes_ptr);

  return JVMTI_ERROR_NONE;
} /* end GetBytecodes */


// method - pre-checked for validity, but may be null meaning obsolete method
// is_native_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsMethodNative(Method* method, jboolean* is_native_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  (*is_native_ptr) = method->is_native();
  return JVMTI_ERROR_NONE;
} /* end IsMethodNative */


// method - pre-checked for validity, but may be null meaning obsolete method
// is_synthetic_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsMethodSynthetic(Method* method, jboolean* is_synthetic_ptr) {
  NULL_CHECK(method, JVMTI_ERROR_INVALID_METHODID);
  (*is_synthetic_ptr) = method->is_synthetic();
  return JVMTI_ERROR_NONE;
} /* end IsMethodSynthetic */


// method - pre-checked for validity, but may be null meaning obsolete method
// is_obsolete_ptr - pre-checked for null
jvmtiError
JvmtiEnv::IsMethodObsolete(Method* method, jboolean* is_obsolete_ptr) {
  if (use_version_1_0_semantics() &&
      get_capabilities()->can_redefine_classes == 0) {
    // This JvmtiEnv requested version 1.0 semantics and this function
    // requires the can_redefine_classes capability in version 1.0 so
    // we need to return an error here.
    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }

  if (method == nullptr || method->is_obsolete()) {
    *is_obsolete_ptr = true;
  } else {
    *is_obsolete_ptr = false;
  }
  return JVMTI_ERROR_NONE;
} /* end IsMethodObsolete */

  //
  // Raw Monitor functions
  //

// name - pre-checked for null
// monitor_ptr - pre-checked for null
jvmtiError
JvmtiEnv::CreateRawMonitor(const char* name, jrawMonitorID* monitor_ptr) {
  JvmtiRawMonitor* rmonitor = new (std::nothrow) JvmtiRawMonitor(name);
  NULL_CHECK(rmonitor, JVMTI_ERROR_OUT_OF_MEMORY);

  *monitor_ptr = (jrawMonitorID)rmonitor;

  return JVMTI_ERROR_NONE;
} /* end CreateRawMonitor */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::DestroyRawMonitor(JvmtiRawMonitor * rmonitor) {
  if (Threads::number_of_threads() == 0) {
    // Remove this monitor from pending raw monitors list
    // if it has entered in onload or start phase.
    JvmtiPendingMonitors::destroy(rmonitor);
  } else {
    Thread* thread  = Thread::current();
    if (rmonitor->owner() == thread) {
      // The caller owns this monitor which we are about to destroy.
      // We exit the underlying synchronization object so that the
      // "delete monitor" call below can work without an assertion
      // failure on systems that don't like destroying synchronization
      // objects that are locked.
      int r;
      int recursion = rmonitor->recursions();
      for (int i = 0; i <= recursion; i++) {
        r = rmonitor->raw_exit(thread);
        assert(r == JvmtiRawMonitor::M_OK, "raw_exit should have worked");
        if (r != JvmtiRawMonitor::M_OK) {  // robustness
          return JVMTI_ERROR_INTERNAL;
        }
      }
    }
    if (rmonitor->owner() != nullptr) {
      // The caller is trying to destroy a monitor that is locked by
      // someone else. While this is not forbidden by the JVMTI
      // spec, it will cause an assertion failure on systems that don't
      // like destroying synchronization objects that are locked.
      // We indicate a problem with the error return (and leak the
      // monitor's memory).
      return JVMTI_ERROR_NOT_MONITOR_OWNER;
    }
  }

  delete rmonitor;

  return JVMTI_ERROR_NONE;
} /* end DestroyRawMonitor */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::RawMonitorEnter(JvmtiRawMonitor * rmonitor) {
  if (Threads::number_of_threads() == 0) {
    // No JavaThreads exist so JvmtiRawMonitor enter cannot be
    // used, add this raw monitor to the pending list.
    // The pending monitors will be actually entered when
    // the VM is setup.
    // See transition_pending_raw_monitors in create_vm()
    // in thread.cpp.
    JvmtiPendingMonitors::enter(rmonitor);
  } else {
    Thread* thread = Thread::current();
    // 8266889: raw_enter changes Java thread state, needs WXWrite
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));
    rmonitor->raw_enter(thread);
  }
  return JVMTI_ERROR_NONE;
} /* end RawMonitorEnter */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::RawMonitorExit(JvmtiRawMonitor * rmonitor) {
  jvmtiError err = JVMTI_ERROR_NONE;

  if (Threads::number_of_threads() == 0) {
    // No JavaThreads exist so just remove this monitor from the pending list.
    // Bool value from exit is false if rmonitor is not in the list.
    if (!JvmtiPendingMonitors::exit(rmonitor)) {
      err = JVMTI_ERROR_NOT_MONITOR_OWNER;
    }
  } else {
    Thread* thread = Thread::current();
    int r = rmonitor->raw_exit(thread);
    if (r == JvmtiRawMonitor::M_ILLEGAL_MONITOR_STATE) {
      err = JVMTI_ERROR_NOT_MONITOR_OWNER;
    }
  }
  return err;
} /* end RawMonitorExit */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::RawMonitorWait(JvmtiRawMonitor * rmonitor, jlong millis) {
  Thread* thread = Thread::current();
  // 8266889: raw_wait changes Java thread state, needs WXWrite
  MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));
  int r = rmonitor->raw_wait(millis, thread);

  switch (r) {
  case JvmtiRawMonitor::M_INTERRUPTED:
    return JVMTI_ERROR_INTERRUPT;
  case JvmtiRawMonitor::M_ILLEGAL_MONITOR_STATE:
    return JVMTI_ERROR_NOT_MONITOR_OWNER;
  default:
    return JVMTI_ERROR_NONE;
  }
} /* end RawMonitorWait */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::RawMonitorNotify(JvmtiRawMonitor * rmonitor) {
  Thread* thread = Thread::current();
  int r = rmonitor->raw_notify(thread);

  if (r == JvmtiRawMonitor::M_ILLEGAL_MONITOR_STATE) {
    return JVMTI_ERROR_NOT_MONITOR_OWNER;
  }
  return JVMTI_ERROR_NONE;
} /* end RawMonitorNotify */


// rmonitor - pre-checked for validity
jvmtiError
JvmtiEnv::RawMonitorNotifyAll(JvmtiRawMonitor * rmonitor) {
  Thread* thread = Thread::current();
  int r = rmonitor->raw_notifyAll(thread);

  if (r == JvmtiRawMonitor::M_ILLEGAL_MONITOR_STATE) {
    return JVMTI_ERROR_NOT_MONITOR_OWNER;
  }
  return JVMTI_ERROR_NONE;
} /* end RawMonitorNotifyAll */


  //
  // JNI Function Interception functions
  //


// function_table - pre-checked for null
jvmtiError
JvmtiEnv::SetJNIFunctionTable(const jniNativeInterface* function_table) {
  // Copy jni function table at safepoint.
  VM_JNIFunctionTableCopier copier(function_table);
  VMThread::execute(&copier);

  return JVMTI_ERROR_NONE;
} /* end SetJNIFunctionTable */


// function_table - pre-checked for null
jvmtiError
JvmtiEnv::GetJNIFunctionTable(jniNativeInterface** function_table) {
  *function_table=(jniNativeInterface*)jvmtiMalloc(sizeof(jniNativeInterface));
  if (*function_table == nullptr)
    return JVMTI_ERROR_OUT_OF_MEMORY;
  memcpy(*function_table,(JavaThread::current())->get_jni_functions(),sizeof(jniNativeInterface));
  return JVMTI_ERROR_NONE;
} /* end GetJNIFunctionTable */


  //
  // Event Management functions
  //

jvmtiError
JvmtiEnv::GenerateEvents(jvmtiEvent event_type) {
  // can only generate two event types
  if (event_type != JVMTI_EVENT_COMPILED_METHOD_LOAD &&
      event_type != JVMTI_EVENT_DYNAMIC_CODE_GENERATED) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }

  // for compiled_method_load events we must check that the environment
  // has the can_generate_compiled_method_load_events capability.
  if (event_type == JVMTI_EVENT_COMPILED_METHOD_LOAD) {
    if (get_capabilities()->can_generate_compiled_method_load_events == 0) {
      return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    return JvmtiCodeBlobEvents::generate_compiled_method_load_events(this);
  } else {
    return JvmtiCodeBlobEvents::generate_dynamic_code_events(this);
  }

} /* end GenerateEvents */


  //
  // Extension Mechanism functions
  //

// extension_count_ptr - pre-checked for null
// extensions - pre-checked for null
jvmtiError
JvmtiEnv::GetExtensionFunctions(jint* extension_count_ptr, jvmtiExtensionFunctionInfo** extensions) {
  return JvmtiExtensions::get_functions(this, extension_count_ptr, extensions);
} /* end GetExtensionFunctions */


// extension_count_ptr - pre-checked for null
// extensions - pre-checked for null
jvmtiError
JvmtiEnv::GetExtensionEvents(jint* extension_count_ptr, jvmtiExtensionEventInfo** extensions) {
  return JvmtiExtensions::get_events(this, extension_count_ptr, extensions);
} /* end GetExtensionEvents */


// callback - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetExtensionEventCallback(jint extension_event_index, jvmtiExtensionEvent callback) {
  return JvmtiExtensions::set_event_callback(this, extension_event_index, callback);
} /* end SetExtensionEventCallback */

  //
  // Timers functions
  //

// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetCurrentThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
  os::current_thread_cpu_time_info(info_ptr);
  return JVMTI_ERROR_NONE;
} /* end GetCurrentThreadCpuTimerInfo */


// nanos_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetCurrentThreadCpuTime(jlong* nanos_ptr) {
  Thread* thread = Thread::current();

  // Surprisingly the GetCurrentThreadCpuTime is used by non-JavaThread's.
  if (thread->is_Java_thread()) {
    if (JavaThread::cast(thread)->is_vthread_mounted()) {
      // No support for a VirtualThread (yet).
      return JVMTI_ERROR_UNSUPPORTED_OPERATION;
    }
  }
  *nanos_ptr = os::current_thread_cpu_time();
  return JVMTI_ERROR_NONE;
} /* end GetCurrentThreadCpuTime */


// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
  os::thread_cpu_time_info(info_ptr);
  return JVMTI_ERROR_NONE;
} /* end GetThreadCpuTimerInfo */


// nanos_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetThreadCpuTime(jthread thread, jlong* nanos_ptr) {
  JavaThread* current_thread = JavaThread::current();
  ThreadsListHandle tlh(current_thread);
  JavaThread* java_thread = nullptr;
  oop thread_oop = nullptr;

  jvmtiError err = get_threadOop_and_JavaThread(tlh.list(), thread, current_thread, &java_thread, &thread_oop);

  if (thread_oop != nullptr && thread_oop->is_a(vmClasses::BaseVirtualThread_klass())) {
    // No support for virtual threads (yet).
    return JVMTI_ERROR_UNSUPPORTED_OPERATION;
  }
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  NULL_CHECK(nanos_ptr, JVMTI_ERROR_NULL_POINTER);

  *nanos_ptr = os::thread_cpu_time(java_thread);
  return JVMTI_ERROR_NONE;
} /* end GetThreadCpuTime */


// info_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetTimerInfo(jvmtiTimerInfo* info_ptr) {
  os::javaTimeNanos_info(info_ptr);
  return JVMTI_ERROR_NONE;
} /* end GetTimerInfo */


// nanos_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetTime(jlong* nanos_ptr) {
  *nanos_ptr = os::javaTimeNanos();
  return JVMTI_ERROR_NONE;
} /* end GetTime */


// processor_count_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetAvailableProcessors(jint* processor_count_ptr) {
  *processor_count_ptr = os::active_processor_count();
  return JVMTI_ERROR_NONE;
} /* end GetAvailableProcessors */

jvmtiError
JvmtiEnv::SetHeapSamplingInterval(jint sampling_interval) {
  if (sampling_interval < 0) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }
  ThreadHeapSampler::set_sampling_interval(sampling_interval);
  return JVMTI_ERROR_NONE;
} /* end SetHeapSamplingInterval */

  //
  // System Properties functions
  //

// count_ptr - pre-checked for null
// property_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetSystemProperties(jint* count_ptr, char*** property_ptr) {
  jvmtiError err = JVMTI_ERROR_NONE;

  // Get the number of readable properties.
  *count_ptr = Arguments::PropertyList_readable_count(Arguments::system_properties());

  // Allocate memory to hold the exact number of readable properties.
  err = allocate(*count_ptr * sizeof(char *), (unsigned char **)property_ptr);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }
  int readable_count = 0;
  // Loop through the system properties until all the readable properties are found.
  for (SystemProperty* p = Arguments::system_properties(); p != nullptr && readable_count < *count_ptr; p = p->next()) {
    if (p->readable()) {
      const char *key = p->key();
      char **tmp_value = *property_ptr+readable_count;
      readable_count++;
      err = allocate((strlen(key)+1) * sizeof(char), (unsigned char**)tmp_value);
      if (err == JVMTI_ERROR_NONE) {
        strcpy(*tmp_value, key);
      } else {
        // clean up previously allocated memory.
        for (int j = 0; j < readable_count; j++) {
          Deallocate((unsigned char*)*property_ptr+j);
        }
        Deallocate((unsigned char*)property_ptr);
        break;
      }
    }
  }
  assert(err != JVMTI_ERROR_NONE || readable_count == *count_ptr, "Bad readable property count");
  return err;
} /* end GetSystemProperties */


// property - pre-checked for null
// value_ptr - pre-checked for null
jvmtiError
JvmtiEnv::GetSystemProperty(const char* property, char** value_ptr) {
  jvmtiError err = JVMTI_ERROR_NONE;
  const char *value;

  // Return JVMTI_ERROR_NOT_AVAILABLE if property is not readable or doesn't exist.
  value = Arguments::PropertyList_get_readable_value(Arguments::system_properties(), property);
  if (value == nullptr) {
    err =  JVMTI_ERROR_NOT_AVAILABLE;
  } else {
    err = allocate((strlen(value)+1) * sizeof(char), (unsigned char **)value_ptr);
    if (err == JVMTI_ERROR_NONE) {
      strcpy(*value_ptr, value);
    }
  }
  return err;
} /* end GetSystemProperty */


// property - pre-checked for null
// value - null is a valid value, must be checked
jvmtiError
JvmtiEnv::SetSystemProperty(const char* property, const char* value_ptr) {
  for (SystemProperty* p = Arguments::system_properties(); p != nullptr; p = p->next()) {
    if (strcmp(property, p->key()) == 0) {
      if (p->writeable()) {
        if (p->set_value(value_ptr, AllocFailStrategy::RETURN_NULL)) {
          return JVMTI_ERROR_NONE;
        } else {
          return JVMTI_ERROR_OUT_OF_MEMORY;
        }
      } else {
        // We found a property, but it's not writeable
        return JVMTI_ERROR_NOT_AVAILABLE;
      }
    }
  }

  // We cannot find a property of the given name
  return JVMTI_ERROR_NOT_AVAILABLE;
} /* end SetSystemProperty */
