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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.jasm.OpcodeTables.Opcode;
import org.openjdk.asmtools.jasm.Tables.AttrTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.openjdk.asmtools.jasm.RuntimeConstants.SPLIT_VERIFIER_CFV;

class CodeAttr extends AttrData {

    protected ClassData cls;

    protected MethodData mtd;
    protected Environment env;
    protected Argument max_stack, max_locals;
    protected Instr zeroInstr, lastInstr;
    protected int cur_pc = 0;
    protected DataVector<TrapData> trap_table; // TrapData
    protected DataVectorAttr<LineNumData> lin_num_tb; // LineNumData
    protected int lastln = 0;
    protected DataVectorAttr<LocVarData> loc_var_tb;  // LocVarData
    protected DataVector<DataVectorAttr<? extends Data>> attrs;
    protected ArrayList<Integer> slots;
    protected HashMap<String, LocVarData> locvarsHash;
    protected HashMap<String, Label> labelsHash;
    protected HashMap<String, Trap> trapsHash;
    protected StackMapData curMapEntry = null;
    protected DataVectorAttr<StackMapData> stackMap;
    // type annotations
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrVis = null;
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrInv = null;

    public CodeAttr(MethodData mtd, int pos, int paramcnt, Argument max_stack, Argument max_locals) {
        super(mtd.cls, AttrTag.ATT_Code.parsekey());
        this.mtd = mtd;
        this.cls = mtd.cls;
        this.env = cls.env;
        this.max_stack = max_stack;
        this.max_locals = max_locals;
        lastInstr = zeroInstr = new Instr();
        trap_table = new DataVector<>(0); // TrapData
        attrs = new DataVector<>();
        if (env.debugInfoFlag) {
            lin_num_tb = new DataVectorAttr<>(cls, AttrTag.ATT_LineNumberTable.parsekey());
            attrs.add(lin_num_tb);
        }
        slots = new ArrayList<>(paramcnt);
        for (int k = 0; k < paramcnt; k++) {
            slots.add(k, 1);
        }
    }

    void endCode() {
        checkTraps();
        checkLocVars();
        checkLabels();
        //
        if (type_annotAttrVis != null) {
            attrs.add(type_annotAttrVis);
        }
        if (type_annotAttrInv != null) {
            attrs.add(type_annotAttrInv);
        }
    }

    public void addAnnotations(ArrayList<AnnotationData> list) {
        for (AnnotationData item : list) {
            boolean invisible = item.invisible;
            if (item instanceof TypeAnnotationData) {
                // Type Annotations
                TypeAnnotationData ta = (TypeAnnotationData) item;
                if (invisible) {
                    if (type_annotAttrInv == null) {
                        type_annotAttrInv = new DataVectorAttr(cls,
                                AttrTag.ATT_RuntimeInvisibleTypeAnnotations.parsekey());
                    }
                    type_annotAttrInv.add(ta);
                } else {
                    if (type_annotAttrVis == null) {
                        type_annotAttrVis = new DataVectorAttr(cls,
                                AttrTag.ATT_RuntimeVisibleTypeAnnotations.parsekey());
                    }
                    type_annotAttrVis.add(ta);
                }
            }
        }
    }

    /* -------------------------------------- Traps */
    Trap trapDecl(int pos, String name) {
        Trap local;
        if (trapsHash == null) {
            trapsHash = new HashMap<>(10);
            local = null;
        } else {
            local = trapsHash.get(name);
        }
        if (local == null) {
            local = new Trap(pos, name);
            trapsHash.put(name, local);
        }
        return local;
    }

    void beginTrap(int pos, String name) {
        Trap trap = trapDecl(pos, name);
        if (trap.start_pc != Argument.NotSet) {
            env.error("trap.tryredecl", name);
            return;
        }
        trap.start_pc = cur_pc;
    }

    void endTrap(int pos, String name) {
        Trap trap = trapDecl(pos, name);
        if (trap.end_pc != Argument.NotSet) {
            env.error("trap.endtryredecl", name);
            return;
        }
        trap.end_pc = cur_pc;
    }

    void trapHandler(int pos, String name, Argument type) {
        Trap trap = trapDecl(pos, name);
        trap.refd = true;
        TrapData trapData = new TrapData(pos, trap, cur_pc, type);
        trap_table.addElement(trapData);
    }

    void checkTraps() {
        if (trapsHash == null) {
            return;
        }
        for (Trap trap : trapsHash.values()) {
            if (!trap.refd) {
                env.error(trap.pos, "warn.trap.notref", trap.name);
            }
        }

        for (TrapData trapData : trap_table) {
            Trap trapLabel = trapData.trap;
            if (trapLabel.start_pc == Argument.NotSet) {
                env.error(trapData.pos, "trap.notry", trapLabel.name);
            }
            if (trapLabel.end_pc == Argument.NotSet) {
                env.error(trapData.pos, "trap.noendtry", trapLabel.name);
            }
        }
    }

    /* -------------------------------------- Labels */
    Label labelDecl(String name) {
        Label local;
        if (labelsHash == null) {
            labelsHash = new HashMap<>(10);
            local = null;
        } else {
            local = labelsHash.get(name);
        }
        if (local == null) {
            local = new Label(name);
            labelsHash.put(name, local);
        }
        return local;
    }

    public Label LabelDef(int pos, String name) {
        Label label = labelDecl(name);
        if (label.defd) {
            env.error(pos, "label.redecl", name);
            return null;
        }
        label.defd = true;
        label.arg = cur_pc;
        return label;
    }

    public Label LabelRef(String name) {
        Label label = labelDecl(name);
        label.refd = true;
        return label;
    }

    void checkLabels() {
        if (labelsHash == null) {
            return;
        }

        for (Label local : labelsHash.values()) {
            // check that every label is defined
            if (!local.defd) {
                env.error("label.undecl", local.name);
            }
        }
    }

    /* -------------------------------------- Variables */
    LocVarData locvarDecl(String name) {
        LocVarData local;
        if (locvarsHash == null) {
            locvarsHash = new HashMap<>(10);
            local = null;
        } else {
            local = locvarsHash.get(name);
        }
        if (local == null) {
            local = new LocVarData(name);
            locvarsHash.put(name, local);
        }
        return local;
    }

    public void LocVarDataDef(int slot) {
        slots.set(slot, 1);
        if ((max_locals != null) && (max_locals.arg < slots.size())) {
            env.error("warn.illslot", Integer.toString(slot));
        }
    }

    public void LocVarDataDef(String name, ConstantPool.ConstCell type) {
        LocVarData locvar = locvarDecl(name);
        if (locvar.defd) {
            env.error("locvar.redecl", name);
            return;
        }
        locvar.defd = true;
        locvar.start_pc = (short) cur_pc;
        locvar.name_cpx = cls.pool.FindCellAsciz(name);
        locvar.sig_cpx = type;
        int k;
        findSlot:
        {
            for (k = 0; k < slots.size(); k++) {
                if (slots.get(k) == 0) {
                    break findSlot;
                }
            }
            k = slots.size();
        }
        LocVarDataDef(k);
        locvar.arg = k;
        if (loc_var_tb == null) {
            loc_var_tb = new DataVectorAttr<>(cls, AttrTag.ATT_LocalVariableTable.parsekey());
            attrs.add(loc_var_tb);
        }
        loc_var_tb.add(locvar);
    }

    public Argument LocVarDataRef(String name) {
        LocVarData locvar = locvarDecl(name);
        if (!locvar.defd) {
            env.error("locvar.undecl", name);
            locvar.defd = true; // to avoid multiple error messages
        }
        locvar.refd = true;
        return locvar;
    }

    public void LocVarDataEnd(int slot) {
        slots.set(slot, 0);
    }

    public void LocVarDataEnd(String name) {
        LocVarData locvar = locvarsHash.get(name);
        if (locvar == null) {
            env.error("locvar.undecl", name);
            return;
        } else if (!locvar.defd) {
            env.error("locvar.undecl", name);
            return;
        }
        locvar.length = (short) (cur_pc - locvar.start_pc);

        slots.set(locvar.arg, 0);
        locvarsHash.put(name, new LocVarData(name));
    }

    void checkLocVars() {
        if (locvarsHash == null) {
            return;
        }
        for (LocVarData locvar : locvarsHash.values()) {
            if (!locvar.defd) {
                continue;
            } // this is false locvar
            // set end of scope, if not set
            if (slots.get(locvar.arg) == 1) {
                locvar.length = (short) (cur_pc - locvar.start_pc);
                slots.set(locvar.arg, 0);
            }
        }
    }

    /* -------------------------------------- StackMap */
    public StackMapData getStackMap() {
        if (curMapEntry == null) {
            curMapEntry = new StackMapData(env);
            if (cls.cfv.major_version() >= SPLIT_VERIFIER_CFV) {
                curMapEntry.setIsStackMapTable(true);
            }
        }
        return curMapEntry;
    }

    /*====================================================== Instr */
    void addInstr(int mnenoc_pos, Opcode opcode, Argument arg, Object arg2) {
        Instr newInstr = new Instr(cur_pc, cls.env.pos, opcode, arg, arg2);
        lastInstr.next = newInstr;
        lastInstr = newInstr;
        int len = opcode.length();
        switch (opcode) {
            case opc_tableswitch:
                len = ((SwitchTable) arg2).recalcTableSwitch(cur_pc);
                break;
            case opc_lookupswitch:
                len = ((SwitchTable) arg2).calcLookupSwitch(cur_pc);
                break;
            case opc_ldc:
                ((ConstantPool.ConstCell) arg).setRank(ConstantPool.ReferenceRank.LDC);
                break;
            default:
                if (arg instanceof ConstantPool.ConstCell) {
                    ((ConstantPool.ConstCell) arg).setRank(ConstantPool.ReferenceRank.ANY);
                }
        }
        if (env.debugInfoFlag) {
            int ln = env.lineNumber(mnenoc_pos);
            if (ln != lastln) { // only one entry in lin_num_tb per line
                lin_num_tb.add(new LineNumData(cur_pc, ln));
                lastln = ln;
            }
        }
        if (curMapEntry != null) {
            curMapEntry.pc = cur_pc;
            StackMapData prevStackFrame = null;
            if (stackMap == null) {
                if (cls.cfv.major_version() >= SPLIT_VERIFIER_CFV) {
                    stackMap = new DataVectorAttr<>(cls, AttrTag.ATT_StackMapTable.parsekey());
                } else {
                    stackMap = new DataVectorAttr<>(cls, AttrTag.ATT_StackMap.parsekey());
                }
                attrs.add(stackMap);
            } else if (stackMap.size() > 0) {
                prevStackFrame = stackMap.get(stackMap.size() - 1);
            }
            curMapEntry.setOffset(prevStackFrame);
            stackMap.add(curMapEntry);
            curMapEntry = null;
        }

        cur_pc += len;
    }

    /*====================================================== Attr interface */
    // subclasses must redefine this
    @Override
    public int attrLength() {
        return 2 + 2 + 4 // for max_stack, max_locals, and cur_pc
                + cur_pc //      + 2+trap_table.size()*8
                + trap_table.getLength() + attrs.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out)
            throws IOException, Parser.CompilerError {
        int mxstck = (max_stack != null) ? max_stack.arg : 0;
        int mxloc = (max_locals != null) ? max_locals.arg : slots.size();
        super.write(out);  // attr name, attr len
        out.writeShort(mxstck);
        out.writeShort(mxloc);
        out.writeInt(cur_pc);
        for (Instr instr = zeroInstr.next; instr != null; instr = instr.next) {
            instr.write(out, env);
        }

        trap_table.write(out);

        attrs.write(out);
    }

    /*-------------------------------------------------------- */
    /* CodeAttr inner classes */
    static public class Local extends Argument {

        String name;
        boolean defd = false, refd = false;

        public Local(String name) {
            this.name = name;
        }
    }

    /**
     *
     */
    static public class Label extends Local {

        public Label(String name) {
            super(name);
        }
    }

    /**
     *
     */
    class LocVarData extends Local implements Data {

        // arg means slot
        short start_pc, length;
        ConstantPool.ConstCell name_cpx, sig_cpx;

        public LocVarData(String name) {
            super(name);
        }

        @Override
        public int getLength() {
            return 10;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(start_pc);
            out.writeShort(length);
            out.writeShort(name_cpx.arg);
            out.writeShort(sig_cpx.arg);
            out.writeShort(arg);
        }
    }

    /**
     *
     */
    class LineNumData implements Data {

        int start_pc, line_number;

        public LineNumData(int start_pc, int line_number) {
            this.start_pc = start_pc;
            this.line_number = line_number;
        }

        @Override
        public int getLength() {
            return 4;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(start_pc);
            out.writeShort(line_number);
        }
    }

    /**
     *
     */
    class Trap extends Local {

        int start_pc = Argument.NotSet, end_pc = Argument.NotSet;
        int pos;

        Trap(int pos, String name) {
            super(name);
            this.pos = pos;
        }
    }

    /**
     *
     */
    class TrapData implements Data {

        int pos;
        Trap trap;
        int handler_pc;
        Argument catchType;

        public TrapData(int pos, Trap trap, int handler_pc, Argument catchType) {
            this.pos = pos;
            this.trap = trap;
            this.handler_pc = handler_pc;
            this.catchType = catchType;
        }

        @Override
        public int getLength() {
            return 8; // add the length of number of elements
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(trap.start_pc);
            out.writeShort(trap.end_pc);
            out.writeShort(handler_pc);
            if (catchType.isSet()) {
                out.writeShort(catchType.arg);
            } else {
                out.writeShort(0);
            }
        }
    }  // end TrapData
} // end CodeAttr

