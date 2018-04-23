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

import java.util.concurrent.atomic.AtomicLong;

public class ProxySideFileBasedTransporter extends AbstractFileBasedTransporter {
    private static AtomicLong idSeq = new AtomicLong(0);
    private final int blockSize;
    private final Channel channel;
    private final Long id = idSeq.incrementAndGet();

    public ProxySideFileBasedTransporter(String name, String path, Encryptor encryptor, int blockSize, Channel channel, int timeOut) {
        super(name, path, encryptor, timeOut);
        this.blockSize = blockSize;
        this.channel = channel;
    }

    @Override
    protected Type getType() {
        return Type.REQUEST;
    }

    @Override
    protected int getBlockSize() {
        return blockSize;
    }

    @Override
    protected void closeTransporter() {
        if (channel.isOpen())
            channel.disconnect();
    }

    @Override
    public void read(byte[] buf) {
        channel.writeAndFlush(channel.alloc().buffer().writeBytes(buf));
    }

    @Override
    public Long getId() {
        return id;
    }
}
