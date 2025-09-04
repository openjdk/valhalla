/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.*;
import java.lang.constant.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/*
 * @test
 * @summary Ensures that preloading self does not cause a deadlock or crash.
 * @enablePreview
 * @run main LoadableDescriptorsTest LTest;
 */

 /*
  * @test
  * @summary Tries to put a primitive in a LoadableDescriptor, should pass silently.
  * @enablePreview
  * @run main LoadableDescriptorsTest I
  */

 /*
  * @test
  * @summary Tries to put an array in a LoadableDescriptor, should pass silently.
  * @enablePreview
  * @run main LoadableDescriptorsTest [[LTest;
  */


public class LoadableDescriptorsTest {

  public static void main(String[] args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("test bug: expected string descriptor");
    }
    String descriptor = args[0];
    try {
      Class<?> clazz = Class.forName("Test", false, new CL(descriptor));
      Object instance = clazz.getDeclaredConstructor().newInstance();
      Field field = clazz.getDeclaredField("theField");
      System.out.println(field.get(instance));
    } catch (ClassNotFoundException |
             NoSuchMethodException |
             NoSuchFieldException |
             IllegalAccessException |
             InstantiationException |
             InvocationTargetException e) {
      throw new RuntimeException("test bug: setup of test saw error", e);
    }
  }

  public static final class CL extends ClassLoader {
    private final String descriptor;

    public CL(String descriptor) {
      this.descriptor = descriptor;
    }

    public Class<?> findClass(String name)
        throws ClassNotFoundException {
      if (!name.equals("Test")) {
        throw new ClassNotFoundException("not one of our special classes");
      }
      return makeClass(name);
    }

    public Class<?> makeClass(String className) {
      ClassDesc thisClass = ClassDesc.of(className);
      ClassDesc loadableClass = ClassDesc.ofDescriptor(descriptor);
      // A class that has itself as a loadable descriptor.
      byte[] bytes = ClassFile.of().build(thisClass,
        clb -> clb.withVersion(ClassFile.JAVA_25_VERSION, ClassFile.PREVIEW_MINOR_VERSION)
                  .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_IDENTITY)
                  // From java.base/java/lang/classfile documentation.
                  .withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, ClassFile.ACC_PUBLIC,
                    cob -> cob.aload(0)
                              .invokespecial(ConstantDescs.CD_Object,
                                             ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
                              .return_())
                  .withField("theField", loadableClass, ClassFile.ACC_PUBLIC)
                  .with(LoadableDescriptorsAttribute.of(clb.constantPool().utf8Entry(loadableClass)))
      );
      return defineClass(className, bytes, 0, bytes.length);
    }
  }

}
