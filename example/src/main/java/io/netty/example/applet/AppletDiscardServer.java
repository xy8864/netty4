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
package io.netty.example.applet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup; 
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import javax.swing.JApplet;

/**
 * {@link JApplet} which starts up a Server that receive data and discard it.
 */
public class AppletDiscardServer extends JApplet {

    private static final long serialVersionUID = -7824894101960583175L;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * applet������ʼ��
     */
    @Override
    public void init() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
        	// ServerBootstrap��Ĭ�Ϲ�����ʲôҲû����
        	// ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel>
            ServerBootstrap bootstrap = new ServerBootstrap();
            
            // group������ʵ�����������������е�boss�߳�������worker�߳������
            // channel������Ҫ���������õ�ǰ�����channel����
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)								// why need a class ?
                     .childHandler(new ChannelInitializer<SocketChannel>() {			// ��ʱ�������Ϊ����ǰ��channel�򵥵ĳ�ʼ��
                         @Override
                         public void initChannel(SocketChannel ch) throws Exception {
                             ch.pipeline().addLast(new DiscardServerHandler());			// ��ʼ��: add some handler to pipeline
                         }
                     });
            ChannelFuture f = bootstrap.bind(9999).sync();				// bind port to 9999. SYNC:DefaultChannelPromise
            f.channel().closeFuture().sync();							// ������2��sync��Ϊ�˱�֤�÷������ꣿ ��Ȼ�����ͬ�����ܻ�����������
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    private static final class DiscardServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            System.out.println("Received: " + msg.toString(CharsetUtil.UTF_8));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
