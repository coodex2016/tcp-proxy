/*
 * Copyright (c) 2018 coodex.org (jujus.shen@126.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.coodex.tcpproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.coodex.util.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.coodex.tcpproxy.ProxyHandle.getData;

public class TCPClient {

    private static final AttributeKey<Long> SESSION_KEY = AttributeKey.valueOf("sessionId");

    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final static Logger log = LoggerFactory.getLogger(TCPClient.class);
    private final String name;
    private final String address;
    private final int port;
    private final Long sessionId;
    private final Singleton<Channel> channel = new Singleton<Channel>(
            new Singleton.Builder<Channel>() {
                public Channel build() {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(workerGroup)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(new ClientHandle());
                                }
                            });
                    try {
                        ChannelFuture future = bootstrap.connect(getAddress(), getPort()).sync();
                        log.info("[{}] connect to {}:{}", getName(), getAddress(), getPort());
                        Channel channel = future.channel();
                        channel.attr(SESSION_KEY).set(sessionId);
                        return channel;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e.getLocalizedMessage(), e);
                    }
                }
            }
    );
    private final Transporter peer;

    public TCPClient(String name, String address, int port, Long sessionId, Transporter peer) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.sessionId = sessionId;
        this.peer = peer;
    }

    public void write(byte[] buf) {
        Channel channel = getChannel();
        channel.writeAndFlush(channel.alloc().buffer().writeBytes(buf));
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void close() {
        Channel channel = getChannel();
        if (channel.isOpen()) {
            channel.disconnect();
        }
    }

    private Channel getChannel() {
        return channel.getInstance();
    }

    public Long getSessionId() {
        return sessionId;
    }

    protected class ClientHandle extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            getChannel();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            peer.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            peer.write(getData((ByteBuf) msg));
            super.channelRead(ctx, msg);
        }
    }
}
