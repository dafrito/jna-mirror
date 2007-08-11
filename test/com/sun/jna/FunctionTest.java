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
package com.sun.jna;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class FunctionTest extends TestCase {
    
    public static interface BooleanTestLibrary extends Library {
        boolean returnInt32Argument(boolean b);
    }
    /*
     * Test that getMethod() returns the proxy Method that invoked it
     */
    public void testMethodIsSet() throws Exception {
        Map options = new HashMap();
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        final boolean[] set = { false };
        final Method method = BooleanTestLibrary.class.getMethod("returnInt32Argument", new Class[] { boolean.class });
        if (method == null) throw new RuntimeException("Could not locate method in interface");
        mapper.addFromNativeConverter(Boolean.class, new FromNativeConverter() {
            public Object fromNative(Object value, FromNativeContext context) {
                FunctionResultContext functionContext = (FunctionResultContext)context;
                Method m = functionContext.getFunction().getMethod();
                // Check that its not null, and that it over-rides the method in the interface
//                set[0] = m != null && m.getName().equals(method.getName()) && 
//                        Arrays.equals(m.getParameterTypes(), method.getParameterTypes());
                set[0] = m.equals(method);
                return Boolean.TRUE;
            }
            public Class nativeType() { 
                return Integer.class;
            }
        });
        options.put(Library.OPTION_TYPE_MAPPER, mapper);
        BooleanTestLibrary lib = (BooleanTestLibrary) 
            Native.loadLibrary("testlib", BooleanTestLibrary.class, options);
        lib.returnInt32Argument(true);
        assertTrue("Method not set on Function instance", set[0]);
    }
    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(FunctionTest.class);
    }
}
