/*
 * Copyright (c) 1999, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @summary Serialize and deserialize value objects
 * @enablePreview
 * @modules java.base/jdk.internal
 * @modules java.base/jdk.internal.value
 * @run testng/othervm SimpleValueGraphs
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.InvalidClassException;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.Objects;

import java.nio.charset.StandardCharsets;

import jdk.internal.value.DeserializeConstructor;
import jdk.internal.MigratedValueClass;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import jdk.test.lib.hexdump.HexPrinter;
import jdk.test.lib.hexdump.ObjectStreamPrinter;

@Test
public class SimpleValueGraphs implements Serializable {

    private static boolean DEBUG = true;

    private static SimpleValue foo1 = new SimpleValue("One", 1);
    private static SimpleValue foo2 = new SimpleValue("Two", 2);

    @DataProvider(name = "ValueObjects")
    public Object[][] valueObjects() {
        return new Object[][] {
                {new SimpleValue(new SimpleValue(1))},
                {new SimpleValue(new SimpleValue(2), 3)},
                {new SimpleValue[] {foo1, foo1, foo2, foo1, foo2 }},
        };
    }

    @Test(enabled = true, dataProvider = "ValueObjects")
    public void roundTrip(Object expected) throws Exception {
        byte[] bytes = serialize(expected);

        if (DEBUG)
            HexPrinter.simple().dest(System.out).formatter(ObjectStreamPrinter.formatter()).format(bytes);

        Object actual = deserialize(bytes);
        System.out.println("actual: " + actual.toString());
        Assert.assertEquals(actual, expected, "Mismatch" + expected.getClass());
    }

    private static Tree treeI = Tree.makeTree(3, (l, r) -> new TreeI((TreeI)l, (TreeI)l));
    private static Tree treeV = Tree.makeTree(3, (l, r) -> new TreeV((TreeV)l, (TreeV)l));

    // Create a tree of identity objects with a cycle; it will serialize ok, but when deserialized as
    // a value class the cycle is broken by replacing the back ref with null.
    private static Tree treeCycle(boolean cycle) {
        TreeI tree = (TreeI)Tree.makeTree(3, (l, r) -> new TreeI((TreeI)l, (TreeI)l));
        tree.setLeft(cycle ? tree : null);     // force a cycle or null
        return tree;
    }

    @DataProvider(name = "CompatibleChanges")
    public Object[][] migrationObjects() {
        return new Object[][] {
                {treeI, "TreeI", "TreeV", treeV},       // Serialize as an identity class, deserialize as Value class
                {treeCycle(true), "TreeI", "TreeV", treeCycle(false)},
                // TBD: add cases for serializing TreeV and converting to TreeI:
                // Waiting for:
                // JDK-8293321: [lworld] PrimitiveObjectMethods.substitutableInvoker StackOverFlowError
        };
    }

    /**
     * Test serializing an object graph, and deserialize with a modification of the serialized form.
     * The modifications to the stream change the class name being deserialized.
     * The cases include serializing an identity class and deserialize the corresponding
     * value class.
     *
     * @param origObj an object to serialize
     * @param origName a string in the serialized stream to replace
     * @param replName a string to replace the original string
     * @param expectedObject the expected object (graph) or an exception if it should fail
     * @throws Exception some unexpected exception may be thrown and cause the test to fail
     */
    @Test(enabled = true, dataProvider = "CompatibleChanges")
    public void treeVTest(Object origObj, String origName, String replName, Object expectedObject) throws Exception {
        byte[] bytes = serialize(origObj);
        if (DEBUG) {
            System.out.println("Original serialized " + origObj.getClass().getName());
            HexPrinter.simple().dest(System.out).formatter(ObjectStreamPrinter.formatter()).format(bytes);
        }

        // Modify the serialized bytes to change a class name from the serialized name
        // to a different class. The replacement name must be the same length as the original name.
        byte[] replBytes = patchBytes(bytes, origName, replName);
        if (DEBUG) {
            System.out.println("Modified serialized " + origObj.getClass().getName());
            HexPrinter.simple().dest(System.out).formatter(ObjectStreamPrinter.formatter()).format(replBytes);
        }
        try {
            Object actual = deserialize(replBytes);
            if (expectedObject instanceof Exception ex) {
                Assert.fail("Unexpected: " + expectedObject);
            }
            // Compare the shape of the actual and expected trees
            Assert.assertEquals(actual.toString(), expectedObject.toString(),
                    "Resulting object not equals: " + actual.getClass().getName());

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.assertEquals(ex.getClass(), expectedObject.getClass(), ex.toString());
            Assert.assertEquals(ex.getMessage(), ((Exception)expectedObject).getMessage(), ex.toString());
        }
    }

    /**
     * Serialize an object and return the serialized bytes.
     *
     * @param expected an object to serialize
     * @return a byte array containing the serialized object
     */
    private static byte[] serialize(Object expected) throws IOException {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             ObjectOutputStream oout = new ObjectOutputStream(bout)) {
            oout.writeObject(expected);
            oout.flush();
            return bout.toByteArray();
        }
    }

    /**
     * Deserialize an object from the byte array.
     * @param bytes a byte array
     * @return an Object read from the byte array
     */
    private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
             ObjectInputStream oin = new ObjectInputStream(bin)) {
            return oin.readObject();
        }
    }

    /**
     * Replace every occurrence of the string in the byte array with the replacement.
     * The strings are US_ASCII only.
     * @param bytes a byte array
     * @param orig a string, converted to bytes using US_ASCII, originally exists in the bytes
     * @param repl a string, converted to byted using US_ASCII, to replace the original bytes
     * @return a new byte array that has been patched
     */
    private byte[] patchBytes(byte[] bytes, String orig, String repl) {
        byte[] alt = patchBytes(bytes,
                orig.getBytes(StandardCharsets.US_ASCII),
                repl.getBytes(StandardCharsets.US_ASCII));
        return alt;
    }

    /**
     * Replace every occurrence of the original bytes in the byte array with the replacement bytes.
     * @param bytes a byte array
     * @param orig a byte array containing existing bytes in the byte array
     * @param repl a byte array to replace the original bytes
     * @return a copy of the bytes array with each occurence of the orig bytes with the replacement bytes
     */
    static byte[] patchBytes(byte[] bytes, byte[] orig, byte[] repl) {
        if (orig.length != repl.length && orig.length > 0)
            throw new IllegalArgumentException("orig bytes and replacement must be same length");
        byte[] result = Arrays.copyOf(bytes, bytes.length);
        for (int i = 0; i < result.length - orig.length; i++) {
            if (Arrays.equals(result, i, i + orig.length, orig, 0, orig.length)) {
                for (int j = 0; j < orig.length; j++) {
                    result[i + j] = repl[j];
                }
                i = i + orig.length - 1;    // continue replacing after this occurrence
            }
        }
        return result;
    }

    public static class SimpleValue implements Serializable {
        private static final long serialVersionUID = 1L;

        int i;
        Serializable obj;

        public SimpleValue(Serializable o) {
            this.obj = o;
            this.i = 0;
        }
        SimpleValue(int i) {
            this.i = i;
        }

        public SimpleValue(Serializable o, int i) {
            this.obj = o;
            this.i = i;
        }

        public boolean equals(Object other) {
            if (other instanceof SimpleValue simpleValue) {
                return (i == simpleValue.i && Objects.equals(obj, simpleValue.obj));
            }
            return false;
        }

        public int hashCode() {
            return i;
        }

        public String toString() {
            return "SimpleValue{" + "i=" + i + ", obj=" + obj + '}';
        }
    }

    interface Tree {
        static Tree makeTree(int depth, BiFunction<Tree, Tree, Tree> genNode) {
            if (depth <= 0) return null;
            Tree left = makeTree(depth - 1, genNode);
            Tree right = makeTree(depth - 1, genNode);
            Tree t = genNode.apply(left, right);
            return t;
        }

        Tree left();
        Tree right();
    }
    static class TreeI implements Tree, Serializable {

        private static final long serialVersionUID = 2L;
        private TreeI left;
        private TreeI right;

        TreeI(TreeI left, TreeI right) {
            this.left = left;
            this.right = right;
        }

        public TreeI left() {
            return left;
        }
        public TreeI right() {
            return right;
        }

        public void setLeft(TreeI left) {
            this.left = left;
        }
        public void setRight(TreeI right) {
            this.right = right;
        }

        public boolean equals(Object other) {
            if (other instanceof TreeV tree) {
                boolean leftEq = (this.left == null && tree.left == null) ||
                        left.equals(tree.left);
                boolean rightEq = (this.right == null && tree.right == null) ||
                        right.equals(tree.right);
                return leftEq == rightEq;
            }
            return false;
        }
        public String toString() {
            return toString(5);
        }
        public String toString(int depth) {
            if (depth <= 0)
                return "!";
            String l = (left != null) ? left.toString(depth - 1) : Character.toString(126);
            String r = (right != null) ? right.toString(depth - 1) : Character.toString(126);
            return "(" + l + r + ")";
        }
    }

    @MigratedValueClass
    static value class TreeV implements Tree, Serializable {

        private static final long serialVersionUID = 2L;
        private TreeV left;
        private TreeV right;

        @DeserializeConstructor
        TreeV(TreeV left, TreeV right) {
            this.left = left;
            this.right = right;
        }

        public TreeV left() {
            return left;
        }
        public TreeV right() {
            return right;
        }

        public boolean equals(Object other) {
            // avoid ==, is substutible check causes stack overflow.
            if (other instanceof TreeV tree) {
                return compRef(this.left, tree.left) && compRef(this.right, tree.right);
            }
            return false;
        }

        // Compare references but don't use ==; isSubstitutable may recurse
        private static boolean compRef(Object o1, Object o2) {
            if (o1 == null && o2 == null)
                return true;
            if (o1 != null && o2 != null)
                return o1.equals(o2);
            return false;

        }
        public String toString() {
            return toString(10);
        }
        public String toString(int depth) {
            if (depth <= 0)
                return "!";
            String l = (left != null) ? left.toString(depth - 1) : Character.toString(126);
            String r = (right != null) ? right.toString(depth - 1) : Character.toString(126);
            return "(" + l + r + ")";
        }
    }

    @Test
    void testExternalizableNotSer() {
        var obj = new ValueExt();
        var ex = Assert.expectThrows(InvalidClassException.class, () -> serialize(obj));
        Assert.assertEquals(ex.getMessage(),
                "SimpleValueGraphs$ValueExt; Externalizable not valid for value class");
    }

    @Test
    void testExternalizableNotDeser() throws IOException {
        var obj = new IdentExt();
        byte[] bytes = serialize(obj);
        byte[] newBytes = patchBytes(bytes, "IdentExt", "ValueExt");
        var ex = Assert.expectThrows(InvalidClassException.class, () -> deserialize(newBytes));
        Assert.assertTrue(ex.getMessage().contains("Externalizable not valid for value class"));
    }

    // Exception trying to serialize
    // Exception trying to deserialize

    static class IdentExt implements Externalizable {
        public void writeExternal(ObjectOutput is) {

        }
        public void readExternal(ObjectInput is) {

        }
        private static final long serialVersionUID = 3L;
    }

    // Not Desrializable or Deserializable, no writeable fields
    static value class ValueExt implements Externalizable {
        public void writeExternal(ObjectOutput is) {

        }
        public void readExternal(ObjectInput is) {

        }
        private static final long serialVersionUID = 3L;

    }
}
