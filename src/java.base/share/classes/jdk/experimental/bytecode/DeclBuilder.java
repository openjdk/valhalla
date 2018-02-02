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

package jdk.experimental.bytecode;

import java.util.function.Consumer;

public class DeclBuilder<S, T, E, D extends DeclBuilder<S, T, E, D>>
        extends AttributeBuilder<S, T, E, D> {

    protected int flags;
    AnnotationsBuilder<S, T, E> runtimeInvisibleAnnotations;
    AnnotationsBuilder<S, T, E> runtimeVisibleAnnotations;


    DeclBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
    }

    public D withFlags(Flag... flags) {
        for (Flag f : flags) {
            this.flags |= f.flag;
        }
        return thisBuilder();
    }

    public D withFlags(int flags) {
        withFlags(Flag.parse(flags));
        return thisBuilder();
    }

    public D withAnnotation(AnnotationsBuilder.Kind kind, T annoType) {
        getAnnotations(kind).withAnnotation(annoType, null);
        return thisBuilder();
    }

    public D withAnnotation(AnnotationsBuilder.Kind kind, T annoType, Consumer<? super AnnotationsBuilder<S, T, E>.AnnotationElementBuilder> annotations) {
        getAnnotations(kind).withAnnotation(annoType, annotations);
        return thisBuilder();
    }

    private AnnotationsBuilder<S, T, E> getAnnotations(AnnotationsBuilder.Kind kind) {
        switch (kind) {
            case RUNTIME_INVISIBLE:
                if (runtimeInvisibleAnnotations == null) {
                    runtimeInvisibleAnnotations = new AnnotationsBuilder<>(poolHelper, typeHelper);
                }
                return runtimeInvisibleAnnotations;
            case RUNTIME_VISIBLE:
                if (runtimeVisibleAnnotations == null) {
                    runtimeVisibleAnnotations = new AnnotationsBuilder<>(poolHelper, typeHelper);
                }
                return runtimeVisibleAnnotations;
        }
        throw new IllegalStateException();
    }

    void addAnnotations() {
        if (runtimeVisibleAnnotations != null) {
            withAttribute("RuntimeVisibleAnnotations", runtimeVisibleAnnotations.build());
        }
        if (runtimeInvisibleAnnotations != null) {
            withAttribute("RuntimeInvisibleAnnotations", runtimeVisibleAnnotations.build());
        }
    }
}
