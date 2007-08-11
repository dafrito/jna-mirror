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

import java.io.File;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;

public class NativeLibraryTest extends TestCase {
    
    public static interface TestLibrary extends Library {
        int callCount();
    }
    
    public void testGCNativeLibrary() throws Exception {
        NativeLibrary lib = NativeLibrary.getInstance("testlib");
        WeakReference ref = new WeakReference(lib);
        lib = null;
        System.gc();
        long start = System.currentTimeMillis();
        while (ref.get() != null) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - start > 5000) 
                break;
        }
        assertNull("Library not GC'd", ref.get());
    }

    public void testAvoidDuplicateLoads() {
        TestLibrary lib = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        assertEquals("Library should be loaded exactly once",
                     1, lib.callCount());
        assertEquals("Library should not be reloaded",
                     2, lib.callCount());
    }
    
    public void testUseSingleLibraryInstance() {
        TestLibrary lib = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        int count = lib.callCount();
        TestLibrary lib2 = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        int count2 = lib2.callCount();
        assertEquals("Interfaces should share a library instance",
                     count + 1, count2);
    }

    public void testAliasLibraryFilename() {
        TestLibrary lib = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        int count = lib.callCount();
        NativeLibrary nl = NativeLibrary.getInstance("testlib");
        TestLibrary lib2 = (TestLibrary)Native.loadLibrary(nl.getFile().getName(), TestLibrary.class);
        int count2 = lib2.callCount();
        assertEquals("Simple filename load not aliased", count + 1, count2);
    }
    
    public void testAliasLibraryFullPath() {
        TestLibrary lib = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        int count = lib.callCount();
        NativeLibrary nl = NativeLibrary.getInstance("testlib");
        TestLibrary lib2 = (TestLibrary)Native.loadLibrary(nl.getFile().getAbsolutePath(), TestLibrary.class);
        int count2 = lib2.callCount();
        assertEquals("Full pathname load not aliased", count + 1, count2);
    }
    
    public void testAliasSimpleLibraryName() throws Exception {
        NativeLibrary nl = NativeLibrary.getInstance("testlib");
        File file = nl.getFile();
        WeakReference ref = new WeakReference(nl);
        nl = null;
        System.gc();
        long start = System.currentTimeMillis();
        while (ref.get() != null) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - start > 5000) 
                fail("Timed out waiting for library to be GC'd");
        }
        TestLibrary lib = (TestLibrary)Native.loadLibrary(file.getAbsolutePath(), TestLibrary.class);
        int count = lib.callCount();
        TestLibrary lib2 = (TestLibrary)Native.loadLibrary("testlib", TestLibrary.class);
        int count2 = lib2.callCount();
        assertEquals("Simple library name not aliased", count + 1, count2);
    }
    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(NativeLibraryTest.class);
    }
}
