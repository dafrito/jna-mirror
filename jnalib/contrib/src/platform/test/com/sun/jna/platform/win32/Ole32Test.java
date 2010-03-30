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
package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Guid.GUID;

import junit.framework.TestCase;

public class Ole32Test extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Ole32Test.class);       
    }
    
    public void testCoCreateGUID() {
    	GUID.ByReference pguid = new GUID.ByReference();
    	assertEquals(W32Errors.S_OK, Ole32.INSTANCE.CoCreateGuid(pguid));
    	assertTrue(pguid.Data1 != 0 || pguid.Data2 != 0 || pguid.Data3 != 0 && pguid.Data4 != null);
    }
    
    public void testIIDFromString() {
    	GUID.ByReference lpiid = new GUID.ByReference();
    	assertEquals(W32Errors.S_OK, Ole32.INSTANCE.IIDFromString(
    			"{13709620-C279-11CE-A49E-444553540000}", lpiid)); // Shell.Application.1
    	assertEquals(0x13709620, lpiid.Data1);
    	assertEquals(0xFFFFC279, lpiid.Data2);
    	assertEquals(0x11CE, lpiid.Data3);
    	assertEquals(0xFFFFFFA4, lpiid.Data4[0]);
    	assertEquals(0xFFFFFF9E, lpiid.Data4[1]);
    	assertEquals(0x44, lpiid.Data4[2]);
    	assertEquals(0x45, lpiid.Data4[3]);
    	assertEquals(0x53, lpiid.Data4[4]);
    	assertEquals(0x54, lpiid.Data4[5]);
    	assertEquals(0, lpiid.Data4[6]);
    	assertEquals(0, lpiid.Data4[7]);   	
    }
    
    public void testStringFromGUID2() {
    	GUID.ByReference pguid = new GUID.ByReference();   	
    	pguid.Data1 = 0;
    	pguid.Data2 = 0;
    	pguid.Data3 = 0;
    	for (int i = 0; i < pguid.Data4.length; i++) {
    		pguid.Data4[i] = 0;
    	}
    	int max = 39;
    	char[] lpsz = new char[max];
    	int len = Ole32.INSTANCE.StringFromGUID2(pguid, lpsz, max);
    	assertTrue(len > 1);
    	lpsz[len - 1] = 0;    	
    	assertEquals("{00000000-0000-0000-0000-000000000000}", Native.toString(lpsz));
    }
}
