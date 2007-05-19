
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <jni.h>
#include <ffi.h>

#ifdef __linux__
#  include <sys/mman.h>
#endif
#include "dispatch.h"

#ifdef __cplusplus
extern "C" {
#endif

static ffi_type *get_ffi_type(char jtype);
static void callback_dispatch(ffi_cif*, void*, void**, void*);
static ffi_closure* alloc_closure();
static void free_closure(ffi_closure *closure);

callback*
create_callback(JNIEnv* env, jobject lib, jobject obj, jobject method,
                jobjectArray param_types, jclass return_type) {
  callback* cb;
  ffi_abi abi = FFI_DEFAULT_ABI;
  int args_size = 0;
  jsize argc;
  JavaVM* vm;
  int i;

  if ((*env)->GetJavaVM(env, &vm) != JNI_OK) {
    throwByName(env, "java/lang/UnsatisfiedLinkError",
                "Can't get Java VM");
    return NULL;
  }
  argc = (*env)->GetArrayLength(env, param_types);
  cb = (callback *)malloc(sizeof(callback));
  cb->ffi_closure = alloc_closure();
  cb->object = (*env)->NewWeakGlobalRef(env, obj);
  cb->methodID = (*env)->FromReflectedMethod(env, method);
  cb->vm = vm;
  for (i=0;i < argc;i++) {
    jclass cls = (*env)->GetObjectArrayElement(env, param_types, i);
    char jtype = get_jtype(env, cls);
    cb->ffi_args[i] = get_ffi_type(jtype);
  }
  cb->return_jtype = get_jtype(env, return_type);

#ifdef _WIN32
  {
    jclass cls = (*env)->FindClass(env, "com/sun/jna/win32/StdCall");
    if ((*env)->IsInstanceOf(env, obj, cls)) {
      abi = FFI_STDCALL;
    }
  }
#endif // _WIN32

  ffi_prep_cif(&cb->ffi_cif, abi, argc, get_ffi_type(cb->return_jtype),
      &cb->ffi_args[0]);
  ffi_prep_closure(cb->ffi_closure, &cb->ffi_cif, callback_dispatch, cb);
  return cb;
}
void 
free_callback(JNIEnv* env, callback *cb) {
  (*env)->DeleteWeakGlobalRef(env, cb->object);
  free_closure(cb->ffi_closure);
  free(cb);
}

static ffi_type*
get_ffi_type(char jtype) {
  switch (jtype) {
  case 'Z': 
  case 'B': 
  case 'C': 
  case 'S':
  case 'I':
    return &ffi_type_sint32;
  case 'J':
    return &ffi_type_sint64;
  case 'F':
    return &ffi_type_float;
  case 'D':
    return &ffi_type_double;
  case 'L':
  default:
    return &ffi_type_pointer;
  }
}
  
static void
callback_dispatch(ffi_cif* cif, void* resp, void** cbargs, void* user_data) {
  callback* cb = (callback *) user_data;
  JavaVM* jvm = cb->vm;
  jobject obj;
  jmethodID mid;
  jvalue args[MAX_NARGS];
  JNIEnv* env;
  int attached;
  unsigned int i;

  attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;
  if (!attached) {
    if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
      fprintf(stderr, "Can't attach to current thread\n");
      return;
    }
  }

  // NOTE: some targets may require alignment of stack items...
  for (i=0;i < cif->nargs;i++) {
    switch(cif->arg_types[i]->type) {
    case FFI_TYPE_POINTER:
      // TODO: create a corresponding java structure type
      // based on the callback argument type
      args[i].l = newJavaPointer(env, *(void **) cbargs[i]);
      break;
    case FFI_TYPE_SINT64:
    case FFI_TYPE_UINT64:
      args[i].j = *(jlong *)cbargs[i];
      break;
    case FFI_TYPE_FLOAT:
      args[i].f = *(float *)cbargs[i];
      break;
    case FFI_TYPE_DOUBLE:
      args[i].d = *(double *)cbargs[i];
    default:
      args[i].i = *(int *)cbargs[i];
      break;
    }
  }

  obj = (*env)->NewLocalRef(env, cb->object);
  mid = cb->methodID;

  // Avoid calling back to a GC'd object
  if ((*env)->IsSameObject(env, obj, NULL)) {
    memset(resp, 0, cif->rtype->size); // Just return 0?
  }
  else switch(cb->return_jtype) {
  case 'Z':
    *(int*)resp = (*env)->CallBooleanMethodA(env, obj, mid, args); break;    
  case 'B':
    *(int*)resp = (*env)->CallByteMethodA(env, obj, mid, args); break;
  case 'C':
    *(int*)resp = (*env)->CallCharMethodA(env, obj, mid, args); break;
  case 'S':
    *(int*)resp = (*env)->CallShortMethodA(env, obj, mid, args); break;
  case 'I':
    *(int*)resp = (*env)->CallIntMethodA(env, obj, mid, args); break;
  case 'J':
    *(long long*)resp = (*env)->CallLongMethodA(env, obj, mid, args); break;
  case 'F':
    *(float*)resp = (*env)->CallFloatMethodA(env, obj, mid, args); break;
  case 'D':
    *(double*)resp = (*env)->CallDoubleMethodA(env, obj, mid, args); break;
  case 'L':
  default:
    *(void**)resp = (*env)->CallObjectMethodA(env, obj, mid, args); break;
  }

  if (!attached) {
    (*jvm)->DetachCurrentThread(jvm);
  }
}

#if defined(__linux__) && defined(__LP64__)
static ffi_closure*
alloc_closure()
{
    void* mem = mmap(0, sizeof(ffi_closure),
        PROT_EXEC | PROT_READ | PROT_WRITE,
	MAP_ANON | MAP_PRIVATE, -1, 0);
    return (ffi_closure*)mem;
}
static void
free_closure(ffi_closure *closure) 
{
    munmap(closure, sizeof(ffi_closure));
}
#else
static ffi_closure*
alloc_closure()
{
  return (ffi_closure *)calloc(1, sizeof(ffi_closure));
}
static void
free_closure(ffi_closure *closure) 
{
  free(closure);
}
#endif

#ifdef __cplusplus
}
#endif
