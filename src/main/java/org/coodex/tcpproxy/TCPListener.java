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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.coodex.util.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPListener {

    private final static Logger log = LoggerFactory.getLogger(TCPListener.class);


    private String address = null;
    private Integer port = null;
    private TransporterFactory transporterFactory = null;
    private boolean running = false;
    private String name = "proxy";

    public TCPListener(String address, Integer port) {
        this.address = address;
        this.port = port;
    }


    private static TransporterFactory getInstance(String name) {
        try {
            return (TransporterFactory) Class.forName(name).getConstructor().newInstance();
        } catch (Throwable th) {
            throw th instanceof RuntimeException ? (RuntimeException) th : new RuntimeException(th.getLocalizedMessage(), th);
        }
    }


    public static void main(String[] args) {
        Profile profile = Profile.getProfile("proxy.properties");
        if (args.length == 0) args = new String[]{"proxy"};
        for (String arg : args) {
            new TCPListener(profile.getString(arg + ".listener.address"),
                    profile.getInt(arg + ".listener.port"))
                    .setName(arg)
                    .setTransporterFactory(
                            getInstance(profile.getString(arg + ".listener.factory")))
                    .start();
        }
    }

    public TCPListener setTransporterFactory(TransporterFactory transporterFactory) {
        this.transporterFactory = transporterFactory;
        return this;
    }

    public TCPListener setName(String name) {
        this.name = name;
        return this;
    }

    public void start() {
        if (!running) {
            synchronized (this) {
                if (!running) {
                    if (transporterFactory == null)
                        throw new NullPointerException("transporterFactory not set.");
                    running = true;
                    new Thread() {
                        @Override
                        public void run() {
                            EventLoopGroup bossGroup = new NioEventLoopGroup();
                            EventLoopGroup workerGroup = new NioEventLoopGroup();
                            try {
                                ServerBootstrap b = new ServerBootstrap();
                                ChannelFuture future = b.group(bossGroup, workerGroup)
                                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                                        .channel(NioServerSocketChannel.class)
                                        .childHandler(new ChannelInitializer<SocketChannel>() {
                                            protected void initChannel(SocketChannel ch) throws Exception {
                                                ch.pipeline().addLast(
                                                        new ProxyHandle(transporterFactory, name)
                                                );
                                            }
                                        })
                                        .bind(address, port).sync();

                                log.info("proxy listening at {}:{}, using {}", address, port,
                                        transporterFactory.getClass().getName());

                                future.channel().closeFuture().sync();
                                log.info("proxy listener {}:{} shutdown.", address, port);
                            } catch (InterruptedException e) {
                                log.warn(e.getLocalizedMessage(), e);
                            } finally {
                                bossGroup.shutdownGracefully();
                                workerGroup.shutdownGracefully();
                                running = false;
                            }
                        }
                    }.start();
                }
            }
        }

    }
}
