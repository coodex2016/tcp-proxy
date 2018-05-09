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

import org.coodex.tcpproxy.transporters.FileListener;
import org.coodex.tcpproxy.transporters.FileWatching;
import org.coodex.tcpproxy.transporters.InvokerSideFileListener;

import static org.coodex.tcpproxy.TcpProxyCommon.profile;

public class InvokerSideDamon {

    private final String name;

    public InvokerSideDamon(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"proxy"};
        }
        for (String arg : args) {
            new InvokerSideDamon(arg).start();
        }
    }

    public void start() {
        FileListener listener = new InvokerSideFileListener(name,
                profile.getInt(name + ".remote.timeout", 2000));
        FileWatching.getInstance().register(
                profile.getString(name + ".remote.path.read"),
                listener
        );
    }

}
