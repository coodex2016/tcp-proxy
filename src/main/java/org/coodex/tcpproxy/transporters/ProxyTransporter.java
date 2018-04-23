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

package org.coodex.tcpproxy.transporters;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.coodex.tcpproxy.TCPClient;

import java.util.concurrent.atomic.AtomicLong;

public class ProxyTransporter extends AbstractTransporter {
    private final AtomicLong idSeq = new AtomicLong();

    private final Channel channel;
    private final TCPClient client;
    private final Long sessionId = idSeq.incrementAndGet();

    public ProxyTransporter(String name, final ChannelHandlerContext ctx, String address, int port) {
        super(name);

        this.channel = ctx.channel();
        this.client = new TCPClient(name, address, port, sessionId, new AbstractTransporter(name) {
            @Override
            public void read(byte[] buf) {
                ProxyTransporter.this.write(buf);
            }

            @Override
            public void write(byte[] buf) {
                ProxyTransporter.this.read(buf);
            }

            @Override
            public void close() {
                if (channel.isOpen())
                    channel.close();
            }

            @Override
            public Long getId() {
                return sessionId;
            }
        });
    }


    @Override
    public void read(byte[] buf) {
        channel.writeAndFlush(channel.alloc().buffer().writeBytes(buf));
    }

    @Override
    public void write(byte[] buf) {
        client.write(buf);
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public Long getId() {
        return sessionId;
    }
}
