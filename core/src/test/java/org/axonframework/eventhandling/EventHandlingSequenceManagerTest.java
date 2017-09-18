/*
 * Copyright (c) 2011. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling;

import org.axonframework.domain.Event;
import org.axonframework.domain.StubDomainEvent;
import org.junit.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class EventHandlingSequenceManagerTest {

    private EventListener eventListener;
    private ScheduledThreadPoolExecutor executorService;
    private AsynchronousEventHandlerWrapper testSubject;

    private CountDownLatch countdownLatch;
    private Field transactionsField;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() {
        eventListener = new StubEventListener();
        executorService = new ScheduledThreadPoolExecutor(2);
        executorService.setMaximumPoolSize(2);
        executorService.setKeepAliveTime(1, TimeUnit.SECONDS);
        testSubject = new AsynchronousEventHandlerWrapper(eventListener, new FullRandomPolicy(), executorService);
    }

    @After
    public void After() throws InterruptedException {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testSequenceManager_SchedulersDiscardedAfterShutdown()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        countdownLatch = new CountDownLatch(1000);
        for (int t = 0; t < 1000; t++) {
            testSubject.handle(new StubDomainEvent());
        }
        assertTrue("Processing took too long.", countdownLatch.await(10, TimeUnit.SECONDS));

        executorService.shutdown();
        assertTrue("Shutdown took too long.", executorService.awaitTermination(5, TimeUnit.SECONDS));

        transactionsField = testSubject.getClass().getSuperclass().getDeclaredField("transactions");
        transactionsField.setAccessible(true);
        Map transactions = (Map) transactionsField.get(testSubject);
        assertTrue("Expected transaction schedulers to be cleaned up", transactions.isEmpty());
    }

    @Test
    public void testDispatchFullConcurrentEvents() throws InterruptedException {
        FullConcurrentEventListener eventListener = new FullConcurrentEventListener();
        testSubject = new AsynchronousEventHandlerWrapper(eventListener, new FullConcurrencyPolicy(), executorService);
        StubDomainEvent event = new StubDomainEvent();
        for (int t = 0; t < 1000; t++) {
            testSubject.handle(event);
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(0, eventListener.transactionCounter.get());
        // it's almost impossible that 2 threads will cause more than 250 transactions.
        assertEquals(1000, eventListener.eventCounter.get());
        assertTrue("Event processing doesn't seem to be optimized", eventListener.totalTransactionCounter.get() < 500);
    }

    /**
     * Very useless implementation of SequencingPolicy that is the fastest way to display a memory leak
     */
    private class FullRandomPolicy implements SequencingPolicy<Event> {

        @Override
        public Object getSequenceIdentifierFor(Event event) {
            return event.getEventIdentifier();
        }
    }

    private class StubEventListener implements EventListener {

        @Override
        public void handle(Event event) {
            countdownLatch.countDown();
        }
    }

    private class FullConcurrentEventListener implements EventListener, TransactionManager {

        private AtomicInteger eventCounter = new AtomicInteger(0);
        private AtomicInteger transactionCounter = new AtomicInteger(0);
        private AtomicInteger totalTransactionCounter = new AtomicInteger(0);

        @Override
        public void handle(Event event) {
            eventCounter.incrementAndGet();
        }

        @Override
        public void beforeTransaction(TransactionStatus transactionStatus) {
            transactionCounter.incrementAndGet();
            totalTransactionCounter.incrementAndGet();
        }

        @Override
        public void afterTransaction(TransactionStatus transactionStatus) {
            transactionCounter.decrementAndGet();
        }
    }
}
