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
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

// A classloader that will generate a big value class inheritance tree (depth,
// and possibly breadth) of classes on the fly. For example, with a
// maximum depth limit of 3, one can load "Gen3" via this classloader,
// which will generate the following:
//
// public value class Gen2 --> Gen1 --> Gen0 --> java.lang.Object
//
// Optionally, a long field chain can also be generated in one of the Gen classes.
// These fields are static and go all the way to maximum depth. For example, with
// a maximum depth limit of 3 and field at 1, this classloader will generate
// the following:
//
// public value class Gen2 --> Gen1 --> Gen0 -> java.lang.Object
//                              | public static Field2 theField;
//         public value class Field2
//                              | theField
//                            Field1
//                              | theField
//                            Field0
//
// Field0 will have a field theField of java.lang.Object. It is possible to change
// both the field class as well as the superclass of Field0 to introduce interesting
// class circularity.
public class BigClassTreeClassLoader extends ClassLoader {

  // TODO: REMOVE THIS
  public static void main(String[] args) throws Exception {
    FieldGeneration fields = new FieldGeneration(1, Optional.empty(), Optional.empty());
    Class<?> clazz = Class.forName("Gen2", false, new BigClassTreeClassLoader(3, fields));
    Object instance = clazz.getDeclaredConstructor().newInstance();
    System.out.println(instance);
  }

  // A field generation strategy that disables field generation.
  public static FieldGeneration NO_FIELD_GEN = new FieldGeneration(-1, Optional.empty(), Optional.empty());


  // A sane depth limit.
  private static final int SANE_LIMIT = 100;

  // We want to perform different things depending on what kind of class we are
  // generating. Therefore, we utilize a strategy pattern.
  private final Strategy[] availableStrategies;

  // A store of all the classes already defined. Existing classes must be reused
  // otherwise an exception will be raised.
  private final Map<String, Class<?>> defined;

  private final int limitInclusive;

  // Create the generator with no fields.
  public BigClassTreeClassLoader(int depthLimitInclusive) {
    this(depthLimitInclusive, NO_FIELD_GEN);
  }

  // Create the generator with fields.
  public BigClassTreeClassLoader(int depthLimitInclusive,
                                 FieldGeneration fields) {
    if (depthLimitInclusive < 0 || depthLimitInclusive > SANE_LIMIT) {
      throw new IllegalArgumentException("depth limit beyond sane bounds");
    }
    // Make it compatible with zero indices.
    this.limitInclusive = depthLimitInclusive - 1;
    this.defined = new HashMap<>();
    if (fields.index > limitInclusive) {
      throw new IllegalArgumentException("field generation index invalid");
    }
    this.availableStrategies = new Strategy[] { new GenStrategy(fields.index), new FieldStrategy(fields) };
  }

  // The index X means GenX will have the field. Set to -1 to disable.
  // The furthest chained field Field0 can have an optional superclass/declared field.
  public static record FieldGeneration (int index,
                                        Optional<String> deepestParentClass,
                                        Optional<String> deepestFieldClass) {}

  // We will bottom-up generate a class tree
  public Class<?> findClass(final String name) throws ClassNotFoundException {
    // We only generate classes starting with our known prefix.
    final Strategy strategy = Arrays.stream(availableStrategies)
                                    .filter(st -> name.startsWith(st.prefix()))
                                    .findFirst()
                                    .orElseThrow(ClassNotFoundException::new);
    String prefix = strategy.prefix();
    try {
      String itersString = name.substring(prefix.length());
      Integer depth = Integer.valueOf(itersString);
      // Some bounds sanity checking.
      if (depth < 0 || depth > limitInclusive) {
        throw new IllegalArgumentException("attempting to generate beyond limits");
      }
      // If we have already generated this class, reuse it.
      if (defined.containsKey(name)) {
        return defined.get(name);
      }
      // The parent is different for the base case.
      // The childmost class is nonabstract
      // Have the strategy decide the parameters.
      Class<?> clazz = makeClass(name,
                                strategy.parent(depth),
                                strategy.flags(limitInclusive, depth),
                                clb -> strategy.process(limitInclusive, depth, clb),
                                cob -> strategy.constructorPre(limitInclusive, depth, cob)
      );
      // Remember it for potential future use.
      defined.put(name, clazz);
      return clazz;
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      throw new ClassNotFoundException("can't generate class since it does not conform to limits", e);
    }
  }

  private interface Strategy {
    String prefix();
    String parent(int depth);
    int flags(int limitInclusive, int depth);
    void process(int limitInclusive, int depth, ClassBuilder builder);
    default void constructorPre(int limitInclusive, int depth, CodeBuilder builder) {}
  }

  // The Gen classes generate classes that have a large inheritance tree.
  // GenX has Gen(X-1) as a superclass. Gen0 inherits from Object.
  private static final class GenStrategy implements Strategy {
    private final int fieldIndex;

    public GenStrategy(int fieldIndex) {
      this.fieldIndex = fieldIndex;
    }

    public String prefix() {
      return "Gen";
    }

    public String parent(int depth) {
      return depth == 0 ? Object.class.getName() : prefix() + (depth - 1);
    }

    public int flags(int limitInclusive, int depth) {
      return depth == limitInclusive ? ClassFile.ACC_FINAL : ClassFile.ACC_ABSTRACT;
    }

    public void process(int limitInclusive, int depth, ClassBuilder builder) {
      // Is this the generation that will have the field chain?
      if (depth == fieldIndex) {
        ClassDesc fieldClass = ClassDesc.of(FieldStrategy.PREFIX + "" + limitInclusive);
        // We use an uninitialized static field to denote the outermost Field class.
        builder.withField("theField", fieldClass, ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC)
               .with(LoadableDescriptorsAttribute.of(builder.constantPool().utf8Entry(fieldClass)));
      }
    }
  }

  // The field strategy allows generating deep fields, including potential circularity.
  // FieldX has Field(X-1) as a field. Field0 is special as it can inherit from something
  // other than Object, and contain a custom field.
  private static final class FieldStrategy implements Strategy {
    public static final String PREFIX = "Field";
    private final FieldGeneration fields;

    public FieldStrategy(FieldGeneration fields) {
      this.fields = fields;
    }

    public String prefix() {
      return PREFIX;
    }

    public String parent(int depth) {
      // Only the deepest class has a custom parent.
      return fields.deepestParentClass().filter(_ -> depth == 0).orElse(Object.class.getName());
    }

    public int flags(int limitInclusive, int depth) {
      // Every field class is final.
      return ClassFile.ACC_FINAL;
    }

    public void process(int limitInclusive, int depth, ClassBuilder builder) {
      ClassDesc fieldClass = computeFieldClass(depth);
      if (depth != 0) {
        builder.with(LoadableDescriptorsAttribute.of(builder.constantPool().utf8Entry(fieldClass)));
      }
      // The field is non-static, final, and therefore needs ACC_STRICT_INIT
      builder.withField("theField", fieldClass, ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_STRICT);
    }

    public void constructorPre(int limitInclusive, int depth, CodeBuilder builder) {
      ClassDesc thisField = ClassDesc.of(prefix() + "" + depth);
      ClassDesc fieldClass = computeFieldClass(depth);
      // We need to make sure to initialize the field as the first thing in the constructor.
      builder.aload(0)
             .aconst_null()
             .putfield(thisField, "theField", fieldClass);
    }

    private ClassDesc computeFieldClass(int depth) {
      if (depth == 0) {
        return ClassDesc.of(fields.deepestFieldClass().orElse(Object.class.getName()));
      } else {
        return ClassDesc.of(prefix() + (depth - 1));
      }
    }
  }

  private Class<?> makeClass(String thisGen,
                             String parentGen,
                             int addFlags,
                             Consumer<ClassBuilder> consumer1,
                             Consumer<CodeBuilder> consumer2) {
    ClassDesc parentDesc = ClassDesc.of(parentGen);
    // A class that has itself as a loadable descriptor.
    byte[] bytes = ClassFile.of().build(ClassDesc.of(thisGen), clb -> {
      // Use Valhalla version.
      clb.withVersion(ClassFile.JAVA_25_VERSION, ClassFile.PREVIEW_MINOR_VERSION)
      // Explicitly do not add ACC_SUPER or ACC_ABSTRACT.
         .withFlags(ClassFile.ACC_PUBLIC | addFlags)
      // Not strictly necessary for java/lang/Object.
         .withSuperclass(parentDesc)
      // Make sure to init the correct superclass.
         .withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, ClassFile.ACC_PUBLIC, cob -> {
           consumer2.accept(cob);
           cob.aload(0)
                     .invokespecial(parentDesc,
                                    ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
                     .return_();
         });
      // Do the additional things defined by the strategy.
      consumer1.accept(clb);
    });
    // TODO: remove
    //try { java.nio.file.Files.write(java.nio.file.Path.of(thisGen + ".class"), bytes); }
    //catch (java.io.IOException e) { e.printStackTrace(); }
    return defineClass(thisGen, bytes, 0, bytes.length);
  }

}
