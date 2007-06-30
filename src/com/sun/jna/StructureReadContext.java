/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
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

import java.lang.reflect.Field;

/** Provide native to Java type conversion context for a {@link Structure} 
 * read. 
 */
public class StructureReadContext extends FromNativeContext {
    
    private Structure structure;
    private Field field;
    public StructureReadContext(Class resultClass, Structure struct, Field field) {
        super(resultClass);
        this.structure = struct;
        this.field = field;
    }
    public Structure getStructure() { return structure; }
    public Field getField() { return field; }
}
