package io.store.ua.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("rabbitmq")
public class SocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void pushToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend("/topic" + topic, payload);
    }

    public void pushToQueue(String queue, Object payload) {
        messagingTemplate.convertAndSend("/queue" + queue, payload);
    }
}
