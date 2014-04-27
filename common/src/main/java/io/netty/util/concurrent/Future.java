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
 * ����netty��һ���첽�����Ľ�����ýӿڼ̳���JUC��Future�ӿڡ�
 * </pre>
 * 
 * The result of an asynchronous operation.
 */
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * <pre>
     * ���ҽ���IO�������ʱ����true��
     * 
     * TODO:
     * 1. ������ָIO������ʲô��˼?
     * 2. ��JUC��Future�е�isDone�����кͲ�ͬ?
     * </pre>
     * 
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     */
    boolean isSuccess();

    /**
     * <pre>
     * ���ҽ����������Ա���ֹ��ʱ�򷵻�true��
     * </pre>
     * 
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     */
    boolean isCancellable();

    /**
     * <pre>
     * ���IO������ʧ�ܷ����򷵻ش����쳣�� 
     * 
     * PS: �����ǰ����û������������ɹ��򷵻�null��
     * 
     * TODO: ������ר��ָ��������IO�����¡�
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
     * ��ָ����listener�ӵ�future�С���future���ʱ����Щlistener���̱����ѿ�ʼִ�С�
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
     * ��ָ����listener�ӵ�future�С���future���ʱ����Щlistener���̱����ѿ�ʼִ�С�(���������)
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
     * ��ָ����listener��future���Ƴ����Ƴ����listener�Ͳ����ٱ������ˡ�
     * ��������listener��future�б�����û�а󶨣���ʲôҲ������
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
     * ��ָ����listener��future���Ƴ����Ƴ����listener�Ͳ����ٱ������ˡ�
     * ��������listener��future�б�����û�а󶨣���ʲôҲ���������������
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
     * ͬ���������ȴ���ֱ��future����ok���������futureʧ�ܵĻ��쳣����(TODO:������û��⡣)��
     * </pre>
     * 
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    Future<V> sync() throws InterruptedException;

    /**
     * <pre>
     * ͬ���������ȴ���ֱ��future����ok���������futureʧ�ܵĻ��쳣����(TODO:������û��⡣)��
     * 
     * ���ץ���ж��쳣��������
     * </pre>
     * 
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    Future<V> syncUninterruptibly();

    /**
     * <pre>
     * �����ȴ����ȴ�future��ɡ�
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
     * �����ȴ����ȴ�future��ɡ����ץ���ж��쳣��������
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
     * �ȴ�future��ָ����ʱ������ɡ�
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
     * �ȴ�future��ָ����ʱ������ɡ�
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
     * �ȴ�future��ָ����ʱ������ɡ����ץ���ж��쳣������
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
     * �ȴ�future��ָ����ʱ������ɡ����ץ���ж��쳣������
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
     * ���̷��ؽ�������future��û������򷵻�null��
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
     * future��ֹ�����future�Ѿ���ֹ���׳�CancellationException�쳣��
     * </pre>
     * 
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
