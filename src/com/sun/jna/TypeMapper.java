/*
 * TypeMapper.java
 * 
 * Created on 31/05/2007, 16:49:29
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.jna;

/**
 *
 * @author wayne
 */
public interface TypeMapper {
    public TypeConverter getTypeConverter(Class javaType);
}
