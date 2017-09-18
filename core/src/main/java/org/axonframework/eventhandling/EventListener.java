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

package org.axonframework.eventhandling;

import org.axonframework.domain.Event;

/**
 * Interface to be implemented by classes that can handle events.
 *
 * @author Allard Buijze
 * @see EventBus
 * @see org.axonframework.domain.DomainEvent
 * @see org.axonframework.eventhandling.annotation.EventHandler
 * @since 0.1
 */
/**
 * 该接口的实例用于处理事件
 * @author Administrator
 *
 */
public interface EventListener {

    /**
     * Process the given event. The implementation may decide to process or skip the given event. It is highly
     * unrecommended to throw any exception during the event handling process.
     *
     * @param event the event to handle
     */
    /**
     * 处理给定的事件，该类的实例将决定是否处理该事件，不建议在处理事件进程中抛出异常
     * @param event
     */
    void handle(Event event);

}
