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


/**
 * 两对socket之间的数据传输器
 */
public interface Transporter {


    /**
     * 当两对socket之间有数据到达时调用
     *
     * @param buf
     */
    void read(byte[] buf);

    /**
     * 向传输通道内写入数据
     *
     * @param buf
     */
    void write(byte[] buf);

    /**
     * 下达关闭对端通道指令
     */
    void close();

    /**
     * 会话id
     *
     * @return
     */
    Long getId();


}
