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

package org.axonframework.saga.repository.jpa;

import org.axonframework.saga.AssociationValue;
import org.axonframework.saga.SagaStorageException;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * JPA wrapper around an Association Value. This entity is used to store relevant Association Values for Sagas.
 *
 * @author Allard Buijze
 * @since 0.7
 */
@Entity
public class AssociationValueEntry {

    @Id
    @GeneratedValue
    private Long id;

    @Basic
    private String sagaId;

    @Basic
    private String associationKey;

    @Lob
    private Serializable associationValue;

    /**
     * Initialize a new AssociationValueEntry for a saga with given <code>sagaIdentifier</code> and
     * <code>associationValue</code>.
     *
     * @param sagaIdentifier   The identifier of the saga
     * @param associationValue The association value for the saga
     */
    public AssociationValueEntry(String sagaIdentifier, AssociationValue associationValue) {
        if (!Serializable.class.isInstance(associationValue.getValue())) {
            throw new SagaStorageException("Could not persist a saga association, since the value is not serializable");
        }
        this.sagaId = sagaIdentifier;
        this.associationKey = associationValue.getKey();
        this.associationValue = (Serializable) associationValue.getValue();
    }

    /**
     * Constructor required by JPA. Do not use directly.
     */
    protected AssociationValueEntry() {
    }

    /**
     * Returns the association value contained in this entry.
     *
     * @return the association value contained in this entry
     */
    public AssociationValue getAssociationValue() {
        return new AssociationValue(associationKey, associationValue);
    }

    /**
     * Returns the Saga Identifier contained in this entry.
     *
     * @return the Saga Identifier contained in this entry
     */
    public String getSagaIdentifier() {
        return sagaId;
    }


    /**
     * The unique identifier of this entry.
     *
     * @return the unique identifier of this entry
     */
    public Long getId() {
        return id;
    }
}
