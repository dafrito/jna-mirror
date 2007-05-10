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

/** Simple wrapper class to identify a wide string argument or return type.
 * @author twall@users.sf.net 
 */
public final class WString implements CharSequence, Comparable {
    private String string;
    public WString(String s){
        this.string = s;
    }
    public String toString() {
        return string;
    }
    public boolean equals(Object o) {
        return o instanceof WString && toString().equals(o.toString());
    }
    public int hashCode() {
        return toString().hashCode();
    }
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    public int length() {
        return toString().length();
    }
    public char charAt(int index) {
        return toString().charAt(index);
    }
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }
}
