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


import io.netty.util.internal.ConcurrentSet;
import org.coodex.util.Singleton;
import org.coodex.util.SingletonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileWatching {

    private final static Logger log = LoggerFactory.getLogger(FileWatching.class);
    private static FileWatching instance = new FileWatching();
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private Singleton<WatchService> watchServiceSingleton
            = new Singleton<>(new Singleton.Builder<WatchService>() {
        @Override
        public WatchService build() {
            try {
                return FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }
    });

    private boolean running = false;

    private SingletonMap<Path, Set<FileListener>> listenerMap =
            new SingletonMap<>(new SingletonMap.Builder<Path, Set<FileListener>>() {
                @Override
                public Set<FileListener> build(Path key) {
                    return new ConcurrentSet<>();
                }
            });

    private FileWatching() {
    }

    public static FileWatching getInstance() {
        return instance;
    }

    public void deregister(String dir, FileListener listener) {
        Path path = Paths.get(dir);
        Set<FileListener> listeners = listenerMap.getInstance(path);
        if (listeners.contains(listener)) {
            synchronized (listener) {
                if (listeners.contains(listener)) {
                    listeners.remove(listener);
                }
            }
        }
    }

    public void register(String dir, FileListener listener) {
        Path path = Paths.get(dir);
        Set<FileListener> listeners = listenerMap.getInstance(path);

        synchronized (listeners) {
            try {
                if (listeners.size() == 0) {
                    path.register(watchServiceSingleton.getInstance(),
                            ENTRY_MODIFY, ENTRY_CREATE);
                    listeners.add(new FileListener() {
                        @Override
                        public boolean handle(Path path) {
                            return false;
                        }

                        @Override
                        public void process(Path path) {

                        }
                    });
                    start();
                }
                listeners.add(listener);
                log.debug("{} listener count: {}", path, listeners.size());
            } catch (IOException ie) {
                throw new RuntimeException(ie.getLocalizedMessage(), ie);
            }
        }
    }

    public synchronized void start() {
        if (!running) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        WatchService watchService = watchServiceSingleton.getInstance();
                        while (true) {
                            try {
                                WatchKey watchKey = watchService.take();
                                Path dir = (Path) watchKey.watchable();
                                for (WatchEvent watchEvent : watchKey.pollEvents()) {
                                    final Path path = dir.resolve((Path) watchEvent.context());
                                    Set<FileListener> listeners = listenerMap.getInstance(dir);
                                    for (final FileListener listener : listeners) {
                                        if (listener.handle(path)) {
                                            executorService.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.process(path);
                                                }
                                            });
                                            break;
                                        }
                                    }
                                }
                                watchKey.reset();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e.getLocalizedMessage(), e);
                            }
                        }
                    } finally {
                        running = false;
                    }
                }
            }.start();
            running = true;
        }
    }

}
