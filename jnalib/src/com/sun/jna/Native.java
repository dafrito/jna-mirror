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
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLDecoder;
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

    private static Map typeMappers = Collections.synchronizedMap(new WeakHashMap());
    private static Map alignments = Collections.synchronizedMap(new WeakHashMap());
    private static Map libraries = Collections.synchronizedMap(new WeakHashMap());
    
    /** The size of a native pointer on the current platform, in bytes. */
    public static final int POINTER_SIZE;
    /** Size of a native long type, in bytes. */
    public static final int LONG_SIZE;
    /** Size of a native wchar_t type, in bytes. */
    public static final int WCHAR_SIZE;

    static {
        if (!loadNativeLibrary()) {
            // Let the system try
            System.loadLibrary("jnidispatch");
        }
        POINTER_SIZE = pointerSize();
        LONG_SIZE = longSize();
        WCHAR_SIZE = wideCharSize();
        // Defer initialization of other JNA classes until *after* initializing 
        // this class's fields
        initIDs();
        if (Boolean.getBoolean("jna.protected")) {
            setProtected(true);
        }
    }

    private Native() { }
    
    private static native void initIDs();

    /** Set whether native memory accesses are protected from invalid
     * accesses.  This should only be set true when testing or debugging,
     * and should not be considered reliable or robust for multithreaded
     * applications.  Protected mode will be automatically set if the
     * system property <code>jna.protected</code> has a value of "true"
     * when the JNA library is first loaded.<p>
     * If not supported by the underlying platform, this setting will
     * have no effect.
     */
    public static native void setProtected(boolean enable);
    
    /** Returns whether protection is enabled.  Check the result of this method
     * after calling {@link #setProtected setProtected(true)} to determine
     * if this platform supports protecting memory accesses.
     */
    public static native boolean isProtected();
    
    /** Utility method to get the native window ID for a Java {@link Window}
     * as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows. 
     */
    public static long getWindowID(Window w) {
        return getComponentID(w);
    }
    
    /** Utility method to get the native window ID for a heavyweight Java 
     * {@link Component} as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows. 
     */
    public static long getComponentID(Component c) {
        if (c.isLightweight()) {
            throw new IllegalArgumentException("Component must be heavyweight");
        }
        if (!c.isDisplayable()) 
            throw new IllegalStateException("Component must be displayable");
        // On X11 VMs prior to 1.5, the window must be visible
        if (Platform.isX11()
            && System.getProperty("java.version").matches("^1\\.4\\..*")) {
            if (!c.isVisible()) {
                throw new IllegalStateException("Component must be visible");
            }
        }
        return getWindowHandle0(c);
    }
    
    /** Utility method to get the native window pointer for a Java 
     * {@link Window} as a {@link Pointer} value.  This method is primarily for 
     * w32, which uses the <code>HANDLE</code> type (actually 
     * <code>void *</code>) to identify windows. 
     */
    public static Pointer getWindowPointer(Window w) {
        return getComponentPointer(w);
    }
    
    /** Utility method to get the native window pointer for a heavyweight Java 
     * {@link Component} as a {@link Pointer} value.  This method is primarily 
     * for w32, which uses the <code>HANDLE</code> type (actually 
     * <code>void *</code>) to identify windows. 
     */
    public static Pointer getComponentPointer(Component c) {
        return new Pointer(getComponentID(c));
    }
    
    private static native long getWindowHandle0(Component c);

    /** Convert a direct {@link Buffer} into a {@link Pointer}. 
     * @throws IllegalArgumentException if the buffer is not direct.
     * @deprecated Use {@link #getDirectBufferPointer} instead. 
     */
    public static Pointer getByteBufferPointer(ByteBuffer b) {
        return getDirectBufferPointer(b);
    }
    
    /** Convert a direct {@link Buffer} into a {@link Pointer}. 
     * @throws IllegalArgumentException if the buffer is not direct.
     */
    public static native Pointer getDirectBufferPointer(Buffer b);
    
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
    public static Object loadLibrary(String name, Class interfaceClass) {
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
    public static Object loadLibrary(String name, 
                                      Class interfaceClass,
                                      Map options) {        
        Library.Handler handler = 
            new Library.Handler(name, interfaceClass, options);
        ClassLoader loader = interfaceClass.getClassLoader();
        Library proxy = (Library)
            Proxy.newProxyInstance(loader, new Class[] {interfaceClass},
                                   handler);
        if (options.containsKey(Library.OPTION_TYPE_MAPPER))
            typeMappers.put(interfaceClass, options.get(Library.OPTION_TYPE_MAPPER));
        if (options.containsKey(Library.OPTION_STRUCTURE_ALIGNMENT))
            alignments.put(interfaceClass, options.get(Library.OPTION_STRUCTURE_ALIGNMENT));
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
                        && Modifier.isStatic(field.getModifiers())) {
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
        if (!loadInstance(interfaceClass) 
            || !typeMappers.containsKey(interfaceClass)) {
            try {
                Field field = interfaceClass.getField("TYPE_MAPPER");
                return (TypeMapper)field.get(null);
            }
            catch (NoSuchFieldException e) {
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
        if (!loadInstance(interfaceClass) 
            || !alignments.containsKey(interfaceClass)) {
            try {
                Field field = interfaceClass.getField("STRUCTURE_ALIGNMENT");
                return ((Integer)field.get(null)).intValue();
            }
            catch(NoSuchFieldException e) {
            }
            catch(Exception e) {
                throw new IllegalArgumentException("STRUCTURE_ALIGNMENT must be a public int field ("
                                                   + e + "): " + interfaceClass);
            }
        }
        Integer value = (Integer)alignments.get(interfaceClass);
        return value != null ? value.intValue() : Structure.ALIGN_DEFAULT;
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

    private static String getNativeLibraryResourcePath() {
        String arch = System.getProperty("os.arch");
        String osPrefix;
        if (Platform.isWindows()) {
            osPrefix = "win32-" + arch;
        }
        else if (Platform.isMac()) {
            osPrefix = "darwin";
        }
        else if (Platform.isLinux()) {
            osPrefix = "linux-" + arch;
        }
        else if (Platform.isSolaris()) {
            osPrefix = "sunos-" + arch;
        }
        else {
            osPrefix = System.getProperty("os.name").toLowerCase();
            int space = osPrefix.indexOf(" ");
            if (space != -1) {
                osPrefix = osPrefix.substring(0, space);
            }
            osPrefix += "-" + arch;
        }
        return "/com/sun/jna/" + osPrefix;
    }

    private static boolean loadNativeLibrary() {
        String libname = System.mapLibraryName("jnidispatch");
        String resourceName = getNativeLibraryResourcePath() + "/" + libname;
        URL url = Native.class.getResource(resourceName);
        if (url == null) {
            System.err.println("Warning: jnidispatch (" + resourceName 
                               + ") not found in resource path");
            return false;
        }
    
        File lib = null;
        if (url.getProtocol().toLowerCase().equals("file")) {
            try {
                lib = new File(URLDecoder.decode(url.getPath(), "UTF8"));
            }
            catch(UnsupportedEncodingException e) {
                throw new Error("JRE is unexpectedly missing UTF8 encoding");
            }
        }
        else {
            InputStream is = Native.class.getResourceAsStream(resourceName);
            if (is == null) {
                throw new Error("Can't obtain jnidispatch InputStream");
            }
            
            FileOutputStream fos = null;
            try {
                lib = File.createTempFile("jna", null);
                lib.deleteOnExit();
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }
            }
            catch(IOException e) {
                throw new Error("Failed to create temporary file for jnidispatch library", e);
            }
            finally {
                try { is.close(); } catch(IOException e) { }
                if (fos != null) {
                    try { fos.close(); } catch(IOException e) { }
                }
            }
        }
        // Avoid dependent library link errors on w32 (this is handled
        // internal to the jnidispatch library for X11-based platforms)
        if (Platform.isWindows()) {
            // Ensure AWT library ("awt") is loaded by the proper class loader, 
            // otherwise Toolkit class init will fail
            Toolkit.getDefaultToolkit();
            try { System.loadLibrary("jawt"); } 
            catch(UnsatisfiedLinkError e) { e.printStackTrace(); }
        }
        System.load(lib.getAbsolutePath());
        return true;
    }

    /**
     * Initialize field and method IDs for native methods of this class. 
     * Returns the size of a native pointer.
     **/
    private static native int pointerSize();

    /** Return the size of a native <code>long</code>. */
    private static native int longSize();

    /** Return the size of a native <code>wchar_t</code>. */
    private static native int wideCharSize();
}
