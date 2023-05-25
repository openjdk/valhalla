/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

import java.lang.reflect.AccessFlag;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jdk.internal.classfile.ClassModel;
import jdk.internal.classfile.ClassTransform;
import jdk.internal.classfile.CodeModel;
import jdk.internal.classfile.CodeTransform;
import jdk.internal.classfile.FieldModel;
import jdk.internal.classfile.MethodModel;
import jdk.internal.classfile.TypeKind;
import jdk.internal.classfile.instruction.InvokeInstruction;

import jdk.internal.classfile.MethodTransform;
import jdk.internal.classfile.components.ClassPrinter;
import jdk.internal.classfile.components.ClassRemapper;
import jdk.internal.classfile.components.CodeLocalsShifter;
import jdk.internal.classfile.components.CodeRelabeler;
import jdk.internal.classfile.instruction.ReturnInstruction;
import jdk.internal.classfile.instruction.StoreInstruction;

class PackageSnippets {

    void printClass(ClassModel classModel) {
        // @start region="printClass"
        ClassPrinter.toJson(classModel, ClassPrinter.Verbosity.TRACE_ALL, System.out::print);
        // @end
    }

    // @start region="customPrint"
    void customPrint(ClassModel classModel) {
        print(ClassPrinter.toTree(classModel, ClassPrinter.Verbosity.TRACE_ALL));
    }

    void print(ClassPrinter.Node node) {
        switch (node) {
            case ClassPrinter.MapNode mn -> {
                // print map header
                mn.values().forEach(this::print);
            }
            case ClassPrinter.ListNode ln -> {
                // print list header
                ln.forEach(this::print);
            }
            case ClassPrinter.LeafNode n -> {
                // print leaf node
            }
        }
    }
    // @end

    // @start region="printNodesInTest"
    @Test
    void printNodesInTest(ClassModel classModel) {
        var classNode = ClassPrinter.toTree(classModel, ClassPrinter.Verbosity.TRACE_ALL);
        assertContains(classNode, "method name", "myFooMethod");
        assertContains(classNode, "field name", "myBarField");
        assertContains(classNode, "inner class", "MyInnerFooClass");
    }

    void assertContains(ClassPrinter.Node node, ConstantDesc key, ConstantDesc value) {
        if (!node.walk().anyMatch(n -> n instanceof ClassPrinter.LeafNode ln
                               && ln.name().equals(key)
                               && ln.value().equals(value))) {
            node.toYaml(System.out::print);
            throw new AssertionError("expected %s: %s".formatted(key, value));
        }
    }
    // @end
    @interface Test{}

    void singleClassRemap(ClassModel... allMyClasses) {
        // @start region="singleClassRemap"
        var classRemapper = ClassRemapper.of(
                Map.of(ClassDesc.of("Foo"), ClassDesc.of("Bar")));

        for (var classModel : allMyClasses) {
            byte[] newBytes = classRemapper.remapClass(classModel);

        }
        // @end
    }

    void allPackageRemap(ClassModel... allMyClasses) {
        // @start region="allPackageRemap"
        var classRemapper = ClassRemapper.of(cd ->
                ClassDesc.ofDescriptor(cd.descriptorString().replace("Lcom/oldpackage/", "Lcom/newpackage/")));

        for (var classModel : allMyClasses) {
            byte[] newBytes = classRemapper.remapClass(classModel);

        }
        // @end
    }

    void codeLocalsShifting(ClassModel classModel) {
        // @start region="codeLocalsShifting"
        byte[] newBytes = classModel.transform((classBuilder, classElement) -> {
                if (classElement instanceof MethodModel method)
                    classBuilder.transformMethod(method,
                            MethodTransform.transformingCode(
                                    CodeLocalsShifter.of(method.flags(), method.methodTypeSymbol())));
                else
                    classBuilder.accept(classElement);
            });
        // @end
    }

    void codeRelabeling(ClassModel classModel) {
        // @start region="codeRelabeling"
        byte[] newBytes = classModel.transform(
                ClassTransform.transformingMethodBodies(
                        CodeTransform.ofStateful(CodeRelabeler::of)));
        // @end
    }

    // @start region="classInstrumentation"
    byte[] classInstrumentation(ClassModel target, ClassModel instrumentor, Predicate<MethodModel> instrumentedMethodsFilter) {
        var instrumentorCodeMap = instrumentor.methods().stream()
                                              .filter(instrumentedMethodsFilter)
                                              .collect(Collectors.toMap(mm -> mm.methodName().stringValue() + mm.methodType().stringValue(), mm -> mm.code().orElseThrow()));
        var targetFieldNames = target.fields().stream().map(f -> f.fieldName().stringValue()).collect(Collectors.toSet());
        var targetMethods = target.methods().stream().map(m -> m.methodName().stringValue() + m.methodType().stringValue()).collect(Collectors.toSet());
        var instrumentorClassRemapper = ClassRemapper.of(Map.of(instrumentor.thisClass().asSymbol(), target.thisClass().asSymbol()));
        return target.transform(
                ClassTransform.transformingMethods(
                        instrumentedMethodsFilter,
                        (mb, me) -> {
                            if (me instanceof CodeModel targetCodeModel) {
                                var mm = targetCodeModel.parent().get();
                                //instrumented methods code is taken from instrumentor
                                mb.transformCode(instrumentorCodeMap.get(mm.methodName().stringValue() + mm.methodType().stringValue()),
                                        //all references to the instrumentor class are remapped to target class
                                        instrumentorClassRemapper.asCodeTransform()
                                        .andThen((codeBuilder, instrumentorCodeElement) -> {
                                            //all invocations of target methods from instrumentor are inlined
                                            if (instrumentorCodeElement instanceof InvokeInstruction inv
                                                && target.thisClass().asInternalName().equals(inv.owner().asInternalName())
                                                && mm.methodName().stringValue().equals(inv.name().stringValue())
                                                && mm.methodType().stringValue().equals(inv.type().stringValue())) {

                                                //store stacked method parameters into locals
                                                var storeStack = new ArrayDeque<StoreInstruction>();
                                                int slot = 0;
                                                if (!mm.flags().has(AccessFlag.STATIC))
                                                    storeStack.push(StoreInstruction.of(TypeKind.ReferenceType, slot++));
                                                for (var pt : mm.methodTypeSymbol().parameterList()) {
                                                    var tk = TypeKind.from(pt);
                                                    storeStack.push(StoreInstruction.of(tk, slot));
                                                    slot += tk.slotSize();
                                                }
                                                storeStack.forEach(codeBuilder::with);

                                                //inlined target locals must be shifted based on the actual instrumentor locals
                                                codeBuilder.block(inlinedBlockBuilder -> inlinedBlockBuilder
                                                        .transform(targetCodeModel, CodeLocalsShifter.of(mm.flags(), mm.methodTypeSymbol())
                                                        .andThen(CodeRelabeler.of())
                                                        .andThen((innerBuilder, shiftedTargetCode) -> {
                                                            //returns must be replaced with jump to the end of the inlined method
                                                            if (shiftedTargetCode instanceof ReturnInstruction)
                                                                innerBuilder.goto_(inlinedBlockBuilder.breakLabel());
                                                            else
                                                                innerBuilder.with(shiftedTargetCode);
                                                        })));
                                            } else
                                                codeBuilder.with(instrumentorCodeElement);
                                        }));
                            } else
                                mb.with(me);
                        })
                .andThen(ClassTransform.endHandler(clb ->
                    //remaining instrumentor fields and methods are injected at the end
                    clb.transform(instrumentor,
                            ClassTransform.dropping(cle ->
                                    !(cle instanceof FieldModel fm
                                            && !targetFieldNames.contains(fm.fieldName().stringValue()))
                                    && !(cle instanceof MethodModel mm
                                            && !"<init>".equals(mm.methodName().stringValue())
                                            && !targetMethods.contains(mm.methodName().stringValue() + mm.methodType().stringValue())))
                            //and instrumentor class references remapped to target class
                            .andThen(instrumentorClassRemapper)))));
    }
    // @end
}
