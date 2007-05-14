/*
 * VarArgsTest.java
 * JUnit based test
 *
 * Created on 14 May 2007, 11:20
 */

package com.sun.jna;

import junit.framework.TestCase;

/**
 *
 * @author wayne
 */
public class VarArgsTest extends TestCase {
    public static interface TestLibrary extends Library {
        TestLibrary INSTANCE = (TestLibrary)
                Native.loadLibrary("testlib", TestLibrary.class);
        public int addInt32VarArgs(String fmt, Object[] args);
        public String returnStringVarArgs(String fmt, Object[] args);
    }
    public VarArgsTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    public void testIntVarArgs() {
        Integer[] args = new Integer[2];
        int arg1 = 1;
        int arg2 = 2;
        args[0] = Integer.valueOf(arg1);
        args[1] = Integer.valueOf(arg2);
        assertEquals("VarArgs not added correctly", arg1 + arg2,
                TestLibrary.INSTANCE.addInt32VarArgs("dd", args));
    }
    public void testShortVarArgs() {
        Object[] args = new Short[2];
        short arg1 = 1;
        short arg2 = 2;
        args[0] = Short.valueOf(arg1);
        args[1] = Short.valueOf(arg2);
        assertEquals("VarArgs not added correctly", arg1 + arg2,
                TestLibrary.INSTANCE.addInt32VarArgs("dd", args));
    }
    public void testLongtVarArgs() {
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
