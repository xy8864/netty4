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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <pre>
 * 		������MultithreadEventExecutorGroup���̳���AbstractEventExecutorGroup��
 * 		MultithreadEventExecutorGroup��ר������������̳߳����ĳ����ϲ��ࡣ
 * </pre>
 * 
 * Abstract base class for {@link EventExecutorGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    private final EventExecutor[] children;
    private final AtomicInteger childIndex = new AtomicInteger();
    private final AtomicInteger terminatedChildren = new AtomicInteger();
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    /**
     * Create a new instance.
     * 
     * ����MultithreadEventExecutorGroupʵ����
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param threadFactory     the ThreadFactory to use, or {@code null} if the default should be used.
     * @param args              arguments which will passed to each {@link #newChild(ThreadFactory, Object...)} call
     */
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        // ���û�д����̹߳������Լ�����һ��Netty��DefaultThreadFactory�ࡣ
        if (threadFactory == null) {
        	// ��������ȥʵ��
            threadFactory = newDefaultThreadFactory();
        }

        // ����ֻ��newһ��SingleThreadEventExecutor�����顣������Ҫʵ�����������
        children = new SingleThreadEventExecutor[nThreads];
        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
            	// ����һ��SingleThreadEventExecutor�� �����ฺ��ʵ�֡�
                children[i] = newChild(threadFactory, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
            	// �������ʧ�ܣ� �ص���Ӧ���̳߳أ��Լ��ȴ���ߵ������ս�
                if (!success) {
                    for (int j = 0; j < i; j ++) {
                        children[j].shutdownGracefully();
                    }

                    for (int j = 0; j < i; j ++) {
                        EventExecutor e = children[j];
                        try {
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        // û���� TODO
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (terminatedChildren.incrementAndGet() == children.length) {
                    terminationFuture.setSuccess(null);
                }
            }
        };

        // û���� TODO
        for (EventExecutor e: children) {
            e.terminationFuture().addListener(terminationListener);
        }
    }

    /**
     * �����µ��̹߳�����
     * 
     * @return
     */
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    /**
     * <pre>
     * 		next������ʵ�ַǳ���Ҫ��
     * 
     * 		������һ���������±�����childIndex(AtomicInteger����)��Ȼ�����child����
     * 		ȡ������Ϊchild�������±ꡣ
     * </pre>
     */
    @Override
    public EventExecutor next() {
        return children[Math.abs(childIndex.getAndIncrement() % children.length)];
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return children().iterator();
    }

    /**
     * Return the number of {@link EventExecutor} this implementation uses. This number is the maps
     * 1:1 to the threads it use.
     */
    public final int executorCount() {
        return children.length;
    }

    /**
     * <pre>
     * 		����EventExecutor�����б�
     * </pre>
     * 
     * Return a safe-copy of all of the children of this group.
     */
    protected Set<EventExecutor> children() {
    	// ��仰���Ƕ����أ��ѵ����ǵ����±���䣿
    	// Set<EventExecutor> children = new HashSet<EventExecutor>();
    	// ��Ҫ��һ��Collections.newSetFromMap��������ڣ� TODO
    	// Ϊʲô��ֱ�ӷ���children��                                                 TODO
        Set<EventExecutor> children = Collections.newSetFromMap(new LinkedHashMap<EventExecutor, Boolean>());
        Collections.addAll(children, this.children);
        return children;
    }

    /**
     * <pre>
     * 		newChild()��������������صķ�����������ȴʵ��
     * </pre>
     * 
     * Create a new EventExecutor which will later then accessible via the {@link #next()}  method. This method will be
     * called for each thread that will serve this {@link MultithreadEventExecutorGroup}.
     */
    protected abstract EventExecutor newChild(
            ThreadFactory threadFactory, Object... args) throws Exception;

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l: children) {
        	// �����ײ�(EventExecutor)ȥƽ���Ĺر�EventExecutorGroup��
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }
        
        // �ر��껹��ҪterminationFuture()��ʲô��˼�� TODO
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l: children) {
            l.shutdown();
        }
    }

    /**
     * <pre>
     * 		�鿴��ǰexecutorGroup�Ƿ���ShuttingDown״̬��
     * </pre>
     */
    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l: children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    /**
     * <pre>
     * 		�鿴��ǰexecutorGroup�Ƿ���ShutDown״̬��
     * </pre>
     */
    @Override
    public boolean isShutdown() {
        for (EventExecutor l: children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventExecutor l: children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop: for (EventExecutor l: children) {
            for (;;) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }
}
