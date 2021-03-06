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
package io.netty.buffer;

import io.netty.util.ReferenceCounted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A random and sequential accessible sequence of zero or more bytes (octets).
 * This interface provides an abstract view for one or more primitive byte
 * arrays ({@code byte[]}) and {@linkplain ByteBuffer NIO buffers}.
 *
 * <h3>Creation of a buffer</h3>
 *
 * It is recommended to create a new buffer using the helper methods in
 * {@link Unpooled} rather than calling an individual implementation's
 * constructor.
 *
 * <h3>Random Access Indexing</h3>
 *
 * Just like an ordinary primitive byte array, {@link ByteBuf} uses
 * <a href="http://en.wikipedia.org/wiki/Zero-based_numbering">zero-based indexing</a>.
 * It means the index of the first byte is always {@code 0} and the index of the last byte is
 * always {@link #capacity() capacity - 1}.  For example, to iterate all bytes of a buffer, you
 * can do the following, regardless of its internal implementation:
 *
 * <pre>
 * {@link ByteBuf} buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++) {
 *     byte b = buffer.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 *
 * <h3>Sequential Access Indexing</h3>
 *
 * {@link ByteBuf} provides two pointer variables to support sequential
 * read and write operations - {@link #readerIndex() readerIndex} for a read
 * operation and {@link #writerIndex() writerIndex} for a write operation
 * respectively.  The following diagram shows how a buffer is segmented into
 * three areas by the two pointers:
 *
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * <h4>Readable bytes (the actual content)</h4>
 *
 * This segment is where the actual data is stored.  Any operation whose name
 * starts with {@code read} or {@code skip} will get or skip the data at the
 * current {@link #readerIndex() readerIndex} and increase it by the number of
 * read bytes.  If the argument of the read operation is also a
 * {@link ByteBuf} and no destination index is specified, the specified
 * buffer's {@link #writerIndex() writerIndex} is increased together.
 * <p>
 * If there's not enough content left, {@link IndexOutOfBoundsException} is
 * raised.  The default value of newly allocated, wrapped or copied buffer's
 * {@link #readerIndex() readerIndex} is {@code 0}.
 *
 * <pre>
 * // Iterates the readable bytes of a buffer.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.readable()) {
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 *
 * <h4>Writable bytes</h4>
 *
 * This segment is a undefined space which needs to be filled.  Any operation
 * whose name ends with {@code write} will write the data at the current
 * {@link #writerIndex() writerIndex} and increase it by the number of written
 * bytes.  If the argument of the write operation is also a {@link ByteBuf},
 * and no source index is specified, the specified buffer's
 * {@link #readerIndex() readerIndex} is increased together.
 * <p>
 * If there's not enough writable bytes left, {@link IndexOutOfBoundsException}
 * is raised.  The default value of newly allocated buffer's
 * {@link #writerIndex() writerIndex} is {@code 0}.  The default value of
 * wrapped or copied buffer's {@link #writerIndex() writerIndex} is the
 * {@link #capacity() capacity} of the buffer.
 *
 * <pre>
 * // Fills the writable bytes of a buffer with random integers.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.maxWritableBytes() >= 4) {
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 *
 * <h4>Discardable bytes</h4>
 *
 * This segment contains the bytes which were read already by a read operation.
 * Initially, the size of this segment is {@code 0}, but its size increases up
 * to the {@link #writerIndex() writerIndex} as read operations are executed.
 * The read bytes can be discarded by calling {@link #discardReadBytes()} to
 * reclaim unused area as depicted by the following diagram:
 *
 * <pre>
 *  BEFORE discardReadBytes()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER discardReadBytes()
 *
 *      +------------------+--------------------------------------+
 *      |  readable bytes  |    writable bytes (got more space)   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (decreased)        <=        capacity
 * </pre>
 *
 * Please note that there is no guarantee about the content of writable bytes
 * after calling {@link #discardReadBytes()}.  The writable bytes will not be
 * moved in most cases and could even be filled with completely different data
 * depending on the underlying buffer implementation.
 *
 * <h4>Clearing the buffer indexes</h4>
 *
 * You can set both {@link #readerIndex() readerIndex} and
 * {@link #writerIndex() writerIndex} to {@code 0} by calling {@link #clear()}.
 * It does not clear the buffer content (e.g. filling with {@code 0}) but just
 * clears the two pointers.  Please also note that the semantic of this
 * operation is different from {@link ByteBuffer#clear()}.
 *
 * <pre>
 *  BEFORE clear()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER clear()
 *
 *      +---------------------------------------------------------+
 *      |             writable bytes (got more space)             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 *
 * <h3>Search operations</h3>
 *
 * For simple single-byte searches, use {@link #indexOf(int, int, byte)} and {@link #bytesBefore(int, int, byte)}.
 * {@link #bytesBefore(byte)} is especially useful when you deal with a {@code NUL}-terminated string.
 * For complicated searches, use {@link #forEachByte(int, int, ByteBufProcessor)} with a {@link ByteBufProcessor}
 * implementation.
 *
 * <h3>Mark and reset</h3>
 *
 * There are two marker indexes in every buffer. One is for storing
 * {@link #readerIndex() readerIndex} and the other is for storing
 * {@link #writerIndex() writerIndex}.  You can always reposition one of the
 * two indexes by calling a reset method.  It works in a similar fashion to
 * the mark and reset methods in {@link InputStream} except that there's no
 * {@code readlimit}.
 *
 * <h3>Derived buffers</h3>
 *
 * You can create a view of an existing buffer by calling either
 * {@link #duplicate()}, {@link #slice()} or {@link #slice(int, int)}.
 * A derived buffer will have an independent {@link #readerIndex() readerIndex},
 * {@link #writerIndex() writerIndex} and marker indexes, while it shares
 * other internal data representation, just like a NIO buffer does.
 * <p>
 * In case a completely fresh copy of an existing buffer is required, please
 * call {@link #copy()} method instead.
 *
 * <h3>Conversion to existing JDK types</h3>
 *
 * <h4>Byte array</h4>
 *
 * If a {@link ByteBuf} is backed by a byte array (i.e. {@code byte[]}),
 * you can access it directly via the {@link #array()} method.  To determine
 * if a buffer is backed by a byte array, {@link #hasArray()} should be used.
 *
 * <h4>NIO Buffers</h4>
 *
 * If a {@link ByteBuf} can be converted into an NIO {@link ByteBuffer} which shares its
 * content (i.e. view buffer), you can get it via the {@link #nioBuffer()} method.  To determine
 * if a buffer can be converted into an NIO buffer, use {@link #nioBufferCount()}.
 *
 * <h4>Strings</h4>
 *
 * Various {@link #toString(Charset)} methods convert a {@link ByteBuf}
 * into a {@link String}.  Please note that {@link #toString()} is not a
 * conversion method.
 *
 * <h4>I/O Streams</h4>
 *
 * Please refer to {@link ByteBufInputStream} and
 * {@link ByteBufOutputStream}.
 */
@SuppressWarnings("ClassMayBeInterface")
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf> {

    /**
     * <pre>
     * 		返回ByteBuf的容量。
     * </pre>
     * 
     * Returns the number of bytes (octets) this buffer can contain.
     */
    public abstract int capacity();

    /**
     * <pre>	
     * 		如果newCapacity大于当前ByteBuf的大小，则截断。
     * 		否则用(newCapacity - 当前大小)作为填充放到后边。  		待确认 TODO
     * </pre>
     * 
     * Adjusts the capacity of this buffer.  If the {@code newCapacity} is less than the current
     * capacity, the content of this buffer is truncated.  If the {@code newCapacity} is greater
     * than the current capacity, the buffer is appended with unspecified data whose length is
     * {@code (newCapacity - currentCapacity)}.
     */
    public abstract ByteBuf capacity(int newCapacity);

    /**
     * <PRE>
     * 		返回当前buffer允许的最大容量。
     * 		如果用户尝试使用capacity(int),ensureWritable(int)去给buffer扩容，当到达上限
     * 		时候，会抛出IllegalArgumentException。
     * </PRE>
     * 
     * Returns the maximum allowed capacity of this buffer.  If a user attempts to increase the
     * capacity of this buffer beyond the maximum capacity using {@link #capacity(int)} or
     * {@link #ensureWritable(int)}, those methods will raise an
     * {@link IllegalArgumentException}.
     */
    public abstract int maxCapacity();

    /**
     * Returns the {@link ByteBufAllocator} which created this buffer.
     */
    public abstract ByteBufAllocator alloc();

    /**
     * <pre>
     * 		拿到字节序。 TODO 这里其实不是太懂
     * </pre>
     * 
     * Returns the <a href="http://en.wikipedia.org/wiki/Endianness">endianness</a>
     * of this buffer.
     */
    public abstract ByteOrder order();

    /**
     * Returns a buffer with the specified {@code endianness} which shares the whole region,
     * indexes, and marks of this buffer.  Modifying the content, the indexes, or the marks of the
     * returned buffer or this buffer affects each other's content, indexes, and marks.  If the
     * specified {@code endianness} is identical to this buffer's byte order, this method can
     * return {@code this}.  This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     */
    public abstract ByteBuf order(ByteOrder endianness);

    /**
     * <pre>
     * 		如果当前的ByteBuf是一个包装buffer，则返回被包装的buffer，否则返回为null。
     * </pre>
     * 
     * Return the underlying buffer instance if this buffer is a wrapper of another buffer.
     *
     * @return {@code null} if this buffer is not a wrapper
     */
    public abstract ByteBuf unwrap();

    /**
     * <pre>
     * 		当且仅当当前ByteBuf支持Nio的direct buffer的场景下才返回true。
     * </pre>
     * 
     * Returns {@code true} if and only if this buffer is backed by an
     * NIO direct buffer.
     */
    public abstract boolean isDirect();

    /**
     * <pre>
     * 		返回当前的readIndex下标。
     * </pre>
     * 
     * Returns the {@code readerIndex} of this buffer.
     */
    public abstract int readerIndex();

    /**
     * <pre>
     * 		设置当前ByteBuf下标为readerIndex，然后返回当前ByteBuf。
     * </pre>
     * 
     * Sets the {@code readerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is
     *            less than {@code 0} or
     *            greater than {@code this.writerIndex}
     */
    public abstract ByteBuf readerIndex(int readerIndex);

    /**
     * <pre>
     * 		返回当前的writeIndex下标。
     * </pre>
     * 
     * Returns the {@code writerIndex} of this buffer.
     */
    public abstract int writerIndex();

    /**
     * <pre>
     * 		设置当前ByteBuf下标为writerIndex，然后返回当前ByteBuf。
     * </pre>
     * 
     * Sets the {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code writerIndex} is
     *            less than {@code this.readerIndex} or
     *            greater than {@code this.capacity}
     */
    public abstract ByteBuf writerIndex(int writerIndex);

    /**
     * <pre>
     * 		提供了一个同时来设置read和write的index的方法。
     * 		需要注意: 
     * 			1. readIndex小于0.
     * 			2. writerIndex小于目前的readerIndex
     * 			3. writerIndex超出目前的记录
     * 		以上三种情况会抛出IndexOutOfBoundsException。
     * 
     * 		这个方法后续要好好理解。
     * </pre>
     * 
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer
     * in one shot.  This method is useful when you have to worry about the
     * invocation order of {@link #readerIndex(int)} and {@link #writerIndex(int)}
     * methods.  For example, the following code will fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 0 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.buffer(8);
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // readerIndex (2) cannot be greater than the current writerIndex (0).
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     *
     * The following code will also fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 8 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.wrappedBuffer(new byte[8]);
     *
     * // readerIndex becomes 8.
     * buf.readLong();
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // writerIndex (4) cannot be less than the current readerIndex (8).
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     *
     * By contrast, this method guarantees that it never
     * throws an {@link IndexOutOfBoundsException} as long as the specified
     * indexes meet basic constraints, regardless what the current index
     * values of the buffer are:
     *
     * <pre>
     * // No matter what the current state of the buffer is, the following
     * // call always succeeds as long as the capacity of the buffer is not
     * // less than 4.
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is less than 0,
     *         if the specified {@code writerIndex} is less than the specified
     *         {@code readerIndex} or if the specified {@code writerIndex} is
     *         greater than {@code this.capacity}
     */
    public abstract ByteBuf setIndex(int readerIndex, int writerIndex);

    /**
     * <pre>
     * 		返回当前可读的byte数。由于writer肯定在reader之前。所以返回值一般也就是writerIndex-readerIndex
     * </pre>
     * 
     * Returns the number of readable bytes which is equal to
     * {@code (this.writerIndex - this.readerIndex)}.
     */
    public abstract int readableBytes();

    /**
     * 	<pre>
     * 		返回当前可写的byte数。由于从当前位置到末尾一般都是可写的。所以返回值一般就是capacity - writerIndex。
     *  </pre> 
     * 
     * Returns the number of writable bytes which is equal to
     * {@code (this.capacity - this.writerIndex)}.
     */
    public abstract int writableBytes();

    /**
     * <pre>
     * 返回最大的可能写的byte位数。和writableBytes()不同的是， 有些buf是可以扩容的。如果可以扩容，
     * 则是用最大可能的位数(maxCapacity) - writerIndex。
     * </pre>
     * 
     * Returns the maximum possible number of writable bytes, which is equal to
     * {@code (this.maxCapacity - this.writerIndex)}.
     */
    public abstract int maxWritableBytes();

    /**
     * <pre>
     * 是否可读。可读的标准是writerIndex必须大于readerIndex。
     * 说白了就是readerIndex和wrieterIndex不在一条线上就可以。
     * </pre>
     * 
     * Returns {@code true}
     * if and only if {@code (this.writerIndex - this.readerIndex)} is greater
     * than {@code 0}.
     */
    public abstract boolean isReadable();

    /**
     * <pre>
     * 看缓冲区是否能写入size大小的位。比isReadable等于说多了一个size条件。 	
     * </pre>
     * 
     * Returns {@code true} if and only if this buffer contains equal to or more than the specified number of elements.
     */
    public abstract boolean isReadable(int size);

    /**
     * <pre>
     * 	是否可写。可写的标准是容量还有。也就是说capacity-writerIndex大于0.
     * </pre>
     * 
     * Returns {@code true}
     * if and only if {@code (this.capacity - this.writerIndex)} is greater
     * than {@code 0}.
     */
    public abstract boolean isWritable();

    /**
     * <pre>
     *  是否可写，同对应的read方法。这个也是看剩下的可写的空间够不够size大小。
     * </pre>
     * 
     * Returns {@code true} if and only if this buffer has enough room to allow writing the specified number of
     * elements.
     */
    public abstract boolean isWritable(int size);

    /**
     * <pre>
     * 设置ByteBuf的readerIndex&writerIndex都到0。 
     * clear()等同于setIndex(0, 0)， 这个很好理解。		
     * </pre>
     * 
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
     * {@code 0}.
     * This method is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
     * <p>
     * Please note that the behavior of this method is different
     * from that of NIO buffer, which sets the {@code limit} to
     * the {@code capacity} of the buffer.
     */
    public abstract ByteBuf clear();

    /**
     * <pre>
     * 标记readerIndex的位置，内部其实是用了一个markReaderIndex的值来保存这个标记位置
     * 信息的。当调用resetReaderIndex()时候，reader标记回到这里
     * </pre>
     * 
     * Marks the current {@code readerIndex} in this buffer.  You can
     * reposition the current {@code readerIndex} to the marked
     * {@code readerIndex} by calling {@link #resetReaderIndex()}.
     * The initial value of the marked {@code readerIndex} is {@code 0}.
     */
    public abstract ByteBuf markReaderIndex();

    /**
     * <pre>
     * 见markReaderIndex()方法。将readerIndex回置到mark标记位。
     * </pre>
     * 
     * Repositions the current {@code readerIndex} to the marked
     * {@code readerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code writerIndex} is less than the marked
     *         {@code readerIndex}
     */
    public abstract ByteBuf resetReaderIndex();

    /**
     * <pre>
     * 和markReaderIndex()类似。保存mark标记位置。
     * </pre>
     * 
     * Marks the current {@code writerIndex} in this buffer.  You can
     * reposition the current {@code writerIndex} to the marked
     * {@code writerIndex} by calling {@link #resetWriterIndex()}.
     * The initial value of the marked {@code writerIndex} is {@code 0}.
     */
    public abstract ByteBuf markWriterIndex();

    /**
     * <pre>
     * 返回到markWriterIndex位置。
     * </pre>
     * 
     * Repositions the current {@code writerIndex} to the marked
     * {@code writerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code readerIndex} is greater than the marked
     *         {@code writerIndex}
     */
    public abstract ByteBuf resetWriterIndex();

    /**
     * <pre>
     * 已读信息丢弃。假如目前的ByteBuf情况如下(其中括号里的3和10是我随便定义的byte数组中的位置):
     *      +-------------------+------------------+------------------+
     *      | discardable bytes |  readable bytes  |  writable bytes  |
     *      |                   |     (CONTENT)    |                  |
     *      +-------------------+------------------+------------------+
     *      |                   |                  |                  |
     *      0      <=      readerIndex(3)   <=   writerIndex(10)    <=    capacity
     *  
     *  丢弃后就会变成下边这个样子:
     *      +------------------+------------------+
     *      |  readable bytes  |  writable bytes  |
     *      |     (CONTENT)    |                  |
     *      +------------------+------------------+
     *      |                  |                  |
     *  readerIndex(3)    <=   writerIndex(10) <= capacity
     * 
     *  解释起来就是一句话。已读过的byte全部丢弃。 
     * </pre>
     * 
     * Discards the bytes between the 0th index and {@code readerIndex}.
     * It moves the bytes between {@code readerIndex} and {@code writerIndex}
     * to the 0th index, and sets {@code readerIndex} and {@code writerIndex}
     * to {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
     * <p>
     * Please refer to the class documentation for more detailed explanation.
     */
    public abstract ByteBuf discardReadBytes();

    /**
     * <pre>
     * 和discardReadBytes类似。不同的是这个方法由自己去实现，有更大的灵活性。
     * 你可以丢弃全部，一部分，或者什么都不做。
     * 
     * TODO 后续可以继续看看。
     * </pre>
     * 
     * Similar to {@link ByteBuf#discardReadBytes()} except that this method might discard
     * some, all, or none of read bytes depending on its internal implementation to reduce
     * overall memory bandwidth consumption at the cost of potentially additional memory
     * consumption.
     */
    public abstract ByteBuf discardSomeReadBytes();

    /**
     * <pre>
     * 当还有minWritableBytes个位置够可写的时候，原封不动的返回当前buf。
     * 否则抛出异常。什么时候抛出异常的简单公式描述如下:
     * writerIndex + minWritableBytes > maxCapacity
     * 
     * PS: 一定要注意这里是和maxCapacity在比较。当前buf的最大容量。
     * </pre>
     * 
     * Makes sure the number of {@linkplain #writableBytes() the writable bytes}
     * is equal to or greater than the specified value.  If there is enough
     * writable bytes in this buffer, this method returns with no side effect.
     * Otherwise, it raises an {@link IllegalArgumentException}.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @throws IndexOutOfBoundsException
     *         if {@link #writerIndex()} + {@code minWritableBytes} > {@link #maxCapacity()}
     */
    public abstract ByteBuf ensureWritable(int minWritableBytes);

    /**
     * <pre>
     * 和这个方法ensureWritable(int minWritableBytes)有点像，但是还是有区别。
     * 首先相比之前这个方法不会抛出异常。其次这个方法返回的是一个数字表示。
     * 
     * 几种场景如下:
     * 1. 当有足够的可写空间，并且容量信息不变，返回0.
     * 2. 当有足够的可写空间，容量信息发生过变化，返回2.
     * 3. 当空间不够的时候，容量有没发生变化，返回1，
     * 4. 容量已经扩充到了maxCapacity，空间仍然
     * 不够的情况下返回3. 	
     * 
     * TODO 对于3 4 两种场景不是太了解，是不是还有其他遗漏的场景?
     * </pre>
     * 
     * Tries to make sure the number of {@linkplain #writableBytes() the writable bytes}
     * is equal to or greater than the specified value.  Unlike {@link #ensureWritable(int)},
     * this method does not raise an exception but returns a code.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @param force
     *        When {@link #writerIndex()} + {@code minWritableBytes} > {@link #maxCapacity()}:
     *        <ul>
     *        <li>{@code true} - the capacity of the buffer is expanded to {@link #maxCapacity()}</li>
     *        <li>{@code false} - the capacity of the buffer is unchanged</li>
     *        </ul>
     * @return {@code 0} if the buffer has enough writable bytes, and its capacity is unchanged.
     *         {@code 1} if the buffer does not have enough bytes, and its capacity is unchanged.
     *         {@code 2} if the buffer has enough writable bytes, and its capacity has been increased.
     *         {@code 3} if the buffer does not have enough bytes, but its capacity has been
     *                   increased to its maximum.
     */
    public abstract int ensureWritable(int minWritableBytes, boolean force);

    /**
     * <p>
     * 从指定的绝对下标下获取所对应的boolean类型值。
     * </p>
     * 
     * Gets a boolean at the specified absolute (@code index) in this buffer.
     * This method does not modify the {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract boolean getBoolean(int index);

    /**
     * <p>
     * 从指定的绝对下标下获取所对应的byte类型值。
     * </p>
     * 
     * Gets a byte at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract byte  getByte(int index);

    /**
     * <pre>
     * 拿到一个无符号的指定index的下标   这个没懂  这个unsign体现在哪里。  TODO
     * </pre>
     * 
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract short getUnsignedByte(int index);

    /**
     * <pre>
     * 获取index位置的short类型。short类型是16位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+2之间的内容，不包括index+2 	
     * </pre>
     * 
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract short getShort(int index);

    /**
     * <pre>
     * 获取index位置的short类型。short类型是16位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+2之间的内容，不包括index+2 	
     * 
     * TODO 不明白这个unsign的含义
     * </pre>
     * 
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract int getUnsignedShort(int index);

    /**
     * <pre>
     * 获取index位置的medium类型。medium类型是16位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+3之间的内容，不包括index+3
     * 
     * TODO java中貌似没有medium。	
     * </pre>
     * 
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract int   getMedium(int index);

    /**
     * <pre>
     * 获取index位置的medium类型。medium类型是24位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+3之间的内容，不包括index+3
     * 
     * TODO java中貌似没有medium。	
     * </pre>
     * 
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract int   getUnsignedMedium(int index);

    /**
     * <pre>
     * 获取index位置的int类型。int类型是32位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+4之间的内容，不包括index+4
     * </pre>
     * 
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract int   getInt(int index);

    /**
     * <pre>
     * 获取index位置的int类型。int类型是32位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+4之间的内容，不包括index+4
     * 
     * TODO UNSIGN
     * </pre>
     * 
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract long  getUnsignedInt(int index);

    /**
     * <pre>
     * 获取index位置的Long类型。int类型是64位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+8之间的内容，不包括index+8
     * 
     * TODO 为什么这里又没有unsign
     * </pre>
     * 
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract long  getLong(int index);

    /**
     * <pre>
     * 获取UTF-16格式的字符。占据2位。
     * 
     * TODO 是否会涉及到字符编码的问题？还是说他就是故意要UTF-16格式的?
     * </pre>
     * 
     * Gets a 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract char  getChar(int index);

    /**
     * <pre>
     * 获取index位置的float类型。float类型是32位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+4之间的内容，不包括index+4
     * </pre>
     * 
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract float getFloat(int index);

    /**
     * <pre>
     * 获取index位置的double类型。double类型是64位。
     * ByteBuf是字节组成的buffer。1字节=8bit。
     * 所以获取的是index -> index+8之间的内容，不包括index+8
     * </pre>
     * 
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract double getDouble(int index);

    /**
     * <pre>
     * 将当前ByteBuf的内容从index开始扔到dst中去。直到dst不可写为止，返回的是dst。
     * 这会有几种可能。
     * 1. 全部写进去，dst还有可写的位置
     * 2. 刚好全部写进去
     * 3. 被截断。只有一部分被写入。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with
     * {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.writableBytes} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst);

    /**
     * <pre>
     * 和getBytes(int index, ByteBuf dst)意义差不多，不同的是getBytes方法只获取
     * 从index开始长度为length的数据到dst。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code dst.writableBytes}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int length);

    /**
     * <pre>
     * 将当前byte从index位置开始的length长度的内容复制到dst数组，并从dst的第dstIndex个下标开始。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length);

    /**
     * <pre>
     * 将当前byte从index位置开始的length长度的内容复制到dst数组，并从dst的第dstIndex个下标开始。
     * 之前的是copy到ByteBuf，这回是真的copy到一个byte数组。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.length} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, byte[] dst);

    /**
     * <pre>
     * 将当前byte从index位置开始的length长度的内容复制到dst数组，并从dst的第dstIndex个下标开始。
     * 之前的一个同样参数的方法是copy到ByteBuf，这个是真的copy到数组。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.length}
     */
    public abstract ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * <pre>
     * 拷贝byte信息到jdk-nio的ByteBuffer。从第index个下表开始，直到到达limit为止。	
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer while the destination's {@code position} will be increased.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.remaining()} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuffer dst);

    /**
     * <pre>
     * 从index开始的length长度的Byte被copy到输出流OutputStream中。
     * </pre>
     * 
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract ByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    /**
     * <pre>
     * 将当前byte的内容从下标index开始，长度为length的内容copy到GatheringByteChannel中。
     * 
     * GatheringByteChannel是什么？ TODO	
     * </pre>
     * 
     * Transfers this buffer's data to the specified channel starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    /**
     * <pre>
     * 在指定位置的下标下设置boolean类型。会占据一个字节。
     * </pre>
     * 
     * Sets the specified boolean at the specified absolute {@code index} in this
     * buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setBoolean(int index, boolean value);

    /**
     * <pre>
     * 在指定位置的下标下设置byte类型。会占据一个字节。
     * 
     * TODO  这里的value是int类型，具体实现会转换为byte??
     * </pre>
     * 
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setByte(int index, int value);

    /**
     * <pre>
     * 在指定位置的下标下设置short类型。会占据2个字节。
     * 
     * TODO 一样的问题，这里的value为什么不直接设置为short类型
     * </pre>
     * 
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setShort(int index, int value);

    /**
     * <pre>
     * 在指定位置的下标下设置"Medium"类型。会占据3个字节。
     * </pre>
     * 
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  Please note that the most significant
     * byte is ignored in the specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setMedium(int index, int   value);

    /**
     * <pre>
     * 在指定位置的下标下设置int类型。会占据4个字节。
     * </pre>
     * 
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setInt(int index, int   value);

    /**
     * <pre>
     * 在指定位置的下标下设置long类型。会占据8个字节。
     * </pre>
     * 
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setLong(int index, long  value);

    /**
     * <pre>
     * 在指定位置的下标下设置char类型。 UTF-16格式。会占据2个字节。
     * 
     * TODO: 传入参数是value?
     * </pre>
     * 
     * Sets the specified 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setChar(int index, int value);

    /**
     * <pre>
     * 在指定位置的下标下设置float类型。会占据4个字节。
     * </pre>
     * 
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setFloat(int index, float value);

    /**
     * <pre>
     * 在指定位置的下标下设置double类型。会占据8个字节。
     * </pre>
     * 
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setDouble(int index, double value);

    /**
     * <pre>
     * 将src中的内容开始复制到当前buffer(从index开始)中。
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with
     * {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.readableBytes} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src);

    /**
     * <pre>
     * 将src中的内容开始复制到当前buffer(从index开始)中, 只复制length个。
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code src.readableBytes}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int length);

    /**
     * <pre>
     * 将src中的内容从srcIndex开始复制到当前buffer(从index开始)中。
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than
     *            {@code src.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length);

    /**
     * <pre>
     * 将src数组中的内容开始复制到当前buffer(从index开始)中。
     * </pre>
     * 
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.length} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, byte[] src);

    /**
     * <pre>
     * 将src数组中的内容从srcIndex开始复制到当前buffer(从index开始)中，长度限制为length。
     * </pre>
     * 
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than {@code src.length}
     */
    public abstract ByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * <pre>
     * 将src(src是jdk-nio的ByteBuffer)中的内容开始复制到当前buffer(从index开始)中。
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.remaining()} is greater than
     *            {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuffer src);

    /**
     * <pre>
     * 将inputStream输入流中的内容开始复制到当前buffer(从index开始)中，并限定复制长度为length。。
     * </pre>
     * 
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    /**
     * <pre>
     * 将src中的内容开始复制到当前buffer(从index开始)中。
     * 
     * ScatteringByteChannel是什么。 TODO
     * </pre>
     * 
     * Transfers the content of the specified source channel to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int  setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    /**
     * <pre>
     * 把当前buffer从index开始，长度为length的内容置为0x00. 有点清空的意思。
     * </pre>
     * 
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
     * absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     */
    public abstract ByteBuf setZero(int index, int length);

    /**
     * <pre>
     * 在当前readerIndex位置获取一个boolean类型，同时readerIndex增加1.
     * </pre>
     * 
     * Gets a boolean at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract boolean readBoolean();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个byte类型，同时readerIndex增加1.
     * </pre>
     * 
     * Gets a byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract byte  readByte();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个short类型，同时readerIndex增加1.
     * 
     * 不明白这里的Unsigned的含义  TODO
     * </pre>
     * 
     * Gets an unsigned byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract short readUnsignedByte();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个short类型，同时readerIndex增加2.
     * </pre>
     * 
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract short readShort();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个short类型，同时readerIndex增加2.
     * 
     * TODO UNSIGN
     * </pre>
     * 
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract int   readUnsignedShort();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个medium类型(int)，同时readerIndex增加3.
     * </pre>
     * 
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readMedium();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个medium类型(int)，同时readerIndex增加3.
     * 
     * TODO UNSIGN
     * </pre>
     * 
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffexr.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */
    public abstract int   readUnsignedMedium();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个int类型，同时readerIndex增加4.
     * </pre>
     * 
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract int   readInt();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个int类型，同时readerIndex增加4.
     * </pre>
     * 
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract long  readUnsignedInt();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个long类型，同时readerIndex增加8.
     * </pre>
     * 
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract long  readLong();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个UTF-16的char类型，同时readerIndex增加2.
     * </pre>
     * 
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract char  readChar();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个float类型(int)，同时readerIndex增加4.
     * </pre>
     * 
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract float readFloat();

    /**
     * <pre>
     * 在当前readerIndex位置获取一个double类型(int)，同时readerIndex增加3.
     * </pre>
     * 
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract double readDouble();

    /**
     * <pre>
     * 这个方法做了以下事情。
     * 1. 新创建了一个buffer。
     * 2. 把源buffer从当前的readerIndex开始一直拷贝length长度的内容到新的buffer。
     * 3. 源buffer的长度对应的变了(readerInde+length)。
     * 4. 新的buffer的readerIndex是0. writerIndex就是length，即这个buffer的长度。
     * </pre>
     * 
     * Transfers this buffer's data to a newly created buffer starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * The returned buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and {@code length} respectively.
     *
     * @param length the number of bytes to transfer
     *
     * @return the newly created buffer which contains the transferred bytes
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(int length);

    /**
     * <pre>
     * 切片返回。
     * 从当前的readerIndex开始，返回长度为length的切片。切片buffer是新建的。
     * </pre>
     * 
     * Returns a new slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     *
     * @param length the size of the new slice
     *
     * @return the newly created slice
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf readSlice(int length);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到dst，直到dst不可写为止。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination becomes
     * non-writable, and increases the {@code readerIndex} by the number of the
     * transferred bytes.  This method is basically same with
     * {@link #readBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code writerIndex} of the destination by the number of
     * the transferred bytes while {@link #readBytes(ByteBuf, int, int)}
     * does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.writableBytes} is greater than
     *            {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuf dst);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到dst，一共写length个长度。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #readBytes(ByteBuf, int, int)},
     * except that this method increases the {@code writerIndex} of the
     * destination by the number of the transferred bytes (= {@code length})
     * while {@link #readBytes(ByteBuf, int, int)} does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes} or
     *         if {@code length} is greater than {@code dst.writableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int length);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到dst(从dst的dstIndex开始)，直到dst不可写为止。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int dstIndex, int length);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到dst数组。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code dst.length}).
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(byte[] dst);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到dst(从dst的dstIndex开始)。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than {@code dst.length}
     */
    public abstract ByteBuf readBytes(byte[] dst, int dstIndex, int length);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始一直写到nioBuffer，直到nioBuffer的限度到limit为止。
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination's position
     * reaches its limit, and increases the {@code readerIndex} by the
     * number of the transferred bytes.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.remaining()} is greater than
     *            {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuffer dst);

    /**
     * <pre>
     * 将当前的buffer内容从readerIndex开始,一共写length长度到输出流OutputStream
     * 其中当前buffer的readerIndex值也会增长。
     * </pre>
     * 
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract ByteBuf readBytes(OutputStream out, int length) throws IOException;

    /**
     * TODO 不懂
     * 
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int  readBytes(GatheringByteChannel out, int length) throws IOException;

    /**
     * <pre>
     * 字面意思即为跳过。
     * 实际上就是将readerIndex增length个长度。
     * </pre>
     * 
     * Increases the current {@code readerIndex} by the specified
     * {@code length} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract ByteBuf skipBytes(int length);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个boolean值。
     * boolean占用一个字节，所以writerIndex插入后+1.
     * </pre>
     * 
     * Sets the specified boolean at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 1}
     */
    public abstract ByteBuf writeBoolean(boolean value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个byte值。
     * byte占用一个字节，所以writerIndex插入后+1.
     * 
     * TODO 下边这句话不知道怎么理解。
     * The 24 high-order bits of the specified value are ignored.
     * </pre>
     * 
     * Sets the specified byte at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * The 24 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 1}
     */
    public abstract ByteBuf writeByte(int value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个short值。
     * short占用一个字节，所以writerIndex插入后+2.
     * 
     * TODO 下边这句话不知道怎么理解。
     * The 24 high-order bits of the specified value are ignored.
     * </pre>
     * 
     * Sets the specified 16-bit short integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ByteBuf writeShort(int value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个medium类型。
     * medium占用3个字节，所以writerIndex插入后+3.
     * </pre>
     * 
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
     * in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 3}
     */
    public abstract ByteBuf writeMedium(int   value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个int值。
     * int占用4个字节，所以writerIndex插入后+4.
     * </pre>
     * 
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ByteBuf writeInt(int   value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个long值。
     * long占用8个字节，所以writerIndex插入后+8.
     * </pre>
     * 
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ByteBuf writeLong(long  value);

    /**
     * <pre>
     * 设定2字节的UTF-16编码的字符到当前writerIndex位置
     * 
     * TODO 下边这句话不知道怎么理解。
     * The 16 high-order bits of the specified value are ignored.
     * </pre>
     * 
     * Sets the specified 2-byte UTF-16 character at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ByteBuf writeChar(int value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个float值。
     * float占用4个字节，所以writerIndex插入后+4.
     * </pre>
     * 
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 4}
     * in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ByteBuf writeFloat(float value);

    /**
     * <pre>
     * 在当前writerIndex为止插入一个double值。
     * double占用8个字节，所以writerIndex插入后+8.
     * </pre>
     * 
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ByteBuf writeDouble(double value);

    /**
     * <pre>
     * 将src的数据全部复制到当前buffer的末尾。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(ByteBuf, int, int)}
     * does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.readableBytes} is greater than
     *            {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuf src);

    /**
     * <pre>
     * 将src的数据前length个长度复制到当前buffer的末尾。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(ByteBuf, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(ByteBuf, int, int)} does not.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes} or
     *         if {@code length} is greater then {@code src.readableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int length);

    /**
     * <pre>
     * 将src的数据从srcIndex开始 全部复制到当前buffer的末尾。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than
     *            {@code src.capacity}, or
     *         if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int srcIndex, int length);

    /**
     * <pre>
     * 将src数组的数据全部复制到当前buffer的末尾。同时也增大了当前buffer的writerIndex(增大数量为src.length)。	
     * </pre>
     * 
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code src.length}).
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(byte[] src);

    /**
     * <pre>
     * 将src的数据从srcIndex开始复制length个长度到当前buffer的末尾。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than
     *            {@code src.length}, or
     *         if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(byte[] src, int srcIndex, int length);

    /**
     * <pre>
     * 将src的数据复制到当前buffer的末尾(直到ByteBuffer的pos到达limit。)。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code src.remaining()} is greater than
     *            {@code this.writableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuffer src);

    /**
     * <pre>
     * 将给定的输入流的数据的前length个长度复制到当前buffer的末尾。同时也增大了当前buffer的writerIndex。	
     * </pre>
     * 
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified stream
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */
    public abstract int  writeBytes(InputStream in, int length) throws IOException;

    /**
     * TODO
     * 
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */
    public abstract int  writeBytes(ScatteringByteChannel in, int length) throws IOException;

    /**
     * <pre>
     * 从writerIndex开始一直length个长度的位置都设置0x00.
     * </pre>
     * 
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ByteBuf writeZero(int length);

    /**
     * <pre>
     * 找到value在fromInde - toIndex之间第一次出现的位置索引。
     * 
     * 如果fromIndex > toIndex则会翻转搜索?(这个要实际看看是否和我想的一样) TODO
     * </pre>
     * 
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the specified {@code fromIndex}
     * (inclusive)  to the specified {@code toIndex} (exclusive).
     * <p>
     * If {@code fromIndex} is greater than {@code toIndex}, the search is
     * performed in a reversed order.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the absolute index of the first occurrence if found.
     *         {@code -1} otherwise.
     */
    public abstract int indexOf(int fromIndex, int toIndex, byte value);

    /**
     * <pre>
     * 这个方法的名字感觉会造成歧义。本身的含义是:
     * 
     * 返回在readerIndex - writerIndex之间第一次出现的value的下标索引，其中要返回的下标索引
     * 包括readerIndex但是不包括writerIndex。
     * 
     * TODO 如果两个index相等怎么办?
     * </pre>
     * 
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the current {@code readerIndex}
     * (inclusive) to the current {@code writerIndex} (exclusive).
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     */
    public abstract int bytesBefore(byte value);

    /**
     * <pre>
     * 返回从readerIndex - (readerIndex+length) 之间，value第一次出现的下标索引。
     * </pre>
     * 
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the current {@code readerIndex}
     * (inclusive) and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public abstract int bytesBefore(int length, byte value);

    /**
     * <pre>
     * 从当前bufer的index - index+length中选出value出现的第一次的下标。
     * </pre>
     * 
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the specified {@code index} (inclusive)
     * and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the specified {@code index}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code index + length} is greater than {@code this.capacity}
     */
    public abstract int bytesBefore(int index, int length, byte value);

    /**
     * TODO 没懂
     * 
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in ascending order.
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the readable bytes.
     *         The last-visited index If the {@link ByteBufProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByte(ByteBufProcessor processor);

    /**
     * TODO 没懂
     * 
     * Iterates over the specified area of this buffer with the specified {@code processor} in ascending order.
     * (i.e. {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the specified area.
     *         The last-visited index If the {@link ByteBufProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByte(int index, int length, ByteBufProcessor processor);

    /**
     * TODO 没懂
     * 
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in descending order.
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the readable bytes.
     *         The last-visited index If the {@link ByteBufProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByteDesc(ByteBufProcessor processor);

    /**
     * TODO 没懂
     * 
     * Iterates over the specified area of this buffer with the specified {@code processor} in descending order.
     * (i.e. {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})
     *
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the specified area.
     *         The last-visited index If the {@link ByteBufProcessor#process(byte)} returned {@code false}.
     */
    public abstract int forEachByteDesc(int index, int length, ByteBufProcessor processor);

    /**
     * <pre>
     * 把当前的buf的readerIndex - writerIndex之间的内容复制到一个新的ByteBuf。
     * </pre>
     * 
     * Returns a copy of this buffer's readable bytes.  Modifying the content
     * of the returned buffer or this buffer does not affect each other at all.
     * This method is identical to {@code buf.copy(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuf copy();

    /**
     * <pre>
     * 把当前的buf的readerIndex - writerIndex之间的内容取一个子区间(index, length)复制到一个新的ByteBuf。
     * </pre>
     * 
     * Returns a copy of this buffer's sub-region.  Modifying the content of
     * the returned buffer or this buffer does not affect each other at all.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuf copy(int index, int length);

    /**
     * <pre>
     * 返回一个当前buffer可读的切片。也即是readerIndex - writerIndex之间。
     * </pre>
     * 
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuf slice();

    /**
     * <pre>
     * 返回一个当前buffer可读的切片。也即是readerIndex - writerIndex之间的子区间，从index开始，取length个长度。。
     * </pre>
     * 
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuf slice(int index, int length);

    /**
     * TODO 没懂
     * 
     * Returns a buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method is identical to {@code buf.slice(0, buf.capacity())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuf duplicate();

    /**
     * Returns the maximum number of NIO {@link ByteBuffer}s that consist this buffer.  Note that {@link #nioBuffers()}
     * or {@link #nioBuffers(int, int)} might return a less number of {@link ByteBuffer}s.
     *
     * @return {@code -1} if this buffer has no underlying {@link ByteBuffer}.
     *         the number of the underlying {@link ByteBuffer}s if this buffer has at least one underlying
     *         {@link ByteBuffer}.  Note that this method does not return {@code 0} to avoid confusion.
     *
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract int nioBufferCount();

    /**
     * TODO 没懂
     * 
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}.  The returned buffer
     * shares the content with this buffer, while changing the position and limit of the returned
     * NIO buffer does not affect the indexes and marks of this buffer.  This method is identical
     * to {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())}.  This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.  Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer();

    /**
     * TODO 没懂
     * 
     * Exposes this buffer's sub-region as an NIO {@link ByteBuffer}.  The returned buffer
     * shares the content with this buffer, while changing the position and limit of the returned
     * NIO buffer does not affect the indexes and marks of this buffer.  This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.  Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer(int index, int length);

    /**
     * TODO  不懂 内部使用?
     * 
     * Internal use only: Exposes the internal NIO buffer.
     */
    public abstract ByteBuffer internalNioBuffer(int index, int length);

    /**
     * TODO 不懂
     * 
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}'s.  The returned buffer
     * shares the content with this buffer, while changing the position and limit of the returned
     * NIO buffer does not affect the indexes and marks of this buffer. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.  Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    public abstract ByteBuffer[] nioBuffers();

    /**
     * TODO 不懂
     * 
     * Exposes this buffer's bytes as an NIO {@link ByteBuffer}'s for the specified index and length
     * The returned buffer shares the content with this buffer, while changing the position and limit
     * of the returned NIO buffer does not affect the indexes and marks of this buffer. This method does
     * not modify {@code readerIndex} or {@code writerIndex} of this buffer.  Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    public abstract ByteBuffer[] nioBuffers(int index, int length);

    /**
     * <pre>
     * 没有恰当的理解这个翻译。  TODO
     * 
     * 当前仅当当前buffer有后备字节数组的时候返回true。 (怎么理解这个backing byte array)
     * 如果这个方法可以返回true，你就可以安全的调用array()方法和arrayOffset()方法啦。
     * </pre>
     * 
     * Returns {@code true} if and only if this buffer has a backing byte array.
     * If this method returns true, you can safely call {@link #array()} and
     * {@link #arrayOffset()}.
     */
    public abstract boolean hasArray();

    /**
     * <pre>
     * 返回当前buffer的后备字节数组。     TODO 同上，这个backing byte array难倒了我。TODO
     * </pre>
     * 
     * Returns the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */
    public abstract byte[] array();

    /**
     * <pre>
     * 什么什么偏移量?都翻译不出来，擦。TODO
     * </pre>
     * 
     * Returns the offset of the first byte within the backing byte array of
     * this buffer.
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */
    public abstract int arrayOffset();

    /**
     * <pre>
     * 当且仅当当前buffer有一个指向后备字节数组的低级别的内存地址的引用时候返回true。 尼玛。这个更难懂 。TODO
     * </pre>
     * 
     * Returns {@code true} if and only if this buffer has a reference to the low-level memory address that points
     * to the backing data.
     */
    public abstract boolean hasMemoryAddress();

    /**
     * <pre>
     * 返回指向后备字节数组的第一个字节的低级别内存地址。 TODO
     * </pre>
     * 
     * Returns the low-level memory address that point to the first byte of ths backing data.
     *
     * @throws UnsupportedOperationException
     *         if this buffer does not support accessing the low-level memory address
     */
    public abstract long memoryAddress();

    /**
     * <pre>
     * 将当前buffer从readerIndex开始一直到不能读位置的这段byte信息用指定的字符集输出为字符串。
     * </pre>
     * 
     * Decodes this buffer's readable bytes into a string with the specified
     * character set name.  This method is identical to
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws UnsupportedCharsetException
     *         if the specified character set name is not supported by the
     *         current VM
     */
    public abstract String toString(Charset charset);

    /**
     * <pre>
     * 将当前buffer从readerIndex开始一直到不能读位置的这段byte信息用指定的字符集输出为字符串。
     * 不过这个方法返回的等于说是上边的一个自己，他自己限定了参数index和length。
     * </pre>
     * 
     * Decodes this buffer's sub-region into a string with the specified
     * character set.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     */
    public abstract String toString(int index, int length, Charset charset);

    /**
     * <pre>
     * ByteBuf的hashCode方法。
     * </pre>
     * 
     * Returns a hash code which was calculated from the content of this
     * buffer.  If there's a byte array which is
     * {@linkplain #equals(Object) equal to} this array, both arrays should
     * return the same value.
     */
    @Override
    public abstract int hashCode();

    /**
     * <pre>
     * equals对象。
     * 两个ByteBuf如果要equals需要满足下边2个条件。
     * 1. size相等。
     * 2. 每个byte的内容相等。
     * 
     * ps:该方法不会去比较readerIndex和writerIndex。
     * </pre>
     * 
     * Determines if the content of the specified buffer is identical to the
     * content of this array.  'Identical' here means:
     * <ul>
     * <li>the size of the contents of the two buffers are same and</li>
     * <li>every single byte of the content of the two buffers are same.</li>
     * </ul>
     * Please note that it does not compare {@link #readerIndex()} nor
     * {@link #writerIndex()}.  This method also returns {@code false} for
     * {@code null} and an object which is not an instance of
     * {@link ByteBuf} type.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * <pre>
     * 比较方法。但是接口没有说的很明白该怎么比较。还是说我没看懂? TODO
     * </pre>
     * 
     * Compares the content of the specified buffer to the content of this
     * buffer.  Comparison is performed in the same manner with the string
     * comparison functions of various languages such as {@code strcmp},
     * {@code memcmp} and {@link String#compareTo(String)}.
     */
    @Override
    public abstract int compareTo(ByteBuf buffer);

    /**
     * <pre>
     * 普通的toString()方法。当前方法不需要返回整个buffer的内容，但需要返回一些关键
     * 的信息，比如readerIndex, writerIndex, capacity等等。
     * </pre>
     * 
     * Returns the string representation of this buffer.  This method does not
     * necessarily return the whole content of the buffer but returns
     * the values of the key properties such as {@link #readerIndex()},
     * {@link #writerIndex()} and {@link #capacity()}.
     */
    @Override
    public abstract String toString();

    /**
     * TODO 没注释 我TM也没看懂
     */
    @Override
    public abstract ByteBuf retain(int increment);

    /**
     * TODO 没注释暂时没看懂
     */
    @Override
    public abstract ByteBuf retain();
}
