/* This library is free software; you can redistribute it and/or
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

import com.sun.jna.win32.StdCallLibrary;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author  Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 * @author wmeissner@gmail.com
 */
class CallbackReference extends WeakReference {
    static final int MAX_NARGS = 32;
    static final Map callbackMap = new WeakHashMap();
    static final Map altCallbackMap = new WeakHashMap();
    private static final Library cLibrary = new Library() {};
    private static final Library altLibrary = new StdCallLibrary() {};
    
    Pointer cbstruct;
    private CallbackReference(Callback callback, Pointer cbstruct) {
        super(callback);
        this.cbstruct = cbstruct;
    }
    public Pointer getTrampoline() {
        return cbstruct.getPointer(0);
    }
    public static CallbackReference getInstance(int callingConvention, Callback cb) {
        CallbackReference cbref;
        Map map = callingConvention == Function.ALT_CONVENTION ? altCallbackMap : callbackMap;
        synchronized (map) {
            cbref = (CallbackReference) map.get(cb);
            if (cbref == null) {
                Pointer cbstruct = createCallback(callingConvention, cb);
                cbref = new CallbackReference(cb, cbstruct);
                map.put(cb, cbref);
            }
            return cbref;
        }
    }
    public static CallbackReference getInstance(Callback cb) {
        return getInstance(Function.C_CONVENTION, cb);
    }
    public static CallbackReference getInstance(Library lib, Callback cb) {
        int callingConvention = lib instanceof StdCallLibrary 
                ? Function.ALT_CONVENTION : Function.C_CONVENTION;
        return getInstance(callingConvention, cb);
    }
    private static Pointer createCallback(int callingConvention, Callback obj) {
        Method[] mlist = obj.getClass().getMethods();
        for (int i=0;i < mlist.length;i++) {
            if (Callback.METHOD_NAME.equals(mlist[i].getName())) {
                Method m = mlist[i];
                Class[] paramTypes;
                Class rtype;
                if (obj instanceof CallbackProxy) {
                    CallbackProxy proxy = (CallbackProxy)obj;
                    paramTypes = proxy.getParameterTypes();
                    rtype = proxy.getReturnType();
                } else {
                    paramTypes = m.getParameterTypes();
                    rtype = m.getReturnType();
                }
                if (paramTypes.length > MAX_NARGS) {
                    String msg = "Method signature exceeds the maximum "
                            + "parameter count: " + m;
                    throw new IllegalArgumentException(msg);
                }
                return createNativeCallback(obj, m, paramTypes, rtype, callingConvention);
            }
        }
        String msg = "Callback must implement method named '"
                + Callback.METHOD_NAME + "'";
        throw new IllegalArgumentException(msg);
    }
    protected void finalize() {
        freeNativeCallback(cbstruct.peer);
        cbstruct.peer = 0;
    }
    private static native Pointer createNativeCallback(Callback obj, Method m, Class[] paramTypes, 
            Class returnType, int callingConvention);
    private static native void freeNativeCallback(long peer);
}
