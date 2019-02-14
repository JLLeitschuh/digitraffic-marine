/**
 * -----
 * Copyright (C) 2018 Digia
 * -----
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * 2019.02.14: Original work is used here as an base implementation
 */
package fi.livi.digitraffic.meri.controller.reader;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import fi.livi.digitraffic.meri.controller.AisMessageConverter;
import fi.livi.digitraffic.meri.controller.MessageConverter;
import fi.livi.digitraffic.meri.controller.ais.AisRadioMsg;
import fi.livi.digitraffic.meri.controller.ais.AisRadioMsgParser;
import fi.livi.digitraffic.meri.model.ais.AISMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("ais.reader.enabled")
public class AisMessageReader implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(AisMessageReader.class);

    private final AisTcpSocketClient aisTcpSocketClient;
    private String cachedFirstPart = null;
    private AtomicBoolean readinEnabled = new AtomicBoolean(false);

    private final LinkedBlockingQueue<AisRadioMsg> queue = new LinkedBlockingQueue<AisRadioMsg>(16384);

    public AisMessageReader(final AisTcpSocketClient aisTcpSocketClient) {
        this.aisTcpSocketClient = aisTcpSocketClient;

        try {
            aisTcpSocketClient.connect();
        } catch (IOException ioe) {
            log.error("Failed to initialize AIS-connector", ioe);
        }
    }

    public AisRadioMsg getAisRadioMessage() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            log.error("Failed to get ais message from queue", e);
        }

        return null;
    }

    @PostConstruct
    public void setUp() {
        readinEnabled.set(true);

        Executors.newSingleThreadExecutor().submit(this);
    }

    @PreDestroy
    public void destroy() {
        log.debug("Shutting down ais reader");
        try {
            readinEnabled.set(false);
            aisTcpSocketClient.close();
        } catch (Exception e) {
            log.error("Failed to close connection", e);
        }
    }

    @Override
    public void run() {
        while (readinEnabled.get()) {
            if (aisTcpSocketClient.isConnected()) {
                final String rawAisMessage = aisTcpSocketClient.readLine();

                if (rawAisMessage != null) {
                    if (AisRadioMsgParser.isSupportedMessageType(rawAisMessage)) {
                        final boolean multipartMessage = AisRadioMsgParser.isMultipartRadioMessage(rawAisMessage);

                        if (cachedFirstPart != null && !(multipartMessage && AisRadioMsgParser.getPartNumber(rawAisMessage) == 2)) {
                             cachedFirstPart = null;
                        }

                        // TODO! no response, no execption
                        AisRadioMsgParser.validateChecksum(rawAisMessage);

                        if (!multipartMessage || (cachedFirstPart != null && AisRadioMsgParser.getPartNumber(rawAisMessage) == 2)) {
                            // Parse message and deliver result
                            final AisRadioMsg msg = parse(rawAisMessage);

                            if (msg != null) {
                                if (!queue.offer(msg)) {
                                    log.warn("AIS message queue is full. message is ignored");
                                }
                            }
                        } else if (AisRadioMsgParser.getPartNumber(rawAisMessage) == 1) {
                            // cache first part now
                            cachedFirstPart = rawAisMessage;
                        }
                    } else {
                        log.info("Not supported message");
                        if (cachedFirstPart != null) {
                            cachedFirstPart = null;
                        }
                    }
                } else {
                    try {
                        // Do small sleep and continue
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                // Connection failure -> let reconnection handler handle this
                log.warn("Unable to establish connection to AIS-connector, waiting retry");

                try {
                    // Do small sleep and continue
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        readinEnabled.set(false);

        /**
        try {
            if (aisTcpSocketClient.connect()) {
                while (readinEnabled.get()) {
                    final String rawAisMessage = aisTcpSocketClient.readLine();
                    final boolean multipartMessage = AisRadioMsgParser.isMultipartRadioMessage(rawAisMessage);

                    //log.info("Raw message: {}, multipart: {}", rawAisMessage, multipartMessage);

                    if (rawAisMessage != null) {
                        if (AisRadioMsgParser.isSupportedMessageType(rawAisMessage)) {

                            if (cachedFirstPart != null && !(multipartMessage && AisRadioMsgParser.getPartNumber(rawAisMessage) == 2)) {
                                cachedFirstPart = null;
                            }

                            // TODO! no response, no execption
                            AisRadioMsgParser.validateChecksum(rawAisMessage);

                            if (!multipartMessage || (cachedFirstPart != null && AisRadioMsgParser.getPartNumber(rawAisMessage) == 2)) {
                                // Parse message and deliver result
                                final AisRadioMsg msg = parse(rawAisMessage);

                                if (msg != null) {
                                    if (!queue.offer(msg)) {
                                        log.warn("AIS message queue is full. message is ignored");
                                    }
                                }
                            } else if (AisRadioMsgParser.getPartNumber(rawAisMessage) == 1) {
                                // cache first part now
                                cachedFirstPart = rawAisMessage;
                            }
                        } else {
                            log.info("Not supported message");
                            if (cachedFirstPart != null) {
                                cachedFirstPart = null;
                            }
                        }
                    } else {
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            log.warn("keskeytetty, mut miks");
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else {
                log.warn("Unable to establish connection to VTS server");
            }
        } catch (IOException ioe) {
            log.error("Failed to establish connection", ioe);
        } catch (Exception e) {
            log.error("Undefined error", e);
        }
*/
    }

    private AisRadioMsg parse(final String messagePart) {
        AisRadioMsg msg;

        if (cachedFirstPart == null) {
            msg = AisRadioMsgParser.parseToAisRadioMessage(messagePart);
        } else {
            msg = AisRadioMsgParser.parseToAisRadioMessage(cachedFirstPart, messagePart);
            cachedFirstPart = null;
        }

        return msg;
    }
}
