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

package org.axonframework.eventsourcing;

import org.axonframework.eventstore.EventStore;
import org.axonframework.repository.GenericJpaRepository;
import org.axonframework.repository.LockingStrategy;

/**
 * Repository that stores both a (JPA based) relational model of the current state of an aggregate and the events
 * produced by that aggregate. When an aggregate is loaded, only the relational model is used to reconstruct the
 * aggregate state.
 * <p/>
 * As events are not used for reconstructing the aggregate state, there is no need for snapshots or upcasters. In some
 * scenario's that could be a sensible choice.
 *
 * @param <T> The type of aggregate stored in this repository. Must implement {@link EventSourcedAggregateRoot}.
 * @author Allard Buijze
 * @since 1.0
 */
public class HybridJpaRepository<T extends EventSourcedAggregateRoot> extends GenericJpaRepository<T> {

    private EventStore eventStore;
    private String aggregateTypeIdentifier;

    /**
     * Initializes a Hybrid Repository that stored entities of the given <code>aggregateType</code> and the locking
     * mechanism provided by the backend storage.
     *
     * @param aggregateType The type of aggregate stored in this repository.
     */
    public HybridJpaRepository(Class<T> aggregateType) {
        this(aggregateType, aggregateType.getSimpleName());
    }

    /**
     * Initializes a Hybrid Repository that stored entities of the given <code>aggregateType</code> without locking. The
     * events are appended to the event store under the given <code>aggregateTypeIdentifier</code>.
     *
     * @param aggregateType           The type of aggregate stored in this repository.
     * @param aggregateTypeIdentifier The type identifier to store events with
     */
    public HybridJpaRepository(Class<T> aggregateType, String aggregateTypeIdentifier) {
        this(aggregateType, aggregateTypeIdentifier, LockingStrategy.NO_LOCKING);
    }

    /**
     * Initializes a Hybrid Repository that stored entities of the given <code>aggregateType</code> and a locking
     * mechanism based on the given <code>lockingStrategy</code>.
     *
     * @param aggregateType   The type of aggregate stored in this repository.
     * @param lockingStrategy The locking strategy to use when loading and storing aggregates
     */
    public HybridJpaRepository(Class<T> aggregateType, LockingStrategy lockingStrategy) {
        this(aggregateType, aggregateType.getSimpleName(), lockingStrategy);
    }

    /**
     * Initializes a Hybrid Repository that stored entities of the given <code>aggregateType</code> and a locking
     * mechanism based on the given <code>lockingStrategy</code>.
     *
     * @param aggregateType           The type of aggregate stored in this repository.
     * @param aggregateTypeIdentifier The type identifier to store events with
     * @param lockingStrategy         The locking strategy to use when loading and storing aggregates
     */
    public HybridJpaRepository(Class<T> aggregateType, String aggregateTypeIdentifier,
                               LockingStrategy lockingStrategy) {
        super(aggregateType, lockingStrategy);
        this.aggregateTypeIdentifier = aggregateTypeIdentifier;
    }

    @Override
    protected void doSaveWithLock(T aggregate) {
        if (eventStore != null) {
            eventStore.appendEvents(getTypeIdentifier(), aggregate.getUncommittedEvents());
        }
        super.doSaveWithLock(aggregate);
    }

    /**
     * Returns the type identifier to use when appending events in the event store. Default to the simple class name of
     * the aggregateType provided in the constructor.
     *
     * @return the type identifier to use when appending events in the event store.
     */
    protected String getTypeIdentifier() {
        return aggregateTypeIdentifier;
    }

    /**
     * The event store to which events are appended. This event store is not used to load events, as the aggregate's
     * state is loaded from a relational model.
     * <p/>
     * If no event store is configured, events are not appended.
     *
     * @param eventStore The event store where events should be appended
     */
    public void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }
}
