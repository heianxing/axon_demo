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

package org.axonframework.integration.adapter;

import org.axonframework.domain.Event;

/**
 * Interface describing an Event Filter. The Event Filter decides which events may be forwarded by adapters and which
 * should be blocked.
 *
 * @author Allard Buijze
 * @since 0.4
 */
public interface EventFilter {

    /**
     * Whether or not this filter allows an event of the given type to pass through or not.
     *
     * @param eventType The actual type of the event.
     * @return <code>true</code> if this event should be forwarded, <code>false</code> otherwise.
     */
    boolean accept(Class<? extends Event> eventType);

}
