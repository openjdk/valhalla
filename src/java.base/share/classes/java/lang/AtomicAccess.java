/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * A primitive class implements the {@code AtomicAccess} interface to
 * request that the JVM take extra care to avoid non-atomic operations
 * when loading or storing any value of the class to a field or array
 * element.  Normally, only naturally atomic fields and fields declared
 * {@code volatile} are always atomic, but a class that implements
 * this marker interface will always be accessed atomically, even when
 * they are stored in array elements or in non-{@code volatile}
 * fields, and even when multiple threads perform racing writes.
 *
 * <p> A primitive instance of multiple components can experience both
 * transient and persistent access atomicity failures.
 *
 * <p> Transient failures usually arise from write-read conflicts:
 * when one thread writes the components one-by-one, and another
 * thread reads the components one-by-one. In doing so, reader
 * observes the intermediate state of the primitive instance.
 * This failure is transient, since "after" (in memory model sense)
 * the writer finishes its writes, the instance is observed in full
 * by any subsequent observer.
 *
 * <p> Permanent failures usually arise from write-write conflicts:
 * when two threads compete to write the components, and one thread
 * writes some components while another thread writes other
 * components. This failure is permanent: as every subsequent observer
 * will read a hybrid composed of field values from both racing writes.
 *
 * Both these effects can be described as if the Java memory model
 * break up primitive class instance reads and writes into reads and
 * writes of their various fields, as it does with longs and doubles
 * (JLS 17.7).
 *
 * <p> In extreme cases, the hybrid observed under non-atomic access
 * might be a value which is impossible to construct by normal means.
 * If data integrity or security depends on proper construction,
 * the class should be declared as implementing {@code AtomicAccess}.
 *
 * <p> Note this atomicity guarantee only relates to the <i>individual
 * accesses</i>, not the compound operations over the values. The
 * read-modify-write operation over {@code AtomicAccess} would still
 * be non-atomic, unless specifically written with appropriate
 * synchronization.
 *
 * @author  John Rose
 * @since   (valhalla)
 */
public interface AtomicAccess {
    // TO DO: Finalize name.
    // TO DO: Decide whether and how to restrict this type to
    // primitive classes only, or if not, whether to document its
    // non-effect on identity classes.
}
