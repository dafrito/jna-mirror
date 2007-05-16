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

import com.sun.jna.Pointer;
import java.nio.ByteBuffer;

/** Represents a reference to a pointer to native data.
 * In C notation, <code>void**</code>.
 * @author twall
 */
public class PointerByReference extends ByReference {
    private Pointer value;
    
    public PointerByReference() {
        this(null);
    }
    
    public PointerByReference(Pointer value) {
        super(Pointer.SIZE);
        setValue(value);
    }
    
    public void setValue(Pointer value) {
        this.value = value;
    }
    
    public Pointer getValue() {
        return value;
    }
    public void writeTo(ByteBuffer buf) {
        long peer = value != null ? value.getNativePeer() : 0;
        if (Pointer.SIZE == 8) {
            buf.putLong(peer);
        } else {
            buf.putInt((int) peer);
        }
    }
    public void readFrom(ByteBuffer buf) {
        long peer = Pointer.SIZE == 8 ? buf.getLong() : buf.getInt();
        value = peer == 0 ? null : new Pointer(peer);
    }
}
