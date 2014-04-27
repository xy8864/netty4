/*
 * Copyright 2013 The Netty Project
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;


/**
 * <pre>
 * 代表netty的一个异步操作的结果，该接口继承了JUC的Future接口。
 * </pre>
 * 
 * The result of an asynchronous operation.
 */
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * <pre>
     * 当且仅当IO操作完成时返回true。
     * 
     * TODO:
     * 1. 这里特指IO操作是什么意思?
     * 2. 和JUC的Future中的isDone方法有和不同?
     * </pre>
     * 
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     */
    boolean isSuccess();

    /**
     * <pre>
     * 当且仅当操作可以被终止的时候返回true。
     * </pre>
     * 
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     */
    boolean isCancellable();

    /**
     * <pre>
     * 如果IO操作有失败发生则返回错误异常。 
     * 
     * PS: 如果当前任务还没结束或者任务成功则返回null。
     * 
     * TODO: 这里有专门指出了是在IO场景下。
     * </pre>
     * 
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     *
     * @return the cause of the failure.
     *         {@code null} if succeeded or this future is not
     *         completed yet.
     */
    Throwable cause();

    /**
     * <pre>
     * 将指定的listener加到future中。当future完成时候，这些listener立刻被唤醒开始执行。
     * </pre>
     * 
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * <pre>
     * 将指定的listener加到future中。当future完成时候，这些listener立刻被唤醒开始执行。(多参数方法)
     * </pre>
     * 
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * <pre>
     * 将指定的listener从future中移除。移除后该listener就不会再被唤醒了。
     * 如果传入的listener在future中本来就没有绑定，则什么也不做。
     * </pre>
     * 
     * Removes the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * <pre>
     * 将指定的listener从future中移除。移除后该listener就不会再被唤醒了。
     * 如果传入的listener在future中本来就没有绑定，则什么也不做。（多参数）
     * </pre>
     * 
     * Removes the specified listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * <pre>
     * 同步方法，等待。直到future返回ok。并且如果future失败的话异常重抛(TODO:后边这句没理解。)。
     * </pre>
     * 
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    Future<V> sync() throws InterruptedException;

    /**
     * <pre>
     * 同步方法，等待。直到future返回ok。并且如果future失败的话异常重抛(TODO:后边这句没理解。)。
     * 
     * 如果抓到中断异常则丢弃它。
     * </pre>
     * 
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    Future<V> syncUninterruptibly();

    /**
     * <pre>
     * 方法等待。等待future完成。
     * </pre>
     * 
     * Waits for this future to be completed.
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    Future<V> await() throws InterruptedException;

    /**
     * <pre>
     * 方法等待。等待future完成。如果抓到中断异常则丢弃它。
     * </pre>
     * 
     * 
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     */
    Future<V> awaitUninterruptibly();

    /**
     * <pre>
     * 等待future在指定的时间内完成。
     * </pre>
     * 
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * <pre>
     * 等待future在指定的时间内完成。
     * </pre>
     * 
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * <pre>
     * 等待future在指定的时间内完成。如果抓到中断异常则丢弃。
     * </pre>
     * 
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * <pre>
     * 等待future在指定的时间内完成。如果抓到中断异常则丢弃。
     * </pre>
     * 
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * <pre>
     * 立刻返回结果。如果future还没有完成则返回null。
     * </pre>
     * 
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not relay on the returned {@code null} value.
     */
    V getNow();

    /**
     * <pre>
     * future终止。如果future已经终止则抛出CancellationException异常。
     * </pre>
     * 
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
