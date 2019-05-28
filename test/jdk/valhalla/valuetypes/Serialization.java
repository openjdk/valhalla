/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary No Serialization support of inline value classes, without a proxy
 * @compile -XDallowWithFieldOperator Point.java Line.java NonFlattenValue.java Serialization.java
 * @run testng/othervm -XX:+EnableValhalla Serialization
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

public class Serialization {

    static final Class<NotSerializableException> NSE = NotSerializableException.class;

    @DataProvider(name = "doesNotImplementSerializable")
    public Object[][] doesNotImplementSerializable() {
        return new Object[][] {
            new Object[] { Point.makePoint(10, 100) },
            new Object[] { Line.makeLine(Point.makePoint(99, 99),
                                         Point.makePoint(888, 888)) },
            new Object[] { NonFlattenValue.make(1001, 10005) },
            // an array of Points
            new Object[] { new Point[] {
                    Point.makePoint(1, 5),
                    Point.makePoint(2, 6) } },
            new Object[] { new Object[] {
                    Point.makePoint(3, 7),
                    Point.makePoint(4, 8) } },
        };
    }

    // inline class that DOES NOT implement Serializable should throw NSE
    @Test(dataProvider = "doesNotImplementSerializable")
    public void doesNotImplementSerializable(Object obj) {
        assertThrows(NSE, () -> serialize(obj));
    }

    /** A Serializable Point */
    static inline class SerializablePoint implements Serializable {
        public int x;
        public int y;
        SerializablePoint() { x = 10; y = 20; }
        static SerializablePoint make(int x, int y) {
            SerializablePoint p = SerializablePoint.default;
            p = __WithField(p.x, x);
            p = __WithField(p.y, y);
            return p;
        }
        @Override public String toString() {
            return "[SerializablePoint x=" + x + " y=" + y + "]"; }
    }

    /** An Externalizable Point */
    static inline class ExternalizablePoint implements Externalizable {
        public int x;
        public int y;
        ExternalizablePoint() { x = 11; y = 21; }
        static ExternalizablePoint make(int x, int y) {
            ExternalizablePoint p = ExternalizablePoint.default;
            p = __WithField(p.x, x);
            p = __WithField(p.y, y);
            return p;
        }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
        @Override public String toString() {
            return "[ExternalizablePoint x=" + x + " y=" + y + "]"; }
    }

    @DataProvider(name = "doesImplementSerializable")
    public Object[][] doesImplementSerializable() {
        return new Object[][] {
            new Object[] { SerializablePoint.make(11, 101) },
            new Object[] { ExternalizablePoint.make(12, 102) },
            new Object[] { new ExternalizablePoint[] {
                    ExternalizablePoint.make(3, 7),
                    ExternalizablePoint.make(2, 8) } },
            new Object[] { new SerializablePoint[] {
                    SerializablePoint.make(1, 5),
                    SerializablePoint.make(2, 6) } },
            new Object[] { new Object[] {
                    SerializablePoint.make(3, 7),
                    SerializablePoint.make(4, 8) } },
            new Object[] { new Object[] {
                    ExternalizablePoint.make(13, 17),
                    ExternalizablePoint.make(14, 18) } },
        };
    }

    // inline class that DOES implement Serializable should throw NSE
    @Test(dataProvider = "doesImplementSerializable")
    public void doesImplementSerializable(Object obj) {
        assertThrows(NSE, () -> serialize(obj));
    }

    /** A Serializable Foo, with a serial proxy */
    static inline class SerializableFoo implements Serializable {
        public int x;
        SerializableFoo() {  x = 10; }
        static SerializableFoo make(int x) {
            SerializableFoo p = SerializableFoo.default;
            p = __WithField(p.x, x);
            return p;
        }
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
                return SerializableFoo.make(this.x);
            }
        }
    }

    /** An Externalizable Foo, with a serial proxy */
    static inline class ExternalizableFoo implements Externalizable {
        public String s;
        ExternalizableFoo() {  s = "hello"; }
        static ExternalizableFoo make(String s) {
            ExternalizableFoo p = ExternalizableFoo.default;
            p = __WithField(p.s, s);
            return p;
        }
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
                return ExternalizableFoo.make(this.s);
            }
        }
        @Override public void readExternal(ObjectInput in) {  }
        @Override public void writeExternal(ObjectOutput out) {  }
    }

    // inline classes that DO implement Serializable, but have a serial proxy
    @Test
    public void serializableFooWithProxy() throws Exception {
        SerializableFoo foo = SerializableFoo.make(45);
        SerializableFoo foo1 = serializeDeserialize(foo);
        assertEquals(foo.x, foo1.x);
    }
    @Test
    public void serializableFooArrayWithProxy() throws Exception {
        SerializableFoo[] fooArray = new SerializableFoo[]{SerializableFoo.make(46)};
        SerializableFoo[] fooArray1 = serializeDeserialize(fooArray);
        assertEquals(fooArray.length, fooArray1.length);
        assertEquals(fooArray[0].x, fooArray1[0].x);
    }
    @Test
    public void externalizableFooWithProxy() throws Exception {
        ExternalizableFoo foo = ExternalizableFoo.make("hello");
        ExternalizableFoo foo1 = serializeDeserialize(foo);
        assertEquals(foo.s, foo1.s);
    }
    @Test
    public void externalizableFooArrayWithProxy() throws Exception {
        ExternalizableFoo[] fooArray = new ExternalizableFoo[] { ExternalizableFoo.make("there") };
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

    @DataProvider(name = "inlineClasses")
    public Object[][] inlineClasses() {
        return new Object[][] {
            new Object[] { Point.class             },
            new Object[] { SerializablePoint.class }
        };
    }

    static final Class<InvalidClassException> ICE = InvalidClassException.class;

    // inline class read directly from a byte stream
    @Test(dataProvider = "inlineClasses")
    public void deserialize(Class<?> cls) throws Exception {
        var clsDesc = ObjectStreamClass.lookup(cls);
        long uid = clsDesc == null ? 0L : clsDesc.getSerialVersionUID();

        byte[] serialBytes = serializableByteStreamFor(cls.getName(), uid);
        expectThrows(ICE, () -> deserialize(serialBytes));

        byte[] extBytes = externalizableByteStreamFor(cls.getName(), uid);
        expectThrows(ICE, () -> deserialize(extBytes));
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
