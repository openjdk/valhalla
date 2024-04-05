/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class FieldLayoutAnalyzer {

  // Mutable wrapper around log output to manage the cursor while parsing
  static class LogOutput {
    List<String> lines;
    int cursor;

    public LogOutput(List<String> lines) {
      this.lines = lines;
      cursor = 0;
    }

    String getCurrentLine() { return lines.get(cursor); }
    String get(int idx) { return lines.get(idx); }
    int size() { return lines.size(); }
    void moveToNextLine() { cursor = cursor + 1; }
    boolean hasMoreLines() { return cursor < lines.size(); }
  }

  static enum BlockType {
    RESERVED,
    INHERITED,
    EMPTY,
    REGULAR,
    PADDING,
    FLAT,
    NULL_MARKER,
    INHERITED_NULL_MARKER;

    static BlockType parseType(String s) {
      switch(s) {
        case "RESERVED"    : return RESERVED;
        case "INHERITED"   : return INHERITED;
        case "EMPTY"       : return EMPTY;
        case "REGULAR"     : return REGULAR;
        case "PADDING"     : return PADDING;
        case "FLAT"        : return FLAT;
        case "NULL_MARKER" : return NULL_MARKER;
        case "INHERITED_NULL_MARKER" : return INHERITED_NULL_MARKER;
        default:
          throw new RuntimeException("Unknown block type: " + s);
      }
    }
  }

  static public record FieldBlock (int offset,
                            BlockType type,
                            int size,
                            int alignment,
                            String name,
                            String signature,
                            String fieldClass,
                            int nullMarkerOffset,
                            boolean hasInternalNullMarker,
                            int referenceFieldOffset) { // for null marker blocks, gives the offset of the field they refer to


    static FieldBlock createSpecialBlock(int offset, BlockType type, int size, int alignment, int referenceFieldOffset) {
      return new FieldBlock(offset, type, size, alignment, null, null, null, -1, false, referenceFieldOffset);
    }

    static FieldBlock createJavaFieldBlock(int offset, BlockType type, int size, int alignment, String name, String signature, String fieldClass, int nullMarkerOffset, boolean hasInternalNullMarker) {
      return new FieldBlock(offset, type, size, alignment, name, signature, fieldClass, nullMarkerOffset, hasInternalNullMarker, -1);
    }

    void print(PrintStream out) {
      out.println("Offset=" + offset+
                " type=" + type +
                " size=" + size +
                " alignment=" + alignment +
                " name=" + name +
                " signature=" + signature +
                " fieldClass=" + fieldClass);
    }

    boolean isFlat() { return type == BlockType.FLAT; } // Warning: always return false for inherited fields, even flat ones
    boolean hasNullMarker() {return nullMarkerOffset != -1; }

    static FieldBlock parseField(String line) {
      String[] fieldLine = line.split("\\s+");
      // for(String  s : fieldLine) {
      //   System.out.print("["+s+"]");  // debugging statement to be removed
      // }
      // System.out.println();
      int offset = Integer.parseInt(fieldLine[1].substring(1, fieldLine[1].length()));
      BlockType type = BlockType.parseType(fieldLine[2]);
      String[] size_align = fieldLine[3].split("/");
      int size = Integer.parseInt(size_align[0]);
      int alignment = -1;
      if (type != BlockType.RESERVED) {
        alignment = Integer.parseInt(size_align[1]);
      } else {
        Asserts.assertTrue(size_align[1].equals("-"));
      }
      FieldBlock block = null;
      switch(type) {
        case BlockType.RESERVED:
        case BlockType.EMPTY:
        case BlockType.PADDING:
        case BlockType.INHERITED_NULL_MARKER: {
            block = FieldBlock.createSpecialBlock(offset, type, size, alignment, 0);
            break;
        }
        case BlockType.REGULAR:
        case BlockType.INHERITED:
        case BlockType.FLAT: {
            String name = fieldLine[4];
            String signature = fieldLine[5];
            String fieldClass = "";
            int nullMarkerOffset = -1;
            boolean hasInternalNullMarker = false;
            if (type == BlockType.FLAT) {
              fieldClass = fieldLine[6];
              if (fieldLine.length >= 11) {
                nullMarkerOffset = Integer.parseInt(fieldLine[10]);
              }
              if (fieldLine.length >= 12 ) {
                Asserts.assertEquals(fieldLine[11], "(internal)");
                hasInternalNullMarker = true;
              }
            }
            block = FieldBlock.createJavaFieldBlock(offset, type, size, alignment, name, signature, fieldClass, nullMarkerOffset, hasInternalNullMarker);
            break;
          }
        case BlockType.NULL_MARKER: {
          int referenceFieldOffset = Integer.parseInt(fieldLine[10]);
          block = FieldBlock.createSpecialBlock(offset, type, size, alignment, referenceFieldOffset);
          break;
        }
      }
      Asserts.assertNotNull(block);
      return block;
    }
  }

  static class ClassLayout {
    String name;
    String superName;
    boolean isValue;
    int size;
    int firstFieldOffset;
    int alignment;
    int exactSize;
    String[] lines;
    ArrayList<FieldBlock> staticFields;
    ArrayList<FieldBlock> nonStaticFields;
    int internalNullMarkerOffset; // -1 if no internal null marker

    private ClassLayout() {
      staticFields = new ArrayList<FieldBlock>();
      nonStaticFields = new ArrayList<FieldBlock>();
    }

    void processField(String line, boolean isStatic) {
      FieldBlock block = FieldBlock.parseField(line);
      if (isStatic) {
        Asserts.assertTrue(block.type != BlockType.INHERITED); // static fields cannotbe inherited
        staticFields.add(block);
      } else {
        nonStaticFields.add(block);
      }
    }

    static ClassLayout parseClassLayout(LogOutput lo) {
      ClassLayout cl = new ClassLayout();
      // Parsing class name
      Asserts.assertTrue(lo.getCurrentLine().startsWith("Layout of class"), lo.getCurrentLine());
      String[] first = lo.getCurrentLine().split("\\s+");
      cl.name = first[3];
      if (first.length == 6) {
        Asserts.assertEquals(first[4], "extends");
        cl.superName = first[5];
      } else {
        cl.superName = null;
      }
      // System.out.println("Class name: " + cl.name);
      lo.moveToNextLine();
      Asserts.assertTrue(lo.getCurrentLine().startsWith("Instance fields:"), lo.getCurrentLine());
      lo.moveToNextLine();
      // Parsing instance fields
      while (lo.getCurrentLine().startsWith(" @")) {
        cl.processField(lo.getCurrentLine(), false);
        lo.moveToNextLine();
      }
      Asserts.assertTrue(lo.getCurrentLine().startsWith("Static fields:"), lo.getCurrentLine());
      lo.moveToNextLine();
      // Parsing static fields
      while (lo.getCurrentLine().startsWith(" @")) {
        cl.processField(lo.getCurrentLine(), true);
        lo.moveToNextLine();
      }
      Asserts.assertTrue(lo.getCurrentLine().startsWith("Instance size ="), lo.getCurrentLine());
      String[] sizeLine = lo.getCurrentLine().split("\\s+");
      cl.size = Integer.parseInt(sizeLine[3]);
      lo.moveToNextLine();
      if (lo.getCurrentLine().startsWith("First field offset =")) {
        // The class is a value class, more lines to parse
        cl.isValue = true;
        String[] firstOffsetLine = lo.getCurrentLine().split("\\s+");
        cl.firstFieldOffset = Integer.parseInt(firstOffsetLine[4]);
        lo.moveToNextLine();
        String[] alignmentLine = lo.getCurrentLine().split("\\s+");
        cl.alignment = Integer.parseInt(alignmentLine[2]);
        lo.moveToNextLine();
        String[] exactSizeLine = lo.getCurrentLine().split("\\s+");
        cl.exactSize = Integer.parseInt(exactSizeLine[3]);
        lo.moveToNextLine();
        if (lo.getCurrentLine().startsWith("Null marker offset")) {
          String[] nullMarkerLine = lo.getCurrentLine().split("\\s+");
          cl.internalNullMarkerOffset = Integer.parseInt(nullMarkerLine[4]);
          lo.moveToNextLine();
        } else {
          cl.internalNullMarkerOffset = -1;
        }
      } else {
        cl.isValue = false;
      }

      Asserts.assertTrue(lo.getCurrentLine().startsWith("---"), lo.getCurrentLine());
      lo.moveToNextLine();
      return cl;
    }

    FieldBlock getFieldAtOffset(int offset, boolean isStatic) {
      ArrayList<FieldBlock> fields = isStatic ? staticFields : nonStaticFields;
      for (FieldBlock block : fields) {
        if (block.offset == offset) return block;
      }
      throw new RuntimeException("No " + (isStatic ? "static" : "nonstatic") + " field found at offset "+ offset);
    }

    FieldBlock getFieldFromName(String fieldName, boolean isStatic) {
      Asserts.assertTrue(fieldName != null);
      ArrayList<FieldBlock> fields = isStatic ? staticFields : nonStaticFields;
      for (FieldBlock block : fields) {
        if (block.name() == null) continue;
        String n = block.name().substring(1, block.name().length() - 1); // in the log, name is surrounded by double quotes
        if (fieldName.equals(n)) return block;
      }
      throw new RuntimeException("No " + (isStatic ? "static" : "nonstatic") + " field found with name "+ fieldName);
    }

  }

  ArrayList<ClassLayout> layouts;
  boolean compressOops;

  static String signatureToName(String sig) {
    Asserts.assertTrue((sig.charAt(0) == 'L'));
    Asserts.assertTrue((sig.charAt(sig.length() - 1) == ';'));
    return sig.substring(1, sig.length() - 1);
  }

  private FieldLayoutAnalyzer(boolean coop) {
    compressOops = coop;
    layouts = new ArrayList<ClassLayout>();
  }

  public static FieldLayoutAnalyzer createFieldLayoutAnalyzer(LogOutput lo, boolean compressOops) {
    FieldLayoutAnalyzer fla = new FieldLayoutAnalyzer(compressOops);
    fla.generate(lo);
    return fla;
  }

  ClassLayout getClassLayout(String name) {
    for(ClassLayout layout : layouts) {
      if (layout.name.equals(name)) return layout;
    }
    return null;
  }

  ClassLayout getClassLayoutFromName(String name) {
    for(ClassLayout layout : layouts) {
      String sub = layout.name.substring(0, layout.name.indexOf('@'));
      if (name.equals(sub)) return layout;
    }
    return null;
  }

  void checkNoOverlapOnFields(ArrayList<FieldBlock> fields) {
    for (int i = 0; i < fields.size() - 1; i++) {
      FieldBlock f0 = fields.get(i);
      FieldBlock f1 = fields.get(i + 1);
      if (f0.offset + f0.size < f1.offset) {
        throw new RuntimeException("Hole issue found at offset " + f1.offset);
      } else if (f0.offset + f0.size > f1.offset) {
        throw new RuntimeException("Overlap issue found at offset " + f1.offset);
      }
    }
  }

  void checkNoOverlap() {
    for (ClassLayout layout : layouts) {
      try {
        checkNoOverlapOnFields(layout.staticFields);
        checkNoOverlapOnFields(layout.nonStaticFields);
      } catch(Throwable t) {
        throw new RuntimeException("Unexpection exception when checking for overlaps/holes in class " + layout.name);
      }
    }
  }

  void checkSizeAndAlignmentForField(FieldBlock block) {
    if (block.type == BlockType.RESERVED) {
      Asserts.assertTrue(block.alignment == -1);
      return;
    }
    if (block.type == BlockType.EMPTY || block.type == BlockType.PADDING
        || block.type == BlockType.NULL_MARKER || block.type == BlockType.INHERITED_NULL_MARKER) {
      Asserts.assertTrue(block.alignment == 1, "alignment = " + block.alignment);
      return;
    }

    switch(block.signature()) {
      case "Z" :
      case "B" : Asserts.assertTrue(block.size() == 1);
                 Asserts.assertTrue(block.alignment() == 1);
        break;
      case "S" :
      case "C" : Asserts.assertTrue(block.size() == 2);
                 Asserts.assertTrue(block.alignment() == 2);
        break;
      case "F" :
      case "I" : Asserts.assertTrue(block.size() == 4);
                 Asserts.assertTrue(block.alignment() == 4);
        break;
      case "J" :
      case "D" : Asserts.assertTrue(block.size() == 8);
                 Asserts.assertTrue(block.alignment() == 8);
        break;
      default: {
        if (block.signature().startsWith("[")) {
          Asserts.assertEquals(compressOops ? 4 : 8, block.size());
        } else if (block.signature().startsWith("L")) {
          if (block.type == BlockType.INHERITED || block.type == BlockType.INHERITED_NULL_MARKER) {
            // Skip for now, will be verified when checking the class declaring the field
          } else if (block.type == BlockType.REGULAR) {
            Asserts.assertEquals(compressOops ? 4 : 8, block.size());
          } else {
            Asserts.assertEquals(BlockType.FLAT, block.type);
            ClassLayout fcl = getClassLayout(block.fieldClass);
            Asserts.assertNotNull(fcl);
            Asserts.assertEquals(block.size(), fcl.exactSize);
            Asserts.assertEquals(block.alignment(), fcl.alignment);
          }
        } else {
          throw new RuntimeException("Unknown signature type: " + block.signature);
        }
      }
      Asserts.assertTrue(block.offset % block.alignment == 0);
    }
  }

  void checkSizeAndAlignment() {
    for (ClassLayout layout : layouts) {
      for (FieldBlock block : layout.staticFields) {
        checkSizeAndAlignmentForField(block);
      }
      for (FieldBlock block : layout.nonStaticFields) {
        checkSizeAndAlignmentForField(block);
      }
    }
  }

  // Verify that fields marked as INHERITED are declared in a super class
  void checkInheritedFields() {
    for (ClassLayout layout : layouts) {
      // Preparing the list of ClassLayout of super classes
      ArrayList<ClassLayout> supers = new ArrayList<ClassLayout>();
      String className = layout.superName;
      while (className != null) {
        ClassLayout cl = getClassLayout(className);
        supers.add(cl);
        className = cl.superName;
      }
      for (FieldBlock field : layout.nonStaticFields) {
        if (field.type == BlockType.INHERITED) {
          int i = 0;
          boolean found = false;
          FieldBlock b = null;
          while(i < supers.size() && !found) {
            b = supers.get(i).getFieldAtOffset(field.offset, false);
            if (b.type != BlockType.INHERITED) found = true;
            i++;
          }
          String location = new String(" at " + layout.name + " offset " + field.offset());
          Asserts.assertTrue(found, "No declaration found for an inherited field " + location);
          Asserts.assertNotEquals(field.type, BlockType.EMPTY, location);
          Asserts.assertEquals(field.size, b.size, location);
          Asserts.assertEquals(field.alignment, b.alignment, location );
          Asserts.assertEquals(field.name(), b.name(), location);
          Asserts.assertEquals(field.signature(), b.signature(), location);
        }
      }
    }
  }

  static class Node {
    ClassLayout classLayout;
    Node superClass;
    ArrayList<Node> subClasses = new ArrayList<Node>();
  }

  // Verify that all fields declared in a class are present in all subclass
  void checkSubClasses() {
    // Generating the class inheritance graph
    HashMap<String, Node> nodes = new HashMap<String, Node>();
    for (ClassLayout layout : layouts) {
      if (layout.name.contains("$$Lambda@0")) continue; // Skipping lambda classes
      Node current = nodes.get(layout.name);
      if (current == null) {
        current = new Node();
        nodes.put(layout.name, current);
      }
      if (current.classLayout == null) {
        current.classLayout = layout;
      } else {
        System.out.println(current.classLayout.name + " vs " + layout.name);
        Asserts.assertEQ(current.classLayout, layout);
      }
      if (layout.superName != null) {
        Node superNode = nodes.get(layout.superName);
        if (superNode == null) {
          superNode = new Node();
          superNode.subClasses.add(current);
          nodes.put(layout.superName, superNode);
        }
        superNode.subClasses.add(current);
      }
    }
    // Field verification
    for (Node node : nodes.values()) {
      ClassLayout layout = node.classLayout;
      for (FieldBlock block : layout.nonStaticFields) {
        if (block.offset() == 0) continue; // Skip object header
        if (block.type() == BlockType.EMPTY) continue; // Empty spaces can be used by subclasses
        // A special case for PADDING might be needed too => must NOT be used in subclasses
        for (Node subnode : node.subClasses) {
          checkFieldInClass(block, subnode);
        }
      }
    }
  }

  void checkFieldInClass(FieldBlock block, Node node) {
    FieldBlock b = node.classLayout.getFieldAtOffset(block.offset, false);
    Asserts.assertTrue((block.type != BlockType.NULL_MARKER && block.type != BlockType.INHERITED_NULL_MARKER && b.type == BlockType.INHERITED)
                       || ((block.type == BlockType.NULL_MARKER || block.type == BlockType.INHERITED_NULL_MARKER) && b.type == BlockType.INHERITED_NULL_MARKER));
    Asserts.assertEquals(b.signature(), block.signature());
    Asserts.assertEquals(b.name(), block.name());
    Asserts.assertEquals(b.size(), block.size());
    Asserts.assertTrue(b.alignment() == block.alignment());
    for (Node subnode : node.subClasses) {
      checkFieldInClass(block, subnode);
    }
  }

  void checkNullMarkers() {
    for (ClassLayout layout : layouts) {
      for (FieldBlock block : layout.nonStaticFields) {
        if (block.type() == BlockType.FLAT && block.nullMarkerOffset() != -1) {
          if (block.hasInternalNullMarker()) {
            Asserts.assertTrue(block.nullMarkerOffset() > block.offset());
            Asserts.assertTrue(block.nullMarkerOffset() < block.offset() + block.size());
          } else {
            FieldBlock marker = layout.getFieldAtOffset(block.nullMarkerOffset(), false);
            Asserts.assertEquals(block.nullMarkerOffset(), marker.offset());
          }
        }
        if (block.type() == BlockType.NULL_MARKER) {
          FieldBlock flatField = layout.getFieldAtOffset(block.referenceFieldOffset(), false);
          Asserts.assertEquals(flatField.type(), BlockType.FLAT);
          Asserts.assertEquals(flatField.nullMarkerOffset(), block.offset());
        }
      }
      // static layout => must not have NULL_MARKERS because static fields are never flat
      for (FieldBlock block : layout.staticFields) {
        Asserts.assertNotEquals(block.type(), BlockType.NULL_MARKER);
        if (block.type() == BlockType.FLAT) {
          Asserts.assertEquals(block.nullMarkerOffset(), -1); // -1 means no null marker
        }
      }
    }
  }

  void check() {
    checkNoOverlap();
    checkSizeAndAlignment();
    checkInheritedFields();
    checkSubClasses();
    checkNullMarkers();
  }

  private void generate(LogOutput lo) {
    while (lo.hasMoreLines()) {
      if (lo.getCurrentLine().startsWith("Layout of class")) {
        ClassLayout cl = ClassLayout.parseClassLayout(lo);
        layouts.add(cl);
      } else {
        lo.moveToNextLine(); // skipping line
      }
    }
  }
 }