/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocTree;

import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlAttr;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.CommentUtils;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.PropertyUtils;
import jdk.javadoc.internal.doclets.toolkit.util.ClassTree;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.ANNOTATION_TYPE_MEMBER_OPTIONAL;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.ANNOTATION_TYPE_MEMBER_REQUIRED;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.CONSTRUCTORS;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.ENUM_CONSTANTS;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.FIELDS;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.METHODS;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.NESTED_CLASSES;
import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.PROPERTIES;

/**
 * Generate the Class Information Page.
 *
 * @see javax.lang.model.element.TypeElement
 */
public class ClassWriter extends SubWriterHolderWriter {

    private static final Set<String> suppressSubtypesSet
            = Set.of("java.lang.Object",
                     "org.omg.CORBA.Object");

    private static final Set<String> suppressImplementingSet
            = Set.of("java.lang.Cloneable",
                     "java.lang.constant.Constable",
                     "java.lang.constant.ConstantDesc",
                     "java.io.Serializable");

    protected final TypeElement typeElement;
    protected final VisibleMemberTable visibleMemberTable;

    protected final ClassTree classTree;

    private final Comparator<Element> summariesComparator;
    private final PropertyUtils.PropertyHelper pHelper;

    /**
     * @param configuration the configuration data for the doclet
     * @param typeElement the class being documented.
     * @param classTree the class tree for the given class.
     */
    public ClassWriter(HtmlConfiguration configuration, TypeElement typeElement,
                       ClassTree classTree) {
        super(configuration, configuration.docPaths.forClass(typeElement));
        this.typeElement = typeElement;
        configuration.currentTypeElement = typeElement;
        this.classTree = classTree;

        visibleMemberTable = configuration.getVisibleMemberTable(typeElement);
        summariesComparator = utils.comparators.makeIndexElementComparator();
        pHelper = new PropertyUtils.PropertyHelper(configuration, typeElement);

        switch (typeElement.getKind()) {
            case ENUM   -> setEnumDocumentation(typeElement);
            case RECORD -> setRecordDocumentation(typeElement);
        }
    }

    public void build() throws DocletException {
        buildClassDoc();
    }

    /**
     * Handles the {@literal <TypeElement>} tag.
     *
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildClassDoc() throws DocletException {
        String key = switch (typeElement.getKind()) {
            case INTERFACE       -> "doclet.Interface";
            case ENUM            -> "doclet.Enum";
            case RECORD          -> "doclet.RecordClass";
            case ANNOTATION_TYPE -> "doclet.AnnotationType";
            case CLASS           -> "doclet.Class";
            default -> throw new IllegalStateException(typeElement.getKind() + " " + typeElement);
        };
        Content content = getHeader(resources.getText(key) + " " + utils.getSimpleName(typeElement));
        Content classContent = getClassContentHeader();

        buildClassTree(classContent);
        buildClassInfo(classContent);
        buildMemberSummary(classContent);
        buildMemberDetails(classContent);

        addClassContent(classContent);
        addFooter();
        printDocument(content);
        copyDocFiles();
    }

    /**
     * Build the class inheritance tree documentation.
     *
     * @param classContent the content to which the documentation will be added
     */
    protected void buildClassTree(Content classContent) {
        addClassTree(classContent);
    }

    /**
     * Build the class information documentation.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildClassInfo(Content target) {
        Content c = getOutputInstance();
        buildParamInfo(c);
        buildSuperInterfacesInfo(c);
        buildImplementedInterfacesInfo(c);
        buildSubClassInfo(c);
        buildSubInterfacesInfo(c);
        buildInterfaceUsageInfo(c);
        buildNestedClassInfo(c);
        buildFunctionalInterfaceInfo(c);
        buildClassSignature(c);
        buildDeprecationInfo(c);
        buildClassDescription(c);
        buildClassTagInfo(c);

        target.add(getClassInfo(c));
    }

    /**
     * Build the type parameters and state components of this class.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildParamInfo(Content target) {
        addParamInfo(target);
    }

    /**
     * If this is an interface, list all superinterfaces.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildSuperInterfacesInfo(Content target) {
        addSuperInterfacesInfo(target);
    }

    /**
     * If this is a class, list all interfaces implemented by this class.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildImplementedInterfacesInfo(Content target) {
        addImplementedInterfacesInfo(target);
    }

    /**
     * List all the classes that extend this one.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildSubClassInfo(Content target) {
        addSubClassInfo(target);
    }

    /**
     * List all the interfaces that extend this one.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildSubInterfacesInfo(Content target) {
        addSubInterfacesInfo(target);
    }

    /**
     * If this is an interface, list all classes that implement this interface.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildInterfaceUsageInfo(Content target) {
        addInterfaceUsageInfo(target);
    }

    /**
     * If this is a functional interface, display appropriate message.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildFunctionalInterfaceInfo(Content target) {
        addFunctionalInterfaceInfo(target);
    }

    /**
     * If this class is deprecated, build the appropriate information.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content target) {
        addClassDeprecationInfo(target);
    }

    /**
     * If this is an inner class or interface, list the enclosing class or interface.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildNestedClassInfo(Content target) {
        addNestedClassInfo(target);
    }

    /**
     * Copy the doc files.
     *
     * @throws DocFileIOException if there is a problem while copying the files
     */
    private void copyDocFiles() throws DocletException {
        PackageElement containingPackage = utils.containingPackage(typeElement);
        var containingPackagesSeen = configuration.getContainingPackagesSeen();
        if ((configuration.packages == null ||
                !configuration.packages.contains(containingPackage)) &&
                !containingPackagesSeen.contains(containingPackage)) {
            //Only copy doc files dir if the containing package is not
            //documented AND if we have not documented a class from the same
            //package already. Otherwise, we are making duplicate copies.
            var docFilesHandler = configuration
                    .getWriterFactory()
                    .getDocFilesHandler(containingPackage);
            docFilesHandler.copyDocFiles();
            containingPackagesSeen.add(containingPackage);
        }
    }

    /**
     * Build the signature of the current class.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildClassSignature(Content target) {
        addClassSignature(target);
    }

    /**
     * Build the class description.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildClassDescription(Content target) {
        addClassDescription(target);
    }

    /**
     * Build the tag information for the current class.
     *
     * @param target the content to which the documentation will be added
     */
    protected void buildClassTagInfo(Content target) {
        addClassTagInfo(target);
    }

    /**
     * Build the member summary contents of the page.
     *
     * @param classContent the content to which the documentation will be added
     */
    protected void buildMemberSummary(Content classContent) {
        Content summariesList = getSummariesList();
        buildSummaries(summariesList);
        classContent.add(getMemberSummary(summariesList));
    }

    protected void buildSummaries(Content target) {
        buildPropertiesSummary(target);
        buildNestedClassesSummary(target);
        buildEnumConstantsSummary(target);
        buildAnnotationTypeRequiredMemberSummary(target);
        buildAnnotationTypeOptionalMemberSummary(target);
        buildFieldsSummary(target);
        buildConstructorsSummary(target);
        buildMethodsSummary(target);
    }

    /**
     * Builds the summary for any optional members of an annotation type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildAnnotationTypeOptionalMemberSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(ANNOTATION_TYPE_MEMBER_OPTIONAL);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getAnnotationTypeOptionalMemberWriter(this);
        addSummary(writer, ANNOTATION_TYPE_MEMBER_OPTIONAL, false, summariesList);
    }

    /**
     * Builds the summary for any required members of an annotation type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildAnnotationTypeRequiredMemberSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(ANNOTATION_TYPE_MEMBER_REQUIRED);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getAnnotationTypeRequiredMemberWriter(this);
        addSummary(writer, ANNOTATION_TYPE_MEMBER_REQUIRED, false, summariesList);
    }

    /**
     * Builds the summary for any enum constants of an enum type.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildEnumConstantsSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(ENUM_CONSTANTS);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getEnumConstantWriter(this);
        addSummary(writer, ENUM_CONSTANTS, false, summariesList);
    }

    /**
     * Builds the summary for any fields.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildFieldsSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(FIELDS);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getFieldWriter(this);
        addSummary(writer, FIELDS, true, summariesList);
    }

    /**
     * Builds the summary for any properties.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildPropertiesSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(PROPERTIES);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getPropertyWriter(this);
        addSummary(writer, PROPERTIES, true, summariesList);
    }

    /**
     * Builds the summary for any nested classes.
     *
     * @param summariesList the list of summaries to which the summary will be added
     */
    protected void buildNestedClassesSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(NESTED_CLASSES);
        var writerFactory = configuration.getWriterFactory();
        var writer = new NestedClassWriter(this, typeElement); // TODO: surprising omission from WriterFactory
        addSummary(writer, NESTED_CLASSES, true, summariesList);
    }

    /**
     * Builds the summary for any methods.
     *
     * @param summariesList the content to which the documentation will be added
     */
    protected void buildMethodsSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(METHODS);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getMethodWriter(this);
        addSummary(writer, METHODS, true, summariesList);
    }

    /**
     * Builds the summary for any constructors.
     *
     * @param summariesList the content to which the documentation will be added
     */
    protected void buildConstructorsSummary(Content summariesList) {
//        MemberSummaryWriter writer = memberSummaryWriters.get(CONSTRUCTORS);
        var writerFactory = configuration.getWriterFactory();
        var writer = writerFactory.getConstructorWriter(this);
        addSummary(writer, CONSTRUCTORS, false, summariesList);
    }


    /**
     * Adds the summary for the documentation.
     *
     * @param writer               the writer for this member summary
     * @param kind                 the kind of members to document
     * @param showInheritedSummary true if a summary of any inherited elements should be documented
     * @param summariesList        the list of summaries to which the summary will be added
     */
    private void addSummary(AbstractMemberWriter writer,
                            VisibleMemberTable.Kind kind,
                            boolean showInheritedSummary,
                            Content summariesList)
    {
        // TODO: could infer the writer from the kind
        // TODO: why LinkedList?
        List<Content> summaryTreeList = new LinkedList<>();
        buildSummary(writer, kind, summaryTreeList);
        if (showInheritedSummary)
            buildInheritedSummary(writer, kind, summaryTreeList);
        if (!summaryTreeList.isEmpty()) {
            Content member = writer.getMemberSummaryHeader(typeElement, summariesList);
            summaryTreeList.forEach(member::add);
            writer.addSummary(summariesList, member);
        }
    }

    /**
     * Build the member summary for the given members.
     *
     * @param writer the summary writer to write the output.
     * @param kind the kind of  members to summarize.
     * @param summaryTreeList the list of contents to which the documentation will be added
     */
    private void buildSummary(AbstractMemberWriter writer,
                              VisibleMemberTable.Kind kind, List<Content> summaryTreeList) {
        SortedSet<? extends Element> members = asSortedSet(visibleMemberTable.getVisibleMembers(kind));
        if (!members.isEmpty()) {
            for (Element member : members) {
                final Element property = pHelper.getPropertyElement(member);
                if (property != null && member instanceof ExecutableElement ee) {
                    configuration.cmtUtils.updatePropertyMethodComment(ee, property);
                }
                if (utils.isMethod(member)) {
                    var docFinder = utils.docFinder();
                    Optional<List<? extends DocTree>> r = docFinder.search((ExecutableElement) member, (m -> {
                        var firstSentenceTrees = utils.getFirstSentenceTrees(m);
                        Optional<List<? extends DocTree>> optional = firstSentenceTrees.isEmpty() ? Optional.empty() : Optional.of(firstSentenceTrees);
                        return DocFinder.Result.fromOptional(optional);
                    })).toOptional();
                    // The fact that we use `member` for possibly unrelated tags is suspicious
                    writer.addMemberSummary(typeElement, member, r.orElse(List.of()));
                } else {
                    writer.addMemberSummary(typeElement, member, utils.getFirstSentenceTrees(member));
                }
            }
            summaryTreeList.add(writer.getSummaryTable(typeElement));
        }
    }

    /**
     * Build the inherited member summary for the given methods.
     *
     * @param writer the writer for this member summary.
     * @param kind the kind of members to document.
     * @param targets the list of contents to which the documentation will be added
     */
    private void buildInheritedSummary(AbstractMemberWriter writer,
                                       VisibleMemberTable.Kind kind, List<Content> targets) {
        SortedSet<? extends Element> inheritedMembersFromMap = asSortedSet(visibleMemberTable.getAllVisibleMembers(kind));

        for (TypeElement inheritedClass : visibleMemberTable.getVisibleTypeElements()) {
            if (!(utils.isPublic(inheritedClass) || utils.isLinkable(inheritedClass))) {
                continue;
            }
            if (Objects.equals(inheritedClass, typeElement)) {
                continue;
            }
            if (utils.hasHiddenTag(inheritedClass)) {
                continue;
            }

            List<? extends Element> members = inheritedMembersFromMap.stream()
                    .filter(e -> Objects.equals(utils.getEnclosingTypeElement(e), inheritedClass))
                    .toList();

            if (!members.isEmpty()) {
                SortedSet<Element> inheritedMembers = new TreeSet<>(summariesComparator);
                inheritedMembers.addAll(members);
                Content inheritedHeader = writer.getInheritedSummaryHeader(inheritedClass);
                Content links = writer.getInheritedSummaryLinks();
                addSummaryFootNote(inheritedClass, inheritedMembers, links, writer);
                inheritedHeader.add(links);
                targets.add(inheritedHeader);
            }
        }
    }

    private void addSummaryFootNote(TypeElement inheritedClass, Iterable<Element> inheritedMembers,
                                    Content links, AbstractMemberWriter writer) {
        boolean isFirst = true;
        for (var iterator = inheritedMembers.iterator(); iterator.hasNext(); ) {
            var member = iterator.next();
            TypeElement t = utils.isUndocumentedEnclosure(inheritedClass)
                    ? typeElement : inheritedClass;
            writer.addInheritedMemberSummary(t, member, isFirst, !iterator.hasNext(), links);
            isFirst = false;
        }
    }

    private SortedSet<? extends Element> asSortedSet(Collection<? extends Element> members) {
        SortedSet<Element> out = new TreeSet<>(summariesComparator);
        out.addAll(members);
        return out;
    }

    /**
     * Build the member details contents of the page.
     *
     * @param classContent the content to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMemberDetails(Content classContent) throws DocletException {
        Content detailsList = getDetailsList();

        buildEnumConstantsDetails(detailsList);
        buildPropertyDetails(detailsList);
        buildFieldDetails(detailsList);
        buildConstructorDetails(detailsList);
        buildAnnotationTypeMemberDetails(detailsList);
        buildMethodDetails(detailsList);

        classContent.add(getMemberDetails(detailsList));
    }

    /**
     * Build the enum constants documentation.
     *
     * @param detailsList the content to which the documentation will be added
     */
    protected void buildEnumConstantsDetails(Content detailsList) {
        var writerFactory = configuration.getWriterFactory();
        var enumConstantWriter = writerFactory.getEnumConstantWriter(this);
        enumConstantWriter.build(detailsList);
    }

    /**
     * Build the field documentation.
     *
     * @param detailsList the content to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildFieldDetails(Content detailsList) throws DocletException {
        var writerFactory = configuration.getWriterFactory();
        var fieldWriter = writerFactory.getFieldWriter(this);
        fieldWriter.build(detailsList);
    }

    /**
     * Build the property documentation.
     *
     * @param detailsList the content to which the documentation will be added
     */
    public void buildPropertyDetails( Content detailsList) {
        var writerFactory = configuration.getWriterFactory();
        var propertyWriter = writerFactory.getPropertyWriter(this);
        propertyWriter.build(detailsList);
    }

    /**
     * Build the constructor documentation.
     *
     * @param detailsList the content to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildConstructorDetails(Content detailsList) throws DocletException {
        var writerFactory = configuration.getWriterFactory();
        var constructorWriter = writerFactory.getConstructorWriter(this);
        constructorWriter.build(detailsList);
    }

    /**
     * Build the method documentation.
     *
     * @param detailsList the content to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMethodDetails(Content detailsList) throws DocletException {
        var writerFactory = configuration.getWriterFactory();
        var methodWriter = writerFactory.getMethodWriter(this);
        methodWriter.build(detailsList);
    }

    /**
     * Build the annotation type optional member documentation.
     *
     * @param target the content to which the documentation will be added
     * @throws DocletException if there is a problem building the documentation
     */
    protected void buildAnnotationTypeMemberDetails(Content target)
            throws DocletException {
        var writerFactory = configuration.getWriterFactory();
        var annotationTypeMemberWriter = writerFactory.getAnnotationTypeMemberWriter(this);
        annotationTypeMemberWriter.build(target);
    }

    /**
     * The documentation for values() and valueOf() in Enums are set by the
     * doclet only iff the user or overridden methods are missing.
     * @param elem the enum element
     */
    private void setEnumDocumentation(TypeElement elem) {
        CommentUtils cmtUtils = configuration.cmtUtils;
        for (ExecutableElement ee : utils.getMethods(elem)) {
            if (!utils.getFullBody(ee).isEmpty()) // ignore if already set
                continue;
            Name name = ee.getSimpleName();
            if (name.contentEquals("values") && ee.getParameters().isEmpty()) {
                utils.removeCommentHelper(ee); // purge previous entry
                cmtUtils.setEnumValuesTree(ee);
            } else if (name.contentEquals("valueOf") && ee.getParameters().size() == 1) {
                // TODO: check parameter type
                utils.removeCommentHelper(ee); // purge previous entry
                cmtUtils.setEnumValueOfTree(ee);
            }
        }
    }

    /**
     * Sets the documentation as needed for the mandated parts of a record type.
     * This includes the canonical constructor, methods like {@code equals},
     * {@code hashCode}, {@code toString}, the accessor methods, and the underlying
     * field.
     * @param elem the record element
     */

    private void setRecordDocumentation(TypeElement elem) {
        CommentUtils cmtUtils = configuration.cmtUtils;
        Set<Name> componentNames = elem.getRecordComponents().stream()
                .map(Element::getSimpleName)
                .collect(Collectors.toSet());

        for (ExecutableElement ee : utils.getConstructors(elem)) {
            if (utils.isCanonicalRecordConstructor(ee)) {
                if (utils.getFullBody(ee).isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordConstructorTree(ee);
                }
                // only one canonical constructor; no need to keep looking
                break;
            }
        }

        var fields = utils.isSerializable(elem)
                ? utils.getFieldsUnfiltered(elem)
                : utils.getFields(elem);
        for (VariableElement ve : fields) {
            // The fields for the record component cannot be declared by the
            // user and so cannot have any pre-existing comment.
            Name name = ve.getSimpleName();
            if (componentNames.contains(name)) {
                utils.removeCommentHelper(ve); // purge previous entry
                cmtUtils.setRecordFieldTree(ve);
            }
        }

        TypeMirror objectType = utils.getObjectType();

        for (ExecutableElement ee : utils.getMethods(elem)) {
            if (!utils.getFullBody(ee).isEmpty()) {
                continue;
            }

            Name name = ee.getSimpleName();
            List<? extends VariableElement> params = ee.getParameters();
            if (name.contentEquals("equals")) {
                if (params.size() == 1 && utils.typeUtils.isSameType(params.get(0).asType(), objectType)) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordEqualsTree(ee);
                }
            } else if (name.contentEquals("hashCode")) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordHashCodeTree(ee);
                }
            } else if (name.contentEquals("toString")) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordToStringTree(ee);
                }
            } else if (componentNames.contains(name)) {
                if (params.isEmpty()) {
                    utils.removeCommentHelper(ee); // purge previous entry
                    cmtUtils.setRecordAccessorTree(ee);
                }
            }
        }

    }

    // TODO: inline this
    public Content getOutputInstance() {
        return new ContentBuilder();
    }

    protected Content getHeader(String header) {
        HtmlTree body = getBody(getWindowTitle(utils.getSimpleName(typeElement)));
        var div = HtmlTree.DIV(HtmlStyle.header);
        if (configuration.showModules) {
            ModuleElement mdle = configuration.docEnv.getElementUtils().getModuleOf(typeElement);
            var classModuleLabel = HtmlTree.SPAN(HtmlStyle.moduleLabelInType, contents.moduleLabel);
            var moduleNameDiv = HtmlTree.DIV(HtmlStyle.subTitle, classModuleLabel);
            moduleNameDiv.add(Entity.NO_BREAK_SPACE);
            moduleNameDiv.add(getModuleLink(mdle,
                    Text.of(mdle.getQualifiedName())));
            div.add(moduleNameDiv);
        }
        PackageElement pkg = utils.containingPackage(typeElement);
        if (!pkg.isUnnamed()) {
            var classPackageLabel = HtmlTree.SPAN(HtmlStyle.packageLabelInType, contents.packageLabel);
            var pkgNameDiv = HtmlTree.DIV(HtmlStyle.subTitle, classPackageLabel);
            pkgNameDiv.add(Entity.NO_BREAK_SPACE);
            Content pkgNameContent = getPackageLink(pkg, getLocalizedPackageName(pkg));
            pkgNameDiv.add(pkgNameContent);
            div.add(pkgNameDiv);
        }
        HtmlLinkInfo linkInfo = new HtmlLinkInfo(configuration,
                HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS_AND_BOUNDS, typeElement)
                .linkToSelf(false);  // Let's not link to ourselves in the header
        var heading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, Text.of(header));
        heading.add(getTypeParameterLinks(linkInfo));
        div.add(heading);
        bodyContents.setHeader(getHeader(PageMode.CLASS, typeElement))
                .addMainContent(MarkerComments.START_OF_CLASS_DATA)
                .addMainContent(div);
        return body;
    }

    protected Content getClassContentHeader() {
        return getContentHeader();
    }

    @Override
    protected Navigation getNavBar(PageMode pageMode, Element element) {
        Content linkContent = getModuleLink(utils.elementUtils.getModuleOf(element),
                contents.moduleLabel);
        return super.getNavBar(pageMode, element)
                .setNavLinkModule(linkContent)
                .setSubNavLinks(() -> {
                    List<Content> list = new ArrayList<>();
                    VisibleMemberTable vmt = configuration.getVisibleMemberTable(typeElement);
                    Set<VisibleMemberTable.Kind> summarySet =
                            VisibleMemberTable.Kind.forSummariesOf(element.getKind());
                    for (VisibleMemberTable.Kind kind : summarySet) {
                        list.add(links.createLink(HtmlIds.forMemberSummary(kind),
                                contents.getNavLinkLabelContent(kind), vmt.hasVisibleMembers(kind)));
                    }
                    return list;
                });
    }

    protected void addFooter() {
        bodyContents.addMainContent(MarkerComments.END_OF_CLASS_DATA);
        bodyContents.setFooter(getFooter());
    }

    protected void printDocument(Content content) throws DocFileIOException {
        String description = getDescription("declaration", typeElement);
        PackageElement pkg = utils.containingPackage(typeElement);
        List<DocPath> localStylesheets = getLocalStylesheets(pkg);
        content.add(bodyContents);
        printHtmlDocument(configuration.metakeywords.getMetaKeywords(typeElement),
                description, localStylesheets, content);
    }

    protected Content getClassInfo(Content classInfo) {
        return getMember(HtmlIds.CLASS_DESCRIPTION, HtmlStyle.classDescription, classInfo);
    }

    @Override
    public TypeElement getCurrentPageElement() {
        return typeElement;
    }

    protected void addClassSignature(Content classInfo) {
        classInfo.add(new HtmlTree(TagName.HR));
        classInfo.add(new Signatures.TypeSignature(typeElement, this)
                .toContent());
    }

    protected void addClassDescription(Content classInfo) {
        addPreviewInfo(classInfo);
        if (!options.noComment()) {
            // generate documentation for the class.
            if (!utils.getFullBody(typeElement).isEmpty()) {
                addInlineComment(typeElement, classInfo);
            }
        }
    }

    private void addPreviewInfo(Content content) {
        addPreviewInfo(typeElement, content);
    }

    protected void addClassTagInfo(Content classInfo) {
        if (!options.noComment()) {
            // Print Information about all the tags here
            addTagsInfo(typeElement, classInfo);
        }
    }

    /**
     * Get the class inheritance tree for the given class.
     *
     * @param type the class to get the inheritance tree for
     * @return the class inheritance tree
     */
    private Content getClassInheritanceTreeContent(TypeMirror type) {
        TypeMirror sup;
        HtmlTree classTree = null;
        do {
            sup = utils.getFirstVisibleSuperClass(type);
            var entry = HtmlTree.DIV(HtmlStyle.inheritance, getClassHelperContent(type));
            if (classTree != null)
                entry.add(classTree);
            classTree = entry;
            type = sup;
        } while (sup != null);
        classTree.put(HtmlAttr.TITLE, contents.getContent("doclet.Inheritance_Tree").toString());
        return classTree;
    }

    /**
     * Get the class helper for the given class.
     *
     * @param type the class to get the helper for
     * @return the class helper
     */
    private Content getClassHelperContent(TypeMirror type) {
        Content result = new ContentBuilder();
        if (utils.typeUtils.isSameType(type, typeElement.asType())) {
            Content typeParameters = getTypeParameterLinks(
                    new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS,
                    typeElement));
            if (configuration.shouldExcludeQualifier(utils.containingPackage(typeElement).toString())) {
                result.add(utils.asTypeElement(type).getSimpleName());
                result.add(typeParameters);
            } else {
                result.add(utils.asTypeElement(type).getQualifiedName());
                result.add(typeParameters);
            }
        } else {
            Content link = getLink(new HtmlLinkInfo(configuration,
                    HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS, type)
                    .label(configuration.getClassName(utils.asTypeElement(type))));
            result.add(link);
        }
        return result;
    }

    protected void addClassTree(Content target) {
        if (!utils.isClass(typeElement)) {
            return;
        }
        target.add(getClassInheritanceTreeContent(typeElement.asType()));
    }

    protected void addParamInfo(Content target) {
        if (utils.hasBlockTag(typeElement, DocTree.Kind.PARAM)) {
            var t = configuration.tagletManager.getTaglet(DocTree.Kind.PARAM);
            Content paramInfo = t.getAllBlockTagOutput(typeElement, getTagletWriterInstance(false));
            if (!paramInfo.isEmpty()) {
                target.add(HtmlTree.DL(HtmlStyle.notes, paramInfo));
            }
        }
    }

    protected void addSubClassInfo(Content target) {
        if (utils.isClass(typeElement)) {
            for (String s : suppressSubtypesSet) {
                if (typeElement.getQualifiedName().contentEquals(s)) {
                    return;    // Don't generate the list, too huge
                }
            }
            Set<TypeElement> subclasses = classTree.hierarchy(typeElement).subtypes(typeElement);
            if (!subclasses.isEmpty()) {
                var dl = HtmlTree.DL(HtmlStyle.notes);
                dl.add(HtmlTree.DT(contents.subclassesLabel));
                dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.PLAIN, subclasses)));
                target.add(dl);
            }
        }
    }

    protected void addSubInterfacesInfo(Content target) {
        if (utils.isPlainInterface(typeElement)) {
            Set<TypeElement> subInterfaces = classTree.hierarchy(typeElement).allSubtypes(typeElement);
            if (!subInterfaces.isEmpty()) {
                var dl = HtmlTree.DL(HtmlStyle.notes);
                dl.add(HtmlTree.DT(contents.subinterfacesLabel));
                dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS, subInterfaces)));
                target.add(dl);
            }
        }
    }

    protected void addInterfaceUsageInfo(Content target) {
        if (!utils.isPlainInterface(typeElement)) {
            return;
        }
        for (String s : suppressImplementingSet) {
            if (typeElement.getQualifiedName().contentEquals(s)) {
                return;    // Don't generate the list, too huge
            }
        }
        Set<TypeElement> implcl = classTree.implementingClasses(typeElement);
        if (!implcl.isEmpty()) {
            var dl = HtmlTree.DL(HtmlStyle.notes);
            dl.add(HtmlTree.DT(contents.implementingClassesLabel));
            dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.PLAIN, implcl)));
            target.add(dl);
        }
    }

    protected void addImplementedInterfacesInfo(Content target) {
        SortedSet<TypeMirror> interfaces = new TreeSet<>(comparators.makeTypeMirrorClassUseComparator());
        interfaces.addAll(utils.getAllInterfaces(typeElement));
        if (utils.isClass(typeElement) && !interfaces.isEmpty()) {
            var dl = HtmlTree.DL(HtmlStyle.notes);
            dl.add(HtmlTree.DT(contents.allImplementedInterfacesLabel));
            dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS, interfaces)));
            target.add(dl);
        }
    }

    protected void addSuperInterfacesInfo(Content target) {
        SortedSet<TypeMirror> interfaces =
                new TreeSet<>(comparators.makeTypeMirrorIndexUseComparator());
        interfaces.addAll(utils.getAllInterfaces(typeElement));

        if (utils.isPlainInterface(typeElement) && !interfaces.isEmpty()) {
            var dl = HtmlTree.DL(HtmlStyle.notes);
            dl.add(HtmlTree.DT(contents.allSuperinterfacesLabel));
            dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.SHOW_TYPE_PARAMS, interfaces)));
            target.add(dl);
        }
    }

    protected void addNestedClassInfo(final Content target) {
        Element outerClass = typeElement.getEnclosingElement();
        if (outerClass == null)
            return;
        new SimpleElementVisitor8<Void, Void>() {
            @Override
            public Void visitType(TypeElement e, Void p) {
                var dl = HtmlTree.DL(HtmlStyle.notes);
                dl.add(HtmlTree.DT(utils.isPlainInterface(e)
                        ? contents.enclosingInterfaceLabel
                        : contents.enclosingClassLabel));
                dl.add(HtmlTree.DD(getClassLinks(HtmlLinkInfo.Kind.LINK_TYPE_PARAMS_AND_BOUNDS, List.of(e))));
                target.add(dl);
                return null;
            }
        }.visit(outerClass);
    }

    protected void addFunctionalInterfaceInfo (Content target) {
        if (isFunctionalInterface()) {
            var dl = HtmlTree.DL(HtmlStyle.notes);
            dl.add(HtmlTree.DT(contents.functionalInterface));
            var dd = new HtmlTree(TagName.DD);
            dd.add(contents.functionalInterfaceMessage);
            dl.add(dd);
            target.add(dl);
        }
    }

    public boolean isFunctionalInterface() {
        List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
        for (AnnotationMirror anno : annotationMirrors) {
            if (utils.isFunctionalInterface(anno)) {
                return true;
            }
        }
        return false;
    }


    protected void addClassDeprecationInfo(Content classInfo) {
        List<? extends DeprecatedTree> deprs = utils.getDeprecatedTrees(typeElement);
        if (utils.isDeprecated(typeElement)) {
            var deprLabel = HtmlTree.SPAN(HtmlStyle.deprecatedLabel, getDeprecatedPhrase(typeElement));
            var div = HtmlTree.DIV(HtmlStyle.deprecationBlock, deprLabel);
            if (!deprs.isEmpty()) {
                CommentHelper ch = utils.getCommentHelper(typeElement);
                DocTree dt = deprs.get(0);
                List<? extends DocTree> commentTags = ch.getBody(dt);
                if (!commentTags.isEmpty()) {
                    addInlineDeprecatedComment(typeElement, deprs.get(0), div);
                }
            }
            classInfo.add(div);
        }
    }

    /**
     * Get the links to the given classes.
     *
     * @param context the id of the context where the links will be added
     * @param list the classes
     * @return the links
     */
    private Content getClassLinks(HtmlLinkInfo.Kind context, Collection<?> list) {
        Content content = new ContentBuilder();
        boolean isFirst = true;
        for (Object type : list) {
            if (!isFirst) {
                content.add(Text.of(", "));
            } else {
                isFirst = false;
            }
            // TODO: should we simply split this method up to avoid instanceof ?
            if (type instanceof TypeElement te) {
                Content link = getLink(
                        new HtmlLinkInfo(configuration, context, te));
                content.add(HtmlTree.CODE(link));
            } else {
                Content link = getLink(
                        new HtmlLinkInfo(configuration, context, ((TypeMirror)type)));
                content.add(HtmlTree.CODE(link));
            }
        }
        return content;
    }

    /**
     * Return the TypeElement being documented.
     *
     * @return the TypeElement being documented.
     */
    public TypeElement getTypeElement() {
        return typeElement;
    }

    protected Content getMemberDetails(Content content) {
        var section = HtmlTree.SECTION(HtmlStyle.details, content);
        // The following id is required by the Navigation bar
        if (utils.isAnnotationInterface(typeElement)) {
            section.setId(HtmlIds.ANNOTATION_TYPE_ELEMENT_DETAIL);
        }
        return section;
    }
}
