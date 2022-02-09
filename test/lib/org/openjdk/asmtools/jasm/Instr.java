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

import java.io.IOException;

import static org.openjdk.asmtools.jasm.OpcodeTables.*;

/**
 *
 */
class Instr {

    Instr next = null;
    int pc;
    int pos;
    Opcode opc;
    Argument arg;
    Object arg2; // second or unusual argument

    public Instr(int pc, int pos, Opcode opc, Argument arg, Object arg2) {
        this.pc = pc;
        this.pos = pos;
        this.opc = opc;
        this.arg = arg;
        this.arg2 = arg2;
    }

    public Instr() {
    }

    public void write(CheckedDataOutputStream out, Environment env) throws IOException {
        OpcodeType type = opc.type();
        switch (type) {
            case NORMAL: {
                if (opc == Opcode.opc_bytecode) {
                    out.writeByte(arg.arg);
                    return;
                }
                out.writeByte(opc.value());
                int opcLen = opc.length();
                if (opcLen == 1) {
                    return;
                }

                switch (opc) {
                    case opc_tableswitch:
                        ((SwitchTable) arg2).writeTableSwitch(out);
                        return;
                    case opc_lookupswitch:
                        ((SwitchTable) arg2).writeLookupSwitch(out);
                        return;
                }

                int iarg;
                try {
                    iarg = arg.arg;
                } catch (NullPointerException e) {
                    throw new Parser.CompilerError(env.errorStr("comperr.instr.nullarg", opc.parsekey()));
                }
//env.traceln("instr:"+opcNamesTab[opc]+" len="+opcLen+" arg:"+iarg);
                switch (opc) {
                    case opc_jsr:
                    case opc_goto:
                    case opc_ifeq:
                    case opc_ifge:
                    case opc_ifgt:
                    case opc_ifle:
                    case opc_iflt:
                    case opc_ifne:
                    case opc_if_icmpeq:
                    case opc_if_icmpne:
                    case opc_if_icmpge:
                    case opc_if_icmpgt:
                    case opc_if_icmple:
                    case opc_if_icmplt:
                    case opc_if_acmpeq:
                    case opc_if_acmpne:
                    case opc_ifnull:
                    case opc_ifnonnull:
                    case opc_jsr_w:
                    case opc_goto_w:
                        iarg = iarg - pc;
                        break;
                    case opc_iinc:
                        iarg = (iarg << 8) | (((Argument) arg2).arg & 0xFF);
                        break;
                    case opc_invokeinterface:
                        iarg = ((iarg << 8) | (((Argument) arg2).arg & 0xFF)) << 8;
                        break;
                    case opc_invokedynamic: // JSR-292
                        iarg = (iarg << 16);
                        break;
                    case opc_ldc:
                        if ((iarg & 0xFFFFFF00) != 0) {
                            throw new Parser.CompilerError(
                                    env.errorStr("comperr.instr.arglong", opc.parsekey(), iarg));
                        }
                        break;
                }
                switch (opcLen) {
                    case 1:
                        return;
                    case 2:
                        out.writeByte(iarg);
                        return;
                    case 3:
                        out.writeShort(iarg);
                        return;
                    case 4: // opc_multianewarray only
                        out.writeShort(iarg);
                        iarg = ((Argument) arg2).arg;
                        out.writeByte(iarg);
                        return;
                    case 5:
                        out.writeInt(iarg);
                        return;
                    default:
                        throw new Parser.CompilerError(
                                env.errorStr("comperr.instr.opclen", opc.parsekey()));
                }
            }
            case WIDE:
                out.writeByte(Opcode.opc_wide.value());
                out.writeByte(opc.value() & 0xFF);
                out.writeShort(arg.arg);
                if (opc == Opcode.opc_iinc_w) {
                    out.writeShort(((Argument) arg2).arg);
                }
                return;
            case PRIVELEGED:
            case NONPRIVELEGED:
                out.writeByte(opc.value() >> 8);
                out.writeByte(opc.value() & 0xFF);
                return;
            default:
                throw new Parser.CompilerError(
                        env.errorStr("comperr.instr.opclen", opc.parsekey()));
        } // end writeSpecCode

    }
} // end Instr

