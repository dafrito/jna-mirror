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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author wmeissner@gmail.com
 */
class SimpleBufferPool implements BufferPool {
    private final int bufferSize, poolSize;
    private final BufferPool parent;
    private ArrayList list;
    
    /**
     * Creates a new instance of SimpleBufferPool
     * @param bufferSize The Size of ByteBuffer this pool should return
     * @param poolSize The maximum number of ByteBuffers to cache
     */
    public SimpleBufferPool(int bufferSize, int poolSize) {
        this(new DefaultPool(), bufferSize, poolSize);
    }
    
    /** Creates a new instance of SimpleBufferPool
     * @param parent The parent pool from which to to fetch/return extra buffers.
     * @param bufferSize The Size of ByteBuffer this pool should return.
     * @param poolSize The maximum number of ByteBuffers to cache.
     */
    public SimpleBufferPool(BufferPool parent, int bufferSize, int poolSize) {
        this.parent = parent;
        this.bufferSize = bufferSize;
        this.poolSize = poolSize;
        this.list = new ArrayList(poolSize);
    }
    
    public ByteBuffer get(int size) {
        if (size <= bufferSize && !list.isEmpty()) {
            //System.out.println("Returning cached buffer for size=" + size);
            // Removing from the end of the ArrayList is O(1)
            ByteBuffer buf = (ByteBuffer) list.remove(list.size() - 1);
            buf.clear();
            return buf;
        }
        // Fetch a new buffer from the parent pool
        //System.out.println("Allocating new direct ByteBuffer");
        // Default to allocating a new buffer - make it at least bufferSize so it
        // can be added back to the pool later
        // This also handles buffers that are larger than the pool bufferSize.
        return parent.get(Math.max(size, bufferSize));
    }
    public void put(ByteBuffer buf) {
        if (list.size() < poolSize && buf.capacity() == bufferSize) {
            //System.out.println("Storing ByteBuffer in pool size=" + bufferSize);
            // Adding at the end of the ArrayList is O(1)
            list.add(buf);
        } else {
            parent.put(buf);
        }
    }
    public void putAll(List list) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            put((ByteBuffer) it.next());
        }
    }
    static class DefaultPool implements BufferPool {
        public ByteBuffer get(int size) {
            return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        // Just let the GC collect the buffers
        public void put(ByteBuffer buffer) { }
        public void putAll(List list) { }
    }
}
