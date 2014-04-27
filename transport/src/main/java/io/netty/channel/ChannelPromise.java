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
package io.netty.channel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * <pre>
 * 单从字面意思来看，是通道承诺。不明白。
 * 
 * 从官方解释的意思来看，意思是该channel是可写的。 没有搞懂这个ChannelPromise本身存在的场景意义。 TODO
 * </pre>
 * 
 * Special {@link ChannelFuture} which is writable.
 */
public interface ChannelPromise extends ChannelFuture, Promise<Void> {

    @Override
    Channel channel();

    @Override
    ChannelPromise setSuccess(Void result);

    @Override
    ChannelPromise setFailure(Throwable cause);
    
    ChannelPromise setSuccess();

    boolean trySuccess();

    /**
     * 下边开始又是重写了ChannelFuture(Future)接口的方法。我就不再具体注释了。
     * 
     * start.
     */
    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise sync() throws InterruptedException;

    @Override
    ChannelPromise syncUninterruptibly();

    @Override
    ChannelPromise await() throws InterruptedException;

    @Override
    ChannelPromise awaitUninterruptibly();
    
    /**
     * end.
     */
}

