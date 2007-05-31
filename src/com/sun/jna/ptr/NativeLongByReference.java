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

package com.sun.jna.ptr;

import com.sun.jna.NativeLong;
import java.nio.ByteBuffer;

/**
 *
 * @author wayne
 */
public class NativeLongByReference extends ByReference {
    private NativeLong value;
    
    public NativeLongByReference() {
        this(0);
    }
    public NativeLongByReference(long value) {
        this(new NativeLong(value));
    }
    public NativeLongByReference(NativeLong value) {
        super(NativeLong.SIZE);
        setValue(value);
    }
    
    public void setValue(NativeLong value) {
        this.value = value;
    }
    
    public NativeLong getValue() {
        return value;
    }
    public void writeTo(ByteBuffer buf) {
        if (NativeLong.SIZE == 4) {
            buf.putInt(value.intValue());
        } else {
            buf.putLong(value.longValue());
        }
    }
    public void readFrom(ByteBuffer buf) {
        if (NativeLong.SIZE == 4) {
            setValue(new NativeLong(buf.getInt()));
        } else {
            setValue(new NativeLong(buf.getLong()));
        }
    }
}