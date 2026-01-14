/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.jimage.ImageStringsReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static jdk.internal.jimage.ImageStringsReader.HASH_MULTIPLIER;
import static jdk.internal.jimage.ImageStringsReader.POSITIVE_MASK;

/*
 * @test
 * @summary Tests for public methods of ImageStringsReader.
 * @modules java.base/jdk.internal.jimage
 * @run junit/othervm -esa ImageStringsReaderTest
 */
public class ImageStringsReaderTest {
    private static final String PREFIX = "THIS SHOULD BE IGNORED...";
    private static final int TEST_SEED = 0x1234;

    // Tests should work for all possible string/character sequences.
    static String[] strings() {
        return new String[]{
                "",
                "\0",
                "/modules/java.base/java/lang/Object.class",
                "Lorem ipsum dolor sit amet.",
                "Hello \0 World",
                "ЈӐѶӐ",    // Cyrillic: 1 char, 2 byte UTF-8
                "ⒿⒹⓀ",   // Symbols:  1 char, 3 byte UTF-8
                "\uD83D\uDE3B",  // Surrogate pair: https://www.compart.com/en/unicode/U+1F63B
                "\uDE3B\uD83D",  // Badly ordered surrogate pair.
        };
    }

    // Test default hashing (including substring hashing).
    @ParameterizedTest
    @MethodSource("strings")
    public void defaultHashCodeEquivalence(String s) {
        int expected = canonicalHashCode(s);
        Assertions.assertEquals(
                expected,
                ImageStringsReader.defaultHashCode(s, 0),
                "unexpected default hash code for: " + s);
        Assertions.assertEquals(
                expected,
                ImageStringsReader.defaultHashCode(PREFIX + s, PREFIX.length()),
                "unexpected default hash code for substring: " + s);
    }

    // Test seeded hashing (including substring hashing).
    @ParameterizedTest
    @MethodSource("strings")
    public void seededHashCodeEquivalence(String s) {
        int expected = canonicalHashCode(s, TEST_SEED);
        Assertions.assertEquals(
                expected,
                ImageStringsReader.seededHashCode(s, 0, TEST_SEED),
                "unexpected seeded hash code for: " + s);
        Assertions.assertEquals(
                expected,
                ImageStringsReader.seededHashCode(PREFIX + s, PREFIX.length(), TEST_SEED),
                "unexpected seeded hash code for substring: " + s);
    }

    // Tests composability of unmasked hash codes.
    @Test
    public void unmaskedHashCodes() {
        int seed = TEST_SEED;
        seed = ImageStringsReader.unmaskedHashCode("Hello", seed);
        seed = ImageStringsReader.unmaskedHashCode(" ", seed);
        seed = ImageStringsReader.unmaskedHashCode("World", seed);
        seed = ImageStringsReader.unmaskedHashCode("!", seed);

        Assertions.assertEquals(
                ImageStringsReader.unmaskedHashCode("Hello World!", TEST_SEED),
                seed,
                "unexpected unmasked hash code");
        Assertions.assertEquals(
                ImageStringsReader.seededHashCode("Hello World!", 0, TEST_SEED),
                seed & POSITIVE_MASK,
                "unexpected hash code");
    }

    // Tests that empty strings don't affect composability.
    @Test
    public void unmaskedHashCodes_empty() {
        Assertions.assertEquals(
                TEST_SEED,
                ImageStringsReader.unmaskedHashCode("", TEST_SEED),
                "hashing empty strings should be idempotent");
    }

    // Test the modified UTF-8 algorithm itself.
    @ParameterizedTest
    @MethodSource("strings")
    public void modifiedUtf8Encoding(String s) {
        Assertions.assertArrayEquals(
                toModifiedUtf8(s),
                ImageStringsReader.mutf8FromString(s),
                "unexpected modified UTF-8 for: " + s);
    }

    // A sanity check to ensure hash codes are not somehow trivially broken.
    @ParameterizedTest
    @MethodSource("strings")
    public void sanityCheck(String s) {
        // Self-test on the exemplar algorithm.
        Assertions.assertNotEquals(
                canonicalHashCode(s),
                canonicalHashCode(s + "x", 0),
                "unexpected hash code equality for: " + s + "[x]");
        Assertions.assertNotEquals(
                ImageStringsReader.defaultHashCode(s, 0),
                ImageStringsReader.defaultHashCode(s + "x", 0),
                "unexpected hash code equality for: " + s + "[x]");
    }

    private static int canonicalHashCode(String s) {
        return canonicalHashCode(s, HASH_MULTIPLIER);
    }

    // The simplest algorithm demonstrating the underlying hashing algorithm.
    private static int canonicalHashCode(String s, int seed) {
        for (byte b : toModifiedUtf8(s)) {
            seed = (seed * HASH_MULTIPLIER) ^ (b & 0xFF);
        }
        return seed & POSITIVE_MASK;
    }

    private static byte[] toModifiedUtf8(String s) {
        var b = new ByteArrayOutputStream();
        var o = new DataOutputStream(b);
        try {
            // Format is defined as unsigned short (2 bytes) for length
            // followed by modified UTF_8 bytes. No terminators.
            o.writeUTF(s);
            o.close();
            byte[] bytes = b.toByteArray();
            return Arrays.copyOfRange(bytes, 2, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
