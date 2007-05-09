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
package com.sun.jna.examples.win32;

import java.util.HashMap;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.examples.win32.GDI32.RECT;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/** Provides access to the w32 user32 library.
 * Minimal implementation to support demos.
 *  
 * @author  Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public interface User32 extends StdCallLibrary {

    User32 INSTANCE = (User32)
        Native.loadLibrary("user32", User32.class, new HashMap() {
            {
                // example of function name remapping, in case you 
                // are annoyed by MS naming conventions
                put("loadIcon", "LoadIconA");
                put("loadImage", "LoadImageA");
                put("destroyIcon", "DestroyIcon");
            }
        });
    
    Pointer GetDC(Pointer hWnd);
    int ReleaseDC(Pointer hWnd, Pointer hDC);

    int FLASHW_STOP = 0;
    int FLASHW_CAPTION = 1;
    int FLASHW_TRAY = 2;
    int FLASHW_ALL = (FLASHW_CAPTION|FLASHW_TRAY);
    int FLASHW_TIMER = 4;
    int FLASHW_TIMERNOFG = 12;
    
    public static class FLASHWINFO extends Structure {
        public int cbSize;
        public Pointer hWnd;
        public int dwFlags;
        public int uCount;
        public int dwTimeout;
    }

    int IMAGE_BITMAP=0;
    int IMAGE_ICON=1;
    int IMAGE_CURSOR=2;
    int IMAGE_ENHMETAFILE=3;

    int LR_DEFAULTCOLOR     =0x0000;
    int LR_MONOCHROME       =0x0001;
    int LR_COLOR            =0x0002;
    int LR_COPYRETURNORG    =0x0004;
    int LR_COPYDELETEORG    =0x0008;
    int LR_LOADFROMFILE     =0x0010;
    int LR_LOADTRANSPARENT  =0x0020;
    int LR_DEFAULTSIZE      =0x0040;
    int LR_VGACOLOR         =0x0080;
    int LR_LOADMAP3DCOLORS  =0x1000;
    int LR_CREATEDIBSECTION =0x2000;
    int LR_COPYFROMRESOURCE =0x4000;
    int LR_SHARED           =0x8000;

    Pointer FindWindowA(String winClass, String title);
    int GetClassNameA(Pointer hWnd, byte[] lpClassName, int nMaxCount);
    public static class GUITHREADINFO extends Structure {
        public int cbSize = size();
        public int flags;
        Pointer hwndActive;
        Pointer hwndFocus;
        Pointer hwndCapture;
        Pointer hwndMenuOwner;
        Pointer hwndMoveSize;
        Pointer hwndCaret;
        RECT rcCaret;
    }
    boolean GetGUIThreadInfo(int idThread, GUITHREADINFO lpgui);
    
    public static class WINDOWINFO extends Structure {
        public int cbSize = size();
        public RECT rcWindow;
        public RECT rcClient;
        public int dwStyle;
        public int dwExStyle;
        public int dwWindowStatus;
        public int cxWindowBorders;
        public int cyWindowBorders;
        public short atomWindowType;
        public short wCreatorVersion;
    }
    boolean GetWindowInfo(Pointer hWnd, WINDOWINFO pwi);
    boolean GetWindowRect(Pointer hWnd, RECT rect);
    int GetWindowTextA(Pointer hWnd, byte[] lpString, int nMaxCount);
    int GetWindowTextLength(Pointer hWnd);
    int GetWindowModuleFileNameA(Pointer hWnd, byte[] lpszFileName, int cchFileNameMax);
    int GetWindowThreadProcessId(Pointer hWnd, IntByReference lpdwProcessId);
    interface WNDENUMPROC extends StdCallCallback {
        /** Return whether to continue enumeration. */
        boolean callback(Pointer hWnd, Pointer data);
    }
    boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer data);
    boolean EnumThreadWindows(int dwThreadId, WNDENUMPROC lpEnumFunc, Pointer data);

    boolean FlashWindowEx(FLASHWINFO info);

    int loadIcon(int hInstance, String iconName);

    int loadImage(int hinst,   // handle to instance 
                  String name,  // image to load 
                  int type,        // image type 
                  int xDesired,     // desired width 
                  int yDesired,     // desired height 
                  int load        // load options 
                  );

    boolean destroyIcon(int hicon);

    int GWL_EXSTYLE = -20;
    int GWL_STYLE = -16;
    int GWL_WNDPROC = -4;
    int GWL_HINSTANCE = -6;
    int GWL_ID = -12;
    int GWL_USERDATA = -21;
    int DWL_DLGPROC = 4;
    int DWL_MSGRESULT = 0;
    int DWL_USER = 8;
    int WS_EX_COMPOSITED = 0x20000000;
    int WS_EX_LAYERED = 0x80000;
    int WS_EX_TRANSPARENT = 32;
    int GetWindowLongA(Pointer hWnd, int nIndex);
    int SetWindowLongA(Pointer hWnd, int nIndex, int dwNewLong);

    int LWA_COLORKEY = 1;
    int LWA_ALPHA = 2;
    int ULW_COLORKEY = 1;
    int ULW_ALPHA = 2;
    int ULW_OPAQUE = 4;
    boolean SetLayeredWindowAttributes(Pointer hwnd, int crKey, 
                                       byte bAlpha, int dwFlags);
    boolean GetLayeredWindowAttributes(Pointer hwnd,
                                       IntByReference pcrKey,
                                       ByteByReference pbAlpha,
                                       IntByReference pdwFlags);

    /** Defines the x- and y-coordinates of a point. */
    public static class POINT extends Structure {
        public int x, y;
    }
    /** Specifies the width and height of a rectangle. */
    public static class SIZE extends Structure {
        public int cx, cy;
    }
    int AC_SRC_OVER = 0x00;
    int AC_SRC_ALPHA = 0x01;
    int AC_SRC_NO_PREMULT_ALPHA = 0x01;
    int AC_SRC_NO_ALPHA = 0x02;
    public static class BLENDFUNCTION extends Structure {
        public byte BlendOp = AC_SRC_OVER; // only valid value
        public byte BlendFlags = 0; // only valid value
        public byte SourceConstantAlpha;
        public byte AlphaFormat;
    }
    boolean UpdateLayeredWindow(Pointer hwnd, Pointer hdcDst, 
                                POINT pptDst, SIZE psize, 
                                Pointer hdcSrc, POINT pptSrc, int crKey, 
                                BLENDFUNCTION pblend, int dwFlags);
    int SetWindowRgn(Pointer hWnd, Pointer hRgn, boolean bRedraw);
    int VK_SHIFT = 16;
    int VK_LSHIFT = 0xA0;
    int VK_RSHIFT = 0xA1;
    int VK_CONTROL = 17;
    int VK_LCONTROL = 0xA2;
    int VK_RCONTROL = 0xA3;
    int VK_MENU = 18;
    int VK_LMENU = 0xA4;
    int VK_RMENU = 0xA5;
    boolean GetKeyboardState(byte[] state);
    short GetAsyncKeyState(int vKey);
}
