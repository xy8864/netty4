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
 * ������˼�ܺ���⡣���Ǵ�����һ�����ԡ�ֻ������netty���ö���
 * Attribute�����档
 * 
 * ��ǰ���Ա���������洢ֵ������(TODO�� ������������)��
 * �������Զ������������̰߳�ȫ��(TODO: WHY???)��
 * </pre>
 * 
 * An attribute which allows to store a value reference. It may be updated atomically and so is thread-safe.
 *
 * @param <T>   the type of the value it holds.
 */
public interface Attribute<T> {

    /**
     * <pre>
     * ���ص�ǰ���Զ�Ӧ��KEY, ��AttributeKey����
     * </pre>
     * 
     * Returns the key of this attribute.
     */
    AttributeKey<T> key();

    /**
     * <pre>
     * ���ص�ǰ���Զ�Ӧ��ʵ�ʰ�����ֵ������Ϊ��!
     * </pre>
     * 
     * Returns the current value, which may be {@code null}
     */
    T get();

    /**
     * <pre>
     * ���õ�ǰ���Ե�valueֵ��	
     * </pre>
     * 
     * Sets the value
     */
    void set(T value);

    /**
     * <pre>
     * �Զ�����valueΪ��ǰֵ�������ϵ�valueֵ�����֮ǰ��û�����û᷵��null��
     * 
     * TODO �������������JUC��Automaic��ķ�������Ҫ��ע��
     * </pre>  
     * 
     * Atomically sets to the given value and returns the old value which may be {@code null} if non was set before.
     */
    T getAndSet(T value);

    /**
     *  <pre>
     *  �����ǰattribute��û��ֵ���ͰѲ�����value��ֵΪ��ǰ���Ե�ֵ��
     *  ����ʲô��������ֻ�ǰѵ�ǰ�е�ֵ���ء�
     * 
     *  TODO �������������JUC��Automaic��ķ�������Ҫ��ע��
     *  </pre>
     * 
     *  Atomically sets to the given value if this {@link Attribute} does not contain a value at the moment.
     *  If it was not possible to set the value as it contains a value it will just return the current value.
     */
    T setIfAbsent(T value);

    /**
     * <pre>
     * ��AttributeMap���Ƴ�����ǰAttribute��������Attribute�е�value��
     * 
     * ������get�������ý�����null(TODO: ���û��)��
     * </pre>
     * 
     * Removes this attribute from the {@link AttributeMap} and returns the old value..  Subsequent {@link #get()}
     * calls will return @{code null}.
     */
    T getAndRemove();

    /**
     * <pre>
     * TODO: ����CAS��ԭ��copy��
     * 
     * ����Ĺٷ�ע���� updated value�� expected value������ʵ����Ӧ����������
     * ���attribute��value == oldValue�Ļ�������attribute��valueΪnewValue��
     * ������������
     * 
     * ����ɹ������򷵻�true�����򷵻�false��
     * </pre>
     * 
     * Atomically sets the value to the given updated value if the current value == the expected value.
     * If it the set was successful it returns {@code true} otherwise {@code false}.
     */
    boolean compareAndSet(T oldValue, T newValue);

    /**
     * <pre>
     * ��AttributeMap���Ƴ�����ǰAttribute��
     * 
     * ������get�������ý�����null(TODO: ���û��)��
     * </pre>
     * 
     * Removes this attribute from the {@link AttributeMap}.  Subsequent {@link #get()} calls will return @{code null}.
     */
    void remove();
}
