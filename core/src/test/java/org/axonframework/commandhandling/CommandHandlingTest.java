/*
 * Copyright (c) 2010-2011. Axon Framework
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

package org.axonframework.commandhandling;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StubAggregate;
import org.axonframework.domain.UUIDAggregateIdentifier;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.GenericEventSourcingRepository;
import org.axonframework.eventstore.EventStore;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.DefaultUnitOfWork;
import org.junit.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class CommandHandlingTest {

    private GenericEventSourcingRepository<StubAggregate> repository;
    private AggregateIdentifier aggregateIdentifier;
    private EventBus mockEventBus;
    private EventStore mockEventStore;

    @Before
    public void setUp() {
        repository = new GenericEventSourcingRepository<StubAggregate>(StubAggregate.class);
        mockEventBus = mock(EventBus.class);
        mockEventStore = new StubEventStore();
        repository.setEventBus(mockEventBus);
        repository.setEventStore(mockEventStore);
        aggregateIdentifier = new UUIDAggregateIdentifier();
    }

    @Test
    public void testCommandHandlerLoadsSameAggregateTwice() {
        DefaultUnitOfWork.startAndGet();
        StubAggregate stubAggregate = new StubAggregate(aggregateIdentifier);
        stubAggregate.doSomething();
        repository.add(stubAggregate);
        CurrentUnitOfWork.commit();

        DefaultUnitOfWork.startAndGet();
        repository.load(aggregateIdentifier).doSomething();
        repository.load(aggregateIdentifier).doSomething();
        CurrentUnitOfWork.commit();

        DomainEventStream es = mockEventStore.readEvents("", aggregateIdentifier);
        assertTrue(es.hasNext());
        assertEquals((Object) 0L, es.next().getSequenceNumber());
        assertTrue(es.hasNext());
        assertEquals((Object) 1L, es.next().getSequenceNumber());
        assertTrue(es.hasNext());
        assertEquals((Object) 2L, es.next().getSequenceNumber());
        assertFalse(es.hasNext());
    }

    /**
     *
     */
    public static class StubEventStore implements EventStore {

        private List<DomainEvent> storedEvents = new LinkedList<DomainEvent>();

        @Override
        public void appendEvents(String type, DomainEventStream events) {
            while (events.hasNext()) {
                storedEvents.add(events.next());
            }
        }

        @Override
        public DomainEventStream readEvents(String type, AggregateIdentifier identifier) {
            return new SimpleDomainEventStream(new ArrayList<DomainEvent>(storedEvents));
        }
    }
}
