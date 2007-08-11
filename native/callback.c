
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <jni.h>
#include <ffi.h>

#if !defined(_WIN32)
#  include <sys/types.h>
#  include <sys/param.h>
#  include <sys/user.h> /* for PAGE_SIZE */
#  include <sys/mman.h>
#  ifdef sun
#    include <sys/sysmacros.h>
#  endif
#  include <sys/queue.h>
#  include <pthread.h>
#  define MMAP_CLOSURE
#endif
#include "dispatch.h"
#include "com_sun_jna_CallbackReference.h"

#ifdef __cplusplus
extern "C" {
#endif

static ffi_type *get_ffi_type(char jtype);
static ffi_type *get_ffi_rtype(char jtype);
static void callback_dispatch(ffi_cif*, void*, void**, void*);
static void callback_proxy_dispatch(ffi_cif*, void*, void**, void*);
static ffi_closure* alloc_closure(JNIEnv *env);
static void free_closure(JNIEnv* env, ffi_closure *closure);
static callback* create_callback(JNIEnv*, jobject, jobject,
                                 jobjectArray, jclass, jint);
static void free_callback(JNIEnv*, callback*);

static jclass classObject;
static jclass classClass;
static jclass classMethod;
static jclass classBoolean, classPrimitiveBoolean;
static jclass classByte, classPrimitiveByte;
static jclass classCharacter, classPrimitiveCharacter;
static jclass classShort, classPrimitiveShort;
static jclass classInteger, classPrimitiveInteger;
static jclass classLong, classPrimitiveLong;
static jclass classFloat, classPrimitiveFloat;
static jclass classDouble, classPrimitiveDouble;
static jclass classString;
static jclass classPointer;
static jclass classByteBuffer;
static jclass classCallbackProxy;

static jmethodID MID_getClass;
static jmethodID MID_Class_getComponentType;
static jmethodID MID_String_getBytes;
static jmethodID MID_String_toCharArray;
static jmethodID MID_String_init_bytes;
static jmethodID MID_Pointer_init;
static jmethodID MID_Method_getReturnType;
static jmethodID MID_Method_getParameterTypes;
static jmethodID MID_Boolean_init;
static jmethodID MID_Byte_init;
static jmethodID MID_Character_init;
static jmethodID MID_Short_init;
static jmethodID MID_Integer_init;
static jmethodID MID_Long_init;
static jmethodID MID_Float_init;
static jmethodID MID_Double_init;

static jfieldID FID_Byte_value;
static jfieldID FID_Short_value;
static jfieldID FID_Integer_value;
static jfieldID FID_Long_value;
static jfieldID FID_Float_value;
static jfieldID FID_Double_value;
static jfieldID FID_Boolean_value;
static jfieldID FID_Pointer_peer;

#ifdef MMAP_CLOSURE
static pthread_mutex_t closure_lock;
static LIST_HEAD(closure_list, closure) closure_list;
#endif
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
  cb->ffi_closure = alloc_closure(env);
  cb->object = (*env)->NewWeakGlobalRef(env, obj);
  cb->methodID = (*env)->FromReflectedMethod(env, method);
  cb->vm = vm;
  cb->return_type = return_type;
  cb->return_jtype = get_jtype(env, return_type);
  cb->is_proxy = (*env)->IsInstanceOf(env, obj, classCallbackProxy);
 
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
          cb->param_constructors[i] = MID_Boolean_init;
          break;
        case 'B':
          cb->param_classes[i] = classByte;
          cb->param_constructors[i] = MID_Byte_init;
          break;
        case 'C':
          cb->param_classes[i] = classCharacter;
          cb->param_constructors[i] = MID_Character_init;
          break;
        case 'S':
          cb->param_classes[i] = classShort;
          cb->param_constructors[i] = MID_Short_init;
          break;
        case 'I':
          cb->param_classes[i] = classInteger;
          cb->param_constructors[i] = MID_Integer_init;
          break;
        case 'J':
          cb->param_classes[i] = classLong;
          cb->param_constructors[i] = MID_Long_init;
          break;
        case 'F':
          cb->param_classes[i] = classFloat;
          cb->param_constructors[i] = MID_Float_init;
          break;
        case 'D':
          cb->param_classes[i] = classDouble;
          cb->param_constructors[i] = MID_Double_init;
          break;
        case 'L':
          cb->param_classes[i] = classPointer;
          cb->param_constructors[i] = MID_Pointer_init;
          break;
        }
    }
    
  }
  

#ifdef _WIN32
  if (calling_convention == CALLCONV_STDCALL) {
    abi = FFI_STDCALL;
  }
#endif // _WIN32

  ffi_prep_cif(&cb->ffi_cif, abi, argc, get_ffi_rtype(cb->return_jtype),
      &cb->ffi_args[0]);
  ffi_prep_closure(cb->ffi_closure, &cb->ffi_cif, dispatch, cb);
  return cb;
}
void 
free_callback(JNIEnv* env, callback *cb) {
  (*env)->DeleteWeakGlobalRef(env, cb->object);
  free_closure(env, cb->ffi_closure);
  free(cb);
}

static ffi_type*
get_ffi_type(char jtype) {
  switch (jtype) {
  case 'Z': 
    return &ffi_type_sint;
  case 'B':
    return &ffi_type_sint8;
  case 'C':
    return &ffi_type_sint;
  case 'S':
    return &ffi_type_sshort;
  case 'I':
    return &ffi_type_sint;
  case 'J':
    return &ffi_type_sint64;
  case 'F':
    return &ffi_type_float;
  case 'D':
    return &ffi_type_double;
  case 'V':
    return &ffi_type_void;
  case 'L':
  default:
    return &ffi_type_pointer;
  }
}
static ffi_type*
get_ffi_rtype(char jtype) {
  switch (jtype) {
  case 'Z': 
  case 'B': 
  case 'C': 
  case 'S':    
  case 'I':
    return &ffi_type_slong;
  case 'J':
    return &ffi_type_sint64;
  case 'F':
    return &ffi_type_float;
  case 'D':
    return &ffi_type_double;
  case 'V':
    return &ffi_type_void;
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
	  break;
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
    (*env)->CallObjectMethodA(env, obj, mid, args);
    *(void **)resp = 0;
    break;
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

    jobjectArray array = (*env)->NewObjectArray(env, cif->nargs, classObject, NULL);
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
        jarg.j = (jlong)(unsigned long)*(void **)cbargs[i];
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
        jobject ret = (*env)->CallObjectMethod(env, obj, mid, array);
        if (ret == NULL) {
            memset(resp, 0, cif->rtype->size); // Just return 0?
        } 
        else switch (cb->return_jtype) {
        case 'L':
            if ((*env)->IsInstanceOf(env, ret, classPointer)) {
                *(intptr_t *)resp = (intptr_t)(*env)->GetLongField(env, ret, FID_Pointer_peer);
            }
            break;
        case 'I':
            if ((*env)->IsInstanceOf(env, ret, classInteger)) {
                *(long*)resp = (*env)->GetIntField(env, ret, FID_Integer_value);
            }
            break;
        case 'J':
            if ((*env)->IsInstanceOf(env, ret, classLong)) {
                *(jlong*)resp = (*env)->GetLongField(env, ret, FID_Long_value);
            }
            break;
        case 'S':
            if ((*env)->IsInstanceOf(env, ret, classShort)) {
                *(short*)resp = (*env)->GetShortField(env, ret, FID_Short_value);
            }
            break;
        case 'B':
            if ((*env)->IsInstanceOf(env, ret, classByte)) {
                *(char*)resp = (*env)->GetByteField(env, ret, FID_Byte_value);
            }
            break;
        case 'Z':
            if ((*env)->IsInstanceOf(env, ret, classBoolean)) {
                *(int*)resp = (*env)->GetBooleanField(env, ret, FID_Boolean_value);
            }
            break;
	case 'F':
            if ((*env)->IsInstanceOf(env, ret, classFloat)) {
                *(float*)resp = (*env)->GetFloatField(env, ret, FID_Float_value);
            }
            break;
	case 'D':
            if ((*env)->IsInstanceOf(env, ret, classDouble)) {
                *(double*)resp = (*env)->GetDoubleField(env, ret, FID_Double_value);
            }
            break;
        default:
            memset(resp, 0, cif->rtype->size); // Just return 0?
        }
    }

    if (!attached) {
        (*jvm)->DetachCurrentThread(jvm);
    }
}

int 
jnidispatch_callback_init(JavaVM* jvm) {
    int attached;
    JNIEnv* env;
    attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;
    if (!attached) {
        if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
          fprintf(stderr, "Can't attach to current thread\n");
          return 0;
        }
    }

    if (!LOAD_CREF(env, Object, "java/lang/Object")) return 0;
    if (!LOAD_CREF(env, Class, "java/lang/Class")) return 0;
    if (!LOAD_CREF(env, Method, "java/lang/reflect/Method")) return 0;
    if (!LOAD_CREF(env, String, "java/lang/String")) return 0;
    if (!LOAD_CREF(env, ByteBuffer, "java/nio/ByteBuffer")) return 0;

    if (!LOAD_CREF(env, Pointer, "com/sun/jna/Pointer")) return 0;
    if (!LOAD_CREF(env, CallbackProxy, "com/sun/jna/CallbackProxy")) return 0;

    if (!LOAD_PCREF(env, Boolean, "java/lang/Boolean")) return 0;
    if (!LOAD_PCREF(env, Byte, "java/lang/Byte")) return 0;
    if (!LOAD_PCREF(env, Character, "java/lang/Character")) return 0;
    if (!LOAD_PCREF(env, Short, "java/lang/Short")) return 0;
    if (!LOAD_PCREF(env, Integer, "java/lang/Integer")) return 0;
    if (!LOAD_PCREF(env, Long, "java/lang/Long")) return 0;
    if (!LOAD_PCREF(env, Float, "java/lang/Float")) return 0;
    if (!LOAD_PCREF(env, Double, "java/lang/Double")) return 0;

    if (!LOAD_MID(env, MID_Pointer_init, classPointer,
                  "<init>", "(J)V"))
      return 0;
    if (!LOAD_MID(env, MID_getClass, classObject,
                  "getClass", "()Ljava/lang/Class;"))
      return 0;
    if (!LOAD_MID(env, MID_Class_getComponentType, classClass,
                  "getComponentType", "()Ljava/lang/Class;"))
      return 0;
    if (!LOAD_MID(env, MID_String_getBytes, classString,
                  "getBytes", "()[B"))
      return 0;
    if (!LOAD_MID(env, MID_String_toCharArray, classString,
                  "toCharArray", "()[C"))
      return 0;
    if (!LOAD_MID(env, MID_String_init_bytes, classString,
                  "<init>", "([B)V"))
      return 0;
    if (!LOAD_MID(env, MID_Method_getParameterTypes, classMethod,
                  "getParameterTypes", "()[Ljava/lang/Class;"))
      return 0;
    if (!LOAD_MID(env, MID_Method_getReturnType, classMethod,
                  "getReturnType", "()Ljava/lang/Class;"))
      return 0;

    if (!LOAD_MID(env, MID_Boolean_init, classBoolean, "<init>", "(Z)V")) return 0;
    if (!LOAD_MID(env, MID_Byte_init, classByte, "<init>", "(B)V")) return 0;
    if (!LOAD_MID(env, MID_Character_init, classCharacter, "<init>", "(C)V")) return 0;
    if (!LOAD_MID(env, MID_Short_init, classShort, "<init>", "(S)V")) return 0;
    if (!LOAD_MID(env, MID_Integer_init, classInteger, "<init>", "(I)V")) return 0;
    if (!LOAD_MID(env, MID_Long_init, classLong, "<init>", "(J)V")) return 0;
    if (!LOAD_MID(env, MID_Float_init, classFloat, "<init>", "(F)V")) return 0;
    if (!LOAD_MID(env, MID_Double_init, classDouble, "<init>", "(D)V")) return 0;
    
    if (!LOAD_FID(env, FID_Byte_value, classByte, "value", "B"))
      return 0;
    if (!LOAD_FID(env, FID_Short_value, classShort, "value", "S"))
      return 0;
    if (!LOAD_FID(env, FID_Integer_value, classInteger, "value", "I"))
      return 0;
    if (!LOAD_FID(env, FID_Long_value, classLong, "value", "J"))
      return 0;
    if (!LOAD_FID(env, FID_Float_value, classFloat, "value", "F"))
      return 0;
    if (!LOAD_FID(env, FID_Double_value, classDouble, "value", "D"))
      return 0;
    if (!LOAD_FID(env, FID_Boolean_value, classBoolean, "value", "Z"))
      return 0;
    if (!LOAD_FID(env, FID_Pointer_peer, classPointer, "peer", "J"))
      return 0;

#ifdef MMAP_CLOSURE
    /*
     * Create the lock for the mmap arena
     */
    pthread_mutex_init(&closure_lock, NULL);
    LIST_INIT(&closure_list);
#endif
    if (!attached) {
        (*jvm)->DetachCurrentThread(jvm);
    }
    return JNI_TRUE;
}

#ifdef MMAP_CLOSURE
# ifndef PAGE_SIZE
#  if defined(PAGESIZE)
#   define PAGE_SIZE PAGESIZE
#  elif defined(NBPG)
#   define PAGE_SIZE NBPG
#  endif   
# endif
typedef struct closure {
    LIST_ENTRY(closure) list;
} closure;

static ffi_closure*
alloc_closure(JNIEnv* env)
{
    closure* closure = NULL;
    pthread_mutex_lock(&closure_lock);

    if (closure_list.lh_first == NULL) {
        /*
         * Get a new page from the kernel and divvy that up
         */
        int clsize = roundup(sizeof(ffi_closure), sizeof(void *));
        int i;
        caddr_t ptr = mmap(0, PAGE_SIZE, PROT_EXEC | PROT_READ | PROT_WRITE,
            MAP_ANON | MAP_PRIVATE, -1, 0);
        if (ptr == NULL) {
            pthread_mutex_unlock(&closure_lock);
            return NULL;
        }
        for (i = 0; i <= (PAGE_SIZE - clsize); i += clsize) {
            closure = (struct closure *)(ptr + i);
            LIST_INSERT_HEAD(&closure_list, closure, list);            
        }
    }
    closure = closure_list.lh_first;
    LIST_REMOVE(closure, list);

    pthread_mutex_unlock(&closure_lock);
    memset(closure, 0, sizeof(*closure));
    return (ffi_closure *)closure;
}

static void
free_closure(JNIEnv* env, ffi_closure *ffi_closure) 
{
    pthread_mutex_lock(&closure_lock);    
    LIST_INSERT_HEAD(&closure_list, (closure*)ffi_closure, list);
    pthread_mutex_unlock(&closure_lock);
    
}
#else
static ffi_closure*
alloc_closure(JNIEnv* env)
{
  return (ffi_closure *)calloc(1, sizeof(ffi_closure));
}
static void
free_closure(JNIEnv* env, ffi_closure *closure) 
{
  free(closure);
}
#endif

#ifdef __cplusplus
}
#endif
