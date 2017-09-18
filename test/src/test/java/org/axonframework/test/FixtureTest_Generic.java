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

package org.axonframework.test;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StringAggregateIdentifier;
import org.axonframework.domain.UUIDAggregateIdentifier;
import org.axonframework.eventstore.EventStoreException;
import org.junit.*;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class FixtureTest_Generic {

    private FixtureConfiguration fixture;

    @Before
    public void setUp() {
        fixture = Fixtures.newGivenWhenThenFixture();
    }

    @Test
    public void testAggregateIdentifier_DefaultsToUUID() {
        assertNotNull(fixture.getAggregateIdentifier());
        // this must work
        UUID.fromString(fixture.getAggregateIdentifier().asString());
    }

    @Test
    public void testAggregateIdentifier_Custom() {
        fixture.setAggregateIdentifier(new StringAggregateIdentifier("My value"));
        assertEquals("My value", fixture.getAggregateIdentifier().asString());
    }

    @Test(expected = EventStoreException.class)
    public void testFixtureGeneratesExceptionOnWrongEvents_DifferentAggregateIdentifiers() {
        fixture.getEventStore().appendEvents("whatever", new SimpleDomainEventStream(
                new StubDomainEvent(new UUIDAggregateIdentifier(), 0),
                new StubDomainEvent(new UUIDAggregateIdentifier(), 1)));
    }

    @Test(expected = EventStoreException.class)
    public void testFixtureGeneratesExceptionOnWrongEvents_WrongSequence() {
        UUIDAggregateIdentifier identifier = new UUIDAggregateIdentifier();
        fixture.getEventStore().appendEvents("whatever", new SimpleDomainEventStream(
                new StubDomainEvent(identifier, 0),
                new StubDomainEvent(identifier, 1),
                new StubDomainEvent(identifier, 3)));
    }

    private class StubDomainEvent extends DomainEvent {
        private static final long serialVersionUID = 123033211453385579L;

        public StubDomainEvent(AggregateIdentifier aggregateIdentifier, long sequenceNumber) {
            super(sequenceNumber, aggregateIdentifier);
        }
    }
}
