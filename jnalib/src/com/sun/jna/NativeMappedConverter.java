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

/** Provides type conversion for instances of {@link NativeMapped}. */
public class NativeMappedConverter implements TypeConverter {
    private Class type;
    private Class nativeType;
    private NativeMapped instance;
    public NativeMappedConverter(Class type) {
        if (!NativeMapped.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must derive from "
                                               + NativeMapped.class);
        this.type = type;
        this.instance = defaultValue();
        this.nativeType = instance.nativeType();
    }
    
    public NativeMapped defaultValue() {
        try {
            return (NativeMapped)type.newInstance();
        }
        catch (InstantiationException e) {
            String msg = "Can't create an instance of " + type
                + ", requires a no-arg constructor: " + e;
            throw new IllegalArgumentException(msg);
        }
        catch (IllegalAccessException e) {
            String msg = "Not allowed to create an instance of " + type
                + ", requires a public, no-arg constructor: " + e;
            throw new IllegalArgumentException(msg);
        }
    }
    public Object fromNative(Object nativeValue, FromNativeContext context) {
        return instance.fromNative(nativeValue, context);
    }

    public Class nativeType() {
        return nativeType;
    }

    public Object toNative(Object value, ToNativeContext context) {
        return ((NativeMapped)value).toNative();
    }
}