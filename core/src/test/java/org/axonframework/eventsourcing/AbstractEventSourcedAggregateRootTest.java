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

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.domain.StubDomainEvent;
import org.axonframework.domain.UUIDAggregateIdentifier;
import org.junit.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class AbstractEventSourcedAggregateRootTest {

    private CompositeAggregateRoot testSubject;

    @Test
    public void testInitializeWithEvents() {
        AggregateIdentifier identifier = new UUIDAggregateIdentifier();
        testSubject = new CompositeAggregateRoot(identifier);
        testSubject.initializeState(new SimpleDomainEventStream(new StubDomainEvent(identifier, 243)));

        assertEquals(identifier, testSubject.getIdentifier());
        assertEquals(0, testSubject.getUncommittedEventCount());
        assertEquals(1, testSubject.getInvocationCount());
        assertEquals(1, testSubject.getSimpleEntity().getInvocationCount());
        assertEquals(new Long(243), testSubject.getVersion());
    }

    @Test
    public void testApplyEvent() {
        testSubject = new CompositeAggregateRoot();

        assertNotNull(testSubject.getIdentifier());
        assertEquals(0, testSubject.getUncommittedEventCount());
        assertEquals(null, testSubject.getVersion());

        testSubject.apply(new StubDomainEvent());

        Map.Entry<SimpleEntity, SimpleEntity> firstMapEntry;
        assertEquals(1, testSubject.getInvocationCount());
        assertEquals(1, testSubject.getUncommittedEventCount());
        assertEquals(1, testSubject.getSimpleEntity().getInvocationCount());
        assertEquals(1, testSubject.getSimpleEntityList().get(0).getInvocationCount());
        firstMapEntry = testSubject.getSimpleEntityMap().entrySet().iterator().next();
        assertEquals(1, firstMapEntry.getKey().getInvocationCount());
        assertEquals(1, firstMapEntry.getValue().getInvocationCount());

        testSubject.getSimpleEntity().applyEvent();
        assertEquals(2, testSubject.getInvocationCount());
        assertEquals(2, testSubject.getUncommittedEventCount());
        assertEquals(2, testSubject.getSimpleEntity().getInvocationCount());
        assertEquals(2, testSubject.getSimpleEntityList().get(0).getInvocationCount());
        firstMapEntry = testSubject.getSimpleEntityMap().entrySet().iterator().next();
        assertEquals(2, firstMapEntry.getKey().getInvocationCount());
        assertEquals(2, firstMapEntry.getValue().getInvocationCount());

        assertEquals(null, testSubject.getVersion());

        testSubject.commitEvents();
        assertEquals(new Long(1), testSubject.getVersion());
        assertFalse(testSubject.getUncommittedEvents().hasNext());
    }

    /**
     * @author Allard Buijze
     */
    public static class CompositeAggregateRoot extends AbstractEventSourcedAggregateRoot {

        private int invocationCount;
        private SimpleEntity childEntity;
        private List<SimpleEntity> childEntitiesList = new ArrayList<SimpleEntity>();
        private Map<SimpleEntity, SimpleEntity> childEntitiesMap = new HashMap<SimpleEntity, SimpleEntity>();

        CompositeAggregateRoot() {
            super();
        }

        CompositeAggregateRoot(AggregateIdentifier identifier) {
            super(identifier);
        }

        @Override
        protected void handle(DomainEvent event) {
            this.invocationCount++;
            if (childEntity == null) {
                childEntity = new SimpleEntity();
            }
            childEntitiesList.add(new SimpleEntity());
            if (childEntitiesMap.isEmpty()) {
                childEntitiesMap.put(new SimpleEntity(), new SimpleEntity());
            }
        }

        public int getInvocationCount() {
            return invocationCount;
        }

        public SimpleEntity getSimpleEntity() {
            return childEntity;
        }

        public List<SimpleEntity> getSimpleEntityList() {
            return childEntitiesList;
        }

        public Map<SimpleEntity, SimpleEntity> getSimpleEntityMap() {
            return childEntitiesMap;
        }
    }

    /**
     * @author Allard Buijze
     */
    public static class SimpleEntity extends AbstractEventSourcedEntity {

        private int invocationCount;

        @Override
        protected void handle(DomainEvent event) {
            this.invocationCount++;
        }

        public int getInvocationCount() {
            return invocationCount;
        }

        public void applyEvent() {
            apply(new StubDomainEvent());
        }
    }
}
