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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class ArgumentConverterTest extends TestCase {
    public static interface TestLibrary extends Library {
        int returnInt32Argument(boolean b);
        int returnInt32Argument(String s);
        int returnInt32Argument(Number n);
    }
    public ArgumentConverterTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    public void testBooleanToInt() {
        final int MAGIC = 0xABEDCF23;
        Map converters = new HashMap();
        converters.put(Boolean.class, new ArgumentConverter() {
            public Object convert(Object arg) {
                return Integer.valueOf(Boolean.TRUE.equals(arg) ? MAGIC : 0);
            }
        });
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib", 
                TestLibrary.class, Collections.EMPTY_MAP, converters);
        assertEquals("Failed to convert Boolean to Int", MAGIC,
                lib.returnInt32Argument(true));
    }
    public void testStringToInt() {
        Map converters = new HashMap();
        converters.put(String.class, new ArgumentConverter() {
          public Object convert(Object arg) {
                return Integer.valueOf((String) arg, 16);
            }
        });
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib", 
                TestLibrary.class, Collections.EMPTY_MAP, converters);
        assertEquals("Failed to convert String to Int", MAGIC,
                lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testCharSequenceToInt() {
        Map converters = new HashMap();
        converters.put(CharSequence.class, new ArgumentConverter() {
          public Object convert(Object arg) {
                return Integer.valueOf(((CharSequence)arg).toString(), 16);
            }
        });
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib", 
                TestLibrary.class, Collections.EMPTY_MAP, converters);
        assertEquals("Failed to convert String to Int", MAGIC,
                lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testNumberToInt() {
        Map converters = new HashMap();
        converters.put(Double.class, new ArgumentConverter() {
            public Object convert(Object arg) {
                return Integer.valueOf(((Double)arg).intValue());
            }
        });
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib", 
                TestLibrary.class, Collections.EMPTY_MAP, converters);
        assertEquals("Failed to convert Double to Int", MAGIC,
                lib.returnInt32Argument(Double.valueOf(MAGIC)));
    }
}
