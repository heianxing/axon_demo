/*
 * Copyright (c) 2010. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventsourcing;

import net.sf.jsr107cache.Cache;
import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.UnitOfWorkListenerAdapter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Snapshotter trigger mechanism that counts the number of events to decide when to create a snapshot. This
 * implementation acts as a proxy towards the actual event store, and keeps track of the number of "unsnapshotted"
 * events for each aggregate. This means repositories should be configured to use an instance of this class instead of
 * the actual event store.
 * <p/>
 *
 * @author Allard Buijze
 * @since 0.6
 */
public class EventCountSnapshotterTrigger implements SnapshotterTrigger {

    private static final int DEFAULT_TRIGGER_VALUE = 50;

    private Snapshotter snapshotter;
    private final ConcurrentMap<AggregateIdentifier, AtomicInteger> counters = new ConcurrentHashMap<AggregateIdentifier, AtomicInteger>();
    private volatile boolean clearCountersAfterAppend = true;
    private int trigger = DEFAULT_TRIGGER_VALUE;

    @Override
    public DomainEventStream decorateForRead(String aggregateType, AggregateIdentifier aggregateIdentifier,
                                             DomainEventStream eventStream) {
        AtomicInteger counter = new AtomicInteger(0);
        counters.put(aggregateIdentifier, counter);
        return new CountingEventStream(eventStream, counter);
    }

    @Override
    public DomainEventStream decorateForAppend(String aggregateType, EventSourcedAggregateRoot aggregate,
                                               DomainEventStream eventStream) {
        AggregateIdentifier aggregateIdentifier = aggregate.getIdentifier();
        counters.putIfAbsent(aggregateIdentifier, new AtomicInteger(0));
        AtomicInteger counter = counters.get(aggregateIdentifier);
        return new TriggeringEventStream(aggregateType, aggregateIdentifier, eventStream, counter);
    }

    private void triggerSnapshotIfRequired(String type, AggregateIdentifier aggregateIdentifier,
                                           final AtomicInteger eventCount) {
        if (eventCount.get() > trigger) {
            snapshotter.scheduleSnapshot(type, aggregateIdentifier);
            eventCount.set(1);
        }
    }

    /**
     * Sets the snapshotter to notify when a snapshot needs to be taken.
     *
     * @param snapshotter the snapshotter to notify
     */
    public void setSnapshotter(Snapshotter snapshotter) {
        this.snapshotter = snapshotter;
    }

    /**
     * Sets the number of events that will trigger the creation of a snapshot events. Defaults to 50.
     * <p/>
     * This means that a snapshot is created as soon as loading an aggregate would require reading in more than 50
     * events.
     *
     * @param trigger The default trigger value.
     */
    public void setTrigger(int trigger) {
        this.trigger = trigger;
    }

    /**
     * Inidicates whether to maintain counters for aggregates after appending events to the event store for these
     * aggregates. Defaults to <code>true</code>.
     * <p/>
     * By setting this value to false, event counters are kept in memory. This is particularly useful when repositories
     * use caches, preventing events from being loaded. Consider registering the Caches use using {@link
     * #setAggregateCache(net.sf.jsr107cache.Cache)} or {@link #setAggregateCaches(java.util.List)}
     *
     * @param clearCountersAfterAppend indicator whether to clear counters after appending events
     */
    public void setClearCountersAfterAppend(boolean clearCountersAfterAppend) {
        this.clearCountersAfterAppend = clearCountersAfterAppend;
    }

    /**
     * Sets the Cache instance used be Caching repositories. By registering them to the snapshotter trigger, it can
     * optimize memory usage by clearing counters held for aggregates that are contained in caches. When an aggregate is
     * evicted or deleted from the cache, its event counter is removed from the trigger.
     * <p/>
     * Use the {@link #setAggregateCaches(java.util.List)} method if you have configured different caches for different
     * repositories.
     * <p/>
     * Using this method will automatically set {@link #setClearCountersAfterAppend(boolean)} to <code>false</code>.
     *
     * @param cache The cache used by caching repositories
     * @see #setAggregateCaches(java.util.List)
     */
    public void setAggregateCache(Cache cache) {
        this.clearCountersAfterAppend = false;
        cache.addListener(new CacheListener());
    }

    /**
     * Sets the Cache instances used be Caching repositories. By registering them to the snapshotter trigger, it can
     * optimize memory usage by clearing counters held for aggregates that are contained in caches. When an aggregate is
     * evicted or deleted from the cache, its event counter is removed from the trigger.
     *
     * @param caches The caches used by caching repositories
     */
    public void setAggregateCaches(List<Cache> caches) {
        for (Cache cache : caches) {
            setAggregateCache(cache);
        }
    }

    private class CountingEventStream implements DomainEventStream {

        private final DomainEventStream delegate;
        private final AtomicInteger counter;

        public CountingEventStream(DomainEventStream delegate, AtomicInteger counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public DomainEvent next() {
            DomainEvent next = delegate.next();
            counter.incrementAndGet();
            return next;
        }

        @Override
        public DomainEvent peek() {
            return delegate.peek();
        }

        /**
         * Returns the counter containing the number of bytes read.
         *
         * @return the counter containing the number of bytes read
         */
        protected AtomicInteger getCounter() {
            return counter;
        }
    }

    private final class TriggeringEventStream extends CountingEventStream {

        private final String aggregateType;
        private AggregateIdentifier aggregateIdentifier;

        private TriggeringEventStream(String aggregateType, AggregateIdentifier aggregateIdentifier,
                                      DomainEventStream delegate, AtomicInteger counter) {
            super(delegate, counter);
            this.aggregateType = aggregateType;
            this.aggregateIdentifier = aggregateIdentifier;
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = super.hasNext();
            if (!hasNext) {
                CurrentUnitOfWork.get().registerListener(new SnapshotTriggeringListener(aggregateType,
                                                                                        aggregateIdentifier,
                                                                                        getCounter()));
                if (clearCountersAfterAppend) {
                    counters.remove(aggregateIdentifier, getCounter());
                }
            }
            return hasNext;
        }
    }

    private final class CacheListener implements net.sf.jsr107cache.CacheListener {

        @Override
        public void onLoad(Object key) {
        }

        @Override
        public void onPut(Object key) {
        }

        @SuppressWarnings({"SuspiciousMethodCalls"})
        @Override
        public void onEvict(Object key) {
            counters.remove(key);
        }

        @SuppressWarnings({"SuspiciousMethodCalls"})
        @Override
        public void onRemove(Object key) {
            counters.remove(key);
        }

        @Override
        public void onClear() {
            counters.clear();
        }
    }

    private class SnapshotTriggeringListener extends UnitOfWorkListenerAdapter {

        private final String aggregateType;
        private final AggregateIdentifier aggregateIdentifier;
        private final AtomicInteger counter;

        public SnapshotTriggeringListener(String aggregateType,
                                          AggregateIdentifier aggregateIdentifier, AtomicInteger counter) {
            this.aggregateType = aggregateType;
            this.aggregateIdentifier = aggregateIdentifier;
            this.counter = counter;
        }

        @Override
        public void onCleanup() {
            triggerSnapshotIfRequired(aggregateType, aggregateIdentifier, counter);
        }
    }
}
