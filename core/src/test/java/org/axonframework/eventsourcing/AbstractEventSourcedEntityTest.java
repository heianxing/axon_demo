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

import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.StubDomainEvent;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class AbstractEventSourcedEntityTest {

    private StubEntity testSubject;

    @Before
    public void setUp() {
        testSubject = new StubEntity();
    }

    @Test
    public void testRecursivelyApplyEvent() {
        testSubject.handleRecursively(new StubDomainEvent());
        assertEquals(1, testSubject.invocationCount);
        testSubject.handleRecursively(new StubDomainEvent());
        assertEquals(2, testSubject.invocationCount);
        assertEquals(1, testSubject.child.invocationCount);
    }

    @Test
    public void testApplyDelegatesToAggregateRoot() {
        AbstractEventSourcedAggregateRoot aggregateRoot = mock(AbstractEventSourcedAggregateRoot.class);
        testSubject.registerAggregateRoot(aggregateRoot);
        StubDomainEvent event = new StubDomainEvent();
        testSubject.apply(event);
        verify(aggregateRoot).apply(event);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateAggregateRootRegistration_DifferentAggregate() {
        AbstractEventSourcedAggregateRoot aggregateRoot1 = mock(AbstractEventSourcedAggregateRoot.class);
        AbstractEventSourcedAggregateRoot aggregateRoot2 = mock(AbstractEventSourcedAggregateRoot.class);
        testSubject.registerAggregateRoot(aggregateRoot1);
        testSubject.registerAggregateRoot(aggregateRoot2);
    }

    @Test
    public void testDuplicateAggregateRootRegistration_SameAggregate() {
        AbstractEventSourcedAggregateRoot aggregateRoot = mock(AbstractEventSourcedAggregateRoot.class);
        testSubject.registerAggregateRoot(aggregateRoot);
        testSubject.registerAggregateRoot(aggregateRoot);
    }

    private class StubEntity extends AbstractEventSourcedEntity {

        private int invocationCount = 0;
        private StubEntity child;

        @Override
        protected void handle(DomainEvent event) {
            if (invocationCount == 1 && child == null) {
                child = new StubEntity();
            }
            invocationCount++;
        }
    }
}
