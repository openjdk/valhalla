/*
 *  Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package java.lang.foreign;

import jdk.internal.foreign.layout.SequenceLayoutImpl;
import jdk.internal.javac.PreviewFeature;

/**
 * A compound layout that denotes a repetition of a given <em>element layout</em>.
 * The repetition count is said to be the sequence layout's <em>element count</em>. A finite sequence can be thought of as a
 * group layout where the sequence layout's element layout is repeated a number of times that is equal to the sequence
 * layout's element count. In other words this layout:
 *
 * {@snippet lang=java :
 * MemoryLayout.sequenceLayout(3, ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN));
 * }
 *
 * is equivalent to the following layout:
 *
 * {@snippet lang=java :
 * MemoryLayout.structLayout(
 *     ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN),
 *     ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN),
 *     ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN));
 * }
 *
 * @implSpec
 * This class is immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 *
 * @since 19
 */
@PreviewFeature(feature=PreviewFeature.Feature.FOREIGN)
public sealed interface SequenceLayout extends MemoryLayout permits SequenceLayoutImpl {


    /**
     * {@return the element layout associated with this sequence layout}
     */
    MemoryLayout elementLayout();

    /**
     * {@return the element count of this sequence layout}
     */
    long elementCount();

    /**
     * Returns a sequence layout with the same element layout, alignment constraint and name as this sequence layout,
     * but with the specified element count.
     * @param elementCount the new element count.
     * @return a sequence layout with the given element count.
     * @throws IllegalArgumentException if {@code elementCount < 0}.
     */
    SequenceLayout withElementCount(long elementCount);

    /**
     * Re-arrange the elements in this sequence layout into a multi-dimensional sequence layout.
     * The resulting layout is a sequence layout where element layouts in the flattened projection of this
     * sequence layout (see {@link #flatten()}) are re-arranged into one or more nested sequence layouts
     * according to the provided element counts. This transformation preserves the layout size;
     * that is, multiplying the provided element counts must yield the same element count
     * as the flattened projection of this sequence layout.
     * <p>
     * For instance, given a sequence layout of the kind:
     * {@snippet lang=java :
     * var seq = MemoryLayout.sequenceLayout(4, MemoryLayout.sequenceLayout(3, ValueLayout.JAVA_INT));
     * }
     * calling {@code seq.reshape(2, 6)} will yield the following sequence layout:
     * {@snippet lang=java :
     * var reshapeSeq = MemoryLayout.sequenceLayout(2, MemoryLayout.sequenceLayout(6, ValueLayout.JAVA_INT));
     * }
     * <p>
     * If one of the provided element count is the special value {@code -1}, then the element
     * count in that position will be inferred from the remaining element counts and the
     * element count of the flattened projection of this layout. For instance, a layout equivalent to
     * the above {@code reshapeSeq} can also be computed in the following ways:
     * {@snippet lang=java :
     * var reshapeSeqImplicit1 = seq.reshape(-1, 6);
     * var reshapeSeqImplicit2 = seq.reshape(2, -1);
     * }
     * @param elementCounts an array of element counts, of which at most one can be {@code -1}.
     * @return a sequence layout where element layouts in the flattened projection of this
     * sequence layout (see {@link #flatten()}) are re-arranged into one or more nested sequence layouts.
     * @throws IllegalArgumentException if two or more element counts are set to {@code -1}, or if one
     * or more element count is {@code <= 0} (but other than {@code -1}) or, if, after any required inference,
     * multiplying the element counts does not yield the same element count as the flattened projection of this
     * sequence layout.
     */
    SequenceLayout reshape(long... elementCounts);

    /**
     * Returns a flattened sequence layout. The element layout of the returned sequence layout
     * is the first non-sequence element layout found by recursively traversing the element layouts of this sequence layout.
     * This transformation preserves the layout size; nested sequence layout in this sequence layout will
     * be dropped and their element counts will be incorporated into that of the returned sequence layout.
     * For instance, given a sequence layout of the kind:
     * {@snippet lang=java :
     * var seq = MemoryLayout.sequenceLayout(4, MemoryLayout.sequenceLayout(3, ValueLayout.JAVA_INT));
     * }
     * calling {@code seq.flatten()} will yield the following sequence layout:
     * {@snippet lang=java :
     * var flattenedSeq = MemoryLayout.sequenceLayout(12, ValueLayout.JAVA_INT);
     * }
     * @return a sequence layout with the same size as this layout (but, possibly, with different
     * element count), whose element layout is not a sequence layout.
     */
    SequenceLayout flatten();

    @Override
    SequenceLayout withName(String name);

    @Override
    SequenceLayout withBitAlignment(long bitAlignment);
}
