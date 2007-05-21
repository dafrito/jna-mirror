
package com.sun.jna;

/**
 *
 * @author wayne
 */
public interface CallbackProxy extends Callback {
    /*
     * This is a special subclass of Callback, that expects its callback() method
     * to take an array of objects, instead of individual arguments.
     */
     public Object callback(Object[] args);
     public Class[] getParameterTypes();
     public Class getReturnType();
}
