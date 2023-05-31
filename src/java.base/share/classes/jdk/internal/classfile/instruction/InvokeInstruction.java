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
package jdk.internal.classfile.instruction;

import java.lang.constant.MethodTypeDesc;

import jdk.internal.classfile.CodeElement;
import jdk.internal.classfile.CodeModel;
import jdk.internal.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.Instruction;
import jdk.internal.classfile.Opcode;
import jdk.internal.classfile.constantpool.InterfaceMethodRefEntry;
import jdk.internal.classfile.constantpool.MemberRefEntry;
import jdk.internal.classfile.constantpool.MethodRefEntry;
import jdk.internal.classfile.constantpool.NameAndTypeEntry;
import jdk.internal.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.AbstractInstruction;
import jdk.internal.classfile.impl.AbstractPoolEntry;
import jdk.internal.classfile.impl.TemporaryConstantPool;
import jdk.internal.classfile.impl.Util;

/**
 * Models a method invocation instruction in the {@code code} array of a {@code
 * Code} attribute, other than {@code invokedynamic}.  Corresponding opcodes
 * will have a {@code kind} of {@link Opcode.Kind#INVOKE}.  Delivered as a
 * {@link CodeElement} when traversing the elements of a {@link CodeModel}.
 */
public sealed interface InvokeInstruction extends Instruction
        permits AbstractInstruction.BoundInvokeInterfaceInstruction, AbstractInstruction.BoundInvokeInstruction, AbstractInstruction.UnboundInvokeInstruction {
    /**
     * {@return the {@link MethodRefEntry} or {@link InterfaceMethodRefEntry}
     * constant described by this instruction}
     */
    MemberRefEntry method();

    /**
     * {@return whether the class holding the method is an interface}
     */
    boolean isInterface();

    /**
     * @return for an {@code invokeinterface}, the {@code count} value, as defined in {@jvms 6.5}
     */
    int count();

    /**
     * {@return the class holding the method}
     */
    default ClassEntry owner() {
        return method().owner();
    }

    /**
     * {@return the name of the method}
     */
    default Utf8Entry name() {
        return method().nameAndType().name();
    }

    /**
     * {@return the method descriptor of the method}
     */
    default Utf8Entry type() {
        return method().nameAndType().type();
    }

    /**
     * {@return a symbolic descriptor for the method type}
     */
    default MethodTypeDesc typeSymbol() {
        return Util.methodTypeSymbol(method().nameAndType());
    }


    /**
     * {@return an invocation instruction}
     *
     * @param op the opcode for the specific type of invocation instruction,
     *           which must be of kind {@link Opcode.Kind#INVOKE}
     * @param method a constant pool entry describing the method
     */
    static InvokeInstruction of(Opcode op, MemberRefEntry method) {
        Util.checkKind(op, Opcode.Kind.INVOKE);
        return new AbstractInstruction.UnboundInvokeInstruction(op, method);
    }

    /**
     * {@return an invocation instruction}
     *
     * @param op the opcode for the specific type of invocation instruction,
     *           which must be of kind {@link Opcode.Kind#INVOKE}
     * @param owner the class holding the method
     * @param name the name of the method
     * @param type the method descriptor
     * @param isInterface whether the class holding the method is an interface
     */
    static InvokeInstruction of(Opcode op,
                                ClassEntry owner,
                                Utf8Entry name,
                                Utf8Entry type,
                                boolean isInterface) {
        return of(op, owner, TemporaryConstantPool.INSTANCE.nameAndTypeEntry(name, type), isInterface);
    }

    /**
     * {@return an invocation instruction}
     *
     * @param op the opcode for the specific type of invocation instruction,
     *           which must be of kind {@link Opcode.Kind#INVOKE}
     * @param owner the class holding the method
     * @param nameAndType the name and type of the method
     * @param isInterface whether the class holding the method is an interface
     */
    static InvokeInstruction of(Opcode op,
                                ClassEntry owner,
                                NameAndTypeEntry nameAndType,
                                boolean isInterface) {
        return of(op, isInterface
                      ? TemporaryConstantPool.INSTANCE.interfaceMethodRefEntry(owner, nameAndType)
                      : TemporaryConstantPool.INSTANCE.methodRefEntry(owner, nameAndType));
    }
}
