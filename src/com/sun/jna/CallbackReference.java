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
    public static CallbackReference getInstance(Library lib, Callback cb) {
        CallbackReference cbref;
        Map map = lib instanceof StdCallLibrary ? altCallbackMap : callbackMap;
        synchronized (map) {
            cbref = (CallbackReference) map.get(cb);
            if (cbref == null) {
                Pointer cbstruct = createCallback(lib, cb);
                cbref = new CallbackReference(cb, cbstruct);
                map.put(cb, cbref);
            }
            return cbref;
        }
    }
    public static CallbackReference getInstance(Callback cb) {
        return getInstance(cLibrary, cb);
    }
    public static CallbackReference getInstance(int callingConvention, Callback cb) {
        Library lib = callingConvention == Function.ALT_CONVENTION ? altLibrary : cLibrary;
        return getInstance(lib, cb);
    }
    private static Pointer createCallback(Library library,  Callback obj) {
        Method[] mlist = obj.getClass().getMethods();
        for (int i=0;i < mlist.length;i++) {
            if (Callback.METHOD_NAME.equals(mlist[i].getName())) {
                Method m = mlist[i];
                Class[] paramTypes = m.getParameterTypes();
                Class rtype = m.getReturnType();
                if (paramTypes.length > MAX_NARGS) {
                    String msg = "Method signature exceeds the maximum "
                            + "parameter count: " + m;
                    throw new IllegalArgumentException(msg);
                }
                return Function.createCallback(library, obj, m, paramTypes, rtype);
            }
        }
        String msg = "Callback must implement method named '"
                + Callback.METHOD_NAME + "'";
        throw new IllegalArgumentException(msg);
    }
    protected void finalize() {
        Function.freeCallback(cbstruct.peer);
        cbstruct.peer = 0;
    }
}
