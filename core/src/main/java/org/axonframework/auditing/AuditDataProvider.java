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

package org.axonframework.auditing;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface describing the instance that provides the relevant information for auditing purposes. The data provided by
 * this class is attached to all events processed by the {@link AuditingInterceptor}.
 *
 * 该接口用于提供相关联的审计信息，这些审计信息会附加到所有被{@link AuditingInterceptor}处理的事件
 * @author Allard Buijze
 * @since 0.7
 */
public interface AuditDataProvider {

    /**
     * Return the relevant auditing information for the given command. This method is called exactly once for each time
     * the command is dispatched.
     *
     * @param command The command being dispatched
     * @return a map containing key-value pairs of relevant information to include in audit logs.
     */
    /**
     * 该方法返回所给命令锁携带的信息。这个方法每次都是在命令被分发的时候调用
     * @param command 将要被分发的命令
     * @return 一张hash表，包含所有命令信息，以及审核的日志信息
     */
    Map<String, Serializable> provideAuditDataFor(Object command);

}
