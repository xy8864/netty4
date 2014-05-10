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
package io.netty.util;

import io.netty.util.internal.PlatformDependent;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * <PRE>
 * AttributeMap的默认实现。使用了简单的同步来保持尽可能低的内存开销(TODO)。
 * </PRE>
 * 
 * Default {@link AttributeMap} implementation which use simple synchronization to keep the memory overhead
 * as low as possible.
 */
public class DefaultAttributeMap implements AttributeMap {

	// 看了一些注释，感觉AtomicReferenceFieldUpdater是用来在2个对象之间交互数据并且是原子操作的辅助类 TODO
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DefaultAttributeMap, Map> updater;

    static {
    	// 又是一个从PlateformDependent依赖获取的例子 不知道这里提升了什么性能 TODO
    	@SuppressWarnings("rawtypes")
        AtomicReferenceFieldUpdater<DefaultAttributeMap, Map> referenceFieldUpdater =
                PlatformDependent.newAtomicReferenceFieldUpdater(DefaultAttributeMap.class, "map");
        if (referenceFieldUpdater == null) {
            referenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(DefaultAttributeMap.class, Map.class, "map");
        }
        updater = referenceFieldUpdater;
    }

    // Initialize lazily to reduce memory consumption; updated by AtomicReferenceFieldUpdater above.
    @SuppressWarnings("UnusedDeclaration")
    private volatile Map<AttributeKey<?>, Attribute<?>> map;

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        Map<AttributeKey<?>, Attribute<?>> map = this.map;
        if (map == null) {
        	// 没有使用ConcurrentHashMap是因为其消耗内存较高 TODO
        	// 这里用IdentifyHashMap的想法是什么？TODO  IdentifyHashMap是根据地址来作为key的
        	// 所以IdentifyHashMap的key可以是equals的(但是address不一致)
            // Not using ConcurrentHashMap due to high memory consumption.
            map = new IdentityHashMap<AttributeKey<?>, Attribute<?>>(2);
            if (!updater.compareAndSet(this, null, map)) {
                map = this.map;
            }
        }

        synchronized (map) {
            @SuppressWarnings("unchecked")
            Attribute<T> attr = (Attribute<T>) map.get(key);
            // 拿不到Attribute的默认初始化
            if (attr == null) {
                attr = new DefaultAttribute<T>(map, key);
                map.put(key, attr);
            }
            return attr;
        }
    }

    private static final class DefaultAttribute<T> extends AtomicReference<T> implements Attribute<T> {

        private static final long serialVersionUID = -2661411462200283011L;

        private final Map<AttributeKey<?>, Attribute<?>> map;
        private final AttributeKey<T> key;

        DefaultAttribute(Map<AttributeKey<?>, Attribute<?>> map, AttributeKey<T> key) {
            this.map = map;
            this.key = key;
        }

        @Override
        public AttributeKey<T> key() {
            return key;
        }

        @Override
        public T setIfAbsent(T value) {
            while (!compareAndSet(null, value)) {
                T old = get();
                if (old != null) {
                    return old;
                }
            }
            return null;
        }

        @Override
        public T getAndRemove() {
            T oldValue = getAndSet(null);
            remove0();
            return oldValue;
        }

        @Override
        public void remove() {
            set(null);
            remove0();
        }

        private void remove0() {
            synchronized (map) {
                map.remove(key);
            }
        }
    }
}
