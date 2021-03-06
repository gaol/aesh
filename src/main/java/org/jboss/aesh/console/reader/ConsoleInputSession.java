/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.reader;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 * @author Mike Brock
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ConsoleInputSession {
    private final InputStream consoleStream;
    private final AeshInputStream aeshInputStream;
    private final ExecutorService executorService;

    private final ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1000);

    private static final Logger LOGGER = LoggerUtil.getLogger(ConsoleInputSession.class.getName());

    public ConsoleInputSession(InputStream consoleStream) {
        this.consoleStream = consoleStream;
        if(Config.isOSPOSIXCompatible())
            executorService = Executors.newSingleThreadExecutor();
        else
            executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setDaemon(true);
                    return thread;
                }
            });
        aeshInputStream = new AeshInputStream(blockingQueue);
        startReader();
    }

    private void startReader() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bBuf = new byte[1024];
                    if(Config.isOSPOSIXCompatible()) {
                        while (!executorService.isShutdown()) {
                            int read = consoleStream.available();
                            if (read > 0) {
                                consoleStream.read(bBuf, 0, read);
                                blockingQueue.put(new String(bBuf, 0, read));
                            }
                            else if (read < 0) {
                                stop();
                            }
                            Thread.sleep(10);
                        }
                    }
                    else {
                        while (!executorService.isShutdown()) {
                            int read = consoleStream.read(bBuf);
                            if (read > 0) {
                                blockingQueue.put(new String(bBuf, 0, read));
                            }
                            else if (read < 0) {
                                stop();
                            }
                            Thread.sleep(10);
                        }
                    }
                }
                catch (RuntimeException e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                        throw e;
                    }
                }
                catch (Exception e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                    }
                    try {
                        stop();
                    }
                    catch (InterruptedException | IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        };

        executorService.execute(reader);
    }


    public void stop() throws IOException, InterruptedException {
        if(!executorService.isShutdown()) {
            consoleStream.close();
            executorService.shutdown();
            aeshInputStream.close();
            LOGGER.info("input stream is closed, readers finished...");
        }
    }

    public AeshInputStream getExternalInputStream() {
        return aeshInputStream;
    }
}
