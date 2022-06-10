/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

import java.io.IOException;
import java.util.Objects;

/**
 * An array of values, that is, an array of ints, boolean, floats, etc.
 * or flat array of primitive objects.
 *
 * @author      Bill Foote
 */
public class JavaValueArray extends JavaLazyReadObject
                /*imports*/ implements ArrayTypeCodes {

    private static int elementSize(byte type) {
        switch (type) {
            case 'B':
            case 'Z':
                return 1;
            case 'C':
            case 'S':
                return 2;
            case 'I':
            case 'F':
                return 4;
            case 'J':
            case 'D':
                return 8;
            default:
                throw new RuntimeException("invalid array element type: " + type);
        }
    }

    /*
     * Java primitive array record (HPROF_GC_PRIM_ARRAY_DUMP) looks
     * as below:
     *
     *    object ID
     *    stack trace serial number (int)
     *    number of elements (int)
     *    element type (byte)
     *    array data
     */
    @Override
    protected final long readValueLength() throws IOException {
        long offset = getOffset() + idSize() + 4;
        // length of the array in elements
        long len = buf().getInt(offset);
        // byte length of array
        return len * elementSize(getRealElementType());
    }

    private long dataStartOffset() {
        return getOffset() + idSize() + 4 + 4 + 1;
    }


    @Override
    protected final JavaThing[] readValue() throws IOException {
        int len = getLength();
        long offset = dataStartOffset();

        JavaThing[] res = new JavaThing[len];
        synchronized (buf()) {
            switch (getElementType()) {
                case 'Z': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaBoolean(booleanAt(offset));
                                  offset += 1;
                              }
                              return res;
                }
                case 'B': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaByte(byteAt(offset));
                                  offset += 1;
                              }
                              return res;
                }
                case 'C': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaChar(charAt(offset));
                                  offset += 2;
                              }
                              return res;
                }
                case 'S': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaShort(shortAt(offset));
                                  offset += 2;
                              }
                              return res;
                }
                case 'I': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaInt(intAt(offset));
                                  offset += 4;
                              }
                              return res;
                }
                case 'J': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaLong(longAt(offset));
                                  offset += 8;
                              }
                              return res;
                }
                case 'F': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaFloat(floatAt(offset));
                                  offset += 4;
                              }
                              return res;
                }
                case 'D': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaDouble(doubleAt(offset));
                                  offset += 8;
                              }
                              return res;
                }
                case 'Q': {
                    for (int i = 0; i < len; i++) {
                        res[i] = new InlinedJavaObject(flatArrayElementClass, offset);
                        offset += flatArrayElementClass.getInlinedInstanceSize();
                    }
                    return res;
                }
                default: {
                             throw new RuntimeException("unknown primitive type?");
                }
            }
        }
    }

    // JavaClass set only after resolve.
    private JavaClass clazz;

    private long objID;

    // This field contains elementSignature byte and
    // divider to be used to calculate length. Note that
    // length of content byte[] is not same as array length.
    // Actual array length is (byte[].length / divider)
    private int data;

    // First 8 bits of data is used for element signature
    private static final int SIGNATURE_MASK = 0x0FF;

    // Next 8 bits of data is used for length divider
    private static final int LENGTH_DIVIDER_MASK = 0x0FF00;

    // Number of bits to shift to get length divider
    private static final int LENGTH_DIVIDER_SHIFT = 8;

    // Flat array support.
    private JavaClass flatArrayElementClass;

    public JavaValueArray(long id, byte elementSignature, long offset) {
        super(offset);
        this.objID = id;
        this.data = (elementSignature & SIGNATURE_MASK);
    }

    public JavaClass getClazz() {
        return clazz;
    }

    public boolean isFlatArray() {
        return flatArrayElementClass != null;
    }

    public JavaClass getFlatElementClazz() {
        return flatArrayElementClass;
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        super.visitReferencedObjects(v);
    }

    public void resolve(Snapshot snapshot) {
        if (clazz instanceof JavaClass) {
            return;
        }

        byte elementType = getElementType();
        String elementSig = "" + (char)elementType;
        // Check if this is a flat array of primitive objects.
        Number elementClassID = snapshot.findFlatArrayElementType(objID);
        if (elementClassID != null) {
            // This is flat array.
            JavaHeapObject elementClazz = snapshot.findThing(getIdValue(elementClassID));
            if (elementClazz instanceof JavaClass elementJavaClazz) {
                flatArrayElementClass = elementJavaClazz;
                // need to resolve the element class
                flatArrayElementClass.resolve(snapshot);
                elementSig = "Q" + flatArrayElementClass.getName() + ";";
            } else {
                // The class not found.
                System.out.println("WARNING: flat array element class not found");
            }
        }
        clazz = snapshot.getArrayClass(elementSig);
        getClazz().addInstance(this);
        super.resolve(snapshot);
    }

    public int getLength() {
        int divider = (data & LENGTH_DIVIDER_MASK) >>> LENGTH_DIVIDER_SHIFT;
        if (divider == 0) {
            byte elementSignature = getElementType();
            switch (elementSignature) {
            case 'B':
            case 'Z':
                divider = 1;
                break;
            case 'C':
            case 'S':
                divider = 2;
                break;
            case 'I':
            case 'F':
                divider = 4;
                break;
            case 'J':
            case 'D':
                divider = 8;
                break;
            case 'Q':
                divider = flatArrayElementClass.getInlinedInstanceSize();
                break;
            default:
                throw new RuntimeException("unknown primitive type: " +
                                elementSignature);
            }
            data |= (divider << LENGTH_DIVIDER_SHIFT);
        }
        return (int)(getValueLength() / divider);
    }

    public JavaThing[] getElements() {
        return getValue();
    }

    public byte getElementType() {
        return isFlatArray() ? (byte)'Q' : getRealElementType();
    }

    private byte getRealElementType() {
        return (byte) (data & SIGNATURE_MASK);
    }

    private void checkIndex(int index) {
        Objects.checkIndex(index, getLength());
    }

    private void requireType(char type) {
        if (getElementType() != type) {
            throw new RuntimeException("not of type : " + type);
        }
    }

    public String valueString() {
        return valueString(true);
    }

    public String valueString(boolean bigLimit) {
        // Char arrays deserve special treatment
        StringBuilder result;
        JavaThing[] things = getValue();
        byte elementSignature = getElementType();
        if (elementSignature == 'C' && !isFlatArray())  {
            result = new StringBuilder();
            for (int i = 0; i < things.length; i++) {
                result.append(things[i]);
            }
        } else {
            int limit = 8;
            if (bigLimit) {
                limit = 1000;
            }
            result = new StringBuilder("{");
            for (int i = 0; i < things.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                if (i >= limit) {
                    result.append("... ");
                    break;
                }
                switch (elementSignature) {
                    case 'Z': {
                        boolean val = ((JavaBoolean)things[i]).value;
                        if (val) {
                            result.append("true");
                        } else {
                            result.append("false");
                        }
                        break;
                    }
                    case 'B': {
                        byte val = ((JavaByte)things[i]).value;
                        result.append("0x").append(Integer.toString(val, 16));
                        break;
                    }
                    case 'S': {
                        short val = ((JavaShort)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'I': {
                        int val = ((JavaInt)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'J': {         // long
                        long val = ((JavaLong)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'F': {
                        float val = ((JavaFloat)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'D': {         // double
                        double val = ((JavaDouble)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'Q': {
                        InlinedJavaObject obj = (InlinedJavaObject)things[i];
                        result.append(obj);
                    }
                    default: {
                        throw new RuntimeException("unknown primitive type?");
                    }
                }
            }
            result.append('}');
        }
        return result.toString();
    }
}
