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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Provide custom mappings to and from native types.  The default lookup
 * checks classes corresponding to converters in the order added; if the
 * class to be converted is an instance of the converter's registered class,
 * the converter will be used.<p>  
 * Derived classes should install additional converters using 
 * {@link #addArgumentConverter}
 * and/or {@link #addResultConverter} in the default constructor.  Classes
 * for primitive types will automatically register for the corresponding
 * Object type and vice versa (i.e. you don't have to register both 
 * <code>int.class</code> and <code>Integer.class</code>).
 * If you want different mapping behavior than the default, simply override
 * {@link #getArgumentConverter} and {@link #getResultConverter}.
 * @see Library#OPTION_TYPE_MAPPER 
 */
public class DefaultTypeMapper implements TypeMapper {
    private static class Entry {
        public Class type;
        public Object converter;
        public Entry(Class type, Object converter) {
            this.type = type;
            this.converter = converter;
        }
    }
    private List argumentConverters = new ArrayList();
    private List resultConverters = new ArrayList();
    private Class getAltClass(Class cls) {
        if (cls == Boolean.class) {
            return boolean.class;
        }
        else if (cls == boolean.class) {
            return Boolean.class;
        }
        else if (cls == Byte.class) {
            return byte.class;
        }
        else if (cls == byte.class) {
            return Byte.class;
        }
        else if (cls == Character.class) {
            return char.class;
        }
        else if (cls == char.class) {
            return Character.class;
        }
        else if (cls == Short.class) {
            return short.class;
        }
        else if (cls == short.class) {
            return Short.class;
        }
        else if (cls == Integer.class) {
            return int.class;
        }
        else if (cls == int.class) {
            return Integer.class;
        }
        else if (cls == Long.class) {
            return long.class;
        }
        else if (cls == long.class) {
            return Long.class;
        }
        else if (cls == Float.class) {
            return float.class;
        }
        else if (cls == float.class) {
            return Float.class;
        }
        else if (cls == Double.class) {
            return double.class;
        }
        else if (cls == double.class) {
            return Double.class;
        }
        return null;
    }
    /** Add a {@link ArgumentConverter} to define the conversion into a native
     * type from arguments of the given Java type.  Converters are
     * checked for in the order added.
     */
    public void addArgumentConverter(Class cls, ArgumentConverter converter) {
        argumentConverters.add(new Entry(cls, converter));
        Class alt = getAltClass(cls);
        if (alt != null) {
            argumentConverters.add(new Entry(alt, converter));
        }
    }
    /** Add a {@link ResultConverter} to convert a native result type into the 
     * given Java type.  Converters are checked for in the order added.
     */
    public void addResultConverter(Class cls, ResultConverter converter) {
        resultConverters.add(new Entry(cls, converter));
        Class alt = getAltClass(cls);
        if (alt != null) {
            resultConverters.add(new Entry(alt, converter));
        }
    }
    /** Add a {@link TpeConverter} to convert a native result type both into the 
     * given Java type and from that java type to the native type.
     * Converters are checked for in the order added.
     */
    public void addTypeConverter(Class cls, TypeConverter converter) {
        addResultConverter(cls, converter);
        addArgumentConverter(cls, converter);
    }
    private Object lookupConverter(Class javaClass, List converters) {
        for (Iterator i=converters.iterator();i.hasNext();) {
            Entry entry = (Entry)i.next();
            Class cls = (Class)entry.type;
            if (cls.isAssignableFrom(javaClass)) {
                return entry.converter;
            }
        }
        return null;
    }
    /* (non-Javadoc)
     * @see com.sun.jna.TypeMapper#getResultConverter(java.lang.Class)
     */
    public ResultConverter getResultConverter(Class javaType) {
        return (ResultConverter)lookupConverter(javaType, resultConverters);
    }
    /* (non-Javadoc)
     * @see com.sun.jna.TypeMapper#getArgumentConverter(java.lang.Class)
     */
    public ArgumentConverter getArgumentConverter(Class javaType) {
        return (ArgumentConverter)lookupConverter(javaType, argumentConverters);
    }
}
