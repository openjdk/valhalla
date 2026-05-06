/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */
package org.openjdk.asmtools.jdis;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.Tables.*;
import static org.openjdk.asmtools.jdis.TraceUtils.mapToHexString;
import static org.openjdk.asmtools.jdis.TraceUtils.traceln;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * represents one entry of StackMap attribute
 */
class StackMapData {
    static int prevFramePC = 0;
    boolean isStackMapTable = false;
    StackMapFrameType stackFrameType = null;
    int start_pc;
    int[] lockMap;
    int[] stackMap;

    public StackMapData(CodeData code, DataInputStream in) throws IOException {
        start_pc = in.readUnsignedShort();
        lockMap = readMap(code, in);
        stackMap = readMap(code, in);
        traceln(2, format("stack_map_entry:pc=%d numloc=%s  numstack=%s",
                start_pc, mapToHexString(lockMap), mapToHexString(stackMap)));
    }

    public StackMapData(CodeData code, DataInputStream in,
            boolean isStackMapTable) throws IOException {
        this.isStackMapTable = isStackMapTable;
        int ft_val = in.readUnsignedByte();
        StackMapFrameType frame_type = stackMapFrameType(ft_val);
        int offset = 0;
        switch (frame_type) {
            case SAME_FRAME:
                // type is same_frame;
                offset = ft_val;
                traceln(2, format("same_frame=%d", ft_val));
                break;
            case SAME_FRAME_EX:
                // type is same_frame_extended;
                offset = in.readUnsignedShort();
                traceln(2, format("same_frame_extended=%d, offset=%d", ft_val, offset));
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                // type is same_locals_1_stack_item_frame
                offset = ft_val - 64;
                stackMap = readMapElements(code, in, 1);
                traceln(2, format("same_locals_1_stack_item_frame=%d, offset=%d, numstack=%s",
                        ft_val, offset, mapToHexString(stackMap)));
                break;
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                // type is same_locals_1_stack_item_frame_extended
                offset = in.readUnsignedShort();
                stackMap = readMapElements(code, in, 1);
                traceln(2, format("same_locals_1_stack_item_frame_extended=%d, offset=%d, numstack=%s",
                        ft_val, offset, mapToHexString(stackMap)));
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
                // type is chop_frame
                offset = in.readUnsignedShort();
                traceln(2, format("chop_frame=%d offset=%d", ft_val, offset));
                break;
            case APPEND_FRAME:
                // type is append_frame
                offset = in.readUnsignedShort();
                lockMap = readMapElements(code, in, ft_val - 251);
                traceln(2, format("append_frame=%d offset=%d numlock=%s",
                        ft_val, offset, mapToHexString(lockMap)));
                break;
            case FULL_FRAME:
                // type is full_frame
                offset = in.readUnsignedShort();
                lockMap = readMap(code, in);
                stackMap = readMap(code, in);
                traceln(2, format("full_frame=%d offset=%d numloc=%s  numstack=%s",
                        ft_val, offset, mapToHexString(lockMap), mapToHexString(stackMap)));
                break;
            default:
                TraceUtils.traceln("incorrect frame_type argument");

        }
        stackFrameType = frame_type;
        start_pc = prevFramePC == 0 ? offset : prevFramePC + offset + 1;
        prevFramePC = start_pc;
    }

    private int[] readMap(CodeData code, DataInputStream in) throws IOException {
        int num = in.readUnsignedShort();
        return readMapElements(code, in, num);
    }

    private int[] readMapElements(CodeData code, DataInputStream in, int num) throws IOException {
        int[] map = new int[num];
        for (int k = 0; k < num; k++) {
            int mt_val = 0;
            try {
                mt_val = in.readUnsignedByte();
            } catch (EOFException eofe) {
                throw eofe;
            }
            StackMapType maptype = stackMapType(mt_val, null);
            switch (maptype) {
                case ITEM_Object:
                    mt_val = mt_val | (in.readUnsignedShort() << 8);
                    break;
                case ITEM_NewObject: {
                    int pc = in.readUnsignedShort();
                    code.get_iAtt(pc).referred = true;
                    mt_val = mt_val | (pc << 8);
                    break;
                }
            }
            map[k] = mt_val;
        }
        return map;
    }

}
