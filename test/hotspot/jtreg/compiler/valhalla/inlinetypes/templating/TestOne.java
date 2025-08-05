/*
 * @test
 * @summary tbd
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 * @enablePreview
 * @compile ../../../../compiler/lib/ir_framework/TestFramework.java
 * @compile ../../../../compiler/lib/verify/Verify.java
 * @run main/othervm --enable-preview
 *                   -DReportStdout=true
 *                   compiler.valhalla.inlinetypes.templating.TestOne
 */

package compiler.valhalla.inlinetypes.templating;

import compiler.lib.compile_framework.CompileFramework;
import compiler.lib.template_framework.Template;
import compiler.lib.template_framework.TemplateToken;
import compiler.lib.template_framework.library.CodeGenerationDataNameType;
import compiler.lib.template_framework.library.PrimitiveType;
import compiler.lib.template_framework.library.TestFrameworkClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static compiler.lib.template_framework.Template.body;
import static compiler.lib.template_framework.Template.let;

public class TestOne {

    public static String generate(CompileFramework compiler) {
        var irNodesTemplate = Template.make(() -> body(
            """
            static final String BOX_KLASS = "compiler/valhalla/inlinetypes/templating/generated/.*Box\\\\w*";
            static final String ANY_KLASS = "compiler/valhalla/inlinetypes/templating/generated/[\\\\w/]*";

            static final String ALLOC_OF_BOX_KLASS = IRNode.PREFIX + "ALLOC_OF_BOX_KLASS" + InlineTypeIRNode.POSTFIX;
            static {
                 IRNode.allocateOfNodes(ALLOC_OF_BOX_KLASS, BOX_KLASS);
            }

            static final String STORE_OF_ANY_KLASS = IRNode.PREFIX + "STORE_OF_ANY_KLASS" + InlineTypeIRNode.POSTFIX;
            static {
                IRNode.anyStoreOfNodes(STORE_OF_ANY_KLASS, ANY_KLASS);
            }
            """
        ));

        final List<TemplateToken> testTokens = new ArrayList<>();
        testTokens.add(irNodesTemplate.asToken());

        for (PrimitiveType fieldType : CodeGenerationDataNameType.PRIMITIVE_TYPES) {
            testTokens.add(uniFieldTest(FieldConstant.of(0, fieldType)));
        }

        // testTokens.add(multiFieldTemplate(List.of(booleans(), booleans())));

        return TestFrameworkClass.render(
            "compiler.valhalla.inlinetypes.templating.generated",
            "TestBox",
            Set.of("compiler.lib.verify.Verify",
                "compiler.valhalla.inlinetypes.InlineTypeIRNode"),
            compiler.getEscapedClassPathOfCompiledClasses(),
            testTokens
        );
    }

    public static void main(String[] args) throws Exception {
        final CompileFramework compiler = new CompileFramework();

        final String code = generate(compiler);
        System.out.println("Code: " + System.lineSeparator() + code);

        compiler.addJavaSourceCode("TestBox", code);

        compiler.compile(
            "--enable-preview",
            "--release",
            System.getProperty("java.specification.version")
        );

        compiler.invoke(
            "compiler.valhalla.inlinetypes.templating.generated.TestBox",
            "main",
            new Object[] {new String[] {"--enable-preview", "-XX:-DoEscapeAnalysis"}}
        );
    }

    record FieldConstant(Object value, int id, PrimitiveType type) {
        String name() {
            return "v" + id;
        }

        static FieldConstant of(int id, PrimitiveType type) {
            return new FieldConstant(type.con(), id, type);
        }
    }

//    static TemplateToken multiFieldTemplate(List<PrimitiveType> fieldTypes) {
//        final AtomicInteger fieldId = new AtomicInteger();
//        final List<FieldConstant> fields = fieldTypes.stream()
//            .map(fieldType -> FieldConstant.of(fieldId.getAndIncrement(), fieldType))
//            .toList();
//
//        return Template.make(() -> body(
//            definition(),
//            """
//            {
//            """,
//            fields(fields),
//            hashMethod(fields),
//            """
//            }
//            """
//            // testMethod(id, constant)
//        )).asToken();
//    }

//    static TemplateToken definition(int typeId) {
//        return Template.make("ID", (Integer id) -> body(
//            """
//            value class Box#ID
//            """
//        )).asToken(typeId);
//    }

    static TemplateToken fields(List<FieldConstant> constants) {
        return Template.make(() -> body(
            constants.stream()
                .map(TestOne::field)
                .toList()
        )).asToken();
    }

    static TemplateToken field(FieldConstant field) {
        return Template.make("FIELD", (FieldConstant f) -> body(
            let("FIELD_TYPE", f.type),
            let("FIELD_VALUE", f.value),
            let("FIELD_NAME", f.name()),
            """
            final #FIELD_TYPE #FIELD_NAME = #FIELD_VALUE;
            """
        )).asToken(field);
    }

    static TemplateToken uniFieldTest(FieldConstant field) {
        return Template.make("FIELD", (FieldConstant f) -> body(
            let("BOXED", f.type.boxedTypeName()),
            let("FIELD_TYPE", f.type),
            let("FIELD_VALUE", f.value),
            let("FIELD_NAME", f.name()),
            """
            value class $Box {
            """,
            field(field),
            """
            }
            """,
            """
            @Test
            @IR(failOn = {ALLOC_OF_BOX_KLASS, STORE_OF_ANY_KLASS, IRNode.UNSTABLE_IF_TRAP, IRNode.PREDICATE_TRAP})
            public #FIELD_TYPE $test() {
                var box = new $Box();
                return box.#FIELD_NAME;
            }

            @Check(test = "$test")
            public void $checkTest(#FIELD_TYPE result) {
                Verify.checkEQ(#FIELD_VALUE, (#BOXED) result);
            }
            """
        )).asToken(field);
    }

    static TemplateToken hashMethod(List<FieldConstant> fields) {
        return Template.make(() -> body(
            """
            int hash() {
                return
            """,
            fields.stream().map(TestOne::hashField).toList(),
            """
                0;
            }
            """
        )).asToken();
    }

    static TemplateToken hashField(FieldConstant field) {
        return Template.make("FIELD", (FieldConstant f) -> body(
            let("BOXED", f.type.boxedTypeName()),
            let("FIELD_NAME", f.name()),
            """
            #BOXED.hashCode(#FIELD_NAME) +
            """
        )).asToken(field);
    }
}
