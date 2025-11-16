package io.store.ua.controller;

import io.store.ua.AbstractIT;
import io.store.ua.configuration.ApplicationWebsocketConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketIT extends AbstractIT {
    private static final long TIMEOUT_SECONDS = 10;
    private static final String SUBSCRIBE_TOPIC = "%s/greetings".formatted(ApplicationWebsocketConfiguration.WEBSOCKET_TOPIC);
    private static final String SEND_DESTINATION = "%s/hello".formatted(ApplicationWebsocketConfiguration.WEBSOCKET_NOTIFICATIONS);
    private static final String outcomingMessage = RandomStringUtils.secure().nextAlphanumeric(10);
    private static final AtomicReference<String> incomingMessage = new AtomicReference<>();

    @LocalServerPort
    private int port;

    @Value("${local.server.host:localhost}")
    private String host;

    private String WEBSOCKET_URI;
    private CompletableFuture<String> messageFuture;
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        this.messageFuture = new CompletableFuture<>();
        this.WEBSOCKET_URI = "ws://%s:%s/socket".formatted(host, port);
        this.stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        this.stompClient.setMessageConverter(new StringMessageConverter());
        incomingMessage.set(null);
    }

    @Test
    void testStompMessageBroker() throws Exception {
        String messageToSend = GENERATOR.nextAlphanumeric(10);
        StompSessionHandlerAdapter sessionHandler = new StompSessionHandler(messageFuture, messageToSend);
        StompSession session = stompClient.connectAsync(WEBSOCKET_URI, sessionHandler).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            String receivedReply = messageFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(outcomingMessage, receivedReply);
            assertEquals(messageToSend, incomingMessage.get());
        } finally {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private static class StompSessionHandler extends StompSessionHandlerAdapter {
        private final CompletableFuture<String> messageFuture;
        private final String messageToSend;

        public StompSessionHandler(CompletableFuture<String> messageFuture, String messageToSend) {
            this.messageFuture = messageFuture;
            this.messageToSend = messageToSend;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe(SUBSCRIBE_TOPIC, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    messageFuture.complete((String) payload);
                }
            });
            session.send(SEND_DESTINATION, messageToSend);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            messageFuture.completeExceptionally(exception);
        }
    }

    @TestConfiguration
    @Controller
    public static class WebSocketController {
        @MessageMapping("/hello")
        @SendTo(ApplicationWebsocketConfiguration.WEBSOCKET_TOPIC + "/greetings")
        public String handle(String clientMessage) {
            incomingMessage.set(clientMessage);

            return outcomingMessage;
        }
    }
}