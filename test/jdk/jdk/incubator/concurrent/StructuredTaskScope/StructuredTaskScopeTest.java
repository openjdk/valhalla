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

/*
 * @test id=platform
 * @bug 8284199
 * @summary Basic tests for StructuredTaskScope
 * @enablePreview
 * @modules jdk.incubator.concurrent
 * @run junit/othervm -DthreadFactory=platform StructuredTaskScopeTest
 */

/*
 * @test id=virtual
 * @enablePreview
 * @modules jdk.incubator.concurrent
 * @run junit/othervm -DthreadFactory=virtual StructuredTaskScopeTest
 */

import jdk.incubator.concurrent.StructuredTaskScope;
import jdk.incubator.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import jdk.incubator.concurrent.StructuredTaskScope.ShutdownOnFailure;
import jdk.incubator.concurrent.StructureViolationException;
import java.time.Duration;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class StructuredTaskScopeTest {
    private static ScheduledExecutorService scheduler;
    private static List<ThreadFactory> threadFactories;

    @BeforeAll
    static void setup() throws Exception {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // thread factories
        String value = System.getProperty("threadFactory");
        List<ThreadFactory> list = new ArrayList<>();
        if (value == null || value.equals("platform"))
            list.add(Thread.ofPlatform().factory());
        if (value == null || value.equals("virtual"))
            list.add(Thread.ofVirtual().factory());
        assertTrue(list.size() > 0, "No thread factories for tests");
        threadFactories = list;
    }

    @AfterAll
    static void shutdown() {
        scheduler.shutdown();
    }

    private static Stream<ThreadFactory> factories() {
        return threadFactories.stream();
    }

    /**
     * Test that each fork creates a thread.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFork1(ThreadFactory factory) throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (var scope = new StructuredTaskScope(null, factory)) {
            for (int i = 0; i < 100; i++) {
                scope.fork(() -> count.incrementAndGet());
            }
            scope.join();
        }
        assertTrue(count.get() == 100);
    }

    /**
     * Test that fork uses the specified thread factory.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFork2(ThreadFactory factory) throws Exception {
        AtomicInteger count = new AtomicInteger();
        ThreadFactory countingFactory = task -> {
            count.incrementAndGet();
            return factory.newThread(task);
        };
        try (var scope = new StructuredTaskScope(null, countingFactory)) {
            for (int i = 0; i < 100; i++) {
                scope.fork(() -> null);
            }
            scope.join();
        }
        assertTrue(count.get() == 100);
    }

    /**
     * Test fork is confined to threads in the scope "tree".
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testForkConfined(ThreadFactory factory) throws Exception {
        try (var scope1 = new StructuredTaskScope();
             var scope2 = new StructuredTaskScope()) {

            // thread in scope1 cannot fork thread in scope2
            Future<Void> future1 = scope1.fork(() -> {
                scope2.fork(() -> null).get();
                return null;
            });
            Throwable ex = assertThrows(ExecutionException.class, future1::get);
            assertTrue(ex.getCause() instanceof WrongThreadException);

            // thread in scope2 can fork thread in scope1
            Future<Void> future2 = scope2.fork(() -> {
                scope1.fork(() -> null).get();
                return null;
            });
            future2.get();
            assertNull(future2.resultNow());

            // random thread cannot fork
            try (var pool = Executors.newCachedThreadPool(factory)) {
                Future<Void> future = pool.submit(() -> {
                    scope1.fork(() -> null);
                    return null;
                });
                ex = assertThrows(ExecutionException.class, future::get);
                assertTrue(ex.getCause() instanceof WrongThreadException);
            }

            scope2.join();
            scope1.join();
        }
    }

    /**
     * Test fork when scope is shutdown.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testForkAfterShutdown(ThreadFactory factory) throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (var scope = new StructuredTaskScope(null, factory)) {
            scope.shutdown();
            Future<String> future = scope.fork(() -> {
                count.incrementAndGet();
                return "foo";
            });
            assertTrue(future.isCancelled());
            scope.join();
        }
        assertTrue(count.get() == 0);   // check that task did not run.
    }

    /**
     * Test fork when scope is closed.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testForkAfterClose(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            scope.join();
            scope.close();
            assertThrows(IllegalStateException.class, () -> scope.fork(() -> null));
        }
    }

    /**
     * Test fork when the thread factory rejects creating a thread.
     */
    @Test
    void testForkReject() throws Exception {
        ThreadFactory factory = task -> null;
        try (var scope = new StructuredTaskScope(null, factory)) {
            assertThrows(RejectedExecutionException.class, () -> scope.fork(() -> null));
            scope.join();
        }
    }

    /**
     * A StructuredTaskScope that collects all Future objects notified to the
     * handleComplete method.
     */
    private static class CollectAll<T> extends StructuredTaskScope<T> {
        private final List<Future<T>> futures = new CopyOnWriteArrayList<>();

        CollectAll(ThreadFactory factory) {
            super(null, factory);
        }

        @Override
        protected void handleComplete(Future<T> future) {
            assertTrue(future.isDone());
            futures.add(future);
        }

        Stream<Future<T>> futures() {
            return futures.stream();
        }

        Set<Future<T>> futuresAsSet() {
            return futures.stream().collect(Collectors.toSet());
        }
    }

    /**
     * Test that handleComplete method is invoked for tasks that complete normally
     * and abnormally.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testHandleComplete1(ThreadFactory factory) throws Exception {
        try (var scope = new CollectAll(factory)) {

            // completes normally
            Future<String> future1 = scope.fork(() -> "foo");

            // completes with exception
            Future<String> future2 = scope.fork(() -> { throw new FooException(); });

            // cancelled
            Future<String> future3 = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return null;
            });
            future3.cancel(true);

            scope.join();

            Set<Future<String>> futures = scope.futuresAsSet();
            assertEquals(Set.of(future1, future2, future3), futures);
        }
    }

    /**
     * Test that the handeComplete method is not invoked after the scope has been shutdown.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testHandleComplete2(ThreadFactory factory) throws Exception {
        try (var scope = new CollectAll(factory)) {
            var latch = new CountDownLatch(1);

            // start task that does not respond to interrupt
            Future<String> future1 = scope.fork(() -> {
                boolean done = false;
                while (!done) {
                    try {
                        latch.await();
                        done = true;
                    } catch (InterruptedException e) { }
                }
                return null;
            });

            // start a second task to shutdown the scope after a short delay
            Future<String> future2 = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(100));
                scope.shutdown();
                return null;
            });

            scope.join();

            // let task finish
            latch.countDown();

            // handleComplete should not have been called
            assertTrue(future1.isDone());
            assertTrue(scope.futures().count() == 0L);
        }
    }

    /**
     * Test join with no threads.
     */
    @Test
    void testJoinWithNoThreads() throws Exception {
        try (var scope = new StructuredTaskScope()) {
            scope.join();
        }
    }

    /**
     * Test join with threads running.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinWithThreads(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(50));
                return "foo";
            });
            scope.join();
            assertEquals("foo", future.resultNow());
        }
    }

    /**
     * Test join is owner confined.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinConfined(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope()) {
            // attempt to join on thread in scope
            Future<Void> future1 = scope.fork(() -> {
                scope.join();
                return null;
            });
            Throwable ex = assertThrows(ExecutionException.class, future1::get);
            assertTrue(ex.getCause() instanceof WrongThreadException);

            // random thread cannot join
            try (var pool = Executors.newCachedThreadPool(factory)) {
                Future<Void> future2 = pool.submit(() -> {
                    scope.join();
                    return null;
                });
                ex = assertThrows(ExecutionException.class, future2::get);
                assertTrue(ex.getCause() instanceof WrongThreadException);
            }

            scope.join();
        }
    }

    /**
     * Test join with interrupt status set.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptJoin1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            Future<String> future = scope.fork(() -> {
                latch.await();
                return "foo";
            });

            // join should throw
            Thread.currentThread().interrupt();
            try {
                scope.join();
                fail("join did not throw");
            } catch (InterruptedException expected) {
                assertFalse(Thread.interrupted());   // interrupt status should be clear
            } finally {
                // let task continue
                latch.countDown();
            }

            // join should complete
            scope.join();
            assertEquals("foo", future.resultNow());
        }
    }

    /**
     * Test interrupt of thread blocked in join.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptJoin2(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            Future<String> future = scope.fork(() -> {
                latch.await();
                return "foo";
            });

            // join should throw
            scheduleInterrupt(Thread.currentThread(), Duration.ofMillis(500));
            try {
                scope.join();
                fail("join did not throw");
            } catch (InterruptedException expected) {
                assertFalse(Thread.interrupted());   // interrupt status should be clear
            } finally {
                // let task continue
                latch.countDown();
            }

            // join should complete
            scope.join();
            assertEquals("foo", future.resultNow());
        }
    }

    /**
     * Test join when scope is already shutdown.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinWithShutdown1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return "foo";
            });
            scope.shutdown();  // interrupts task
            scope.join();

            // task should have completed abnormally
            assertTrue(future.isDone() && future.state() != Future.State.SUCCESS);
        }
    }

    /**
     * Test shutdown when owner is blocked in join.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinWithShutdown2(ThreadFactory factory) throws Exception {
        class MyScope<T> extends StructuredTaskScope<T> {
            MyScope(ThreadFactory factory) {
                super(null, factory);
            }
            @Override
            protected void handleComplete(Future<T> future) {
                shutdown();
            }
        }

        try (var scope = new MyScope(factory)) {
            Future<String> future1 = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return "foo";
            });
            Future<String> future2 = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(50));
                return null;
            });
            scope.join();

            // task1 should have completed abnormally
            assertTrue(future1.isDone() && future1.state() != Future.State.SUCCESS);

            // task2 should have completed normally
            assertTrue(future2.isDone() && future2.state() == Future.State.SUCCESS);
        }
    }

    /**
     * Test join after scope is shutdown.
     */
    @Test
    void testJoinAfterShutdown() throws Exception {
        try (var scope = new StructuredTaskScope()) {
            scope.shutdown();
            scope.join();
        }
    }

    /**
     * Test join after scope is closed.
     */
    @Test
    void testJoinAfterClose() throws Exception {
        try (var scope = new StructuredTaskScope()) {
            scope.join();
            scope.close();
            assertThrows(IllegalStateException.class, () -> scope.join());
            assertThrows(IllegalStateException.class, () -> scope.joinUntil(Instant.now()));
        }
    }

    /**
     * Test joinUntil, threads finish before deadline expires.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinUntil1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                try {
                    Thread.sleep(Duration.ofSeconds(2));
                } catch (InterruptedException e) { }
                return null;
            });

            long startMillis = millisTime();
            scope.joinUntil(Instant.now().plusSeconds(30));
            assertTrue(future.isDone());
            assertNull(future.resultNow());
            expectDuration(startMillis, /*min*/1900, /*max*/20_000);
        }
    }

    /**
     * Test joinUntil, deadline expires before threads finish.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinUntil2(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                try {
                    Thread.sleep(Duration.ofSeconds(30));
                } catch (InterruptedException e) { }
                return null;
            });

            long startMillis = millisTime();
            try {
                scope.joinUntil(Instant.now().plusSeconds(2));
            } catch (TimeoutException e) {
                expectDuration(startMillis, /*min*/1900, /*max*/20_000);
            }
            assertFalse(future.isDone());
        }
    }

    /**
     * Test joinUntil many times.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinUntil3(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                try {
                    Thread.sleep(Duration.ofSeconds(30));
                } catch (InterruptedException e) { }
                return null;
            });

            try {
                for (int i = 0; i < 3; i++) {
                    try {
                        scope.joinUntil(Instant.now().plusMillis(50));
                        fail("joinUntil did not throw");
                    } catch (TimeoutException expected) {
                        assertFalse(future.isDone());
                    }
                }
            } finally {
                future.cancel(true);
            }
        }
    }

    /**
     * Test joinUntil with a deadline that has already expired.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testJoinUntil4(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                try {
                    Thread.sleep(Duration.ofSeconds(30));
                } catch (InterruptedException e) { }
                return null;
            });

            try {

                // now
                try {
                    scope.joinUntil(Instant.now());
                    fail("joinUntil did not throw");
                } catch (TimeoutException expected) {
                    assertFalse(future.isDone());
                }

                // in the past
                try {
                    scope.joinUntil(Instant.now().minusSeconds(1));
                    fail("joinUntil did not throw");
                } catch (TimeoutException expected) {
                    assertFalse(future.isDone());
                }

            } finally {
                future.cancel(true);
            }
        }
    }

    /**
     * Test joinUntil with interrupt status set.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptJoinUntil1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            Future<String> future = scope.fork(() -> {
                latch.await();
                return "foo";
            });

            // joinUntil should throw
            Thread.currentThread().interrupt();
            try {
                scope.joinUntil(Instant.now().plusSeconds(30));
                fail("joinUntil did not throw");
            } catch (InterruptedException expected) {
                assertFalse(Thread.interrupted());   // interrupt status should be clear
            } finally {
                // let task continue
                latch.countDown();
            }

            // join should complete
            scope.join();
            assertEquals("foo", future.resultNow());
        }
    }

    /**
     * Test interrupt of thread blocked in joinUntil.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptJoinUntil2(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            Future<String> future = scope.fork(() -> {
                latch.await();
                return "foo";
            });

            // joinUntil should throw
            scheduleInterrupt(Thread.currentThread(), Duration.ofMillis(500));
            try {
                scope.joinUntil(Instant.now().plusSeconds(10));
                fail("joinUntil did not throw");
            } catch (InterruptedException expected) {
                assertFalse(Thread.interrupted());   // interrupt status should be clear
            } finally {
                // let task continue
                latch.countDown();
            }

            // join should complete
            scope.join();
            assertEquals("foo", future.resultNow());
        }
    }

    /**
     * Test shutdown after scope is closed.
     */
    @Test
    void testShutdownAfterClose() throws Exception {
        try (var scope = new StructuredTaskScope()) {
            scope.join();
            scope.close();
            assertThrows(IllegalStateException.class, () -> scope.shutdown());
        }
    }

    /**
     * Test shutdown is confined to threads in the scope "tree".
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testShutdownConfined(ThreadFactory factory) throws Exception {
        try (var scope1 = new StructuredTaskScope();
             var scope2 = new StructuredTaskScope()) {

            // random thread cannot shutdown
            try (var pool = Executors.newCachedThreadPool(factory)) {
                Future<Void> future = pool.submit(() -> {
                    scope1.shutdown();
                    return null;
                });
                Throwable ex = assertThrows(ExecutionException.class, future::get);
                assertTrue(ex.getCause() instanceof WrongThreadException);
            }

            // thread in scope1 cannot shutdown scope2
            Future<Void> future1 = scope1.fork(() -> {
                scope2.shutdown();
                return null;
            });
            Throwable ex = assertThrows(ExecutionException.class, future1::get);
            assertTrue(ex.getCause() instanceof WrongThreadException);

            // thread in scope2 can shutdown scope1
            Future<Void> future2 = scope2.fork(() -> {
                scope1.shutdown();
                return null;
            });
            future2.get();
            assertNull(future2.resultNow());

            scope2.join();
            scope1.join();
        }
    }

    /**
     * Test close without join, no threads forked.
     */
    @Test
    void testCloseWithoutJoin1() {
        try (var scope = new StructuredTaskScope()) {
            // do nothing
        }
    }

    /**
     * Test close without join, threads forked.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testCloseWithoutJoin2(ThreadFactory factory) {
        try (var scope = new StructuredTaskScope(null, factory)) {
            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return null;
            });
            assertThrows(IllegalStateException.class, scope::close);
            assertTrue(future.isDone() && future.exceptionNow() != null);
        }
    }

    /**
     * Test close with threads forked after join.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testCloseWithoutJoin3(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            scope.fork(() -> "foo");
            scope.join();

            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return null;
            });
            assertThrows(IllegalStateException.class, scope::close);
            assertTrue(future.isDone() && future.exceptionNow() != null);
        }
    }

    /**
     * Test close is owner confined.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testCloseConfined(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope()) {
            // attempt to close on thread in scope
            Future<Void> future1 = scope.fork(() -> {
                scope.close();
                return null;
            });
            Throwable ex = assertThrows(ExecutionException.class, future1::get);
            assertTrue(ex.getCause() instanceof WrongThreadException);

            // random thread cannot close scope
            try (var pool = Executors.newCachedThreadPool(factory)) {
                Future<Void> future2 = pool.submit(() -> {
                    scope.close();
                    return null;
                });
                ex = assertThrows(ExecutionException.class, future2::get);
                assertTrue(ex.getCause() instanceof WrongThreadException);
            }

            scope.join();
        }
    }

    /**
     * Test close with interrupt status set.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptClose1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            // start task that does not respond to interrupt
            scope.fork(() -> {
                boolean done = false;
                while (!done) {
                    try {
                        latch.await();
                        done = true;
                    } catch (InterruptedException e) { }
                }
                return null;
            });

            scope.shutdown();
            scope.join();

            // release task after a delay
            scheduler.schedule(latch::countDown, 100, TimeUnit.MILLISECONDS);

            // invoke close with interrupt status set
            Thread.currentThread().interrupt();
            try {
                scope.close();
            } finally {
                assertTrue(Thread.interrupted());   // clear interrupt status
            }
        }
    }

    /**
     * Test interrupting thread waiting in close.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testInterruptClose2(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {
            var latch = new CountDownLatch(1);

            // start task that does not respond to interrupt
            scope.fork(() -> {
                boolean done = false;
                while (!done) {
                    try {
                        latch.await();
                        done = true;
                    } catch (InterruptedException e) { }
                }
                return null;
            });

            scope.shutdown();
            scope.join();

            // release task after a delay
            scheduleInterrupt(Thread.currentThread(), Duration.ofMillis(500));
            scheduler.schedule(latch::countDown, 3, TimeUnit.SECONDS);
            try {
                scope.close();
            } finally {
                assertTrue(Thread.interrupted());   // clear interrupt status
            }
        }
    }

    /**
     * Test that closing an enclosing scope closes the thread flock of a
     * nested scope.
     */
    @Test
    void testStructureViolation1() throws Exception {
        try (var scope1 = new StructuredTaskScope()) {
            try (var scope2 = new StructuredTaskScope()) {

                // join + close enclosing scope
                scope1.join();
                try {
                    scope1.close();
                    fail("close did not throw");
                } catch (StructureViolationException expected) { }

                // underlying flock should be closed, fork should return a cancelled task
                AtomicBoolean ran = new AtomicBoolean();
                Future<String> future = scope2.fork(() -> {
                    ran.set(true);
                    return null;
                });
                assertTrue(future.isCancelled());
                scope2.join();
                assertFalse(ran.get());
            }
        }
    }

    /**
     * Test Future::get, task completes normally.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFuture1(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {

            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(20));
                return "foo";
            });

            assertEquals("foo", future.get());
            assertTrue(future.state() == Future.State.SUCCESS);
            assertEquals("foo", future.resultNow());

            scope.join();
        }
    }

    /**
     * Test Future::get, task completes with exception.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFuture2(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {

            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(20));
                throw new FooException();
            });

            Throwable ex = assertThrows(ExecutionException.class, future::get);
            assertTrue(ex.getCause() instanceof FooException);
            assertTrue(future.state() == Future.State.FAILED);
            assertTrue(future.exceptionNow() instanceof FooException);

            scope.join();
        }
    }

    /**
     * Test Future::get, task is cancelled.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFuture3(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {

            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return null;
            });

            // timed-get, should timeout
            try {
                future.get(20, TimeUnit.MILLISECONDS);
                fail("Future.get did not throw");
            } catch (TimeoutException expected) { }

            future.cancel(true);
            assertThrows(CancellationException.class, future::get);
            assertTrue(future.state() == Future.State.CANCELLED);

            scope.join();
        }
    }

    /**
     * Test scope shutdown with a thread blocked in Future::get.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFutureWithShutdown(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope(null, factory)) {

            // long running task
            Future<String> future = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return null;
            });

            // start a thread to wait in Future::get
            AtomicBoolean waitDone = new AtomicBoolean();
            Thread waiter = Thread.startVirtualThread(() -> {
                try {
                    future.get();
                } catch (ExecutionException | CancellationException e) {
                    waitDone.set(true);
                } catch (InterruptedException e) {
                    System.out.println("waiter thread interrupted!");
                }
            });

            // shutdown scope
            scope.shutdown();

            // Future should be done and thread should be awakened
            assertTrue(future.isDone());
            waiter.join();
            assertTrue(waitDone.get());

            scope.join();
        }
    }

    /**
     * Test Future::cancel throws if invoked by a thread that is not in the tree.
     */
    @ParameterizedTest
    @MethodSource("factories")
    void testFutureCancelConfined(ThreadFactory factory) throws Exception {
        try (var scope = new StructuredTaskScope()) {
            Future<String> future1 = scope.fork(() -> {
                Thread.sleep(Duration.ofDays(1));
                return "foo";
            });

            // random thread cannot cancel
            try (var pool = Executors.newCachedThreadPool(factory)) {
                Future<Void> future2 = pool.submit(() -> {
                    future1.cancel(true);
                    return null;
                });
                Throwable ex = assertThrows(ExecutionException.class, future2::get);
                assertTrue(ex.getCause() instanceof WrongThreadException);
            } finally {
                future1.cancel(true);
            }
            scope.join();
        }
    }

    /**
     * Test StructuredTaskScope::toString includes the scope name.
     */
    @Test
    void testToString() throws Exception {
        ThreadFactory factory = Thread.ofVirtual().factory();
        try (var scope = new StructuredTaskScope("xxx", factory)) {
            // open
            assertTrue(scope.toString().contains("xxx"));

            // shutdown
            scope.shutdown();
            assertTrue(scope.toString().contains("xxx"));

            // closed
            scope.join();
            scope.close();
            assertTrue(scope.toString().contains("xxx"));
        }
    }

    /**
     * Test for NullPointerException.
     */
    @Test
    void testNulls() throws Exception {
        assertThrows(NullPointerException.class, () -> new StructuredTaskScope("", null));
        try (var scope = new StructuredTaskScope()) {
            assertThrows(NullPointerException.class, () -> scope.fork(null));
            assertThrows(NullPointerException.class, () -> scope.joinUntil(null));
        }

        assertThrows(NullPointerException.class, () -> new ShutdownOnSuccess("", null));
        try (var scope = new ShutdownOnSuccess<Object>()) {
            assertThrows(NullPointerException.class, () -> scope.fork(null));
            assertThrows(NullPointerException.class, () -> scope.joinUntil(null));
            assertThrows(NullPointerException.class, () -> scope.result(null));
        }

        assertThrows(NullPointerException.class, () -> new ShutdownOnFailure("", null));
        try (var scope = new ShutdownOnFailure()) {
            assertThrows(NullPointerException.class, () -> scope.fork(null));
            assertThrows(NullPointerException.class, () -> scope.joinUntil(null));
            assertThrows(NullPointerException.class, () -> scope.throwIfFailed(null));
        }
    }

    /**
     * Test ShutdownOnSuccess with no completed tasks.
     */
    @Test
    void testShutdownOnSuccess1() throws Exception {
        try (var scope = new ShutdownOnSuccess<String>()) {
            assertThrows(IllegalStateException.class, () -> scope.result());
            assertThrows(IllegalStateException.class, () -> scope.result(e -> null));
        }
    }

    /**
     * Test ShutdownOnSuccess with tasks that completed normally.
     */
    @Test
    void testShutdownOnSuccess2() throws Exception {
        try (var scope = new ShutdownOnSuccess<String>()) {

            // two tasks complete normally
            scope.fork(() -> "foo");
            scope.join();  // ensures foo completes first
            scope.fork(() -> "bar");
            scope.join();

            assertEquals("foo", scope.result());
            assertEquals("foo", scope.result(e -> null));
        }
    }

    /**
     * Test ShutdownOnSuccess with tasks that completed normally and abnormally.
     */
    @Test
    void testShutdownOnSuccess3() throws Exception {
        try (var scope = new ShutdownOnSuccess<String>()) {

            // one task completes normally, the other with an exception
            scope.fork(() -> "foo");
            scope.fork(() -> { throw new ArithmeticException(); });
            scope.join();

            assertEquals("foo", scope.result());
            assertEquals("foo", scope.result(e -> null));
        }
    }

    /**
     * Test ShutdownOnSuccess with a task that completed with an exception.
     */
    @Test
    void testShutdownOnSuccess4() throws Exception {
        try (var scope = new ShutdownOnSuccess<String>()) {

            // tasks completes with exception
            scope.fork(() -> { throw new ArithmeticException(); });
            scope.join();

            Throwable ex = assertThrows(ExecutionException.class, () -> scope.result());
            assertTrue(ex.getCause() instanceof  ArithmeticException);

            ex = assertThrows(FooException.class, () -> scope.result(e -> new FooException(e)));
            assertTrue(ex.getCause() instanceof  ArithmeticException);
        }
    }

    /**
     * Test ShutdownOnSuccess with a cancelled task.
     */
    @Test
    void testShutdownOnSuccess5() throws Exception {
        try (var scope = new ShutdownOnSuccess<String>()) {

            // cancelled task
            var future = scope.fork(() -> {
                Thread.sleep(60_000);
                return null;
            });
            future.cancel(false);

            scope.join();

            assertThrows(CancellationException.class, () -> scope.result());
            Throwable ex = assertThrows(FooException.class,
                                        () -> scope.result(e -> new FooException(e)));
            assertTrue(ex.getCause() instanceof CancellationException);
        }
    }

    /**
     * Test ShutdownOnFailure with no completed tasks.
     */
    @Test
    void testShutdownOnFailure1() throws Throwable {
        try (var scope = new ShutdownOnFailure()) {
            assertTrue(scope.exception().isEmpty());
            scope.throwIfFailed();
            scope.throwIfFailed(e -> new FooException(e));
        }
    }

    /**
     * Test ShutdownOnFailure with tasks that completed normally.
     */
    @Test
    void testShutdownOnFailure2() throws Throwable {
        try (var scope = new ShutdownOnFailure()) {
            scope.fork(() -> "foo");
            scope.fork(() -> "bar");
            scope.join();

            // no exception
            assertTrue(scope.exception().isEmpty());
            scope.throwIfFailed();
            scope.throwIfFailed(e -> new FooException(e));
        }
    }

    /**
     * Test ShutdownOnFailure with tasks that completed normally and abnormally.
     */
    @Test
    void testShutdownOnFailure3() throws Throwable {
        try (var scope = new ShutdownOnFailure()) {

            // one task completes normally, the other with an exception
            scope.fork(() -> "foo");
            scope.fork(() -> { throw new ArithmeticException(); });
            scope.join();

            Throwable ex = scope.exception().orElse(null);
            assertTrue(ex instanceof ArithmeticException);

            ex = assertThrows(ExecutionException.class, () -> scope.throwIfFailed());
            assertTrue(ex.getCause() instanceof ArithmeticException);

            ex = assertThrows(FooException.class,
                              () -> scope.throwIfFailed(e -> new FooException(e)));
            assertTrue(ex.getCause() instanceof ArithmeticException);
        }
    }

    /**
     * Test ShutdownOnFailure with a cancelled task.
     */
    @Test
    void testShutdownOnFailure4() throws Throwable {
        try (var scope = new ShutdownOnFailure()) {

            var future = scope.fork(() -> {
                Thread.sleep(60_000);
                return null;
            });
            future.cancel(false);

            scope.join();

            Throwable ex = scope.exception().orElse(null);
            assertTrue(ex instanceof CancellationException);

            assertThrows(CancellationException.class, () -> scope.throwIfFailed());

            ex = assertThrows(FooException.class,
                              () -> scope.throwIfFailed(e -> new FooException(e)));
            assertTrue(ex.getCause() instanceof CancellationException);
        }
    }

    /**
     * A runtime exception for tests.
     */
    private static class FooException extends RuntimeException {
        FooException() { }
        FooException(Throwable cause) { super(cause); }
    }

    /**
     * Schedules a thread to be interrupted after the given delay.
     */
    private void scheduleInterrupt(Thread thread, Duration delay) {
        long millis = delay.toMillis();
        scheduler.schedule(thread::interrupt, millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the current time in milliseconds.
     */
    private static long millisTime() {
        long now = System.nanoTime();
        return TimeUnit.MILLISECONDS.convert(now, TimeUnit.NANOSECONDS);
    }

    /**
     * Check the duration of a task
     * @param start start time, in milliseconds
     * @param min minimum expected duration, in milliseconds
     * @param max maximum expected duration, in milliseconds
     * @return the duration (now - start), in milliseconds
     */
    private static long expectDuration(long start, long min, long max) {
        long duration = millisTime() - start;
        assertTrue(duration >= min,
                "Duration " + duration + "ms, expected >= " + min + "ms");
        assertTrue(duration <= max,
                "Duration " + duration + "ms, expected <= " + max + "ms");
        return duration;
    }
}
