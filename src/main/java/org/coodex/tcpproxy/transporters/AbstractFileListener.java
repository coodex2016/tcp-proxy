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

import org.coodex.util.Common;
import org.coodex.util.SingletonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractFileListener implements FileListener {

    private final static Logger log = LoggerFactory.getLogger(AbstractFileListener.class);
    private final static ScheduledExecutorService retryPool = Executors.newSingleThreadScheduledExecutor();
    private final String name;
    private final int timeOut;
    private SingletonMap<Long, SessionState> sessionStatus = new SingletonMap<>(
            new SingletonMap.Builder<Long, SessionState>() {
                @Override
                public SessionState build(Long key) {
                    return new SessionState();
                }
            }
    );

    protected AbstractFileListener(String name, int timeOut) {
        this.name = name;
        this.timeOut = timeOut;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean handle(Path path) {
        Command command = buildCommand(path);
        return command != null && command.getName().equalsIgnoreCase(name);
    }

    protected abstract String suffix();

    protected abstract AbstractFileBasedTransporter getTransporterById(Long id);

    protected Command buildCommand(Path path) {
        File file = path.toFile();
        String fileName = file.getName();
        if (!file.exists() || !fileName.endsWith("." + suffix())) return null;

        String cmd = fileName.substring(0, fileName.length() - suffix().length() - 1);
        String[] properties = Common.toArray(cmd, "_", new String[0]);

        if (properties.length >= 3) {
            Command command = new Command();
            command.path = path;
            command.name = properties[0];
            command.sessionId = Long.valueOf(properties[1]);
            if ("close".equalsIgnoreCase(properties[2])) {
                command.closed = true;
                return command;
            } else {
                command.index = Long.valueOf(properties[2]);
                command.size = Integer.valueOf(properties[3]);
                return (file.length() == command.getSize()) ? command : null;
            }
        }
        return null;
    }

    @Override
    public synchronized void process(final Path path) {
        Command command = buildCommand(path);
        if (command == null) return;
        boolean deleteFile = true;
        final File f = path.toFile();
        try {
            AbstractFileBasedTransporter transporter = getTransporterById(command.getSessionId());
            if (transporter == null) {
                return;
            }

            if (command.isClosed()) {
                log.debug("transporter {} closed.", transporter.getId());
                transporter.closeTransporter();
                sessionStatus.remove(command.getSessionId());
                return;
            }

            SessionState sessionState = sessionStatus.getInstance(command.getSessionId());

            synchronized (sessionState) {
                if (sessionState.required > command.index) {
                    return;
                } else if (sessionState.required == command.index) {
                    sessionState.required++;
                    sessionState.outOfDate = 0l;
                } else {
                    if (sessionState.outOfDate == 0) {
                        sessionState.outOfDate = System.currentTimeMillis() + timeOut;
                    }
                    if (sessionState.outOfDate < System.currentTimeMillis()) {
                        log.info("transporter {}, {} missing package: {}",
                                name,
                                command.getSessionId(),
                                sessionState.required);
                        transporter.closeTransporter();
                        sessionStatus.remove(command.getSessionId());
                    } else {
                        deleteFile = false;
                        retryPool.schedule(new Runnable() {
                            @Override
                            public void run() {
                                process(path);
                            }
                        }, 50, TimeUnit.MILLISECONDS);
                    }
                    return;
                }
            }


            try {
                InputStream inputStream = new FileInputStream(f);
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    Common.copyStream(inputStream, byteArrayOutputStream);
                    byte[] data = transporter.getEncryptor().decrypt(byteArrayOutputStream.toByteArray());
                    transporter.read(data);
                } finally {
                    inputStream.close();

                }
            } catch (Throwable th) {
                throw th instanceof RuntimeException ?
                        (RuntimeException) th : new RuntimeException(th.getLocalizedMessage(), th);
            }
        } finally {
            if (deleteFile) {
                retryPool.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (f.exists()) f.delete();
                    }
                }, 50, TimeUnit.MILLISECONDS);
            }
        }

    }

    protected static class SessionState {
        private int required = 1;
        private long outOfDate = 0l;
    }

    protected static class Command {
        private String name;
        private Long sessionId;
        private Path path;
        private Long index;
        private int size;
        private boolean closed;

        public String getName() {
            return name;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public Path getPath() {
            return path;
        }

        public Long getIndex() {
            return index;
        }

        public int getSize() {
            return size;
        }

        public boolean isClosed() {
            return closed;
        }
    }

}
