/*
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

import junit.framework.TestCase;

public class GlobalVariableTest extends TestCase {
    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(GlobalVariableTest.class);
    }
    public GlobalVariableTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    static interface TestLibrary extends Library {
        static TestLibrary INSTANCE = (TestLibrary) Native.loadLibrary("testlib", TestLibrary.class);
        GlobalVariable<Integer> globalInt = GlobalVariable.getInstance(Integer.class, TestLibrary.class, "globalInt");
        GlobalVariable<Long> globalLong = GlobalVariable.getInstance(Long.class, TestLibrary.class, "globalLong");
        GlobalVariable<String> globalString = GlobalVariable.getInstance(String.class, TestLibrary.class, "globalString");
    }
    public void testGetGlobalInt() {
        final int MAGIC = 0x12345678;        
        
        GlobalVariable<Integer> gi = TestLibrary.globalInt;
        assertEquals("Global Integer does not match", MAGIC, gi.getValue().intValue());
    }
    public void testSetGlobalInt() {
        TestLibrary.globalInt.setValue(0xdeadbeef);
        Pointer p = NativeLibrary.getInstance("testlib").getFunction("globalInt");
        assertEquals("Global Int not set", 0xdeadbeef, p.getInt(0));
    }
    public void testGetGlobalLong() {
        final long MAGIC = 0x123456789ABCDEF0L;
        
        GlobalVariable<Long> g = TestLibrary.globalLong;
        assertEquals("Global Long does not match", MAGIC, g.getValue().longValue());
    }
    public void testSetGlobalLong() {
        TestLibrary.globalLong.setValue(0xdeadbeefL);
        Pointer p = NativeLibrary.getInstance("testlib").getFunction("globalLong");
        assertEquals("Global Long not set", 0xdeadbeefL, p.getLong(0));
    }
    public void testGetGlobalString() {
        final String MAGIC = "A Global String";
        
        GlobalVariable<String> g = TestLibrary.globalString;
        assertEquals("Global String does not match", MAGIC, g.getValue());
    }
}
