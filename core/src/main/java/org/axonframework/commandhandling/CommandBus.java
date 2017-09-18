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

package org.axonframework.commandhandling;

/**
 * 该接口用于描述 分发命令如何到达所匹配的命令处理器。命令处理器都可以订阅或取消订阅者一个类型的命令在这个命令总线上，
 * 任何时候都只存在一个命令处理器对应一个类型的命令
 * The mechanism that dispatches Command objects to their appropriate CommandHandler. CommandHandlers can subscribe and
 * unsubscribe to specific types of commands on the command bus. Only a single handler may be subscribed for a single
 * type of command at any time.
 *
 * @author Allard Buijze
 * @since 0.5
 */
public interface CommandBus {

    /**
     * 分发一条命令到已订阅有该命令的命令处理器的命令总线，处理后立即返回，没有反馈这个分发的状态
     * Dispatch the given <code>command</code> to the CommandHandler subscribed to that type of <code>command</code>. No
     * feedback is given about the status of the dispatching process. Implementations may return immediately after
     * asserting a valid handler is registered for the given command.
     *
     * @param command The Command to dispatch
     * @throws NoHandlerForCommandException when no command handler is registered for the given <code>command</code>.
     */
    void dispatch(Object command);

    /**
     * Dispatch the given <code>command</code> to the CommandHandler subscribed to that type of <code>command</code>.
     * When the command is processed, on of the callback methods is called, depending on the result of the processing.
     * <p/>
     * When the method returns, the only guarantee provided by the CommandBus implementation, is that the command has
     * been successfully received. Implementations are highly recommended to perform basic validation of the command
     * before returning from this method call.
     * <p/>
     * Implementations must start a UnitOfWork when before dispatching the command, and either commit or rollback after
     * a successful or failed execution, respectively.
     *
     * @param command  The Command to dispatch
     * @param callback The callback to invoke when command processing is complete
     * @param <R>      The type of the expected result
     * @throws NoHandlerForCommandException when no command handler is registered for the given <code>command</code>.
     */
    /**
     * 分发一条命令到已订阅有该命令的命令处理器的命令总线，处理完成后，调用反馈的那种方法执行，取决于处理的结果
     * <p/>
     * 当该方法返回时，CommandBus实现提供的唯一保证是该命令已成功接收。 强烈推荐实现在从该方法调用返回之前执行命令的基本验证。
     * <p/>
     * 实现类必须在命令被分发之前启动一个工作单元(UnitOfWork) 并在命令执行成功或失败后  提交commit or 回滚rollback
     * @param command
     */
    <R> void dispatch(Object command, CommandCallback<R> callback);

    /**
     * Subscribe the given <code>handler</code> to commands of type <code>commandType</code>.
     * <p/>
     * If a subscription already exists for the given type, the behavior is undefined. Implementations may throw an
     * Exception to refuse duplicate subscription or alternatively decide whether the existing or new
     * <code>handler</code> gets the subscription.
     *
     * @param commandType The type of command to subscribe the handler to
     * @param handler     The handler instance that handles the given type of command
     * @param <C>         The Type of command
     */
    /**
     * 将命令处理器网总线上订阅一种命令
     * <p/>
     * 如果该类型的订阅已存在，行为未定义， 实现类需要抛出异常，并拒绝重复订阅，或者 决定保存原有的还是新的处理器存在在命令总线上
     * @param commandName
     * @param handler
     */
    <C> void subscribe(Class<C> commandType, CommandHandler<? super C> handler);

    /**
     * Unsubscribe the given <code>handler</code> to commands of type <code>commandType</code>. If the handler is not
     * currently assigned to that type of command, no action is taken.
     *
     * @param commandType The type of command the handler is subscribed to
     * @param handler     The handler instance to unsubscribe from the CommandBus
     * @param <C>         The Type of command
     */
    /**
     * 将一个命令处理器从总线上移除掉，移除后该命令处理器不再接收处理新的命令
     * @param commandName
     * @param handler
     */
    <C> void unsubscribe(Class<C> commandType, CommandHandler<? super C> handler);

}
