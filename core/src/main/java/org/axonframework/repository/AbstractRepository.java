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

package org.axonframework.repository;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.AggregateRoot;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.SaveAggregateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Abstract implementation of the {@link Repository} that takes care of the dispatching of events when an aggregate is
 * persisted. All uncommitted events on an aggregate are dispatched when the aggregate is saved.
 * <p/>
 * Note that this repository implementation does not take care of any locking. The underlying persistence is expected
 * to
 * deal with concurrency. Alternatively, consider using the {@link LockingRepository}.
 *
 * @param <T> The type of aggregate this repository stores
 * @author Allard Buijze
 * @see #setEventBus(org.axonframework.eventhandling.EventBus)
 * @see LockingRepository
 * @since 0.1
 */
public abstract class AbstractRepository<T extends AggregateRoot> implements Repository<T> {

    private EventBus eventBus;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SimpleSaveAggregateCallback saveAggregateCallback = new SimpleSaveAggregateCallback();

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(T aggregate) {
        if (aggregate.getVersion() != null) {
            throw new IllegalArgumentException("Only newly created (unpersisted) aggregates may be added.");
        }
        CurrentUnitOfWork.get().registerAggregate(aggregate, saveAggregateCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @throws AggregateNotFoundException if aggregate with given id cannot be found
     * @throws RuntimeException           any exception thrown by implementing classes
     */
    @Override
    public T load(AggregateIdentifier aggregateIdentifier, Long expectedVersion) {
        T aggregate = doLoad(aggregateIdentifier, expectedVersion);
        validateOnLoad(aggregate, expectedVersion);
        return CurrentUnitOfWork.get().registerAggregate(aggregate, saveAggregateCallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T load(AggregateIdentifier aggregateIdentifier) {
        return load(aggregateIdentifier, null);
    }

    /**
     * Checks the aggregate for concurrent changes. Throws a {@link org.axonframework.repository.ConflictingModificationException}
     * when conflicting changes have been detected.
     * <p/>
     * This implementation throws a {@link ConflictingAggregateVersionException} if the expected version is not null
     * and
     * the version number of the aggregate does not match the expected version
     *
     * @param aggregate       The loaded aggregate
     * @param expectedVersion The expected version of the aggregate
     * @throws ConflictingModificationException
     *
     * @throws ConflictingAggregateVersionException
     *
     */
    protected void validateOnLoad(T aggregate, Long expectedVersion) {
        if (expectedVersion != null && aggregate.getVersion() != null && aggregate.getVersion() > expectedVersion) {
            throw new ConflictingAggregateVersionException(
                    String.format("Aggregate with identifier [%s] contains conflicting changes. "
                                          + "Expected version [%s], but was [%s]",
                                  aggregate.getIdentifier(),
                                  expectedVersion,
                                  aggregate.getVersion()));
        }
    }

    /**
     * Performs the actual saving of the aggregate.
     *
     * @param aggregate the aggregate to store
     */
    protected abstract void doSave(T aggregate);

    /**
     * Loads and initialized the aggregate with the given aggregateIdentifier.
     *
     * @param aggregateIdentifier the identifier of the aggregate to load
     * @param expectedVersion     The expected version of the aggregate to load
     * @return a fully initialized aggregate
     *
     * @throws AggregateNotFoundException if the aggregate with given identifier does not exist
     */
    protected abstract T doLoad(AggregateIdentifier aggregateIdentifier, Long expectedVersion);

    /**
     * Sets the event bus to which newly stored events should be published. Optional. By default, the repository tries
     * to autowire the event bus.
     *
     * @param eventBus the event bus to publish events to
     */
    @Resource
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private class SimpleSaveAggregateCallback implements SaveAggregateCallback<T> {

        @Override
        public void save(final T aggregate) {
            DomainEventStream uncommittedEvents = aggregate.getUncommittedEvents();
            doSave(aggregate);
            aggregate.commitEvents();
            dispatchUncommittedEvents(uncommittedEvents);
        }

        private void dispatchUncommittedEvents(DomainEventStream uncommittedEvents) {
            while (uncommittedEvents.hasNext()) {
                DomainEvent event = uncommittedEvents.next();
                if (logger.isDebugEnabled()) {
                    logger.debug("Publishing event [{}] to the UnitOfWork", event.getClass().getSimpleName());
                }
                CurrentUnitOfWork.get().publishEvent(event, eventBus);
            }
        }
    }
}
