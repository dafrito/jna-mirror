/* Copyright (c) 2007 Wayne Meissner, All Rights Reserved
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

package com.sun.jna.types;

/**
 * If you want to use a NativeValue as a return type, you will also have to
 * implement a static method 'nativeType' that returns the JNA type used to 
 * represent this class natively.
 * e.g.
 *     public static Class nativeType() {
 *         return Pointer.class;
 *     }
 * 
 * The implementing class will also need a public constructor that takes an
 * argument of the type returned from nativeType()
 * e.g.
 *     public Foo(Pointer pointer) { ... }
 * 
 */
public interface NativeValue {
    Object toNativeValue();
}
