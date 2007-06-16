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

package com.sun.jna;

public interface CallbackProxy extends Callback {
    /*
     * This is a special subclass of Callback, that expects its callback() method
     * to take an array of objects, instead of individual arguments.
     */
     public Object callback(Object[] args);
     public Class[] getParameterTypes();
     public Class getReturnType();
}
