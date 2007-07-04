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

import com.sun.jna.NativeLong;

public class size_t extends NativeLong {

    public size_t(Long value) {
        super(value.longValue());
    }
    public size_t(Integer value) {
        super(value.longValue());
    }
    public size_t(long value) {
        super(value);
    }

}
