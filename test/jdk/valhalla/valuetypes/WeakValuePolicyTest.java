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
 * @summary Test WeakHashMap.ValuePolicy modes
 * @run testng/othervm WeakValuePolicyTest
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

    @DataProvider(name="Keys")
    Object[][] keys() {
        return new Object[][] {
                {new IntValue(1), "IntValue(1)"},
                {new StringValue("xyz"), "StringValue(\"xyz\")"},
                {Integer.valueOf(2), "Integer.valueOf(2)"},
        };
    }

    @DataProvider(name="WeakHashMaps")
    Object[][] weakValuePolicy() {
        return new Object[][] {
                {new WeakHashMap<Object, String>(0, 0.75f, WeakHashMap.ValuePolicy.SOFT), WeakHashMap.ValuePolicy.SOFT},
                {new WeakHashMap<Object, String>(0, 0.75f, WeakHashMap.ValuePolicy.STRONG), WeakHashMap.ValuePolicy.STRONG},
//                {new WeakHashMap<Object, String>(0, 0.75f, WeakHashMap.ValuePolicy.THROW)},
        };
    }

    @Test(dataProvider="WeakHashMaps")
    public void one(WeakHashMap map, WeakHashMap.ValuePolicy policy) {
        for (Object[] c : keys()) {
            Object k = c[0];
            String v = (String)c[1];
            System.out.println("k: " + k);
            Assert.assertFalse(map.containsKey(k), "map.contains on empty map: " + k);
            Assert.assertNull(map.get(k), "map.get on empty map: " + k);
            var prev = map.put(k, v);
            Assert.assertNull(prev, "map.put on empty map did not return null: " + k);
            Assert.assertEquals(map.get(k), v, "map.get after put: " + k);

            forceGC();

            if (k.getClass().isValue() &&
                    policy.equals(WeakHashMap.ValuePolicy.SOFT)) {
                Assert.assertFalse(map.containsKey(k), "map.containsKey after GC: " + k);
                v = (String)map.get(k);
                Assert.assertNull(v, "map.get after GC should return null: " + k);
            } else {
                prev = map.remove(k);
                Assert.assertEquals(prev, v, "map.remove: " + k);
                Assert.assertNull(map.get(k), "map.get after remove: " + k);
            }
            Assert.assertTrue(map.isEmpty(), "m.isEmpty()");
        }
    }

    private void forceGC()  {
        Object marker = new Object();
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        SoftReference expected = new SoftReference(marker, queue);
        marker = null;
        Reference<?> actual = waitForReference(queue);

        System.out.println("expected soft.get: " + Objects.toString(expected.get()));

        if (actual != null) {
            System.out.println("found soft.get: " + Objects.toString(actual.get()));
        }
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
    }

    static value class StringValue {
        String value;

        StringValue(String value) {
            this.value = value;
        }
    }
}
