/*
 * Copyright (c) 2019, 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Test serialization of value classes
 * @enablePreview
 * @modules java.base/jdk.internal.value
 * @library /test/lib
 * @compile ValueSerializationTest.java
 * @comment run the StrictProcessor over the IdentityStrictPoint to generate a class with
 *          STRICT_INIT access flags for its annotated fields
 * @run driver jdk.test.lib.helpers.StrictProcessor ValueSerializationTest$IdentityStrictPoint
 * @run junit ${test.main.class}
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.stream.Stream;

import jdk.internal.value.Deserializer;
import jdk.test.lib.helpers.StrictInit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static java.io.ObjectStreamConstants.SC_EXTERNALIZABLE;
import static java.io.ObjectStreamConstants.SC_SERIALIZABLE;
import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.STREAM_VERSION;
import static java.io.ObjectStreamConstants.TC_CLASSDESC;
import static java.io.ObjectStreamConstants.TC_ENDBLOCKDATA;
import static java.io.ObjectStreamConstants.TC_NULL;
import static java.io.ObjectStreamConstants.TC_OBJECT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValueSerializationTest {

    private static final Class<NotSerializableException> NSE = NotSerializableException.class;
    private static final Class<InvalidClassException> ICE = InvalidClassException.class;

    static Stream<Arguments> doesNotImplementSerializable() {
        return Stream.of(
                Arguments.of(
                        new NonSerializableValue(10, 100),
                        NSE
                ),

                Arguments.of(
                        new ValueWithNoDeserializer(10, 100),
                        ICE
                ),

                Arguments.of(
                        new IdentityStrictPoint(),
                        ICE
                ),

                // an array of non-serializable value objects
                Arguments.of(
                        new NonSerializableValue[]{
                                new NonSerializableValue(1, 5)
                        },
                        NSE
                ),

                Arguments.of(
                        new Object[]{
                                new NonSerializableValue(3, 7)
                        },
                        NSE
                ),

                Arguments.of(
                        new ExternalizableValue(12, 102),
                        ICE
                ),

                Arguments.of(
                        new ExternalizableValue[]{
                                new ExternalizableValue(3, 7),
                                new ExternalizableValue(2, 8)
                        },
                        ICE
                ),

                Arguments.of(
                        new Object[]{
                                new ExternalizableValue(13, 17),
                                new ExternalizableValue(14, 18)
                        },
                        ICE
                )
        );
    }

    /*
     * Verifies that a value object that doesn't implement java.io.Serializable
     * throws the expected exception from ObjectOutputStream.writeObject()
     */
    @ParameterizedTest
    @MethodSource("doesNotImplementSerializable")
    void doesNotImplementSerializable(Object obj, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> serialize(obj));
    }

    static Stream<Arguments> implementSerializable() {
        return Stream.of(
                Arguments.of(
                        new ValueWithDeserializer(11, 101)
                ),

                Arguments.of(
                        (Object) new ValueWithDeserializer[]{
                                new ValueWithDeserializer(1, 5),
                                new ValueWithDeserializer(2, 6)
                        }
                ),

                Arguments.of(
                        (Object) new Object[]{
                                new ValueWithDeserializer(3, 7),
                                new ValueWithDeserializer(4, 8)
                        }
                ),

                Arguments.of(
                        new ValueWriteReplaceWithIdentity(45)
                ),

                Arguments.of(
                        (Object) new ValueWriteReplaceWithIdentity[]{
                                new ValueWriteReplaceWithIdentity(46)
                        }
                ),

                Arguments.of(
                        new ExtValueWithIdentityReplacement("hello")
                ),

                Arguments.of(
                        (Object) new ExtValueWithIdentityReplacement[]{
                                new ExtValueWithIdentityReplacement("there")
                        }
                )
        );
    }

    /*
     * Verifies that a value object that implements java.io.Serializable and is associated with
     * the JDK internal jdk.internal.value.Deserializer can be serialized and deserialized through
     * the use of ObjectOutputStream.writeObject() and ObjectInputStream.readObject() successfully.
     * The deserialized object is then compared with the given obj to verify that they are equal.
     */
    @ParameterizedTest
    @MethodSource("implementSerializable")
    void implementSerializable(Object obj) throws IOException, ClassNotFoundException {
        byte[] bytes = serialize(obj);
        Object actual = deserialize(bytes);
        if (obj.getClass().isArray()) {
            assertArrayEquals((Object[]) actual, (Object[]) obj);
        } else {
            assertEquals(actual, obj);
        }
    }

    static Stream<Arguments> classes() {
        return Stream.of(
                Arguments.of(
                        ExtValueWithIdentityReplacement.class,
                        SC_EXTERNALIZABLE,
                        ICE
                ),

                Arguments.of(
                        ExtValueWithIdentityReplacement.class,
                        SC_SERIALIZABLE,
                        ICE
                ),

                Arguments.of(
                        ValueWithDeserializer.class,
                        SC_EXTERNALIZABLE,
                        ICE
                ),

                Arguments.of(
                        ValueWithDeserializer.class,
                        SC_SERIALIZABLE,
                        null
                )
        );
    }

    /*
     * A byte stream is generated containing a reference to the given value class
     * with the given flags and a serial version UID determined in the test method.
     * The byte stream is then read using ObjectInputStream.readObject() and the test verifies
     * that if an exception is expected to be thrown then it is thrown, or if the deserialization
     * is expected to complete normally, then it verifies that no exception is thrown.
     */
    @ParameterizedTest
    @MethodSource("classes")
    void deserialize(Class<?> valueClass, byte flags, Class<Exception> expected) throws Exception {
        // as a precaution verify that we are indeed testing a value class
        assertTrue(valueClass.isValue(), "not a value class: " + valueClass);
        ObjectStreamClass clsDesc = ObjectStreamClass.lookup(valueClass);
        long uid = clsDesc == null ? 0L : clsDesc.getSerialVersionUID();
        byte[] serialBytes = byteStreamFor(valueClass.getName(), uid, flags);
        if (expected == null) {
            assertDoesNotThrow(() -> deserialize(serialBytes));
        } else {
            assertThrows(expected, () -> deserialize(serialBytes));
        }
    }

    // Generate a byte stream containing a reference to the named class with the SVID and flags.
    private static byte[] byteStreamFor(String className, long uid, byte flags) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(className);
        dos.writeLong(uid);
        dos.writeByte(flags);
        dos.writeShort(0);             // number of fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.close();
        return baos.toByteArray();
    }

    private static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] streamBytes) throws IOException,
            ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }

    /**
     * A concrete value class that doesn't implement Serializable (or Externalizable) interface
     */
    public static value class NonSerializableValue {
        public int x;
        public int y;

        public NonSerializableValue(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[NonSerializableValue x=" + x + " y=" + y + "]";
        }
    }

    /**
     * An identity class with strict initialized instance fields
     */
    public static class IdentityStrictPoint implements Serializable {
        static {
            for (var f : IdentityStrictPoint.class.getDeclaredFields()) {
                assertTrue(f.isStrictInit(), f.getName());
            }
        }

        @StrictInit
        public int x;
        @StrictInit
        public int y;

        public IdentityStrictPoint() {
            x = 3;
            y = 5;
            super();
        }
    }

    /**
     * A concrete value class that implements java.io.Serializable and doesn't have any
     * jdk.internal.value.Deserializer on its constructor.
     */
    public static value class ValueWithNoDeserializer implements Serializable {
        public int x;
        public int y;

        // Note: Must NOT have @Deserializer annotation
        public ValueWithNoDeserializer(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[ValueWithNoDeserializer x=" + x + " y=" + y + "]";
        }
    }

    /**
     * A concrete value class which implements java.io.Externalizable and doesn't
     * implement writeReplace().
     */
    static value class ExternalizableValue implements Externalizable {
        public int x;
        public int y;

        public ExternalizableValue() {
            this.x = 0;
            this.y = 0;
        }

        ExternalizableValue(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void readExternal(ObjectInput in) {
        }

        @Override
        public void writeExternal(ObjectOutput out) {
        }

        @Override
        public String toString() {
            return "[ExternalizableValue x=" + x + " y=" + y + "]";
        }
    }


    /**
     * A concrete value class which implements java.io.Serializable and has a
     * jdk.internal.value.Deserializer associated with its constructor.
     */
    static value class ValueWithDeserializer implements Serializable {
        public int x;
        public int y;

        @Deserializer({"x", "y"})
        private ValueWithDeserializer(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[ValueWithDeserializer x=" + x + " y=" + y + "]";
        }
    }

    /**
     * A concrete value class which implements java.io.Serializable
     * and implements the writeReplace() method to return an identity
     * object.
     */
    static value class ValueWriteReplaceWithIdentity implements Serializable {
        public int x;

        ValueWriteReplaceWithIdentity(int x) {
            this.x = x;
        }

        @Serial
        Object writeReplace() throws ObjectStreamException {
            return new IdentityRecord(x);
        }

        @Serial
        private void readObject(ObjectInputStream s) throws InvalidObjectException {
            throw new InvalidObjectException("not expected to be invoked on " + this);
        }

        private record IdentityRecord(int x) implements Serializable {
            @Serial
            Object readResolve() throws ObjectStreamException {
                return new ValueWriteReplaceWithIdentity(x);
            }
        }
    }

    /**
     * A concrete value class which implements java.io.Externalizable and implements
     * the writeReplace() method to return an identity object.
     */
    static value class ExtValueWithIdentityReplacement implements Externalizable {
        public String s;

        ExtValueWithIdentityReplacement(String s) {
            this.s = s;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ExtValueWithIdentityReplacement foo) {
                return s.equals(foo.s);
            } else {
                return false;
            }
        }

        @Serial
        Object writeReplace() throws ObjectStreamException {
            return new IdentityRecord(s);
        }

        private record IdentityRecord(String s) implements Serializable {
            @Serial
            Object readResolve() throws ObjectStreamException {
                return new ExtValueWithIdentityReplacement(s);
            }
        }

        @Override
        public void readExternal(ObjectInput in) {
        }

        @Override
        public void writeExternal(ObjectOutput out) {
        }
    }
}
