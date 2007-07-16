/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package com.sun.jna;

import com.sun.jna.types.NativeValue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 */
class CallbackReference extends WeakReference {
    static final int MAX_NARGS = Function.MAX_NARGS;
    static final Map callbackMap = new WeakHashMap();
    static final Map altCallbackMap = new WeakHashMap();

    Pointer cbstruct;

    CallbackProxy proxy;
    private CallbackReference(Callback callback, int callingConvention) {
        super(callback);
        Method m = getCallbackMethod(callback);
        Class[] paramTypes;
        Class rtype;
        if (callback instanceof CallbackProxy) {
            CallbackProxy proxy = (CallbackProxy)callback;
            paramTypes = proxy.getParameterTypes();
            rtype = proxy.getReturnType();
        }
        else {
            paramTypes = m.getParameterTypes();
            rtype = m.getReturnType();
            if (requiresConversion(paramTypes, rtype)) {
                proxy = new ConversionCallbackProxy(callback, m, paramTypes, rtype);
                paramTypes = proxy.getParameterTypes();
                rtype = proxy.getReturnType();
                m = getCallbackMethod(proxy);                
                callback = proxy;
            }
        }
        if (paramTypes.length > MAX_NARGS) {
            String msg = "Method signature exceeds the maximum "
                + "parameter count: " + m;
            throw new IllegalArgumentException(msg);
        }
        this.cbstruct = createNativeCallback(callback, m, paramTypes, rtype, callingConvention);
    }
    
    public static CallbackReference getInstance(Callback cb, int callingConvention) {
        CallbackReference cbref;
        Map map = callingConvention == Function.ALT_CONVENTION
            ? altCallbackMap : callbackMap;
        synchronized (map) {
            cbref = (CallbackReference) map.get(cb);
            if (cbref == null) {                
                cbref = new CallbackReference(cb, callingConvention);
                map.put(cb, cbref);
            }
            return cbref;
        }
    }
    public static CallbackReference getInstance(Callback cb) {
        int callingConvention = cb instanceof AltCallingConvention
                ? Function.ALT_CONVENTION : Function.C_CONVENTION;
        return getInstance(cb, callingConvention);
    }
    
    private static boolean requiresConversion(Class[] paramTypes, Class rtype) {
        if (!isAllowableNativeType(rtype)) {
            return true;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (!isAllowableNativeType(paramTypes[i])) {
                return true;
            }
        }
        return false;        
    }
    
    private static Method getCallbackMethod(Callback callback) {
        Method[] mlist = callback.getClass().getMethods();
        for (int mi=0;mi < mlist.length;mi++) {
            Method m = mlist[mi];
            if (Callback.METHOD_NAME.equals(m.getName())) {
                if (m.getParameterTypes().length > Function.MAX_NARGS) {
                    String msg = "Method signature exceeds the maximum "
                        + "parameter count: " + m;
                    throw new IllegalArgumentException(msg);
                }
                if (!m.isAccessible()) {
                    try {
                        m.setAccessible(true);
                    } catch (SecurityException e) {
                        throw new IllegalArgumentException("Callback method is inaccessible, make sure the interface is public: " + m);
                    }
                }

                return m;
            }
        }
        String msg = "Callback must implement method named '"
            + Callback.METHOD_NAME + "'";
        throw new IllegalArgumentException(msg);
    }
    
    public Pointer getTrampoline() {
        return cbstruct.getPointer(0);
    }
    protected void finalize() {
        freeNativeCallback(cbstruct.peer);
        cbstruct.peer = 0;
    }
    
    /** Returns whether the given class is supported in native code.
     * Other types (String, WString, Structure, arrays, NativeLong,
     * etc) are supported in the Java library.
     */
    static boolean isAllowableNativeType(Class cls) {
        return cls == boolean.class || cls == Boolean.class
            || cls == byte.class || cls == Byte.class
            || cls == short.class || cls == Short.class
            || cls == char.class || cls == Character.class
            || cls == int.class || cls == Integer.class
            || cls == long.class || cls == Long.class
            || cls == float.class || cls == Float.class
            || cls == double.class || cls == Double.class
            || cls == void.class || cls == Void.class
            || Pointer.class.isAssignableFrom(cls);
    }
    
    private static native Pointer createNativeCallback(Callback obj, Method m, Class[] paramTypes, 
            Class returnType, int callingConvention);
    private static native void freeNativeCallback(long peer);
    
    static class ConversionCallbackProxy implements CallbackProxy {
        final Method callbackMethod;        
        final WeakReference callbackRef;
        Class nativeReturnType;
        Class[] nativeParamTypes;
        FromNativeConverter[] paramConverters;
        ToNativeConverter resultConverter;
        public ConversionCallbackProxy(Callback callback, Method method, 
                Class[] paramTypes, Class returnType) {
            this.callbackMethod = method;
            this.callbackRef = new WeakReference(callback);
            this.paramConverters = new FromNativeConverter[paramTypes.length];
            this.nativeParamTypes = new Class[paramTypes.length];
            this.nativeReturnType = returnType;
            
            // 
            // Locate the definitition of the Callback, so the enclosing Library
            // definition can be found to obtain the TypeMapper.
            //
            Class type = callback.getClass();
            Class[] ifaces = type.getInterfaces();
            for (int i = 0; i < ifaces.length; i++) {
                if (Callback.class.isAssignableFrom(ifaces[i])) {
                    type = ifaces[i];
                    break;
                }
            }
            TypeMapper mapper = null;
            Class declaring = type.getDeclaringClass();
            if (declaring != null) {
                mapper = Native.getTypeMapper(declaring);
            }
            // Generate a list of parameter types that the native code can 
            // handle.  Let the CallbackProxy to do any further conversion
            // to match the true callback signature
            System.arraycopy(paramTypes, 0, nativeParamTypes, 0, paramTypes.length);            
            if (mapper != null) {
                for (int i = 0; i < nativeParamTypes.length; i++) {
                    FromNativeConverter rc = mapper.getFromNativeConverter(nativeParamTypes[i]);
                    if (rc != null) {
                        paramConverters[i] = rc;
                        nativeParamTypes[i] = rc.nativeType();
                    }
                }
                resultConverter = mapper.getToNativeConverter(callbackMethod.getReturnType());
            }
            for (int i = 0; i < nativeParamTypes.length; i++) {
                Class cls = nativeParamTypes[i];
                if (Structure.class.isAssignableFrom(cls)) {
                    // Make sure we can instantiate an argument of this type
                    try {
                        cls.newInstance();
                    } catch (InstantiationException e) {
                        throw new IllegalArgumentException("Cannot instantiate " + cls + ": " + e);
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException("Instantiation of " + cls + " not allowed (is it public?): " + e);
                    }
                    nativeParamTypes[i] = Pointer.class;
                } else if (NativeLong.class.isAssignableFrom(cls)) {
                    nativeParamTypes[i] = NativeLong.nativeType;
                } else if (cls == String.class || cls == WString.class) {
                    nativeParamTypes[i] = Pointer.class;
                } else if (!isAllowableNativeType(cls)) {
                    throw new IllegalArgumentException("Callback argument " + cls + " requires custom type conversion");
                }
            }            
            if (Structure.class.isAssignableFrom(returnType)) {
                nativeReturnType = Pointer.class;                
            }
            else if (NativeLong.class.isAssignableFrom(returnType)) {
                nativeReturnType = NativeLong.nativeType;                
            }            
            else if (returnType == boolean.class || returnType == Boolean.class) {
                nativeReturnType = Integer.class;                
            }
            else if (returnType == String.class || returnType == WString.class) {
                nativeReturnType = Pointer.class;
            }            
        }
        
        public Object proxyCallback(Object[] args) {
            Class[] paramTypes = callbackMethod.getParameterTypes();
            Object[] callbackArgs = new Object[args.length];

            // convert basic supported types to appropriate Java parameter types
            for (int i = 0; i < args.length; i++) {
                if (paramConverters[i] != null) {
                    FromNativeContext context = new CallbackInvocationContext(paramTypes[i], callbackMethod, args);
                    args[i] = paramConverters[i].fromNative(args[i], context);
                }
                callbackArgs[i] = convertArgument(args[i], paramTypes[i]);
            }

            try {
                Callback callback = (Callback)callbackRef.get();
                if (callback != null) {
                    return convertResult(callbackMethod.invoke(callback, callbackArgs));
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return Long.valueOf(0);            
        }
        /** Convert argument from its basic native type to the given
         * Java parameter type.
         */
        private Object convertArgument(Object value, Class dstType) {
            if (value instanceof Pointer) {
                if (dstType == String.class) {
                    value = ((Pointer)value).getString(0, false);
                }
                else if (dstType == WString.class) {
                    value = new WString(((Pointer)value).getString(0, true));
                }
                else if (Structure.class.isAssignableFrom(dstType)) {
                    Pointer p = (Pointer)value;
                    try {
                        Structure s = (Structure)dstType.newInstance();
                        s.useMemory(p);
                        s.read();
                        value = s;
                    }
                    catch(InstantiationException e) {
                        // can't happen, already checked for
                    }
                    catch(IllegalAccessException e) {
                        // can't happen, already checked for
                    }
                }
            }
            else if (NativeLong.class.isAssignableFrom(dstType)
                     && (value instanceof Integer || value instanceof Long)) {
                value = new NativeLong(((Number)value).longValue());
            }
            else if ((boolean.class == dstType || Boolean.class == dstType)
                     && value instanceof Number) {
                value = Boolean.valueOf(((Number)value).intValue() != 0);
            }
            return value;
        }
        final static Integer falseInteger = new Integer(0);
        final static Integer trueInteger = new Integer(-1);
        
        private Object convertResult(Object value) {
            if (resultConverter != null && value != null) {
                value = resultConverter.toNative(value);
            }
            if (value == null) {
                return Long.valueOf(0);
            }
            Class cls = value.getClass();
            if (Structure.class.isAssignableFrom(cls)) {
                return ((Structure)value).getPointer();
            }
            else if (NativeValue.class.isAssignableFrom(cls)) {
                return ((NativeValue)value).toNativeValue();
            }
            else if (cls == boolean.class || cls == Boolean.class) {
                return Boolean.TRUE.equals(value) ? trueInteger : falseInteger;
            }
            else if (cls == String.class) {
                // FIXME: need to prevent GC, but for how long?
                return new NativeString(value.toString(), false).getPointer();
            }
            else if (cls == WString.class) {
                // FIXME: need to prevent GC, but for how long?
                return new NativeString(value.toString(), true).getPointer();
            }
            else if (!isAllowableNativeType(cls)) {
                throw new IllegalArgumentException("Return type " + cls + " will be ignored");
            }
            return value;
        }
        /** Called from native code.  All arguments are in an array of 
         * Object as the first argument.  Converts all arguments to types
         * required by the actual callback method signature, and converts
         * the result back into an appropriate native type.
         * This method <em>must not</em> throw exceptions. 
         */
        public Object callback(Object[] args) {
            try {
                return proxyCallback(args);
            } catch (Exception e) {
                return Integer.valueOf(0);
            }
            
        }

        public Class[] getParameterTypes() {
            return nativeParamTypes;
        }

        public Class getReturnType() {
            return nativeReturnType;
        }
        
    }
}
