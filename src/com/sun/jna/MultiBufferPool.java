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
import java.util.Iterator;
import java.util.List;

/**
 * @author wmeissner@gmail.com
 */
public class MultiBufferPool implements BufferPool {
    
    /**
     * Creates a new instance of MultiBufferPool
     */
    public MultiBufferPool(int maxBufferSize, int maxItemsPerSize, boolean threadSafe) {
        this.maxBufferSize = maxBufferSize;
        this.maxItemsPerSize = maxItemsPerSize;
        maxPoolIndex = getSizeIndex(maxBufferSize);
        pools = new SimpleBufferPool[maxPoolIndex + 1];
        
        // Now create each of the buckets
        for (int i = 0; i <= maxPoolIndex; ++i) {
            if (threadSafe) {
                pools[i] = new SynchronizedPool(1 << i, maxItemsPerSize);
            } else {
                pools[i] = new SimpleBufferPool(1 << i, maxItemsPerSize);
            }
        }
    }
    public MultiBufferPool(int maxBufferSize, int maxItemsPerSize) {
        this(maxBufferSize, maxItemsPerSize, false);
    }
    
    private final int maxBufferSize, maxItemsPerSize, maxPoolIndex;
    private SimpleBufferPool[] pools;
    private static final int getSizeIndex(int size) {
        for (int i = 0; i < 32; ++i) {
            if ((1 << i) >= size) {
                //System.out.println("size " + size + " maps to pool index " + i);
                return i;
            }
        }
        return -1;
    }
    public ByteBuffer get(int size) {
        int index = getSizeIndex(size);
        if (index <= maxPoolIndex) {
            return pools[index].get(size);
        }
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
    public void put(ByteBuffer buf) {
        int index = getSizeIndex(buf.capacity());
        if (index <= maxPoolIndex) {
            pools[index].put(buf);
        }
    }
    public void putAll(List list) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            put((ByteBuffer) it.next());
        }
    }
    
    static class SynchronizedPool extends SimpleBufferPool {
        public SynchronizedPool(int bufferSize, int poolSize) {
            super(bufferSize, poolSize);
        }
        public synchronized ByteBuffer get(int size) {
            return super.get(size);
        }
        public synchronized void put(ByteBuffer buf) {
            super.put(buf);
        }
    }
}
