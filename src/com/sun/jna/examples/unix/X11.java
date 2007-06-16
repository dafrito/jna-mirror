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
package com.sun.jna.examples.unix;

import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ResultContext;
import com.sun.jna.Structure;
import com.sun.jna.TypeConverter;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.HashMap;

/** Definition (incomplete) of the X library. */
public interface X11 extends Library {
    public static class XID {
        public XID(Integer id) { this.id = id; }
        public XID(int id) { this.id = new Integer(id); }
        public Integer getId() { return id; }
        Integer id;
    }
    public static class Drawable extends XID {
        public Drawable() { super(None); }
        public Drawable(Integer id) { super(id); }
        public Drawable(int id) { super(id); }
    }
    public static class Window extends Drawable {
        public Window() { this(None); }
        public Window(Integer id) { super(id); }
        public Window(int id) { super(id); }
    }
    public static class Pixmap extends Drawable {
        public Pixmap() { this(None); }
        public Pixmap(Integer id) { super(id); }
        public Pixmap(int id) { super(id); }
    }
    public static class PointerType {
        public PointerType(Pointer ptr) { this.ptr = ptr; }
        Pointer ptr;
    }
    public static class Display extends PointerType {
        public Display(Pointer display) { super(display); }
    }
    public static class Visual extends PointerType {
        public Visual(Pointer visual) { super(visual); }
    }
    public static class GC extends PointerType {
        public GC(Pointer gc) { super(gc); }
    }
    public static class XIDTypeConverter implements TypeConverter {

        public Object toNative(Object value) {
            return ((XID)value).id;
        }

        public Object fromNative(Object value, ResultContext context) {
            return new XID((Integer)value);
        }

        public Class nativeType() {
            return Integer.class;
        }   
    }
    public class WindowTypeConverter extends XIDTypeConverter {
        public Object fromNative(Object value, Class returnType) {
            return new Window((Integer)value);
        }
    }
    public class PixmapTypeConverter extends XIDTypeConverter {
        public Object fromNative(Object value, Class returnType) {
            return new Pixmap((Integer)value);
        }
    }
    public abstract static class PointerTypeConverter implements TypeConverter {

        public Object toNative(Object value) {
            return ((PointerType)value).ptr;
        }

        public Class nativeType() {
            return Pointer.class;
        }   
    }
    
    public static class DisplayTypeConverter extends PointerTypeConverter {
        public Object fromNative(Object value, ResultContext context) {
            return value != null ? new Display((Pointer)value) : null;
        }
    }
    public static class VisualTypeConverter extends PointerTypeConverter {
        public Object fromNative(Object value, ResultContext context) {
            return value != null ? new Visual((Pointer)value) : null;
        }
    }
    public static class GCTypeConverter extends PointerTypeConverter {
        public Object fromNative(Object value, ResultContext context) {
            return value != null ? new GC((Pointer)value) : null;
        }
    }
    public static class OptionMap extends HashMap {
        public OptionMap() {
            DefaultTypeMapper mapper = new DefaultTypeMapper();
            mapper.addTypeConverter(Window.class, new WindowTypeConverter());
            mapper.addTypeConverter(Pixmap.class, new PixmapTypeConverter());
            mapper.addTypeConverter(Display.class, new DisplayTypeConverter());
            mapper.addTypeConverter(Visual.class, new VisualTypeConverter());
            mapper.addTypeConverter(GC.class, new GCTypeConverter());
            put(Library.OPTION_TYPE_MAPPER, mapper);
        }
    }
    /** Definition (incomplete) of the Xext library. */
    public interface Xext extends Library {
        Xext INSTANCE = (Xext)Native.loadLibrary("Xext", Xext.class, new OptionMap());

        void XShapeCombineMask(Display display, Window window, int dest_kind,
                               int x_off, int y_off, XID src, int op);
    }
    
    /** Definition (incomplete) of the Xrender library. */
    public interface Xrender extends Library {
        Xrender INSTANCE = (Xrender)Native.loadLibrary("Xrender", Xrender.class, new OptionMap());
        public static class XRenderDirectFormat extends Structure {
            public short green, greenMask;
            public short blue, blueMask;
            public short alpha, alphaMask;
        }
        public static class XRenderPictFormat extends Structure {
            public int id;
            public int type;
            public int depth;
            public XRenderDirectFormat direct;
            public int colormap;
        }
        int PictTypeIndexed = 0x0;
        int PictTypeDirect = 0x1;
        XRenderPictFormat XRenderFindVisualFormat(Display display, Visual visual);
    }
    
    X11 INSTANCE = (X11)Native.loadLibrary("X11", X11.class, new OptionMap());

    int Success = 0;
    int BadRequest = 1;
    int BadValue = 2;
    int BadWindow = 3;
    int BadPixmap = 4;
    int BadAtom = 5;
    int BadCursor = 6;
    int BadFont = 7;
    int BadMatch = 8;
    int BadDrawable = 9;
    int BadAccess = 10;
    int BadAlloc = 11;
    int BadColor = 12;
    int BadGC = 13;
    int BadIDChoice = 14;
    int BadName = 15;
    int BadLength = 16;
    int BadImplementation = 17;
    
    public static class XWMHints extends Structure {
        public NativeLong flags;
        public int input;
        public int initial_state;
        public int icon_pixmap;
        public int icon_window;
        public int icon_x, icon_y;
        public int icon_mask;
        public int window_group;
    }

    public static class XTextProperty extends Structure {
        public String value;
        public int encoding;
        public int format;
        public NativeLong nitems;
    }

    public static class XSizeHints extends Structure {
        public NativeLong flags;
        public int x, y;
        public int width, height;
        public int min_width, min_height;
        public int max_width, max_height;
        public int width_inc, height_inc;
        // TODO: nested struct
        public int min_aspect_x;
        public int min_aspect_y;
        public int max_aspect_x;
        public int max_aspect_y;
        public int base_width_y;
        public int base_width, base_height;
        public int win_gravity;
    }
    
    public static class XWindowAttributes extends Structure {
        public int x, y;
        public int width, height;
        public int border_width;
        public int depth;
        public Visual visual;
        public int root;
        public int c_class;
        public int bit_gravity;
        public int win_gravity;
        public int backing_store;
        public NativeLong backing_planes;
        public NativeLong backing_pixel;
        public int save_under;
        public int colormap;
        public int map_installed;
        public int map_state;
        public NativeLong all_event_masks;
        public NativeLong your_event_mask;
        public NativeLong do_not_propagate_mask;
        public int override_redirect;
        public Pointer screen;
    }
    
    int CWBackPixmap = (1<<0);
    int CWBackPixel = (1<<1);
    int CWBorderPixmap = (1<<2);
    int CWBorderPixel = (1<<3);
    int CWBitGravity = (1<<4);
    int CWWinGravity = (1<<5);
    int CWBackingStore = (1<<6);
    int CWBackingPlanes = (1<<7);
    int CWBackingPixel = (1<<8);
    int CWOverrideRedirect = (1<<9);
    int CWSaveUnder = (1<<10);
    int CWEventMask = (1<<11);
    int CWDontPropagate = (1<<12);
    int CWColormap = (1<<13);
    int CWCursor = (1<<14);
    public static class XSetWindowAttributes extends Structure {
        public int background_pixmap;
        public NativeLong background_pixel;
        public int border_pixmap;
        public NativeLong border_pixel;
        public int bit_gravity;
        public int win_gravity;
        public int backing_store;
        public NativeLong backing_planes;
        public NativeLong backing_pixel;
        public int save_under; // boolean
        public NativeLong event_mask;
        public NativeLong do_not_propagate_mask;
        public int override_redirect; // boolean
        public int colormap;
        public int cursor;
        
    }
    
    int XK_0 = 0x30;
    int XK_9 = 0x39;
    int XK_A = 0x41;
    int XK_Z = 0x5a;
    int XK_a = 0x61;
    int XK_z = 0x7a;
    int XK_Shift_L = 0xffe1;
    int XK_Shift_R = 0xffe1;
    int XK_Control_L = 0xffe3;
    int XK_Control_R = 0xffe4;
    int XK_CapsLock = 0xffe5;
    int XK_ShiftLock = 0xffe6;
    int XK_Meta_L = 0xffe7;
    int XK_Meta_R = 0xffe8;
    int XK_Alt_L = 0xffe9;
    int XK_Alt_R = 0xffea;

    int VisualNoMask = 0x0;
    int VisualIDMask = 0x1;
    int VisualScreenMask = 0x2;
    int VisualDepthMask = 0x4;
    int VisualClassMask = 0x8;
    int VisualRedMaskMask = 0x10;
    int VisualGreenMaskMask = 0x20;
    int VisualBlueMaskMask = 0x40;
    int VisualColormapSizeMask = 0x80;
    int VisualBitsPerRGBMask = 0x100;
    int VisualAllMask = 0x1FF;
    
    int StaticGray = 0x0;
    int GrayScale = 0x1;
    int StaticColor = 0x2;
    int PseudoColor = 0x3;
    int TrueColor = 0x4;
    int DirectColor = 0x5;
    public static class XVisualInfo extends Structure {
        public Visual visual;
        public int visualID;
        public int screen;
        public int depth;
        public int clazz;
        public NativeLong red_mask;
        public NativeLong green_mask;
        public NativeLong blue_mask;
        public int colormap_size;
        public int bits_per_rgb;
    }
    int AllocNone = 0;
    int AllocAll = 1;
    
    int XA_PRIMARY = 0;
    int XA_SECONDARY = 1;
    int XA_ARC = 2;
    int XA_ATOM = 4;
    int XA_BITMAP = 5;
    int XA_CARDINAL = 6;
    
    int PropModeReplace = 0;
    int PropModePrepend = 1;
    int PropModeAppend = 2;

    int None = 0;
    int ParentRelative = 1;
    int CopyFromParent = 0;
    int PointerWindow = 0;
    int InputFocus = 1;
    int PointerRoot = 1;
    int AnyPropertyType = 0;
    int AnyKey = 0;
    int AnyButton = 0;
    int AllTemporary = 0;
    int CurrentTime = 0;
    int NoSymbol = 0;
    
    Display XOpenDisplay(String name);
    int XGetErrorText(Display display, int code, byte[] buffer, int len);
    int XDefaultScreen(Display display);
    Visual XDefaultVisual(Display display, int screen);
    int XDefaultColormap(Display display, int screen);
    int XDisplayWidth(Display display, int screen);
    int XDisplayHeight(Display display, int screen);
    Window XDefaultRootWindow(Display display);
    int XRootWindow(Display display, int screen);
    int XAllocNamedColor(Display display, int colormap, String color_name, 
                         Pointer screen_def_return, Pointer exact_def_return);
    XSizeHints XAllocSizeHints();
    void XSetWMProperties(Display display, Window window, String window_name,
                          String icon_name, Pointer argv, int argc,
                          XSizeHints normal_hints, Pointer wm_hints,
                          Pointer class_hints);
    int XFree(Pointer data);
    Window XCreateSimpleWindow(Display display, Window parent, int x, int y, 
                            int width, int height, int border_width,
                            NativeLong border, NativeLong background);
    int XCreateBitmapFromData(Display display, Window window, Pointer data, 
                              int width, int height);
    int XMapWindow(Display display, Window window);
    int XFlush(Display display);
    int XUnmapWindow(Display display, Window window);
    int XDestroyWindow(Display display, Window window);
    int XCloseDisplay(Display display);
    int XClearWindow(Display display, Window window);
    int XClearArea(Display display, Drawable window, int x, int y, int w, int h, int exposures);
    Pixmap XCreatePixmap(Display display, Drawable drawable, int width, int height, int depth);
    int XFreePixmap(Display display, Pixmap pixmap);
    GC XCreateGC(Display display, Drawable drawable, NativeLong mask, Pointer values);
    int XFreeGC(Display display, GC gc);
    int XFillRectangle(Display display, Drawable drawable, GC gc, 
                       int x, int y, int width, int height);
    int XSetForeground(Display display, GC gc, int color);
    int XSetBackground(Display display, GC gc, int color);
    int XFillArc(Display display, Drawable drawable, GC gc, int x, int y, 
                 int width, int height, int angle1, int angle2);
    int XQueryTree(Display display, Window window, IntByReference root, 
                   IntByReference parent, PointerByReference children,
                   IntByReference childCount);
    boolean XQueryPointer(Display display, Window window, 
                          IntByReference root_return,
                          IntByReference child_return, 
                          IntByReference root_x_return,
                          IntByReference root_y_return, 
                          IntByReference win_x_return,
                          IntByReference win_y_return,
                          IntByReference mask_return);
    int XGetWindowAttributes(Display display, XID window, XWindowAttributes attributes);
    int XChangeWindowAttributes(Display display, XID window, NativeLong valuemask, XSetWindowAttributes attributes);
    
    int NoEventMask = 0;
    int KeyPressMask = (1<<0);
    int KeyReleaseMask = (1<<1);
    int ButtonPressMask = (1<<2);
    int ButtonReleaseMask = (1<<3);
    int EnterWindowMask = (1<<4);
    int LeaveWindowMask = (1<<5);
    int PointerMotionMask = (1<<6);
    int PointerMotionHintMask = (1<<7);
    int Button1MotionMask = (1<<8);
    int Button2MotionMask = (1<<9);
    int Button3MotionMask = (1<<10);
    int Button4MotionMask = (1<<11);
    int Button5MotionMask = (1<<12);
    int ButtonMotionMask = (1<<13);
    int KeymapStateMask = (1<<14);
    int ExposureMask = (1<<15);
    int VisibilityChangeMask = (1<<16);
    int StructureNotifyMask = (1<<17);
    int ResizeRedirectMask = (1<<18);
    int SubstructureNotifyMask = (1<<19);
    int SubstructureRedirectMask = (1<<20);
    int FocusChangeMask = (1<<21);
    int PropertyChangeMask = (1<<22);
    int ColormapChangeMask = (1<<23);
    int OwnerGrabButtonMask = (1<<24);
    
    int XSelectInput(Display display, Window window, int eventMask);
    /** Returns an {@link XWMHints} which must be freed by {@link #XFree}. */
    XWMHints XGetWMHints(Display display, Window window);
    int XGetWMName(Display display, Window window,
                   XTextProperty text_property_return);
    int XQueryKeymap(Display display, byte[] keys_return);
    int XKeycodeToKeysym(Display display, int keycode, int index);
    /** Returns an array of {@link XVisualInfo} which must be freed by {@link #XFree}.
     * Use {@link XVisualInfo#toArray(Structure[]) 
     * toArray(new XVisualInfo[nitems_return.getValue()]} to obtain the array. 
     */
    XVisualInfo XGetVisualInfo(Display display, int vinfo_mask, XVisualInfo vinfo_template,
                               IntByReference nitems_return);
    int XCreateColormap(Display display, Window w, Visual visual, int alloc);
    int XGetWindowProperty(Display display, int w, int property, int long_offset,
                           int long_length, boolean delete, int reg_type,
                           IntByReference actual_type_return,
                           IntByReference actual_format_return,
                           IntByReference nitems_return,
                           IntByReference bytes_after_return,
                           PointerByReference prop_return);
    int XChangeProperty(Display display, XID w, int property, int type, 
                        int format, int mode, Pointer data, int nelements);
    int XDeleteProperty(Display display, XID w, int property);
    int XInternAtom(Display display, String name, boolean only_if_exists);
    int XCopyArea(Display dpy, Drawable src, Drawable dst, GC gc, 
                  int src_x, int src_y, int w, int h, int dst_x, int dst_y);
}
