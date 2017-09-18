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

package org.axonframework.eventstore.jpa;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.DomainEvent;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventSerializer;
import org.axonframework.eventstore.EventStoreManagement;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.eventstore.EventVisitor;
import org.axonframework.eventstore.SnapshotEventStore;
import org.axonframework.eventstore.XStreamEventSerializer;
import org.axonframework.repository.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

/**
 * An EventStore implementation that uses JPA to store DomainEvents in a database. The actual DomainEvent is stored as a
 * serialized blob of bytes. Other columns are used to store meta-data that allow quick finding of DomainEvents for a
 * specific aggregate in the correct order.
 * <p/>
 * The serializer used to serialize the events is configurable. By default, the {@link XStreamEventSerializer} is used.
 *
 * @author Allard Buijze
 * @since 0.5
 */
public class JpaEventStore implements SnapshotEventStore, EventStoreManagement {

    private static final Logger logger = LoggerFactory.getLogger(JpaEventStore.class);

    private EntityManager entityManager;

    private final EventSerializer eventSerializer;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private PersistenceExceptionResolver persistenceExceptionResolver;

    /**
     * Initialize a JpaEventStore using an {@link org.axonframework.eventstore.XStreamEventSerializer}, which serializes
     * events as XML.
     */
    public JpaEventStore() {
        this(new XStreamEventSerializer());
    }

    /**
     * Initialize a JpaEventStore which serializes events using the given {@link org.axonframework.eventstore.EventSerializer}.
     *
     * @param eventSerializer The serializer to (de)serialize domain events with.
     */
    public JpaEventStore(EventSerializer eventSerializer) {
        this.eventSerializer = eventSerializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendEvents(String type, DomainEventStream events) {

        DomainEvent event = null;
        try {
            while (events.hasNext()) {
                event = events.next();
                DomainEventEntry entry = new DomainEventEntry(type, event, eventSerializer);
                entityManager.persist(entry);
            }
        } catch (RuntimeException exception) {
            if (persistenceExceptionResolver != null
                    && persistenceExceptionResolver.isDuplicateKeyViolation(exception)) {
                throw new ConcurrencyException(
                        String.format("Concurrent modification detected for Aggregate identifier [%s], sequence: [%s]",
                                      event.getAggregateIdentifier(),
                                      event.getSequenceNumber().toString()),
                        exception);
            }
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DomainEventStream readEvents(String type, AggregateIdentifier identifier) {
        long snapshotSequenceNumber = -1;
        SnapshotEventEntry lastSnapshotEvent = loadLastSnapshotEvent(type, identifier);
        if (lastSnapshotEvent != null) {
            snapshotSequenceNumber = lastSnapshotEvent.getSequenceNumber();
        }

        List<DomainEvent> events = fetchBatch(type, identifier, snapshotSequenceNumber + 1);
        if (lastSnapshotEvent != null) {
            events.add(0, lastSnapshotEvent.getDomainEvent(eventSerializer));
        }
        if (events.isEmpty()) {
            throw new EventStreamNotFoundException(type, identifier);
        }
        return new BatchingDomainEventStream(events, identifier, type);
    }

    @SuppressWarnings({"unchecked"})
    private List<DomainEvent> fetchBatch(String type, AggregateIdentifier identifier, long firstSequenceNumber) {
        List<DomainEventEntry> entries = (List<DomainEventEntry>) entityManager.createQuery(
                "SELECT e FROM DomainEventEntry e "
                        + "WHERE e.aggregateIdentifier = :id AND e.type = :type AND e.sequenceNumber >= :seq "
                        + "ORDER BY e.sequenceNumber ASC")
                .setParameter("id",
                              identifier.asString())
                .setParameter("type", type)
                .setParameter("seq", firstSequenceNumber)
                .setMaxResults(batchSize)
                .getResultList();
        List<DomainEvent> events = new ArrayList<DomainEvent>(entries.size());
        for (DomainEventEntry entry : entries) {
            events.add(entry.getDomainEvent(eventSerializer));
        }
        return events;
    }

    @SuppressWarnings({"unchecked"})
    private SnapshotEventEntry loadLastSnapshotEvent(String type, AggregateIdentifier identifier) {
        List<SnapshotEventEntry> entries = entityManager.createQuery(
                "SELECT e FROM SnapshotEventEntry e "
                        + "WHERE e.aggregateIdentifier = :id AND e.type = :type "
                        + "ORDER BY e.sequenceNumber DESC")
                .setParameter("id", identifier.asString())
                .setParameter("type", type)
                .setMaxResults(1)
                .setFirstResult(0)
                .getResultList();
        if (entries.size() < 1) {
            return null;
        }
        return entries.get(0);
    }

    @Override
    public void appendSnapshotEvent(String type, DomainEvent snapshotEvent) {
        entityManager.persist(new SnapshotEventEntry(type, snapshotEvent, eventSerializer));
    }

    @Override
    public void visitEvents(EventVisitor visitor) {
        int first = 0;
        List<DomainEventEntry> batch;
        boolean shouldContinue = true;
        while (shouldContinue) {
            batch = fetchBatch(first);
            for (DomainEventEntry entry : batch) {
                visitor.doWithEvent(entry.getDomainEvent(eventSerializer));
            }
            shouldContinue = (batch.size() >= batchSize);
            first += batchSize;
        }
    }

    @SuppressWarnings({"unchecked"})
    private List<DomainEventEntry> fetchBatch(int startPosition) {
        return entityManager.createQuery(
                "SELECT e FROM DomainEventEntry e ORDER BY e.timeStamp ASC, e.sequenceNumber ASC")
                .setFirstResult(startPosition)
                .setMaxResults(batchSize)
                .getResultList();
    }

    /**
     * Sets the EntityManager for this EventStore to use. This EntityManager must be assigned to a persistence context
     * that contains the {@link DomainEventEntry} as one of the managed entity types.
     *
     * @param entityManager the EntityManager to use.
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Registers the data source that allows the EventStore to detect the database type and define the error codes that
     * represent concurrent access failures.
     * <p/>
     * Should not be used in combination with {@link #setPersistenceExceptionResolver(PersistenceExceptionResolver)},
     * but rather as a shorthand alternative for most common database types.
     *
     * @param dataSource A data source providing access to the backing database
     * @throws SQLException If an error occurs while accessing the dataSource
     */
    public void setDataSource(DataSource dataSource) throws SQLException {
        if (persistenceExceptionResolver == null) {
            persistenceExceptionResolver = new SQLErrorCodesResolver(dataSource);
        }
    }

    /**
     * Sets the persistenceExceptionResolver that will help detect concurrency exceptions from the backing database.
     *
     * @param persistenceExceptionResolver the persistenceExceptionResolver that will help detect concurrency
     *                                     exceptions
     */
    public void setPersistenceExceptionResolver(PersistenceExceptionResolver persistenceExceptionResolver) {
        this.persistenceExceptionResolver = persistenceExceptionResolver;
    }

    /**
     * Sets the number of events that should be read at each database access. When more than this number of events must
     * be read to rebuild an aggregate's state, the events are read in batches of this size. Defaults to 100.
     * <p/>
     * Tip: if you use a snapshotter, make sure to choose snapshot trigger and batch size such that a single batch will
     * generally retrieve all events required to rebuild an aggregate's state.
     *
     * @param batchSize the number of events to read on each database access. Default to 100.
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private final class BatchingDomainEventStream implements DomainEventStream {

        private int currentBatchSize;
        private Iterator<DomainEvent> currentBatch;
        private DomainEvent next;
        private final AggregateIdentifier id;
        private final String typeId;

        private BatchingDomainEventStream(List<DomainEvent> firstBatch, AggregateIdentifier id,
                                          String typeId) {
            this.id = id;
            this.typeId = typeId;
            this.currentBatchSize = firstBatch.size();
            this.currentBatch = firstBatch.iterator();
            if (currentBatch.hasNext()) {
                next = currentBatch.next();
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public DomainEvent next() {
            DomainEvent nextEvent = next;
            if (!currentBatch.hasNext() && currentBatchSize >= batchSize) {
                logger.debug("Fetching new batch for Aggregate [{}]", id.asString());
                currentBatch = fetchBatch(typeId, id, next.getSequenceNumber() + 1).iterator();
            }
            next = currentBatch.hasNext() ? currentBatch.next() : null;
            return nextEvent;
        }

        @Override
        public DomainEvent peek() {
            return next;
        }
    }
}
