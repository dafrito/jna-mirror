/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
#ifndef DISPATCH_H
#define DISPATCH_H

#include "ffi.h"
#include "com_sun_jna_Function.h"

#ifdef __cplusplus
extern "C" {
#endif

/* These are the calling conventions an invocation can handle. */
typedef enum _callconv {
    CALLCONV_C = com_sun_jna_Function_C_CONVENTION,
#if defined(_WIN32) && !defined(_WIN64)
    CALLCONV_STDCALL = com_sun_jna_Function_ALT_CONVENTION,
#endif
} callconv_t;

/* Maximum number of allowed arguments in libffi. */
#define MAX_NARGS com_sun_jna_Function_MAX_NARGS

typedef struct _callback {
  // Location of this field must agree with CallbackReference.getTrampoline()
  void* x_closure;
  ffi_closure* ffi_closure;
  ffi_cif ffi_cif;
  ffi_type* ffi_args[MAX_NARGS];
  JavaVM* vm;
  jobject object;
  jmethodID methodID;
  char param_jtypes[MAX_NARGS];
} callback;

// Size of a register
typedef long word_t;

#if defined(SOLARIS2) || defined(__GNUC__)
#if defined(_WIN64)
#define L2A(X) ((void *)(long long)(X))
#define A2L(X) ((jlong)(long long)(X))
#else
#define L2A(X) ((void *)(unsigned long)(X))
#define A2L(X) ((jlong)(unsigned long)(X))
#endif
#endif

#if defined(_MSC_VER)
#define L2A(X) ((void *)(X))
#define A2L(X) ((jlong)(X))
#define snprintf sprintf_s
#endif

/* Convenience macros */
#define LOAD_WEAKREF(ENV,VAR) \
  ((VAR == 0) \
   ? 0 : ((VAR = (*ENV)->NewWeakGlobalRef(ENV, VAR)) == 0 ? 0 : VAR))
#define FIND_CLASS(ENV,SIMPLE,NAME) \
  (class ## SIMPLE = (*ENV)->FindClass(ENV, NAME))
#define FIND_PRIMITIVE_CLASS(ENV,SIMPLE) \
  (classPrimitive ## SIMPLE = (*ENV)->GetStaticObjectField(ENV,class ## SIMPLE,(*ENV)->GetStaticFieldID(ENV,class ## SIMPLE,"TYPE","Ljava/lang/Class;")))
#define LOAD_CREF(ENV,SIMPLE,NAME) \
  (FIND_CLASS(ENV,SIMPLE,NAME) && LOAD_WEAKREF(ENV,class ## SIMPLE))
#define LOAD_PCREF(ENV,SIMPLE,NAME) \
  (LOAD_CREF(ENV,SIMPLE,NAME) \
   && FIND_PRIMITIVE_CLASS(ENV,SIMPLE) \
   && LOAD_WEAKREF(ENV,classPrimitive ## SIMPLE))
#define LOAD_MID(ENV,VAR,CLASS,NAME,SIG) \
   ((VAR = (*ENV)->GetMethodID(ENV, CLASS, NAME, SIG)) ? VAR : 0)
#define LOAD_FID(ENV,VAR,CLASS,NAME,SIG) \
   ((VAR = (*ENV)->GetFieldID(ENV, CLASS, NAME, SIG)) ? VAR : 0)

// Avoid typos in class names
#define EIllegalArgument "java/lang/IllegalArgumentException"
#define EOutOfMemory "java/lang/OutOfMemoryError"
#define EUnsatisfiedLink "java/lang/UnsatisfiedLinkError"
#define EIllegalState "java/lang/IllegalStateException"
#define EUnsupportedOperation "java/lang/UnsupportedOperationException"
#define EError "java/lang/Error"

extern void throwByName(JNIEnv *env, const char *name, const char *msg);
extern jobject newJavaPointer(JNIEnv *, void *);
extern char get_jtype(JNIEnv*, jclass);
extern ffi_type* get_ffi_type(JNIEnv*, jclass, char);
extern ffi_type* get_ffi_rtype(JNIEnv*, jclass, char);
extern const char* jnidispatch_callback_init(JNIEnv*);
extern void jnidispatch_callback_dispose(JNIEnv*);
extern callback* create_callback(JNIEnv*, jobject, jobject,
                                 jobjectArray, jclass, 
                                 callconv_t);
extern void free_callback(JNIEnv*, callback*);
extern void extract_value(JNIEnv*, jobject, void*, size_t size);
extern jobject new_object(JNIEnv*, char, void*);
#ifdef __cplusplus
}
#endif
#endif /* DISPATCH_H */
