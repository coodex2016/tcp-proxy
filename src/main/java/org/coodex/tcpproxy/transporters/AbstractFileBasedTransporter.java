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

import org.coodex.concurrent.Coalition;
import org.coodex.concurrent.Debouncer;
import org.coodex.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.coodex.tcpproxy.transporters.AbstractFileBasedTransporter.Type.RESPONSE;


public abstract class AbstractFileBasedTransporter extends AbstractTransporter {

    private final static Logger log = LoggerFactory.getLogger(AbstractFileBasedTransporter.class);

    private final Encryptor encryptor;
    private final String path;
    private AtomicLong seq = new AtomicLong(0);
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private Debouncer<ByteArrayOutputStream> debouncer = new Debouncer<>(
            new Coalition.Callback<ByteArrayOutputStream>() {
                @Override
                public void call(ByteArrayOutputStream arg) {
                    synchronized (AbstractFileBasedTransporter.this) {
                        byte[] buf = outputStream.toByteArray();
                        outputStream = new ByteArrayOutputStream();
                        int offset = 0;
                        while (offset < buf.length) {
                            // 输出file
                            int blockSize = Math.min(buf.length - offset, getBlockSize());
                            try {
                                writeToFile(encryptor.encrypt(buf, offset, blockSize));
                            } catch (Throwable th) {
                                throw th instanceof RuntimeException ?
                                        (RuntimeException) th : new RuntimeException(th.getLocalizedMessage(), th);
                            }
                            offset += getBlockSize();
                        }
                    }
                }
            }, 50
    );

    public AbstractFileBasedTransporter(String name, String path, Encryptor encryptor, int waitingTimeOut) {
        super(name);
        this.path = path;
        this.encryptor = encryptor;
    }

    public Encryptor getEncryptor() {
        return encryptor;
    }

    protected abstract Type getType();

    protected abstract int getBlockSize();

    @Override
    public void write(byte[] buf) {
        synchronized (this) {
            try {
                outputStream.write(buf);
                debouncer.call(outputStream);
            } catch (IOException e) {

            }
        }
    }


    /**
     * 文件名格式：name_sessionId_index_size.[req|res]
     */
    private void writeToFile(byte[] buf) throws IOException {
        String fileName = String.format("%s_%d_%d_%d.%s",
                getName(), getId(), seq.incrementAndGet(), buf.length, fileType());
        File file = Common.getNewFile(path + Common.FILE_SEPARATOR + fileName);
        OutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(buf);
        } finally {
            outputStream.close();
        }

    }

    protected String fileType() {
        return RESPONSE.equals(getType()) ? "res" : "req";
    }

    public void close() {
        String fileName = String.format("%s_%d_close.%s", getName(), getId(), fileType());
        try {
            Common.getNewFile(path + Common.FILE_SEPARATOR + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * 关闭通道及其引用
     */
    protected abstract void closeTransporter();

    public enum Type {
        REQUEST, RESPONSE
    }

}
