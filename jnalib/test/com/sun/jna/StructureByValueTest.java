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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;

import junit.framework.TestCase;

/** General structure by value functionality tests. */
public class StructureByValueTest extends TestCase {

    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(StructureByValueTest.class);
    }

    public static class TestNativeMappedInStructure extends Structure {
        public static class ByValue extends TestNativeMappedInStructure implements Structure.ByValue { }
        public NativeLong field;
    }
    public void testNativeMappedInByValue() {
        new TestNativeMappedInStructure.ByValue();
    }
}