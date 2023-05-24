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
package jdk.internal.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jdk.internal.classfile.impl.TransformImpl;

/**
 * A transformation on streams of {@link FieldElement}.
 *
 * @see ClassfileTransform
 */
@FunctionalInterface
public non-sealed interface FieldTransform
        extends ClassfileTransform<FieldTransform, FieldElement, FieldBuilder> {

    /**
     * A field transform that sends all elements to the builder.
     */
    FieldTransform ACCEPT_ALL = new FieldTransform() {
        @Override
        public void accept(FieldBuilder builder, FieldElement element) {
            builder.with(element);
        }
    };

    /**
     * Create a stateful field transform from a {@link Supplier}.  The supplier
     * will be invoked for each transformation.
     *
     * @param supplier a {@link Supplier} that produces a fresh transform object
     *                 for each traversal
     * @return the stateful field transform
     */
    static FieldTransform ofStateful(Supplier<FieldTransform> supplier) {
        return new TransformImpl.SupplierFieldTransform(supplier);
    }

    /**
     * Create a field transform that passes each element through to the builder,
     * and calls the specified function when transformation is complete.
     *
     * @param finisher the function to call when transformation is complete
     * @return the field transform
     */
    static FieldTransform endHandler(Consumer<FieldBuilder> finisher) {
        return new FieldTransform() {
            @Override
            public void accept(FieldBuilder builder, FieldElement element) {
                builder.with(element);
            }

            @Override
            public void atEnd(FieldBuilder builder) {
                finisher.accept(builder);
            }
        };
    }

    /**
     * Create a field transform that passes each element through to the builder,
     * except for those that the supplied {@link Predicate} is true for.
     *
     * @param filter the predicate that determines which elements to drop
     * @return the field transform
     */
    static FieldTransform dropping(Predicate<FieldElement> filter) {
        return (b, e) -> {
            if (!filter.test(e))
                b.with(e);
        };
    }

    @Override
    default FieldTransform andThen(FieldTransform t) {
        return new TransformImpl.ChainedFieldTransform(this, t);
    }

    @Override
    default ResolvedTransform<FieldElement> resolve(FieldBuilder builder) {
        return new TransformImpl.ResolvedTransformImpl<>(e -> accept(builder, e),
                                                         () -> atEnd(builder),
                                                         () -> atStart(builder));
    }
}
