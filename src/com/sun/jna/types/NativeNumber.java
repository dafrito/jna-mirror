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

public class NativeNumber extends Number implements NativeValue {

    protected Number nativeValue;

    protected NativeNumber(Number nativeValue) {
        this.nativeValue = nativeValue;
    }

    public Object toNativeValue() {
        return nativeValue;
    }

    public int intValue() {
        return nativeValue.intValue();
    }

    public long longValue() {
        return nativeValue.longValue();
    }

    public float floatValue() {
        return nativeValue.floatValue();
    }

    public double doubleValue() {
        return nativeValue.doubleValue();
    }
}
