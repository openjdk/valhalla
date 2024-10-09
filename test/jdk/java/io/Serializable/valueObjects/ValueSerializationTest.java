/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal java.base/jdk.internal.value
 * @compile ValueSerializationTest.java
 * @run testng/othervm ValueSerializationTest
 */

import static java.io.ObjectStreamConstants.*;

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

import jdk.internal.MigratedValueClass;
import jdk.internal.value.DeserializeConstructor;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class ValueSerializationTest {

    static final Class<NotSerializableException> NSE = NotSerializableException.class;
    private static final Class<InvalidClassException> ICE = InvalidClassException.class;

    @DataProvider(name = "doesNotImplementSerializable")
    public Object[][] doesNotImplementSerializable() {
        return new Object[][] {
            new Object[] { new NonSerializablePoint(10, 100), NSE},
            new Object[] { new NonSerializablePointNoCons(10, 100), ICE},
            // an array of Points
            new Object[] { new NonSerializablePoint[] {new NonSerializablePoint(1, 5)}, NSE},
            new Object[] { new Object[] {new NonSerializablePoint(3, 7)}, NSE},
            new Object[] { new ExternalizablePoint(12, 102), ICE},
            new Object[] { new ExternalizablePoint[] {
                    new ExternalizablePoint(3, 7),
                    new ExternalizablePoint(2, 8) }, ICE},
            new Object[] { new Object[] {
                    new ExternalizablePoint(13, 17),
                    new ExternalizablePoint(14, 18) }, ICE},
        };
    }

    // value class that DOES NOT implement Serializable should throw ICE
    @Test(dataProvider = "doesNotImplementSerializable")
    public void doesNotImplementSerializable(Object obj, Class expectedException) {
        assertThrows(expectedException, () -> serialize(obj));
    }

    /* Non-Serializable point. */
    public static value class NonSerializablePoint {
        public int x;
        public int y;

        public NonSerializablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override public String toString() {
            return "[NonSerializablePoint x=" + x + " y=" + y + "]";
        }
    }

    /* Non-Serializable point, because it does not have an @DeserializeConstructor constructor. */
    public static value class NonSerializablePointNoCons implements Serializable {
        public int x;
        public int y;

        // Note: Must NOT have @DeserializeConstructor annotation
        public NonSerializablePointNoCons(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override public String toString() {
            return "[NonSerializablePointNoCons x=" + x + " y=" + y + "]";
        }
    }

    /* An Externalizable Point is not Serializable, readExternal cannot modify fields */
    static value class ExternalizablePoint implements Externalizable {
        public int x;
        public int y;
        public ExternalizablePoint() {this.x = 0; this.y = 0;}
        ExternalizablePoint(int x, int y) { this.x = x; this.y = y; }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
        @Override public String toString() {
            return "[ExternalizablePoint x=" + x + " y=" + y + "]"; }
    }

    @DataProvider(name = "ImplementSerializable")
    public Object[][] implementSerializable() {
        return new Object[][]{
                new Object[]{new SerializablePoint(11, 101)},
                new Object[]{new SerializablePoint[]{
                        new SerializablePoint(1, 5),
                        new SerializablePoint(2, 6)}},
                new Object[]{new Object[]{
                        new SerializablePoint(3, 7),
                        new SerializablePoint(4, 8)}},
                new Object[]{new SerializableFoo(45)},
                new Object[]{new SerializableFoo[]{new SerializableFoo(46)}},
                new Object[]{new ExternalizableFoo("hello")},
                new Object[]{new ExternalizableFoo[]{new ExternalizableFoo("there")}},
        };
    }

    // value class that DOES implement Serializable is supported
    @Test(dataProvider = "ImplementSerializable")
    public void implementSerializable(Object obj) throws IOException, ClassNotFoundException {
        byte[] bytes = serialize(obj);
        Object actual = deserialize(bytes);
        if (obj.getClass().isArray())
            assertEquals((Object[])obj, (Object[])actual);
        else
            assertEquals(obj, actual);
    }

    /* A Serializable value class Point */
    @MigratedValueClass
    static value class SerializablePoint implements Serializable {
        public int x;
        public int y;
        @DeserializeConstructor
        private SerializablePoint(int x, int y) { this.x = x; this.y = y; }

        @Override public String toString() {
            return "[SerializablePoint x=" + x + " y=" + y + "]";
        }
    }

    /* A Serializable Foo, with a serial proxy */
    static value class SerializableFoo implements Serializable {
        public int x;
        @DeserializeConstructor
        SerializableFoo(int x) { this.x = x; }

        @Serial Object writeReplace() throws ObjectStreamException {
            return new SerialFooProxy(x);
        }
        @Serial private void readObject(ObjectInputStream s) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }
        private record SerialFooProxy(int x) implements Serializable {
            @Serial Object readResolve() throws ObjectStreamException {
                return new SerializableFoo(x);
            }
        }
    }

    /* An Externalizable Foo, with a serial proxy */
    static value class ExternalizableFoo implements Externalizable {
        public String s;
        ExternalizableFoo(String s) {  this.s = s; }
        public boolean equals(Object other) {
            if (other instanceof ExternalizableFoo foo) {
                return s.equals(foo.s);
            } else {
                return false;
            }
        }
        @Serial  Object writeReplace() throws ObjectStreamException {
            return new SerialFooProxy(s);
        }
        private record SerialFooProxy(String s) implements Serializable {
            @Serial Object readResolve() throws ObjectStreamException {
                return new ExternalizableFoo(s);
            }
        }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
    }

    // Generate a byte stream containing a reference to the named class with the SVID and flags.
    private static byte[] byteStreamFor(String className, long uid, byte flags)
        throws Exception
    {
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

    @DataProvider(name = "classes")
    public Object[][] classes() {
        return new Object[][] {
            new Object[] { ExternalizableFoo.class, SC_EXTERNALIZABLE, ICE },
            new Object[] { ExternalizableFoo.class, SC_SERIALIZABLE, ICE },
            new Object[] { SerializablePoint.class, SC_EXTERNALIZABLE, ICE },
            new Object[] { SerializablePoint.class, SC_SERIALIZABLE, null },
        };
    }

    // value class read directly from a byte stream
    // a byte stream is generated containing a reference to the class with the flags  and SVID.
    // Reading the class from the stream verifies the exceptions thrown if there is a mismatch
    // between the stream and the local class.
    @Test(dataProvider = "classes")
    public void deserialize(Class<?> cls, byte flags, Class<?> expected) throws Exception {
        var clsDesc = ObjectStreamClass.lookup(cls);
        long uid = clsDesc == null ? 0L : clsDesc.getSerialVersionUID();
        byte[] serialBytes = byteStreamFor(cls.getName(), uid, flags);
        try {
            deserialize(serialBytes);
            Assert.assertNull(expected, "Expected exception");
        } catch (IOException ioe) {
            Assert.assertEquals(ioe.getClass(), expected);
        }
    }

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }
}
