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

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class TypeConverterTest extends TestCase {
    public static interface TestLibrary extends Library {
        int returnInt32Argument(boolean b);
        int returnInt32Argument(String s);
        int returnInt32Argument(Number n);
    }
    public TypeConverterTest(String testName) {
        super(testName);
    }
    public void testBooleanToInt() {
        final int MAGIC = 0xABEDCF23;
        Map options = new HashMap();
        final TypeConverter booleanConverter = new TypeConverter() {
            public Object toNative(Object arg) {
                return Integer.valueOf(Boolean.TRUE.equals(arg) ? MAGIC : 0);
            }

            public Object fromNative(Object value, Class returnType) {
                return Boolean.valueOf(((Integer) value).intValue() != 0);
            }

            public Class invocationType() { return Integer.class; }
        };
        TypeMapper typeMapper = new TypeMapper() {
            public TypeConverter getTypeConverter(Class cls) {
                if (Boolean.class.isAssignableFrom(cls)) {
                    return booleanConverter;
                }
                return null;
            }
        };
        options.put("type-mapper", typeMapper);
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib",
                TestLibrary.class, options);
        assertEquals("Failed to convert Boolean to Int", MAGIC,
                lib.returnInt32Argument(true));
    }
    public void testStringToInt() {
        final TypeConverter stringConverter = new TypeConverter() {
            public Object toNative(Object arg) {
                return Integer.valueOf((String) arg, 16);
            }

            public Object fromNative(Object value, Class returnType) {
                return value;
            }
            public Class invocationType() { return Integer.class; }
        };
        TypeMapper typeMapper = new TypeMapper() {
            public TypeConverter getTypeConverter(Class cls) {
                if (String.class.isAssignableFrom(cls)) {
                    return stringConverter;
                }
                return null;
            }
        };
        Map options = new HashMap();
        options.put("type-mapper", typeMapper);
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib",
                TestLibrary.class, options);
        assertEquals("Failed to convert String to Int", MAGIC,
                lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testCharSequenceToInt() {
        final TypeConverter stringConverter = new TypeConverter() {
            public Object toNative(Object arg) {
                return Integer.valueOf(((CharSequence)arg).toString(), 16);
            }

            public Object fromNative(Object value, Class returnType) {
                return value;
            }
            public Class invocationType() { return Integer.class; }
        };
        TypeMapper typeMapper = new TypeMapper() {
            public TypeConverter getTypeConverter(Class cls) {
                if (CharSequence.class.isAssignableFrom(cls)) {
                    return stringConverter;
                }
                return null;
            }
        };
        Map options = new HashMap();
        options.put("type-mapper", typeMapper);
        
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib",
                TestLibrary.class, options);
        assertEquals("Failed to convert String to Int", MAGIC,
                lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testNumberToInt() {
        final TypeConverter numberConverter = new TypeConverter() {
            public Object toNative(Object arg) {
                return Integer.valueOf(((Double)arg).intValue());
            }

            public Object fromNative(Object value, Class returnType) {
                return value;
            }
            public Class invocationType() { return Integer.class; }
        };
        TypeMapper typeMapper = new TypeMapper() {
            public TypeConverter getTypeConverter(Class cls) {
                if (Double.class.isAssignableFrom(cls)) {
                    return numberConverter;
                }
                return null;
            }
        };
        Map options = new HashMap();
        options.put("type-mapper", typeMapper);
        
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = (TestLibrary) Native.loadLibrary("testlib",
                TestLibrary.class, options);
        assertEquals("Failed to convert Double to Int", MAGIC,
                lib.returnInt32Argument(Double.valueOf(MAGIC)));
    }
    public static interface BooleanTestLibrary extends Library {
        boolean returnInt32Argument(boolean b);
    }
    public void testBooleanReturn() throws Exception {
        final int MAGIC = 0xABEDCF23;
        Map options = new HashMap();
        final TypeConverter booleanConverter = new TypeConverter() {
            public Object toNative(Object arg) {
                return Integer.valueOf(Boolean.TRUE.equals(arg) ? MAGIC : 0);
            }

            public Object fromNative(Object value, Class returnType) {
                return Boolean.valueOf(((Integer) value).intValue() == MAGIC);
            }

            public Class invocationType() { return Integer.class; }
        };
        TypeMapper typeMapper = new TypeMapper() {
            public TypeConverter getTypeConverter(Class cls) {
                if (Boolean.class.isAssignableFrom(cls)) {
                    return booleanConverter;
                }
                return null;
            }
        };
        options.put("type-mapper", typeMapper);
        BooleanTestLibrary lib = (BooleanTestLibrary) Native.loadLibrary("testlib",
                BooleanTestLibrary.class, options);
        assertEquals("Failed to convert Boolean.TRUE to Int", true,
                lib.returnInt32Argument(true));
        assertEquals("Failed to convert Boolean.FALSE to Int", false,
                lib.returnInt32Argument(false));
    }
}
