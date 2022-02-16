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

import org.openjdk.asmtools.jasm.Modifiers;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.Tables.AttrTag;
import static org.openjdk.asmtools.jasm.Tables.CF_Context;

/**
 * Method data for method members in a class of the Java Disassembler
 */
public class MethodData extends MemberData {

    /**
     * CP index to the method name
     */
    protected int name_cpx;

    /**
     * CP index to the method type
     */
    protected int sig_cpx;
    protected String lP;        // labelPrefix
    /**
     * The parameter names for this method
     */
    protected ArrayList<ParamNameData> paramNames;
    /**
     * The visible parameter annotations for this method
     */
    protected ParameterAnnotationData visibleParameterAnnotations;
    /**
     * The invisible parameter annotations for this method
     */
    protected ParameterAnnotationData invisibleParameterAnnotations;
    /**
     * The invisible parameter annotations for this method
     */
    protected AnnotationElement.AnnotValue defaultAnnotation;
    /**
     * The code data for this method. May be null
     */
    private CodeData code;
    /**
     * The exception table (thrown exceptions) for this method. May be null
     */
    private int[] exc_table = null;

    public MethodData(ClassData cls) {
        super(cls);
        memberType = "MethodData";
        lP = (options.contains(Options.PR.LABS)) ? "L" : "";
        paramNames = null;
    }

    /*========================================================*/
    /* Read Methods */
    @Override
    protected boolean handleAttributes(DataInputStream in, AttrTag attrtag, int attrlen) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attrtag) {
            case ATT_Code:
                code = new CodeData(this);
                code.read(in, attrlen);
                break;
            case ATT_Exceptions:
                readExceptions(in);
                break;
            case ATT_MethodParameters:
                readMethodParameters(in);
                break;
            case ATT_RuntimeVisibleParameterAnnotations:
            case ATT_RuntimeInvisibleParameterAnnotations:
                boolean invisible = (attrtag == AttrTag.ATT_RuntimeInvisibleParameterAnnotations);
                ParameterAnnotationData pannots = new ParameterAnnotationData(cls, invisible);
                pannots.read(in);
                if (invisible) {
                    invisibleParameterAnnotations = pannots;
                } else {
                    visibleParameterAnnotations = pannots;
                }
                break;
            case ATT_AnnotationDefault:
                defaultAnnotation = AnnotationElement.readValue(in, cls, false);
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    /**
     * read
     * read and resolve the method data called from ClassData.
     * Precondition: NumFields has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        access = in.readUnsignedShort(); // & MM_METHOD; // Q
        name_cpx = in.readUnsignedShort();
        sig_cpx = in.readUnsignedShort();
        TraceUtils.traceln(2,"MethodData: {modifiers}: " + Modifiers.toString(access, CF_Context.CTX_METHOD),
                            "      MethodData: name[" + name_cpx + "]=" + cls.pool.getString(name_cpx) + " sig[" + sig_cpx + "]=" + cls.pool.getString(sig_cpx));
        // Read the attributes
        readAttributes(in);
    }

    private void readExceptions(DataInputStream in) throws IOException {
        // this is not really a CodeAttr attribute, it's part of the CodeAttr
        int exc_table_len = in.readUnsignedShort();
        TraceUtils.traceln(3,"ExceptionsAttr[" + exc_table_len + "]");
        exc_table = new int[exc_table_len];
        for (int l = 0; l < exc_table_len; l++) {
            int exc = in.readShort();
            TraceUtils.traceln(4,"throws:#" + exc);
            exc_table[l] = exc;
        }
    }

    private void readMethodParameters(DataInputStream in) throws IOException {
        // this is not really a CodeAttr attribute, it's part of the CodeAttr
        int num_params = in.readUnsignedByte();
        TraceUtils.traceln(3,"MethodParametersAttr[" + num_params + "]");
        paramNames = new ArrayList<>(num_params);
        for (int l = 0; l < num_params; l++) {
            short pname_cpx = (short) in.readUnsignedShort();
            int paccess = in.readUnsignedShort();
            TraceUtils.traceln(4,"P[" + l + "] ={ name[" + pname_cpx + "]: " + cls.pool.getString(pname_cpx)
                    + " modifiers [" + paccess + "]: " + Modifiers.toString(paccess, CF_Context.CTX_METHOD) + "}");
            paramNames.add(l, new ParamNameData(pname_cpx, paccess));
        }
    }

    /**
     * printPAnnotations
     * <p>
     * prints the parameter annotations for this method. called from CodeAttr (since JASM
     * code integrates the PAnnotation Syntax inside the method body).
     */
    // This is called from the CodeAttr
    public void printPAnnotations() throws IOException {
        int visSize = 0;
        int invisSize = 0;
        int pNumSize = 0;

        if (visibleParameterAnnotations != null) {
            visSize = visibleParameterAnnotations.numParams();
        }
        if (invisibleParameterAnnotations != null) {
            invisSize = invisibleParameterAnnotations.numParams();
        }
        if (paramNames != null) {
            pNumSize = paramNames.size();
        }

        int maxParams;
        maxParams = (pNumSize > invisSize) ? pNumSize : invisSize;
        maxParams = (visSize > maxParams) ? visSize : maxParams;

        for (int paramNum = 0; paramNum < maxParams; paramNum++) {
            ArrayList<AnnotationData> visAnnots = null;
            if (visibleParameterAnnotations != null && paramNum < visSize) {
                visAnnots = visibleParameterAnnotations.get(paramNum);
            }
            ArrayList<AnnotationData> invisAnnots = null;
            if (invisibleParameterAnnotations != null && paramNum < invisSize) {
                invisAnnots = invisibleParameterAnnotations.get(paramNum);
            }
            ParamNameData pname = (paramNames == null) ? null : paramNames.get(paramNum);

            boolean nullAnnots = ((visAnnots == null) && (invisAnnots == null));
            if (pname != null && pname.name_cpx == 0) {
                pname = null;
            }

            // Print the Param number (header)
            if ((pname != null) || !nullAnnots) {
                out.print("\t" + paramNum + ": ");
            } else {
                continue;
            }

            boolean firstTime = true;

            // Print the Parameter name
            if (pname != null) {
                out.print(Token.PARAM_NAME.parseKey());
                out.print(Token.LBRACE.parseKey());
                out.print(cls.pool.getString(pname.name_cpx));
                out.print(" ");
                out.print(Modifiers.toString(pname.access, CF_Context.CTX_METHOD));
                out.print(Token.RBRACE.parseKey());
                out.print(" ");
            }

            // Print any visible param annotations
            if (visAnnots != null) {
                for (AnnotationData annot : visAnnots) {
                    if (!firstTime) {
                        out.print("\t   ");
                    }
                    annot.print(out, getIndentString());
//                    out.println();
                    firstTime = false;
                }
            }

            // Print any invisible param annotations
            if (invisAnnots != null) {
                for (AnnotationData annot : invisAnnots) {
                    if (!firstTime) {
                        out.print("\t   ");
                    }
                    annot.print(out, getIndentString());
//                    out.println();
                    firstTime = false;
                }
            }

            // Reset the line, if there were parameters
            if ((pname != null) || !nullAnnots) {
                out.println();
            }

        }

    }

    /**
     * Prints the method data to the current output stream. called from ClassData.
     */
    @Override
    public void print() throws IOException {

        printAnnotations(getIndentString());

        out.print(getIndentString() + Modifiers.accessString(access, CF_Context.CTX_METHOD));

        if (isSynthetic) {
            out.print(Token.SYNTHETIC.parseKey() + " ");
        }
        if (isDeprecated) {
            out.print(Token.DEPRECATED.parseKey() + " ");
        }
        out.print(Token.METHODREF.parseKey() + " ");

        if (pr_cpx) {
            // print the CPX method descriptor
            out.print("#" + name_cpx + ":#" + sig_cpx +
                    ((code == null && exc_table == null && defaultAnnotation == null) ? ";" : "") +
                    "\t // " + cls.pool.getName(name_cpx) + ":" + cls.pool.getName(sig_cpx));
        } else {
            out.print(cls.pool.getName(name_cpx) + ":" + cls.pool.getName(sig_cpx) +
                    ((code == null && exc_table == null && defaultAnnotation == null) ? ";" : ""));
        }
        // followed by default annotation
        if (defaultAnnotation != null) {
            out.print(" default { ");
            defaultAnnotation.print(out, getIndentString());
            out.print(" }" + ((code == null && exc_table == null) ? ";" : " "));
        }
        // followed by exception table
        printExceptionTable();

        if (code != null) {
            code.print();
        } else {
            if( exc_table != null ) {
                out.print(';');
            }
            out.println();
        }
    }

    private void printExceptionTable() {
        if (exc_table != null) {
            out.print("\n\tthrows ");
            int len = exc_table.length;
            for (int exceptNum = 0; exceptNum < len; exceptNum++) {
                out.print(cls.pool.getClassName(exc_table[exceptNum]));
                if (exceptNum < len - 1) {
                    out.print(", ");
                }
            }
        }
    }

    /**
     * MethodParamData
     */
    class ParamNameData {

        public int access;
        public int name_cpx;

        public ParamNameData(int name, int access) {
            this.access = access;
            this.name_cpx = name;
        }
    }
} // end MethodData
