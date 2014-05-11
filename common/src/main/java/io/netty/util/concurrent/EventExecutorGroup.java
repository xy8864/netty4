/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 *     EventExecutorGroup翻译成中文就是 事件执行组。他里边包含了多个事件执行器。	
 *     接口提供了next方法去获取一个执行器。除了这点之外，还提供了在全局方法里处理
 *     生命周期相关＆允许关闭的功能。
 * </pre>
 * 
 * The {@link EventExecutorGroup} is responsible to provide {@link EventExecutor}'s to use via its
 * {@link #next()} method. Beside this it also is responsible to handle their live-cycle and allows
 * to shut them down in a global fashion.
 *
 */
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor> {

    /**
     * <pre>
     * 		真心没看懂这里的方法到底是返回Group是在关闭中还是已经关闭。妈的。TODO
     * </pre>
     * 
     * Returns {@code true} if and only if this executor was started to be
     * {@linkplain #shutdownGracefully() shut down gracefuclly} or was {@linkplain #isShutdown() shut down}.
     * 
     */
    boolean isShuttingDown();

    /**
     * <pre>
     * 		优雅的实现group的关闭。么有参数传入(用默认值处理)。
     * </pre>
     * 
     * Shortcut method for {@link #shutdownGracefully(long, long, TimeUnit)} with sensible default values.
     *
     * @return the {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully();

    /**
     * <pre>
     * 		该方法被调用后会向executorGroup发出一个信号，表示我想要关闭你了。一旦调
     * 		用，isShunttingDown()将返回为true。executorGroup也将准备开始关闭自己。
     * 		这个方法不像shutdown()，他会确保没有任务在"平稳期"被提交。如果在平稳期有
     * 		任务进入，他会接受，同时平稳期重新开始计算。 
     * </pre>
     * 
     * Signals this executor that the caller wants the executor to be shut down.  Once this method is called,
     * {@link #isShuttingDown()} starts to return {@code true}, and the executor prepares to shut itself down.
     * Unlike {@link #shutdown()}, graceful shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
     * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
     * it is guaranteed to be accepted and the quiet period will start over.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
     *                    regardless if a task was submitted during the quiet period
     * @param unit        the unit of {@code quietPeriod} and {@code timeout}
     *
     * @return the {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);

    /**
     * Returns the {@link Future} which is notified when this executor has been terminated.
     */
    Future<?> terminationFuture();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    List<Runnable> shutdownNow();

    /**
     * <pre>
     * 		返回EventExecutorGroup中的其中一个executor。
     * </pre>
     * 
     * Returns one of the {@link EventExecutor}s that belong to this group.
     */
    EventExecutor next();

    /**
     * Returns a read-only {@link Iterator} over all {@link EventExecutor}, which are handled by this
     * {@link EventExecutorGroup} at the time of invoke this method.
     */
    @Override
    Iterator<EventExecutor> iterator();

    @Override
    Future<?> submit(Runnable task);

    @Override
    <T> Future<T> submit(Runnable task, T result);

    @Override
    <T> Future<T> submit(Callable<T> task);

    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
