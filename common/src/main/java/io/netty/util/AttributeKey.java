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

import java.util.concurrent.ConcurrentMap;

/**
 * <pre>
 * 属性的Key。
 * </pre>
 * 
 * Key which can be used to access {@link Attribute} out of the {@link AttributeMap}. Be aware that it is not be
 * possible to have multiple keys with the same name.
 *
 *
 * @param <T>   the type of the {@link Attribute} which can be accessed via this {@link AttributeKey}.
 * 
 * TODO:这里官方有注释注明这个泛型的T是仅仅被用在编译时期， 怎么理解?
 */
@SuppressWarnings({ "UnusedDeclaration", "deprecation" }) // 'T' is used only at compile time
public final class AttributeKey<T> extends UniqueName {

	// 使用了一个变量CAN_USE_CHM_V8来判断到底是哪种并发Map。提升性能的一个举措。 TODO
    private static final ConcurrentMap<String, Boolean> names = PlatformDependent.newConcurrentHashMap();

    /**
     * <pre>
     * 根据指定的名称创建一个新的AttribueKey对象。
     * 新的valueOf方法替代了以前的公开的构造器。
     * </pre>
     * 
     * Creates a new {@link AttributeKey} with the specified {@code name}.
     */
    @SuppressWarnings("deprecation")
    public static <T> AttributeKey<T> valueOf(String name) {
        return new AttributeKey<T>(name);
    }

    /**
     * <PRE>
     * 该方法已经废弃。
     * </PRE>
     * 
     * @deprecated Use {@link #valueOf(String)} instead.
     */
    @Deprecated
    public AttributeKey(String name) {
    	// go： UniqueName构造器
        super(names, name);
    }
}
