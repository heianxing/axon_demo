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

import org.axonframework.domain.AbstractAggregateRoot;

import javax.persistence.Basic;
import javax.persistence.Entity;

@Entity
public class JpaAggregate extends AbstractAggregateRoot {

    @Basic
    private String message;

    public JpaAggregate(String message) {
        this.message = message;
    }

    /**
     * Constructor performing very basic initialization, as required by JPA.
     */
    protected JpaAggregate() {
    }

    public void setMessage(String newMessage) {
        this.message = newMessage;
        registerEvent(new MessageUpdatedEvent(message));
    }
}
