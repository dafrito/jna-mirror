/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
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

import java.lang.reflect.Method;

/** Provide argument conversion context for a callback invocation. */
public class CallbackInvocationContext extends FromNativeContext {
    private Method method;
    private Object[] args;
    public CallbackInvocationContext(Class javaType, Method m, Object[] args) {
        super(javaType);
        this.method = m;
        this.args = args;
    }
    public Method getMethod() { return method; }
    public Object[] getArguments() { return args; }
}
