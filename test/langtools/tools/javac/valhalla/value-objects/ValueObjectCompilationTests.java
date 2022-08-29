/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * ValueObjectCompilationTests
 *
 * @test
 * @bug 8287136 8292630 8279368 8287136
 * @summary Negative compilation tests, and positive compilation (smoke) tests for Value Objects
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run testng ValueObjectCompilationTests
 */

import java.lang.constant.ClassDesc;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;

import com.sun.tools.javac.util.Assert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

@Test
public class ValueObjectCompilationTests extends CompilationTestCase {

    ToolBox tb = new ToolBox();

    public ValueObjectCompilationTests() {
        setDefaultFilename("ValueObjectsTest.java");
    }

    public void testAbstractValueClassConstraints() {
        assertFail("compiler.err.super.field.not.allowed",
                """
                abstract value class V {
                    int f;  // Error, abstract value class may not declare an instance field.
                }
                """);
        assertFail("compiler.err.super.class.cannot.be.inner",
                """
                class Outer {
                    abstract value class V {
                        // Error, an abstract value class cant be an inner class
                    }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                abstract value class V {
                    synchronized void foo() {
                     // Error, abstract value class may not declare a synchronized instance method.
                    }
                }    
                """);
        assertFail("compiler.err.super.class.declares.init.block",
                """
                abstract value class V {
                    { int f = 42; } // Error, abstract value class may not declare an instance initializer.
                }
                """);
        assertFail("compiler.err.super.constructor.cannot.take.arguments",
                """
                abstract value class V {
                    V(int x) {}  // Error, abstract value class may not declare a non-trivial constructor.
                }
                """);
    }

    public void testAnnotationsConstraints() {
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                identity @interface IA {}
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                value @interface IA {}
                """);
    }

    public void testCheckFeatureSourceLevel() {
        setCompileOptions(new String[]{"--release", "13"});
        assertFail("compiler.err.feature.not.supported.in.source.plural",
                """
                value class V {
                    public int v = 42;
                }
                """);
        setCompileOptions(new String[]{});
    }

    /*
    public void testCheckObjectMethodsUsage() {
        assertFail("compiler.err.value.class.may.not.override",
                """
                value class V {
                    public void finalize() {}
                }
                """);
        assertFail("compiler.err.value.class.may.not.override",
                """
                value class V {
                    public Object clone() { return null; }
                }
                """);
    }
    */

    public void testSuperClassConstraints() {
        assertFail("compiler.err.super.field.not.allowed",
                """
                abstract class I { // has identity since it declares an instance field.
                    int f;
                }
                        
                value class V extends I {}
                """);

        assertFail("compiler.err.super.class.cannot.be.inner",
                """
                class Outer {
                    abstract class I { /* has identity since is an inner class */ }
                    static value class V extends I
                }
                """);

        assertFail("compiler.err.super.method.cannot.be.synchronized",
                """
                abstract class I { // has identity since it declared a synchronized instance method.
                    synchronized void foo() {}
                }
                            
                value class V extends I {}
                """);

        assertFail("compiler.err.super.class.declares.init.block",
                """
                abstract class I { // has identity since it declares an instance initializer
                    { int f = 42; }
                }
                                
                value class V extends I {}
                """);

        assertFail("compiler.err.super.constructor.cannot.take.arguments",
                """
                abstract class I { // has identity since it declares a non-trivial constructor
                    I(int x) {}
                }
                            
                value class V extends I {}
                """);
    }
}
