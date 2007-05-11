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

package com.sun.jna;

/**
 *
 * @author wmeissner@gmail.com
 */
public class NativeLong extends Number {
    public static final int SIZE = Pointer.SIZE;
    private final Number value;
    public NativeLong(long value) {
        if (SIZE == Integer.SIZE) {
            this.value = new Integer((int) (value & 0xffffffff));
        } else {
            this.value = new Long(value);
        }
    }
    /**
     * Return the appropriate Number sublclass to natively represent this value
     */
    Number asNativeValue() {
        return value;
    }
    
    public int intValue() {
        return value.intValue();
    }
    
    public long longValue() {
        return value.longValue();
    }
    
    public float floatValue() {
        return value.floatValue();
    }
    
    public double doubleValue() {
        return value.doubleValue();
    }
    public boolean equals(Object rhs) {
        return rhs instanceof NativeLong && value.equals(((NativeLong) rhs).value);
    }
    public String toString() {
        return value.toString();
    }
    public int hashCode() {
        return value.hashCode();
    }
}
