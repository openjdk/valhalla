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

import org.openjdk.asmtools.jasm.Tables.AttrTag;
import org.openjdk.asmtools.jasm.ConstantPool.ConstCell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 *
 */
class MethodData extends MemberData {

    /**
     * MethodParamData
     */
    class ParamNameData implements Data {

        int access;
        ConstCell name;

        public ParamNameData(int access, ConstCell name) {
            this.access = access;
            this.name = name;
        }

        @Override
        public int getLength() {
            return 4;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            int nm = 0;
            int ac = 0;
            if (name != null) {
                nm = name.arg;
                ac = access;
            }
            out.writeShort(nm);
            out.writeShort(ac);
        }
    }// end class MethodParamData

    /**
     * Used to store Parameter Arrays (as attributes)
     */
    static public class DataPArrayAttr<T extends Data> extends AttrData implements Constants {

        TreeMap<Integer, ArrayList<T>> elements; // Data
        int paramsTotal;

        public DataPArrayAttr(ClassData cls, String name, int paramsTotal, TreeMap<Integer, ArrayList<T>> elements) {
            super(cls, name);
            this.paramsTotal = paramsTotal;
            this.elements = elements;
        }

        public DataPArrayAttr(ClassData cls, String name, int paramsTotal) {
            this(cls, name, paramsTotal, new TreeMap<Integer, ArrayList<T>>());
        }

        public void put(int paramNum, T element) {
            ArrayList<T> v = get(paramNum);
            if (v == null) {
                v = new ArrayList<>();
                elements.put(paramNum, v);
            }

            v.add(element);
        }

        public ArrayList<T> get(int paramNum) {
            return elements.get(paramNum);
        }

        @Override
        public int attrLength() {
            int length = 1;  // One byte for the parameter count

            // calculate overall size here rather than in add()
            // because it may not be available at the time of invoking of add()
            for (int i = 0; i < paramsTotal; i++) {
                ArrayList<T> attrarray = get(i);
                if (attrarray != null) {
                    for (Data item : attrarray) {
                        length += item.getLength();
                    }
                }
                length += 2; // 2 bytes for the annotation count for each parameter
            }

            return length;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);  // attr name, attr len
            out.writeByte(paramsTotal); // number of parameters total (in byte)

            for (int i = 0; i < paramsTotal; i++) {
                ArrayList<T> attrarray = get(i);
                if (attrarray != null) {
                    // write out the number of annotations for the current param
                    out.writeShort(attrarray.size());
                    for (T item : attrarray) {
                        item.write(out); // write the current annotation
                    }
                } else {
                    out.writeShort(0);
                    // No annotations to write out
                }
            }
        }
    }// end class DataPArrayAttr


    /* Method Data Fields */
    protected Environment env;
    protected ConstCell nameCell, sigCell;
    protected CodeAttr code;
    protected DataVectorAttr<ConstCell> exceptions = null;
    protected DataVectorAttr<ParamNameData> paramNames = null;
    protected DataPArrayAttr<AnnotationData> pannotAttrVis = null;
    protected DataPArrayAttr<AnnotationData> pannotAttrInv = null;
    protected DefaultAnnotationAttr defaultAnnot = null;

    public MethodData(ClassData cls, int acc,
            ConstCell name, ConstCell sig, ArrayList<ConstCell> exc_table) {
        super(cls, acc);
        this.env = cls.env;
        nameCell = name;
        sigCell = sig;
        if ((exc_table != null) && (!exc_table.isEmpty())) {
            exceptions = new DataVectorAttr<>(cls,
                    AttrTag.ATT_Exceptions.parsekey(),
                    exc_table);
        }
        // Normalize the modifiers to access flags
        if (Modifiers.hasPseudoMod(acc)) {
            createPseudoMod();
        }
    }

    public void addMethodParameter(int totalParams, int paramNum, ConstCell name, int access) {
        env.traceln("addMethodParameter Param[" + paramNum + "] (name: " + name.toString() + ", Flags (" + access + ").");
        if (paramNames == null) {
            paramNames = new DataVectorAttr<>(cls, AttrTag.ATT_MethodParameters.parsekey(), true);
            for (int i = 0; i < totalParams; i++) {
                // initialize the paramName array (in case the name is not given in Jasm syntax)
                paramNames.add(new ParamNameData(0, null));
            }
        }
        paramNames.put(paramNum, new ParamNameData(access, name));
    }

    public CodeAttr startCode(int pos, int paramcnt, Argument max_stack, Argument max_locals) {
        code = new CodeAttr(this, pos, paramcnt, max_stack, max_locals);
        return code;
    }

    public void addDefaultAnnotation(DefaultAnnotationAttr data) {
        defaultAnnot = data;
    }

    public void addParamAnnotation(int totalParams, int paramNum, AnnotationData data) {
        if (!data.invisible) {
            if (pannotAttrVis == null) {
                pannotAttrVis = new DataPArrayAttr<>(cls,
                        AttrTag.ATT_RuntimeVisibleParameterAnnotations.parsekey(),
                        totalParams);
            }
            pannotAttrVis.put(paramNum, data);

        } else {
            if (pannotAttrInv == null) {
                pannotAttrInv = new DataPArrayAttr<>(cls,
                        AttrTag.ATT_RuntimeInvisibleParameterAnnotations.parsekey(),
                        totalParams);
            }
            pannotAttrInv.put(paramNum, data);
        }
    }

    @Override
    protected DataVector getAttrVector() {
        DataVector dv = getDataVector( exceptions, syntheticAttr, deprecatedAttr, paramNames, code, defaultAnnot);
        if (pannotAttrVis != null) {
            dv.add(pannotAttrVis);
        }
        if (pannotAttrInv != null) {
            dv.add(pannotAttrInv);
        }
        return dv;
    }

    /*====================================================== Write */
    public void write(CheckedDataOutputStream out) throws IOException, Parser.CompilerError {
        out.writeShort(access);
        out.writeShort(nameCell.arg);
        out.writeShort(sigCell.arg);
        getAttrVector().write(out);
    }
} // end MethodData

