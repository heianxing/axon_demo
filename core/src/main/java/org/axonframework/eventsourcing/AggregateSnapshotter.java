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
import org.axonframework.domain.DomainEventStream;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;

/**
 * Implementation of a snapshotter that uses the actual aggregate and its state to create a snapshot event. The
 * motivation is that an aggregate always contains all relevant state. Therefore, storing the aggregate itself inside an
 * event should capture all necessary information.
 *
 * @author Allard Buijze
 * @since 0.6
 */
public class AggregateSnapshotter extends AbstractSnapshotter {

    private Map<String, AggregateFactory<?>> aggregateFactories = new ConcurrentHashMap<String, AggregateFactory<?>>();

    @Override
    protected DomainEvent createSnapshot(String typeIdentifier, DomainEventStream eventStream) {
        AggregateFactory<?> aggregateFactory = aggregateFactories.get(typeIdentifier);

        DomainEvent firstEvent = eventStream.peek();
        AggregateIdentifier aggregateIdentifier = firstEvent.getAggregateIdentifier();

        EventSourcedAggregateRoot aggregate = aggregateFactory.createAggregate(aggregateIdentifier, firstEvent);
        aggregate.initializeState(eventStream);

        return new AggregateSnapshot<EventSourcedAggregateRoot>(aggregate);
    }

    /**
     * Sets the aggregate factory to use. The aggregate factory is responsible for creating the aggregates that should
     * be stored within the {@link AggregateSnapshot AggegateSnapshots}.
     *
     * @param aggregateFactories The list of aggregate factories creating the aggregates to store. May not be
     *                           <code>null</code> or contain any <code>null</code> values.
     * @throws NullPointerException if <code>aggregateFactories</code> is <code>null</code> or if contains any
     *                              <code>null</code> values.
     */
    @Resource
    public void setAggregateFactories(List<AggregateFactory<?>> aggregateFactories) {
        for (AggregateFactory<?> factory : aggregateFactories) {
            this.aggregateFactories.put(factory.getTypeIdentifier(), factory);
        }
    }
}
