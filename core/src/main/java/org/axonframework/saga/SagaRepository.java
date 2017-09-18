/*
 * Copyright (c) 2010. Axon Framework
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

package org.axonframework.saga;

import java.util.Set;

/**
 * Interface towards the storage mechanism of Saga instances. Saga Repositories can find sagas either through the values
 * they have been associated with (see {@link AssociationValue}) or via their unique identifier.
 * 该接口用于实现存储saga的机制
 * @author Allard Buijze
 * @since 0.7
 */
public interface SagaRepository {

    /**
     * Find saga instances of the given <code>type</code> that have been associated with the given
     * 通过saga类型以及关联值来获取saga
     * <code>associationValue</code>.
     * <p/>
     * Returned Sagas must be {@link #commit(Saga) committed} after processing.
     * 返回的saga必须是{@link #commit(Saga)}处理后的
     * @param type             The type of Saga to return
     * @param associationValue The value that the returned Sagas must be associated with
     * @param <T>              The type of Saga to return
     * @return A Set containing the found Saga instances. If none are found, an empty Set is returned. Will never return
     *         <code>null</code>.
     */
    <T extends Saga> Set<T> find(Class<T> type, Set<AssociationValue> associationValue);

    /**
     * Loads a known Saga instance by its unique identifier. Returned Sagas must be {@link #commit(Saga) committed}
     * after processing.
     *
     * @param type           The expected type of Saga
     * @param sagaIdentifier The unique identifier of the Saga to load
     * @param <T>            The expected type of Saga
     * @return The Saga instance
     *
     * 通过标识符来加载saga
     * @throws NoSuchSagaException if no Saga with given identifier can be found
     */

    <T extends Saga> T load(Class<T> type, String sagaIdentifier) throws NoSuchSagaException;

    /**
     * Commits the changes made to the Saga instance. At this point, the repository may release any resources kept for
     * this saga.
     * 提交已改变saga实例。如果一个saga已标记为inactive，那么存储会删掉该saga，并且移掉该saga的关联值
     *
     * @param saga The Saga instance to commit
     */
    void commit(Saga saga);

    /**
     * Registers a newly created Saga with the Repository. Once a Saga instance has been added, it can be found using
     * its association values or its unique identifier.
     * 添加一个新的saga实例，如果该saga是inactive，那么会添加失败
     *
     * @param saga The Saga instances to add.
     */
    void add(Saga saga);

}
