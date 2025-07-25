/*
 * Copyright (c) 2009, 2025, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import jdk.internal.javac.PreviewFeature;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;

import java.util.function.Supplier;

/**
 * This class consists of {@code static} utility methods for operating
 * on objects, or checking certain conditions before operation.  These utilities
 * include {@code null}-safe or {@code null}-tolerant methods for computing the
 * hash code of an object, returning a string for an object, comparing two
 * objects, and checking if indexes or sub-range values are out of bounds.
 *
 * @since 1.7
 */
public final class Objects {
    private Objects() {
        throw new AssertionError("No java.util.Objects instances for you!");
    }

    /**
     * {@return {@code true} if the arguments are equal to each other
     * and {@code false} otherwise}
     * Consequently, if both arguments are {@code null}, {@code true}
     * is returned.  Otherwise, if the first argument is not {@code
     * null}, equality is determined by calling the {@link
     * Object#equals equals} method of the first argument with the
     * second argument of this method. Otherwise, {@code false} is
     * returned.
     *
     * @param a an object
     * @param b an object to be compared with {@code a} for equality
     * @see Object#equals(Object)
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

   /**
    * {@return {@code true} if the arguments are deeply equal to each other
    * and {@code false} otherwise}
    *
    * Two {@code null} values are deeply equal.  If both arguments are
    * arrays, the algorithm in {@link Arrays#deepEquals(Object[],
    * Object[]) Arrays.deepEquals} is used to determine equality.
    * Otherwise, equality is determined by using the {@link
    * Object#equals equals} method of the first argument.
    *
    * @param a an object
    * @param b an object to be compared with {@code a} for deep equality
    * @see Arrays#deepEquals(Object[], Object[])
    * @see Objects#equals(Object, Object)
    */
    public static boolean deepEquals(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return Arrays.deepEquals0(a, b);
    }

    /**
     * {@return the hash code of a non-{@code null} argument and 0 for
     * a {@code null} argument}
     *
     * @param o an object
     * @see Object#hashCode
     */
    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

   /**
    * {@return a hash code for a sequence of input values} The hash
    * code is generated as if all the input values were placed into an
    * array, and that array were hashed by calling {@link
    * Arrays#hashCode(Object[])}.
    *
    * <p>This method is useful for implementing {@link
    * Object#hashCode()} on objects containing multiple fields. For
    * example, if an object that has three fields, {@code x}, {@code
    * y}, and {@code z}, one could write:
    *
    * <blockquote><pre>
    * &#064;Override public int hashCode() {
    *     return Objects.hash(x, y, z);
    * }
    * </pre></blockquote>
    *
    * <b>Warning: When a single object reference is supplied, the returned
    * value does not equal the hash code of that object reference.</b> This
    * value can be computed by calling {@link #hashCode(Object)}.
    *
    * @param values the values to be hashed
    * @see Arrays#hashCode(Object[])
    * @see List#hashCode
    */
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    /**
     * {@return the result of calling {@code toString} for a
     * non-{@code null} argument and {@code "null"} for a
     * {@code null} argument}
     *
     * @param o an object
     * @see Object#toString
     * @see String#valueOf(Object)
     */
    public static String toString(Object o) {
        return String.valueOf(o);
    }

    /**
     * {@return the result of calling {@code toString} on the first
     * argument if the first argument is not {@code null} and the
     * second argument otherwise}
     *
     * @param o an object
     * @param nullDefault string to return if the first argument is
     *        {@code null}
     * @see Objects#toString(Object)
     */
    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }

    /**
     * {@return a string equivalent to the string returned by {@code
     * Object.toString} if that method and {@code hashCode} are not
     * overridden}
     *
     * @implNote
     * This method constructs a string for an object without calling
     * any overridable methods of the object.
     *
     * @implSpec
     * The method returns a string equivalent to:<br>
     * {@code o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o))}
     *
     * @param o an object
     * @throws NullPointerException if the argument is null
     * @see Object#toString
     * @see System#identityHashCode(Object)
     * @since 19
     */
    public static String toIdentityString(Object o) {
        requireNonNull(o);
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

   /**
    * {@return {@code true} if the object is a non-null reference
    * to an {@linkplain Class#isIdentity() identity object}, otherwise {@code false}}
    *
    * @apiNote
    * If the parameter is {@code null}, there is no object
    * and hence no class to check for identity; the return is {@code false}.
    * To test for a {@linkplain Class#isValue() value object} use:
    * {@snippet type="java" :
    *     if (obj != null && !Objects.hasIdentity(obj)) {
    *         // obj is a non-null value object
    *     }
    * }
    * @param obj an object or {@code null}
    * @since Valhalla
    */
   @PreviewFeature(feature = PreviewFeature.Feature.VALUE_OBJECTS)
//    @IntrinsicCandidate
    public static boolean hasIdentity(Object obj) {
        return (obj == null) ? false : obj.getClass().isIdentity();
    }

   /**
    * {@return {@code true} if the object is a non-null reference
    * to a {@linkplain Class#isValue() value object}, otherwise {@code false}}
    *
    * @param obj an object or {@code null}
    * @since Valhalla
    */
   @PreviewFeature(feature = PreviewFeature.Feature.VALUE_OBJECTS)
//    @IntrinsicCandidate
    public static boolean isValueObject(Object obj) {
        return (obj == null) ? false : obj.getClass().isValue();
    }

    /**
     * Checks that the specified object reference is an identity object.
     *
     * @param obj the object reference to check for identity
     * @param <T> the type of the reference
     * @return {@code obj} if {@code obj} is an identity object
     * @throws NullPointerException if {@code obj} is {@code null}
     * @throws IdentityException if {@code obj} is not an identity object
     * @since Valhalla
     */
    @PreviewFeature(feature = PreviewFeature.Feature.VALUE_OBJECTS)
    @ForceInline
    public static <T> T requireIdentity(T obj) {
        Objects.requireNonNull(obj);
        if (!hasIdentity(obj))
            throw new IdentityException(obj.getClass());
        return obj;
    }

    /**
     * Checks that the specified object reference is an identity object.
     *
     * @param obj the object reference to check for identity
     * @param message detail message to be used in the event that an
     *        {@code IdentityException} is thrown; may be null
     * @param <T> the type of the reference
     * @return {@code obj} if {@code obj} is an identity object
     * @throws NullPointerException if {@code obj} is {@code null}
     * @throws IdentityException if {@code obj} is not an identity object
     * @since Valhalla
     */
    @PreviewFeature(feature = PreviewFeature.Feature.VALUE_OBJECTS)
    @ForceInline
    public static <T> T requireIdentity(T obj, String message) {
        Objects.requireNonNull(obj);
        if (!hasIdentity(obj))
            throw new IdentityException(message);
        return obj;
    }

    /**
     * Checks that the specified object reference is an identity object.
     *
     * @param obj the object reference to check for identity
     * @param messageSupplier supplier of the detail message to be
     *        used in the event that an {@code IdentityException} is thrown; may be null
     * @param <T> the type of the reference
     * @return {@code obj} if {@code obj} is an identity object
     * @throws NullPointerException if {@code obj} is {@code null}
     * @throws IdentityException if {@code obj} is not an identity object
     * @since Valhalla
     */
    @PreviewFeature(feature = PreviewFeature.Feature.VALUE_OBJECTS)
    @ForceInline
    public static <T> T requireIdentity(T obj, Supplier<String> messageSupplier) {
        Objects.requireNonNull(obj);
        if (!hasIdentity(obj))
            throw new IdentityException(messageSupplier == null ?
                    null : messageSupplier.get());
        return obj;
    }

    /**
     * {@return 0 if the arguments are identical and {@code
     * c.compare(a, b)} otherwise}
     * Consequently, if both arguments are {@code null} 0
     * is returned.
     *
     * <p>Note that if one of the arguments is {@code null}, a {@code
     * NullPointerException} may or may not be thrown depending on
     * what ordering policy, if any, the {@link Comparator Comparator}
     * chooses to have for {@code null} values.
     *
     * @param <T> the type of the objects being compared
     * @param a an object
     * @param b an object to be compared with {@code a}
     * @param c the {@code Comparator} to compare the first two arguments
     * @see Comparable
     * @see Comparator
     */
    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        return (a == b) ? 0 :  c.compare(a, b);
    }

    /**
     * Checks that the specified object reference is not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar) {
     *     this.bar = Objects.requireNonNull(bar);
     * }
     * </pre></blockquote>
     *
     * @param obj the object reference to check for nullity
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @ForceInline
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    /**
     * Checks that the specified object reference is not {@code null} and
     * throws a customized {@link NullPointerException} if it is. This method
     * is designed primarily for doing parameter validation in methods and
     * constructors with multiple parameters, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar, Baz baz) {
     *     this.bar = Objects.requireNonNull(bar, "bar must not be null");
     *     this.baz = Objects.requireNonNull(baz, "baz must not be null");
     * }
     * </pre></blockquote>
     *
     * @param obj     the object reference to check for nullity
     * @param message detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @ForceInline
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

    /**
     * {@return {@code true} if the provided reference is {@code
     * null}; {@code false} otherwise}
     *
     * @apiNote This method exists to be used as a
     * {@link java.util.function.Predicate}, {@code filter(Objects::isNull)}
     *
     * @param obj a reference to be checked against {@code null}
     *
     * @see java.util.function.Predicate
     * @since 1.8
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * {@return {@code true} if the provided reference is non-{@code null};
     * {@code false} otherwise}
     *
     * @apiNote This method exists to be used as a
     * {@link java.util.function.Predicate}, {@code filter(Objects::nonNull)}
     *
     * @param obj a reference to be checked against {@code null}
     *
     * @see java.util.function.Predicate
     * @since 1.8
     */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * {@return the first argument if it is non-{@code null} and
     * otherwise the second argument if it is non-{@code null}}
     *
     * @param obj an object
     * @param defaultObj a non-{@code null} object to return if the first argument
     *                   is {@code null}
     * @param <T> the type of the reference
     * @throws NullPointerException if both {@code obj} is null and
     *        {@code defaultObj} is {@code null}
     * @since 9
     */
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
    }

    /**
     * {@return the first argument if it is non-{@code null} and
     * otherwise the value from {@code supplier.get()} if it is
     * non-{@code null}}
     *
     * @param obj an object
     * @param supplier of a non-{@code null} object to return if the first argument
     *                 is {@code null}
     * @param <T> the type of the first argument and return type
     * @throws NullPointerException if both {@code obj} is null and
     *        either the {@code supplier} is {@code null} or
     *        the {@code supplier.get()} value is {@code null}
     * @since 9
     */
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier) {
        return (obj != null) ? obj
                : requireNonNull(requireNonNull(supplier, "supplier").get(), "supplier.get()");
    }

    /**
     * Checks that the specified object reference is not {@code null} and
     * throws a customized {@link NullPointerException} if it is.
     *
     * <p>Unlike the method {@link #requireNonNull(Object, String)},
     * this method allows creation of the message to be deferred until
     * after the null check is made. While this may confer a
     * performance advantage in the non-null case, when deciding to
     * call this method care should be taken that the costs of
     * creating the message supplier are less than the cost of just
     * creating the string message directly.
     *
     * @param obj     the object reference to check for nullity
     * @param messageSupplier supplier of the detail message to be
     * used in the event that a {@code NullPointerException} is thrown
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.8
     */
    public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
        if (obj == null)
            throw new NullPointerException(messageSupplier == null ?
                                           null : messageSupplier.get());
        return obj;
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The {@code index} is defined to be out of bounds if any of the
     * following inequalities is true:
     * <ul>
     *  <li>{@code index < 0}</li>
     *  <li>{@code index >= length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @return {@code index} if it is within bounds of the range
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     * @since 9
     */
    @ForceInline
    public static
    int checkIndex(int index, int length) {
        return Preconditions.checkIndex(index, length, null);
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code fromIndex > toIndex}</li>
     *  <li>{@code toIndex > length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 9
     */
    public static
    int checkFromToIndex(int fromIndex, int toIndex, int length) {
        return Preconditions.checkFromToIndex(fromIndex, toIndex, length, null);
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code size < 0}</li>
     *  <li>{@code fromIndex + size > length}, taking into account integer overflow</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 9
     */
    public static
    int checkFromIndexSize(int fromIndex, int size, int length) {
        return Preconditions.checkFromIndexSize(fromIndex, size, length, null);
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The {@code index} is defined to be out of bounds if any of the
     * following inequalities is true:
     * <ul>
     *  <li>{@code index < 0}</li>
     *  <li>{@code index >= length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @return {@code index} if it is within bounds of the range
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     * @since 16
     */
    @ForceInline
    public static
    long checkIndex(long index, long length) {
        return Preconditions.checkIndex(index, length, null);
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code fromIndex > toIndex}</li>
     *  <li>{@code toIndex > length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 16
     */
    public static
    long checkFromToIndex(long fromIndex, long toIndex, long length) {
        return Preconditions.checkFromToIndex(fromIndex, toIndex, length, null);
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code size < 0}</li>
     *  <li>{@code fromIndex + size > length}, taking into account integer overflow</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 16
     */
    public static
    long checkFromIndexSize(long fromIndex, long size, long length) {
        return Preconditions.checkFromIndexSize(fromIndex, size, length, null);
    }
}
