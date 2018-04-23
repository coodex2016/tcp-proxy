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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 代理服务端的handle，收到socket传来的数据时，向传输器方向写入数据，socket inactive时断开传输器
 */
public class ProxyHandle extends ChannelInboundHandlerAdapter {

    public final static AttributeKey<Transporter> TRANSPORTER_ATTRIBUTE_KEY = AttributeKey.valueOf("sessionId");
    private final static Logger log = LoggerFactory.getLogger(ProxyHandle.class);
    private final TransporterFactory transporterFactory;
    private final String name;

    public ProxyHandle(TransporterFactory transporterFactory, String name) {
        this.transporterFactory = transporterFactory;
        this.name = name;
    }

    public static byte[] getData(ByteBuf byteBuf) {
        byte[] buf = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buf);
        return buf;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        try {
            Transporter transporter = transporterFactory.build(name, ctx);
            ctx.channel().attr(TRANSPORTER_ATTRIBUTE_KEY).set(transporter);
        } catch (Throwable th) {
            log.warn(th.getLocalizedMessage(), th);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Transporter transporter = ctx.channel().attr(TRANSPORTER_ATTRIBUTE_KEY).get();
        super.channelInactive(ctx);
        transporter.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Transporter transporter = ctx.channel().attr(TRANSPORTER_ATTRIBUTE_KEY).get();
        transporter.write(getData((ByteBuf) msg));
        super.channelRead(ctx, msg);
    }

}
