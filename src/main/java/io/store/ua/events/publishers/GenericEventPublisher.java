package io.store.ua.events.publishers;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenericEventPublisher<T> {
  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishEvent(T event) {
    applicationEventPublisher.publishEvent(event);
  }
}
