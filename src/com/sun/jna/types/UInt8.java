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

public class UInt8 extends NativeInt8 {

    public UInt8(Byte value) {
        super(value);
    }

    public UInt8(short value) {
        super(convert(value));
    }

    public short shortValue() {
        short i = nativeValue.byteValue();
        return (i < 0) ? (short) ((i & 0x7F) + 0x80) : i;
    }

    public int intValue() {
        return shortValue();
    }

    public long longValue() {
        return intValue();
    }

    private static Byte convert(short value) {        
        if (value >= 0 && value <= 0xff) {
            return new Byte((byte)value);
        }
        throw new IllegalArgumentException(value + " exceeds capacity of java.lang.Byte");
    }
}
