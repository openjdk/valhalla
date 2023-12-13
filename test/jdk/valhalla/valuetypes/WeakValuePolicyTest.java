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
 */

/*
 * @test
 * @run testng/othervm WeakValuePolicyTest
 * @summary Test WeakHashMap.ValuePolicy modes
 */
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.WeakHashMap;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

@Test
public class WeakValuePolicyTest {

    record KeyValue(Object key, String value){};

    @DataProvider(name="Keys")
    KeyValue[] keys() {
        return new KeyValue[] {
                new KeyValue(new IntValue(1), "IntValue(1)"),
                new KeyValue(new StringValue("xyz"), "StringValue(\"xyz\")"),
                new KeyValue(Integer.valueOf(2), "Integer.valueOf(2)"),
        };
    }

    @DataProvider(name="WeakHashMaps")
    Object[][] weakValuePolicy() {
        return new Object[][] {
                {new WeakHashMap<Object, String>(0, 0.75f, WeakHashMap.ValuePolicy.SOFT)},
                {new WeakHashMap<Object, String>(0, 0.75f, WeakHashMap.ValuePolicy.STRONG)},
        };
    }

    @Test(dataProvider="WeakHashMaps")
    public void putValueSoftStrong(WeakHashMap map) {
        WeakHashMap.ValuePolicy policy = map.valuePolicy();
        for (KeyValue kv : keys()) {
            System.out.println("k: " + kv.key);
            Assert.assertFalse(map.containsKey(kv.key), "map.contains on empty map: " + kv.key);
            Assert.assertNull(map.get(kv.key), "map.get on empty map: " + kv.key);
            var prev = map.put(kv.key, kv.value);
            Assert.assertNull(prev, "map.put on empty map did not return null: " + kv.key);
            Assert.assertEquals(map.get(kv.key), kv.value, "map.get after put: " + kv.key);

            forceGC();

            if (kv.key.getClass().isValue() &&
                    policy.equals(WeakHashMap.ValuePolicy.SOFT)) {
                Assert.assertFalse(map.containsKey(kv.key), "map.containsKey after GC: " + kv.key);
                String value = (String)map.get(kv.key);
                Assert.assertNull(value, "map.get after GC should return null: " + kv.key);
            } else {
                prev = map.remove(kv.key);
                Assert.assertEquals(prev, kv.value, "map.remove: " + kv.key);
                Assert.assertNull(map.get(kv.key), "map.get after remove: " + kv.key);
            }
            Assert.assertTrue(map.isEmpty(), "m.isEmpty()");
        }
    }

    @Test
    public void putValueDiscard() {
        final WeakHashMap<Object, String> map = new WeakHashMap<>(0, 0.75f, WeakHashMap.ValuePolicy.DISCARD);
        final IntValue intValue = new IntValue(1);
        String old = map.put(intValue, "IntValue(1)");
        Assert.assertNull(old, "old");
        old = map.get(intValue);
        Assert.assertNull(old, "get after put of discarded value");
    }

    @Test
    public void putValueThrows() {
        final WeakHashMap<Object, String> map = new WeakHashMap<>(0, 0.75f, WeakHashMap.ValuePolicy.THROW);
        Assert.assertThrows(IdentityException.class, () -> map.put(new IntValue(1), "IntValue(1)"));
    }

    private void forceGC()  {
        Object marker = new Object();
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        SoftReference expected = new SoftReference(marker, queue);
        marker = null;
        Reference<?> actual = waitForReference(queue);
        assertEquals(actual, expected, "Unexpected Reference queued");
    }

    /**
     * Wait for any Reference to be enqueued to a ReferenceQueue.
     * The garbage collector is invoked to find unreferenced objects.
     *
     * @param queue a ReferenceQueue
     * @return true if the reference was enqueued, false if not enqueued within
     */
    private static Reference<?> waitForReference(ReferenceQueue<Object> queue) {
        Objects.requireNonNull(queue, "queue should not be null");
        ArrayList<Object> chunks = new ArrayList<>(10000);
        try {
            for (int i = 0; i < 10_000; i++) {
                chunks.add(new byte[100_000]);
            }
        } catch (OutOfMemoryError oome) {

        } finally {
            chunks = null;
        }
        for (int retries = 100; retries > 0; retries--) {
            try {
                var r = queue.remove(10L);
                if (r != null) {
                    return r;
                }
            } catch (InterruptedException ie) {
                // ignore, the loop will try again
            }
        }
        return null;
    }

    static value class IntValue {
        int value;

        IntValue(int value) {
            this.value = value;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "IntValue{" + "value=" + value + '}';
        }
    }

    static value class StringValue {
        String value;

        StringValue(String value) {
            this.value = value;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "StringValue{" + "value='" + value + '\'' + '}';
        }
    }
}
