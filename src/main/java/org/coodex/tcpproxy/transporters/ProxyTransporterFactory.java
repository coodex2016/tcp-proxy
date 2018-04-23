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
import org.coodex.util.Profile;

import static org.coodex.tcpproxy.TcpProxyCommon.profile;

public class ProxyTransporterFactory implements TransporterFactory {



    public Transporter build(String name, ChannelHandlerContext ctx) {
        return new ProxyTransporter(name, ctx,
                profile.getString(name + ".remote.address"),
                profile.getInt(name + ".remote.port"));
    }
}
