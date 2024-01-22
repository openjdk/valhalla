/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.ValhallaFeatures;

import org.junit.*;
import static org.junit.Assert.*;

/*
 * @test
 * @summary Test feature flags reflect command line flags
 * @modules java.base/jdk.internal.misc
 * @run junit/othervm -Dexpected=true ValhallaFeaturesTest
 * @run junit/othervm -XX:+EnableValhalla -Dexpected=true ValhallaFeaturesTest
 * @run junit/othervm -XX:-EnableValhalla -Dexpected=false ValhallaFeaturesTest
 */

public class ValhallaFeaturesTest {

    // Save the expected enable from the command line -Dexpected
    private static boolean expected = Boolean.getBoolean("expected");

    @Test
    public void checkEnable() {
        boolean enabled = ValhallaFeatures.isEnabled();
        System.out.println("EnableValhalla: " + enabled);
        assertEquals("EnableValhalla Flag", expected, enabled);
    }

    @Test
    public void checkEnsure() {
        if (expected) {
            // Throwing an exception is an error
            ValhallaFeatures.ensureValhallaEnabled();
        } else {
            assertThrows("EnableValhalla Flag", UnsupportedOperationException.class,
                    () -> ValhallaFeatures.ensureValhallaEnabled());
        }
    }
}
