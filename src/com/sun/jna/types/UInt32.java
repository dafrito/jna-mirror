/* Copyright (c) 2007 Wayne Meissner, All Rights Reserved
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

package com.sun.jna.types;


public class UInt32 extends NativeInt32 {

    public UInt32(Integer value) {
        super(value);
    }
    public UInt32(long value) {
        super(convert(value));
    }
    
    public long longValue() {
        long i = nativeValue.intValue();
        return (i < 0) ? (i & 0x7FFFFFFFL) + 0x80000000L : i;
    }

    private static Integer convert(long value) {        
        if (value >= 0 && value <= 0xFFFFFFFFL) {
            return new Integer((int)value);
        }
        throw new IllegalArgumentException(value + " exceeds capacity of java.lang.Byte");
    }
}
