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

package com.sun.jna.types.posix;

import com.sun.jna.Platform;
import com.sun.jna.types.NativeNumber;

public class off_t extends NativeNumber {
    public static final int SIZE = Platform.isLinux() ? 4 : 8;
    private static final Class nativeType = Platform.isLinux() ? Integer.class : Long.class;
    public off_t(Integer value) {
        super(convert(value));
    }
    public off_t(Long value) {
        super(convert(value));
    }
    public Class nativeType() {
        return nativeType;
    }
    private static Number convert(Number value) {
        if (SIZE == 4) {
            long masked = value.longValue() & 0xFFFFFFFF80000000L;
            if (masked != 0 && masked != 0xFFFFFFFF80000000L) {
                throw new IllegalArgumentException("Argument exceeds native long capacity");
            }
            return new Integer((int) (value.longValue() & 0xFFFFFFFF));
        } else {
            return new Long(value.longValue());
        }
    }
}
