package io.store.ua.events;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class LoginEvent extends ApplicationEvent {
  private final UserDetails userDetails;

  public LoginEvent(UserDetails userDetails) {
    super(userDetails, Clock.systemUTC());
    this.userDetails = userDetails;
  }
}
