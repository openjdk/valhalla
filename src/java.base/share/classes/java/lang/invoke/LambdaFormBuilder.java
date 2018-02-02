/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.experimental.bytecode.*;
import jdk.experimental.bytecode.MacroCodeBuilder.FieldAccessKind;
import jdk.experimental.bytecode.MacroCodeBuilder.InvocationKind;
import jdk.experimental.value.MethodHandleBuilder;
import sun.invoke.util.VerifyType;
import sun.invoke.util.Wrapper;
import valhalla.shady.MinimalValueTypes_1_0;

import java.lang.invoke.LambdaForm.BasicType;
import java.lang.invoke.LambdaForm.Name;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.BasicType.L_TYPE;
import static java.lang.invoke.LambdaForm.BasicType.V_TYPE;
import static java.lang.invoke.LambdaForm.BasicType.basicType;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_getField;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_getStatic;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_invokeInterface;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_invokeSpecial;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_invokeStatic;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_invokeVirtual;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_putField;
import static java.lang.invoke.MethodHandleNatives.Constants.REF_putStatic;
import static java.lang.invoke.MethodHandleStatics.PROFILE_GWT;
import static java.lang.invoke.MethodHandleStatics.PROFILE_LEVEL;
import static java.lang.invoke.MethodHandleStatics.newInternalError;
import static jdk.experimental.bytecode.MacroCodeBuilder.CondKind.EQ;
import static jdk.experimental.bytecode.MacroCodeBuilder.CondKind.NE;

/**
 * Utility class for spinning classfiles associated with lambda forms.
 */
class LambdaFormBuilder extends MethodHandleBuilder {

    private static final String OBJ     = "java/lang/Object";
    private static final String CLASS_PREFIX   = "java/lang/invoke/LambdaForm$Value$";
    private static final String DEFAULT_CLASS  = "MH";
    private static final String LL_SIG  = "(L" + OBJ + ";)L" + OBJ + ";";
    private static final String LLV_SIG = "(L" + OBJ + ";L" + OBJ + ";)V";

    private static final String MH           = "java/lang/invoke/MethodHandle";
    private static final String MHARY2       = "[[L" + MH + ";";

    static MemberName generateCustomizedCode(LambdaForm form, MethodType invokerType) {
        String invokerName = form.lambdaName();
        int p = invokerName.indexOf('.');
        boolean overrideNames = p != -1;
        String methodName = overrideNames ? invokerName.substring(p + 1) : invokerName;
        String className = overrideNames ?
                CLASS_PREFIX + invokerName.substring(0, p) :
                CLASS_PREFIX + DEFAULT_CLASS;
        if (MinimalValueTypes_1_0.DUMP_CLASS_FILES) {
            // When DUMP_CLASS_FILES is true methodName will have a unique id
            className = className + "_" + methodName;
        }
        return MethodHandleBuilder.loadCode(Lookup.IMPL_LOOKUP.in(LambdaForm.class), className, methodName, invokerType.toMethodDescriptorString(),
                M -> new LambdaFormCodeBuilder(form, invokerType, M), clazz -> InvokerBytecodeGenerator.resolveInvokerMember(clazz, methodName, invokerType),
                C -> new LambdaFormBuilder(C, form).generateLambdaFormBody());
    }

    LambdaFormCodeBuilder builder;
    LambdaForm lambdaForm;

    LambdaFormBuilder(LambdaFormCodeBuilder builder, LambdaForm lambdaForm) {
        this.builder = builder;
        this.lambdaForm = lambdaForm;
    }

    /** Generates code to check that actual receiver and LambdaForm matches */
    private boolean checkActualReceiver() {
        // Expects MethodHandle on the stack and actual receiver MethodHandle in slot #0
        builder.dup().load(0).invokestatic(MethodHandleImpl.class, "assertSame", LLV_SIG, false);
        return true;
    }

    void generateLambdaFormBody() {
        if (lambdaForm.customized != null && MethodHandleBuilder.ENABLE_POOL_PATCHES) {
            // Since LambdaForm is customized for a particular MethodHandle, it's safe to substitute
            // receiver MethodHandle (at slot #0) with an embedded constant and use it instead.
            // It enables more efficient code generation in some situations, since embedded constants
            // are compile-time constants for JIT compiler.
            builder.ldc(lambdaForm.customized)
                   .checkcast(MethodHandle.class);
            assert(checkActualReceiver()); // generates runtime check
            builder.store(0);
        }

        // iterate over the form's names, generating bytecode instructions for each
        // start iterating at the first name following the arguments
        Name onStack = null;
        for (int i = lambdaForm.arity; i < lambdaForm.names.length; i++) {
            Name name = lambdaForm.names[i];

            if (onStack != null && onStack.type != V_TYPE) {
                // non-void: actually assign
                builder.store(fromBasicType(onStack.type), onStack.index());
            }
            onStack = name;  // unless otherwise modified below
            MemberName member = name.function.member();
            MethodHandleImpl.Intrinsic intr = name.function.intrinsicName();
            switch (intr) {
                 case SELECT_ALTERNATIVE: {
                     assert lambdaForm.isSelectAlternative(i);
                     if (PROFILE_GWT) {
                         assert(name.arguments[0] instanceof Name &&
                                 ((Name)name.arguments[0]).refersTo(MethodHandleImpl.class, "profileBoolean"));
                         builder.method().withAnnotation(AnnotationsBuilder.Kind.RUNTIME_VISIBLE, InvokerBytecodeGenerator.INJECTEDPROFILE_SIG);
                     }
                     onStack = emitSelectAlternative(name, lambdaForm.names[i+1]);
                     i++;  // skip MH.invokeBasic of the selectAlternative result
                     continue;

                 }

                case LOOP: {
                    assert lambdaForm.isLoop(i);
                    onStack = emitLoop(i);
                    i += 2; // jump to the end of the LOOP idiom
                    continue;
                }
                case IDENTITY: {
                    assert (name.arguments.length == 1);
                    builder.pushArguments(name, 0);
                    continue;
                }
                case ZERO: {
                    assert (name.arguments.length == 0);
                    assert (name.type != BasicType.Q_TYPE);
                    builder.ldc(name.type.basicTypeWrapper().zero());
                    continue;
                }
                // TODO: case GUARD_WITH_CATCH:
                // TODO: case TRY_FINALLY:
                // TODO: case NEW_ARRAY:
                // TODO: case ARRAY_LOAD:
                // TODO: case ARRAY_STORE:
                // TODO: case ARRAY_LENGTH:
            }
            if (InvokerBytecodeGenerator.isStaticallyInvocable(member)) {
                builder.invokeStaticName(member, name);
            } else {
                builder.invokeName(name);
            }
        }
        builder.return_(onStack);
    }

    private Name emitLoop(int pos) {
        Name args    = lambdaForm.names[pos];
        Name invoker = lambdaForm.names[pos+1];
        Name result  = lambdaForm.names[pos+2];

        // extract clause and loop-local state types
        // find the type info in the loop invocation
        BasicType[] loopClauseTypes = (BasicType[]) invoker.arguments[0];
        Class<?>[] loopLocalStateTypes = Stream.of(loopClauseTypes).
                filter(bt -> bt != BasicType.V_TYPE).map(BasicType::basicTypeClass).toArray(Class<?>[]::new);

        Class<?>[] localTypes = new Class<?>[loopLocalStateTypes.length + 1];
        localTypes[0] = MethodHandleImpl.LoopClauses.class;
        System.arraycopy(loopLocalStateTypes, 0, localTypes, 1, loopLocalStateTypes.length);

        final int clauseDataIndex     = builder.extendLocalsMap(localTypes);
        final int firstLoopStateIndex = clauseDataIndex + 1;

        Class<?> returnType = result.function.resolvedHandle().type().returnType();
        MethodType loopType = args.function.resolvedHandle().type()
                .dropParameterTypes(0,1)
                .changeReturnType(returnType);
        MethodType loopHandleType = loopType.insertParameterTypes(0, loopLocalStateTypes);
        MethodType predType = loopHandleType.changeReturnType(boolean.class);
        MethodType finiType = loopHandleType;

        final int nClauses = loopClauseTypes.length;

        // indices to invoker arguments to load method handle arrays
        final int inits = 1;
        final int steps = 2;
        final int preds = 3;
        final int finis = 4;

        // PREINIT:
        builder.pushArgument(MethodHandleImpl.LoopClauses.class, invoker.arguments[1])
                .getfield(MethodHandleImpl.LoopClauses.class, "clauses", MHARY2)
                .store(TypeTag.A, clauseDataIndex);

        // INIT:
        for (int c = 0, state = 0; c < nClauses; ++c) {
            MethodType cInitType = loopType.changeReturnType(loopClauseTypes[c].basicTypeClass());
            builder.invokeLoopHandle(inits, c, args, false, cInitType, loopLocalStateTypes, clauseDataIndex, firstLoopStateIndex);
            if (cInitType.returnType() != void.class) {
                builder.store(fromClass(cInitType.returnType()), firstLoopStateIndex + state);
                ++state;
            }
        }

        // LOOP:
        String loopLabel = builder.label();
        String doneLabel = builder.label();
        builder.label(loopLabel);

        String val = null;
        for (int c = 0, s = 0; c < nClauses; ++c) {
            MethodType stepType = loopHandleType.changeReturnType(loopClauseTypes[c].basicTypeClass());
            boolean isVoid = (stepType.returnType() == void.class);

            // invoke loop step
            builder.invokeLoopHandle(steps, c, args, true, stepType, loopLocalStateTypes, clauseDataIndex, firstLoopStateIndex);
            if (!isVoid) {
                builder.store(fromClass(stepType.returnType()), firstLoopStateIndex + s);
                ++s;
            }

            String nextLabel = builder.label();

            // invoke loop predicate
            builder.invokeLoopHandle(preds, c, args, true, predType, loopLocalStateTypes, clauseDataIndex, firstLoopStateIndex)
                    .emitCondJump(Opcode.IFEQ, NE, nextLabel)
                    .invokeLoopHandle(finis, c, args, true, finiType, loopLocalStateTypes, clauseDataIndex, firstLoopStateIndex)
                    .goto_(doneLabel)
                    .label(nextLabel);
        }
        builder.goto_(loopLabel)
                .label(doneLabel);

        return result;
    }

    /**
     * Emit bytecode for the selectAlternative idiom.
     *
     * The pattern looks like (Cf. MethodHandleImpl.makeGuardWithTest):
     * <blockquote><pre>{@code
     *   Lambda(a0:L,a1:I)=>{
     *     t2:I=foo.test(a1:I);
     *     t3:L=MethodHandleImpl.selectAlternative(t2:I,(MethodHandle(int)int),(MethodHandle(int)int));
     *     t4:I=MethodHandle.invokeBasic(t3:L,a1:I);t4:I}
     * }</pre></blockquote>
     */
    private Name emitSelectAlternative(Name selectAlternativeName, Name invokeBasicName) {
        assert InvokerBytecodeGenerator.isStaticallyInvocable(invokeBasicName);

        Name receiver = (Name) invokeBasicName.arguments[0];

        String fallbackLabel = builder.label();
        String doneLabel = builder.label();

        builder.pushArgument(selectAlternativeName, 0) // load test result
                .emitCondJump(Opcode.IFEQ, EQ, fallbackLabel) // if_icmp L_fallback
                .pushArgument(selectAlternativeName, 1)  // get 2nd argument of selectAlternative
                .store(TypeTag.A, receiver.index())  // store the MH in the receiver slot
                .invokeStaticName(invokeBasicName) // invoke selectAlternativeName.arguments[1]
                .goto_(doneLabel)
                .label(fallbackLabel)
                .pushArgument(selectAlternativeName, 2)  // get 3rd argument of selectAlternative
                .store(TypeTag.A, receiver.index())  // store the MH in the receiver slot
                .invokeStaticName(invokeBasicName) // invoke selectAlternativeName.arguments[2]
                .label(doneLabel);

        return invokeBasicName;  // return what's on stack
    }

    static class LambdaFormCodeBuilder extends MethodHandleCodeBuilder<LambdaFormCodeBuilder> {

        LambdaForm lambdaForm;
        MethodType invokerType;
        int maxLocals;
        int[] localsMap;
        private MethodBuilder<Class<?>, String, byte[]> methodBuilder;
        private int labelCount = 0;

        public LambdaFormCodeBuilder(LambdaForm form, MethodType invokerType, MethodBuilder<Class<?>, String, byte[]> methodBuilder) {
            super(methodBuilder);
            if (form.forceInline) {
                methodBuilder.withAnnotation(AnnotationsBuilder.Kind.RUNTIME_VISIBLE, "Ljdk/internal/vm/annotation/ForceInline;");
            }
            methodBuilder.withAnnotation(AnnotationsBuilder.Kind.RUNTIME_VISIBLE, "Ljava/lang/invoke/LambdaForm$Hidden;")
                    .withAnnotation(AnnotationsBuilder.Kind.RUNTIME_VISIBLE, "Ljava/lang/invoke/LambdaForm$Compiled;");
            this.lambdaForm = form;
            this.invokerType = invokerType;
            this.maxLocals = form.names.length;
            this.localsMap = computeLocalsMap(form);
            this.methodBuilder = methodBuilder;
        }

        static int[] computeLocalsMap(LambdaForm lform) {
            int localsMapSize = lform.names.length;
            int[] localsMap = new int[localsMapSize+1]; // last entry of localsMap is count of allocated local slots
            for (int i = 0, index = 0; i < localsMap.length; i++) {
                localsMap[i] = index;
                if (i < lform.names.length) {
                    BasicType type = lform.names[i].type();
                    index += type.basicTypeSlots();
                }
            }
            return localsMap;
        }

        @Override
        public LambdaFormCodeBuilder load(int index) {
            return load(tagOfLocal(index), index);
        }

        @Override
        public LambdaFormCodeBuilder store(int index) {
            return store(tagOfLocal(index), index);
        }

        @Override
        public LambdaFormCodeBuilder load(TypeTag type, int n) {
            return super.load(type, localsMap[n]);
        }

        @Override
        public LambdaFormCodeBuilder store(TypeTag type, int n) {
            return super.store(type, localsMap[n]);
        }

        /**
         * Emits a return statement from a LF invoker. If required, the result type is cast to the correct return type.
         */
        private void return_(Name onStack) {
            // return statement
            Class<?> rclass = invokerType.returnType();
            BasicType rtype = lambdaForm.returnType();
            assert(rtype == basicType(rclass));  // must agree
            if (rtype == V_TYPE) {
                // void
                return_();
                // it doesn't matter what rclass is; the JVM will discard any value
            } else {
                LambdaForm.Name rn = lambdaForm.names[lambdaForm.result];

                // put return value on the stack if it is not already there
                if (rn != onStack) {
                    load(lambdaForm.result);
                }

                coerce(rtype, rclass, rn);

                // generate actual return statement
                return_(fromBasicType(rtype));
            }
        }

        /**
         * Emit an implicit conversion for an argument which must be of the given pclass.
         * This is usually a no-op, except when pclass is a subword type or a reference other than Object or an interface.
         *
         * @param ptype type of value present on stack
         * @param pclass type of value required on stack
         * @param arg compile-time representation of value on stack (Node, constant) or null if none
         */
        private LambdaFormCodeBuilder coerce(BasicType ptype, Class<?> pclass, Object arg) {
            assert(basicType(pclass) == ptype);  // boxing/unboxing handled by caller
            if (pclass == ptype.basicTypeClass() && ptype != L_TYPE)
                return this;   // nothing to do
            switch (ptype) {
                case L_TYPE:
                    if (VerifyType.isNullConversion(Object.class, pclass, false)) {
                        if (PROFILE_LEVEL > 0)
                            coerce(Object.class, arg);
                        return this;
                    }
                    coerce(pclass, arg);
                    return this;
                case I_TYPE:
                    if (!VerifyType.isNullConversion(int.class, pclass, false))
                        conv(fromBasicType(ptype), fromBasicType(BasicType.basicType(pclass)));
                    return this;
                case Q_TYPE:
                    if (!MinimalValueTypes_1_0.isValueType(pclass)) {
                        vbox(Object.class);
                        return this;
                    }
                    assert pclass == arg.getClass();
                    return this; //assume they are equal
            }
            throw newInternalError("bad implicit conversion: tc="+ptype+": "+pclass);
        }

        private LambdaFormCodeBuilder coerce(Class<?> cls, Object arg) {
            Name writeBack = null;  // local to write back result
            if (arg instanceof Name) {
                Name n = (Name) arg;
                if (cls.isAssignableFrom(typeOfLocal(n.index())))
                    return this;  // this cast was already performed
                if (lambdaForm.useCount(n) > 1) {
                    // This guy gets used more than once.
                    writeBack = n;
                }
            }
            if (InvokerBytecodeGenerator.isStaticallyNameable(cls)) {
                checkcast(cls);
            } else {
                ldc(cls)
                    .checkcast(Class.class)
                    .swap()
                    .invokevirtual(Class.class, "cast", LL_SIG, false);
                if (Object[].class.isAssignableFrom(cls))
                    checkcast(Object[].class);
                else if (PROFILE_LEVEL > 0)
                    checkcast(Object.class);
            }
            if (writeBack != null) {
                dup().store(TypeTag.A, writeBack.index());
            }
            return this;
        }

        LambdaFormCodeBuilder invokeStaticName(Name name) {
            return invokeStaticName(name.function.member(), name);
        }

        /**
         * Emit an invoke for the given name, using the MemberName directly.
         */
        LambdaFormCodeBuilder invokeStaticName(MemberName member, Name name) {
            assert(member.equals(name.function.member()));
            Class<?> defc = member.getDeclaringClass();
            String mname = member.getName();
            String mtype;
            byte refKind = member.getReferenceKind();
            if (refKind == REF_invokeSpecial) {
                // in order to pass the verifier, we need to convert this to invokevirtual in all cases
                assert(member.canBeStaticallyBound()) : member;
                refKind = REF_invokeVirtual;
            }

            assert(!(member.getDeclaringClass().isInterface() && refKind == REF_invokeVirtual));

            // push arguments
            pushArguments(name);

            // invocation
            if (member.isMethod()) {
                mtype = member.getMethodType().toMethodDescriptorString();
                invoke(invKindFromRefKind(refKind), defc, mname, mtype,
                        member.getDeclaringClass().isInterface());
            } else {
                mtype = MethodType.toFieldDescriptorString(member.getFieldType());
                getfield(fieldKindFromRefKind(refKind), defc, mname, mtype);
            }
            // Issue a type assertion for the result, so we can avoid casts later.
            if (name.type == L_TYPE) {
                Class<?> rtype = member.getInvocationType().returnType();
                assert(!rtype.isPrimitive());
            }
            return this;
        }

        /**
         * Emit an invoke for the given name.
         */
        LambdaFormCodeBuilder invokeName(Name name) {
            //assert(!isLinkerMethodInvoke(name));  // should use the static path for these
            if (true) {
                // push receiver
                MethodHandle target = name.function.resolvedHandle();
                assert(target != null) : name.exprString();
                ldc(target);
                coerce(MethodHandle.class, target);
            }

            // push arguments
            pushArguments(name);

            // invocation
            MethodType type = name.function.methodType();
            invokevirtual(MethodHandle.class, "invokeBasic", type.basicType().toMethodDescriptorString(), false);
            return this;
        }

        private LambdaFormCodeBuilder pushArguments(Name args) {
            return pushArguments(args, 0);
        }

        private LambdaFormCodeBuilder pushArguments(Name args, int start) {
            for (int i = start; i < args.arguments.length; i++) {
                pushArgument(args, i);
            }
            return this;
        }

        private LambdaFormCodeBuilder pushArgument(Name name, int paramIndex) {
            Object arg = name.arguments[paramIndex];
            Class<?> ptype = name.function.methodType().parameterType(paramIndex);
            return pushArgument(ptype, arg);
        }

        private LambdaFormCodeBuilder pushArgument(Class<?> ptype, Object arg) {
            BasicType bptype = basicType(ptype);
            if (arg instanceof Name) {
                Name n = (Name) arg;
                load(fromBasicType(n.type), n.index());
                coerce(n.type, ptype, n);
            } else if ((arg == null || arg instanceof String) && bptype == L_TYPE) {
                ldc(arg);
            } else {
                if (Wrapper.isWrapperType(arg.getClass()) && bptype != L_TYPE) {
                    ldc(arg);
                } else {
                    ldc(arg);
                    coerce(L_TYPE, ptype, arg);
                }
            }
            return this;
        }

        private LambdaFormCodeBuilder invokeLoopHandle(int handles, int clause, Name args, boolean pushLocalState,
                                          MethodType type, Class<?>[] loopLocalStateTypes, int clauseDataSlot,
                                          int firstLoopStateSlot) {
            // load handle for clause
            load(TypeTag.A, clauseDataSlot)
                .ldc(handles - 1)
                .aaload()
                .ldc(clause)
                .aaload();

            // load loop state (preceding the other arguments)
            if (pushLocalState) {
                for (int s = 0; s < loopLocalStateTypes.length; ++s) {
                    load(fromClass(loopLocalStateTypes[s]), firstLoopStateSlot + s);
                }
            }
            // load loop args (skip 0: method handle)
            return pushArguments(args, 1)
                    .invokevirtual(MethodHandle.class, "invokeBasic", type.toMethodDescriptorString(), false);
        }

        MethodBuilder<Class<?>, String, byte[]> method() {
            return methodBuilder;
        }

        String label() {
            return "label" + labelCount++;
        }

        private int extendLocalsMap(Class<?>[] types) {
            int firstSlot = localsMap.length - 1;
            localsMap = Arrays.copyOf(localsMap, localsMap.length + types.length);
            int index = localsMap[firstSlot - 1] + 1;
            int lastSlots = 0;
            for (int i = 0; i < types.length; ++i) {
                localsMap[firstSlot + i] = index;
                lastSlots = BasicType.basicType(types[i]).basicTypeSlots();
                index += lastSlots;
            }
            localsMap[localsMap.length - 1] = index - lastSlots;
            maxLocals = types.length;
            return firstSlot;
        }

        Class<?> typeOfLocal(int index) {
            return typeHelper.symbol(descOfLocal(index));
        }

        TypeTag tagOfLocal(int index) {
            return typeHelper.tag(descOfLocal(index));
        }

        String descOfLocal(int index) {
            return state.locals.get(localsMap[index]);
        }
    }

    /*** Utility methods ***/

    static TypeTag fromBasicType(BasicType type) {
        switch (type) {
            case I_TYPE:  return TypeTag.I;
            case J_TYPE:  return TypeTag.J;
            case F_TYPE:  return TypeTag.F;
            case D_TYPE:  return TypeTag.D;
            case L_TYPE:  return TypeTag.A;
            case V_TYPE:  return TypeTag.V;
            case Q_TYPE:  return TypeTag.Q;
            default:
                throw new InternalError("unknown type: " + type);
        }
    }

    static InvocationKind invKindFromRefKind(int refKind) {
        switch (refKind) {
            case REF_invokeVirtual:      return InvocationKind.INVOKEVIRTUAL;
            case REF_invokeStatic:       return InvocationKind.INVOKESTATIC;
            case REF_invokeSpecial:      return InvocationKind.INVOKESPECIAL;
            case REF_invokeInterface:    return InvocationKind.INVOKEINTERFACE;
        }
        throw new InternalError("refKind="+refKind);
    }

    static FieldAccessKind fieldKindFromRefKind(int refKind) {
        switch (refKind) {
            case REF_getField:
            case REF_putField:            return FieldAccessKind.INSTANCE;
            case REF_getStatic:
            case REF_putStatic:          return FieldAccessKind.STATIC;
        }
        throw new InternalError("refKind="+refKind);
    }

    static TypeTag fromClass(Class<?> cls) {
        return fromBasicType(BasicType.basicType(cls));
    }
}
