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

import org.axonframework.domain.Event;

/**
 * Interface describing an implementation of a Saga. Sagas are instances that handle events and may possibly produce new
 * commands or have other side effects. Typically, Sagas are used to manage long running business transactions.
 * <p/>
 * Multiple instances of a single type of Saga may exist. In that case, each Saga will be managing a different
 * transaction. Sagas need to be associated with concepts in order to receive specific events. These associations are
 * managed through AssociationValues. For example, to associate a saga with an Order with ID 1234, this saga needs an
 * association value with key <code>"orderId"</code> and value <code>"1234"</code>.
 *
 * @author Allard Buijze
 * @since 0.7
 */
public interface Saga {

    /**
     * Returns the unique identifier of this saga.
     * 返回saga的唯一标识符
     * @return the unique identifier of this saga
     */
    String getSagaIdentifier();

    /**
     * Returns a view on the Association Values for this saga instance. The returned instance is mutable.
     * 返回saga的关联属性，该属性是只读的
     * @return a view on the Association Values for this saga instance
     */
    AssociationValues getAssociationValues();

    /**
     * Handle the given event. The actual result of processing depends on the implementation of the saga.
     * <p/>
     * Implementations are highly discouraged from throwing exceptions.
     *
     * @param event the event to handle
     */
    /**
     * 处理给定的事件，实际的处理过程依靠该接口的具体实现。
     * <p/>
     * 具体的实例不推荐抛出异常
     * @param event
     */
    void handle(Event event);

    /**
     * Indicates whether or not this saga is active. A Saga is active when its life cycle has not been ended.
     *
     * @return <code>true</code> if this saga is active, <code>false</code> otherwise.
     */
    /**
     * 判断saga是否为活跃状态。saga保持活跃状态直到该saga的生命周期结束
     * @return
     */
    boolean isActive();
}
