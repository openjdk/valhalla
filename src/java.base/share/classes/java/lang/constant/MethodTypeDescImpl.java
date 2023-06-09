/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.constant;

import jdk.internal.vm.annotation.Stable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodType}.  A {@linkplain MethodTypeDescImpl} corresponds to a
 * {@code Constant_MethodType_info} entry in the constant pool of a classfile.
 */
final class MethodTypeDescImpl implements MethodTypeDesc {
    private final ClassDesc returnType;
    private final @Stable ClassDesc[] argTypes;
    private @Stable String cachedDescriptorString;

    /**
     * Constructs a {@linkplain MethodTypeDesc} with the specified return type
     * and a trusted and already-validated parameter types array.
     *
     * @param returnType a {@link ClassDesc} describing the return type
     * @param validatedArgTypes {@link ClassDesc}s describing the trusted and validated parameter types
     */
    private MethodTypeDescImpl(ClassDesc returnType, ClassDesc[] validatedArgTypes) {
        this.returnType = requireNonNull(returnType);
        this.argTypes = requireNonNull(validatedArgTypes);
    }

    /**
     * Constructs a {@linkplain MethodTypeDesc} with the specified return type
     * and a trusted parameter types array, which will be validated.
     *
     * @param returnType a {@link ClassDesc} describing the return type
     * @param trustedArgTypes {@link ClassDesc}s describing the trusted parameter types
     */
    static MethodTypeDescImpl ofTrusted(ClassDesc returnType, ClassDesc[] trustedArgTypes) {
        Objects.requireNonNull(returnType);
        if (trustedArgTypes.length == 0) // implicit null check
            return new MethodTypeDescImpl(returnType, ConstantUtils.EMPTY_CLASSDESC);

        for (ClassDesc cd : trustedArgTypes)
            if (cd.isPrimitive() && cd.descriptorString().charAt(0) == 'V') // implicit null check
                throw new IllegalArgumentException("Void parameters not permitted");

        return new MethodTypeDescImpl(returnType, trustedArgTypes);
    }

    /**
     * Creates a {@linkplain MethodTypeDescImpl} given a method descriptor string.
     *
     * @param descriptor the method descriptor string
     * @return a {@linkplain MethodTypeDescImpl} describing the desired method type
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method descriptor
     * @jvms 4.3.3 Method Descriptors
     */
    static MethodTypeDescImpl ofDescriptor(String descriptor) {
        requireNonNull(descriptor);

        List<String> types = ConstantUtils.parseMethodDescriptor(descriptor);

        int paramCount = types.size() - 1;
        var paramTypes = paramCount > 0 ? new ClassDesc[paramCount] : ConstantUtils.EMPTY_CLASSDESC;
        for (int i = 0; i < paramCount; i++) {
            paramTypes[i] = ClassDesc.ofDescriptor(types.get(i + 1));
        }

        MethodTypeDescImpl result = ofTrusted(ClassDesc.ofDescriptor(types.getFirst()), paramTypes);
        result.cachedDescriptorString = descriptor;
        return result;
    }

    @Override
    public ClassDesc returnType() {
        return returnType;
    }

    @Override
    public int parameterCount() {
        return argTypes.length;
    }

    @Override
    public ClassDesc parameterType(int index) {
        return argTypes[index];
    }

    @Override
    public List<ClassDesc> parameterList() {
        return List.of(argTypes);
    }

    @Override
    public ClassDesc[] parameterArray() {
        return argTypes.clone();
    }

    @Override
    public MethodTypeDesc changeReturnType(ClassDesc returnType) {
        return new MethodTypeDescImpl(returnType, argTypes);
    }

    @Override
    public MethodTypeDesc changeParameterType(int index, ClassDesc paramType) {
        ClassDesc[] newArgs = argTypes.clone();
        newArgs[index] = paramType;
        return ofTrusted(returnType, newArgs);
    }

    @Override
    public MethodTypeDesc dropParameterTypes(int start, int end) {
        Objects.checkIndex(start, argTypes.length);
        Objects.checkFromToIndex(start, end, argTypes.length);

        ClassDesc[] newArgs = new ClassDesc[argTypes.length - (end - start)];
        System.arraycopy(argTypes, 0, newArgs, 0, start);
        System.arraycopy(argTypes, end, newArgs, start, argTypes.length - end);
        return ofTrusted(returnType, newArgs);
    }

    @Override
    public MethodTypeDesc insertParameterTypes(int pos, ClassDesc... paramTypes) {
        if (pos < 0 || pos > argTypes.length)
            throw new IndexOutOfBoundsException(pos);

        ClassDesc[] newArgs = new ClassDesc[argTypes.length + paramTypes.length];
        System.arraycopy(argTypes, 0, newArgs, 0, pos);
        System.arraycopy(paramTypes, 0, newArgs, pos, paramTypes.length);
        System.arraycopy(argTypes, pos, newArgs, pos+paramTypes.length, argTypes.length - pos);

        return ofTrusted(returnType, newArgs);
    }

    @Override
    public String descriptorString() {
        var desc = this.cachedDescriptorString;
        if (desc != null)
            return desc;

        var sj = new StringJoiner("", "(", ")" + returnType().descriptorString());
        for (int i = 0; i < parameterCount(); i++) {
            sj.add(parameterType(i).descriptorString());
        }
        return cachedDescriptorString = sj.toString();
    }

    @Override
    public MethodType resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        @SuppressWarnings("removal")
        MethodType mtype = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public MethodType run() {
                return MethodType.fromMethodDescriptorString(descriptorString(),
                                                             lookup.lookupClass().getClassLoader());
            }
        });

        // let's check that the lookup has access to all the types in the method type
        lookup.accessClass(mtype.returnType());
        for (Class<?> paramType: mtype.parameterArray()) {
            lookup.accessClass(paramType);
        }
        return mtype;
    }

    /**
     * Returns {@code true} if this {@linkplain MethodTypeDescImpl} is
     * equal to another {@linkplain MethodTypeDescImpl}.  Equality is
     * determined by the two descriptors having equal return types and argument
     * types.
     *
     * @param o the {@code MethodTypeDescImpl} to compare to this
     *       {@code MethodTypeDescImpl}
     * @return {@code true} if the specified {@code MethodTypeDescImpl}
     *      is equal to this {@code MethodTypeDescImpl}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodTypeDescImpl constant = (MethodTypeDescImpl) o;

        return returnType.equals(constant.returnType)
               && Arrays.equals(argTypes, constant.argTypes);
    }

    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + Arrays.hashCode(argTypes);
        return result;
    }

    @Override
    public String toString() {
        return String.format("MethodTypeDesc[%s]", displayDescriptor());
    }
}
