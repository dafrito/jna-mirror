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

/**
 *
 * @author wmeissner@gmail.com
 */
final class OS {
    static final int MAC = 0;
    static final int LINUX = 1;
    static final int WINDOWS = 2;
    static final int osType;
    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            osType = LINUX;
        } else if (osName.startsWith("Mac")) {
            osType = MAC;
        } else if (osName.startsWith("Windows")) {
            osType = WINDOWS;
        } else {
            osType = ~0;
        }
    }
    private OS() {
    }
    public final static boolean isMac() {
        return osType == MAC;
    }
    public final static boolean isLinux() {
        return osType == LINUX;
    }
    public final static boolean isWindows() {
        return osType == WINDOWS;
    }
}
