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

package org.axonframework.commandhandling.callbacks;

import org.axonframework.commandhandling.CommandCallback;

/**
 * Abstract callback that can be extended when no result is expected from the command handler execution.
 *
 * @author Allard Buijze
 * @since 0.6
 */
public abstract class VoidCallback implements CommandCallback<Class<Void>> {

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation merely invokes {@link #onSuccess()}.
     */
    @Override
    public void onSuccess(Class<Void> result) {
        onSuccess();
    }

    /**
     * Invoked when command handling execution was successful.
     */
    protected abstract void onSuccess();
}
