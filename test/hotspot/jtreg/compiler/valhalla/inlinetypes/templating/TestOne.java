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
import compiler.lib.template_framework.library.Hooks;
import compiler.lib.template_framework.library.PrimitiveType;
import compiler.lib.template_framework.library.TestFrameworkClass;

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

        var testTemplate = Template.make("TYPE", (PrimitiveType type) -> body(
            let("CONSTANT", type.con()),
            let("BOXED", type.boxedTypeName()),
            """
                value class Box1 {
                    final #TYPE v;

                    Box1(#TYPE v) {
                        this.v = v;
                    }
                }

                @Test
                @IR(failOn = {ALLOC_OF_BOX_KLASS, STORE_OF_ANY_KLASS, IRNode.UNSTABLE_IF_TRAP, IRNode.PREDICATE_TRAP})
                public #TYPE test1() {
                    final Box1 box = new Box1(#CONSTANT);
                    return box.v;
                }

                @Check(test = "test1")
                public void checkTest1(#TYPE result) {
                    Verify.checkEQ(#CONSTANT, (#BOXED) result);
                }
                """,
                Hooks.CLASS_HOOK.insert(irNodesTemplate.asToken())
        ));

        final TemplateToken testToken = testTemplate.asToken(CodeGenerationDataNameType.booleans());

        return TestFrameworkClass.render(
            "compiler.valhalla.inlinetypes.templating.generated",
            "TestBox",
            Set.of("compiler.lib.verify.Verify",
                "compiler.valhalla.inlinetypes.InlineTypeIRNode",
                "jdk.test.lib.Asserts"),
            compiler.getEscapedClassPathOfCompiledClasses(),
            List.of(testToken)
        );
    }

    public static void main(String[] args) throws Exception {
        final CompileFramework compiler = new CompileFramework();

        compiler.addJavaSourceCode("TestBox", generate(compiler));

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
}
