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
 * A primitive class implements the {@code NonTearable} interface to
 * request that the JVM take extra care to avoid structure tearing
 * when loading or storing any value of the class to a field or array
 * element.  Normally, only fields declared {@code volatile} are
 * protected against structure tearing, but a class that implements
 * this marker interface will never have its values torn, even when
 * they are stored in array elements or in non-{@code volatile}
 * fields, and even when multiple threads perform racing writes.
 *
 * <p> An primitive instance of multiple components is said to be "torn"
 * when two racing threads compete to write those components, and one
 * thread writes some components while another thread writes other
 * components, so a subsequent observer will read a hybrid composed,
 * as if "out of thin air", of field values from both racing writes.
 * Tearing can also occur when the effects of two non-racing writes
 * are observed by a racing read.  In general, structure tearing
 * requires a read and two writes (initialization counting as a write)
 * of a multi-component value, with a race between any two of the
 * accesses.  The effect can also be described as if the Java memory
 * model break up primitive classinstance reads and writes into reads and
 * writes of their various fields, as it does with longs and doubles
 * (JLS 17.7).
 *
 * <p> In extreme cases, the hybrid observed after structure tearing
 * might be a value which is impossible to construct by normal means.
 * If data integrity or security depends on proper construction,
 * the class should be declared as implementing {@code NonTearable}.
 *
 * @author  John Rose
 * @since   (valhalla)
 */
public interface NonTearable {
    // TO DO: Finalize name.
    // TO DO: Decide whether and how to restrict this type to
    // primitive classes only, or if not, whether to document its
    // non-effect on identity classes.
}
