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
package com.sun.jna.examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import junit.framework.TestCase;

// TODO: test method invocations before/after pack, before/after setvisible
// TODO: test RootPaneContainer/non-RootPaneContainer variations
// TODO: use ComponentTestFixture from abbot
public class WindowUtilsTest extends TestCase {

    MouseInputAdapter handler = new MouseInputAdapter() {
        private Point offset;
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e))
                offset = e.getPoint();
        }
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                System.exit(1);
            }
        }
        public void mouseReleased(MouseEvent e) {
            offset = null;
        }
        public void mouseDragged(MouseEvent e) {
            if (offset != null) {
                Window w = (Window)e.getSource();
                Point where = e.getPoint();
                where.translate(-offset.x, -offset.y);
                Point loc = w.getLocationOnScreen();
                loc.translate(where.x, where.y);
                w.setLocation(loc.x, loc.y);
            }
        }
    };
    
    private Robot robot;
    
    protected void setUp() throws Exception {
        robot = new Robot();
    }
    
    protected void tearDown() {
        robot = null;
        Window[] owned = JOptionPane.getRootFrame().getOwnedWindows();
        for (int i=0;i < owned.length;i++) {
            owned[i].dispose();
        }
    }
    
    private static final int X = 100;
    private static final int Y = 100;
    private static final int W = 100;
    private static final int H = 100;

    // Expect failure on windows and x11, since transparent pixels are not 
    // properly captured by java.awt.Robot
    public void testWindowTransparency() throws Exception {
        if (GraphicsEnvironment.isHeadless())
            return;
        System.setProperty("sun.java2d.noddraw", "true");
        GraphicsConfiguration gconfig = WindowUtils.getAlphaCompatibleGraphicsConfiguration();
        Frame root = JOptionPane.getRootFrame();
        final Window background = new Window(root);
        background.setBackground(Color.white);
        background.setLocation(X, Y);
        final JWindow transparent = new JWindow(root, gconfig);
        transparent.setLocation(X, Y);
        ((JComponent)transparent.getContentPane()).setOpaque(false);
        transparent.getContentPane().add(new JComponent() {
            public Dimension getPreferredSize() {
                return new Dimension(W, H);
            }
            protected void paintComponent(Graphics g) {
                g = g.create();
                g.setColor(Color.red);
                g.fillRect(getWidth()/4, getHeight()/4, getWidth()/2, getHeight()/2);
                g.drawRect(0, 0, getWidth()-1, getHeight()-1);
                g.dispose();
            }
        });
        transparent.addMouseListener(handler);
        transparent.addMouseMotionListener(handler);
        
        SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            background.pack();
            background.setSize(new Dimension(W, H));
            background.setVisible(true);
            transparent.pack();
            transparent.setSize(new Dimension(W, H));
            transparent.setVisible(true);
            transparent.toFront();
        }});
        
        WindowUtils.setWindowTransparent(transparent, true);
        
        //robot.delay(60000);

        Color sample = robot.getPixelColor(X + W/2, Y + H/2);
        assertEquals("Painted pixel should be opaque", Color.red, sample);
        
        sample = robot.getPixelColor(X + 10, Y + 10);
        assertEquals("Unpainted pixel should be transparent", Color.white, sample);
    }
    
    // Expect failure on windows and x11, since transparent pixels are not 
    // properly captured by java.awt.Robot
    public void testWindowAlpha() throws Exception {
        if (GraphicsEnvironment.isHeadless())
            return;
        System.setProperty("sun.java2d.noddraw", "true");
        GraphicsConfiguration gconfig = WindowUtils.getAlphaCompatibleGraphicsConfiguration();
        Frame root = JOptionPane.getRootFrame();
        final Window background = new Window(root);
        background.setBackground(Color.white);
        background.setLocation(X, Y);
        final Window transparent = new Window(root, gconfig);
        transparent.setBackground(Color.black);
        transparent.setLocation(X, Y);
        WindowUtils.setWindowAlpha(transparent, .5f);
        
        transparent.addMouseListener(handler);
        transparent.addMouseMotionListener(handler);

        SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            background.pack();
            background.setSize(new Dimension(W, H));
            background.setVisible(true);
            transparent.pack();
            transparent.setSize(new Dimension(W, H));
            transparent.setVisible(true);
        }});
        
        //robot.delay(60000);

        Point where = new Point(transparent.getX() + W/2, 
                                transparent.getY() + H/2);
        Color sample = robot.getPixelColor(where.x, where.y);
        // NOTE: w32 won't sample non-opaque windows
        if (System.getProperty("os.name").startsWith("Windows")) {
            assertFalse("Sample not transparent (w32)",
                        sample.equals(transparent.getBackground()));
        }
        else {
            assertEquals("Sample should be 50% fg/bg",
                         new Color(128, 128, 128), sample);
        }
        
        SwingUtilities.invokeAndWait(new Runnable() {public void run() {
            WindowUtils.setWindowAlpha(transparent, 1f);
        }});
        sample = robot.getPixelColor(where.x, where.y);
        assertEquals("Window should be opaque with alpha=1f",
                     transparent.getBackground(), sample);
        
        SwingUtilities.invokeAndWait(new Runnable() {public void run() {
            WindowUtils.setWindowAlpha(transparent, 0f);
        }});
        sample = robot.getPixelColor(where.x, where.y);
        assertEquals("Window should be transparent with alpha=0f",
                     transparent.getBackground(), sample);
    }
    
    public void testWindowRegion() throws Exception {
        if (GraphicsEnvironment.isHeadless())
            return;
        Frame root = JOptionPane.getRootFrame();
        final Window background = new Window(root);
        background.setBackground(new Color(255, 255, 255));
        background.setLocation(X, Y);
        final JWindow foreground = new JWindow(root);
        Color fgColor = new Color(0, 0, 255);
        foreground.getContentPane().setBackground(fgColor);
        foreground.setLocation(X, Y);
        Area mask = new Area(new Rectangle(0, 0, W, H));
        mask.subtract(new Area(new Rectangle(W/4, H/4, W/2, H/2)));
        WindowUtils.setWindowMask(foreground, mask);
        
        foreground.addMouseListener(handler);
        foreground.addMouseMotionListener(handler);

        SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            background.pack();
            background.setSize(new Dimension(W, H));
            background.setVisible(true);
            foreground.pack();
            foreground.setSize(new Dimension(W, H));
            foreground.setVisible(true);
        }});
        
        //robot.delay(60000);

        Point where = new Point(foreground.getX() + W/2, 
                                foreground.getY() + H/2);
        Color sample = robot.getPixelColor(where.x, where.y);
        assertEquals("Background window should show through",
                     background.getBackground(), sample);

        sample = robot.getPixelColor(where.x-W/2, where.y-H/2);
        assertEquals("Foreground window should show through",
                     fgColor, sample);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(WindowUtilsTest.class);
    }
}
