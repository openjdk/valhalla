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
 * @summary ValueSerialization support of value classes
 * @enablePreview
 * @modules java.base/jdk.internal
 * @compile ValueSerialization.java
 * @run testng/othervm ValueSerialization
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
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

public class ValueSerialization {

    static final Class<NotSerializableException> NSE = NotSerializableException.class;

    @DataProvider(name = "doesNotImplementSerializable")
    public Object[][] doesNotImplementSerializable() {
        return new Object[][] {
            new Object[] { new NonSerializablePoint(10, 100) },
            // an array of Points
            new Object[] { new NonSerializablePoint[] {new NonSerializablePoint(1, 5)} },
            new Object[] { new Object[] {new NonSerializablePoint(3, 7)} },
            new Object[] { new ExternalizablePoint(12, 102) },
            new Object[] { new ExternalizablePoint[] {
                    new ExternalizablePoint(3, 7),
                    new ExternalizablePoint(2, 8) } },
            new Object[] { new Object[] {
                    new ExternalizablePoint(13, 17),
                    new ExternalizablePoint(14, 18) } },
        };
    }

    // value class that DOES NOT implement Serializable should throw NSE
    @Test(dataProvider = "doesNotImplementSerializable")
    public void doesNotImplementSerializable(Object obj) {
        assertThrows(NSE, () -> serialize(obj));
    }

    /** Non-Serializable point. */
    public static value class NonSerializablePoint {
        public int x;
        public int y;

        public NonSerializablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** A Serializable value class Point */
    @jdk.internal.MigratedValueClass
    static value class SerializablePoint implements Serializable {
        public int x;
        public int y;
        SerializablePoint(int x, int y) { this.x = x; this.y = y; }
        @Override public String toString() {
            return "[SerializablePoint x=" + x + " y=" + y + "]"; }
    }

    /** A Serializable value class Point */
    @jdk.internal.MigratedValueClass
    static value class SerializablePrimitivePoint implements Serializable {
        public int x;
        public int y;
        SerializablePrimitivePoint(int x, int y) { this.x = x; this.y = y; }
        @Override public String toString() {
            return "[SerializablePrimitivePoint x=" + x + " y=" + y + "]"; }
    }

    /** An Externalizable Point is not Serializable, readExternal cannot modify fields */
    static value class ExternalizablePoint implements Externalizable {
        public int x;
        public int y;
        ExternalizablePoint(int x, int y) { this.x = x; this.y = y; }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
        @Override public String toString() {
            return "[ExternalizablePoint x=" + x + " y=" + y + "]"; }
    }

    @DataProvider(name = "doesImplementSerializable")
    public Object[][] doesImplementSerializable() {
        return new Object[][] {
            new Object[] { new SerializablePoint(11, 101) },
            new Object[] { new SerializablePoint[] {
                    new SerializablePoint(1, 5),
                    new SerializablePoint(2, 6) } },
            new Object[] { new Object[] {
                    new SerializablePoint(3, 7),
                    new SerializablePoint(4, 8) } },
            new Object[] { new SerializablePrimitivePoint(711, 7101) },
            new Object[] { new SerializablePrimitivePoint[] {
                    new SerializablePrimitivePoint(71, 75),
                    new SerializablePrimitivePoint(72, 76) } },
            new Object[] { new Object[] {
                    new SerializablePrimitivePoint(73, 77),
                    new SerializablePrimitivePoint(74, 78) } },
        };
    }

    // value class that DOES implement Serializable all supported
    @Test(dataProvider = "doesImplementSerializable")
    public void doesImplementSerializable(Object obj) throws IOException {
        serialize(obj);
    }

    /** A Serializable Foo, with a serial proxy */
    static value class SerializableFoo implements Serializable {
        public int x;
        SerializableFoo(int x) { this.x = x; }

        Object writeReplace() throws ObjectStreamException {
            return new SerialFooProxy(x);
        }
        private void readObject(ObjectInputStream s) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }
        private static class SerialFooProxy implements Serializable {
            final int x;
            SerialFooProxy(int x) { this.x = x; }
            Object readResolve() throws ObjectStreamException {
                return new SerializableFoo(this.x);
            }
        }
    }

    /** An Externalizable Foo, with a serial proxy */
    static value class ExternalizableFoo implements Externalizable {
        public String s;
        ExternalizableFoo(String s) {  this.s = s; }
        Object writeReplace() throws ObjectStreamException {
            return new SerialFooProxy(s);
        }
        private void readObject(ObjectInputStream s) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }
        private static class SerialFooProxy implements Serializable {
            final String s;
            SerialFooProxy(String s) { this.s = s; }
            Object readResolve() throws ObjectStreamException {
                return new ExternalizableFoo(this.s);
            }
        }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
    }

    // value classes that DO implement Serializable, but have a serial proxy
    @Test
    public void serializableFooWithProxy() throws Exception {
        SerializableFoo foo = new SerializableFoo(45);
        SerializableFoo foo1 = serializeDeserialize(foo);
        assertEquals(foo.x, foo1.x);
    }
    @Test
    public void serializableFooArrayWithProxy() throws Exception {
        SerializableFoo[] fooArray = new SerializableFoo[]{new SerializableFoo(46)};
        SerializableFoo[] fooArray1 = serializeDeserialize(fooArray);
        assertEquals(fooArray.length, fooArray1.length);
        assertEquals(fooArray[0].x, fooArray1[0].x);
    }
    @Test
    public void externalizableFooWithProxy() throws Exception {
        ExternalizableFoo foo = new ExternalizableFoo("hello");
        ExternalizableFoo foo1 = serializeDeserialize(foo);
        assertEquals(foo.s, foo1.s);
    }
    @Test
    public void externalizableFooArrayWithProxy() throws Exception {
        ExternalizableFoo[] fooArray = new ExternalizableFoo[] { new ExternalizableFoo("there") };
        ExternalizableFoo[] fooArray1 = serializeDeserialize(fooArray);
        assertEquals(fooArray.length, fooArray1.length);
        assertEquals(fooArray[0].s, fooArray1[0].s);
    }

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

    private static byte[] serializableByteStreamFor(String className, long uid)
        throws Exception
    {
        return byteStreamFor(className, uid, SC_SERIALIZABLE);
    }

    private static byte[] externalizableByteStreamFor(String className, long uid)
         throws Exception
    {
        return byteStreamFor(className, uid, SC_EXTERNALIZABLE);
    }

    @DataProvider(name = "classes")
    public Object[][] classes() {
        return new Object[][] {
            new Object[] { NonSerializablePoint.class },
            new Object[] { SerializablePoint.class },
            new Object[] { SerializablePrimitivePoint.class }
        };
    }

    static final Class<InvalidClassException> ICE = InvalidClassException.class;

    // value class read directly from a byte stream, both serializable and externalizable are supported
    @Test(dataProvider = "classes")
    public void deserialize(Class<?> cls) throws Exception {
        var clsDesc = ObjectStreamClass.lookup(cls);
        long uid = clsDesc == null ? 0L : clsDesc.getSerialVersionUID();

        byte[] serialBytes = serializableByteStreamFor(cls.getName(), uid);

        byte[] extBytes = externalizableByteStreamFor(cls.getName(), uid);
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

    @SuppressWarnings("unchecked")
    static <T> T serializeDeserialize(T obj)
        throws IOException, ClassNotFoundException
    {
        return (T) deserialize(serialize(obj));
    }
}
