/* Copyright (c) 2007 Wayne Meissner, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */

package com.sun.jna;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple TypeMapper that internally handles checking for types using an ordered list.
 * Types will be searched in the order they are added.
 */
public class SimpleTypeMapper implements TypeMapper {
    private LinkedHashMap types = new LinkedHashMap();
    public SimpleTypeMapper() {
    }
    public void add(Class type, TypeConverter converter) {
        types.put(type, converter);
    }

    public TypeConverter getTypeConverter(Class javaType) {
        Set entries = types.entrySet();
        for (Iterator it = entries.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Class type = (Class)entry.getKey();
            if (type.isAssignableFrom(javaType)) {
                return (TypeConverter) entry.getValue();
            }
        }
        return null;
    }
    
}
