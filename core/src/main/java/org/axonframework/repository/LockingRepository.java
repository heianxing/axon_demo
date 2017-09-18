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
import org.axonframework.unitofwork.CurrentUnitOfWork;
import org.axonframework.unitofwork.UnitOfWorkListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Repository interface that takes provides a locking mechanism to prevent concurrent
 * modifications of persisted aggregates. Unless there is a locking mechanism present in the underlying persistence
 * environment, it is recommended to use a LockingRepository (or one of its subclasses).
 * <p/>
 * The LockingRepository can be initialized with two strategies: <ul><li><em>Optimistic Locking</em> strategy: This
 * strategy performs better than the pessimistic one, but you will only discover a concurrency issue at the time a
 * thread tries to save an aggregate. If another thread has saved the same aggregate earlier (but after the first
 * thread
 * loaded its copy), an exception is thrown. The only way to recover from this exception is to load the aggregate from
 * the repository again, replay all actions on it and save it. <li><em>Pessimistic Locking</em> strategy (default):
 * Pessimistic Locking requires an exclusive lock to be handed to a thread loading an aggregate before the aggregate is
 * handed over. This means that, once an aggregate is loaded, it has full exclusive access to it, until it saves the
 * aggregate. With this strategy, it is important that -no matter what- the aggregate is saved to the repository. Any
 * failure to do so will result in threads blocking endlessly, waiting for a lock that might never be released. </ul>
 * <p/>
 * Important: If an exception is thrown during the saving process, any locks held are released. The calling thread may
 * reattempt saving the aggregate again. If the lock is available, the thread automatically takes back the lock. If,
 * however, another thread has obtained the lock first, a ConcurrencyException is thrown.
 *
 * @param <T> The type that this aggregate stores
 * @author Allard Buijze
 * @since 0.3
 */
public abstract class LockingRepository<T extends AggregateRoot> extends AbstractRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(LockingRepository.class);

    private final LockManager lockManager;

    /**
     * Initialize a repository with a pessimistic locking strategy.
     */
    protected LockingRepository() {
        this(LockingStrategy.PESSIMISTIC);
    }

    /**
     * Initialize the repository with the given <code>lockingStrategy</code>.
     *
     * @param lockingStrategy the locking strategy to use
     */
    protected LockingRepository(LockingStrategy lockingStrategy) {
        switch (lockingStrategy) {
            case PESSIMISTIC:
                lockManager = new PessimisticLockManager();
                break;
            case OPTIMISTIC:
                lockManager = new OptimisticLockManager();
                break;
            case NO_LOCKING:
                lockManager = new NullLockManager();
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("This repository implementation does not support the [%s] locking strategy",
                                      lockingStrategy.name()));
        }
    }

    /**
     * Utility constructor for testing.
     *
     * @param lockManager the lock manager to use
     */
    LockingRepository(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public void add(T aggregate) {
        lockManager.obtainLock(aggregate.getIdentifier());
        try {
            super.add(aggregate);
            CurrentUnitOfWork.get().registerListener(new LockCleaningListener(aggregate));
        } catch (RuntimeException ex) {
            logger.warn("Exception occurred while trying to add an aggregate. Releasing lock.", ex);
            lockManager.releaseLock(aggregate.getIdentifier());
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws AggregateNotFoundException if aggregate with given id cannot be found
     * @throws RuntimeException           any exception thrown by implementing classes
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public T load(AggregateIdentifier aggregateIdentifier, Long expectedVersion) {
        lockManager.obtainLock(aggregateIdentifier);
        try {
            final T aggregate = super.load(aggregateIdentifier, expectedVersion);
            CurrentUnitOfWork.get().registerListener(new LockCleaningListener(aggregate));
            return aggregate;
        } catch (RuntimeException ex) {
            logger.warn("Exception occurred while trying to load an aggregate. Releasing lock.", ex);
            lockManager.releaseLock(aggregateIdentifier);
            throw ex;
        }
    }

    /**
     * Verifies whether all locks are valid and delegates to {@link #doSaveWithLock(org.axonframework.domain.AggregateRoot)}
     * to perform actual storage.
     *
     * @param aggregate the aggregate to store
     */
    @Override
    protected final void doSave(T aggregate) {
        if (aggregate.getVersion() != null && !lockManager.validateLock(aggregate)) {
            throw new ConcurrencyException(String.format(
                    "The aggregate of type [%s] with identifier [%s] could not be "
                            + "saved, as a valid lock is not held. Either another thread has saved an aggregate, or "
                            + "the current thread had released its lock earlier on.",
                    aggregate.getClass().getSimpleName(),
                    aggregate.getIdentifier()));
        }
        doSaveWithLock(aggregate);
    }

    /**
     * Perform the actual saving of the aggregate. All necessary locks have been verified.
     *
     * @param aggregate the aggregate to store
     */
    protected abstract void doSaveWithLock(T aggregate);

    /**
     * Perform the actual loading of an aggregate. The necessary locks have been obtained.
     *
     * @param aggregateIdentifier the identifier of the aggregate to load
     * @param expectedVersion     The expected version of the aggregate
     * @return the fully initialized aggregate
     *
     * @throws AggregateNotFoundException if aggregate with given id cannot be found
     */
    @Override
    protected abstract T doLoad(AggregateIdentifier aggregateIdentifier, Long expectedVersion);

    /**
     * UnitOfWorkListeners that cleans up remaining locks after a UnitOfWork has been committed or rolled back.
     */
    private class LockCleaningListener extends UnitOfWorkListenerAdapter {

        private final T aggregate;

        public LockCleaningListener(T aggregate) {
            this.aggregate = aggregate;
        }

        @Override
        public void onCleanup() {
            lockManager.releaseLock(aggregate.getIdentifier());
        }
    }
}
