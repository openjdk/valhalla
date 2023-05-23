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
import java.util.function.Supplier;

import jdk.internal.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * A transformation on streams of elements. Transforms are used during
 * transformation of classfile entities; a transform is provided to a method like
 * {@link ClassModel#transform(ClassTransform)}, and the elements of the class,
 * along with a builder, are presented to the transform.
 *
 * <p>The subtypes of {@linkplain
 * ClassfileTransform} (e.g., {@link ClassTransform}) are functional interfaces
 * that accept an element and a corresponding builder.  Since any element can be
 * reproduced on the builder via {@link ClassBuilder#with(ClassfileElement)}, a
 * transform can easily leave elements in place, remove them, replace them, or
 * augment them with other elements.  This enables localized transforms to be
 * represented concisely.
 *
 * <p>Transforms also have an {@link #atEnd(ClassfileBuilder)} method, for
 * which the default implementation does nothing, so that a transform can
 * perform additional building after the stream of elements is exhausted.
 *
 * <p>Transforms can be chained together via the {@link
 * #andThen(ClassfileTransform)} method, so that the output of one becomes the
 * input to another.  This allows smaller units of transformation to be captured
 * and reused.
 *
 * <p>Some transforms are stateful; for example, a transform that injects an
 * annotation on a class may watch for the {@link RuntimeVisibleAnnotationsAttribute}
 * element and transform it if found, but if it is not found, will generate a
 * {@linkplain RuntimeVisibleAnnotationsAttribute} element containing the
 * injected annotation from the {@linkplain #atEnd(ClassfileBuilder)} handler.
 * To do this, the transform must accumulate some state during the traversal so
 * that the end handler knows what to do.  If such a transform is to be reused,
 * its state must be reset for each traversal; this will happen automatically if
 * the transform is created with {@link ClassTransform#ofStateful(Supplier)} (or
 * corresponding methods for other classfile locations.)
 * <p>
 * Class transformation sample where code transformation is stateful:
 * {@snippet lang="java" class="PackageSnippets" region="codeRelabeling"}
 * <p>
 * Complex class instrumentation sample chaining multiple transformations:
 * {@snippet lang="java" class="PackageSnippets" region="classInstrumentation"}
 */
public sealed interface ClassfileTransform<
        C extends ClassfileTransform<C, E, B>,
        E extends ClassfileElement,
        B extends ClassfileBuilder<E, B>>
        permits ClassTransform, FieldTransform, MethodTransform, CodeTransform {
    /**
     * Transform an element by taking the appropriate actions on the builder.
     * Used when transforming a classfile entity (class, method, field, method
     * body.) If no transformation is desired, the element can be presented to
     * {@link B#with(ClassfileElement)}.  If the element is to be dropped, no
     * action is required.
     *
     * @param builder the builder for the new entity
     * @param element the element
     */
    void accept(B builder, E element);

    /**
     * Take any final action during transformation of a classfile entity.  Called
     * after all elements of the class are presented to {@link
     * #accept(ClassfileBuilder, ClassfileElement)}.
     *
     * @param builder the builder for the new entity
     * @implSpec The default implementation does nothing.
     */
    default void atEnd(B builder) {
    }

    /**
     * Take any preliminary action during transformation of a classfile entity.
     * Called before any elements of the class are presented to {@link
     * #accept(ClassfileBuilder, ClassfileElement)}.
     *
     * @param builder the builder for the new entity
     * @implSpec The default implementation does nothing.
     */
    default void atStart(B builder) {
    }

    /**
     * Chain this transform with another; elements presented to the builder of
     * this transform will become the input to the next transform.
     *
     * @param next the downstream transform
     * @return the chained transform
     */
    C andThen(C next);

    /**
     * The result of binding a transform to a builder.  Used primarily within
     * the implementation to perform transformation.
     *
     * @param <E> the element type
     */
    interface ResolvedTransform<E extends ClassfileElement> {
        /**
         * {@return a {@link Consumer} to receive elements}
         */
        Consumer<E> consumer();

        /**
         * {@return an action to call at the end of transformation}
         */
        Runnable endHandler();

        /**
         * {@return an action to call at the start of transformation}
         */
        Runnable startHandler();
    }

    /**
     * Bind a transform to a builder.  If the transform is chained, intermediate
     * builders are created for each chain link.  If the transform is stateful
     * (see, e.g., {@link ClassTransform#ofStateful(Supplier)}), the supplier is
     * invoked to get a fresh transform object.
     *
     * <p>This method is a low-level method that should rarely be used by
     * user code; most of the time, user code should prefer
     * {@link ClassfileBuilder#transform(CompoundElement, ClassfileTransform)},
     * which resolves the transform and executes it on the current builder.
     *
     * @param builder the builder to bind to
     * @return the bound result
     */
    ResolvedTransform<E> resolve(B builder);
}
