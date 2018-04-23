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

import org.coodex.util.SingletonMap;

import static org.coodex.tcpproxy.TcpProxyCommon.encryptors;
import static org.coodex.tcpproxy.TcpProxyCommon.profile;

public class InvokerSideFileListener extends AbstractFileListener {

    private SingletonMap<Long, AbstractFileBasedTransporter> transporters
            = new SingletonMap<>(new SingletonMap.Builder<Long, AbstractFileBasedTransporter>() {
        @Override
        public AbstractFileBasedTransporter build(Long key) {

            return new InvokerSideFileBasedTransporter(
                    getName(), encryptors.getInstance(
                    profile.getString(getName() + ".remote.encryptor")), key
            ) {
                @Override
                protected void closeTransporter() {
                    transporters.remove(getId());
                    super.closeTransporter();
                }
            };
        }
    });

    public InvokerSideFileListener(String name, int timeOut) {
        super(name, timeOut);
    }

    @Override
    protected String suffix() {
        return "req";
    }

    @Override
    protected AbstractFileBasedTransporter getTransporterById(Long id) {
        return transporters.getInstance(id);
    }
}
