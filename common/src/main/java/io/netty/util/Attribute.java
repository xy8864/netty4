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

/**
 * <pre>
 * 字面意思很好理解。就是代表了一个属性。只不过在netty中用对象
 * Attribute来代替。
 * 
 * 当前属性被设计用来存储值的引用(TODO： 理解这里的引用)。
 * 他可以自动更新所以是线程安全的(TODO: WHY???)。
 * </pre>
 * 
 * An attribute which allows to store a value reference. It may be updated atomically and so is thread-safe.
 *
 * @param <T>   the type of the value it holds.
 */
public interface Attribute<T> {

    /**
     * <pre>
     * 返回当前属性对应的KEY, 即AttributeKey对象。
     * </pre>
     * 
     * Returns the key of this attribute.
     */
    AttributeKey<T> key();

    /**
     * <pre>
     * 返回当前属性对应的实际包含的值。可能为空!
     * </pre>
     * 
     * Returns the current value, which may be {@code null}
     */
    T get();

    /**
     * <pre>
     * 设置当前属性的value值。	
     * </pre>
     * 
     * Sets the value
     */
    void set(T value);

    /**
     * <pre>
     * 自动设置value为当前值并返回老的value值。如果之前还没有设置会返回null。
     * 
     * TODO 这个方法类似于JUC的Automaic类的方法。需要关注。
     * </pre>  
     * 
     * Atomically sets to the given value and returns the old value which may be {@code null} if non was set before.
     */
    T getAndSet(T value);

    /**
     *  <pre>
     *  如果当前attribute还没有值，就把参数的value赋值为当前属性的值。
     *  否则什么都不做，只是把当前有的值返回。
     * 
     *  TODO 这个方法类似于JUC的Automaic类的方法。需要关注。
     *  </pre>
     * 
     *  Atomically sets to the given value if this {@link Attribute} does not contain a value at the moment.
     *  If it was not possible to set the value as it contains a value it will just return the current value.
     */
    T setIfAbsent(T value);

    /**
     * <pre>
     * 从AttributeMap中移除掉当前Attribute。并返回Attribute中的value。
     * 
     * 子序列get方法调用将返回null(TODO: 这句没懂)。
     * </pre>
     * 
     * Removes this attribute from the {@link AttributeMap} and returns the old value..  Subsequent {@link #get()}
     * calls will return @{code null}.
     */
    T getAndRemove();

    /**
     * <pre>
     * TODO: 又是CAS的原语copy。
     * 
     * 这里的官方注释是 updated value， expected value。但是实际上应该是这样。
     * 如果attribute的value == oldValue的话，设置attribute的value为newValue。
     * 否则不做操作。
     * 
     * 如果成功设置则返回true，否则返回false。
     * </pre>
     * 
     * Atomically sets the value to the given updated value if the current value == the expected value.
     * If it the set was successful it returns {@code true} otherwise {@code false}.
     */
    boolean compareAndSet(T oldValue, T newValue);

    /**
     * <pre>
     * 从AttributeMap中移除掉当前Attribute。
     * 
     * 子序列get方法调用将返回null(TODO: 这句没懂)。
     * </pre>
     * 
     * Removes this attribute from the {@link AttributeMap}.  Subsequent {@link #get()} calls will return @{code null}.
     */
    void remove();
}
