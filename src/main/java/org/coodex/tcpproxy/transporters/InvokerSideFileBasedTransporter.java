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

import org.coodex.tcpproxy.TCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.coodex.tcpproxy.TcpProxyCommon.profile;

public class InvokerSideFileBasedTransporter extends AbstractFileBasedTransporter {
    private final static Logger log = LoggerFactory.getLogger(InvokerSideFileBasedTransporter.class);

    private final TCPClient client;
    private final Long sessionId;
    private final int blockSize;

    public InvokerSideFileBasedTransporter(String name, Encryptor encryptor, Long sessionId) {
        super(name,
                profile.getString(name + ".remote.path.write"),
                encryptor,
                profile.getInt(name + ".remote.timeout", 2000));
        this.sessionId = sessionId;
        this.blockSize = profile.getInt(name + ".remote.blockSize", 2048);
        client = new TCPClient(name,
                profile.getString(name + ".remote.address"),
                profile.getInt(name + ".remote.port"),
                getId(), this);
    }

    @Override
    protected Type getType() {
        return Type.RESPONSE;
    }

    @Override
    protected int getBlockSize() {
        return blockSize;
    }

    @Override
    protected void closeTransporter() {
        client.close();
    }

    @Override
    public void read(byte[] buf) {
        log.debug("send to: {}:{}, bytes:{}", client.getAddress(), client.getPort(), buf.length);
        client.write(buf);
    }

    @Override
    public Long getId() {
        return sessionId;
    }

}
