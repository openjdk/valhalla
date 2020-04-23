/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.runtime;

import java.util.*;

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ObjectSynchronizer {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type;
    try {
      type = db.lookupType("ObjectSynchronizer");
      gBlockList = type.getAddressField("g_block_list").getValue();
      blockSize = db.lookupIntConstant("ObjectSynchronizer::_BLOCKSIZE").intValue();
      defaultCacheLineSize = db.lookupIntConstant("DEFAULT_CACHE_LINE_SIZE").intValue();
    } catch (RuntimeException e) { }
    type = db.lookupType("ObjectMonitor");
    objectMonitorTypeSize = type.getSize();
    if ((objectMonitorTypeSize % defaultCacheLineSize) != 0) {
      // sizeof(ObjectMonitor) is not already a multiple of a cache line.
      // The ObjectMonitor allocation code in ObjectSynchronizer pads each
      // ObjectMonitor in a block to the next cache line boundary.
      int needLines = ((int)objectMonitorTypeSize / defaultCacheLineSize) + 1;
      objectMonitorTypeSize = needLines * defaultCacheLineSize;
    }
  }

  public long identityHashValueFor(Oop obj) {
    Mark mark = obj.getMark();
    if (mark.isUnlocked()) {
      // FIXME: can not generate marks in debugging system
      return mark.hash();
    } else if (mark.hasMonitor()) {
      ObjectMonitor monitor = mark.monitor();
      Mark temp = monitor.header();
      return temp.hash();
    } else {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(VM.getVM().isDebugging(), "Can not access displaced header otherwise");
      }
      if (mark.hasDisplacedMarkHelper()) {
        Mark temp = mark.displacedMarkHelper();
        return temp.hash();
      }
      // FIXME: can not do anything else here in debugging system
      return 0;
    }
  }

  public static Iterator objectMonitorIterator() {
    if (gBlockList != null) {
      return new ObjectMonitorIterator();
    } else {
      return null;
    }
  }

  private static class ObjectMonitorIterator implements Iterator {

    // JVMTI raw monitors are not pointed by gBlockList
    // and are not included by this Iterator. May add them later.

    ObjectMonitorIterator() {
      blockAddr = gBlockList;
      index = blockSize - 1;
      block = new ObjectMonitor(blockAddr);
    }

    public boolean hasNext() {
      return (index > 0 || block.nextOM() != null);
    }

    public Object next() {
      Address addr;
      if (index == 0) {
        // advance to next block
        blockAddr = block.nextOM();
        if (blockAddr == null) {
          throw new NoSuchElementException();
        }
        block = new ObjectMonitor(blockAddr);
        index = blockSize - 1;
      }
      addr = blockAddr.addOffsetTo(index*objectMonitorTypeSize);
      index --;
      return new ObjectMonitor(addr);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private ObjectMonitor block;
    private int index;
    private Address blockAddr;
  }

  private static Address gBlockList;
  private static int blockSize;
  private static int defaultCacheLineSize;
  private static long objectMonitorTypeSize;

}
