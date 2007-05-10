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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sheng Liang, originator (Function.java)
 * @author Todd Fast, suitability modifications
 * @author Wayne Meissner, split library loading from Funcion.java
 */
public class NativeLibrary {
    /* Instance variables */
    private long handle;
    private String libraryName;
    private String libraryPath;
    
    /* Static variables */
    private static final Map libraries = new HashMap();
    private static final Map paths = new HashMap();
    private static final Map functions = new HashMap();
    private static final Map libraryNameMap = new HashMap();
    private static String[] sys_paths;
    private static String[] usr_paths;
    private static String[] jna_paths;
    private static final Pointer NULL = Pointer.NULL; // Dummy to get it to load the jnidispatch library
    
    private NativeLibrary(String libraryName) {
        this.libraryName = libraryName;
        libraryPath = getAbsoluteLibraryPath(libraryName);
        handle = open(libraryPath);
        if (handle == 0) {
            throw new UnsatisfiedLinkError("Cannot locate library " + libraryName);
        }
    }
    public static final NativeLibrary getInstance(String libraryName) {
        synchronized (libraries) {
            Object val = libraries.get(libraryName);
            if (val != null) {
                return (NativeLibrary) val;
            }
            
            NativeLibrary lib = new NativeLibrary(libraryName);
            libraries.put(libraryName, lib);
            return lib;
        }
    }
    
    /**
     * Create a new {@link Function} that is linked with a native
     * function that follows the standard "C" calling convention.
     *
     * <p>The allocated instance represents a pointer to the named native
     * function from the library, called with the standard "C" calling
     * convention.
     *
     * @param	functionName
     *			Name of the native function to be linked with
     */
    public Function getFunction(String functionName) {
        return getFunction(functionName, Function.C_CONVENTION);
    }
    
    /**
     * Create a new  @{link Function} that is linked with a native
     * function that follows a given calling convention.
     *
     * <p>The allocated instance represents a pointer to the named native
     * function from the library, called with the named calling convention.
     *
     * @param	functionName
     *			Name of the native function to be linked with
     * @param	callingConvention
     *			Calling convention used by the native function
     */
    public Function getFunction(String functionName, int callingConvention) {
        synchronized (functions) {
            Function function = (Function) functions.get(functionName);
            if (function == null) {
                function = new Function(this, functionName, callingConvention);
                functions.put(functionName, function);
            }
            return function;
        }
    }
    
    /**
     * Used by the Function class to locate a symbol
     */
    long getFunctionAddress(String functionName) {
        long func = findSymbol(handle, functionName);
        if (func == 0) {
            throw new UnsatisfiedLinkError("Cannot locate function '" + functionName + "'");
        }
        return func;
    }
    public String toString() {
        return "Native Library <" + libraryPath + "@" + handle + ">";
    }
    public String getName() {
        return libraryName;
    }
    // Close the library when it is no longer referenced
    protected void finalize() throws Throwable {
        try {
            close(handle);
        } finally {
            super.finalize();
        }
    }
    
    private synchronized static String getAbsoluteLibraryPath(String libName) {
        String path = (String)paths.get(libName);
        if (path == null) {
            path = findLibrary(libName);
            paths.put(libName, path);
        }
        return path;
    }
    
    private static String[] initPaths(String key) {
        return System.getProperty(key, "").split(File.pathSeparator);
    }
    
    /** Use standard library search paths to find the library. */
    private static String findLibrary(String libName) {
        if (libraryNameMap.containsKey(libName)) {
            return (String) libraryNameMap.get(libName);
        }
        String name = mapLibraryName(libName);
        String path = findPath(sys_paths, name);
        if (path != null)
            return path;
        if ((path = findPath(usr_paths, name)) != null)
            return path;
        if ((path = findPath(jna_paths, name)) != null)
            return path;
        
        return libName;
    }
    private static String mapLibraryName(String libName) {
        //
        // On MacOSX, System.mapLibraryName() returns the .jnilib extension for all libs
        // but native libs that JNA needs to load are .dylib
        //
        if (System.getProperty("os.name").startsWith("Mac")) {
            String name = System.mapLibraryName(libName);
            if (name.endsWith(".jnilib")) {
                return name.substring(0, name.lastIndexOf(".jnilib")) + ".dylib";
            }
            return name;
        }
        return System.mapLibraryName(libName);
    }
    private static String findPath(String[] paths, String name) {
        for (int i=0;i < paths.length;i++) {
            File file = new File(paths[i], name);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
    private static native long open(String name);
    private static native void close(long handle);
    private static native long findSymbol(long handle, String name);
    static {
        if (System.getProperty("os.name").startsWith("Linux")) {
            // Some versions of linux don't have libc.so
            libraryNameMap.put("c", "libc.so.6");
        }
        sys_paths = initPaths("sun.boot.library.path");
        usr_paths = initPaths("java.library.path");
        jna_paths = initPaths("jna.library.path");
    }
}
