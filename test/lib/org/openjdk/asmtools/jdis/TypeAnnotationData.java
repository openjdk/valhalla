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

import org.openjdk.asmtools.jasm.TypeAnnotationTargetInfoData;
import org.openjdk.asmtools.jasm.TypeAnnotationTypePathData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.openjdk.asmtools.jasm.TypeAnnotationTargetInfoData.*;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.*;

/**
 * Type Annotation data is a specific kind of AnnotationData. As well as the normal data
 * items needed to present an annotation, Type annotations require a TargetInfo
 * descriptor. This descriptor is based on a TargetType, and it optionally may contain a
 * location descriptor (when the Type is embedded in a collection).
 * <p>
 * The TypeAnnotationData class is based on JDis's AnnotationData class, and contains the
 * (jasm) class for representing TargetInfo.
 */
public class TypeAnnotationData extends AnnotationData {

    private static TTVis TT_Visitor = new TTVis();
    private TypeAnnotationTargetInfoData targetInfo;
    private TypeAnnotationTypePathData typePath;

    public TypeAnnotationData(boolean invisible, ClassData cls) {
        super(invisible, cls);
        targetInfo = null;
        typePath  = new TypeAnnotationTypePathData();
        visAnnotToken = "@T+";
        invAnnotToken = "@T-";
        dataName = "TypeAnnotationData";
    }

    @Override
    public void read(DataInputStream in) throws IOException {

        int ttype = in.readUnsignedByte();
        ETargetType targetType = ETargetType.getTargetType(ttype);

        if (targetType == null) {
            // Throw some kind of error for bad target type index
            throw new IOException("Bad target type: " + ttype + " in TypeAnnotationData");
        }

        // read the target info
        TT_Visitor.init(in);
        TT_Visitor.visitExcept(targetType);
        targetInfo = TT_Visitor.getTargetInfo();

        // read the target path info
        int len = in.readUnsignedByte();
        TraceUtils.traceln(4,"[TypeAnnotationData.read]: Reading Location (length = " + len + ").");
        TraceUtils.trace(4,"[TypeAnnotationData.read]: [ ");
        for (int i = 0; i < len; i++) {
            int pathType = in.readUnsignedByte();
            String pk = (getPathKind(pathType)).parseKey();
            char pathArgIndex = (char) in.readUnsignedByte();
            typePath.addTypePathEntry(new TypePathEntry(pathType, pathArgIndex));
            TraceUtils.trace(" " + pk + "(" + pathType + "," + pathArgIndex + "), ");
        }
        TraceUtils.traceln("] ");
        super.read(in);
    }

    @Override
    protected void printBody(PrintWriter out, String tab) {
        // For a type annotation, print out brackets,
        // print out the (regular) annotation name/value pairs,
        // then print out the target types.
        out.print(" {");
        super.printBody(out, "");
        targetInfo.print(out, tab);
        typePath.print(out, tab);
        out.print(tab + "}");
    }

    /**
     * TTVis
     * <p>
     * Target Type visitor, used for constructing the target-info within a type
     * annotation. visitExcept() is the entry point. ti is the constructed target info.
     */
    private static class TTVis extends TypeAnnotationTargetVisitor {

        private TypeAnnotationTargetInfoData targetInfo = null;
        private IOException IOProb = null;
        private DataInputStream in;

        public void init(DataInputStream in) {
            this.in = in;
        }

        public int scanByteVal() {
            int val = 0;
            try {
                val = in.readUnsignedByte();
            } catch (IOException e) {
                IOProb = e;
            }
            return val;
        }

        public int scanShortVal() {
            int val = 0;
            try {
                val = in.readUnsignedShort();
            } catch (IOException e) {
                IOProb = e;
            }
            return val;
        }

        //This is the entry point for a visitor that tunnels exceptions
        public void visitExcept(ETargetType tt) throws IOException {
            IOProb = null;
            targetInfo = null;

            TraceUtils.traceln(4,"Target Type: " + tt.parseKey());
            visit(tt);

            if (IOProb != null) {
                throw IOProb;
            }
        }

        public TypeAnnotationTargetInfoData getTargetInfo() {
            return targetInfo;
        }

        private boolean error() {
            return IOProb != null;
        }

        @Override
        public void visit_type_param_target(ETargetType tt) {
            TraceUtils.trace(4,"Type Param Target: ");
            int byteval = scanByteVal(); // param index
            TraceUtils.traceln("{ param_index: " + byteval + "}");
            if (!error()) {
                targetInfo = new type_parameter_target(tt, byteval);
            }
        }

        @Override
        public void visit_supertype_target(ETargetType tt) {
            TraceUtils.trace(4,"SuperType Target: ");
            int shortval = scanShortVal(); // type index
            TraceUtils.traceln("{ type_index: " + shortval + "}");
            if (!error()) {
                targetInfo = new supertype_target(tt, shortval);
            }
        }

        @Override
        public void visit_typeparam_bound_target(ETargetType tt) {
            TraceUtils.trace(4,"TypeParam Bound Target: ");
            int byteval1 = scanByteVal(); // param index
            if (error()) {
                return;
            }
            int byteval2 = scanByteVal(); // bound index
            if (error()) {
                return;
            }
            TraceUtils.traceln("{ param_index: " + byteval1 + " bound_index: " + byteval2 + "}");
            targetInfo = new type_parameter_bound_target(tt, byteval1, byteval2);
        }

        @Override
        public void visit_empty_target(ETargetType tt) {
            TraceUtils.traceln(4,"Empty Target: ");
            if (!error()) {
                targetInfo = new empty_target(tt);
            }
        }

        @Override
        public void visit_methodformalparam_target(ETargetType tt) {
            TraceUtils.trace(4,"MethodFormalParam Target: ");
            int byteval = scanByteVal(); // param index
            TraceUtils.traceln("{ param_index: " + byteval + "}");
            if (!error()) {
                targetInfo = new formal_parameter_target(tt, byteval);
            }
        }

        @Override
        public void visit_throws_target(ETargetType tt) {
            TraceUtils.trace(4,"Throws Target: ");
            int shortval = scanShortVal(); // exception index
            TraceUtils.traceln("{ exception_index: " + shortval + "}");
            if (!error()) {
                targetInfo = new throws_target(tt, shortval);
            }
        }

        @Override
        public void visit_localvar_target(ETargetType tt) {
            TraceUtils.traceln(4,"LocalVar Target: ");
            int tblsize = scanShortVal(); // table length (short)
            if (error()) {
                return;
            }
            localvar_target locvartab = new localvar_target(tt, tblsize);
            targetInfo = locvartab;

            for (int i = 0; i < tblsize; i++) {
                int shortval1 = scanShortVal(); // startPC
                if (error()) {
                    return;
                }
                int shortval2 = scanShortVal(); // length
                if (error()) {
                    return;
                }
                int shortval3 = scanShortVal(); // CPX
                TraceUtils.trace(4,"LocalVar[" + i + "]: ");
                TraceUtils.traceln("{ startPC: " + shortval1 + ", length: " + shortval2 + ", CPX: " + shortval3 + "}");
                locvartab.addEntry(shortval1, shortval2, shortval3);
            }
        }

        @Override
        public void visit_catch_target(ETargetType tt) {
            TraceUtils.trace(4,"Catch Target: ");
            int shortval = scanShortVal(); // catch index
            TraceUtils.traceln("{ catch_index: " + shortval + "}");
            if (!error()) {
                targetInfo = new catch_target(tt, shortval);
            }
        }

        @Override
        public void visit_offset_target(ETargetType tt) {
            TraceUtils.trace(4,"Offset Target: ");
            int shortval = scanShortVal(); // offset index
            TraceUtils.traceln("{ offset_index: " + shortval + "}");
            if (!error()) {
                targetInfo = new offset_target(tt, shortval);
            }
        }

        @Override
        public void visit_typearg_target(ETargetType tt) {
            TraceUtils.trace(4,"TypeArg Target: ");
            int shortval = scanShortVal(); // offset
            if (error()) {
                return;
            }
            int byteval = scanByteVal(); // type index
            if (error()) {
                return;
            }
            TraceUtils.traceln("{ offset: " + shortval + " type_index: " + byteval + "}");
            targetInfo = new type_argument_target(tt, shortval, byteval);
        }
    }
}
