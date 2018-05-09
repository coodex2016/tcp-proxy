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

import io.netty.channel.ChannelHandlerContext;
import org.coodex.tcpproxy.Transporter;
import org.coodex.tcpproxy.TransporterFactory;
import org.coodex.util.SingletonMap;

import static org.coodex.tcpproxy.TcpProxyCommon.encryptors;
import static org.coodex.tcpproxy.TcpProxyCommon.profile;

public class FileBasedTransporterFactory implements TransporterFactory {

    private SingletonMap<String, ProxySideFileListener> listeners = new SingletonMap<>(
            new SingletonMap.Builder<String, ProxySideFileListener>() {
                @Override
                public ProxySideFileListener build(String key) {
                    String path = profile.getString(key + ".listener.path.read");
                    ProxySideFileListener listener =
                            new ProxySideFileListener(key,
                                    profile.getInt(key + ".listener.timeout"));
                    FileWatching.getInstance().register(path, listener);
                    return listener;
                }
            }
    );

    @Override
    public Transporter build(final String name, final ChannelHandlerContext ctx) {
        final int blockSize = profile.getInt(name + ".listener.blockSize", 2048);

        ProxySideFileBasedTransporter transporter =
                new ProxySideFileBasedTransporter(name,
                        profile.getString(name + ".listener.path.write"),
                        encryptors.getInstance(profile.getString(name + ".listener.encryptor")),
                        blockSize,
                        ctx.channel(),
                        profile.getInt(name + ".listener.timeout")) {
                    @Override
                    protected void closeTransporter() {
                        listeners.getInstance(name).remove(getId());
                        super.closeTransporter();
                    }
                };
        listeners.getInstance(name).put(transporter.getId(), transporter);
        return transporter;
    }
}
