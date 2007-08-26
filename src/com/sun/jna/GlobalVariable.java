/*
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


public class GlobalVariable<T> {
    Pointer valuePointer;
    FromNativeConverter fromNative;
    ToNativeConverter toNative;
    Class<T> type;
    public GlobalVariable(Class<T> type, Class<? extends Library> libClass, String name) {
        this(type, getVariablePointer(libClass, name), libClass, name);
    }
    GlobalVariable(Class<T> type, Pointer ptr, Class<? extends Library> libClass, String name) {
        valuePointer = ptr;
        this.type = type;
        TypeMapper mapper = Native.getTypeMapper(libClass);
        if (mapper != null) {
            fromNative = mapper.getFromNativeConverter(type);
            toNative = mapper.getToNativeConverter(type);
        } else {
            fromNative = new FromNativeConverter() {
                public Object fromNative(Object value, FromNativeContext context) {
                    return value;
                }

                public Class nativeType() {
                    return GlobalVariable.this.type;
                }                
            };
            toNative = new ToNativeConverter() {
                public Object toNative(Object value) {
                    return value;
                }                
            };
        }
    }
    protected static Pointer getVariablePointer(Class<? extends Library> libClass, String name) {
        NativeLibrary l = Native.getNativeLibrary(libClass);
        Pointer p= l.getFunction(name);
        if (p == null) {
            throw new IllegalArgumentException("Cannot locate symbol " + name);
        }
        return p;
    }
    public static <U> GlobalVariable<U> getInstance(Class<U> type, Class<? extends Library> libClass, String name) {
        return new GlobalVariable<U>(type, getVariablePointer(libClass, name), libClass, name);
    }
    public T getValue() {
        final Class nativeType = fromNative.nativeType();
        Object value = null;
        if (Byte.class == nativeType) {
            value = valuePointer.getByte(0);
        }
        else if (Short.class == nativeType) {
            value = valuePointer.getShort(0);
        }
        else if (Integer.class == nativeType) {
            value = valuePointer.getInt(0);
        }
        else if (Long.class == nativeType) {
            value = valuePointer.getLong(0);
        }
        else if (Float.class == nativeType) {
            value = valuePointer.getFloat(0);
        }
        else if (Double.class == nativeType) {
            value = valuePointer.getDouble(0);
        }
        else if (Pointer.class == nativeType) {
            value = valuePointer.getPointer(0);
        }
        else if (String.class == nativeType) {
            //
            // The valuePointer is the address of the String pointer, so indirect
            // to get the actual String value.
            //
            Pointer p = valuePointer.getPointer(0);
            value = p != null ? p.getString(0) : null;            
        }
        else {
            throw new IllegalArgumentException("Cannot read global variable of type: " + type);
        }        
        if (value == null) {
            return null;
        } else {
            return type.cast(fromNative.fromNative(value, new FromNativeContext(nativeType)));
        }
    }
    public void setValue(T value) {
        Object nativeValue = toNative.toNative(value);
        
        if (nativeValue instanceof Byte) {
            valuePointer.setByte(0, ((Number)nativeValue).byteValue());
        }
        else if (nativeValue instanceof Short) {
            valuePointer.setShort(0, ((Number)nativeValue).shortValue());
        }
        else if (nativeValue instanceof Integer) {
            valuePointer.setInt(0, ((Number)nativeValue).intValue());
        }
        else if (nativeValue instanceof Long) {
            valuePointer.setLong(0, ((Number)nativeValue).longValue());
        }
        else if (nativeValue instanceof Float) {
            valuePointer.setFloat(0, ((Number)nativeValue).floatValue());
        }
        else if (nativeValue instanceof Double) {
            valuePointer.setDouble(0, ((Number)nativeValue).doubleValue());
        }
        else if (nativeValue instanceof Pointer) {
            valuePointer.setPointer(0, (Pointer)nativeValue);
        }
//        else if (nativeValue instanceof String) {
//            valuePointer.setString(0, (String)nativeValue);
//        }
        else {
            throw new IllegalArgumentException("Cannot set global variable of type: " + value.getClass());
        }
    }    
}
