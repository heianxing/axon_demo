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
import net.sf.jsr107cache.CacheListener;
import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StubAggregate;
import org.axonframework.domain.StubDomainEvent;
import org.axonframework.domain.UUIDAggregateIdentifier;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.DefaultUnitOfWork;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.*;
import org.mockito.internal.matchers.*;

import java.util.Arrays;

import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class EventCountSnapshotterTriggerTest {

    private EventCountSnapshotterTrigger testSubject;
    private Snapshotter mockSnapshotter;
    private AggregateIdentifier aggregateIdentifier;
    private Cache mockCache;
    private CapturingMatcher<CacheListener> listener;
    private EventSourcedAggregateRoot aggregate;

    private UnitOfWork unitOfWork;

    @Before
    public void setUp() throws Exception {
        mockSnapshotter = mock(Snapshotter.class);
        testSubject = new EventCountSnapshotterTrigger();
        testSubject.setTrigger(3);
        testSubject.setSnapshotter(mockSnapshotter);
        aggregateIdentifier = new UUIDAggregateIdentifier();
        aggregate = new StubAggregate(aggregateIdentifier);
        mockCache = mock(Cache.class);
        listener = new CapturingMatcher<CacheListener>();
        doNothing().when(mockCache).addListener(argThat(listener));

        unitOfWork = DefaultUnitOfWork.startAndGet();
    }

    @After
    public void tearDown() {
        if (unitOfWork.isStarted()) {
            unitOfWork.rollback();
        }
    }

    @Test
    public void testSnapshotterTriggered() {
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1),
                new StubDomainEvent(aggregateIdentifier, 2)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter).scheduleSnapshot("some", aggregateIdentifier);
    }

    @Test
    public void testSnapshotterNotTriggeredOnRead() {
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1),
                new StubDomainEvent(aggregateIdentifier, 2),
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
    }

    @Test
    public void testSnapshotterNotTriggeredOnSave() {
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 2)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
    }

    @Test
    public void testCounterDoesNotResetWhenUsingCache() {
        testSubject.setAggregateCache(mockCache);
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 2)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter).scheduleSnapshot("some", aggregateIdentifier);
    }

    @Test
    public void testCounterResetWhenCacheEvictsEntry() {
        testSubject.setAggregateCaches(Arrays.asList(mockCache));
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 2)
        )));

        listener.getLastValue().onEvict(aggregateIdentifier);

        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter, never()).scheduleSnapshot("test", aggregateIdentifier);
    }

    @Test
    public void testCounterResetWhenCacheRemovesEntry() {
        testSubject.setAggregateCaches(Arrays.asList(mockCache));
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 2)
        )));

        listener.getLastValue().onRemove(aggregateIdentifier);

        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter, never()).scheduleSnapshot("test", aggregateIdentifier);
    }

    @Test
    public void testCounterResetWhenCacheCleared() {
        testSubject.setAggregateCaches(Arrays.asList(mockCache));
        readAllFrom(testSubject.decorateForRead("some", aggregateIdentifier, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 0),
                new StubDomainEvent(aggregateIdentifier, 1)
        )));
        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 2)
        )));

        listener.getLastValue().onClear();

        readAllFrom(testSubject.decorateForAppend("some", aggregate, new SimpleDomainEventStream(
                new StubDomainEvent(aggregateIdentifier, 3)
        )));

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
        verify(mockSnapshotter, never()).scheduleSnapshot("test", aggregateIdentifier);
    }

    @Test
    public void testStoringAggregateWithoutChanges() {
        SimpleDomainEventStream emptyEventStream = new SimpleDomainEventStream();
        testSubject.decorateForAppend("test", aggregate, emptyEventStream);

        verify(mockSnapshotter, never()).scheduleSnapshot("some", aggregateIdentifier);
        CurrentUnitOfWork.commit();
    }

    private void readAllFrom(DomainEventStream events) {
        while (events.hasNext()) {
            events.next();
        }
    }
}
