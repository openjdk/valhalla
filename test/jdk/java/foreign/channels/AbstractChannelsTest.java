/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jdk.test.lib.RandomFactory;
import org.testng.annotations.*;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static org.testng.Assert.*;

/**
 * Not a test, but infra for channel tests.
 */
public class AbstractChannelsTest {

    static final Class<IOException> IOE = IOException.class;
    static final Class<ExecutionException> EE = ExecutionException.class;
    static final Class<IllegalStateException> ISE = IllegalStateException.class;

    @FunctionalInterface
    interface ThrowingConsumer<T, X extends Throwable> {
        void accept(T action) throws X;
    }

    static long remaining(ByteBuffer[] buffers) {
        return Arrays.stream(buffers).mapToLong(ByteBuffer::remaining).sum();
    }

    static ByteBuffer[] flip(ByteBuffer[] buffers) {
        Arrays.stream(buffers).forEach(ByteBuffer::flip);
        return buffers;
    }

    static ByteBuffer[] clear(ByteBuffer[] buffers) {
        Arrays.stream(buffers).forEach(ByteBuffer::clear);
        return buffers;
    }

    static final Random RANDOM = RandomFactory.getRandom();

    static ByteBuffer segmentBufferOfSize(Arena session, int size) {
        var segment = session.allocate(size, 1);
        for (int i = 0; i < size; i++) {
            segment.set(JAVA_BYTE, i, ((byte)RANDOM.nextInt()));
        }
        return segment.asByteBuffer();
    }

    static ByteBuffer[] segmentBuffersOfSize(int len, Arena session, int size) {
        ByteBuffer[] bufs = new ByteBuffer[len];
        for (int i = 0; i < len; i++)
            bufs[i] = segmentBufferOfSize(session, size);
        return bufs;
    }

    /**
     * Returns an array of mixed source byte buffers; both heap and direct,
     * where heap can be from the global session or session-less, and direct are
     * associated with the given session.
     */
    static ByteBuffer[] mixedBuffersOfSize(int len, Arena session, int size) {
        ByteBuffer[] bufs;
        boolean atLeastOneSessionBuffer = false;
        do {
            bufs = new ByteBuffer[len];
            for (int i = 0; i < len; i++) {
                bufs[i] = switch (RANDOM.nextInt(3)) {
                    case 0 -> { byte[] b = new byte[size];
                                RANDOM.nextBytes(b);
                                yield ByteBuffer.wrap(b); }
                    case 1 -> { byte[] b = new byte[size];
                                RANDOM.nextBytes(b);
                                yield MemorySegment.ofArray(b).asByteBuffer(); }
                    case 2 -> { atLeastOneSessionBuffer = true;
                                yield segmentBufferOfSize(session, size); }
                    default -> throw new AssertionError("cannot happen");
                };
            }
        } while (!atLeastOneSessionBuffer);
        return bufs;
    }

    static void assertMessage(Exception ex, String msg) {
        assertTrue(ex.getMessage().contains(msg), "Expected [%s], in: [%s]".formatted(msg, ex.getMessage()));
    }

    static void assertCauses(Throwable ex, Class<? extends Exception>... exceptions) {
        for (var expectedClass : exceptions) {
            ex = ex.getCause();
            assertTrue(expectedClass.isInstance(ex), "Expected %s, got: %s".formatted(expectedClass, ex));
        }
    }

    @DataProvider(name = "confinedArenas")
    public static Object[][] confinedArenas() {
        return new Object[][] {
                { ArenaSupplier.NEW_CONFINED          },
        };
    }

    @DataProvider(name = "sharedArenas")
    public static Object[][] sharedArenas() {
        return new Object[][] {
                { ArenaSupplier.NEW_SHARED          },
        };
    }

    @DataProvider(name = "closeableArenas")
    public static Object[][] closeableArenas() {
        return Stream.of(sharedArenas(), confinedArenas())
                .flatMap(Arrays::stream)
                .toArray(Object[][]::new);
    }

    @DataProvider(name = "sharedArenasAndTimeouts")
    public static Object[][] sharedArenasAndTimeouts() {
        return new Object[][] {
                { ArenaSupplier.NEW_SHARED          ,  0 },
                { ArenaSupplier.NEW_SHARED          , 30 },
        };
    }

    static class ArenaSupplier implements Supplier<Arena> {

        static final Supplier<Arena> NEW_CONFINED =
                new ArenaSupplier(Arena::ofConfined, "confined arena");
        static final Supplier<Arena> NEW_SHARED =
                new ArenaSupplier(Arena::ofShared, "shared arena");

        private final Supplier<Arena> supplier;
        private final String str;
        private ArenaSupplier(Supplier<Arena> supplier, String str) {
            this.supplier = supplier;
            this.str = str;
        }
        @Override public String toString() { return str; }
        @Override public Arena get() { return supplier.get(); }
    }
}

