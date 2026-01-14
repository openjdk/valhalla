/*
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jimage;

import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public class ImageStringsReader implements ImageStrings {
    public static final int HASH_MULTIPLIER = 0x01000193;
    public static final int POSITIVE_MASK = 0x7FFFFFFF;

    private final BasicImageReader reader;

    ImageStringsReader(BasicImageReader reader) {
        this.reader = Objects.requireNonNull(reader);
    }

    @Override
    public String get(int offset) {
        return reader.getString(offset);
    }

    @Override
    public int match(int offset, String string, int stringOffset) {
        return reader.match(offset, string, stringOffset);
    }

    @Override
    public int add(final String string) {
        throw new InternalError("Can not add strings at runtime");
    }

    /**
     * {@return the lookup hash code for a string, starting at the given index,
     * with the default seed value}
     */
    public static int defaultHashCode(String s, int index) {
        return seededHashCode(s, index, HASH_MULTIPLIER);
    }

    /**
     * {@return the lookup hash code for a string, starting at the given index,
     * with a specified seed value}
     */
    public static int seededHashCode(String s, int index, int seed) {
        return unmaskedHashCode(s, index, seed) & POSITIVE_MASK;
    }

    /**
     * {@return the lookup hash code for the path in the given module with the
     * default seed value}
     *
     * This method behaves exactly like:
     * <pre>{@code
     *   defaultHashCode("/" + module + "/" + path, 0);
     * }</pre>
     */
    public static int defaultHashCode(String module, String name) {
        return seededHashCode(module, name, HASH_MULTIPLIER);
    }

    /**
     * {@return the lookup hash code for the path in the given module with a
     * specified seed value}
     *
     * This method behaves exactly like:
     * <pre>{@code
     *   seededHashCode("/" + module + "/" + path, 0, seed);
     * }</pre>
     */
    public static int seededHashCode(String module, String path, int seed) {
        seed = (seed * HASH_MULTIPLIER) ^ ('/');
        seed = unmaskedHashCode(module, 0, seed);
        seed = (seed * HASH_MULTIPLIER) ^ ('/');
        seed = unmaskedHashCode(path, 0, seed);
        return seed & POSITIVE_MASK;
    }

    /**
     * {@return the raw 32-bit hash code of the given string, suitable for using
     * as a seed to another unmaskedHashCode() call}
     *
     * This is useful when constructing compound hash values, since:
     * <pre>{@code
     *   int seed = unmaskedHashCode("hello ", startingSeed);
     *   int hash = unmaskedHashCode("world!", seed) & POSITIVE_MASK;
     * }</pre>
     *
     * Is the same as:
     * <pre>{@code
     *   int hash = seededHashCode("hello world!", 0, startingSeed);
     * }</pre>
     *
     * but returned values should not be used directly for location entry lookup.
     *
     * @param s the string to process
     * @param seed the hash seed (possibly returned from a previous call to
     *             {@code unmaskedHashCode()}).
     */
    public static int unmaskedHashCode(String s, int seed) {
        return unmaskedHashCode(s, 0, seed);
    }

    /**
     * This algorithm hashes each byte of the modified UTF-8 representation of
     * the string, starting at 'startIndex'.
     *
     * <p>It is equivalent to:
     * <pre>{@code
     *   for (byte b : mutf8FromString(s.substring(startIndex))) {
     *       seed = (seed * HASH_MULTIPLIER) ^ (b & 0xFF);
     *   }
     *   return seed;
     * }</pre>
     * but avoids any allocations.
     */
    private static int unmaskedHashCode(String s, int startIndex, int seed) {
        int slen = s.length();
        for (int i = startIndex; i < slen; i++) {
            // Upper 16-bits MUST be zero.
            int uch = s.charAt(i);
            // Copied and adapted from jdk/internal/util/ModifiedUtf.java
            if (uch != 0 && uch < 0x80) {
                // 7-bits (ASCII) not including nul.
                seed = (seed * HASH_MULTIPLIER) ^ (uch);
            } else if (uch >= 0x800) {
                // 12-16 bits, three bytes.
                seed = (seed * HASH_MULTIPLIER) ^ (0xE0 | uch >> 12 & 0x0F);
                seed = (seed * HASH_MULTIPLIER) ^ (0x80 | uch >> 6 & 0x3F);
                seed = (seed * HASH_MULTIPLIER) ^ (0x80 | uch & 0x3F);
            } else {
                // 7-11 bits, two bytes (or variant nul encoding).
                seed = (seed * HASH_MULTIPLIER) ^ (0xC0 | uch >> 6 & 0x1F);
                seed = (seed * HASH_MULTIPLIER) ^ (0x80 | uch & 0x3F);
            }
        }
        return seed;
    }

    static int charsFromMUTF8Length(byte[] bytes, int offset, int count) {
        int length = 0;

        for (int i = offset; i < offset + count; i++) {
            byte ch = bytes[i];

            if (ch == 0) {
                break;
            }

            if ((ch & 0xC0) != 0x80) {
                length++;
            }
        }

        return length;
    }

    static void charsFromMUTF8(char[] chars, byte[] bytes, int offset, int count) throws UTFDataFormatException {
        int j = 0;

        for (int i = offset; i < offset + count; i++) {
            byte ch = bytes[i];

            if (ch == 0) {
                break;
            }

            boolean is_unicode = (ch & 0x80) != 0;
            int uch = ch & 0x7F;

            if (is_unicode) {
                int mask = 0x40;

                while ((uch & mask) != 0) {
                    ch = bytes[++i];

                    if ((ch & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("bad continuation 0x" + Integer.toHexString(ch));
                    }

                    uch = ((uch & ~mask) << 6) | (ch & 0x3F);
                    mask <<= 6 - 1;
                }

                if ((uch & 0xFFFF) != uch) {
                    throw new UTFDataFormatException("character out of range \\u" + Integer.toHexString(uch));
                }
            }

            chars[j++] = (char)uch;
        }
    }

    public static String stringFromMUTF8(byte[] bytes, int offset, int count) {
        int length = charsFromMUTF8Length(bytes, offset, count);
        char[] chars = new char[length];

        try {
            charsFromMUTF8(chars, bytes, offset, count);
        } catch (UTFDataFormatException ex) {
            throw new InternalError("Attempt to convert non modified UTF-8 byte sequence", ex);
        }

        return new String(chars);
    }

    public static String stringFromMUTF8(byte[] bytes) {
        return stringFromMUTF8(bytes, 0, bytes.length);
    }

    /**
     * Calculates the number of characters in the String present at the
     * specified offset. As an optimization, the length returned will
     * be positive if the characters are all ASCII, and negative otherwise.
     */
    private static int charsFromByteBufferLength(ByteBuffer buffer, int offset) {
        int length = 0;

        int limit = buffer.limit();
        boolean asciiOnly = true;
        while (offset < limit) {
            byte ch = buffer.get(offset++);

            if (ch < 0) {
                asciiOnly = false;
            } else if (ch == 0) {
                return asciiOnly ? length : -length;
            }

            if ((ch & 0xC0) != 0x80) {
                length++;
            }
        }
        throw new InternalError("No terminating zero byte for modified UTF-8 byte sequence");
    }

    private static void charsFromByteBuffer(char[] chars, ByteBuffer buffer, int offset) {
        int j = 0;

        int limit = buffer.limit();
        while (offset < limit) {
            byte ch = buffer.get(offset++);

            if (ch == 0) {
                return;
            }

            boolean is_unicode = (ch & 0x80) != 0;
            int uch = ch & 0x7F;

            if (is_unicode) {
                int mask = 0x40;

                while ((uch & mask) != 0) {
                    ch = buffer.get(offset++);

                    if ((ch & 0xC0) != 0x80) {
                        throw new InternalError("Bad continuation in " +
                            "modified UTF-8 byte sequence: " + ch);
                    }

                    uch = ((uch & ~mask) << 6) | (ch & 0x3F);
                    mask <<= 6 - 1;
                }
            }

            if ((uch & 0xFFFF) != uch) {
                throw new InternalError("UTF-32 char in modified UTF-8 " +
                    "byte sequence: " + uch);
            }

            chars[j++] = (char)uch;
        }

        throw new InternalError("No terminating zero byte for modified UTF-8 byte sequence");
    }

    public static String stringFromByteBuffer(ByteBuffer buffer) {
        return stringFromByteBuffer(buffer, 0);
    }

    /* package-private */
    static String stringFromByteBuffer(ByteBuffer buffer, int offset) {
        int length = charsFromByteBufferLength(buffer, offset);
        if (length > 0) {
            byte[] asciiBytes = new byte[length];
            // Ideally we could use buffer.get(offset, asciiBytes, 0, length)
            // here, but that was introduced in JDK 13
            for (int i = 0; i < length; i++) {
                asciiBytes[i] = buffer.get(offset++);
            }
            return new String(asciiBytes, StandardCharsets.US_ASCII);
        }
        char[] chars = new char[-length];
        charsFromByteBuffer(chars, buffer, offset);
        return new String(chars);
    }

    /* package-private */
    static int stringFromByteBufferMatches(ByteBuffer buffer, int offset, String string, int stringOffset) {
        // ASCII fast-path
        int limit = buffer.limit();
        int current = offset;
        int slen = string.length();
        while (current < limit) {
            byte ch = buffer.get(current);
            if (ch <= 0) {
                if (ch == 0) {
                    // Match
                    return current - offset;
                }
                // non-ASCII byte, run slow-path from current offset
                break;
            }
            if (slen <= stringOffset || string.charAt(stringOffset) != (char)ch) {
                // No match
                return -1;
            }
            stringOffset++;
            current++;
        }
        // invariant: remainder of the string starting at current is non-ASCII,
        // so return value from charsFromByteBufferLength will be negative
        int length = -charsFromByteBufferLength(buffer, current);
        char[] chars = new char[length];
        charsFromByteBuffer(chars, buffer, current);
        for (int i = 0; i < length; i++) {
            if (string.charAt(stringOffset++) != chars[i]) {
                return -1;
            }
        }
        return current - offset + length;
    }

    static int mutf8FromStringLength(String s) {
        int length = 0;
        int slen = s.length();

        for (int i = 0; i < slen; i++) {
            char ch = s.charAt(i);
            int uch = ch & 0xFFFF;

            if ((uch & ~0x7F) != 0) {
                int mask = ~0x3F;
                int n = 0;

                do {
                    n++;
                    uch >>= 6;
                    mask >>= 1;
                } while ((uch & mask) != 0);

                length += n + 1;
            } else if (uch == 0) {
                length += 2;
            } else {
                length++;
            }
        }

        return length;
    }

    static void mutf8FromString(byte[] bytes, int offset, String s) {
        int j = offset;
        byte[] buffer = null;
        int slen = s.length();

        for (int i = 0; i < slen; i++) {
            char ch = s.charAt(i);
            int uch = ch & 0xFFFF;

            if ((uch & ~0x7F) != 0) {
                if (buffer == null) {
                    buffer = new byte[8];
                }
                int mask = ~0x3F;
                int n = 0;

                do {
                    buffer[n++] = (byte)(0x80 | (uch & 0x3F));
                    uch >>= 6;
                    mask >>= 1;
                } while ((uch & mask) != 0);

                buffer[n] = (byte)((mask << 1) | uch);

                do {
                    bytes[j++] = buffer[n--];
                } while (0 <= n);
            } else if (uch == 0) {
                bytes[j++] = (byte)0xC0;
                bytes[j++] = (byte)0x80;
            } else {
                bytes[j++] = (byte)uch;
            }
        }
    }

    public static byte[] mutf8FromString(String string) {
        int length = mutf8FromStringLength(string);
        byte[] bytes = new byte[length];
        mutf8FromString(bytes, 0, string);

        return bytes;
    }
}
