package io.store.ua.events;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class LogoutEvent extends ApplicationEvent {
  private final UserDetails userDetails;

  public LogoutEvent(UserDetails userDetails) {
    super(userDetails, Clock.systemUTC());
    this.userDetails = userDetails;
  }
}
