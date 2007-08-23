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

import java.awt.Component;
import java.awt.Window;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Provides generation of invocation plumbing for a defined native
 * library interface.
 * <p>
 * {@link #getTypeMapper} and {@link #getStructureAlignment} are provided
 * to avoid having to explicitly pass these parameters to {@link Structure}s, 
 * which would require every {@link Structure} which requries custom mapping
 * or alignment to define a constructor and pass parameters to the superclass.
 * To avoid lots of boilerplate, the base {@link Structure} constructor
 * figures out these properties based on the defining interface.
 * 
 * @see Library
 * @author Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public final class Native {
    private static Map libraries  = Collections.synchronizedMap(new WeakHashMap());
    private static Map nativeLibraries  = Collections.synchronizedMap(new WeakHashMap());
    private static Map typeMappers = Collections.synchronizedMap(new WeakHashMap());
    private static Map alignments = Collections.synchronizedMap(new WeakHashMap());
    
    private Native() { }
    
    /** Utility method to get the native window ID for a Java {@link Window}
     * as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows. 
     */
    public static long getWindowID(Window w) {        
        return getComponentID(w);
    }
    
    /** Utility method to get the native window pointer for a Java 
     * {@link Window} as a {@link Pointer} value.  This method is primarily for 
     * Windows, which uses the <code>HANDLE</code> type (actually 
     * <code>void *</code>) to identify windows. 
     */
    public static Pointer getWindowPointer(Window w) {        
        return getComponentPointer(w);        
    }
    
    /** Utility method to get the native window pointer for a Java 
     * {@link Component} as a {@link Pointer} value.  This method is primarily for 
     * Windows, which uses the <code>HANDLE</code> type (actually 
     * <code>void *</code>) to identify windows. 
     */
    public static Pointer getComponentPointer(Component c) {
        if (!c.isDisplayable())
            throw new IllegalStateException("Component is not yet displayable");
        return new Pointer(getComponentID(c));
    }
    
    /** Utility method to get the native window ID for a Java {@link Component}
     * as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>int32</code>) to identify windows. 
     */
    public static long getComponentID(Component c) {
        if (c.isLightweight()) {
            throw new IllegalArgumentException("Component cannot be lightweight");
        }
        if (!c.isDisplayable()) {
            throw new IllegalStateException("Component is not yet displayable");
        }
        // On X11 VMs prior to 1.5, the component must be visible
        if (System.getProperty("java.version").matches("^1\\.4\\..*")) {
            if (!c.isVisible()) {
                throw new IllegalStateException("Component is not yet visible");
            }
        }
        return getWindowHandle0(c);
    }
    private static native long getWindowHandle0(Component c);
    
    /** Convert a direct {@link ByteBuffer} into a {@link Pointer}. 
     * @throws IllegalArgumentException if the byte buffer is not direct.
     */
    @Deprecated
    public static Pointer getByteBufferPointer(ByteBuffer b) {
        return getDirectBufferPointer(b);
    }
    
    /** Convert a direct {@link Buffer} into a {@link Pointer}. 
     * @throws IllegalArgumentException if the byte buffer is not direct.
     */
    public static Pointer getBufferPointer(ByteBuffer b) {
        return getDirectBufferPointer(b);
    }
    
    /** Convert a direct {@link Buffer} into a {@link Pointer}. 
     * @throws IllegalArgumentException if the byte buffer is not direct.
     */
    static native Pointer getDirectBufferPointer(Buffer b);
    
    /** Obtain a Java String from the given native byte array.  If there is
     * no NUL terminator, the String will comprise the entire array.
     */
    public static String toString(byte[] buf) {
        String encoding = System.getProperty("jna.encoding");
        if (encoding != null) {
            try {
                return new String(buf, encoding);
            }
            catch(UnsupportedEncodingException e) { }
        }
        String s = new String(buf);
        int term = s.indexOf(0);
        if (term != -1)
            s = s.substring(0, term);
        return s;
    }
    
    /** Obtain a Java String from the given native wchar_t array.  If there is
     * no NUL terminator, the String will comprise the entire array.
     */
    public static String toString(char[] buf) {
        String s = new String(buf); 
        int term = s.indexOf(0);
        if (term != -1)
            s = s.substring(0, term);
        return s;
    }
    
    /** Load a library interface from the given shared library, providing
     * the explicit interface class.
     */
    public static Library loadLibrary(String name, Class interfaceClass) {
        return loadLibrary(name, interfaceClass, Collections.EMPTY_MAP);
    }

    /** Load a library interface from the given shared library, providing
     * the explicit interface class and a map of options for the library.
     * If no library options are detected the map is interpreted as a map
     * of Java method names to native function names.
     * @param name
     * @param interfaceClass
     * @param options Map of library options
     */
    public static Library loadLibrary(String name, 
                                      Class interfaceClass,
                                      Map options) {
        if (!Library.class.isAssignableFrom(interfaceClass)) {
            throw new IllegalArgumentException("Not a valid native library interface: " + interfaceClass);
        }
        InvocationHandler handler = 
            new Library.Handler(name, interfaceClass, options);
        ClassLoader loader = interfaceClass.getClassLoader();
        Library proxy = (Library)
            Proxy.newProxyInstance(loader, new Class[] {interfaceClass},
                                   handler);
        if (options.containsKey(Library.OPTION_TYPE_MAPPER))
            typeMappers.put(interfaceClass, options.get(Library.OPTION_TYPE_MAPPER));
        if (options.containsKey(Library.OPTION_STRUCTURE_ALIGNMENT))
            alignments.put(interfaceClass, options.get(Library.OPTION_STRUCTURE_ALIGNMENT));
        nativeLibraries.put(interfaceClass, new WeakReference(NativeLibrary.getInstance(name)));
        return proxy;
    }
    
    /** Returns whether an instance variable was instantiated. */
    private static boolean loadInstance(Class cls) {
        if (libraries.containsKey(cls)) {
            return true;
        }
        if (cls != null) {
            try {
                Field[] fields = cls.getFields();
                for (int i=0;i < fields.length;i++) {
                    Field field = fields[i];
                    if (field.getType() == cls 
                        && (field.getModifiers() & Modifier.STATIC) != 0) {
                        libraries.put(cls, field.get(null));
                        return true;
                    }
                }
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Could not access instance of " 
                                                   + cls + " (" + e + ")");
            }
        }
        return false;
    }
    
    /** Return the preferred {@link TypeMapper} for the given native interface.
     */
    public static TypeMapper getTypeMapper(Class interfaceClass) {
		//
        // Abort if the structure was declared inside some other type of class
        //
        if (interfaceClass == null
            || !Library.class.isAssignableFrom(interfaceClass)) {
            return null;
        }
        if (typeMappers.containsKey(interfaceClass)) {
            return (TypeMapper)typeMappers.get(interfaceClass);
        }
        if (!loadInstance(interfaceClass)
            || !typeMappers.containsKey(interfaceClass)) {
            try {
                Field field = interfaceClass.getField("TYPE_MAPPER");
                typeMappers.put(interfaceClass, field.get(null));
            }
            catch (NoSuchFieldException e) {
                //
                // insert a null TypeMapper so the field is not repeatedly
                // looked up
                typeMappers.put(interfaceClass, null);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("TYPE_MAPPER must be a public TypeMapper field (" 
                                                   + e + "): " + interfaceClass);
            }
        }
        return (TypeMapper)typeMappers.get(interfaceClass);
    }

    /** Return the preferred structure alignment for the given native interface. 
     */
    public static int getStructureAlignment(Class interfaceClass) {
        //
        // Abort if the structure was declared inside some other type of class
        //
        if (interfaceClass == null
            || !Library.class.isAssignableFrom(interfaceClass)) {
            return Structure.ALIGN_DEFAULT;
        }
        if (alignments.containsKey(interfaceClass)) {
            Integer value = (Integer)alignments.get(interfaceClass);
            return value != null ? value.intValue() : Structure.ALIGN_DEFAULT;
        }
        if (!loadInstance(interfaceClass) 
            || !alignments.containsKey(interfaceClass)) {
            try {
                Field field = interfaceClass.getField("STRUCTURE_ALIGNMENT");
                alignments.put(interfaceClass, field.get(null));
            }
            catch(NoSuchFieldException e) {
                //
                // insert a default alignment so the field is not repeatedly
                // looked up
                alignments.put(interfaceClass,
                               new Integer(Structure.ALIGN_DEFAULT));
            }
            catch(Exception e) {
                throw new IllegalArgumentException("STRUCTURE_ALIGNMENT must be a public int field ("
                                                   + e + "): " + interfaceClass);
            }
        }
        Integer value = (Integer)alignments.get(interfaceClass);
        return value != null ? value.intValue() : Structure.ALIGN_DEFAULT;
    }
    
    static NativeLibrary getNativeLibrary(Class interfaceClass) {
        WeakReference ref = (WeakReference)nativeLibraries.get(interfaceClass);
        if (ref != null) {
            return (NativeLibrary)ref.get();
        }
        return null;
    }
    
    /** Return an byte array corresponding to the given String.  If the
     * system property <code>jna.encoding</code> is set, it will override
     * the default platform encoding (if supported).
     */
    static byte[] getBytes(String s) {
        String encoding = System.getProperty("jna.encoding");
        if (encoding != null) {
            try {
                return s.getBytes(encoding);
            }
            catch (UnsupportedEncodingException e) {
            }
        }
        return s.getBytes();
    }
}
