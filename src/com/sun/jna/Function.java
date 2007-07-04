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

import com.sun.jna.ptr.ByReference;
import com.sun.jna.types.NativeValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An abstraction for a native function pointer.  An instance of 
 * <code>Function</code> repesents a pointer to some native function.  
 * {@link #invoke(Class,Object[],Map)} is the primary means to call
 * the function. 
 *
 * @author Sheng Liang, originator
 * @author Todd Fast, suitability modifications
 * @see Pointer
 */
public class Function extends Pointer {
    /** Maximum number of arguments supported by a JNA function call. */
    public static final int MAX_NARGS = 32;

    /** Standard C calling convention. */
    public static final int C_CONVENTION = 0;
    /** First alternate convention (currently used only for w32 stdcall). */
    public static final int ALT_CONVENTION = 1;

    private NativeLibrary library;
    private String functionName;
    private int callingConvention;
    private Method method = null;
    
    private static final BufferPool globalBufferPool = new MultiBufferPool(512, 128, true);
   
    /**
     * Create a new {@link Function} that is linked with a native 
     * function that follows the standard "C" calling convention.
     * 
     * <p>The allocated instance represents a pointer to the named native 
     * function from the named library, called with the standard "C" calling
     * convention.
     *
     * @param	libraryName
     *			Library in which to find the native function
     * @param	functionName
     *			Name of the native function to be linked with
     */
    public Function(String libraryName, String functionName) {
        this(libraryName, functionName, C_CONVENTION);
    }


    /**
     * Create a new @{link Function} that is linked with a native 
     * function that follows a given calling convention.
     * 
     * <p>The allocated instance represents a pointer to the named native 
     * function from the named library, called with the named calling 
     * convention.
     *
     * @param	libraryName
     *			Library in which to find the function
     * @param	functionName
     *			Name of the native function to be linked with
     * @param	callingConvention
     *			Calling convention used by the native function
     */
    public Function(String libraryName, String functionName, 
                    int callingConvention) {
        this(NativeLibrary.getInstance(libraryName), functionName, callingConvention);
    }
    
    /**
     * Create a new @{link Function} that is linked with a native 
     * function that follows a given calling convention.
     * 
     * <p>The allocated instance represents a pointer to the named native 
     * function from the supplied library, called with the named calling 
     * convention.
     *
     * @param  library
     *                 {@link NativeLibrary} in which to find the function
     * @param  functionName
     *                 Name of the native function to be linked with
     * @param  callingConvention
     *                 Calling convention used by the native function
     */
    Function(NativeLibrary library, String functionName, int callingConvention) {
        checkCallingConvention(callingConvention);
        this.library = library;
        this.functionName = functionName;
        this.callingConvention = callingConvention;
        this.peer = library.getFunctionAddress(functionName);
    }
    
    /**
     * Create a new @{link Function} that is linked with a native 
     * function that follows a given calling convention.
     * 
     * <p>The allocated instance represents a pointer to the named native 
     * function from the supplied library, called with the named calling 
     * convention.
     *
     * @param  library
     *                 {@link NativeLibrary} in which to find the function
     * @param  method
     *                 @{link Method} in the Library that this method represents
     * @param  callingConvention
     *                 Calling convention used by the native function
     */
    Function(NativeLibrary library, Method method, int callingConvention) {
        this(library, method.getName(), callingConvention);
        this.method = method;
    }
    
    
    private void checkCallingConvention(int convention)
        throws IllegalArgumentException {
        switch(convention) {
        case C_CONVENTION:
        case ALT_CONVENTION:
            break;
        default:
            throw new IllegalArgumentException("Unrecognized calling convention: " 
                                               + convention);
        }
    }

    public NativeLibrary getLibrary() {
        return library;
    }
    
    public String getLibraryName() {
        return library.getName();
    }

    public String getName() {
        return functionName;
    }


    public int getCallingConvention() {
        return callingConvention;
    }
    
    /**
     * Get the Java @{link Method} that is the proxy for this @{link Function}
     * 
     * @return The Java Method that invoked this Function, or null if not invoked 
     *         from a @{link Library} subclass.
     */
    public Method getMethod() {
        return method;
    }
    
    /*
     * The reason for using pools of ByteBuffer objects instead of allocating 
     * on demand is: speed.
     * 
     * Direct ByteBuffers are _really_ slow to allocate - about 3 times slower 
     * than a Memory object - but once allocated, are quite a bit faster to 
     * read/write, as they can be accessed directly from java vs going through 
     * JNI.
     * 
     * Having both a thread local and a global synchronized pool was to balance 
     * between speed and memory usage - getting a buffer from the thread local 
     * pool is at least twice as fast as doing the synchronize needed for the 
     * global pool.
     * 
     * So a 1K per-thread pool is allocated which should handle 99% of cases 
     * (most functions have less than 8 args, and most strings are less than 128 bytes), 
     * but can fallback to a still fairly fast pool of larger/more buffers if needed.
     */ 
    private static ThreadLocal localBufferPool = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new SimpleBufferPool(globalBufferPool, 128, 8);
        }
    };
    
    private static BufferPool getBufferPool() {
        return (BufferPool) localBufferPool.get();
    }
    public Object invoke(Class returnType, Object[] inArgs) {
        return invoke(callingConvention, returnType, inArgs, Collections.EMPTY_MAP);
    }
    public Object invoke(int callingConvention, Class returnType, Object[] inArgs) {
        return invoke(callingConvention, returnType, inArgs, Collections.EMPTY_MAP);
    }
    public Object invoke(Class returnType, Object[] inArgs, Map converters) {
        return invoke(callingConvention, returnType, inArgs, converters);
    }
    private Object invoke(int callingConvention, Class returnType, Object[] inArgs,Map options) {
        
        // This will be set to the full set of arguments if a varargs
        // argument is encountered
        Object result = null;

        // If the final argument is an array of Object, treat it as
        // varargs and concatenate the previous arguments with the varargs 
        // elements.
        if (inArgs != null && inArgs.length > 0) {
            Object lastArg = inArgs[inArgs.length-1];
            if (lastArg != null
                && lastArg.getClass().isArray() 
                && !isPrimitiveArray(lastArg.getClass())
                && !isStructureArray(lastArg.getClass())
                && String[].class != lastArg.getClass()) {
                Object[] varArgs = (Object[])lastArg;
                Object[] fullArgs = new Object[inArgs.length+varArgs.length];
                System.arraycopy(inArgs, 0, fullArgs, 0, inArgs.length-1);
                System.arraycopy(varArgs, 0, fullArgs, inArgs.length-1, varArgs.length);
                // For convenience, always append a NULL argument to the end
                // of varargs, whether the called API requires it or not. If
                // it is not needed, it will be ignored, but if it *is* 
                // required, it avoids forcing the Java client to always
                // explicitly add it.
                fullArgs[fullArgs.length-1] = null;
                inArgs = fullArgs;
            }
        }

        // Clone the argument array
        Object[] args = { };
        if (inArgs != null) {
            if (inArgs.length > MAX_NARGS) {
                throw new UnsupportedOperationException("Maximum argument count is " + MAX_NARGS);
            }
            args = new Object[inArgs.length];
            System.arraycopy(inArgs, 0, args, 0, args.length);
        }
        
        TypeMapper mapper = (TypeMapper)options.get(Library.OPTION_TYPE_MAPPER);

        // Keep track of allocated ByteBuffers so they can be released again
        int bufferMask = 0;

        // String arguments are converted to native pointers here rather
        // than in native code so that the values will be valid until
        // this method returns.  At one point the conversion was in native
        // code, which left the pointer values invalid before this method
        // returned (so you couldn't do something like strstr).
        for (int i=0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null && mapper != null) {
                ToNativeConverter converter = mapper.getToNativeConverter(arg.getClass());
                if (converter != null) {
                    args[i] = arg = converter.toNative(arg);
                }

                //
                // Let the converted argument be further converted to standard types
                //
            }
            if (arg instanceof NativeValue) {
                args[i] = arg = ((NativeValue)arg).toNativeValue();
            }
            if (arg == null || isPrimitiveArray(arg.getClass())) {
                continue;
            }
            Class argClass = arg.getClass();
            // Convert Structures to native pointers 
            if (arg instanceof Structure) {
                Structure struct = (Structure)arg;
                struct.write();
                args[i] = struct.getPointer();
            }
            // Convert reference class to pointer
            else if (arg instanceof ByReference) {
                ByteBuffer buf = getBufferPool().get(8);
                ((ByReference)arg).writeTo(buf);
                buf.flip();
                args[i] = buf;
                bufferMask |= (1 << i);
            }
            // Convert Callback to Pointer
            else if (arg instanceof Callback) {
                CallbackReference cbref = 
                        CallbackReference.getInstance((Callback) arg, callingConvention);
                // Use pointer to trampoline (callback->insns, see dispatch.h)
                args[i] = cbref.getTrampoline();
            }
            // String arguments are converted to native pointers here rather
            // than in native code so that the values will be valid until
            // this method returns.  At one point the conversion was in native
            // code, which left the pointer values invalid before this method
            // returned (so you couldn't do something like strstr).
            // Convert String to native pointer (const)
            else if (arg instanceof String) {
                String s = (String) arg;
                ByteBuffer buf = getBufferPool().get(s.length() + 1);
                buf.put(s.getBytes()).put((byte) 0).flip();
                args[i] = buf;
                bufferMask |= (1 << i);
            }
            // Convert WString to native pointer (const)
            else if (arg instanceof WString) {
                args[i] = new NativeString(arg.toString(), true).getPointer();
            }
            // Default conversion of boolean to int; if you want something
            // different, use an ArgumentConverter
            else if (arg instanceof Boolean) {
                args[i] = new Integer(Boolean.TRUE.equals(arg) ? -1 : 0);
            }
            else if (String[].class.equals(argClass)) {
                args[i] = new StringArray((String[])arg);
            }
            else if (isStructureArray(argClass)) {
                // Initialize uninitialized arrays of Structure to point
                // to a single block of memory
                Structure[] ss = (Structure[])arg;
                if (ss.length == 0) {
                    args[i] = null;
                }
                else if (ss[0] == null) {
                    Class type = argClass.getComponentType();
                    try {
                        Structure struct = (Structure)type.newInstance(); 
                        int size = struct.size();
                        Memory m = new Memory(size * ss.length);
                        struct.useMemory(m);
                        Structure[] tmp = struct.toArray(ss.length);
                        for (int si=0;si < ss.length;si++) {
                            ss[si] = tmp[si];
                        }
                    }
                    catch(InstantiationException e) {
                        throw new IllegalArgumentException("Instantiation of "
                                                           + type + " failed: " 
                                                           + e);
                    }
                    catch(IllegalAccessException e) {
                        throw new IllegalArgumentException("Not allowed to instantiate "
                                                           + type + ": " + e);
                    }
                    args[i] = ss[0].getPointer();
                }
                else {
                    Pointer base = ss[0].getPointer();
                    int size = ss[0].size();
                    for (int si=1;si < ss.length;si++) {
                        try {
                            Pointer p = base.share(size*si, size);
                            if (ss[si].getPointer().peer != p.peer) {
                                throw new RuntimeException();
                            }
                        }
                        catch(RuntimeException e) {
                            String msg = "Structure array elements must use"
                                + " contiguous memory: " + si;     
                            throw new IllegalArgumentException(msg);
                        }
                    }
                    args[i] = base;
                }
            }
            else if (arg instanceof ByteBuffer && !((ByteBuffer)arg).isDirect()) {
                ByteBuffer buf = (ByteBuffer)arg;
                if (buf.hasArray()) {
                    args[i] = buf.array();
                }
                else {
                    throw new IllegalArgumentException("Unsupported non-direct ByteBuffer with no array");
                }
            }
            else if (argClass.isArray()){
                throw new IllegalArgumentException("Unsupported array argument type: " 
                                                   + argClass.getComponentType());
            }
        }
        
        Class nativeType = returnType;
        FromNativeConverter resultConverter = null;
        if (mapper != null) {
            resultConverter = mapper.getFromNativeConverter(returnType);
            if (resultConverter != null) {
                nativeType = resultConverter.nativeType();
            }
        }
        Constructor resultConstructor = null;
        if (nativeType == returnType && NativeValue.class.isAssignableFrom(returnType)) {
            Class javaType = returnType;
            while (javaType != null && resultConstructor == null) {
                try {
                    Method typeMethod = javaType.getDeclaredMethod("nativeType", new Class[]{});
                    nativeType = (Class) typeMethod.invoke(null, null);
                    resultConstructor = returnType.getDeclaredConstructor(new Class[]{nativeType});                    
                } catch (NoSuchMethodException e) {                    
                    javaType = javaType.getSuperclass();
                    if (javaType == null) {
                        throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
                }
            }
        }
        if (nativeType == null || nativeType==void.class || nativeType==Void.class) {
            invokeVoid(callingConvention, args);
        }
        else if (nativeType==boolean.class || nativeType==Boolean.class) {
            result = Boolean.valueOf(invokeInt(callingConvention, args) != 0);
        }
        else if (nativeType==byte.class || nativeType==Byte.class) {
            result = new Byte((byte)invokeInt(callingConvention, args));
        }
        else if (nativeType==short.class || nativeType==Short.class) {
            result = new Short((short)invokeInt(callingConvention, args));
        }
        else if (nativeType==int.class || nativeType==Integer.class) {
            result = new Integer(invokeInt(callingConvention, args));
        }
        else if (nativeType==long.class || nativeType==Long.class) {
            result = new Long(invokeLong(callingConvention, args));
        }
        else if (nativeType==float.class || nativeType==Float.class) {
            result = new Float(invokeFloat(callingConvention, args));
        }
        else if (nativeType==double.class || nativeType==Double.class) {
            result = new Double(invokeDouble(callingConvention, args));
        }
        else if (nativeType==String.class) {
            result = invokeString(args, false);
        }
        else if (nativeType==WString.class) {
            result = new WString(invokeString(args, true));
        }
        else if (Pointer.class.isAssignableFrom(nativeType)) {
            result = invokePointer(callingConvention, args);
        }
        else if (Structure.class.isAssignableFrom(nativeType)) {
            result = invokePointer(callingConvention, args);
            if (result != null) {
                try {
                    Structure s = (Structure)nativeType.newInstance();
                    s.useMemory((Pointer)result);
                    s.read();
                    result = s;
                }
                catch(InstantiationException e) {
                    throw new IllegalArgumentException("Instantiation of "
                                                       + nativeType + " failed: " 
                                                       + e);
                }
                catch(IllegalAccessException e) {
                    throw new IllegalArgumentException("Not allowed to instantiate "
                                                       + nativeType + ": " + e);
                }
            }
        }
        else {
            throw new IllegalArgumentException("Unsupported return type "
                                               + returnType);
        }

        // Convert the result to a custom value/type if appropriate
        if (resultConverter != null) {
            FromNativeContext context = new FunctionResultContext(returnType, this, inArgs);
            result = resultConverter.fromNative(result, context);
        } else if (resultConstructor != null) {
            try {
                result = resultConstructor.newInstance(new Object[] { result });
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
            } catch (IllegalAccessException e) {    
                throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
            } catch (InvocationTargetException e) {                
                throw new IllegalArgumentException("Invalid return type: " + returnType.getName(), e);
            }
            
        }
        
        // Sync java fields in structures to native memory after invocation
        if (inArgs != null) {
            for (int i=0; i < inArgs.length; i++) {
                Object arg = inArgs[i];
                if (arg == null)
                    continue;
                if (arg instanceof Structure) {
                    ((Structure)arg).read();
                }
                else if (String[].class == arg.getClass()) {
                    // Copy back the string values, just in case they were
                    // modified
                    StringArray buf = (StringArray)args[i];
                    String[] array = (String[])arg;
                    for (int si=0;si < array.length;si++) {
                        array[si] = buf.getPointer(si * Pointer.SIZE).getString(0, false);
                    }
                }
                else if (isStructureArray(arg.getClass())) {
                    Structure[] ss = (Structure[])arg;
                    for (int si=0;si < ss.length;si++) {
                        ss[si].read();
                    }
                }				
            }
        }
        // Return all the temporary buffers to the Buffer pool
        if (bufferMask != 0) {
            BufferPool pool = getBufferPool();
            for (int i = 0; i < MAX_NARGS && bufferMask != 0; i++) {
                if ((bufferMask & 1) != 0) {
                    ByteBuffer buf = (ByteBuffer)args[i];
                    if (inArgs[i] instanceof ByReference) {
                        ((ByReference)inArgs[i]).readFrom(buf);
                    }
                    pool.put(buf);
                }
                bufferMask >>= 1;
            }
        }
                        
        return result;
    }

    private boolean isStructureArray(Class argClass) {
        return argClass.isArray()
            && Structure.class.isAssignableFrom(argClass.getComponentType());
    }


    private boolean isPrimitiveArray(Class argClass) {
        return argClass.isArray() 
            && argClass.getComponentType().isPrimitive();
    }
    
    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public native int invokeInt(int callingConvention, Object[] args);

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public native long invokeLong(int callingConvention, Object[] args);

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     */
    public void invoke(Object[] args) {
        invoke(Void.class, args);
    }


    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     */
    public native void invokeVoid(int callingConvention, Object[] args);

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public native float invokeFloat(int callingConvention, Object[] args);

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public native double invokeDouble(int callingConvention, Object[] args);

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public String invokeString(Object[] args, boolean wide) {
        Pointer ptr = invokePointer(callingConvention, args);
        String s = null;
        if (ptr != null) {
            s = ptr.getString(0, wide);
        }
        return s;
    }

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The native pointer returned by the target native function
     */
    public native Pointer invokePointer(int callingConvention, Object[] args);

    /** Create a callback function pointer. */
    static native Pointer createCallback(Callback callback, Method method, 
                                         Class[] parameterTypes, 
                                         Class returnType,
                                         int callingConvention);
    /** Free the given callback function pointer. */
    static native void freeCallback(long ptr);
    
    /** Provide a human-readable representation of this object. */
    public String toString() {
        return "native function " + functionName + "(" + library.getName() 
            + ")@0x" + Long.toHexString(peer);
    }
    
    /** Handle native array of char* type by managing allocation/disposal of 
     * native strings within an array of pointers.  Always NULL-terminates
     * the array. 
     */
    private class StringArray extends Memory {
        private List natives = new ArrayList();
        public StringArray(String[] strings) {
            super((strings.length + 1) * Pointer.SIZE);
            for (int i=0;i < strings.length;i++) {
                NativeString ns = new NativeString(strings[i]);
                natives.add(ns);
                setPointer(Pointer.SIZE * i, ns.getPointer());
            }
            setPointer(Pointer.SIZE * strings.length, null);
        }
    }
    
    // The following convenience methods are provided for using a Function
    // instance directly
    
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Pointer.class, args)}.
     */
    public Pointer invokePointer(Object[] args) {
        return (Pointer)invoke(Pointer.class, args);
    }
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Integer.class, args)}.
     */
    public int invokeInt(Object[] args) {
        return ((Integer)invoke(Integer.class, args)).intValue();
    }
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Long.class, args)}.
     */
    public long invokeLong(Object[] args) {
        return ((Long)invoke(Long.class, args)).longValue();
    }
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Float.class, args)}.
     */
    public float invokeFloat(Object[] args) {
        return ((Float)invoke(Float.class, args)).floatValue();
    }
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Double.class, args)}.
     */
    public double invokeDouble(Object[] args) {
        return ((Double)invoke(Double.class, args)).doubleValue();
    }
    /** Convenience method for 
     * {@link #invoke(Class,Object[]) invoke(Void.class, args)}.
     */
    public void invokeVoid(Object[] args) {
        invoke(Void.class, args);
    }
}
