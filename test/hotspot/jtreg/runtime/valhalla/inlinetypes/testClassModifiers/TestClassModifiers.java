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
 *
 */

/*
 * @test
 * @summary test that the JVM correctly accepts or rejects classes based on their
 *          ACC_VALUE/ACC_IDENTITY modifiers
 * @library /test/lib
 * @compile NeutralInterface.java ValueInterface.java IdentityInterface.java
 * @compile NeutralAbstract.java ValueAbstract.java IdentityAbstract.java AbstractWithField.java AbstractWithSynchMethod.java
 * @compile ClassesWithInvalidModifiers.jcod
 * @run main/othervm -verify TestClassModifiers
 */

import jdk.test.lib.Asserts;

 public class TestClassModifiers {
  static value class VC0 {}
  static value class VC1 implements NeutralInterface { }
  static value class VC2 implements ValueInterface { }
  static value class VC3 extends NeutralAbstract { }
  static value class VC4 extends ValueAbstract { }
  static value class VC5 extends NeutralAbstract implements NeutralInterface { }
  static value class VC6 extends NeutralAbstract implements ValueInterface { }
  static value class VC7 extends ValueAbstract implements NeutralInterface { }
  static value class VC8 extends ValueAbstract implements ValueInterface { }

  static identity class IC0 { }
  static identity class IC1 implements NeutralInterface { }
  static identity class IC2 implements IdentityInterface { }
  static identity class IC3 extends NeutralAbstract { }
  static identity class IC4 extends IdentityAbstract { }
  static identity class IC5 extends AbstractWithField { }
  static identity class IC6 extends AbstractWithSynchMethod { }
  static identity class IC7 extends NeutralAbstract implements NeutralInterface { }
  static identity class IC8 extends NeutralAbstract implements IdentityInterface { }
  static identity class IC9 extends IdentityAbstract implements NeutralInterface { }
  static identity class IC10 extends IdentityAbstract implements IdentityInterface { }


  static abstract class AC0 extends AbstractWithField implements NeutralInterface { }
  static abstract class AC1 extends AbstractWithField implements IdentityInterface { }
  static abstract class AC2 extends AbstractWithSynchMethod implements NeutralInterface { }
  static abstract class AC3 extends AbstractWithSynchMethod implements IdentityInterface { }

  static String[] validClasses = {"VC0", "VC1", "VC2", "VC3", "VC4", "VC5", "VC6", "VC7", "VC8",
                                  "IC0", "IC1", "IC2", "IC3", "IC4", "IC5", "IC6", "IC7", "IC8", "IC9", "IC10",
                                  "AC0", "AC1", "AC2", "AC3"};

  static String[] invalidClassesWithICCE = {"ValueClassExtendingIdentityClass", "ValueClassExtendingAbstractClassWithField",
                                    "ValueClassExtendingAbstractClassWithSynchMethod", "ValueClassImplementingIdentityInterface",
                                    "IdentityClassExtendingValueClass", "IdentityClassImplementingValueInterface",
                                    "AbstractClassWithFieldExtendingValueClass", "AbstractClassWithFieldImplementingValueInterface"};
  static String[] invalidClassesWithCFE = {"AbstractClassWithFieldWithNoIdentityModifier", "AbstractClassWithSynchMethodWithNoIdentityModifier",
                                           "AbstractClassWithBothModifiers", "ConcreteClassWithNoModifiers"};

  public static void main(String[] args) throws Exception {
    // testing valid cases first
    try {
      for (String name : validClasses) {
        System.out.println("Trying to load "+name);
        Class c = Class.forName("TestClassModifiers$"+name);
      }
    } catch(Throwable t) {
      t.printStackTrace();
      throw t;
    }

    // Testing invalid cases
    for (String name: invalidClassesWithICCE) {
      boolean icce = false;
      boolean otherException = false;
      Throwable exception = null;
      try {
        System.out.println("Trying to load "+name);
        Class c = Class.forName(name);
      } catch (IncompatibleClassChangeError e) {
        icce = true;
      } catch (Throwable t) {
        otherException = true;
        exception = t;
      }
      Asserts.assertFalse(otherException, "Unexpected exception "+exception);
      Asserts.assertTrue(icce, "IncompatibleClassChangeError not thrown");
    }
    for (String name: invalidClassesWithCFE) {
      boolean cfe = false;
      boolean otherException = false;
      Throwable exception = null;
      try {
        System.out.println("Trying to load "+name);
        Class c = Class.forName(name);
      } catch (ClassFormatError e) {
        cfe = true;
      } catch (Throwable t) {
        otherException = true;
        exception = t;
      }
      Asserts.assertFalse(otherException, "Unexpected exception "+exception);
      Asserts.assertTrue(cfe, "ClassFormatError not thrown");
    }
  }
 }
