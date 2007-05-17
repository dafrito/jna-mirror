
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <jni.h>
#include <ffi.h>

#include "dispatch.h"

#ifdef __cplusplus
extern "C" {
#endif

static type_t get_type(char type);
static ffi_type *get_ffi_type(type_t type);
static void callback_dispatch(ffi_cif*, void*, void**, void*);
static ffi_closure* alloc_closure();
static void free_closure(ffi_closure *closure);

callback*
create_callback(JNIEnv* env, jobject lib, jobject obj, jobject method,
                jobjectArray param_types, jclass return_type) {
  callback* cb;
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
  cb->param_count = argc;
  cb->vm = vm;
  for (i=0;i < argc;i++) {
    jclass cls = (*env)->GetObjectArrayElement(env, param_types, i);
    char jtype = get_jtype(env, cls);
    type_t type = get_type(jtype);
    cb->param_jtypes[i] = jtype;
    cb->ffi_args[i] = get_ffi_type(type);
  }
  cb->return_jtype = get_jtype(env, return_type);
  cb->return_type = get_type(cb->return_jtype);
  ffi_prep_cif(&cb->ffi_cif, FFI_DEFAULT_ABI, argc,
      get_ffi_type(cb->return_type),
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
get_ffi_type(type_t type) {
  switch (type) {
  case TYPE_INT32:
    return &ffi_type_sint32;
  case TYPE_INT64:
    return &ffi_type_sint64;
  case TYPE_FP32:
    return &ffi_type_float;
  case TYPE_FP64:
    return &ffi_type_double;
  case TYPE_PTR:
    return &ffi_type_pointer;
  }
  return &ffi_type_void;
}
  
static type_t
get_type(char type) {
  switch(type) {
  case 'Z': 
  case 'B': 
  case 'C': 
  case 'S':
  case 'I':
    return TYPE_INT32; 
  case 'J':
    return TYPE_INT64; 
  case 'F':
    return TYPE_FP32; 
  case 'D':
    return TYPE_FP64; 
  case 'L':
  default:
    return TYPE_PTR; 
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
  int i;

  attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;
  if (!attached) {
    if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
      fprintf(stderr, "Can't attach to current thread\n");
      return;
    }
  }

  // NOTE: some targets may require alignment of stack items...
  for (i=0;i < cb->param_count;i++) {
    switch(cb->param_jtypes[i]) {
    case 'L':
      // TODO: create a corresponding java structure type
      // based on the callback argument type
      args[i].l = newJavaPointer(env, *(void **) cbargs[i]);
      break;
    case 'J':
      args[i].j = *(jlong *)cbargs[i];
      break;
    case 'F':
      args[i].f = *(float *)cbargs[i];
      break;
    case 'D':
      args[i].d = *(double *)cbargs[i];
      break;
    case 'Z':
    case 'B':
    case 'S':
    case 'I':
    default:
      args[i].i = *(int *)cbargs[i];
      break;
    }
  }

  obj = (*env)->NewLocalRef(env, cb->object);
  mid = cb->methodID;

  // Avoid calling back to a GC'd object
  if ((*env)->IsSameObject(env, obj, NULL)) {
    *(long long *)resp = 0;
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

#ifdef notyet
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
