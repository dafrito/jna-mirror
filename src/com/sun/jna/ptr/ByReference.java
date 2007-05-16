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

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import java.nio.ByteBuffer;

/** Provides generic "pointer to type" functionality. */
public abstract class ByReference {
    
    protected final int dataSize;
    protected ByReference(int dataSize) {
        this.dataSize = dataSize;
    }

    public Pointer getPointer() {
        Pointer ptr = new Memory(dataSize);
        writeTo(ptr.getByteBuffer(0, dataSize));
        return ptr;
    }
    public abstract void writeTo(ByteBuffer buf);
    public abstract void readFrom(ByteBuffer buf);
    
}
