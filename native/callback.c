
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <jni.h>
#include <ffi.h>

#ifdef __linux__
#  include <sys/mman.h>
#endif
#include "dispatch.h"
#include "com_sun_jna_CallbackReference.h"

#ifdef __cplusplus
extern "C" {
#endif

static ffi_type *get_ffi_type(char jtype);
static void callback_dispatch(ffi_cif*, void*, void**, void*);
static void callback_proxy_dispatch(ffi_cif*, void*, void**, void*);
static ffi_closure* alloc_closure();
static void free_closure(ffi_closure *closure);
static callback* create_callback(JNIEnv*, jobject, jobject,
                                 jobjectArray, jclass, jint);
static void free_callback(JNIEnv*, callback*);
/*
 * Class:     com_sun_jna_CallbackReference
 * Method:    createNativeCallback
 * Signature: (Lcom/sun/jna/Callback;Ljava/lang/reflect/Method;[Ljava/lang/Class;Ljava/lang/Class;I)Lcom/sun/jna/Pointer;
 */
JNIEXPORT jobject JNICALL 
Java_com_sun_jna_CallbackReference_createNativeCallback(JNIEnv *env, jclass cls, jobject obj, 
  jobject method, jobjectArray param_types, jclass return_type, jint calling_convention)
{
  callback* cb =
    create_callback(env, obj, method, param_types, return_type, calling_convention);
  if (cb == NULL) {
    return NULL;
  }
  return newJavaPointer(env, cb);
}

/*
 * Class:     com_sun_jna_CallbackReference
 * Method:    freeNativeCallback
 * Signature: (J)V
 */
JNIEXPORT void JNICALL 
Java_com_sun_jna_CallbackReference_freeNativeCallback(JNIEnv *env, jclass cls, jlong ptr)
{
  free_callback(env, (callback*)L2A(ptr));
}

callback*
create_callback(JNIEnv* env, jobject obj, jobject method,
                jobjectArray param_types, jclass return_type, jint calling_convention) {
  callback* cb;
  ffi_abi abi = FFI_DEFAULT_ABI;
  int args_size = 0;
  jsize argc;
  JavaVM* vm;
  int i;
  void (*dispatch)(ffi_cif*, void*, void**, void*) = callback_dispatch;

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
  cb->return_type = return_type;
  cb->return_jtype = get_jtype(env, return_type);
  jclass cls = (*env)->FindClass(env, "com/sun/jna/CallbackProxy");
  cb->is_proxy = (*env)->IsInstanceOf(env, obj, cls);
 
  for (i=0;i < argc;i++) {
    jclass cls = (*env)->GetObjectArrayElement(env, param_types, i);
    cb->param_jtypes[i] = get_jtype(env, cls);
    cb->ffi_args[i] = get_ffi_type(cb->param_jtypes[i]);
  }

  if (cb->is_proxy) {
    dispatch = callback_proxy_dispatch;
    for (i=0;i < argc;i++) {
        switch(cb->param_jtypes[i]) {
        case 'Z':
          cb->param_classes[i] = classBoolean;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(Z)V");
          break;
        case 'B':
          cb->param_classes[i] = classByte;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(B)V");
          break;
        case 'C':
          cb->param_classes[i] = (*env)->FindClass(env, "java/lang/Char");
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(C)V");
          break;
        case 'S':
          cb->param_classes[i] = classShort;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(S)V");
          break;
        case 'I':
          cb->param_classes[i] = classInteger;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(I)V");
          break;
        case 'J':
          cb->param_classes[i] = classLong;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(J)V");
          break;
        case 'F':
          cb->param_classes[i] = classFloat;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(F)V");
          break;
        case 'D':
          cb->param_classes[i] = classDouble;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(D)V");
          break;
        case 'L':
          cb->param_classes[i] = classPointer;
          cb->param_constructors[i] = (*env)->GetMethodID(env, cb->param_classes[i], "<init>", "(J)V");
          break;
        }
    }
    
  }
  

#ifdef _WIN32
  if (calling_convention == CALLCONV_STDCALL) {
    abi = FFI_STDCALL;
  }
#endif // _WIN32

  ffi_prep_cif(&cb->ffi_cif, abi, argc, get_ffi_type(cb->return_jtype),
      &cb->ffi_args[0]);
  ffi_prep_closure(cb->ffi_closure, &cb->ffi_cif, dispatch, cb);
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
  jobject obj, ret;
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
    ret = (*env)->CallObjectMethodA(env, obj, mid, args);
    if (ret != 0 && (*env)->IsSameObject(env, classPointer, cb->return_type)) {
      *(long long *)resp = (*env)->GetLongField(env, ret, FID_Pointer_peer);
    } 
    else {
      *(jobject*)resp = 0;
    }

    *(void **)resp = ret;
  }

  if (!attached) {
    (*jvm)->DetachCurrentThread(jvm);
  }
}

static void
callback_proxy_dispatch(ffi_cif* cif, void* resp, void** cbargs, void* user_data) {
    callback* cb = (callback *) user_data;
    JavaVM* jvm = cb->vm;
    jobject obj;
    jmethodID mid;
    jvalue args;
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

    jclass cls = (*env)->FindClass(env, "java/lang/Object");
    jobjectArray array = (*env)->NewObjectArray(env, cif->nargs, cls, NULL);
    for (i=0;i < cif->nargs;i++) {
      jobject obj = 0;
      jvalue jarg;
      switch(cb->param_jtypes[i]) {
      case 'Z':
        jarg.z = *(int *)cbargs[i] == 0 ? 0 : 1;
        break;
      case 'B':
        jarg.b = *(char*)cbargs[i];
        break;
      case 'C':
        jarg.i = *(int *)cbargs[i];
        break;
      case 'S':
        jarg.s = *(short *)cbargs[i];
        break;
      case 'I':
        jarg.i = *(int *)cbargs[i];
        break;
      case 'J':
        jarg.j = *(jlong *)cbargs[i];
        break;
      case 'F':
        jarg.f = *(float *)cbargs[i];
        break;
      case 'D':
        jarg.d = *(double *)cbargs[i];
        break;
      case 'L':
        jarg.j = *(jlong *)cbargs[i];
        break;
      }
      obj = (*env)->NewObjectA(env, cb->param_classes[i], cb->param_constructors[i], &jarg);
      (*env)->SetObjectArrayElement(env, array, i, obj);
    }
    args.l = array;
  
    obj = (*env)->NewLocalRef(env, cb->object);
    mid = cb->methodID;

    // Avoid calling back to a GC'd object
    if ((*env)->IsSameObject(env, obj, NULL)) {
        memset(resp, 0, cif->rtype->size); // Just return 0?
    }
    else {
        jobject ret = (*env)->CallObjectMethodA(env, obj, mid, &args);
        switch (cb->return_jtype) {
        case 'L':
            if ((*env)->IsSameObject(env, classPointer, cb->return_type)) {
                *(long long *)resp = (*env)->GetLongField(env, ret, FID_Pointer_peer);
            }
            break;
        case 'I':
            if ((*env)->IsInstanceOf(env, ret, classInteger)) {
                *(int*)resp = (*env)->GetIntField(env, ret, FID_Integer_value);
            }
            break;
        case 'J':
            if ((*env)->IsInstanceOf(env, ret, classLong)) {
                *(jlong*)resp = (*env)->GetLongField(env, ret, FID_Long_value);
            }
            break;
        case 'S':
            if ((*env)->IsInstanceOf(env, ret, classShort)) {
                *(short*)resp = (*env)->GetLongField(env, ret, FID_Short_value);
            }
            break;
        case 'B':
            if ((*env)->IsInstanceOf(env, ret, classByte)) {
                *(char*)resp = (*env)->GetLongField(env, ret, FID_Byte_value);
            }
            break;
        case 'Z':
            if ((*env)->IsInstanceOf(env, ret, classBoolean)) {
                *(int*)resp = (*env)->GetBooleanField(env, ret, FID_Boolean_value);
            }
            break;
        }
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
