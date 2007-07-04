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


public class UInt16 extends NativeInt16 {

    public UInt16(Short value) {
        super(value);
    }
    public UInt16(int value) {
        this(convert(value));
    }
    
    public int intValue() {
        int i = nativeValue.shortValue();
        return (i < 0) ? ((i & 0x7FFF) + 0x8000) : i;
    }

    public long longValue() {
        return intValue();
    }

    private static Short convert(int value) {        
        if (value >= 0 && value <= 0xFFFF) {
            return new Short((short)value);
        }
        throw new IllegalArgumentException(value + " exceeds capacity of java.lang.Short");
    }
}
