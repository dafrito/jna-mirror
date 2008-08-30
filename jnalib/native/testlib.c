/* Copyright (c) 2007-2008 Timothy Wall, All Rights Reserved
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

/* Native library implementation to support JUnit tests. */
#ifdef __cplusplus
extern "C" {
#endif

#include <wchar.h>
#include <stdio.h>
#include <stdarg.h>

#ifdef _WIN32
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

#ifdef _MSC_VER
#define int64 __int64
#define LONG(X) X ## I64
#elif __GNUC__
#define int64 long long
#define LONG(X) X ## LL
#else
#error 64-bit type not defined for this compiler
#endif

#define MAGICSTRING "magic";
#define MAGICWSTRING L"magic"
#define MAGIC32 0x12345678L
#define MAGIC64 LONG(0x123456789ABCDEF0)
#define MAGICFLOAT -118.625
#define MAGICDOUBLE ((double)(-118.625))
#define int8 signed char
#define int16 short
#define int32 int

#define MAGICDATA "0123456789"

EXPORT int test_global = MAGIC32;

// TODO: check more fields/alignments
struct CheckFieldAlignment {
  int8 int8Field;
  int16 int16Field;
  int32 int32Field;
  int64 int64Field;
  float floatField;
  double doubleField;
};

static int _callCount;

EXPORT int
callCount() {
  return ++_callCount;
}

EXPORT int  
returnFalse() {
  return 0;
}

EXPORT int  
returnTrue() {
  return -1;
}

EXPORT int
returnBooleanArgument(int arg) {
  return arg;
}

EXPORT int8  
returnInt8Argument(int8 arg) {
  return arg;
}

EXPORT wchar_t
returnWideCharArgument(wchar_t arg) {
  return arg;
}

EXPORT int16  
returnInt16Argument(int16 arg) {
  return arg;
}

EXPORT int32  
returnInt32Zero() {
  int32 value = 0;
  return value;
}

EXPORT int32  
returnInt32Magic() {
  int32 value = MAGIC32;
  return value;
}

EXPORT int32  
returnInt32Argument(int32 arg) {
  return arg;
}

EXPORT int64  
returnInt64Zero() {
  int64 value = 0;
  return value;
}

EXPORT int64  
returnInt64Magic() {
  int64 value = MAGIC64;
  return value;
}

EXPORT int64  
returnInt64Argument(int64 arg) {
  return arg;
}

EXPORT long
returnLongZero() {
  long value = 0;
  return value;
}

EXPORT long  
returnLongMagic() {
  long value = sizeof(long) == 4 ? MAGIC32 : MAGIC64;
  return value;
}

EXPORT long  
returnLongArgument(long arg) {
  return arg;
}

EXPORT float  
returnFloatZero() {
  float value = 0.0;
  return value;
}

EXPORT float  
returnFloatMagic() {
  float value = MAGICFLOAT;
  return value;
}

EXPORT float  
returnFloatArgument(float arg) {
  return arg;
}

EXPORT double  
returnDoubleZero() {
  double value = (double)0.0;
  return value;
}

EXPORT double  
returnDoubleMagic() {
  double value = MAGICDOUBLE;
  return value;
}

EXPORT double  
returnDoubleArgument(double arg) {
  return arg;
}

EXPORT void* 
returnPointerArgument(void *arg) {
  return arg;
}

EXPORT char* 
returnStringMagic() {
  return MAGICSTRING;
}

EXPORT char* 
returnStringArgument(char *arg) {
  return arg;
}

EXPORT wchar_t* 
returnWStringMagic() {
  return MAGICWSTRING;
}

EXPORT wchar_t* 
returnWStringArgument(wchar_t *arg) {
  return arg;
}

EXPORT char*
returnStringArrayElement(char* args[], int which) {
  return args[which];
}

EXPORT wchar_t*
returnWideStringArrayElement(wchar_t* args[], int which) {
  return args[which];
}

EXPORT void*
returnPointerArrayElement(void* args[], int which) {
  return args[which];
}

EXPORT int
returnRotatedArgumentCount(char* args[]) {
  int count = 0;
  char* first = args[0];
  while (args[count] != NULL) {
    ++count;
    args[count-1] = args[count] ? args[count] : first;
  }
  return count;
}

typedef struct _TestStructure {
  double value;
} TestStructure;

EXPORT TestStructure*
returnStaticTestStructure() {
  static TestStructure test_structure;
  test_structure.value = MAGICDOUBLE;
  return &test_structure;
}

EXPORT TestStructure*
returnNullTestStructure() {
  return NULL;
}

typedef struct _VariableSizedStructure {
  int length;
  char buffer[1];
} VariableSizedStructure;

EXPORT char*
returnStringFromVariableSizedStructure(VariableSizedStructure* s) {
  return s->buffer;
}

typedef struct _TestStructureByValue {
  int8 c;
  int16 s;
  int32 i;
  int64 j;
  TestStructure inner;
} TestStructureByValue;

EXPORT TestStructureByValue
returnStructureByValue() {
  TestStructureByValue v;
  v.c = 1;
  v.s = 2;
  v.i = 3;
  v.j = 4;
  v.inner.value = 5;
  return v;
}

typedef void (*callback_t)();
typedef int32 (*int32_callback_t)(int32);
typedef callback_t (*cb_callback_t)(callback_t);

EXPORT int32_callback_t
returnCallback() {
  return &returnInt32Argument;
}

EXPORT int32_callback_t
returnCallbackArgument(int32_callback_t arg) {
  return arg;
}

EXPORT void 
incrementInt8ByReference(int8 *arg) {
  if (arg) ++*arg;
}

EXPORT void 
incrementInt16ByReference(int16 *arg) {
  if (arg) ++*arg;
}

EXPORT void 
incrementInt32ByReference(int32 *arg) {
  if (arg) ++*arg;
}

EXPORT void 
incrementInt64ByReference(int64 *arg) {
  if (arg) ++*arg;
}

EXPORT void 
complementFloatByReference(float *arg) {
  if (arg) *arg = -*arg;
}

EXPORT void 
complementDoubleByReference(double *arg) {
  if (arg) *arg = -*arg;
}

EXPORT void 
setPointerByReferenceNull(void **arg) {
  if (arg) *arg = NULL;
}

EXPORT int64 
checkInt64ArgumentAlignment(int32 i, int64 j, int32 i2, int64 j2) {
  if (i != 0x10101010 || j != LONG(0x1111111111111111)
      || i2 != 0x01010101 || j2 != LONG(0x2222222222222222))
    return -1;

  return i + j + i2 + j2;
}

EXPORT double 
checkDoubleArgumentAlignment(float f, double d, float f2, double d2) {
  // float:  1=3f800000 2=40000000 3=40400000 4=40800000
  // double: 1=3ff00... 2=40000... 3=40080... 4=40100...
  if (f != 1 || d != 2 || f2 != 3 || d2 != 4)
    return -1;

  return f + d + f2 + d2;
}

EXPORT void*
testStructurePointerArgument(struct CheckFieldAlignment* arg) {
  return arg;
}

EXPORT double
testStructureByValueArgument(struct CheckFieldAlignment arg) {
  return arg.int8Field + arg.int16Field + arg.int32Field
    + arg.int64Field + arg.floatField + arg.doubleField;
}

typedef struct {
  int8 field0;
  int16 field1;
} Align16BitField8;
typedef struct {
  int8 field0;
  int32 field1;
} Align32BitField8;
typedef struct {
  int16 field0;
  int32 field1;
} Align32BitField16;
typedef struct {
  int32 field0;
  int16 field1;
  int32 field2;
} Align32BitField16_2;
typedef struct {
  int32 field0;
  int64 field1;
  int32 field2;
  int64 field3;
} Align64BitField32;
typedef struct {
  int64 field0;
  int8 field1;
} PadTrailingSmallField;
static int STRUCT_SIZES[] = {
  sizeof(Align16BitField8),
  sizeof(Align32BitField8),
  sizeof(Align32BitField16),
  sizeof(Align32BitField16_2),
  sizeof(Align64BitField32),
  sizeof(PadTrailingSmallField),
};
EXPORT int32
getStructureSize(unsigned index) {
  if (index >= (int)sizeof(STRUCT_SIZES)/sizeof(STRUCT_SIZES[0]))
    return -1;
  return STRUCT_SIZES[index];
}

extern void exit(int);
#define FIELD(T,X,N) (((T*)X)->field ## N)
#define OFFSET(T,X,N) (int)(((char*)&FIELD(T,X,N))-((char*)&FIELD(T,X,0)))
#define V8(N) (N+1)
#define V16(N) ((((int32)V8(N))<<8)|V8(N))
#define V32(N) ((((int32)V16(N))<<16)|V16(N))
#define V64(N) ((((int64)V32(N))<<32)|V32(N))
#define VALUE(T,X,N) \
((sizeof(FIELD(T,X,N)) == 1) \
 ? V8(N)  : ((sizeof(FIELD(T,X,N)) == 2) \
 ? V16(N) : ((sizeof(FIELD(T,X,N)) == 4) \
 ? V32(N) : V64(N))))
#define VALIDATE_FIELDN(T,X,N) \
do { if (FIELD(T,X,N) != VALUE(T,X,N)) {*offsetp=OFFSET(T,X,N); *valuep=FIELD(T,X,N); return N;} } while (0)
#define VALIDATE1(T,X) VALIDATE_FIELDN(T,X,0)
#define VALIDATE2(T,X) do { VALIDATE1(T,X); VALIDATE_FIELDN(T,X,1); } while(0)
#define VALIDATE3(T,X) do { VALIDATE2(T,X); VALIDATE_FIELDN(T,X,2); } while(0)
#define VALIDATE4(T,X) do { VALIDATE3(T,X); VALIDATE_FIELDN(T,X,3); } while(0)

// returns the field index which failed, and the expected field offset
// returns -2 on success
EXPORT int32
testStructureAlignment(void* s, unsigned index, int* offsetp, int64* valuep) {
  if (index >= sizeof(STRUCT_SIZES)/sizeof(STRUCT_SIZES[0]))
    return -1;

  switch(index) {
  case 0: VALIDATE2(Align16BitField8,s); break;
  case 1: VALIDATE2(Align32BitField8,s); break;
  case 2: VALIDATE2(Align32BitField16,s); break;
  case 3: VALIDATE3(Align32BitField16_2,s); break;
  case 4: VALIDATE4(Align64BitField32,s); break;
  case 5: VALIDATE2(PadTrailingSmallField,s); break;
  }
  return -2;
}

EXPORT int32
testStructureArrayInitialization(struct CheckFieldAlignment arg[], int len) {
  int i;
  for (i=0;i < len;i++) {
    if (arg[i].int32Field != i)
      return i;
  }
  return -1;
}

EXPORT void
modifyStructureArray(struct CheckFieldAlignment arg[], int length) {
  int i;
  for (i=0;i < length;i++) {
    arg[i].int32Field = i;
    arg[i].int64Field = i+1;
    arg[i].floatField = (float)i+2;
    arg[i].doubleField = (double)i+3;
  }
}


EXPORT void
callVoidCallback(void (*func)(void)) {
  (*func)();
}

EXPORT int 
callBooleanCallback(int (*func)(int arg, int arg2),
                    int arg, int arg2) {
  return (*func)(arg, arg2);
}

EXPORT int8
callInt8Callback(int8 (*func)(int8 arg, int8 arg2), int8 arg, int8 arg2) {
  return (*func)(arg, arg2);
}

EXPORT int16
callInt16Callback(int16 (*func)(int16 arg, int16 arg2), int16 arg, int16 arg2) {
  return (*func)(arg, arg2);
}

EXPORT int32 
callInt32Callback(int32 (*func)(int32 arg, int32 arg2),
                  int32 arg, int32 arg2) {
  return (*func)(arg, arg2);
}

EXPORT long 
callNativeLongCallback(long (*func)(long arg, long arg2),
                       long arg, long arg2) {
  return (*func)(arg, arg2);
}

EXPORT int64 
callInt64Callback(int64 (*func)(int64 arg, int64 arg2),
                  int64 arg, int64 arg2) {
  return (*func)(arg, arg2);
}

EXPORT float 
callFloatCallback(float (*func)(float arg, float arg2),
                  float arg, float arg2) {
  return (*func)(arg, arg2);
}

EXPORT double 
callDoubleCallback(double (*func)(double arg, double arg2),
                   double arg, double arg2) {
  return (*func)(arg, arg2);
}

EXPORT TestStructure*
callStructureCallback(TestStructure* (*func)(TestStructure*), TestStructure* arg) {
  return (*func)(arg);
}

EXPORT int
callCallbackWithByReferenceArgument(int (*func)(int arg, int* result), int arg, int* result) {
  return (*func)(arg, result);
}

EXPORT char*
callStringCallback(char* (*func)(char* arg), char* arg) {
  return (*func)(arg);
}

EXPORT char**
callStringArrayCallback(char** (*func)(char** arg), char** arg) {
  return (*func)(arg);
}

EXPORT wchar_t*
callWideStringCallback(wchar_t* (*func)(wchar_t* arg), wchar_t* arg) {
  return (*func)(arg);
}

struct cbstruct {
  void (*func)(void);
};

EXPORT void
callCallbackInStruct(struct cbstruct *cb) {
  (*cb->func)();
}

EXPORT TestStructureByValue
callCallbackWithStructByValue(TestStructureByValue (*func)(TestStructureByValue),
                              TestStructureByValue s) {
  return (*func)(s);
}

EXPORT callback_t
callCallbackWithCallback(cb_callback_t cb) {
  return (*cb)((callback_t)cb);
}

static int 
structCallbackFunction(int arg1, int arg2) {
  return arg1 + arg2;
}

EXPORT void
setCallbackInStruct(struct cbstruct* cb) {
  cb->func = (void (*)(void))structCallbackFunction;
}


EXPORT int32 
fillInt8Buffer(char *buf, int len, char value) {
  int i;

  for (i=0;i < len;i++) {
    buf[i] = value;
  }
  return len;
}

EXPORT int32 
fillInt16Buffer(short *buf, int len, short value) {
  int i;
  for (i=0;i < len;i++) {
    buf[i] = value;
  }
  return len;
}

EXPORT int32 
fillInt32Buffer(int32 *buf, int len, int32 value) {
  int i;
  for (i=0;i < len;i++) {
    buf[i] = value;
  }
  return len;
}

EXPORT int32
fillInt64Buffer(int64 *buf, int len, int64 value) {
  int i;
  for (i=0;i < len;i++) {
    buf[i] = value;
  }
  return len;
}

EXPORT int32
addInt32VarArgs(const char *fmt, ...) {
  va_list ap;
  int32 sum = 0;
  va_start(ap, fmt);
  
  while (*fmt) {
    switch (*fmt++) {
    case 'd':
      sum += va_arg(ap, int32);
      break;
    case 'l':
      sum += (int) va_arg(ap, int64);
      break;
    case 'c':
      sum += (int) va_arg(ap, int);
      break;
    default:
      break;
    }
  }
  va_end(ap);
  return sum;
}

EXPORT void
modifyStructureVarArgs(const char* fmt, ...) {
  struct _ss {
    int32 magic;
  } *s;
  va_list ap;
  va_start(ap, fmt);
  while (*fmt) {
    switch(*fmt++) {
    case 's': 
      s = (struct _ss *)va_arg(ap, void*);
      s->magic = MAGIC32;
      break;
    default:
      break;
    }
  }

  va_end(ap);
}

EXPORT char *
returnStringVarArgs(const char *fmt, ...) {
  char* cp;
  va_list ap;
  va_start(ap, fmt);
  cp = va_arg(ap, char *);
  va_end(ap);
  return cp;
}

#if defined(_WIN32) && !defined(_WIN64)
///////////////////////////////////////////////////////////////////////
// stdcall tests
///////////////////////////////////////////////////////////////////////
EXPORT int32 __stdcall
returnInt32ArgumentStdCall(int32 arg) {
  return arg;
}

EXPORT TestStructureByValue __stdcall
returnStructureByValueArgumentStdCall(TestStructureByValue arg) {
  return arg;
}

EXPORT int32 __stdcall
callInt32StdCallCallback(int32 (__stdcall *func)(int32 arg, int32 arg2),
                         int32 arg, int32 arg2) {
  void* sp1 = NULL;
  void* sp2 = NULL;
  int value = -1;

#if defined(_MSC_VER)
  __asm mov sp1, esp;
  value = (*func)(arg, arg2);
  __asm mov sp2, esp;
#elif defined(__GNUC__)
  asm volatile (" movl %%esp,%0" : "=g" (sp1));
  value = (*func)(arg, arg2);
  asm volatile (" movl %%esp,%0" : "=g" (sp2));
#endif

  if (sp1 != sp2) {
    return -1;
  }
  return value;
}
#endif /* _WIN32 && !_WIN64 */

#ifdef __cplusplus
}
#endif
