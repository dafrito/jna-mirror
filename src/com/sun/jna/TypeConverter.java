/*
 * TypeConverter.java
 * 
 * Created on 31/05/2007, 16:50:04
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.jna;

/**
 *
 * @author wayne
 */
public interface TypeConverter {
    public Object toNative(Object value);
    public Object fromNative(Object value, Class returnType);
    public Class invocationType();
}
