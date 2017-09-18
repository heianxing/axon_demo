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

package org.axonframework.repository;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.AggregateRoot;

/**
 * LockManager implementation that does nothing. Can be useful in cases where a repository extending from the {@link
 * org.axonframework.repository.LockingRepository} needs to be configured to ignore locks, for example in scenario's
 * where an underlying storage mechanism already performs the necessary locking.
 *
 * @author Allard Buijze
 * @since 0.6
 */
class NullLockManager implements LockManager {

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation always returns true.
     */
    @Override
    public boolean validateLock(AggregateRoot aggregate) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does nothing.
     */
    @Override
    public void obtainLock(AggregateIdentifier aggregateIdentifier) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does nothing.
     */
    @Override
    public void releaseLock(AggregateIdentifier aggregateIdentifier) {
    }
}
