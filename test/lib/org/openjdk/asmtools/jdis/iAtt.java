/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.RuntimeConstants;
import static org.openjdk.asmtools.jasm.Tables.*;
import static org.openjdk.asmtools.jasm.OpcodeTables.*;
import org.openjdk.asmtools.jasm.Tables;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * instruction attributes
 */
class iAtt {

    /*-------------------------------------------------------- */
    /* iAtt Fields */
    private Options options;

    short lnum = 0;
    boolean referred = false; // from some other instruction
    ArrayList<CodeData.LocVarData> vars;
    ArrayList<CodeData.LocVarData> endvars;
    ArrayList<TrapData> handlers;
    ArrayList<TrapData> traps;
    ArrayList<TrapData> endtraps;
    StackMapData stackMapEntry;
    CodeData code;
    ClassData cls;
    PrintWriter out; // =cls.out;
 /*-------------------------------------------------------- */

    public iAtt(CodeData code) {
        this.code = code;
        this.cls = code.meth.cls;
        out = cls.out;
        options = cls.options;
    }

    void add_var(CodeData.LocVarData var) {
        if (vars == null) {
            vars = new ArrayList<>(4);
        }
        vars.add(var);
    }

    void add_endvar(CodeData.LocVarData endvar) {
        if (endvars == null) {
            endvars = new ArrayList<>(4);
        }
        endvars.add(endvar);
    }

    void add_trap(TrapData trap) {
        if (traps == null) {
            traps = new ArrayList<>(4);
        }
        traps.add(trap);
    }

    void add_endtrap(TrapData endtrap) {
        if (endtraps == null) {
            endtraps = new ArrayList<>(4);
        }
        endtraps.add(endtrap);
    }

    void add_handler(TrapData endtrap) {
        if (handlers == null) {
            handlers = new ArrayList<>(4);
        }
        handlers.add(endtrap);
    }

    public void printEnds() throws IOException {
// prints additional information for instruction:
//  end of local variable and trap scopes;
        int len;
        if ((endvars != null) && (options.contains(Options.PR.VAR))) {
            len = endvars.size() - 1;
            out.print("\t\tendvar");
            for (CodeData.LocVarData line : endvars) {
                out.print(" " + line.slot);
                if (len-- > 0) {
                    out.print(",");
                }
            }
            out.println(";");
        }

        if (endtraps != null) {
            len = endtraps.size() - 1;
            out.print("\t\tendtry");
            for (TrapData line : endtraps) {
                out.print(" " + line.ident());
                if (len-- > 0) {
                    out.print(",");
                }
            }
            out.println(";");
        }
    }

    public void printBegins()
            throws IOException {
// prints additional information for instruction:
// source line number;
// start of exception handler;
// begin of locvar and trap scopes;
        boolean eitherOpt = options.contains(Options.PR.LNT) || options.contains(Options.PR.SRC);
        boolean bothOpt = options.contains(Options.PR.LNT) && options.contains(Options.PR.SRC);
        int k;

        if ((lnum != 0) && eitherOpt) {
            if (bothOpt) {
                out.println("// " + lnum + ": " + cls.getSrcLine(lnum));
            } else if (options.contains(Options.PR.LNT)) {
                out.print(lnum);
            } else if (options.contains(Options.PR.SRC)) {
                out.println("// " + cls.getSrcLine(lnum));
            }
        }
        out.print("\t");
        if (handlers != null) {
            for (TrapData line : handlers) {
                out.print("\tcatch " + line.ident());
                out.print(" " + cls.pool.getClassName(line.catch_cpx) + ";\n\t");
            }
        }
        if (traps != null) {
            int len = traps.size() - 1;
            out.print("\ttry");
            for (TrapData line : traps) {
                out.print(" " + line.ident());
                if (len-- > 0) {
                    out.print(",");
                }
            }
            out.print(";\n\t");
        }
        if ((vars != null) && options.contains(Options.PR.VAR)) {
            for (CodeData.LocVarData line : vars) {
                out.println("\tvar " + line.slot + "; // " + cls.pool.getName(line.name_cpx) + ":" + cls.pool.getName(line.sig_cpx));
                out.print("\t");
            }
        }
    }

    public void printMapList(int[] map) throws IOException {
        boolean pr_cpx = options.contains(Options.PR.CPX);

        for (int k = 0; k < map.length; k++) {
            int fullmaptype = map[k];
            int mt_val = fullmaptype & 0xFF;
            StackMapType maptype = stackMapType(mt_val, out);
            int argument = fullmaptype >> 8;
            switch (maptype) {
                case ITEM_Object:
                    if (pr_cpx) {
                        out.print(" #" + argument);
                    } else {
                        out.print(" ");
                        cls.pool.PrintConstant(out, argument);
                    }
                    break;
                case ITEM_NewObject:
                    if (pr_cpx) {
                        out.print(" " + mt_val);
                    } else {
                        out.print(" " + maptype.printval());
                    }
                    out.print(" " + code.meth.lP + argument);
                    break;
                default:
                    if (pr_cpx) {
                        out.print(" " + mt_val);
                    } else {
                        out.print(" " + maptype.printval());
                    }
            }
            out.print((k == (map.length - 1) ? ';' : ','));
        }
    }

    public void printStackMap() throws IOException {
        if (stackMapEntry == null) {
            return;
        }
        boolean printed = false;
        if (stackMapEntry.stackFrameType != null) {
            out.print(Opcode.opc_stack_frame_type.parsekey()); //    opcNamesTab[opc_stackframetype]);
            out.print(" " + stackMapEntry.stackFrameType.parsekey() + ';');
            out.print("\n\t\t");
            printed = true;
        }
        int[] map = stackMapEntry.lockMap;
        if ((map != null) && (map.length > 0)) {
            out.print(Opcode.opc_locals_map.parsekey());
            printMapList(map);
            out.print("\n\t\t");
            printed = true;
        }
        map = stackMapEntry.stackMap;
        if ((map != null) && (map.length > 0)) {
            out.print(Opcode.opc_stack_map.parsekey());
            printMapList(map);
            out.print("\n\t\t");
            printed = true;
        }
        if (!printed) {
// empty attribute should be printed anyway - it should not
// be eliminated after jdis/jasm cycle
            out.print(Opcode.opc_locals_map.parsekey() + " ;\n\t\t");
        }
    }
}
