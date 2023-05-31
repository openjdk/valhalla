/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import java.lang.Enum.EnumDesc;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import jdk.internal.vm.annotation.Stable;

/**
 * Bootstrap methods for linking {@code invokedynamic} call sites that implement
 * the selection functionality of the {@code switch} statement.  The bootstraps
 * take additional static arguments corresponding to the {@code case} labels
 * of the {@code switch}, implicitly numbered sequentially from {@code [0..N)}.
 *
 * @since 21
 */
public class SwitchBootstraps {

    private SwitchBootstraps() {}

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle DO_TYPE_SWITCH;
    private static final MethodHandle DO_ENUM_SWITCH;

    static {
        try {
            DO_TYPE_SWITCH = LOOKUP.findStatic(SwitchBootstraps.class, "doTypeSwitch",
                                           MethodType.methodType(int.class, Object.class, int.class, Object[].class));
            DO_ENUM_SWITCH = LOOKUP.findStatic(SwitchBootstraps.class, "doEnumSwitch",
                                           MethodType.methodType(int.class, Enum.class, int.class, Object[].class,
                                                                 MethodHandles.Lookup.class, Class.class, ResolvedEnumLabels.class));
        }
        catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a target of a reference type.  The static
     * arguments are an array of case labels which must be non-null and of type
     * {@code String} or {@code Integer} or {@code Class} or {@code EnumDesc}.
     * <p>
     * The type of the returned {@code CallSite}'s method handle will have
     * a return type of {@code int}.   It has two parameters: the first argument
     * will be an {@code Object} instance ({@code target}) and the second
     * will be {@code int} ({@code restart}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     * <p>
     * If the {@code target} is not {@code null}, then the method of the call site
     * returns the index of the first element in the {@code labels} array starting from
     * the {@code restart} index matching one of the following conditions:
     * <ul>
     *   <li>the element is of type {@code Class} that is assignable
     *       from the target's class; or</li>
     *   <li>the element is of type {@code String} or {@code Integer} and
     *       equals to the target.</li>
     *   <li>the element is of type {@code EnumDesc}, that describes a constant that is
     *       equals to the target.</li>
     * </ul>
     * <p>
     * If no element in the {@code labels} array matches the target, then
     * the method of the call site return the length of the {@code labels} array.
     * <p>
     * The value of the {@code restart} index must be between {@code 0} (inclusive) and
     * the length of the {@code labels} array (inclusive),
     * both  or an {@link IndexOutOfBoundsException} is thrown.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller.  When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with two parameters,
     *                       a reference type, an {@code int}, and {@code int} as a return type.
     * @param labels case labels - {@code String} and {@code Integer} constants
     *               and {@code Class} and {@code EnumDesc} instances, in any combination
     * @return a {@code CallSite} returning the first matching element as described above
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element in the labels array is null, if the
     * invocation type is not not a method type of first parameter of a reference type,
     * second parameter of type {@code int} and with {@code int} as its return type,
     * or if {@code labels} contains an element that is not of type {@code String},
     * {@code Integer}, {@code Class} or {@code EnumDesc}.
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite typeSwitch(MethodHandles.Lookup lookup,
                                      String invocationName,
                                      MethodType invocationType,
                                      Object... labels) {
        if (invocationType.parameterCount() != 2
            || (!invocationType.returnType().equals(int.class))
            || invocationType.parameterType(0).isPrimitive()
            || !invocationType.parameterType(1).equals(int.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        requireNonNull(labels);

        labels = labels.clone();
        Stream.of(labels).forEach(SwitchBootstraps::verifyLabel);

        MethodHandle target = MethodHandles.insertArguments(DO_TYPE_SWITCH, 2, (Object) labels);
        return new ConstantCallSite(target);
    }

    private static void verifyLabel(Object label) {
        if (label == null) {
            throw new IllegalArgumentException("null label found");
        }
        Class<?> labelClass = label.getClass();
        if (labelClass != Class.class &&
            labelClass != String.class &&
            labelClass != Integer.class &&
            labelClass != EnumDesc.class) {
            throw new IllegalArgumentException("label with illegal type found: " + label.getClass());
        }
    }

    private static int doTypeSwitch(Object target, int startIndex, Object[] labels) {
        Objects.checkIndex(startIndex, labels.length + 1);

        if (target == null)
            return -1;

        // Dumbest possible strategy
        Class<?> targetClass = target.getClass();
        for (int i = startIndex; i < labels.length; i++) {
            Object label = labels[i];
            if (label instanceof Class<?> c) {
                if (c.isAssignableFrom(targetClass))
                    return i;
            } else if (label instanceof Integer constant) {
                if (target instanceof Number input && constant.intValue() == input.intValue()) {
                    return i;
                } else if (target instanceof Character input && constant.intValue() == input.charValue()) {
                    return i;
                }
            } else if (label instanceof EnumDesc<?> enumDesc) {
                if (target.getClass().isEnum() &&
                    ((Enum<?>) target).describeConstable().stream().anyMatch(d -> d.equals(enumDesc))) {
                    return i;
                }
            } else if (label.equals(target)) {
                return i;
            }
        }

        return labels.length;
    }

    /**
     * Bootstrap method for linking an {@code invokedynamic} call site that
     * implements a {@code switch} on a target of an enum type. The static
     * arguments are used to encode the case labels associated to the switch
     * construct, where each label can be encoded in two ways:
     * <ul>
     *   <li>as a {@code String} value, which represents the name of
     *       the enum constant associated with the label</li>
     *   <li>as a {@code Class} value, which represents the enum type
     *       associated with a type test pattern</li>
     * </ul>
     * <p>
     * The returned {@code CallSite}'s method handle will have
     * a return type of {@code int} and accepts two parameters: the first argument
     * will be an {@code Enum} instance ({@code target}) and the second
     * will be {@code int} ({@code restart}).
     * <p>
     * If the {@code target} is {@code null}, then the method of the call site
     * returns {@literal -1}.
     * <p>
     * If the {@code target} is not {@code null}, then the method of the call site
     * returns the index of the first element in the {@code labels} array starting from
     * the {@code restart} index matching one of the following conditions:
     * <ul>
     *   <li>the element is of type {@code Class} that is assignable
     *       from the target's class; or</li>
     *   <li>the element is of type {@code String} and equals to the target
     *       enum constant's {@link Enum#name()}.</li>
     * </ul>
     * <p>
     * If no element in the {@code labels} array matches the target, then
     * the method of the call site return the length of the {@code labels} array.
     * <p>
     * The value of the {@code restart} index must be between {@code 0} (inclusive) and
     * the length of the {@code labels} array (inclusive),
     * both  or an {@link IndexOutOfBoundsException} is thrown.
     *
     * @param lookup Represents a lookup context with the accessibility
     *               privileges of the caller. When used with {@code invokedynamic},
     *               this is stacked automatically by the VM.
     * @param invocationName unused
     * @param invocationType The invocation type of the {@code CallSite} with two parameters,
     *                       an enum type, an {@code int}, and {@code int} as a return type.
     * @param labels case labels - {@code String} constants and {@code Class} instances,
     *               in any combination
     * @return a {@code CallSite} returning the first matching element as described above
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element in the labels array is null, if the
     * invocation type is not a method type whose first parameter type is an enum type,
     * second parameter of type {@code int} and whose return type is {@code int},
     * or if {@code labels} contains an element that is not of type {@code String} or
     * {@code Class} of the target enum type.
     * @jvms 4.4.6 The CONSTANT_NameAndType_info Structure
     * @jvms 4.4.10 The CONSTANT_Dynamic_info and CONSTANT_InvokeDynamic_info Structures
     */
    public static CallSite enumSwitch(MethodHandles.Lookup lookup,
                                      String invocationName,
                                      MethodType invocationType,
                                      Object... labels) {
        if (invocationType.parameterCount() != 2
            || (!invocationType.returnType().equals(int.class))
            || invocationType.parameterType(0).isPrimitive()
            || !invocationType.parameterType(0).isEnum()
            || !invocationType.parameterType(1).equals(int.class))
            throw new IllegalArgumentException("Illegal invocation type " + invocationType);
        requireNonNull(labels);

        labels = labels.clone();

        Class<?> enumClass = invocationType.parameterType(0);
        Stream.of(labels).forEach(l -> validateEnumLabel(enumClass, l));
        MethodHandle temporary =
                MethodHandles.insertArguments(DO_ENUM_SWITCH, 2, labels, lookup, enumClass, new ResolvedEnumLabels());
        temporary = temporary.asType(invocationType);

        return new ConstantCallSite(temporary);
    }

    private static <E extends Enum<E>> void validateEnumLabel(Class<?> enumClassTemplate, Object label) {
        if (label == null) {
            throw new IllegalArgumentException("null label found");
        }
        Class<?> labelClass = label.getClass();
        if (labelClass == Class.class) {
            if (label != enumClassTemplate) {
                throw new IllegalArgumentException("the Class label: " + label +
                                                   ", expected the provided enum class: " + enumClassTemplate);
            }
        } else if (labelClass != String.class) {
            throw new IllegalArgumentException("label with illegal type found: " + labelClass +
                                               ", expected label of type either String or Class");
        }
    }

    private static <E extends Enum<E>> Object convertEnumConstants(MethodHandles.Lookup lookup, Class<?> enumClassTemplate, Object label) {
        if (label == null) {
            throw new IllegalArgumentException("null label found");
        }
        Class<?> labelClass = label.getClass();
        if (labelClass == Class.class) {
            if (label != enumClassTemplate) {
                throw new IllegalArgumentException("the Class label: " + label +
                                                   ", expected the provided enum class: " + enumClassTemplate);
            }
            return label;
        } else if (labelClass == String.class) {
            @SuppressWarnings("unchecked")
            Class<E> enumClass = (Class<E>) enumClassTemplate;
            try {
                return ConstantBootstraps.enumConstant(lookup, (String) label, enumClass);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("label with illegal type found: " + labelClass +
                                               ", expected label of type either String or Class");
        }
    }

    private static int doEnumSwitch(Enum<?> target, int startIndex, Object[] unresolvedLabels,
                                    MethodHandles.Lookup lookup, Class<?> enumClass,
                                    ResolvedEnumLabels resolvedLabels) {
        Objects.checkIndex(startIndex, unresolvedLabels.length + 1);

        if (target == null)
            return -1;

        if (resolvedLabels.resolvedLabels == null) {
            resolvedLabels.resolvedLabels = Stream.of(unresolvedLabels)
                                                  .map(l -> convertEnumConstants(lookup, enumClass, l))
                                                  .toArray();
        }

        Object[] labels = resolvedLabels.resolvedLabels;

        // Dumbest possible strategy
        Class<?> targetClass = target.getClass();
        for (int i = startIndex; i < labels.length; i++) {
            Object label = labels[i];
            if (label instanceof Class<?> c) {
                if (c.isAssignableFrom(targetClass))
                    return i;
            } else if (label == target) {
                return i;
            }
        }

        return labels.length;
    }

    private static final class ResolvedEnumLabels {
        @Stable
        public Object[] resolvedLabels;
    }
}
