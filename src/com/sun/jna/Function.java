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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/**
 * An abstraction for a native function pointer.  An instance of 
 * <code>Function</code> repesents a pointer to some native function.  
 * <code>invokeXXX</code> methods provide means to call the function; select a 
 * <code>XXX</code> variant based on the return type of the Java interface
 * method. 
 *
 * @author Sheng Liang, originator
 * @author Todd Fast, suitability modifications
 * @see Pointer
 */
public class Function extends Pointer {

    /** Standard C calling convention. */
    public static final int C_CONVENTION = 0;
    /** Alternate convention (currently used only for w32 stdcall). */
    public static final int ALT_CONVENTION = 1;
    public static final int MAX_NARGS = 32;
    
    private int callingConvention;
    private String libraryName;
    private String functionName;
    private NativeLibrary library;
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
     * @param	library
     *			Library in which to find the function
     * @param	functionName
     *			Name of the native function to be linked with
     * @param	callingConvention
     *			Calling convention used by the native function
     */
    Function(NativeLibrary library, String functionName, 
                    int callingConvention) {
        checkCallingConvention(callingConvention);
        this.library = library;
        this.libraryName = library.getName();
        this.functionName = functionName;
        this.callingConvention = callingConvention;
        peer = library.getFunctionAddress(functionName);
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

    public String getLibraryName() {
        return libraryName;
    }

    public String getName() {
        return functionName;
    }


    public int getCallingConvention() {
        return callingConvention;
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
    private Object invoke(int callingConvention, Class returnType, Object[] inArgs, Map converters) {
        Object result=null;
        
        // Clone the argument array
        Object[] args = { };
        if (inArgs != null) {
            args = new Object[inArgs.length];
            System.arraycopy(inArgs, 0, args, 0, args.length);
        }
        
        // Keep track of allocated ByteBuffers so they can be released again
        byte[] bufferIndexes = null;
        int bufferCount = 0;
        // String arguments are converted to native pointers here rather
        // than in native code so that the values will be valid until
        // this method returns.  At one point the conversion was in native
        // code, which left the pointer values invalid before this method
        // returned (so you couldn't do something like strstr).
        for (int i=0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                ArgumentConverter converter = (ArgumentConverter)converters.get(arg.getClass());
                if (converter != null) {
                    args[i] = arg = converter.convert(arg);
                }
                //
                // Let the converted argument be further converted to standard types
                //
            }
            if (arg == null
                    || (arg.getClass().isArray()
                    && arg.getClass().getComponentType().isPrimitive())) {
                continue;
            }           
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
                if (bufferIndexes == null) {
                    bufferIndexes = new byte[Function.MAX_NARGS];
                }
                bufferIndexes[bufferCount++] = (byte) i;
            }
            // Convert Callback to Pointer
            else if (arg instanceof Callback) {
                CallbackReference cbref = 
                        CallbackReference.getInstance(callingConvention, (Callback) arg);
                // Use pointer to trampoline (callback->insns, see dispatch.h)
                args[i] = cbref.getTrampoline();
            }
            // Convert String to native pointer (const)
            else if (arg instanceof String) {
                String s = (String) arg;
                ByteBuffer buf = getBufferPool().get(s.length() + 1);
                buf.put(s.getBytes()).put((byte) 0).flip();
                args[i] = buf;
                if (bufferIndexes == null) {
                    bufferIndexes = new byte[Function.MAX_NARGS];
                }
                bufferIndexes[bufferCount++] = (byte) i;
            }
            // Convert WString to native pointer (const)
            else if (arg instanceof WString) {
                args[i] = new NativeString(arg.toString(), true).getPointer();
            }
            else if (arg instanceof NativeLong) {
                args[i] = ((NativeLong) arg).asNativeValue();
            }
            // Convert boolean to int
            // NOTE: this is specifically for BOOL on w32; most other
            // platforms simply use an 'int' or 'char' argument.
            else if (arg instanceof Boolean) {
                args[i] = new Integer(Boolean.TRUE.equals(arg) ? -1 : 0);
            } 
            //
            // Convert Java 1.5+ varargs to C stdargs
            //
            else if (arg.getClass().isArray() && 
                    Object.class.isAssignableFrom(arg.getClass().getComponentType()) &&
                    i == inArgs.length - 1) {
                Object[] varargs = (Object[]) arg;
                Object[] newArgs = new Object[i + varargs.length + 1];
                // Copy the original arg array (less the current arg)
                System.arraycopy(args, 0, newArgs, 0, i);
                
                // Copy the varargs array onto the end of the main args array
                System.arraycopy(varargs, 0, newArgs, i, varargs.length);
                 // Make sure varargs are NULL terminated
                newArgs[newArgs.length - 1] = null;
                args = newArgs;
                --i; // Jump back and process the first arg of the varargs
            } else if (arg.getClass().isArray()) {
                throw new IllegalArgumentException("Unsupported array type: " + arg.getClass());
            }
        }
        
        if (returnType==Void.TYPE || returnType==Void.class) {
            invokeVoid(callingConvention, args);
        } else if (returnType==Boolean.TYPE || returnType==Boolean.class) {
            result = new Boolean(invokeInt(callingConvention, args) != 0);
        } else if (returnType==Byte.TYPE || returnType==Byte.class) {
            result = new Byte((byte)invokeInt(callingConvention, args));
        } else if (returnType==Short.TYPE || returnType==Short.class) {
            result = new Short((short)invokeInt(callingConvention, args));
        } else if (returnType==Integer.TYPE || returnType==Integer.class) {
            result = new Integer(invokeInt(callingConvention, args));
        } else if (returnType==Long.TYPE || returnType==Long.class) {
            result = new Long(invokeLong(callingConvention, args));
        } else if (returnType==Float.TYPE || returnType==Float.class) {
            result = new Float(invokeFloat(callingConvention, args));
        } else if (returnType==Double.TYPE || returnType==Double.class) {
            result = new Double(invokeDouble(callingConvention, args));
        } else if (returnType==NativeLong.class) {
            if (NativeLong.SIZE == 4) {
                result = new NativeLong(invokeInt(callingConvention, args)); 
            } else {
                result = new NativeLong(invokeLong(callingConvention, args)); 
            }
        } else if (returnType==String.class) {
            Pointer ptr = invokePointer(callingConvention, args);
            result = ptr != null ? ptr.getString(0, false) : null;
        } else if (returnType==WString.class) {
            Pointer ptr = invokePointer(callingConvention, args);
            result = ptr != null ? new WString(ptr.getString(0, true)) : null;
        } else if (Pointer.class.isAssignableFrom(returnType)) {
            result = invokePointer(callingConvention, args);
        } else if (Structure.class.isAssignableFrom(returnType)) {
            result = invokePointer(callingConvention, args);
            try {
            Structure s = (Structure)returnType.newInstance();
            s.useMemory((Pointer)result);
            s.read();
            result = s;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported return type "
                    + returnType);
        }
        
        // Sync java fields in structures to native memory after invocation
        if (inArgs != null) {
            for (int i=0; i < inArgs.length; i++) {
                Object arg = inArgs[i];
                if (arg instanceof Structure) {
                    ((Structure)arg).read();
                }
            }
        }
        // Return all the temporary buffers to the Buffer pool
        if (bufferIndexes != null) {
            BufferPool pool = getBufferPool();
            for (int i = 0; i < bufferCount; i++) {
                int idx = bufferIndexes[i];
                ByteBuffer buf = (ByteBuffer)args[idx];
                if (idx < inArgs.length && inArgs[idx] instanceof ByReference) {
                    ((ByReference)inArgs[idx]).readFrom(buf);
                }
                pool.put(buf);
            }
        }
        return result;
    }
    
    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target function
     */
    public int invokeInt(Object[] args) {
        return ((Integer) invoke(Integer.class, args)).intValue();
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
     * @return	The value returned by the target function
     */
    public long invokeLong(Object[] args) {
        return ((Long) invoke(Long.class, args)).longValue();
    }


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
     * @return	The value returned by the target function
     */
    public boolean invokeBoolean(Object[] args) {
        return invokeInt(args) != 0;
    }


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
    public float invokeFloat(Object[] args) {
        return ((Float) invoke(Float.class, args)).floatValue();
    }


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
    public double invokeDouble(Object[] args) {
        return ((Double) invoke(Double.class, args)).doubleValue();
    }


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
        return invokeString(callingConvention, args, wide);
    }

    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The value returned by the target native function
     */
    public String invokeString(int callingConvention, Object[] args, boolean wide) {
        Pointer ptr = (Pointer) invoke(callingConvention, Pointer.class, args);
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
    public Pointer invokePointer(Object[] args) {
        return (Pointer) invoke(Pointer.class, args);
    }


    /**
     * Call the native function being represented by this object
     *
     * @param	args
     *			Arguments to pass to the native function
     * @return	The native pointer returned by the target native function
     */
    public native Pointer invokePointer(int callingConvention, Object[] args);

    /**
     * Find named function in the named library.  Note, this may also be useful
     * to obtain the pointer to a function and pass it back into native code.
     * The library name argument should be the full path to the library file,
     * otherwise the library lookup will use a search algorithm dependent on 
     * the native shared library loading implementation.
     */
    public native long find(String libraryPath, String fname);
    
    public String toString() {
        return functionName + "(" + libraryName + ")";
    }
}
