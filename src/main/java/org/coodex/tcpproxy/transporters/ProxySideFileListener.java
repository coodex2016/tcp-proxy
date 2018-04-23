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

import java.util.HashMap;
import java.util.Map;

public class ProxySideFileListener extends AbstractFileListener {

    private Map<Long, AbstractFileBasedTransporter> transporters = new HashMap<>();

    protected ProxySideFileListener(String name, int timeOut) {
        super(name, timeOut);
    }

    @Override
    protected String suffix() {
        return "res";
    }

    @Override
    protected AbstractFileBasedTransporter getTransporterById(Long id) {
        return transporters.get(id);
    }

    void remove(Long id) {
        transporters.remove(id);
    }

    void put(Long id, AbstractFileBasedTransporter transporter) {
        transporters.put(id, transporter);
    }
}
