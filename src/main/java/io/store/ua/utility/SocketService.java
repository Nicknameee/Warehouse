package io.store.ua.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void pushToTopic(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend("/topic" + topic, RegularObjectMapper.writeToString(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
