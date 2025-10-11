/*
 * Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;


import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Represents a resolved Java method. Methods, like fields and types, are resolved through
 * {@link ConstantPool constant pools}.
 */
public interface ResolvedJavaMethod extends JavaMethod, InvokeTarget, ModifiersProvider, AnnotatedElement, Annotated {

    /**
     * Returns the method's bytecode. The returned bytecode does not contain breakpoints or non-Java
     * bytecodes. This will return {@code null} if {@link #getCodeSize()} returns {@code <= 0} or if
     * {@link #hasBytecodes()} returns {@code false}.
     *
     * The contained constant pool indexes may not be the ones found in the original class file but
     * they can be used with the JVMCI API (e.g. methods in {@link ConstantPool}).
     *
     * @return {@code null} if {@code getLinkedCodeSize() <= 0} otherwise the bytecode of the method
     *         whose length is guaranteed to be {@code > 0}
     */
    byte[] getCode();

    /**
     * Returns the size of the method's bytecode. If this method returns a value {@code > 0} then
     * {@link #getCode()} will not return {@code null}.
     *
     * @return 0 if the method has no bytecode, {@code -1} if the method does have bytecode but its
     *         {@linkplain #getDeclaringClass() declaring class} is not
     *         {@linkplain ResolvedJavaType#isLinked() linked} otherwise the size of the bytecode in
     *         bytes (guaranteed to be {@code > 0})
     */
    int getCodeSize();

    /**
     * Returns the {@link ResolvedJavaType} object representing the class or interface that declares
     * this method.
     */
    @Override
    ResolvedJavaType getDeclaringClass();

    /**
     * Returns the maximum number of locals used in this method's bytecodes.
     */
    int getMaxLocals();

    /**
     * Returns the maximum number of stack slots used in this method's bytecodes.
     */
    int getMaxStackSize();

    default boolean isFinal() {
        return ModifiersProvider.super.isFinalFlagSet();
    }

    /**
     * Determines if this method is a synthetic method as defined by the Java Language
     * Specification.
     */
    boolean isSynthetic();

    /**
     * Checks if the method is a varargs method.
     *
     * @return whether the method is a varargs method
     * @jvms 4.6
     */
    boolean isVarArgs();

    /**
     * Checks if the method is a bridge method.
     *
     * @return whether the method is a bridge method
     * @jvms 4.6
     */
    boolean isBridge();

    /**
     * Returns {@code true} if this method is a default method; returns {@code false} otherwise.
     *
     * A default method is a public non-abstract instance method, that is, a non-static method with
     * a body, declared in an interface type.
     *
     * @return true if and only if this method is a default method as defined by the Java Language
     *         Specification.
     */
    boolean isDefault();

    /**
     * Returns {@code true} if this method is contained in the array returned by
     * {@code getDeclaringClass().getDeclaredMethods()}
     */
    boolean isDeclared();

    /**
     * Checks whether this method is a class initializer.
     *
     * @return {@code true} if the method is a class initializer
     */
    boolean isClassInitializer();

    /**
     * Checks whether this method is a constructor.
     *
     * @return {@code true} if the method is a constructor
     */
    boolean isConstructor();

    /**
     * Checks whether this method can be statically bound (usually, that means it is final or
     * private or static, but not abstract, or the declaring class is final).
     *
     * @return {@code true} if this method can be statically bound
     */
    boolean canBeStaticallyBound();

    /**
     * Returns the list of exception handlers for this method.
     */
    ExceptionHandler[] getExceptionHandlers();

    /**
     * Returns a stack trace element for this method and a given bytecode index.
     */
    StackTraceElement asStackTraceElement(int bci);

    /**
     * Returns an object that provides access to the profiling information recorded for this method.
     */
    default ProfilingInfo getProfilingInfo() {
        return getProfilingInfo(true, true);
    }

    /**
     * Returns an object that provides access to the profiling information recorded for this method.
     *
     * @param includeNormal if true,
     *            {@linkplain ProfilingInfo#getDeoptimizationCount(DeoptimizationReason)
     *            deoptimization counts} will include deoptimization that happened during execution
     *            of standard non-osr methods.
     * @param includeOSR if true,
     *            {@linkplain ProfilingInfo#getDeoptimizationCount(DeoptimizationReason)
     *            deoptimization counts} will include deoptimization that happened during execution
     *            of on-stack-replacement methods.
     */
    ProfilingInfo getProfilingInfo(boolean includeNormal, boolean includeOSR);

    /**
     * Invalidates the profiling information and restarts profiling upon the next invocation.
     */
    void reprofile();

    /**
     * Returns the constant pool of this method.
     */
    ConstantPool getConstantPool();

    /**
     * A {@code Parameter} provides information about method parameters.
     */
    class Parameter implements AnnotatedElement {
        private final String name;
        private final ResolvedJavaMethod method;
        private final int modifiers;
        private final int index;

        /**
         * Constructor for {@code Parameter}.
         *
         * @param name the name of the parameter or {@code null} if there is no
         *            {@literal MethodParameters} class file attribute providing a non-empty name
         *            for the parameter
         * @param modifiers the modifier flags for the parameter
         * @param method the method which defines this parameter
         * @param index the index of the parameter
         */
        public Parameter(String name,
                        int modifiers,
                        ResolvedJavaMethod method,
                        int index) {
            assert name == null || !name.isEmpty();
            this.name = name;
            this.modifiers = modifiers;
            this.method = method;
            this.index = index;
        }

        /**
         * Gets the name of the parameter. If the parameter's name is {@linkplain #isNamePresent()
         * present}, then this method returns the name provided by the class file. Otherwise, this
         * method synthesizes a name of the form argN, where N is the index of the parameter in the
         * descriptor of the method which declares the parameter.
         *
         * @return the name of the parameter, either provided by the class file or synthesized if
         *         the class file does not provide a name
         */
        public String getName() {
            if (name == null) {
                return "arg" + index;
            } else {
                return name;
            }
        }

        /**
         * Gets the method declaring the parameter.
         */
        public ResolvedJavaMethod getDeclaringMethod() {
            return method;
        }

        /**
         * Get the modifier flags for the parameter.
         */
        public int getModifiers() {
            return modifiers;
        }

        /**
         * Gets the kind of the parameter.
         */
        public JavaKind getKind() {
            return method.getSignature().getParameterKind(index);
        }

        /**
         * Gets the formal type of the parameter.
         */
        public Type getParameterizedType() {
            return method.getGenericParameterTypes()[index];
        }

        /**
         * Gets the type of the parameter.
         */
        public JavaType getType() {
            return method.getSignature().getParameterType(index, method.getDeclaringClass());
        }

        /**
         * Determines if the parameter has a name according to a {@literal MethodParameters} class
         * file attribute.
         *
         * @return true if and only if the parameter has a name according to the class file.
         */
        public boolean isNamePresent() {
            return name != null;
        }

        /**
         * Determines if the parameter represents a variable argument list.
         */
        public boolean isVarArgs() {
            return method.isVarArgs() && index == method.getSignature().getParameterCount(false) - 1;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return method.getParameterAnnotations(annotationClass)[index];
        }

        @Override
        public Annotation[] getAnnotations() {
            return method.getParameterAnnotations()[index];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }

        @Override
        public String toString() {
            Type type = getParameterizedType();
            String typename = type.getTypeName();
            if (isVarArgs()) {
                typename = typename.replaceFirst("\\[\\]$", "...");
            }

            final StringBuilder sb = new StringBuilder(Modifier.toString(getModifiers()));
            if (sb.length() != 0) {
                sb.append(' ');
            }
            return sb.append(typename).append(' ').append(getName()).toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Parameter) {
                Parameter other = (Parameter) obj;
                return (other.method.equals(method) && other.index == index);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return method.hashCode() ^ index;
        }
    }

    /**
     * Returns an array of {@code Parameter} objects that represent all the parameters to this
     * method. Returns an array of length 0 if this method has no parameters. Returns {@code null}
     * if the parameter information is unavailable.
     */
    default Parameter[] getParameters() {
        return null;
    }

    /**
     * Returns an array of arrays that represent the annotations on the formal parameters, in
     * declaration order, of this method.
     *
     * @see Method#getParameterAnnotations()
     */
    Annotation[][] getParameterAnnotations();

    /**
     * Returns an array of {@link Type} objects that represent the formal parameter types, in
     * declaration order, of this method.
     *
     * @see Method#getGenericParameterTypes()
     */
    Type[] getGenericParameterTypes();

    /**
     * Returns {@code true} if this method is not excluded from inlining and has associated Java
     * bytecodes (@see {@link ResolvedJavaMethod#hasBytecodes()}).
     */
    boolean canBeInlined();

    /**
     * Determines if this method is targeted by a VM directive (e.g.,
     * {@code -XX:CompileCommand=dontinline,<pattern>}) or VM recognized annotation (e.g.,
     * {@code jdk.internal.vm.annotation.DontInline}) that specifies it should not be inlined.
     */
    boolean hasNeverInlineDirective();

    /**
     * Returns {@code true} if the inlining of this method should be forced.
     */
    boolean shouldBeInlined();

    /**
     * Returns the LineNumberTable of this method or null if this method does not have a line
     * numbers table.
     */
    LineNumberTable getLineNumberTable();

    /**
     * Returns the local variable table of this method or null if this method does not have a local
     * variable table.
     */
    LocalVariableTable getLocalVariableTable();

    /**
     * Gets the encoding of (that is, a constant representing the value of) this method.
     *
     * @return a constant representing a reference to this method
     */
    Constant getEncoding();

    /**
     * Checks if this method is present in the virtual table for subtypes of the specified
     * {@linkplain ResolvedJavaType type}.
     *
     * @return true is this method is present in the virtual table for subtypes of this type.
     */
    boolean isInVirtualMethodTable(ResolvedJavaType resolved);

    /**
     * Gets the annotation of a particular type for a formal parameter of this method.
     *
     * @param annotationClass the Class object corresponding to the annotation type
     * @param parameterIndex the index of a formal parameter of {@code method}
     * @return the annotation of type {@code annotationClass} for the formal parameter present, else
     *         null
     * @throws IndexOutOfBoundsException if {@code parameterIndex} does not denote a formal
     *             parameter
     */
    default <T extends Annotation> T getParameterAnnotation(Class<T> annotationClass, int parameterIndex) {
        if (parameterIndex >= 0) {
            Annotation[][] parameterAnnotations = getParameterAnnotations();
            for (Annotation a : parameterAnnotations[parameterIndex]) {
                if (a.annotationType() == annotationClass) {
                    return annotationClass.cast(a);
                }
            }
        }
        return null;
    }

    default JavaType[] toParameterTypes() {
        JavaType receiver = isStatic() || isConstructor() ? null : getDeclaringClass();
        return getSignature().toParameterTypes(receiver);
    }

    /**
     * Gets the annotations of a particular type for the formal parameters of this method.
     *
     * @param annotationClass the Class object corresponding to the annotation type
     * @return the annotation of type {@code annotationClass} (if any) for each formal parameter
     *         present
     */
    @SuppressWarnings("unchecked")
    default <T extends Annotation> T[] getParameterAnnotations(Class<T> annotationClass) {
        Annotation[][] parameterAnnotations = getParameterAnnotations();
        T[] result = (T[]) Array.newInstance(annotationClass, parameterAnnotations.length);
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation a : parameterAnnotations[i]) {
                if (a.annotationType() == annotationClass) {
                    result[i] = annotationClass.cast(a);
                }
            }
        }
        return result;
    }

    /**
     * @see #getCodeSize()
     * @return {@code getCodeSize() > 0}
     */
    default boolean hasBytecodes() {
        return getCodeSize() > 0;
    }

    /**
     * Checks whether the method has a receiver parameter - i.e., whether it is not static.
     *
     * @return whether the method has a receiver parameter
     */
    default boolean hasReceiver() {
        return !isStatic();
    }

    /**
     * Determines if this method is {@link java.lang.Object#Object()}.
     */
    default boolean isJavaLangObjectInit() {
        return getDeclaringClass().isJavaLangObject() && (getName().equals("<init>") || getName().equals("<vnew>"));
    }

    /**
     * Returns true if this method has a
     * {@code jdk.internal.misc.ScopedMemoryAccess.Scoped} annotation.
     *
     * @return true if Scoped annotation present, false otherwise.
     */
    default boolean isScoped() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a speculation log that can be used when compiling this method to make new speculations
     * and query previously failed speculations. The implementation may return a new
     * {@link SpeculationLog} object each time this method is called so its the caller's
     * responsibility to ensure the same speculation log is used throughout a compilation.
     */
    SpeculationLog getSpeculationLog();

    /**
     * Gets the information if a parameter at a certain position in the method signature is scalarized.
     * Inline type arguments may not be passed by reference, but in scalarized form.
     * We get an argument per field of the inline type.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return true if the parameter is scalarized, false otherwise
     */
    default boolean isScalarizedParameter(int index, boolean indexIncludesReceiverIfExists) {
        return false;
    }

    /**
     * Counts the number of scalarized parameters of a method.
     *
     * @return the number of scalarized parameters in this method
     */
    default int getScalarizedParametersCount() {
        int count = 0;
        for (int i = 0; i < getSignature().getParameterCount(!isStatic()); i++) {
            if (isScalarizedParameter(i, true)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the information if a parameter at a certain position in the method signature is non free.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return true if the parameter is null free, false otherwise
     */
    default boolean isParameterNullRestricted(int index, boolean indexIncludesReceiverIfExists) {
        return false;
    }

    /**
     * Finds out if this method has scalarized parameters.
     *
     * @return true if the method has scalarized parameters, false otherwise
     */
    default boolean hasScalarizedParameters() {
        return false;
    }

    /**
     * Finds out if this method has a scalarized return.
     *
     * @return true if this method returns it's return value in a scalarized form, false otherwise
     */
    default boolean hasScalarizedReturn() {
        return false;
    }

    /**
     * Finds out if this method has a scalarized receiver.
     *
     * @return true if this method's receiver is passed scalarized, false otherwise
     */
    default boolean hasScalarizedReceiver() {
        return false;
    }

    /**
     * Finds out if the scalarized calling convention of a method does not match that of a subclass.
     * See CompiledEntrySignature::compute_calling_conventions which detects these mismatches.
     *
     * @return true if there is a mismatch
     */
    default boolean hasCallingConventionMismatch() {
        return false;
    }

    /**
     * Gets the type information of the method's scalarized return.
     *
     * @return the scalarized return type which consists of the return type as well as the instance fields of the return type
     */
    default List<JavaType> getScalarizedReturn() {
        throw new UnsupportedOperationException("scalarized return not yet implemented");
    }

    /**
     * Gets the scalarized method signature.
     *
     * @param scalarizeReceiver true if the receiver should be scalarized as well, false otherwise
     * @return the types representing the scalarized method signature
     */
    default List<JavaType> getScalarizedParameters(boolean scalarizeReceiver) {
        throw new UnsupportedOperationException("scalarized parameters not yet implemented");
    }

    /**
     * Similar to as {@link #getScalarizedParameterNullRestricted(int, boolean)} but also includes the is not null type if the parameter is not null free.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return the instance fields as types including the is not null type
     */
    default List<JavaType> getScalarizedParameter(int index, boolean indexIncludesReceiverIfExists) {
        throw new UnsupportedOperationException("scalarized parameter not yet implemented");
    }

    /**
     * Gets the instance fields of a scalarized parameter as types.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return the instance fields as types
     */
    default List<JavaType> getScalarizedParameterNullRestricted(int index, boolean indexIncludesReceiverIfExists) {
        throw new UnsupportedOperationException("scalarized parameter not yet implemented");
    }

    /**
     * Gets the instance fields of a scalarized parameter.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return the instance fields of a scalarized parameter
     */
    default List<ResolvedJavaField> getScalarizedParameterFields(int index, boolean indexIncludesReceiverIfExists) {
        throw new UnsupportedOperationException("getParameterFields is not supported");
    }

    /**
     * Gets the type used for a scalarized parameter to represent its is not null information.
     *
     * @param index                         the index of a formal parameter in the signature
     * @param indexIncludesReceiverIfExists true if the receiver is included in the {@code index}, false otherwise
     * @return the type representing the is not null information
     */
    default JavaType getScalarizedParameterNonNullType(int index, boolean indexIncludesReceiverIfExists) {
        throw new UnsupportedOperationException("getScalarizedParameterNonNull is not supported");
    }

    /**
     * Gets the ordered type information of each field according to {@link ResolvedJavaType#getInstanceFields(boolean)} of the method's scalarized receiver.
     *
     * @return the scalarized receiver which consists of its instance fields.
     */
    default List<JavaType> getScalarizedReceiver() {
        assert hasScalarizedReceiver() : "Scalarized receiver presumed";
        return getScalarizedParameter(0, true);
    }
}
