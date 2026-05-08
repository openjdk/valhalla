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

import static org.openjdk.asmtools.jasm.RuntimeConstants.DEPRECATED_ATTRIBUTE;
import static org.openjdk.asmtools.jasm.RuntimeConstants.SYNTHETIC_ATTRIBUTE;
import org.openjdk.asmtools.jasm.Tables.AttrTag;
import java.util.ArrayList;

/**
 * The common base structure for field_info, method_info, and component_info
 */
abstract public class MemberData {

    protected int access;
    protected AttrData syntheticAttr, deprecatedAttr;
    protected DataVectorAttr<AnnotationData> annotAttrVis = null;
    protected DataVectorAttr<AnnotationData> annotAttrInv = null;
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrVis = null;
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrInv = null;
    protected ClassData cls;
    protected AttrData signatureAttr;

    public MemberData(ClassData cls, int access) {
        this.cls = cls;
        init(access);
    }

    public MemberData(ClassData cls) {
        this.cls = cls;
    }

    public void init(int access) {
        this.access = access;
    }

    public void createPseudoMod() {
        // If a member has a Pseudo-modifier

        // create the appropriate marker attributes,
        // and clear the PseudoModifiers from the access flags.
        if (Modifiers.isSyntheticPseudoMod(access)) {
            syntheticAttr = new AttrData(cls, AttrTag.ATT_Synthetic.parsekey());
            access &= ~SYNTHETIC_ATTRIBUTE;
        }
        if (Modifiers.isDeprecatedPseudoMod(access)) {
            deprecatedAttr = new AttrData(cls, AttrTag.ATT_Deprecated.parsekey());
            access &= ~DEPRECATED_ATTRIBUTE;
        }
    }

    public void setSignatureAttr(ConstantPool.ConstCell value_cpx) {
        signatureAttr = new CPXAttr(cls, Tables.AttrTag.ATT_Signature.parsekey(), value_cpx);
    }

    protected abstract DataVector getAttrVector();

    protected final DataVector getDataVector(Data... extraAttrs) {
        DataVector attrs = new DataVector();
        for( Data extra : extraAttrs ) {
            if (extra != null) {
                attrs.add(extra);
            }
        }
        // common set for [ FieldData, MethodData, RecordData ]
        if (annotAttrVis != null) {
            attrs.add(annotAttrVis);
        }
        if (annotAttrInv != null) {
            attrs.add(annotAttrInv);
        }
        if (type_annotAttrVis != null) {
            attrs.add(type_annotAttrVis);
        }
        if (type_annotAttrInv != null) {
            attrs.add(type_annotAttrInv);
        }
        return attrs;
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
            } else {
                // Regular Annotations
                if (invisible) {
                    if (annotAttrInv == null) {
                        annotAttrInv = new DataVectorAttr(cls,
                                AttrTag.ATT_RuntimeInvisibleAnnotations.parsekey());
                    }
                    annotAttrInv.add(item);
                } else {
                    if (annotAttrVis == null) {
                        annotAttrVis = new DataVectorAttr(cls,
                                AttrTag.ATT_RuntimeVisibleAnnotations.parsekey());
                    }
                    annotAttrVis.add(item);
                }
            }
        }
    }
}
