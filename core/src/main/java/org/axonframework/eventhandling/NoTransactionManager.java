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

package org.axonframework.eventhandling;

/**
 * TransactionManager implementation that does nothing. Is a placeholder implementation for the cases where no special
 * transaction management is required.
 *
 * @author Allard Buijze
 * @since 1.0
 */
public class NoTransactionManager implements TransactionManager {

    @Override
    public void beforeTransaction(TransactionStatus transactionStatus) {
    }

    @Override
    public void afterTransaction(TransactionStatus transactionStatus) {
    }
}
