/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package valhalla.shady;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jdk.experimental.bytecode.AnnotationsBuilder.Kind;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.Opcode;
import jdk.experimental.value.MethodHandleBuilder.IsolatedMethodBuilder;
import jdk.experimental.value.MethodHandleBuilder.MethodHandleCodeBuilder;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.Wrapper;
import valhalla.shady.ValueTypeHolder.ValueHandleKind.ValueHandleKey;

// Rough place holder just now...
public class ValueTypeHolder<T> {

    enum ValueHandleKind {
        BOX("box"),
        UNBOX("unbox"),
        DEFAULT("defaultValueConstant"),
        EQ("substitutabilityTest"),
        HASH("substitutabilityHashCode"),
        ARRAYLENGTH("arrayLength"),
        WITHER("findWither", Lookup.class, String.class, Class.class) {
            @Override
            ValueHandleKey key(Object fieldName) {
               return new ValueHandleKey(this, fieldName);
            }
        },
        UNREFLECT_WITHERS("unreflectWithers", Lookup.class, boolean.class, Field[].class) {
            @Override
            ValueHandleKey key(Object fields) {
                return new ValueHandleKey(this, fields);
            }
        },
        NEWARRAY("newArray"),
        VALOAD("arrayGetter"),
        VASTORE("arraySetter"),
        MULTINEWARRAY("newMultiArray", int.class) {
            @Override
            ValueHandleKey key(Object dims) {
               return new ValueHandleKey(this, dims);
            }
        },
        IDENTITY("identity"),
        GETTER("findGetter", Lookup.class, String.class, Class.class) {
            @Override
            ValueHandleKey key(Object fieldName) {
               return new ValueHandleKey(this, fieldName);
            }
        };

        final MethodHandle handle;

        ValueHandleKind(String handleName, Class<?>... argtypes) {
            try {
                this.handle = MethodHandles.lookup().findVirtual(ValueTypeHolder.class, handleName, MethodType.methodType(MethodHandle.class, argtypes));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Cannot initialize value handle key for: " + handleName);
            }
        }

        String handleName() {
            return MethodHandles.lookup().revealDirect(handle).getName();
        }

        MethodType handleType() {
            return MethodHandles.lookup().revealDirect(handle).getMethodType();
        }

        ValueHandleKey key() {
            return new ValueHandleKey(this, null);
        }

        ValueHandleKey key(Object optArg) {
            throw new IllegalStateException();
        }

        static class ValueHandleKey {
            ValueHandleKind kind;
            Object optArg;

            ValueHandleKey(ValueHandleKind kind, Object optArg) {
                this.kind = kind;
                this.optArg = optArg;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof ValueHandleKey) {
                    ValueHandleKey that = (ValueHandleKey)obj;
                    return Objects.equals(kind, that.kind) &&
                            Objects.equals(optArg, that.optArg);
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(kind) * 31 + Objects.hashCode(optArg);
            }
        }
    }

    private static final Lookup IMPL_LOOKUP;

    static {
        try {
            Field f = Lookup.class.getDeclaredField("IMPL_LOOKUP");
            f.setAccessible(true);
            IMPL_LOOKUP = (Lookup)f.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    public static <T> Class<T> makeValueTypeClass(Lookup lookup, String name, String[] fieldNames, Class<?>... fieldTypes) throws ReflectiveOperationException {
        if (fieldNames.length != fieldTypes.length) {
            throw new IllegalArgumentException("Field names length and field types length must match");
        }
        if (!(fieldNames.length > 0)) {
            throw new IllegalArgumentException("Field length must be greater than zero");
        }
        IsolatedMethodBuilder builder = new IsolatedMethodBuilder(name, lookup);
        builder.withMajorVersion(53)
               .withMinorVersion(0)
               .withSuperclass(Object.class)
               .withFlags(Flag.ACC_FINAL)
               .withAnnotation(Kind.RUNTIME_VISIBLE, MinimalValueTypes_1_0.DERIVE_VALUE_TYPE_DESC);
        //add fields
        for (int i = 0 ; i < fieldNames.length ; i++) {
            builder.withField(fieldNames[i], BytecodeDescriptor.unparse(fieldTypes[i]), F -> F.withFlags(Flag.ACC_FINAL));
        }
        //add constructor
        String ctype = BytecodeDescriptor.unparseMethod(void.class, fieldTypes);
        builder.withMethod("<init>", ctype, M -> M.withCode(MethodHandleCodeBuilder::new, C -> {
                C.aload_0().invokespecial(Object.class, "<init>", "()V", false);
                int l = 1;
                for (int i = 0 ; i < fieldNames.length ; i++) {
                    String fType = BytecodeDescriptor.unparse(fieldTypes[i]);
                    C.aload_0().load(l).putfield(builder.thisClass(), fieldNames[i], fType);
                    l += Wrapper.forBasicType(fieldTypes[i]).stackSlots();
                }
                C.return_();
        }));
        //add equals and hashCode
        builder.withMethod("equals", "(Ljava/lang/Object;)Z", M ->
            M.withFlags(Flag.ACC_PUBLIC).withCode(MethodHandleCodeBuilder::new,
                    C -> substitutabilityTestBuilder(true, builder.thisClass(), FieldInfo.stream(fieldNames, fieldTypes), C)));
        builder.withMethod("hashCode", "()I", M ->
            M.withFlags(Flag.ACC_PUBLIC).withCode(MethodHandleCodeBuilder::new,
                    C -> substitutabilityHashCodeBuilder(builder.thisClass(), FieldInfo.stream(fieldNames, fieldTypes), C)));
        byte[] barr = builder.build();
        MinimalValueTypes_1_0.maybeDump(name, barr);
        @SuppressWarnings("unchecked")
        Class<T> vtClass = (Class<T>)lookup.defineClass(barr);
        return vtClass;
    }

    private Lookup boxLookup;
    private Lookup valueLookup;
    private Map<ValueHandleKind.ValueHandleKey, MethodHandle> handleMap = new ConcurrentHashMap<>();

    ValueTypeHolder(Class<T> boxClass, Class<T> valueClass) {
        this.boxLookup = IMPL_LOOKUP.in(boxClass);
        this.valueLookup = IMPL_LOOKUP.in(valueClass);
    }

    @SuppressWarnings("unchecked")
    public Class<T> boxClass() {
        return (Class<T>)boxLookup.lookupClass();
    }

    public Class<?> sourceClass() {
        return boxClass();
    }

    public Class<?> valueClass() {
        return valueLookup.lookupClass();
    }

    public Class<?> arrayValueClass() {
        return arrayValueClass(1);
    }

    public Class<?> arrayValueClass(int dims) {
        String dimsStr = "[[[[[[[[[[[[[[[[";
        if (dims < 1 || dims > 16) {
            throw new IllegalArgumentException("cannot create array class for dimension > 16");
        }
        String cn = dimsStr.substring(0, dims) + "Q" + valueClass().getName() + ";";
        return MinimalValueTypes_1_0.loadValueTypeClass(boxLookup.lookupClass(), cn);
    }

    public String toString() {
        return "ValueType boxClass=" + boxClass() + " valueClass=" + valueClass();
    }

    public MethodHandle defaultValueConstant() {
        ValueHandleKey key = ValueHandleKind.DEFAULT.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(valueClass()),
                C -> C.vdefault(valueClass()).vreturn());
    }

    public MethodHandle substitutabilityTest() {
        ValueHandleKey key = ValueHandleKind.EQ.key();
        return getOrLoad(valueLookup, key,
                () -> MethodType.methodType(boolean.class, valueClass(), valueClass()),
                C ->  substitutabilityTestBuilder(false, valueClass(), FieldInfo.stream(valueFields()), C));
    }

    private static <T extends MethodHandleCodeBuilder<T>> void substitutabilityTestBuilder(boolean needsInstanceCheck, Class<?> clazz, Stream<FieldInfo> fInfos, MethodHandleCodeBuilder<T> C) {
        if (needsInstanceCheck) {
            C.aload_1()
             .instanceof_(clazz)
             .emitCondJump(Opcode.IFEQ, CondKind.EQ, "fail")
             .aload_1()
             .checkcast(clazz)
             .store(1);
        }
        fInfos.forEach(fInfo -> {
            String fDesc = BytecodeDescriptor.unparse(fInfo.getType());
            if (fInfo.getType().isPrimitive()) {
                //field is a primitive type - perform bytecode comparison
                C.load(0).getfield(clazz, fInfo.getName(), fDesc);
                C.load(1).getfield(clazz, fInfo.getName(), fDesc);
                C.ifcmp(fDesc, CondKind.NE, "fail");
            } else if (MinimalValueTypes_1_0.isValueType(fInfo.getType())) {
                //field is a value type - call subst handle recursively
                C.load(0).getfield(clazz, fInfo.getName(), fDesc).dup().store(2);
                valueHandleBuilder(fInfo.getType(), ValueHandleKind.EQ.key(), C)
                 .load(2)
                 .load(1).getfield(clazz, fInfo.getName(), fDesc)
                 .invokevirtual(MethodHandle.class, "invoke",
                         MethodType.methodType(boolean.class, fInfo.getType(), fInfo.getType()).toMethodDescriptorString(), false)
                 .const_(0).ifcmp(TypeTag.I, CondKind.EQ, "fail");
            } else {
                //otherwise, field is a reference type, fallback to Objects.equals
                C.load(0).getfield(clazz, fInfo.getName(), fDesc);
                C.load(1).getfield(clazz, fInfo.getName(), fDesc);
                C.invokestatic(Objects.class, "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                C.const_(0).ifcmp(TypeTag.I, CondKind.EQ, "fail");
            }
        });
        C.const_(1);
        C.ireturn();
        C.label("fail");
        C.const_(0);
        C.ireturn();
    }

    public MethodHandle substitutabilityHashCode() {
        ValueHandleKey key = ValueHandleKind.HASH.key();
        return getOrLoad(valueLookup, key,
                () -> MethodType.methodType(int.class, valueClass()),
                C -> substitutabilityHashCodeBuilder(valueClass(), FieldInfo.stream(valueFields()), C));
    }

    private static <T extends MethodHandleCodeBuilder<T>> void substitutabilityHashCodeBuilder(Class<?> clazz, Stream<FieldInfo> fInfos, MethodHandleCodeBuilder<T> C) {
        C.withLocal("res", "I");
        C.const_(1).store("res");
        fInfos.forEach(fInfo -> {
            String desc = BytecodeDescriptor.unparse(fInfo.getType());
            if (fInfo.getType().isPrimitive()) {
                C.load(0).getfield(clazz, fInfo.getName(), desc);
                C.invokestatic(Wrapper.asWrapperType(fInfo.getType()), "hashCode", "(" + desc + ")I", false);
            } else if (MinimalValueTypes_1_0.isValueType(fInfo.getType())) {
                //field is a value type - call subst handle recursively
                C.load(0).getfield(clazz, fInfo.getName(), desc).dup().store(2);
                valueHandleBuilder(fInfo.getType(), ValueHandleKind.HASH.key(), C)
                 .load(2)
                 .invokevirtual(MethodHandle.class, "invoke",
                         MethodType.methodType(int.class, fInfo.getType()).toMethodDescriptorString(), false);
            } else {
                C.load(0).getfield(clazz, fInfo.getName(), desc);
                C.invokestatic(Objects.class, "hashCode", "(Ljava/lang/Object;)I", false);
            }
            C.load("res").const_(31).imul();
            C.iadd().store("res");
        });
        C.load("res").ireturn();
    }

    // ()Q
    public MethodHandle findConstructor(Lookup lookup, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.filterReturnValue(lookup.findConstructor(boxClass(), type), unbox());
    }

    // (F1, ..., Fn)Q, fromDefault == true
    // (Q, F1, ..., Fn)Q, fromDefault == false
    public MethodHandle unreflectWithers(Lookup lookup,
                                         boolean fromDefault,
                                         Field... fields) throws NoSuchFieldException, IllegalAccessException {
        // Allow access if the lookup class is the VCC or DVT and the lookup
        // has private access
        Class<?> lc = lookup.lookupClass();
        if (!lookup.hasPrivateAccess() || (valueClass() != lc && boxClass() != lc)) {
            throw new IllegalAccessException(String.format("Class %s does not have vwithfield access to fields of %s",
                                                           lc.getName(), boxClass().getName()));
        }

        // Ensure fields are value component fields declared by the VCC
        for (Field f : fields) {
            if (!isValueField(f) || f.getDeclaringClass() != sourceClass()) {
                throw new IllegalArgumentException(
                        String.format("Field %s is not a value component field declared in value capable class %s", f.getName(), sourceClass().getName()));
            }
        }

        ValueHandleKey key = ValueHandleKind.UNREFLECT_WITHERS.key(List.of(fromDefault,
                FieldInfo.stream(fields).collect(Collectors.toList())));
        return getOrLoad(valueLookup, key,
                () -> {
                    MethodType mt = MethodType.methodType(
                        valueClass(),
                        Stream.of(fields).map(Field::getType).toArray(Class[]::new));

                    if (!fromDefault) {
                        mt = mt.insertParameterTypes(0, valueClass());
                    }
                    return mt;
                },
                C -> {
                    int l = 0;
                    if (fromDefault) {
                        C.vdefault(valueClass());
                    } else {
                        C.load(0);
                        l = 1;
                    }

                    for (Field f : fields) {
                        String fType = BytecodeDescriptor.unparse(f.getType());
                        C.load(l).vwithfield(valueClass(), f.getName(), fType);
                        l += Wrapper.forBasicType(f.getType()).stackSlots();
                    }
                    C.vreturn();
                });
    }

    // (Q, T)Q
    public MethodHandle findWither(Lookup lookup, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        // Allow access if the lookup class is the VCC or DVT and the lookup
        // has private access
        Class<?> lc = lookup.lookupClass();
        if (!lookup.hasPrivateAccess() || (valueClass() != lc && boxClass() != lc)) {
            throw new IllegalAccessException(String.format("Class %s does not have vwithfield access to field %s.%s",
                                                           lc.getName(), boxClass().getName(), name));
        }

        // Check field exists on VCC
        lookup.findGetter(boxClass(), name, type);

        ValueHandleKey key = ValueHandleKind.WITHER.key(new FieldInfo(name, type));
        return getOrLoad(valueLookup, key,
                () -> MethodType.methodType(valueClass(), valueClass(), type),
                C -> C.vload(0).load(1).vwithfield(valueClass(), name, BytecodeDescriptor.unparse(type)).vreturn());
    }

    public MethodHandle unbox() {
        ValueHandleKey key = ValueHandleKind.UNBOX.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(valueClass(), boxClass()),
                C -> C.load(0).vunbox(valueClass()).vreturn());
    }

    public MethodHandle box() {
        ValueHandleKey key = ValueHandleKind.BOX.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(boxClass(), valueClass()),
                C -> C.vload(0).vbox(boxClass()).areturn());
    }

    public MethodHandle newArray() {
        ValueHandleKey key = ValueHandleKind.NEWARRAY.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(arrayValueClass(), int.class),
                C -> C.load(0).anewarray(valueClass()).areturn());
    }

    public MethodHandle arrayGetter() {
        ValueHandleKey key = ValueHandleKind.VALOAD.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(valueClass(), arrayValueClass(), int.class),
                C -> C.load(0).load(1).vaload().vreturn());
    }

    public MethodHandle arraySetter() {
        ValueHandleKey key = ValueHandleKind.VASTORE.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(void.class, arrayValueClass(), int.class, valueClass()),
                C -> C.load(0).load(1).load(2).vastore().return_());
    }

    public MethodHandle newMultiArray(int dims) {
        Class<?> arrayValueClass = arrayValueClass(dims);
        ValueHandleKey key = ValueHandleKind.MULTINEWARRAY.key(dims);
        return getOrLoad(boxLookup, key,
                () -> {
                    Class<?>[] params = new Class<?>[dims];
                    Arrays.fill(params, int.class);
                    return MethodType.methodType(arrayValueClass, params);
                },
                C -> {
                    for (int i = 0 ; i < dims ; i++) {
                        C.load(i);
                    }
                    C.multianewarray(arrayValueClass, (byte)dims).areturn();
                });
    }

    public MethodHandle arrayLength() {
        ValueHandleKey key = ValueHandleKind.ARRAYLENGTH.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(int.class, arrayValueClass()),
                C -> C.load(0).arraylength().ireturn());
    }

    public MethodHandle identity() {
        ValueHandleKey key = ValueHandleKind.IDENTITY.key();
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(valueClass(), valueClass()),
                C -> C.vload(0).vreturn());
    }

    public MethodHandle findGetter(Lookup lookup, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        //force access-check
        lookup.findGetter(boxClass(), name, type);

        ValueHandleKey key = ValueHandleKind.GETTER.key(new FieldInfo(name, type));
        String fieldType = BytecodeDescriptor.unparse(type);
        return getOrLoad(boxLookup, key,
                () -> MethodType.methodType(type, valueClass()),
                C -> C.vload(0).getfield(valueClass(), name, fieldType).return_(fieldType));
    }

    private static <T extends MethodHandleCodeBuilder<T>> T valueHandleBuilder(Class<?> dvt, ValueHandleKey key, MethodHandleCodeBuilder<T> C) {
        MethodType mt = key.kind.handleType();
        if (mt.parameterCount() > 0) {
            throw new AssertionError("Non-nilary handle builders not supported yet");
        }
        Class<?> vtSupportClass = MinimalValueTypes_1_0.getIncubatorValueTypeClass();
        return C.vbox(MinimalValueTypes_1_0.getValueCapableClass(dvt))
                 .invokevirtual(Object.class, "getClass", "()Ljava/lang/Class;", false)
                 .invokestatic(vtSupportClass, "forClass",
                         MethodType.methodType(vtSupportClass, Class.class).toMethodDescriptorString(), false)
                 .invokevirtual(vtSupportClass, key.kind.handleName(), key.kind.handleType().toMethodDescriptorString(), false);
    }

    private MethodHandle getOrLoad(Lookup lookup, ValueHandleKey key, Supplier<MethodType> typeSupplier, Consumer<? super MethodHandleCodeBuilder<?>> codeBuilder) {
        MethodHandle result = handleMap.get(key);
        if (result == null) {
            String handleClassName = sourceClass().getName() + "_" + key.kind.handleName();
            result = MethodHandleBuilder.loadCode(lookup, handleClassName, key.kind.handleName(), typeSupplier.get(), codeBuilder);
            handleMap.put(key, result);
        }
        return result;
    }

    boolean isValueField(Field f) {
        return (f.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) == Modifier.FINAL;
    }

    public Field[] valueFields() {
        return Stream.of(sourceClass().getDeclaredFields())
                .filter(this::isValueField)
                .toArray(Field[]::new);
    }

    final static class FieldInfo {

        private final String name;
        private final Class<?> type;

        FieldInfo(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        String getName() { return name; }
        Class<?> getType() { return type; }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FieldInfo) {
                FieldInfo that = (FieldInfo)o;
                return Objects.equals(name, that.name) &&
                        Objects.equals(type, that.type);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        private static Stream<FieldInfo> stream(Field[] fields) {
           return Stream.of(fields).map(f -> new FieldInfo(f.getName(), f.getType()));
        }

        private static Stream<FieldInfo> stream(String[] fieldNames, Class<?>[] fieldTypes) {
            return IntStream.range(0, fieldNames.length)
                        .mapToObj(i -> new FieldInfo(fieldNames[i], fieldTypes[i]));
        }
    }
}
