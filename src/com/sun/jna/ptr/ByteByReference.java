/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
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

import java.nio.ByteBuffer;

public class ByteByReference extends ByReference {
    
    private byte value;
    
    public ByteByReference() {
        this((byte)0);
    }
    
    public ByteByReference(byte value) {
        super(1);
        setValue(value);
    }
    
    public void setValue(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return value;
    }
    public void writeTo(ByteBuffer buf) {
        buf.put((byte) getValue());
    }
    public void readFrom(ByteBuffer buf) {
        setValue(buf.get());
    }
}
