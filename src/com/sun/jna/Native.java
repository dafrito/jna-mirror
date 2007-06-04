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

import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Provides generation of invocation plumbing for a defined native
 * library interface.
 *
 * @author Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public class Native {

    private Native() { }

    /** Utility method to get the native window ID for a Java {@link Window}
     * as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows. 
     */
    public static long getWindowID(Window w) {
        if (!w.isDisplayable()) 
            throw new IllegalStateException("Window is not yet displayable");
        // On X11 VMs prior to 1.5, the window must be visible
        if (System.getProperty("java.version").matches("^1\\.4\\..*")) {
            if (!w.isVisible()) {
                throw new IllegalStateException("Window is not yet visible");
            }
        }
        return getWindowHandle0(w);
    }
    
    /** Utility method to get the native window pointer for a Java 
     * {@link Window} as a {@link Pointer} value.  This method is primarily for 
     * Windows, which uses the <code>HANDLE</code> type (actually 
     * <code>void *</code>) to identify windows. 
     */
    public static Pointer getWindowPointer(Window w) {
        if (!w.isDisplayable())
            throw new IllegalStateException("Window is not yet displayable");
        return new Pointer(getWindowHandle0(w));
    }
    
    private static native long getWindowHandle0(Window w);
    
    /** Obtain a Java String from the given native char array. */
    public static String toString(byte[] buf) {
        String s = new String(buf);
        return s.substring(0, s.indexOf(0));
    }
    
    /** Obtain a Java String from the given native wchar_t array. */
    public static String toString(char[] buf) {
        String s = new String(buf); 
        return s.substring(0, s.indexOf(0));
    }
    
    /** Load a library interface from the given shared library, providing
     * the explicit interface class.
     */
    public static Library loadLibrary(String name, Class interfaceClass) {
        return loadLibrary(name, interfaceClass, Collections.EMPTY_MAP);
    }
    /** Load a library interface from the given shared library, providing
     * the explicit interface class and a map from the interface method
     * names to the actual shared library function names.
     * @param name
     * @param interfaceClass
     * @param optionMap Map of options to use when accessing functions from the library.
     * <p>
     * e.g. "type-mapper" is an instance of TypeMapper used to map types.
     */
    public static Library loadLibrary(String name, 
                                      Class interfaceClass,
                                      Map optionMap) {
        if (!Library.class.isAssignableFrom(interfaceClass)) {
            throw new IllegalArgumentException("Not a valid native library interface: " + interfaceClass);
        }
        typeMappers.put(interfaceClass, optionMap.get("type-mapper"));
        InvocationHandler handler = 
            new Library.Handler(name, interfaceClass, optionMap);
        ClassLoader loader = interfaceClass.getClassLoader();
        Library proxy = (Library)
            Proxy.newProxyInstance(loader, new Class[] {interfaceClass},
                                   handler);
        return proxy;
    }
    
    static TypeMapper getTypeMapper(Class interfaceClass) {
        if (interfaceClass == null) {
            return null;
        }
        
        if (typeMappers.containsKey(interfaceClass)) {
            return (TypeMapper)typeMappers.get(interfaceClass);
        }
        
        //
        // Abort if the structure was declared inside some other type of class
        //
        if (!Library.class.isAssignableFrom(interfaceClass)) {
            return null;
        }
        
        /*
         * Force load the interface class for the library.  This assumes that 
         * all library interfaces follow the convention of declaring an instance
         * of themselves as a static field in order to load the library.
         */
        Field[] fields = interfaceClass.getDeclaredFields();
        for (int i = 0; i < fields.length; ++i) {
            if (fields[i].getType() == interfaceClass) {
                try {
                    fields[i].get(interfaceClass);
                } catch (Exception e) {}
                break;
            }
        }
        return (TypeMapper)typeMappers.get(interfaceClass);
    }
    private static Map typeMappers = Collections.synchronizedMap(new HashMap());
}
