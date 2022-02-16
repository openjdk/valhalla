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

import org.openjdk.asmtools.jasm.Tables;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Base class for ClassData, MethodData, FieldData and RecordData(JEP 360)
 */
public abstract class MemberData extends Indenter {

    // access flags (modifiers)
    protected int access;

    // flags
    protected boolean isSynthetic = false;
    protected boolean isDeprecated = false;

    // Signature can be located in ClassFile, field_info, method_info, and component_info
    protected SignatureData signature;

    /**
     * The visible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<AnnotationData> visibleAnnotations;

    /**
     * The invisible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<AnnotationData> invisibleAnnotations;

    /**
     * The visible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<TypeAnnotationData> visibleTypeAnnotations;

    /**
     * The invisible annotations for this class, member( field or method) or record component
     */
    protected ArrayList<TypeAnnotationData> invisibleTypeAnnotations;

    /**
     * The remaining attributes of this class, member( field or method) or record component
     */
    protected ArrayList<AttrData> attrs;

    // internal references
    protected final Options options = Options.OptionObject();
    protected final  boolean pr_cpx = options.contains(Options.PR.CPX);;
    protected ClassData cls;
    protected PrintWriter out;
    protected String memberType = "";

    public MemberData(ClassData cls) {
        this();
        init(cls);
    }

    public MemberData() {
    }

    public void init(ClassData cls) {
        this.out = cls.out;
        this.cls = cls;
    }

    protected boolean handleAttributes(DataInputStream in, Tables.AttrTag attrtag, int attrlen) throws IOException {
        // sub-classes override
        return false;
    }

    protected abstract void print() throws IOException;

    final protected int getAnnotationsCount() {
        return  ((visibleAnnotations == null) ? 0 : visibleAnnotations.size()) +
                ((invisibleAnnotations == null) ? 0 : invisibleAnnotations.size()) +
                ((visibleTypeAnnotations == null) ? 0 : visibleTypeAnnotations.size()) +
                ((invisibleTypeAnnotations == null) ? 0 : invisibleTypeAnnotations.size());

    }

    final protected void printAnnotations(String initialTab) {
        if( getAnnotationsCount() > 0 ) {
            if (visibleAnnotations != null) {
                for (AnnotationData visad : visibleAnnotations) {
                    // out.print(initialTab);
                    visad.print(out, initialTab);
                    out.println();
                }
            }
            if (invisibleAnnotations != null) {
                for (AnnotationData invisad : invisibleAnnotations) {
                    invisad.print(out, initialTab);
                    out.println();
                }
            }

            if (visibleTypeAnnotations != null) {
                for (TypeAnnotationData visad : visibleTypeAnnotations) {
                    visad.print(out, initialTab);
                    out.println();
                }
            }
            if (invisibleTypeAnnotations != null) {
                for (TypeAnnotationData invisad : invisibleTypeAnnotations) {
                    invisad.print(out, initialTab);
                    out.println();
                }
            }
        }
    }

    protected void printVar(StringBuilder bodyPrefix, StringBuilder tailPrefix, int name_cpx, int type_cpx) {
        if( pr_cpx ) {
            bodyPrefix.append('#').append(name_cpx).append(":#").append(type_cpx);
            tailPrefix.append(";\t // ").append(cls.pool.getName(name_cpx)).append(':').append(cls.pool.getName(type_cpx));

        } else {
            bodyPrefix.append(cls.pool.getName(name_cpx)).append(':').append(cls.pool.getName(type_cpx));
            tailPrefix.append(';');
        }

        if (signature != null) {
            signature.print(bodyPrefix.append(':').toString(), tailPrefix.append( pr_cpx ? ":" : "" ).toString());
        } else {
            out.print(bodyPrefix);
            out.print(tailPrefix);
        }
        out.println();
    }

    protected void readAttributes(DataInputStream in) throws IOException {
        // Read the Attributes
        int natt = in.readUnsignedShort();
        attrs = new ArrayList<>(natt);
        TraceUtils.traceln(format("%s - Attributes[%d]", memberType , natt));
        AttrData attr;
        for (int k = 0; k < natt; k++) {
            int name_cpx = in.readUnsignedShort();
            attr = new AttrData(cls);
            attrs.add(attr);
            String attr_name = cls.pool.getString(name_cpx);
            TraceUtils.traceln(format("   #%d name[%d]=\"%s\"", k, name_cpx, attr_name));
            Tables.AttrTag tag = Tables.attrtag(attr_name);
            int attrlen = in.readInt();
            switch (tag) {
                case ATT_Synthetic:
                    // Read Synthetic Attr
                    if (attrlen != 0) {
                        throw new ClassFormatError("invalid Synthetic attr length");
                    }
                    isSynthetic = true;
                    break;
                case ATT_Deprecated:
                    // Read Deprecated Attr
                    if (attrlen != 0) {
                        throw new ClassFormatError("invalid Deprecated attr length");
                    }
                    isDeprecated = true;
                    break;
                case ATT_RuntimeVisibleAnnotations:
                case ATT_RuntimeInvisibleAnnotations:
                    // Read Annotations Attr
                    int cnt = in.readShort();
                    ArrayList<AnnotationData> annots = new ArrayList<>(cnt);
                    boolean invisible = (tag == Tables.AttrTag.ATT_RuntimeInvisibleAnnotations);
                    for (int i = 0; i < cnt; i++) {
                        TraceUtils.traceln("      AnnotationData: #" + i);
                        AnnotationData annot = new AnnotationData(invisible, cls);
                        annot.read(in);
                        annots.add(annot);
                    }

                    if (invisible) {
                        invisibleAnnotations = annots;
                    } else {
                        visibleAnnotations = annots;
                    }
                    break;
                case ATT_RuntimeVisibleTypeAnnotations:
                case ATT_RuntimeInvisibleTypeAnnotations:
                    // Read Type Annotations Attr
                    int tcnt = in.readShort();
                    ArrayList<TypeAnnotationData> tannots = new ArrayList<>(tcnt);
                    boolean tinvisible = (tag == Tables.AttrTag.ATT_RuntimeInvisibleTypeAnnotations);
                    for (int tindex = 0; tindex < tcnt; tindex++) {
                        TraceUtils.traceln("      TypeAnnotationData: #" + tindex);
                        TypeAnnotationData tannot = new TypeAnnotationData(tinvisible, cls);
                        tannot.read(in);
                        tannots.add(tannot);
                    }

                    if (tinvisible) {
                        invisibleTypeAnnotations = tannots;
                    } else {
                        visibleTypeAnnotations = tannots;
                    }
                    break;
                default:
                    boolean handled = handleAttributes(in, tag, attrlen);
                    if (!handled) {
                        attr.read(name_cpx, attrlen, in);
                    }
                    break;

            }
        }
    }
}
