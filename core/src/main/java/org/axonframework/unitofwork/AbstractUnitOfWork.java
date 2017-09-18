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

package org.axonframework.unitofwork;

import org.axonframework.domain.AggregateRoot;
import org.axonframework.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of the UnitOfWork interface. Provides the necessary implementations to support most actions
 * required by any Unit of Work, such as managing registration with the {@link CurrentUnitOfWork} and support for
 * nesting a Unit of Work.
 *
 * @author Allard Buijze
 * @see CurrentUnitOfWork
 * @since 0.7
 */
public abstract class AbstractUnitOfWork implements UnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUnitOfWork.class);

    private boolean isStarted;
    private UnitOfWork outerUnitOfWork;
    private List<AbstractUnitOfWork> innerUnitsOfWork = new ArrayList<AbstractUnitOfWork>();

    @Override
    public void commit() {
        logger.debug("Committing Unit Of Work");
        assertStarted();
        try {
            notifyListenersPrepareCommit();
            saveAggregates();
            if (outerUnitOfWork == null) {
                logger.debug("This Unit Of Work is not nested. Finalizing commit...");
                doCommit();
                stop();
                notifyListenersCleanup();
            } else if (logger.isDebugEnabled()) {
                logger.debug("This Unit Of Work is nested. Commit will be finalized by outer Unit Of Work.");
            }
        } catch (RuntimeException e) {
            logger.debug("An error occurred while committing this UnitOfWork. Performing rollback...");
            doRollback(e);
            stop();
            notifyListenersCleanup();
            throw e;
        } finally {
            logger.debug("Clearing resources of this Unit Of Work.");
            clear();
        }
    }

    /**
     * Send a {@link org.axonframework.unitofwork.UnitOfWorkListener#onCleanup()} notification to all registered
     * listeners. The implementation must ensure that all listeners are notified, even if one throws an exception.
     */
    protected abstract void notifyListenersCleanup();

    /**
     * Send a {@link UnitOfWorkListener#onRollback(Throwable)} notification to all registered listeners.
     *
     * @param cause The cause of the rollback
     */
    protected abstract void notifyListenersRollback(Throwable cause);

    @Override
    public void rollback() {
        rollback(null);
    }

    @Override
    public void rollback(Throwable cause) {
        if (cause != null && logger.isInfoEnabled()) {
            logger.debug("Rollback requested for Unit Of Work due to exception. ", cause);
        } else if (logger.isInfoEnabled()) {
            logger.debug("Rollback requested for Unit Of Work for unknown reason.");
        }

        try {
            if (isStarted()) {
                doRollback(cause);
            }
        } finally {
            notifyListenersCleanup();
            clear();
            stop();
        }
    }

    @Override
    public void start() {
        logger.debug("Starting Unit Of Work.");
        if (isStarted) {
            throw new IllegalStateException("UnitOfWork is already started");
        } else if (CurrentUnitOfWork.isStarted()) {
            // we're nesting.
            this.outerUnitOfWork = CurrentUnitOfWork.get();
            if (outerUnitOfWork instanceof AbstractUnitOfWork) {
                ((AbstractUnitOfWork) outerUnitOfWork).registerInnerUnitOfWork(this);
            } else {
                outerUnitOfWork.registerListener(new CommitOnOuterCommitTask());
            }
        }
        logger.debug("Registering Unit Of Work as CurrentUnitOfWork");
        CurrentUnitOfWork.set(this);
        isStarted = true;
        doStart();
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    private void stop() {
        logger.debug("Stopping Unit Of Work");
        isStarted = false;
    }

    /**
     * Performs logic required when starting this UnitOfWork instance.
     * <p/>
     * This implementation does nothing and may be freely overridden.
     */
    protected void doStart() {
    }

    /**
     * Executes the logic required to commit this unit of work.
     */
    protected abstract void doCommit();

    /**
     * Executes the logic required to commit this unit of work.
     *
     * @param cause the cause of the rollback
     */
    protected abstract void doRollback(Throwable cause);

    private void performInnerCommit() {
        logger.debug("Finalizing commit of inner Unit Of Work...");
        CurrentUnitOfWork.set(this);
        try {
            doCommit();
        } catch (RuntimeException t) {
            doRollback(t);
            throw t;
        } finally {
            clear();
            stop();
            notifyListenersCleanup();
        }
    }

    private void assertStarted() {
        if (!isStarted) {
            throw new IllegalStateException("UnitOfWork is not started");
        }
    }

    private void clear() {
        CurrentUnitOfWork.clear(this);
    }

    /**
     * Commit all registered inner units of work. This should be invoked after events have been dispatched and before
     * any listeners are notified of the commit.
     */
    protected void commitInnerUnitOfWork() {
        for (AbstractUnitOfWork unitOfWork : innerUnitsOfWork) {
            if (unitOfWork.isStarted()) {
                unitOfWork.performInnerCommit();
            }
        }
    }

    private void registerInnerUnitOfWork(AbstractUnitOfWork unitOfWork) {
        innerUnitsOfWork.add(unitOfWork);
    }

    /**
     * Saves all registered aggregates by calling their respective callbacks.
     */
    protected abstract void saveAggregates();

    /**
     * Send a {@link org.axonframework.unitofwork.UnitOfWorkListener#onPrepareCommit(java.util.Set, java.util.List)}
     * notification to all registered listeners.
     */
    protected abstract void notifyListenersPrepareCommit();

    private class CommitOnOuterCommitTask implements UnitOfWorkListener {

        @Override
        public void afterCommit() {
            performInnerCommit();
        }

        @Override
        public void onRollback(Throwable failureCause) {
            CurrentUnitOfWork.set(AbstractUnitOfWork.this);
            try {
                doRollback(failureCause);
            } finally {
                CurrentUnitOfWork.clear(AbstractUnitOfWork.this);
            }
        }

        @Override
        public void onPrepareCommit(Set<AggregateRoot> aggregateRoots, List<Event> events) {
        }

        @Override
        public void onCleanup() {
        }
    }
}
