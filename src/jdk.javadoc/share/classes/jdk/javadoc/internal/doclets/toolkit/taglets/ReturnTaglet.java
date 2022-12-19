/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.taglets;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReturnTree;
import jdk.javadoc.doclet.Taglet.Location;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder.Result;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * A taglet that represents the {@code @return} and {@code {@return }} tags.
 */
public class ReturnTaglet extends BaseTaglet implements InheritableTaglet {

    public ReturnTaglet() {
        super(DocTree.Kind.RETURN, true, EnumSet.of(Location.METHOD));
    }

    @Override
    public boolean isBlockTag() {
        return true;
    }

    @Override
    public Output inherit(Element owner, DocTree tag, boolean isFirstSentence, BaseConfiguration configuration) {
        try {
            var docFinder = configuration.utils.docFinder();
            var r = docFinder.trySearch((ExecutableElement) owner, m -> Result.fromOptional(extract(configuration.utils, m))).toOptional();
            return r.map(result -> new Output(result.returnTree, result.method, result.returnTree.getDescription(), true))
                    .orElseGet(() -> new Output(null, null, List.of(), true));
        } catch (DocFinder.NoOverriddenMethodsFound e) {
            return new Output(null, null, List.of(), false);
        }
    }

    @Override
    public Content getInlineTagOutput(Element element, DocTree tag, TagletWriter writer) {
        return writer.returnTagOutput(element, (ReturnTree) tag, true);
    }

    @Override
    public Content getAllBlockTagOutput(Element holder, TagletWriter writer) {
        assert holder.getKind() == ElementKind.METHOD : holder.getKind();
        var method = (ExecutableElement) holder;
        Messages messages = writer.configuration().getMessages();
        Utils utils = writer.configuration().utils;
        List<? extends ReturnTree> tags = utils.getReturnTrees(holder);

        // make sure we are not using @return on a method with the void return type
        TypeMirror returnType = utils.getReturnType(writer.getCurrentPageElement(), method);
        if (returnType != null && utils.isVoid(returnType)) {
            if (!tags.isEmpty() && !writer.configuration().isDocLintReferenceGroupEnabled()) {
                messages.warning(holder, "doclet.Return_tag_on_void_method");
            }
            return null;
        }

        // it would also be good to check if there are more than one @return
        // tags and produce a warning or error similarly to how it's done
        // above for a case where @return is used for void

        var docFinder = utils.docFinder();
        return docFinder.search(method, m -> Result.fromOptional(extract(utils, m))).toOptional()
                .map(r -> writer.returnTagOutput(r.method, r.returnTree, false))
                .orElse(null);
    }

    private record Documentation(ReturnTree returnTree, ExecutableElement method) { }

    private static Optional<Documentation> extract(Utils utils, ExecutableElement method) {
        // TODO
        //  Using getBlockTags(..., Kind.RETURN) for clarity. Since @return has become a bimodal tag,
        //  Utils.getReturnTrees is now a misnomer: it returns only block returns, not all returns.
        //  We could revisit this later.
        Stream<? extends ReturnTree> blockTags = utils.getBlockTags(method, DocTree.Kind.RETURN, ReturnTree.class).stream();
        Stream<? extends ReturnTree> mainDescriptionTags = utils.getFirstSentenceTrees(method).stream()
                .mapMulti((t, c) -> {
                    if (t.getKind() == DocTree.Kind.RETURN) c.accept((ReturnTree) t);
                });
        // this method should not check validity of @return tags, hence findAny and not findFirst or what have you
        return Stream.concat(blockTags, mainDescriptionTags)
                .map(t -> new Documentation(t, method)).findAny();
    }
}
