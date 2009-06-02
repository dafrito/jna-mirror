/* Copyright (c) 2009 Timothy Wall, All Rights Reserved
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

import com.sun.jna.ptr.IntByReference;

/** Exercise callback-related functionality.
 *
 * @author twall@users.sf.net
 */
public class RawCallbacksTest extends CallbacksTest {

    public static class RawTestLibrary implements TestLibrary {
        public native void callVoidCallback(VoidCallbackCustom c);
        public native boolean callBooleanCallback(BooleanCallback c, boolean arg, boolean arg2);
        public native byte callInt8Callback(ByteCallback c, byte arg, byte arg2);
        public native short callInt16Callback(ShortCallback c, short arg, short arg2);
        public native int callInt32Callback(Int32Callback c, int arg, int arg2);
        public NativeLong callNativeLongCallback(NativeLongCallback c, NativeLong arg, NativeLong arg2) { throw new UnsupportedOperationException(); }
        public native long callInt64Callback(Int64Callback c, long arg, long arg2);
        public native float callFloatCallback(FloatCallback c, float arg, float arg2);
        public native double callDoubleCallback(DoubleCallback c, double arg, double arg2);
        public native SmallTestStructure callStructureCallback(StructureCallback c, SmallTestStructure arg);
        public native String callStringCallback(StringCallback c, String arg);
        public WString callWideStringCallback(WideStringCallback c, WString arg) { throw new UnsupportedOperationException(); }
        public Pointer callStringArrayCallback(StringArrayCallback c, String[] arg) { throw new UnsupportedOperationException(); }
        public int callCallbackWithByReferenceArgument(CopyArgToByReference cb, int arg, IntByReference result) { throw new UnsupportedOperationException(); }
        public native TestStructure.ByValue callCallbackWithStructByValue(TestStructure.TestCallback callback, TestStructure.ByValue cbstruct);
        public native CbCallback callCallbackWithCallback(CbCallback cb);
        public native Int32CallbackX returnCallback();
        public native Int32CallbackX returnCallbackArgument(Int32CallbackX cb);
        public native void callVoidCallback(VoidCallback c);

        static {
            Native.register("testlib");
        }
    }

    protected void setUp() {
        //System.out.println(getName());
        lib = new RawTestLibrary();
    }
    
    // Currently unsupported tests
    public void testCallNativeLongCallback() { }
    public void testCallWideStringCallback() { }
    public void testCallStringArrayCallback() { }
    public void testCallCallbackWithByReferenceArgument() { }
    public void testCallbackExceptionHandler() { }
    public void testCallbackExceptionHandlerWithCallbackProxy() { }

    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(RawCallbacksTest.class);
    }
}
