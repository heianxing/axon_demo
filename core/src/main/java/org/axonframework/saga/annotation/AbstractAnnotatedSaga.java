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

package org.axonframework.saga.annotation;

import org.axonframework.domain.Event;
import org.axonframework.saga.AssociationValue;
import org.axonframework.saga.AssociationValues;
import org.axonframework.saga.Saga;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

/**
 * Implementation of the {@link Saga interface} that delegates incoming events to {@link
 * org.axonframework.saga.annotation.SagaEventHandler @SagaEventHandler} annotated methods.
 * saga接口的抽象实现类，该类可以用saga中有注解@SagaEventHandler的事件处理器来处理传入的事件
 * @author Allard Buijze
 * @since 0.7
 */
public abstract class AbstractAnnotatedSaga implements Saga, Serializable {

    private final AssociationValues associationValues;
    private final String identifier;
    private transient volatile SagaEventHandlerInvoker eventHandlerInvoker;
    private volatile boolean isActive = true;

    /**
     * Initialize the saga with a random identifier. The identifier used is a randomly generated {@link UUID}.
     * 使用IdentifierFactory产生的识别码产生Saga
     */
    protected AbstractAnnotatedSaga() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Initialize the saga with the given identifier.
     *
     * @param identifier the identifier to initialize the saga with.
     */
    protected AbstractAnnotatedSaga(String identifier) {
        this.identifier = identifier;
        associationValues = new AssociationValuesImpl();
        associationValues.add(new AssociationValue("sagaIdentifier", identifier));
        eventHandlerInvoker = new SagaEventHandlerInvoker(this);
    }

    @Override
    public String getSagaIdentifier() {
        return identifier;
    }

    @Override
    public AssociationValues getAssociationValues() {
        return associationValues;
    }

    @Override
    public final void handle(Event event) {
        doHandle(event);
    }

    private void doHandle(Event event) {
        if (isActive) {
            eventHandlerInvoker.invokeSagaEventHandlerMethod(event);
            if (eventHandlerInvoker.isEndingEvent(event)) {
                end();
            }
        }
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    /**
     * Marks the saga as ended. Ended saga's may be cleaned up by the repository when they are committed.
     */
    protected void end() {
        isActive = false;
    }

    /**
     * Registers a AssociationValue with the given saga. When the saga is committed, it can be found using the
     * registered property.
     *
     * @param property The value to associate this saga with.
     */
    protected void associateWith(AssociationValue property) {
        associationValues.add(property);
    }

    /**
     * Registers a AssociationValue with the given saga. When the saga is committed, it can be found using the
     * registered property.
     *
     * @param key   The key of the association value to associate this saga with.
     * @param value The value of the association value to associate this saga with.
     */
    protected void associateWith(String key, Object value) {
        associationValues.add(new AssociationValue(key, value));
    }

    /**
     * Removes the given association from this Saga. When the saga is committed, it can no longer be found using the
     * given association. If the given property wasn't registered with the saga, nothing happens.
     *
     * @param property the association value to remove from the saga.
     */
    protected void removeAssociationWith(AssociationValue property) {
        associationValues.remove(property);
    }

    /**
     * Removes the given association from this Saga. When the saga is committed, it can no longer be found using the
     * given association value. If the given saga wasn't associated with given values, nothing happens.
     *
     * @param key   The key of the association value to remove from this saga.
     * @param value The value of the association value to remove from this saga.
     */
    protected void removeAssociationWith(String key, Object value) {
        associationValues.remove(new AssociationValue(key, value));
    }

    // Java Serialization methods

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        eventHandlerInvoker = new SagaEventHandlerInvoker(this);
    }
}
