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

import junit.framework.TestCase;

public class VarArgsTest extends TestCase {
    public static interface TestLibrary extends Library {
        TestLibrary INSTANCE = (TestLibrary)
                Native.loadLibrary("testlib", TestLibrary.class);
        public int addInt32VarArgs(String fmt, Object[] args);
        public String returnStringVarArgs(String fmt, Object[] args);
    }
    public void testIntVarArgs() {
        Integer[] args = new Integer[2];
        int arg1 = 1;
        int arg2 = 2;
        args[0] = new Integer(arg1);
        args[1] = new Integer(arg2);
        assertEquals("VarArgs not added correctly", arg1 + arg2,
                TestLibrary.INSTANCE.addInt32VarArgs("dd", args));
    }
    public void testShortVarArgs() {
        Object[] args = new Short[2];
        short arg1 = 1;
        short arg2 = 2;
        args[0] = new Short(arg1);
        args[1] = new Short(arg2);
        assertEquals("VarArgs not added correctly", arg1 + arg2,
                TestLibrary.INSTANCE.addInt32VarArgs("dd", args));
    }
    public void testLongVarArgs() {
        Object[] args = new Object[2];
        short arg1 = 1;
        short arg2 = 2;
        args[0] = Long.valueOf(arg1);
        args[1] = Long.valueOf(arg2);
        assertEquals("VarArgs not added correctly", arg1 + arg2,
                TestLibrary.INSTANCE.addInt32VarArgs("ll", args));
    }
    public void testStringVarArgs() {
        String[] args = new String[] { "Test" };
        assertEquals("Did not return correct string", args[0],
                TestLibrary.INSTANCE.returnStringVarArgs("", args));
    }
}
