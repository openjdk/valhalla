/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import static org.openjdk.asmtools.jasm.Tables.*;
import java.io.IOException;

/**
 *
 */
public class StackMapData implements Data {

    /**
     *
     */
    static public class StackMapItem1 implements Data {

        StackMapType itemType;

        StackMapItem1(StackMapType itemType) {
            this.itemType = itemType;
        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(itemType.value());
        }
    }

    /**
     *
     */
    static public class StackMapItem2 implements Data {

        StackMapType itemType;
        Argument arg;

        StackMapItem2(StackMapType itemType, Argument arg) {
            this.itemType = itemType;
            this.arg = arg;
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(itemType.value());
            out.writeShort(arg.arg);
        }
    }

    int pc;
    int offset;
    int type;
    String stackFrameType = null;
    boolean isStackMapTable = false;
    DataVector localsMap, stackMap;
    Environment env;

    StackMapData(Environment env) {
        this.env = env;
    }

    void setPC(int pc) {
        this.pc = pc;
    }

    void setOffset(int offset) {
        this.offset = offset;
    }

    void setOffset(StackMapData prevFrame) {
        offset = (prevFrame == null) ? pc : (pc - prevFrame.pc - 1);
    }

    void setStackFrameType(String stackFrameType) {
        this.stackFrameType = stackFrameType;

        if (stackFrameType != null) {
            type = stackMapFrameTypeValue(stackFrameType);
        }

        if (stackFrameType == null || type == -1) {
            env.error(pc, "invalid.stack.frame.type", stackFrameType, "" + type);
        }
    }

    void setIsStackMapTable(boolean isStackMapTable) {
        this.isStackMapTable = isStackMapTable;
    }

    void setLocalsMap(DataVector localsMap) {
        this.localsMap = localsMap;
    }

    void setStackMap(DataVector stackMap) {
        this.stackMap = stackMap;
    }

    @Override
    public int getLength() {
        int res = 0;
        StackMapFrameType frame_type = StackMapFrameType.FULL_FRAME;
        //    int frame_type = FULL_FRAME;

        if (isStackMapTable) {
            if (stackFrameType != null) {
                frame_type = stackMapFrameType(type);
            }
            res += 1;
        }

        switch (frame_type) {
            case SAME_FRAME:
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                res += stackMap.getLength() - 2;
                break;
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                res += stackMap.getLength();
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
                res += 2;
                break;
            case SAME_FRAME_EX:
                res += 2;
                break;
            case APPEND_FRAME:
                res += 2 + (localsMap == null ? 0 : (localsMap.getLength() - 2));
                break;
            case FULL_FRAME:
                res += 2;
                res += (localsMap == null ? 2 : localsMap.getLength());
                res += (stackMap == null ? 2 : stackMap.getLength());
                break;
            default:
                ;
        }
        return res;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        StackMapFrameType frame_type = StackMapFrameType.FULL_FRAME;

        if (isStackMapTable) {
            if (stackFrameType != null) {
                frame_type = stackMapFrameType(type);
            }
        }

        switch (frame_type) {
            case SAME_FRAME:
                if (offset >= 64) {
                    env.error(pc, "invalid.offset.same.frame", "" + offset);
                }
                out.writeByte(offset);
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                if (stackMap == null) {
                    env.error(pc, "no.stack.map.same.locals");
                    break;
                }

                if (stackMap.elements.size() != 1) {
                    env.error(pc, "should.be.only.one.stack.map.element");
                    break;
                }

                if (offset >= 64) {
                    env.error(pc, "invalid.offset.same.locals", "" + offset);
                    break;
                }
                out.writeByte(frame_type.value() + offset);
                stackMap.writeElements(out);
                break;
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                if (stackMap == null) {
                    env.error(pc, "no.stack.map.same.locals");
                    break;
                }

                if (stackMap.elements.size() != 1) {
                    env.error(pc, "should.be.only.one.stack.map.element");
                    break;
                }
                out.writeByte(frame_type.value());
                out.writeShort(offset);
                stackMap.writeElements(out);
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
            case SAME_FRAME_EX:
                boolean error = false;

                if (stackMap != null) {
                    env.error(pc, "unexpected.stack.maps");
                    error = true;
                }

                if (localsMap != null) {
                    env.error(pc, "unexpected.locals.maps");
                    error = true;
                }

                if (error) {
                    break;
                }
                out.writeByte(frame_type.value());
                out.writeShort(offset);
                break;
            case APPEND_FRAME:
                if (localsMap == null) {
                    env.error(pc, "no.locals.map.append");
                    break;
                }

                if (localsMap.elements.size() > 3) {
                    env.error(pc, "more.locals.map.elements");
                    break;
                }
                out.writeByte(frame_type.value() + localsMap.elements.size() - 1);
                out.writeShort(offset);
                localsMap.writeElements(out);
                break;
            case FULL_FRAME:
                if (isStackMapTable) {
                    out.writeByte(frame_type.value());
                    out.writeShort(offset);
                } else {
                    out.writeShort(pc);
                }

                if (localsMap == null) {
                    out.writeShort(0);
                } else {
                    localsMap.write(out);
                }

                if (stackMap == null) {
                    out.writeShort(0);
                } else {
                    stackMap.write(out);
                }
                break;
            default:
                env.error(pc, "invalid.stack.frame.type", "" + frame_type);
        }
    }
}
